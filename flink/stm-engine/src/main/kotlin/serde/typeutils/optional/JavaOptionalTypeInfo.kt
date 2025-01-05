package com.stm.pm.serde.typeutils.optional

import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.serialization.SerializerConfig
import org.apache.flink.api.common.typeinfo.AtomicType
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.common.typeutils.TypeComparator
import org.apache.flink.api.common.typeutils.TypeSerializer
import java.util.Optional

@Suppress("UNCHECKED_CAST")
class JavaOptionalTypeInfo<A: Any>(private val elemTypeInfo: TypeInformation<A>?):
    TypeInformation<Optional<A>>(), AtomicType<Optional<A>> {

    override fun equals(other: Any?): Boolean =
        if (other is JavaOptionalTypeInfo<*>)
            other.canEqual(this) && elemTypeInfo == other.elemTypeInfo
        else false

    override fun hashCode(): Int = elemTypeInfo.hashCode()

    override fun toString(): String = "Option[$elemTypeInfo]"

    override fun isBasicType(): Boolean = false

    override fun isTupleType(): Boolean = false

    override fun getArity(): Int = 1

    override fun getTotalFields(): Int = 1

    override fun getTypeClass(): Class<Optional<A>> = Optional::class.java as Class<Optional<A>>

    override fun isKeyType(): Boolean = elemTypeInfo?.isKeyType ?: false

    @Deprecated("Deprecated in Flink 1.19")
    override fun createSerializer(config: ExecutionConfig?): TypeSerializer<Optional<A>> =
        if(elemTypeInfo == null)
            OptionalSerializer(NothingSerializer()) as TypeSerializer<Optional<A>>
        else
            OptionalSerializer(elemTypeInfo.createSerializer(config))

    override fun canEqual(obj: Any?): Boolean = obj is JavaOptionalTypeInfo<*>

    override fun createComparator(sortOrderAscending: Boolean, executionConfig: ExecutionConfig?): TypeComparator<Optional<A>> =
        if (isKeyType) {
            val elemComparator = (elemTypeInfo as AtomicType<A>).createComparator(sortOrderAscending, executionConfig)
            OptionalTypeComparator<A>(sortOrderAscending, elemComparator)
        } else {
            throw UnsupportedOperationException("Element type that doesn't support ")
        }

    override fun createSerializer(config: SerializerConfig): TypeSerializer<Optional<A>> =
        if(elemTypeInfo == null)
            OptionalSerializer(NothingSerializer()) as TypeSerializer<Optional<A>>
        else
            OptionalSerializer(elemTypeInfo.createSerializer(config))

    override fun getGenericParameters(): MutableMap<String, TypeInformation<*>> =
        mutableMapOf("A" to elemTypeInfo!!)
}