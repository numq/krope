package io.github.numq.krope.text

import io.github.numq.krope.core.Encoding
import io.github.numq.krope.core.Rope
import io.github.numq.krope.core.RopeNodeFactory
import io.github.numq.krope.core.fp.Either
import io.github.numq.krope.core.fp.either
import io.github.numq.krope.core.fp.left
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RopeTextBuffer(
    initialText: String, initialLineEnding: TextLineEnding, initialEncoding: Encoding, enablePooling: Boolean
) : TextBuffer {
    private val mutex = Mutex()

    private val _revision = atomic(0L)

    private val _rope = MutableStateFlow(
        Rope(
            initialText = when {
                initialText.isEmpty() -> initialText

                else -> normalizeLineEndings(text = initialText)
            },
            encoding = initialEncoding,
            ropeNodeFactory = RopeNodeFactory(enablePooling = enablePooling, encoding = initialEncoding)
        )
    )

    private val _encoding = MutableStateFlow(initialEncoding)

    private val _lineEnding = MutableStateFlow(initialLineEnding)

    private val _snapshot = MutableStateFlow<TextSnapshot>(
        ImmutableTextSnapshot(
            rope = _rope.value,
            revision = TextRevision(_revision.value),
            lineEnding = _lineEnding.value,
            encoding = _encoding.value
        )
    )

    override val snapshot = _snapshot.asStateFlow()

    private val _data = MutableSharedFlow<TextEdit.Data>(replay = 0, extraBufferCapacity = 64)

    override val data = _data.asSharedFlow()

    private fun normalizeLineEndings(text: String) = when {
        !text.contains('\r') -> text

        else -> text.replace("\r\n", "\n").replace("\r", "\n")
    }

    private fun doInsert(rope: Rope, position: TextPosition, text: String) = Either.catch {
        require(RopeNavigator.isValidInsertPosition(rope = rope, position = position))

        when {
            text.isEmpty() -> null

            else -> {
                val charOffset =
                    RopeNavigator.getCharOffset(rope = rope, position = position).coerceIn(0, rope.totalChars)

                val normalizedText = normalizeLineEndings(text = text)

                val startByte = rope.getByteOffset(charOffset = charOffset)

                val newRope = rope.insert(offset = charOffset, text = normalizedText)

                val newEndCharOffset = charOffset + normalizedText.length

                newRope to TextEdit.Data.Single.Insert(
                    startPosition = position,
                    newEndPosition = RopeNavigator.getPosition(rope = newRope, charOffset = newEndCharOffset),
                    insertedText = normalizedText,
                    startByte = startByte,
                    newEndByte = newRope.getByteOffset(charOffset = newEndCharOffset)
                )
            }
        }
    }

    private fun doReplace(rope: Rope, range: TextRange, text: String) = Either.catch {
        when {
            range.isEmpty && text.isEmpty() -> null

            else -> {
                val normalizedText = normalizeLineEndings(text = text)

                val startOffset = RopeNavigator.getCharOffset(rope = rope, position = range.start)

                val endOffset = RopeNavigator.getCharOffset(rope = rope, position = range.end)

                val newRope = when {
                    range.isEmpty -> rope.insert(offset = startOffset, text = normalizedText)

                    text.isEmpty() -> rope.delete(offset = startOffset, length = endOffset - startOffset)

                    else -> rope.delete(offset = startOffset, length = endOffset - startOffset)
                        .insert(offset = startOffset, text = normalizedText)
                }

                val startByte = rope.getByteOffset(charOffset = startOffset)

                val oldEndByte = rope.getByteOffset(charOffset = endOffset)

                val oldText = when {
                    range.isEmpty -> ""

                    else -> rope.getText(offset = startOffset, length = endOffset - startOffset)
                }

                val newEndCharOffset = startOffset + normalizedText.length

                val newEndPosition = RopeNavigator.getPosition(rope = newRope, charOffset = newEndCharOffset)

                val newEndByte = newRope.getByteOffset(charOffset = newEndCharOffset)

                newRope to when {
                    range.isEmpty -> TextEdit.Data.Single.Insert(
                        startPosition = range.start,
                        newEndPosition = newEndPosition,
                        insertedText = normalizedText,
                        startByte = startByte,
                        newEndByte = newEndByte
                    )

                    text.isEmpty() -> TextEdit.Data.Single.Delete(
                        startPosition = range.start,
                        oldEndPosition = range.end,
                        deletedText = oldText,
                        startByte = startByte,
                        oldEndByte = oldEndByte
                    )

                    else -> TextEdit.Data.Single.Replace(
                        startPosition = range.start,
                        oldEndPosition = range.end,
                        newEndPosition = newEndPosition,
                        oldText = oldText,
                        newText = normalizedText,
                        startByte = startByte,
                        oldEndByte = oldEndByte,
                        newEndByte = newEndByte
                    )
                }
            }
        }
    }

    private fun doDelete(rope: Rope, range: TextRange) = Either.catch {
        when {
            range.isEmpty -> null

            else -> {
                val startOffset = RopeNavigator.getCharOffset(rope = rope, position = range.start)

                val endOffset = RopeNavigator.getCharOffset(rope = rope, position = range.end)

                val newRope = rope.delete(offset = startOffset, length = endOffset - startOffset)

                newRope to TextEdit.Data.Single.Delete(
                    startPosition = range.start,
                    oldEndPosition = range.end,
                    deletedText = rope.getText(offset = startOffset, length = endOffset - startOffset),
                    startByte = rope.getByteOffset(charOffset = startOffset),
                    oldEndByte = rope.getByteOffset(charOffset = endOffset)
                )
            }
        }
    }

    private suspend fun updateRope(result: Pair<Rope, TextEdit.Data>?) = result?.let { (newRope, data) ->
        val revision = TextRevision(_revision.incrementAndGet())

        _rope.value = newRope

        _snapshot.value = ImmutableTextSnapshot(
            rope = newRope, revision = revision, lineEnding = _lineEnding.value, encoding = _encoding.value
        )

        _data.emit(data)

        data
    }

    override suspend fun changeEncoding(encoding: Encoding) = Either.catch {
        mutex.withLock {
            if (_encoding.value != encoding) {
                val newEncoding = _encoding.updateAndGet { encoding }

                _rope.update { currentRope ->
                    currentRope.rebuildWithEncoding(newEncoding = newEncoding)
                }
            }
        }
    }

    override suspend fun changeLineEnding(lineEnding: TextLineEnding) = Either.catch {
        _lineEnding.value = lineEnding
    }

    override suspend fun insert(position: TextPosition, text: String) = either {
        mutex.withLock {
            updateRope(
                result = doInsert(rope = _rope.value, position = position, text = text).bind()
            ) as? TextEdit.Data.Single.Insert
        }
    }

    override suspend fun replace(range: TextRange, text: String) = either {
        mutex.withLock {
            updateRope(
                result = doReplace(rope = _rope.value, range = range, text = text).bind()
            ) as? TextEdit.Data.Single.Replace
        }
    }

    override suspend fun delete(range: TextRange) = either {
        mutex.withLock {
            updateRope(
                result = doDelete(rope = _rope.value, range = range).bind()
            ) as? TextEdit.Data.Single.Delete
        }
    }

    override suspend fun withBatch(block: suspend (TextBuffer) -> Unit) = Either.catch {
        mutex.withLock {
            var currentRope = _rope.value

            val accumulatedSingles = mutableListOf<TextEdit.Data.Single>()

            val batchBuffer = object : TextBuffer by this@RopeTextBuffer {
                override suspend fun changeLineEnding(lineEnding: TextLineEnding): Either<Throwable, Unit> =
                    IllegalStateException("Cannot change line endings during a batch").left()

                override suspend fun changeEncoding(encoding: Encoding): Either<Throwable, Unit> =
                    IllegalStateException("Cannot change encoding during a batch").left()

                private suspend fun apply(
                    action: suspend (Rope) -> Either<Throwable, Pair<Rope, TextEdit.Data.Single>?>
                ) = either {
                    when (val result = action(currentRope).bind()) {
                        null -> TextEdit.Data.Single.Insert(
                            startPosition = TextPosition.ZERO,
                            newEndPosition = TextPosition.ZERO,
                            insertedText = "",
                            startByte = 0,
                            newEndByte = 0
                        )

                        else -> {
                            currentRope = result.first

                            accumulatedSingles.add(result.second)

                            result.second
                        }
                    }
                }

                override suspend fun insert(position: TextPosition, text: String) = apply { rope ->
                    doInsert(rope = rope, position = position, text = text)
                }

                override suspend fun delete(range: TextRange) = apply { rope ->
                    doDelete(rope = rope, range = range)
                }

                override suspend fun replace(range: TextRange, text: String) = apply { rope ->
                    doReplace(rope = rope, range = range, text = text)
                }

                override suspend fun withBatch(
                    block: suspend (TextBuffer) -> Unit
                ): Either<Throwable, TextEdit.Data.Batch?> = IllegalStateException(
                    "Nested batches not supported"
                ).left()
            }

            block(batchBuffer)

            when {
                accumulatedSingles.isNotEmpty() -> {
                    val batchData = TextEdit.Data.Batch(singles = accumulatedSingles.toList())

                    updateRope(result = currentRope to batchData) as? TextEdit.Data.Batch ?: batchData
                }

                else -> TextEdit.Data.Batch(singles = emptyList())
            }
        }
    }
}