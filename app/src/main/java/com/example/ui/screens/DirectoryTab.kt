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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PeopleOutline
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.DirectoryConstants
import com.example.ui.DirectoryFilter
import com.example.ui.DirectoryUiState
import com.example.ui.components.DropdownField
import com.example.ui.components.NurseProfileCard
import com.example.util.PrintHelper

@Composable
fun DirectoryTab(
  state: DirectoryUiState,
  onFilterChange: ((DirectoryFilter) -> DirectoryFilter) -> Unit,
  onClearFilter: () -> Unit,
  onTogglePrintCol: (String, Boolean) -> Unit,
  onSetAllPrintCols: (Boolean) -> Unit,
  onPrintError: (String) -> Unit,
  onEditUser: (String) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val filtered = state.filteredNurses
  val currentManager = state.currentUser
  val isAdmin = currentManager?.isManagerRole == true
  var showPrintOptions by rememberSaveable { mutableStateOf(true) }

  val districtOptions =
    DirectoryConstants.DIVISIONS[state.filter.division] ?: DirectoryConstants.ALL_DISTRICTS
  val upazilaOptions = DirectoryConstants.UPAZILAS[state.filter.district]

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
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedTextField(
            value = state.filter.query,
            onValueChange = { q -> onFilterChange { it.copy(query = q) } },
            label = { Text("Name / Phone / BNMC search…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
              if (state.filter.query.isNotEmpty()) {
                IconButton(onClick = { onFilterChange { it.copy(query = "") } }) {
                  Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                }
              }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("directory_search_input"),
          )

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DropdownField(
              label = "Category",
              selectedValue = state.filter.category,
              placeholder = "All categories",
              options = DirectoryConstants.CATEGORIES.keys.toList(),
              includeEmptyOption = true,
              onSelected = { c -> onFilterChange { it.copy(category = c) } },
              modifier = Modifier.weight(1f),
              testTag = "filter_category_dropdown",
            )
            DropdownField(
              label = "Designation",
              selectedValue = state.filter.designation,
              placeholder = "All designations",
              options = DirectoryConstants.ALL_DESIGNATIONS,
              includeEmptyOption = true,
              onSelected = { s -> onFilterChange { it.copy(designation = s) } },
              modifier = Modifier.weight(1f),
              testTag = "filter_designation_dropdown",
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DropdownField(
              label = "Division",
              selectedValue = state.filter.division,
              placeholder = "All divisions",
              options = DirectoryConstants.DIVISIONS.keys.toList(),
              includeEmptyOption = true,
              onSelected = { d ->
                onFilterChange { it.copy(division = d, district = "", upazila = "") }
              },
              modifier = Modifier.weight(1f),
              testTag = "filter_division_dropdown",
            )
            DropdownField(
              label = "District",
              selectedValue = state.filter.district,
              placeholder = "All districts",
              options = districtOptions,
              includeEmptyOption = true,
              onSelected = { dist ->
                onFilterChange { it.copy(district = dist, upazila = "") }
              },
              modifier = Modifier.weight(1f),
              testTag = "filter_district_dropdown",
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (upazilaOptions != null) {
              DropdownField(
                label = "Upazila/Thana",
                selectedValue = state.filter.upazila,
                placeholder = "All upazila/thana",
                options = upazilaOptions + "Other",
                includeEmptyOption = true,
                onSelected = { u -> onFilterChange { it.copy(upazila = u) } },
                modifier = Modifier.weight(1f),
                testTag = "filter_upazila_dropdown",
              )
            } else {
              OutlinedTextField(
                value = state.filter.upazila,
                onValueChange = { u -> onFilterChange { it.copy(upazila = u) } },
                label = { Text("Upazila/Thana") },
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("filter_upazila_input"),
              )
            }
            OutlinedTextField(
              value = state.filter.workplace,
              onValueChange = { h -> onFilterChange { it.copy(workplace = h) } },
              label = { Text("Hospital / Institute") },
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("filter_workplace_input"),
            )
          }

          OutlinedButton(
            onClick = onClearFilter,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("filter_clear_button"),
          ) {
            Text("Filter clear")
          }
        }
      }
    }

    if (isAdmin) {
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clickable { showPrintOptions = !showPrintOptions }
                  .padding(vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Filled.Print,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                  text = "Print Options (Admin)",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
              }
              Icon(
                imageVector =
                  if (showPrintOptions) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = "Toggle print options",
              )
            }

            AnimatedVisibility(visible = showPrintOptions) {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                  text = "যে তথ্যগুলো print করতে চান সেগুলোতে tick দিন:",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DirectoryConstants.PRINT_FIELDS.chunked(2).forEach { pair ->
                  Row(modifier = Modifier.fillMaxWidth()) {
                    pair.forEach { (key, label) ->
                      val checked = key in state.selectedPrintColumns
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                          Modifier.weight(1f)
                            .clickable { onTogglePrintCol(key, !checked) }
                            .padding(vertical = 2.dp),
                      ) {
                        Checkbox(
                          checked = checked,
                          onCheckedChange = { onTogglePrintCol(key, it) },
                        )
                        Text(
                          text = label,
                          style = MaterialTheme.typography.bodySmall,
                        )
                      }
                    }
                    if (pair.size == 1) {
                      Spacer(modifier = Modifier.weight(1f))
                    }
                  }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  OutlinedButton(
                    onClick = { onSetAllPrintCols(true) },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("print_select_all"),
                  ) {
                    Text("Select all")
                  }
                  OutlinedButton(
                    onClick = { onSetAllPrintCols(false) },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("print_clear_all"),
                  ) {
                    Text("Clear all")
                  }
                }
                Button(
                  onClick = {
                    when {
                      state.selectedPrintColumns.isEmpty() ->
                        onPrintError("কমপক্ষে একটি তথ্যে tick দিন")
                      filtered.isEmpty() ->
                        onPrintError("Print করার মতো কোনো তথ্য নেই")
                      else -> {
                        onPrintError("")
                        PrintHelper.printDirectory(
                          context = context,
                          records = filtered,
                          selectedKeys = state.selectedPrintColumns,
                          filterSummary = state.filter.summary(),
                        )
                      }
                    }
                  },
                  modifier = Modifier.fillMaxWidth().height(48.dp).testTag("print_button"),
                ) {
                  Icon(Icons.Filled.Print, contentDescription = null)
                  Spacer(modifier = Modifier.size(8.dp))
                  Text("Print (${filtered.size} জন)")
                }
                if (state.printError.isNotBlank()) {
                  Text(
                    text = state.printError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                  )
                }
              }
            }
          }
        }
      }
    }

    item {
      Text(
        text = "${filtered.size} জন পাওয়া গেছে",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    if (filtered.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.PeopleOutline,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(36.dp),
            )
            Text(
              text = "কোনো অনুমোদিত তথ্য পাওয়া যায়নি",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    } else {
      items(items = filtered, key = { it.phone }) { nurse ->
        val canEditHere = currentManager?.canManageNurse(nurse) == true
        NurseProfileCard(
          nurse = nurse,
          footerContent =
            if (canEditHere) {
              {
                OutlinedButton(
                  onClick = { onEditUser(nurse.phone) },
                  modifier =
                    Modifier.fillMaxWidth().height(48.dp).testTag("dir_edit_${nurse.phone}"),
                ) {
                  Icon(Icons.Filled.Edit, contentDescription = null)
                  Spacer(modifier = Modifier.size(8.dp))
                  Text("তথ্য Edit করুন")
                }
              }
            } else null,
        )
      }
    }
  }
}
