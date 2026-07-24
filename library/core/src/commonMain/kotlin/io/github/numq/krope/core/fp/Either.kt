package io.github.numq.krope.core.fp

import kotlin.coroutines.cancellation.CancellationException

/**
 * A lightweight, dependency-free representation of a value that is one of two possible types.
 * Traditionally used to encode success/failure outcomes without throwing hard exceptions.
 *
 * Instances of Either are either an instance of [Left] (representing an error/failure)
 * or [Right] (representing a successful value).
 */
sealed class Either<out L, out R> {
    /** Contains the failure or error context. */
    data class Left<out L>(val value: L) : Either<L, Nothing>()

    /** Contains the successful data payload. */
    data class Right<out R>(val value: R) : Either<Nothing, R>()

    /** True if this result is an error. */
    val isLeft: Boolean get() = this is Left

    /** True if this result is successful. */
    val isRight: Boolean get() = this is Right

    /**
     * Executes the appropriate block depending on whether the result is Left or Right.
     */
    inline fun <T> fold(onLeft: (L) -> T, onRight: (R) -> T): T = when (this) {
        is Left -> onLeft(value)

        is Right -> onRight(value)
    }

    /**
     * Transforms the successful [Right] value, while passing a [Left] failure through unchanged.
     */
    inline fun <T> map(transform: (R) -> T): Either<L, T> = when (this) {
        is Left -> this

        is Right -> Right(transform(value))
    }

    /**
     * Unwraps the successful value if present, returning null if the result was a failure.
     */
    fun getOrNull(): R? = when (this) {
        is Right -> value

        is Left -> null
    }

    companion object {
        /**
         * Safely executes a block of code, catching standard exceptions into a [Left].
         * Safely re-throws internal flow control exceptions like Coroutine cancellations.
         *
         * @param block The code to execute.
         * @return [Right] with the result, or [Left] wrapping the thrown exception.
         */
        inline fun <R> catch(block: () -> R): Either<Throwable, R> = try {
            Right(block())
        } catch (e: Throwable) {
            if (e is CancellationException || e is EitherControlException) throw e

            Left(e)
        }
    }
}