package com.melihaksoy.betterlogcat.ui

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.melihaksoy.betterlogcat.core.Either
import com.melihaksoy.betterlogcat.core.EitherFlow
import com.melihaksoy.betterlogcat.domain.usecase.ClearDeviceLogInteractor
import com.melihaksoy.betterlogcat.domain.usecase.LogcatRequest
import com.melihaksoy.betterlogcat.domain.usecase.ObserveDevicesInteractor
import com.melihaksoy.betterlogcat.domain.usecase.ObserveProcessesInteractor
import com.melihaksoy.betterlogcat.domain.usecase.ObserveProjectAppIdsInteractor
import com.melihaksoy.betterlogcat.domain.usecase.ProcessSnapshot
import com.melihaksoy.betterlogcat.domain.usecase.StreamLogcatInteractor
import com.melihaksoy.betterlogcat.ide.AppearanceStore
import com.melihaksoy.betterlogcat.ide.BookmarkStore
import com.melihaksoy.betterlogcat.ide.PackageAliasStore
import com.melihaksoy.betterlogcat.ide.SourceNavigator
import com.melihaksoy.betterlogcat.model.Device
import com.melihaksoy.betterlogcat.model.DeviceState
import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
import com.melihaksoy.betterlogcat.settings.BetterLogcatSettings
import com.melihaksoy.betterlogcat.ui.logcat.LogcatIntent
import com.melihaksoy.betterlogcat.ui.logcat.LogcatState
import com.melihaksoy.betterlogcat.ui.logcat.LogcatViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicLong

/** The ViewModel with fake adb: entries go in through [batches], state comes out. */
class LogcatViewModelTest : BasePlatformTestCase() {

    private lateinit var scope: CoroutineScope
    private lateinit var viewModel: LogcatViewModel
    private val batches = Channel<List<LogEntry>>(capacity = Channel.UNLIMITED)
    private val ids = AtomicLong()
    private var clearCount = 0

    override fun setUp() {
        super.setUp()
        BetterLogcatSettings.getInstance().loadState(BetterLogcatSettings.SettingsState())
        // The store is a project service and the light test project is shared, so start empty.
        BookmarkStore.getInstance(project = project).loadState(state = BookmarkStore.BookmarksState())
        scope = CoroutineScope(context = SupervisorJob() + Dispatchers.Default)
        viewModel = LogcatViewModel(
            scope = scope,
            initialSerial = "",
            initialQuery = "",
            observeDevices = object : ObserveDevicesInteractor {
                override fun invoke(): EitherFlow<List<Device>> = flowOf(Either.Right(value = listOf(DEVICE)))
            },
            observeProjectAppIds = object : ObserveProjectAppIdsInteractor {
                override fun invoke(): EitherFlow<Set<String>> = flowOf(Either.Right(value = setOf(APP)))
            },
            streamLogcat = object : StreamLogcatInteractor {
                override fun invoke(request: LogcatRequest): EitherFlow<List<LogEntry>> = batches.receiveAsFlow().map { Either.Right(value = it) }
            },
            observeProcesses = object : ObserveProcessesInteractor {
                override fun invoke(
                    serial: String,
                    refreshRequests: Flow<Unit>,
                ): EitherFlow<ProcessSnapshot> = flowOf(
                    Either.Right(
                        value = ProcessSnapshot(
                            names = mapOf(APP_PID to APP, SYSTEM_PID to "system_server"),
                            appPackages = setOf(APP),
                        ),
                    ),
                )
            },
            clearDeviceLog = object : ClearDeviceLogInteractor {
                override fun invoke(serial: String): EitherFlow<Unit> = flowOf(Either.Right(value = Unit)).also { clearCount++ }
            },
            sourceNavigator = SourceNavigator(project = project),
            settings = BetterLogcatSettings.getInstance(),
            aliasStore = PackageAliasStore.getInstance(),
            appearanceStore = AppearanceStore.getInstance(),
            bookmarkStore = BookmarkStore.getInstance(project = project),
        )
    }

    override fun tearDown() {
        try {
            scope.cancel()
        } finally {
            super.tearDown()
        }
    }

    fun `test entries stream into the visible list`() {
        send(entry(tag = "Garage", message = "Cold start"), entry(tag = "OkHttp", message = "--> GET https://x.com/a"))

        val state = await { it.entries.size == 2 }
        assertEquals(listOf("Garage", "OkHttp"), state.entries.map { it.tag })
        assertEquals(DEVICE.serial, state.selectedSerial)
    }

