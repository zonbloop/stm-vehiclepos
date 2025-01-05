package com.stm.pm.pipelines

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment

abstract class Pipeline(
    val env: StreamExecutionEnvironment,
    val parameters: ParameterTool,
    val pipelineName: String
) {

    abstract fun buildPipeline()
}