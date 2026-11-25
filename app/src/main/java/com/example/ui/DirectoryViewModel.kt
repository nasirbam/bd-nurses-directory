package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DirectoryConstants
import com.example.data.NurseFormDraft
import com.example.data.NurseRecord
import com.example.data.NurseRepository
import com.example.data.PendingEdit
import com.example.data.ResetRequest
import com.example.data.toFormDraft
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
  val isLoading: Boolean = true,
  val isBusy: Boolean = false,
  val records: List<NurseRecord> = emptyList(),
  val currentUser: NurseRecord? = null,
  val authView: AuthView = AuthView.LOGIN,
  val activeTab: MainTab = MainTab.DIRECTORY,
  val editingPhone: String? = null,
  val formDraft: NurseFormDraft = NurseFormDraft(),
  val skipFirstPasswordChange: Boolean = false,
  val filter: DirectoryFilter = DirectoryFilter(),
  val selectedPrintColumns: Set<String> = DirectoryConstants.PRINT_FIELDS.map { it.first }.toSet(),
  val errorMessage: String = "",
  val successMessage: String = "",
  val bannerNote: String = "",
  val printError: String = "",
) {
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

  val pendingCount: Int
    get() =
      records.count {
        it.role == "user" &&
          (it.status == "pending" || (it.status == "approved" && it.pendingEdit != null) || it.resetReq != null)
      }

  val mustForcePasswordScreen: Boolean
    get() = currentUser?.mustChange == true && !skipFirstPasswordChange
}

