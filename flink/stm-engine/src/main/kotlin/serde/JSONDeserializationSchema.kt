package com.stm.pm.serde

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.lapanthere.flink.api.kotlin.typeutils.createTypeInformation
import org.apache.flink.api.common.serialization.AbstractDeserializationSchema
import org.apache.flink.api.common.serialization.DeserializationSchema
import org.apache.flink.api.common.typeinfo.TypeInformation

class JSONDeserializationSchema<T>(private val ti: TypeInformation<T>): AbstractDeserializationSchema<T>(ti) {

    @Transient
    private lateinit var mapper: ObjectMapper

    override fun open(context: DeserializationSchema.InitializationContext?) {
        mapper = ObjectMapper()
            .registerKotlinModule()
            .registerModules(Jdk8Module())

        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    override fun deserialize(message: ByteArray?): T? =
    try {
        if (message == null) {
            throw IllegalArgumentException("Received null message")
        }
        val jsonString = String(message)
        //println("Deserializing message: $jsonString")
        mapper.readValue(jsonString, ti.typeClass)
    } catch (e: Exception) {
        //println("Failed to deserialize message: ${message?.let { String(it) }}")
        e.printStackTrace()
        null
    }

    companion object {
        inline operator fun <reified T: Any> invoke(): JSONDeserializationSchema<T> =
            JSONDeserializationSchema(createTypeInformation<T>())
    }
}