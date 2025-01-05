package com.stm.pm.functions.enrich

import com.stm.pm.model.Enrichment
import com.stm.pm.model.PM
import com.stm.pm.util.datastream.state.getState
import org.apache.flink.api.common.functions.OpenContext
import org.apache.flink.api.common.state.ValueState
import org.apache.flink.streaming.api.functions.co.RichCoFlatMapFunction
import org.apache.flink.util.Collector
import kotlin.math.*

class StopsEnrichmentFunction : RichCoFlatMapFunction<Enrichment.StopsCatalog, PM.FlattenedVehicleRecord, PM.FlattenedVehicleRecord>() {

    private lateinit var coreState: ValueState<Enrichment.StopsCatalog>

    override fun open(openContext: OpenContext?) {
        coreState = runtimeContext.getState("source_stops_enrichment_data")
    }

    override fun flatMap1(value: Enrichment.StopsCatalog, out: Collector<PM.FlattenedVehicleRecord>) {
        coreState.update(value)
    }

    override fun flatMap2(value: PM.FlattenedVehicleRecord, out: Collector<PM.FlattenedVehicleRecord>) {
        coreState.value()?.let { stop ->
            // Enrich the vehicle record with stop details
            value.stopName = stop.stop_name
            value.stopLat = stop.stop_lat.toDoubleOrNull()
            value.stopLon = stop.stop_lon.toDoubleOrNull()

            // Calculate distance to the stop
            val stopLat = value.stopLat
            val stopLon = value.stopLon
            val vehicleLat = value.latitude
            val vehicleLon = value.longitude

            if (stopLat != null && stopLon != null && vehicleLat != null && vehicleLon != null) {
                val distanceToStop = calculateDistance(vehicleLat, vehicleLon, stopLat, stopLon)
                val MAX_ROUTE_DEVIATION_THRESHOLD = 500.0 // in meters

                // Flag anomaly if the distance exceeds the threshold
                if (distanceToStop > MAX_ROUTE_DEVIATION_THRESHOLD) {
                    value.anomaly = true
                }
            }
        }

        out.collect(value)
    }

    // Haversine formula to calculate the distance between two coordinates
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371e3 // Earth's radius in meters
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val a = sin(deltaPhi / 2) * sin(deltaPhi / 2) +
                cos(phi1) * cos(phi2) *
                sin(deltaLambda / 2) * sin(deltaLambda / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return R * c // Distance in meters
    }
}
