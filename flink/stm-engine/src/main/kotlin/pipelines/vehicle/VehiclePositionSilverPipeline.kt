package com.stm.pm.pipelines.vehicle

import com.stm.pm.functions.enrich.*
import com.stm.pm.model.Enrichment
import com.stm.pm.model.PM
import com.stm.pm.pipelines.Pipeline
import com.stm.pm.serde.JSONDeserializationSchema
import com.stm.pm.serde.JSONSerializationSchema
import com.stm.pm.util.Utils
import org.apache.flink.api.common.eventtime.WatermarkStrategy
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.connector.kafka.source.KafkaSource
import org.apache.flink.connector.kafka.sink.KafkaSink
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema
import org.apache.flink.connector.base.DeliveryGuarantee


import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.kafka.clients.producer.ProducerConfig
import java.util.*

import org.apache.flink.api.common.typeinfo.TypeInformation

import io.delta.flink.sink.DeltaSink
import org.apache.flink.core.fs.Path
import org.apache.hadoop.conf.Configuration
import org.apache.flink.table.types.logical.*
import org.apache.flink.table.data.GenericRowData
import org.apache.flink.table.data.RowData
import org.apache.flink.types.RowKind
import org.apache.flink.table.data.StringData
import org.apache.kafka.clients.consumer.OffsetResetStrategy

class VehiclePositionSilverPipeline (env: StreamExecutionEnvironment, parameters: ParameterTool):
    Pipeline(env, parameters, "Vehicle Silver Pipeline") {

    override fun buildPipeline() {
        println("Initializing Flink at ${System.currentTimeMillis()}")

        env.enableCheckpointing(30000)
        val processType = parameters.get("process-type")

        val kafkaBrokers = parameters.get("kafka-brokers")
        val kafkaReader = parameters.get("kafka-brokers-reader", kafkaBrokers)
        val kafkaWriter = parameters.get("kafka-brokers-writer", kafkaBrokers)
        val kafkaGroup = parameters.get("kafka-group", Utils.KAFKA_GROUP_ID)
        val transactionTimeout = parameters.get("transaction-timeout", Utils.DEFAULT_TRANSACTION_TIMEOUT)
        val trxPrefix = parameters.get("trx-prefix", Utils.DEFAULT_TRANSACTION_PREFIX)
        val trxIdPrefix = "$processType-$trxPrefix"

        // Metrics
        val metricsTopicName = parameters.get("metrics-topic", Utils.VEHICLE_TOPIC)
        val enrichedADLS = parameters.get("enriched-adls", Utils.VEHICLE_ENRICHED_TOPIC)
        val sourceFS = "abfss://silver@databrickspersonal.dfs.core.windows.net/"
        val enrichedMetricsTopicName = parameters.get("enriched-metrics-topic", Utils.VEHICLE_ENRICHED_TOPIC)
        val anomalyMetricsTopicName = parameters.get("anomaly-metrics-topic", Utils.VEHICLE_ANOMALY_TOPIC)
        // Catalogs
        val stopTimesTopicName = parameters.get("stop-times-topic", Utils.STOP_TIMES_TOPIC)
        val stopsTopicName = parameters.get("stops-topic", Utils.STOPS_TOPIC)
        val routesTopicName = parameters.get("routes-topic", Utils.ROUTES_TOPIC)
        // Anomaly Detection
        val MAX_DELAY_THRESHOLD = 3600 // 1 hour in seconds
        val MAX_SPEED_THRESHOLD = 120.0 // 120 km/h


        val kafkaTransactionTimeout = Properties()
        kafkaTransactionTimeout.setProperty(ProducerConfig.TRANSACTION_TIMEOUT_CONFIG, transactionTimeout)
        // Disable JMX
        // kafkaTransactionTimeout.setProperty("metrics.recording.level", "NONE")
        kafkaTransactionTimeout.setProperty("enable.jmx", "false")

        // Kafka Sources
        val metricsSource = KafkaSource.builder<PM.VehicleRecord>()
            .setBootstrapServers(kafkaReader)
            .setGroupId(kafkaGroup)
            .setTopics(metricsTopicName)
            .setValueOnlyDeserializer(JSONDeserializationSchema())
            //.setStartingOffsets(OffsetsInitializer.latest())
            .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.LATEST))
            .build()

        val stopTimesCatalogSource = KafkaSource.builder<Enrichment.StopTimesCatalog>()
            .setBootstrapServers(kafkaReader)
            .setGroupId(kafkaGroup)
            .setTopics(stopTimesTopicName)
            .setValueOnlyDeserializer(JSONDeserializationSchema())
            .setStartingOffsets(OffsetsInitializer.earliest())
            .build()

        val stopsCatalogSource = KafkaSource.builder<Enrichment.StopsCatalog>()
            .setBootstrapServers(kafkaReader)
            .setGroupId(kafkaGroup)
            .setTopics(stopsTopicName)
            .setValueOnlyDeserializer(JSONDeserializationSchema())
            .setStartingOffsets(OffsetsInitializer.earliest())
            .build()

        val routesCatalogSource = KafkaSource.builder<Enrichment.RoutesCatalog>()
            .setBootstrapServers(kafkaReader)
            .setGroupId(kafkaGroup)
            .setTopics(routesTopicName)
            .setValueOnlyDeserializer(JSONDeserializationSchema())
            .setStartingOffsets(OffsetsInitializer.earliest())
            .build()

        // ADLS Sink
        val rowType: RowType = createVehicleMetricSilverRowType()
        val config = Configuration()
        config.set("delta.writer.buffer-size", "10000")
        val deltaSink = DeltaSink.forRowData(
            Path(sourceFS +enrichedADLS ),
            config,
            rowType)
