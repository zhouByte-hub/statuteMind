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
        parseStepContext.set(documentParseRecord);
        return parseStep;
    }


    /**
     * 添加子步骤：将步骤添加到对应的父级步骤下
     */
    public DocumentParseStep addStep(String documentName, String parentStepId, String stepName) {
        if(StringUtils.isEmpty(stepName)) {
            throw new IllegalArgumentException("StepName不能为空");
        }
        Map<String, List<DocumentParseStep>> documentParseRecord = parseStepContext.get();
        if(documentParseRecord == null || documentParseRecord.containsKey(documentName)) {
            throw new IllegalArgumentException(documentName + "文件的上下文未初始化");
        }
        DocumentParseStep parentStepNode = this.findParentStepNode(documentParseRecord.get(documentName), parentStepId);
        if(parentStepNode == null) {
            throw new RuntimeException("未找到父级步骤信息");
        }
        List<DocumentParseStep> subStep = parentStepNode.getSubStep();
        if(subStep == null) {
            subStep = new LinkedList<>();
        }
        DocumentParseStep subParseStep = new DocumentParseStep(stepName);
        subStep.add(subParseStep);
        return subParseStep;
    }

    private DocumentParseStep findParentStepNode(List<DocumentParseStep> documentParseSteps, String parentId) {
        if(documentParseSteps == null || documentParseSteps.isEmpty()) {
            return null;
        }
        for (DocumentParseStep documentParseStep : documentParseSteps) {
            if(documentParseStep.getStepId().equals(parentId)) {
                return documentParseStep;
            }
            return findParentStepNode(documentParseStep.getSubStep(), parentId);
        }
        return null;
    }

    /**
     *  添加子步骤
     *  recursion = true：将步骤添加到末尾步骤到子步骤中
     *  recursion = false：将步骤添加到步骤列表到末尾
     */
    public DocumentParseStep addStep(String documentName, String stepName, boolean recursion) {
        if(StringUtils.isEmpty(stepName)) {
            throw new RuntimeException("StepName不能为空");
        }
        Map<String, List<DocumentParseStep>> documentParseRecord = parseStepContext.get();
        if(documentParseRecord == null || documentParseRecord.containsKey(documentName)) {
            throw new IllegalArgumentException(documentName + "文件的上下文未初始化");
        }
        DocumentParseStep documentParseStep = new DocumentParseStep(stepName);
        List<DocumentParseStep> subStep;
        if(recursion) {
            // 寻找最末尾的步骤节点
            DocumentParseStep lastNode = this.findLastNode(documentParseRecord.get(documentName));
            subStep = lastNode == null ? new LinkedList<>() : lastNode.getSubStep();
        }else {
            subStep = documentParseRecord.get(documentName);
            if(subStep == null) {
                subStep = new LinkedList<>();
            }
        }
        subStep.add(documentParseStep);
        documentParseRecord.put(documentName, subStep);
        parseStepContext.set(documentParseRecord);
        return documentParseStep;
    }

    private DocumentParseStep findLastNode(List<DocumentParseStep> documentParseSteps) {
        if(documentParseSteps == null || documentParseSteps.isEmpty()) {
            return null;
        }
        // 获取最后一个步骤
        DocumentParseStep lastDocumentParseStep = documentParseSteps.getLast();
        if(lastDocumentParseStep.getSubStep() != null && !lastDocumentParseStep.getSubStep().isEmpty()) {
            lastDocumentParseStep = findLastNode(lastDocumentParseStep.getSubStep());
        }
        return lastDocumentParseStep;
    }


}
