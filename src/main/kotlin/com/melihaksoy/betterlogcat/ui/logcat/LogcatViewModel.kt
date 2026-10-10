package com.melihaksoy.betterlogcat.ui.logcat

import com.intellij.openapi.ide.CopyPasteManager
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.adb.AdbNotFoundException
import com.melihaksoy.betterlogcat.core.mapEither
import com.melihaksoy.betterlogcat.domain.usecase.ClearDeviceLogInteractor
import com.melihaksoy.betterlogcat.domain.usecase.LogcatRequest
import com.melihaksoy.betterlogcat.domain.usecase.ObserveDevicesInteractor
import com.melihaksoy.betterlogcat.domain.usecase.ObserveProcessesInteractor
import com.melihaksoy.betterlogcat.domain.usecase.ObserveProjectAppIdsInteractor
import com.melihaksoy.betterlogcat.domain.usecase.ProcessSnapshot
import com.melihaksoy.betterlogcat.domain.usecase.StreamLogcatInteractor
import com.melihaksoy.betterlogcat.filter.LogBuffer
import com.melihaksoy.betterlogcat.filter.LogFilter
import com.melihaksoy.betterlogcat.filter.LogFilterMatcher
import com.melihaksoy.betterlogcat.filter.PackageFilter
import com.melihaksoy.betterlogcat.filter.QueryKey
import com.melihaksoy.betterlogcat.filter.QueryParser
import com.melihaksoy.betterlogcat.filter.QueryTerm
import com.melihaksoy.betterlogcat.filter.TagVisibility
import com.melihaksoy.betterlogcat.format.CopyMode
import com.melihaksoy.betterlogcat.format.LogCopyFormatter
import com.melihaksoy.betterlogcat.format.StackFrame
import com.melihaksoy.betterlogcat.format.StackRetracer
import com.melihaksoy.betterlogcat.ide.AppearanceStore
import com.melihaksoy.betterlogcat.ide.BookmarkStore
import com.melihaksoy.betterlogcat.ide.PackageAliasStore
import com.melihaksoy.betterlogcat.ide.SourceNavigator
import com.melihaksoy.betterlogcat.model.Bookmark
import com.melihaksoy.betterlogcat.model.BookmarkKey
import com.melihaksoy.betterlogcat.model.Device
import com.melihaksoy.betterlogcat.model.DeviceState
import com.melihaksoy.betterlogcat.model.LogColumn
import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
import com.melihaksoy.betterlogcat.model.TimeFormat
import com.melihaksoy.betterlogcat.network.NetworkCall
import com.melihaksoy.betterlogcat.network.NetworkTracker
import com.melihaksoy.betterlogcat.settings.BetterLogcatSettings
import com.melihaksoy.betterlogcat.ui.logcat.model.BookmarkUiModel
import com.melihaksoy.betterlogcat.ui.logcat.model.LogcatContentUiModel
import com.melihaksoy.betterlogcat.ui.logcat.model.NetworkDetailUiModel
import com.melihaksoy.betterlogcat.ui.logcat.model.SavedFiltersUiModel
import com.melihaksoy.betterlogcat.ui.logcat.model.SelectionUiModel
import com.melihaksoy.betterlogcat.ui.logcat.model.SidePanel
import com.melihaksoy.betterlogcat.ui.logcat.model.ViewOptionsUiModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.datatransfer.StringSelection
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import com.melihaksoy.betterlogcat.ide.TagNotesStore

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class LogcatViewModel(
    private val scope: CoroutineScope,
    private val initialSerial: String,
    /** Filter query to start with. Null restores the last filter the user had. */
    private val initialQuery: String?,
    private val observeDevices: ObserveDevicesInteractor,
    private val observeProjectAppIds: ObserveProjectAppIdsInteractor,
    private val streamLogcat: StreamLogcatInteractor,
    private val observeProcesses: ObserveProcessesInteractor,
    private val clearDeviceLog: ClearDeviceLogInteractor,
    private val sourceNavigator: SourceNavigator,
    private val settings: BetterLogcatSettings,
    private val aliasStore: PackageAliasStore,
    private val appearanceStore: AppearanceStore,
    private val bookmarkStore: BookmarkStore,
    private val tagNotesStore: TagNotesStore,
) {
    private val _state = MutableStateFlow(value = initialState())
    val state: StateFlow<LogcatState> = _state.asStateFlow()

    private val _events = Channel<LogcatEvent>(capacity = Channel.BUFFERED)
    val events: Flow<LogcatEvent> = _events.receiveAsFlow()

    /** All buffer work runs on this one-thread view of Default, so [buffer] needs no locks. */
    private val bufferDispatcher = Dispatchers.Default.limitedParallelism(parallelism = 1)

    /** Every coroutine of this class runs here, so stream jobs and the buffer never race. */
    private val workScope = CoroutineScope(context = scope.coroutineContext + bufferDispatcher)
    private val buffer = LogBuffer(capacity = settings.state.maxEntries)
    private var matcher = LogFilterMatcher(
        filter = _state.value.filter,
        processNames = emptyMap(),
    )
    private var isDirty = false
    private val requestedPids = HashSet<Int>()

    private val processRefresh = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val filterChanges = MutableStateFlow(value = _state.value.filter)

    private var streamJob: Job? = null
    private var processJob: Job? = null
    private var streamingSerial = ""
    private var streamGeneration = 0
    private var retracer: StackRetracer? = null
    private val networkTracker = NetworkTracker()
    private var isNetworkDirty = false
    private var lastRatesPublish = 0L

    /** The project's bookmarks, and the id of each one's line in this buffer. Buffer thread only. */
    private var bookmarks: List<Bookmark> = emptyList()
    private var bookmarkKeys: Set<BookmarkKey> = emptySet()
    private val liveBookmarkIds = HashMap<BookmarkKey, Long>()

    init {
        buffer.refilter(predicate = matcher::matches)
        observeDevices()
            .mapEither(
                onLeft = ::onDevicesError,
                onRight = ::onDevicesChanged,
            )
            .launchIn(scope = workScope)
        aliasStore.aliases
            .onEach { aliases -> _state.update { it.copy(packageAliases = aliases) } }
            .launchIn(scope = workScope)
        tagNotesStore.notes
            .onEach { notes -> _state.update { it.copy(tagNotes = notes) } }
            .launchIn(scope = workScope)
        tagNotesStore.groups
            .onEach { groups -> _state.update { it.copy(tagGroups = groups) } }
            .launchIn(scope = workScope)
        appearanceStore.levelColors
            .onEach { colors -> _state.update { it.copy(levelColors = colors) } }
            .launchIn(scope = workScope)
        appearanceStore.highlightRules
            .onEach { rules -> _state.update { it.copy(highlightRules = rules) } }
            .launchIn(scope = workScope)
        bookmarkStore.bookmarks
            .onEach(::onBookmarksChanged)
            .launchIn(scope = workScope)
        observeProjectAppIds()
            .mapEither(
                onLeft = {},
                onRight = ::onProjectAppIds,
            )
            .launchIn(scope = workScope)
        filterChanges
            .drop(count = 1)
            .debounce(timeout = FILTER_DEBOUNCE)
            .onEach { withContext(context = bufferDispatcher) { refilter() } }
            .launchIn(scope = workScope)
        // A filter that stays unchanged for a few seconds counts as used and goes into the history.
        filterChanges
            .drop(count = 1)
            .map { it.toQueryString() }
            .debounce(timeout = HISTORY_DEBOUNCE)
            .filter { it.isNotBlank() }
            .distinctUntilChanged()
            .onEach(::addToHistory)
            .launchIn(scope = workScope)
        workScope.launch { publishLoop() }
        workScope.launch { ageRefreshLoop() }
    }

    fun onIntent(intent: LogcatIntent) {
        when (intent) {
            is LogcatIntent.SelectDevice -> selectDevice(serial = intent.serial)
            is LogcatIntent.ChangeQuery -> updateFilter { it.copy(terms = emptyList(), query = intent.query) }
            LogcatIntent.RefreshSavedFilters -> refreshSavedFilters()
            LogcatIntent.ResetViewAfterCrash -> updateViewOptions { it.copy(sidePanel = SidePanel.NONE) }
            is LogcatIntent.ResizeSidePanel -> updateViewOptions {
                when (intent.panel) {
                    SidePanel.TAGS -> it.copy(tagsPanelWidth = intent.widthDp)
                    SidePanel.NETWORK -> it.copy(networkPanelWidth = intent.widthDp)
                    SidePanel.BOOKMARKS -> it.copy(bookmarksPanelWidth = intent.widthDp)
                    SidePanel.NONE -> it
                }
            }
            is LogcatIntent.ResizeNetworkDetail -> updateViewOptions { it.copy(networkDetailHeight = intent.heightDp) }
            is LogcatIntent.SelectNetworkCall -> workScope.launch { showNetworkDetail(call = intent.call) }
            is LogcatIntent.ToggleBookmark -> workScope.launch { toggleBookmark(id = intent.id) }
            is LogcatIntent.RemoveBookmark -> bookmarkStore.remove(key = intent.key)
            is LogcatIntent.JumpToBookmark -> jumpToBookmark(delta = intent.delta)
            LogcatIntent.JumpToCrash -> jumpToCrash()
            is LogcatIntent.JumpToEntry -> jumpToEntry(id = intent.id)
            is LogcatIntent.CopyEntry -> copyEntries(
                entries = _state.value.entries.filter { it.id == intent.id },
                mode = intent.mode,
            )
            is LogcatIntent.SetLevelColor -> appearanceStore.setLevelColor(
                level = intent.level,
                colorArgb = intent.colorArgb,
            )
            is LogcatIntent.AddHighlightRule -> appearanceStore.addRule(rule = intent.rule)
            is LogcatIntent.RemoveHighlightRule -> appearanceStore.removeRule(rule = intent.rule)
            is LogcatIntent.SetPackageAlias -> aliasStore.set(
                packageName = intent.packageName,
                alias = intent.alias,
            )
            is LogcatIntent.ApplySavedFilter -> updateFilter { it.withQueryString(text = intent.query) }
            is LogcatIntent.ToggleFavoriteFilter -> toggleFavorite(query = intent.query)
            is LogcatIntent.RemoveFromHistory -> removeFromHistory(query = intent.query)
            is LogcatIntent.SetRegex -> setRegex(isRegex = intent.isRegex)
            is LogcatIntent.SetMatchCase -> setMatchCase(isMatchCase = intent.isMatchCase)
            is LogcatIntent.ToggleLevel -> toggleLevel(level = intent.level)
            is LogcatIntent.SelectPackage -> updateFilter { it.withPackageFilter(packageFilter = intent.packageFilter) }
            is LogcatIntent.ShowOnlyTag -> showOnlyTag(tag = intent.tag)
            is LogcatIntent.ShowOnlyTags -> updateFilter {
                TagVisibility.showOnly(
                    filter = it,
                    tags = intent.tags,
                )
            }
            is LogcatIntent.SetTagNote -> tagNotesStore.setNote(
                tag = intent.tag,
                note = intent.note,
            )
            is LogcatIntent.AddTagToGroup -> tagNotesStore.addToGroup(
                tag = intent.tag,
                groupName = intent.groupName,
            )
            is LogcatIntent.RemoveTagFromGroup -> tagNotesStore.removeFromGroup(tag = intent.tag)
            is LogcatIntent.RenameTagGroup -> tagNotesStore.renameGroup(
                from = intent.from,
                to = intent.to,
            )
            is LogcatIntent.DeleteTagGroup -> tagNotesStore.deleteGroup(name = intent.name)
            is LogcatIntent.HideTag -> hideTag(tag = intent.tag)
            is LogcatIntent.SetTagsVisible -> setTagsVisible(
                tags = intent.tags,
                isVisible = intent.isVisible,
            )
            LogcatIntent.ShowAllTags -> updateFilter { TagVisibility.showAll(filter = it) }
            is LogcatIntent.ToggleSidePanel -> updateViewOptions {
                it.copy(sidePanel = if (it.sidePanel == intent.panel) SidePanel.NONE else intent.panel)
            }
            LogcatIntent.ResetFilters -> resetFilters()
            LogcatIntent.TogglePause -> togglePause()
            LogcatIntent.Clear -> clear()
            is LogcatIntent.SetViewToggle -> updateViewOptions {
                it.with(
                    toggle = intent.toggle,
                    isOn = intent.isOn,
                )
            }
            is LogcatIntent.SetTimeFormat -> setTimeFormat(format = intent.format)
            is LogcatIntent.ToggleColumn -> toggleColumn(column = intent.column)
            is LogcatIntent.ResizeColumn -> resizeColumn(
                column = intent.column,
                widthDp = intent.widthDp,
            )
            is LogcatIntent.ToggleExpanded -> toggleExpanded(id = intent.id)
            is LogcatIntent.SetSelectionExpanded -> setSelectionExpanded(isExpanded = intent.isExpanded)
            LogcatIntent.ExpandAll -> setExpandByDefault(isExpanded = true)
            LogcatIntent.CollapseAll -> setExpandByDefault(isExpanded = false)
            is LogcatIntent.PressRow -> pressRow(intent = intent)
            is LogcatIntent.DragSelect -> selectRange(
                anchorId = intent.anchorId,
                leadId = intent.currentId,
            )
            is LogcatIntent.MoveSelection -> moveSelection(
                delta = intent.delta,
                isExtend = intent.isExtend,
            )
            LogcatIntent.SelectAll -> selectAll()
            LogcatIntent.ClearSelection -> _state.update { it.copy(selection = SelectionUiModel()) }
            is LogcatIntent.Copy -> copy(mode = intent.mode)
            LogcatIntent.Export -> requestExport()
            is LogcatIntent.ExportTo -> exportTo(file = intent.file)
            is LogcatIntent.OpenFrame -> openFrame(frame = intent.frame)
            LogcatIntent.ChooseMapping -> _events.trySend(element = LogcatEvent.ShowMappingChooser)
            is LogcatIntent.LoadMapping -> loadMapping(file = intent.file)
            LogcatIntent.RemoveMapping -> removeMapping()
        }
    }

    // region Devices and streaming

    private suspend fun onDevicesError(throwable: Throwable) {
        val content = if (throwable is AdbNotFoundException) LogcatContentUiModel.AdbNotFound else LogcatContentUiModel.NoDevices
        _state.update { current ->
            if (current.totalCount > 0) {
                current.copy(content = LogcatContentUiModel.Logs(isDeviceConnected = false))
            } else {
                current.copy(content = content)
            }
        }
    }

    private suspend fun onDevicesChanged(devices: List<Device>) {
        val current = _state.value
        val selected = devices.firstOrNull { it.serial == current.selectedSerial }
        val serial = when {
            selected != null -> selected.serial
            current.totalCount == 0 -> devices.firstOrNull { it.state == DeviceState.ONLINE }?.serial
                ?: devices.firstOrNull()?.serial.orEmpty()
            else -> current.selectedSerial
        }
        if (serial != current.selectedSerial && current.selectedSerial.isNotEmpty() && current.totalCount == 0) {
            stopStreaming()
        }
        _state.update { it.copy(devices = devices, selectedSerial = serial) }
        val device = devices.firstOrNull { it.serial == serial }
        if (device?.state == DeviceState.ONLINE && streamJob?.isActive != true) {
            startStreaming(serial = serial)
        }
        refreshContent()
    }

    private fun selectDevice(serial: String) {
        if (serial == _state.value.selectedSerial) return
        workScope.launch {
            stopStreaming()
            buffer.clear()
            requestedPids.clear()
            _state.update {
                it.copy(
                    selectedSerial = serial,
                    entries = emptyList(),
                    totalCount = 0,
                    processNames = emptyMap(),
                    packages = emptyList(),
                    selection = SelectionUiModel(),
                    toggledIds = emptySet(),
                    pausedNewCount = 0,
                )
            }
            val device = _state.value.devices.firstOrNull { it.serial == serial }
            if (device?.state == DeviceState.ONLINE) startStreaming(serial = serial)
            refreshContent()
        }
    }

    private suspend fun startStreaming(serial: String) {
        stopStreaming()
        streamingSerial = serial
        val generation = ++streamGeneration
        val since = withContext(context = bufferDispatcher) {
            val last = buffer.lastEntry
            when {
                last != null -> last.epochSeconds * NANOS_PER_SECOND + last.nanos + 1
                settings.state.loadExistingBuffer -> 0L
                else -> -1L
            }
        }
        streamJob = streamLogcat(request = LogcatRequest(serial = serial, sinceEpochNanos = since))
            .mapEither(
                onLeft = { refreshContent() },
                onRight = { batch -> withContext(context = bufferDispatcher) { onBatch(received = batch) } },
            )
            .onCompletion {
                if (generation == streamGeneration) streamingSerial = ""
                refreshContent()
            }
            .launchIn(scope = workScope)
        processJob = observeProcesses(
            serial = serial,
            refreshRequests = processRefresh,
        )
            .mapEither(
                onLeft = {},
                onRight = { snapshot -> withContext(context = bufferDispatcher) { onProcesses(snapshot = snapshot) } },
            )
            .launchIn(scope = workScope)
        refreshContent()
    }

    private fun stopStreaming() {
        streamJob?.cancel()
        processJob?.cancel()
        streamJob = null
        processJob = null
        streamingSerial = ""
    }

    private fun refreshContent() {
        _state.update { current ->
            val device = current.selectedDevice
            val isStreaming = streamJob?.isActive == true && streamingSerial == current.selectedSerial
            val content = when {
                current.totalCount > 0 || isStreaming -> LogcatContentUiModel.Logs(isDeviceConnected = isStreaming)
                device == null && current.devices.isEmpty() -> LogcatContentUiModel.NoDevices
                device == null -> LogcatContentUiModel.Loading
                device.state != DeviceState.ONLINE -> LogcatContentUiModel.DeviceNotReady(
                    deviceName = device.name,
                    state = device.state,
                )
                else -> LogcatContentUiModel.Loading
            }
            current.copy(content = content)
        }
    }

    // endregion

    // region Buffer

    private fun onBatch(received: List<LogEntry>) {
        val batch = retracer?.let { current -> received.map { it.withMessage(message = current.retrace(message = it.message)) } } ?: received
        buffer.append(entries = batch)
        isDirty = true
        if (bookmarkKeys.isNotEmpty()) {
            var isFound = false
            for (entry in batch) {
                val key = BookmarkKey.of(entry = entry)
                if (key in bookmarkKeys) {
                    liveBookmarkIds[key] = entry.id
                    isFound = true
                }
            }
            if (isFound) publishBookmarks()
        }
        var crashes = 0
        for (entry in batch) {
            if (networkTracker.onEntry(entry = entry)) isNetworkDirty = true
            if (entry.isCrash) crashes++
        }
        if (crashes > 0) _state.update { it.copy(crashCount = it.crashCount + crashes) }
        val names = _state.value.processNames
        for (entry in batch) {
            if (entry.pid !in names && requestedPids.add(entry.pid)) {
                processRefresh.tryEmit(Unit)
                break
            }
        }
        if (_state.value.isPaused) {
            _state.update { it.copy(pausedNewCount = it.pausedNewCount + batch.size) }
        }
    }

    private fun onProcesses(snapshot: ProcessSnapshot) {
        val current = _state.value
        val packages = snapshot.appPackages.toList()
        if (snapshot.names == current.processNames && packages == current.packages) return
        _state.update {
            it.copy(
                processNames = snapshot.names,
                packages = packages,
            )
        }
        val usesProcessNames = current.filter.effective().terms.any { it.key == QueryKey.PACKAGE || it.key == QueryKey.PROCESS }
        if (usesProcessNames) refilter() else rebuildMatcher()
    }

    private fun onProjectAppIds(ids: Set<String>) {
        if (ids == _state.value.projectAppIds) return
        _state.update { it.copy(projectAppIds = ids) }
        if (QueryTerm.Mine in _state.value.filter.effective().terms) refilter() else rebuildMatcher()
    }

    private suspend fun publishLoop() {
        while (true) {
            delay(duration = PUBLISH_INTERVAL)
            if (isDirty && !_state.value.isPaused) publish()
        }
    }

    private fun publish() {
        isDirty = false
        // Lines trimmed from the buffer are no longer in the log.
        if (liveBookmarkIds.values.removeIf { buffer.find(id = it) == null }) publishBookmarks()
        val entries = buffer.visibleSnapshot()
        val tagCounts = buffer.tagCountsSnapshot()
        val tagPids = buffer.tagPidsSnapshot()
        val calls = if (isNetworkDirty) networkTracker.snapshot() else null
        isNetworkDirty = false
        // A pending call in the details pane gets its response as soon as it arrives.
        val openDetail = _state.value.networkDetail
        if (calls != null && openDetail != null) {
            calls.firstOrNull { it.requestEntryId == openDetail.call.requestEntryId && it.requestEpochMillis == openDetail.call.requestEpochMillis }
                ?.takeIf { it != openDetail.call }
                ?.let { showNetworkDetail(call = it) }
        }
        // Rates change every second at most; refresh them once a second.
        val now = System.currentTimeMillis()
        val rates = if (now - lastRatesPublish >= RATES_INTERVAL_MILLIS) buffer.tagRatesSnapshot() else null
        if (rates != null) lastRatesPublish = now
        _state.update {
            it.copy(
                entries = entries,
                totalCount = buffer.totalCount,
                tagCounts = tagCounts,
                tagPids = tagPids,
                networkCalls = calls ?: it.networkCalls,
                tagRates = rates ?: it.tagRates,
            )
        }
    }

    private fun rebuildMatcher() {
        matcher = LogFilterMatcher(
            filter = _state.value.filter.effective(),
            processNames = _state.value.processNames,
            projectAppIds = _state.value.projectAppIds,
        )
    }

    private fun refilter() {
        rebuildMatcher()
        val current = matcher
        buffer.refilter(predicate = current::matches)
        _state.update { it.copy(isQueryValid = current.isQueryValid) }
        publish()
    }

    private fun clear() {
        workScope.launch {
            buffer.clear()
            networkTracker.clear()
            liveBookmarkIds.clear()
            bookmarkStore.onLogCleared()
            _state.update {
                it.copy(
                    selection = SelectionUiModel(),
                    toggledIds = emptySet(),
                    pausedNewCount = 0,
                    networkCalls = emptyList(),
                    networkDetail = null,
                    crashCount = 0,
                    tagRates = emptyMap(),
                )
            }
            publishBookmarks()
            publish()
        }
        val serial = _state.value.selectedSerial
        val device = _state.value.selectedDevice
        if (settings.state.clearDeviceBuffer && device?.state == DeviceState.ONLINE) {
            clearDeviceLog(serial = serial)
                .mapEither(
                    onLeft = { showStatus(message = BetterLogcatBundle.message("status.clearDeviceFailed")) },
                    onRight = {},
                )
                .launchIn(scope = workScope)
        }
    }

    private fun togglePause() {
        val isPaused = !_state.value.isPaused
        _state.update {
            it.copy(
                isPaused = isPaused,
                pausedNewCount = 0,
            )
        }
        if (!isPaused) workScope.launch { publish() }
    }

    // endregion

    // region Filters

    private fun updateFilter(transform: (LogFilter) -> LogFilter) {
        val filter = transform(_state.value.filter)
        _state.update { it.copy(filter = filter) }
        filterChanges.value = filter
        settings.state.lastFilter = filter.toQueryString()
    }

    private fun refreshSavedFilters() {
        workScope.launch {
            val favorites = settings.state.favoriteFilters.toList()
            val history = settings.state.filterHistory.filter { it !in favorites }
            val current = _state.value
            val counts = (favorites + history).associateWith { query ->
                val savedMatcher = LogFilterMatcher(
                    filter = LogFilter().withQueryString(text = query).effective(),
                    processNames = current.processNames,
                    projectAppIds = current.projectAppIds,
                )
                buffer.count(
                    predicate = savedMatcher::matches,
                    limit = SavedFiltersUiModel.COUNT_LIMIT,
                )
            }
            _state.update {
                it.copy(
                    savedFilters = SavedFiltersUiModel(
                        favorites = favorites,
                        history = history,
                        counts = counts,
                    ),
                )
            }
        }
    }

    // Settings lists are replaced, not changed in place, so the settings store sees the change.
    private fun addToHistory(query: String) {
        val history = settings.state.filterHistory
        settings.state.filterHistory = (listOf(query) + history.filter { it != query }).take(MAX_HISTORY).toMutableList()
    }

    private fun toggleFavorite(query: String) {
        if (query.isBlank()) return
        val favorites = settings.state.favoriteFilters
        settings.state.favoriteFilters = (if (query in favorites) favorites - query else listOf(query) + favorites).toMutableList()
        refreshSavedFilters()
    }

    private fun removeFromHistory(query: String) {
        settings.state.filterHistory = (settings.state.filterHistory - query).toMutableList()
        refreshSavedFilters()
    }

    private fun setRegex(isRegex: Boolean) {
        settings.state.isRegex = isRegex
        updateFilter { it.copy(isRegex = isRegex) }
    }

    private fun setMatchCase(isMatchCase: Boolean) {
        settings.state.isMatchCase = isMatchCase
        updateFilter { it.copy(isMatchCase = isMatchCase) }
    }

    private fun toggleLevel(level: LogLevel) {
        val current = _state.value.filter.levels
        // Keep at least one level on, so the list never goes empty by accident.
        if (level in current && current.size == 1) return
        val levels = if (level in current) current - level else current + level
        settings.state.levels = levels.sorted().joinToString(separator = ",") { it.name }
        updateFilter { it.copy(levels = levels) }
    }

    private fun showOnlyTag(tag: String) {
        updateFilter {
            TagVisibility.showOnly(
                filter = it,
                tag = tag,
            )
        }
    }

    private fun hideTag(tag: String) {
        setTagsVisible(
            tags = listOf(tag),
            isVisible = false,
        )
    }

    private fun setTagsVisible(
        tags: List<String>,
        isVisible: Boolean,
    ) {
        val knownTags = _state.value.tagCounts.keys
        updateFilter {
            TagVisibility.setVisible(
                filter = it,
                tags = tags,
                isVisible = isVisible,
                knownTags = knownTags,
            )
        }
    }

    private fun resetFilters() {
        updateFilter {
            LogFilter(
                isRegex = it.isRegex,
                isMatchCase = it.isMatchCase,
            ).withQueryString(text = QueryTerm.Mine.text)
        }
        settings.state.levels = LogLevel.entries.joinToString(separator = ",") { it.name }
    }

    // endregion

    // region View options

    private fun updateViewOptions(transform: (ViewOptionsUiModel) -> ViewOptionsUiModel) {
        val options = transform(_state.value.viewOptions)
        _state.update { it.copy(viewOptions = options) }
        saveViewOptions(options = options)
    }

    private fun toggleColumn(column: LogColumn) {
        updateViewOptions { options ->
            val visible = if (column in options.visibleColumns) options.visibleColumns - column else options.visibleColumns + column
            options.copy(visibleColumns = visible)
        }
    }

    private fun resizeColumn(
        column: LogColumn,
        widthDp: Int,
    ) {
        updateViewOptions { options ->
            options.copy(columnWidths = options.columnWidths + (column to widthDp.coerceIn(minimumValue = MIN_COLUMN_WIDTH, maximumValue = MAX_COLUMN_WIDTH)))
        }
    }

    private fun setTimeFormat(format: TimeFormat) {
        updateViewOptions { options ->
            options.copy(
                timeFormat = format,
                columnWidths = options.columnWidths + (LogColumn.TIME to format.widthDp),
            )
        }
    }

    /** Expand all / collapse all: plain multi-line messages and JSON alike. */
    private fun setExpandByDefault(isExpanded: Boolean) {
        _state.update {
            it.copy(
                viewOptions = it.viewOptions.copy(
                    expandByDefault = isExpanded,
                    expandJsonByDefault = isExpanded,
                ),
                toggledIds = emptySet(),
            )
        }
        saveViewOptions(options = _state.value.viewOptions)
    }

    private fun toggleExpanded(id: Long) {
        _state.update { it.copy(toggledIds = if (id in it.toggledIds) it.toggledIds - id else it.toggledIds + id) }
    }

    private fun setSelectionExpanded(isExpanded: Boolean) {
        _state.update { current ->
            val ids = current.selection.selectedIds
            val toggled = current.toggledIds.toMutableSet()
            current.entries.filter { it.id in ids }.forEach { entry ->
                if (isExpanded == current.viewOptions.isExpandedByDefault(entry = entry)) toggled -= entry.id else toggled += entry.id
            }
            current.copy(toggledIds = toggled)
        }
    }

    private fun saveViewOptions(options: ViewOptionsUiModel) {
        with(settings.state) {
            softWrap = options.softWrap
            compactRows = options.compactRows
            expandByDefault = options.expandByDefault
            prettyJson = options.prettyJson
            timeFormat = options.timeFormat
            sidePanel = options.sidePanel.name
            tagsPanelWidth = options.tagsPanelWidth
            networkPanelWidth = options.networkPanelWidth
            bookmarksPanelWidth = options.bookmarksPanelWidth
            networkDetailHeight = options.networkDetailHeight
            includeThreadId = options.includeThreadId
            showRepeatedTags = options.showRepeatedTags
            showRepeatedPackages = options.showRepeatedPackages
            colorizeTags = options.colorizeTags
            copyMessageOnly = options.copyMessageOnly
            expandJsonByDefault = options.expandJsonByDefault
            showTime = LogColumn.TIME in options.visibleColumns
            showProcess = LogColumn.PROCESS in options.visibleColumns
            showPackage = LogColumn.PACKAGE in options.visibleColumns
            showTag = LogColumn.TAG in options.visibleColumns
            showLevel = LogColumn.LEVEL in options.visibleColumns
            timeWidth = options.width(column = LogColumn.TIME)
            processWidth = options.width(column = LogColumn.PROCESS)
            packageWidth = options.width(column = LogColumn.PACKAGE)
            tagWidth = options.width(column = LogColumn.TAG)
        }
    }

    // endregion

    // region Selection

    private fun pressRow(intent: LogcatIntent.PressRow) {
        val selection = _state.value.selection
        when {
            intent.isToggle -> _state.update {
                val ids = if (intent.id in selection.selectedIds) selection.selectedIds - intent.id else selection.selectedIds + intent.id
                it.copy(
                    selection = SelectionUiModel(
                        anchorId = intent.id,
                        leadId = intent.id,
                        selectedIds = ids,
                    ),
                )
            }
            intent.isExtend && selection.anchorId != SelectionUiModel.NO_ROW -> selectRange(
                anchorId = selection.anchorId,
                leadId = intent.id,
            )
            else -> _state.update {
                it.copy(
                    selection = SelectionUiModel(
                        anchorId = intent.id,
                        leadId = intent.id,
                        selectedIds = setOf(intent.id),
                    ),
                )
            }
        }
    }

    private fun selectRange(
        anchorId: Long,
        leadId: Long,
    ) {
        _state.update { current ->
            val anchorIndex = indexOf(
                entries = current.entries,
                id = anchorId,
            )
            val leadIndex = indexOf(
                entries = current.entries,
                id = leadId,
            )
            if (anchorIndex < 0 || leadIndex < 0) return@update current
            val range = if (anchorIndex <= leadIndex) anchorIndex..leadIndex else leadIndex..anchorIndex
            current.copy(
                selection = SelectionUiModel(
                    anchorId = anchorId,
                    leadId = leadId,
                    selectedIds = range.mapTo(destination = HashSet(range.count())) { current.entries[it].id },
                ),
            )
        }
    }

    private fun moveSelection(
        delta: Int,
        isExtend: Boolean,
    ) {
        val current = _state.value
        if (current.entries.isEmpty()) return
        val leadIndex = indexOf(
            entries = current.entries,
            id = current.selection.leadId,
        )
        val targetIndex = when {
            leadIndex < 0 && delta > 0 -> 0
            leadIndex < 0 -> current.entries.lastIndex
            else -> (leadIndex + delta).coerceIn(minimumValue = 0, maximumValue = current.entries.lastIndex)
        }
        val targetId = current.entries[targetIndex].id
        if (isExtend && current.selection.anchorId != SelectionUiModel.NO_ROW) {
            selectRange(
                anchorId = current.selection.anchorId,
                leadId = targetId,
            )
        } else {
            pressRow(
                intent = LogcatIntent.PressRow(
                    id = targetId,
                    isExtend = false,
                    isToggle = false,
                ),
            )
        }
    }

    private fun selectAll() {
        _state.update { current ->
            current.copy(
                selection = SelectionUiModel(
                    anchorId = current.entries.firstOrNull()?.id ?: SelectionUiModel.NO_ROW,
                    leadId = current.entries.lastOrNull()?.id ?: SelectionUiModel.NO_ROW,
                    selectedIds = current.entries.mapTo(destination = HashSet(current.entries.size)) { it.id },
                ),
            )
        }
    }

    /** Entries are in id order, so a binary search finds the row. */
    private fun indexOf(
        entries: List<LogEntry>,
        id: Long,
    ): Int {
        if (id == SelectionUiModel.NO_ROW) return -1
        val index = entries.binarySearch { it.id.compareTo(other = id) }
        return if (index >= 0) index else -1
    }

    // endregion

    // region Copy, export, navigation

    private fun copy(mode: CopyMode) {
        val current = _state.value
        copyEntries(
            entries = current.entries.filter { it.id in current.selection.selectedIds },
            mode = mode,
        )
    }

    private fun copyEntries(
        entries: List<LogEntry>,
        mode: CopyMode,
    ) {
        if (entries.isEmpty()) return
        val text = LogCopyFormatter.format(
            entries = entries,
            mode = mode,
            processNames = _state.value.processNames,
        )
        CopyPasteManager.getInstance().setContents(StringSelection(text))
        showStatus(message = BetterLogcatBundle.message("status.copied", entries.size))
    }

    private fun requestExport() {
        val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        val device = _state.value.selectedDevice?.name?.replace(
            regex = Regex(pattern = "[^A-Za-z0-9._-]+"),
            replacement = "-",
        ).orEmpty().ifEmpty { "device" }
        workScope.launch { _events.send(element = LogcatEvent.ShowExportDialog(defaultFileName = "logcat-$device-$time.txt")) }
    }

    private fun exportTo(file: File) {
        val current = _state.value
        workScope.launch {
            val text = LogCopyFormatter.format(
                entries = current.entries,
                mode = CopyMode.FULL,
                processNames = current.processNames,
            )
            val isWritten = withContext(context = Dispatchers.IO) {
                runCatching { file.writeText(text = text) }.isSuccess
            }
            val message = if (isWritten) {
                BetterLogcatBundle.message("status.exported", current.entries.size, file.name)
            } else {
                BetterLogcatBundle.message("status.exportFailed", file.name)
            }
            showStatus(message = message)
        }
    }

    private fun openFrame(frame: StackFrame) {
        workScope.launch {
            val isOpened = sourceNavigator.navigate(frame = frame)
            if (!isOpened) showStatus(message = BetterLogcatBundle.message("status.sourceNotFound", frame.fileName))
        }
    }

    private fun loadMapping(file: File) {
        workScope.launch {
            val loaded = withContext(context = Dispatchers.IO) {
                runCatching { StackRetracer.load(mappingFile = file.toPath()) }
            }
            loaded
                .onSuccess { current ->
                    retracer = current
                    buffer.replaceAll { entry -> entry.withMessage(message = current.retrace(message = entry.message)) }
                    _state.update { it.copy(mappingFileName = file.name) }
                    publish()
                    showStatus(message = BetterLogcatBundle.message("status.mappingLoaded", file.name))
                }
                .onFailure { showStatus(message = BetterLogcatBundle.message("status.mappingFailed", file.name)) }
        }
    }

    /** New entries stop being retraced. Entries already retraced keep their readable names. */
    private fun removeMapping() {
        workScope.launch {
            retracer = null
            _state.update { it.copy(mappingFileName = "") }
        }
    }

    /** `age:` is relative to now, so filters with it are rerun every few seconds. */
    private suspend fun ageRefreshLoop() {
        while (true) {
            delay(duration = AGE_REFRESH)
            if (_state.value.filter.effective().terms.any { it.key == QueryKey.AGE }) refilter()
        }
    }

    private fun showNetworkDetail(call: NetworkCall?) {
        val detail = call?.let {
            NetworkDetailUiModel(
                call = it,
                request = buffer.find(id = it.requestEntryId),
                response = buffer.find(id = it.responseEntryId),
            )
        }
        _state.update { it.copy(networkDetail = detail) }
    }

    private fun jumpToEntry(id: Long) {
        if (_state.value.entries.any { it.id == id }) {
            pressRow(
                intent = LogcatIntent.PressRow(
                    id = id,
                    isExtend = false,
                    isToggle = false,
                ),
            )
        } else {
            showStatus(message = BetterLogcatBundle.message("status.entryHidden"))
        }
    }

    private fun jumpToCrash() {
        val crash = _state.value.entries.lastOrNull { it.isCrash }
        if (crash == null) {
            showStatus(message = BetterLogcatBundle.message("status.crashHidden"))
        } else {
            jumpToEntry(id = crash.id)
        }
    }

    private fun onBookmarksChanged(saved: List<Bookmark>) {
        bookmarks = saved
        bookmarkKeys = saved.mapTo(HashSet()) { it.key }
        liveBookmarkIds.clear()
        if (bookmarkKeys.isNotEmpty()) {
            buffer.forEachEntry { entry ->
                val key = BookmarkKey.of(entry = entry)
                if (key in bookmarkKeys) liveBookmarkIds[key] = entry.id
            }
        }
        publishBookmarks()
    }

    private fun publishBookmarks() {
        val models = bookmarks.map {
            BookmarkUiModel(
                bookmark = it,
                liveId = liveBookmarkIds[it.key] ?: BookmarkUiModel.NOT_IN_LOG,
            )
        }
        val ids = liveBookmarkIds.values.toSet()
        _state.update {
            it.copy(
                bookmarks = models,
                bookmarkedIds = ids,
            )
        }
    }

    private fun toggleBookmark(id: Long) {
        val entry = buffer.find(id = id) ?: return
        val current = _state.value
        bookmarkStore.toggle(
            entry = entry,
            processName = current.processNames[entry.pid].orEmpty(),
            deviceName = current.selectedDevice?.name.orEmpty(),
        )
    }

    /** Next (delta 1) or previous (-1) bookmark from the selected row, wrapping around. */
    private fun jumpToBookmark(delta: Int) {
        val current = _state.value
        val marked = current.entries.filter { it.id in current.bookmarkedIds }
        if (marked.isEmpty()) return
        val lead = current.selection.leadId
        val target = if (delta > 0) {
            marked.firstOrNull { it.id > lead } ?: marked.first()
        } else {
            marked.lastOrNull { it.id < lead } ?: marked.last()
        }
        jumpToEntry(id = target.id)
    }

    private fun showStatus(message: String) {
        _events.trySend(element = LogcatEvent.ShowStatus(message = message))
    }

    // endregion

    private fun initialState(): LogcatState {
        val saved = settings.state
        val columns = buildSet {
            if (saved.showTime) add(LogColumn.TIME)
            if (saved.showProcess) add(LogColumn.PROCESS)
            if (saved.showPackage) add(LogColumn.PACKAGE)
            if (saved.showTag) add(LogColumn.TAG)
            if (saved.showLevel) add(LogColumn.LEVEL)
        }
        val baseFilter = LogFilter(
            levels = parseLevels(value = saved.levels.orEmpty()),
            isRegex = saved.isRegex,
            isMatchCase = saved.isMatchCase,
        )
        // First run ever: start like Android Studio, with package:mine.
        val startQuery = initialQuery ?: saved.lastFilter
        return LogcatState(
            selectedSerial = initialSerial,
            filter = baseFilter.withQueryString(text = startQuery ?: QueryTerm.Mine.text),
            viewOptions = ViewOptionsUiModel(
                softWrap = saved.softWrap,
                compactRows = saved.compactRows,
                expandByDefault = saved.expandByDefault,
                prettyJson = saved.prettyJson,
                timeFormat = saved.timeFormat,
                sidePanel = SidePanel.entries.firstOrNull { it.name == saved.sidePanel } ?: SidePanel.NONE,
                tagsPanelWidth = saved.tagsPanelWidth,
                networkPanelWidth = saved.networkPanelWidth,
                bookmarksPanelWidth = saved.bookmarksPanelWidth,
                networkDetailHeight = saved.networkDetailHeight,
                includeThreadId = saved.includeThreadId,
                showRepeatedTags = saved.showRepeatedTags,
                showRepeatedPackages = saved.showRepeatedPackages,
                colorizeTags = saved.colorizeTags,
                copyMessageOnly = saved.copyMessageOnly,
                expandJsonByDefault = saved.expandJsonByDefault,
                visibleColumns = columns,
                columnWidths = mapOf(
                    LogColumn.TIME to saved.timeWidth,
                    LogColumn.PROCESS to saved.processWidth,
                    LogColumn.PACKAGE to saved.packageWidth,
                    LogColumn.TAG to saved.tagWidth,
                    LogColumn.LEVEL to LogColumn.LEVEL.defaultWidthDp,
                ),
            ),
        )
    }

    private fun parseLevels(value: String): Set<LogLevel> {
        val levels = value.split(',').mapNotNull { name -> LogLevel.entries.firstOrNull { it.name == name.trim() } }.toSet()
        return levels.ifEmpty { LogLevel.entries.toSet() }
    }

    private companion object {
        val PUBLISH_INTERVAL = 80.milliseconds
        val FILTER_DEBOUNCE = 120.milliseconds
        val HISTORY_DEBOUNCE = 3.seconds
        val AGE_REFRESH = 5.seconds
        const val RATES_INTERVAL_MILLIS = 1_000L
        const val MAX_HISTORY = 20
        const val NANOS_PER_SECOND = 1_000_000_000L
        const val MIN_COLUMN_WIDTH = 32
        const val MAX_COLUMN_WIDTH = 600
    }
}
