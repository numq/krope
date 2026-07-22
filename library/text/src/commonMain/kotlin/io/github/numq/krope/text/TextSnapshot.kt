package io.github.numq.krope.text

import io.github.numq.krope.core.Encoding

interface TextSnapshot {
    val revision: TextRevision

    val lineEnding: TextLineEnding

    val encoding: Encoding

    val lines: Int

    val maxLineLength: Int

    val lastPosition: TextPosition

    val text: String

    fun isValidPosition(position: TextPosition): Boolean

    fun getLineText(line: Int): String

    fun getLineLength(line: Int): Int

    fun getTextInRange(range: TextRange): String

    fun getBytePosition(position: TextPosition): Int?

    fun getTextPosition(bytePosition: Int): TextPosition?
}