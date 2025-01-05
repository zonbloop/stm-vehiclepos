import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    application
    kotlin("jvm") version "1.9.23"
    id("com.github.johnrengelman.shadow") version "8.1.1" // shadow plugin to produce fat JARs
}

group = "com.stm.pm"
version = "1.0-SNAPSHOT"

val flinkVersion = "1.19.0"
val jacksonVersion = "2.15.2"
val jexlVersion = "3.2.1"
val mathVersion = "3.6.1"
val hadoopVersion = "3.3.2"

val logbackVersion = "1.4.12"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

application {
    mainClass = "com.stm.pm.MainKt"
}

repositories {
    mavenCentral()
}

// NOTE: We cannot use "compileOnly" or "shadow" configurations since then we could not run code
// in the IDE or with "gradle run". We also cannot exclude transitive dependencies from the
// shadowJar yet (see https://github.com/johnrengelman/shadow/issues/159).
// -> Explicitly define the // libraries we want to be included in the "flinkShadowJar" configuration!
val flinkShadowJar: Configuration by configurations.creating // dependencies which go into the shadowJar
// always exclude these (also from transitive dependencies) since they are provided by Flink
flinkShadowJar.exclude(group = "org.apache.flink", module = "force-shading")
flinkShadowJar.exclude(group = "com.google.code.findbugs", module = "jsr305")
flinkShadowJar.exclude(group = "org.slf4j")
flinkShadowJar.exclude(group = "org.apache.logging.log4j")

