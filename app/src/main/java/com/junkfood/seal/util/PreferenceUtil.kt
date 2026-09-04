package com.junkfood.seal.util

import android.os.Build
import androidx.annotation.DeprecatedSinceApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.google.android.material.color.DynamicColors
import com.junkfood.seal.App
import com.junkfood.seal.App.Companion.applicationScope
import com.junkfood.seal.App.Companion.context
import com.junkfood.seal.App.Companion.isDebugBuild
import com.junkfood.seal.App.Companion.isFDroidBuild
import com.junkfood.seal.R
import com.junkfood.seal.database.objects.CommandTemplate
import com.junkfood.seal.download.Task
import com.junkfood.seal.ui.theme.DEFAULT_SEED_COLOR
import com.junkfood.seal.util.PreferenceUtil.getInt
import com.kyant.monet.PaletteStyle
import com.tencent.mmkv.MMKV
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

const val CUSTOM_COMMAND = "custom_command"
const val CONCURRENT = "concurrent_fragments"
const val EXTRACT_AUDIO = "extract_audio"
const val THUMBNAIL = "create_thumbnail"
const val YT_DLP_VERSION = "yt-dlp_init"
const val YT_DLP_AUTO_UPDATE = "yt-dlp_update"
const val DEBUG = "debug"
const val CONFIGURE = "configure"
const val DARK_THEME_VALUE = "dark_theme_value"
const val AUDIO_CONVERT = "audio_convert"
const val AUDIO_CONVERSION_FORMAT = "audio_convert_format"
const val AUDIO_FORMAT = "audio_format_preferred"
const val AUDIO_QUALITY = "audio_quality"
const val VIDEO_FORMAT = "video_format"
const val VIDEO_QUALITY = "quality"
const val AUDIO_CODEC = "audio_codec_preferred"
const val AUDIO_COVER_MODE = "audio_cover_mode"
const val AUDIO_COVER_FORMAT = "audio_cover_format"
const val VIDEO_CODEC = "video_codec_preferred"
const val VIDEO_CONTAINER = "video_container_preferred"

const val FORMAT_SORTING = "format_sorting"
const val SORTING_FIELDS = "sorting_fields"

const val WELCOME_DIALOG = "welcome_dialog"
const val ONBOARDING_COMPLETED = "onboarding_completed"
const val VIDEO_DIRECTORY = "download_dir"
const val AUDIO_DIRECTORY = "audio_dir"
const val COMMAND_DIRECTORY = "command_directory"
const val GALLERY_DL_DIRECTORY = "gallery_dl_directory"
const val SDCARD_DOWNLOAD = "sdcard_download"
const val SDCARD_URI = "sd_card_uri"
const val SUBDIRECTORY_EXTRACTOR = "sub-directory"
const val SUBDIRECTORY_PLAYLIST_TITLE = "subdirectory_playlist_title"
const val PLAYLIST = "playlist"
private const val LANGUAGE = "language"
const val NOTIFICATION = "notification"
private const val THEME_COLOR = "theme_color"
const val PALETTE_STYLE = "palette_style"
const val SUBTITLE = "subtitle"
const val EMBED_SUBTITLE = "embed_subtitle"
const val KEEP_SUBTITLE_FILES = "keep_subtitle"
const val SUBTITLE_LANGUAGE = "sub_lang"
const val AUTO_SUBTITLE = "auto_subtitle"
const val CONVERT_SUBTITLE = "convert_subtitle"
const val AUTO_TRANSLATED_SUBTITLES = "translated_subs"

const val TEMPLATE_ID = "template_id"
const val MAX_FILE_SIZE = "max_file_size"
const val SPONSORBLOCK = "sponsorblock"
const val SPONSORBLOCK_CATEGORIES = "sponsorblock_categories"
const val ARIA2C = "aria2c"
const val ARIA2C_CONNECTIONS = "aria2c_connections"

// KirinDL Bilibili-specific transfer profile. These settings are intentionally
// separate from the global concurrent-fragment/Aria2 preferences so changing
// Bilibili speed mode never changes another site's download behaviour.
const val BILIBILI_SPEED_MODE = "bilibili_speed_mode"
const val BILIBILI_CUSTOM_FRAGMENTS = "bilibili_custom_fragments"
const val BILIBILI_SPEED_AUTO = 0
const val BILIBILI_SPEED_BALANCED = 1
const val BILIBILI_SPEED_FAST = 2
const val BILIBILI_SPEED_CUSTOM = 3

