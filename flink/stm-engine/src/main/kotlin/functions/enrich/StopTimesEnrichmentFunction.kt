package com.stm.pm.functions.enrich

import com.stm.pm.model.Enrichment
import com.stm.pm.model.PM
import com.stm.pm.util.datastream.state.getState
import org.apache.flink.api.common.functions.OpenContext
import org.apache.flink.api.common.state.ValueState
import org.apache.flink.streaming.api.functions.co.RichCoFlatMapFunction
import org.apache.flink.util.Collector
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class StopTimesEnrichmentFunction : RichCoFlatMapFunction<Enrichment.StopTimesCatalog, PM.FlattenedVehicleRecord, PM.FlattenedVehicleRecord>() {

    private lateinit var stopState: ValueState<Enrichment.StopTimesCatalog>

    override fun open(openContext: OpenContext?) {
        stopState = runtimeContext.getState("source_stop_times_enrichment_data")
    }

    override fun flatMap1(value: Enrichment.StopTimesCatalog, out: Collector<PM.FlattenedVehicleRecord>) {
        stopState.update(value)
    }

    override fun flatMap2(value: PM.FlattenedVehicleRecord, out: Collector<PM.FlattenedVehicleRecord>) {
        stopState.value()?.let { stopTimes ->
            try {
                val arrivalTime = stopTimes.arrival_time // arrival_time as String?
                val timestampString = value.timestamp   // timestamp as String?

                if (arrivalTime != null && timestampString != null) {
                    // Parse arrival_time (HH:mm:ss) into seconds since midnight
                    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                    val arrivalTimeSeconds = LocalTime.parse(arrivalTime, formatter).toSecondOfDay()

                    // Convert timestamp from String to Long, then to seconds since midnight
                    val timestampSeconds = timestampString.toLongOrNull()?.rem(24 * 3600)

                    if (timestampSeconds != null) {
                        // Calculate delay (positive if late, negative if early)
                        value.delay = (timestampSeconds - arrivalTimeSeconds).toString() // Store as String
                    } else {
                        println("Invalid timestamp: $timestampString")
                        value.delay = null // Default or fallback value
                    }
                } else {
                    println("Missing arrival_time or timestamp: arrival_time=$arrivalTime, timestamp=$timestampString")
                    value.delay = null // Default or fallback value
                }

                // Enrich with stop_id
                value.stopId = stopTimes.stop_id
            } catch (e: Exception) {
                // Handle parsing errors or other exceptions
                println("Error calculating delay: ${e.message}")
                value.delay = null // Default or fallback value
            }
        }

        out.collect(value)
    }
}

