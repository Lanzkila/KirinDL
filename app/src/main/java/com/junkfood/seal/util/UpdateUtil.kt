package com.junkfood.seal.util

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.junkfood.seal.App
import com.junkfood.seal.App.Companion.context
import com.junkfood.seal.util.PreferenceUtil.getInt
import com.junkfood.seal.util.PreferenceUtil.getLong
import com.junkfood.seal.util.PreferenceUtil.updateLong
import com.yausername.youtubedl_android.YoutubeDL
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

object UpdateUtil {

    private const val OWNER = "Lanzkila"
    private const val REPO = "KirinDL"

    private const val APP_UPDATE_PREFS = "kirin_app_update_state"
    private const val KEY_PENDING_DOWNLOAD_ID = "pending_download_id"
    private const val KEY_PENDING_VERSION = "pending_version"
    private const val KEY_LAST_INSTALLED_VERSION = "last_installed_version"
    private const val KEY_LAST_CHECKED_VERSION = "last_checked_version"
    private const val KEY_LAST_CHECKED_CHANNEL = "last_checked_channel"
    private const val KEY_AVAILABLE_RELEASE_JSON = "available_release_json"
    private const val KEY_WAITING_INSTALL_PERMISSION = "waiting_install_permission"
    private const val NO_DOWNLOAD_ID = -1L
    private const val APK_MIME = "application/vnd.android.package-archive"
    private const val UPDATE_NOTIFICATION_CHANNEL = "kirin_app_updates"
    private const val UPDATE_NOTIFICATION_ID = 32001

    private const val YTDLP_STABLE_RELEASE =
        "https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest"
    private const val YTDLP_NIGHTLY_RELEASE =
        "https://api.github.com/repos/yt-dlp/yt-dlp-nightly-builds/releases/latest"

    private val jsonFormat = Json { ignoreUnknownKeys = true }

    private val _availableAppUpdate = MutableStateFlow<Release?>(null)
    val availableAppUpdate: StateFlow<Release?> = _availableAppUpdate.asStateFlow()

    private fun getClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    private val requestForReleases =
        Request.Builder()
            .url("https://api.github.com/repos/${OWNER}/${REPO}/releases")
            .build()

    suspend fun updateYtDlp(): YoutubeDL.UpdateStatus? =
        withContext(Dispatchers.IO) {
            val channel =
                when (YT_DLP_UPDATE_CHANNEL.getInt()) {
                    YT_DLP_NIGHTLY -> YoutubeDL.UpdateChannel.NIGHTLY
                    else -> YoutubeDL.UpdateChannel.STABLE
                }

            YoutubeDL.getInstance()
                .updateYoutubeDL(appContext = context, updateChannel = channel)
                .also { status ->
                    if (status == YoutubeDL.UpdateStatus.DONE) {
                        YoutubeDL.getInstance().version(context)?.takeIf { it.isNotBlank() }?.let {
                            version -> PreferenceUtil.encodeString(YT_DLP_VERSION, version)
                        }
                    }
                    YT_DLP_UPDATE_TIME.updateLong(System.currentTimeMillis())
                }
        }

    suspend fun checkLatestYtDlpVersion(): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoint =
                    when (YT_DLP_UPDATE_CHANNEL.getInt()) {
                        YT_DLP_NIGHTLY -> YTDLP_NIGHTLY_RELEASE
                        else -> YTDLP_STABLE_RELEASE
                    }

                val request =
                    Request.Builder()
                        .url(endpoint)
                        .header("Accept", "application/vnd.github+json")
                        .header("User-Agent", "KirinDL yt-dlp update checker")
                        .build()

