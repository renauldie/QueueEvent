package com.renauldiedev.event.infrastructure.elasticsearch.repository;

import com.renauldiedev.event.infrastructure.elasticsearch.document.EventDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventSearchRepository extends ElasticsearchRepository<EventDocument, String> {

    Page<EventDocument> findByCategory(String category, Pageable pageable);

    Page<EventDocument> findByStatus(String status, Pageable pageable);

    Page<EventDocument> findByCategoryAndStatus(String category, String status, Pageable pageable);
}
