package com.generator.summary.event.producer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActionItemEvent {

    @NonNull
    private String fileId;

    @NonNull
    private String chunkId;

    @NonNull
    private String chunk;

    private String prevId;

}