//            .withWriterBufferSize(10000)
            .build()

        // Kafka Sinks
        val enrichedMetricsSink = KafkaSink.builder<PM.FlattenedVehicleRecord>()
            .setBootstrapServers(kafkaWriter)
            .setRecordSerializer(
                KafkaRecordSerializationSchema.builder<PM.FlattenedVehicleRecord>()
                    .setTopic(enrichedMetricsTopicName)
                    .setValueSerializationSchema(JSONSerializationSchema<PM.FlattenedVehicleRecord>())
                    .build()
            )
            .setDeliveryGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
            .setTransactionalIdPrefix(trxIdPrefix)
            .setKafkaProducerConfig(kafkaTransactionTimeout)
            .build()
        
        val anomalySink = KafkaSink.builder<PM.FlattenedVehicleRecord>()
        .setBootstrapServers(kafkaWriter)
        .setRecordSerializer(
            KafkaRecordSerializationSchema.builder<PM.FlattenedVehicleRecord>()
                .setTopic(anomalyMetricsTopicName)
                .setValueSerializationSchema(JSONSerializationSchema<PM.FlattenedVehicleRecord>())
                .build()
        )
        .setDeliveryGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
        .setTransactionalIdPrefix(trxIdPrefix)
        .setKafkaProducerConfig(kafkaTransactionTimeout)
        .build()

        // Flink Streams

        val metricsStream = env.fromSource(
            metricsSource,
            WatermarkStrategy.noWatermarks(),
            "Vehicle Metrics Source"
        )
        .uid("metrics_source")
        
        val flattenedStream = metricsStream
            .filter { record -> record.vehicle.currentStatus != "IN_TRANSIT_TO"}
            .name("Filter STOPPED_AT Records")
            .map { vehicleRecord ->
            // Extract nested fields and map to flattened structure
            PM.FlattenedVehicleRecord(
                id = vehicleRecord.id,
                currentStatus = vehicleRecord.vehicle.currentStatus,
                currentStopSequence = vehicleRecord.vehicle.currentStopSequence,
                occupancyStatus = vehicleRecord.vehicle.occupancyStatus,
                latitude = vehicleRecord.vehicle.position?.latitude,
                longitude = vehicleRecord.vehicle.position?.longitude,
                speed = vehicleRecord.vehicle.position?.speed,
                timestamp = vehicleRecord.vehicle.timestamp,
                routeId = vehicleRecord.vehicle.trip?.routeId,
                startDate = vehicleRecord.vehicle.trip?.startDate,
                startTime = vehicleRecord.vehicle.trip?.startTime,
                tripId = vehicleRecord.vehicle.trip?.tripId,
                vehicleId = vehicleRecord.vehicle.vehicle?.id
            )
            }
            .keyBy {"${it.tripId},${it.currentStopSequence}"}


        //stopTimes
        val stopTimesStream = env.fromSource(
            stopTimesCatalogSource,
            WatermarkStrategy.noWatermarks(),
            "stopTimes Catalog Source"
        )
        .uid("stoptime_catalog_source")
        .keyBy {"${it.trip_id},${it.stop_sequence}"}

        //stops
        val stopsStream = env.fromSource(
            stopsCatalogSource,
            WatermarkStrategy.noWatermarks(),
            "Stops Catalog Source"
        )
        .uid("stops_catalog_source")
        .keyBy { "${it.stop_id}" }

        //routes
        val routesStream = env.fromSource(
            routesCatalogSource,
            WatermarkStrategy.noWatermarks(),
            "Routes Catalog Source"
        )
        .uid("route_catalog_source")
        .keyBy{"${it.route_id}"}

        // Flink Enriched Streams
        val metricsWithStopsTimeStream = stopTimesStream
            .connect(flattenedStream)
            .flatMap(StopTimesEnrichmentFunction())
            .name("Enrich With Stops Times")
            .uid("enrich_metrics_with_stops_times")
            .keyBy{"${it.stopId}"}

        val metricsWithStopsStream = stopsStream
            .connect(metricsWithStopsTimeStream)
            .flatMap(StopsEnrichmentFunction())
            .name("Enrich With Stops")
            .uid("enrich_metrics_with_stops")
            .keyBy{"${it.routeId}"}

        val metricsWithRoutesStream = routesStream
            .connect(metricsWithStopsStream)
            .flatMap(RoutesEnrichmentFunction())
            .name("Enrich With Routes")
            .uid("enrich_metrics_with_routes")

        // Flink Sinks
        val rowDataStream = metricsWithRoutesStream
            .map { metric -> mapToRowData(metric, rowType) }
            .returns(TypeInformation.of(RowData::class.java))
            .startNewChain()

        rowDataStream
            .sinkTo(deltaSink)
            .name("Enriched Metrics Sink")
            .uid("enriched_metrics_sink")
