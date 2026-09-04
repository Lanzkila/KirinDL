package com.junkfood.seal.util

import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import com.junkfood.seal.App
import com.junkfood.seal.App.Companion.context
import com.junkfood.seal.util.PreferenceUtil.getInt
import com.junkfood.seal.util.PreferenceUtil.updateLong
import com.yausername.youtubedl_android.YoutubeDL
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

object UpdateUtil {

    private const val OWNER = "Lanzkila"
    private const val REPO = "KirinDL"

    private const val YTDLP_STABLE_RELEASE =
        "https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest"
    private const val YTDLP_NIGHTLY_RELEASE =
        "https://api.github.com/repos/yt-dlp/yt-dlp-nightly-builds/releases/latest"

    private val jsonFormat = Json { ignoreUnknownKeys = true }

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
        val stable = UPDATE_CHANNEL.getInt() == STABLE
        return getReleaseList()
            .filter { release ->
                val version = (release.tagName ?: release.name).toVersion()
                if (stable) version is Version.Stable else version != EMPTY_VERSION
            }
            .maxByOrNull { (it.tagName ?: it.name).toVersion() }
            ?: throw IOException("No valid KirinDL release found")
    }

    /**
     * Compatibility no-op for the old startup cleanup hook. KirinDL no longer downloads,
     * caches, or installs application APKs itself.
     */
    suspend fun deleteOutdatedApk() = Unit

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
            manager.enqueue(request)
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

    suspend fun checkForUpdateResult(context: Context = App.context): Result<Release?> =
        withContext(Dispatchers.IO) {
            runCatching {
                    val currentVersion = context.getCurrentVersion()
                    val latestRelease = getLatestRelease()
                    val latestVersion = (latestRelease.tagName ?: latestRelease.name).toVersion()
                    if (currentVersion < latestVersion) latestRelease else null
                }
                .also { APP_UPDATE_CHECK_TIME.updateLong(System.currentTimeMillis()) }
        }

    suspend fun checkForUpdate(context: Context = App.context): Release? =
        checkForUpdateResult(context).getOrNull()

    suspend fun getCurrentReleaseResult(context: Context = App.context): Result<Release> =
        withContext(Dispatchers.IO) {
            runCatching {
                val currentVersion = context.getCurrentVersion()
                getReleaseList().firstOrNull { release ->
                    (release.tagName ?: release.name).toVersion().compareTo(currentVersion) == 0
                } ?: throw IOException("Current KirinDL release notes were not found")
            }
        }

    private fun Context.getCurrentVersion(): Version =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager
                .getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
                .versionName
                .toVersion()
        } else {
            packageManager.getPackageInfo(packageName, 0).versionName.toVersion()
        }

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

    private val pattern = Pattern.compile("""v?(\d+)\.(\d+)\.(\d+)(-(\w+)\.(\d+))?""")
    private val EMPTY_VERSION = Version.Stable()

    fun String?.toVersion(): Version =
        this?.run {
            val matcher = pattern.matcher(this)
            if (matcher.find()) {
                val major = matcher.group(1)?.toInt() ?: 0
                val minor = matcher.group(2)?.toInt() ?: 0
                val patch = matcher.group(3)?.toInt() ?: 0
                val buildNumber = matcher.group(6)?.toInt() ?: 0
                when (matcher.group(5)) {
                    "alpha" -> Version.Alpha(major, minor, patch, buildNumber)
                    "beta" -> Version.Beta(major, minor, patch, buildNumber)
                    "rc" -> Version.ReleaseCandidate(major, minor, patch, buildNumber)
                    else -> Version.Stable(major, minor, patch)
                }
            } else {
                EMPTY_VERSION
            }
        } ?: EMPTY_VERSION

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

            private const val STABLE = VARIANT * 4
            private const val ALPHA = VARIANT
            private const val BETA = VARIANT * 2
            private const val RELEASE_CANDIDATE = VARIANT * 3
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

        class Stable(
            versionMajor: Int = 0,
            versionMinor: Int = 0,
            versionPatch: Int = 0,
        ) : Version(versionMajor, versionMinor, versionPatch) {
            override fun toVersionName(): String = "${major}.${minor}.${patch}"

            override fun toNumber(): Long =
                major * MAJOR + minor * MINOR + patch * PATCH + build * BUILD + STABLE
        }

        override operator fun compareTo(other: Version): Int =
            toNumber().compareTo(other.toNumber())
    }
}
