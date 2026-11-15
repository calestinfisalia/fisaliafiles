package com.fisalia.files

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.rememberDrawerState
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fisalia.files.data.local.FileOperationsHelper
import com.fisalia.files.data.local.SettingsPreferences
import com.fisalia.files.ui.components.GoogleFilesBottomNav
import com.fisalia.files.ui.components.NavTab
import com.fisalia.files.ui.player.ActiveMedia
import com.fisalia.files.ui.player.ImageViewerScreen
import com.fisalia.files.ui.player.VideoPlayerScreen
import com.fisalia.files.ui.screens.browse.BrowseScreen
import com.fisalia.files.ui.screens.browse.BrowseViewModel
import com.fisalia.files.ui.screens.clean.CleanScreen
import com.fisalia.files.ui.screens.clean.CleanViewModel
import com.fisalia.files.ui.screens.network.NetworkScreen
import com.fisalia.files.ui.screens.network.NetworkViewModel
import com.fisalia.files.ui.screens.settings.SettingsScreen
import com.fisalia.files.ui.screens.trash.TrashScreen
import com.fisalia.files.ui.theme.FisaliaFilesTheme
import com.fisalia.files.ui.theme.GoogleBlueContainer
import com.fisalia.files.ui.theme.GoogleBluePrimary
import com.fisalia.files.ui.theme.GoogleCyan
import com.fisalia.files.ui.theme.GoogleGreen
import com.fisalia.files.ui.theme.GooglePurple
import com.fisalia.files.ui.theme.GoogleRed
import com.fisalia.files.ui.theme.GoogleYellow

class MainActivity : ComponentActivity() {

    private var hasStoragePermission by mutableStateOf(false)

    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkPermission()
    }

    private val legacyPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        checkPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkPermission()

        val settingsPrefs = SettingsPreferences.getInstance(applicationContext)

        setContent {
            val appTheme by settingsPrefs.theme.collectAsState()
            val dynamicColor by settingsPrefs.dynamicColor.collectAsState()
            val wallpaperBg by settingsPrefs.wallpaperBackground.collectAsState()

            val isDark = when (appTheme) {
                com.fisalia.files.data.local.AppTheme.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                com.fisalia.files.data.local.AppTheme.LIGHT -> false
                com.fisalia.files.data.local.AppTheme.DARK -> true
            }

            LaunchedEffect(wallpaperBg) {
                if (wallpaperBg) {
                    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
                    window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                } else {
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
                    window.setBackgroundDrawable(null)
                }
            }

            FisaliaFilesTheme(
                appTheme = appTheme,
                dynamicColor = dynamicColor,
                isTransparentBackground = wallpaperBg
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (wallpaperBg) Color.Transparent
                            else MaterialTheme.colorScheme.background
                        )
                ) {
                    if (wallpaperBg) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (isDark) Color.Black.copy(alpha = 0.55f)
                                    else Color.White.copy(alpha = 0.65f)
                                )
                        )
                    }

                    if (hasStoragePermission) {
                        MainScreen(settingsPrefs = settingsPrefs)
                    } else {
                        StoragePermissionScreen(
                            onRequestPermission = { requestStoragePermission() }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermission()
    }

    private fun checkPermission() {
        hasStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                manageStorageLauncher.launch(intent)
            } catch (_: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                manageStorageLauncher.launch(intent)
            }
        } else {
            legacyPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }
}

