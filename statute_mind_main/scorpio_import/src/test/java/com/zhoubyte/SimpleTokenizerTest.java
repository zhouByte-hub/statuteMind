package com.zhoubyte;

import com.zhoubyte.utils.SimpleTokenizer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest(classes = SimpleTokenizerTest.class)
public class SimpleTokenizerTest {

    @Test
    public void test() {
        String content = "feat(backend): 文档并发解析与结构感知分块。";
        System.out.println(SimpleTokenizer.preHandleTokenizer(content, 2));
    }

    @Test
    public void test2() {
        String content = "1、李琳，男，1988年12月25日出生，户籍地址：江苏省新沂市草桥镇堰头街120号，身份证号码：320381198812252138。";
        System.out.println(SimpleTokenizer.preHandleTokenizer(content, 15));
    }
}
