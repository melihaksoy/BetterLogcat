package com.melihaksoy.betterlogcat.format

/** One frame of a tombstone backtrace: `#00 pc 000000000004f3c8  /data/app/…/lib/arm64/libgame.so (update+24)`. */
data class NativeFrame(
    val index: Int,
    /** Offset inside the library, in hex without 0x. */
    val pc: String,
    val libraryPath: String,
    val symbol: String,
    val line: String,
) {
    val libraryName: String
        get() = libraryPath.substringAfterLast(delimiter = '/')
}

/** Reads the frames and the ABI of a native crash dump, as crash_dump writes it to logcat with tag DEBUG. */
object NativeBacktrace {

    fun frames(lines: List<String>): List<NativeFrame> = lines.mapNotNull { line ->
        val match = FRAME.find(input = line) ?: return@mapNotNull null
        NativeFrame(
            index = match.groupValues[1].toInt(),
            pc = match.groupValues[2],
            libraryPath = match.groupValues[3],
            symbol = match.groupValues[4],
            line = line.trim(),
        )
    }

    /** The ABI folder name of the crash, like "arm64-v8a" for `ABI: 'arm64'`. Null when the dump has no ABI line. */
    fun abiFolder(lines: List<String>): String? {
        val abi = lines.firstNotNullOfOrNull { ABI.find(input = it)?.groupValues?.get(1) } ?: return null
        return when (abi) {
            "arm64" -> "arm64-v8a"
            "arm" -> "armeabi-v7a"
            else -> abi
        }
    }

    private val FRAME = Regex(pattern = """#(\d+)\s+pc\s+([0-9a-fA-F]+)\s+(\S+)(?:\s+\(([^)]*)\))?""")
    private val ABI = Regex(pattern = """ABI: '([\w-]+)'""")
}
