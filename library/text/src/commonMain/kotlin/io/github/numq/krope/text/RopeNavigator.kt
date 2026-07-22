package io.github.numq.krope.text

import io.github.numq.krope.core.Rope
import kotlin.math.max

object RopeNavigator {
    const val LINE_BREAK_LENGTH = 1

    fun calculateLineLength(rope: Rope, line: Int) = when {
        rope.totalLines == 0 || line < 0 || line >= rope.totalLines -> 0

        else -> {
            val lineStart = rope.getOffsetOfLine(line)

            val lineEnd = when (line) {
                rope.totalLines - 1 -> rope.totalChars

                else -> max(
                    lineStart, rope.getOffsetOfLine(lineIndex = line + 1) - LINE_BREAK_LENGTH
                )
            }

            max(0, lineEnd - lineStart)
        }
    }

    fun calculateLastPosition(rope: Rope) = when (rope.totalLines) {
        0 -> TextPosition.ZERO

        else -> {
            val lastLineIndex = rope.totalLines - 1

            TextPosition(
                line = lastLineIndex, column = rope.totalChars - rope.getOffsetOfLine(lineIndex = lastLineIndex)
            )
        }
    }

    fun getCharOffset(rope: Rope, position: TextPosition) = when (rope.totalChars) {
        0 -> 0

        else -> when (position.line) {
            rope.totalLines -> rope.totalChars

            else -> rope.getOffsetOfLine(lineIndex = position.line) + position.column
        }
    }

    fun getPosition(rope: Rope, charOffset: Int) = when (charOffset) {
        rope.totalChars -> calculateLastPosition(rope = rope)

        else -> {
            val (line, column) = rope.getPositionAtOffset(charOffset = charOffset)

            TextPosition(line = line, column = column)
        }
    }

    fun isValidInsertPosition(rope: Rope, position: TextPosition) = when {
        position.line < 0 || position.column < 0 || position.line > rope.totalLines -> false

        position.line == rope.totalLines -> position.column == 0

        else -> position.column <= calculateLineLength(rope, position.line)
    }

    fun isValidRange(
        rope: Rope, range: TextRange
    ) = isValidInsertPosition(rope = rope, position = range.start) && isValidInsertPosition(
        rope = rope, position = range.end
    )

    fun restoreLineEndings(text: String, lineEnding: TextLineEnding) = when (lineEnding) {
        TextLineEnding.LF -> text

        TextLineEnding.CRLF -> text.replace("\n", "\r\n")

        TextLineEnding.CR -> text.replace("\n", "\r")
    }

    fun getFullText(rope: Rope, lineEnding: TextLineEnding) =
        restoreLineEndings(text = rope.getText(offset = 0, length = rope.totalChars), lineEnding = lineEnding)
}