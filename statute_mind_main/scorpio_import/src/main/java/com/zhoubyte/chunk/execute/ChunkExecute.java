package com.zhoubyte.chunk.execute;

import com.zhoubyte.block.Block;
import com.zhoubyte.chunk.ChunkResult;
import com.zhoubyte.common.SupportFunction;
import com.zhoubyte.config.ChunkProperties;

public interface ChunkExecute extends SupportFunction {

    ChunkResult blockToChunk(Block block, String sectionPath, ChunkProperties properties);

}
