/**
 * 工作流模拟执行引擎。
 * 数据流语义：从「开始」节点出发，节点在其所有入边落定后被激活或跳过；
 * 条件分支只激活命中的分支，未命中分支沿边传递「跳过」信号级联传播；
 * 变量引用统一 {{#nodeId.var#}} 语法，可真实计算的部分（分类/检索/模板/代码）真实计算。
 */
import { NODE_TYPES, ELSE_HANDLE } from './registry'
import { STATUTES, PRESET_KEYS, JUDGMENTS } from '@/data/mock'

const VAR_RE = /\{\{#([\w-]+)\.(\w+)#\}\}/g

/** 把上游输出渲染为可读文本（法条数组 → 条文清单） */
function stringify(v){
  if(v == null) return ''
  if(Array.isArray(v)){
    return v.map((c, i) => {
      if(c && c.law) return `${i + 1}. 《${String(c.law).replace(/[《》]/g, '')}》${c.art}：${c.text}`
      return `${i + 1}. ${typeof c === 'string' ? c : JSON.stringify(c)}`
    }).join('\n')
  }
  if(typeof v === 'object') return JSON.stringify(v, null, 2)
  return String(v)
}

/** 解析 {{#id.var#}} 引用；未命中的引用原样保留 */
export function resolveRefs(str, ctx){
  if(typeof str !== 'string') return str
  return str.replace(VAR_RE, (m, id, name) => {
    const out = ctx[id]
    return out && Object.prototype.hasOwnProperty.call(out, name) ? stringify(out[name]) : m
  })
}

const sleep = (ms, signal) => new Promise((resolve, reject) => {
  const t = setTimeout(resolve, ms)
  signal?.addEventListener('abort', () => {
    clearTimeout(t)
    reject(new DOMException('已停止', 'AbortError'))
  }, { once: true })
})

/** 从「变量引用配置」或上游输出中取文本作为本节点的处理输入 */
function pickText(ref, ctx, outs){
  if(ref){
    const r = resolveRefs(ref, ctx)
    if(!/\{\{#/.test(r)) return r
  }
  for(const o of outs){
    if(o.query != null) return String(o.query)
    if(o.class_name != null) return String(o.class_name)
    if(typeof o.text === 'string') return o.text
    if(typeof o.output === 'string') return o.output
  }
  return ''
}

/** 问题分类：预设正则粗匹配 → 类名包含 → 兜底最后一类 */
function classify(text, classes){
  const PRESET_CLASS = { j1: '劳动争议', j2: '侵权纠纷', j3: '侵权纠纷' }
  for(const [re, jid] of PRESET_KEYS){
    try{
      if(new RegExp(re).test(text)){
        const c = PRESET_CLASS[jid]
        if(classes.includes(c)) return c
      }
    }catch{ /* 忽略非法正则 */ }
  }
  const direct = classes.find(c => c !== '其他' && c && text.includes(c))
  return direct || classes[classes.length - 1] || '其他'
}

/** 知识检索：内置法条库关键词双向包含计分，取 TopK */
function searchStatutes(q, topK){
  const query = (q || '').trim()
  if(!query) return []
  return STATUTES
    .map(s => {
      const hits = s.kws.filter(k => query.includes(k) || k.includes(query))
      return hits.length ? { s, n: hits.length } : null
    })
    .filter(Boolean)
    .sort((a, b) => b.n - a.n)
    .slice(0, Math.max(1, topK || 3))
    .map(({ s }) => ({ law: s.law, art: s.art, text: s.text }))
}

/** 由开始问题定位预设判断，拼装 LLM 模拟输出；无预设时基于检索条文给通用建议 */
function composeLlm(ctx){
  let query = ''
  let chunks = []
  for(const out of Object.values(ctx)){
    if(out.query != null) query = String(out.query)
    if(Array.isArray(out.chunks)) chunks = out.chunks
  }
  for(const [re, jid] of PRESET_KEYS){
    if(new RegExp(re).test(query)){
      const j = JUDGMENTS[jid]
      if(j){
        return j.cps.map(c => `【${c.t}】${c.d}`).join('\n\n')
          + '\n\n（以上为演示环境的模拟分析，仅供参考，不构成法律意见）'
      }
    }
  }
  if(chunks.length){
    return `根据检索到的法条，初步建议如下：\n${
      chunks.map((c, i) => `${i + 1}. 《${String(c.law).replace(/[《》]/g, '')}》${c.art}——${c.text}`).join('\n')
    }\n\n建议结合具体事实咨询专业律师。（演示环境的模拟分析，仅供参考）`
  }
  return '未检索到直接相关的法条。建议补充更具体的关键词后再试。（演示环境的模拟分析，仅供参考）'
}

/**
 * 执行工作流。
 * @param flow {nodes, edges, input}  input 为开始节点的字段值，如 { query: '...' }
 * @param hooks {onNodeStart,onNodeFinish,onEdgeState,onToken}
 * @returns {answer, logs, ms}
 */
export async function runFlow(flow, hooks = {}, signal){
  const { nodes, edges, input = {} } = flow
  const nodeMap = new Map(nodes.map(n => [n.id, n]))
  const ctx = {}
  const logs = []
  const t0 = performance.now()
  let answer = ''

  const startNode = nodes.find(n => n.type === 'start')
  if(!startNode) throw new Error('流程缺少「开始」节点，无法运行')

  // pending：目标节点尚待落定的入边数；activated：已激活（真实送达）的入边数
  const pending = new Map()
  const activated = new Map()
  const settled = new Set()
  const queue = []

  const outEdges = new Map()
  for(const e of edges || []){
    if(!nodeMap.has(e.source) || !nodeMap.has(e.target)) continue
    if(!outEdges.has(e.source)) outEdges.set(e.source, [])
    outEdges.get(e.source).push(e)
    pending.set(e.target, (pending.get(e.target) || 0) + 1)
  }

  /** 源节点结束后沿出边传播：takenHandles 为空/undefined 表示全部激活（条件分支只传命中项） */
  function propagate(node, takenHandles){
    for(const e of outEdges.get(node.id) || []){
      const taken = !takenHandles || takenHandles.includes(e.sourceHandle ?? null)
      if(taken){
        activated.set(e.target, (activated.get(e.target) || 0) + 1)
        hooks.onEdgeState?.(e.id, 'active')
        settle(e.target)
      }else{
        skipEdge(e.target)
      }
    }
  }

  /** 跳过信号沿边级联：入边全部落定却无激活输入时，目标标记 skipped 并继续传播 */
  function skipEdge(targetId){
    const left = (pending.get(targetId) || 0) - 1
    pending.set(targetId, left)
    if(left > 0 || settled.has(targetId)) return
    if(activated.get(targetId)){
      queue.push(targetId)
      return
    }
    const node = nodeMap.get(targetId)
    node.data.status = 'skipped'
    node.data.result = '上游未命中'
    settled.add(targetId)
    hooks.onNodeFinish?.(targetId, 'skipped')
    propagate(node, [])
  }

  /** 激活信号：入边全部落定且有激活输入时入队执行 */
  function settle(targetId){
    const left = (pending.get(targetId) || 0) - 1
    pending.set(targetId, left)
    if(left <= 0 && !settled.has(targetId)) queue.push(targetId)
  }

  queue.push(startNode.id)

  while(queue.length){
    if(signal?.aborted) throw new DOMException('已停止', 'AbortError')
    const id = queue.shift()
    if(settled.has(id)) continue
    const node = nodeMap.get(id)
    const meta = NODE_TYPES[node.type]
    node.data.status = 'running'
    hooks.onNodeStart?.(id)
    const nt0 = performance.now()
    const outs = (edges || [])
      .filter(e => e.target === id && ctx[e.source])
      .map(e => ctx[e.source])
      .filter(Boolean)

    try{
      if(node.type !== 'start') await sleep(320 + Math.random() * 320, signal)
      const out = await runNode(node, { ctx, outs, input, hooks, signal })
      ctx[id] = out
      const ms = Math.round(performance.now() - nt0)
      node.data.status = 'success'
      node.data.result = summarize(node.type, out)
      settled.add(id)
      if(out && out.__answer) answer = out.text || ''
      logs.push({
        id, type: node.type, label: node.data.name || meta?.label || node.type,
        status: 'success', ms,
        input: captureInput(node, ctx, outs, input), output: publicOut(node.type, out)
      })
      hooks.onNodeFinish?.(id, 'success', node.data.result, ms)
      for(const e of edges || []){
        if(e.target === id && ctx[e.source]) hooks.onEdgeState?.(e.id, 'done')
      }
      propagate(node, out && out.__branch ? [out.__branch] : undefined)
    }catch(err){
      if(err?.name === 'AbortError') throw err
      const ms = Math.round(performance.now() - nt0)
      node.data.status = 'failed'
      node.data.result = err?.message || '执行出错'
      settled.add(id)
      logs.push({
        id, type: node.type, label: node.data.name || meta?.label || node.type,
        status: 'failed', ms,
        input: { 上游: outs.map(o => Object.keys(o).join(',')) },
        output: { error: err?.message || String(err) }
      })
      hooks.onNodeFinish?.(id, 'failed', node.data.result, ms)
      propagate(node, [])
    }
  }

  // 兜底：因断链/成环滞留的节点标记为跳过
  for(const n of nodes){
    if(!settled.has(n.id)){
      n.data.status = 'skipped'
      n.data.result = '未接入流程'
      hooks.onNodeFinish?.(n.id, 'skipped')
    }
  }

  return { answer, logs, ms: Math.round(performance.now() - t0) }
}

/** 单节点执行：按类型分发（可真实计算的真实计算，外部依赖的模拟返回） */
async function runNode(node, env){
  const { ctx, outs, input, hooks, signal } = env
  const d = node.data

  switch(node.type){
    case 'start': {
      const out = {}
      for(const f of d.fields || []) out[f.name] = input[f.name] ?? ''
      return out
    }
    case 'classifier':
      return { class_name: classify(pickText(d.input, ctx, outs), d.classes || []) }
    case 'ifelse': {
      const text = pickText(d.input, ctx, outs)
      const hit = (d.cases || []).find(c => c.keyword && text.includes(c.keyword))
      return {
        __branch: hit ? hit.id : ELSE_HANDLE,
        __branchLabel: hit ? (hit.label || hit.id) : '否则'
      }
    }
    case 'knowledge':
      return { chunks: searchStatutes(pickText(d.input, ctx, outs), d.topK) }
    case 'llm': {
      resolveRefs(d.prompt, ctx) // 预热模板解析；正文由 composeLlm 依上下文拼装
      const text = composeLlm(ctx)
      // 模拟流式输出：按小块逐字回调
      const step = 3
      for(let i = 0; i < text.length; i += step){
        if(signal?.aborted) throw new DOMException('已停止', 'AbortError')
        hooks.onToken?.(node.id, text.slice(i, i + step))
        await sleep(14, signal)
      }
      return { text }
    }
    case 'code': {
      const merged = Object.assign({}, ...outs)
      const fn = new Function('inputs', `"use strict";\n${d.code || 'return {}'}`)
      const out = await fn(merged)
      return { output: out === undefined ? null : out }
    }
    case 'http': {
      await sleep(500 + Math.random() * 300, signal)
      let body = d.mockResponse
      try{ body = JSON.parse(d.mockResponse) }catch{ /* 保留原文 */ }
      return { body, status: 200 }
    }
    case 'template':
      return { output: resolveRefs(d.template, ctx) }
    case 'aggregator': {
      const vals = (d.refs || [])
        .map(r => resolveRefs(r, ctx))
        .filter(v => v !== '' && !/\{\{#/.test(v))
      return { output: vals.length === 1 ? vals[0] : vals }
    }
    case 'answer':
      return { text: resolveRefs(d.text, ctx) || '（未配置回复内容）', __answer: true }
    default:
      throw new Error(`未知节点类型：${node.type}`)
  }
}

/** 日志里的输入快照 */
function captureInput(node, ctx, outs, input){
  if(node.type === 'start') return { ...input }
  if(node.type === 'llm') return { prompt: resolveRefs(node.data.prompt, ctx) }
  if(node.type === 'code') return { inputs: Object.assign({}, ...outs) }
  return { 上游: outs.length ? outs.map(stringify).join('\n---\n') : '（无上游输入）' }
}

/** 日志输出瘦身：剥离内部字段 */
function publicOut(type, out){
  if(type === 'ifelse') return { 走向: out.__branchLabel || out.__branch }
  const clean = {}
  for(const [k, v] of Object.entries(out)){
    if(!k.startsWith('__')) clean[k] = v
  }
  return clean
}

/** 节点卡片上的一行结果摘要 */
function summarize(type, out){
  if(!out) return ''
  if(type === 'classifier') return `分类：${out.class_name}`
  if(type === 'knowledge') return `命中 ${out.chunks.length} 条法条`
  if(type === 'llm') return `生成 ${out.text.length} 字`
  if(type === 'ifelse') return `走向：${out.__branchLabel || out.__branch}`
  if(type === 'answer') return '已回复'
  return stringify(out.output ?? out.body ?? Object.values(out)[0]).slice(0, 40)
}
