package com.zhoubyte.core.service.impl;

import com.zhoubyte.block.ParsedDocument;
import com.zhoubyte.chunk.ChunkResult;
import com.zhoubyte.chunk.StructureAwareChunk;
import com.zhoubyte.model.ImportResponse;
import com.zhoubyte.core.service.ImportService;
import com.zhoubyte.parser.ParserRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        // 等待全部完成 + 收集结果
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).join();
        List<ParsedDocument> resultList = futureList.stream().map(CompletableFuture::join).toList();
        // 分片
        resultList.stream().map(structureAwareChunk::chunk).forEach(item -> {
            String docId = "doc-" + UUID.randomUUID();
            Map<String, Long> blockTypeCounts = new LinkedHashMap<>();
            for (ChunkResult chunkResult : item) {

            }

        });
        return null;
    }
}
