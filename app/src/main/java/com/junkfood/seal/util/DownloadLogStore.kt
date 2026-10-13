package com.junkfood.seal.util

/** Recent per-task diagnostics, available while active as well as after failure. */
internal object DownloadLogStore {
    private const val MAX_TASKS = 20
    private const val MAX_LINES = 250
    private const val MAX_LINE_LENGTH = 2000
    private val logs = linkedMapOf<String, ArrayDeque<String>>()
    private val urlQuery = Regex("(https?://[^\\s?]+)\\?[^\\s]+", RegexOption.IGNORE_CASE)
    private val credentials = Regex("(?i)(cookie|authorization|proxy-authorization)(\\s*[:=]\\s*).*?")

    @Synchronized
    fun append(taskId: String, message: String) {
        val lines = logs.getOrPut(taskId) { ArrayDeque() }
        message.lineSequence().filter { it.isNotBlank() }.forEach { raw ->
            val clean = sanitize(raw).take(MAX_LINE_LENGTH)
            if (lines.lastOrNull() != clean) lines.addLast(clean)
        }
        while (lines.size > MAX_LINES) lines.removeFirst()
        while (logs.size > MAX_TASKS) logs.remove(logs.keys.first())
    }

    internal fun sanitize(message: String): String {
        val text = urlQuery.replace(message) { "${it.groupValues[1]}?[redacted]" }
        val credential = credentials.find(text) ?: return text
        return text.take(credential.range.first) + credential.groupValues[1] + ": [redacted]"
    }

    @Synchronized
    fun read(taskId: String): String = logs[taskId]?.joinToString("\n")
        ?: "No log available for this task in this app session."
}
