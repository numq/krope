package io.github.numq.krope.text

import io.github.numq.krope.core.Encoding

/**
 * An immutable representation of the text buffer at a specific moment in time.
 * Provides O(1) or O(log N) access to text metrics and substrings without copying the entire string.
 */
interface TextSnapshot {
    /** The chronological revision number of this snapshot. */
    val revision: TextRevision

    /** The line ending format currently applied to the text representation. */
    val lineEnding: TextLineEnding

    /** The encoding used for byte-level offset calculations. */
    val encoding: Encoding

    /** The total number of lines in the buffer (minimum 1). */
    val lines: Int

    /** The length of the longest single line in the buffer. */
    val maxLineLength: Int

    /** The absolute final position (end of document) in the buffer. */
    val lastPosition: TextPosition

    /** The complete text of the buffer as a single string. Note: may trigger allocation. */
    val text: String

    /**
     * Checks if the given position falls within the bounds of the current text.
     *
     * @param position The position to validate.
     * @return True if the position exists within the text, false otherwise.
     */
    fun isValidPosition(position: TextPosition): Boolean

    /**
     * Retrieves the text contents of a specific line (0-indexed).
     *
     * @param line The index of the line.
     * @return The string content of the requested line.
     */
    fun getLineText(line: Int): String

    /**
     * Retrieves the character length of a specific line.
     *
     * @param line The index of the line.
     * @return The number of characters in the line.
     */
    fun getLineLength(line: Int): Int

    /**
     * Extracts a substring from the buffer spanning the given range.
     *
     * @param range The start and end positions.
     * @return The substring contained within the range.
     */
    fun getTextInRange(range: TextRange): String

    /**
     * Calculates the memory byte offset for a specific text position based on the current [encoding].
     * Useful for native I/O interoperability or specific cursor positioning.
     *
     * @param position The text coordinate to convert.
     * @return The absolute byte offset, or null if the position is invalid.
     */
    fun getBytePosition(position: TextPosition): Int?

    /**
     * Resolves an absolute memory byte offset back into a line/column [TextPosition].
     *
     * @param bytePosition The byte offset to resolve.
     * @return The calculated position, or null if the byte offset is out of bounds.
     */
    fun getTextPosition(bytePosition: Int): TextPosition?
}