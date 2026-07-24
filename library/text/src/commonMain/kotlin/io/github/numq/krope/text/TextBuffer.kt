package io.github.numq.krope.text

import io.github.numq.krope.core.Encoding
import io.github.numq.krope.core.fp.Either
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A thread-safe, reactive text buffer backed by a persistent Rope data structure.
 *
 * This buffer provides methods for mutating text (insert, replace, delete) safely using [Either]
 * for error handling. It exposes reactivity through [snapshot] for current state reading
 * and [data] for observing atomic edit events.
 */
interface TextBuffer {
    /**
     * A state flow representing the current, immutable snapshot of the text buffer.
     * Updates automatically after every successful mutation.
     */
    val snapshot: StateFlow<TextSnapshot>

    /**
     * A shared flow emitting the structural deltas ([TextEdit.Data]) for every operation
     * successfully applied to the buffer.
     */
    val data: SharedFlow<TextEdit.Data>

    /**
     * Changes the dominant line ending format of the buffer.
     *
     * @param lineEnding The new [TextLineEnding] to apply.
     * @return [Either.Right] on success, or [Either.Left] containing the exception on failure.
     */
    suspend fun changeLineEnding(lineEnding: TextLineEnding): Either<Throwable, Unit>

    /**
     * Rebuilds the underlying structure using a new text encoding.
     * Useful for dynamically recalculating byte offsets when switching encodings (e.g., UTF-8 to UTF-16).
     *
     * @param encoding The new [Encoding] to apply.
     * @return [Either.Right] on success, or [Either.Left] containing the exception on failure.
     */
    suspend fun changeEncoding(encoding: Encoding): Either<Throwable, Unit>

    /**
     * Inserts text at the specified line and column.
     *
     * @param position The [TextPosition] where the text should be inserted.
     * @param text The string to insert.
     * @return An [Either] containing the resulting [TextEdit.Data.Single] delta, or null if the input was empty.
     */
    suspend fun insert(position: TextPosition, text: String): Either<Throwable, TextEdit.Data.Single?>

    /**
     * Replaces the text within the specified range with new text.
     *
     * @param range The [TextRange] of text to replace.
     * @param text The new string to replace the range with.
     * @return An [Either] containing the resulting [TextEdit.Data.Single] delta.
     */
    suspend fun replace(range: TextRange, text: String): Either<Throwable, TextEdit.Data.Single?>

    /**
     * Deletes the text within the specified range.
     *
     * @param range The [TextRange] of text to remove.
     * @return An [Either] containing the resulting [TextEdit.Data.Single] delta.
     */
    suspend fun delete(range: TextRange): Either<Throwable, TextEdit.Data.Single?>

    /**
     * Executes multiple operations atomically as a single batch.
     * The underlying tree structure is rebalanced only once at the end of the batch,
     * making this highly efficient for massive refactoring or multi-cursor edits.
     *
     * @param block A suspending block providing a scoped [TextBuffer] for batch operations.
     * @return An [Either] containing the combined [TextEdit.Data.Batch] delta.
     */
    suspend fun withBatch(block: suspend (TextBuffer) -> Unit): Either<Throwable, TextEdit.Data.Batch?>
}