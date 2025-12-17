package com.generator.summary.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.generator.summary.event.consumer.ActionItemProcessedEvent;
import com.generator.summary.event.consumer.SummaryProcessedEvent;
import com.generator.summary.service.ActionItemsEventHandler;
import com.generator.summary.service.SummaryEventHandler;
import com.generator.summary.util.ObjectMapperWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KafkaConsumerService {

    private static final String TOPIC_PREFIX = "summary.generator.events";

    private static final String ACTION_ITEMS_EVENT = ".action.items";

    private static final String SUMMARY_EVENT = ".summary";

    @Autowired
    private SummaryEventHandler summaryEventHandler;

    @Autowired
    private ActionItemsEventHandler actionItemsEventHandler;

    @KafkaListener(topicPattern = TOPIC_PREFIX + ACTION_ITEMS_EVENT,
            containerFactory = "kafkaListenerContainerFactory", groupId = "my-consumer-group")
    public void consumeActionItemEvent(@Payload(required = false) String eventJson) {
        log.info("Consuming kafka action items event: {}", eventJson);
        ActionItemProcessedEvent event;
        try {
            ObjectMapper objectMapper = ObjectMapperWrapper.getObjectMapper();
            event = objectMapper.readValue(eventJson, ActionItemProcessedEvent.class);
            actionItemsEventHandler.handleActionItemsEvent(event);
        } catch (JsonProcessingException jpe) {
            log.error("Failed to parse event. Exception: ", jpe);

        } catch (Exception e) {
            log.error("Failed to process event due to exception:", e);
        }
    }

    @KafkaListener(topicPattern = TOPIC_PREFIX + SUMMARY_EVENT,
            containerFactory = "kafkaListenerContainerFactory", groupId = "my-consumer-group2")
    public void consumeSummaryEvent(@Payload(required = false) String eventJson) {
        log.info("Consuming kafka summary event: {}", eventJson);
        SummaryProcessedEvent event;
        try {
            ObjectMapper objectMapper = ObjectMapperWrapper.getObjectMapper();
            event = objectMapper.readValue(eventJson, SummaryProcessedEvent.class);
            summaryEventHandler.handleSummaryEvent(event);
        } catch (JsonProcessingException jpe) {
            log.error("Failed to parse event. Exception: ", jpe);

        } catch (Exception e) {
            log.error("Failed to process event due to exception:", e);
        }
    }
}