package com.zhoubyte.common;

public interface SparseEncoderFunction {

    // 获取全库的Chunk数
    Long chunkTotal();

    // 包含指定词的chunk数量
    Long includeChunkTotal(String chunk);
}
