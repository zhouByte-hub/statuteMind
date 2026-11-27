package com.zhoubyte.parser;

import com.zhoubyte.block.Block;
import com.zhoubyte.block.ParsedDocument;
import com.zhoubyte.context.ScorpioContext;
import com.zhoubyte.context.parseContext.DocumentParseContext;
import com.zhoubyte.utils.MarkdownUtils;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Component("excelParser")
@Slf4j
public class ExcelParser implements DocumentParser{

    private final DataFormatter dataFormatter = new DataFormatter();

    @Override
    public boolean support(String extension) {
        return "XLSX".equals(extension) || "XLSM".equals(extension) || "XLS".equals(extension);
    }

    @Override
    public ParsedDocument parse(byte[] documentData, String filename, DocumentParseContext context) {
        List<Block> blocks = new LinkedList<>();
        try(XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(documentData))) {
            for(int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                XSSFSheet sheet = workbook.getSheetAt(sheetIndex);
                String markdown = this.sheetToMarkdown(sheet, this.collectMergeValues(sheet));
                if(StringUtils.isEmpty(markdown)){
                    continue;
                }
                blocks.add(Block.table(markdown, sheetIndex));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new ParsedDocument(filename, blocks);
    }

    private String sheetToMarkdown(XSSFSheet sheet, Map<String, String> mergedArea) {
        List<String> title = new LinkedList<>();
        List<List<String>> sheetRows = new LinkedList<>();
        for(int rowIndex = 0; rowIndex < sheet.getLastRowNum(); rowIndex++) {
            XSSFRow row = sheet.getRow(rowIndex);
            if(row == null) {
                continue;
            }
            List<String> cells = new LinkedList<>();
            for(int colIndex = 0; colIndex < row.getLastCellNum(); colIndex++) {
                String value = mergedArea.getOrDefault(
                        rowIndex + "-" + colIndex,
                        dataFormatter.formatCellValue(row.getCell(colIndex)));
                cells.add(value);
            }
            if(rowIndex == 0) {
                title.addAll(cells); continue;
            }
            sheetRows.add(cells);
        }
        return MarkdownUtils.toMarkdown(title, sheetRows);
    }

    private Map<String, String> collectMergeValues(XSSFSheet sheet) {
        Map<String, String> mergeValues = new HashMap<>();
        for(int mergeAreaIndex = 0; mergeAreaIndex < sheet.getNumMergedRegions(); mergeAreaIndex++) {
            CellRangeAddress mergedRegion = sheet.getMergedRegion(mergeAreaIndex);
            XSSFRow row = sheet.getRow(mergedRegion.getFirstRow());
            if(row == null) {
                continue;
            }
            String value = dataFormatter.formatCellValue(row.getCell(mergedRegion.getFirstColumn())).trim();
            int rowIndex = Math.max(mergedRegion.getFirstRow(), 0);
            do {
                for(int colIndex = Math.max(mergedRegion.getFirstColumn(), 0); colIndex <= mergedRegion.getLastColumn(); colIndex++) {
                    if(rowIndex == mergedRegion.getFirstRow() && colIndex == mergedRegion.getLastColumn()) {
                        mergeValues.put(rowIndex + "-" + colIndex, value);
                        continue;
                    }
                    mergeValues.put(rowIndex + "-" + colIndex, value + "[merged]");
                }
            } while(rowIndex++ <= mergedRegion.getLastRow());
        }
        return mergeValues;
    }
}
