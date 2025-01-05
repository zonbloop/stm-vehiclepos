package main

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"github.com/IBM/sarama"
	"google.golang.org/protobuf/encoding/protojson"
	"google.golang.org/protobuf/proto"

	"stmkafka/gtfs_realtime"
)

const (
	apiURL      = "https://api.stm.info/pub/od/gtfs-rt/ic/v2/vehiclePositions"
	apiKey      = "" 
	kafkaBroker = "localhost:9092"
	kafkaTopic  = "gtfs_realtime"
)

func main() {
	// Fetch GTFS Realtime data
	data, err := fetchGTFSRealtime()
	if err != nil {
		log.Fatalf("Failed to fetch GTFS data: %v", err)
	}

	// Parse Protobuf and convert to JSON
	entities, err := parseProtobufToJSON(data)
	if err != nil {
		log.Fatalf("Failed to parse Protobuf: %v", err)
	}

	// Publish data to Kafka
	err = publishToKafka(entities)
	if err != nil {
		log.Fatalf("Failed to publish to Kafka: %v", err)
	}

	log.Println("Successfully published GTFS Realtime data to Kafka.")
}

func fetchGTFSRealtime() ([]byte, error) {
	client := &http.Client{}
	req, err := http.NewRequest("GET", apiURL, nil)
	if err != nil {
		return nil, fmt.Errorf("failed to create HTTP request: %v", err)
	}

	// Add API key to request headers
	req.Header.Add("apikey", apiKey)

	// Execute the HTTP request
	resp, err := client.Do(req)
	if err != nil {
		return nil, fmt.Errorf("failed to execute HTTP request: %v", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		body, _ := ioutil.ReadAll(resp.Body)
		return nil, fmt.Errorf("API returned status %d: %s", resp.StatusCode, string(body))
	}

	// Read the response body
	return ioutil.ReadAll(resp.Body)
}

func parseProtobufToJSON(data []byte) ([]map[string]interface{}, error) {
	// Parse Protobuf data into FeedMessage
	feed := &gtfs_realtime.FeedMessage{}
	if err := proto.Unmarshal(data, feed); err != nil {
		return nil, fmt.Errorf("failed to unmarshal Protobuf: %v", err)
	}

	// Convert FeedMessage to JSON using protojson
	jsonData, err := protojson.Marshal(feed)
	if err != nil {
		return nil, fmt.Errorf("failed to marshal to JSON: %v", err)
	}

	// Parse the JSON into Go maps for easier processing
	var result map[string]interface{}
	if err := json.Unmarshal(jsonData, &result); err != nil {
		return nil, fmt.Errorf("failed to parse JSON: %v", err)
	}

	// Extract "entity" field, which contains the vehicle data
	entities, ok := result["entity"].([]interface{})
	if !ok {
		return nil, fmt.Errorf("unexpected data format: no 'entity' field found")
	}

	// Convert entities to a slice of maps
	var entityMaps []map[string]interface{}
	for _, entity := range entities {
		entityMap, ok := entity.(map[string]interface{})
		if ok {
			entityMaps = append(entityMaps, entityMap)
		}
	}

	return entityMaps, nil
}

func publishToKafka(entities []map[string]interface{}) error {
	// Configure Kafka producer
	producer, err := sarama.NewSyncProducer([]string{kafkaBroker}, nil)
	if err != nil {
		return fmt.Errorf("failed to create Kafka producer: %v", err)
	}
	defer producer.Close()

	// Publish each entity to Kafka
	for _, entity := range entities {
		jsonValue, err := json.Marshal(entity)
		if err != nil {
			log.Printf("Failed to marshal entity to JSON: %v", err)
			continue
		}

		msg := &sarama.ProducerMessage{
			Topic: kafkaTopic,
			Value: sarama.StringEncoder(jsonValue),
		}

		_, _, err = producer.SendMessage(msg)
		if err != nil {
			log.Printf("Failed to send message to Kafka: %v", err)
			continue
		}

		log.Printf("Published entity to Kafka: %s", string(jsonValue))
	}

	return nil
}