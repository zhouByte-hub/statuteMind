package com.zhoubyte.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "scorpio.chunk")
@Data
public class ChunkProperties {

    /**
     * TEXT 块目标长度（字符）
     */
    private Integer chunkSize = 500;

    /**
     * 相邻块重叠字符数
     */
    private Integer overlap = 50;

    /**
     * 表格块最大字符数，超出按行组切分
     */
    private Integer maxTableChars = 6000;

    /**
     * 是否启用语义分块（超长 TEXT 块按 embedding 相似度找切分点）
     */
    private Boolean semantic = false;

    /**
     * 中、日、韩文字的拆分长度
     */
    private Integer cjkSize = 2;

}
