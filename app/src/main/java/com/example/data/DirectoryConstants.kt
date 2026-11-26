package com.example.data

import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DirectoryConstants {
  const val DEFAULT_NEWS_TICKER =
    "🩺 BD Nurses Directory-তে স্বাগতম — নতুন নিবন্ধন ও তথ্য পরিবর্তনের জন্য অনুগ্রহ করে সঠিক তথ্য প্রদান করুন।"

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
      // Dhaka Division
      "Dhaka" to
        listOf(
          "Dhamrai",
          "Dohar",
          "Keraniganj",
          "Nawabganj",
          "Savar",
          "Dhaka Metro (Thana)",
          "Dhanmondi",
          "Gulshan",
          "Mirpur",
          "Mohammadpur",
          "Motijheel",
          "Shahbagh",
          "Tejgaon",
          "Uttara",
        ),
      "Gazipur" to
        listOf("Gazipur Sadar", "Kaliakair", "Kaliganj", "Kapasia", "Sreepur", "Tongi"),
      "Narayanganj" to
        listOf(
          "Araihazar",
          "Bandar",
          "Narayanganj Sadar",
          "Rupganj",
          "Sonargaon",
          "Fatullah",
          "Siddhirganj",
        ),
      "Narsingdi" to
        listOf("Belabo", "Monohardi", "Narsingdi Sadar", "Palash", "Raipura", "Shibpur"),
      "Manikganj" to
        listOf("Daulatpur", "Ghior", "Harirampur", "Manikganj Sadar", "Saturia", "Shivalaya", "Singair"),
      "Munshiganj" to
        listOf("Gazaria", "Lohajang", "Munshiganj Sadar", "Sirajdikhan", "Sreenagar", "Tongibari"),
      "Tangail" to
        listOf(
          "Basail",
          "Bhuapur",
          "Delduar",
          "Dhanbari",
          "Ghatail",
          "Gopalpur",
          "Kalihati",
          "Madhupur",
          "Mirzapur",
          "Nagarpur",
          "Sakhipur",
          "Tangail Sadar",
        ),
      "Kishoreganj" to
        listOf(
          "Austagram",
          "Bajitpur",
          "Bhairab",
          "Hossainpur",
          "Itna",
          "Karimganj",
          "Katiadi",
          "Kishoreganj Sadar",
          "Kuliarchar",
          "Mithamain",
          "Nikli",
          "Pakundia",
          "Tarail",
        ),
      "Faridpur" to
        listOf(
          "Alfadanga",
          "Bhanga",
          "Boalmari",
          "Charbhadrasan",
          "Faridpur Sadar",
          "Madhukhali",
          "Nagarkanda",
          "Sadarpur",
          "Saltha",
        ),
      "Gopalganj" to
        listOf("Gopalganj Sadar", "Kashiani", "Kotalipara", "Muksudpur", "Tungipara"),
      "Madaripur" to listOf("Dasar", "Kalkini", "Madaripur Sadar", "Rajoir", "Shibchar"),
      "Rajbari" to listOf("Baliakandi", "Goalandaghat", "Kalukhali", "Pangsha", "Rajbari Sadar"),
      "Shariatpur" to
        listOf("Bhedarganj", "Damudya", "Gosairhat", "Naria", "Shariatpur Sadar", "Zajira"),

      // Chattogram Division
      "Chattogram" to
        listOf(
          "Anwara",
          "Banshkhali",
          "Boalkhali",
          "Chandanaish",
          "Fatikchhari",
          "Hathazari",
          "Karnaphuli",
          "Lohagara",
          "Mirsharai",
          "Patiya",
          "Rangunia",
          "Raozan",
          "Sandwip",
          "Satkania",
          "Sitakunda",
          "Chattogram Metro (Thana)",
        ),
      "Cox's Bazar" to
        listOf(
          "Chakaria",
          "Cox's Bazar Sadar",
          "Eidgaon",
          "Kutubdia",
          "Maheshkhali",
          "Pekua",
          "Ramu",
          "Teknaf",
          "Ukhia",
        ),
      "Cumilla" to
        listOf(
          "Barura",
          "Brahmanpara",
          "Burichang",
          "Chandina",
          "Chauddagram",
          "Cumilla Adarsha Sadar",
          "Cumilla Sadar Dakshin",
          "Daudkandi",
          "Debidwar",
          "Homna",
          "Laksam",
          "Lalmai",
          "Meghna",
          "Monohorgonj",
          "Muradnagar",
          "Nangalkot",
          "Titas",
        ),
      "Feni" to
        listOf("Chhagalnaiya", "Daganbhuiyan", "Feni Sadar", "Fulgazi", "Parshuram", "Sonagazi"),
      "Brahmanbaria" to
        listOf(
          "Akhaura",
          "Ashuganj",
          "Bancharampur",
          "Bijoynagar",
          "Brahmanbaria Sadar",
          "Kasba",
          "Nabinagar",
          "Nasirnagar",
          "Sarail",
        ),
      "Noakhali" to
        listOf(
          "Begumganj",
          "Chatkhil",
          "Companiganj",
          "Hatiya",
          "Kabirhat",
          "Noakhali Sadar",
          "Senbagh",
          "Sonaimuri",
          "Subarnachar",
        ),
      "Lakshmipur" to
        listOf("Kamalnagar", "Lakshmipur Sadar", "Raipur", "Ramganj", "Ramgati"),
      "Chandpur" to
        listOf(
          "Chandpur Sadar",
          "Faridganj",
          "Haimchar",
          "Haziganj",
          "Kachua",
          "Matlab Dakshin",
          "Matlab Uttar",
          "Shahrasti",
        ),
      "Rangamati" to
        listOf(
          "Baghaichhari",
          "Barkal",
          "Belaichhari",
          "Juraichhari",
          "Kaptai",
          "Kawkhali",
          "Langadu",
          "Naniarchar",
          "Rajasthali",
          "Rangamati Sadar",
        ),
      "Khagrachhari" to
        listOf(
          "Dighinala",
          "Guimara",
          "Khagrachhari Sadar",
          "Lakshmichhari",
          "Mahalchhari",
          "Manikchhari",
          "Matiranga",
          "Panchhari",
          "Ramgarh",
        ),
      "Bandarban" to
        listOf(
          "Alikadam",
          "Bandarban Sadar",
          "Lama",
          "Naikhongchhari",
          "Rowangchhari",
          "Ruma",
          "Thanchi",
        ),

      // Rajshahi Division
      "Rajshahi" to
        listOf(
          "Bagha",
          "Bagmara",
          "Charghat",
          "Durgapur",
          "Godagari",
          "Mohanpur",
          "Paba",
          "Puthia",
          "Tanore",
          "Rajshahi Metro (Thana)",
        ),
      "Bogura" to
        listOf(
          "Adamdighi",
          "Bogura Sadar",
          "Dhunat",
          "Dhupchanchia",
          "Gabtali",
          "Kahaloo",
          "Nandigram",
          "Sariakandi",
          "Shajahanpur",
          "Sherpur",
          "Shibganj",
          "Sonatala",
        ),
      "Pabna" to
        listOf(
          "Atgharia",
          "Bera",
          "Bhangura",
          "Chatmohar",
          "Faridpur",
          "Ishwardi",
          "Pabna Sadar",
          "Santhia",
          "Sujanagar",
        ),
      "Sirajganj" to
        listOf(
          "Belkuchi",
          "Chauhali",
          "Kamarkhanda",
          "Kazipur",
          "Raiganj",
          "Shahjadpur",
          "Sirajganj Sadar",
          "Tarash",
          "Ullahpara",
        ),
      "Natore" to
        listOf("Bagatipara", "Baraigram", "Gurudaspur", "Lalpur", "Naldanga", "Natore Sadar", "Singra"),
      "Naogaon" to
        listOf(
          "Atrai",
          "Badalgachhi",
          "Dhamoirhat",
          "Manda",
          "Mohadevpur",
          "Naogaon Sadar",
          "Niamatpur",
          "Patnitala",
          "Porsha",
          "Raninagar",
          "Sapahar",
        ),
      "Chapainawabganj" to
        listOf("Bholahat", "Chapainawabganj Sadar", "Gomastapur", "Nachole", "Shibganj"),
      "Joypurhat" to
        listOf("Akkelpur", "Joypurhat Sadar", "Kalai", "Khetlal", "Panchbibi"),

      // Khulna Division
      "Khulna" to
        listOf(
          "Batiaghata",
          "Dacope",
          "Dighalia",
          "Dumuria",
          "Koyra",
          "Paikgachha",
          "Phultala",
          "Rupsha",
          "Terokhada",
          "Khulna Metro (Thana)",
        ),
      "Jashore" to
        listOf(
          "Abhaynagar",
          "Bagherpara",
          "Chaugachha",
          "Jashore Sadar",
          "Jhikargachha",
          "Keshabpur",
          "Manirampur",
          "Sharsha",
        ),
      "Satkhira" to
        listOf("Assasuni", "Debhata", "Kalaroa", "Kaliganj", "Satkhira Sadar", "Shyamnagar", "Tala"),
      "Bagerhat" to
        listOf(
          "Bagerhat Sadar",
          "Chitalmari",
          "Fakirhat",
          "Kachua",
          "Mollahat",
          "Mongla",
          "Morrelganj",
          "Rampal",
          "Sarankhola",
        ),
      "Narail" to listOf("Kalia", "Lohagara", "Narail Sadar"),
      "Magura" to listOf("Magura Sadar", "Mohammadpur", "Shalikha", "Sreepur"),
      "Jhenaidah" to
        listOf(
          "Harinakunda",
          "Jhenaidah Sadar",
          "Kaliganj",
          "Kotchandpur",
          "Maheshpur",
          "Shailkupa",
        ),
      "Kushtia" to
        listOf("Bheramara", "Daulatpur", "Khoksa", "Kumarkhali", "Kushtia Sadar", "Mirpur"),
      "Chuadanga" to listOf("Alamdanga", "Chuadanga Sadar", "Damurhuda", "Jibannagar"),
      "Meherpur" to listOf("Gangni", "Meherpur Sadar", "Mujibnagar"),

      // Barishal Division
      "Barishal" to
        listOf(
          "Agailjhara",
          "Babuganj",
          "Bakerganj",
          "Banaripara",
          "Barishal Sadar",
          "Gaurnadi",
          "Hizla",
          "Mehendiganj",
          "Muladi",
          "Wazirpur",
          "Barishal Metro (Thana)",
        ),
      "Bhola" to
        listOf(
          "Bhola Sadar",
          "Borhanuddin",
          "Char Fasson",
          "Daulatkhan",
          "Lalmohan",
          "Manpura",
          "Tazumuddin",
        ),
      "Patuakhali" to
        listOf(
          "Bauphal",
          "Dashmina",
          "Dumki",
          "Galachipa",
          "Kalapara",
          "Mirzaganj",
          "Patuakhali Sadar",
          "Rangabali",
        ),
      "Pirojpur" to
        listOf(
          "Bhandaria",
          "Indurkani",
          "Kawkhali",
          "Mathbaria",
          "Nazirpur",
          "Nesarabad (Swarupkathi)",
          "Pirojpur Sadar",
        ),
      "Jhalokati" to listOf("Jhalokati Sadar", "Kathalia", "Nalchity", "Rajapur"),
      "Barguna" to
        listOf("Amtali", "Bamna", "Barguna Sadar", "Betagi", "Patharghata", "Taltali"),

      // Sylhet Division
      "Sylhet" to
        listOf(
          "Balaganj",
          "Beanibazar",
          "Bishwanath",
          "Companiganj",
          "Dakshin Surma",
          "Fenchuganj",
          "Golapganj",
          "Gowainghat",
          "Jaintiapur",
          "Kanaighat",
          "Osmani Nagar",
          "Sylhet Sadar",
          "Zakiganj",
          "Sylhet Metro (Thana)",
        ),
      "Moulvibazar" to
        listOf(
          "Barlekha",
          "Juri",
          "Kamalganj",
          "Kulaura",
          "Moulvibazar Sadar",
          "Rajnagar",
          "Sreemangal",
        ),
      "Habiganj" to
        listOf(
          "Ajmiriganj",
          "Bahubal",
          "Baniyachong",
          "Chunarughat",
          "Habiganj Sadar",
          "Lakhai",
          "Madhabpur",
          "Nabiganj",
          "Shayestaganj",
        ),
      "Sunamganj" to
        listOf(
          "Bishwamvarpur",
          "Chhatak",
          "Derai",
          "Dharamapasha",
          "Dowarabazar",
          "Jagannathpur",
          "Jamalganj",
          "Madhyanagar",
          "Shalla",
          "Shantiganj",
          "Sunamganj Sadar",
          "Tahirpur",
        ),

      // Rangpur Division
      "Rangpur" to
        listOf(
          "Badarganj",
          "Gangachhara",
          "Kaunia",
          "Mithapukur",
          "Pirgachha",
          "Pirganj",
          "Rangpur Sadar",
          "Taraganj",
          "Rangpur Metro (Thana)",
        ),
      "Dinajpur" to
        listOf(
          "Birampur",
          "Birganj",
          "Biral",
          "Bochaganj",
          "Chirirbandar",
          "Dinajpur Sadar",
          "Fulbari",
          "Ghoraghat",
          "Hakimpur",
          "Kaharole",
          "Khansama",
          "Nawabganj",
          "Parbatipur",
        ),
      "Kurigram" to
        listOf(
          "Bhurungamari",
          "Char Rajibpur",
          "Chilmari",
          "Kurigram Sadar",
          "Nageshwari",
          "Phulbari",
          "Rajarhat",
          "Raomari",
          "Ulipur",
        ),
      "Gaibandha" to
        listOf(
          "Fulchhari",
          "Gaibandha Sadar",
          "Gobindaganj",
          "Palashbari",
          "Sadullapur",
          "Saghata",
          "Sundarganj",
        ),
      "Nilphamari" to
        listOf("Dimla", "Domar", "Jaldhaka", "Kishoreganj", "Nilphamari Sadar", "Saidpur"),
      "Lalmonirhat" to
        listOf("Aditmari", "Hatibandha", "Kaliganj", "Lalmonirhat Sadar", "Patgram"),
      "Thakurgaon" to
        listOf("Baliadangi", "Haripur", "Pirganj", "Ranisankail", "Thakurgaon Sadar"),
      "Panchagarh" to
        listOf("Atwari", "Boda", "Debiganj", "Panchagarh Sadar", "Tetulia"),

      // Mymensingh Division
      "Mymensingh" to
        listOf(
          "Bhaluka",
          "Dhobaura",
          "Fulbaria",
          "Gafargaon",
          "Gauripur",
          "Haluaghat",
          "Ishwarganj",
          "Muktagachha",
          "Mymensingh Sadar",
          "Nandail",
          "Phulpur",
          "Tara Khanda",
          "Trishal",
          "Mymensingh Metro (Thana)",
        ),
      "Jamalpur" to
        listOf(
          "Bakshiganj",
          "Dewanganj",
          "Islampur",
          "Jamalpur Sadar",
          "Madarganj",
          "Melandaha",
          "Sarishabari",
        ),
      "Sherpur" to listOf("Jhenaigati", "Nakla", "Nalitabari", "Sherpur Sadar", "Sreebardi"),
      "Netrokona" to
        listOf(
          "Atpara",
          "Barhatta",
          "Durgapur",
          "Kalmakanda",
          "Kendua",
          "Khaliajuri",
          "Madan",
          "Mohanganj",
          "Netrokona Sadar",
          "Purbadhala",
        ),
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

  val EDU_CATEGORIES: List<String> = listOf("Nursing", "HSC", "SSC", "Other")

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
