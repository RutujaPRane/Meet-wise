package com.generator.summary.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class ChunkTO {

    @NonNull
    private String chunkId;

    private List<Map<String, String>> chunkItems;
}
