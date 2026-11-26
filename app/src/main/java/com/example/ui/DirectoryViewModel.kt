package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DirectoryConstants
import com.example.data.EmailJsConfig
import com.example.data.NurseFormDraft
import com.example.data.NurseRecord
import com.example.data.NurseRepository
import com.example.data.PendingEdit
import com.example.data.ResetRequest
import com.example.data.extractEmailJsConfig
import com.example.data.toFormDraft
import com.example.util.QrCodeHelper
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

enum class AuthView {
  LOGIN,
  ADMIN_LOGIN,
  REGISTER,
  FORGOT_PASSWORD,
  REGISTRATION_SUBMITTED,
}

enum class MainTab {
  DIRECTORY,
  REQUESTS,
  ALL_USERS,
  MY_PROFILE,
  PASSWORD,
}

data class DirectoryFilter(
  val query: String = "",
  val category: String = "",
  val designation: String = "",
  val division: String = "",
  val district: String = "",
  val upazila: String = "",
  val workplace: String = "",
) {
  fun summary(): String =
    listOf(
        "Division" to division,
        "District" to district,
        "Upazila/Thana" to upazila,
        "Hospital" to workplace,
        "Category" to category,
        "Designation" to designation,
      )
      .filter { it.second.isNotBlank() }
      .joinToString(" | ") { "${it.first}: ${it.second}" }
}

data class DirectoryUiState(
  val isSupabaseConfigured: Boolean = true,
  val isEmailRecoveryConfigured: Boolean = false,
  val isRealtimeConnected: Boolean = false,
  val isLoading: Boolean = true,
  val isBusy: Boolean = false,
  val records: List<NurseRecord> = emptyList(),
  val currentUser: NurseRecord? = null,
  val authView: AuthView = AuthView.LOGIN,
  val activeTab: MainTab = MainTab.DIRECTORY,
  val editingPhone: String? = null,
  val isAddingNurseByManager: Boolean = false,
  val formDraft: NurseFormDraft = NurseFormDraft(),
  val showPasswordPromptDialog: Boolean = false,
  val qrTargetPhone: String? = null,
  val detailTargetPhone: String? = null,
  val pendingQrTargetPhone: String? = null,
  val showQrScanner: Boolean = false,
  val recoveryStepCodeSent: Boolean = false,
  val recoveryTargetPhone: String = "",
  val recoveryTargetEmailMasked: String = "",
  val filter: DirectoryFilter = DirectoryFilter(),
  val selectedPrintColumns: Set<String> = DirectoryConstants.PRINT_FIELDS.map { it.first }.toSet(),
  val errorMessage: String = "",
  val successMessage: String = "",
  val bannerNote: String = "",
  val printError: String = "",
) {
  val qrTargetNurse: NurseRecord?
    get() = qrTargetPhone?.let { p -> records.find { it.phone == p } }

  val detailTargetNurse: NurseRecord?
    get() = detailTargetPhone?.let { p -> records.find { it.phone == p } }
  val storedEmailJsConfig: EmailJsConfig?
    get() = records.extractEmailJsConfig()

  val newsTickerText: String
    get() =
      records.find { it.phone == "__news_ticker__" }
        ?.name
        ?.takeIf { it.isNotBlank() }
        ?: DirectoryConstants.DEFAULT_NEWS_TICKER

  val approvedNurses: List<NurseRecord>
    get() = records.filter { it.role == "user" && it.status == "approved" }

  val filteredNurses: List<NurseRecord>
    get() {
      val q = filter.query.trim().lowercase()
      val u = filter.upazila.trim().lowercase()
      val h = filter.workplace.trim().lowercase()
      return approvedNurses.filter { x ->
        (filter.category.isBlank() || x.category == filter.category) &&
          (filter.division.isBlank() || x.division == filter.division) &&
          (filter.designation.isBlank() || x.designation == filter.designation) &&
          (filter.district.isBlank() || x.district == filter.district) &&
          (u.isBlank() || x.upazila.lowercase().contains(u)) &&
          (h.isBlank() || x.workplace.lowercase().contains(h)) &&
          (q.isBlank() ||
            listOf(x.name, x.phone, x.bnmc).joinToString(" ").lowercase().contains(q))
      }
    }

  val scopedUsers: List<NurseRecord>
    get() {
      val mgr = currentUser ?: return emptyList()
      return records.filter { it.role == "user" && mgr.canManageNurse(it) }
    }

  val scopedAdminsAndEditors: List<NurseRecord>
    get() =
      records.filter {
        (it.role == "subadmin" || it.role == "editor") && it.phone != "admin"
      }

  val pendingCount: Int
    get() =
      scopedUsers.count {
        it.status == "pending" || (it.status == "approved" && it.pendingEdit != null) || it.resetReq != null
      }
}

