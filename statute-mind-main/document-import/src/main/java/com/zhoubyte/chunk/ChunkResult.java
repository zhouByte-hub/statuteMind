package com.zhoubyte.chunk;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChunkResult {

    private String content;
    private String blockType;
    private String sectionPath;
    private Integer page;
    private Integer position;
    private String title;
    private String sourceFilename;

}
