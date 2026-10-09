package com.melihaksoy.betterlogcat.format

import com.android.tools.r8.DiagnosticsHandler
import com.android.tools.r8.retrace.ProguardMapProducer
import com.android.tools.r8.retrace.ProguardMappingSupplier
import com.android.tools.r8.retrace.RetraceOptions
import com.android.tools.r8.retrace.RetraceStackTraceContext
import com.android.tools.r8.retrace.StringRetrace
import java.nio.file.Path

/**
 * Turns obfuscated stack traces back into source names with an R8/ProGuard `mapping.txt`.
 * Uses R8's own retrace (bundled with the Android plugin). Not thread safe.
 */
class StackRetracer private constructor(private val retrace: StringRetrace) {

    /** Retraces messages that look like stack traces; returns other messages unchanged. */
    fun retrace(message: String): String {
        if (!looksLikeStackTrace(message = message)) return message
        val lines = message.split('\n')
        val result = retrace.retrace(lines, RetraceStackTraceContext.empty()).result
        return if (result.isEmpty()) message else result.joinToString(separator = "\n")
    }

    private fun looksLikeStackTrace(message: String): Boolean = message.contains('\n') && message.contains("at ")

    companion object {
        fun load(mappingFile: Path): StackRetracer {
            val supplier = ProguardMappingSupplier.builder()
                .setProguardMapProducer(ProguardMapProducer.fromPath(mappingFile))
                .setLoadAllDefinitions(false)
                .build()
            val retrace = StringRetrace.create(
                supplier,
                object : DiagnosticsHandler {},
                RetraceOptions.defaultRegularExpression(),
                false,
            )
            return StackRetracer(retrace = retrace)
        }
    }
}