class DirectoryViewModel(private val repository: NurseRepository = NurseRepository()) :
  ViewModel() {

  private val _uiState =
    MutableStateFlow(
      DirectoryUiState(
        isSupabaseConfigured = repository.isConfigured,
        isEmailRecoveryConfigured = repository.isBuildEmailRecoveryConfigured,
      )
    )
  val uiState: StateFlow<DirectoryUiState> = _uiState.asStateFlow()

  private val secureRandom = SecureRandom()
  @Volatile private var isSilentSyncRunning = false

  init {
    refreshData()
    startRealtimeObserver()
    startPeriodicAutoSync()
  }

  private fun nowIso(): String =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

  private fun startRealtimeObserver() {
    if (!repository.isConfigured) return
    viewModelScope.launch {
      while (isActive) {
        try {
          repository
            .observeRealtimeChanges { connected ->
              _uiState.update { it.copy(isRealtimeConnected = connected) }
            }
            .collect {
              delay(250L)
              refreshData(silent = true)
            }
        } catch (_: Exception) {
          _uiState.update { it.copy(isRealtimeConnected = false) }
        }
        delay(4_000L)
      }
    }
  }

  private fun startPeriodicAutoSync() {
    if (!repository.isConfigured) return
    viewModelScope.launch {
      while (isActive) {
        delay(15_000L)
        refreshData(silent = true)
      }
    }
  }

  fun refreshData(silent: Boolean = false) {
    if (!repository.isConfigured) {
      _uiState.update {
        it.copy(
          isSupabaseConfigured = false,
          isLoading = false,
          errorMessage =
            "Supabase সংযোগ সেট করা নেই। AI Studio Secrets প্যানেলে SUPABASE_URL এবং SUPABASE_ANON_KEY যোগ করুন।",
        )
      }
      return
    }
    if (silent && isSilentSyncRunning) return
    viewModelScope.launch {
      if (silent) isSilentSyncRunning = true
      if (!silent) {
        _uiState.update { it.copy(isLoading = true, errorMessage = "") }
      }
      try {
        repository
          .fetchAllAndEnsureAdmin()
          .onSuccess { list ->
            val emailReady =
              repository.isBuildEmailRecoveryConfigured ||
                (list.extractEmailJsConfig()?.isValid == true)
            _uiState.update { state ->
              val updatedCurrent =
                state.currentUser?.let { cur -> list.find { it.phone == cur.phone } }
              val pendingPhone = state.pendingQrTargetPhone
              val resolvedPending =
                if (updatedCurrent != null && pendingPhone != null) {
                  list.find { it.phone == pendingPhone && it.role == "user" && it.status == "approved" }
                } else null
              state.copy(
                isSupabaseConfigured = true,
                isEmailRecoveryConfigured = emailReady,
                isLoading = false,
                records = list,
                currentUser = updatedCurrent,
                activeTab = if (resolvedPending != null) MainTab.DIRECTORY else state.activeTab,
                detailTargetPhone = resolvedPending?.phone ?: state.detailTargetPhone,
                pendingQrTargetPhone = if (resolvedPending != null) null else pendingPhone,
              )
            }
          }
          .onFailure { err ->
            if (!silent) {
              _uiState.update {
                it.copy(
                  isLoading = false,
                  errorMessage = err.localizedMessage ?: "সার্ভার থেকে তথ্য লোড করা যায়নি",
                )
              }
            }
          }
      } finally {
        if (silent) isSilentSyncRunning = false
      }
    }
  }

  fun setAuthView(view: AuthView) {
    _uiState.update {
      it.copy(
        authView = view,
        errorMessage = "",
        successMessage = "",
        recoveryStepCodeSent = false,
        recoveryTargetPhone = "",
        recoveryTargetEmailMasked = "",
        formDraft = if (view == AuthView.REGISTER) NurseFormDraft() else it.formDraft,
      )
    }
  }

  fun selectTab(tab: MainTab) {
    _uiState.update {
      it.copy(
        activeTab = tab,
        editingPhone = null,
        isAddingNurseByManager = false,
        errorMessage = "",
        printError = "",
      )
    }
  }

  fun clearBannerNote() {
    _uiState.update { it.copy(bannerNote = "") }
  }

  fun dismissPasswordPrompt() {
    _uiState.update { it.copy(showPasswordPromptDialog = false) }
  }

  fun openQrCodeDialog(phone: String) {
    _uiState.update { it.copy(qrTargetPhone = phone) }
  }

  fun dismissQrCodeDialog() {
    _uiState.update { it.copy(qrTargetPhone = null) }
  }

  fun openNurseDetailDialog(phone: String) {
    _uiState.update { it.copy(detailTargetPhone = phone) }
  }

  fun dismissNurseDetailDialog() {
    _uiState.update { it.copy(detailTargetPhone = null) }
  }

  fun openQrScanner() {
    _uiState.update { it.copy(showQrScanner = true, errorMessage = "") }
  }

  fun dismissQrScanner() {
    _uiState.update { it.copy(showQrScanner = false) }
  }

  fun handleScannedOrDeepLinkQr(raw: String) {
    val phone = QrCodeHelper.extractNursePhone(raw)
    if (phone.isNullOrBlank()) {
      _uiState.update {
        it.copy(
          showQrScanner = false,
          errorMessage = "বৈধ নার্স QR কোড শনাক্ত করা যায়নি।",
        )
      }
      return
    }
    val state = _uiState.value
    if (state.currentUser == null) {
      _uiState.update {
        it.copy(
          showQrScanner = false,
          pendingQrTargetPhone = phone,
          successMessage = "📱 QR কোড পাওয়া গেছে ($phone)। বিস্তারিত দেখতে Login করুন।",
          errorMessage = "",
        )
      }
      return
    }
    if (state.isLoading && state.records.isEmpty()) {
      _uiState.update {
        it.copy(showQrScanner = false, pendingQrTargetPhone = phone, errorMessage = "")
      }
      return
    }
    val target =
      state.records.find { it.phone == phone && it.role == "user" && it.status == "approved" }
    if (target != null) {
      _uiState.update {
        it.copy(
          showQrScanner = false,
          activeTab = MainTab.DIRECTORY,
          editingPhone = null,
          isAddingNurseByManager = false,
          detailTargetPhone = target.phone,
          pendingQrTargetPhone = null,
          errorMessage = "",
        )
      }
    } else {
      _uiState.update {
        it.copy(
          showQrScanner = false,
          pendingQrTargetPhone = null,
          errorMessage = "এই QR কোডের ($phone) কোনো অনুমোদিত নার্স ডিরেক্টরিতে পাওয়া যায়নি।",
        )
      }
    }
  }

  fun openPasswordTabFromPrompt() {
    _uiState.update {
      it.copy(
        showPasswordPromptDialog = false,
        activeTab = MainTab.PASSWORD,
        editingPhone = null,
        isAddingNurseByManager = false,
        errorMessage = "",
      )
    }
  }

  fun loginUser(phone: String, password: String) {
    val p = phone.trim()
    val hash = DirectoryConstants.sha256Password(password)
    viewModelScope.launch {
      _uiState.update { it.copy(isBusy = true, errorMessage = "") }
      val latestRecords =
        repository.fetchAllAndEnsureAdmin().getOrElse { err ->
          _uiState.update {
            it.copy(isBusy = false, errorMessage = err.localizedMessage ?: "Connection error")
          }
          return@launch
        }
      val user = latestRecords.find { it.phone == p && it.role == "user" }
      val err =
        when {
          user == null || user.pw != hash -> "Phone বা Password ভুল"
          user.status == "pending" -> "আপনার আবেদন এখনো Admin approval এর অপেক্ষায়"
          user.status == "rejected" -> "আপনার আবেদন বাতিল করা হয়েছে"
          else -> null
        }
      if (err != null) {
        _uiState.update { it.copy(isBusy = false, records = latestRecords, errorMessage = err) }
      } else {
        _uiState.update { state ->
          val pendingPhone = state.pendingQrTargetPhone
          val resolved =
            pendingPhone?.let { target ->
              latestRecords.find {
                it.phone == target && it.role == "user" && it.status == "approved"
              }
            }
          state.copy(
            isBusy = false,
            records = latestRecords,
            currentUser = user,
            activeTab = MainTab.DIRECTORY,
            showPasswordPromptDialog = user?.mustChange == true,
            detailTargetPhone = resolved?.phone,
            pendingQrTargetPhone = null,
            errorMessage =
              if (pendingPhone != null && resolved == null)
                "QR কোডের ($pendingPhone) কোনো অনুমোদিত নার্স পাওয়া যায়নি।"
              else "",
            bannerNote = "",
          )
        }
      }
    }
  }

  fun loginAdmin(adminId: String, password: String) {
    val id = adminId.trim()
    val hash = DirectoryConstants.sha256Password(password)
    viewModelScope.launch {
      _uiState.update { it.copy(isBusy = true, errorMessage = "") }
      val latestRecords =
        repository.fetchAllAndEnsureAdmin().getOrElse { err ->
          _uiState.update {
            it.copy(isBusy = false, errorMessage = err.localizedMessage ?: "Connection error")
          }
          return@launch
        }
      val manager = latestRecords.find { it.isManagerRole && it.phone == id }
      if (manager == null || manager.pw != hash) {
        _uiState.update {
          it.copy(
            isBusy = false,
            records = latestRecords,
            errorMessage = "Admin/Editor ID বা Password ভুল",
          )
        }
      } else {
        _uiState.update { state ->
          val pendingPhone = state.pendingQrTargetPhone
          val resolved =
            pendingPhone?.let { target ->
              latestRecords.find {
                it.phone == target && it.role == "user" && it.status == "approved"
              }
            }
          state.copy(
            isBusy = false,
            records = latestRecords,
            currentUser = manager,
            activeTab = if (resolved != null) MainTab.DIRECTORY else MainTab.REQUESTS,
            showPasswordPromptDialog = manager.mustChange,
            detailTargetPhone = resolved?.phone,
            pendingQrTargetPhone = null,
            errorMessage =
              if (pendingPhone != null && resolved == null)
                "QR কোডের ($pendingPhone) কোনো অনুমোদিত নার্স পাওয়া যায়নি।"
              else "",
            bannerNote = "",
          )
        }
      }
    }
  }

  fun sendEmailRecoveryCode(emailOrPhone: String) {
    val q = emailOrPhone.trim()
    if (q.isBlank()) {
      _uiState.update { it.copy(errorMessage = "আপনার নিবন্ধিত Email অথবা Phone নম্বর দিন") }
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(isBusy = true, errorMessage = "", successMessage = "") }
      val latestRecords =
        repository.fetchAllAndEnsureAdmin().getOrElse { err ->
          _uiState.update {
            it.copy(isBusy = false, errorMessage = err.localizedMessage ?: "Connection error")
          }
          return@launch
        }
      val user =
        latestRecords.find {
          it.role == "user" &&
            it.status == "approved" &&
            (it.email.equals(q, ignoreCase = true) || it.phone == q)
        }
      if (user == null) {
        _uiState.update {
          it.copy(
            isBusy = false,
            records = latestRecords,
            errorMessage = "এই Email বা Phone নম্বরে কোনো approved account পাওয়া যায়নি",
          )
        }
        return@launch
      }
      if (user.email.isBlank() || !user.email.contains("@")) {
        _uiState.update {
          it.copy(
            isBusy = false,
            records = latestRecords,
            errorMessage =
              "এই অ্যাকাউন্টে কোনো Email যোগ করা নেই। নিচের 'Admin কে অনুরোধ পাঠান' ব্যবহার করুন।",
          )
        }
        return@launch
      }

      val code = (100000 + secureRandom.nextInt(900000)).toString()
      val expiresAt = (System.currentTimeMillis() + 15 * 60 * 1000L).toString()

      val saveResult =
        repository.upsertNurse(user.copy(recoveryCode = code, recoveryExp = expiresAt))
      saveResult
        .onFailure { err ->
          _uiState.update {
            it.copy(isBusy = false, errorMessage = err.localizedMessage ?: "Code save failed")
          }
        }
        .onSuccess { updatedList ->
          repository
            .sendRecoveryCodeEmail(
              toEmail = user.email,
              userName = user.name,
              code = code,
              storedConfig = updatedList.extractEmailJsConfig(),
            )
            .onSuccess {
              _uiState.update {
                it.copy(
                  isBusy = false,
                  records = updatedList,
                  recoveryStepCodeSent = true,
                  recoveryTargetPhone = user.phone,
                  recoveryTargetEmailMasked = maskEmail(user.email),
                  successMessage =
                    "✅ ${maskEmail(user.email)} ঠিকানায় ৬ ডিজিটের ভেরিফিকেশন কোড পাঠানো হয়েছে (১৫ মিনিট মেয়াদ)।",
                  errorMessage = "",
                )
              }
            }
            .onFailure { mailErr ->
              _uiState.update {
                it.copy(
                  isBusy = false,
                  records = updatedList,
                  errorMessage = mailErr.localizedMessage ?: "ইমেইল পাঠানো যায়নি",
                )
              }
            }
        }
    }
  }

  fun verifyEmailCodeAndResetPassword(code: String, newPw: String, confirmPw: String) {
    val state = _uiState.value
    val targetPhone = state.recoveryTargetPhone
    val trimmedCode = code.trim()
    if (trimmedCode.length != 6) {
      _uiState.update { it.copy(errorMessage = "৬ ডিজিটের কোড লিখুন") }
      return
    }
    if (newPw.length < 4 || newPw != confirmPw || newPw == "1234" || newPw == "admin123") {
      _uiState.update {
        it.copy(
          errorMessage = "Password কমপক্ষে ৪ অক্ষর, মিলতে হবে এবং default password হওয়া যাবে না"
        )
      }
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(isBusy = true, errorMessage = "", successMessage = "") }
      val latestRecords =
        repository.fetchAllAndEnsureAdmin().getOrElse { err ->
          _uiState.update {
            it.copy(isBusy = false, errorMessage = err.localizedMessage ?: "Connection error")
          }
          return@launch
        }
      val user = latestRecords.find { it.phone == targetPhone }
      if (user == null) {
        _uiState.update {
          it.copy(isBusy = false, records = latestRecords, errorMessage = "অ্যাকাউন্ট পাওয়া যায়নি")
        }
        return@launch
      }
      val expMillis = user.recoveryExp.toLongOrNull() ?: 0L
      if (user.recoveryCode != trimmedCode) {
        _uiState.update {
          it.copy(isBusy = false, records = latestRecords, errorMessage = "ভেরিফিকেশন কোড ভুল হয়েছে")
        }
        return@launch
      }
      if (System.currentTimeMillis() > expMillis) {
        _uiState.update {
          it.copy(
            isBusy = false,
            records = latestRecords,
            errorMessage = "কোডের মেয়াদ শেষ হয়ে গেছে। আবার কোড পাঠান।",
          )
        }
        return@launch
      }

      repository
        .upsertNurse(
          user.copy(
            pw = DirectoryConstants.sha256Password(newPw),
            mustChange = false,
            recoveryCode = "",
            recoveryExp = "",
          )
        )
        .onSuccess { updated ->
          _uiState.update {
            it.copy(
              isBusy = false,
              records = updated,
              recoveryStepCodeSent = false,
              recoveryTargetPhone = "",
              successMessage =
                "✅ Password সফলভাবে পরিবর্তন হয়েছে! এখন নতুন Password দিয়ে Login করুন।",
              errorMessage = "",
            )
          }
        }
        .onFailure { err ->
          _uiState.update {
            it.copy(isBusy = false, errorMessage = err.localizedMessage ?: "Password reset failed")
          }
        }
    }
  }

  fun sendPasswordResetRequest(phone: String) {
    val p = phone.trim()
    viewModelScope.launch {
      _uiState.update { it.copy(isBusy = true, errorMessage = "", successMessage = "") }
      val latestRecords =
        repository.fetchAllAndEnsureAdmin().getOrElse { err ->
          _uiState.update {
            it.copy(isBusy = false, errorMessage = err.localizedMessage ?: "Connection error")
          }
          return@launch
        }
      val user = latestRecords.find { it.phone == p && it.role == "user" && it.status == "approved" }
      if (user == null) {
        _uiState.update {
          it.copy(
            isBusy = false,
            records = latestRecords,
            errorMessage = "এই নম্বরে কোনো approved account পাওয়া যায়নি",
          )
        }
        return@launch
      }
      repository
        .upsertNurse(user.copy(resetReq = ResetRequest(at = nowIso())))
        .onSuccess { updated ->
          _uiState.update {
            it.copy(
              isBusy = false,
              records = updated,
              successMessage = "✅ অনুরোধ Admin এর কাছে পাঠানো হয়েছে। Approve হলে আবার login করুন।",
            )
          }
        }
        .onFailure { err ->
          _uiState.update {
            it.copy(isBusy = false, errorMessage = err.localizedMessage ?: "Request failed")
          }
        }
    }
  }

  fun logout() {
    _uiState.update {
      it.copy(
        currentUser = null,
        editingPhone = null,
        isAddingNurseByManager = false,
        showPasswordPromptDialog = false,
        qrTargetPhone = null,
        detailTargetPhone = null,
        showQrScanner = false,
        authView = AuthView.LOGIN,
        errorMessage = "",
        successMessage = "",
        bannerNote = "",
      )
    }
  }

  fun updateDraft(transform: (NurseFormDraft) -> NurseFormDraft) {
    _uiState.update { it.copy(formDraft = transform(it.formDraft), errorMessage = "") }
  }

  fun startEditing(phone: String) {
    val target = _uiState.value.records.find { it.phone == phone } ?: return
    _uiState.update {
      it.copy(
        editingPhone = phone,
        isAddingNurseByManager = false,
        formDraft = target.toFormDraft(),
        errorMessage = "",
      )
    }
  }

  fun startAddingNurseByManager() {
    val mgr = _uiState.value.currentUser ?: return
    _uiState.update {
      it.copy(
        editingPhone = null,
        isAddingNurseByManager = true,
        formDraft =
          NurseFormDraft(
            division = mgr.scopeDivision,
            district = mgr.scopeDistrict,
            upazila = mgr.scopeUpazila,
            workplace = mgr.scopeWorkplace,
          ),
        errorMessage = "",
      )
    }
  }

  fun cancelEditing() {
    _uiState.update {
      it.copy(editingPhone = null, isAddingNurseByManager = false, errorMessage = "")
    }
  }

  fun submitRegistration() {
    val draft = _uiState.value.formDraft
    val validationError = draft.validate()
    if (validationError != null) {
      _uiState.update { it.copy(errorMessage = validationError) }
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(isBusy = true, errorMessage = "") }
      val latestRecords =
        repository.fetchAllAndEnsureAdmin().getOrElse { err ->
          _uiState.update {
            it.copy(isBusy = false, errorMessage = err.localizedMessage ?: "Connection error")
          }
          return@launch
        }
      val cleanPhone = draft.phone.trim()
      if (latestRecords.any { it.phone == cleanPhone }) {
        _uiState.update {
          it.copy(isBusy = false, records = latestRecords, errorMessage = "এই নম্বর আগেই নিবন্ধিত")
        }
        return@launch
      }
      val newRecord = draftToRecord(draft = draft, status = "pending")
      repository
        .upsertNurse(newRecord)
        .onSuccess { updated ->
          _uiState.update {
            it.copy(
              isBusy = false,
              records = updated,
              authView = AuthView.REGISTRATION_SUBMITTED,
              errorMessage = "",
            )
          }
        }
        .onFailure { err ->
          _uiState.update {
            it.copy(isBusy = false, errorMessage = err.localizedMessage ?: "Registration failed")
          }
        }
    }
  }

  fun submitAddNurseByManager() {
    val mgr = _uiState.value.currentUser ?: return
    val draft = _uiState.value.formDraft
    val validationError = draft.validate()
    if (validationError != null) {
      _uiState.update { it.copy(errorMessage = validationError) }
      return
    }
    val candidate = draftToRecord(draft = draft, status = "approved")
    if (!mgr.canManageNurse(candidate)) {
      _uiState.update {
        it.copy(
          errorMessage =
            "আপনার নির্ধারিত এলাকা (${mgr.scopeLabel()}) এর বাইরে নার্স যোগ করা যাবে না।"
        )
      }
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(isBusy = true, errorMessage = "") }
      val latestRecords =
        repository.fetchAllAndEnsureAdmin().getOrElse { err ->
          _uiState.update {
            it.copy(isBusy = false, errorMessage = err.localizedMessage ?: "Connection error")
          }
          return@launch
        }
      if (latestRecords.any { it.phone == candidate.phone }) {
        _uiState.update {
          it.copy(isBusy = false, records = latestRecords, errorMessage = "এই নম্বর আগেই নিবন্ধিত")
        }
        return@launch
      }
      repository
        .upsertNurse(candidate)
        .onSuccess { updated ->
          _uiState.update {
            it.copy(
              isBusy = false,
              records = updated,
              isAddingNurseByManager = false,
              bannerNote =
                "নতুন নার্স সফলভাবে যোগ ও Approve হয়েছে ✅ (Default Password: 1234)",
              errorMessage = "",
            )
          }
        }
        .onFailure { err ->
          _uiState.update {
            it.copy(isBusy = false, errorMessage = err.localizedMessage ?: "Add nurse failed")
          }
        }
    }
  }

  fun saveEdit() {
    val state = _uiState.value
    val me = state.currentUser ?: return
    val targetPhone = state.editingPhone ?: return
    val target = state.records.find { it.phone == targetPhone } ?: return
    val draft = state.formDraft
    val validationError = draft.validate()
    if (validationError != null) {
      _uiState.update { it.copy(errorMessage = validationError) }
      return
    }
    val formattedDob = DirectoryConstants.formatDateDdMmYyyy(draft.dob)
    val formattedJoinDate = DirectoryConstants.formatDateDdMmYyyy(draft.joinDate)
    val prl = DirectoryConstants.calculatePrlDate(formattedDob)
    val updatedRecord =
      if (me.isManagerRole) {
        target.copy(
          category = draft.category.trim(),
          name = draft.name.trim(),
          father = draft.father.trim(),
          mother = draft.mother.trim(),
          dob = formattedDob,
          prl = prl,
          joinDate = formattedJoinDate,
          bnmc = draft.bnmc.trim(),
          designation = draft.designation.trim(),
          workplace = draft.workplace.trim(),
          division = draft.division.trim(),
          district = draft.district.trim(),
          upazila = draft.upazila.trim(),
          address = draft.computedAddress,
          education = draft.computedEducation,
          eduCat = draft.computedEduCat,
          eduSub = draft.computedEduSub,
          eduInfo = draft.computedEduInfo,
          email = draft.email.trim(),
          photo = draft.photo,
          pendingEdit = null,
        )
      } else {
        target.copy(
          photo = draft.photo,
          pendingEdit =
            PendingEdit(
              category = draft.category.trim(),
              name = draft.name.trim(),
              father = draft.father.trim(),
              mother = draft.mother.trim(),
              dob = formattedDob,
              prl = prl,
              joinDate = formattedJoinDate,
              phone = target.phone,
              bnmc = draft.bnmc.trim(),
              designation = draft.designation.trim(),
              workplace = draft.workplace.trim(),
              division = draft.division.trim(),
              district = draft.district.trim(),
              upazila = draft.upazila.trim(),
              address = draft.computedAddress,
              education = draft.computedEducation,
              eduCat = draft.computedEduCat,
              eduSub = draft.computedEduSub,
              eduInfo = draft.computedEduInfo,
              email = draft.email.trim(),
              requestedAt = nowIso(),
            ),
        )
      }

    val noteMsg =
      if (me.isManagerRole) "তথ্য আপডেট হয়েছে ✅"
      else "আপনার পরিবর্তনের অনুরোধ Admin এর কাছে পাঠানো হয়েছে। Approve হলে তথ্য আপডেট হবে।"

    mutateRecord(updatedRecord, clearEditing = true, bannerNote = noteMsg)
  }

  fun changePassword(newPassword: String, confirmPassword: String) {
    val me = _uiState.value.currentUser ?: return
    if (
      newPassword.length < 4 ||
        newPassword != confirmPassword ||
        newPassword == "1234" ||
        newPassword == "admin123"
    ) {
      _uiState.update {
        it.copy(
          errorMessage = "Password কমপক্ষে ৪ অক্ষর, মিলতে হবে এবং default password হওয়া যাবে না"
        )
      }
      return
    }
    val updated = me.copy(pw = DirectoryConstants.sha256Password(newPassword), mustChange = false)
    viewModelScope.launch {
      _uiState.update { it.copy(isBusy = true, errorMessage = "") }
      repository
        .upsertNurse(updated)
        .onSuccess { list ->
          val cur = list.find { it.phone == me.phone } ?: updated
          _uiState.update {
            it.copy(
              isBusy = false,
              records = list,
              currentUser = cur,
              showPasswordPromptDialog = false,
              activeTab = if (cur.isManagerRole) MainTab.REQUESTS else MainTab.DIRECTORY,
              bannerNote = "Password পরিবর্তন হয়েছে ✅",
            )
          }
        }
        .onFailure { err ->
          _uiState.update {
            it.copy(isBusy = false, errorMessage = err.localizedMessage ?: "Password update failed")
          }
        }
    }
  }

  fun updateProfilePhoto(base64Photo: String?) {
    val me = _uiState.value.currentUser ?: return
    val note =
      if (base64Photo != null) "Profile picture সেভ হয়েছে ✅" else "Picture মুছে ফেলা হয়েছে"
    mutateRecord(me.copy(photo = base64Photo), bannerNote = note)
  }

  fun setPhotoReadError() {
    _uiState.update { it.copy(bannerNote = "ছবি পড়া যায়নি") }
  }

  fun approveRegistration(phone: String, status: String) {
    val target = _uiState.value.records.find { it.phone == phone } ?: return
    val msg = if (status == "approved") "আবেদন Approve হয়েছে ✅" else "আবেদন Reject করা হয়েছে"
    mutateRecord(target.copy(status = status), bannerNote = msg)
  }

  fun resolvePendingEdit(phone: String, approve: Boolean) {
    val target = _uiState.value.records.find { it.phone == phone } ?: return
    val updated = if (approve) target.appliedPendingEdit() else target.copy(pendingEdit = null)
    val msg = if (approve) "তথ্য পরিবর্তন Approve হয়েছে ✅" else "পরিবর্তন Reject করা হয়েছে"
    mutateRecord(updated, bannerNote = msg)
  }

  fun resolveResetRequest(phone: String, approve: Boolean) {
    val target = _uiState.value.records.find { it.phone == phone } ?: return
    val updated =
      if (approve) {
        target.copy(
          pw = DirectoryConstants.sha256Password("1234"),
          mustChange = true,
          resetReq = null,
        )
      } else {
        target.copy(resetReq = null)
      }
    val msg =
      if (approve) "Password reset হয়েছে ✅ ইউজার পরবর্তী login এ নতুন password দেবে"
      else "Password reset অনুরোধ Reject হয়েছে"
    mutateRecord(updated, bannerNote = msg)
  }

  fun adminResetPassword(phone: String) {
    val target = _uiState.value.records.find { it.phone == phone } ?: return
    mutateRecord(
      target.copy(pw = DirectoryConstants.sha256Password("1234"), mustChange = true),
      bannerNote = "Password reset হয়েছে ✅",
    )
  }

  fun adminDeleteUser(phone: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isBusy = true, errorMessage = "") }
      repository
        .deleteNurse(phone)
        .onSuccess { list ->
          _uiState.update {
            it.copy(isBusy = false, records = list, bannerNote = "অ্যাকাউন্ট Delete করা হয়েছে")
          }
        }
        .onFailure { err ->
          _uiState.update {
            it.copy(isBusy = false, errorMessage = err.localizedMessage ?: "Delete failed")
          }
        }
    }
  }

  fun saveManagerAccount(
    userId: String,
    name: String,
    password: String,
    role: String,
    scopeDivision: String,
    scopeDistrict: String,
    scopeUpazila: String,
    scopeWorkplace: String,
    isUpdate: Boolean = false,
  ) {
    val id = userId.trim()
    val cleanName = name.trim()
    if (
      id.isBlank() ||
        cleanName.isBlank() ||
        id == "admin" ||
        id == "__news_ticker__" ||
        id == "__emailjs_config__"
    ) {
      _uiState.update { it.copy(errorMessage = "সঠিক ID এবং Name দিন") }
      return
    }
    val existing = _uiState.value.records.find { it.phone == id }
    if (!isUpdate && existing != null) {
      _uiState.update { it.copy(errorMessage = "এই ID আগেই ব্যবহৃত হয়েছে") }
      return
    }
    if (isUpdate && existing == null) {
      _uiState.update { it.copy(errorMessage = "এই Admin / Editor পাওয়া যায়নি") }
      return
    }

    val trimmedPw = password.trim()
    if (trimmedPw.isNotEmpty() && trimmedPw.length < 4) {
      _uiState.update { it.copy(errorMessage = "Password কমপক্ষে ৪ অক্ষরের হতে হবে") }
      return
    }

    val finalPwHash: String
    val finalMustChange: Boolean
    if (isUpdate && existing != null) {
      if (trimmedPw.isEmpty()) {
        finalPwHash = existing.pw
        finalMustChange = existing.mustChange
      } else {
        finalPwHash = DirectoryConstants.sha256Password(trimmedPw)
        finalMustChange = trimmedPw == "1234"
      }
    } else {
      val rawPw = trimmedPw.ifBlank { "1234" }
      finalPwHash = DirectoryConstants.sha256Password(rawPw)
      finalMustChange = rawPw == "1234"
    }

    val record =
      NurseRecord(
        phone = id,
        role = if (role == "subadmin") "subadmin" else "editor",
        status = "approved",
        pw = finalPwHash,
        mustChange = finalMustChange,
        name = cleanName,
        scopeDivision = scopeDivision.trim(),
        scopeDistrict = scopeDistrict.trim(),
        scopeUpazila = scopeUpazila.trim(),
        scopeWorkplace = scopeWorkplace.trim(),
        createdAt = existing?.createdAt ?: nowIso(),
      )
    val actionWord = if (isUpdate) "আপডেট" else "যোগ"
    mutateRecord(
      record,
      bannerNote =
        "${if (role == "subadmin") "Admin" else "Editor"} $actionWord করা হয়েছে ✅ (ID: $id)",
    )
  }

  fun saveEmailJsConfig(serviceId: String, templateId: String, publicKey: String) {
    val sId = serviceId.trim()
    val tId = templateId.trim()
    val pKey = publicKey.trim()
    if (sId.isBlank() || tId.isBlank() || pKey.isBlank()) {
      _uiState.update {
        it.copy(errorMessage = "Service ID, Template ID এবং Public Key তিনটিই দিন")
      }
      return
    }
    val json =
      JSONObject()
        .put("service_id", sId)
        .put("template_id", tId)
        .put("public_key", pKey)
        .toString()
    val configRecord =
      NurseRecord(
        phone = "__emailjs_config__",
        role = "config",
        status = "approved",
        pw = "",
        mustChange = false,
        name = "EmailJS Config",
        address = json,
      )
    mutateRecord(
      configRecord,
      bannerNote = "Email Recovery (EmailJS) সেটিংস সব ডিভাইসের জন্য সেভ হয়েছে ✅",
    )
  }

  fun saveNewsTicker(text: String) {
    val cleaned = text.trim().ifBlank { DirectoryConstants.DEFAULT_NEWS_TICKER }
    val tickerRecord =
      NurseRecord(
        phone = "__news_ticker__",
        role = "config",
        status = "approved",
        pw = "",
        mustChange = false,
        name = cleaned,
      )
    mutateRecord(tickerRecord, bannerNote = "News Ticker আপডেট হয়েছে ✅")
  }

  fun updateFilter(transform: (DirectoryFilter) -> DirectoryFilter) {
    _uiState.update { it.copy(filter = transform(it.filter), printError = "") }
  }

  fun clearFilter() {
    _uiState.update { it.copy(filter = DirectoryFilter(), printError = "") }
  }

  fun togglePrintColumn(key: String, checked: Boolean) {
    _uiState.update { state ->
      val next =
        if (checked) state.selectedPrintColumns + key else state.selectedPrintColumns - key
      state.copy(selectedPrintColumns = next, printError = "")
    }
  }

  fun setAllPrintColumns(selectAll: Boolean) {
    _uiState.update {
      it.copy(
        selectedPrintColumns =
          if (selectAll) DirectoryConstants.PRINT_FIELDS.map { f -> f.first }.toSet()
          else emptySet(),
        printError = "",
      )
    }
  }

  fun setPrintError(msg: String) {
    _uiState.update { it.copy(printError = msg) }
  }

  private fun draftToRecord(draft: NurseFormDraft, status: String): NurseRecord =
    NurseRecord(
      phone = draft.phone.trim(),
      role = "user",
      status = status,
      pw = DirectoryConstants.sha256Password("1234"),
      mustChange = true,
      category = draft.category.trim(),
      name = draft.name.trim(),
      father = draft.father.trim(),
      mother = draft.mother.trim(),
      dob = DirectoryConstants.formatDateDdMmYyyy(draft.dob),
      prl = DirectoryConstants.calculatePrlDate(draft.dob),
      joinDate = DirectoryConstants.formatDateDdMmYyyy(draft.joinDate),
      bnmc = draft.bnmc.trim(),
      designation = draft.designation.trim(),
      workplace = draft.workplace.trim(),
      division = draft.division.trim(),
      district = draft.district.trim(),
      upazila = draft.upazila.trim(),
      address = draft.computedAddress,
      education = draft.computedEducation,
      eduCat = draft.computedEduCat,
      eduSub = draft.computedEduSub,
      eduInfo = draft.computedEduInfo,
      email = draft.email.trim(),
      photo = draft.photo,
      createdAt = nowIso(),
    )

  private fun maskEmail(email: String): String {
    val parts = email.split("@")
    if (parts.size != 2) return email
    val local = parts[0]
    val maskedLocal =
      if (local.length <= 2) "${local.firstOrNull() ?: '*'}*"
      else "${local.take(2)}***${local.last()}"
    return "$maskedLocal@${parts[1]}"
  }

  private fun mutateRecord(
    updated: NurseRecord,
    clearEditing: Boolean = false,
    bannerNote: String = "",
  ) {
    viewModelScope.launch {
      _uiState.update { it.copy(isBusy = true, errorMessage = "") }
      repository
        .upsertNurse(updated)
        .onSuccess { list ->
          _uiState.update { state ->
            val current = state.currentUser?.let { cur -> list.find { it.phone == cur.phone } }
            state.copy(
              isBusy = false,
              records = list,
              currentUser = current,
              editingPhone = if (clearEditing) null else state.editingPhone,
              bannerNote = bannerNote,
              errorMessage = "",
            )
          }
        }
        .onFailure { err ->
          _uiState.update {
            it.copy(isBusy = false, errorMessage = err.localizedMessage ?: "Operation failed")
          }
        }
    }
  }
}
