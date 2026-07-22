package io.github.numq.krope.text;

enum class TextLineEnding {
    LF, CRLF, CR;

    val text: String
        get() = when (this) {
            LF -> "\n"

            CRLF -> "\r\n"

            CR -> "\r"
        }

    data class DetectionResult(val dominant: TextLineEnding, val isMixed: Boolean)

    companion object {
        fun analyze(text: String): DetectionResult {
            var lfCount = 0

            var crlfCount = 0

            var crCount = 0

            var mask = 0

            var i = 0

            val n = minOf(text.length, 8192)

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