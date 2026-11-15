// DISABLED TEMPORARILY: Startup update popup
// Entire original source is preserved below as line comments.
// Remove the leading // markers to restore this popup.

// package com.junkfood.seal.ui.page

// import android.content.Context
// import androidx.compose.foundation.layout.Column
// import androidx.compose.foundation.layout.Row
// import androidx.compose.foundation.layout.Spacer
// import androidx.compose.foundation.layout.fillMaxWidth
// import androidx.compose.foundation.layout.height
// import androidx.compose.foundation.layout.padding
// import androidx.compose.foundation.layout.size
// import androidx.compose.foundation.shape.CircleShape
// import androidx.compose.foundation.shape.RoundedCornerShape
// import androidx.compose.material.icons.Icons
// import androidx.compose.material.icons.outlined.Close
// import androidx.compose.material.icons.outlined.NewReleases
// import androidx.compose.material3.AlertDialog
// import androidx.compose.material3.Card
// import androidx.compose.material3.CardDefaults
// import androidx.compose.material3.Icon
// import androidx.compose.material3.IconButton
// import androidx.compose.material3.MaterialTheme
// import androidx.compose.material3.Surface
// import androidx.compose.material3.Text
// import androidx.compose.material3.TextButton
// import androidx.compose.runtime.Composable
// import androidx.compose.runtime.LaunchedEffect
// import androidx.compose.runtime.getValue
// import androidx.compose.runtime.mutableStateOf
// import androidx.compose.runtime.remember
// import androidx.compose.runtime.setValue
// import androidx.compose.ui.Alignment
// import androidx.compose.ui.Modifier
// import androidx.compose.ui.platform.LocalContext
// import androidx.compose.ui.text.font.FontWeight
// import androidx.compose.ui.unit.dp
// import com.junkfood.seal.util.DEFAULT_INTERVAL
// import com.junkfood.seal.util.PreferenceUtil
// import com.junkfood.seal.util.UpdateUtil

// private const val UPDATE_POPUP_PREFERENCES = "kirin_update_popup"
// private const val KEY_DISMISSED_RELEASE = "dismissed_release"
// private const val ENABLE_STARTUP_UPDATE_POPUP = false

// /**
//  * Checks for a KirinDL release when the main app opens and presents an Updates inbox card.
//  * Tapping the card opens the existing release-details dialog. The About update page remains an
//  * independent manual entry point and is intentionally not coupled to this startup popup.
//  *
//  * Dismissal is remembered per release tag, so a dismissed update does not interrupt every launch.
//  */
// @Composable
// fun StartupUpdatePopup() {
//     // Popup startup dimatikan sementara; kod asal dikekalkan di bawah.
//     if (!ENABLE_STARTUP_UPDATE_POPUP) return
//     val context = LocalContext.current
//     val popupPreferences = remember(context) {
//         context.getSharedPreferences(UPDATE_POPUP_PREFERENCES, Context.MODE_PRIVATE)
//     }

//     var releaseToShow by remember { mutableStateOf<UpdateUtil.Release?>(null) }
//     var showPopup by remember { mutableStateOf(false) }
//     var showDetails by remember { mutableStateOf(false) }

//     LaunchedEffect(context) {
//         // The existing App Update setting controls automatic checks. Notifications are separate.
//         if (!PreferenceUtil.isAutoUpdateEnabled()) return@LaunchedEffect

//         val cachedRelease = UpdateUtil.restoreCachedAvailableUpdate(context)
//         val candidate = if (
//             UpdateUtil.shouldRunAutomaticUpdateCheck(
//                 context = context,
//                 intervalMs = DEFAULT_INTERVAL,
//             )
//         ) {
//             // Keep a previously resolved update available when the network check fails.
//             UpdateUtil.checkForUpdateResult(context).getOrElse { cachedRelease }
//         } else {
//             cachedRelease
//         }

//         val releaseKey = candidate?.let(::releaseKey) ?: return@LaunchedEffect
//         val dismissedRelease = popupPreferences.getString(KEY_DISMISSED_RELEASE, null)
//         if (releaseKey != dismissedRelease) {
//             releaseToShow = candidate
//             showPopup = true
//         }
//     }

