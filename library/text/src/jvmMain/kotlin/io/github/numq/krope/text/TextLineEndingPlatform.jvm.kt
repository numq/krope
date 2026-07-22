package io.github.numq.krope.text

internal actual fun getSystemLineEnding() = when (System.lineSeparator()) {
    "\r\n" -> TextLineEnding.CRLF

    "\r" -> TextLineEnding.CR

    else -> TextLineEnding.LF
}