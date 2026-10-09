package com.melihaksoy.betterlogcat.interactor

import com.melihaksoy.betterlogcat.adb.AdbClient
import com.melihaksoy.betterlogcat.adb.AdbSession
import com.melihaksoy.betterlogcat.adb.DeviceListParser
import com.melihaksoy.betterlogcat.adb.DeviceListing
import com.melihaksoy.betterlogcat.core.Either
import com.melihaksoy.betterlogcat.core.EitherFlow
import com.melihaksoy.betterlogcat.domain.usecase.ObserveDevicesInteractor
import com.melihaksoy.betterlogcat.model.Device
import com.melihaksoy.betterlogcat.model.DeviceState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

/** One shared poll serves every tab. */
class ObserveDevicesInteractorImpl(
    private val session: AdbSession,
    scope: CoroutineScope,
) : ObserveDevicesInteractor {

    private val details = ConcurrentHashMap<String, DeviceDetails>()

    private val shared = flow<Either<Throwable, List<Device>>> {
        while (true) {
            val result = try {
                Either.Right(value = loadDevices())
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Either.Left(value = exception)
            }
            emit(result)
            delay(duration = POLL_INTERVAL)
        }
    }
        .distinctUntilChanged()
        .flowOn(context = Dispatchers.IO)
        .shareIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            replay = 1,
        )

    override fun invoke(): EitherFlow<List<Device>> = shared

    private suspend fun loadDevices(): List<Device> {
        val client = session.client()
        val listings = DeviceListParser.parse(output = client.run(args = listOf("devices", "-l")))
        return listings.map { listing ->
            val info = if (listing.state == DeviceState.ONLINE) {
                detailsFor(
                    client = client,
                    serial = listing.serial,
                )
            } else {
                DeviceDetails.EMPTY
            }
            listing.toDevice(details = info)
        }
    }

    private suspend fun detailsFor(
        client: AdbClient,
        serial: String,
    ): DeviceDetails {
        details[serial]?.let { return it }
        val output = client.run(
            args = listOf(
                "-s",
                serial,
                "shell",
                "getprop ro.product.manufacturer; getprop ro.product.model; getprop ro.build.version.release; " +
                    "getprop ro.build.version.sdk; getprop ro.boot.qemu.avd_name",
            ),
        )
        val lines = output.lines().map { it.trim() }
        val loaded = DeviceDetails(
            manufacturer = lines.getOrElse(index = 0) { "" },
            model = lines.getOrElse(index = 1) { "" },
            release = lines.getOrElse(index = 2) { "" },
            sdk = lines.getOrElse(index = 3) { "" },
            avdName = lines.getOrElse(index = 4) { "" },
        )
        if (loaded.model.isNotEmpty()) details[serial] = loaded
        return loaded
    }

    private fun DeviceListing.toDevice(details: DeviceDetails): Device = Device(
        serial = serial,
        state = state,
        name = details.displayName().ifEmpty { model }.ifEmpty { serial },
        androidVersion = details.release,
        apiLevel = details.sdk,
    )

    private data class DeviceDetails(
        val manufacturer: String,
        val model: String,
        val release: String,
        val sdk: String,
        val avdName: String,
    ) {
        fun displayName(): String = when {
            avdName.isNotEmpty() -> avdName.replace('_', ' ')
            model.isEmpty() -> ""
            manufacturer.isEmpty() || model.startsWith(manufacturer, ignoreCase = true) -> model
            else -> manufacturer.replaceFirstChar { it.titlecase(Locale.ROOT) } + " " + model
        }

        companion object {
            val EMPTY = DeviceDetails(
                manufacturer = "",
                model = "",
                release = "",
                sdk = "",
                avdName = "",
            )
        }
    }

    private companion object {
        val POLL_INTERVAL = 2.seconds
    }
}
