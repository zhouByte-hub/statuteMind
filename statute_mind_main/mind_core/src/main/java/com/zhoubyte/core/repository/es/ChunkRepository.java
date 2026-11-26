package com.zhoubyte.core.repository.es;

import com.zhoubyte.core.model.ChunkEntity;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ChunkRepository extends ElasticsearchRepository<ChunkEntity, String> {
}
