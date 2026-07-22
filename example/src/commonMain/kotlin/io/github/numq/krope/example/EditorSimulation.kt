package io.github.numq.krope.example

import io.github.numq.krope.core.Encoding
import io.github.numq.krope.text.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal object EditorSimulation {
    /**
     * Simulates a collaborative or reactive text editor using the Krope TextBuffer.
     * This demonstrates asynchronous event tracking, batch operations, and memory-safe text manipulation.
     */
    suspend fun start() = coroutineScope {
        println("=== Starting Krope Editor Simulation ===\n")

        // 1. Initialization
        val factory = RopeTextBufferFactory()
        val initialText = """
            fun calculateTotal(price: Double, tax: Double): Double {
                return price + tax
            }
        """.trimIndent()

        val buffer = factory.create(
            text = initialText, encoding = Encoding.UTF8, lineEnding = TextLineEnding.LF, enablePooling = true
        ).fold(onLeft = { error ->
            println("Failed to initialize buffer: ${error.message}")
            return@coroutineScope
        }, onRight = { it })

        println("[Initial State]")
        println(buffer.snapshot.value.text)
        println("Total lines: ${buffer.snapshot.value.lines}\n")

        // 2. Reactive Observation
        val observerJob = launch {
            buffer.data.collect { editData ->
                when (editData) {
                    is TextEdit.Data.Single.Insert -> println("~ Event: Inserted '${editData.insertedText}' at ${editData.startPosition}")
                    is TextEdit.Data.Single.Delete -> println("~ Event: Deleted text from ${editData.startPosition} to ${editData.oldEndPosition}")
                    is TextEdit.Data.Single.Replace -> println("~ Event: Replaced '${editData.oldText}' with '${editData.newText}'")
                    is TextEdit.Data.Batch -> println("~ Event: Batch operation executed with ${editData.singles.size} sub-edits")
                }
            }
        }

        // Give the observer a millisecond to start
        delay(50.milliseconds)

        // 3. Single Operations
        println("\n[Action] Renaming 'calculateTotal' to 'calculateGrossTotal'...")
        val replaceRange = TextRange(TextPosition(0, 4), TextPosition(0, 18))
        buffer.replace(replaceRange, "calculateGrossTotal")
            .fold(onLeft = { println("Error: ${it.message}") }, onRight = { /* Handled reactively by observer */ })

        delay(50.milliseconds) // Pause to let observer print

        // 4. Batch Operations
        println("\n[Action] Refactoring 'tax' to 'vatAmount' across multiple lines using a Batch...")
        buffer.withBatch { batch ->
            // Replace in parameters
            batch.replace(
                range = TextRange(TextPosition(0, 39), TextPosition(0, 42)), text = "vatAmount"
            )
            // Replace in return statement (line 1 wasn't affected by line 0 shifts)
            batch.replace(
                range = TextRange(TextPosition(1, 19), TextPosition(1, 22)), text = "vatAmount"
            )
        }.fold(onLeft = { println("Batch Error: ${it.message}") }, onRight = { /* Handled reactively */ })

        delay(50.milliseconds)

        // 5. Querying the Snapshot
        println("\n[Final State]")
        val finalSnapshot = buffer.snapshot.value
        println(finalSnapshot.text)

        println("\n[Buffer Metrics]")
        println("Encoding: ${finalSnapshot.encoding::class.simpleName}")
        println("Line Ending: ${finalSnapshot.lineEnding.name}")
        println("Max Line Length: ${finalSnapshot.maxLineLength} characters")

        // Demonstrate byte offset calculation (useful for cursors or interacting with native systems)
        val targetPos = TextPosition(1, 11) // The word 'price' in the return statement
        val byteOffset = finalSnapshot.getBytePosition(targetPos)
        println("Byte offset for position $targetPos is: $byteOffset bytes")

        // Cleanup
        observerJob.cancel()
        println("\n=== Simulation Complete ===")
    }
}