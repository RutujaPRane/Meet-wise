package com.generator.summary.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import java.util.List;

@Data
@AllArgsConstructor
public class SubmitRequest {

    @NonNull
    private String fileId;

    @NonNull
    private String summary;

    @NonNull
    private List<ChunkTO> chunkTOs;

}




