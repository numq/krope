package io.github.numq.krope.text

import io.github.numq.krope.core.Encoding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.*

internal class TextBufferTest {
    private fun createBuffer(
        initialText: String = "",
        textLineEnding: TextLineEnding = TextLineEnding.LF,
        encoding: Encoding = Encoding.UTF8,
    ) = RopeTextBuffer(
        initialText = initialText, initialLineEnding = textLineEnding, initialEncoding = encoding, enablePooling = true
    )

    @Test
    fun testInitialEmptyBuffer() {
        val buffer = createBuffer()

        assertEquals("", buffer.snapshot.value.text)
        assertEquals(1, buffer.snapshot.value.lines)
        assertEquals(0, buffer.snapshot.value.maxLineLength)
        assertEquals(TextPosition(0, 0), buffer.snapshot.value.lastPosition)
        assertEquals(TextLineEnding.LF, buffer.snapshot.value.lineEnding)
        assertEquals(Encoding.UTF8, buffer.snapshot.value.encoding)
    }

    @Test
    fun testInitialContent() {
        val text = "Hello World"
        val buffer = createBuffer(text)

        assertEquals(text, buffer.snapshot.value.text)
        assertEquals(1, buffer.snapshot.value.lines)
        assertEquals(text.length, buffer.snapshot.value.maxLineLength)
        assertEquals(TextPosition(0, text.length), buffer.snapshot.value.lastPosition)
    }

    @Test
    fun testLongestLineLength() {
        val buffer1 = createBuffer("Hello", TextLineEnding.CR)
        assertEquals(5, buffer1.snapshot.value.maxLineLength)

        val buffer2 = createBuffer("Hello\nWorld\nTest", TextLineEnding.CR)
        assertEquals(5, buffer2.snapshot.value.maxLineLength)

        val buffer3 = createBuffer("Hello\nWorld", TextLineEnding.CRLF)
        assertEquals(5, buffer3.snapshot.value.maxLineLength)
    }

    @Test
    fun testInsertAtBeginning() = runTest {
        val buffer = createBuffer("World")
        buffer.insert(TextPosition(0, 0), "Hello ")

        assertEquals("Hello World", buffer.snapshot.value.text)
        assertEquals(1, buffer.snapshot.value.lines)
        assertEquals(TextPosition(0, 11), buffer.snapshot.value.lastPosition)
    }

    @Test
    fun testInsertAtEnd() = runTest {
        val buffer = createBuffer("Hello")
        buffer.insert(TextPosition(0, 5), " World")

        assertEquals("Hello World", buffer.snapshot.value.text)
        assertEquals(TextPosition(0, 11), buffer.snapshot.value.lastPosition)
    }

    @Test
    fun testInsertInMiddle() = runTest {
        val buffer = createBuffer("Hello World")
        buffer.insert(TextPosition(0, 6), "Beautiful ")

        assertEquals("Hello Beautiful World", buffer.snapshot.value.text)
        assertEquals(TextPosition(0, 21), buffer.snapshot.value.lastPosition)
    }

    @Test
    fun testInsertNewLine() = runTest {
        val buffer = createBuffer("Line1Line3")
        buffer.insert(TextPosition(0, 5), "\nLine2\n")

        assertEquals("Line1\nLine2\nLine3", buffer.snapshot.value.text)
        assertEquals(3, buffer.snapshot.value.lines)
        assertEquals(5, buffer.snapshot.value.maxLineLength)
        assertEquals(TextPosition(2, 5), buffer.snapshot.value.lastPosition)
    }

    @Test
    fun testDeleteRange() = runTest {
        val buffer = createBuffer("Hello Beautiful World")
        buffer.delete(TextRange(TextPosition(0, 6), TextPosition(0, 16)))

        assertEquals("Hello World", buffer.snapshot.value.text)
        assertEquals(TextPosition(0, 11), buffer.snapshot.value.lastPosition)
    }

