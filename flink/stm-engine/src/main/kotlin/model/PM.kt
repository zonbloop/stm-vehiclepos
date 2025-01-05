package com.stm.pm.model

import com.stm.pm.serde.typeutils.dataclass.OptionalDataClassTypeInfoFactory
import com.stm.pm.util.toOptional
import org.apache.flink.api.common.typeinfo.TypeInfo
import java.util.Optional

object PM {
    data class VehicleRecord(
        val id: String,
        val vehicle: VehicleDetails
    )
    
    data class VehicleDetails(
        val currentStatus: String?,
        val currentStopSequence: Int?,
        val occupancyStatus: String?,
        val position: Position?,
        val timestamp: String?,
        val trip: TripDetails?,
        val vehicle: VehicleInfo?
    )
    
    data class Position(
        val latitude: Double?,
        val longitude: Double?,
        val speed: Double?
    )
    
    data class TripDetails(
        val routeId: String?,
        val startDate: String?,
        val startTime: String?,
        val tripId: String?
    )
    
    data class VehicleInfo(
        val id: String?
    )
    
    data class FlattenedVehicleRecord(
        val id: String,
        val currentStatus: String?,
        val currentStopSequence: Int?,
        val occupancyStatus: String?,
        val latitude: Double?,
        val longitude: Double?,
        val speed: Double?,
        val timestamp: String?,
        val routeId: String?,
        val startDate: String?,
        val startTime: String?,
        val tripId: String?,
        val vehicleId: String?,
        // Stop Times
        var delay: String? = null,
        var stopId: String? = null,
        // Stop
        var stopName: String? = null,
        // Route
        var routeName: String? = null,
        var stopLat: Double? = null,
        var stopLon: Double? = null,
        var anomaly: Boolean = false

    )
}