const val COOKIES = "cookies"
const val USER_AGENT = "user_agent"
const val USER_AGENT_STRING = "user_agent_string"
const val AUTO_UPDATE = "auto_update"
const val UPDATE_CHANNEL = "update_channel"
const val PRIVATE_MODE = "private_mode"
private const val DYNAMIC_COLOR = "dynamic_color"
const val CELLULAR_DOWNLOAD = "cellular_download"
const val RATE_LIMIT = "rate_limit"
const val MAX_RATE = "max_rate"
private const val HIGH_CONTRAST = "high_contrast"
private const val GRADIENT_DARK_MODE = "gradient_dark_mode"
private const val KIRIN_BODY_COLOR_PRESET = "kirin_body_color_preset"
private const val KIRIN_BUTTON_COLOR_PRESET = "kirin_button_color_preset"
private const val KIRIN_FAVORITE_BODY_COLOR_PRESET = "kirin_favorite_body_color_preset"
private const val KIRIN_FAVORITE_BUTTON_COLOR_PRESET = "kirin_favorite_button_color_preset"
const val DISABLE_PREVIEW = "disable_preview"
const val PRIVATE_DIRECTORY = "private_directory"
const val CROP_ARTWORK = "crop_artwork"
const val EMBED_THUMBNAIL = "embed_thumbnail"
const val FORMAT_SELECTION = "format_selection"
const val VIDEO_CLIP = "video_clip"
const val SHOW_SPONSOR_MSG = "sponsor_msg_v1"
const val OUTPUT_TEMPLATE = "output_template"
const val CUSTOM_OUTPUT_TEMPLATE = "custom_output_template"
const val DOWNLOAD_ARCHIVE = "download_archive"
const val EMBED_METADATA = "embed_metadata"
const val RESTRICT_FILENAMES = "restrict_filenames"
const val AV1_HARDWARE_ACCELERATED = "av1_hardware_accelerated"
const val FORCE_IPV4 = "force_ipv4"
const val NO_CHECK_CERTIFICATE = "no_check_certificate"
const val MERGE_OUTPUT_MKV = "merge_to_mkv"
const val USE_CUSTOM_AUDIO_PRESET = "custom_audio_preset"

const val MERGE_MULTI_AUDIO_STREAM = "multi_audio_stream"

const val DOWNLOAD_TYPE_INITIALIZATION = "download_type_init"
private const val DOWNLOAD_TYPE = "download_type"

// Network Type Restriction
const val NETWORK_TYPE_RESTRICTION = "network_type_restriction"
const val NETWORK_PAUSE_DELAY_SECONDS = "network_pause_delay_seconds"

// Download Control
const val MAX_CONCURRENT_DOWNLOADS = "max_concurrent_downloads"

// Format Selection Layout
const val FORMAT_LIST_VIEW = "format_list_view"
const val HOME_RECENT_LIMIT = "home_recent_limit"
const val HOME_TRANSFER_DETAILS = "home_transfer_details"
const val HOME_INPUT_ANIMATION = "home_input_animation"
const val HOME_QUICK_TOOLS = "home_quick_tools"
const val HOME_SHOW_ACTIVITY = "home_show_activity"
const val HOME_COMPACT_ACTIVITY = "home_compact_activity"
const val QUEUE_BULK_CONFIRM = "queue_bulk_confirm"
const val SMART_DOWNLOAD_PROFILE = "smart_download_profile"
const val SMART_PROFILE_SLOT_1 = "smart_profile_slot_1"
const val SMART_PROFILE_SLOT_2 = "smart_profile_slot_2"
const val SMART_PROFILE_SLOT_3 = "smart_profile_slot_3"

// When true, the format-selection screen only lists MP4-family formats
// (mp4 video / m4a audio). Falls back to showing all formats if a site has none.
const val FORMAT_MP4_ONLY = "format_mp4_only"

// When true, a text file with video metadata (title, description, tags) is saved
const val DOWNLOAD_DOCS = "download_docs"

// Smart Notifications
const val NOTIFICATION_SOUND = "notification_sound"
const val NOTIFICATION_VIBRATE = "notification_vibrate"
const val NOTIFICATION_LED = "notification_led"
const val NOTIFICATION_SUCCESS_SOUND = "notification_success_sound"
const val NOTIFICATION_ERROR_SOUND = "notification_error_sound"

// Sponsor Support Dialog
const val SPONSOR_DIALOG_FREQUENCY = "sponsor_dialog_frequency"  // 0=Off, 1=Weekly, 2=Monthly
const val SPONSOR_DIALOG_LAST_SHOWN = "sponsor_dialog_last_shown"
const val SPONSOR_FREQ_OFF = 0
const val SPONSOR_FREQ_WEEKLY = 1
const val SPONSOR_FREQ_MONTHLY = 2

// Battery Optimization Dialog
// NOTE: there is intentionally NO dismissal flag or cooldown here. The home-screen dialog is
// re-evaluated fresh every time the app is opened/resumed purely from the live
// BatteryUtil.isIgnoringBatteryOptimizations() check — if it's still not disabled, the dialog
// shows again, every single time, no matter how many times it was dismissed before. Earlier
// versions of this feature used a permanent "don't show again" flag (optionally with a
// regression-detection workaround), which failed to re-show the dialog for anyone who dismissed
// it while battery optimization was already restricted (there's no state transition to detect
// there). Since disabling battery optimization is required for reliable background downloads,
// this reminder must not be permanently silenceable.
const val YT_DLP_UPDATE_CHANNEL = "yt-dlp_update_channel"
const val YT_DLP_UPDATE_TIME = "yt-dlp_last_update"
const val YT_DLP_UPDATE_INTERVAL = "yt-dlp_update_interval"
const val GALLERY_DL_AUTO_UPDATE = "gallery_dl_auto_update"
const val GALLERY_DL_UPDATE_TIME = "gallery_dl_last_update"
const val GALLERY_DL_UPDATE_INTERVAL = "gallery_dl_update_interval"
const val APP_UPDATE_CHECK_TIME = "app_update_last_check"

