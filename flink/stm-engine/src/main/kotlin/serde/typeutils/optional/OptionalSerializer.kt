package com.stm.pm.serde.typeutils.optional

import org.apache.flink.api.common.typeutils.TypeSerializer
import org.apache.flink.api.common.typeutils.TypeSerializerSnapshot
import org.apache.flink.core.memory.DataInputView
import org.apache.flink.core.memory.DataOutputView
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

class OptionalSerializer<A: Any>(val elemSerializer: TypeSerializer<A>?): TypeSerializer<Optional<A>>() {

    override fun equals(other: Any?): Boolean =
        if (other is OptionalSerializer<*>)
            elemSerializer!! == other.elemSerializer
        else false

    override fun hashCode(): Int = elemSerializer.hashCode()

    override fun isImmutableType(): Boolean = elemSerializer == null || elemSerializer.isImmutableType

    override fun duplicate(): TypeSerializer<Optional<A>> {
        val duplicatedElemSerializer = elemSerializer!!.duplicate()

        return if (duplicatedElemSerializer == elemSerializer)
            this
        else
            OptionalSerializer<A>(duplicatedElemSerializer)
    }

    override fun createInstance(): Optional<A> = Optional.empty()

    override fun copy(source: DataInputView, target: DataOutputView) {
        val isSome = source.readBoolean()
        target.writeBoolean(isSome)
        if (isSome){
            elemSerializer!!.copy(source, target)
        }
    }

    override fun getLength(): Int = -1

    override fun deserialize(source: DataInputView): Optional<A> {
        val isSome = source.readBoolean()
        return if (isSome)
            Optional.ofNullable(elemSerializer!!.deserialize(source))
        else
            Optional.empty()
    }

    override fun snapshotConfiguration(): TypeSerializerSnapshot<Optional<A>> =
        OptionalSerializerSnapshot(this)

    override fun deserialize(reuse: Optional<A>, source: DataInputView): Optional<A> = deserialize(source)

    override fun serialize(record: Optional<A>, target: DataOutputView) {
        if (record.isPresent){
            target.writeBoolean(true)
            elemSerializer!!.serialize(record.getOrNull(), target)
        } else
            target.writeBoolean(false)
    }

    override fun copy(from: Optional<A>, reuse: Optional<A>): Optional<A> = copy(from)

    override fun copy(from: Optional<A>): Optional<A> = Optional.ofNullable(from.getOrNull())

    companion object {
        private const val serialVersionUID = -8635243274072627338L
    }
}