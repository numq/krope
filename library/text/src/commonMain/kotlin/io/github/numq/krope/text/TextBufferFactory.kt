package io.github.numq.krope.text

import io.github.numq.krope.core.Encoding
import io.github.numq.krope.core.fp.Either

interface TextBufferFactory {
    suspend fun create(
        text: String, lineEnding: TextLineEnding, encoding: Encoding, enablePooling: Boolean
    ): Either<Throwable, TextBuffer>
}