private const val INTERVAL_DAY = 86_400_000L
private const val INTERVAL_WEEK = 86_400_000L * 7
private const val INTERVAL_MONTH = 86_400_000L * 30

const val DEFAULT_INTERVAL = INTERVAL_DAY // every day

val UpdateIntervalList =
    mapOf(
        INTERVAL_DAY to R.string.every_day,
        INTERVAL_WEEK to R.string.every_week,
        INTERVAL_MONTH to R.string.every_month,
    )

const val NOT_SPECIFIED = 0
const val DEFAULT = NOT_SPECIFIED
const val SYSTEM_DEFAULT = NOT_SPECIFIED
const val NOT_CONVERT = NOT_SPECIFIED

const val NONE = NOT_SPECIFIED
const val USE_PREVIOUS_SELECTION = 1

enum class DownloadType {
    Audio,
    Video,
    Playlist,
    Command,
}

const val CONVERT_ASS = 1
const val CONVERT_LRC = 2
const val CONVERT_SRT = 3
const val CONVERT_VTT = 4

const val STABLE = 0
const val PRE_RELEASE = 1

const val YT_DLP_STABLE = 0
const val YT_DLP_NIGHTLY = 1

const val OPUS = 1
const val M4A = 2

const val FORMAT_COMPATIBILITY = 1
const val FORMAT_QUALITY = 2

const val CONVERT_MP3 = 0
const val CONVERT_M4A = 1
const val CONVERT_OPUS = 2
const val CONVERT_FLAC = 3
const val CONVERT_WAV = 4
const val CONVERT_VORBIS = 5
const val CONVERT_AAC = 6
const val CONVERT_ALAC = 7

// Keep the original 1..4 values stable for old queued tasks, then extend around them.
const val HIGH = 1          // 192 kbps
const val MEDIUM = 2        // 128 kbps
const val LOW = 3           // 64 kbps
const val ULTRA_LOW = 4     // 32 kbps
const val AUDIO_320 = 5
const val AUDIO_256 = 6
const val AUDIO_160 = 7
const val AUDIO_96 = 8
const val AUDIO_LOWEST = 9

val AudioQualityOptions =
    listOf(NOT_SPECIFIED, AUDIO_320, AUDIO_256, HIGH, AUDIO_160, MEDIUM, AUDIO_96, LOW, ULTRA_LOW, AUDIO_LOWEST)
val AudioConversionOptions =
    listOf(CONVERT_MP3, CONVERT_M4A, CONVERT_OPUS, CONVERT_FLAC, CONVERT_WAV, CONVERT_VORBIS, CONVERT_AAC, CONVERT_ALAC)

const val AUDIO_CODEC_AUTO = 0
const val AUDIO_CODEC_AAC = 1
const val AUDIO_CODEC_OPUS = 2
const val AUDIO_CODEC_VORBIS = 3
const val AUDIO_CODEC_MP3 = 4
const val AUDIO_CODEC_FLAC = 5
const val AUDIO_CODEC_ALAC = 6

const val AUDIO_COVER_LEGACY = 0
const val AUDIO_COVER_NONE = 1
const val AUDIO_COVER_EMBED = 2
const val AUDIO_COVER_SAVE = 3
const val AUDIO_COVER_BOTH = 4

const val AUDIO_COVER_FORMAT_AUTO = 0
const val AUDIO_COVER_FORMAT_JPG = 1
const val AUDIO_COVER_FORMAT_PNG = 2
const val AUDIO_COVER_FORMAT_WEBP = 3

const val VIDEO_CODEC_AUTO = 0
const val VIDEO_CODEC_H264 = 1
const val VIDEO_CODEC_VP9 = 2
const val VIDEO_CODEC_AV1 = 3
const val VIDEO_CODEC_HEVC = 4

const val VIDEO_CONTAINER_AUTO = 0
const val VIDEO_CONTAINER_MP4 = 1
const val VIDEO_CONTAINER_WEBM = 2
const val VIDEO_CONTAINER_MKV = 3

const val RES_HIGHEST = 0
const val RES_2160P = 1
const val RES_1440P = 2
const val RES_1080P = 3
const val RES_720P = 4
const val RES_480P = 5
const val RES_360P = 6
const val RES_LOWEST = 7

const val TEMPLATE_EXAMPLE = """--no-mtime -S "ext""""

const val TEMPLATE_SHORTCUTS = "template_shortcuts"

const val TASK_LIST = "task_list"
const val SAVED_LINKS = "saved_links"

val paletteStyles =
    listOf(
        PaletteStyle.TonalSpot,
        PaletteStyle.Spritz,
        PaletteStyle.FruitSalad,
        PaletteStyle.Vibrant,
        PaletteStyle.Monochrome,
    )

const val STYLE_TONAL_SPOT = 0
const val STYLE_SPRITZ = 1
const val STYLE_FRUIT_SALAD = 2
const val STYLE_VIBRANT = 3
const val STYLE_MONOCHROME = 4

// Network Type Restriction Options
const val NETWORK_ANY = 0
const val NETWORK_WIFI_ONLY = 1
const val NETWORK_MOBILE_ONLY = 2

