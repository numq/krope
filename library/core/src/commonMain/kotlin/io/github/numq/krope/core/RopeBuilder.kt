package io.github.numq.krope.core

class RopeBuilder private constructor() {
    companion object {
        fun build(baseRope: Rope, builder: RopeBuilder.() -> Unit): Rope {
            val instance = RopeBuilder()

            instance.builder()

            return instance.buildInternal(baseRope = baseRope)
        }
    }

    private val insertOperations = ArrayList<Pair<Int, String>>()

    private val deleteOperations = ArrayList<Pair<Int, Int>>()

    fun insert(offset: Int, text: String): RopeBuilder {
        if (text.isNotEmpty()) {
            insertOperations.add(offset to text)
        }

        return this
    }

    fun delete(offset: Int, length: Int): RopeBuilder {
        if (length > 0) {
            deleteOperations.add(offset to length)
        }

        return this
    }

    private fun buildInternal(baseRope: Rope) = when {
        insertOperations.isEmpty() && deleteOperations.isEmpty() -> baseRope

        else -> baseRope.applyBatchOperations(deletions = deleteOperations, insertions = insertOperations)
    }
}