package com.junkfood.seal.ui.page.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.junkfood.seal.App
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
        val isInstalling: Boolean = false,
        val isDownloading: Boolean = false,
        val statusMessage: String? = null,
        val errorMessage: String? = null,
        val savedFiles: List<String> = emptyList(),
        val destinationDirectory: String? = null,
    ) {
        val isInstalled: Boolean get() = !installedVersion.isNullOrBlank()
        val isBusy: Boolean get() = isInstalling || isDownloading
        val canDownload: Boolean
            get() = isInstalled && !isBusy && (url.trim().startsWith("https://") || url.trim().startsWith("http://"))
    }

    private val mutableState =
        MutableStateFlow(
            ViewState(installedVersion = GalleryDlEngine.installedVersion(App.context))
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
