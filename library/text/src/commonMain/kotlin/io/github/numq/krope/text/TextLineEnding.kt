package io.github.numq.krope.text;

/**
 * Defines the standard line ending characters used across different operating systems.
 */
enum class TextLineEnding {
    /** Unix / Linux / macOS (\n) */
    LF,

    /** Windows (\r\n) */
    CRLF,

    /** Classic macOS (\r) */
    CR;

    /** Returns the string representation of the line ending. */
    val text: String
        get() = when (this) {
            LF -> "\n"

            CRLF -> "\r\n"

            CR -> "\r"
        }

    /** Result of analyzing a string to detect its line endings. */
    data class DetectionResult(val dominant: TextLineEnding, val isMixed: Boolean)

    companion object {
        /**
         * Analyzes a given text block to determine its primary line ending format.
         * Falls back to the platform's default line ending if none are found.
         *
         * @param text The text to scan.
         * @return A [DetectionResult] containing the primary format and whether multiple formats were found.
         */
        fun analyze(text: String): DetectionResult {
            var lfCount = 0

            var crlfCount = 0

            var crCount = 0

            var mask = 0

            var i = 0

            val n = minOf(text.length, 8192) // Scan a reasonable chunk

            while (i < n) {
                when (text[i]) {
                    '\r' -> when {
                        i + 1 < n && text[i + 1] == '\n' -> {
                            crlfCount++; mask = mask or 2; i++
                        }

                        else -> {
                            crCount++; mask = mask or 4
                        }
                    }

                    '\n' -> {
                        lfCount++; mask = mask or 1
                    }
                }

                i++
            }

            val dominant = when {
                lfCount >= crlfCount && lfCount >= crCount && lfCount > 0 -> LF

                crlfCount >= crCount && crlfCount > 0 -> CRLF

                crCount > 0 -> CR

                else -> getSystemLineEnding()
            }

            val isMixed = (mask and (mask - 1)) != 0

            return DetectionResult(dominant = dominant, isMixed = isMixed)
        }
    }
}