package com.generator.summary.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.generator.summary.constants.ActionItemsKeys;
import com.generator.summary.constants.JiraMapping;
import com.generator.summary.dto.ChunkTO;
import com.generator.summary.dto.SubmitRequest;
import com.generator.summary.dto.SubmitResponse;
import com.generator.summary.event.producer.ActionItemEvent;
import com.generator.summary.event.producer.SummaryEvent;
import com.generator.summary.kafka.KafkaProducerService;
import com.generator.summary.model.ActionItem;
import com.generator.summary.model.TranscriptSummary;
import com.generator.summary.repository.ActionItemRepository;
import com.generator.summary.repository.TranscriptSummaryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;



import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

import static com.generator.summary.service.JiraService.DEFAULT_PROJECT_ID;

@Service
@Slf4j
public class SummaryService {

    @Autowired
    private ActionItemRepository actionItemRepository;

    @Autowired
    private TranscriptSummaryRepository transcriptSummaryRepository;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private JiraService jiraService;

   


    /*
        Below function performs following operations
        1. It reads the file
        2. Creates chunk of file (maxSentences = 15), saves each of them to repository and sends them on Kafka topic for action items generation
        3. Saves the whole file to another repository and sends document reference in a kafka message for summary generation
   */
    public String processFileForSummaryGeneration(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        log.info("Processing file {}", fileName);

        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        String fileID = UUID.randomUUID().toString();

        processChunks(fileName, fileID, content);
        processFileForSummaryGeneration(fileID, content);

        log.info("File {} processed successfully", fileName);
        return fileID;
    }

    /*
        1. Update final summary in the summary collection against fileId
        2. Update action items in action items collection against chunkID
        3. For every action item pair, create Jira ticket
     */
    public SubmitResponse submitSummary(SubmitRequest request) {
        // 1. Update final summary
        updateFinalSummary(request.getFileId(), request.getSummary());

        // 2. Update final chunks and create jira ticket for each action item
        List<ChunkTO> chunks = updateFinalChunkItems(request.getChunkTOs());

        SubmitResponse submitResponse = new SubmitResponse();
        submitResponse.setFileId(request.getFileId());
        submitResponse.setSummary(request.getSummary());
        submitResponse.setActionItems(chunks);

        return submitResponse;

    }

    private void updateFinalSummary(String fileId, String summary) {
        Optional<TranscriptSummary> optionalTranscriptSummary = transcriptSummaryRepository.findById(fileId);

        if (optionalTranscriptSummary.isPresent()) {
            TranscriptSummary transcriptSummary = optionalTranscriptSummary.get();
            // TODO: Address NPE
            transcriptSummary.setSummary(summary);
            transcriptSummary.setTimeModified(Instant.now().getEpochSecond());
            transcriptSummaryRepository.save(transcriptSummary);

            log.info("Updated final summary for the fileId {}", fileId);
        } else {
            log.error("Didn't find summary for fileId {}", fileId);
        }


    }

    private List<ChunkTO> updateFinalChunkItems(List<ChunkTO> chunkTOs) {
        // Update every action item
        for (ChunkTO chunkTO : chunkTOs) {
            ActionItem actionItems = updateActionItems(chunkTO);
            List<Map<String, String>> newActionItems = new ArrayList<>();
            if (actionItems != null) {
                // We have updated action item, now proceed with Jira creation
                for (Map<String, String> kv : actionItems.getActionItems()) {
                    String jiraIssueUrl = createJira(kv);
                    kv.put(ActionItemsKeys.JIRA_ISSUE_URL, jiraIssueUrl);
                    newActionItems.add(kv);
                }
            }

            chunkTO.setChunkItems(newActionItems);
            updateActionItems(chunkTO);
        }
        return chunkTOs;
    }

    private String createJira(Map<String, String> map) {
        String summary = map.getOrDefault(ActionItemsKeys.SUMMARY, "TEST_TICKET");
        String desc = map.getOrDefault(ActionItemsKeys.DESCRIPTION, "");

        // Get assignee
        String user = map.get(ActionItemsKeys.ASSIGNEE);
        String assigneeId = JiraMapping.DEFAULT_ASSIGNEE_USER_ID;
        Map<String, String> userDetails = JiraMapping.getUserDetails(user);
        if (!userDetails.isEmpty()) {
            assigneeId = userDetails.getOrDefault("accountId", assigneeId);
        }
// Get issue type (we pass it directly to JiraService)
String issueType = map.get(ActionItemsKeys.ISSUE_TYPE);

// Get Priority
String priority = map.get(ActionItemsKeys.PRIORITY);

try {
    // Pass issueType as-is; JiraService will map/handle it as needed
    String response = jiraService.createJiraTicket(summary, desc, issueType, assigneeId, user, priority);
    return extractJiraTicketLink(response);
} catch (Exception ex) {
    log.error("Failed to create Jira for action item {}: {}", map, ex.getMessage());
    return "";
}

    }

    private String extractJiraTicketLink(String createIssueResponse)  {
        try {
            // Parse the response JSON
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> responseMap = objectMapper.readValue(createIssueResponse, Map.class);

            // Extract 'key' from the response
            String issueKey = (String) responseMap.get("key");

            // Construct the issue link (standard Jira browse URL)
            String jiraBaseUrl = jiraService.getJiraBaseUrl();
            String issueUrl = String.format("%s/browse/%s", jiraBaseUrl, issueKey);
            log.info("Constructed jira issue url {}", issueUrl);

            return issueUrl;
        } catch (JsonProcessingException ex) {
            log.error("Received exception while parsing jira create issue response ", ex);
            return "";
        }

    }

