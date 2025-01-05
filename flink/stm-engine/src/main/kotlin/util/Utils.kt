package com.stm.pm.util

import com.stm.pm.model.PM
import java.util.Optional
import java.util.UUID

fun <T: Any> T?.toOptional(): Optional<T> = Optional.ofNullable(this)

object Utils {
    val KAFKA_GROUP_ID = UUID.randomUUID().toString()

    const val DEFAULT_TRANSACTION_PREFIX = "trx-prefix"
    const val DEFAULT_TRANSACTION_TIMEOUT = "900000"
    const val DEFAULT_WINDOWS_TIME = 1L
    const val DEFAULT_CP_ASSOC_TIME = 10L
    const val DEFAULT_CP_ASSOC_COUNT = 2L

    const val STOP_TIMES_TOPIC = "cat_stop_times"
    const val STOPS_TOPIC = "cat_stops"
    const val ROUTES_TOPIC = "cat_routes"

    const val VEHICLE_TOPIC = "gtfs_realtime"
    const val VEHICLE_ANOMALY_TOPIC = "gtfs_anomalies"
    const val VEHICLE_ENRICHED_TOPIC = "gtfs_realtime_enriched"
    
}