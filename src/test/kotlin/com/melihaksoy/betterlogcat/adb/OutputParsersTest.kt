package com.melihaksoy.betterlogcat.adb

import com.melihaksoy.betterlogcat.model.DeviceState
import org.junit.Assert.assertEquals
import org.junit.Test

class OutputParsersTest {

    @Test
    fun `parses adb devices -l output`() {
        val output = """
            * daemon not running; starting now at tcp:5037
            * daemon started successfully
            List of devices attached
            emulator-5554          device product:sdk_gphone64_arm64 model:sdk_gphone64_arm64 device:emu64a transport_id:1
            38211FDJH00A4K         unauthorized usb:1-1 transport_id:2
            192.168.1.20:5555      offline transport_id:3
        """.trimIndent()

        val devices = DeviceListParser.parse(output = output)

        assertEquals(listOf("emulator-5554", "38211FDJH00A4K", "192.168.1.20:5555"), devices.map { it.serial })
        assertEquals(listOf(DeviceState.ONLINE, DeviceState.UNAUTHORIZED, DeviceState.OFFLINE), devices.map { it.state })
        assertEquals("sdk gphone64 arm64", devices.first().model)
    }

    @Test
    fun `parses toybox ps output and marks app processes`() {
        val output = """
              PID USER         NAME
                1 root         init
              449 system       android.hardware.gatekeeper-service.nonsecure
            14302 u0_a215      com.vintagegarage.app
            14388 u0_a215      com.vintagegarage.app:sync
        """.trimIndent()

        val processes = ProcessListParser.parse(output = output)

        assertEquals("com.vintagegarage.app", processes[14302]?.name)
        assertEquals("com.vintagegarage.app:sync", processes[14388]?.name)
        assertEquals(true, processes[14302]?.isApp)
        assertEquals(false, processes[449]?.isApp)
        assertEquals(false, processes[1]?.isApp)
    }

    @Test
    fun `parses old toolbox ps output`() {
        val output = """
            USER     PID   PPID  VSIZE  RSS     WCHAN    PC         NAME
            root      1     0     8904   784   ffffffff 00000000 S /init
            u0_a52    1830  176   901232 41284 ffffffff 00000000 S com.vintagegarage.app
        """.trimIndent()

        val names = ProcessListParser.parse(output = output)

        assertEquals("com.vintagegarage.app", names[1830]?.name)
        assertEquals(true, names[1830]?.isApp)
    }
}
