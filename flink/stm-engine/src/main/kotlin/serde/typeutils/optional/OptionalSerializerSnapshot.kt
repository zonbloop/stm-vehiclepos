package com.stm.pm.serde.typeutils.optional

import org.apache.flink.api.common.typeutils.CompositeTypeSerializerSnapshot
import org.apache.flink.api.common.typeutils.TypeSerializer
import java.util.Optional

class OptionalSerializerSnapshot<E: Any>: CompositeTypeSerializerSnapshot<Optional<E>, OptionalSerializer<E>> {

    private val VERSION = 2

    constructor(): super()

    constructor(serializerInstance: OptionalSerializer<E>): super(serializerInstance)

    override fun getCurrentOuterSnapshotVersion(): Int = VERSION

    @Suppress("UNCHECKED_CAST")
    override fun createOuterSerializerWithNestedSerializers(nestedSerializers: Array<out TypeSerializer<*>>):
            OptionalSerializer<E> {
        val nestedSerializer =nestedSerializers[0] as TypeSerializer<E>
        return OptionalSerializer(nestedSerializer)
    }

    override fun getNestedSerializers(outerSerializer: OptionalSerializer<E>): Array<TypeSerializer<*>> =
        arrayOf(outerSerializer.elemSerializer!!)
}