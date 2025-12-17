package com.generator.summary.event.consumer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActionItemProcessedEvent {
    @NonNull
    private String fileId;

    @NonNull
    private String chunkId;
}
