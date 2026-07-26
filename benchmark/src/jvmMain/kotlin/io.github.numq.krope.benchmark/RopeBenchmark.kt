package io.github.numq.krope.benchmark

import io.github.numq.krope.core.Encoding
import io.github.numq.krope.core.Rope
import io.github.numq.krope.core.RopeNodeFactory
import kotlinx.benchmark.*
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.AverageTime)
open class RopeBenchmark {
    @Param("100000", "1000000", "10000000", "100000000")
    var baseTextSize: Int = 0

    private val insertText = "---NEW TEXT---"

    private lateinit var initialString: String

    private lateinit var ropeFactory: RopeNodeFactory

    private lateinit var rope: Rope

    @Setup
    fun setup() {
        val sb = StringBuilder(baseTextSize)

        for (i in 0 until baseTextSize / 10) {
            sb.append("0123456789")
        }

        initialString = sb.toString()

        ropeFactory = RopeNodeFactory(enablePooling = true, encoding = Encoding.UTF8)

        rope = Rope(initialString, Encoding.UTF8, ropeFactory)
    }

    @Benchmark
    fun stringBuilderInsertMiddle(): String {
        val sb = StringBuilder(initialString)
        sb.insert(baseTextSize / 2, insertText)
        return sb.toString()
    }

    @Benchmark
    fun ropeInsertMiddle(): String {
        val newRope = rope.insert(baseTextSize / 2, insertText)
        return newRope.getFullText()
    }

    @Benchmark
    fun stringBuilderMultipleEdits(): String {
        val sb = StringBuilder(initialString)
        for (i in 0 until 100) {
            sb.insert(sb.length / 2, "A")
        }
        return sb.toString()
    }

    @Benchmark
    fun ropeMultipleEdits(): String {
        var currentRope = rope
        for (i in 0 until 100) {
            currentRope = currentRope.insert(currentRope.totalChars / 2, "A")
        }
        return currentRope.getFullText()
    }
}