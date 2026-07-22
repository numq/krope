package io.github.numq.krope.core

import kotlinx.atomicfu.atomic
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

class Rope private constructor(
    private val root: Node,
    private val ropeNodeFactory: RopeNodeFactory,
    val encoding: Encoding,
    val textVersion: Long = 0L,
) {
    constructor(
        initialText: String,
        encoding: Encoding,
        ropeNodeFactory: RopeNodeFactory,
    ) : this(
        root = when {
            initialText.isEmpty() -> Node.Empty

            else -> ropeNodeFactory.create(text = initialText)
        }, ropeNodeFactory = ropeNodeFactory, encoding = encoding, textVersion = 0L
    )

    private val lineOffsetCache = ThreadSafeLruCache<Int, Int>(1024)

    private val byteOffsetCache = ThreadSafeLruCache<Int, Int>(1024)

    private var cachedTextRef: SoftReference<String>? = null

    private val cachedTextVersion = atomic(-1L)

    val totalBytes: Int get() = root.byteCount + encoding.bomSize

    val totalChars: Int get() = root.charCount

    val totalLines: Int get() = root.lineBreakCount + 1

    val maxLineLength: Int get() = root.maxLineLength

    private fun createNode(text: String) = ropeNodeFactory.create(text = text)

    private fun isRed(node: Node) = node.color == Node.Color.RED

    private fun rotateLeft(node: Node.Branch) = when (val x = node.right) {
        is Node.Branch -> Node.Branch(
            left = Node.Branch(left = node.left, right = x.left, color = node.color), right = x.right, color = x.color
        )

        else -> node
    }

    private fun rotateRight(node: Node.Branch) = when (val x = node.left) {
        is Node.Branch -> Node.Branch(
            left = x.left, right = Node.Branch(left = x.right, right = node.right, color = node.color), color = x.color
        )

        else -> node
    }

    private fun flipColors(node: Node.Branch): Node.Branch {
        fun flip(color: Node.Color) = when (color) {
            Node.Color.BLACK -> Node.Color.RED

            else -> Node.Color.BLACK
        }

        val newLeft = when (val l = node.left) {
            is Node.Leaf -> l.copy(color = flip(color = l.color))

            is Node.Branch -> l.copy(color = flip(color = l.color))

            Node.Empty -> l
        }

        val newRight = when (val r = node.right) {
            is Node.Leaf -> r.copy(color = flip(color = r.color))

            is Node.Branch -> r.copy(color = flip(color = r.color))

            Node.Empty -> r
        }

        return node.copy(left = newLeft, right = newRight, color = flip(color = node.color))
    }

    private fun quickBalance(node: Node) = when (node) {
        is Node.Branch -> {
            var current = node

            if (isRed(node = current.right) && !isRed(node = current.left) && current.right is Node.Branch) {
                current = rotateLeft(node = current)
            }

            if (isRed(node = current.left) && current.left is Node.Branch && isRed(node = current.left.left)) {
                current = rotateRight(node = current)
            }

            if (isRed(node = current.left) && isRed(current.right)) {
                current = flipColors(node = current)
            }

            current
        }

        else -> node
    }

    private fun splitAt(offset: Int, node: Node): Pair<Node, Node> = when {
        offset <= 0 -> Pair(Node.Empty, node)

        offset >= node.charCount -> Pair(node, Node.Empty)

        node is Node.Leaf -> Pair(
            createNode(text = node.text.substring(0, offset)), createNode(text = node.text.substring(offset))
        )

        node is Node.Branch -> when {
            offset < node.left.charCount -> {
                val (ll, lr) = splitAt(offset = offset, node = node.left)

                Pair(ll, concat(left = lr, right = node.right))
            }

            else -> {
                val (rl, rr) = splitAt(offset = offset - node.left.charCount, node = node.right)

                Pair(concat(left = node.left, right = rl), rr)
            }
        }

        else -> Pair(Node.Empty, Node.Empty)
    }

    private fun concat(left: Node, right: Node) = when {
        left == Node.Empty -> right

        right == Node.Empty -> left

        left is Node.Leaf && right is Node.Leaf && left.length + right.length <= 8192 -> createNode(text = left.text + right.text)

        else -> quickBalance(node = Node.Branch(left = left, right = right, color = Node.Color.RED))
    }

    private fun needsFullRebalance(node: Node) = when (node) {
        is Node.Branch -> {
            val maxHeight = max(node.left.height, node.right.height)

            val heightDiff = abs(node.left.height - node.right.height)

            maxHeight > max(2, (ln(node.charCount.toDouble()) / ln(1.618)).toInt() + 1) + 2 || heightDiff > 3
        }

        else -> false
    }

    private fun forceBalance(node: Node): Node {
        val leaves = ArrayList<Node>(128)

        fun collectLeaves(n: Node) {
            when (n) {
                is Node.Leaf if n.text.isNotEmpty() -> {
                    leaves.add(n)
                }

                is Node.Branch -> {
                    collectLeaves(n = n.left)

                    collectLeaves(n = n.right)
                }

                else -> Unit
            }
        }

        collectLeaves(n = node)

        return buildBalancedTree(nodes = leaves)
    }

    private fun collectRange(node: Node, start: Int, end: Int, acc: MutableList<Node>) {
        when {
            start >= end || node === Node.Empty -> Unit

            start == 0 && end == node.charCount -> acc.add(node)

            else -> when (node) {
                is Node.Leaf -> acc.add(createNode(text = node.text.substring(start, end)))

                is Node.Branch -> {
                    val leftCount = node.left.charCount

                    if (start < leftCount) {
                        collectRange(node = node.left, start = start, end = min(end, leftCount), acc = acc)
                    }

                    if (end > leftCount) {
                        collectRange(
                            node = node.right, start = max(0, start - leftCount), end = end - leftCount, acc = acc
                        )
                    }
                }

                Node.Empty -> Unit
            }
        }
    }

    private fun buildBalancedTree(nodes: List<Node>) = when {
        nodes.isEmpty() -> Node.Empty

        else -> {
            val mergedNodes = ArrayList<Node>(nodes.size)

            buildString {
                for (node in nodes) {
                    when (node) {
                        is Node.Leaf -> when {
                            length + node.length <= 4096 -> append(node.text)

                            else -> {
                                if (isNotEmpty()) {
                                    mergedNodes.add(createNode(text = toString()))

                                    clear()
                                }

                                when {
                                    node.length > 4096 -> mergedNodes.add(node)

                                    else -> append(node.text)
                                }
                            }
                        }

                        else -> {
                            if (isNotEmpty()) {
                                mergedNodes.add(createNode(text = toString()))

                                clear()
                            }

                            mergedNodes.add(node)
                        }
                    }
                }

                if (isNotEmpty()) {
                    mergedNodes.add(createNode(text = toString()))
                }
            }

            fun buildRecursive(start: Int, end: Int): Node = when (val count = end - start) {
                0 -> Node.Empty

                1 -> mergedNodes[start]

                else -> {
                    val mid = start + count / 2

                    Node.Branch(
                        left = buildRecursive(start = start, end = mid),
                        right = buildRecursive(start = mid, end = end),
                        color = Node.Color.BLACK
                    )
                }
            }

            buildRecursive(start = 0, end = mergedNodes.size)
        }
    }

    internal fun applyBatchOperations(deletions: List<Pair<Int, Int>>, insertions: List<Pair<Int, String>>) = when {
        deletions.isEmpty() && insertions.isEmpty() -> this

        deletions.isEmpty() -> insertBatchFast(insertions = insertions)

        insertions.isEmpty() -> deleteBatchFast(deletions = deletions)

        else -> {
            val nodes = ArrayList<Node>(deletions.size + insertions.size + 16)

            val allOps = ArrayList<RopeBatch>(deletions.size + insertions.size)

            allOps.addAll(deletions.map { deletion ->
                RopeBatch.Delete(offset = deletion.first, length = deletion.second)
            })

            allOps.addAll(insertions.map { insertion ->
                RopeBatch.Insert(offset = insertion.first, text = insertion.second)
            })

            allOps.sortWith(compareBy(RopeBatch::offset).thenBy { batch ->
                batch is RopeBatch.Insert
            })

            var lastOpEnd = 0

            for (op in allOps) {
                if (op.offset > lastOpEnd) {
                    collectRange(node = root, start = lastOpEnd, end = op.offset, acc = nodes)
                }

                when (op) {
                    is RopeBatch.Delete -> lastOpEnd = max(lastOpEnd, op.offset + op.length)

                    is RopeBatch.Insert -> {
                        nodes.add(createNode(text = op.text))

                        lastOpEnd = max(lastOpEnd, op.offset)
                    }
                }
            }

            if (lastOpEnd < totalChars) {
                collectRange(node = root, start = lastOpEnd, end = totalChars, acc = nodes)
            }

            lineOffsetCache.clear()

            byteOffsetCache.clear()

            cachedTextRef = null

            cachedTextVersion.value = -1L

            Rope(
                root = buildBalancedTree(nodes = nodes),
                ropeNodeFactory = ropeNodeFactory,
                encoding = encoding,
                textVersion = textVersion + 1
            )
        }
    }

    private fun insertBatchFast(insertions: List<Pair<Int, String>>): Rope {
        val sortedInserts = insertions.sortedBy(Pair<Int, String>::first)

        val nodes = ArrayList<Node>()

        var lastPos = 0

        for ((offset, text) in sortedInserts) {
            if (offset > lastPos) {
                collectRange(node = root, start = lastPos, end = offset, acc = nodes)
            }

            nodes.add(createNode(text = text))

            lastPos = offset
        }

        if (lastPos < totalChars) {
            collectRange(node = root, start = lastPos, end = totalChars, acc = nodes)
        }

        return Rope(
            root = buildBalancedTree(nodes = nodes),
            ropeNodeFactory = ropeNodeFactory,
            encoding = encoding,
            textVersion = textVersion + 1
        )
    }

    private fun deleteBatchFast(deletions: List<Pair<Int, Int>>): Rope {
        val sortedDeletions = deletions.sortedBy(Pair<Int, Int>::first)

        val nodes = ArrayList<Node>()

        var currentPos = 0

        for ((delOffset, delLength) in sortedDeletions) {
            if (delOffset > currentPos) {
                collectRange(node = root, start = currentPos, end = delOffset, acc = nodes)
            }

            currentPos = max(currentPos, delOffset + delLength)
        }

        if (currentPos < totalChars) {
            collectRange(node = root, start = currentPos, end = totalChars, acc = nodes)
        }

        return Rope(
            root = buildBalancedTree(nodes),
            ropeNodeFactory = ropeNodeFactory,
            encoding = encoding,
            textVersion = textVersion + 1
        )
    }

    fun getByteOffset(charOffset: Int) = when (charOffset) {
        !in 0..totalChars -> throw IndexOutOfBoundsException()

        0 -> encoding.bomSize

        else -> when (val byteOffset = byteOffsetCache[charOffset]) {
            null -> {
                var bytes = 0

                var remaining = charOffset

                fun traverse(node: Node): Unit = when {
                    remaining <= 0 -> Unit

                    else -> when (node) {
                        is Node.Leaf -> when {
                            remaining >= node.length -> {
                                bytes += node.byteCount

                                remaining -= node.length
                            }

                            else -> {
                                val multiplier = when (encoding) {
                                    is Encoding.UTF32LE, is Encoding.UTF32BE -> 4

                                    is Encoding.UTF16LE, is Encoding.UTF16BE -> 2

                                    else -> null
                                }

                                when {
                                    multiplier != null -> {
                                        bytes += remaining * multiplier

                                        remaining = 0
                                    }

                                    encoding is Encoding.UTF8 -> {
                                        var localBytes = 0

                                        var failedFastPath = false

                                        for (i in 0 until remaining) {
                                            val c = node.text[i]

                                            if (c.isSurrogate()) {
                                                failedFastPath = true

                                                break
                                            }

                                            val code = c.code

                                            localBytes += when {
                                                code < 0x80 -> 1

                                                code < 0x800 -> 2

                                                else -> 3
                                            }
                                        }

                                        bytes += when {
                                            failedFastPath -> node.text.substring(0, remaining)
                                                .toByteArrayForEncoding(encoding = encoding).size

                                            else -> localBytes
                                        }

                                        remaining = 0
                                    }

                                    else -> Unit
                                }
                            }
                        }

                        is Node.Branch -> when {
                            remaining >= node.left.charCount -> {
                                bytes += node.left.byteCount

                                remaining -= node.left.charCount

                                traverse(node = node.right)
                            }

                            else -> traverse(node = node.left)
                        }

                        Node.Empty -> Unit
                    }
                }

                traverse(node = root)

                val finalBytes = bytes + encoding.bomSize

                byteOffsetCache[charOffset] = finalBytes

                finalBytes
            }

            else -> byteOffset
        }
    }

    fun getOffsetOfLine(lineIndex: Int) = when (lineIndex) {
        !in 0..<totalLines -> throw IndexOutOfBoundsException()

        0 -> 0

        else -> when (val lineOffset = lineOffsetCache[lineIndex]) {
            null -> {
                var chars = 0

                var targetLines = lineIndex

                fun traverse(node: Node): Unit = when {
                    targetLines <= 0 -> Unit

                    else -> when (node) {
                        is Node.Leaf -> when {
                            node.lineBreakCount < targetLines -> {
                                chars += node.length

                                targetLines -= node.lineBreakCount
                            }

                            else -> {
                                var i = 0

                                var linesFound = 0

                                var nextNewline = node.text.indexOf('\n', i)

                                while (nextNewline != -1 && linesFound < targetLines) {
                                    linesFound++

                                    i = nextNewline + 1

                                    nextNewline = node.text.indexOf('\n', i)
                                }

                                chars += i

                                targetLines = 0
                            }
                        }

                        is Node.Branch -> when {
                            node.left.lineBreakCount < targetLines -> {
                                chars += node.left.charCount

                                targetLines -= node.left.lineBreakCount

                                traverse(node = node.right)
                            }

                            else -> traverse(node = node.left)
                        }

                        Node.Empty -> Unit
                    }
                }

                traverse(node = root)

                lineOffsetCache[lineIndex] = chars

                chars
            }

            else -> lineOffset
        }
    }

    fun getPositionAtOffset(charOffset: Int) = when (charOffset) {
        !in 0..totalChars -> throw IndexOutOfBoundsException()

        else -> {
            var remainingOffset = charOffset

            var currentLine = 0

            fun find(node: Node) {
                when (node) {
                    is Node.Leaf -> {
                        val textBefore = when {
                            remainingOffset >= node.length -> node.text

                            else -> node.text.substring(0, remainingOffset)
                        }

                        val linesInLeaf = textBefore.count { char ->
                            char == '\n'
                        }

                        currentLine += linesInLeaf

                        if (linesInLeaf > 0) {
                            remainingOffset -= (textBefore.lastIndexOf('\n') + 1)
                        }
                    }

                    is Node.Branch -> when {
                        remainingOffset >= node.left.charCount -> {
                            currentLine += node.left.lineBreakCount

                            remainingOffset -= node.left.charCount

                            find(node = node.right)
                        }

                        else -> find(node = node.left)
                    }

                    Node.Empty -> Unit
                }
            }

            find(node = root)

            currentLine to remainingOffset
        }
    }

    fun getText(offset: Int, length: Int) = when {
        offset < 0 || length < 0 || offset + length > totalChars -> throw IndexOutOfBoundsException()

        length == 0 -> ""

        offset == 0 && length == totalChars -> getFullText()

        else -> {
            val result = CharArray(length)

            var writePos = 0

            var remaining = length

            var currentOffset = offset

            fun traverse(node: Node): Unit = when {
                remaining <= 0 -> Unit

                else -> when (node) {
                    is Node.Leaf -> {
                        val nodeLength = node.length

                        when {
                            currentOffset < nodeLength -> {
                                val charsToCopy = min(nodeLength - currentOffset, remaining)

                                node.text.toCharArray(result, writePos, currentOffset, currentOffset + charsToCopy)

                                writePos += charsToCopy

                                remaining -= charsToCopy

                                when (remaining) {
                                    0 -> Unit

                                    else -> currentOffset = 0
                                }
                            }

                            else -> currentOffset -= nodeLength
                        }
                    }

                    is Node.Branch -> {
                        val leftCount = node.left.charCount

                        when {
                            currentOffset < leftCount -> {
                                traverse(node = node.left)

                                when {
                                    remaining > 0 -> traverse(node = node.right)

                                    else -> Unit
                                }
                            }

                            else -> {
                                currentOffset -= leftCount

                                traverse(node = node.right)
                            }
                        }
                    }

                    Node.Empty -> Unit
                }
            }

            traverse(node = root)

            result.concatToString(startIndex = 0, endIndex = 0 + length)
        }
    }

    fun getFullText(): String {
        val cached = cachedTextRef?.get()

        if (cached != null && cachedTextVersion.value == textVersion) {
            return cached
        }

        val res = buildString(totalChars) {
            fun appendAll(n: Node) {
                when (n) {
                    is Node.Leaf -> append(n.text)

                    is Node.Branch -> {
                        appendAll(n = n.left)

                        appendAll(n = n.right)
                    }

                    Node.Empty -> Unit
                }
            }

            appendAll(n = root)
        }

        cachedTextRef = SoftReference(referent = res)

        cachedTextVersion.value = textVersion

        return res
    }

    fun insert(offset: Int, text: String) = when {
        offset !in 0..totalChars -> throw IndexOutOfBoundsException()

        text.isEmpty() -> this

        else -> {
            val newLeaf = createNode(text = text)

            when {
                root === Node.Empty -> Rope(
                    root = newLeaf,
                    ropeNodeFactory = ropeNodeFactory,
                    encoding = encoding,
                    textVersion = textVersion + 1
                )

                else -> {
                    val (left, right) = splitAt(offset = offset, node = root)

                    val newRoot = concat(left = concat(left = left, right = newLeaf), right = right)

                    val balanced = when {
                        needsFullRebalance(node = newRoot) -> forceBalance(node = newRoot)

                        else -> newRoot
                    }

                    lineOffsetCache.clear()

                    byteOffsetCache.clear()

                    Rope(
                        root = balanced,
                        ropeNodeFactory = ropeNodeFactory,
                        encoding = encoding,
                        textVersion = textVersion + 1
                    )
                }
            }
        }
    }

    fun delete(offset: Int, length: Int) = when {
        offset < 0 || length < 0 || offset + length > totalChars -> throw IndexOutOfBoundsException()

        length == 0 -> this

        else -> {
            val (left, temp) = splitAt(offset = offset, node = root)

            val (_, right) = splitAt(offset = length, node = temp)

            val newRoot = concat(left = left, right = right)

            val balanced = when {
                needsFullRebalance(node = newRoot) -> forceBalance(node = newRoot)

                else -> newRoot
            }

            lineOffsetCache.clear()

            byteOffsetCache.clear()

            Rope(root = balanced, ropeNodeFactory = ropeNodeFactory, encoding = encoding, textVersion = textVersion + 1)
        }
    }

    fun batch(block: RopeBuilder.() -> Unit) = RopeBuilder.build(baseRope = this, builder = block)

    fun rebuildWithEncoding(newEncoding: Encoding) = when (encoding) {
        newEncoding -> this

        else -> {
            val newFactory = RopeNodeFactory(enablePooling = true, encoding = newEncoding)

            fun rebuildNode(node: Node): Node = when (node) {
                is Node.Leaf -> newFactory.create(text = node.text)

                is Node.Branch -> Node.Branch(
                    left = rebuildNode(node = node.left), right = rebuildNode(node = node.right), color = node.color
                )

                Node.Empty -> Node.Empty
            }

            Rope(
                root = rebuildNode(node = root),
                ropeNodeFactory = newFactory,
                encoding = newEncoding,
                textVersion = textVersion
            )
        }
    }
}