package com.zhoubyte.core.service.common;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.CountResponse;
import com.zhoubyte.common.SparseEncoderFunction;
import com.zhoubyte.core.common.EsIndexConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
public class SparseEncoderFunctionImpl implements SparseEncoderFunction {

    private final ElasticsearchClient elasticsearchClient;

    public SparseEncoderFunctionImpl(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    @Override
    public Long chunkTotal() {
        try {
            CountResponse countResponse = elasticsearchClient.count(count -> count.index(EsIndexConstant.CHUNK_INDEX));
            return countResponse.count();
        } catch (IOException e) {
            log.error("SparseEncoderFunctionImpl.chunkTotal出现错误", e);
            return 0L;
        }
    }

    @Override
    public Long includeChunkTotal(String chunk) {
        try {
            CountRequest countRequest = CountRequest.of(count -> count
                    .index(EsIndexConstant.CHUNK_INDEX)
                    .query(query -> query.matchPhrase(match -> match.field("contentTok").query(chunk))));
            return elasticsearchClient.count(countRequest).count();
        } catch (IOException e) {
            log.error("SparseEncoderFunctionImpl.includeChunkTotal出现错误, chunk: {}", chunk, e);
            return 0L;
        }
    }


}
