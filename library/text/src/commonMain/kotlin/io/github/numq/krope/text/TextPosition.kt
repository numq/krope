package io.github.numq.krope.text

/**
 * Represents a specific cursor location in the text by line and column.
 * Both indices are 0-based.
 *
 * @property line The 0-indexed line number.
 * @property column The 0-indexed character offset within the line.
 */
data class TextPosition(val line: Int, val column: Int) : Comparable<TextPosition> {
    companion object {
        /** Represents the very beginning of the document (line 0, column 0). */
        val ZERO = TextPosition(line = 0, column = 0)
    }

    init {
        require(line >= 0) { "Line must be non-negative: $line" }
        require(column >= 0) { "Column must be non-negative: $column" }
    }

    override fun compareTo(other: TextPosition) = compareValuesBy(this, other, TextPosition::line, TextPosition::column)

    /**
     * Ensures this position falls within the valid boundaries of the provided [snapshot].
     * If the position exceeds the text limits, it snaps to the nearest valid boundary.
     *
     * @param snapshot The text snapshot to evaluate bounds against.
     * @return A safely coerced [TextPosition].
     */
    fun coerceIn(snapshot: TextSnapshot): TextPosition {
        val line = line.coerceIn(0, snapshot.lines - 1)
        val column = column.coerceIn(0, snapshot.getLineLength(line = line))
        return TextPosition(line = line, column = column)
    }
}