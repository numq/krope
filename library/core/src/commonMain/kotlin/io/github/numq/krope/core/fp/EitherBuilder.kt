package io.github.numq.krope.core.fp

/**
 * Receiver scope for the [either] DSL block.
 * Provides the [bind] extension to safely unwrap [Either] values.
 */
class EitherBuilder<L> {
    /**
     * Unwraps the [Either.Right] value, or breaks the surrounding [either] block
     * execution, instantly returning this [Either.Left] failure.
     *
     * @return The successfully unwrapped value.
     */
    fun <R> Either<L, R>.bind(): R = when (this) {
        is Either.Right -> value

        is Either.Left -> throw EitherControlException(value)
    }
}