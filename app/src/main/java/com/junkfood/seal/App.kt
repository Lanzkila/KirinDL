package com.junkfood.seal

import android.annotation.SuppressLint
import android.app.Application
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.getSystemService
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.google.android.material.color.DynamicColors
import com.junkfood.seal.download.DownloaderV2
import com.junkfood.seal.download.DownloaderV2Impl
import com.junkfood.seal.ui.page.download.HomePageViewModel
import com.junkfood.seal.ui.page.downloadv2.configure.DownloadDialogViewModel
import com.junkfood.seal.ui.page.settings.directory.Directory
import com.junkfood.seal.ui.page.settings.network.CookiesViewModel
import com.junkfood.seal.ui.page.hidden.HiddenContentViewModel
import com.junkfood.seal.ui.page.tools.CommentDownloadViewModel
import com.junkfood.seal.ui.page.tools.GalleryDlViewModel
import com.junkfood.seal.ui.page.tools.ThumbnailDownloadViewModel
import com.junkfood.seal.ui.page.tools.VideoInfoDownloadViewModel
import com.junkfood.seal.ui.page.videolist.VideoListViewModel
import com.junkfood.seal.util.AUDIO_DIRECTORY
import com.junkfood.seal.util.COMMAND_DIRECTORY
import com.junkfood.seal.util.GALLERY_DL_DIRECTORY
import com.junkfood.seal.util.DownloadUtil
import com.junkfood.seal.util.FileUtil
import com.junkfood.seal.util.FileUtil.createEmptyFile
import com.junkfood.seal.util.FileUtil.getCookiesFile
import com.junkfood.seal.util.FileUtil.getExternalDownloadDirectory
import com.junkfood.seal.util.FileUtil.getExternalPrivateDownloadDirectory
import com.junkfood.seal.util.NotificationUtil
import com.junkfood.seal.util.PreferenceUtil
import com.junkfood.seal.util.PreferenceUtil.getString
import com.junkfood.seal.util.PreferenceUtil.updateString
import com.junkfood.seal.util.makeToast
import com.junkfood.seal.util.SDCARD_URI
import com.junkfood.seal.util.UpdateUtil
import com.junkfood.seal.util.VIDEO_DIRECTORY
import com.junkfood.seal.util.YT_DLP_VERSION
import com.tencent.mmkv.MMKV
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

class App : Application(), SingletonImageLoader.Factory {

