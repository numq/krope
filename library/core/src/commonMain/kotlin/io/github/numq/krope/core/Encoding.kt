package io.github.numq.krope.core

sealed interface Encoding {
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
        fun detectFromBOM(bytes: ByteArray) = when {
            bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() -> UTF16LE

            bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() -> UTF16BE

            bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte() -> UTF8

            else -> UTF8
        }
    }
}