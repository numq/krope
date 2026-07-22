package io.github.numq.krope.core

import kotlin.math.max
import kotlin.math.min

class RopeNodeFactory(
    val enablePooling: Boolean, val encoding: Encoding, private val maxLeafSize: Int = 8192,
) : NodeFactory {
    private companion object {
        const val DEFAULT_LEAF_CACHE_SIZE = 2048

        const val DEFAULT_POOL_SIZE = 4096

        const val SEARCH_BACK_LIMIT = 128

        const val SMALL_STRING_THRESHOLD = 64

        const val TINY_STRING_THRESHOLD = 8
    }

    private class StringPool(private val maxEntries: Int = DEFAULT_POOL_SIZE) {
        private val tinyStringsPool = ThreadSafeLruCache<String, String>(256)

        private val smallStringsPool = ThreadSafeLruCache<String, String>(maxEntries / 3)

        private val largeStringsPool = ThreadSafeLruCache<String, String>(maxEntries / 3)

        fun getOrCreate(text: String) = when {
            text.length <= TINY_STRING_THRESHOLD -> tinyStringsPool.getOrPut(text) { text }

            text.length <= SMALL_STRING_THRESHOLD -> smallStringsPool.getOrPut(text) { text }

            else -> largeStringsPool.getOrPut(text) { text }
        }

        fun clear() {
            tinyStringsPool.clear()

            smallStringsPool.clear()

            largeStringsPool.clear()
        }
    }

    private data class StringStats(
        val bytes: Int,
        val newlineCount: Int,
        val maxLineLen: Int,
        val prefixLen: Int,
        val suffixLen: Int,
    )

    private val leafCache = ThreadSafeLruCache<String, Node>(DEFAULT_LEAF_CACHE_SIZE)

    private val stringPool = if (enablePooling) StringPool() else null

    private fun calculateStringStats(text: String): StringStats {
        var newlineCount = 0

        var maxLineLen = 0

        var currentLineLen = 0

        val len = text.length

        var prefixLen = len

        var foundFirstNewline = false

        for (i in 0 until len) {
            when (text[i]) {
                '\n' -> {
                    if (!foundFirstNewline) {
                        prefixLen = i

                        foundFirstNewline = true
                    }

                    if (currentLineLen > maxLineLen) {
                        maxLineLen = currentLineLen
                    }

                    newlineCount++

                    currentLineLen = 0
                }

                else -> currentLineLen++
            }
        }

        if (currentLineLen > maxLineLen) {
            maxLineLen = currentLineLen
        }

        val suffixLen = when (val lastNewlineIndex = text.lastIndexOf('\n')) {
            -1 -> len

            else -> len - (lastNewlineIndex + 1)
        }

        val bytes = when (encoding) {
            is Encoding.UTF32LE, is Encoding.UTF32BE -> text.length * 4

            is Encoding.UTF16LE, is Encoding.UTF16BE -> text.length * 2

            is Encoding.UTF8 -> {
                var utf8Size = 0

                var i = 0

                while (i < text.length) {
                    val c = text[i].code

                    when {
                        c <= 0x7F -> utf8Size += 1

                        c <= 0x7FF -> utf8Size += 2

                        text[i].isHighSurrogate() && i + 1 < len && text[i + 1].isLowSurrogate() -> {
                            utf8Size += 4

                            i++
                        }

                        else -> utf8Size += 3
                    }

                    i++
                }

                utf8Size
            }
        }

        return StringStats(
            bytes = bytes,
            newlineCount = newlineCount,
            maxLineLen = maxLineLen,
            prefixLen = prefixLen,
            suffixLen = suffixLen
        )
    }

    private fun splitLargeText(text: String): Node {
        val parts = ArrayList<Node>(text.length / maxLeafSize + 2)

        var start = 0

        val len = text.length

        while (start < len) {
            var end = min(start + maxLeafSize, len)

            if (end < len) {
                var newlinePos = -1

                val searchLimit = max(start, end - SEARCH_BACK_LIMIT)

                for (i in end - 1 downTo searchLimit) {
                    if (text[i] == '\n') {
                        newlinePos = i + 1

                        break
                    }
                }

                if (newlinePos != -1) {
                    end = newlinePos
                }
            }

            parts.add(create(text = text.substring(start, end)))

            start = end
        }

        return buildBalancedTree(nodes = parts)
    }

    private fun buildBalancedTree(nodes: List<Node>): Node {
        fun build(start: Int, end: Int): Node = when (val count = end - start) {
            0 -> Node.Empty

            1 -> nodes[start]

            else -> {
                val mid = start + count / 2

                Node.Branch(
                    left = build(start = start, end = mid),
                    right = build(start = mid, end = end),
                    color = Node.Color.BLACK
                )
            }
        }

        return build(start = 0, end = nodes.size)
    }

    override fun create(text: String) = when {
        text.isEmpty() -> Node.Empty

        text.length > maxLeafSize -> splitLargeText(text = text)

        else -> {
            val pooledText = stringPool?.getOrCreate(text = text) ?: text

            when (val node = leafCache[pooledText]) {
                null -> {
                    val stats = calculateStringStats(text = pooledText)

                    val leaf = Node.Leaf(
                        text = pooledText,
                        byteCount = stats.bytes,
                        lineBreakCount = stats.newlineCount,
                        prefixLineLength = stats.prefixLen,
                        suffixLineLength = stats.suffixLen,
                        maxLineLength = stats.maxLineLen,
                        color = Node.Color.BLACK
                    )

                    leafCache[pooledText] = leaf

                    leaf
                }

                else -> node
            }
        }
    }
}