//     val release = releaseToShow
//     val dismissRelease: () -> Unit = {
//         showPopup = false
//         showDetails = false
//         release?.let { target ->
//             releaseKey(target)?.let { key ->
//                 popupPreferences.edit()
//                     .putString(KEY_DISMISSED_RELEASE, key)
//                     .apply()
//             }
//         }
//     }

//     if (showPopup && release != null) {
//         val releaseLabel = release.tagName?.takeIf { it.isNotBlank() }
//             ?: release.name?.takeIf { it.isNotBlank() }
//             ?: "New release"
//         val cardTitle = if (releaseLabel.startsWith("KirinDL", ignoreCase = true)) {
//             releaseLabel
//         } else {
//             "KirinDL $releaseLabel"
//         }

//         AlertDialog(
//             onDismissRequest = dismissRelease,
//             title = {
//                 Column {
//                     Text(
//                         text = "Updates",
//                         style = MaterialTheme.typography.headlineSmall,
//                         fontWeight = FontWeight.Bold,
//                     )
//                     Spacer(modifier = Modifier.height(2.dp))
//                     Text(
//                         text = "1 update available",
//                         style = MaterialTheme.typography.bodyMedium,
//                         color = MaterialTheme.colorScheme.onSurfaceVariant,
//                     )
//                 }
//             },
//             text = {
//                 Card(
//                     onClick = {
//                         showPopup = false
//                         showDetails = true
//                     },
//                     modifier = Modifier.fillMaxWidth(),
//                     shape = RoundedCornerShape(20.dp),
//                     colors = CardDefaults.cardColors(
//                         containerColor = MaterialTheme.colorScheme.primaryContainer,
//                     ),
//                 ) {
//                     Row(
//                         modifier = Modifier.fillMaxWidth().padding(
//                             start = 14.dp,
//                             top = 14.dp,
//                             end = 6.dp,
//                             bottom = 14.dp,
//                         ),
//                         verticalAlignment = Alignment.CenterVertically,
//                     ) {
//                         Surface(
//                             modifier = Modifier.size(42.dp),
//                             shape = CircleShape,
//                             color = MaterialTheme.colorScheme.primary,
//                         ) {
//                             Icon(
//                                 imageVector = Icons.Outlined.NewReleases,
//                                 contentDescription = null,
//                                 modifier = Modifier.padding(10.dp),
//                                 tint = MaterialTheme.colorScheme.onPrimary,
//                             )
//                         }
//                         Column(
//                             modifier = Modifier.weight(1f).padding(start = 12.dp),
//                         ) {
//                             Text(
//                                 text = cardTitle,
//                                 style = MaterialTheme.typography.titleMedium,
//                                 fontWeight = FontWeight.SemiBold,
//                                 color = MaterialTheme.colorScheme.onPrimaryContainer,
//                             )
//                             Spacer(modifier = Modifier.height(2.dp))
//                             Text(
//                                 text = "Tap to see changes and install",
//                                 style = MaterialTheme.typography.bodySmall,
//                                 color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
//                                     alpha = 0.8f,
//                                 ),
//                             )
//                         }
//                         IconButton(
//                             onClick = dismissRelease,
//                         ) {
//                             Icon(
//                                 imageVector = Icons.Outlined.Close,
//                                 contentDescription = "Dismiss update",
//                                 tint = MaterialTheme.colorScheme.onPrimaryContainer,
//                             )
//                         }
//                     }
//                 }
//             },
//             confirmButton = {
//                 TextButton(onClick = dismissRelease) {
//                     Text("Dismiss")
//                 }
//             },
//         )
//     }

//     if (showDetails && release != null) {
//         UpdateDialog(
//             onDismissRequest = dismissRelease,
//             release = release,
//             isUpdateAvailable = true,
//             useInAppUpdate = true,
//             onUpdateLaunched = {
//                 // Hide the dialog while Android's installer is in front. Do not mark the
//                 // release dismissed: a cancelled system install can be retried on next launch.
//                 showDetails = false
//             },
//         )
//     }
// }

// private fun releaseKey(release: UpdateUtil.Release): String? =
//     release.tagName?.takeIf { it.isNotBlank() }
//         ?: release.name?.takeIf { it.isNotBlank() }