@Composable
fun MainScreen(
    browseViewModel: BrowseViewModel = viewModel(),
    cleanViewModel: CleanViewModel = viewModel(),
    networkViewModel: NetworkViewModel = viewModel(),
    settingsPrefs: SettingsPreferences = SettingsPreferences.getInstance(LocalContext.current)
) {
    var showSettings by remember { mutableStateOf(false) }
    var showTrashScreen by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var privacyTermsType by remember { mutableStateOf<Boolean?>(null) }
    var showSafePinFromDrawer by remember { mutableStateOf(false) }
    val activeMedia by browseViewModel.activeMedia.collectAsState()
    val browseUiState by browseViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val internalStorageUsed = browseUiState.storageDevices
        .firstOrNull { !it.isRemovable && !it.isNetwork }
        ?.usedBytes
        ?: Environment.getExternalStorageDirectory().let { storage ->
            (storage.totalSpace - storage.freeSpace).coerceAtLeast(0L)
        }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = 1,
        pageCount = { 3 }
    )
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    val selectedTab = NavTab.entries.getOrElse(pagerState.currentPage) { NavTab.BROWSE }

    // When entering network storage, automatically highlight NETWORK tab (page 2) once
    LaunchedEffect(browseUiState.activeConnection?.id) {
        if (browseUiState.activeConnection != null && pagerState.currentPage != NavTab.NETWORK.ordinal) {
            pagerState.animateScrollToPage(NavTab.NETWORK.ordinal)
        }
    }

    val handleSmbBack: () -> Unit = {
        val state = browseViewModel.uiState.value
        val isAtSmbRoot = state.activeConnection != null &&
            (state.currentPath.isBlank() || state.currentPath == "/") &&
            !state.isSelectionMode &&
            !state.isSearchActive

        browseViewModel.navigateBack()
        if (isAtSmbRoot) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(NavTab.BROWSE.ordinal)
            }
        }
    }

    androidx.activity.compose.BackHandler(
        enabled = browseUiState.activeConnection != null &&
            selectedTab == NavTab.NETWORK &&
            activeMedia == null &&
            !showSettings &&
            !showTrashScreen &&
            !showHelpDialog &&
            privacyTermsType == null &&
            !showSafePinFromDrawer &&
            drawerState.isClosed
    ) {
        handleSmbBack()
    }

    val handleTabSelect: (NavTab) -> Unit = { tab ->
        if (tab == NavTab.BROWSE && pagerState.currentPage == NavTab.BROWSE.ordinal) {
            browseViewModel.resetToHome()
        }
        coroutineScope.launch {
            pagerState.animateScrollToPage(tab.ordinal)
        }
    }

    if (showSettings) {
        SettingsScreen(
            settingsPrefs = settingsPrefs,
            onBackClick = { showSettings = false }
        )
    } else if (showTrashScreen) {
        TrashScreen(
            onBackClick = { showTrashScreen = false }
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = activeMedia == null && !browseUiState.isSelectionMode,
            drawerContent = {
                ModalDrawerSheet(
                    drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(com.fisalia.files.ui.theme.GoogleBlueContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderSpecial,
                                contentDescription = null,
                                tint = com.fisalia.files.ui.theme.GoogleBluePrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.app_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    NavigationDrawerItem(
                        label = {
                            Text(
                                stringResource(R.string.drawer_browse),
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        icon = { Icon(Icons.Default.Folder, contentDescription = null, tint = com.fisalia.files.ui.theme.GoogleBluePrimary) },
                        selected = selectedTab == NavTab.BROWSE,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            handleTabSelect(NavTab.BROWSE)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    // Clean Item
                    NavigationDrawerItem(
                        label = {
                            Column {
                                Text(stringResource(R.string.drawer_clean), fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = stringResource(
                                        R.string.drawer_clean_sub,
                                        FileOperationsHelper.formatFileSize(internalStorageUsed)
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GoogleYellow) },
                        selected = selectedTab == NavTab.CLEAN,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            handleTabSelect(NavTab.CLEAN)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    // Network Storage Item (Moved to Left Drawer as requested!)
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.drawer_network), fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Dns, contentDescription = null, tint = GoogleCyan) },
                        selected = selectedTab == NavTab.NETWORK,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            handleTabSelect(NavTab.NETWORK)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    // Safe Folder Item
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.drawer_safe_folder), fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = GooglePurple) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            showSafePinFromDrawer = true
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    // Trash Item
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.drawer_trash), fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = GoogleRed) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            showTrashScreen = true
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    // Settings Item
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.drawer_settings), fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = GoogleGreen) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            showSettings = true
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    // Help & Feedback
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.drawer_help), fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.HelpOutline, contentDescription = null, tint = GoogleBluePrimary) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            showHelpDialog = true
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.drawer_privacy),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable {
                                coroutineScope.launch { drawerState.close() }
                                privacyTermsType = true
                            }
                        )
                        Text(
                            text = stringResource(R.string.drawer_terms),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable {
                                coroutineScope.launch { drawerState.close() }
                                privacyTermsType = false
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                MainTabContent(
                    pagerState = pagerState,
                    cleanViewModel = cleanViewModel,
                    browseViewModel = browseViewModel,
                    networkViewModel = networkViewModel,
                    onBackFromSmb = handleSmbBack,
                    onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                    onOpenNetworkStorage = { handleTabSelect(NavTab.NETWORK) },
                    onOpenSettings = { showSettings = true },
                    userScrollEnabled = activeMedia == null && !browseUiState.isSelectionMode
                )

                // Top-level Media Pager (Swiping between photos, videos, and audio)
                activeMedia?.let { media ->
                    com.fisalia.files.ui.player.MediaPagerScreen(
                        activeMedia = media,
                        onClose = { browseViewModel.closeMedia() }
                    )
                }

                if (showHelpDialog) {
                    com.fisalia.files.ui.components.HelpFeedbackDialog(
                        onDismiss = { showHelpDialog = false }
                    )
                }

                privacyTermsType?.let { isPrivacy ->
                    com.fisalia.files.ui.components.PrivacyTermsDialog(
                        isPrivacy = isPrivacy,
                        onDismiss = { privacyTermsType = null }
                    )
                }

                if (showSafePinFromDrawer) {
                    val savedPin = browseViewModel.settingsPrefs.getSafeFolderPin()
                    val isSettingPin = savedPin == null
                    com.fisalia.files.ui.components.SafeFolderPinDialog(
                        isSettingPin = isSettingPin,
                        onDismiss = { showSafePinFromDrawer = false },
                        onConfirm = { pin ->
                            if (isSettingPin) {
                                browseViewModel.settingsPrefs.setSafeFolderPin(pin)
                                showSafePinFromDrawer = false
                                browseViewModel.openSafeFolder()
                            } else if (pin == savedPin) {
                                showSafePinFromDrawer = false
                                browseViewModel.openSafeFolder()
                            } else {
                                android.widget.Toast.makeText(context, context.getString(R.string.safe_folder_pin_incorrect), android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MainTabContent(
    pagerState: androidx.compose.foundation.pager.PagerState,
    cleanViewModel: CleanViewModel,
    browseViewModel: BrowseViewModel,
    networkViewModel: NetworkViewModel,
    onBackFromSmb: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenNetworkStorage: () -> Unit,
    onOpenSettings: () -> Unit,
    userScrollEnabled: Boolean = true
) {
    androidx.compose.foundation.pager.HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = userScrollEnabled
    ) { page ->
        when (NavTab.entries[page]) {
            NavTab.CLEAN -> CleanScreen(
                viewModel = cleanViewModel,
                isActive = pagerState.currentPage == NavTab.CLEAN.ordinal
            )
            NavTab.BROWSE -> BrowseScreen(
                viewModel = browseViewModel,
                onOpenDrawer = onOpenDrawer,
                onOpenNetworkStorage = onOpenNetworkStorage,
                onOpenSettings = onOpenSettings
            )
            NavTab.NETWORK -> {
                val browseUiState by browseViewModel.uiState.collectAsState()
                if (browseUiState.activeConnection != null) {
                    com.fisalia.files.ui.screens.browse.ExplorerScreen(
                        state = browseUiState,
                        viewModel = browseViewModel,
                        onBackClick = onBackFromSmb
                    )
                } else {
                    NetworkScreen(
                        viewModel = networkViewModel,
                        onConnectServer = { conn ->
                            browseViewModel.openNetworkConnection(conn)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StoragePermissionScreen(
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(GoogleBlueContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = GoogleBluePrimary,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.permission_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.permission_desc),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRequestPermission,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoogleBluePrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = stringResource(R.string.btn_grant_permission),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