    /**
     * Coil 3 does NOT bundle a network fetcher by default — without an explicit network
     * component every remote image (e.g. video thumbnails) silently fails to load, leaving
     * blank poster areas in the format/configure screens. We register the OkHttp fetcher and
     * attach a desktop browser User-Agent so thumbnail CDNs that reject the default client
     * (returning 403) still serve the image.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val callFactory = {
            OkHttpClient.Builder()
                .addNetworkInterceptor(
                    Interceptor { chain ->
                        val request =
                            chain
                                .request()
                                .newBuilder()
                                .header(
                                    "User-Agent",
                                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                        "Chrome/124.0.0.0 Safari/537.36",
                                )
                                .build()
                        chain.proceed(request)
                    }
                )
                .build()
        }
        return ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = callFactory)) }
            .crossfade(true)
            .build()
    }

    override fun onCreate() {
        super.onCreate()

        // Establish the minimum crash-report context before any native/runtime initialization.
        // The previous ordering installed the uncaught handler only at the very end of onCreate(),
        // so a first-launch failure in MMKV/Koin/updater migration simply killed the process.
        context = applicationContext
        packageInfo =
            packageManager.run {
                if (Build.VERSION.SDK_INT >= 33)
                    getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
                else getPackageInfo(packageName, 0)
            }
        applicationScope = CoroutineScope(SupervisorJob())
        installCrashHandler()

        initializeMmkvWithRetry()

        // Koin should normally be empty here. Guarding it avoids a duplicate-start exception in
        // unusual process recreation / instrumentation paths.
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidLogger()
                androidContext(this@App)
                modules(
                    module {
                        single<DownloaderV2> { DownloaderV2Impl(androidContext()) }
                        viewModel { DownloadDialogViewModel(downloader = get()) }
                        viewModel { HomePageViewModel() }
                        viewModel { CookiesViewModel() }
                        viewModel { VideoListViewModel() }
                        viewModel { HiddenContentViewModel() }
                        viewModel { VideoInfoDownloadViewModel() }
                        viewModel { ThumbnailDownloadViewModel() }
                        viewModel { CommentDownloadViewModel() }
                        viewModel { GalleryDlViewModel() }
                    }
                )
            }
        }

        // A package update keeps SharedPreferences. This path only runs fully on the first launch
        // after a version change, so every side effect here must be non-fatal.
        runStartupStep("updater version-state sync") {
            UpdateUtil.syncInstalledVersionState(this)
        }
        runStartupStep("dynamic color setup") {
            DynamicColors.applyToActivitiesIfAvailable(this)
        }

        clipboard = getSystemService()!!
        connectivityManager = getSystemService()!!

        applicationScope.launch(Dispatchers.IO) {
            initializeBundledEngine("yt-dlp") {
                YoutubeDL.init(this@App)
                YoutubeDL.getInstance()
                    .version(this@App)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { YT_DLP_VERSION.updateString(it) }
            }
            initializeBundledEngine("FFmpeg") { FFmpeg.init(this@App) }
            initializeBundledEngine("Aria2c") { Aria2c.init(this@App) }

            // Cookie preparation and updater-state cleanup are helpful startup work, but neither
            // is allowed to take down the whole app if storage/OEM timing is temporarily bad.
            runCatching {
                DownloadUtil.getCookiesContentFromDatabase().getOrNull()?.let {
                    FileUtil.writeContentToFile(it, getCookiesFile())
                }
            }.onFailure { Log.e(STARTUP_TAG, "Cookie preparation failed", it) }

            runCatching { UpdateUtil.deleteOutdatedApk() }
                .onFailure { Log.e(STARTUP_TAG, "Updater startup sync failed", it) }
        }

        videoDownloadDir = VIDEO_DIRECTORY.getString(getExternalDownloadDirectory().absolutePath)

        audioDownloadDir = AUDIO_DIRECTORY.getString(File(videoDownloadDir, "Audio").absolutePath)
        if (!PreferenceUtil.containsKey(COMMAND_DIRECTORY)) {
            COMMAND_DIRECTORY.updateString(videoDownloadDir)
        }
        if (Build.VERSION.SDK_INT >= 26) {
            runStartupStep("notification channel setup") {
                NotificationUtil.createNotificationChannel()
            }
        }
    }

    private fun initializeMmkvWithRetry() {
        var lastError: Throwable? = null
        repeat(3) { attempt ->
            try {
                MMKV.initialize(this)
                return
            } catch (error: Throwable) {
                lastError = error
                Log.e(
                    STARTUP_TAG,
                    "MMKV initialization attempt ${attempt + 1} failed",
                    error,
                )
                if (attempt < 2) {
                    // Some OEMs briefly race native-library extraction immediately after replacing
                    // the APK. Retry inside the same launch instead of requiring a second app open.
                    Thread.sleep(200L * (attempt + 1))
                }
            }
        }
        throw IllegalStateException("MMKV initialization failed after retry", lastError)
    }

    private fun installCrashHandler() {
        Thread.setDefaultUncaughtExceptionHandler { _, error ->
            try {
                GlobalContext.getOrNull()?.get<DownloaderV2>()?.cleanup()
            } catch (cleanupError: Throwable) {
                Log.e(STARTUP_TAG, "Crash cleanup failed", cleanupError)
            }

            try {
                startCrashReportActivity(error)
            } catch (reportError: Throwable) {
                Log.e(STARTUP_TAG, "Unable to open crash report", reportError)
                error.printStackTrace()
            } finally {
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }

    private inline fun runStartupStep(
        name: String,
        block: () -> Unit,
    ) {
        runCatching(block)
            .onFailure { Log.e(STARTUP_TAG, "$name failed; continuing startup", it) }
    }

    private suspend fun initializeBundledEngine(
        name: String,
        initialize: () -> Unit,
    ) {
        var lastError: Throwable? = null
        repeat(2) { attempt ->
            try {
                initialize()
                return
            } catch (error: Throwable) {
                lastError = error
                if (attempt == 0) {
                    // First launch after an APK replacement can race native-library extraction on
                    // some OEM devices. One short retry avoids turning that transient race into a
                    // crash-report screen.
                    delay(350)
                }
            }
        }
        Log.e(STARTUP_TAG, "$name initialization failed after retry", lastError)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        GlobalContext.getOrNull()?.get<DownloaderV2>()?.cleanup()
    }

    private fun startCrashReportActivity(th: Throwable) {
        th.printStackTrace()
        startActivity(
            Intent(this, CrashReportActivity::class.java)
                .setAction("$packageName.error_report")
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra("error_report", getVersionReport() + "\n" + th.stackTraceToString())
                }
        )
    }

    companion object {
        private const val STARTUP_TAG = "KirinDL.Startup"
        lateinit var clipboard: ClipboardManager
        lateinit var videoDownloadDir: String
        lateinit var audioDownloadDir: String
        lateinit var applicationScope: CoroutineScope
        lateinit var connectivityManager: ConnectivityManager
        lateinit var packageInfo: PackageInfo

        @Volatile var isServiceRunning = false
        @Volatile private var boundDownloadService: DownloadService? = null

        private val connection =
            object : ServiceConnection {
                override fun onServiceConnected(className: ComponentName, service: IBinder) {
                    val binder = service as DownloadService.DownloadServiceBinder
                    boundDownloadService = binder.getService()
                    isServiceRunning = true
                }

                override fun onServiceDisconnected(arg0: ComponentName) {
                    // OS killed the service unexpectedly — allow startService() to restart it.
                    boundDownloadService = null
                    isServiceRunning = false
                }
            }

        /**
         * Called when the app becomes visible again (e.g. MainActivity.onResume()). If a
         * download's foreground promotion was previously blocked by the Android 12+
         * background-start restriction (see DownloadService.attemptPromoteToForeground), the
         * app being visible now satisfies the exemption, so we retry immediately instead of
         * waiting for the next task-state change to trigger it.
         */
        fun retryForegroundPromotionIfNeeded() {
            boundDownloadService?.retryPromoteToForegroundIfNeeded()
        }

