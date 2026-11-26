package com.zhoubyte;

import com.zhoubyte.utils.MarkdownUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.LinkedList;
import java.util.List;

@SpringBootTest(classes = MarkdownUtilsTest.class)
public class MarkdownUtilsTest {

    @Test
    public void test(){
        List<String> title = List.of("序号", "姓名", "性别", "家庭住址");
        List<List<String>> rows = new LinkedList<>();
        rows.add(List.of("1", "张三", "男", "湖南省长沙市岳麓区"));
        rows.add(List.of("2", "李四", "男", "广东省深圳市福田区"));
        String markdown = MarkdownUtils.toMarkdown(title, rows);
        System.out.println(markdown);
    }
}
