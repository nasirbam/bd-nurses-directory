package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DirectoryConstants
import com.example.data.EducationEntry
import com.example.data.NurseFormDraft
import com.example.ui.AuthView
import com.example.ui.DirectoryUiState
import com.example.ui.components.DropdownField
import com.example.ui.components.IsoDatePickerField
import com.example.ui.components.StatusBanner
import com.example.util.ImageHelper

@Composable
fun AuthFlowContent(
  state: DirectoryUiState,
  onLogin: (String, String) -> Unit,
  onAdminLogin: (String, String) -> Unit,
  onSendEmailRecoveryCode: (String) -> Unit,
  onVerifyEmailCodeReset: (String, String, String) -> Unit,
  onSendResetRequest: (String) -> Unit,
  onNavigateAuth: (AuthView) -> Unit,
  onDraftChange: ((NurseFormDraft) -> NurseFormDraft) -> Unit,
  onSubmitRegistration: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    if (!state.isSupabaseConfigured) {
      StatusBanner(
        text =
          "⚠️ Supabase সংযোগ কনফিগার করা হয়নি। AI Studio Secrets প্যানেলে SUPABASE_URL এবং SUPABASE_ANON_KEY সেট করুন।",
        isWarning = true,
      )
    }

    if (state.authView == AuthView.LOGIN && state.successMessage.isNotBlank()) {
      StatusBanner(text = state.successMessage)
    }

    when (state.authView) {
      AuthView.LOGIN ->
        LoginCard(
          isBusy = state.isBusy,
          errorMessage = state.errorMessage,
          onLogin = onLogin,
          onForgot = { onNavigateAuth(AuthView.FORGOT_PASSWORD) },
          onRegister = { onNavigateAuth(AuthView.REGISTER) },
        )

      AuthView.ADMIN_LOGIN -> {
        BackHandler { onNavigateAuth(AuthView.LOGIN) }
        AdminLoginCard(
          isBusy = state.isBusy,
          errorMessage = state.errorMessage,
          onAdminLogin = onAdminLogin,
          onBack = { onNavigateAuth(AuthView.LOGIN) },
        )
      }

      AuthView.FORGOT_PASSWORD -> {
        BackHandler { onNavigateAuth(AuthView.LOGIN) }
        ForgotPasswordCard(
          isBusy = state.isBusy,
          codeSent = state.recoveryStepCodeSent,
          maskedEmail = state.recoveryTargetEmailMasked,
          errorMessage = state.errorMessage,
          successMessage = state.successMessage,
          onSendEmailCode = onSendEmailRecoveryCode,
          onVerifyEmailCodeReset = onVerifyEmailCodeReset,
          onSendAdminReset = onSendResetRequest,
          onBack = { onNavigateAuth(AuthView.LOGIN) },
        )
      }

      AuthView.REGISTER -> {
        BackHandler { onNavigateAuth(AuthView.LOGIN) }
        NurseFormCard(
          title = "Nurse Registration",
          submitLabel = "Submit for Approval",
          cancelLabel = "Back to Login",
          isEditMode = false,
          isBusy = state.isBusy,
          draft = state.formDraft,
          errorMessage = state.errorMessage,
          onDraftChange = onDraftChange,
          onSubmit = onSubmitRegistration,
          onCancel = { onNavigateAuth(AuthView.LOGIN) },
        )
      }

      AuthView.REGISTRATION_SUBMITTED -> {
        BackHandler { onNavigateAuth(AuthView.LOGIN) }
        Card(
          shape = RoundedCornerShape(16.dp),
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
            ),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
              )
              Spacer(modifier = Modifier.size(8.dp))
              Text(
                text = "আবেদন জমা হয়েছে",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
              )
            }
            Text(
              text =
                "Admin approve করলে আপনি Phone নম্বর ও Password 1234 দিয়ে login করতে পারবেন। প্রথমবার login এ password বদলানোর অপশন পাবেন।",
              style = MaterialTheme.typography.bodyLarge,
            )
            Button(
              onClick = { onNavigateAuth(AuthView.LOGIN) },
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("go_to_login_button"),
            ) {
              Text("Login এ যান")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun LoginCard(
  isBusy: Boolean,
  errorMessage: String,
  onLogin: (String, String) -> Unit,
  onForgot: () -> Unit,
  onRegister: () -> Unit,
) {
  var phone by rememberSaveable { mutableStateOf("") }
  var password by rememberSaveable { mutableStateOf("") }
  var passwordVisible by rememberSaveable { mutableStateOf(false) }

  Card(
    shape = RoundedCornerShape(18.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
      ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Image(
        painter = painterResource(id = R.drawable.img_hero_banner),
        contentDescription = "BD Nurses Directory illustration",
        contentScale = ContentScale.Crop,
        modifier =
          Modifier.fillMaxWidth().height(155.dp).clip(RoundedCornerShape(14.dp)),
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = "Login",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
      )
      OutlinedTextField(
        value = phone,
        onValueChange = { phone = it },
        label = { Text("Phone number (User ID)") },
        placeholder = { Text("01XXXXXXXXX") },
        singleLine = true,
        keyboardOptions =
          KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth().testTag("login_phone_input"),
      )
      OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password") },
        singleLine = true,
        visualTransformation =
          if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
          IconButton(onClick = { passwordVisible = !passwordVisible }) {
            Icon(
              imageVector =
                if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
              contentDescription = if (passwordVisible) "Hide password" else "Show password",
            )
          }
        },
        keyboardOptions =
          KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onLogin(phone, password) }),
        modifier = Modifier.fillMaxWidth().testTag("login_password_input"),
      )
      if (errorMessage.isNotBlank()) {
        Text(
          text = errorMessage,
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
        )
      }
      Button(
        onClick = { onLogin(phone, password) },
        enabled = !isBusy,
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("login_button"),
      ) {
        if (isBusy) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary,
          )
        } else {
          Text("Login")
        }
      }
      OutlinedButton(
        onClick = onForgot,
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("forgot_password_button"),
      ) {
        Text("Password ভুলে গেছেন?")
      }
      OutlinedButton(
        onClick = onRegister,
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("open_register_button"),
      ) {
        Text("Register (নতুন নিবন্ধন)")
      }
    }
  }
}

