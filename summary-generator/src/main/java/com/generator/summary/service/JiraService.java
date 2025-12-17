package com.generator.summary.service;

import com.generator.summary.config.JiraConfig;
import com.generator.summary.constants.JiraKeys;
import com.generator.summary.constants.JiraMapping;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.HashSet;

@Service
@Slf4j
public class JiraService {

    private final JiraConfig jiraConfig;

    private final RestTemplate restTemplate;

    // Simple in-memory cache for resolved displayName -> accountId
    private final Map<String, String> assigneeCache = new ConcurrentHashMap<>();
    // Cache of allowed issue type names for the configured project
    private final Set<String> allowedIssueTypes = ConcurrentHashMap.newKeySet();

    public static String DEFAULT_PROJECT_ID = "10000";
    private static String DEFAULT_REPORTER_ID = "712020:5ff16db6-863b-4672-a6bb-635a5a4bb490";

    public JiraService(JiraConfig jiraConfig, RestTemplate restTemplate) {
        this.jiraConfig = jiraConfig;
        this.restTemplate = restTemplate;
    }

    public String createJiraTicket(String summary, String description, String issueId, String assigneeId, String assigneeName, String issuePriority) {
        String url = jiraConfig.getUrl() + "/rest/api/3/issue";

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(jiraConfig.getUsername(), jiraConfig.getApiToken());
        headers.set("Content-Type", "application/json");

        // Ensure we have allowed issue types for the project and pick a safe fallback if needed
        ensureAllowedIssueTypesLoaded();

        // If caller passed an issue type name that is not allowed for the project, pick a safe fallback
        if (issueId == null || issueId.isEmpty() || !allowedIssueTypes.contains(issueId)) {
            String fallback = null;
            if (allowedIssueTypes.contains("Task")) {
                fallback = "Task";
            } else if (!allowedIssueTypes.isEmpty()) {
                fallback = allowedIssueTypes.iterator().next();
            }
            log.info("Requested issue type '{}' is not allowed for project {}, using fallback '{}'", issueId, jiraConfig.getProjectKey(), fallback);
            issueId = fallback;
        }

        // If assigneeId is not provided or is the default fallback, try resolving by display name at runtime
        if ((assigneeId == null || assigneeId.isEmpty() || JiraMapping.DEFAULT_ASSIGNEE_USER_ID.equals(assigneeId)) && assigneeName != null && !assigneeName.isEmpty()) {
            try {
                String resolved = resolveAssigneeAccountId(assigneeName);
                if (resolved != null && !resolved.isEmpty()) {
                    assigneeId = resolved;
                } else {
                    // If we couldn't resolve a valid accountId, omit assignee to avoid sending an invalid value
                    log.info("Could not resolve assignee '{}', will create issue without assignee.", assigneeName);
                    assigneeId = null;
                }
            } catch (Exception e) {
                log.warn("Failed to resolve assignee '{}' via Jira API: {}", assigneeName, e.getMessage());
                // On error, omit assignee rather than sending potentially invalid default
                assigneeId = null;
            }
        }

        Map<String, Object> requestBody = getRequestBody(summary, description, issueId, assigneeId, issuePriority);

        log.info("Jira request body: {}", requestBody);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        log.info("Jira request entity: {}", requestEntity);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            log.info("Jira create response status: {}", response.getStatusCodeValue());
            return response.getBody();
        } catch (Exception ex) {
            log.error("Error creating Jira ticket", ex);
            // Propagate the exception so caller can capture and surface the error per-action-item
            throw new RuntimeException("Error creating Jira ticket: " + ex.getMessage(), ex);
        }
    }

    private Map<String, Object> getRequestBody(String summary, String desc, String issueId, String assigneeId, String issuePriority) {
        log.info("summary: {}, desc {}, issueId {}, assigneeId {}, issuePriority{}", summary, desc, issueId, assigneeId, issuePriority);
        // Define the request body
        Map<String, Object> requestBody = new HashMap<>();

        Map<String, Object> fields = new HashMap<>();
        fields.put(JiraKeys.FIELD_SUMMARY, summary);

        Map<String, String> project = new HashMap<>();
        // Use project key from config (cloud Jira expects project key)
        project.put("key", jiraConfig.getProjectKey() != null ? jiraConfig.getProjectKey() : DEFAULT_PROJECT_ID);
        fields.put(JiraKeys.FIELD_PROJECT, project);

    Map<String, String> issueType = new HashMap<>();
    // Use issue type name (e.g., "Task", "Bug") for Jira Cloud
    issueType.put("name", issueId);
    fields.put(JiraKeys.FIELD_ISSUE_TYPE, issueType);

        // Description field as a complex structure
        Map<String, Object> description = new HashMap<>();
        description.put(JiraKeys.ISSUE_VERSION, 1);
        description.put("type", "doc");

        Map<String, Object> paragraph = new HashMap<>();
        paragraph.put("type", "paragraph");

        Map<String, String> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", desc);

        paragraph.put("content", new Object[]{textContent});
        description.put("content", new Object[]{paragraph});
        fields.put(JiraKeys.FIELD_DESCRIPTION, description);

        // Assignee
        // Assignee should be specified by accountId for Jira Cloud
        if (assigneeId != null && !assigneeId.isEmpty()) {
            Map<String, String> assignee = new HashMap<>();
            assignee.put("accountId", assigneeId);
            fields.put("assignee", assignee);
        }

        // Priority
        Map<String, String> priority = new HashMap<>();
        priority.put(JiraKeys.FIELD_ID, JiraMapping.getPriorityValue(issuePriority));
        priority.put("name", issuePriority);
        priority.put("iconUrl", JiraMapping.getPriorityIconUrl(issuePriority));
        fields.put("priority", priority);

        // Reporter
    // Do not force reporter; Jira will use the authenticated user if reporter is omitted

    fields.put("labels", new String[]{});
    // Omit customfield_10021 by default - sending unknown custom fields may cause Jira to reject the request

        requestBody.put("fields", fields);
        requestBody.put("update", new HashMap<>());
        requestBody.put("watchers", new String[]{});
        //requestBody.put("externalToken", "0.5996048452798277");

        return requestBody;
    }

    /**
     * Resolve a display name to an assignable accountId using Jira API and cache the result.
     */
    private String resolveAssigneeAccountId(String displayName) throws Exception {
        if (displayName == null || displayName.isEmpty()) return null;

        // Check cache first
        if (assigneeCache.containsKey(displayName)) {
            return assigneeCache.get(displayName);
        }

        String projectKey = jiraConfig.getProjectKey();
        String query = URLEncoder.encode(displayName, StandardCharsets.UTF_8.toString());
        String url = String.format("%s/rest/api/3/user/assignable/search?project=%s&query=%s", jiraConfig.getUrl(), projectKey, query);

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(jiraConfig.getUsername(), jiraConfig.getApiToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            String body = response.getBody();
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> users = mapper.readValue(body, new TypeReference<List<Map<String, Object>>>(){});
            if (users != null && !users.isEmpty()) {
                Map<String, Object> first = users.get(0);
                Object accountIdObj = first.get("accountId");
                if (accountIdObj != null) {
                    String accountId = accountIdObj.toString();
                    assigneeCache.put(displayName, accountId);
                    log.info("Resolved assignee '{}' -> {}", displayName, accountId);
                    return accountId;
                }
            }
        }

        return null;
    }

    private synchronized void ensureAllowedIssueTypesLoaded() {
        if (!allowedIssueTypes.isEmpty()) return;

        String projectKey = jiraConfig.getProjectKey();
        if (projectKey == null || projectKey.isEmpty()) return;

        String url = String.format("%s/rest/api/3/project/%s", jiraConfig.getUrl(), projectKey);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(jiraConfig.getUsername(), jiraConfig.getApiToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                String body = response.getBody();
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> project = mapper.readValue(body, new TypeReference<Map<String, Object>>(){});
                Object issueTypesObj = project.get("issueTypes");
                if (issueTypesObj instanceof List) {
                    List<Map<String, Object>> types = (List<Map<String, Object>>) issueTypesObj;
                    for (Map<String, Object> t : types) {
                        Object nameObj = t.get("name");
                        if (nameObj != null) allowedIssueTypes.add(nameObj.toString());
                    }
                    log.info("Loaded allowed issue types for project {}: {}", projectKey, allowedIssueTypes);
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to load allowed issue types for project {}: {}", projectKey, ex.getMessage());
        }
    }

    public String getJiraBaseUrl() {
        return jiraConfig.getUrl();
    }
}

