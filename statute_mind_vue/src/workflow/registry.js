/** 工作流节点类型注册表：图标 / 分组 / 默认配置 / 输出变量 / 摘要 */

/** 内联 SVG 图标（stroke 风格，由组件统一包 <svg> 后 v-html 渲染） */
export const NODE_ICONS = {
  start: '<circle cx="12" cy="12" r="8.5"/><path d="M10.2 8.8l4.6 3.2-4.6 3.2V8.8Z" fill="currentColor" stroke="none"/>',
  answer: '<path d="M21 14a2 2 0 0 1-2 2H8l-5 4V6a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v8Z"/>',
  llm: '<path d="M12 3.5l1.7 4.4 4.4 1.7-4.4 1.7L12 15.7l-1.7-4.4-4.4-1.7 4.4-1.7L12 3.5Z"/><path d="M18.6 14.6l.8 2 2 .8-2 .8-.8 2-.8-2-2-.8 2-.8.8-2Z"/>',
  classifier: '<rect x="3.5" y="3.5" width="7" height="7" rx="1.5"/><rect x="13.5" y="3.5" width="7" height="7" rx="1.5"/><rect x="3.5" y="13.5" width="7" height="7" rx="1.5"/><rect x="13.5" y="13.5" width="7" height="7" rx="1.5"/>',
  knowledge: '<ellipse cx="12" cy="5.5" rx="7.5" ry="2.5"/><path d="M4.5 5.5v13c0 1.4 3.4 2.5 7.5 2.5s7.5-1.1 7.5-2.5v-13"/><path d="M4.5 12c0 1.4 3.4 2.5 7.5 2.5s7.5-1.1 7.5-2.5"/>',
  ifelse: '<circle cx="6" cy="5" r="2"/><circle cx="6" cy="19" r="2"/><circle cx="18" cy="12" r="2"/><path d="M6 7v10M8 5h4a4 4 0 0 1 4 4v1M8 19h4a4 4 0 0 0 4-4v-1"/>',
  code: '<path d="M8.5 7.5 4 12l4.5 4.5M15.5 7.5 20 12l-4.5 4.5"/>',
  http: '<circle cx="12" cy="12" r="8.5"/><path d="M3.5 12h17M12 3.5c2.3 2.4 3.7 5.3 3.7 8.5s-1.4 6.1-3.7 8.5c-2.3-2.4-3.7-5.3-3.7-8.5s1.4-6.1 3.7-8.5Z"/>',
  template: '<rect x="4" y="4" width="16" height="16" rx="2"/><path d="M4 9.5h16M9.5 9.5V20"/>',
  aggregator: '<path d="M4.5 5h15l-5.5 6.5v5l-4 2.5v-7.5L4.5 5Z"/>'
}

/** 节点分组（左侧节点库的展示顺序） */
export const NODE_GROUPS = [
  { key: 'basic', label: '基础', types: ['start', 'answer'] },
  { key: 'ai', label: 'AI 能力', types: ['llm', 'classifier'] },
  { key: 'know', label: '知识', types: ['knowledge'] },
  { key: 'logic', label: '逻辑', types: ['ifelse', 'code', 'aggregator'] },
  { key: 'trans', label: '转换', types: ['template', 'http'] }
]

/** 变量引用语法包裹：{{#nodeId.var#}} */
export const refOf = (nodeId, varName) => `{{#${nodeId}.${varName}#}}`