    @Test
    fun testDeleteEmptyRange() = runTest {
        val buffer = createBuffer("Hello World")
        buffer.delete(TextRange(TextPosition(0, 5), TextPosition(0, 5)))

        assertEquals("Hello World", buffer.snapshot.value.text)
    }

    @Test
    fun testReplaceRange() = runTest {
        val buffer = createBuffer("Hello Beautiful World")
        buffer.replace(TextRange(TextPosition(0, 6), TextPosition(0, 15)), "Wonderful")

        assertEquals("Hello Wonderful World", buffer.snapshot.value.text)
    }

    @Test
    fun testReplaceWithEmpty() = runTest {
        val buffer = createBuffer("Hello World")
        buffer.replace(TextRange(TextPosition(0, 5), TextPosition(0, 11)), "")

        assertEquals("Hello", buffer.snapshot.value.text)
        assertEquals(TextPosition(0, 5), buffer.snapshot.value.lastPosition)
    }

    @Test
    fun testReplaceEmptyRange() = runTest {
        val buffer = createBuffer("Hello World")
        buffer.replace(TextRange(TextPosition(0, 5), TextPosition(0, 5)), " Beautiful")

        assertEquals("Hello Beautiful World", buffer.snapshot.value.text)
    }

    @Test
    fun testGetLineText() = runTest {
        val buffer = createBuffer("Line1\nLine2\nLine3")

        assertEquals("Line1", buffer.snapshot.value.getLineText(0))
        assertEquals("Line2", buffer.snapshot.value.getLineText(1))
        assertEquals("Line3", buffer.snapshot.value.getLineText(2))
    }

    @Test
    fun testGetLineTextOutOfBounds() = runTest {
        assertFailsWith<IllegalArgumentException> {
            val buffer = createBuffer("Line1\nLine2")
            buffer.snapshot.value.getLineText(5)
        }
    }

    @Test
    fun testGetTextInRange() = runTest {
        val buffer = createBuffer("Hello Beautiful World")
        val range = TextRange(TextPosition(0, 6), TextPosition(0, 15))

        assertEquals("Beautiful", buffer.snapshot.value.getTextInRange(range))
    }

    @Test
    fun testGetTextInRangeCrossLines() = runTest {
        val buffer = createBuffer("Line1\nLine2\nLine3")

        val range = TextRange(TextPosition(0, 3), TextPosition(2, 2))
        assertEquals("e1\nLine2\nLi", buffer.snapshot.value.getTextInRange(range))
    }

    @Test
    fun testGetTextInEmptyRange() = runTest {
        val buffer = createBuffer("Hello World")
        val range = TextRange(TextPosition(0, 5), TextPosition(0, 5))

        assertEquals("", buffer.snapshot.value.getTextInRange(range))
    }

    @Test
    fun testGetTextInInvalidRange() = runTest {
        assertFailsWith<IllegalArgumentException> {
            val buffer = createBuffer("Hello")
            val range = TextRange(TextPosition(0, 10), TextPosition(0, 15))

            buffer.snapshot.value.getTextInRange(range)
        }
    }

    @Test
    fun testGetBytePosition() = runTest {
        val buffer = createBuffer("Hello мир")

        assertEquals(0, buffer.snapshot.value.getBytePosition(TextPosition(0, 0)))
        assertEquals(1, buffer.snapshot.value.getBytePosition(TextPosition(0, 1)))
        assertEquals(6, buffer.snapshot.value.getBytePosition(TextPosition(0, 6)))
    }

    @Test
    fun testGetBytePositionInvalid() = runTest {
        val buffer = createBuffer("Hello")

        assertNull(buffer.snapshot.value.getBytePosition(TextPosition(0, 10)))
        assertNull(buffer.snapshot.value.getBytePosition(TextPosition(5, 0)))
    }

