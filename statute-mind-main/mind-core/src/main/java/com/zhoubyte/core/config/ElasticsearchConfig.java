package com.zhoubyte.core.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.DenseVectorSimilarity;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.zhoubyte.core.common.EsIndexConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@Slf4j
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private List<String> uris;

    @Bean(destroyMethod = "close")
    public RestClient elasticsearchRestClient() {
        HttpHost[] hosts = uris.stream()
                .map(String::trim)
                .map(HttpHost::create)
                .toArray(HttpHost[]::new);
        return RestClient.builder(hosts).build();
    }

    @Bean
    public ElasticsearchTransport elasticsearchTransport(RestClient elasticsearchRestClient) {
        return new RestClientTransport(elasticsearchRestClient, new JacksonJsonpMapper());
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport elasticsearchTransport) {
        return new ElasticsearchClient(elasticsearchTransport);
    }

    /**
     * 启动时确保分片索引存在（原先由 @Document(createIndex = true) 触发）
     */
    @Bean
    public ApplicationRunner chunkIndexInitializer(ElasticsearchClient elasticsearchClient) {
        return args -> {
            boolean exists = elasticsearchClient.indices()
                    .exists(e -> e.index(EsIndexConstant.CHUNK_INDEX))
                    .value();
            if (exists) {
                return;
            }
            elasticsearchClient.indices().create(c -> c
                    .index(EsIndexConstant.CHUNK_INDEX)
                    .mappings(m -> m
                            .properties("chunkId", p -> p.keyword(k -> k))
                            .properties("title", p -> p.keyword(k -> k))
                            .properties("sourceFile", p -> p.keyword(k -> k))
                            .properties("targetFile", p -> p.keyword(k -> k))
                            .properties("content", Property.of(p -> p.matchOnlyText(t -> t)))
                            // 当前 ES 服务端未安装 IK 插件，暂用内置 standard 分词器；
                            // 安装 IK 后改回 t.analyzer("ik_max_word").searchAnalyzer("ik_smart") 并删除重建索引
                            .properties("contentTok", p -> p.text(t -> t.analyzer("standard").searchAnalyzer("standard")))
                            .properties("sparse", Property.of(p -> p.rankFeatures(r -> r)))
                            .properties("embedding", p -> p.denseVector(d -> d.dims(768).index(true).similarity(DenseVectorSimilarity.Cosine)))
                            .properties("blockType", p -> p.keyword(k -> k))
                            .properties("sectionPath", p -> p.keyword(k -> k))
                            .properties("page", p -> p.integer(i -> i))));
            log.info("已创建ES索引 {}", EsIndexConstant.CHUNK_INDEX);
        };
    }
}
