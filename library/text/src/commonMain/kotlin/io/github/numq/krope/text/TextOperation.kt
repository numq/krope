package io.github.numq.krope.text

sealed interface TextOperation {
    val revision: TextRevision

    val data: Data

    data class User(override val revision: TextRevision, override val data: Data) : TextOperation

    data class System(override val revision: TextRevision, override val data: Data) : TextOperation

    sealed interface Data {
        sealed interface Single : Data {
            data class Insert(val position: TextPosition, val text: String) : Single

            data class Replace(val range: TextRange, val text: String) : Single

            data class Delete(val range: TextRange) : Single
        }

        data class Batch(val operations: List<Single>) : Data
    }
}