package io.github.numq.krope.core.fp

class EitherBuilder<L> {
    fun <R> Either<L, R>.bind(): R = when (this) {
        is Either.Right -> value

        is Either.Left -> throw EitherControlException(value)
    }
}