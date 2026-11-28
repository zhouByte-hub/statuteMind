package com.zhoubyte.core.repository.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.zhoubyte.core.common.EsIndexConstant;
import com.zhoubyte.core.model.ChunkEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * 分片ES存取，替代原 spring-data 的 ChunkRepository
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ChunkEsDao {

    private final ElasticsearchClient elasticsearchClient;

    public void saveAll(List<ChunkEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        try {
            BulkResponse response = elasticsearchClient.bulk(BulkRequest.of(b -> b
                    .index(EsIndexConstant.CHUNK_INDEX)
                    .operations(entities.stream()
                            .map(e -> BulkOperation.of(op -> op.index(i -> i.id(e.getChunkId()).document(e))))
                            .toList())));
            if (response.errors()) {
                response.items().stream()
                        .filter(item -> item.error() != null)
                        .forEach(item -> log.error("分片写入ES失败 id={} 原因={}", item.id(), item.error().reason()));
                throw new IllegalStateException("存在分片写入ES失败，详见日志");
            }
        } catch (IOException e) {
            throw new RuntimeException("分片批量写入ES失败", e);
        }
    }
}
