package com.generator.summary.repository;

import com.generator.summary.model.ActionItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActionItemRepository extends MongoRepository<ActionItem, String> {
    List<ActionItem> findByFileId(String fileId);
}
