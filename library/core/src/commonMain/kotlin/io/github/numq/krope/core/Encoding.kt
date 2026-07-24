package io.github.numq.krope.core

/**
 * Supported text encodings for the Rope structure.
 * Used internally for accurate memory byte offset calculations across platforms.
 */
sealed interface Encoding {
    /** The size of the Byte Order Mark (BOM) header in bytes. */
    val bomSize: Int

    data object UTF8 : Encoding {
        override val bomSize: Int = 0
    }

    data object UTF16LE : Encoding {
        override val bomSize: Int = 2
    }

    data object UTF16BE : Encoding {
        override val bomSize: Int = 2
    }

    data object UTF32LE : Encoding {
        override val bomSize: Int = 4
    }

    data object UTF32BE : Encoding {
        override val bomSize: Int = 4
    }

    companion object {
        /**
         * Parses a byte array header to detect the encoding via its Byte Order Mark.
         * Falls back to UTF-8 if no valid BOM is found.
         *
         * @param bytes The raw file or stream bytes.
         * @return The detected [Encoding].
         */
        fun detectFromBOM(bytes: ByteArray) = when {
            bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() -> UTF16LE

            bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() -> UTF16BE

            bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte() -> UTF8

            else -> UTF8
        }
    }
}