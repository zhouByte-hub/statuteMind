package com.zhoubyte.core.service.impl;

import com.zhoubyte.block.ParsedDocument;
import com.zhoubyte.chunk.StructureAwareChunk;
import com.zhoubyte.config.ChunkProperties;
import com.zhoubyte.core.model.ChunkEntity;
import com.zhoubyte.core.repository.es.ChunkRepository;
import com.zhoubyte.model.ImportResponse;
import com.zhoubyte.core.service.ImportService;
import com.zhoubyte.parser.ParserRouter;
import com.zhoubyte.utils.SimpleTokenizer;
import com.zhoubyte.utils.SparseEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImportServiceImpl implements ImportService {

    private final ParserRouter parserRouter;
    private final Executor fileImportExecutor;
    private final StructureAwareChunk structureAwareChunk;
    private final ChunkProperties chunkProperties;
    private final SparseEncoder sparseEncoder;
    private final EmbeddingModel embeddingModel;
    private final ChunkRepository chunkRepository;

    @Override
    public List<String> supportFileExtensions() {
        return parserRouter.supportedExtensions();
    }

    @Override
    public List<ImportResponse> importFile(List<MultipartFile> fileList) {
        if(fileList.size() > parserRouter.importFileSize()) {
            throw new IllegalArgumentException("单次导入最多支持"+parserRouter.importFileSize()+"个文件");
        }
        List<CompletableFuture<ParsedDocument>> futureList = new ArrayList<>();
        for (MultipartFile file : fileList) {
            CompletableFuture<ParsedDocument> futureItem = CompletableFuture.supplyAsync(() -> {
                try {
                    return parserRouter.parse(file.getInputStream(), file.getOriginalFilename());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, fileImportExecutor);
            futureList.add(futureItem);
        }
        // 等待全部完成 + 收集分块结果
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).join();
        List<ParsedDocument> resultList = futureList.stream().map(CompletableFuture::join).toList();
        // 分片向量化 -> 入库
        List<ChunkEntity> list = resultList.stream().map(structureAwareChunk::chunk)
                .flatMap(List::stream)
                .map(chunkResult -> {
                    String chunkId = "chunk-" + UUID.randomUUID();
                    // 预处理
                    String preTokenizer = SimpleTokenizer.preHandleTokenizer(chunkResult.getContent(), chunkProperties.getCjkSize());
                    ChunkEntity chunkEntity = ChunkEntity.builder()
                            .chunkId(chunkId)
                            .title(chunkResult.getTitle())
                            .sourceFile(chunkResult.getSourceFilename())
                            .targetFile(null)
                            .content(chunkResult.getContent())
                            .contentTok(preTokenizer)
                            .sparse(sparseEncoder.encodeIndex(preTokenizer))
                            .embedding(null)
                            .sectionPath(chunkResult.getSectionPath())
                            .blockType(chunkResult.getBlockType())
                            .page(chunkResult.getPage())
                            .build();
                    float[] chunkEmbed = embeddingModel.embed(chunkResult.getContent());
                    chunkEntity.setEmbedding(chunkEmbed);
                    return chunkEntity;
                }).toList();
        // 批量入库
        chunkRepository.saveAll(list);
        // 返回导入结果
        return list.stream().map(entity -> {
            return new ImportResponse(entity.getChunkId(), entity.getTitle(), entity.getSourceFile(), entity.getBlockType());
        }).toList();
    }
}
