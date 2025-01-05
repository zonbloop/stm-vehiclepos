package main

import (
	"log"
	"context"

	"github.com/IBM/sarama"
)

func main() {
	// Kafka configuration
	broker := "localhost:9092"
	sourceTopic := "gtfs_realtime"
	destinationTopic := "vehiclepos"
	groupID := "group-id"


	// Sarama configuration
	config := sarama.NewConfig()
	config.Consumer.Group.Rebalance.Strategy = sarama.BalanceStrategyRoundRobin
	config.Producer.Return.Successes = true
	config.Consumer.Offsets.Initial = sarama.OffsetOldest // Start reading from the earliest offset

	// Create a new consumer group
	consumerGroup, err := sarama.NewConsumerGroup([]string{broker}, groupID, config)
	if err != nil {
		log.Fatalf("Error creating consumer group: %v", err)
	}
	defer consumerGroup.Close()

	// Create a new producer
	producer, err := sarama.NewSyncProducer([]string{broker}, config)
	if err != nil {
		log.Fatalf("Error creating producer: %v", err)
	}
	defer producer.Close()

	log.Printf("Starting to copy messages from %s to %s\n", sourceTopic, destinationTopic)

	handler := &consumerGroupHandler{
		producer:         producer,
		destinationTopic: destinationTopic,
	}

	for {
		if err := consumerGroup.Consume(context.Background(), []string{sourceTopic}, handler); err != nil {
			log.Fatalf("Error during consumption: %v", err)
		}
	}
}

type consumerGroupHandler struct {
	producer         sarama.SyncProducer
	destinationTopic string
}

func (h *consumerGroupHandler) Setup(_ sarama.ConsumerGroupSession) error {
	return nil
}

func (h *consumerGroupHandler) Cleanup(_ sarama.ConsumerGroupSession) error {
	return nil
}

func (h *consumerGroupHandler) ConsumeClaim(session sarama.ConsumerGroupSession, claim sarama.ConsumerGroupClaim) error {
	for message := range claim.Messages() {
		log.Printf("Read message: %s\n", string(message.Value))

		// Produce the message to the destination topic
		msg := &sarama.ProducerMessage{
			Topic: h.destinationTopic,
			Key:   sarama.ByteEncoder(message.Key),
			Value: sarama.ByteEncoder(message.Value),
		}

		partition, offset, err := h.producer.SendMessage(msg)
		if err != nil {
			log.Printf("Failed to produce message: %v", err)
		} else {
			log.Printf("Message written to partition %d, offset %d\n", partition, offset)
		}

		// Mark the message as processed
		session.MarkMessage(message, "")
	}
	return nil
}

