package io.github.numq.krope.text

/**
 * Represents an actionable payload intended to mutate the text buffer.
 * Unlike [TextEdit] (which represents an already applied delta with exact memory metrics),
 * an operation represents an *intent* to change the text at a specific revision.
 */
sealed interface TextOperation {
    /** The revision of the buffer this operation is intended to apply to. */
    val revision: TextRevision

    /** The structural mutation instructions. */
    val data: Data

    /** An operation initiated by an active user. */
    data class User(override val revision: TextRevision, override val data: Data) : TextOperation

    /** An operation initiated by the system (e.g., formatting tools, async updates). */
    data class System(override val revision: TextRevision, override val data: Data) : TextOperation

    /** Represents the structural intent of the operation. */
    sealed interface Data {
        /** A single, atomic mutation intent. */
        sealed interface Single : Data {
            /** Intent to insert text at a specific position. */
            data class Insert(val position: TextPosition, val text: String) : Single

            /** Intent to replace a specific range with new text. */
            data class Replace(val range: TextRange, val text: String) : Single

            /** Intent to remove a specific range of text. */
            data class Delete(val range: TextRange) : Single
        }

        /** Intent to apply multiple operations atomically as a single transaction. */
        data class Batch(val operations: List<Single>) : Data
    }
}