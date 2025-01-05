package com.stm.pm.serde

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.apache.flink.api.common.serialization.SerializationSchema

class JSONSerializationSchema<T>: SerializationSchema<T> {

    @Transient
    private lateinit var mapper: ObjectMapper

    override fun open(context: SerializationSchema.InitializationContext?) {
        mapper = ObjectMapper()
            .registerKotlinModule()
            .registerModules(Jdk8Module())
    }

    override fun serialize(element: T): ByteArray =
        mapper.writeValueAsBytes(element)
}