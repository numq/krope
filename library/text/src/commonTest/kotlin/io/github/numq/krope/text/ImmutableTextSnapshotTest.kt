package io.github.numq.krope.text

import io.github.numq.krope.core.Encoding
import io.github.numq.krope.core.Rope
import io.github.numq.krope.core.RopeNodeFactory
import kotlin.test.*

internal class ImmutableTextSnapshotTest {
    private lateinit var emptyRope: Rope
    private lateinit var simpleRope: Rope
    private lateinit var multilineRope: Rope
    private val encoding = Encoding.UTF8

    private fun createRope(text: String): Rope {
        return Rope(
            initialText = text,
            encoding = encoding,
            ropeNodeFactory = RopeNodeFactory(enablePooling = true, encoding = encoding)
        )
    }

    @BeforeTest
    fun setUp() {
        emptyRope = createRope("")
        simpleRope = createRope("Hello World")
        multilineRope = createRope("Hello\nWorld\nTest")
    }

    @Test
    fun testConstructorShouldCreateSnapshotWithCorrectProperties() {
        val snapshot = ImmutableTextSnapshot(
            rope = simpleRope, revision = TextRevision(value = 42L), encoding = encoding, lineEnding = TextLineEnding.LF
        )

        assertEquals(42L, snapshot.revision.value)
        assertEquals(encoding, snapshot.encoding)
        assertEquals(TextLineEnding.LF, snapshot.lineEnding)
    }

    @Test
    fun testLinesShouldReturnTotalLineCount() {
        val snapshot1 = ImmutableTextSnapshot(emptyRope, TextRevision(0), TextLineEnding.LF, encoding)
        assertEquals(1, snapshot1.lines)

        val snapshot2 = ImmutableTextSnapshot(simpleRope, TextRevision(0), TextLineEnding.LF, encoding)
        assertEquals(1, snapshot2.lines)

        val snapshot3 = ImmutableTextSnapshot(multilineRope, TextRevision(0), TextLineEnding.LF, encoding)
        assertEquals(3, snapshot3.lines)
    }

    @Test
    fun testMaxLineLengthShouldReturnMaximumLineLength() {
        val snapshot1 = ImmutableTextSnapshot(emptyRope, TextRevision(0), TextLineEnding.LF, encoding)
        assertEquals(0, snapshot1.maxLineLength)

        val snapshot2 = ImmutableTextSnapshot(simpleRope, TextRevision(0), TextLineEnding.LF, encoding)
        assertEquals(11, snapshot2.maxLineLength)

        val snapshot3 = ImmutableTextSnapshot(multilineRope, TextRevision(0), TextLineEnding.LF, encoding)
        assertEquals(5, snapshot3.maxLineLength)
    }

    @Test
    fun testLastPositionShouldReturnCorrectPosition() {
        val snapshot1 = ImmutableTextSnapshot(emptyRope, TextRevision(0), TextLineEnding.LF, encoding)
        assertEquals(TextPosition(0, 0), snapshot1.lastPosition)

        val snapshot2 = ImmutableTextSnapshot(simpleRope, TextRevision(0), TextLineEnding.LF, encoding)
        assertEquals(TextPosition(0, 11), snapshot2.lastPosition)

        val snapshot3 = ImmutableTextSnapshot(multilineRope, TextRevision(0), TextLineEnding.LF, encoding)
        assertEquals(TextPosition(2, 4), snapshot3.lastPosition)
    }

    @Test
    fun testTextShouldReturnFullTextWithCorrectLineEndings() {
        val snapshot1 = ImmutableTextSnapshot(emptyRope, TextRevision(0), TextLineEnding.LF, encoding)
        assertEquals("", snapshot1.text)

        val snapshot2 = ImmutableTextSnapshot(simpleRope, TextRevision(0), TextLineEnding.LF, encoding)
        assertEquals("Hello World", snapshot2.text)

        val snapshot3 = ImmutableTextSnapshot(multilineRope, TextRevision(0), TextLineEnding.LF, encoding)
        assertEquals("Hello\nWorld\nTest", snapshot3.text)
    }

    @Test
    fun testTextShouldConvertLineEndingsToCRLFWhenRequested() {
        val snapshot = ImmutableTextSnapshot(
            rope = multilineRope, revision = TextRevision(0), encoding = encoding, lineEnding = TextLineEnding.CRLF
        )
        assertEquals("Hello\r\nWorld\r\nTest", snapshot.text)
    }

    @Test
    fun testTextShouldConvertLineEndingsToCRWhenRequested() {
        val snapshot = ImmutableTextSnapshot(
            rope = multilineRope, revision = TextRevision(0), encoding = encoding, lineEnding = TextLineEnding.CR
        )
        assertEquals("Hello\rWorld\rTest", snapshot.text)
    }

