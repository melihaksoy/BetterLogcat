package com.melihaksoy.betterlogcat.domain.usecase

import com.melihaksoy.betterlogcat.core.EitherFlow

/**
 * [ids] are the application ids of the open project. [launched] is the app started last from the IDE in this
 * session, [selected] the one the selected run configuration deploys. Each is null while unknown.
 */
data class ProjectApps(
    val ids: Set<String>,
    val launched: String? = null,
    val selected: String? = null,
) {
    /** The app the user works on: the one started last, else the one about to start. */
    val current: String?
        get() = launched ?: selected
}

/** Emits the application ids of the open project. Empty until the build system knows them. */
interface ObserveProjectAppIdsInteractor {
    operator fun invoke(): EitherFlow<ProjectApps>
}
