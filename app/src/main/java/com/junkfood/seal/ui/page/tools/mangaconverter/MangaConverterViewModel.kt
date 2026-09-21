package com.junkfood.seal.ui.page.tools.mangaconverter

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

class MangaConverterViewModel {
    data class UiState(
        val inputs: List<MangaInput> = emptyList(),
        val settings: MangaConverterSettings = MangaConverterSettings(),
        val isRunning: Boolean = false,
        val progress: Float? = null,
        val stage: String = "Idle",
        val currentItem: String = "",
        val completed: List<MangaConversionResult> = emptyList(),
        val errorMessage: String? = null,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = mutableState.asStateFlow()
    private var conversionJob: Job? = null

    fun addUris(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return
        val newItems = uris.map { MangaConverterEngine.describeUri(context, it) }
        mutableState.update { current ->
            current.copy(
                inputs = (current.inputs + newItems).distinctBy { it.uri.toString() },
                errorMessage = null,
            )
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

    fun updateSettings(transform: (MangaConverterSettings) -> MangaConverterSettings) {
        if (state.value.isRunning) return
        mutableState.update { it.copy(settings = transform(it.settings), errorMessage = null) }
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
            val results = mutableListOf<MangaConversionResult>()
            try {
                snapshot.inputs.forEachIndexed { index, input ->
                    val result =
                        MangaConverterEngine.convert(context.applicationContext, input, snapshot.settings) { progress ->
                            val overall =
                                (index.toFloat() + progress.fraction) /
                                    snapshot.inputs.size.toFloat()
                            mutableState.update {
                                it.copy(
                                    progress = overall.coerceIn(0f, 1f),
                                    stage = progress.stage,
                                    currentItem = progress.current.ifBlank { input.name },
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
        mutableState.update { it.copy(isRunning = false, progress = null, stage = "Cancelled") }
    }

    fun close() {
        conversionJob?.cancel()
        scope.cancel()
    }
}
