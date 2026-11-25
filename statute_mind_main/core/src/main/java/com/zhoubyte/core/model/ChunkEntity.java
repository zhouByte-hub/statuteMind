package com.zhoubyte.core.model;

import com.zhoubyte.core.common.EmbeddingConstant;
import com.zhoubyte.core.common.EsIndexConstant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Map;

/**
 * 分片实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Builder
@Document(indexName = EsIndexConstant.CHUNK_INDEX, createIndex = true)
public class ChunkEntity {

    @Id
    private String chunkId;                     // 分片ID

    @Field(type = FieldType.Keyword)
    private String title;                       // 分段标题

    @Field(type = FieldType.Keyword)
    private String sourceFile;                  // 源文件名

    @Field(type = FieldType.Keyword)
    private String targetFile;                  // 上传后存储的文件名

    @Field(type = FieldType.Match_Only_Text)
    private String content;                     // 分片内容

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String contentTok;                  // 支持搜索的内容转换

    @Field(type = FieldType.Rank_Features)
    private Map<String, Double> sparse;         // 根据contentTok推算出的稀疏数组

    @Field(type = FieldType.Dense_Vector, dims = EmbeddingConstant.DIMENSION, similarity = "cosine")
    private float[] embedding;                  // 分片内容向量

    @Field(type = FieldType.Keyword)
    private String blockType;                   // 分片类型

    @Field(type = FieldType.Keyword)
    private String sectionPath;                 // 分片路径

    @Field(type = FieldType.Integer)
    private Integer page;                       // 分片所在页

}
