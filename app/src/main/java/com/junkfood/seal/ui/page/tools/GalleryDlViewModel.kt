package com.junkfood.seal.ui.page.tools

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.junkfood.seal.App
import com.junkfood.seal.util.GalleryDlConfig
import com.junkfood.seal.util.GalleryDlEngine
import com.junkfood.seal.util.GalleryDlRunner
import com.junkfood.seal.util.GalleryDlStore
import java.util.UUID
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
        val queue: List<GalleryDlStore.QueueRecord> = emptyList(),
        val history: List<GalleryDlStore.HistoryRecord> = emptyList(),
        val isQueueRunning: Boolean = false,
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
                    isManagingCache ||
                    isQueueRunning

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
                queue = GalleryDlStore.loadQueue(App.context),
                history = GalleryDlStore.loadHistory(App.context),
            )
        )
    val state = mutableState.asStateFlow()

    fun refreshFromDisk() {
        val snapshot = GalleryDlConfig.snapshot(App.context)
        mutableState.update {
            it.copy(
                installedVersion = GalleryDlEngine.installedVersion(App.context),
                configText = snapshot.configText,
                configValid = snapshot.configValid,
                cookiesImported = snapshot.cookiesImported,
                cookiesSize = snapshot.cookiesSize,
                cacheSize = snapshot.cacheSize,
                queue = GalleryDlStore.loadQueue(App.context),
                history = GalleryDlStore.loadHistory(App.context),
            )
        }
    }

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

    fun addCurrentToQueue() {
        val url = mutableState.value.url.trim()
        if (!GalleryDlRunner.isCandidateUrl(url)) {
            mutableState.update { it.copy(errorMessage = "Enter a valid Gallery URL") }
            return
        }
        addQueueUrls(listOf(url))
    }

    fun addBatch(text: String) {
        val urls =
            text.lines()
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct()
        val valid = urls.filter(GalleryDlRunner::isCandidateUrl)
        val invalidCount = urls.size - valid.size

        if (valid.isEmpty()) {
            mutableState.update {
                it.copy(errorMessage = "No valid gallery URLs found in batch input")
            }
            return
        }

        addQueueUrls(valid)
        mutableState.update {
            it.copy(
                statusMessage =
                    buildString {
                        append("Added ${valid.size} URL(s) to queue")
                        if (invalidCount > 0) append(" • skipped $invalidCount invalid")
                    }
            )
        }
    }

    private fun addQueueUrls(urls: List<String>) {
        val currentUrls = mutableState.value.queue.map { it.url }.toSet()
        val additions =
            urls.filterNot(currentUrls::contains).map { url ->
                GalleryDlStore.QueueRecord(
                    id = UUID.randomUUID().toString(),
                    url = url,
                    state = "pending",
                )
            }

        if (additions.isEmpty()) {
            mutableState.update { it.copy(statusMessage = "Those URLs are already in the queue") }
            return
        }

        val updated = mutableState.value.queue + additions
        GalleryDlStore.saveQueue(App.context, updated)
        mutableState.update {
            it.copy(
                queue = updated,
                statusMessage = "Added ${additions.size} URL(s) to queue",
                errorMessage = null,
            )
        }
    }

    fun removeQueueItem(id: String) {
        if (mutableState.value.isQueueRunning) return
        val updated = mutableState.value.queue.filterNot { it.id == id }
        GalleryDlStore.saveQueue(App.context, updated)
        mutableState.update { it.copy(queue = updated) }
    }

    fun clearFinishedQueue() {
        if (mutableState.value.isQueueRunning) return
        val updated = mutableState.value.queue.filter { it.state == "pending" || it.state == "running" }
        GalleryDlStore.saveQueue(App.context, updated)
        mutableState.update { it.copy(queue = updated) }
    }

    fun runQueue() {
        val current = mutableState.value
        if (
            current.isBusy ||
                !current.isInstalled ||
                current.queue.none { it.state == "pending" || it.state == "failed" }
        ) return

        mutableState.update {
            it.copy(isQueueRunning = true, statusMessage = "Queue started", errorMessage = null)
        }

        viewModelScope.launch(Dispatchers.IO) {
            val ids =
                mutableState.value.queue
                    .filter { it.state == "pending" || it.state == "failed" }
                    .map { it.id }

            ids.forEach { id ->
                val record = mutableState.value.queue.firstOrNull { it.id == id } ?: return@forEach
                updateQueueRecord(record.copy(state = "running", error = ""))

                GalleryDlRunner.download(App.context, record.url)
                    .onSuccess { result ->
                        updateQueueRecord(
                            record.copy(
                                state = "completed",
                                extractor = result.extractorLabel,
                                error = "",
                            )
                        )
                        addHistory(record.url, result, true, "")
                    }
                    .onFailure { error ->
                        val message = error.message ?: "Download failed"
                        updateQueueRecord(record.copy(state = "failed", error = message))
                        addHistory(record.url, null, false, message)
                    }
            }

            val snapshot = GalleryDlConfig.snapshot(App.context)
            mutableState.update {
                it.copy(
                    isQueueRunning = false,
                    cacheSize = snapshot.cacheSize,
                    statusMessage = "Queue finished",
                )
            }
        }
    }

    private fun updateQueueRecord(record: GalleryDlStore.QueueRecord) {
        val updated =
            mutableState.value.queue.map { current ->
                if (current.id == record.id) record else current
            }
        GalleryDlStore.saveQueue(App.context, updated)
        mutableState.update { it.copy(queue = updated) }
    }

    fun clearHistory() {
        if (mutableState.value.isBusy) return
        GalleryDlStore.clearHistory(App.context)
        mutableState.update {
            it.copy(history = emptyList(), statusMessage = "Gallery history cleared", errorMessage = null)
        }
    }

    fun reuseHistoryUrl(url: String) {
        updateUrl(url)
        mutableState.update { it.copy(statusMessage = "URL copied back to Download") }
    }

    private fun addHistory(
        url: String,
        result: GalleryDlRunner.DownloadResult?,
        success: Boolean,
        error: String,
    ) {
        val record =
            GalleryDlStore.HistoryRecord(
                id = UUID.randomUUID().toString(),
                url = url,
                extractor = result?.extractorLabel.orEmpty(),
                destination = result?.destinationDirectory.orEmpty(),
                fileCount = result?.savedFiles?.size ?: 0,
                success = success,
                error = error,
                finishedAt = System.currentTimeMillis(),
            )
        val updated = listOf(record) + mutableState.value.history.filterNot { it.url == url }
        GalleryDlStore.saveHistory(App.context, updated)
        mutableState.update { it.copy(history = GalleryDlStore.loadHistory(App.context)) }
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
        mutableState.update { it.copy(isSavingConfig = true, errorMessage = null, statusMessage = null) }
        viewModelScope.launch(Dispatchers.IO) {
            GalleryDlConfig.saveConfig(App.context, current.configText)
                .onSuccess { refreshSnapshot("gallery-dl configuration saved", true) }
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
        mutableState.update { it.copy(isSavingConfig = true, errorMessage = null, statusMessage = null) }
        viewModelScope.launch(Dispatchers.IO) {
            GalleryDlConfig.resetConfig(App.context)
                .onSuccess { refreshSnapshot("gallery-dl configuration reset", true) }
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

    fun importConfig(uri: Uri) {
        if (mutableState.value.isBusy) return
        mutableState.update { it.copy(isSavingConfig = true, errorMessage = null, statusMessage = null) }
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
                            errorMessage = error.message ?: "Could not import gallery-dl configuration",
                        )
                    }
                }
        }
    }

    fun exportConfig(uri: Uri) {
        if (mutableState.value.isBusy) return
        mutableState.update { it.copy(isSavingConfig = true, errorMessage = null, statusMessage = null) }
        viewModelScope.launch(Dispatchers.IO) {
            GalleryDlConfig.exportConfig(App.context, uri)
                .onSuccess {
                    mutableState.update {
                        it.copy(isSavingConfig = false, statusMessage = "gallery-dl configuration exported", errorMessage = null)
                    }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(
                            isSavingConfig = false,
                            errorMessage = error.message ?: "Could not export gallery-dl configuration",
                        )
                    }
                }
        }
    }

    fun importCookies(uri: Uri) {
        if (mutableState.value.isBusy) return
        mutableState.update { it.copy(isImportingCookies = true, errorMessage = null, statusMessage = null) }
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
                        it.copy(isImportingCookies = false, errorMessage = error.message ?: "Could not import cookies.txt")
                    }
                }
        }
    }

    fun clearCookies() {
        if (mutableState.value.isBusy) return
        mutableState.update { it.copy(isImportingCookies = true, errorMessage = null, statusMessage = null) }
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
                        it.copy(isImportingCookies = false, errorMessage = error.message ?: "Could not clear Gallery DL cookies")
                    }
                }
        }
    }

    fun clearCache() {
        if (mutableState.value.isBusy) return
        mutableState.update { it.copy(isManagingCache = true, errorMessage = null, statusMessage = null) }
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
                        it.copy(isManagingCache = false, errorMessage = error.message ?: "Could not clear gallery-dl cache")
                    }
                }
        }
    }

    fun checkExtractor() {
        val current = mutableState.value
        if (current.isBusy || !current.isInstalled || !GalleryDlRunner.isCandidateUrl(current.url)) return

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
                            extractorLabel = info.label.takeIf(String::isNotBlank),
                            statusMessage = if (info.supported) "Extractor ready: ${info.label}" else null,
                            errorMessage = if (info.supported) null else "No gallery-dl extractor matched this URL",
                        )
                    }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(isCheckingExtractor = false, extractorSupported = false, errorMessage = error.message ?: "Could not check this URL")
                    }
                }
        }
    }

    fun runDiagnostics() {
        val current = mutableState.value
        if (current.isBusy || !current.isInstalled) return

        mutableState.update { it.copy(isCheckingRuntime = true, errorMessage = null, statusMessage = null) }
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
                        it.copy(isCheckingRuntime = false, errorMessage = error.message ?: "Gallery DL compatibility check failed")
                    }
                }
        }
    }

    fun installOrUpdateEngine() {
        if (mutableState.value.isBusy) return
        mutableState.update {
            it.copy(isInstalling = true, errorMessage = null, statusMessage = null, savedFiles = emptyList())
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
                        it.copy(isInstalling = false, errorMessage = error.message ?: "Could not install gallery-dl")
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
                    addHistory(current.url, result, true, "")
                    val snapshot = GalleryDlConfig.snapshot(App.context)
                    mutableState.update {
                        it.copy(
                            isDownloading = false,
                            installedVersion = result.version,
                            savedFiles = result.savedFiles,
                            destinationDirectory = result.destinationDirectory,
                            extractorSupported = true,
                            extractorLabel = result.extractorLabel.takeIf(String::isNotBlank),
                            cacheSize = snapshot.cacheSize,
                            statusMessage =
                                buildString {
                                    append("Saved ${result.savedFiles.size} file(s)")
                                    if (result.extractorLabel.isNotBlank()) append(" • ${result.extractorLabel}")
                                },
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    val message = error.message ?: "Download failed"
                    addHistory(current.url, null, false, message)
                    val snapshot = GalleryDlConfig.snapshot(App.context)
                    mutableState.update {
                        it.copy(isDownloading = false, cacheSize = snapshot.cacheSize, errorMessage = message)
                    }
                }
        }
    }

    private fun refreshSnapshot(statusMessage: String, finishConfigAction: Boolean = false) {
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
