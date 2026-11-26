package com.zhoubyte.parser.wordElement;

import com.zhoubyte.block.Block;
import com.zhoubyte.utils.MarkdownUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

@Component("wordTableElementParser")
@Slf4j
public class WordTableElementParser implements WordElementParser{

    @Override
    public boolean support(IBodyElement element) {
        return element instanceof XWPFTable;
    }

    @Override
    public List<Block> parseWordElement(IBodyElement element) {
        XWPFTable table = (XWPFTable) element;
        List<String> tableTitle = new LinkedList<>();
        List<List<String>> rows = new LinkedList<>();

        for(int rowIndex = 0; rowIndex < table.getRows().size(); rowIndex++) {
            List<String> list = table.getRow(rowIndex).getTableCells()
                    .stream()
                    .map(XWPFTableCell::getText)
                    .toList();
            // 首行设为标题行
            if(rowIndex == 0) {
                tableTitle.addAll(list);
            }else {
                // 内容行
                rows.add(list);
            }
        }
        if(tableTitle.isEmpty() && rows.isEmpty()) {
            return List.of();
        }
        String markdown = MarkdownUtils.toMarkdown(tableTitle, rows);
        return List.of(Block.table(markdown, 0));
    }


}
