package io.github.numq.krope.text

import io.github.numq.krope.core.Encoding
import io.github.numq.krope.core.fp.Either
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface TextBuffer {
    val snapshot: StateFlow<TextSnapshot>

    val data: SharedFlow<TextEdit.Data>

    suspend fun changeLineEnding(lineEnding: TextLineEnding): Either<Throwable, Unit>

    suspend fun changeEncoding(encoding: Encoding): Either<Throwable, Unit>

    suspend fun insert(position: TextPosition, text: String): Either<Throwable, TextEdit.Data.Single?>

    suspend fun replace(range: TextRange, text: String): Either<Throwable, TextEdit.Data.Single?>

    suspend fun delete(range: TextRange): Either<Throwable, TextEdit.Data.Single?>

    suspend fun withBatch(block: suspend (TextBuffer) -> Unit): Either<Throwable, TextEdit.Data.Batch?>
}