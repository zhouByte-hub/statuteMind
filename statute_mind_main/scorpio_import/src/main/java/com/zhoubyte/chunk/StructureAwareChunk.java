package com.zhoubyte.chunk;

import com.zhoubyte.block.Block;
import com.zhoubyte.block.BlockType;
import com.zhoubyte.block.ParsedDocument;
import com.zhoubyte.chunk.execute.ChunkExecute;
import com.zhoubyte.config.ChunkProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class StructureAwareChunk {

    private final ChunkProperties chunkProperties;
    private final List<ChunkExecute> chunkExecutes;

    public List<ChunkResult> chunk(ParsedDocument document) {
        return this.chunk(document, chunkProperties);
    }

    public List<ChunkResult> chunk(ParsedDocument document, ChunkProperties chunkProperties) {
        List<ChunkResult> result = new LinkedList<>();
        Deque<int[]> titleStack = new ArrayDeque<>(); // [level, index]
        List<String> titles = new LinkedList<>();
        for (Block block : document.getBlocks()) {
            // 如果是标题
            if(BlockType.TITLE == block.getType()) {
                // 标题级别一般都是【1，2，3，4，5】; 如果当前标题大于上一个标题，则表示一节结束
                while(!titleStack.isEmpty() && titleStack.peek()[0] >= block.getLevel()) {
                    titleStack.pop();
                }
                titles.add(block.getText());
                titleStack.push(new int[] {block.getLevel(), titles.size() - 1});
                continue;
            }
            String sectionPath = sectionPath(titleStack, titles);
            for (ChunkExecute chunkExecute : chunkExecutes) {
                if(!chunkExecute.support(block.getType().name())){
                    continue;
                }
                ChunkResult chunkResult = chunkExecute.blockToChunk(block, sectionPath, chunkProperties);
                result.add(chunkResult);
            }
        }
        return result;
    }

    private String sectionPath(Deque<int[]> titleStack, List<String> titles) {
        if(titles.isEmpty()) {
            return "[default]";
        }
        return titleStack.stream()
                .map(item -> titles.get(item[1]))
                .reduce((a, b) -> a + ">" + b)
                .orElse("");
    }
}
