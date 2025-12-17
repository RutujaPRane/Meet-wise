package com.generator.summary.handlers;

import com.generator.summary.dto.GenerateActionItemsResponse;
import com.generator.summary.dto.SubmitRequest;
import com.generator.summary.dto.SubmitResponse;
import com.generator.summary.error.ErrorDetail;
import com.generator.summary.error.ErrorDetailBuilder;
import com.generator.summary.error.SummaryGeneratorErrorCode;
import com.generator.summary.error.SummaryGeneratorException;
import com.generator.summary.model.ActionItem;
import com.generator.summary.service.SummaryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.bind.annotation.CrossOrigin;
import com.generator.summary.model.ActionItem;
import java.util.HashMap;
import java.util.Map;


import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
@RequestMapping("/smartmeet")
public class SummaryGenerationController {

    @Autowired
    private SummaryService summaryService;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final List<SseEmitter> summaryEmmitters = new CopyOnWriteArrayList<>();

    @PostMapping("/generate-summary")
    @CrossOrigin(origins = "http://localhost:8080")
    public ResponseEntity<Object> generateSummary(@RequestParam("file") MultipartFile file) {
        log.info("Received request to process summary and action items for the file {}", file.getOriginalFilename());
        try {
            if (file.isEmpty()) {
                throw new IOException("file is empty");
            }
            String fileID = summaryService.processFileForSummaryGeneration(file);

            // Return the fileId in the response body
            return ResponseEntity.ok().body(new GenerateActionItemsResponse(fileID));
        } catch (IOException | SummaryGeneratorException e) {

            ErrorDetail errorDetail = ErrorDetailBuilder.errorBuilder(SummaryGeneratorErrorCode.FILE_PROCESSING_ERROR).build();
            SummaryGeneratorException  ex = new SummaryGeneratorException(errorDetail)
                    .withHttpStatus(HttpStatus.BAD_REQUEST)
                    .logError(log);

            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/chunks/{fileId}")
    public ResponseEntity<List<ActionItem>> getActionItemsByFileId(@PathVariable String fileId) {
        log.info("Received request to find chunks for fileId {}", fileId);
        List<ActionItem> actionItems = summaryService.getActionItemsByFileId(fileId);

        if (actionItems.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(actionItems);
    }

    @GetMapping("/subscribe")
    @CrossOrigin(origins = "http://localhost:8080")
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(86400000L);
        
        log.info("got request to subscribe");

        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));

        return emitter;
    }

    public void broadcast(Object message) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(message);
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }

    @GetMapping("/subscribe-summary")
    @CrossOrigin(origins = "http://localhost:8080")
    public SseEmitter subscribeSummary() {
        SseEmitter emitter = new SseEmitter(86400000L);
        
        log.info("got request to subscribe for summary");

        summaryEmmitters.add(emitter);

        // emitter.onCompletion(() -> emitters.remove(emitter));
        // emitter.onTimeout(() -> emitters.remove(emitter));
        // emitter.onError((e) -> emitters.remove(emitter));

        return emitter;
    }

    public void broadcastSummary(Object message) {
        for (SseEmitter emitter : summaryEmmitters) {
            try {
                emitter.send(message);
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }

    private void broadcastUpdatedActionItems(String fileId) {
    log.info("Broadcasting updated action items for fileId {}", fileId);

    // Fetch all action items for this file
    var items = summaryService.getActionItemsByFileId(fileId);

    for (ActionItem item : items) {
        Map<String, Object> ssePayload = new HashMap<>();
        ssePayload.put("id", item.getId());
        ssePayload.put("fileId", item.getFileId());
        ssePayload.put("actionItems", item.getActionItems()); // includes jiraIssueUrl

        // Reuse existing SSE broadcast method
        broadcast(ssePayload);
    }
}

    @PostMapping("/submit/{fileId}")
    @CrossOrigin(origins = "http://localhost:8080")
    public ResponseEntity<Object> submit(@PathVariable String fileId, @RequestBody SubmitRequest request) {
        log.info("Received request to submit the summary for file {}", fileId);

        try {
            SubmitResponse submitResponse = summaryService.submitSummary(request);

            // ✅ After Jira tickets are created, broadcast updated action items
            broadcastUpdatedActionItems(fileId);

            // Return the fileId in the response body
            return ResponseEntity.ok().body(submitResponse);
        } catch (SummaryGeneratorException e) {

            ErrorDetail errorDetail = ErrorDetailBuilder.errorBuilder(SummaryGeneratorErrorCode.FILE_PROCESSING_ERROR).build();
            SummaryGeneratorException ex = new SummaryGeneratorException(errorDetail)
                    .withHttpStatus(HttpStatus.BAD_REQUEST)
                    .logError(log);

            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
