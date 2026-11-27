package com.zhoubyte.utils;

import com.zhoubyte.common.SparseEncoderFunction;
import io.micrometer.common.util.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConditionalOnBean(SparseEncoderFunction.class)
public class SparseEncoder {

    private final SparseEncoderFunction sparseEncoderFunction;

    public SparseEncoder(SparseEncoderFunction sparseEncoderFunction) {
        this.sparseEncoderFunction = sparseEncoderFunction;
    }

    /**
     * 为 chunk 生成稀疏向量
     * 词项 → TF × IDF 权重
     */
    public Map<String, Double> encodeIndex(String preHandlerTokenizer) {
        if(StringUtils.isEmpty(preHandlerTokenizer)) {
            return Map.of();
        }
        Map<String, Integer> termFrequency = new HashMap<>(); // 词项
        for (String item : preHandlerTokenizer.split(" ")) {
            termFrequency.merge(item, 1, Integer::sum);
        }
        Map<String, Double> sparse = new HashMap<>();
        for (Map.Entry<String, Integer> entry : termFrequency.entrySet()) {
            // total：词库中所有的chunk数量，df：包含指定词的chunk数量
            long df = sparseEncoderFunction.includeChunkTotal(entry.getKey());
            Double idf = Math.log((double) (sparseEncoderFunction.chunkTotal() + 1) / (df + 1)) + 1.0;
            sparse.put(entry.getKey(), entry.getValue() * idf);
        }
        return sparse;
    }
}
