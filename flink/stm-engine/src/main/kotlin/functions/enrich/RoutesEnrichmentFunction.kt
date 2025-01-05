package com.stm.pm.functions.enrich

import com.stm.pm.model.Enrichment
import com.stm.pm.model.PM
import com.stm.pm.util.datastream.state.getState
import org.apache.flink.api.common.functions.OpenContext
import org.apache.flink.api.common.state.ValueState
import org.apache.flink.streaming.api.functions.co.RichCoFlatMapFunction
import org.apache.flink.util.Collector


class RoutesEnrichmentFunction : RichCoFlatMapFunction<Enrichment.RoutesCatalog, PM.FlattenedVehicleRecord, PM.FlattenedVehicleRecord>() {

    private lateinit var coreState: ValueState<Enrichment.RoutesCatalog>

    override fun open(openContext: OpenContext?) {
        coreState = runtimeContext.getState("source_routes_enrichment_data")
    }

    override fun flatMap1(value: Enrichment.RoutesCatalog, out: Collector<PM.FlattenedVehicleRecord>) {
        coreState.update(value)
    }

    override fun flatMap2(value: PM.FlattenedVehicleRecord, out: Collector<PM.FlattenedVehicleRecord>) {
        coreState.value()?.let { stopTimes ->
            value.routeName = stopTimes.route_long_name
        }

        out.collect(value)
    }
}

