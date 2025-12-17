package com.generator.summary.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class JiraConfig {

    @Value("${jira.url}")
    private String url;

    @Value("${jira.username}")
    private String username;

    @Value("${jira.api-token}")
    private String apiToken;

    @Value("${jira.project-key}")
    private String projectKey;
}
