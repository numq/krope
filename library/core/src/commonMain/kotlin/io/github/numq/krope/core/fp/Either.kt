package io.github.numq.krope.core.fp

import kotlin.coroutines.cancellation.CancellationException

sealed class Either<out L, out R> {
    data class Left<out L>(val value: L) : Either<L, Nothing>()

    data class Right<out R>(val value: R) : Either<Nothing, R>()

    val isLeft: Boolean get() = this is Left

    val isRight: Boolean get() = this is Right

    inline fun <T> fold(onLeft: (L) -> T, onRight: (R) -> T): T = when (this) {
        is Left -> onLeft(value)

        is Right -> onRight(value)
    }

    inline fun <T> map(transform: (R) -> T): Either<L, T> = when (this) {
        is Left -> this

        is Right -> Right(transform(value))
    }

    fun getOrNull(): R? = when (this) {
        is Right -> value

        is Left -> null
    }

    companion object {
        inline fun <R> catch(block: () -> R): Either<Throwable, R> = try {
            Right(block())
        } catch (e: Throwable) {
            if (e is CancellationException || e is EitherControlException) throw e

            Left(e)
        }
    }
}