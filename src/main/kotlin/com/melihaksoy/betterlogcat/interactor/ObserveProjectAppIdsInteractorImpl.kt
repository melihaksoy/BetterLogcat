package com.melihaksoy.betterlogcat.interactor

import com.android.tools.idea.projectsystem.getProjectSystem
import com.intellij.openapi.application.readAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.melihaksoy.betterlogcat.core.Either
import com.melihaksoy.betterlogcat.core.EitherFlow
import com.melihaksoy.betterlogcat.domain.usecase.ObserveProjectAppIdsInteractor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlin.time.Duration.Companion.seconds

/**
 * Reads ids from the Android project system, the same source as Studio's `package:mine`.
 * Polls, because ids appear only after Gradle sync. One shared poll serves every tab.
 */
class ObserveProjectAppIdsInteractorImpl(
    private val project: Project,
    scope: CoroutineScope,
) : ObserveProjectAppIdsInteractor {

    private val shared = flow<Either<Throwable, Set<String>>> {
        while (true) {
            emit(load())
            delay(duration = POLL_INTERVAL)
        }
    }
        .distinctUntilChanged()
        .shareIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            replay = 1,
        )

    override fun invoke(): EitherFlow<Set<String>> = shared

    private suspend fun load(): Either<Throwable, Set<String>> = try {
        Either.Right(value = readAction { project.getProjectSystem().getKnownApplicationIds() }.sorted().toSet())
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Either.Left(value = exception)
    } catch (error: LinkageError) {
        // The project system API is internal to the Android plugin. If it changes, fall back to no ids.
        thisLogger().warn("Android project system API is not available", error)
        Either.Left(value = IllegalStateException(error))
    }

    private companion object {
        val POLL_INTERVAL = 15.seconds
    }
}
