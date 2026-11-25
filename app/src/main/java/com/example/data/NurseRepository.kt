package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class NurseRepository {
  private val rawUrl = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
  private val anonKey = BuildConfig.SUPABASE_ANON_KEY.trim()

  val isConfigured: Boolean
    get() =
      rawUrl.startsWith("https://") &&
        !rawUrl.contains("YOUR_SUPABASE", ignoreCase = true) &&
        anonKey.isNotBlank() &&
        !anonKey.contains("YOUR_SUPABASE", ignoreCase = true)

  private val api: SupabaseApi? by lazy {
    if (!isConfigured) return@lazy null
    val logging =
      HttpLoggingInterceptor().apply {
        level =
          if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
          else HttpLoggingInterceptor.Level.NONE
      }
    val client =
      OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
          val req =
            chain
              .request()
              .newBuilder()
              .addHeader("apikey", anonKey)
              .addHeader("Authorization", "Bearer $anonKey")
              .addHeader("Content-Type", "application/json")
              .build()
          chain.proceed(req)
        }
        .addInterceptor(logging)
        .build()

    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    Retrofit.Builder()
      .baseUrl("$rawUrl/")
      .client(client)
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
        service.upsertNurse(defaultAdmin)
        list = service.getAllNurses()
      }
      list
    }
  }

  suspend fun upsertNurse(record: NurseRecord): Result<List<NurseRecord>> {
    return runCatchingSafe {
      val service = requireApi()
      service.upsertNurse(record)
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

  private inline fun <T> runCatchingSafe(block: () -> T): Result<T> {
    return try {
      Result.success(block())
    } catch (ce: CancellationException) {
      throw ce
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
