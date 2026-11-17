package com.renauldiedev.event.infrastructure.elasticsearch;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.renauldiedev.event.infrastructure.elasticsearch.repository")
public class ElasticsearchConfig {
}
