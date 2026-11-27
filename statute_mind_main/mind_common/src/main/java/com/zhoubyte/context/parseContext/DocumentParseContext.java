package com.zhoubyte.context.parseContext;

import software.amazon.awssdk.utils.StringUtils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 提供对文档解析上下文的操作
 */
public class DocumentParseContext {

    // Map<documentName, stepEntity>
    private final ThreadLocal<Map<String, List<DocumentParseStep>>> parseStepContext;

    private DocumentParseContext(){
        this.parseStepContext = new ThreadLocal<>();
    }

    public static DocumentParseContext instanceContext() {
        return new DocumentParseContext();
    }

    public DocumentParseStep initParseStep(String documentName, String stepName) {
        if(StringUtils.isEmpty(documentName) || StringUtils.isEmpty(stepName)) {
            return null;
        }
        Map<String, List<DocumentParseStep>> documentParseRecord = parseStepContext.get();
        if(documentParseRecord == null) {
            documentParseRecord = new HashMap<>();
        }
        List<DocumentParseStep> documentParseSteps = documentParseRecord.get(documentName);
        if(documentParseSteps == null) {
            documentParseSteps = new LinkedList<>();
        }
        DocumentParseStep parseStep = documentParseSteps.stream()
                .filter(step -> stepName.equals(step.getStepName()))
                .findFirst().orElse(null);
        if(parseStep == null) {
            parseStep = new DocumentParseStep(stepName);
            documentParseSteps.add(parseStep);
            documentParseRecord.put(documentName, documentParseSteps);
        }
        return parseStep;
    }


    public DocumentParseStep addStep(String documentName, String parentStepId, String stepName) {
        if(StringUtils.isEmpty(stepName)) {
            throw new IllegalArgumentException("StepName不能为空");
        }
        Map<String, List<DocumentParseStep>> documentParseRecord = parseStepContext.get();
        if(documentParseRecord.containsKey(documentName)) {
            throw new IllegalArgumentException(documentName + "文件的上下文未初始化");
        }
        List<DocumentParseStep> documentParseSteps = documentParseRecord.get(documentName);
        DocumentParseStep parseStep = documentParseSteps.stream()
                .filter(step -> step.getStepId().equals(parentStepId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("未找到父级步骤信息"));
        List<DocumentParseStep> subStep = parseStep.getSubStep();
        if(subStep == null) {
            subStep = new LinkedList<>();
        }
        DocumentParseStep subParseStep = new DocumentParseStep(stepName);
        subStep.add(subParseStep);
        return subParseStep;
    }


}
