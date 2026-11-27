package com.zhoubyte.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImportResponse {

    private String chunkId;
    private String title;
    private String sourceFile;
//    private int chunkCount;
    private String blockType;

}
