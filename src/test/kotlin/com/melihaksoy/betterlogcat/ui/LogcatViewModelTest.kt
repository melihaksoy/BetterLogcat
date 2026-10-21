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
import com.melihaksoy.betterlogcat.ide.LastLogStore
import com.melihaksoy.betterlogcat.ide.ProblemHistoryStore
import com.melihaksoy.betterlogcat.ide.TagNotesStore
import com.melihaksoy.betterlogcat.ide.PackageAliasStore
import com.melihaksoy.betterlogcat.ide.SourceNavigator
import com.melihaksoy.betterlogcat.model.Device
import com.melihaksoy.betterlogcat.model.DeviceState
import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
import com.melihaksoy.betterlogcat.model.SavedLog
import com.melihaksoy.betterlogcat.settings.BetterLogcatSettings
import com.melihaksoy.betterlogcat.ui.logcat.LogcatIntent
import com.melihaksoy.betterlogcat.ui.logcat.LogcatState
import com.melihaksoy.betterlogcat.ui.logcat.LogcatViewModel
import com.melihaksoy.betterlogcat.ui.logcat.model.ViewToggle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.nio.file.Files
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

/** The ViewModel with fake adb: entries go in through [batches], state comes out. */
class LogcatViewModelTest : BasePlatformTestCase() {

    private lateinit var scope: CoroutineScope
    private lateinit var viewModel: LogcatViewModel
    private val batches = Channel<List<LogEntry>>(capacity = Channel.UNLIMITED)
    private val ids = AtomicLong()
    private var clearCount = 0
    private val requests = CopyOnWriteArrayList<LogcatRequest>()
    private val lastLogStore = LastLogStore(directory = Files.createTempDirectory("last-log"))

    override fun setUp() {
        super.setUp()
        BetterLogcatSettings.getInstance().loadState(BetterLogcatSettings.SettingsState())
        // The store is a project service and the light test project is shared, so start empty.
        BookmarkStore.getInstance(project = project).loadState(state = BookmarkStore.BookmarksState())
        ProblemHistoryStore.getInstance(project = project).loadState(state = ProblemHistoryStore.HistoryState())
        scope = CoroutineScope(context = SupervisorJob() + Dispatchers.Default)
        viewModel = createViewModel(initialSerial = "")
    }

    private fun createViewModel(initialSerial: String): LogcatViewModel =
        LogcatViewModel(
            scope = scope,
            initialSerial = initialSerial,
            initialQuery = "",
            observeDevices = object : ObserveDevicesInteractor {
                override fun invoke(): EitherFlow<List<Device>> = flowOf(Either.Right(value = listOf(DEVICE)))
            },
            observeProjectAppIds = object : ObserveProjectAppIdsInteractor {
                override fun invoke(): EitherFlow<Set<String>> = flowOf(Either.Right(value = setOf(APP)))
            },
            streamLogcat = object : StreamLogcatInteractor {
                override fun invoke(request: LogcatRequest): EitherFlow<List<LogEntry>> {
                    requests += request
                    // Like the real stream: new ids start above the lines already in the log.
                    ids.accumulateAndGet(request.afterId, ::maxOf)
                    return batches.receiveAsFlow().map { Either.Right(value = it) }
                }
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
            tagNotesStore = TagNotesStore.getInstance(),
            problemHistoryStore = ProblemHistoryStore.getInstance(project = project),
            lastLogStore = lastLogStore,
        )

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

    fun `test problems stay after clear and are saved to the history`() {
        send(
            entry(tag = "AndroidRuntime", message = "FATAL EXCEPTION: main\njava.lang.IllegalStateException: boom", level = LogLevel.ERROR),
            entry(tag = "Choreographer", message = "Skipped 42 frames!", level = LogLevel.WARN),
        )
        await { it.problems.currentCount == 2 }

        viewModel.onIntent(LogcatIntent.Clear)

        val cleared = await { it.entries.isEmpty() && it.totalCount == 0 }
        assertEquals(listOf("IllegalStateException", "Skipped 42 frames!"), cleared.problems.current?.problems?.map { it.title })
        val saved = runBlocking {
            withTimeout(timeMillis = 5_000) { ProblemHistoryStore.getInstance(project = project).sessions.first { it.isNotEmpty() } }
        }
        assertEquals(2, saved.single().problems.size)
    }

    fun `test the project app starting begins a new session and can clear the log`() {
        viewModel.onIntent(
            LogcatIntent.SetViewToggle(
                toggle = ViewToggle.CLEAR_ON_APP_LAUNCH,
                isOn = true,
            ),
        )
        send(entry(tag = "Garage", message = "Low memory", level = LogLevel.WARN))
        await { it.problems.currentCount == 1 }

        send(
            entry(tag = "ActivityManager", message = "Start proc $APP_PID:$APP/u0a123 for pre-top-activity {$APP/$APP.MainActivity}"),
            entry(tag = "Garage", message = "Cold start"),
        )

        val launched = await { state -> state.problems.current?.title?.contains(APP) == true && state.entries.size == 2 }
        assertEquals(0, launched.problems.currentCount)
        assertEquals(listOf("Low memory"), launched.problems.history.single().problems.map { it.title })
    }

    fun `test the saved log shows first and the stream continues after it`() {
        scope.cancel()
        val saved = listOf(
            entry(tag = "Garage", message = "Before the restart"),
            entry(tag = "AndroidRuntime", message = "FATAL EXCEPTION: main\njava.lang.IllegalStateException: old", level = LogLevel.ERROR),
        )
        lastLogStore.save(
            log = SavedLog(
                serial = DEVICE.serial,
                deviceName = DEVICE.name,
                entries = saved,
                processNames = mapOf(APP_PID to APP),
            ),
        )
        ids.set(0)
        requests.clear()
        scope = CoroutineScope(context = SupervisorJob() + Dispatchers.Default)
        viewModel = createViewModel(initialSerial = DEVICE.serial)

        val restored = await { it.entries.size == 2 && requests.isNotEmpty() }
        assertEquals(2L, restored.previousLog?.lastId)
        assertEquals(DEVICE.name, restored.previousLog?.deviceName)
        // Its crash is in the saved history already, so the new session does not count it again.
        assertEquals(0, restored.problems.currentCount)
        val request = requests.single()
        assertEquals(2L, request.afterId)
        assertEquals(saved.last().epochSeconds * 1_000_000_000L + saved.last().nanos + 1, request.sinceEpochNanos)

        send(entry(tag = "Garage", message = "After the restart"))
        await { it.entries.size == 3 }
        viewModel.saveLastLogNow()
        val reloaded = lastLogStore.load(serial = DEVICE.serial, firstId = 1L)!!
        assertEquals(listOf("Before the restart", "FATAL EXCEPTION: main\njava.lang.IllegalStateException: old", "After the restart"), reloaded.entries.map { it.message })

        viewModel.onIntent(LogcatIntent.Clear)
        val cleared = await { it.totalCount == 0 && it.previousLog == null }
        assertEquals(0, cleared.entries.size)
        runBlocking {
            withTimeout(timeMillis = 5_000) {
                while (lastLogStore.load(serial = DEVICE.serial, firstId = 1L) != null) delay(timeMillis = 20)
            }
        }
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