    @Test
    fun testIsValidPositionShouldReturnTrueForValidPositions() {
        val snapshot = ImmutableTextSnapshot(multilineRope, TextRevision(0), TextLineEnding.LF, encoding)

        assertTrue(snapshot.isValidPosition(TextPosition(0, 0)))
        assertTrue(snapshot.isValidPosition(TextPosition(0, 5)))
        assertTrue(snapshot.isValidPosition(TextPosition(1, 5)))
        assertTrue(snapshot.isValidPosition(TextPosition(2, 4)))
    }

    @Test
    fun testIsValidPositionShouldReturnFalseForInvalidPositions() {
        val snapshot = ImmutableTextSnapshot(multilineRope, TextRevision(0), TextLineEnding.LF, encoding)

        assertFalse(snapshot.isValidPosition(TextPosition(3, 0)))
        assertFalse(snapshot.isValidPosition(TextPosition(0, 6)))
        assertFalse(snapshot.isValidPosition(TextPosition(1, 6)))
        assertFalse(snapshot.isValidPosition(TextPosition(2, 5)))
    }

    @Test
    fun testGetLineTextShouldReturnCorrectLineText() {
        val snapshot = ImmutableTextSnapshot(multilineRope, TextRevision(0), TextLineEnding.LF, encoding)

        assertEquals("Hello", snapshot.getLineText(0))
        assertEquals("World", snapshot.getLineText(1))
        assertEquals("Test", snapshot.getLineText(2))
    }

    @Test
    fun testGetLineTextShouldThrowExceptionForInvalidLineIndex() {
        val snapshot = ImmutableTextSnapshot(multilineRope, TextRevision(0), TextLineEnding.LF, encoding)

        assertFailsWith<IllegalArgumentException> { snapshot.getLineText(-1) }
        assertFailsWith<IllegalArgumentException> { snapshot.getLineText(3) }
    }

    @Test
    fun testGetLineTextShouldHandleEmptySnapshot() {
        val snapshot = ImmutableTextSnapshot(emptyRope, TextRevision(0), TextLineEnding.LF, encoding)

        assertEquals("", snapshot.getLineText(0))
    }

    @Test
    fun testGetLineTextShouldRestoreLineEndingsCorrectly() {
        val snapshot = ImmutableTextSnapshot(
            rope = multilineRope, revision = TextRevision(0), encoding = encoding, lineEnding = TextLineEnding.CRLF
        )

        assertEquals("Hello", snapshot.getLineText(0))
        assertEquals("World", snapshot.getLineText(1))
        assertEquals("Test", snapshot.getLineText(2))
    }

    @Test
    fun testGetLineLengthShouldReturnCorrectLineLength() {
        val snapshot = ImmutableTextSnapshot(multilineRope, TextRevision(0), TextLineEnding.LF, encoding)

        assertEquals(5, snapshot.getLineLength(0))
        assertEquals(5, snapshot.getLineLength(1))
        assertEquals(4, snapshot.getLineLength(2))
    }

    @Test
    fun testGetLineLengthShouldHandleEmptySnapshot() {
        val snapshot = ImmutableTextSnapshot(emptyRope, TextRevision(0), TextLineEnding.LF, encoding)

        assertEquals(0, snapshot.getLineLength(0))
    }

    @Test
    fun testGetTextInRangeShouldReturnCorrectSubstring() {
        val snapshot = ImmutableTextSnapshot(simpleRope, TextRevision(0), TextLineEnding.LF, encoding)

        val range = TextRange(TextPosition(0, 0), TextPosition(0, 5))
        assertEquals("Hello", snapshot.getTextInRange(range))
    }

    @Test
    fun testGetTextInRangeShouldReturnEmptyStringForEmptyRange() {
        val snapshot = ImmutableTextSnapshot(simpleRope, TextRevision(0), TextLineEnding.LF, encoding)

        val range = TextRange(TextPosition(0, 0), TextPosition(0, 0))
        assertEquals("", snapshot.getTextInRange(range))
    }

    @Test
    fun testGetTextInRangeShouldHandleMultilineRanges() {
        val snapshot = ImmutableTextSnapshot(multilineRope, TextRevision(0), TextLineEnding.LF, encoding)

        val range = TextRange(TextPosition(0, 2), TextPosition(2, 2))
        assertEquals("llo\nWorld\nTe", snapshot.getTextInRange(range))
    }

    @Test
    fun testGetTextInRangeShouldRestoreLineEndings() {
        val snapshot = ImmutableTextSnapshot(
            rope = multilineRope, revision = TextRevision(0), encoding = encoding, lineEnding = TextLineEnding.CRLF
        )

        val range = TextRange(TextPosition(0, 0), TextPosition(2, 4))
        assertEquals("Hello\r\nWorld\r\nTest", snapshot.getTextInRange(range))
    }