@Composable
private fun AdminLoginCard(
  isBusy: Boolean,
  errorMessage: String,
  onAdminLogin: (String, String) -> Unit,
  onBack: () -> Unit,
) {
  var adminId by rememberSaveable { mutableStateOf("") }
  var password by rememberSaveable { mutableStateOf("") }
  var passwordVisible by rememberSaveable { mutableStateOf(false) }

  Card(
    shape = RoundedCornerShape(18.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
      ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Filled.AdminPanelSettings,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
          text = "Admin / Editor Panel Login",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
        )
      }
      OutlinedTextField(
        value = adminId,
        onValueChange = { adminId = it },
        label = { Text("Admin / Editor ID") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth().testTag("admin_id_input"),
      )
      OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password") },
        singleLine = true,
        visualTransformation =
          if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
          IconButton(onClick = { passwordVisible = !passwordVisible }) {
            Icon(
              imageVector =
                if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
              contentDescription = if (passwordVisible) "Hide password" else "Show password",
            )
          }
        },
        keyboardOptions =
          KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onAdminLogin(adminId, password) }),
        modifier = Modifier.fillMaxWidth().testTag("admin_password_input"),
      )
      if (errorMessage.isNotBlank()) {
        Text(
          text = errorMessage,
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
        )
      }
      Button(
        onClick = { onAdminLogin(adminId, password) },
        enabled = !isBusy,
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_login_submit_button"),
      ) {
        if (isBusy) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary,
          )
        } else {
          Text("Login as Admin / Editor")
        }
      }
      OutlinedButton(
        onClick = onBack,
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_back_button"),
      ) {
        Text("Back")
      }
    }
  }
}

