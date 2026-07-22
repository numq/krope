package io.github.numq.krope.core

import kotlin.math.max

sealed interface Node {
    enum class Color { RED, BLACK }

    val charCount: Int

    val byteCount: Int

    val lineBreakCount: Int

    val prefixLineLength: Int

    val suffixLineLength: Int

    val longestLineThroughSplit: Int

    val maxLineLength: Int

    val color: Color

    val height: Int

    val left: Node

    val right: Node

    data class Leaf(
        val text: String,
        override val byteCount: Int,
        override val lineBreakCount: Int,
        override val prefixLineLength: Int,
        override val suffixLineLength: Int,
        override val maxLineLength: Int,
        override val color: Color = Color.BLACK,
    ) : Node {
        override val charCount: Int get() = text.length

        override val longestLineThroughSplit = 0

        override val height: Int get() = 1

        override val left: Node get() = Empty

        override val right: Node get() = Empty

        val length: Int get() = text.length
    }

    data class Branch(
        override val left: Node, override val right: Node, override val color: Color = Color.BLACK,
    ) : Node {
        override val charCount = left.charCount + right.charCount

        override val byteCount = left.byteCount + right.byteCount

        override val lineBreakCount = left.lineBreakCount + right.lineBreakCount

        override val prefixLineLength = when (left.lineBreakCount) {
            0 -> left.charCount + right.prefixLineLength

            else -> left.prefixLineLength
        }

        override val suffixLineLength = when (right.lineBreakCount) {
            0 -> right.charCount + left.suffixLineLength

            else -> right.suffixLineLength
        }

        override val longestLineThroughSplit = maxOf(
            left.longestLineThroughSplit, right.longestLineThroughSplit, left.suffixLineLength + right.prefixLineLength
        )

        override val maxLineLength = maxOf(left.maxLineLength, right.maxLineLength, longestLineThroughSplit)

        override val height = 1 + max(left.height, right.height)
    }

    object Empty : Node {
        override val charCount = 0
        override val byteCount = 0
        override val lineBreakCount = 0
        override val prefixLineLength = 0
        override val suffixLineLength = 0
        override val longestLineThroughSplit = 0
        override val maxLineLength = 0
        override val color = Color.BLACK
        override val height = 0
        override val left: Node get() = this
        override val right: Node get() = this
    }
}