/*
        metricsWithRoutesStream
            .filter { it.anomaly }
            .sinkTo(anomalySink)
            .name("Anomaly Sink")
        
        metricsWithRoutesStream
            .filter { !it.anomaly }
            .sinkTo(enrichedMetricsSink)
            .name("Enriched Metrics Sink")
*/        

    }
}

fun createVehicleMetricSilverRowType(): RowType {
    return RowType.of(
        arrayOf(
            VarCharType(VarCharType.MAX_LENGTH), // id
            VarCharType(VarCharType.MAX_LENGTH), // currentStatus
            IntType(),                            // currentStopSequence
            VarCharType(VarCharType.MAX_LENGTH), // occupancyStatus
            DoubleType(),                         // latitude
            DoubleType(),                         // longitude
            DoubleType(),                         // speed
            VarCharType(VarCharType.MAX_LENGTH), // timestamp
            VarCharType(VarCharType.MAX_LENGTH), // routeId
            VarCharType(VarCharType.MAX_LENGTH), // startDate
            VarCharType(VarCharType.MAX_LENGTH), // startTime
            VarCharType(VarCharType.MAX_LENGTH), // tripId
            VarCharType(VarCharType.MAX_LENGTH), // vehicleId
            VarCharType(VarCharType.MAX_LENGTH), // delay
            VarCharType(VarCharType.MAX_LENGTH), // stopId
            VarCharType(VarCharType.MAX_LENGTH), // stopName
            VarCharType(VarCharType.MAX_LENGTH), // routeName
            DoubleType(),                         // stopLat
            DoubleType(),                         // stopLon
            BooleanType()                         // anomaly
        ),
        arrayOf(
            "id",
            "currentStatus",
            "currentStopSequence",
            "occupancyStatus",
            "latitude",
            "longitude",
            "speed",
            "timestamp",
            "routeId",
            "startDate",
            "startTime",
            "tripId",
            "vehicleId",
            "delay",
            "stopId",
            "stopName",
            "routeName",
            "stopLat",
            "stopLon",
            "anomaly"
        )
    )
}

fun mapToRowData(metric: PM.FlattenedVehicleRecord, rowType: RowType): RowData {
    val row = GenericRowData(RowKind.INSERT, rowType.fieldCount)

    // Map each field to its position in the RowType
    row.setField(0, StringData.fromString(metric.id))
    row.setField(1, StringData.fromString(metric.currentStatus))
    row.setField(2, metric.currentStopSequence)
    row.setField(3, StringData.fromString(metric.occupancyStatus))
    row.setField(4, metric.latitude)
    row.setField(5, metric.longitude)
    row.setField(6, metric.speed)
    row.setField(7, StringData.fromString(metric.timestamp))
    row.setField(8, StringData.fromString(metric.routeId))
    row.setField(9, StringData.fromString(metric.startDate))
    row.setField(10, StringData.fromString(metric.startTime))
    row.setField(11, StringData.fromString(metric.tripId))
    row.setField(12, StringData.fromString(metric.vehicleId))
    row.setField(13, StringData.fromString(metric.delay))
    row.setField(14, StringData.fromString(metric.stopId))
    row.setField(15, StringData.fromString(metric.stopName))
    row.setField(16, StringData.fromString(metric.routeName))
    row.setField(17, metric.stopLat)
    row.setField(18, metric.stopLon)
    row.setField(19, metric.anomaly)

    return row
}
