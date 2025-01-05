package com.stm.pm.model

import com.stm.pm.serde.typeutils.dataclass.OptionalDataClassTypeInfoFactory
import org.apache.flink.api.common.typeinfo.TypeInfo
import com.fasterxml.jackson.annotation.JsonProperty

object Enrichment {
    @TypeInfo(OptionalDataClassTypeInfoFactory::class)
    data class StopTimesCatalog(
        val arrival_time: String,
        val departure_time: String,
        val stop_id: String,
        val stop_sequence: String,
        val trip_id: String
    )

    @TypeInfo(OptionalDataClassTypeInfoFactory::class)
    data class StopsCatalog(
        val location_type: String,
        val parent_station: String,
        val stop_code: String,
        val stop_id: String,
        val stop_lat: String,
        val stop_lon: String,
        val stop_name: String,
        val stop_url: String,
        val wheelchair_boarding: String
    )

    @TypeInfo(OptionalDataClassTypeInfoFactory::class)
    data class RoutesCatalog(
        val agency_id: String,
        val route_color: String,
        val route_id: String,
        val route_long_name: String,
        val route_short_name: String,
        val route_text_color: String,
        val route_type: String,
        val route_url: String
    )
}