    @Test
    fun testGetTextPosition() = runTest {
        val text = "Hello мир"
        val buffer = createBuffer(text, encoding = Encoding.UTF8)

        assertEquals(TextPosition(0, 0), buffer.snapshot.value.getTextPosition(0))
        assertEquals(TextPosition(0, 1), buffer.snapshot.value.getTextPosition(1))
        assertEquals(TextPosition(0, 6), buffer.snapshot.value.getTextPosition(6))
        assertEquals(
            TextPosition(0, text.length), buffer.snapshot.value.getTextPosition(
                buffer.snapshot.value.getBytePosition(buffer.snapshot.value.lastPosition) ?: -1
            )
        )
    }

    @Test
    fun testGetTextPositionInvalid() = runTest {
        val buffer = createBuffer("Hello", encoding = Encoding.UTF8)

        assertNull(buffer.snapshot.value.getTextPosition(-1))
        assertNull(buffer.snapshot.value.getTextPosition(100))

        val emptyBuffer = createBuffer("", encoding = Encoding.UTF8)
        assertEquals(TextPosition(0, 0), emptyBuffer.snapshot.value.getTextPosition(0))
        assertNull(emptyBuffer.snapshot.value.getTextPosition(1))
    }

    @Test
    fun testLastPosition() = runTest {
        val buffer = createBuffer("Line1\nLine2\nVeryLongLine\nShort")

        assertEquals(TextPosition(3, 5), buffer.snapshot.value.lastPosition)
        assertEquals(4, buffer.snapshot.value.lines)
        assertEquals(12, buffer.snapshot.value.maxLineLength)
    }

    @Test
    fun testLineEndingNormalizationLF() = runTest {
        val buffer = createBuffer("Line1\r\nLine2\rLine3", TextLineEnding.LF)

        assertEquals("Line1\nLine2\nLine3", buffer.snapshot.value.text)
        assertEquals(3, buffer.snapshot.value.lines)
    }

    @Test
    fun testLineEndingNormalizationCRLF() = runTest {
        val buffer = createBuffer("Line1\nLine2\rLine3", TextLineEnding.CRLF)

        assertEquals("Line1\r\nLine2\r\nLine3", buffer.snapshot.value.text)
        assertEquals(3, buffer.snapshot.value.lines)
    }

    @Test
    fun testLineEndingNormalizationCR() = runTest {
        val buffer = createBuffer("Line1\nLine2\r\nLine3", TextLineEnding.CR)

        assertEquals("Line1\rLine2\rLine3", buffer.snapshot.value.text)
        assertEquals(3, buffer.snapshot.value.lines)
    }

    @Test
    fun testInsertWithDifferentLineEndings() = runTest {
        val buffer = createBuffer("", TextLineEnding.CRLF)
        buffer.insert(TextPosition(0, 0), "Line1\nLine2\rLine3\r\nLine4")

        assertEquals("Line1\r\nLine2\r\nLine3\r\nLine4", buffer.snapshot.value.text)
        assertEquals(4, buffer.snapshot.value.lines)
    }

    @Test
    fun testDataFlow() = runTest {
        val buffer = createBuffer("Initial")
        val data = mutableListOf<TextEdit.Data>()

        val job = launch {
            buffer.data.collect(data::add)
        }

        delay(10L)

        buffer.insert(TextPosition(0, 7), " Text")
        buffer.delete(TextRange(TextPosition(0, 0), TextPosition(0, 8)))

        delay(100L)
        job.cancel()

        assertEquals(2, data.size)
    }

    @Test
    fun testMultiLineOperations() = runTest {
        val buffer = createBuffer("Line1\nLine2\nLine3")

        buffer.delete(TextRange(TextPosition(1, 0), TextPosition(2, 0)))
        assertEquals("Line1\nLine3", buffer.snapshot.value.text)
        assertEquals(2, buffer.snapshot.value.lines)

        buffer.insert(TextPosition(1, 0), "New")
        assertEquals("Line1\nNewLine3", buffer.snapshot.value.text)
        assertEquals(TextPosition(1, 8), buffer.snapshot.value.lastPosition)
    }

