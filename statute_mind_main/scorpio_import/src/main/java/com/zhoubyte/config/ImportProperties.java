package com.zhoubyte.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "scorpio.import")
public class ImportProperties {

    /**
     * 支持导入的文件扩展名
     */
    private List<String> support;

    /**
     * 单次导入文件的文件数量
     */
    private Integer importFileSize;

}
