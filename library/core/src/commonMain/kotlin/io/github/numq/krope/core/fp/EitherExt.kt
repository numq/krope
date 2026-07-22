package io.github.numq.krope.core.fp

fun <L, R> R.right(): Either<L, R> = Either.Right(this)

fun <L, R> L.left(): Either<L, R> = Either.Left(this)

inline fun <L, R> either(block: EitherBuilder<L>.() -> R): Either<L, R> = try {
    Either.Right(EitherBuilder<L>().block())
} catch (e: EitherControlException) {
    @Suppress("UNCHECKED_CAST") Either.Left(e.value as L)
}