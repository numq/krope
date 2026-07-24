package io.github.numq.krope.text

import io.github.numq.krope.core.Encoding
import io.github.numq.krope.core.fp.Either

/**
 * A standard implementation of [TextBufferFactory] that yields a [RopeTextBuffer] instance.
 * The resulting buffer operates efficiently using balanced Red-Black Rope trees.
 */
class RopeTextBufferFactory : TextBufferFactory {
    override suspend fun create(
        text: String, lineEnding: TextLineEnding, encoding: Encoding, enablePooling: Boolean
    ) = Either.catch {
        RopeTextBuffer(
            initialText = text,
            initialLineEnding = lineEnding,
            initialEncoding = encoding,
            enablePooling = enablePooling
        )
    }
}