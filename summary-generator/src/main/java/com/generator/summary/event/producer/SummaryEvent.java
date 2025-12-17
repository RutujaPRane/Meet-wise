package com.generator.summary.event.producer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SummaryEvent {

    @NonNull
    private String fileId;
}
