package com.dachshaus.common.kafka

import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Publishes failed messages to the Dead Letter Queue (DLQ) with diagnostic headers.
 *
 * The DLQ allows for offline analysis of failed messages, helping debug issues
 * in message processing without blocking the main processing pipeline.
 *
 * Diagnostic headers include:
 * - dlq.source.topic: The original topic where the message came from
 * - dlq.source.partition: The original partition number
 * - dlq.source.offset: The original offset within the partition
 * - dlq.source.service: The service that encountered the error
 * - dlq.error.class: The exception class name
 * - dlq.error.message: The exception message
 * - dlq.timestamp: When the error occurred (ISO-8601)
 */
@Component
class DeadLetterPublisher(
    private val kafkaTemplate: KafkaTemplate<String, ByteArray>
) {

    companion object {
        private const val HEADER_SOURCE_TOPIC = "dlq.source.topic"
        private const val HEADER_SOURCE_PARTITION = "dlq.source.partition"
        private const val HEADER_SOURCE_OFFSET = "dlq.source.offset"
        private const val HEADER_SOURCE_SERVICE = "dlq.source.service"
        private const val HEADER_ERROR_CLASS = "dlq.error.class"
        private const val HEADER_ERROR_MESSAGE = "dlq.error.message"
        private const val HEADER_TIMESTAMP = "dlq.timestamp"
    }

    /**
     * Publishes a failed message to the DLQ.
     *
     * @param key The message key (may be null)
     * @param value The message payload
     * @param sourceTopic The topic where the original message came from
     * @param sourcePartition The partition where the original message came from
     * @param sourceOffset The offset of the original message
     * @param serviceName The name of the service that encountered the error
     * @param error The exception that caused the failure
     */
    fun publish(
        key: String?,
        value: ByteArray,
        sourceTopic: String,
        sourcePartition: Int,
        sourceOffset: Long,
        serviceName: String,
        error: Throwable
    ) {
        val record = ProducerRecord<String, ByteArray>(
            TopicNames.DLQ,
            null, // partition - let Kafka decide
            key,
            value
        )

        // Add all 7 diagnostic headers
        record.headers().add(
            RecordHeader(HEADER_SOURCE_TOPIC, sourceTopic.toByteArray())
        )
        record.headers().add(
            RecordHeader(HEADER_SOURCE_PARTITION, sourcePartition.toString().toByteArray())
        )
        record.headers().add(
            RecordHeader(HEADER_SOURCE_OFFSET, sourceOffset.toString().toByteArray())
        )
        record.headers().add(
            RecordHeader(HEADER_SOURCE_SERVICE, serviceName.toByteArray())
        )
        record.headers().add(
            RecordHeader(HEADER_ERROR_CLASS, error.javaClass.name.toByteArray())
        )
        record.headers().add(
            RecordHeader(HEADER_ERROR_MESSAGE, (error.message ?: "No message").toByteArray())
        )
        record.headers().add(
            RecordHeader(HEADER_TIMESTAMP, Instant.now().toString().toByteArray())
        )

        // Send to DLQ (fire and forget for simplicity, as DLQ failures are not typically retried)
        kafkaTemplate.send(record)
    }

    /**
     * Publishes a failed message to the DLQ with minimal information.
     *
     * This is a convenience method for cases where partition/offset information
     * is not available.
     *
     * @param key The message key (may be null)
     * @param value The message payload
     * @param sourceTopic The topic where the original message came from
     * @param serviceName The name of the service that encountered the error
     * @param error The exception that caused the failure
     */
    fun publish(
        key: String?,
        value: ByteArray,
        sourceTopic: String,
        serviceName: String,
        error: Throwable
    ) {
        publish(key, value, sourceTopic, -1, -1L, serviceName, error)
    }
}