private val StringPreferenceDefaults =
    mapOf(
        SPONSORBLOCK_CATEGORIES to "default",
        MAX_RATE to "1000",
        SUBTITLE_LANGUAGE to "en.*,.*-orig",
        OUTPUT_TEMPLATE to DownloadUtil.OUTPUT_TEMPLATE_ID,
        CUSTOM_OUTPUT_TEMPLATE to DownloadUtil.OUTPUT_TEMPLATE_ID,
    )

private val BooleanPreferenceDefaults =
    mapOf(
        FORMAT_SELECTION to true,
        CONFIGURE to true,
        CELLULAR_DOWNLOAD to false,
        YT_DLP_AUTO_UPDATE to true,
        GALLERY_DL_AUTO_UPDATE to true,
        NOTIFICATION to true,
        EMBED_METADATA to true,
        USE_CUSTOM_AUDIO_PRESET to false,
        AUTO_UPDATE to true,
        NOTIFICATION_SOUND to true,
        NOTIFICATION_VIBRATE to true,
        NOTIFICATION_LED to true,
        NOTIFICATION_SUCCESS_SOUND to true,
        NOTIFICATION_ERROR_SOUND to true,
        ONBOARDING_COMPLETED to false,
        FORMAT_LIST_VIEW to false,
        HOME_TRANSFER_DETAILS to true,
        HOME_INPUT_ANIMATION to true,
        HOME_QUICK_TOOLS to true,
        HOME_SHOW_ACTIVITY to true,
        HOME_COMPACT_ACTIVITY to false,
        QUEUE_BULK_CONFIRM to true,
        FORMAT_MP4_ONLY to true,
        DOWNLOAD_DOCS to false,
        USER_AGENT to true,
    )

private val IntPreferenceDefaults =
    mapOf(
        TEMPLATE_ID to 0,
        CONCURRENT to 8,
        LANGUAGE to SYSTEM_DEFAULT,
        PALETTE_STYLE to 0,
        DARK_THEME_VALUE to DarkThemePreference.ON,
        WELCOME_DIALOG to 1,
        AUDIO_CONVERSION_FORMAT to NOT_SPECIFIED,
        VIDEO_QUALITY to NOT_SPECIFIED,
        VIDEO_FORMAT to FORMAT_QUALITY,
        AUDIO_CODEC to AUDIO_CODEC_AUTO,
        AUDIO_COVER_MODE to AUDIO_COVER_LEGACY,
        AUDIO_COVER_FORMAT to AUDIO_COVER_FORMAT_AUTO,
        VIDEO_CODEC to VIDEO_CODEC_AUTO,
        VIDEO_CONTAINER to VIDEO_CONTAINER_AUTO,
        KIRIN_BODY_COLOR_PRESET to 0,
        KIRIN_BUTTON_COLOR_PRESET to 0,
        KIRIN_FAVORITE_BODY_COLOR_PRESET to 0,
        KIRIN_FAVORITE_BUTTON_COLOR_PRESET to 0,
        SMART_DOWNLOAD_PROFILE to 0,
        HOME_RECENT_LIMIT to 5,
        UPDATE_CHANNEL to STABLE,
        SHOW_SPONSOR_MSG to 0,
        CONVERT_SUBTITLE to NOT_SPECIFIED,
        DOWNLOAD_TYPE_INITIALIZATION to USE_PREVIOUS_SELECTION,
        YT_DLP_UPDATE_CHANNEL to YT_DLP_STABLE,
        DOWNLOAD_TYPE to DownloadType.Video.ordinal,
        NETWORK_TYPE_RESTRICTION to NETWORK_ANY,
        NETWORK_PAUSE_DELAY_SECONDS to 25,
        MAX_CONCURRENT_DOWNLOADS to 1,
        ARIA2C_CONNECTIONS to 16,
        BILIBILI_SPEED_MODE to BILIBILI_SPEED_AUTO,
        BILIBILI_CUSTOM_FRAGMENTS to 8,
        SPONSOR_DIALOG_FREQUENCY to SPONSOR_FREQ_WEEKLY,
    )

private val LongPreferenceDefaults = mapOf(
    YT_DLP_UPDATE_INTERVAL to DEFAULT_INTERVAL,
    GALLERY_DL_UPDATE_INTERVAL to DEFAULT_INTERVAL,
    GALLERY_DL_UPDATE_TIME to 0L,
    APP_UPDATE_CHECK_TIME to 0L,
    SPONSOR_DIALOG_LAST_SHOWN to 0L,
)

fun String.getStringDefault() = StringPreferenceDefaults.getOrElse(this) { "" }

object PreferenceUtil {
    private val kv: MMKV = MMKV.defaultMMKV()
    private val json = Json {
        ignoreUnknownKeys = true
        allowStructuredMapKeys = true
    }

    fun String.getInt(default: Int = IntPreferenceDefaults.getOrElse(this) { 0 }): Int =
        kv.decodeInt(this, default)

    fun String.getString(
        default: String = StringPreferenceDefaults.getOrElse(this) { "" }
    ): String = kv.decodeString(this) ?: default

    fun String.getBoolean(
        default: Boolean = BooleanPreferenceDefaults.getOrElse(this) { false }
    ): Boolean = kv.decodeBool(this, default)

