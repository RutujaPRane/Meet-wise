package com.generator.summary.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitResponse {

    @NonNull
    private String fileId;

    @NonNull
    private String summary;

    @NonNull
    private List<ChunkTO> actionItems;

}
