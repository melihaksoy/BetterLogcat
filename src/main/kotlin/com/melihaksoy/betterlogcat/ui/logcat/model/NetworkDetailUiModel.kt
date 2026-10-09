package com.melihaksoy.betterlogcat.ui.logcat.model

import androidx.compose.runtime.Immutable
import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.network.NetworkCall

/** The call open in the network details pane. [request] or [response] is null when not logged or dropped. */
@Immutable
data class NetworkDetailUiModel(
    val call: NetworkCall,
    val request: LogEntry?,
    val response: LogEntry?,
)
