package io.github.numq.krope.text

import io.github.numq.krope.core.Encoding
import io.github.numq.krope.core.fp.Either

/**
 * Factory for creating configured instances of [TextBuffer].
 */
interface TextBufferFactory {
    /**
     * Instantiates a new text buffer safely.
     *
     * @param text The initial text string to populate the buffer with.
     * @param lineEnding The dominant line ending style to enforce.
     * @param encoding The memory encoding structure to use for byte sizing.
     * @param enablePooling If true, internal string objects will be pooled (LRU Cache) to dramatically reduce memory allocation on massive batch edits.
     * @return An [Either] containing the [TextBuffer], or catching an initialization error.
     */
    suspend fun create(
        text: String, lineEnding: TextLineEnding, encoding: Encoding, enablePooling: Boolean
    ): Either<Throwable, TextBuffer>
}