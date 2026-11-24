package com.zhoubyte.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * 分片实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class ChunkEntity {

    private String chunkId;                     // 分片ID
    private String title;                       // 分段标题
    private String sourceFile;                  // 源文件名
    private String targetFile;                  // 上传后存储的文件名
    private String content;                     // 分片内容
    private String contentTok;                  // 支持搜索的内容转换
    private Map<String, Double> sparse;         // 根据contentTok推算出的稀疏数组
    private float[] embedding;                  // 分片内容向量
    private String blockType;                   // 分片类型
    private String sectionPath;                 // 分片路径
    private Integer page;                       // 分片所在页

}
