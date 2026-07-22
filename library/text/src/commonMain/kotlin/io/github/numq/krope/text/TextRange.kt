package io.github.numq.krope.text

data class TextRange(val start: TextPosition, val end: TextPosition) {
    companion object {
        val EMPTY = TextRange(start = TextPosition.ZERO, end = TextPosition.ZERO)

        fun fromPositions(p1: TextPosition, p2: TextPosition) = when {
            p1 <= p2 -> TextRange(start = p1, end = p2)

            else -> TextRange(start = p2, end = p1)
        }
    }

    init {
        require(start <= end) { "Start must be <= end: start=$start, end=$end" }
    }

    val isEmpty: Boolean get() = start == end

    val isNotEmpty: Boolean get() = start != end

    val isSingleLine: Boolean get() = start.line == end.line

    val isMultiLine: Boolean get() = start.line != end.line

    fun coerceIn(snapshot: TextSnapshot) = TextRange(start = start.coerceIn(snapshot), end = end.coerceIn(snapshot))

    fun contains(position: TextPosition) = when {
        position.line < start.line || position.line > end.line -> false

        isSingleLine -> position.column in start.column..end.column

        position.line == start.line -> position.column >= start.column

        position.line == end.line -> position.column <= end.column

        else -> true
    }

    fun contains(other: TextRange) = start <= other.start && end >= other.end

    fun intersects(other: TextRange) =
        !(end.line < other.start.line || end.line == other.start.line && end.column <= other.start.column || other.end.line < start.line || other.end.line == start.line && other.end.column <= start.column)
}