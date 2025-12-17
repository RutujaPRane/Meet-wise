package com.generator.summary.model;

import java.util.List;
import java.util.Map;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "action-items")
public class ActionItem {

    @Id
    private String id;

    @Indexed
    private String fileId;  // Indexing the fileId field

    private String chunkText;
    private List<Map<String, String>> actionItems;  // Assuming action items are stored as a list of key-value pairs
    private long timeCreated;
    private long timeModified;
}


