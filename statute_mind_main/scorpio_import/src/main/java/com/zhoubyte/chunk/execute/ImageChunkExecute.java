package com.zhoubyte.chunk.execute;

import com.zhoubyte.block.Block;
import com.zhoubyte.block.BlockType;
import com.zhoubyte.chunk.ChunkResult;
import com.zhoubyte.config.ChunkProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("imageChunkExecute")
@Slf4j
public class ImageChunkExecute implements ChunkExecute{
    @Override
    public List<ChunkResult> blockToChunk(Block block, String sectionPath, ChunkProperties properties) {
        return null;
    }

    @Override
    public boolean support(String content) {
        try{
            return BlockType.valueOf(content) == BlockType.IMAGE;
        }catch (Exception e) {
            log.error("ImageChunkExecute.support 方法解析失败", e);
            return false;
        }
    }
}
