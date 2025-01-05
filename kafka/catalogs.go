package main

import (
	"encoding/csv"
	"encoding/json"
	"flag"
	"log"
	"os"

	"github.com/IBM/sarama"
)

func main() {
	// Define command-line arguments
	fileName := flag.String("file", "", "Path to the CSV file")
	topicName := flag.String("topic", "", "Kafka topic to publish messages")
	brokerAddress := flag.String("broker", "localhost:9092", "Kafka broker address")
	flag.Parse()

	// Validate arguments
	if *fileName == "" || *topicName == "" {
		log.Fatalf("Usage: go run main.go --file <file_path> --topic <topic_name> [--broker <broker_address>]")
	}

	// Open the CSV file
	file, err := os.Open(*fileName)
	if err != nil {
		log.Fatalf("Failed to open CSV file: %v", err)
	}
	defer file.Close()

	// Read CSV data
	reader := csv.NewReader(file)
	records, err := reader.ReadAll()
	if err != nil {
		log.Fatalf("Failed to read CSV file: %v", err)
	}

	// Extract header
	if len(records) < 2 {
		log.Fatal("CSV file must have a header and at least one row")
	}
	header := records[0]
	dataRows := records[1:]

	// Configure Kafka producer
	producer, err := sarama.NewSyncProducer([]string{*brokerAddress}, nil)
	if err != nil {
		log.Fatalf("Failed to create Kafka producer: %v", err)
	}
	defer producer.Close()

	// Convert each record to JSON and send to Kafka
	for _, row := range dataRows {
		if len(row) != len(header) {
			log.Printf("Skipping invalid row: %v", row)
			continue
		}

		// Convert CSV row to a map
		recordMap := make(map[string]string)
		for i, field := range header {
			recordMap[field] = row[i]
		}

		// Convert map to JSON
		jsonData, err := json.Marshal(recordMap)
		if err != nil {
			log.Printf("Failed to convert row to JSON: %v", err)
			continue
		}

		// Publish JSON to Kafka
		msg := &sarama.ProducerMessage{
			Topic: *topicName,
			Value: sarama.StringEncoder(jsonData),
		}
		_, _, err = producer.SendMessage(msg)
		if err != nil {
			log.Printf("Failed to publish message to Kafka: %v", err)
		} else {
			//log.Printf("Published message: %s", jsonData)
		}
	}

	log.Println("CSV export to Kafka completed.")
}