        fun startService() {
            if (isServiceRunning) return
            // bindService() itself is safe on all API levels — the foreground-service-start
            // restriction (API 31+) is handled defensively inside DownloadService.onBind()
            // instead, so a crash there can never propagate back to this call site. This
            // try/catch is a last-resort safety net for any other unexpected binding failure
            // (e.g. SecurityException on some OEM ROMs), so a download task is never able to
            // take down the whole app process over a service-binding issue.
            runCatching {
                Intent(context.applicationContext, DownloadService::class.java).also { intent ->
                    context.applicationContext.bindService(
                        intent,
                        connection,
                        Context.BIND_AUTO_CREATE,
                    )
                }
            }
        }

        fun stopService() {
            if (!isServiceRunning) return
            try {
                isServiceRunning = false
                boundDownloadService = null
                context.applicationContext.run { unbindService(connection) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val privateDownloadDir: String
            get() =
                getExternalPrivateDownloadDirectory().run {
                    createEmptyFile(".nomedia")
                    absolutePath
                }

        fun updateDownloadDir(uri: Uri, directoryType: Directory) {
            when (directoryType) {
                Directory.AUDIO -> {
                    if (!FileUtil.isPrimaryStorageUri(uri)) {
                        context.makeToast(R.string.directory_not_supported)
                        return
                    }
                    val path = FileUtil.getRealPath(uri)
                    audioDownloadDir = path
                    PreferenceUtil.encodeString(AUDIO_DIRECTORY, path)
                }

                Directory.VIDEO -> {
                    if (!FileUtil.isPrimaryStorageUri(uri)) {
                        context.makeToast(R.string.directory_not_supported)
                        return
                    }
                    val path = FileUtil.getRealPath(uri)
                    videoDownloadDir = path
                    PreferenceUtil.encodeString(VIDEO_DIRECTORY, path)
                }

                Directory.GALLERY -> {
                    if (!FileUtil.isPrimaryStorageUri(uri)) {
                        context.makeToast(R.string.directory_not_supported)
                        return
                    }
                    val path = FileUtil.getRealPath(uri)
                    PreferenceUtil.encodeString(GALLERY_DL_DIRECTORY, path)
                }

                Directory.CUSTOM_COMMAND -> {
                    if (!FileUtil.isPrimaryStorageUri(uri)) {
                        context.makeToast(R.string.directory_not_supported)
                        return
                    }
                    val path = FileUtil.getRealPath(uri)
                    PreferenceUtil.encodeString(COMMAND_DIRECTORY, path)
                }

                Directory.SDCARD -> {
                    context.contentResolver?.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                    PreferenceUtil.encodeString(SDCARD_URI, uri.toString())
                }
            }
        }

        fun getVersionReport(): String {
            val versionName = packageInfo.versionName
            val page = packageInfo
            val versionCode =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    packageInfo.versionCode.toLong()
                }
            val release =
                if (Build.VERSION.SDK_INT >= 30) {
                    Build.VERSION.RELEASE_OR_CODENAME
                } else {
                    Build.VERSION.RELEASE
                }
            return StringBuilder()
                .append("App version: $versionName ($versionCode)\n")
                .append("Device information: Android $release (API ${Build.VERSION.SDK_INT})\n")
                .append("Supported ABIs: ${Build.SUPPORTED_ABIS.contentToString()}\n")
                .append("Yt-dlp version: ${YT_DLP_VERSION.getString()}\n")
                .toString()
        }

        fun isFDroidBuild(): Boolean = BuildConfig.FLAVOR == "fdroid"

        fun isDebugBuild(): Boolean = BuildConfig.DEBUG

        @SuppressLint("StaticFieldLeak") lateinit var context: Context
    }
}