    @Test
    fun testGetTextInRangeShouldThrowExceptionForInvalidRange() {
        val snapshot = ImmutableTextSnapshot(multilineRope, TextRevision(0), TextLineEnding.LF, encoding)

        assertFailsWith<IllegalArgumentException> {
            TextRange(TextPosition(0, 6), TextPosition(0, 5))
        }
    }

    @Test
    fun testGetBytePositionShouldReturnCorrectBytePosition() {
        val snapshot = ImmutableTextSnapshot(simpleRope, TextRevision(0), TextLineEnding.LF, encoding)

        val bytePos = snapshot.getBytePosition(TextPosition(0, 0))
        assertNotNull(bytePos)

        assertEquals(0, bytePos)
    }

    @Test
    fun testGetBytePositionShouldReturnNullForInvalidPosition() {
        val snapshot = ImmutableTextSnapshot(multilineRope, TextRevision(0), TextLineEnding.LF, encoding)

        val bytePos = snapshot.getBytePosition(TextPosition(0, 10))
        assertNull(bytePos)
    }

    @Test
    fun testGetBytePositionShouldHandleUTF8MultiByteCharacters() {
        val utf8Rope = createRope("Hello 世界")
        val snapshot = ImmutableTextSnapshot(utf8Rope, TextRevision(0), TextLineEnding.LF, Encoding.UTF8)

        val bytePos1 = snapshot.getBytePosition(TextPosition(0, 6))
        assertNotNull(bytePos1)
        assertEquals(6, bytePos1)

        val bytePos2 = snapshot.getBytePosition(TextPosition(0, 7))
        assertNotNull(bytePos2)
        assertEquals(9, bytePos2) // 世 is 3 bytes

        val bytePos3 = snapshot.getBytePosition(TextPosition(0, 8))
        assertNotNull(bytePos3)
        assertEquals(12, bytePos3) // 界 is 3 bytes
    }

    @Test
    fun testGetBytePositionShouldHandleUTF16Encoding() {
        val utf16Rope = Rope(
            "Hello World", Encoding.UTF16LE, RopeNodeFactory(true, Encoding.UTF16LE)
        )
        val snapshot = ImmutableTextSnapshot(utf16Rope, TextRevision(0), TextLineEnding.LF, Encoding.UTF16LE)

        val bytePos = snapshot.getBytePosition(TextPosition(0, 5))
        assertNotNull(bytePos)
        // 5 chars * 2 bytes + 2 BOM = 12
        assertEquals(12, bytePos)
    }

    @Test
    fun testGetTextPositionShouldConvertBytePositionToTextPosition() {
        val snapshot = ImmutableTextSnapshot(simpleRope, TextRevision(0), TextLineEnding.LF, encoding)

        val position = snapshot.getTextPosition(5)
        assertEquals(TextPosition(0, 5), position)
    }

    @Test
    fun testGetTextPositionShouldReturnNullForInvalidBytePosition() {
        val snapshot = ImmutableTextSnapshot(simpleRope, TextRevision(0), TextLineEnding.LF, encoding)

        assertNull(snapshot.getTextPosition(-1))
        assertNull(snapshot.getTextPosition(100))
    }

    @Test
    fun testGetTextPositionShouldHandleZeroBytePosition() {
        val snapshot = ImmutableTextSnapshot(simpleRope, TextRevision(0), TextLineEnding.LF, encoding)

        val position = snapshot.getTextPosition(0)
        assertEquals(TextPosition(0, 0), position)
    }

    @Test
    fun testGetTextPositionShouldHandleEndOfText() {
        val snapshot = ImmutableTextSnapshot(simpleRope, TextRevision(0), TextLineEnding.LF, encoding)

        val maxBytes = snapshot.getBytePosition(TextPosition(0, 11))
        val position = snapshot.getTextPosition(maxBytes!!)
        assertEquals(TextPosition(0, 11), position)
    }

    @Test
    fun testGetTextPositionShouldHandleMultilineContent() {
        val snapshot = ImmutableTextSnapshot(multilineRope, TextRevision(0), TextLineEnding.LF, encoding)

        val worldBytePos = snapshot.getBytePosition(TextPosition(1, 0))
        val position = snapshot.getTextPosition(worldBytePos!!)
        assertEquals(TextPosition(1, 0), position)
    }

    @Test
    fun testGetTextPositionShouldHandleUTF8MultiByteCharacters() {
        val utf8Rope = createRope("Hello 世界")
        val snapshot = ImmutableTextSnapshot(utf8Rope, TextRevision(0), TextLineEnding.LF, Encoding.UTF8)

        val position = snapshot.getTextPosition(6)
        assertEquals(TextPosition(0, 6), position)
    }

