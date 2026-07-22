package io.github.numq.krope.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class RopeTest {
    private fun createRope(initialText: String): Rope {
        val encoding = Encoding.UTF8
        return Rope(
            initialText = initialText,
            encoding = encoding,
            ropeNodeFactory = RopeNodeFactory(enablePooling = true, encoding = encoding)
        )
    }

    @Test
    fun testInitialEmptyBuffer() {
        val rope = createRope("")

        assertEquals(0, rope.totalChars)
        assertEquals(0, rope.totalBytes)
        assertEquals(1, rope.totalLines)
        assertEquals(0, rope.maxLineLength)
    }

    @Test
    fun testInitialContent() {
        val text = "Hello World"
        val rope = createRope(text)

        assertEquals(text.length, rope.totalChars)
        assertEquals(text.encodeToByteArray().size, rope.totalBytes)
        assertEquals(1, rope.totalLines)
        assertEquals(text.length, rope.maxLineLength)
        assertEquals(text, rope.getText(0, text.length))
    }

    @Test
    fun testInsertAtBeginning() {
        var rope = createRope("World")
        rope = rope.insert(0, "Hello ")

        assertEquals("Hello World", rope.getText(0, 11))
        assertEquals(11, rope.totalChars)
    }

    @Test
    fun testInsertAtEnd() {
        var rope = createRope("Hello")
        rope = rope.insert(5, " World")

        assertEquals("Hello World", rope.getText(0, 11))
        assertEquals(11, rope.totalChars)
    }

    @Test
    fun testInsertInMiddle() {
        var rope = createRope("Hello World")
        rope = rope.insert(6, "Beautiful ")

        assertEquals("Hello Beautiful World", rope.getText(0, 21))
        assertEquals(21, rope.totalChars)
    }

    @Test
    fun testDeleteFromBeginning() {
        var rope = createRope("Hello World")
        rope = rope.delete(0, 6)

        assertEquals("World", rope.getText(0, 5))
        assertEquals(5, rope.totalChars)
    }

    @Test
    fun testDeleteFromEnd() {
        var rope = createRope("Hello World")
        rope = rope.delete(5, 6)

        assertEquals("Hello", rope.getText(0, 5))
        assertEquals(5, rope.totalChars)
    }

    @Test
    fun testDeleteFromMiddle() {
        var rope = createRope("Hello Beautiful World")
        rope = rope.delete(6, 10)

        assertEquals("Hello World", rope.getText(0, 11))
        assertEquals(11, rope.totalChars)
    }

    @Test
    fun testGetPartialText() {
        val rope = createRope("Hello World")

        assertEquals("Hello", rope.getText(0, 5))
        assertEquals("World", rope.getText(6, 5))
        assertEquals("lo Wo", rope.getText(3, 5))
    }

    @Test
    fun testUnicodeCharacters() {
        val text = "Привет мир 🌍"
        val rope = createRope(text)

        assertEquals(text.length, rope.totalChars)
        assertEquals(text.encodeToByteArray().size, rope.totalBytes)
        assertEquals(text, rope.getText(0, text.length))
    }

    @Test
    fun testMultipleLines() {
        val text = "Line1\nLine2\nLine3"
        val rope = createRope(text)

        assertEquals(3, rope.totalLines)
        assertEquals(5, rope.maxLineLength)
    }

    @Test
    fun testLineCountAfterInsert() {
        var rope = createRope("Line1\nLine3")
        rope = rope.insert(6, "Line2\n")

        assertEquals(3, rope.totalLines)
    }

    @Test
    fun testLineCountAfterDelete() {
        var rope = createRope("Line1\nLine2\nLine3")
        rope = rope.delete(6, 6)

        assertEquals(2, rope.totalLines)
    }

    @Test
    fun testMaxLineLength() {
        val rope = createRope("Short\nVeryLongLine\nMid")

        assertEquals(12, rope.maxLineLength)
    }

    @Test
    fun testGetByteOffset() {
        val text = "Hello мир"
        val rope = createRope(text)

        assertEquals(0, rope.getByteOffset(0))
        assertEquals(1, rope.getByteOffset(1))
        assertEquals(6, rope.getByteOffset(6))
    }

    @Test
    fun testGetOffsetOfLine() {
        val rope = createRope("First\nSecond\nThird")

        assertEquals(0, rope.getOffsetOfLine(0))
        assertEquals(6, rope.getOffsetOfLine(1))
        assertEquals(13, rope.getOffsetOfLine(2))
    }

    @Test
    fun testGetOffsetOfLineOutOfBounds() {
        assertFailsWith<IndexOutOfBoundsException> {
            val rope = createRope("Text")
            rope.getOffsetOfLine(5)
        }
    }

    @Test
    fun testComplexOperations() {
        var rope = createRope("Start")
        rope = rope.insert(5, "\nMiddle")
        rope = rope.delete(0, 6)
        rope = rope.insert(0, "New")

        assertEquals("NewMiddle", rope.getText(0, 9))
        assertEquals(9, rope.totalChars)
        assertEquals(1, rope.totalLines)
    }

    @Test
    fun testEmptyGetText() {
        val rope = createRope("")

        assertEquals("", rope.getText(0, 0))
    }

    @Test
    fun testDeleteEmpty() {
        var rope = createRope("Test")
        rope = rope.delete(2, 0)

        assertEquals("Test", rope.getText(0, 4))
    }

    @Test
    fun testInsertEmpty() {
        var rope = createRope("Test")
        rope = rope.insert(2, "")

        assertEquals("Test", rope.getText(0, 4))
    }

    @Test
    fun testInsertOutOfBounds() {
        assertFailsWith<IndexOutOfBoundsException> {
            val rope = createRope("Test")
            rope.insert(10, "X")
        }
    }

    @Test
    fun testDeleteOutOfBounds() {
        assertFailsWith<IndexOutOfBoundsException> {
            val rope = createRope("Test")
            rope.delete(2, 10)
        }
    }

    @Test
    fun testGetTextOutOfBounds() {
        assertFailsWith<IndexOutOfBoundsException> {
            val rope = createRope("Test")
            rope.getText(2, 10)
        }
    }

    @Test
    fun testGetByteOffsetOutOfBounds() {
        assertFailsWith<IndexOutOfBoundsException> {
            val rope = createRope("Test")
            rope.getByteOffset(10)
        }
    }

    @Test
    fun testConsecutiveOperations() {
        var rope = createRope("")
        rope = rope.insert(0, "A")
        rope = rope.insert(1, "B")
        rope = rope.insert(2, "C")
        rope = rope.delete(1, 1)
        rope = rope.insert(1, "D")

        assertEquals("ADC", rope.getText(0, 3))
        assertEquals(3, rope.totalChars)
    }

    @Test
    fun testNewlineHandling() {
        var rope = createRope("Line1\nLine2")

        assertEquals(2, rope.totalLines)
        assertEquals(5, rope.maxLineLength)

        rope = rope.insert(11, "\nLine3")

        assertEquals(3, rope.totalLines)
    }

    @Test
    fun testMixedNewlineFormats() {
        val rope = createRope("Line1\r\nLine2\nLine3")

        assertEquals(3, rope.totalLines)
    }

    @Test
    fun testGetByteOffsetWithUnicode() {
        val text = "aбc"
        val rope = createRope(text)

        assertEquals(0, rope.getByteOffset(0))
        assertEquals(1, rope.getByteOffset(1))
        assertEquals(3, rope.getByteOffset(2))
    }

    @Test
    fun testLargeInsert() {
        var rope = createRope("")
        val largeText = "A".repeat(10000)
        rope = rope.insert(0, largeText)

        assertEquals(10000, rope.totalChars)
        assertEquals(largeText, rope.getText(0, 10000))
    }

    @Test
    fun testStressOperations() {
        var rope = createRope("Initial")

        repeat(101) {
            rope = rope.insert(0, "X")
        }
        assertEquals(108, rope.totalChars)

        repeat(50) {
            if (rope.totalChars > 0) {
                rope = rope.delete(0, 1)
            }
        }
        assertTrue(rope.totalChars > 0)
    }

    @Test
    fun testGetTextBeyondEnd() {
        assertFailsWith<IndexOutOfBoundsException> {
            val rope = createRope("Hello")
            rope.getText(3, 10)
        }
    }

    @Test
    fun testDeletePartial() {
        var rope = createRope("Hello World")
        rope = rope.delete(3, 5)

        assertEquals("Helrld", rope.getText(0, 6))
    }

    @Test
    fun testInsertMultipleTimes() {
        var rope = createRope("")
        rope = rope.insert(0, "A")
        rope = rope.insert(1, "B")
        rope = rope.insert(2, "C")

        assertEquals("ABC", rope.getText(0, 3))
    }

    @Test
    fun testInsertAtExactEnd() {
        var rope = createRope("Hello")
        rope = rope.insert(5, " World")

        assertEquals("Hello World", rope.getText(0, 11))
    }

    @Test
    fun testInsertBeyondEnd() {
        assertFailsWith<IndexOutOfBoundsException> {
            val rope = createRope("Hello")
            rope.insert(6, " World")
        }
    }

    @Test
    fun testGetTextZeroLength() {
        val rope = createRope("Hello")

        assertEquals("", rope.getText(0, 0))
        assertEquals("", rope.getText(5, 0))
    }

    @Test
    fun testGetTextNegativeLength() {
        assertFailsWith<IndexOutOfBoundsException> {
            val rope = createRope("Hello")
            rope.getText(0, -1)
        }
    }

    @Test
    fun testDeleteNegativeLength() {
        assertFailsWith<IndexOutOfBoundsException> {
            val rope = createRope("Hello")
            rope.delete(0, -1)
        }
    }

    @Test
    fun testMergeAdjacentOriginalPieces() {
        var rope = createRope("Hello World")
        rope = rope.delete(5, 1)

        assertEquals("HelloWorld", rope.getText(0, 10))
    }

    @Test
    fun testLargeNumberOfPieces() {
        var rope = createRope("")
        for (i in 0..1000) {
            rope = rope.insert(i, "A")
        }

        assertEquals(1001, rope.totalChars)
        assertEquals("A".repeat(1001), rope.getText(0, 1001))
    }

    @Test
    fun testGetTextWithMultiplePieces() {
        var rope = createRope("Hello World")
        rope = rope.insert(6, "Beautiful ")
        rope = rope.delete(16, 1)
        rope = rope.insert(11, "!!!")

        val result = rope.getText(0, rope.totalChars)
        assertEquals("Hello Beaut!!!iful orld", result)
    }

    @Test
    fun testGetOffsetOfLineWithMultipleNewlinesInPiece() {
        val rope = createRope("Line1\nLine2\nLine3\nLine4")

        assertEquals(0, rope.getOffsetOfLine(0))
        assertEquals(6, rope.getOffsetOfLine(1))
        assertEquals(12, rope.getOffsetOfLine(2))
        assertEquals(18, rope.getOffsetOfLine(3))
    }

    @Test
    fun testGetOffsetOfLineWithEmptyLines() {
        val rope = createRope("Line1\n\nLine3")

        assertEquals(0, rope.getOffsetOfLine(0))
        assertEquals(6, rope.getOffsetOfLine(1))
        assertEquals(7, rope.getOffsetOfLine(2))
    }

    @Test
    fun testGetOffsetOfLineWithMixedNewlines() {
        val rope = createRope("Line1\r\nLine2\nLine3")

        assertEquals(0, rope.getOffsetOfLine(0))
        assertEquals(7, rope.getOffsetOfLine(1))
        assertEquals(13, rope.getOffsetOfLine(2))
    }

    @Test
    fun testEmojiAndSurrogates() {
        val emoji = "🚀 "
        var rope = createRope("Hello World")
        rope = rope.insert(6, emoji)

        assertEquals(14, rope.totalChars)
        assertEquals("🚀 ", rope.getText(6, 3))
        assertEquals("🚀 World", rope.getText(6, 8))
    }

    @Test
    fun testDeepBalance() {
        var rope = createRope("")
        repeat(1000) {
            rope = rope.insert(0, "a")
        }

        assertTrue(rope.totalChars == 1000, "Tree is too deep: ${rope.maxLineLength}")
    }
}