export const NODE_TYPES = {
  start: {
    label: '开始', group: 'basic', color: '#3D5CE5', icon: NODE_ICONS.start,
    desc: '定义工作流的输入参数', outputs: 1, hasTarget: false,
    defaults: { fields: [{ name: 'query', label: '用户问题' }] },
    outVars: [{ name: 'query', label: '用户问题' }],
    summary: d => `${(d.fields || []).length} 个输入字段`
  },
  answer: {
    label: '直接回复', group: 'basic', color: '#3F3129', icon: NODE_ICONS.answer,
    desc: '把最终结果回复给用户，可作为结束节点', outputs: 0, hasTarget: true,
    defaults: { text: '好的，已为你梳理相关法律依据。' },
    outVars: [],
    summary: d => truncate(d.text || '', 18) || '未配置回复内容'
  },
  llm: {
    label: 'LLM', group: 'ai', color: '#7C5CE5', icon: NODE_ICONS.llm,
    desc: '调用大语言模型生成分析（演示环境为模拟推理）', hasTarget: true,
    defaults: { model: 'qwen2.5:7b · 演示', temperature: 0.7, prompt: '你是法律领域智能助手，请依据给定材料专业、克制地回答用户问题。' },
    outVars: [{ name: 'text', label: '生成文本' }],
    summary: d => `${d.model || 'LLM'} · T=${d.temperature ?? 0.7}`
  },
  classifier: {
    label: '问题分类', group: 'ai', color: '#B25E00', icon: NODE_ICONS.classifier,
    desc: '把输入文本归类到预设分类，常用于分流', hasTarget: true,
    defaults: { input: '', classes: ['劳动争议', '侵权纠纷', '其他'] },
    outVars: [{ name: 'class_name', label: '分类名' }],
    summary: d => `${(d.classes || []).length} 个分类`
  },
  knowledge: {
    label: '知识检索', group: 'know', color: '#2E9E6B', icon: NODE_ICONS.knowledge,
    desc: '在法条知识库中按关键词命中检索', hasTarget: true,
    defaults: { input: '', topK: 3 },
    outVars: [{ name: 'chunks', label: '命中条文' }],
    summary: d => `Top ${d.topK || 3} 条`
  },
  ifelse: {
    label: '条件分支', group: 'logic', color: '#C63A30', icon: NODE_ICONS.ifelse,
    desc: '按条件把流程路由到不同分支（IF / ELSE）', outputs: -1, hasTarget: true,
    defaults: { cases: [{ id: 'c1', label: '分支 1', keyword: '' }] },
    outVars: [],
    summary: d => `${(d.cases || []).length} 个分支 + 否则`
  },
  code: {
    label: '代码执行', group: 'logic', color: '#2440B1', icon: NODE_ICONS.code,
    desc: '运行一段 JS 代码做数据处理（演示环境，请勿写死循环）', hasTarget: true,
    defaults: { language: 'javascript', code: '// inputs 为上游输出合集\nreturn { greeting: \' statuteMind\' }' },
    outVars: [{ name: 'output', label: '输出' }],
    summary: () => 'JavaScript'
  },
  http: {
    label: 'HTTP 请求', group: 'trans', color: '#D06A45', icon: NODE_ICONS.http,
    desc: '请求外部接口（演示环境返回预设响应，不实际发请求）', hasTarget: true,
    defaults: { method: 'GET', url: 'https://api.example.com/legal/lookup', mockResponse: '{\n  "status": 200,\n  "demo": true,\n  "message": "模拟外部接口响应"\n}' },
    outVars: [{ name: 'body', label: '响应体' }, { name: 'status', label: '状态码' }],
    summary: d => `${d.method || 'GET'} · 演示`
  },
  template: {
    label: '模板转换', group: 'trans', color: '#0E8A8A', icon: NODE_ICONS.template,
    desc: '用模板语法把多个变量拼装为一段文本', hasTarget: true,
    defaults: { template: '处理结果：{{#start_1.query#}}' },
    outVars: [{ name: 'output', label: '输出' }],
    summary: () => '模板转换'
  },
  aggregator: {
    label: '变量聚合', group: 'logic', color: '#5F6672', icon: NODE_ICONS.aggregator,
    desc: '把多个分支的变量汇聚为一个输出', hasTarget: true,
    defaults: { refs: [] },
    outVars: [{ name: 'output', label: '聚合结果' }],
    summary: d => `${(d.refs || []).length} 个变量`
  }
}

/** 条件分支的「否则」输出锚点 id */
export const ELSE_HANDLE = '__else__'

let seq = 0

/** 节点工厂：按类型生成带默认配置的画布节点 */
export function createNode(type, position){
  const meta = NODE_TYPES[type]
  if(!meta) return null
  return { id: `${type}_${++seq}`, type, position, data: structuredClone(meta.defaults) }
}

/** 载入既有流程后同步 id 序号，避免新建节点与已存节点 id 冲突 */
export function syncSeq(nodes){
  for(const n of nodes || []){
    const m = /_(\d+)$/.exec(n.id || '')
    if(m) seq = Math.max(seq, Number(m[1]))
  }
}

/** 判断变量引用是否存在于输出变量表 */
export function findOutVar(nodeType, varName){
  return (NODE_TYPES[nodeType]?.outVars || []).some(v => v.name === varName)
}

function truncate(str, n){
  const s = String(str).replace(/\s+/g, ' ')
  return s.length > n ? s.slice(0, n) + '…' : s
}
