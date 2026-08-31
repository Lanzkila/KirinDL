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
        val isInstalling: Boolean = false,
        val isDownloading: Boolean = false,
        val isSavingConfig: Boolean = false,
        val isImportingCookies: Boolean = false,
        val statusMessage: String? = null,
        val errorMessage: String? = null,
        val savedFiles: List<String> = emptyList(),
        val destinationDirectory: String? = null,
    ) {
        val isInstalled: Boolean get() = !installedVersion.isNullOrBlank()
        val isBusy: Boolean
            get() = isInstalling || isDownloading || isSavingConfig || isImportingCookies
        val canDownload: Boolean
            get() =
                isInstalled &&
                    configValid &&
                    !isBusy &&
                    (url.trim().startsWith("https://") || url.trim().startsWith("http://"))
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
            )
        )
    val state = mutableState.asStateFlow()

    fun updateUrl(value: String) {
        mutableState.update {
            it.copy(
                url = value,
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
                    val snapshot = GalleryDlConfig.snapshot(App.context)
                    mutableState.update {
                        it.copy(
                            isSavingConfig = false,
                            configText = snapshot.configText,
                            configValid = snapshot.configValid,
                            statusMessage = "gallery-dl configuration saved",
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(
                            isSavingConfig = false,
                            errorMessage = error.message ?: "Could not save gallery-dl configuration",
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
                    val snapshot = GalleryDlConfig.snapshot(App.context)
                    mutableState.update {
                        it.copy(
                            isSavingConfig = false,
                            configText = snapshot.configText,
                            configValid = snapshot.configValid,
                            statusMessage = "gallery-dl configuration reset",
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(
                            isSavingConfig = false,
                            errorMessage = error.message ?: "Could not reset gallery-dl configuration",
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
                .onSuccess { size ->
                    mutableState.update {
                        it.copy(
                            isImportingCookies = false,
                            cookiesImported = size > 0L,
                            cookiesSize = size,
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
                    mutableState.update {
                        it.copy(
                            isImportingCookies = false,
                            cookiesImported = false,
                            cookiesSize = 0L,
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
                    mutableState.update {
                        it.copy(
                            isDownloading = false,
                            installedVersion = result.version,
                            savedFiles = result.savedFiles,
                            destinationDirectory = result.destinationDirectory,
                            statusMessage = "Saved ${result.savedFiles.size} file(s)",
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(
                            isDownloading = false,
                            errorMessage = error.message ?: "gallery-dl download failed",
                        )
                    }
                }
        }
    }
}
