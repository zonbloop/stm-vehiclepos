package com.stm.pm.util.datastream.state

import com.stm.pm.serde.typeutils.dataclass.createTypeInformation
import org.apache.flink.api.common.functions.ReduceFunction
import org.apache.flink.api.common.functions.RuntimeContext
import org.apache.flink.api.common.state.ListStateDescriptor
import org.apache.flink.api.common.state.MapState
import org.apache.flink.api.common.state.MapStateDescriptor
import org.apache.flink.api.common.state.ReducingStateDescriptor
import org.apache.flink.api.common.state.ValueState
import org.apache.flink.api.common.state.ValueStateDescriptor

inline fun <reified T: Any> RuntimeContext.getState(name: String): ValueState<T> =
    getState(ValueStateDescriptor(name, createTypeInformation<T>()))

inline fun <reified K: Any, reified V: Any> RuntimeContext.getMapState(name: String): MapState<K, V> =
    getMapState(MapStateDescriptor(name, createTypeInformation<K>(), createTypeInformation<V>()))

inline fun <reified T: Any> newListStateDescriptor(name: String): ListStateDescriptor<T> =
    ListStateDescriptor(name, createTypeInformation<T>())

inline fun <reified T: Any> newValueStateDescriptor(name: String): ValueStateDescriptor<T> =
    ValueStateDescriptor(name, createTypeInformation<T>())

inline fun <reified K: Any, reified  V: Any> newMapStateDescriptor(name: String): MapStateDescriptor<K, V> =
    MapStateDescriptor(name, createTypeInformation<K>(), createTypeInformation<V>())

inline fun <reified T: Any> newReduceStateDescriptor(name: String, rFun: ReduceFunction<T>): ReducingStateDescriptor<T> =
    ReducingStateDescriptor(name, rFun, createTypeInformation<T>())