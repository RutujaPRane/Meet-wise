package com.generator.summary.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "transcript-summaries")
public class TranscriptSummary {

    @Id
    private String id;

    @Indexed
    private String fileId;  // Indexing the fileId field

    private String transcript;
    private String summary;
    private long timeCreated;
    private long timeModified;
}
