package com.zhoubyte.context;

import com.zhoubyte.context.parseContext.DocumentParseContext;
import lombok.Data;

/**
 * 顶层上下文对象，用于操作其他子集上下文对象
 */
@Data
public class ScorpioContext {

    private final DocumentParseContext documentParseContext;

    public ScorpioContext() {
        this.documentParseContext = DocumentParseContext.instanceContext();
    }


}
