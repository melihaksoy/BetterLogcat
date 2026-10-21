package com.melihaksoy.betterlogcat.interactor

import com.android.tools.idea.projectsystem.AndroidProjectSystem
import com.android.tools.idea.projectsystem.getProjectSystem
import com.intellij.execution.ExecutionListener
import com.intellij.execution.ExecutionManager
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.application.readAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.melihaksoy.betterlogcat.core.Either
import com.melihaksoy.betterlogcat.core.EitherFlow
import com.melihaksoy.betterlogcat.domain.usecase.ObserveProjectAppIdsInteractor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

/**
 * Reads ids from the Android project system, the same source as Studio's `package:mine`.
 * Polls, because ids appear only after Gradle sync. One shared poll serves every tab.
 *
 * The project system leaves out a variant whose id Gradle sets at build time, so the ids of the selected
 * run configuration and of every app launched in this session count too. A launch reloads at once.
 */
class ObserveProjectAppIdsInteractorImpl(
    private val project: Project,
    private val scope: CoroutineScope,
) : ObserveProjectAppIdsInteractor {

    private val launchedIds = ConcurrentHashMap.newKeySet<String>()
    private val reloads = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val shared = flow<Either<Throwable, Set<String>>> {
        while (true) {
            emit(load())
            withTimeoutOrNull(timeout = POLL_INTERVAL) { reloads.first() }
        }
    }
        .distinctUntilChanged()
        .shareIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            replay = 1,
        )

    init {
        project.messageBus.connect(scope).subscribe(
            ExecutionManager.EXECUTION_TOPIC,
            object : ExecutionListener {
                override fun processStarted(
                    executorId: String,
                    env: ExecutionEnvironment,
                    handler: ProcessHandler,
                ) {
                    val configuration = env.runProfile as? RunConfiguration ?: return
                    scope.launch { onLaunched(configuration = configuration) }
                }
            },
        )
    }

    override fun invoke(): EitherFlow<Set<String>> = shared

    private suspend fun onLaunched(configuration: RunConfiguration) {
        val id = try {
            readAction {
                packageName(
                    system = project.getProjectSystem(),
                    configuration = configuration,
                )
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            null
        } catch (_: LinkageError) {
            null
        } ?: return
        if (launchedIds.add(id)) reloads.tryEmit(Unit)
    }

    private suspend fun load(): Either<Throwable, Set<String>> = try {
        val ids = readAction {
            val system = project.getProjectSystem()
            val selected = RunManager.getInstance(project).selectedConfiguration?.configuration
            system.getKnownApplicationIds() + launchedIds + listOfNotNull(
                selected?.let {
                    packageName(
                        system = system,
                        configuration = it,
                    )
                },
            )
        }
        Either.Right(value = ids.sorted().toSet())
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Either.Left(value = exception)
    } catch (error: LinkageError) {
        // The project system API is internal to the Android plugin. If it changes, fall back to no ids.
        thisLogger().warn("Android project system API is not available", error)
        Either.Left(value = IllegalStateException(error))
    }

    /** The app id [configuration] deploys, or null for a configuration that deploys no Android app. */
    private fun packageName(
        system: AndroidProjectSystem,
        configuration: RunConfiguration,
    ): String? = try {
        system.getApplicationIdProvider(configuration)?.packageName
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: Exception) {
        // ApkProvisionException: the id is not known before the first build.
        null
    }

    private companion object {
        val POLL_INTERVAL = 15.seconds
    }
}
