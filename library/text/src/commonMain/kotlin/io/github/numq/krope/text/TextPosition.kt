package io.github.numq.krope.text

data class TextPosition(val line: Int, val column: Int) : Comparable<TextPosition> {
    companion object {
        val ZERO = TextPosition(line = 0, column = 0)
    }

    init {
        require(line >= 0) { "Line must be non-negative: $line" }

        require(column >= 0) { "Column must be non-negative: $column" }
    }

    override fun compareTo(other: TextPosition) = compareValuesBy(this, other, TextPosition::line, TextPosition::column)

    fun coerceIn(snapshot: TextSnapshot): TextPosition {
        val line = line.coerceIn(0, snapshot.lines - 1)

        val column = column.coerceIn(0, snapshot.getLineLength(line = line))

        return TextPosition(line = line, column = column)
    }
}