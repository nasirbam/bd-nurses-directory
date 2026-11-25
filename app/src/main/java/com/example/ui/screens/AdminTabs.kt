package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.DirectoryConstants
import com.example.data.EmailJsConfig
import com.example.data.NurseRecord
import com.example.ui.components.DropdownField
import com.example.ui.components.NurseProfileCard

@Composable
fun RequestsTab(
  scopedUsers: List<NurseRecord>,
  currentManager: NurseRecord,
  isBusy: Boolean,
  onRegistrationDecision: (String, String) -> Unit,
  onEditDecision: (String, Boolean) -> Unit,
  onResetDecision: (String, Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val pendingRegs =
    remember(scopedUsers) { scopedUsers.filter { it.status == "pending" } }
  val pendingEdits =
    remember(scopedUsers) {
      scopedUsers.filter { it.status == "approved" && it.pendingEdit != null }
    }
  val resetReqs = remember(scopedUsers) { scopedUsers.filter { it.resetReq != null } }

  LazyColumn(
    modifier = modifier,
    contentPadding = PaddingValues(bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    if (!currentManager.isGlobalAdmin) {
      item {
        Text(
          text = "দায়িত্বপ্রাপ্ত এলাকা: ${currentManager.scopeLabel()}",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.SemiBold,
        )
      }
    }

    item {
      Text(
        text = "নতুন নিবন্ধন (${pendingRegs.size})",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
    }
    if (pendingRegs.isEmpty()) {
      item {
        Text(
          text = "কোনো pending আবেদন নেই",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      items(pendingRegs, key = { "reg_${it.phone}" }) { nurse ->
        NurseProfileCard(
          nurse = nurse,
          footerContent = {
            ApproveRejectRow(
              approveText = "Approve",
              rejectText = "Reject",
              enabled = !isBusy,
              onApprove = { onRegistrationDecision(nurse.phone, "approved") },
              onReject = { onRegistrationDecision(nurse.phone, "rejected") },
            )
          },
        )
      }
    }

    item {
      Text(
        text = "তথ্য পরিবর্তন / বদলির অনুরোধ (${pendingEdits.size})",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 6.dp),
      )
    }
    if (pendingEdits.isEmpty()) {
      item {
        Text(
          text = "কোনো অনুরোধ নেই",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      items(pendingEdits, key = { "edit_${it.phone}" }) { nurse ->
        NurseProfileCard(
          nurse = nurse.appliedPendingEdit(),
          footerContent = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = "পুরাতন কর্মস্থল: ${nurse.workplace} (${nurse.district})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              ApproveRejectRow(
                approveText = "Approve",
                rejectText = "Reject",
                enabled = !isBusy,
                onApprove = { onEditDecision(nurse.phone, true) },
                onReject = { onEditDecision(nurse.phone, false) },
              )
            }
          },
        )
      }
    }

    item {
      Text(
        text = "Password reset অনুরোধ (${resetReqs.size})",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 6.dp),
      )
    }
    if (resetReqs.isEmpty()) {
      item {
        Text(
          text = "কোনো অনুরোধ নেই",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      items(resetReqs, key = { "rst_${it.phone}" }) { nurse ->
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
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = "${nurse.name} · ${nurse.phone}",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "${nurse.designation}, ${nurse.workplace}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ApproveRejectRow(
              approveText = "Reset Password",
              rejectText = "Reject",
              enabled = !isBusy,
              onApprove = { onResetDecision(nurse.phone, true) },
              onReject = { onResetDecision(nurse.phone, false) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ApproveRejectRow(
  approveText: String,
  rejectText: String,
  enabled: Boolean,
  onApprove: () -> Unit,
  onReject: () -> Unit,
) {
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
    Button(
      onClick = onApprove,
      enabled = enabled,
      modifier = Modifier.weight(1f).height(48.dp).testTag("approve_action_button"),
    ) {
      Text(approveText)
    }
    Button(
      onClick = onReject,
      enabled = enabled,
      colors =
        ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.error,
          contentColor = Color.White,
        ),
      modifier = Modifier.weight(1f).height(48.dp).testTag("reject_action_button"),
    ) {
      Text(rejectText)
    }
  }
}

@Composable
fun AllUsersTab(
  scopedUsers: List<NurseRecord>,
  adminsAndEditors: List<NurseRecord>,
  currentManager: NurseRecord,
  newsTickerText: String,
  storedEmailJsConfig: EmailJsConfig?,
  isBusy: Boolean,
  onAddNurse: () -> Unit,
  onSaveNewsTicker: (String) -> Unit,
  onSaveEmailJsConfig: (String, String, String) -> Unit,
  onSaveManager: (String, String, String, String, String, String, String, String, Boolean) -> Unit,
  onDeleteManager: (String) -> Unit,
  onEditUser: (String) -> Unit,
  onResetUserPassword: (String) -> Unit,
  onDeleteUser: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val approved = remember(scopedUsers) { scopedUsers.filter { it.status == "approved" } }
  val canDeleteUsers = currentManager.role == "admin" || currentManager.role == "subadmin"

  var confirmDeletePhone by remember { mutableStateOf<String?>(null) }
  var confirmResetPhone by remember { mutableStateOf<String?>(null) }
  var showTickerEditor by rememberSaveable { mutableStateOf(false) }
  var tickerDraft by rememberSaveable(newsTickerText) { mutableStateOf(newsTickerText) }
  var showEmailConfigEditor by rememberSaveable { mutableStateOf(false) }
  var showManagerSection by rememberSaveable { mutableStateOf(false) }

  LazyColumn(
    modifier = modifier,
    contentPadding = PaddingValues(bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Button(
        onClick = onAddNurse,
        enabled = !isBusy,
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_add_nurse_button"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = null)
        Spacer(modifier = Modifier.size(8.dp))
        Text("নতুন নার্স যোগ করুন (Add Nurse)")
      }
    }

    if (currentManager.isGlobalAdmin) {
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
            ),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clickable { showTickerEditor = !showTickerEditor }
                  .padding(vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Filled.Campaign,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                  text = "News Ticker আপডেট করুন",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
              }
              Icon(
                imageVector =
                  if (showTickerEditor) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = "Toggle news ticker editor",
              )
            }

            AnimatedVisibility(visible = showTickerEditor) {
              Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                OutlinedTextField(
                  value = tickerDraft,
                  onValueChange = { tickerDraft = it },
                  label = { Text("উপরের News Ticker বার্তা") },
                  minLines = 2,
                  modifier = Modifier.fillMaxWidth().testTag("news_ticker_input"),
                )
                Button(
                  onClick = { onSaveNewsTicker(tickerDraft) },
                  enabled = !isBusy,
                  modifier =
                    Modifier.fillMaxWidth().height(48.dp).testTag("save_news_ticker_button"),
                ) {
                  Text("News Ticker সেভ করুন")
                }
              }
            }
          }
        }
      }

      item {
        AdminAndEditorSection(
          expanded = showManagerSection,
          onToggleExpand = { showManagerSection = !showManagerSection },
          adminsAndEditors = adminsAndEditors,
          isBusy = isBusy,
          onSaveManager = onSaveManager,
          onDeleteManager = onDeleteManager,
        )
      }

      item {
        EmailJsConfigSection(
          expanded = showEmailConfigEditor,
          onToggleExpand = { showEmailConfigEditor = !showEmailConfigEditor },
          storedConfig = storedEmailJsConfig,
          isBusy = isBusy,
          onSaveEmailJsConfig = onSaveEmailJsConfig,
        )
      }
    }

    item {
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
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          if (!currentManager.isGlobalAdmin) {
            Text(
              text = "এলাকা: ${currentManager.scopeLabel()}",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          Text(
            text = "মোট নিবন্ধিত: ${scopedUsers.size}  ·  Approved: ${approved.size}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          DirectoryConstants.CATEGORIES.keys.forEach { cat ->
            val count = approved.count { it.category == cat }
            Text(
              text = "$cat: $count",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    items(scopedUsers, key = { it.phone }) { user ->
      NurseProfileCard(
        nurse = user,
        footerContent = {
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Button(
              onClick = { onEditUser(user.phone) },
              enabled = !isBusy,
              modifier = Modifier.weight(1f).height(48.dp).testTag("admin_edit_${user.phone}"),
            ) {
              Text("Edit")
            }
            Button(
              onClick = { confirmResetPhone = user.phone },
              enabled = !isBusy,
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.secondaryContainer,
                  contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
              modifier = Modifier.weight(1.2f).height(48.dp).testTag("admin_reset_${user.phone}"),
            ) {
              Text("Reset PW")
            }
            if (canDeleteUsers) {
              Button(
                onClick = { confirmDeletePhone = user.phone },
                enabled = !isBusy,
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = Color.White,
                  ),
                modifier = Modifier.weight(1f).height(48.dp).testTag("admin_delete_${user.phone}"),
              ) {
                Text("Delete")
              }
            }
          }
        },
      )
    }
  }

  confirmDeletePhone?.let { phone ->
    AlertDialog(
      onDismissRequest = { confirmDeletePhone = null },
      title = { Text("Delete User") },
      text = { Text("এই ইউজারকে স্থায়ীভাবে Delete করবেন?") },
      confirmButton = {
        TextButton(
          onClick = {
            confirmDeletePhone = null
            onDeleteUser(phone)
          }
        ) {
          Text("Delete", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { confirmDeletePhone = null }) { Text("Cancel") }
      },
    )
  }

  confirmResetPhone?.let { phone ->
    AlertDialog(
      onDismissRequest = { confirmResetPhone = null },
      title = { Text("Reset Password") },
      text = {
        Text("Password default (1234) এ reset করবেন? ইউজারকে পরবর্তী login এ বদলাতে হবে।")
      },
      confirmButton = {
        TextButton(
          onClick = {
            confirmResetPhone = null
            onResetUserPassword(phone)
          }
        ) {
          Text("Reset")
        }
      },
      dismissButton = {
        TextButton(onClick = { confirmResetPhone = null }) { Text("Cancel") }
      },
    )
  }
}

@Composable
private fun AdminAndEditorSection(
  expanded: Boolean,
  onToggleExpand: () -> Unit,
  adminsAndEditors: List<NurseRecord>,
  isBusy: Boolean,
  onSaveManager: (String, String, String, String, String, String, String, String, Boolean) -> Unit,
  onDeleteManager: (String) -> Unit,
) {
  var editingManagerId by rememberSaveable { mutableStateOf<String?>(null) }
  var userId by rememberSaveable { mutableStateOf("") }
  var name by rememberSaveable { mutableStateOf("") }
  var password by rememberSaveable { mutableStateOf("") }
  var roleLabel by rememberSaveable { mutableStateOf("Admin (Scoped)") }
  var scopeDiv by rememberSaveable { mutableStateOf("") }
  var scopeDist by rememberSaveable { mutableStateOf("") }
  var scopeUpa by rememberSaveable { mutableStateOf("") }
  var scopeWork by rememberSaveable { mutableStateOf("") }

  val isEditMode = editingManagerId != null
  val districtOptions =
    DirectoryConstants.DIVISIONS[scopeDiv] ?: DirectoryConstants.ALL_DISTRICTS
  val upazilaOptions = DirectoryConstants.UPAZILAS[scopeDist]

  fun clearForm() {
    editingManagerId = null
    userId = ""
    name = ""
    password = ""
    roleLabel = "Admin (Scoped)"
    scopeDiv = ""
    scopeDist = ""
    scopeUpa = ""
    scopeWork = ""
  }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
      ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .clickable { onToggleExpand() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Filled.AdminPanelSettings,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Spacer(modifier = Modifier.size(8.dp))
          Text(
            text = "Admin / Editor পরিচালনা (${adminsAndEditors.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
        Icon(
          imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
          contentDescription = "Toggle Admin & Editor section",
        )
      }

      AnimatedVisibility(visible = expanded) {
        Column(
          modifier = Modifier.padding(top = 10.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text =
              if (isEditMode) "Admin / Editor তথ্য সম্পাদনা করছেন (ID: $editingManagerId)"
              else "Division, District, Upazila/Thana বা Hospital অনুযায়ী নতুন Admin / Editor তৈরি করুন (খালি রাখলে সব এলাকা প্রযোজ্য):",
            style = MaterialTheme.typography.bodySmall,
            color =
              if (isEditMode) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isEditMode) FontWeight.SemiBold else FontWeight.Normal,
          )

          DropdownField(
            label = "Role",
            selectedValue = roleLabel,
            placeholder = "Select role",
            options = listOf("Admin (Scoped)", "Editor"),
            onSelected = { roleLabel = it },
            testTag = "manager_role_dropdown",
          )

          OutlinedTextField(
            value = userId,
            onValueChange = { if (!isEditMode) userId = it },
            readOnly = isEditMode,
            enabled = !isEditMode,
            label = { Text("Login ID (Phone/Username) *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("manager_id_input"),
          )

          OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("manager_name_input"),
          )

          OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = {
              Text(
                if (isEditMode) "New Password (অপরিবর্তিত রাখতে খালি রাখুন)"
                else "Password (খালি রাখলে 1234)"
              )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("manager_password_input"),
          )

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DropdownField(
              label = "Scope Division",
              selectedValue = scopeDiv,
              placeholder = "All divisions",
              options = DirectoryConstants.DIVISIONS.keys.toList(),
              includeEmptyOption = true,
              onSelected = { d ->
                scopeDiv = d
                scopeDist = ""
                scopeUpa = ""
              },
              modifier = Modifier.weight(1f),
              testTag = "manager_division_dropdown",
            )
            DropdownField(
              label = "Scope District",
              selectedValue = scopeDist,
              placeholder = "All districts",
              options = districtOptions,
              includeEmptyOption = true,
              onSelected = { dist ->
                scopeDist = dist
                scopeUpa = ""
              },
              modifier = Modifier.weight(1f),
              testTag = "manager_district_dropdown",
            )
          }

          if (upazilaOptions != null) {
            DropdownField(
              label = "Scope Upazila/Thana",
              selectedValue = scopeUpa,
              placeholder = "All upazila/thana",
              options = upazilaOptions + "Other",
              includeEmptyOption = true,
              onSelected = { scopeUpa = it },
              testTag = "manager_upazila_dropdown",
            )
          } else {
            OutlinedTextField(
              value = scopeUpa,
              onValueChange = { scopeUpa = it },
              label = { Text("Scope Upazila/Thana (optional)") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("manager_upazila_input"),
            )
          }

          OutlinedTextField(
            value = scopeWork,
            onValueChange = { scopeWork = it },
            label = { Text("Scope Hospital / Working Place (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("manager_workplace_input"),
          )

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = {
                val roleKey = if (roleLabel.startsWith("Admin")) "subadmin" else "editor"
                onSaveManager(
                  userId,
                  name,
                  password,
                  roleKey,
                  scopeDiv,
                  scopeDist,
                  scopeUpa,
                  scopeWork,
                  isEditMode,
                )
                if (userId.isNotBlank() && name.isNotBlank()) {
                  clearForm()
                }
              },
              enabled = !isBusy,
              modifier = Modifier.weight(1f).height(48.dp).testTag("create_manager_button"),
            ) {
              Text(if (isEditMode) "আপডেট করুন" else "Admin / Editor যোগ করুন")
            }

            if (isEditMode) {
              OutlinedButton(
                onClick = { clearForm() },
                enabled = !isBusy,
                modifier = Modifier.height(48.dp).testTag("cancel_edit_manager_button"),
              ) {
                Text("Cancel")
              }
            }
          }

          if (adminsAndEditors.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            adminsAndEditors.forEach { mgr ->
              Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text =
                      "${mgr.name} (${mgr.phone}) — ${if (mgr.role == "subadmin") "Admin" else "Editor"}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                  )
                  Text(
                    text = "Scope: ${mgr.scopeLabel()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                IconButton(
                  onClick = {
                    editingManagerId = mgr.phone
                    userId = mgr.phone
                    name = mgr.name
                    password = ""
                    roleLabel = if (mgr.role == "subadmin") "Admin (Scoped)" else "Editor"
                    scopeDiv = mgr.scopeDivision
                    scopeDist = mgr.scopeDistrict
                    scopeUpa = mgr.scopeUpazila
                    scopeWork = mgr.scopeWorkplace
                  },
                  enabled = !isBusy,
                  modifier = Modifier.testTag("edit_manager_${mgr.phone}"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit ${mgr.name}",
                    tint = MaterialTheme.colorScheme.primary,
                  )
                }
                IconButton(
                  onClick = {
                    if (editingManagerId == mgr.phone) clearForm()
                    onDeleteManager(mgr.phone)
                  },
                  enabled = !isBusy,
                  modifier = Modifier.testTag("delete_manager_${mgr.phone}"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete ${mgr.name}",
                    tint = MaterialTheme.colorScheme.error,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun EmailJsConfigSection(
  expanded: Boolean,
  onToggleExpand: () -> Unit,
  storedConfig: EmailJsConfig?,
  isBusy: Boolean,
  onSaveEmailJsConfig: (String, String, String) -> Unit,
) {
  var serviceId by rememberSaveable(storedConfig?.serviceId) {
    mutableStateOf(storedConfig?.serviceId.orEmpty())
  }
  var templateId by rememberSaveable(storedConfig?.templateId) {
    mutableStateOf(storedConfig?.templateId.orEmpty())
  }
  var publicKey by rememberSaveable(storedConfig?.publicKey) {
    mutableStateOf(storedConfig?.publicKey.orEmpty())
  }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
      ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .clickable { onToggleExpand() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Filled.Email,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Spacer(modifier = Modifier.size(8.dp))
          Text(
            text = "Email Password Recovery (EmailJS)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
        Icon(
          imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
          contentDescription = "Toggle EmailJS settings",
        )
      }

      AnimatedVisibility(visible = expanded) {
        Column(
          modifier = Modifier.padding(top = 8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "এখানে EmailJS তথ্য সেভ করলে আলাদা করে কোথাও পরিবর্তন করা ছাড়াই সব ইউজারের ফোনে ইমেইলে ৬ ডিজিটের পাসওয়ার্ড কোড কাজ করবে।",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          OutlinedTextField(
            value = serviceId,
            onValueChange = { serviceId = it },
            label = { Text("EmailJS Service ID") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("emailjs_service_id_input"),
          )
          OutlinedTextField(
            value = templateId,
            onValueChange = { templateId = it },
            label = { Text("EmailJS Template ID") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("emailjs_template_id_input"),
          )
          OutlinedTextField(
            value = publicKey,
            onValueChange = { publicKey = it },
            label = { Text("EmailJS Public Key") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("emailjs_public_key_input"),
          )
          Button(
            onClick = { onSaveEmailJsConfig(serviceId, templateId, publicKey) },
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_emailjs_button"),
          ) {
            Text("EmailJS সেটিংস সেভ করুন")
          }
        }
      }
    }
  }
}
