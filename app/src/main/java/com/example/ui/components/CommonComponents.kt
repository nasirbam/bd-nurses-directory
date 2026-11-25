package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.DirectoryConstants
import com.example.data.NurseRecord
import com.example.util.ImageHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun StatusBanner(
  text: String,
  isWarning: Boolean = false,
  onDismiss: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  if (text.isBlank()) return
  val containerColor =
    if (isWarning) MaterialTheme.colorScheme.tertiaryContainer
    else MaterialTheme.colorScheme.primary
  val contentColor =
    if (isWarning) MaterialTheme.colorScheme.onTertiaryContainer
    else MaterialTheme.colorScheme.onPrimary

  Surface(
    color = containerColor,
    contentColor = contentColor,
    shape = RoundedCornerShape(12.dp),
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.weight(1f),
      )
      if (onDismiss != null) {
        TextButton(onClick = onDismiss) {
          Text("OK", color = contentColor)
        }
      }
    }
  }
}

@Composable
fun DropdownField(
  label: String,
  selectedValue: String,
  placeholder: String,
  options: List<String>,
  onSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  includeEmptyOption: Boolean = false,
  testTag: String = "",
) {
  var expanded by remember { mutableStateOf(false) }

  Box(modifier = modifier) {
    OutlinedTextField(
      value = selectedValue.ifBlank { "" },
      onValueChange = {},
      readOnly = true,
      enabled = enabled,
      label = { Text(label) },
      placeholder = { Text(placeholder) },
      trailingIcon = {
        Icon(
          imageVector = Icons.Filled.KeyboardArrowDown,
          contentDescription = "Select $label",
        )
      },
      modifier =
        Modifier.fillMaxWidth().let { if (testTag.isNotBlank()) it.testTag(testTag) else it },
    )
    Box(
      modifier =
        Modifier.matchParentSize()
          .clickable(enabled = enabled) { expanded = true }
    )
    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
    ) {
      if (includeEmptyOption) {
        DropdownMenuItem(
          text = { Text(placeholder) },
          onClick = {
            expanded = false
            onSelected("")
          },
        )
      }
      options.forEach { option ->
        DropdownMenuItem(
          text = { Text(option) },
          onClick = {
            expanded = false
            onSelected(option)
          },
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IsoDatePickerField(
  label: String,
  value: String,
  onValueSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  testTag: String = "",
) {
  var showPicker by remember { mutableStateOf(false) }

  Box(modifier = modifier) {
    OutlinedTextField(
      value = value,
      onValueChange = {},
      readOnly = true,
      label = { Text(label) },
      placeholder = { Text("YYYY-MM-DD") },
      trailingIcon = {
        Icon(imageVector = Icons.Filled.Cake, contentDescription = "Pick $label")
      },
      modifier =
        Modifier.fillMaxWidth().let { if (testTag.isNotBlank()) it.testTag(testTag) else it },
    )
    Box(modifier = Modifier.matchParentSize().clickable { showPicker = true })
  }

  if (showPicker) {
    val initialMillis =
      remember(value) {
        if (value.isBlank()) null
        else {
          try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US)
              .apply { timeZone = TimeZone.getTimeZone("UTC") }
              .parse(value)
              ?.time
          } catch (_: Exception) {
            null
          }
        }
      }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
      onDismissRequest = { showPicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { millis ->
              val formatted =
                SimpleDateFormat("yyyy-MM-dd", Locale.US)
                  .apply { timeZone = TimeZone.getTimeZone("UTC") }
                  .format(Date(millis))
              onValueSelected(formatted)
            }
            showPicker = false
          }
        ) {
          Text("OK")
        }
      },
      dismissButton = {
        TextButton(onClick = { showPicker = false }) {
          Text("Cancel")
        }
      },
    ) {
      DatePicker(state = datePickerState)
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NurseProfileCard(
  nurse: NurseRecord,
  modifier: Modifier = Modifier,
  footerContent: @Composable (() -> Unit)? = null,
) {
  val context = LocalContext.current
  val avatarBitmap = remember(nurse.photo) { ImageHelper.decodeBase64Avatar(nurse.photo) }
  val prlDate = remember(nurse.dob, nurse.prl) {
    DirectoryConstants.calculatePrlDate(nurse.dob).ifBlank { nurse.prl }
  }

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = nurse.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Spacer(modifier = Modifier.height(6.dp))
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            TagChip(text = nurse.designation)
            TagChip(text = nurse.category)
            if (nurse.status.isNotBlank() && nurse.status != "approved") {
              TagChip(text = nurse.status, isWarning = true)
            }
          }
        }
        Spacer(modifier = Modifier.width(12.dp))
        if (avatarBitmap != null) {
          Image(
            bitmap = avatarBitmap,
            contentDescription = "${nurse.name} profile photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(64.dp).clip(CircleShape),
          )
        } else {
          Box(
            modifier =
              Modifier.size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Filled.Person,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
          Modifier.clip(RoundedCornerShape(8.dp))
            .clickable {
              val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${nurse.phone}"))
              context.startActivity(intent)
            }
            .padding(vertical = 4.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.Call,
          contentDescription = "Call ${nurse.phone}",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = nurse.phone,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.SemiBold,
        )
        if (nurse.bnmc.isNotBlank()) {
          Text(
            text = "  ·  BNMC: ${nurse.bnmc}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      InfoLine(icon = Icons.Filled.LocalHospital, text = nurse.workplace)
      InfoLine(
        icon = Icons.Filled.LocationOn,
        text =
          "${nurse.upazila}, ${nurse.district}, ${nurse.division}" +
            if (nurse.address.isNotBlank()) " — ${nurse.address}" else "",
      )
      InfoLine(
        icon = Icons.Filled.School,
        text =
          nurse.education +
            if (nurse.eduInfo.isNotBlank()) " (${nurse.eduInfo})" else "",
      )
      InfoLine(
        icon = Icons.Filled.Badge,
        text = "Father: ${nurse.father}  ·  Mother: ${nurse.mother}",
      )
      InfoLine(
        icon = Icons.Filled.Cake,
        text = "DOB: ${nurse.dob}  ·  PRL: $prlDate",
        highlight = true,
      )
      InfoLine(
        icon = Icons.Filled.WorkHistory,
        text = "Joining Date: ${nurse.joinDate.ifBlank { "—" }}",
      )
      if (nurse.email.isNotBlank()) {
        InfoLine(icon = Icons.Filled.Email, text = nurse.email)
      }

      if (footerContent != null) {
        Spacer(modifier = Modifier.height(10.dp))
        footerContent()
      }
    }
  }
}

@Composable
private fun TagChip(text: String, isWarning: Boolean = false) {
  if (text.isBlank()) return
  Surface(
    shape = RoundedCornerShape(99.dp),
    color =
      if (isWarning) MaterialTheme.colorScheme.tertiaryContainer
      else MaterialTheme.colorScheme.secondaryContainer,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelMedium,
      color =
        if (isWarning) MaterialTheme.colorScheme.onTertiaryContainer
        else MaterialTheme.colorScheme.onSecondaryContainer,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
    )
  }
}

@Composable
private fun InfoLine(icon: ImageVector, text: String, highlight: Boolean = false) {
  if (text.isBlank()) return
  Row(
    verticalAlignment = Alignment.Top,
    modifier = Modifier.padding(vertical = 2.dp),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(top = 2.dp).size(15.dp),
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = text,
      style = MaterialTheme.typography.bodySmall,
      fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal,
      color =
        if (highlight) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
