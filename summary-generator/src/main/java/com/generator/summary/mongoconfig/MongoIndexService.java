package com.generator.summary.mongoconfig;

import com.generator.summary.model.ActionItem;
import com.generator.summary.model.TranscriptSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class MongoIndexService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @PostConstruct
    public void createIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps(ActionItem.class);
        IndexOperations indexOps2 = mongoTemplate.indexOps(TranscriptSummary.class);

        // Create an index on 'fileId'
        indexOps.ensureIndex(new Index().on("fileId", Sort.Order.asc("fileId").getDirection()));

        // Create an index on 'fileId'
        indexOps2.ensureIndex(new Index().on("fileId", Sort.Order.asc("fileId").getDirection()));

    }
}
