package com.generator.summary.service;

import com.generator.summary.handlers.SummaryGenerationController;
import com.generator.summary.event.consumer.SummaryProcessedEvent;
import com.generator.summary.model.TranscriptSummary;
import com.generator.summary.repository.TranscriptSummaryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class SummaryEventHandler {

    @Autowired
    private SummaryGenerationController summaryGenerationController;

    @Autowired
    private TranscriptSummaryRepository transcriptSummaryRepository;

    public void handleSummaryEvent(SummaryProcessedEvent event) {
        log.info("Handling summary processed event {} for file {}", event, event.getFileId());
        Optional<TranscriptSummary> optionalTranscriptSummary = transcriptSummaryRepository.findById((event.getFileId()));

        if (optionalTranscriptSummary.isPresent()) {
            log.info("Found summary file {}", event.getFileId());

            summaryGenerationController.broadcastSummary(optionalTranscriptSummary.get().getSummary());
        } else {
            log.info("No summary file found for file {}", event.getFileId());
        }

    }



}
