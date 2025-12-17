package com.generator.summary.repository;

import com.generator.summary.model.TranscriptSummary;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TranscriptSummaryRepository extends MongoRepository<TranscriptSummary, String> {
    Optional<TranscriptSummary> findById(String fileId);
}
