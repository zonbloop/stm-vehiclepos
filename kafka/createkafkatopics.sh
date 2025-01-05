#!/bin/bash

# Kafka broker container name
BROKER_CONTAINER="broker"

BROKER="broker:9092"

TOPICS=(
  "cat_stop_times"
  "cat_stops"
  "cat_routes"
  "gtfs_realtime"
  "gtfs_anomalies"
  "gtfs_realtime_enriched"
  "vehiclepos"
)

# Number of partitions
PARTITIONS=4

REPLICATION_FACTOR=1

KAFKA_TOPICS_SCRIPT="kafka-topics"

# Create each topic
for TOPIC in "${TOPICS[@]}"; do
  echo "Creating topic: $TOPIC with $PARTITIONS partitions..."
  docker exec "$BROKER_CONTAINER" "$KAFKA_TOPICS_SCRIPT" --create \
    --bootstrap-server "$BROKER" \
    --replication-factor "$REPLICATION_FACTOR" \
    --partitions "$PARTITIONS" \
    --topic "$TOPIC"

  if [ $? -eq 0 ]; then
    echo "Topic '$TOPIC' created successfully."
  else
    echo "Failed to create topic '$TOPIC'."
  fi
done

echo "All topics creation attempted."