    @Test
    fun testSnapshotShouldBeImmutable() {
        val snapshot1 = ImmutableTextSnapshot(simpleRope, TextRevision(0), TextLineEnding.LF, encoding)
        val originalText = snapshot1.text

        val newRope = simpleRope.insert(5, " Big")
        val snapshot2 = ImmutableTextSnapshot(newRope, TextRevision(1), TextLineEnding.LF, encoding)

        assertEquals("Hello World", originalText)
        assertEquals("Hello Big World", snapshot2.text)
        assertNotEquals(originalText, snapshot2.text)
    }

    @Test
    fun testSnapshotWithCRLFLineEndingsShouldMaintainCorrectPositions() {
        val snapshot = ImmutableTextSnapshot(
            rope = multilineRope, revision = TextRevision(0), encoding = encoding, lineEnding = TextLineEnding.CRLF
        )

        assertEquals("Hello", snapshot.getLineText(0))
        assertEquals("World", snapshot.getLineText(1))
        assertEquals("Test", snapshot.getLineText(2))

        assertEquals(TextPosition(2, 4), snapshot.lastPosition)
    }

    @Test
    fun testSnapshotWithCRLineEndingsShouldWorkCorrectly() {
        val snapshot = ImmutableTextSnapshot(
            rope = multilineRope, revision = TextRevision(0), encoding = encoding, lineEnding = TextLineEnding.CR
        )

        assertEquals("Hello", snapshot.getLineText(0))
        assertEquals("World", snapshot.getLineText(1))
        assertEquals("Test", snapshot.getLineText(2))
    }

    @Test
    fun testShouldHandleVeryLargeLinesCorrectly() {
        val longLine = "A".repeat(10000)
        val rope = createRope(longLine)
        val snapshot = ImmutableTextSnapshot(rope, TextRevision(0), TextLineEnding.LF, encoding)

        assertEquals(1, snapshot.lines)
        assertEquals(10000, snapshot.maxLineLength)
        assertEquals(TextPosition(0, 10000), snapshot.lastPosition)
        assertEquals(longLine, snapshot.text)
    }

    @Test
    fun testShouldHandleManyLinesCorrectly() {
        val lines = (1..1000).joinToString("\n") { "Line $it" }
        val rope = createRope(lines)
        val snapshot = ImmutableTextSnapshot(rope, TextRevision(0), TextLineEnding.LF, encoding)

        assertEquals(1000, snapshot.lines)
        assertEquals(8, snapshot.getLineLength(499))
        assertEquals("Line 500", snapshot.getLineText(499))
    }

    @Test
    fun testGetTextPositionShouldFindCorrectPositionViaBinarySearch() {
        val lines = (1..100).joinToString("\n") { "Line $it" }
        val rope = createRope(lines)
        val snapshot = ImmutableTextSnapshot(rope, TextRevision(0), TextLineEnding.LF, encoding)

        val middleCharPos = snapshot.text.length / 2
        val bytePos = rope.getByteOffset(middleCharPos)

        val position = snapshot.getTextPosition(bytePos)
        assertNotNull(position)

        val recoveredBytePos = snapshot.getBytePosition(position)
        assertNotNull(recoveredBytePos)
        assertTrue(recoveredBytePos == bytePos || recoveredBytePos == bytePos + 1 || recoveredBytePos == bytePos - 1)
    }

    @Test
    fun testGetTextPositionShouldHandlePositionsAtLineBoundaries() {
        val snapshot = ImmutableTextSnapshot(multilineRope, TextRevision(0), TextLineEnding.LF, encoding)

        val pos1 = snapshot.getBytePosition(TextPosition(0, 5))
        val recovered1 = snapshot.getTextPosition(pos1!!)
        assertEquals(TextPosition(0, 5), recovered1)

        val pos2 = snapshot.getBytePosition(TextPosition(1, 0))
        val recovered2 = snapshot.getTextPosition(pos2!!)
        assertEquals(TextPosition(1, 0), recovered2)
    }

    @Test
    fun testGetBytePositionShouldHandlePositionAfterAllText() {
        val snapshot = ImmutableTextSnapshot(multilineRope, TextRevision(0), TextLineEnding.LF, encoding)

        val lastPos = snapshot.lastPosition
        val bytePos = snapshot.getBytePosition(lastPos)
        assertNotNull(bytePos)

        val lastBytePos = multilineRope.totalBytes
        assertEquals(lastBytePos, bytePos)
    }

    @Test
    fun testGetTextPositionShouldHandleBytePositionAtBOMBoundaryForUTF16() {
        val utf16Rope = Rope("Hello", Encoding.UTF16LE, RopeNodeFactory(true, Encoding.UTF16LE))
        val snapshot = ImmutableTextSnapshot(utf16Rope, TextRevision(0), TextLineEnding.LF, Encoding.UTF16LE)

        val position = snapshot.getTextPosition(2) // 2 represents BOM length for UTF-16
        assertEquals(TextPosition(0, 0), position)
    }
}