dependencies {
    // --------------------------------------------------------------
    // Compile-time dependencies that should NOT be part of the
    // shadow jar and are provided in the lib folder of Flink
    // --------------------------------------------------------------
    implementation("org.apache.flink:flink-streaming-java:${flinkVersion}")
    implementation("org.apache.flink:flink-table-api-java-bridge:${flinkVersion}")
    implementation("org.apache.flink:flink-clients:${flinkVersion}")
    implementation("org.apache.flink:flink-runtime-web:${flinkVersion}")
    implementation("org.apache.flink:flink-connector-base:${flinkVersion}")
    implementation("org.apache.flink:flink-metrics-datadog:${flinkVersion}") 

    //implementation("org.apache.flink:flink-sql-connector-kafka:3.2.0-1.19")
    //implementation("io.delta:delta-flink:3.2.1")
    implementation("org.apache.flink:flink-parquet:${flinkVersion}")
    implementation("org.apache.hadoop:hadoop-common:${hadoopVersion}")
    implementation("org.apache.flink:flink-connector-files:${flinkVersion}")
    implementation("org.apache.hadoop:hadoop-azure:${hadoopVersion}")
    implementation("org.apache.flink:flink-azure-fs-hadoop:${flinkVersion}")
    implementation("org.apache.hadoop:hadoop-hdfs:${hadoopVersion}")
    implementation("org.apache.hadoop:hadoop-client:${hadoopVersion}")
    implementation("org.wildfly.openssl:wildfly-openssl:2.2.5.Final")
    implementation("org.elasticsearch.client:elasticsearch-rest-high-level-client:7.17.12") // Adjust version to match your Elasticsearch version
    implementation("org.apache.httpcomponents:httpclient:4.5.13") // For HTTP connections
    implementation("org.apache.httpcomponents:httpasyncclient:4.1.5")
    // JSON Parsing Library
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")



  
    // Dependencies for Hadoop/Azure, must be part of the shadow jar
    flinkShadowJar("io.delta:delta-flink:3.2.1")
    flinkShadowJar("io.delta:delta-standalone_2.12:3.2.1"){
        exclude(group = "io.delta", module = "delta-flink")
    }
    flinkShadowJar("io.delta:delta-kernel-api:4.0.0rc1")

    flinkShadowJar("org.jetbrains.kotlin:kotlin-reflect:1.9.23") // Match the Kotlin version in your build
    flinkShadowJar("org.jetbrains.kotlin:kotlin-stdlib:1.9.23")
    flinkShadowJar("org.wildfly.openssl:wildfly-openssl:2.2.5.Final")
    flinkShadowJar("org.apache.flink:flink-parquet:${flinkVersion}")



    flinkShadowJar("org.apache.hadoop:hadoop-common:${hadoopVersion}")
    flinkShadowJar("org.apache.hadoop:hadoop-auth:${hadoopVersion}")
    flinkShadowJar("org.apache.hadoop:hadoop-azure:${hadoopVersion}")
    flinkShadowJar("org.apache.hadoop:hadoop-hdfs:${hadoopVersion}")
    flinkShadowJar("org.apache.hadoop:hadoop-client:${hadoopVersion}")
    
    flinkShadowJar("org.apache.flink:flink-azure-fs-hadoop:${flinkVersion}")
    // Elasticsearch Dependencies
    flinkShadowJar("org.elasticsearch.client:elasticsearch-rest-high-level-client:7.17.12")
    flinkShadowJar("org.apache.httpcomponents:httpasyncclient:4.1.5")
    flinkShadowJar("org.apache.httpcomponents:httpclient:4.5.13")
    flinkShadowJar("org.apache.httpcomponents:httpcore-nio:4.4.15")
    flinkShadowJar("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    //Conexión con Cassandra
    implementation("org.apache.flink:flink-connector-cassandra_2.12:1.16.0")  
    implementation("com.datastax.cassandra:cassandra-driver-core:4.0.0")  // Controlador de Cassandra
    implementation("org.apache.cassandra:java-driver-core:4.18.1")  // Controlador de Cassandra
    flinkShadowJar("org.apache.flink:flink-connector-cassandra_2.12:1.16.0")  
    flinkShadowJar("com.datastax.cassandra:cassandra-driver-core:4.0.0")  // Controlador de Cassandra
    flinkShadowJar("org.apache.cassandra:java-driver-core:4.18.1")  // Controlador de Cassandra


    // JSON Parsing (optional)
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    



    // --------------------------------------------------------------
    // Dependencies that should be part of the shadow jar, e.g.
    // connectors. These must be in the flinkShadowJar configuration!
    // --------------------------------------------------------------
    flinkShadowJar("org.apache.flink:flink-connector-kafka:3.1.0-1.18")
    flinkShadowJar("org.apache.flink:flink-metrics-dropwizard:${flinkVersion}")
    flinkShadowJar("com.fasterxml.jackson.module:jackson-module-kotlin:${jacksonVersion}")
    flinkShadowJar("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:${jacksonVersion}")

    flinkShadowJar("org.apache.commons:commons-jexl3:${jexlVersion}")
    flinkShadowJar("org.apache.commons:commons-math3:${mathVersion}")
    flinkShadowJar("com.lapanthere:flink-kotlin:0.3.0")

    runtimeOnly("ch.qos.logback:logback-classic:${logbackVersion}") //Needed to have logs when running in the IDE
    flinkShadowJar("ch.qos.logback:logback-classic:${logbackVersion}")

    testImplementation(kotlin("test"))
}

// make compileOnly dependencies available for tests:
sourceSets {
    main.get().compileClasspath += flinkShadowJar
    main.get().runtimeClasspath += flinkShadowJar
    test.get().compileClasspath += flinkShadowJar
    test.get().runtimeClasspath += flinkShadowJar
}

tasks.run.get().classpath += sourceSets.main.get().runtimeClasspath
tasks.javadoc.get().classpath += flinkShadowJar

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Jar> {
    manifest {
        attributes(
            "Built-By" to System.getProperty("user.name"),
            "Build-Jdk" to System.getProperty("java.version")
        )
    }
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveClassifier.set("all")
    configurations = listOf(flinkShadowJar)
    mergeServiceFiles() // Merge service files to avoid conflicts
    isZip64 = true // Enable Zip64 to support large JARs
}

tasks.withType<ShadowJar> {
    configurations = listOf(flinkShadowJar)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}