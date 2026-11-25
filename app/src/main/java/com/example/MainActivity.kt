package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AuthView
import com.example.ui.DirectoryUiState
import com.example.ui.DirectoryViewModel
import com.example.ui.MainTab
import com.example.ui.components.FirstLoginPasswordPromptDialog
import com.example.ui.components.LiveClockAndNewsTickerBar
import com.example.ui.components.StatusBanner
import com.example.ui.screens.AllUsersTab
import com.example.ui.screens.AuthFlowContent
import com.example.ui.screens.DirectoryTab
import com.example.ui.screens.MyProfileTab
import com.example.ui.screens.NurseFormCard
import com.example.ui.screens.PasswordTab
import com.example.ui.screens.RequestsTab
import com.example.ui.theme.BdNursesDirectoryTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      BdNursesDirectoryTheme {
        BdNursesDirectoryApp()
      }
    }
  }
}

private data class NavTabItem(
  val tab: MainTab,
  val label: String,
  val icon: androidx.compose.ui.graphics.vector.ImageVector,
  val badgeCount: Int = 0,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BdNursesDirectoryApp(viewModel: DirectoryViewModel = viewModel()) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val me = state.currentUser
  val isManager = me?.isManagerRole == true
  val isDark = isSystemInDarkTheme()
  val lifecycleOwner = LocalLifecycleOwner.current

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        viewModel.refreshData(silent = true)
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  val navItems =
    if (me == null) {
      emptyList()
    } else if (isManager) {
      listOf(
        NavTabItem(MainTab.DIRECTORY, "Directory", Icons.Filled.Badge),
        NavTabItem(MainTab.REQUESTS, "Requests", Icons.Filled.PendingActions, state.pendingCount),
        NavTabItem(MainTab.ALL_USERS, "Manage", Icons.Filled.Group),
        NavTabItem(MainTab.PASSWORD, "Password", Icons.Filled.Lock),
      )
    } else {
      listOf(
        NavTabItem(MainTab.DIRECTORY, "Directory", Icons.Filled.Badge),
        NavTabItem(MainTab.MY_PROFILE, "My Profile", Icons.Filled.Person),
        NavTabItem(MainTab.PASSWORD, "Password", Icons.Filled.Lock),
      )
    }

  if (me != null && state.showPasswordPromptDialog && me.mustChange) {
    FirstLoginPasswordPromptDialog(
      onChangeNow = viewModel::openPasswordTabFromPrompt,
      onLater = viewModel::dismissPasswordPrompt,
    )
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useRail = maxWidth >= 600.dp && navItems.isNotEmpty()

    Scaffold(
      containerColor = Color.Transparent,
      topBar = {
        Column {
          TopAppBar(
            colors =
              TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
              ),
            title = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Filled.MedicalServices,
                  contentDescription = null,
                  modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                  text = "BD Nurses Directory",
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            },
            actions = {
              IconButton(
                onClick = { viewModel.refreshData() },
                modifier = Modifier.testTag("sync_button"),
              ) {
                Icon(
                  imageVector = Icons.Filled.Refresh,
                  contentDescription = "Sync with Supabase",
                )
              }
              if (me == null) {
                if (state.authView != AuthView.ADMIN_LOGIN) {
                  TextButton(
                    onClick = { viewModel.setAuthView(AuthView.ADMIN_LOGIN) },
                    modifier = Modifier.testTag("header_admin_login_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.AdminPanelSettings,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onPrimary,
                      modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Admin", color = MaterialTheme.colorScheme.onPrimary)
                  }
                }
              } else {
                Text(
                  text = me.name,
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.onPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.widthIn(max = 110.dp),
                )
                IconButton(
                  onClick = { viewModel.logout() },
                  modifier = Modifier.testTag("logout_button"),
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Logout",
                  )
                }
              }
            },
          )
          LiveClockAndNewsTickerBar(
            tickerText = state.newsTickerText,
            isRealtimeConnected = state.isRealtimeConnected,
          )
        }
      },
      bottomBar = {
        if (!useRail && navItems.isNotEmpty()) {
          NavigationBar(windowInsets = WindowInsets.navigationBars) {
            navItems.forEach { item ->
              val selected =
                state.activeTab == item.tab &&
                  state.editingPhone == null &&
                  !state.isAddingNurseByManager
              NavigationBarItem(
                selected = selected,
                onClick = { viewModel.selectTab(item.tab) },
                icon = { TabIconWithBadge(item = item) },
                label = {
                  Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                modifier = Modifier.testTag("nav_tab_${item.tab.name.lowercase()}"),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Box(modifier = Modifier.fillMaxSize()) {
        // Light watercolor medical background + gentle gradient tint
        Image(
          painter = painterResource(id = R.drawable.img_bg_pattern),
          contentDescription = null,
          contentScale = ContentScale.Crop,
          alpha = if (isDark) 0.14f else 0.45f,
          modifier = Modifier.fillMaxSize(),
        )
        Box(
          modifier =
            Modifier.fillMaxSize()
              .background(
                Brush.verticalGradient(
                  colors =
                    if (isDark) {
                      listOf(
                        MaterialTheme.colorScheme.background.copy(alpha = 0.88f),
                        MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
                      )
                    } else {
                      listOf(
                        Color(0xFFF2FAF8).copy(alpha = 0.70f),
                        Color(0xFFE6F4F1).copy(alpha = 0.78f),
                      )
                    }
                )
              )
        )

        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          if (useRail) {
            NavigationRail(modifier = Modifier.fillMaxHeight()) {
              navItems.forEach { item ->
                val selected =
                  state.activeTab == item.tab &&
                    state.editingPhone == null &&
                    !state.isAddingNurseByManager
                NavigationRailItem(
                  selected = selected,
                  onClick = { viewModel.selectTab(item.tab) },
                  icon = { TabIconWithBadge(item = item) },
                  label = { Text(item.label) },
                  modifier = Modifier.testTag("rail_tab_${item.tab.name.lowercase()}"),
                )
              }
            }
          }

          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
          ) {
            Column(
              modifier =
                Modifier.fillMaxSize()
                  .widthIn(max = 720.dp)
                  .padding(horizontal = 14.dp, vertical = 12.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              if (state.isLoading || state.isBusy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
              }

              if (me == null) {
                AuthFlowContent(
                  state = state,
                  onLogin = viewModel::loginUser,
                  onAdminLogin = viewModel::loginAdmin,
                  onSendEmailRecoveryCode = viewModel::sendEmailRecoveryCode,
                  onVerifyEmailCodeReset = viewModel::verifyEmailCodeAndResetPassword,
                  onSendResetRequest = viewModel::sendPasswordResetRequest,
                  onNavigateAuth = viewModel::setAuthView,
                  onDraftChange = viewModel::updateDraft,
                  onSubmitRegistration = viewModel::submitRegistration,
                  modifier = Modifier.fillMaxSize(),
                )
              } else {
                AuthenticatedContent(
                  state = state,
                  isManager = isManager,
                  viewModel = viewModel,
                  modifier = Modifier.fillMaxSize(),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun TabIconWithBadge(item: NavTabItem) {
  if (item.badgeCount > 0) {
    BadgedBox(badge = { Badge { Text("${item.badgeCount}") } }) {
      Icon(imageVector = item.icon, contentDescription = item.label)
    }
  } else {
    Icon(imageVector = item.icon, contentDescription = item.label)
  }
}

@Composable
private fun AuthenticatedContent(
  state: DirectoryUiState,
  isManager: Boolean,
  viewModel: DirectoryViewModel,
  modifier: Modifier = Modifier,
) {
  val me = state.currentUser ?: return

  if (state.editingPhone != null || state.isAddingNurseByManager) {
    BackHandler { viewModel.cancelEditing() }
  } else if (state.activeTab != MainTab.DIRECTORY) {
    BackHandler { viewModel.selectTab(MainTab.DIRECTORY) }
  }

  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
    if (state.bannerNote.isNotBlank()) {
      StatusBanner(
        text = state.bannerNote,
        onDismiss = { viewModel.clearBannerNote() },
      )
    }
    if (me.mustChange) {
      StatusBanner(
        text = "⚠️ আপনি এখনো default password ব্যবহার করছেন। Password tab থেকে বদলে নিন।",
        isWarning = true,
      )
    }
    if (
      state.errorMessage.isNotBlank() &&
        state.editingPhone == null &&
        !state.isAddingNurseByManager &&
        state.activeTab != MainTab.PASSWORD
    ) {
      StatusBanner(
        text = state.errorMessage,
        isWarning = true,
      )
    }

    if (state.isAddingNurseByManager) {
      Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        NurseFormCard(
          title = "নতুন নার্স যোগ করুন (Admin / Editor)",
          submitLabel = "Save & Approve Nurse",
          cancelLabel = "Cancel",
          isEditMode = false,
          isBusy = state.isBusy,
          draft = state.formDraft,
          errorMessage = state.errorMessage,
          onDraftChange = viewModel::updateDraft,
          onSubmit = viewModel::submitAddNurseByManager,
          onCancel = viewModel::cancelEditing,
        )
      }
    } else if (state.editingPhone != null) {
      Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        NurseFormCard(
          title =
            if (isManager) "তথ্য সম্পাদনা (Admin / Editor)"
            else "তথ্য Edit / বদলি (Admin approval লাগবে)",
          submitLabel = if (isManager) "Save Changes" else "Send to Admin for Approval",
          cancelLabel = "Cancel",
          isEditMode = true,
          isBusy = state.isBusy,
          draft = state.formDraft,
          errorMessage = state.errorMessage,
          onDraftChange = viewModel::updateDraft,
          onSubmit = viewModel::saveEdit,
          onCancel = viewModel::cancelEditing,
        )
      }
    } else {
      when (state.activeTab) {
        MainTab.DIRECTORY ->
          DirectoryTab(
            state = state,
            onFilterChange = viewModel::updateFilter,
            onClearFilter = viewModel::clearFilter,
            onTogglePrintCol = viewModel::togglePrintColumn,
            onSetAllPrintCols = viewModel::setAllPrintColumns,
            onPrintError = viewModel::setPrintError,
            onEditUser = viewModel::startEditing,
            modifier = Modifier.fillMaxSize(),
          )

        MainTab.REQUESTS ->
          if (isManager) {
            RequestsTab(
              scopedUsers = state.scopedUsers,
              currentManager = me,
              isBusy = state.isBusy,
              onRegistrationDecision = viewModel::approveRegistration,
              onEditDecision = viewModel::resolvePendingEdit,
              onResetDecision = viewModel::resolveResetRequest,
              modifier = Modifier.fillMaxSize(),
            )
          }

        MainTab.ALL_USERS ->
          if (isManager) {
            AllUsersTab(
              scopedUsers = state.scopedUsers,
              adminsAndEditors = state.scopedAdminsAndEditors,
              currentManager = me,
              newsTickerText = state.newsTickerText,
              storedEmailJsConfig = state.storedEmailJsConfig,
              isBusy = state.isBusy,
              onAddNurse = viewModel::startAddingNurseByManager,
              onSaveNewsTicker = viewModel::saveNewsTicker,
              onSaveEmailJsConfig = viewModel::saveEmailJsConfig,
              onSaveManager = viewModel::saveManagerAccount,
              onDeleteManager = viewModel::adminDeleteUser,
              onEditUser = viewModel::startEditing,
              onResetUserPassword = viewModel::adminResetPassword,
              onDeleteUser = viewModel::adminDeleteUser,
              modifier = Modifier.fillMaxSize(),
            )
          }

        MainTab.MY_PROFILE ->
          MyProfileTab(
            me = me,
            isBusy = state.isBusy,
            onUploadPhoto = viewModel::updateProfilePhoto,
            onPhotoError = viewModel::setPhotoReadError,
            onRemovePhoto = { viewModel.updateProfilePhoto(null) },
            onStartEdit = { viewModel.startEditing(me.phone) },
            modifier = Modifier.fillMaxSize(),
          )

        MainTab.PASSWORD ->
          PasswordTab(
            isFirstTime = false,
            isRegularUser = me.role == "user",
            isBusy = state.isBusy,
            errorMessage = state.errorMessage,
            onSavePassword = viewModel::changePassword,
            onLater = {},
            modifier = Modifier.fillMaxSize(),
          )
      }
    }
  }
}