    @Test
    fun testInsertAtLastPosition() = runTest {
        val buffer = createBuffer("Hello")
        buffer.insert(TextPosition(0, 5), " World")

        assertEquals("Hello World", buffer.snapshot.value.text)
        assertEquals(TextPosition(0, 11), buffer.snapshot.value.lastPosition)
    }

    @Test
    fun testInsertInvalidPosition() = runTest {
        assertFailsWith<IllegalArgumentException> {
            val buffer = createBuffer("Hello")
            buffer.insert(TextPosition(0, 10), " World").getOrNull() ?: throw IllegalArgumentException()
        }
    }

    @Test
    fun testInsertInvalidLine() = runTest {
        assertFailsWith<IllegalArgumentException> {
            val buffer = createBuffer("Hello")
            buffer.insert(TextPosition(5, 0), " World").getOrNull() ?: throw IllegalArgumentException()
        }
    }

    @Test
    fun testInsertAtEndOfLastLine() = runTest {
        val buffer = createBuffer("Line1\nLine2")
        buffer.insert(TextPosition(1, 5), "\nLine3")

        assertEquals("Line1\nLine2\nLine3", buffer.snapshot.value.text)
        assertEquals(3, buffer.snapshot.value.lines)
        assertEquals(TextPosition(2, 5), buffer.snapshot.value.lastPosition)
    }

    @Test
    fun testGetPositionFromOffset() = runTest {
        val buffer = createBuffer("Line1\nLine2\nLine3")

        val offset1 = buffer.snapshot.value.getBytePosition(TextPosition(0, 3))
        val offset2 = buffer.snapshot.value.getBytePosition(TextPosition(1, 2))
        val offset3 = buffer.snapshot.value.getBytePosition(TextPosition(2, 4))

        assertNotNull(offset1)
        assertNotNull(offset2)
        assertNotNull(offset3)
    }

    @Test
    fun testComplexScenario() = runTest {
        val buffer = createBuffer()

        buffer.insert(TextPosition(0, 0), "Start")
        buffer.insert(TextPosition(0, 5), "\nMiddle")
        buffer.delete(TextRange(TextPosition(0, 0), TextPosition(1, 0)))
        buffer.insert(TextPosition(0, 0), "New")
        buffer.replace(TextRange(TextPosition(0, 3), TextPosition(0, 9)), "End")

        assertEquals("NewEnd", buffer.snapshot.value.text)
    }

    @Test
    fun testEmptyBufferOperations() = runTest {
        val buffer = createBuffer()

        assertEquals(TextPosition(0, 0), buffer.snapshot.value.lastPosition)
        assertEquals("", buffer.snapshot.value.getLineText(0))
        assertEquals("", buffer.snapshot.value.getTextInRange(TextRange.EMPTY))

        buffer.insert(TextPosition(0, 0), "Test")
        assertEquals("Test", buffer.snapshot.value.text)
    }

    @Test
    fun testLongTextPerformance() = runTest {
        val longText = "A".repeat(10000) + "\n" + "B".repeat(10000)
        val buffer = createBuffer(longText)

        assertEquals(2, buffer.snapshot.value.lines)
        assertEquals(10000, buffer.snapshot.value.maxLineLength)
        assertEquals(20001, buffer.snapshot.value.text.length)

        buffer.insert(TextPosition(1, 5000), "INSERT")
        assertEquals(10006, buffer.snapshot.value.getLineText(1).length)
    }

    @Test
    fun testUnicodeText() = runTest {
        val text = "Привет 🌍 你好 😊"
        val buffer = createBuffer(text)

        assertEquals(text, buffer.snapshot.value.text)
        assertEquals(1, buffer.snapshot.value.lines)

        buffer.insert(TextPosition(0, 9), " мир")
        assertEquals("Привет 🌍 мир 你好 😊", buffer.snapshot.value.text)
    }

