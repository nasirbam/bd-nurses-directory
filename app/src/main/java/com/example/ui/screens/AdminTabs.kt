package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.DirectoryConstants
import com.example.data.NurseRecord
import com.example.ui.components.NurseProfileCard

@Composable
fun RequestsTab(
  records: List<NurseRecord>,
  isBusy: Boolean,
  onRegistrationDecision: (String, String) -> Unit,
  onEditDecision: (String, Boolean) -> Unit,
  onResetDecision: (String, Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val pendingRegs =
    remember(records) { records.filter { it.role == "user" && it.status == "pending" } }
  val pendingEdits =
    remember(records) {
      records.filter { it.role == "user" && it.status == "approved" && it.pendingEdit != null }
    }
  val resetReqs = remember(records) { records.filter { it.role == "user" && it.resetReq != null } }

  LazyColumn(
    modifier = modifier,
    contentPadding = PaddingValues(bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
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
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
  records: List<NurseRecord>,
  isBusy: Boolean,
  onEditUser: (String) -> Unit,
  onResetUserPassword: (String) -> Unit,
  onDeleteUser: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val users = remember(records) { records.filter { it.role == "user" } }
  val approved = remember(users) { users.filter { it.status == "approved" } }

  var confirmDeletePhone by remember { mutableStateOf<String?>(null) }
  var confirmResetPhone by remember { mutableStateOf<String?>(null) }

  LazyColumn(
    modifier = modifier,
    contentPadding = PaddingValues(bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = "মোট নিবন্ধিত: ${users.size}  ·  Approved: ${approved.size}",
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

    items(users, key = { it.phone }) { user ->
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