                val release =
                    getClient().newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            throw IOException("yt-dlp release check failed (HTTP ${response.code})")
                        }
                        val body =
                            response.body?.string()?.takeIf { it.isNotBlank() }
                                ?: throw IOException("yt-dlp release check returned an empty response")
                        jsonFormat.decodeFromString<Release>(body)
                    }

                release.tagName?.takeIf { it.isNotBlank() }
                    ?: release.name?.takeIf { it.isNotBlank() }
                    ?: throw IOException("yt-dlp release did not include a version tag")
            }
        }

    private fun getReleaseList(): List<Release> =
        getClient().newCall(requestForReleases).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("KirinDL releases request failed (HTTP ${response.code})")
            }
            val body =
                response.body?.string()?.takeIf { it.isNotBlank() }
                    ?: throw IOException("Empty response body from releases API")
            jsonFormat.decodeFromString<List<Release>>(body)
        }

    private fun getLatestRelease(): Release {
        val stableChannel = UPDATE_CHANNEL.getInt() == STABLE
        return getReleaseList()
            .asSequence()
            .filter { it.draft != true }
            .filter { release ->
                // Stable users never receive GitHub pre-releases. Pre-release testers can see
                // both channels and simply take the highest valid KirinDL version.
                !stableChannel || release.preRelease != true
            }
            .filter { (it.tagName ?: it.name).toVersionOrNull() != null }
            .maxByOrNull { (it.tagName ?: it.name).toVersionOrNull()!! }
            ?: throw IOException(
                if (stableChannel) "No stable KirinDL release found"
                else "No prerelease KirinDL build found"
            )
    }

    /**
     * Synchronize the installed app version with updater state.
     *
     * Android keeps SharedPreferences across an in-place APK update. Without this reset, the
     * automatic checker can inherit the previous version's cooldown and incorrectly wait up to
     * two days before checking again. A changed installed version clears that cooldown
     * immediately, so the newly installed KirinDL build becomes the new update baseline.
     */
    fun syncInstalledVersionState(context: Context = App.context): Boolean {
        val installed = context.getCurrentVersionName()
        val prefs = context.getSharedPreferences(APP_UPDATE_PREFS, Context.MODE_PRIVATE)
        val previous = prefs.getString(KEY_LAST_INSTALLED_VERSION, null)
        if (previous == installed) return false

        prefs.edit()
            .putString(KEY_LAST_INSTALLED_VERSION, installed)
            .remove(KEY_LAST_CHECKED_VERSION)
            .remove(KEY_LAST_CHECKED_CHANNEL)
            .remove(KEY_PENDING_DOWNLOAD_ID)
            .remove(KEY_PENDING_VERSION)
            .remove(KEY_AVAILABLE_RELEASE_JSON)
            .putBoolean(KEY_WAITING_INSTALL_PERMISSION, false)
            .apply()
        APP_UPDATE_CHECK_TIME.updateLong(0L)
        _availableAppUpdate.value = null
        NotificationUtil.cancelAppUpdateAvailableNotification(context)
        return previous != null
    }

    /**
     * Kept for the existing App startup hook. Version-state cleanup now replaces the legacy APK
     * cache cleanup that KirinDL no longer needs.
     */
    suspend fun deleteOutdatedApk() {
        syncInstalledVersionState(App.context)
    }

    /**
     * Starts the APK download through Android's DownloadManager. This is deliberately separate
     * from the normal media downloader: Android owns the background transfer and its notification,
     * while KirinDL keeps app installation manual.
     */
    fun enqueueBackgroundAppUpdate(
        context: Context = App.context,
        release: Release,
    ): Result<Long> =
        runCatching {
            val asset = preferredApkAsset(release)
                ?: throw IOException("No downloadable KirinDL APK was attached to this release")
            val downloadUrl =
                asset.browserDownloadUrl?.takeIf { it.startsWith("https://") }
                    ?: throw IOException("The KirinDL APK download URL is unavailable")
            val rawVersion = release.tagName ?: release.name ?: "update"
            val safeVersion = rawVersion.removePrefix("v").replace(Regex("[^A-Za-z0-9._-]"), "-")
            val fileName =
                asset.name?.takeIf { it.endsWith(".apk", ignoreCase = true) }
                    ?: "KirinDL-$safeVersion-universal.apk"

            val request =
                DownloadManager.Request(Uri.parse(downloadUrl))
                    .setTitle("KirinDL $rawVersion")
                    .setDescription("Downloading app update in background")
                    .setMimeType("application/vnd.android.package-archive")
                    .setAllowedOverRoaming(false)
                    .setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                    )
                    .setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        "KirinDL/Updates/$fileName",
                    )

            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = manager.enqueue(request)
            context.getSharedPreferences(APP_UPDATE_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_PENDING_DOWNLOAD_ID, downloadId)
                .putString(KEY_PENDING_VERSION, rawVersion.removePrefix("v"))
                .putBoolean(KEY_WAITING_INSTALL_PERMISSION, false)
                .apply()
            downloadId
        }

    fun isPendingAppUpdateDownload(
        context: Context,
        downloadId: Long,
    ): Boolean =
        context.getSharedPreferences(APP_UPDATE_PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_PENDING_DOWNLOAD_ID, NO_DOWNLOAD_ID) == downloadId

    /**
     * Called from the DownloadManager completion receiver. A successful KirinDL APK download is
     * handed directly to Android's package installer. Android still owns the final confirmation;
     * with the same release signature this is an in-place update/overwrite, not a second app.
     */
    fun handleCompletedAppUpdateDownload(
        context: Context,
        downloadId: Long,
    ): Result<Boolean> =
        runCatching {
            if (!isPendingAppUpdateDownload(context, downloadId)) return@runCatching false

            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val status =
                manager.query(DownloadManager.Query().setFilterById(downloadId))?.use { cursor ->
                    if (!cursor.moveToFirst()) null
                    else {
                        val index = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        if (index >= 0) cursor.getInt(index) else null
                    }
                }

            when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    showInstallReadyNotification(context, downloadId)
                    openDownloadedAppUpdateInstaller(context, downloadId)
                }
                DownloadManager.STATUS_FAILED -> {
                    clearPendingAppUpdate(context)
                    false
                }
                else -> false
            }
        }

    /**
     * If Android required the user to enable "Install unknown apps", MainActivity calls this on
     * resume so the installer continues automatically after permission is granted.
     */
    fun resumePendingAppUpdateInstall(context: Context): Result<Boolean> =
        runCatching {
            val prefs = context.getSharedPreferences(APP_UPDATE_PREFS, Context.MODE_PRIVATE)
            if (!prefs.getBoolean(KEY_WAITING_INSTALL_PERMISSION, false)) {
                return@runCatching false
            }
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    !context.packageManager.canRequestPackageInstalls()
            ) {
                return@runCatching false
            }
            val downloadId = prefs.getLong(KEY_PENDING_DOWNLOAD_ID, NO_DOWNLOAD_ID)
            if (downloadId == NO_DOWNLOAD_ID) return@runCatching false
            prefs.edit().putBoolean(KEY_WAITING_INSTALL_PERMISSION, false).apply()
            openDownloadedAppUpdateInstaller(context, downloadId)
        }

    private fun openDownloadedAppUpdateInstaller(
        context: Context,
        downloadId: Long,
    ): Boolean {
        val prefs = context.getSharedPreferences(APP_UPDATE_PREFS, Context.MODE_PRIVATE)

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !context.packageManager.canRequestPackageInstalls()
        ) {
            prefs.edit().putBoolean(KEY_WAITING_INSTALL_PERMISSION, true).apply()
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return true
        }

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val apkUri =
            manager.getUriForDownloadedFile(downloadId)
                ?: throw IOException("Downloaded KirinDL APK is unavailable")

        val installIntent =
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(apkUri, APK_MIME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        context.startActivity(installIntent)
        return true
    }

    private fun showInstallReadyNotification(
        context: Context,
        downloadId: Long,
    ) {
        runCatching {
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val apkUri = manager.getUriForDownloadedFile(downloadId) ?: return@runCatching
            val installIntent =
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(apkUri, APK_MIME)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            val pendingIntent =
                PendingIntent.getActivity(
                    context,
                    UPDATE_NOTIFICATION_ID,
                    installIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager.createNotificationChannel(
                    NotificationChannel(
                        UPDATE_NOTIFICATION_CHANNEL,
                        "KirinDL app updates",
                        NotificationManager.IMPORTANCE_HIGH,
                    ).apply {
                        description = "Install downloaded KirinDL application updates"
                    }
                )
            }

            val notification =
                NotificationCompat.Builder(context, UPDATE_NOTIFICATION_CHANNEL)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setContentTitle("KirinDL update ready")
                    .setContentText("Tap to install and update KirinDL")
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build()

            notificationManager.notify(UPDATE_NOTIFICATION_ID, notification)
        }
    }

    private fun clearPendingAppUpdate(context: Context) {
        context.getSharedPreferences(APP_UPDATE_PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PENDING_DOWNLOAD_ID)
            .remove(KEY_PENDING_VERSION)
            .putBoolean(KEY_WAITING_INSTALL_PERMISSION, false)
            .apply()
    }

    fun hasBackgroundAppUpdate(release: Release): Boolean = preferredApkAsset(release) != null

    private fun preferredApkAsset(release: Release): ReleaseAsset? =
        release.assets
            .asSequence()
            .filter { asset ->
                asset.name?.endsWith(".apk", ignoreCase = true) == true &&
                    asset.name.orEmpty().contains("universal", ignoreCase = true) &&
                    asset.browserDownloadUrl?.startsWith("https://") == true
            }
            .sortedByDescending {
                it.name.orEmpty().contains("release", ignoreCase = true)
            }
            .firstOrNull()

    fun shouldRunAutomaticUpdateCheck(
        context: Context = App.context,
        intervalMs: Long,
    ): Boolean {
        val prefs = context.getSharedPreferences(APP_UPDATE_PREFS, Context.MODE_PRIVATE)
        val installed = context.getCurrentVersionName()
        val channel = UPDATE_CHANNEL.getInt()
        val checkedVersion = prefs.getString(KEY_LAST_CHECKED_VERSION, null)
        val checkedChannel = prefs.getInt(KEY_LAST_CHECKED_CHANNEL, Int.MIN_VALUE)
        val lastChecked = APP_UPDATE_CHECK_TIME.getLong()
        val now = System.currentTimeMillis()

        if (checkedVersion != installed || checkedChannel != channel) return true
        if (lastChecked <= 0L || now < lastChecked) return true
        return now - lastChecked >= intervalMs
    }

    private fun recordSuccessfulAppUpdateCheck(context: Context) {
        context.getSharedPreferences(APP_UPDATE_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_CHECKED_VERSION, context.getCurrentVersionName())
            .putInt(KEY_LAST_CHECKED_CHANNEL, UPDATE_CHANNEL.getInt())
            .apply()
        APP_UPDATE_CHECK_TIME.updateLong(System.currentTimeMillis())
    }

    suspend fun checkForUpdateResult(context: Context = App.context): Result<Release?> =
        withContext(Dispatchers.IO) {
            runCatching {
                    val currentVersion = context.getCurrentVersion()
                    val latestRelease = getLatestRelease()
                    val latestVersion = (latestRelease.tagName ?: latestRelease.name).toVersion()
                    if (currentVersion < latestVersion) latestRelease else null
                }
                .onSuccess { candidate ->
                    _availableAppUpdate.value = candidate
                    val prefs = context.getSharedPreferences(APP_UPDATE_PREFS, Context.MODE_PRIVATE)
                    if (candidate != null) {
                        prefs.edit()
                            .putString(KEY_AVAILABLE_RELEASE_JSON, jsonFormat.encodeToString(candidate))
                            .apply()
                    } else {
                        prefs.edit().remove(KEY_AVAILABLE_RELEASE_JSON).apply()
                        NotificationUtil.cancelAppUpdateAvailableNotification(context)
                    }
                    recordSuccessfulAppUpdateCheck(context)
                }
        }

    suspend fun checkForUpdate(context: Context = App.context): Release? =
        checkForUpdateResult(context).getOrNull()
    fun restoreCachedAvailableUpdate(context: Context = App.context): Release? {
        _availableAppUpdate.value?.let { return it }

        val prefs = context.getSharedPreferences(APP_UPDATE_PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_AVAILABLE_RELEASE_JSON, null) ?: return null
        val cached = runCatching { jsonFormat.decodeFromString<Release>(raw) }.getOrNull()
        val cachedVersion = (cached?.tagName ?: cached?.name).toVersionOrNull()
        if (cached == null || cachedVersion == null || context.getCurrentVersion() >= cachedVersion) {
            prefs.edit().remove(KEY_AVAILABLE_RELEASE_JSON).apply()
            return null
        }

        _availableAppUpdate.value = cached
        return cached
    }


    suspend fun getCurrentReleaseResult(context: Context = App.context): Result<Release> =
        withContext(Dispatchers.IO) {
            runCatching {
                val currentVersion = context.getCurrentVersion()
                getReleaseList().firstOrNull { release ->
                    (release.tagName ?: release.name).toVersion().compareTo(currentVersion) == 0
                } ?: throw IOException("Current KirinDL release notes were not found")
            }
        }
    suspend fun getLatestStableReleaseResult(): Result<Release> =
        withContext(Dispatchers.IO) {
            runCatching {
                getReleaseList()
                    .asSequence()
                    .filter { it.draft != true && it.preRelease != true }
                    .filter { (it.tagName ?: it.name).toVersionOrNull() != null }
                    .maxByOrNull { (it.tagName ?: it.name).toVersionOrNull()!! }
                    ?: throw IOException("No Stable KirinDL release notes were found")
            }
        }

    suspend fun getLatestPreReleaseResult(): Result<Release> =
        withContext(Dispatchers.IO) {
            runCatching {
                getReleaseList()
                    .asSequence()
                    .filter { it.draft != true && it.preRelease == true }
                    .filter { (it.tagName ?: it.name).toVersionOrNull() != null }
                    .maxByOrNull { (it.tagName ?: it.name).toVersionOrNull()!! }
                    ?: throw IOException("No KirinDL Pre-release notes were found")
            }
        }


    fun installedVersionName(context: Context = App.context): String =
        context.getCurrentVersionName()

    private fun Context.getCurrentVersionName(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager
                .getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
                .versionName
                .orEmpty()
        } else {
            packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
        }

    private fun Context.getCurrentVersion(): Version = getCurrentVersionName().toVersion()

    @Serializable
    data class Release(
        @SerialName("html_url") val htmlUrl: String? = null,
        @SerialName("tag_name") val tagName: String? = null,
        val name: String? = null,
        val draft: Boolean? = null,
        @SerialName("prerelease") val preRelease: Boolean? = null,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("published_at") val publishedAt: String? = null,
        val body: String? = null,
        val assets: List<ReleaseAsset> = emptyList(),
    )

    @Serializable
    data class ReleaseAsset(
        val name: String? = null,
        @SerialName("browser_download_url") val browserDownloadUrl: String? = null,
        @SerialName("content_type") val contentType: String? = null,
        val size: Long? = null,
    )

    private val pattern =
        Pattern.compile(
            """v?(\d+)\.(\d+)\.(\d+)(?:\.(\d+))?(?:-([a-zA-Z]+)\.?(\d+)?)?""",
            Pattern.CASE_INSENSITIVE,
        )
    private val EMPTY_VERSION = Version.Stable()

    fun String?.toVersion(): Version = toVersionOrNull() ?: EMPTY_VERSION

    fun String?.toVersionOrNull(): Version? =
        this?.run {
            val matcher = pattern.matcher(this)
            if (matcher.find()) {
                val major = matcher.group(1)?.toInt() ?: 0
                val minor = matcher.group(2)?.toInt() ?: 0
                val patch = matcher.group(3)?.toInt() ?: 0
                val stableRevision = matcher.group(4)?.toInt() ?: 0
                val preReleaseBuild = matcher.group(6)?.toInt() ?: 0
                when (matcher.group(5)?.lowercase()) {
                    "alpha" -> Version.Alpha(major, minor, patch, preReleaseBuild)
                    "beta" -> Version.Beta(major, minor, patch, preReleaseBuild)
                    "rc" -> Version.ReleaseCandidate(major, minor, patch, preReleaseBuild)
                    "devpatch", "dev" -> Version.DevPatch(major, minor, patch, preReleaseBuild)
                    null, "", "stable" -> Version.Stable(major, minor, patch, stableRevision)
                    else -> Version.Stable(major, minor, patch, stableRevision)
                }
            } else {
                null
            }
        }

    sealed class Version(
        val major: Int,
        val minor: Int,
        val patch: Int,
        val build: Int = 0,
    ) : Comparable<Version> {
        companion object {
            private const val BUILD = 10L
            private const val VARIANT = 100L
            private const val PATCH = 10_000L
            private const val MINOR = 1_000_000L
            private const val MAJOR = 100_000_000L

            private const val STABLE = VARIANT * 5
            private const val ALPHA = VARIANT
            private const val BETA = VARIANT * 2
            private const val RELEASE_CANDIDATE = VARIANT * 3
            private const val DEVPATCH = VARIANT * 4
        }

        abstract fun toVersionName(): String
        abstract fun toNumber(): Long

        class Alpha(
            versionMajor: Int = 0,
            versionMinor: Int = 0,
            versionPatch: Int = 0,
            versionBuild: Int = 0,
        ) : Version(versionMajor, versionMinor, versionPatch, versionBuild) {
            override fun toVersionName(): String = "${major}.${minor}.${patch}-alpha.$build"

            override fun toNumber(): Long =
                major * MAJOR + minor * MINOR + patch * PATCH + build * BUILD + ALPHA
        }

        class Beta(
            versionMajor: Int,
            versionMinor: Int,
            versionPatch: Int,
            versionBuild: Int,
        ) : Version(versionMajor, versionMinor, versionPatch, versionBuild) {
            override fun toVersionName(): String = "${major}.${minor}.${patch}-beta.$build"

            override fun toNumber(): Long =
                major * MAJOR + minor * MINOR + patch * PATCH + build * BUILD + BETA
        }

        class ReleaseCandidate(
            versionMajor: Int,
            versionMinor: Int,
            versionPatch: Int,
            versionBuild: Int,
        ) : Version(versionMajor, versionMinor, versionPatch, versionBuild) {
            override fun toVersionName(): String = "${major}.${minor}.${patch}-rc.$build"

            override fun toNumber(): Long =
                major * MAJOR +
                    minor * MINOR +
                    patch * PATCH +
                    build * BUILD +
                    RELEASE_CANDIDATE
        }

        class DevPatch(
            versionMajor: Int,
            versionMinor: Int,
            versionPatch: Int,
            versionBuild: Int,
        ) : Version(versionMajor, versionMinor, versionPatch, versionBuild) {
            override fun toVersionName(): String = "${major}.${minor}.${patch}-devpatch$build"

            override fun toNumber(): Long =
                major * MAJOR + minor * MINOR + patch * PATCH + build * BUILD + DEVPATCH
        }

        class Stable(
            versionMajor: Int = 0,
            versionMinor: Int = 0,
            versionPatch: Int = 0,
            versionBuild: Int = 0,
        ) : Version(versionMajor, versionMinor, versionPatch, versionBuild) {
            override fun toVersionName(): String =
                if (build > 0) "${major}.${minor}.${patch}.${build}"
                else "${major}.${minor}.${patch}"

            override fun toNumber(): Long =
                major * MAJOR + minor * MINOR + patch * PATCH + build * BUILD + STABLE
        }

        private fun channelRank(): Int =
            when (this) {
                is Alpha -> 1
                is Beta -> 2
                is ReleaseCandidate -> 3
                is DevPatch -> 4
                is Stable -> 5
            }

        override operator fun compareTo(other: Version): Int {
            if (major != other.major) return major.compareTo(other.major)
            if (minor != other.minor) return minor.compareTo(other.minor)
            if (patch != other.patch) return patch.compareTo(other.patch)

            val rankComparison = channelRank().compareTo(other.channelRank())
            if (rankComparison != 0) return rankComparison
            return build.compareTo(other.build)
        }
    }
}
