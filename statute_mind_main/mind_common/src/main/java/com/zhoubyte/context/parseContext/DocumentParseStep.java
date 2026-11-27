package com.zhoubyte.context.parseContext;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class DocumentParseStep {

    private String stepId;                                      // 步骤编号-自动生成
    private String stepName;                                    // 步骤名称
    private String description;                                 // 步骤基本描述
    private LocalDateTime createTime;                           // 步骤创建时间
    private LocalDateTime completeTime;                         // 步骤完成时间
    private Integer order;                                      // 步骤顺序
    private List<DocumentParseStep> subStep;                    // 子步骤
    private Float executeProgress;                              // 执行进度
    private StepExecuteResultEnum result;                       // 执行结果
    private String errorMsg;                                    // 当前步骤的错误信息

    private DocumentParseStep(){

    }

    public DocumentParseStep(String stepName) {
        this.stepId = UUID.randomUUID().toString();
        this.stepName = stepName;
        this.createTime = LocalDateTime.now();
        this.order = 0;
        this.subStep = List.of();
        this.executeProgress = 0.0f;
        this.result = StepExecuteResultEnum.UNEXECUTE;
    }


}
