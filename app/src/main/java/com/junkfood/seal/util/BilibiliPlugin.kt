package com.junkfood.seal.util

import android.content.Context
import android.util.AtomicFile
import java.io.File

/** Ship the small extractor extension with the APK, independently of engine updates. */
internal object BilibiliPlugin {
    private const val ASSET = "yt-dlp-plugins/kirin/yt_dlp_plugins/extractor/kirin_bilibili.py"

    @Synchronized
    fun install(context: Context): String {
        val root = File(context.filesDir, "kirin-bilibili-recovery-v1")
        val target = File(root, "kirin/yt_dlp_plugins/extractor/kirin_bilibili.py")
        val source = context.assets.open(ASSET).use { it.readBytes() }
        if (!target.isFile || !target.readBytes().contentEquals(source)) {
            check(target.parentFile!!.mkdirs() || target.parentFile!!.isDirectory)
            val atomic = AtomicFile(target)
            val output = atomic.startWrite()
            try {
                output.write(source)
                atomic.finishWrite(output)
            } catch (error: Exception) {
                atomic.failWrite(output)
                throw error
            }
        }
        return root.absolutePath
    }
}