    @Test
    fun testLineEndingDetection() = runTest {
        val lfText = "Line1\nLine2\nLine3"
        val crlfText = "Line1\r\nLine2\r\nLine3"
        val crText = "Line1\rLine2\rLine3"
        val mixedText = "Line1\nLine2\r\nLine3\rLine4"

        assertEquals(TextLineEnding.LF, TextLineEnding.analyze(lfText).dominant)
        assertEquals(TextLineEnding.CRLF, TextLineEnding.analyze(crlfText).dominant)
        assertEquals(TextLineEnding.CR, TextLineEnding.analyze(crText).dominant)
        assertEquals(TextLineEnding.LF, TextLineEnding.analyze(mixedText).dominant)
    }

    @Test
    fun testUTF_8_Multibyte() = runTest {
        val text = "A©π€"
        val buffer = createBuffer(text, encoding = Encoding.UTF8)

        assertEquals(Encoding.UTF8, buffer.snapshot.value.encoding)
        assertEquals(0, buffer.snapshot.value.getBytePosition(TextPosition(0, 0)))
        assertEquals(1, buffer.snapshot.value.getBytePosition(TextPosition(0, 1)))
        assertEquals(3, buffer.snapshot.value.getBytePosition(TextPosition(0, 2)))
        assertEquals(5, buffer.snapshot.value.getBytePosition(TextPosition(0, 3)))
        assertEquals(8, buffer.snapshot.value.getBytePosition(TextPosition(0, 4)))
    }

    @Test
    fun testUTF_16BE() = runTest {
        val text = "Hello"
        val buffer = createBuffer(text, encoding = Encoding.UTF16BE)

        // TextEncoding.UTF16BE bomSize = 2. Each char is 2 bytes.
        assertEquals(2, buffer.snapshot.value.getBytePosition(TextPosition(0, 0)))
        assertEquals(4, buffer.snapshot.value.getBytePosition(TextPosition(0, 1)))
        assertEquals(12, buffer.snapshot.value.getBytePosition(TextPosition(0, 5)))
    }

    @Test
    fun testUTF_16LE() = runTest {
        val text = "Hello"
        val buffer = createBuffer(text, encoding = Encoding.UTF16LE)

        assertEquals(12, buffer.snapshot.value.getBytePosition(TextPosition(0, 5)))
    }

    @Test
    fun testUTF_32BE() = runTest {
        val text = "Hello"
        val buffer = createBuffer(text, encoding = Encoding.UTF32BE)

        // TextEncoding.UTF32BE bomSize = 4. Each char is 4 bytes.
        assertEquals(8, buffer.snapshot.value.getBytePosition(TextPosition(0, 1)))
        assertEquals(24, buffer.snapshot.value.getBytePosition(TextPosition(0, 5)))
    }

    @Test
    fun testUTF_32LE() = runTest {
        val text = "Hello"
        val buffer = createBuffer(text, encoding = Encoding.UTF32LE)

        assertEquals(24, buffer.snapshot.value.getBytePosition(TextPosition(0, 5)))
    }

    @Test
    fun testEmojiSurrogatesInUTF8() = runTest {
        val text = "🌍"
        val buffer = createBuffer(text, encoding = Encoding.UTF8)

        assertEquals(2, buffer.snapshot.value.text.length)
        assertEquals(4, buffer.snapshot.value.getBytePosition(TextPosition(0, 2)))
    }

    @Test
    fun testPositionConversionEdgeCases() = runTest {
        val buffer = createBuffer("Line1\nLine2")

        assertEquals(5, buffer.snapshot.value.getBytePosition(TextPosition(0, 5)))
        assertEquals(6, buffer.snapshot.value.getBytePosition(TextPosition(1, 0)))
        assertNull(buffer.snapshot.value.getBytePosition(TextPosition(5, 0)))
    }

    @Test
    fun testBatchOverlappingOperations() = runTest {
        val buffer = createBuffer("Haskell")
        buffer.withBatch { batch ->
            batch.delete(TextRange(TextPosition(0, 0), TextPosition(0, 7)))
            batch.insert(TextPosition(0, 0), "Kotlin")
        }

        assertEquals("Kotlin", buffer.snapshot.value.text)
    }
}