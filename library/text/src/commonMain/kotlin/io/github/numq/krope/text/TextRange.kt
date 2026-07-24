package io.github.numq.krope.text

/**
 * Represents a continuous span of text between a [start] and [end] position.
 * The [start] position must always be less than or equal to the [end] position.
 *
 * @property start The starting boundary of the range (inclusive).
 * @property end The ending boundary of the range (exclusive in most contexts).
 */
data class TextRange(val start: TextPosition, val end: TextPosition) {
    companion object {
        /** An empty range starting and ending at the beginning of the document. */
        val EMPTY = TextRange(start = TextPosition.ZERO, end = TextPosition.ZERO)

        /**
         * Safely constructs a [TextRange] by ensuring the smaller position acts as the start.
         */
        fun fromPositions(p1: TextPosition, p2: TextPosition) = when {
            p1 <= p2 -> TextRange(start = p1, end = p2)

            else -> TextRange(start = p2, end = p1)
        }
    }

    init {
        require(start <= end) { "Start must be <= end: start=$start, end=$end" }
    }

    /** True if the range spans exactly 0 characters. */
    val isEmpty: Boolean get() = start == end

    /** True if the range spans at least 1 character. */
    val isNotEmpty: Boolean get() = start != end

    /** True if the range starts and ends on the exact same line. */
    val isSingleLine: Boolean get() = start.line == end.line

    /** True if the range spans across multiple lines. */
    val isMultiLine: Boolean get() = start.line != end.line

    /**
     * Ensures the range remains strictly within the bounds of the given [snapshot].
     */
    fun coerceIn(snapshot: TextSnapshot) = TextRange(start = start.coerceIn(snapshot), end = end.coerceIn(snapshot))

    /**
     * Checks if a given text position falls within this range.
     */
    fun contains(position: TextPosition) = when {
        position.line < start.line || position.line > end.line -> false

        isSingleLine -> position.column in start.column..end.column

        position.line == start.line -> position.column >= start.column

        position.line == end.line -> position.column <= end.column

        else -> true
    }

    /** Checks if another range is fully contained within this range. */
    fun contains(other: TextRange) = start <= other.start && end >= other.end

    /** Checks if another range overlaps with this range in any way. */
    fun intersects(other: TextRange) =
        !(end.line < other.start.line || end.line == other.start.line && end.column <= other.start.column || other.end.line < start.line || other.end.line == start.line && other.end.column <= start.column)
}