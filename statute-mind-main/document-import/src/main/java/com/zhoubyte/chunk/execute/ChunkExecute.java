package com.zhoubyte.chunk.execute;

import com.zhoubyte.block.Block;
import com.zhoubyte.chunk.ChunkResult;
import com.zhoubyte.common.SupportFunction;
import com.zhoubyte.config.ChunkProperties;

import java.util.List;

public interface ChunkExecute extends SupportFunction {

    List<ChunkResult> blockToChunk(Block block, String sectionPath, ChunkProperties properties);

}