    fun `test search terms narrow the list`() {
        send(entry(tag = "Garage", message = "Cold start"), entry(tag = "OkHttp", message = "--> GET https://x.com/a"))
        await { it.entries.size == 2 }

        viewModel.onIntent(LogcatIntent.ChangeQuery(query = "tag:OkHttp "))

        assertEquals(listOf("OkHttp"), await { it.entries.size == 1 }.entries.map { it.tag })
    }

    fun `test pause holds new entries until resume`() {
        send(entry(tag = "Garage", message = "one"))
        await { it.entries.size == 1 }

        viewModel.onIntent(LogcatIntent.TogglePause)
        send(entry(tag = "Garage", message = "two"), entry(tag = "Garage", message = "three"))
        val paused = await { it.pausedNewCount == 2 }
        assertEquals(1, paused.entries.size)

        viewModel.onIntent(LogcatIntent.TogglePause)
        assertEquals(3, await { it.entries.size == 3 }.entries.size)
    }

    fun `test crashes are counted and jumped to`() {
        send(
            entry(tag = "Garage", message = "fine"),
            entry(tag = "AndroidRuntime", message = "FATAL EXCEPTION: main", level = LogLevel.ERROR),
        )
        val state = await { it.crashCount == 1 && it.entries.size == 2 }

        viewModel.onIntent(LogcatIntent.JumpToCrash)

        val crashId = state.entries.last().id
        assertEquals(crashId, await { it.selection.leadId == crashId }.selection.leadId)
    }

    fun `test network calls pair up`() {
        send(
            entry(tag = "Ktor Client", message = "REQUEST: https://x.com/v1/items\nMETHOD: GET"),
            entry(tag = "Ktor Client", message = "RESPONSE: 200\nMETHOD: GET\nFROM: https://x.com/v1/items"),
        )

        val call = await { state -> state.networkCalls.any { !it.isPending } }.networkCalls.single()
        assertEquals(200, call.statusCode)
    }

    fun `test clear empties the list and the counters`() {
        send(entry(tag = "AndroidRuntime", message = "FATAL EXCEPTION: main", level = LogLevel.ERROR))
        await { it.crashCount == 1 && it.entries.isNotEmpty() }

        viewModel.onIntent(LogcatIntent.Clear)

        val state = await { it.entries.isEmpty() && it.crashCount == 0 }
        assertEquals(0, state.totalCount)
    }

    fun `test bookmarks survive clear and find their line again`() {
        val line = entry(tag = "AndroidRuntime", message = "FATAL EXCEPTION: main", level = LogLevel.ERROR)
        send(line)
        await { it.entries.size == 1 }

        viewModel.onIntent(LogcatIntent.ToggleBookmark(id = line.id))
        await { it.bookmarkedIds == setOf(line.id) && it.bookmarks.single().isInLog }

        viewModel.onIntent(LogcatIntent.Clear)
        val cleared = await { it.entries.isEmpty() && it.bookmarks.size == 1 && !it.bookmarks.single().isInLog }
        assertEquals("FATAL EXCEPTION: main", cleared.bookmarks.single().bookmark.message)

        // The same line read again (new session, new id) is found by its time, process, thread and tag.
        val again = LogEntry(
            id = 1_000,
            epochSeconds = line.epochSeconds,
            nanos = line.nanos,
            pid = line.pid,
            tid = line.tid,
            level = line.level,
            tag = line.tag,
            message = line.message,
        )
        send(again)
        assertEquals(1_000L, await { it.bookmarks.singleOrNull()?.liveId == 1_000L }.bookmarkedIds.single())
    }

    private fun send(vararg entries: LogEntry) {
        batches.trySend(element = entries.toList())
    }

    private fun await(predicate: (LogcatState) -> Boolean): LogcatState = runBlocking {
        withTimeout(timeMillis = 5_000) { viewModel.state.first(predicate) }
    }

    private fun entry(
        tag: String,
        message: String,
        level: LogLevel = LogLevel.INFO,
    ) = LogEntry(
        id = ids.incrementAndGet(),
        epochSeconds = 1_791_540_000L,
        nanos = ids.get().toInt(),
        pid = APP_PID,
        tid = APP_PID,
        level = level,
        tag = tag,
        message = message,
    )

    private companion object {
        const val APP = "com.example.app"
        const val APP_PID = 14_302
        const val SYSTEM_PID = 1_822
        val DEVICE = Device(
            serial = "emulator-5554",
            state = DeviceState.ONLINE,
            name = "Pixel 10 Pro XL",
            androidVersion = "17",
            apiLevel = "37",
        )
    }
}
