package com.varshudev.a1swaara.bugreport

import android.content.Context
import java.io.File

/**
 * Lightweight crash persistence utility used by [com.varshudev.a1swaara.MainApplication] to record
 * fatal uncaught exceptions so the next launch can offer a one-tap recovery report.
 */
object SwaaraCrashRecovery {
    private const val CRASH_FILE_NAME = "last_crash_summary.txt"
    private const val MAX_STACK_LENGTH = 1800

    fun recordCrash(context: Context, threadName: String, throwable: Throwable) {
        runCatching {
            val file = File(context.filesDir, CRASH_FILE_NAME)
            val summary = buildString {
                appendLine("Thread: $threadName")
                appendLine("Exception: ${throwable::class.java.name}: ${throwable.message ?: "No message"}")
                appendLine("Trace:")
                append(throwable.stackTraceToString().take(MAX_STACK_LENGTH))
            }
            file.writeText(summary)
        }
    }

    fun readPendingCrash(context: Context): String? {
        return runCatching {
            val file = File(context.filesDir, CRASH_FILE_NAME)
            if (file.exists() && file.length() > 0L) file.readText().trim().takeIf { it.isNotEmpty() } else null
        }.getOrNull()
    }

    fun clearPendingCrash(context: Context) {
        runCatching {
            val file = File(context.filesDir, CRASH_FILE_NAME)
            if (file.exists()) file.delete()
        }
    }
}