    private ActionItem updateActionItems(ChunkTO chunkTO) {
        String chunkId = chunkTO.getChunkId();
        Optional<ActionItem> optionalActionItem = actionItemRepository.findById(chunkId);

        if (optionalActionItem.isPresent()) {
            ActionItem actionItem = optionalActionItem.get();
            actionItem.setActionItems(chunkTO.getChunkItems());
            actionItem.setTimeModified(Instant.now().getEpochSecond());

            actionItemRepository.save(actionItem);
            log.info("Updated action item for the chunkId {}", chunkId);
            return actionItem;
        }

        return null;
    }

    private void processChunks(String fileName, String fileID, String content) {
        List<String> chunks = splitTextIntoChunks(fileName, content, 15);
        log.info("Generated {} chunks for the file {}", chunks.size(), fileID);
        String prevId = "";
        int index = 0;
        // For each chunk, create a record in MongoDB
        for (String chunk : chunks) {
            // Save entity to mongo
            ActionItem entity = actionItemRepository.save(getActionItem(fileID, chunk));
            log.info("Chunk {} saved successfully in mongo collection", index);

            // Process a kafka message
            if(prevId.equals("")){
                createAndSendActionItemsKafkaMessage(entity);
            } else{
                createAndSendActionItemsKafkaMessage(entity, prevId);
            }

            prevId = entity.getId();
            
            index++;
        }
    }

    private void processFileForSummaryGeneration(String fileID, String content) {
        TranscriptSummary summary = getTranscriptSummary(fileID, content);
        transcriptSummaryRepository.save(summary);
        log.info("Transcript file {} saved successfully in mongo collection", fileID);

        // Send kafka message
        createAndSendSummaryKafkaMessage(fileID);
    }

    private TranscriptSummary getTranscriptSummary(String fileID, String content) {
        TranscriptSummary summary = new TranscriptSummary();
        summary.setId(fileID);
        summary.setTranscript(content);
        summary.setTimeCreated(Instant.now().getEpochSecond());
        summary.setSummary("");
        return summary;
    }

    private ActionItem getActionItem(String fileId, String chunkText) {
        ActionItem actionItem = new ActionItem();
        actionItem.setFileId(fileId);
        actionItem.setChunkText(chunkText);
        actionItem.setTimeCreated(Instant.now().getEpochSecond());

        // actionItem.setActionItems(new ArrayList<>());

        // TODO : Only for testing, delete later
        List<Map<String, String>> actionItems = new ArrayList<>();
        // Map<String, String> kv = new HashMap<>();
        // kv.put(ActionItemsKeys.SUMMARY, "TEST_SUMMARY");
        // kv.put(ActionItemsKeys.DESCRIPTION, "TEST_DESCRIPTION");
        // kv.put(ActionItemsKeys.ISSUE_TYPE, "Bug");
        // kv.put(ActionItemsKeys.PRIORITY, "Highest");
        // kv.put(ActionItemsKeys.ASSIGNEE, "Saksham");
        // actionItems.add(kv);

        actionItem.setActionItems(actionItems);
        return actionItem;
    }

       

    private void createAndSendActionItemsKafkaMessage(ActionItem actionItem) {
        ActionItemEvent event = new ActionItemEvent();
        event.setFileId(actionItem.getFileId());
        event.setChunkId(actionItem.getId());
        event.setChunk(actionItem.getChunkText());

        kafkaProducerService.sendActionItemsMessage(event);
    }

    private void createAndSendActionItemsKafkaMessage(ActionItem actionItem, String prevId) {
        ActionItemEvent event = new ActionItemEvent();
        event.setFileId(actionItem.getFileId());
        event.setChunkId(actionItem.getId());
        event.setChunk(actionItem.getChunkText());
        event.setPrevId(prevId);

        kafkaProducerService.sendActionItemsMessage(event);
    }

    private void createAndSendSummaryKafkaMessage(String fileID) {
        SummaryEvent event = new SummaryEvent(fileID);
        kafkaProducerService.sendSummaryMessage(event);
    }

    // Splits the text into multiple chunks
    private List<String> splitTextIntoChunks(String fileName, String text, int minSentences) {
        log.info("Generating multiple chunks of file {}", fileName);
        String[] sentences = text.split("\\.\\s*");
        List<String> chunks = new ArrayList<>();

        StringBuilder chunk = new StringBuilder();
        int sentenceCount = 0;

        for (String sentence : sentences) {
            chunk.append(sentence).append(" ");
            sentenceCount++;

            if (sentenceCount >= minSentences) {
                chunks.add(chunk.toString().trim());
                chunk.setLength(0); // Reset for next chunk
                sentenceCount = 0;
            }
        }

        // Add the last chunk if any remaining sentences
        if (chunk.length() > 0) {
            chunks.add(chunk.toString().trim());
        }
        
        log.info("Number of chunks {}", chunks.size());

        return chunks;
    }

    // Method to get action items by fileId
    public List<ActionItem> getActionItemsByFileId(String fileId) {
        return actionItemRepository.findByFileId(fileId);
    }


}