    fun String.getLong(default: Long = LongPreferenceDefaults.getOrElse(this) { 0L }) =
        kv.decodeLong(this, default)

    fun String.updateString(newString: String) = kv.encode(this, newString)

    fun String.updateInt(newInt: Int) = kv.encode(this, newInt)

    fun String.updateLong(newLong: Long) = kv.encode(this, newLong)

    fun String.updateBoolean(newValue: Boolean) = kv.encode(this, newValue)

    fun updateValue(key: String, b: Boolean) = key.updateBoolean(b)

    fun encodeInt(key: String, int: Int) = key.updateInt(int)

    fun encodeString(key: String, string: String) = key.updateString(string)

    fun containsKey(key: String) = kv.containsKey(key)

    fun getAudioConvertFormat(): Int = AUDIO_CONVERSION_FORMAT.getInt()

    fun getVideoResolution(): Int = VIDEO_QUALITY.getInt()

    fun getAudioQuality(): Int = AUDIO_QUALITY.getInt()

    fun getVideoFormat(): Int = VIDEO_FORMAT.getInt()

    fun getAudioFormat(): Int = AUDIO_FORMAT.getInt()

    fun getDownloadType(
        usePreviousType: Boolean = DOWNLOAD_TYPE_INITIALIZATION.getInt() == USE_PREVIOUS_SELECTION
    ): DownloadType? {
        return if (usePreviousType) {
            DownloadType.entries.firstOrNull { it.ordinal == DOWNLOAD_TYPE.getInt() }
                ?: DownloadType.Video
        } else {
            null
        }
    }

    fun updateDownloadType(type: DownloadType) = DOWNLOAD_TYPE.updateInt(type.ordinal)

