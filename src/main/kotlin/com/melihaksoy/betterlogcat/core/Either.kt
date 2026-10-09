package com.melihaksoy.betterlogcat.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach

sealed interface Either<out L, out R> {
    data class Left<out L>(val value: L) : Either<L, Nothing>
    data class Right<out R>(val value: R) : Either<Nothing, R>
}

typealias EitherFlow<T> = Flow<Either<Throwable, T>>

fun <T> eitherFlow(block: suspend FlowCollector<Either<Throwable, T>>.() -> Unit): EitherFlow<T> =
    flow(block = block).catchToLeft()

fun <T> Flow<Either<Throwable, T>>.catchToLeft(): EitherFlow<T> =
    catch { throwable ->
        if (throwable is CancellationException) throw throwable
        emit(Either.Left(value = throwable))
    }

fun <T> EitherFlow<T>.mapEither(
    onLeft: suspend (Throwable) -> Unit,
    onRight: suspend (T) -> Unit,
): Flow<Either<Throwable, T>> = onEach { either ->
    when (either) {
        is Either.Left -> onLeft(either.value)
        is Either.Right -> onRight(either.value)
    }
}
