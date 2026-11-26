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

data class EducationEntry(
  val cat: String = "",
  val sub: String = "",
  val customSub: String = "",
  val institute: String = "",
  val passingYear: String = "",
  val major: String = "",
) {
  fun resolvedSub(): String =
    when (cat.trim()) {
      "Nursing" -> if (sub.trim() == "Other") customSub.trim() else sub.trim()
      "Other" -> customSub.trim()
      else -> ""
    }

  fun title(): String {
    val c = cat.trim()
    return when (c) {
      "Nursing" -> {
        val rs = resolvedSub()
        if (rs.isNotBlank()) "Nursing — $rs" else ""
      }
      "Other" -> resolvedSub()
      else -> c
    }
  }

  fun detailsString(): String {
    val parts = mutableListOf<String>()
    val cleanInst = institute.trim().replace(" | ", ", ")
    val cleanMajor = major.trim().replace(" | ", ", ")
    val cleanYear = passingYear.trim().replace(" | ", ", ")
    if (cleanInst.isNotBlank()) parts.add(cleanInst)
    if (cleanMajor.isNotBlank()) parts.add("Major: $cleanMajor")
    if (cleanYear.isNotBlank()) parts.add("Year: $cleanYear")
    return parts.joinToString(", ")
  }

  fun formattedWithInfo(): String {
    val t = title()
    if (t.isBlank()) return ""
    val cleanInfo = detailsString()
    return if (cleanInfo.isNotBlank()) "$t ($cleanInfo)" else t
  }
}

private data class ParsedEduDetails(
  val institute: String = "",
  val passingYear: String = "",
  val major: String = "",
)

private fun parseEduDetails(rawInfo: String): ParsedEduDetails {
  val trimmed = rawInfo.trim()
  if (trimmed.isBlank()) return ParsedEduDetails()
  val instParts = mutableListOf<String>()
  var majorVal = ""
  var yearVal = ""
  trimmed.split(",").forEach { token ->
    val part = token.trim()
    when {
      part.startsWith("Major:", ignoreCase = true) ->
        majorVal = part.substringAfter(":").trim()
      part.startsWith("Year:", ignoreCase = true) ->
        yearVal = part.substringAfter(":").trim()
      part.isNotEmpty() -> instParts.add(part)
    }
  }
  return ParsedEduDetails(
    institute = instParts.joinToString(", "),
    passingYear = yearVal,
    major = majorVal,
  )
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
  val homeDistrict: String = "",
  val homeAddress: String = "",
  val educationEntries: List<EducationEntry> = listOf(EducationEntry()),
  val email: String = "",
  val photo: String? = null,
) {
  private val effectiveEntries: List<EducationEntry>
    get() = educationEntries.ifEmpty { listOf(EducationEntry()) }

  val computedAddress: String
    get() = DirectoryConstants.buildEncodedAddress(address, homeDistrict, homeAddress)

  val computedEduCat: String
    get() = effectiveEntries.first().cat.trim()

  val computedEduSub: String
    get() = effectiveEntries.first().resolvedSub()

  val computedEduInfo: String
    get() = if (effectiveEntries.size == 1) effectiveEntries.first().detailsString() else ""

  val computedEducation: String
    get() {
      val list = effectiveEntries
      if (list.size == 1) return list.first().title()
      return list.mapNotNull { it.formattedWithInfo().takeIf { s -> s.isNotBlank() } }
        .joinToString(" | ")
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
        "education" to effectiveEntries.first().title(),
      )
    for ((key, value) in required) {
      if (value.isBlank()) return "সব বাধ্যতামূলক তথ্য দিন ($key)"
    }
    if (DirectoryConstants.parseAnyDate(dob) == null) {
      return "Date of Birth সঠিক dd/mm/yyyy ফরম্যাটে দিন"
    }
    if (DirectoryConstants.parseAnyDate(joinDate) == null) {
      return "Joining Date সঠিক dd/mm/yyyy ফরম্যাটে দিন"
    }
    if (effectiveEntries.drop(1).any { it.title().isBlank() }) {
      return "যোগ করা প্রতিটি শিক্ষাগত যোগ্যতার তথ্য পূরণ করুন অথবা অতিরিক্ত ঘরটি মুছে দিন"
    }
    if (!DirectoryConstants.isValidBdPhone(phone)) {
      return "সঠিক ১১ ডিজিটের মোবাইল নম্বর দিন"
    }
    return null
  }
}

