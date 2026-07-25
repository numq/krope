package io.github.numq.krope.core.fp

/** Wraps this value in a successful [Either.Right] instance. */
fun <L, R> R.right(): Either<L, R> = Either.Right(this)

/** Wraps this value in a failed [Either.Left] instance. */
fun <L, R> L.left(): Either<L, R> = Either.Left(this)

/**
 * A DSL for executing sequential, fail-fast [Either] operations.
 * Allows extracting successful values using [EitherBuilder.bind], and instantly
 * short-circuits the entire block if any bounded [Either] is a [Either.Left].
 *
 * @param block The sequential operations to evaluate.
 * @return The final [Either.Right] if all operations succeed, or the first [Either.Left] encountered.
 */
inline fun <L, R> either(block: EitherBuilder<L>.() -> R): Either<L, R> = try {
    Either.Right(EitherBuilder<L>().block())
} catch (e: EitherControlException) {
    @Suppress("UNCHECKED_CAST") Either.Left(e.value as L)
}