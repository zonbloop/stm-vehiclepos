# **Real-Time Transit Data Pipeline with Kafka and Flink**

This project demonstrates a real-time data pipeline for ingesting, processing, and analyzing transit data using GTFS Realtime feeds. The pipeline leverages Kafka for data streaming, Flink for processing, and AKHQ for monitoring, making it a robust solution for transit authorities or data engineering roles.

---

## **Features**
1. **GTFS Realtime Data Ingestion**: Fetch and parse live transit data from STM's GTFS Realtime API.
2. **Kafka Integration**: Publish GTFS data to Kafka topics for streaming.
3. **Flink Processing**: Set up for future stream processing to calculate delays, occupancy trends, and route performance.
4. **AKHQ Monitoring**: Manage and monitor Kafka topics, producers, and consumers with an intuitive UI.

---

## **Architecture**
### Components
- **Python**: Fetch and parse GTFS Realtime data.
- **Go**: Publish parsed data to Kafka.
- **Docker**: Run Kafka, Zookeeper, Flink, and AKHQ services.
- **AKHQ**: Monitor Kafka clusters and topics.

---

## **Getting Started**

### **Prerequisites**
1. **Install Required Tools**:
   - [Docker](https://www.docker.com/get-started)
   - [Go](https://go.dev/doc/install)
   - [Python 3](https://www.python.org/downloads/)
   - `protobuf-compiler`:
     ```bash
     sudo apt install protobuf-compiler
     ```
   - Python packages:
     ```bash
     pip install requests protobuf
     ```

2. **Download GTFS Realtime Protobuf Definition**:
   - Download the `gtfs-realtime.proto` file from [GTFS Realtime](https://github.com/google/transit/blob/master/gtfs-realtime/proto/gtfs-realtime.proto).

---

### **Setup Steps**

#### **1. Parse GTFS Realtime Data**
Fetch and parse GTFS Realtime data using Python:
- Compile the Protobuf file:
  ```bash
  protoc --python_out=. gtfs-realtime.proto
  ```
- Use the Python script (`getData.py`) to fetch and parse data:
  ```bash
  python3 getData.py
  ```

#### **2. Kafka Integration Using Go**
- Initialize a Go module:
  ```bash
  go mod init stmkafka
  ```
- Install dependencies:
  ```bash
  go get github.com/IBM/sarama@latest
  go get google.golang.org/protobuf/encoding/protojson
  go get google.golang.org/protobuf/proto
  ```
- Compile the Protobuf file for Go:
  ```bash
  protoc --go_out=. --go_opt=paths=source_relative gtfs-realtime.proto
  mv gtfs-realtime.pb.go gtfs_realtime/
  ```
- Run the Go producer to publish data to Kafka:
  ```bash
  go run main.go
  ```

#### **3. Docker Compose Setup**
Run Kafka, Zookeeper, Flink, and AKHQ using Docker Compose:
- Use the following `docker-compose.yml`:
  ```yaml
  version: '3.6'

  services:
    zookeeper:
      image: confluentinc/cp-zookeeper:7.4.0
      ports:
        - "2181:2181"
      environment:
        ZOOKEEPER_CLIENT_PORT: 2181

    broker:
      image: confluentinc/cp-kafka:7.4.0
      depends_on:
        - zookeeper
      ports:
        - "9092:9092"
      environment:
        KAFKA_BROKER_ID: 1
        KAFKA_ZOOKEEPER_CONNECT: "zookeeper:2181"
        KAFKA_ADVERTISED_LISTENERS: "PLAINTEXT://broker:29092,PLAINTEXT_HOST://localhost:9092"
        KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: "PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT"

    akhq:
      image: tchiotludo/akhq
      ports:
        - "8080:8080"
      environment:
        AKHQ_CONFIGURATION: |
          akhq:
            connections:
              docker-kafka-server:
                properties:
                  bootstrap.servers: "broker:29092"
  ```
- Start the services:
  ```bash
  docker compose up -d
  ```

#### **4. Monitor Kafka with AKHQ**
- Access the AKHQ UI at [http://localhost:8080](http://localhost:8080).
- Verify the `gtfs_realtime` topic and inspect data.

---

## **Usage**
1. **Fetch Realtime Data**:
   Use the Python script or Go producer to ingest GTFS Realtime data.
2. **Publish to Kafka**:
   Run the Go producer to stream data into Kafka.
3. **Monitor in AKHQ**:
   Visualize topics, producers, and consumers.

---

## **Future Enhancements**
- **Stream Processing with Flink**:
  - Aggregate and analyze delays, route performance, and occupancy trends.
- **Visualization**:
  - Build dashboards using Grafana or similar tools.

---

## **Showcase**
This project demonstrates:
1. **Real-Time Data Engineering**: Handling high-frequency transit data.
2. **Kafka Expertise**: Setting up robust streaming pipelines.
3. **Infrastructure Management**: Using Docker for scalable deployments.

---

## **Contact**
Feel free to reach out for collaboration or inquiries:
- **Name**: [Your Name]
- **Email**: [Your Email]
- **LinkedIn**: [Your LinkedIn Profile]