class DirectoryViewModel(private val repository: NurseRepository = NurseRepository()) :
  ViewModel() {

  private val _uiState =
    MutableStateFlow(DirectoryUiState(isSupabaseConfigured = repository.isConfigured))
  val uiState: StateFlow<DirectoryUiState> = _uiState.asStateFlow()

  init {
    refreshData()
  }

  private fun nowIso(): String =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

  fun refreshData(silent: Boolean = false) {
    if (!repository.isConfigured) {
      _uiState.update {
        it.copy(
          isSupabaseConfigured = false,
          isLoading = false,
          errorMessage =
            "Supabase সেট করা নেই। AI Studio Secrets প্যানেলে SUPABASE_URL এবং SUPABASE_ANON_KEY যোগ করুন।",
        )
      }
      return
    }
    viewModelScope.launch {
      if (!silent) {
        _uiState.update { it.copy(isLoading = true, errorMessage = "") }
      }
      repository
        .fetchAllAndEnsureAdmin()
        .onSuccess { list ->
          _uiState.update { state ->
            val updatedCurrent =
              state.currentUser?.let { cur -> list.find { it.phone == cur.phone } }
            state.copy(
              isSupabaseConfigured = true,
              isLoading = false,
              records = list,
              currentUser = updatedCurrent,
            )
          }
        }
        .onFailure { err ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = err.localizedMessage ?: "সার্ভার থেকে তথ্য লোড করা যায়নি",
            )
          }
        }
    }
  }

  fun setAuthView(view: AuthView) {
    _uiState.update {
      it.copy(
        authView = view,
        errorMessage = "",
        successMessage = "",
        formDraft = if (view == AuthView.REGISTER) NurseFormDraft() else it.formDraft,
      )
    }
  }

  fun selectTab(tab: MainTab) {
    _uiState.update {
      it.copy(activeTab = tab, editingPhone = null, errorMessage = "", printError = "")
    }
  }

  fun clearBannerNote() {
    _uiState.update { it.copy(bannerNote = "") }
  }

  fun loginUser(phone: String, password: String) {
    val p = phone.trim()
    val hash = DirectoryConstants.sha256Password(password)
    viewModelScope.launch {
      _uiState.update { it.copy(isBusy = true, errorMessage = "") }
      val latestRecords =
        if (_uiState.value.records.isEmpty()) {
          repository.fetchAllAndEnsureAdmin().getOrElse { err ->
            _uiState.update {
              it.copy(isBusy = false, errorMessage = err.localizedMessage ?: "Connection error")
            }
            return@launch
          }
        } else {
          repository.fetchAllAndEnsureAdmin().getOrDefault(_uiState.value.records)
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
        _uiState.update {
          it.copy(
            isBusy = false,
            records = latestRecords,
            currentUser = user,
            activeTab = MainTab.DIRECTORY,
            skipFirstPasswordChange = false,
            errorMessage = "",
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
      val admin = latestRecords.find { it.role == "admin" && it.phone == id }
      if (admin == null || admin.pw != hash) {
        _uiState.update {
          it.copy(
            isBusy = false,
            records = latestRecords,
            errorMessage = "Admin ID বা Password ভুল",
          )
        }
      } else {
        _uiState.update {
          it.copy(
            isBusy = false,
            records = latestRecords,
            currentUser = admin,
            activeTab = MainTab.REQUESTS,
            skipFirstPasswordChange = false,
            errorMessage = "",
            bannerNote = "",
          )
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
        skipFirstPasswordChange = false,
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
      it.copy(editingPhone = phone, formDraft = target.toFormDraft(), errorMessage = "")
    }
  }

  fun cancelEditing() {
    _uiState.update { it.copy(editingPhone = null, errorMessage = "") }
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
      val newRecord =
        NurseRecord(
          phone = cleanPhone,
          role = "user",
          status = "pending",
          pw = DirectoryConstants.sha256Password("1234"),
          mustChange = true,
          category = draft.category.trim(),
          name = draft.name.trim(),
          father = draft.father.trim(),
          mother = draft.mother.trim(),
          dob = draft.dob.trim(),
          prl = DirectoryConstants.calculatePrlDate(draft.dob.trim()),
          joinDate = draft.joinDate.trim(),
          bnmc = draft.bnmc.trim(),
          designation = draft.designation.trim(),
          workplace = draft.workplace.trim(),
          division = draft.division.trim(),
          district = draft.district.trim(),
          upazila = draft.upazila.trim(),
          address = draft.address.trim(),
          education = draft.computedEducation,
          eduCat = draft.eduCat.trim(),
          eduSub = if (draft.eduCat.trim() == "Nursing") draft.eduSub.trim() else "",
          eduInfo = draft.eduInfo.trim(),
          email = draft.email.trim(),
          createdAt = nowIso(),
        )
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
    val prl = DirectoryConstants.calculatePrlDate(draft.dob.trim())
    val updatedRecord =
      if (me.role == "admin") {
        target.copy(
          category = draft.category.trim(),
          name = draft.name.trim(),
          father = draft.father.trim(),
          mother = draft.mother.trim(),
          dob = draft.dob.trim(),
          prl = prl,
          joinDate = draft.joinDate.trim(),
          bnmc = draft.bnmc.trim(),
          designation = draft.designation.trim(),
          workplace = draft.workplace.trim(),
          division = draft.division.trim(),
          district = draft.district.trim(),
          upazila = draft.upazila.trim(),
          address = draft.address.trim(),
          education = draft.computedEducation,
          eduCat = draft.eduCat.trim(),
          eduSub = if (draft.eduCat.trim() == "Nursing") draft.eduSub.trim() else "",
          eduInfo = draft.eduInfo.trim(),
          email = draft.email.trim(),
          pendingEdit = null,
        )
      } else {
        target.copy(
          pendingEdit =
            PendingEdit(
              category = draft.category.trim(),
              name = draft.name.trim(),
              father = draft.father.trim(),
              mother = draft.mother.trim(),
              dob = draft.dob.trim(),
              prl = prl,
              joinDate = draft.joinDate.trim(),
              phone = target.phone,
              bnmc = draft.bnmc.trim(),
              designation = draft.designation.trim(),
              workplace = draft.workplace.trim(),
              division = draft.division.trim(),
              district = draft.district.trim(),
              upazila = draft.upazila.trim(),
              address = draft.address.trim(),
              education = draft.computedEducation,
              eduCat = draft.eduCat.trim(),
              eduSub = if (draft.eduCat.trim() == "Nursing") draft.eduSub.trim() else "",
              eduInfo = draft.eduInfo.trim(),
              email = draft.email.trim(),
              requestedAt = nowIso(),
            )
        )
      }

    val noteMsg =
      if (me.role == "admin") "তথ্য আপডেট হয়েছে ✅"
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
              skipFirstPasswordChange = false,
              activeTab = if (cur.role == "admin") MainTab.REQUESTS else MainTab.DIRECTORY,
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

  fun skipPasswordForNow() {
    _uiState.update { it.copy(skipFirstPasswordChange = true, activeTab = MainTab.DIRECTORY) }
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
            it.copy(isBusy = false, records = list, bannerNote = "ইউজার Delete করা হয়েছে")
          }
        }
        .onFailure { err ->
          _uiState.update {
            it.copy(isBusy = false, errorMessage = err.localizedMessage ?: "Delete failed")
          }
        }
    }
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
