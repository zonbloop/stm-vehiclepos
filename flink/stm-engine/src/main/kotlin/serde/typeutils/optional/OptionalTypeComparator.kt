package com.stm.pm.serde.typeutils.optional

import org.apache.flink.api.common.typeutils.TypeComparator
import org.apache.flink.core.memory.DataInputView
import org.apache.flink.core.memory.DataOutputView
import org.apache.flink.core.memory.MemorySegment
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

class OptionalTypeComparator<A: Any>(private val ascending: Boolean, private val typeComparator: TypeComparator<A>):
    TypeComparator<Optional<A>>() {

    private lateinit var reference: Optional<A>

    companion object {
        const val ZeroInByte = 0.toByte()
        const val OneInByte = 1.toByte()
    }

    override fun hash(record: Optional<A>?): Int = record.hashCode()

    override fun compare(first: Optional<A>, second: Optional<A>): Int = when {
        first.isPresent && second.isPresent -> typeComparator.compare(first.getOrNull(), second.getOrNull())
        first.isPresent && !second.isPresent -> if (ascending) 1 else -1
        !first.isPresent && second.isPresent -> if (ascending) -1 else 1
        else -> 0
    }

    override fun setReference(toCompare: Optional<A>) {
        reference = toCompare
    }

    override fun equalToReference(candidate: Optional<A>): Boolean =
        compare(reference, candidate) == 0

    override fun compareToReference(referencedComparator: TypeComparator<Optional<A>>): Int =
        compare((referencedComparator as OptionalTypeComparator<A>).reference, reference)

    override fun compareSerialized(firstSource: DataInputView, secondSource: DataInputView): Int {
        val firstSome = firstSource.readBoolean()
        val secondSome = secondSource.readBoolean()

        return if (firstSome){
            if (secondSome)
                typeComparator.compareSerialized(firstSource, secondSource)
            else
                if (ascending) 1 else -1
        } else {
            if (secondSome)
                if (ascending) -1 else 1
            else
                0
        }
    }

    override fun supportsNormalizedKey(): Boolean = typeComparator.supportsNormalizedKey()

    override fun supportsSerializationWithKeyNormalization(): Boolean = false

    override fun getNormalizeKeyLen(): Int = 1 + typeComparator.normalizeKeyLen

    override fun isNormalizedKeyPrefixOnly(keyBytes: Int): Boolean =
        typeComparator.isNormalizedKeyPrefixOnly(keyBytes - 1)

    override fun invertNormalizedKey(): Boolean = !ascending

    override fun duplicate(): TypeComparator<Optional<A>> = OptionalTypeComparator(ascending, typeComparator)

    override fun extractKeys(record: Any, target: Array<Any>, index: Int): Int {
        target[index] = record
        return 1
    }

    @Suppress("UNCHECKED_CAST")
    override fun getFlatComparators(): Array<TypeComparator<Any>> =
        arrayOf(this) as Array<TypeComparator<Any>>

    override fun readWithKeyDenormalization(reuse: Optional<A>?, source: DataInputView?): Optional<A> {
        throw UnsupportedOperationException()
    }

    override fun writeWithKeyNormalization(record: Optional<A>?, target: DataOutputView?) {
        throw UnsupportedOperationException()
    }

    override fun putNormalizedKey(record: Optional<A>, target: MemorySegment, offset: Int, numBytes: Int) {
        if (numBytes >= 1) {
            if (record.isPresent) {
                target.put(offset, OneInByte)
                typeComparator.putNormalizedKey(record.getOrNull(), target, offset + 1, numBytes - 1)
            } else {
                target.put(offset, ZeroInByte)
                var i = 1
                while (i < numBytes){
                    target.put(offset + i, ZeroInByte)
                    i += 1
                }
            }
        }
    }


}