package com.melihaksoy.betterlogcat.ide

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.melihaksoy.betterlogcat.adb.AdbLocator
import com.melihaksoy.betterlogcat.format.NativeBacktrace
import com.melihaksoy.betterlogcat.format.NativeFrame
import java.io.File
import java.util.concurrent.TimeUnit

/** The result of symbolizing: readable text, or why it could not run. */
sealed interface SymbolizeResult {
    data class Done(val text: String) : SymbolizeResult
    data object NoBacktrace : SymbolizeResult
    data object NoSymbolizer : SymbolizeResult
    data class NoLibraries(val names: List<String>) : SymbolizeResult
}

/**
 * Turns a native backtrace into function names and source lines, like `ndk-stack`: the NDK's
 * llvm-symbolizer reads the unstripped `.so` files that the project's build left (CMake `obj` and
 * `merged_native_libs` folders). Blocking: run it off the EDT.
 */
class NativeSymbolizer(private val project: Project) {

    fun symbolize(backtrace: List<String>): SymbolizeResult {
        val frames = NativeBacktrace.frames(lines = backtrace)
        if (frames.isEmpty()) return SymbolizeResult.NoBacktrace
        val symbolizer = findSymbolizer() ?: return SymbolizeResult.NoSymbolizer
        val abi = NativeBacktrace.abiFolder(lines = backtrace)
        val names = frames.map { it.libraryName }.filter { it.endsWith(suffix = ".so") }.toSet()
        val libraries = findLibraries(
            names = names,
            abi = abi,
        )
        if (libraries.isEmpty()) return SymbolizeResult.NoLibraries(names = names.sorted())
        val resolved = frames.groupBy { it.libraryName }.flatMap { (name, libraryFrames) ->
            val library = libraries[name] ?: return@flatMap emptyList()
            libraryFrames.zip(
                other = run(
                    symbolizer = symbolizer,
                    library = library,
                    frames = libraryFrames,
                ),
            )
        }.toMap()
        val text = buildString {
            frames.forEach { frame ->
                append(frame.line).append('\n')
                resolved[frame]?.forEach { append("        ").append(it).append('\n') }
            }
            append('\n')
            libraries.values.forEach { append("# ").append(it.path).append('\n') }
        }
        return SymbolizeResult.Done(text = text)
    }

    /** One `function (file:line)` list per frame, inlined calls first, as llvm-symbolizer prints them. */
    private fun run(
        symbolizer: File,
        library: File,
        frames: List<NativeFrame>,
    ): List<List<String>> {
        val process = ProcessBuilder(
            listOf(symbolizer.path, "--obj=${library.path}", "--inlining", "--pretty-print") + frames.map { "0x${it.pc}" },
        )
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        if (!process.waitFor(SYMBOLIZE_SECONDS, TimeUnit.SECONDS)) process.destroyForcibly()
        // One block per address, split by a blank line.
        val blocks = output.trim().split(Regex(pattern = """\n\s*\n""")).map { block -> block.lines().filter { it.isNotBlank() }.map { it.trim() } }
        return frames.indices.map { blocks.getOrElse(index = it) { emptyList() } }
    }

    private fun findSymbolizer(): File? {
        val name = if (SystemInfo.isWindows) "llvm-symbolizer.exe" else "llvm-symbolizer"
        return AdbLocator(project = project).sdkRoots()
            .flatMap { sdk -> (File(sdk, "ndk").listFiles()?.toList().orEmpty() + File(sdk, "ndk-bundle")) }
            .filter { it.isDirectory }
            .sortedByDescending { it.name }
            .flatMap { ndk -> File(ndk, "toolchains/llvm/prebuilt").listFiles()?.toList().orEmpty() }
            .map { File(it, "bin/$name") }
            .firstOrNull { it.isFile && it.canExecute() }
    }

    /** Unstripped copies of [names] in the project's build folders, newest first, for [abi] when known. */
    private fun findLibraries(
        names: Set<String>,
        abi: String?,
    ): Map<String, File> {
        val base = project.basePath?.let { File(it) } ?: return emptyMap()
        val found = base.walkTopDown()
            .onEnter { it.name !in SKIPPED_FOLDERS }
            .maxDepth(depth = MAX_DEPTH)
            .filter { it.isFile && it.name in names }
            .filter { file ->
                val path = file.invariantSeparatorsPath
                "stripped_native_libs" !in path && (abi == null || "/$abi/" in path) && UNSTRIPPED.any { it in path }
            }
            .toList()
        return found.groupBy { it.name }.mapValues { (_, files) -> files.maxBy { it.lastModified() } }
    }

    private companion object {
        const val SYMBOLIZE_SECONDS = 30L
        const val MAX_DEPTH = 14
        val SKIPPED_FOLDERS = setOf(".git", ".gradle", ".idea", "node_modules", ".kotlin")
        val UNSTRIPPED = listOf("/obj/", "merged_native_libs", "/cxx/", "/intermediates/")
    }
}
