package com.generator.summary.service;

import com.generator.summary.event.consumer.ActionItemProcessedEvent;
import com.generator.summary.handlers.SummaryGenerationController;
import com.generator.summary.model.ActionItem;
import com.generator.summary.repository.ActionItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;



import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class ActionItemsEventHandler {

    @Autowired
    private SummaryGenerationController summaryGenerationController;

    @Autowired
    private ActionItemRepository actionItemRepository;

    public void handleActionItemsEvent(ActionItemProcessedEvent event) {
        log.info("Handling action items processed event {} for file {} and chunk {}",
                 event, event.getFileId(), event.getChunkId());

        ActionItem actionItem = actionItemRepository.findById(event.getChunkId()).orElse(null);

        if (actionItem == null) {
            log.info("No action item found for chunk {}", event.getChunkId());
            return;
        }

        log.info("Found action item {} for chunk {}", actionItem.getActionItems(), event.getChunkId());

        Map<String, Object> sseResponse = new HashMap<>();
        sseResponse.put("id", actionItem.getId());
        sseResponse.put("fileId", actionItem.getFileId());
        sseResponse.put("actionItems", actionItem.getActionItems()); // this is already a list

        // ❌ no ObjectMapper here
        // ✅ let Spring convert the Map to JSON once
        summaryGenerationController.broadcast(sseResponse);
    }
}
