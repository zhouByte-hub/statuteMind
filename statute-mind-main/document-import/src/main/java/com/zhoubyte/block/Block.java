package com.zhoubyte.block;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Block {

    private BlockType type;
    private String text;
    private int level;
    private int page;

    public static Block title(String text, int level, Integer page) {
        return new Block(BlockType.TITLE, text, level, page);
    }

    public static Block text(String text, Integer page) {
        return new Block(BlockType.TEXT, text, 0, page);
    }

    public static Block table(String markdown, Integer page) {
        return new Block(BlockType.TABLE, markdown, 0, page);
    }

    public static Block image(String description, Integer page) {
        return new Block(BlockType.IMAGE, description, 0, page);
    }
}
