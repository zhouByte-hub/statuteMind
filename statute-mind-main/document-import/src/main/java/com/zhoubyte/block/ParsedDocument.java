package com.zhoubyte.block;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParsedDocument {

    private String title;
    private List<Block> blocks;
}