@Composable
private fun ForgotPasswordCard(
  isBusy: Boolean,
  codeSent: Boolean,
  maskedEmail: String,
  errorMessage: String,
  successMessage: String,
  onSendEmailCode: (String) -> Unit,
  onVerifyEmailCodeReset: (String, String, String) -> Unit,
  onSendAdminReset: (String) -> Unit,
  onBack: () -> Unit,
) {
  var emailOrPhone by rememberSaveable { mutableStateOf("") }
  var code by rememberSaveable { mutableStateOf("") }
  var newPw by rememberSaveable { mutableStateOf("") }
  var confirmPw by rememberSaveable { mutableStateOf("") }
  var adminPhone by rememberSaveable { mutableStateOf("") }

  Card(
    shape = RoundedCornerShape(18.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
      ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Filled.Email,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
          text = "Password Recovery (Email কোড)",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )
      }
      Text(
        text =
          "নিবন্ধিত Email অথবা Phone নম্বর দিন। আপনার Email-এ ৬ ডিজিটের ভেরিফিকেশন কোড পাঠানো হবে, যা দিয়ে সরাসরি নতুন Password সেট করতে পারবেন।",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      OutlinedTextField(
        value = emailOrPhone,
        onValueChange = { emailOrPhone = it },
        label = { Text("Registered Email বা Phone") },
        placeholder = { Text("name@example.com / 01XXXXXXXXX") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("recovery_email_or_phone_input"),
      )
      Button(
        onClick = { onSendEmailCode(emailOrPhone) },
        enabled = !isBusy,
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("send_email_code_button"),
      ) {
        Text(if (codeSent) "আবার কোড পাঠান" else "Email-এ কোড পাঠান")
      }

      if (codeSent) {
        Text(
          text = "কোড পাঠানো হয়েছে: $maskedEmail",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
        )
        OutlinedTextField(
          value = code,
          onValueChange = { code = it },
          label = { Text("৬ ডিজিটের ভেরিফিকেশন কোড") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth().testTag("recovery_code_input"),
        )
        OutlinedTextField(
          value = newPw,
          onValueChange = { newPw = it },
          label = { Text("New Password (min 4)") },
          singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          modifier = Modifier.fillMaxWidth().testTag("recovery_new_pw_input"),
        )
        OutlinedTextField(
          value = confirmPw,
          onValueChange = { confirmPw = it },
          label = { Text("Confirm Password") },
          singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          modifier = Modifier.fillMaxWidth().testTag("recovery_confirm_pw_input"),
        )
        Button(
          onClick = { onVerifyEmailCodeReset(code, newPw, confirmPw) },
          enabled = !isBusy,
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("verify_code_reset_button"),
        ) {
          Text("Verify & Change Password")
        }
      }

      HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

      Text(
        text = "অথবা Email যোগ করা না থাকলে Admin-এর কাছে অনুরোধ পাঠান:",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      OutlinedTextField(
        value = adminPhone,
        onValueChange = { adminPhone = it },
        label = { Text("Phone number") },
        placeholder = { Text("01XXXXXXXXX") },
        singleLine = true,
        keyboardOptions =
          KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth().testTag("forgot_phone_input"),
      )
      OutlinedButton(
        onClick = { onSendAdminReset(adminPhone) },
        enabled = !isBusy,
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("send_reset_request_button"),
      ) {
        Text("Admin কে অনুরোধ পাঠান")
      }

      if (errorMessage.isNotBlank()) {
        Text(
          text = errorMessage,
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
        )
      }
      if (successMessage.isNotBlank()) {
        Text(
          text = successMessage,
          color = MaterialTheme.colorScheme.primary,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium,
        )
      }

      OutlinedButton(
        onClick = onBack,
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("forgot_back_button"),
      ) {
        Text("Back to Login")
      }
    }
  }
}

@Composable
fun NurseFormCard(
  title: String,
  submitLabel: String,
  cancelLabel: String,
  isEditMode: Boolean,
  isBusy: Boolean,
  draft: NurseFormDraft,
  errorMessage: String,
  onDraftChange: ((NurseFormDraft) -> NurseFormDraft) -> Unit,
  onSubmit: () -> Unit,
  onCancel: () -> Unit,
) {
  val context = LocalContext.current
  val designationOptions =
    DirectoryConstants.CATEGORIES[draft.category] ?: emptyList()
  val districtOptions =
    DirectoryConstants.DIVISIONS[draft.division] ?: emptyList()
  val upazilaPreset = DirectoryConstants.UPAZILAS[draft.district]
  val prlPreview = remember(draft.dob) { DirectoryConstants.calculatePrlDate(draft.dob) }
  val previewBitmap = remember(draft.photo) { ImageHelper.decodeBase64Avatar(draft.photo) }

  val photoPickerLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
      if (uri != null) {
        val encoded = ImageHelper.encodeAvatarFromUri(context, uri)
        if (encoded != null) {
          onDraftChange { it.copy(photo = encoded) }
        }
      }
    }

  Card(
    shape = RoundedCornerShape(18.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
      ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
      )

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        if (previewBitmap != null) {
          Image(
            bitmap = previewBitmap,
            contentDescription = "Selected profile picture",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(56.dp).clip(CircleShape),
          )
        }
        OutlinedButton(
          onClick = {
            photoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
          modifier = Modifier.weight(1f).height(48.dp).testTag("form_pick_photo_button"),
        ) {
          Icon(Icons.Filled.PhotoCamera, contentDescription = null)
          Spacer(modifier = Modifier.size(6.dp))
          Text(
            if (draft.photo.isNullOrBlank()) "Profile Photo (ঐচ্ছিক)"
            else "Photo পরিবর্তন করুন"
          )
        }
        if (!draft.photo.isNullOrBlank()) {
          OutlinedButton(
            onClick = { onDraftChange { it.copy(photo = null) } },
            modifier = Modifier.height(48.dp),
          ) {
            Text("মুছুন")
          }
        }
      }

      DropdownField(
        label = "Category *",
        selectedValue = draft.category,
        placeholder = "Select category",
        options = DirectoryConstants.CATEGORIES.keys.toList(),
        onSelected = { cat ->
          onDraftChange { it.copy(category = cat, designation = "") }
        },
        testTag = "form_category_dropdown",
      )

      OutlinedTextField(
        value = draft.name,
        onValueChange = { v -> onDraftChange { it.copy(name = v) } },
        label = { Text("Name *") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("form_name_input"),
      )

      OutlinedTextField(
        value = draft.father,
        onValueChange = { v -> onDraftChange { it.copy(father = v) } },
        label = { Text("Father's Name *") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("form_father_input"),
      )

      OutlinedTextField(
        value = draft.mother,
        onValueChange = { v -> onDraftChange { it.copy(mother = v) } },
        label = { Text("Mother's Name *") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("form_mother_input"),
      )

      IsoDatePickerField(
        label = "Date of Birth *",
        value = draft.dob,
        onValueSelected = { d -> onDraftChange { it.copy(dob = d) } },
        testTag = "form_dob_input",
      )
      if (prlPreview.isNotBlank()) {
        Text(
          text = "PRL Date: $prlPreview",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.SemiBold,
        )
      }

      IsoDatePickerField(
        label = "Joining Date (চাকরিতে/বর্তমান কর্মস্থলে যোগদানের তারিখ) *",
        value = draft.joinDate,
        onValueSelected = { d -> onDraftChange { it.copy(joinDate = d) } },
        testTag = "form_join_date_input",
      )

      OutlinedTextField(
        value = draft.phone,
        onValueChange = { v -> onDraftChange { it.copy(phone = v) } },
        label = { Text("Phone number (User ID) *") },
        placeholder = { Text("01XXXXXXXXX") },
        readOnly = isEditMode,
        enabled = !isEditMode,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier = Modifier.fillMaxWidth().testTag("form_phone_input"),
      )

      OutlinedTextField(
        value = draft.bnmc,
        onValueChange = { v -> onDraftChange { it.copy(bnmc = v) } },
        label = { Text("BNMC Reg no. *") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("form_bnmc_input"),
      )

      DropdownField(
        label = "Designation *",
        selectedValue = draft.designation,
        placeholder =
          if (draft.category.isBlank()) "Select category first" else "Select designation",
        options = designationOptions,
        enabled = designationOptions.isNotEmpty(),
        onSelected = { des -> onDraftChange { it.copy(designation = des) } },
        testTag = "form_designation_dropdown",
      )

      OutlinedTextField(
        value = draft.workplace,
        onValueChange = { v -> onDraftChange { it.copy(workplace = v) } },
        label = { Text("Working Place (Institute/Hospital name) *") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("form_workplace_input"),
      )

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DropdownField(
          label = "Division *",
          selectedValue = draft.division,
          placeholder = "Division",
          options = DirectoryConstants.DIVISIONS.keys.toList(),
          onSelected = { div ->
            onDraftChange { it.copy(division = div, district = "", upazila = "") }
          },
          modifier = Modifier.weight(1f),
          testTag = "form_division_dropdown",
        )
        DropdownField(
          label = "District *",
          selectedValue = draft.district,
          placeholder = "District",
          options = districtOptions,
          enabled = districtOptions.isNotEmpty(),
          onSelected = { dist ->
            onDraftChange { it.copy(district = dist, upazila = "") }
          },
          modifier = Modifier.weight(1f),
          testTag = "form_district_dropdown",
        )
      }

      var customUpazilaSelected by rememberSaveable(draft.district) {
        mutableStateOf(
          upazilaPreset != null && draft.upazila.isNotBlank() && draft.upazila !in upazilaPreset
        )
      }

      if (upazilaPreset != null) {
        DropdownField(
          label = "Upazila/Thana *",
          selectedValue =
            if (customUpazilaSelected) "Other" else draft.upazila,
          placeholder = "Upazila/Thana",
          options = upazilaPreset + "Other",
          onSelected = { up ->
            if (up == "Other") {
              customUpazilaSelected = true
              if (draft.upazila in upazilaPreset || draft.upazila == "Other") {
                onDraftChange { it.copy(upazila = "") }
              }
            } else {
              customUpazilaSelected = false
              onDraftChange { it.copy(upazila = up) }
            }
          },
          testTag = "form_upazila_dropdown",
        )
        if (customUpazilaSelected) {
          OutlinedTextField(
            value = draft.upazila,
            onValueChange = { v -> onDraftChange { it.copy(upazila = v) } },
            label = { Text("Custom Upazila/Thana নাম লিখুন *") },
            placeholder = { Text("Upazila/Thana লিখুন") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("form_upazila_custom_input"),
          )
        }
      } else {
        OutlinedTextField(
          value = draft.upazila,
          onValueChange = { v -> onDraftChange { it.copy(upazila = v) } },
          label = { Text("Upazila/Thana *") },
          placeholder = { Text("Upazila/Thana লিখুন") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("form_upazila_input"),
        )
      }

      OutlinedTextField(
        value = draft.address,
        onValueChange = { v -> onDraftChange { it.copy(address = v) } },
        label = { Text("Present / Working Address") },
        minLines = 2,
        maxLines = 3,
        modifier = Modifier.fillMaxWidth().testTag("form_address_input"),
      )

      DropdownField(
        label = "Home District (নিজ জেলা)",
        selectedValue = draft.homeDistrict,
        placeholder = "Select Home District",
        options = DirectoryConstants.ALL_DISTRICTS,
        includeEmptyOption = true,
        onSelected = { hd -> onDraftChange { it.copy(homeDistrict = hd) } },
        testTag = "form_home_district_dropdown",
      )

      OutlinedTextField(
        value = draft.homeAddress,
        onValueChange = { v -> onDraftChange { it.copy(homeAddress = v) } },
        label = { Text("Home Address (গ্রামের বাড়ি / স্থায়ী ঠিকানা)") },
        placeholder = { Text("গ্রাম/বাড়ি, ডাকঘর, উপজেলা") },
        minLines = 2,
        maxLines = 3,
        modifier = Modifier.fillMaxWidth().testTag("form_home_address_input"),
      )

      val eduEntries = draft.educationEntries.ifEmpty { listOf(EducationEntry()) }
      val updateEntry: (Int, EducationEntry) -> Unit = { idx, updatedEntry ->
        onDraftChange { cur ->
          val base = cur.educationEntries.ifEmpty { listOf(EducationEntry()) }.toMutableList()
          if (idx in base.indices) {
            base[idx] = updatedEntry
          }
          cur.copy(educationEntries = base)
        }
      }

      Text(
        text = "Educational Qualifications *",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
      )

      eduEntries.forEachIndexed { index, entry ->
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text =
                  if (index == 0) "মূল শিক্ষাগত যোগ্যতা #1 *"
                  else "অতিরিক্ত যোগ্যতা #${index + 1}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
              )
              if (eduEntries.size > 1) {
                IconButton(
                  onClick = {
                    onDraftChange { cur ->
                      val list = cur.educationEntries.toMutableList()
                      if (index in list.indices && list.size > 1) {
                        list.removeAt(index)
                      }
                      cur.copy(educationEntries = list)
                    }
                  },
                  modifier = Modifier.size(36.dp).testTag("remove_edu_button_$index"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Remove qualification ${index + 1}",
                    tint = MaterialTheme.colorScheme.error,
                  )
                }
              }
            }

            DropdownField(
              label = "Educational Qualification — Category *",
              selectedValue = entry.cat,
              placeholder = "Select",
              options = DirectoryConstants.EDU_CATEGORIES,
              onSelected = { ec ->
                updateEntry(
                  index,
                  entry.copy(
                    cat = ec,
                    sub = if (ec == "Nursing") entry.sub else "",
                    customSub = if (ec == "Other" || (ec == "Nursing" && entry.sub == "Other")) entry.customSub else "",
                  ),
                )
              },
              testTag = if (index == 0) "form_edu_category_dropdown" else "form_edu_category_dropdown_$index",
            )

            if (entry.cat == "Nursing") {
              DropdownField(
                label = "Nursing — Sub Category *",
                selectedValue = entry.sub,
                placeholder = "Select",
                options = DirectoryConstants.NURSING_SUB_CATEGORIES,
                onSelected = { sub ->
                  updateEntry(
                    index,
                    entry.copy(
                      sub = sub,
                      customSub = if (sub == "Other") entry.customSub else "",
                    ),
                  )
                },
                testTag = if (index == 0) "form_edu_sub_dropdown" else "form_edu_sub_dropdown_$index",
              )
              if (entry.sub == "Other") {
                OutlinedTextField(
                  value = entry.customSub,
                  onValueChange = { v -> updateEntry(index, entry.copy(customSub = v)) },
                  label = { Text("Custom Nursing Degree / Training লিখুন *") },
                  placeholder = { Text("যেমন: MPH, OT/ICU Specialization") },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth().testTag("form_edu_custom_sub_$index"),
                )
              }
            } else if (entry.cat == "Other") {
              OutlinedTextField(
                value = entry.customSub,
                onValueChange = { v -> updateEntry(index, entry.copy(customSub = v)) },
                label = { Text("Qualification / Degree নাম লিখুন *") },
                placeholder = { Text("যেমন: B.A, B.S.S, MPH, PhD") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("form_edu_custom_cat_$index"),
              )
            }

            OutlinedTextField(
              value = entry.institute,
              onValueChange = { v -> updateEntry(index, entry.copy(institute = v)) },
              label = { Text("Institute Name") },
              placeholder = { Text("প্রতিষ্ঠানের নাম") },
              singleLine = true,
              modifier =
                Modifier.fillMaxWidth()
                  .testTag(if (index == 0) "form_edu_institute_input" else "form_edu_institute_input_$index"),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = entry.passingYear,
                onValueChange = { v -> updateEntry(index, entry.copy(passingYear = v)) },
                label = { Text("Passing Year") },
                placeholder = { Text("e.g. 2018") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier =
                  Modifier.weight(1f)
                    .testTag(if (index == 0) "form_edu_year_input" else "form_edu_year_input_$index"),
              )
              OutlinedTextField(
                value = entry.major,
                onValueChange = { v -> updateEntry(index, entry.copy(major = v)) },
                label = { Text("Major / Subject") },
                placeholder = { Text("e.g. Science / Nursing") },
                singleLine = true,
                modifier =
                  Modifier.weight(1f)
                    .testTag(if (index == 0) "form_edu_major_input" else "form_edu_major_input_$index"),
              )
            }
          }
        }
      }

      OutlinedButton(
        onClick = {
          onDraftChange { cur ->
            val base = cur.educationEntries.ifEmpty { listOf(EducationEntry()) }
            cur.copy(educationEntries = base + EducationEntry())
          }
        },
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("add_more_edu_button"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = null)
        Spacer(modifier = Modifier.size(6.dp))
        Text("আরও Qualification যোগ করুন (Add More)")
      }

      OutlinedTextField(
        value = draft.email,
        onValueChange = { v -> onDraftChange { it.copy(email = v) } },
        label = { Text("Email (পাসওয়ার্ড রিকভারির জন্য)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier.fillMaxWidth().testTag("form_email_input"),
      )

      if (errorMessage.isNotBlank()) {
        Text(
          text = errorMessage,
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
        )
      }

      Button(
        onClick = onSubmit,
        enabled = !isBusy,
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("form_submit_button"),
      ) {
        if (isBusy) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary,
          )
        } else {
          Text(submitLabel)
        }
      }

      OutlinedButton(
        onClick = onCancel,
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("form_cancel_button"),
      ) {
        Text(cancelLabel)
      }
    }
  }
}
