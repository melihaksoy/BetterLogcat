package com.melihaksoy.betterlogcat.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class StackRetracerTest {

    private val mapping = """
        com.vintagegarage.listing.ListingRepository -> a.b:
            1:1:void load(java.lang.String):58:58 -> c
        com.vintagegarage.listing.ListingViewModel -> a.d:
            1:1:void refresh():31:31 -> e
    """.trimIndent()

    @Test
    fun `retraces obfuscated frames`() {
        val file = Files.createTempFile("mapping", ".txt")
        Files.writeString(file, mapping)
        val retracer = StackRetracer.load(mappingFile = file)

        val result = retracer.retrace(
            message = "java.lang.IllegalStateException: Listing id must not be blank\n\tat a.b.c(SourceFile:1)\n\tat a.d.e(SourceFile:1)",
        )

        assertTrue(result, result.contains("com.vintagegarage.listing.ListingRepository.load(ListingRepository.java:58)"))
        assertTrue(result, result.contains("com.vintagegarage.listing.ListingViewModel.refresh(ListingViewModel.java:31)"))
        Files.delete(file)
    }

    @Test
    fun `leaves plain messages alone`() {
        val file = Files.createTempFile("mapping", ".txt")
        Files.writeString(file, mapping)
        val retracer = StackRetracer.load(mappingFile = file)

        assertEquals("Cold start finished in 412 ms", retracer.retrace(message = "Cold start finished in 412 ms"))
        Files.delete(file)
    }
}
