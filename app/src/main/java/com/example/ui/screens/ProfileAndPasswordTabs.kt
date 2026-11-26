package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.NurseRecord
import com.example.ui.components.NurseProfileCard
import com.example.ui.components.StatusBanner
import com.example.util.ImageHelper

@Composable
fun MyProfileTab(
  me: NurseRecord,
  isBusy: Boolean,
  onUploadPhoto: (String) -> Unit,
  onPhotoError: () -> Unit,
  onRemovePhoto: () -> Unit,
  onStartEdit: () -> Unit,
  onShowQr: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val photoPickerLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
      if (uri != null) {
        val encoded = ImageHelper.encodeAvatarFromUri(context, uri)
        if (encoded != null) onUploadPhoto(encoded) else onPhotoError()
      }
    }

  Column(
    modifier = modifier.verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    if (me.pendingEdit != null) {
      StatusBanner(
        text = "⏳ আপনার তথ্য পরিবর্তনের অনুরোধ Admin approval এর অপেক্ষায় আছে",
        isWarning = false,
      )
    }

    NurseProfileCard(
      nurse = me,
      footerContent = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pick_photo_button"),
          ) {
            Icon(Icons.Filled.PhotoCamera, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text(
              if (!me.photo.isNullOrBlank()) "Profile picture পরিবর্তন করুন"
              else "Profile picture যোগ করুন (ঐচ্ছিক)"
            )
          }

          if (!me.photo.isNullOrBlank()) {
            OutlinedButton(
              onClick = onRemovePhoto,
              enabled = !isBusy,
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("remove_photo_button"),
            ) {
              Text("Picture মুছুন")
            }
          }

          OutlinedButton(
            onClick = onShowQr,
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("my_profile_qr_button"),
          ) {
            Icon(Icons.Filled.QrCode2, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text("My Directory QR Code")
          }

          Button(
            onClick = onStartEdit,
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("edit_profile_button"),
          ) {
            Icon(Icons.Filled.Edit, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text("তথ্য Edit / বদলি Request")
          }
        }
      },
    )
  }
}

@Composable
fun PasswordTab(
  isFirstTime: Boolean,
  isRegularUser: Boolean,
  isBusy: Boolean,
  errorMessage: String,
  onSavePassword: (String, String) -> Unit,
  onLater: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var newPw by rememberSaveable { mutableStateOf("") }
  var confirmPw by rememberSaveable { mutableStateOf("") }
  var showPw by rememberSaveable { mutableStateOf(false) }

  Column(modifier = modifier.verticalScroll(rememberScrollState())) {
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = if (isFirstTime) "প্রথমবার Password পরিবর্তন করুন" else "Change Password",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
        )
        OutlinedTextField(
          value = newPw,
          onValueChange = { newPw = it },
          label = { Text("New Password (min 4)") },
          singleLine = true,
          visualTransformation =
            if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
          trailingIcon = {
            IconButton(onClick = { showPw = !showPw }) {
              Icon(
                imageVector = if (showPw) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                contentDescription = if (showPw) "Hide password" else "Show password",
              )
            }
          },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          modifier = Modifier.fillMaxWidth().testTag("new_password_input"),
        )
        OutlinedTextField(
          value = confirmPw,
          onValueChange = { confirmPw = it },
          label = { Text("Confirm") },
          singleLine = true,
          visualTransformation =
            if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          modifier = Modifier.fillMaxWidth().testTag("confirm_password_input"),
        )
        if (errorMessage.isNotBlank()) {
          Text(
            text = errorMessage,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
          )
        }
        Button(
          onClick = { onSavePassword(newPw, confirmPw) },
          enabled = !isBusy,
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_password_button"),
        ) {
          if (isBusy) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.onPrimary,
            )
          } else {
            Text("Save")
          }
        }
        if (isFirstTime && isRegularUser) {
          OutlinedButton(
            onClick = onLater,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("later_password_button"),
          ) {
            Text("পরে করব (Later)")
          }
        }
      }
    }
  }
}
