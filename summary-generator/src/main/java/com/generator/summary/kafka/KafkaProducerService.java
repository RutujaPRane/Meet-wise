package com.generator.summary.kafka;

import com.generator.summary.event.producer.ActionItemEvent;
import com.generator.summary.event.producer.SummaryEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KafkaProducerService {

    private static final String TOPIC_PREFIX = "llm_service.events.";

    private static final String ACTION_ITEMS_TOPIC = "generate.action.items";

    private static final String SUMMARY_TOPIC = "generate.summary";

    private KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaProducerService(final KafkaTemplate kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendActionItemsMessage(ActionItemEvent event) {
        log.info("Sending action items kafka event {} on topic {}", event, getActionItemsTopic());
        this.kafkaTemplate.send(getActionItemsTopic(), event);
        log.info("Successfully sent action items event");
    }
    
    public void sendSummaryMessage(SummaryEvent event) {
        log.info("Sending summary kafka event {} on topic {}", event, getSummaryTopic());
        this.kafkaTemplate.send(getSummaryTopic(), event);
        log.info("Successfully sent summary event");
    }

    private String getActionItemsTopic() {
        return TOPIC_PREFIX +  ACTION_ITEMS_TOPIC;
    }

    private String getSummaryTopic() {
        return TOPIC_PREFIX + SUMMARY_TOPIC;
    }
}
