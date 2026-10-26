/** 内置示例工作流：「欠薪维权助手」——展示 分类 → 分支 → 检索 → LLM → 回复 全链路 */
import { ELSE_HANDLE } from './registry'

const n = (id, type, x, y, data) => ({ id, type, position: { x, y }, data })

export function makeSampleFlow(){
  return {
    name: '欠薪维权助手（示例）',
    nodes: [
      n('start_1', 'start', 60, 240, { fields: [{ name: 'query', label: '用户问题' }] }),
      n('classifier_1', 'classifier', 330, 225, {
        input: '{{#start_1.query#}}',
        classes: ['劳动争议', '侵权纠纷', '其他']
      }),
      n('ifelse_1', 'ifelse', 600, 210, {
        input: '{{#classifier_1.class_name#}}',
        cases: [{ id: 'c1', label: '劳动争议', keyword: '劳动' }]
      }),
      n('knowledge_1', 'knowledge', 880, 90, { input: '{{#start_1.query#}}', topK: 3 }),
      n('llm_1', 'llm', 1150, 85, {
        model: 'qwen2.5:7b · 演示',
        temperature: 0.7,
        prompt: '你是法律领域智能助手。用户问题：{{#start_1.query#}}\n\n检索到的法条：\n{{#knowledge_1.chunks#}}\n\n请给出行为定性、法律依据与维权建议。'
      }),
      n('answer_1', 'answer', 1420, 100, { text: '{{#llm_1.text#}}' }),
      n('template_1', 'template', 880, 340, {
        template: '抱歉，当前演示知识库主要覆盖劳动争议场景，暂未命中「{{#classifier_1.class_name#}}」的专属处理流程。'
      }),
      n('answer_2', 'answer', 1150, 355, { text: '{{#template_1.output#}}' })
    ],
    edges: [
      { id: 'e1', source: 'start_1', target: 'classifier_1' },
      { id: 'e2', source: 'classifier_1', target: 'ifelse_1' },
      { id: 'e3', source: 'ifelse_1', sourceHandle: 'c1', target: 'knowledge_1' },
      { id: 'e4', source: 'ifelse_1', sourceHandle: ELSE_HANDLE, target: 'template_1' },
      { id: 'e5', source: 'knowledge_1', target: 'llm_1' },
      { id: 'e6', source: 'llm_1', target: 'answer_1' },
      { id: 'e7', source: 'template_1', target: 'answer_2' }
    ]
  }
}
