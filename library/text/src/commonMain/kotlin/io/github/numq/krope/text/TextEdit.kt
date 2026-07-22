package io.github.numq.krope.text

import kotlin.time.Clock
import kotlin.time.Instant

sealed interface TextEdit {
    val revision: TextRevision

    val data: Data

    data class User(override val revision: TextRevision, override val data: Data) : TextEdit

    data class System(override val revision: TextRevision, override val data: Data) : TextEdit

    fun toSystemOperation(snapshot: TextSnapshot): TextOperation.System = TextOperation.System(
        revision = snapshot.revision, data = data.toOperationData(snapshot = snapshot)
    )

    fun invert(): TextEdit = when (this) {
        is User -> User(revision = revision, data = data.invert())

        is System -> System(revision = revision, data = data.invert())
    }

    sealed interface Data {
        val startByte: Int

        val oldEndByte: Int

        val newEndByte: Int

        val startPosition: TextPosition

        val oldEndPosition: TextPosition

        val newEndPosition: TextPosition

        val instant: Instant

        fun toOperationData(snapshot: TextSnapshot): TextOperation.Data = when (this) {
            is Single.Insert -> TextOperation.Data.Single.Insert(
                position = startPosition.coerceIn(snapshot), text = insertedText
            )

            is Single.Delete -> TextOperation.Data.Single.Delete(
                range = TextRange(
                    start = startPosition, end = oldEndPosition
                ).coerceIn(snapshot)
            )

            is Single.Replace -> TextOperation.Data.Single.Replace(
                range = TextRange(
                    start = startPosition, end = oldEndPosition
                ).coerceIn(snapshot), text = newText
            )

            is Batch -> TextOperation.Data.Batch(operations = singles.mapNotNull { single ->
                single.toOperationData(snapshot = snapshot) as? TextOperation.Data.Single
            })
        }

        fun isEffectivelyEmpty(): Boolean = when (this) {
            is Single.Insert -> insertedText.isEmpty()

            is Single.Delete -> deletedText.isEmpty() || startPosition == oldEndPosition

            is Single.Replace -> (oldText == newText) || (startPosition == oldEndPosition && newText.isEmpty())

            is Batch -> singles.all(Single::isEffectivelyEmpty)
        }

        fun invert(): Data = when (this) {
            is Single.Insert -> Single.Delete(
                startPosition = startPosition,
                oldEndPosition = newEndPosition,
                deletedText = insertedText,
                startByte = startByte,
                oldEndByte = newEndByte
            )

            is Single.Replace -> Single.Replace(
                startPosition = startPosition,
                oldEndPosition = newEndPosition,
                newEndPosition = oldEndPosition,
                oldText = newText,
                newText = oldText,
                startByte = startByte,
                oldEndByte = newEndByte,
                newEndByte = oldEndByte
            )

            is Single.Delete -> Single.Insert(
                startPosition = startPosition,
                newEndPosition = oldEndPosition,
                insertedText = deletedText,
                startByte = startByte,
                newEndByte = oldEndByte
            )

            is Batch -> Batch(singles.mapNotNull { single ->
                single.invert() as? Single
            }.reversed())
        }

        sealed interface Single : Data {
            val oldText: String?

            val newText: String?

            data class Insert(
                override val startPosition: TextPosition,
                override val newEndPosition: TextPosition,
                val insertedText: String,
                override val startByte: Int,
                override val newEndByte: Int,
            ) : Single {
                override val oldEndPosition = startPosition

                override val oldEndByte = startByte

                override val oldText: String? = null

                override val newText = insertedText

                override val instant = Clock.System.now()
            }

            data class Replace(
                override val startPosition: TextPosition,
                override val oldEndPosition: TextPosition,
                override val newEndPosition: TextPosition,
                override val oldText: String,
                override val newText: String,
                override val startByte: Int,
                override val oldEndByte: Int,
                override val newEndByte: Int,
            ) : Single {
                override val instant = Clock.System.now()
            }

            data class Delete(
                override val startPosition: TextPosition,
                override val oldEndPosition: TextPosition,
                val deletedText: String,
                override val startByte: Int,
                override val oldEndByte: Int,
            ) : Single {
                override val newEndPosition = startPosition

                override val newEndByte = startByte

                override val oldText = deletedText

                override val newText: String? = null

                override val instant = Clock.System.now()
            }
        }

        data class Batch(val singles: List<Single>) : Data {
            init {
                require(singles.isNotEmpty()) { "Batch edit cannot be empty" }
            }

            override val startByte = singles.first().startByte

            override val startPosition = singles.first().startPosition

            override val oldEndByte = singles.last().oldEndByte

            override val oldEndPosition = singles.last().oldEndPosition

            override val newEndByte = singles.last().newEndByte

            override val newEndPosition = singles.last().newEndPosition

            override val instant = singles.first().instant
        }
    }
}