    fun isNetworkAvailable(): Boolean {
        val connectivityManager = App.connectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun isNetworkAvailableForDownload(): Boolean {
        // First check if network is available at all
        if (!isNetworkAvailable()) {
            return false
        }
        
        val networkRestriction = NETWORK_TYPE_RESTRICTION.getInt()
        val isMetered = App.connectivityManager.isActiveNetworkMetered
        
        return when (networkRestriction) {
            NETWORK_WIFI_ONLY -> !isMetered  // Only allow WiFi (non-metered)
            NETWORK_MOBILE_ONLY -> isMetered  // Only allow Mobile (metered)
            NETWORK_ANY -> true  // Allow any network type (both WiFi and Mobile)
            else -> CELLULAR_DOWNLOAD.getBoolean() || !isMetered
        }
    }

    fun getNetworkPauseDelayMs(): Long {
        val seconds = NETWORK_PAUSE_DELAY_SECONDS.getInt().coerceIn(5, 120)
        return seconds.toLong() * 1000L
    }

    fun getNetworkErrorMessage(): Int {
        if (!isNetworkAvailable()) {
            return R.string.network_unavailable
        }
        
        val networkRestriction = NETWORK_TYPE_RESTRICTION.getInt()
        val isMetered = App.connectivityManager.isActiveNetworkMetered
        
        return when (networkRestriction) {
            NETWORK_WIFI_ONLY -> 
                if (isMetered) R.string.wifi_only_restriction_message
                else R.string.network_unavailable
            NETWORK_MOBILE_ONLY -> 
                if (!isMetered) R.string.mobile_only_restriction_message
                else R.string.network_unavailable
            else -> R.string.cellular_data_warning
        }
    }

    fun isAutoUpdateEnabled(): Boolean {
        return when {
            isFDroidBuild() -> false
            isDebugBuild() -> false
            else -> AUTO_UPDATE.getBoolean()
        }
    }

    @DeprecatedSinceApi(api = 33)
    fun getLocaleFromPreference(): Locale? {
        val languageCode = LANGUAGE.getInt()
        return LocaleLanguageCodeMap.entries.find { it.value == languageCode }?.key
    }

    fun saveLocalePreference(locale: Locale?) {
        if (Build.VERSION.SDK_INT >= 33) {
            // No op
        } else {
            LANGUAGE.updateInt(LocaleLanguageCodeMap[locale] ?: SYSTEM_DEFAULT)
        }
    }

    fun getConcurrentFragments(level: Int = CONCURRENT.getInt()): Float {
        return when (level) {
            1 -> 0f
            8 -> 0.33f
            16 -> 0.66f
            else -> 1f
        }
    }

    fun getSponsorBlockCategories(): String = SPONSORBLOCK_CATEGORIES.getString()

    const val COOKIE_HEADER =
        "# Netscape HTTP Cookie File\n" + "# Auto-generated by Seal built-in WebView\n"

    val templateListStateFlow: StateFlow<List<CommandTemplate>> =
        DatabaseUtil.getTemplateFlow()
            .stateIn(applicationScope, started = SharingStarted.Eagerly, emptyList())

    private val List<CommandTemplate>.selectedTemplate: CommandTemplate?
        get() = find { it.id == TEMPLATE_ID.getInt() }

    fun getTemplate(): CommandTemplate {
        return templateListStateFlow.value.selectedTemplate
            ?: templateListStateFlow.value.firstOrNull()
            ?: throw NoSuchElementException("No command template found")
    }

    suspend fun initializeTemplateSample() {
        TEMPLATE_ID.updateInt(
            DatabaseUtil.insertTemplate(
                    CommandTemplate(
                        id = 0,
                        name = context.getString(R.string.custom_command_template),
                        template = TEMPLATE_EXAMPLE,
                    )
                )
                .toInt()
        )
    }

    data class AppSettings(
        val darkTheme: DarkThemePreference = DarkThemePreference(),
        val isDynamicColorEnabled: Boolean = false,
        val seedColor: Int = DEFAULT_SEED_COLOR,
        val paletteStyleIndex: Int = 0,
        val isGradientDarkModeEnabled: Boolean = false,
        val bodyColorPreset: Int = 0,
        val buttonColorPreset: Int = 0,
    )

    fun getMaxDownloadRate(): String = MAX_RATE.getString()

    private val mutableAppSettingsStateFlow =
        MutableStateFlow(
            AppSettings(
                DarkThemePreference(
                    darkThemeValue =
                        kv.decodeInt(DARK_THEME_VALUE, DarkThemePreference.ON),
                    isHighContrastModeEnabled = kv.decodeBool(HIGH_CONTRAST, false),
                ),
                isDynamicColorEnabled =
                    kv.decodeBool(DYNAMIC_COLOR, DynamicColors.isDynamicColorAvailable()),
                seedColor = kv.decodeInt(THEME_COLOR, DEFAULT_SEED_COLOR),
                paletteStyleIndex = kv.decodeInt(PALETTE_STYLE, 0),
                isGradientDarkModeEnabled = kv.decodeBool(GRADIENT_DARK_MODE, true),
                bodyColorPreset = kv.decodeInt(KIRIN_BODY_COLOR_PRESET, 0),
                buttonColorPreset = kv.decodeInt(KIRIN_BUTTON_COLOR_PRESET, 0),
            )
        )
    val AppSettingsStateFlow = mutableAppSettingsStateFlow.asStateFlow()

    fun modifyDarkThemePreference(
        darkThemeValue: Int = AppSettingsStateFlow.value.darkTheme.darkThemeValue,
        isHighContrastModeEnabled: Boolean =
            AppSettingsStateFlow.value.darkTheme.isHighContrastModeEnabled,
    ) {
        applicationScope.launch(Dispatchers.IO) {
            mutableAppSettingsStateFlow.update {
                it.copy(
                    darkTheme =
                        AppSettingsStateFlow.value.darkTheme.copy(
                            darkThemeValue = darkThemeValue,
                            isHighContrastModeEnabled = isHighContrastModeEnabled,
                        )
                )
            }
            kv.encode(DARK_THEME_VALUE, darkThemeValue)
            kv.encode(HIGH_CONTRAST, isHighContrastModeEnabled)
        }
    }

    fun modifyThemeSeedColor(colorArgb: Int, paletteStyleIndex: Int) {
        applicationScope.launch(Dispatchers.IO) {
            mutableAppSettingsStateFlow.update {
                it.copy(seedColor = colorArgb, paletteStyleIndex = paletteStyleIndex)
            }
            kv.encode(THEME_COLOR, colorArgb)
            kv.encode(PALETTE_STYLE, paletteStyleIndex)
        }
    }

    fun modifyBodyColorPreset(index: Int) {
        applicationScope.launch(Dispatchers.IO) {
            mutableAppSettingsStateFlow.update { it.copy(bodyColorPreset = index) }
            kv.encode(KIRIN_BODY_COLOR_PRESET, index)
        }
    }

    fun modifyButtonColorPreset(index: Int) {
        applicationScope.launch(Dispatchers.IO) {
            mutableAppSettingsStateFlow.update { it.copy(buttonColorPreset = index) }
            kv.encode(KIRIN_BUTTON_COLOR_PRESET, index)
        }
    }

    fun saveFavoriteColorPair() {
        val current = AppSettingsStateFlow.value
        kv.encode(KIRIN_FAVORITE_BODY_COLOR_PRESET, current.bodyColorPreset)
        kv.encode(KIRIN_FAVORITE_BUTTON_COLOR_PRESET, current.buttonColorPreset)
    }

    fun getFavoriteColorPair(): Pair<Int, Int> =
        kv.decodeInt(KIRIN_FAVORITE_BODY_COLOR_PRESET, 0) to
            kv.decodeInt(KIRIN_FAVORITE_BUTTON_COLOR_PRESET, 0)

    fun applyFavoriteColorPair() {
        val (body, button) = getFavoriteColorPair()
        modifyBodyColorPreset(body)
        modifyButtonColorPreset(button)
    }

    fun resetKirinColorPair() {
        modifyBodyColorPreset(0)
        modifyButtonColorPreset(0)
    }

    fun switchDynamicColor(
        enabled: Boolean = !mutableAppSettingsStateFlow.value.isDynamicColorEnabled
    ) {
        applicationScope.launch(Dispatchers.IO) {
            mutableAppSettingsStateFlow.update { it.copy(isDynamicColorEnabled = enabled) }
            kv.encode(DYNAMIC_COLOR, enabled)
        }
    }

    fun switchGradientDarkMode(
        enabled: Boolean = !mutableAppSettingsStateFlow.value.isGradientDarkModeEnabled
    ) {
        applicationScope.launch(Dispatchers.IO) {
            mutableAppSettingsStateFlow.update { it.copy(isGradientDarkModeEnabled = enabled) }
            kv.encode(GRADIENT_DARK_MODE, enabled)
        }
    }

    fun encodeTaskListBackup(map: Map<Task, Task.State>) =
        runCatching { json.encodeToString<Map<Task, Task.State>>(map) }
            .onSuccess { kv.encode(TASK_LIST, it) }
            .onFailure { it.printStackTrace() }

    fun decodeTaskListBackup(): Map<Task, Task.State> =
        runCatching {
                kv.decodeString(TASK_LIST)?.let { json.decodeFromString<Map<Task, Task.State>>(it) }
            }
            .onFailure { it.printStackTrace() }
            .getOrNull() ?: emptyMap()

    fun getSavedLinks(): Set<String> = kv.decodeStringSet(SAVED_LINKS) ?: emptySet()

    fun updateSavedLinks(links: Set<String>) = kv.encode(SAVED_LINKS, links)

    private const val TAG = "PreferenceUtil"
}

data class DarkThemePreference(
    val darkThemeValue: Int = ON,
    val isHighContrastModeEnabled: Boolean = false,
) {
    companion object {
        const val FOLLOW_SYSTEM = 1
        const val ON = 2
        const val OFF = 3
    }

    @Composable
    fun isDarkTheme(): Boolean {
        return if (darkThemeValue == FOLLOW_SYSTEM) isSystemInDarkTheme() else darkThemeValue == ON
    }

    @Composable
    fun getDarkThemeDesc(): String {
        return when (darkThemeValue) {
            FOLLOW_SYSTEM -> stringResource(R.string.follow_system)
            ON -> stringResource(R.string.on)
            else -> stringResource(R.string.off)
        }
    }
}

object PreferenceStrings {
    fun getSubtitleConversionFormat(subtitleFormat: Int = CONVERT_SUBTITLE.getInt()): String =
        when (subtitleFormat) {
            CONVERT_LRC -> context.getString(R.string.convert_to, "lrc")
            CONVERT_ASS -> context.getString(R.string.convert_to, "ass")
            CONVERT_SRT -> context.getString(R.string.convert_to, "srt")
            CONVERT_VTT -> context.getString(R.string.convert_to, "vtt")
            else -> context.getString(R.string.not_convert)
        }

