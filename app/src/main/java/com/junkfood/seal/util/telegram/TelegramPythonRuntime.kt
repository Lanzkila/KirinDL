package com.junkfood.seal.util.telegram

import android.content.Context
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

internal object TelegramPythonRuntime {
    fun bridge(context: Context): PyObject {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context.applicationContext))
        }
        return Python.getInstance().getModule("kirindl_telegram.telegram_bridge")
    }
}
