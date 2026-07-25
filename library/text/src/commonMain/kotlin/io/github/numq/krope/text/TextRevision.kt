package io.github.numq.krope.text

import kotlin.jvm.JvmInline

/**
 * A chronological identifier representing a specific state of the [TextBuffer].
 * Revisions strictly increment with every successful mutation, allowing systems
 * (like Undo/Redo managers or collaborative CRDTs) to track history and state alignment.
 *
 * @property value The numeric revision sequence.
 */
@JvmInline
value class TextRevision(val value: Long) : Comparable<TextRevision> {
    companion object {
        /** The initial revision state of an empty or newly created buffer. */
        val ZERO = TextRevision(value = 0)
    }

    override fun compareTo(other: TextRevision) = value.compareTo(other.value)
}