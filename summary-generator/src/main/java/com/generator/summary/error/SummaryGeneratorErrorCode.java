package com.generator.summary.error;

public enum SummaryGeneratorErrorCode {

    INPUT_INVALID_ERROR("Provided input is invalid"),

    FILE_PROCESSING_ERROR("File processing failed");

    private final String message;

    SummaryGeneratorErrorCode(final String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
