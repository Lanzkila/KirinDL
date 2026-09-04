package com.junkfood.seal.ui.page.settings.sealplus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.SignalCellular4Bar
import androidx.compose.material.icons.outlined.SignalWifi4Bar
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.junkfood.seal.R
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.component.PreferenceItem
import com.junkfood.seal.ui.component.PreferenceSubtitle
import com.junkfood.seal.ui.component.PreferenceSingleChoiceItem
import com.junkfood.seal.ui.component.PreferenceSwitch
import com.junkfood.seal.ui.page.security.LockScreen
import com.junkfood.seal.util.AuthenticationManager
import com.junkfood.seal.util.BILIBILI_CUSTOM_FRAGMENTS
import com.junkfood.seal.util.BILIBILI_SPEED_AUTO
import com.junkfood.seal.util.BILIBILI_SPEED_BALANCED
import com.junkfood.seal.util.BILIBILI_SPEED_CUSTOM
import com.junkfood.seal.util.BILIBILI_SPEED_FAST
import com.junkfood.seal.util.BILIBILI_SPEED_MODE
import com.junkfood.seal.util.FORMAT_LIST_VIEW
import com.junkfood.seal.util.HOME_RECENT_LIMIT
import com.junkfood.seal.util.HOME_TRANSFER_DETAILS
import com.junkfood.seal.util.HOME_INPUT_ANIMATION
import com.junkfood.seal.util.HOME_QUICK_TOOLS
import com.junkfood.seal.util.HOME_SHOW_ACTIVITY
import com.junkfood.seal.util.HOME_COMPACT_ACTIVITY
import com.junkfood.seal.util.QUEUE_BULK_CONFIRM
import com.junkfood.seal.util.MAX_CONCURRENT_DOWNLOADS
import com.junkfood.seal.util.FileUtil
import com.junkfood.seal.util.GALLERY_DL_DIRECTORY
import com.junkfood.seal.util.NETWORK_ANY
import com.junkfood.seal.util.NETWORK_MOBILE_ONLY
import com.junkfood.seal.util.NETWORK_PAUSE_DELAY_SECONDS
import com.junkfood.seal.util.NETWORK_TYPE_RESTRICTION
import com.junkfood.seal.util.NETWORK_WIFI_ONLY
import com.junkfood.seal.util.NOTIFICATION_ERROR_SOUND
import com.junkfood.seal.util.NOTIFICATION_LED
import com.junkfood.seal.util.NOTIFICATION_SOUND
import com.junkfood.seal.util.NOTIFICATION_SUCCESS_SOUND
import com.junkfood.seal.util.NOTIFICATION_VIBRATE
import com.junkfood.seal.util.SPONSOR_DIALOG_FREQUENCY
import com.junkfood.seal.util.SPONSOR_FREQ_MONTHLY
import com.junkfood.seal.util.SPONSOR_FREQ_OFF
import com.junkfood.seal.util.SPONSOR_FREQ_WEEKLY
import com.junkfood.seal.util.PreferenceUtil.getBoolean
import com.junkfood.seal.util.PreferenceUtil.getInt
import com.junkfood.seal.util.PreferenceUtil.getString
import com.junkfood.seal.util.PreferenceUtil.updateBoolean
import com.junkfood.seal.util.PreferenceUtil.updateInt
import com.junkfood.seal.util.makeToast
import com.junkfood.seal.util.GalleryDlBehaviorPreference
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SealPlusExtrasPage(
    onNavigateBack: () -> Unit,
    onNavigateToSecurity: () -> Unit = {},
    onNavigateToHiddenContent: () -> Unit = {},
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var networkTypeRestriction by remember { mutableStateOf(NETWORK_TYPE_RESTRICTION.getInt()) }
    var showNetworkDialog by remember { mutableStateOf(false) }
    var networkPauseDelaySeconds by remember {
        mutableStateOf(NETWORK_PAUSE_DELAY_SECONDS.getInt())
    }
    
    var notificationSound by remember { mutableStateOf(NOTIFICATION_SOUND.getBoolean()) }
    var notificationVibrate by remember { mutableStateOf(NOTIFICATION_VIBRATE.getBoolean()) }
    var notificationLed by remember { mutableStateOf(NOTIFICATION_LED.getBoolean()) }
    var notificationSuccessSound by remember { mutableStateOf(NOTIFICATION_SUCCESS_SOUND.getBoolean()) }
    var sponsorDialogFrequency by remember { mutableStateOf(SPONSOR_DIALOG_FREQUENCY.getInt()) }
    var showSponsorFrequencyDialog by remember { mutableStateOf(false) }
    var notificationErrorSound by remember { mutableStateOf(NOTIFICATION_ERROR_SOUND.getBoolean()) }
    var formatListView by remember { mutableStateOf(FORMAT_LIST_VIEW.getBoolean()) }
    var homeRecentLimit by remember { mutableStateOf(HOME_RECENT_LIMIT.getInt().coerceIn(3, 10)) }
    var homeTransferDetails by remember { mutableStateOf(HOME_TRANSFER_DETAILS.getBoolean()) }
    var homeInputAnimation by remember { mutableStateOf(HOME_INPUT_ANIMATION.getBoolean()) }
    var homeQuickTools by remember { mutableStateOf(HOME_QUICK_TOOLS.getBoolean()) }
    var homeShowActivity by remember { mutableStateOf(HOME_SHOW_ACTIVITY.getBoolean()) }
    var homeCompactActivity by remember { mutableStateOf(HOME_COMPACT_ACTIVITY.getBoolean()) }
    var queueBulkConfirm by remember { mutableStateOf(QUEUE_BULK_CONFIRM.getBoolean()) }
    var showHomeRecentDialog by remember { mutableStateOf(false) }
    var storageSnapshot by remember { mutableStateOf(StorageSnapshot()) }
    var storageMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun refreshStorage() {
        scope.launch {
            storageSnapshot = withContext(Dispatchers.IO) { measureStorage() }
        }
    }

    LaunchedEffect(Unit) { refreshStorage() }

    val galleryConfirmBeforeDownload by
        GalleryDlBehaviorPreference.confirmBeforeDownload.collectAsState()

    val bilibiliFragmentOptions = remember { listOf(1, 4, 8, 12, 16) }
    var bilibiliSpeedMode by remember {
        mutableStateOf(BILIBILI_SPEED_MODE.getInt().coerceIn(BILIBILI_SPEED_AUTO, BILIBILI_SPEED_CUSTOM))
    }
    var bilibiliCustomFragments by remember {
        mutableStateOf(
            BILIBILI_CUSTOM_FRAGMENTS.getInt().let { saved ->
                if (saved in bilibiliFragmentOptions) saved else 8
            }
        )
    }
    var showBilibiliSpeedDialog by remember { mutableStateOf(false) }
    var showBilibiliFragmentsDialog by remember { mutableStateOf(false) }
    
    // Authentication state for AppLock settings
    var showAuthScreen by remember { mutableStateOf(false) }
    var isAuthenticated by remember { mutableStateOf(false) }

    // Authentication state for Hidden Content navigation
    var showHiddenContentAuthScreen by remember { mutableStateOf(false) }
    var hiddenContentAuthDone by remember { mutableStateOf(false) }

    // Show authentication screen if AppLock is enabled and user tries to access settings
    if (showAuthScreen && !isAuthenticated) {
        LockScreen(
            onUnlocked = {
                isAuthenticated = true
                showAuthScreen = false
                onNavigateToSecurity()
            },
            useBiometric = AuthenticationManager.useBiometric()
        )
        return
    }

    // Show authentication screen before entering Hidden Content page
    if (showHiddenContentAuthScreen && !hiddenContentAuthDone) {
        LockScreen(
            onUnlocked = {
                hiddenContentAuthDone = true
                showHiddenContentAuthScreen = false
                onNavigateToHiddenContent()
            },
            useBiometric = AuthenticationManager.useBiometric()
        )
        return
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { 
                    Text(text = stringResource(id = R.string.sealplus_extras)) 
                },
                navigationIcon = { BackButton(onNavigateBack) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues)
        ) {
            item {
                PreferenceSubtitle(text = stringResource(R.string.download_control))
            }
            
            item {
                var maxConcurrentDownloads by remember { 
                    mutableStateOf(MAX_CONCURRENT_DOWNLOADS.getInt()) 
                }
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.max_concurrent_downloads),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (maxConcurrentDownloads == 0) {
                                    stringResource(R.string.unlimited_concurrent)
                                } else {
                                    stringResource(R.string.concurrent_downloads_desc, maxConcurrentDownloads)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = if (maxConcurrentDownloads == 0) "∞" else maxConcurrentDownloads.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    androidx.compose.material3.Slider(
                        value = maxConcurrentDownloads.toFloat(),
                        onValueChange = { newValue ->
                            maxConcurrentDownloads = newValue.toInt()
                        },
                        onValueChangeFinished = {
                            MAX_CONCURRENT_DOWNLOADS.updateInt(maxConcurrentDownloads)
                        },
                        valueRange = 0f..5f,
                        steps = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "0 (${stringResource(R.string.unlimited)})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "5",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            item {
                PreferenceSwitch(
                    title = "Confirm bulk queue cleanup",
                    description = "Ask before Clear Completed removes completed items from the active queue view",
                    icon = Icons.Outlined.Delete,
                    isChecked = queueBulkConfirm,
                    onClick = {
                        queueBulkConfirm = !queueBulkConfirm
                        QUEUE_BULK_CONFIRM.updateBoolean(queueBulkConfirm)
                    },
                )
            }

            item {
                PreferenceSubtitle(text = "Storage Manager")
            }
            item {
                StorageManagerCard(
                    snapshot = storageSnapshot,
                    message = storageMessage,
                    onRefresh = { refreshStorage() },
                    onClearTemp = {
                        scope.launch {
                            val removed =
                                withContext(Dispatchers.IO) {
                                    FileUtil.clearTempFiles(FileUtil.getExternalTempDir())
                                }
                            storageMessage = "Cleared $removed temporary file${if (removed == 1) "" else "s"}."
                            refreshStorage()
                        }
                    },
                )
            }

            item {
                PreferenceSubtitle(text = "Gallery DL")
            }

            item {
                PreferenceSwitch(
                    title = "Confirm before downloading",
                    description =
                        "Analyze the Gallery DL URL and show the Seal-style review sheet before Download or Queue.",
                    icon = Icons.Outlined.ViewAgenda,
                    isChecked = galleryConfirmBeforeDownload,
                    onClick = {
                        GalleryDlBehaviorPreference.setConfirmBeforeDownload(!galleryConfirmBeforeDownload)
                    },
                )
            }

            item {
                PreferenceSubtitle(text = "Bilibili")
            }

            item {
                PreferenceItem(
                    title = "Bilibili Speed Mode",
                    description =
                        when (bilibiliSpeedMode) {
                            BILIBILI_SPEED_BALANCED ->
                                "Balanced • 4 fragments • steadier on weaker routes"
                            BILIBILI_SPEED_FAST ->
                                "Fast • 12 fragments • higher parallelism"
                            BILIBILI_SPEED_CUSTOM ->
                                "Custom • $bilibiliCustomFragments fragments"
                            else ->
                                "Auto • 8 fragments • recommended"
                        } + " • Bilibili only",
                    icon = Icons.Rounded.NetworkCheck,
                    onClick = { showBilibiliSpeedDialog = true },
                )
            }

            if (bilibiliSpeedMode == BILIBILI_SPEED_CUSTOM) {
                item {
                    PreferenceItem(
                        title = "Bilibili concurrent fragments",
                        description =
                            "$bilibiliCustomFragments fragment${if (bilibiliCustomFragments == 1) "" else "s"} • " +
                                "Aria2 profile cap follows this value; the global Aria2 limit still applies",
                        icon = Icons.Outlined.SignalCellular4Bar,
                        onClick = { showBilibiliFragmentsDialog = true },
                    )
                }
            }

            item {
                PreferenceSubtitle(text = stringResource(R.string.format_selection_layout))
            }

            item {
                PreferenceSwitch(
                    title = stringResource(R.string.format_list_view_title),
                    description = stringResource(R.string.format_list_view_desc),
                    icon = Icons.Outlined.ViewAgenda,
                    isChecked = formatListView,
                    onClick = {
                        FORMAT_LIST_VIEW.updateBoolean(!formatListView)
                        formatListView = !formatListView
                    }
                )
            }

            item {
                PreferenceSubtitle(text = "Home experience")
            }
            item {
                PreferenceItem(
                    title = "Recent downloads on Home",
                    description = "Show the latest $homeRecentLimit completed items",
                    icon = Icons.Outlined.ViewAgenda,
                    onClick = { showHomeRecentDialog = true },
                )
            }
            item {
                PreferenceSwitch(
                    title = "Detailed transfer line",
                    description = "Show live speed and ETA under active Home download cards",
                    icon = Icons.Rounded.NetworkCheck,
                    isChecked = homeTransferDetails,
                    onClick = {
                        homeTransferDetails = !homeTransferDetails
                        HOME_TRANSFER_DETAILS.updateBoolean(homeTransferDetails)
                    },
                )
            }
            item {
                PreferenceSwitch(
                    title = "Animated URL hints",
                    description = "Typewriter + gradient hint inside Media and Gallery URL fields",
                    icon = Icons.Outlined.ViewAgenda,
                    isChecked = homeInputAnimation,
                    onClick = {
                        homeInputAnimation = !homeInputAnimation
                        HOME_INPUT_ANIMATION.updateBoolean(homeInputAnimation)
                    },
                )
            }
            item {
                PreferenceSwitch(
                    title = "Home quick tools",
                    description = "Show the icon shortcut row above the URL inputs",
                    icon = Icons.Outlined.ViewAgenda,
                    isChecked = homeQuickTools,
                    onClick = {
                        homeQuickTools = !homeQuickTools
                        HOME_QUICK_TOOLS.updateBoolean(homeQuickTools)
                    },
                )
            }
            item {
                PreferenceSwitch(
                    title = "Home activity statistics",
                    description = "Show Media and Gallery Active/Queue/Done dashboard cards",
                    icon = Icons.Outlined.ViewAgenda,
                    isChecked = homeShowActivity,
                    onClick = {
                        homeShowActivity = !homeShowActivity
                        HOME_SHOW_ACTIVITY.updateBoolean(homeShowActivity)
                    },
                )
            }
            item {
                PreferenceSwitch(
                    title = "Compact activity cards",
                    description = "Use shorter Home statistics cards while keeping all counters",
                    icon = Icons.Outlined.ViewAgenda,
                    isChecked = homeCompactActivity,
                    enabled = homeShowActivity,
                    onClick = {
                        homeCompactActivity = !homeCompactActivity
                        HOME_COMPACT_ACTIVITY.updateBoolean(homeCompactActivity)
                    },
                )
            }

            item {
                PreferenceSubtitle(text = stringResource(R.string.security_and_privacy))
            }
            
            item {
                PreferenceItem(
                    title = stringResource(R.string.app_lock),
                    description = stringResource(R.string.lock_app_with_pin_biometric),
                    icon = Icons.Outlined.Lock,
                    onClick = {
                        // Check if AppLock is enabled and PIN is set
                        if (AuthenticationManager.isSecurityEnabled() && AuthenticationManager.isPinSet()) {
                            // Show authentication screen before allowing access
                            showAuthScreen = true
                        } else {
                            // AppLock not enabled, go directly to settings
                            onNavigateToSecurity()
                        }
                    }
                )
            }

            item {
                PreferenceItem(
                    title = stringResource(R.string.hidden_content),
                    description = stringResource(R.string.hidden_content_desc),
                    icon = Icons.Outlined.VisibilityOff,
                    onClick = {
                        // Hidden Content requires App Lock to be enabled
                        if (AuthenticationManager.isSecurityEnabled() && AuthenticationManager.isPinSet()) {
                            hiddenContentAuthDone = false
                            showHiddenContentAuthScreen = true
                        } else {
                            // App Lock not set up — cannot access hidden content
                            context.makeToast(R.string.hidden_content_requires_app_lock)
                        }
                    }
                )
            }
            
            item {
                PreferenceItem(
                    title = stringResource(R.string.network_type_restriction),
                    description = when (networkTypeRestriction) {
                        NETWORK_WIFI_ONLY -> stringResource(R.string.wifi_only)
                        NETWORK_MOBILE_ONLY -> stringResource(R.string.mobile_only)
                        else -> stringResource(R.string.any_network)
                    },
                    icon = when (networkTypeRestriction) {
                        NETWORK_WIFI_ONLY -> Icons.Outlined.SignalWifi4Bar
                        NETWORK_MOBILE_ONLY -> Icons.Outlined.SignalCellular4Bar
                        else -> Icons.Rounded.NetworkCheck
                    },
                    onClick = { showNetworkDialog = true }
                )
            }

            item {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.network_pause_delay_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text =
                                    stringResource(
                                        R.string.network_pause_delay_desc,
                                        networkPauseDelaySeconds,
                                    ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = networkPauseDelaySeconds.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    androidx.compose.material3.Slider(
                        value = networkPauseDelaySeconds.toFloat(),
                        onValueChange = { newValue ->
                            networkPauseDelaySeconds = newValue.roundToInt().coerceIn(5, 120)
                        },
                        onValueChangeFinished = {
                            NETWORK_PAUSE_DELAY_SECONDS.updateInt(networkPauseDelaySeconds)
                        },
                        valueRange = 5f..120f,
                        steps = 114,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "5",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "120",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            
            item {
                PreferenceSubtitle(text = stringResource(R.string.notification_settings))
            }
            
            item {
                PreferenceSwitch(
                    title = stringResource(R.string.notification_sound_settings),
                    description = stringResource(R.string.notification_sound_desc),
                    icon = Icons.Outlined.Notifications,
                    isChecked = notificationSound,
                    onClick = { 
                        NOTIFICATION_SOUND.updateBoolean(!notificationSound)
                        notificationSound = !notificationSound
                    }
                )
            }
            
            item {
                PreferenceSwitch(
                    title = stringResource(R.string.notification_vibrate_settings),
                    description = stringResource(R.string.notification_vibrate_desc),
                    icon = Icons.Outlined.Notifications,
                    isChecked = notificationVibrate,
                    enabled = notificationSound,
                    onClick = { 
                        NOTIFICATION_VIBRATE.updateBoolean(!notificationVibrate)
                        notificationVibrate = !notificationVibrate
                    }
                )
            }
            
            item {
                PreferenceSwitch(
                    title = stringResource(R.string.notification_led_settings),
                    description = stringResource(R.string.notification_led_desc),
                    icon = Icons.Outlined.Notifications,
                    isChecked = notificationLed,
                    enabled = notificationSound,
                    onClick = { 
                        NOTIFICATION_LED.updateBoolean(!notificationLed)
                        notificationLed = !notificationLed
                    }
                )
            }
            
            item {
                PreferenceSwitch(
                    title = stringResource(R.string.notification_success_sound_settings),
                    description = stringResource(R.string.notification_success_sound_desc),
                    icon = Icons.Outlined.Notifications,
                    isChecked = notificationSuccessSound,
                    enabled = notificationSound,
                    onClick = { 
                        NOTIFICATION_SUCCESS_SOUND.updateBoolean(!notificationSuccessSound)
                        notificationSuccessSound = !notificationSuccessSound
                    }
                )
            }
            
            item {
                PreferenceSwitch(
                    title = stringResource(R.string.notification_error_sound_settings),
                    description = stringResource(R.string.notification_error_sound_desc),
                    icon = Icons.Outlined.Notifications,
                    isChecked = notificationErrorSound,
                    enabled = notificationSound,
                    onClick = { 
                        NOTIFICATION_ERROR_SOUND.updateBoolean(!notificationErrorSound)
                        notificationErrorSound = !notificationErrorSound
                    }
                )
            }

            item {
                PreferenceSubtitle(text = stringResource(R.string.sponsor_support_section))
            }

            item {
                PreferenceItem(
                    title = stringResource(R.string.sponsor_dialog_frequency_title),
                    description = when (sponsorDialogFrequency) {
                        SPONSOR_FREQ_OFF -> stringResource(R.string.sponsor_dialog_off)
                        SPONSOR_FREQ_MONTHLY -> stringResource(R.string.sponsor_dialog_monthly)
                        else -> stringResource(R.string.sponsor_dialog_weekly)
                    },
                    icon = Icons.Outlined.VolunteerActivism,
                    onClick = { showSponsorFrequencyDialog = true },
                )
            }
        }

        if (showBilibiliSpeedDialog) {
            BilibiliSpeedModeDialog(
                currentSelection = bilibiliSpeedMode,
                onDismissRequest = { showBilibiliSpeedDialog = false },
                onConfirm = { selected ->
                    BILIBILI_SPEED_MODE.updateInt(selected)
                    bilibiliSpeedMode = selected
                    showBilibiliSpeedDialog = false
                },
            )
        }

        if (showBilibiliFragmentsDialog) {
            BilibiliFragmentsDialog(
                currentSelection = bilibiliCustomFragments,
                options = bilibiliFragmentOptions,
                onDismissRequest = { showBilibiliFragmentsDialog = false },
                onConfirm = { selected ->
                    BILIBILI_CUSTOM_FRAGMENTS.updateInt(selected)
                    bilibiliCustomFragments = selected
                    showBilibiliFragmentsDialog = false
                },
            )
        }

        if (showHomeRecentDialog) {
            HomeRecentLimitDialog(
                currentSelection = homeRecentLimit,
                onDismissRequest = { showHomeRecentDialog = false },
                onConfirm = { selected ->
                    homeRecentLimit = selected
                    HOME_RECENT_LIMIT.updateInt(selected)
                    showHomeRecentDialog = false
                },
            )
        }

        if (showSponsorFrequencyDialog) {
            SponsorFrequencyDialog(
                currentSelection = sponsorDialogFrequency,
                onDismissRequest = { showSponsorFrequencyDialog = false },
                onConfirm = { selected ->
                    SPONSOR_DIALOG_FREQUENCY.updateInt(selected)
                    sponsorDialogFrequency = selected
                    showSponsorFrequencyDialog = false
                },
            )
        }

        if (showNetworkDialog) {
            NetworkTypeDialog(
                currentSelection = networkTypeRestriction,
                onDismissRequest = { showNetworkDialog = false },
                onConfirm = { selectedType ->
                    NETWORK_TYPE_RESTRICTION.updateInt(selectedType)
                    networkTypeRestriction = selectedType
                    showNetworkDialog = false
                }
            )
        }
    }
}

private data class StorageSnapshot(
    val kirinRootBytes: Long = 0L,
    val galleryBytes: Long = 0L,
    val tempBytes: Long = 0L,
    val freeBytes: Long = 0L,
)

private fun measureStorage(): StorageSnapshot {
    val root = FileUtil.getExternalDownloadDirectory()
    val galleryConfigured = GALLERY_DL_DIRECTORY.getString().trim()
    val gallery =
        if (galleryConfigured.isNotBlank()) File(galleryConfigured)
        else File(root, "GalleryDL")
    val temp = FileUtil.getExternalTempDir()
    return StorageSnapshot(
        kirinRootBytes = directorySize(root),
        galleryBytes = directorySize(gallery),
        tempBytes = directorySize(temp),
        freeBytes = root.usableSpace.coerceAtLeast(0L),
    )
}

private fun directorySize(root: File): Long {
    if (!root.exists()) return 0L
    return runCatching {
            root.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        }
        .getOrDefault(0L)
}

private fun readableBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024.0 && unit < units.lastIndex) {
        value /= 1024.0
        unit++
    }
    return if (unit == 0) "${value.toLong()} ${units[unit]}"
    else "%.1f %s".format(value, units[unit])
}

@Composable
private fun StorageManagerCard(
    snapshot: StorageSnapshot,
    message: String?,
    onRefresh: () -> Unit,
    onClearTemp: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Storage, contentDescription = null)
                Text(
                    "  Download storage",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text("KirinDL root • ${readableBytes(snapshot.kirinRootBytes)}")
            Text("Gallery DL • ${readableBytes(snapshot.galleryBytes)}")
            Text("Temporary files • ${readableBytes(snapshot.tempBytes)}")
            Text(
                "Free storage • ${readableBytes(snapshot.freeBytes)}",
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onRefresh, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null)
                    Text(" Refresh")
                }
                Button(onClick = onClearTemp, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Delete, contentDescription = null)
                    Text(" Clear temp")
                }
            }
            Text(
                "Clear temp only removes temporary downloader files; completed media is untouched.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            message?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun BilibiliSpeedModeDialog(
    currentSelection: Int,
    onDismissRequest: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var selected by remember { mutableStateOf(currentSelection) }
    val modes =
        listOf(
            Triple(BILIBILI_SPEED_AUTO, "Auto", "8 fragments • recommended • adaptive safe default"),
            Triple(BILIBILI_SPEED_BALANCED, "Balanced", "4 fragments • steadier on weaker routes"),
            Triple(BILIBILI_SPEED_FAST, "Fast", "12 fragments • higher parallelism"),
            Triple(BILIBILI_SPEED_CUSTOM, "Custom", "Choose 1 / 4 / 8 / 12 / 16 fragments"),
        )

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(
                    Icons.Rounded.NetworkCheck,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(12.dp).size(28.dp),
                )
            }
        },
        title = { Text("Bilibili Speed Mode", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text =
                        "Bilibili/b23.tv only. Other sites keep the normal Network settings. " +
                            "Aria2 caps follow the selected profile without changing your global limit.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                modes.forEach { (value, title, description) ->
                    val active = selected == value
                    Surface(
                        onClick = { selected = value },
                        shape = RoundedCornerShape(14.dp),
                        color =
                            if (active) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        tonalElevation = if (active) 2.dp else 0.dp,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    if (value == BILIBILI_SPEED_AUTO) {
                                        Text(
                                            "  RECOMMENDED",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                                Text(
                                    description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            androidx.compose.material3.RadioButton(
                                selected = active,
                                onClick = { selected = value },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { onConfirm(selected) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismissRequest) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun BilibiliFragmentsDialog(
    currentSelection: Int,
    options: List<Int>,
    onDismissRequest: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var selected by remember { mutableStateOf(currentSelection) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Bilibili concurrent fragments") },
        text = {
            Column {
                Text(
                    text =
                        "Higher values request more fragments in parallel. " +
                            "This does not change the global setting for other sites.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                options.forEach { value ->
                    PreferenceSingleChoiceItem(
                        text = "$value fragment${if (value == 1) "" else "s"}",
                        selected = selected == value,
                        containerColor = Color.Transparent,
                        onClick = { selected = value },
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { onConfirm(selected) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismissRequest) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun HomeRecentLimitDialog(
    currentSelection: Int,
    onDismissRequest: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var selected by remember { mutableStateOf(currentSelection) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Recent downloads on Home") },
        text = {
            Column {
                listOf(3, 5, 8, 10).forEach { value ->
                    PreferenceSingleChoiceItem(
                        text = "$value items",
                        selected = selected == value,
                        containerColor = Color.Transparent,
                        onClick = { selected = value },
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { onConfirm(selected) }) { Text("Apply") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismissRequest) { Text("Cancel") }
        },
    )
}

@Composable
private fun NetworkTypeDialog(
    currentSelection: Int,
    onDismissRequest: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedType by remember { mutableStateOf(currentSelection) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.network_type_restriction)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.network_type_restriction_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                PreferenceSingleChoiceItem(
                    text = stringResource(R.string.any_network),
                    selected = selectedType == NETWORK_ANY,
                    containerColor = Color.Transparent,
                    onClick = { selectedType = NETWORK_ANY }
                )
                
                PreferenceSingleChoiceItem(
                    text = stringResource(R.string.wifi_only),
                    selected = selectedType == NETWORK_WIFI_ONLY,
                    containerColor = Color.Transparent,
                    onClick = { selectedType = NETWORK_WIFI_ONLY }
                )
                
                PreferenceSingleChoiceItem(
                    text = stringResource(R.string.mobile_only),
                    selected = selectedType == NETWORK_MOBILE_ONLY,
                    containerColor = Color.Transparent,
                    onClick = { selectedType = NETWORK_MOBILE_ONLY }
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { onConfirm(selectedType) }
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(
                onClick = onDismissRequest
            ) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}
@Composable
private fun SponsorFrequencyDialog(
    currentSelection: Int,
    onDismissRequest: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var selected by remember { mutableStateOf(currentSelection) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Outlined.VolunteerActivism, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(stringResource(R.string.sponsor_dialog_frequency_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.sponsor_dialog_frequency_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                PreferenceSingleChoiceItem(
                    text = stringResource(R.string.sponsor_dialog_off),
                    selected = selected == SPONSOR_FREQ_OFF,
                    containerColor = Color.Transparent,
                    onClick = { selected = SPONSOR_FREQ_OFF },
                )
                PreferenceSingleChoiceItem(
                    text = stringResource(R.string.sponsor_dialog_weekly),
                    selected = selected == SPONSOR_FREQ_WEEKLY,
                    containerColor = Color.Transparent,
                    onClick = { selected = SPONSOR_FREQ_WEEKLY },
                )
                PreferenceSingleChoiceItem(
                    text = stringResource(R.string.sponsor_dialog_monthly),
                    selected = selected == SPONSOR_FREQ_MONTHLY,
                    containerColor = Color.Transparent,
                    onClick = { selected = SPONSOR_FREQ_MONTHLY },
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { onConfirm(selected) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismissRequest) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}