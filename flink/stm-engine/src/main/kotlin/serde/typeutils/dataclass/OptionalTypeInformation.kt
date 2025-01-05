package com.stm.pm.serde.typeutils.dataclass

import com.lapanthere.flink.api.kotlin.typeutils.DataClassTypeInformation
import com.stm.pm.serde.typeutils.optional.JavaOptionalTypeInfo
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.java.typeutils.ListTypeInfo
import org.apache.flink.api.java.typeutils.MapTypeInfo
import org.apache.flink.api.java.typeutils.TypeExtractor
import java.util.Optional
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.typeOf

fun createTypeInformation(type: KType): TypeInformation<*> {

    val klass = type.jvmErasure
    return when {
        klass.isData -> {
            val generics = klass.typeParameters.map { it.starProjectedType }
            val projected = type.arguments.map { it.type ?: Any::class.starProjectedType }
            val mapping = generics.zip(projected).toMap()
            val fields = klass.primaryConstructor?.parameters ?: emptyList()
            val parameters = fields.map { mapping.getOrDefault(it.type, it.type) }
            DataClassTypeInformation(
                klass.java,
                mapping.map { (key, value) -> key.javaType.typeName to createTypeInformation(value) }.toMap(),
                parameters.map { createTypeInformation(it) }.toTypedArray(),
                fields.map { it.name!! }.toTypedArray()
            )
        }

        klass.isSubclassOf(Map::class) -> {
            val (key, value) = type.arguments.map { it.type ?: Any::class.starProjectedType }
            MapTypeInfo(createTypeInformation(key), createTypeInformation(value))
        }

        klass.isSubclassOf(Collection::class) -> {
            ListTypeInfo(createTypeInformation(type.arguments.map { it.type ?: Any::class.starProjectedType }.first()))
        }

        klass.isSubclassOf(Optional::class) -> {
            JavaOptionalTypeInfo(createTypeInformation(type.arguments.map { it.type ?: Any::class.starProjectedType }
                .first()))
        }

        else -> TypeExtractor.createTypeInfo(type.javaType)
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T: Any> createTypeInformation(): TypeInformation<T> =
    createTypeInformation(typeOf<T>()) as TypeInformation<T>