private fun parseEducationSegment(segment: String, fallbackInfo: String = ""): EducationEntry {
  val trimmed = segment.trim()
  if (trimmed.isBlank()) {
    val d = parseEduDetails(fallbackInfo)
    return EducationEntry(institute = d.institute, passingYear = d.passingYear, major = d.major)
  }

  var titlePart = trimmed
  var infoPart = fallbackInfo

  if (fallbackInfo.isBlank() && trimmed.endsWith(")")) {
    var depth = 0
    var openIdx = -1
    for (i in trimmed.length - 1 downTo 0) {
      when (trimmed[i]) {
        ')' -> depth++
        '(' -> {
          depth--
          if (depth == 0) {
            openIdx = i
            break
          }
        }
      }
    }
    if (openIdx > 0 && trimmed[openIdx - 1] == ' ') {
      titlePart = trimmed.substring(0, openIdx - 1).trim()
      infoPart = trimmed.substring(openIdx + 1, trimmed.length - 1).trim()
    }
  }

  val d = parseEduDetails(infoPart)
  return when {
    titlePart.startsWith("Nursing — ") -> {
      val subRaw = titlePart.removePrefix("Nursing — ").trim()
      if (subRaw in DirectoryConstants.NURSING_SUB_CATEGORIES && subRaw != "Other") {
        EducationEntry(
          cat = "Nursing",
          sub = subRaw,
          institute = d.institute,
          passingYear = d.passingYear,
          major = d.major,
        )
      } else {
        EducationEntry(
          cat = "Nursing",
          sub = "Other",
          customSub = subRaw,
          institute = d.institute,
          passingYear = d.passingYear,
          major = d.major,
        )
      }
    }
    titlePart == "HSC" || titlePart == "SSC" ->
      EducationEntry(
        cat = titlePart,
        institute = d.institute,
        passingYear = d.passingYear,
        major = d.major,
      )
    else ->
      EducationEntry(
        cat = "Other",
        customSub = titlePart,
        institute = d.institute,
        passingYear = d.passingYear,
        major = d.major,
      )
  }
}

fun NurseRecord.toFormDraft(): NurseFormDraft {
  val parsedEntries =
    if (education.contains(" | ")) {
      education.split(" | ").map { parseEducationSegment(it) }.ifEmpty { listOf(EducationEntry()) }
    } else if (education.isNotBlank() || eduCat.isNotBlank() || eduSub.isNotBlank() || eduInfo.isNotBlank()) {
      val d = parseEduDetails(eduInfo)
      val single =
        when (eduCat.trim()) {
          "Nursing" -> {
            val subRaw = eduSub.trim().ifBlank { education.removePrefix("Nursing — ").trim() }
            if (subRaw.isBlank()) {
              EducationEntry(cat = "Nursing", institute = d.institute, passingYear = d.passingYear, major = d.major)
            } else if (subRaw in DirectoryConstants.NURSING_SUB_CATEGORIES && subRaw != "Other") {
              EducationEntry(cat = "Nursing", sub = subRaw, institute = d.institute, passingYear = d.passingYear, major = d.major)
            } else {
              EducationEntry(cat = "Nursing", sub = "Other", customSub = subRaw, institute = d.institute, passingYear = d.passingYear, major = d.major)
            }
          }
          "HSC", "SSC" ->
            EducationEntry(cat = eduCat.trim(), institute = d.institute, passingYear = d.passingYear, major = d.major)
          "Other" ->
            EducationEntry(cat = "Other", customSub = eduSub.trim().ifBlank { education }, institute = d.institute, passingYear = d.passingYear, major = d.major)
          else -> parseEducationSegment(education, fallbackInfo = eduInfo)
        }
      listOf(single)
    } else {
      listOf(EducationEntry())
    }

  val addrParts = DirectoryConstants.parseAddressParts(address)

  return NurseFormDraft(
    category = category,
    name = name,
    father = father,
    mother = mother,
    dob = DirectoryConstants.formatDateDdMmYyyy(dob),
    joinDate = DirectoryConstants.formatDateDdMmYyyy(joinDate),
    phone = phone,
    bnmc = bnmc,
    designation = designation,
    workplace = workplace,
    division = division,
    district = district,
    upazila = upazila,
    address = addrParts.presentAddress,
    homeDistrict = addrParts.homeDistrict,
    homeAddress = addrParts.homeAddress,
    educationEntries = parsedEntries,
    email = email,
    photo = photo,
  )
}

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

