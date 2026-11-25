package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PendingEdit(
  val category: String = "",
  val name: String = "",
  val father: String = "",
  val mother: String = "",
  val dob: String = "",
  val prl: String = "",
  val joinDate: String = "",
  val phone: String = "",
  val bnmc: String = "",
  val designation: String = "",
  val workplace: String = "",
  val division: String = "",
  val district: String = "",
  val upazila: String = "",
  val address: String = "",
  val education: String = "",
  val eduCat: String = "",
  val eduSub: String = "",
  val eduInfo: String = "",
  val email: String = "",
  val requestedAt: String = "",
)

@JsonClass(generateAdapter = true)
data class ResetRequest(val at: String = "")

@JsonClass(generateAdapter = true)
data class NurseRecord(
  val phone: String = "",
  val role: String = "user", // "user", "admin", "subadmin", "editor", "config"
  val status: String = "pending",
  val pw: String = "",
  @Json(name = "must_change") val mustChange: Boolean = true,
  val category: String = "",
  val name: String = "",
  val father: String = "",
  val mother: String = "",
  val dob: String = "",
  val prl: String = "",
  @Json(name = "join_date") val joinDate: String = "",
  val bnmc: String = "",
  val designation: String = "",
  val workplace: String = "",
  val division: String = "",
  val district: String = "",
  val upazila: String = "",
  val address: String = "",
  val education: String = "",
  @Json(name = "edu_cat") val eduCat: String = "",
  @Json(name = "edu_sub") val eduSub: String = "",
  @Json(name = "edu_info") val eduInfo: String = "",
  val email: String = "",
  val photo: String? = null,
  @Json(name = "pending_edit") val pendingEdit: PendingEdit? = null,
  @Json(name = "reset_req") val resetReq: ResetRequest? = null,
  @Json(name = "scope_division") val scopeDivision: String = "",
  @Json(name = "scope_district") val scopeDistrict: String = "",
  @Json(name = "scope_upazila") val scopeUpazila: String = "",
  @Json(name = "scope_workplace") val scopeWorkplace: String = "",
  @Json(name = "recovery_code") val recoveryCode: String = "",
  @Json(name = "recovery_exp") val recoveryExp: String = "",
  @Json(name = "created_at") val createdAt: String? = null,
) {
  val isGlobalAdmin: Boolean
    get() = role == "admin"

  val isManagerRole: Boolean
    get() = role == "admin" || role == "subadmin" || role == "editor"

  fun scopeLabel(): String {
    if (role == "admin") return "All Bangladesh (Super Admin)"
    val parts =
      listOf(
          scopeDivision,
          scopeDistrict,
          scopeUpazila,
          scopeWorkplace,
        )
        .filter { it.isNotBlank() }
    return if (parts.isEmpty()) "All Bangladesh" else parts.joinToString(" › ")
  }

  fun canManageNurse(nurse: NurseRecord): Boolean {
    if (role == "admin") return true
    if (role != "subadmin" && role != "editor") return false
    if (scopeDivision.isNotBlank() && !nurse.division.equals(scopeDivision, ignoreCase = true)) {
      return false
    }
    if (scopeDistrict.isNotBlank() && !nurse.district.equals(scopeDistrict, ignoreCase = true)) {
      return false
    }
    if (scopeUpazila.isNotBlank() && !nurse.upazila.equals(scopeUpazila, ignoreCase = true)) {
      return false
    }
    if (
      scopeWorkplace.isNotBlank() &&
        !nurse.workplace.contains(scopeWorkplace, ignoreCase = true)
    ) {
      return false
    }
    return true
  }

  fun appliedPendingEdit(): NurseRecord {
    val pe = pendingEdit ?: return this
    return copy(
      category = pe.category,
      name = pe.name,
      father = pe.father,
      mother = pe.mother,
      dob = pe.dob,
      prl = pe.prl.ifBlank { DirectoryConstants.calculatePrlDate(pe.dob) },
      joinDate = pe.joinDate,
      bnmc = pe.bnmc,
      designation = pe.designation,
      workplace = pe.workplace,
      division = pe.division,
      district = pe.district,
      upazila = pe.upazila,
      address = pe.address,
      education = pe.education,
      eduCat = pe.eduCat,
      eduSub = pe.eduSub,
      eduInfo = pe.eduInfo,
      email = pe.email,
      pendingEdit = null,
    )
  }
}

data class NurseFormDraft(
  val category: String = "",
  val name: String = "",
  val father: String = "",
  val mother: String = "",
  val dob: String = "",
  val joinDate: String = "",
  val phone: String = "",
  val bnmc: String = "",
  val designation: String = "",
  val workplace: String = "",
  val division: String = "",
  val district: String = "",
  val upazila: String = "",
  val address: String = "",
  val eduCat: String = "",
  val eduSub: String = "",
  val eduInfo: String = "",
  val email: String = "",
  val photo: String? = null,
) {
  val computedEducation: String
    get() =
      when (eduCat.trim()) {
        "Nursing" -> if (eduSub.isNotBlank()) "Nursing — ${eduSub.trim()}" else ""
        else -> eduCat.trim()
      }

  fun validate(): String? {
    val required =
      listOf(
        "category" to category,
        "name" to name,
        "father" to father,
        "mother" to mother,
        "dob" to dob,
        "joinDate" to joinDate,
        "phone" to phone,
        "bnmc" to bnmc,
        "designation" to designation,
        "workplace" to workplace,
        "division" to division,
        "district" to district,
        "upazila" to upazila,
        "education" to computedEducation,
      )
    for ((key, value) in required) {
      if (value.isBlank()) return "সব বাধ্যতামূলক তথ্য দিন ($key)"
    }
    if (!DirectoryConstants.isValidBdPhone(phone)) {
      return "সঠিক ১১ ডিজিটের মোবাইল নম্বর দিন"
    }
    return null
  }
}

fun NurseRecord.toFormDraft(): NurseFormDraft =
  NurseFormDraft(
    category = category,
    name = name,
    father = father,
    mother = mother,
    dob = dob,
    joinDate = joinDate,
    phone = phone,
    bnmc = bnmc,
    designation = designation,
    workplace = workplace,
    division = division,
    district = district,
    upazila = upazila,
    address = address,
    eduCat = eduCat,
    eduSub = eduSub,
    eduInfo = eduInfo,
    email = email,
    photo = photo,
  )

data class EmailJsConfig(
  val serviceId: String = "",
  val templateId: String = "",
  val publicKey: String = "",
) {
  val isValid: Boolean
    get() =
      serviceId.isNotBlank() &&
        !serviceId.contains("YOUR_EMAILJS", ignoreCase = true) &&
        templateId.isNotBlank() &&
        !templateId.contains("YOUR_EMAILJS", ignoreCase = true) &&
        publicKey.isNotBlank() &&
        !publicKey.contains("YOUR_EMAILJS", ignoreCase = true)
}

fun List<NurseRecord>.extractEmailJsConfig(): EmailJsConfig? {
  val row = find { it.phone == "__emailjs_config__" && it.role == "config" } ?: return null
  return try {
    val obj = org.json.JSONObject(row.address)
    EmailJsConfig(
      serviceId = obj.optString("service_id", "").trim(),
      templateId = obj.optString("template_id", "").trim(),
      publicKey = obj.optString("public_key", "").trim(),
    ).takeIf { it.isValid }
  } catch (_: Exception) {
    null
  }
}

