package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.net.URLEncoder
import java.util.Collections
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class NurseRepository {
  private val rawUrl = BuildConfig.SUPABASE_URL.trim().trim('"', '\'').trim().trimEnd('/')
  private val anonKey = BuildConfig.SUPABASE_ANON_KEY.trim().trim('"', '\'').trim()

  private val buildEmailJsConfig =
    EmailJsConfig(
      serviceId = BuildConfig.EMAILJS_SERVICE_ID.trim().trim('"', '\'').trim(),
      templateId = BuildConfig.EMAILJS_TEMPLATE_ID.trim().trim('"', '\'').trim(),
      publicKey = BuildConfig.EMAILJS_PUBLIC_KEY.trim().trim('"', '\'').trim(),
    )

  private val unsupportedColumns: MutableSet<String> =
    Collections.synchronizedSet(mutableSetOf())

  val isConfigured: Boolean
    get() =
      rawUrl.startsWith("https://") &&
        !rawUrl.contains("YOUR_SUPABASE", ignoreCase = true) &&
        anonKey.isNotBlank() &&
        !anonKey.contains("YOUR_SUPABASE", ignoreCase = true)

  val isBuildEmailRecoveryConfigured: Boolean
    get() = buildEmailJsConfig.isValid

  private val moshi: Moshi by lazy {
    Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  }

  private val nurseJsonAdapter by lazy {
    moshi.adapter(NurseRecord::class.java).serializeNulls()
  }

  private val httpClient: OkHttpClient by lazy {
    val logging =
      HttpLoggingInterceptor().apply {
        level =
          if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
          else HttpLoggingInterceptor.Level.NONE
      }
    OkHttpClient.Builder()
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(20, TimeUnit.SECONDS)
      .writeTimeout(20, TimeUnit.SECONDS)
      .pingInterval(25, TimeUnit.SECONDS)
      .addInterceptor { chain ->
        val original = chain.request()
        if (original.url.host.contains("emailjs.com", ignoreCase = true)) {
          chain.proceed(original)
        } else {
          val builder =
            original
              .newBuilder()
              .addHeader("apikey", anonKey)
              .addHeader("Content-Type", "application/json")
          if (anonKey.startsWith("eyJ")) {
            builder.addHeader("Authorization", "Bearer $anonKey")
          }
          chain.proceed(builder.build())
        }
      }
      .addInterceptor(logging)
      .build()
  }

  private val api: SupabaseApi? by lazy {
    if (!isConfigured) return@lazy null
    Retrofit.Builder()
      .baseUrl("$rawUrl/")
      .client(httpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi).withNullSerialization())
      .build()
      .create(SupabaseApi::class.java)
  }

  private fun requireApi(): SupabaseApi =
    api
      ?: throw IllegalStateException(
        "Supabase সংযোগ সেট করা নেই। AI Studio Secrets প্যানেলে SUPABASE_URL এবং SUPABASE_ANON_KEY যোগ করুন।"
      )

  suspend fun fetchAllAndEnsureAdmin(): Result<List<NurseRecord>> {
    return runCatchingSafe {
      val service = requireApi()
      var list = service.getAllNurses()
      if (list.none { it.role == "admin" }) {
        val defaultAdmin =
          NurseRecord(
            phone = "admin",
            role = "admin",
            name = "Administrator",
            pw = DirectoryConstants.sha256Password("admin123"),
            mustChange = true,
            status = "approved",
          )
        upsertWithSchemaFallback(service, defaultAdmin)
        list = service.getAllNurses()
      }
      list
    }
  }

  suspend fun upsertNurse(record: NurseRecord): Result<List<NurseRecord>> {
    return runCatchingSafe {
      val service = requireApi()
      upsertWithSchemaFallback(service, record)
      service.getAllNurses()
    }
  }

  suspend fun deleteNurse(phone: String): Result<List<NurseRecord>> {
    return runCatchingSafe {
      val service = requireApi()
      service.deleteNurse("eq.$phone")
      service.getAllNurses()
    }
  }

  private suspend fun upsertWithSchemaFallback(
    service: SupabaseApi,
    record: NurseRecord,
  ): List<NurseRecord> {
    val payload = JSONObject(nurseJsonAdapter.toJson(record))
    if (payload.isNull("created_at")) {
      payload.remove("created_at")
    }
    synchronized(unsupportedColumns) {
      unsupportedColumns.forEach { payload.remove(it) }
    }

    var attempt = 0
    while (true) {
      attempt++
      try {
        val body =
          payload
            .toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())
        return service.upsertNurse(body)
      } catch (he: retrofit2.HttpException) {
        val errText =
          try {
            he.response()?.errorBody()?.string().orEmpty()
          } catch (_: Exception) {
            ""
          }
        val missingCol = extractMissingColumn(errText)
        if (he.code() == 400 && missingCol != null && payload.has(missingCol) && attempt <= 8) {
          unsupportedColumns.add(missingCol)
          payload.remove(missingCol)
          continue
        }
        val msg =
          if (errText.isNotBlank()) "HTTP ${he.code()}: $errText"
          else "HTTP ${he.code()} ${he.message()}"
        throw IllegalStateException(msg, he)
      }
    }
  }

  private fun extractMissingColumn(errorBody: String): String? {
    val match = MISSING_COLUMN_REGEX.find(errorBody)
    return match?.groupValues?.getOrNull(1)
  }

  fun observeRealtimeChanges(onConnectionChanged: (Boolean) -> Unit): Flow<Unit> = callbackFlow {
    if (!isConfigured) {
      onConnectionChanged(false)
      close()
      return@callbackFlow
    }

    val wsBase = rawUrl.replaceFirst("https://", "wss://").replaceFirst("http://", "ws://")
    val encodedKey = URLEncoder.encode(anonKey, "UTF-8")
    val wsUrl = "$wsBase/realtime/v1/websocket?apikey=$encodedKey&vsn=1.0.0"

    val reqBuilder = Request.Builder().url(wsUrl).addHeader("apikey", anonKey)
    if (anonKey.startsWith("eyJ")) {
      reqBuilder.addHeader("Authorization", "Bearer $anonKey")
    }

    val heartbeatJob =
      launch(Dispatchers.IO) {
        while (isActive) {
          delay(25_000L)
          activeSocketRef?.send(
            """{"topic":"phoenix","event":"heartbeat","payload":{},"ref":"hb"}"""
          )
        }
      }

    var socket: WebSocket? = null
    val listener =
      object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
          activeSocketRef = webSocket
          val joinPayload =
            JSONObject()
              .put("topic", "realtime:public:nurses")
              .put("event", "phx_join")
              .put(
                "payload",
                JSONObject()
                  .put(
                    "config",
                    JSONObject()
                      .put("broadcast", JSONObject().put("self", false))
                      .put("presence", JSONObject().put("key", ""))
                      .put(
                        "postgres_changes",
                        org.json.JSONArray()
                          .put(
                            JSONObject()
                              .put("event", "*")
                              .put("schema", "public")
                              .put("table", "nurses")
                          ),
                      ),
                  ),
              )
              .put("ref", "1")
          webSocket.send(joinPayload.toString())
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
          try {
            val json = JSONObject(text)
            val event = json.optString("event", "")
            val topic = json.optString("topic", "")
            when {
              event == "phx_reply" && topic == "realtime:public:nurses" -> {
                val status = json.optJSONObject("payload")?.optString("status", "")
                if (status == "ok") {
                  onConnectionChanged(true)
                }
              }
              event == "postgres_changes" || event == "INSERT" || event == "UPDATE" || event == "DELETE" -> {
                trySend(Unit)
              }
            }
          } catch (_: Exception) {
            // Ignore malformed socket frames
          }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
          onConnectionChanged(false)
          close()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
          onConnectionChanged(false)
          close()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
          onConnectionChanged(false)
          close(t)
        }
      }

    socket = httpClient.newWebSocket(reqBuilder.build(), listener)

    awaitClose {
      onConnectionChanged(false)
      heartbeatJob.cancel()
      if (activeSocketRef === socket) {
        activeSocketRef = null
      }
      socket.close(1000, "Flow closed")
    }
  }

  @Volatile private var activeSocketRef: WebSocket? = null

  suspend fun sendRecoveryCodeEmail(
    toEmail: String,
    userName: String,
    code: String,
    storedConfig: EmailJsConfig? = null,
  ): Result<Unit> {
    val effective =
      when {
        buildEmailJsConfig.isValid -> buildEmailJsConfig
        storedConfig?.isValid == true -> storedConfig
        else -> null
      }
    if (effective == null) {
      return Result.failure(
        IllegalStateException(
          "ইমেইল সার্ভিস (EmailJS) কনফিগার করা নেই। Global Admin প্যানেল থেকে EmailJS সেটিংস সেভ করুন অথবা নিচের 'Admin কে অনুরোধ পাঠান' ব্যবহার করুন।"
        )
      )
    }
    return withContext(Dispatchers.IO) {
      try {
        val params =
          JSONObject().apply {
            put("to_email", toEmail)
            put("to_name", userName)
            put("passcode", code)
            put("code", code)
            put("app_name", "BD Nurses Directory")
          }
        val payload =
          JSONObject().apply {
            put("service_id", effective.serviceId)
            put("template_id", effective.templateId)
            put("user_id", effective.publicKey)
            put("template_params", params)
          }
        val req =
          Request.Builder()
            .url("https://api.emailjs.com/api/v1.0/email/send")
            .post(
              payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            )
            .build()
        val response = httpClient.newCall(req).execute()
        response.use { resp ->
          if (!resp.isSuccessful) {
            val errBody = resp.body?.string().orEmpty()
            return@withContext Result.failure(
              IllegalStateException("EmailJS HTTP ${resp.code}: $errBody")
            )
          }
        }
        Result.success(Unit)
      } catch (ce: CancellationException) {
        throw ce
      } catch (e: Exception) {
        Result.failure(e)
      }
    }
  }

  private inline fun <T> runCatchingSafe(block: () -> T): Result<T> {
    return try {
      Result.success(block())
    } catch (ce: CancellationException) {
      throw ce
    } catch (he: retrofit2.HttpException) {
      val body =
        try {
          he.response()?.errorBody()?.string()?.trim().orEmpty()
        } catch (_: Exception) {
          ""
        }
      val msg =
        if (body.isNotBlank()) "HTTP ${he.code()}: $body"
        else "HTTP ${he.code()} ${he.message()}"
      Result.failure(IllegalStateException(msg, he))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  companion object {
    private val MISSING_COLUMN_REGEX =
      Regex("""Could not find the '([^']+)' column""")
  }
}