    @Composable
    fun getAudioFormatDesc(audioFormatCode: Int = PreferenceUtil.getAudioFormat()): String =
        when (audioFormatCode) {
            M4A -> "M4A"
            OPUS -> "OPUS"
            else -> stringResource(R.string.not_specified)
        }

    @Composable
    fun getAudioQualityDesc(audioQualityCode: Int = PreferenceUtil.getAudioQuality()): String =
        when (audioQualityCode) {
            NOT_SPECIFIED -> stringResource(R.string.best_quality)
            AUDIO_320 -> "320 Kbps"
            AUDIO_256 -> "256 Kbps"
            HIGH -> "192 Kbps"
            AUDIO_160 -> "160 Kbps"
            MEDIUM -> "128 Kbps"
            AUDIO_96 -> "96 Kbps"
            LOW -> "64 Kbps"
            ULTRA_LOW -> "32 Kbps"
            AUDIO_LOWEST -> stringResource(R.string.lowest_bitrate)
            else -> stringResource(R.string.best_quality)
        }

    @Composable
    fun getAudioConvertDesc(audioFormatCode: Int = PreferenceUtil.getAudioConvertFormat()): String {
        val format =
            when (audioFormatCode) {
                CONVERT_M4A -> "M4A"
                CONVERT_OPUS -> "OPUS"
                CONVERT_FLAC -> "FLAC"
                CONVERT_WAV -> "WAV"
                CONVERT_VORBIS -> "VORBIS"
                CONVERT_AAC -> "AAC"
                CONVERT_ALAC -> "ALAC"
                else -> "MP3"
            }
        return stringResource(R.string.convert_to).format(format)
    }

    fun getAudioCodecDesc(code: Int): String =
        when (code) {
            AUDIO_CODEC_AAC -> "AAC"
            AUDIO_CODEC_OPUS -> "Opus"
            AUDIO_CODEC_VORBIS -> "Vorbis"
            AUDIO_CODEC_MP3 -> "MP3"
            AUDIO_CODEC_FLAC -> "FLAC"
            AUDIO_CODEC_ALAC -> "ALAC"
            else -> "Auto / source best"
        }

    fun getAudioCoverModeDesc(code: Int): String =
        when (code) {
            AUDIO_COVER_NONE -> "No cover"
            AUDIO_COVER_EMBED -> "Embed cover"
            AUDIO_COVER_SAVE -> "Save cover file"
            AUDIO_COVER_BOTH -> "Embed + save cover"
            else -> "Automatic (current behavior)"
        }

    fun getAudioCoverFormatDesc(code: Int): String =
        when (code) {
            AUDIO_COVER_FORMAT_JPG -> "JPG"
            AUDIO_COVER_FORMAT_PNG -> "PNG"
            AUDIO_COVER_FORMAT_WEBP -> "WebP"
            else -> "Auto / source format"
        }

