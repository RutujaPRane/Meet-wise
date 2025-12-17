package com.generator.summary.error;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SummaryGeneratorException extends RuntimeException {

    private HttpStatus httpStatus;

    private final List<ErrorDetail> errors = new ArrayList<>();

    public SummaryGeneratorException(final ErrorDetail errorDetail) {
        super(errorDetail.getMessage());
        this.errors.add(errorDetail);
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public SummaryGeneratorException(final ErrorDetail errorDetail, final Throwable cause) {
        super(errorDetail.getMessage(), cause);
        this.errors.add(errorDetail);
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public SummaryGeneratorException(final ErrorDetail errorDetail, final Throwable cause, final HttpStatus httpStatus) {
        super(errorDetail.getMessage(), cause);
        this.errors.add(errorDetail);
        this.httpStatus = httpStatus;
    }

    public SummaryGeneratorException withHttpStatus(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
        return this;
    }

    public SummaryGeneratorException logError(@Nullable Logger logger) {
        if (logger == null) {
            logger = log;
        }
        logger.info("ERROR: {}", getLogString());
        return this;
    }

    String getLogString() {
        StringBuilder logString = new StringBuilder(getMessage());

        // then add each error detail
        // then add each error detail
        for (ErrorDetail errorDetail : errors) {
            logString.append("; ").append(errorDetail.toExpandedString());
        }
        return logString.toString();
    }




}
