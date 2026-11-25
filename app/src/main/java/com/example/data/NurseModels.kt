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
  val role: String = "user",
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
  @Json(name = "created_at") val createdAt: String? = null,
) {
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
  )
