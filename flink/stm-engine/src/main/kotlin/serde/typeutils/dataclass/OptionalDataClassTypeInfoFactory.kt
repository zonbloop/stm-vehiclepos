package com.stm.pm.serde.typeutils.dataclass

import org.apache.flink.api.common.typeinfo.TypeInfoFactory
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.java.typeutils.TypeExtractionUtils
import java.lang.reflect.Type
import kotlin.reflect.full.starProjectedType

class OptionalDataClassTypeInfoFactory<T: Any>: TypeInfoFactory<T>() {

    @Suppress("UNCHECKED_CAST")
    override fun createTypeInfo(
        t: Type,
        genericParameters: MutableMap<String, TypeInformation<*>>
    ): TypeInformation<T>? {
        val klass = TypeExtractionUtils.typeToClass<T>(t).kotlin
        if (!klass.isData) {
            return null
        }
        return createTypeInformation(klass.starProjectedType) as TypeInformation<T>
    }
}