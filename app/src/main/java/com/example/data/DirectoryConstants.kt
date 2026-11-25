package com.example.data

import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DirectoryConstants {
  val DIVISIONS: Map<String, List<String>> =
    linkedMapOf(
      "Dhaka" to
        listOf(
          "Dhaka",
          "Gazipur",
          "Narayanganj",
          "Narsingdi",
          "Manikganj",
          "Munshiganj",
          "Tangail",
          "Kishoreganj",
          "Faridpur",
          "Gopalganj",
          "Madaripur",
          "Rajbari",
          "Shariatpur",
        ),
      "Chattogram" to
        listOf(
          "Chattogram",
          "Cox's Bazar",
          "Cumilla",
          "Feni",
          "Brahmanbaria",
          "Noakhali",
          "Lakshmipur",
          "Chandpur",
          "Rangamati",
          "Khagrachhari",
          "Bandarban",
        ),
      "Rajshahi" to
        listOf(
          "Rajshahi",
          "Bogura",
          "Pabna",
          "Sirajganj",
          "Natore",
          "Naogaon",
          "Chapainawabganj",
          "Joypurhat",
        ),
      "Khulna" to
        listOf(
          "Khulna",
          "Jashore",
          "Satkhira",
          "Bagerhat",
          "Narail",
          "Magura",
          "Jhenaidah",
          "Kushtia",
          "Chuadanga",
          "Meherpur",
        ),
      "Barishal" to
        listOf("Barishal", "Bhola", "Patuakhali", "Pirojpur", "Jhalokati", "Barguna"),
      "Sylhet" to listOf("Sylhet", "Moulvibazar", "Habiganj", "Sunamganj"),
      "Rangpur" to
        listOf(
          "Rangpur",
          "Dinajpur",
          "Kurigram",
          "Gaibandha",
          "Nilphamari",
          "Lalmonirhat",
          "Thakurgaon",
          "Panchagarh",
        ),
      "Mymensingh" to listOf("Mymensingh", "Jamalpur", "Sherpur", "Netrokona"),
    )

  val ALL_DISTRICTS: List<String> = DIVISIONS.values.flatten().sorted()

  val UPAZILAS: Map<String, List<String>> =
    mapOf(
      "Dhaka" to listOf("Savar", "Dhamrai", "Keraniganj", "Nawabganj", "Dohar", "Dhaka Metro (Thana)"),
      "Gazipur" to listOf("Gazipur Sadar", "Kaliakair", "Kapasia", "Sreepur", "Kaliganj"),
      "Chattogram" to
        listOf("Hathazari", "Raozan", "Sitakunda", "Patiya", "Chattogram Metro (Thana)"),
      "Rajshahi" to listOf("Paba", "Godagari", "Mohanpur", "Rajshahi Metro (Thana)"),
      "Khulna" to listOf("Dumuria", "Batiaghata", "Dacope", "Khulna Metro (Thana)"),
      "Sylhet" to listOf("Sylhet Sadar", "Beanibazar", "Jaintiapur", "Golapganj"),
      "Rangpur" to listOf("Rangpur Sadar", "Mithapukur", "Badarganj", "Pirganj"),
      "Barishal" to listOf("Barishal Sadar", "Bakerganj", "Babuganj", "Gournadi"),
      "Mymensingh" to listOf("Mymensingh Sadar", "Trishal", "Muktagachha", "Phulpur"),
    )

  val CATEGORIES: Map<String, List<String>> =
    linkedMapOf(
      "Govt. Nurse/Midwife" to
        listOf(
          "Senior Staff Nurse",
          "Staff Nurse",
          "Midwife",
          "Nursing Instructor",
          "Lecturer",
          "Principal",
        ),
      "Non-Govt. Nurse/Midwife" to
        listOf(
          "Senior Staff Nurse",
          "Staff Nurse",
          "Midwife",
          "Nursing Instructor",
          "Lecturer",
          "Principal",
        ),
      "Student Nurse/Midwife" to listOf("Student Nurse", "Student Midwife"),
    )

  val ALL_DESIGNATIONS: List<String> = CATEGORIES.values.flatten().distinct()

  val EDU_CATEGORIES: List<String> = listOf("Nursing", "HSC", "SSC")

  val NURSING_SUB_CATEGORIES: List<String> =
    listOf(
      "Diploma in Nursing Science & Midwifery",
      "Diploma in Midwifery",
      "B.Sc in Nursing",
      "Post Basic B.Sc in Nursing",
      "B.Sc in Public Health Nursing",
      "M.Sc in Nursing",
      "Other",
    )

  val PRINT_FIELDS: List<Pair<String, String>> =
    listOf(
      "name" to "Name",
      "father" to "Father's Name",
      "mother" to "Mother's Name",
      "dob" to "Date of Birth",
      "prl" to "PRL Date",
      "joinDate" to "Joining Date",
      "phone" to "Phone",
      "bnmc" to "BNMC Reg no.",
      "category" to "Category",
      "designation" to "Designation",
      "workplace" to "Hospital / Working Place",
      "division" to "Division",
      "district" to "District",
      "upazila" to "Upazila/Thana",
      "address" to "Address",
      "education" to "Education",
      "email" to "Email",
    )

  private val bdPhoneRegex = Regex("^01[3-9]\\d{8}$")

  fun isValidBdPhone(phone: String): Boolean = bdPhoneRegex.matches(phone.trim())

  fun sha256Password(raw: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest("ndbd:$raw".toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
  }

  fun calculatePrlDate(dobIso: String): String {
    if (dobIso.isBlank()) return ""
    return try {
      val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
      val date = parser.parse(dobIso.trim()) ?: return ""
      val cal = Calendar.getInstance().apply {
        time = date
        add(Calendar.YEAR, 59)
      }
      SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH).format(cal.time)
    } catch (_: Exception) {
      ""
    }
  }

  fun fieldValue(record: NurseRecord, key: String): String =
    when (key) {
      "name" -> record.name
      "father" -> record.father
      "mother" -> record.mother
      "dob" -> record.dob
      "prl" -> calculatePrlDate(record.dob).ifBlank { record.prl }
      "joinDate" -> record.joinDate
      "phone" -> record.phone
      "bnmc" -> record.bnmc
      "category" -> record.category
      "designation" -> record.designation
      "workplace" -> record.workplace
      "division" -> record.division
      "district" -> record.district
      "upazila" -> record.upazila
      "address" -> record.address
      "education" -> record.education
      "email" -> record.email
      else -> ""
    }
}
