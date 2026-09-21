package com.junkfood.seal.ui.page.tools.mediaconverter

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MediaConverterViewModel {
    data class UiState(
        val inputs: List<MediaInput> = emptyList(),
        val settings: MediaConverterSettings = MediaConverterSettings(),
        val preset: MediaConverterPreset = MediaConverterPreset.BALANCED,
        val showAdvanced: Boolean = false,
        val isRunning: Boolean = false,
        val progress: Float? = null,
        val stage: String = "Idle",
        val currentItem: String = "",
        val completed: List<MediaConversionResult> = emptyList(),
        val errorMessage: String? = null,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow(UiState(settings = MediaConverterPreset.BALANCED.applyTo(MediaConverterSettings())))
    val state: StateFlow<UiState> = mutableState.asStateFlow()
    private var conversionJob: Job? = null

    fun addUris(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return
        val newItems = uris.map { MediaConverterEngine.describeUri(context, it) }
        mutableState.update { current ->
            val merged = (current.inputs + newItems).distinctBy { it.uri.toString() }
            current.copy(inputs = merged, errorMessage = null)
        }
    }

    fun removeInput(uri: Uri) {
        if (state.value.isRunning) return
        mutableState.update { it.copy(inputs = it.inputs.filterNot { item -> item.uri == uri }) }
    }

    fun clearInputs() {
        if (state.value.isRunning) return
        mutableState.update { it.copy(inputs = emptyList(), completed = emptyList(), errorMessage = null) }
    }

    fun setPreset(preset: MediaConverterPreset) {
        if (state.value.isRunning) return
        mutableState.update { it.copy(preset = preset, settings = preset.applyTo(it.settings)) }
    }

    fun updateSettings(transform: (MediaConverterSettings) -> MediaConverterSettings) {
        if (state.value.isRunning) return
        mutableState.update { it.copy(settings = transform(it.settings), errorMessage = null) }
    }

    fun toggleAdvanced() {
        mutableState.update { it.copy(showAdvanced = !it.showAdvanced) }
    }

    fun convertAll(context: Context) {
        val snapshot = state.value
        if (snapshot.isRunning || snapshot.inputs.isEmpty()) return
        conversionJob = scope.launch {
            mutableState.update {
                it.copy(
                    isRunning = true,
                    progress = 0f,
                    stage = "Preparing",
                    currentItem = "",
                    completed = emptyList(),
                    errorMessage = null,
                )
            }
            val results = mutableListOf<MediaConversionResult>()
            try {
                snapshot.inputs.forEachIndexed { index, input ->
                    val result =
                        MediaConverterEngine.convert(context.applicationContext, input, snapshot.settings) { progress ->
                            val fileFraction = progress.fraction ?: 0f
                            val overall = (index.toFloat() + fileFraction) / snapshot.inputs.size.toFloat()
                            mutableState.update {
                                it.copy(
                                    progress = overall.coerceIn(0f, 1f),
                                    stage = progress.stage,
                                    currentItem = progress.detail.ifBlank { input.name },
                                )
                            }
                        }
                    results += result
                    mutableState.update {
                        it.copy(
                            completed = results.toList(),
                            progress = (index + 1).toFloat() / snapshot.inputs.size.toFloat(),
                        )
                    }
                }
                mutableState.update {
                    it.copy(
                        isRunning = false,
                        progress = 1f,
                        stage = "Done",
                        currentItem = "${results.size} file(s) converted",
                    )
                }
            } catch (t: Throwable) {
                mutableState.update {
                    it.copy(
                        isRunning = false,
                        stage = "Failed",
                        errorMessage = t.message ?: t::class.java.simpleName,
                    )
                }
            }
        }
    }

    fun cancel() {
        conversionJob?.cancel()
        conversionJob = null
        mutableState.update { it.copy(isRunning = false, stage = "Cancelled", progress = null) }
    }

    fun close() {
        conversionJob?.cancel()
        scope.cancel()
    }
}