    fun getVideoCodecDesc(code: Int): String =
        when (code) {
            VIDEO_CODEC_H264 -> "H.264 / AVC"
            VIDEO_CODEC_VP9 -> "VP9"
            VIDEO_CODEC_AV1 -> "AV1"
            VIDEO_CODEC_HEVC -> "H.265 / HEVC"
            else -> "Auto / profile default"
        }

    fun getVideoContainerDesc(code: Int): String =
        when (code) {
            VIDEO_CONTAINER_MP4 -> "MP4"
            VIDEO_CONTAINER_WEBM -> "WebM"
            VIDEO_CONTAINER_MKV -> "MKV"
            else -> "Auto / source best"
        }

    @Composable
    fun getVideoFormatDescComp(videoFormatCode: Int = PreferenceUtil.getVideoFormat()): String {
        return when (videoFormatCode) {
            FORMAT_COMPATIBILITY -> stringResource(R.string.prefer_compatibility_desc)
            FORMAT_QUALITY -> stringResource(R.string.prefer_quality_desc)
            else -> stringResource(R.string.not_specified)
        }
    }

    @Composable
    fun getVideoResolutionDesc(
        videoQualityCode: Int = PreferenceUtil.getVideoResolution()
    ): String {
        return when (videoQualityCode) {
            1 -> "2160p"
            2 -> "1440p"
            3 -> "1080p"
            4 -> "720p"
            5 -> "480p"
            6 -> "360p"
            7 -> stringResource(R.string.lowest_quality)
            8 -> "4320p (8K)"
            9 -> "2880p (5K)"
            10 -> "240p"
            11 -> "144p"
            else -> stringResource(R.string.best_quality)
        }
    }

    @Composable
    fun getVideoFormatLabel(videoFormatPreference: Int = PreferenceUtil.getVideoFormat()): String {
        return when (videoFormatPreference) {
            FORMAT_COMPATIBILITY -> stringResource(id = R.string.legacy)
            else -> stringResource(id = R.string.quality)
        }
    }

    @Composable
    fun getUpdateIntervalText(interval: Long): String {
        return stringResource(
            id =
                when (interval) {
                    INTERVAL_DAY -> R.string.every_day
                    INTERVAL_WEEK -> R.string.every_week
                    INTERVAL_MONTH -> R.string.every_month
                    else -> R.string.disabled
                }
        )
    }

    @Composable
    fun getAudioPresetText(preferences: DownloadUtil.DownloadPreferences): String {
        return with(preferences) {
            when {
                formatSorting -> {
                    sortingFields
                }

                !useCustomAudioPreset -> {
                    stringResource(R.string.best_quality)
                }

                convertAudio -> {
                    val name =
                        when (audioConvertFormat) {
                            CONVERT_M4A -> "M4A"
                            CONVERT_OPUS -> "OPUS"
                            CONVERT_FLAC -> "FLAC"
                            CONVERT_WAV -> "WAV"
                            CONVERT_VORBIS -> "VORBIS"
                            CONVERT_AAC -> "AAC"
                            CONVERT_ALAC -> "ALAC"
                            else -> "MP3"
                        }
                    stringResource(R.string.convert_to, name)
                }

                else -> {
                    val preferredFormat =
                        when (audioFormat) {
                            M4A -> stringResource(R.string.prefer_placeholder, "M4A")
                            OPUS -> stringResource(R.string.prefer_placeholder, "OPUS")
                            else -> null
                        }
                    val preferredQuality =
                        when (audioQuality) {
                            NOT_SPECIFIED -> stringResource(R.string.best_quality)
                            AUDIO_320 -> "320 Kbps"
                            AUDIO_256 -> "256 Kbps"
                            HIGH -> "192 Kbps"
                            AUDIO_160 -> "160 Kbps"
                            MEDIUM -> "128 Kbps"
                            AUDIO_96 -> "96 Kbps"
                            LOW -> "64 Kbps"
                            ULTRA_LOW -> "32 Kbps"
                            AUDIO_LOWEST -> stringResource(R.string.lowest_bitrate)
                            else -> stringResource(R.string.best_quality)
                        }
                    val preferredCodec =
                        audioCodec.takeIf { it != AUDIO_CODEC_AUTO }?.let(::getAudioCodecDesc)
                    listOfNotNull(preferredFormat, preferredCodec, preferredQuality)
                        .joinToString(separator = ", ")
                }
            }
        }
    }

    @Composable
    fun getVideoPresetText(preferences: DownloadUtil.DownloadPreferences): String {
        return with(preferences) {
            when {
                formatSorting -> {
                    sortingFields
                }

                else -> {
                    val preferredFormat =
                        stringResource(
                            id = R.string.prefer_placeholder,
                            stringResource(
                                id =
                                    if (videoFormat == FORMAT_QUALITY) R.string.quality
                                    else R.string.legacy
                            ),
                        )
                    val preferredResolution = getVideoResolutionDesc(videoResolution)
                    val preferredCodec =
                        videoCodec.takeIf { it != VIDEO_CODEC_AUTO }?.let(::getVideoCodecDesc)
                    val preferredContainer =
                        videoContainer.takeIf { it != VIDEO_CONTAINER_AUTO }?.let(::getVideoContainerDesc)
                    listOfNotNull(preferredFormat, preferredResolution, preferredCodec, preferredContainer)
                        .joinToString(separator = ", ")
                }
            }
        }
    }
}
