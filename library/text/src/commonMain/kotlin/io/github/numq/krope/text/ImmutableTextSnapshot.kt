package io.github.numq.krope.text

import io.github.numq.krope.core.Encoding
import io.github.numq.krope.core.Rope
import kotlin.math.max

internal class ImmutableTextSnapshot(
    private val rope: Rope,
    override val revision: TextRevision,
    override val lineEnding: TextLineEnding,
    override val encoding: Encoding,
) : TextSnapshot {
    override val lines = rope.totalLines

    override val maxLineLength = rope.maxLineLength

    override val lastPosition get() = RopeNavigator.calculateLastPosition(rope = rope)

    override val text get() = RopeNavigator.getFullText(rope = rope, lineEnding = lineEnding)

    override fun isValidPosition(
        position: TextPosition
    ) = position.line in 0 until rope.totalLines && position.column <= RopeNavigator.calculateLineLength(
        rope = rope, line = position.line
    )

    override fun getLineText(line: Int): String {
        require(line in 0 until rope.totalLines)

        val lineStart = rope.getOffsetOfLine(line)

        val lineEnd = when (line) {
            rope.totalLines - 1 -> rope.totalChars

            else -> max(
                lineStart, rope.getOffsetOfLine(line + 1) - RopeNavigator.LINE_BREAK_LENGTH
            )
        }

        return RopeNavigator.restoreLineEndings(
            text = rope.getText(offset = lineStart, length = lineEnd - lineStart), lineEnding = lineEnding
        )
    }

    override fun getLineLength(line: Int) = RopeNavigator.calculateLineLength(rope = rope, line = line)

    override fun getTextInRange(range: TextRange): String {
        require(RopeNavigator.isValidRange(rope = rope, range = range))

        return when {
            range.isEmpty -> ""

            else -> {
                val startOffset = RopeNavigator.getCharOffset(rope = rope, position = range.start)

                val endOffset = RopeNavigator.getCharOffset(rope = rope, position = range.end)

                RopeNavigator.restoreLineEndings(
                    text = rope.getText(
                        offset = startOffset, length = endOffset - startOffset
                    ), lineEnding = lineEnding
                )
            }
        }
    }

    override fun getBytePosition(position: TextPosition) = when {
        RopeNavigator.isValidInsertPosition(rope = rope, position = position) -> rope.getByteOffset(
            charOffset = RopeNavigator.getCharOffset(rope = rope, position = position)
        )

        else -> null
    }

    override fun getTextPosition(bytePosition: Int): TextPosition? = when {
        bytePosition < 0 || bytePosition > rope.totalBytes -> null

        bytePosition == 0 || rope.totalChars == 0 -> TextPosition.ZERO

        bytePosition >= rope.totalBytes -> RopeNavigator.calculateLastPosition(rope = rope)

        else -> {
            var low = 0

            var high = rope.totalChars - 1

            var result = 0

            while (low <= high) {
                val mid = (low + high) ushr 1

                val midByteOffset = rope.getByteOffset(charOffset = mid)

                when {
                    midByteOffset < bytePosition -> {
                        result = mid

                        low = mid + 1
                    }

                    midByteOffset > bytePosition -> high = mid - 1

                    else -> return RopeNavigator.getPosition(rope = rope, charOffset = mid)
                }
            }

            val currentByteOffset = rope.getByteOffset(charOffset = result)

            when {
                currentByteOffset <= bytePosition -> RopeNavigator.getPosition(rope = rope, charOffset = result)

                else -> {
                    if (result > 0) {
                        for (i in result - 1 downTo 0) {
                            if (rope.getByteOffset(charOffset = i) <= bytePosition) {
                                return RopeNavigator.getPosition(rope, i)
                            }
                        }
                    }

                    TextPosition.ZERO
                }
            }
        }
    }
}