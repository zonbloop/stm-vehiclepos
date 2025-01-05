package com.stm.pm.serde.typeutils.optional

import org.apache.flink.api.common.typeutils.SimpleTypeSerializerSnapshot
import org.apache.flink.api.common.typeutils.TypeSerializer
import org.apache.flink.api.common.typeutils.TypeSerializerSnapshot
import org.apache.flink.core.memory.DataInputView
import org.apache.flink.core.memory.DataOutputView

class NothingSerializer: TypeSerializer<Any>() {

    companion object {
        class NothingSerializerSnapshot: SimpleTypeSerializerSnapshot<Any>({ NothingSerializer() })
    }

    override fun equals(other: Any?): Boolean = other is NothingSerializer

    override fun hashCode(): Int = NothingSerializer::class.java.hashCode()

    override fun isImmutableType(): Boolean = true

    override fun duplicate(): TypeSerializer<Any> = this

    override fun createInstance(): Any = -1

    override fun copy(source: DataInputView?, target: DataOutputView?) {
        throw RuntimeException("This must not be used. You encountered a bug.")
    }

    override fun getLength(): Int = -1

    override fun deserialize(source: DataInputView?): Any {
        throw RuntimeException("This must not be used. You encountered a bug.")
    }

    override fun snapshotConfiguration(): TypeSerializerSnapshot<Any> = NothingSerializerSnapshot()

    override fun deserialize(reuse: Any?, source: DataInputView?): Any {
        throw RuntimeException("This must not be used. You encountered a bug.")
    }

    override fun serialize(record: Any?, target: DataOutputView?) {
        throw RuntimeException("This must not be used. You encountered a bug.")
    }

    override fun copy(from: Any?, reuse: Any?): Any = copy(from)

    override fun copy(from: Any?): Any {
        throw RuntimeException("This must not be used. You encountered a bug.")
    }
}