package com.junkfood.seal.ui.page.tools

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.junkfood.seal.App
import com.junkfood.seal.util.GalleryDlConfig
import com.junkfood.seal.util.GalleryDlEngine
import com.junkfood.seal.util.GalleryDlRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GalleryDlViewModel : ViewModel() {
    data class ViewState(
        val url: String = "",
        val installedVersion: String? = null,
        val configText: String = "",
        val configValid: Boolean = true,
        val cookiesImported: Boolean = false,
        val cookiesSize: Long = 0L,
        val cacheSize: Long = 0L,
        val extractorSupported: Boolean? = null,
        val extractorLabel: String? = null,
        val runtimeVersion: String? = null,
        val runtimeReadyModules: List<String> = emptyList(),
        val runtimeMissingOptionalModules: List<String> = emptyList(),
        val isInstalling: Boolean = false,
        val isDownloading: Boolean = false,
        val isSavingConfig: Boolean = false,
        val isImportingCookies: Boolean = false,
        val isCheckingExtractor: Boolean = false,
        val isCheckingRuntime: Boolean = false,
        val isManagingCache: Boolean = false,
        val statusMessage: String? = null,
        val errorMessage: String? = null,
        val savedFiles: List<String> = emptyList(),
        val destinationDirectory: String? = null,
    ) {
        val isInstalled: Boolean get() = !installedVersion.isNullOrBlank()

        val isBusy: Boolean
            get() =
                isInstalling ||
                    isDownloading ||
                    isSavingConfig ||
                    isImportingCookies ||
                    isCheckingExtractor ||
                    isCheckingRuntime ||
                    isManagingCache

        val canDownload: Boolean
            get() =
                isInstalled &&
                    configValid &&
                    !isBusy &&
                    GalleryDlRunner.isCandidateUrl(url)
    }

    private val initialConfig = GalleryDlConfig.snapshot(App.context)
    private val mutableState =
        MutableStateFlow(
            ViewState(
                installedVersion = GalleryDlEngine.installedVersion(App.context),
                configText = initialConfig.configText,
                configValid = initialConfig.configValid,
                cookiesImported = initialConfig.cookiesImported,
                cookiesSize = initialConfig.cookiesSize,
                cacheSize = initialConfig.cacheSize,
            )
        )
    val state = mutableState.asStateFlow()

    fun updateUrl(value: String) {
        mutableState.update {
            it.copy(
                url = value,
                extractorSupported = null,
                extractorLabel = null,
                errorMessage = null,
                statusMessage = null,
                savedFiles = emptyList(),
                destinationDirectory = null,
            )
        }
    }

    fun updateConfigText(value: String) {
        mutableState.update {
            it.copy(
                configText = value,
                configValid = GalleryDlConfig.validateConfig(value).isSuccess,
                extractorSupported = null,
                extractorLabel = null,
                errorMessage = null,
                statusMessage = null,
            )
        }
    }

    fun saveConfig() {
        val current = mutableState.value
        if (current.isBusy || !current.configValid) return
        mutableState.update {
            it.copy(isSavingConfig = true, errorMessage = null, statusMessage = null)
        }
        viewModelScope.launch(Dispatchers.IO) {
            GalleryDlConfig.saveConfig(App.context, current.configText)
                .onSuccess {
                    refreshSnapshot(
                        statusMessage = "gallery-dl configuration saved",
                        finishConfigAction = true,
                    )
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(
                            isSavingConfig = false,
                            errorMessage =
                                error.message ?: "Could not save gallery-dl configuration",
                        )
                    }
                }
        }
    }

    fun resetConfig() {
        if (mutableState.value.isBusy) return
        mutableState.update {
            it.copy(isSavingConfig = true, errorMessage = null, statusMessage = null)
        }
        viewModelScope.launch(Dispatchers.IO) {
            GalleryDlConfig.resetConfig(App.context)
                .onSuccess {
                    refreshSnapshot(
                        statusMessage = "gallery-dl configuration reset",
                        finishConfigAction = true,
                    )
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(
                            isSavingConfig = false,
                            errorMessage =
                                error.message ?: "Could not reset gallery-dl configuration",
                        )
                    }
                }
        }
    }

    fun importConfig(uri: Uri) {
        if (mutableState.value.isBusy) return
        mutableState.update {
            it.copy(isSavingConfig = true, errorMessage = null, statusMessage = null)
        }
        viewModelScope.launch(Dispatchers.IO) {
            GalleryDlConfig.importConfig(App.context, uri)
                .onSuccess { snapshot ->
                    mutableState.update {
                        it.copy(
                            isSavingConfig = false,
                            configText = snapshot.configText,
                            configValid = snapshot.configValid,
                            cookiesImported = snapshot.cookiesImported,
                            cookiesSize = snapshot.cookiesSize,
                            cacheSize = snapshot.cacheSize,
                            extractorSupported = null,
                            extractorLabel = null,
                            statusMessage = "gallery-dl configuration imported",
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(
                            isSavingConfig = false,
                            errorMessage =
                                error.message ?: "Could not import gallery-dl configuration",
                        )
                    }
                }
        }
    }

    fun exportConfig(uri: Uri) {
        if (mutableState.value.isBusy) return
        mutableState.update {
            it.copy(isSavingConfig = true, errorMessage = null, statusMessage = null)
        }
        viewModelScope.launch(Dispatchers.IO) {
            GalleryDlConfig.exportConfig(App.context, uri)
                .onSuccess {
                    mutableState.update {
                        it.copy(
                            isSavingConfig = false,
                            statusMessage = "gallery-dl configuration exported",
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(
                            isSavingConfig = false,
                            errorMessage =
                                error.message ?: "Could not export gallery-dl configuration",
                        )
                    }
                }
        }
    }

    fun importCookies(uri: Uri) {
        if (mutableState.value.isBusy) return
        mutableState.update {
            it.copy(isImportingCookies = true, errorMessage = null, statusMessage = null)
        }
        viewModelScope.launch(Dispatchers.IO) {
            GalleryDlConfig.importCookies(App.context, uri)
                .onSuccess {
                    val snapshot = GalleryDlConfig.snapshot(App.context)
                    mutableState.update {
                        it.copy(
                            isImportingCookies = false,
                            cookiesImported = snapshot.cookiesImported,
                            cookiesSize = snapshot.cookiesSize,
                            cacheSize = snapshot.cacheSize,
                            extractorSupported = null,
                            extractorLabel = null,
                            statusMessage = "cookies.txt imported",
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(
                            isImportingCookies = false,
                            errorMessage = error.message ?: "Could not import cookies.txt",
                        )
                    }
                }
        }
    }

    fun clearCookies() {
        if (mutableState.value.isBusy) return
        mutableState.update {
            it.copy(isImportingCookies = true, errorMessage = null, statusMessage = null)
        }
        viewModelScope.launch(Dispatchers.IO) {
            GalleryDlConfig.clearCookies(App.context)
                .onSuccess {
                    val snapshot = GalleryDlConfig.snapshot(App.context)
                    mutableState.update {
                        it.copy(
                            isImportingCookies = false,
                            cookiesImported = false,
                            cookiesSize = 0L,
                            cacheSize = snapshot.cacheSize,
                            extractorSupported = null,
                            extractorLabel = null,
                            statusMessage = "Gallery DL cookies cleared",
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(
                            isImportingCookies = false,
                            errorMessage = error.message ?: "Could not clear Gallery DL cookies",
                        )
                    }
                }
        }
    }

    fun clearCache() {
        if (mutableState.value.isBusy) return
        mutableState.update {
            it.copy(isManagingCache = true, errorMessage = null, statusMessage = null)
        }
        viewModelScope.launch(Dispatchers.IO) {
            GalleryDlConfig.clearCache(App.context)
                .onSuccess {
                    val snapshot = GalleryDlConfig.snapshot(App.context)
                    mutableState.update {
                        it.copy(
                            isManagingCache = false,
                            cacheSize = snapshot.cacheSize,
                            extractorSupported = null,
                            extractorLabel = null,
                            statusMessage = "gallery-dl cache cleared",
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(
                            isManagingCache = false,
                            errorMessage = error.message ?: "Could not clear gallery-dl cache",
                        )
                    }
                }
        }
    }

    fun checkExtractor() {
        val current = mutableState.value
        if (current.isBusy || !current.isInstalled || !GalleryDlRunner.isCandidateUrl(current.url)) {
            return
        }

        mutableState.update {
            it.copy(
                isCheckingExtractor = true,
                extractorSupported = null,
                extractorLabel = null,
                errorMessage = null,
                statusMessage = null,
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            GalleryDlRunner.inspectUrl(App.context, current.url)
                .onSuccess { info ->
                    mutableState.update {
                        it.copy(
                            isCheckingExtractor = false,
                            extractorSupported = info.supported,
                            extractorLabel = info.label.takeIf { label -> label.isNotBlank() },
                            statusMessage =
                                if (info.supported) {
                                    "Extractor ready: ${info.label}"
                                } else {
                                    null
                                },
                            errorMessage =
                                if (info.supported) {
                                    null
                                } else {
                                    "No gallery-dl extractor matched this URL"
                                },
                        )
                    }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(
                            isCheckingExtractor = false,
                            extractorSupported = false,
                            errorMessage = error.message ?: "Could not check this URL",
                        )
                    }
                }
        }
    }

    fun runDiagnostics() {
        val current = mutableState.value
        if (current.isBusy || !current.isInstalled) return

        mutableState.update {
            it.copy(
                isCheckingRuntime = true,
                errorMessage = null,
                statusMessage = null,
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            GalleryDlRunner.diagnostics(App.context)
                .onSuccess { diagnostics ->
                    val snapshot = GalleryDlConfig.snapshot(App.context)
                    mutableState.update {
                        it.copy(
                            isCheckingRuntime = false,
                            runtimeVersion = diagnostics.engineVersion,
                            runtimeReadyModules = diagnostics.readyModules,
                            runtimeMissingOptionalModules = diagnostics.missingOptionalModules,
                            cacheSize = snapshot.cacheSize,
                            statusMessage = "Gallery DL compatibility check completed",
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(
                            isCheckingRuntime = false,
                            errorMessage =
                                error.message ?: "Gallery DL compatibility check failed",
                        )
                    }
                }
        }
    }

    fun installOrUpdateEngine() {
        if (mutableState.value.isBusy) return
        mutableState.update {
            it.copy(
                isInstalling = true,
                errorMessage = null,
                statusMessage = null,
                savedFiles = emptyList(),
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            GalleryDlEngine.installLatest(App.context)
                .onSuccess { info ->
                    mutableState.update {
                        it.copy(
                            isInstalling = false,
                            installedVersion = info.version,
                            runtimeVersion = null,
                            runtimeReadyModules = emptyList(),
                            runtimeMissingOptionalModules = emptyList(),
                            extractorSupported = null,
                            extractorLabel = null,
                            statusMessage = "gallery-dl ${info.version} is ready",
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(
                            isInstalling = false,
                            errorMessage = error.message ?: "Could not install gallery-dl",
                        )
                    }
                }
        }
    }

    fun download() {
        val current = mutableState.value
        if (!current.canDownload) return

        mutableState.update {
            it.copy(
                isDownloading = true,
                errorMessage = null,
                statusMessage = null,
                savedFiles = emptyList(),
                destinationDirectory = null,
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            GalleryDlRunner.download(App.context, current.url)
                .onSuccess { result ->
                    val snapshot = GalleryDlConfig.snapshot(App.context)
                    mutableState.update {
                        it.copy(
                            isDownloading = false,
                            installedVersion = result.version,
                            savedFiles = result.savedFiles,
                            destinationDirectory = result.destinationDirectory,
                            extractorSupported = true,
                            extractorLabel =
                                result.extractorLabel.takeIf { label -> label.isNotBlank() },
                            cacheSize = snapshot.cacheSize,
                            statusMessage =
                                buildString {
                                    append("Saved ${result.savedFiles.size} file(s)")
                                    if (result.extractorLabel.isNotBlank()) {
                                        append(" • ${result.extractorLabel}")
                                    }
                                },
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    val snapshot = GalleryDlConfig.snapshot(App.context)
                    mutableState.update {
                        it.copy(
                            isDownloading = false,
                            cacheSize = snapshot.cacheSize,
                            errorMessage = error.message ?: "gallery-dl download failed",
                        )
                    }
                }
        }
    }

    private fun refreshSnapshot(
        statusMessage: String,
        finishConfigAction: Boolean = false,
    ) {
        val snapshot = GalleryDlConfig.snapshot(App.context)
        mutableState.update {
            it.copy(
                isSavingConfig = if (finishConfigAction) false else it.isSavingConfig,
                configText = snapshot.configText,
                configValid = snapshot.configValid,
                cookiesImported = snapshot.cookiesImported,
                cookiesSize = snapshot.cookiesSize,
                cacheSize = snapshot.cacheSize,
                extractorSupported = null,
                extractorLabel = null,
                statusMessage = statusMessage,
                errorMessage = null,
            )
        }
    }
}
