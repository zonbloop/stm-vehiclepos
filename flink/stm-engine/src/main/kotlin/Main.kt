package com.stm.pm

import com.stm.pm.pipelines.vehicle.*
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.runtime.state.filesystem.FsStateBackend

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment

fun main(args: Array<String>) {
    /** Path to store checkpoint data. In production, please set this to a HDFS-like filesystem. */
    val env = StreamExecutionEnvironment.getExecutionEnvironment()

    val parameters = ParameterTool.fromArgs(args)
    env.config.globalJobParameters = parameters //make parameters available in the web interface


    val processType = parameters.get("process-type")

    val pipeline = when(processType) {
        "vehiclePositions" -> VehiclePositionSilverPipeline(env, parameters)
        else -> null
    }

    pipeline?.let {
        it.buildPipeline()

        //execute program
        env.execute("STM " + it.pipelineName)
    }
    //println("Hello World!")
}