package com.generator.summary.constants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class JiraMapping {

    // Static map to store issue type to ID mapping
    private static final Map<String, String> ISSUE_TYPE_ID_MAP;

    // Static map to store user details: name -> (email, accountId)
    private static final Map<String, Map<String, String>> USER_DETAILS_MAP;

    // Default issue type
    public static final String DEFAULT_ISSUE_TYPE = "10001";
    static {
        Map<String, String> map = new HashMap<>();
        map.put("Task", "10001");
        map.put("Epic", "10002");
        map.put("Subtask", "10003");
        map.put("Story", "10012");
        map.put("Bug", "10011");

        ISSUE_TYPE_ID_MAP = Collections.unmodifiableMap(map);
    }

    // Public method to access the map
    public static String getIssueTypeId(String issueType) {
        return ISSUE_TYPE_ID_MAP.getOrDefault(issueType, DEFAULT_ISSUE_TYPE);
    }

    public static final String DEFAULT_ASSIGNEE_USER_ID = "712020:ccda6e83-08c8-42c4-8f10-0957aac0bef2";
    static {
        Map<String, Map<String, String>> map = new HashMap<>();

        // Example entries
        map.put("Default", Map.of("email", "prki8112@colorado.edu", "accountId", "712020:ccda6e83-08c8-42c4-8f10-0957aac0bef2"));
        

        USER_DETAILS_MAP = Collections.unmodifiableMap(map);
    }

    // Public method to get user details by name
    public static Map<String, String> getUserDetails(String userName) {
        return USER_DETAILS_MAP.getOrDefault(userName, Map.of("email", "prki8112@colorado.edu", "accountId", "712020:ccda6e83-08c8-42c4-8f10-0957aac0bef2"));
    }



    // Default priority value
    public static final String DEFAULT_PRIORITY = "3";

    private static final Map<String, String> PRIORITY_MAP = Map.of(
            "Highest", "1",
            "High", "2",
            "Medium", "3",
            "Low", "4",
            "Lowest", "5"
    );

    public static String getPriorityValue(String priorityName) {
        return PRIORITY_MAP.getOrDefault(priorityName, DEFAULT_PRIORITY); // Default to Medium
    }

    public static String getPriorityIconUrl(String priorityName) {
        return PRIORITY_ICON_URLS.getOrDefault(priorityName,
                "https://cubouldergrad.atlassian.net/images/icons/priorities/medium.svg"); // Default to Medium icon
    }

    private static final Map<String, String> PRIORITY_ICON_URLS = Map.of(
            "Highest", "https://cubouldergrad.atlassian.net/images/icons/priorities/highest.svg",
            "High", "https://cubouldergrad.atlassian.net/images/icons/priorities/high.svg",
            "Medium", "https://cubouldergrad.atlassian.net/images/icons/priorities/medium.svg",
            "Low", "https://cubouldergrad.atlassian.net/images/icons/priorities/low.svg",
            "Lowest", "https://cubouldergrad.atlassian.net/images/icons/priorities/lowest.svg"
    );


}
