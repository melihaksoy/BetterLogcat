package com.melihaksoy.betterlogcat.ide

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.melihaksoy.betterlogcat.adb.AdbLocator
import com.melihaksoy.betterlogcat.adb.AdbSession
import com.melihaksoy.betterlogcat.domain.usecase.ObserveDevicesInteractor
import com.melihaksoy.betterlogcat.domain.usecase.ObserveProjectAppIdsInteractor
import com.melihaksoy.betterlogcat.interactor.ObserveDevicesInteractorImpl
import com.melihaksoy.betterlogcat.interactor.ObserveProjectAppIdsInteractorImpl
import kotlinx.coroutines.CoroutineScope

/**
 * Owns the project coroutine scope (the platform cancels it when the project closes) and the
 * pieces every tab shares: one adb session, one device poll, one project id poll.
 */
@Service(Service.Level.PROJECT)
class BetterLogcatProjectService(
    val project: Project,
    val scope: CoroutineScope,
) {
    val adbSession: AdbSession by lazy { AdbSession(locator = AdbLocator(project = project)) }

    val observeDevices: ObserveDevicesInteractor by lazy {
        ObserveDevicesInteractorImpl(
            session = adbSession,
            scope = scope,
        )
    }

    val observeProjectAppIds: ObserveProjectAppIdsInteractor by lazy {
        ObserveProjectAppIdsInteractorImpl(
            project = project,
            scope = scope,
        )
    }
}
