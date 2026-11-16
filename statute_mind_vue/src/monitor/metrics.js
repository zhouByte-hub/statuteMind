/**
 * Agent 系统运行指标采集层。
 * 全部为真实埋点数据：
 * - 任务执行：beginTask 包装各视图的检索/判断/导入/对话/工作流运行，记录耗时与状态
 * - 响应延迟 / 吞吐量 / 错误率：按 1s 桶从任务完成事件聚合
 * - 大模型推理：recordLlm 记录 LLM 节点的 TTFT / 生成速度 / 输出长度
 * - 按日聚合：每条任务/LLM 事件计入当天记录（完成率/失败率/文档解析/RAG），localStorage 持久化
 * - 全局异常：window error / unhandledrejection
 */
import { reactive } from 'vue'

const MAX = 120 // 每条时间序列保留 120 个 1s 采样点（2 分钟窗口）

let seq = 0
let inited = false

/** 任务事件计数桶（按 1s 聚合出延迟/吞吐/错误率） */
let bucket = { ts: 0, reqs: 0, failed: 0, msSum: 0, msN: 0 }

function stepBucket(ts){
  // 补齐空桶（无活动的秒也占位，保持时间轴均匀）
  if(!bucket.ts) bucket.ts = ts
  while(ts - bucket.ts >= 1000){
    bucket.ts += 1000
    const b = bucket
    monitor.series.latency.push(b.msN ? Math.round(b.msSum / b.msN) : null)
    monitor.series.throughput.push(b.reqs)
    monitor.series.errorRate.push(b.reqs ? Math.round(b.failed / b.reqs * 1000) / 10 : null)
    for(const k of ['latency', 'throughput', 'errorRate']) if(monitor.series[k].length > MAX) monitor.series[k].shift()
    bucket = { ts, reqs: 0, failed: 0, msSum: 0, msN: 0 }
  }
}

function tick(){
  stepBucket(Date.now())
  monitor.now = Date.now()
  if(monitor.lastError && monitor.now - monitor.lastError.at > 8000) monitor.lastError = null
}

export const monitor = reactive({
  now: 0,
  /** 运行中任务 */
  active: [],
  /** 已完成任务历史（新在前） */
  tasks: [],
  totals: { requests: 0, success: 0, failed: 0, skipped: 0, nodes: 0 },
  /** 大模型推理指标（按 LLM 节点调用逐次记录，新在前，最多 60 次） */
  llm: { calls: 0, ttft: [], speed: [], outLen: [] },
  lastError: null,
  /** 按日聚合记录（key: YYYY-MM-DD），用于运行日历 */
  days: {},
  /** 1s 粒度时间序列 */
  series: {
    latency: [],    // ms（任务均值，无活动为 null）
    throughput: [], // 请求/s
    errorRate: []   // %（无请求为 null）
  }
})

/** 全局初始化（幂等），在 main.js 挂载前调用 */
export function initMonitor(){
  if(inited) return
  inited = true

  // 按日记录（运行日历数据源）
  loadDays()

  // 1s 采样与聚合
  setInterval(tick, 1000)

  // 全局异常捕获
  window.addEventListener('error', e => {
    monitor.lastError = { at: Date.now(), msg: e.message || '脚本错误', src: e.filename ? 'window' : 'resource' }
  })
  window.addEventListener('unhandledrejection', e => {
    monitor.lastError = { at: Date.now(), msg: String(e.reason?.message || e.reason || '未处理的 Promise 拒绝'), src: 'promise' }
  })
}

/**
 * 开始一个任务并计数。返回 end(status) 结束函数。
 * @param type 任务类型：search|judge|import|chat|workflow
 */
export function beginTask(type, label){
  const id = ++seq
  const start = performance.now()
  const item = { id, type, label, start }
  monitor.active.push(item)
  monitor.totals.requests++
  bucket.reqs++
  return (status = 'success', detail = '') => {
    const i = monitor.active.indexOf(item)
    if(i >= 0) monitor.active.splice(i, 1)
    const ms = Math.round(performance.now() - start)
    bucket.msSum += ms
    bucket.msN++
    if(status === 'failed') bucket.failed++
    monitor.totals[status] = (monitor.totals[status] || 0) + 1
    monitor.tasks.unshift({ id, type, label, ms, status, detail, at: Date.now() })
    if(monitor.tasks.length > 30) monitor.tasks.pop()
    recDayTask(type, status, ms, detail)
    return ms
  }
}

/** 工作流节点执行计数（由 WorkflowView 的 onNodeFinish 钩子喂入） */
export function countNode(ok){
  monitor.totals.nodes++
  if(!ok) bucket.failed++
}

/**
 * 大模型推理指标（由工作流 LLM 节点经 onLlmMetric 钩子喂入，全部实测）。
 * @param m { ttftMs 首字延迟, totalMs 总生成耗时, chars 输出字符数 }
 */
export function recordLlm({ ttftMs, totalMs, chars }){
  const L = monitor.llm
  L.calls++
  L.ttft.unshift(Math.round(ttftMs))
  L.speed.unshift(chars > 0 && totalMs > 0 ? Math.round(chars / (totalMs / 1000)) : 0)
  L.outLen.unshift(chars)
  for(const k of ['ttft', 'speed', 'outLen']) if(L[k].length > 60) L[k].pop()
  recDayLlm()
}

/* ===== 按日聚合（运行日历数据源） =====
 * 记录结构：t 任务数 / ok 成功 / fail 失败 / skip 跳过
 *           imp 导入次数 / impOk 解析成功次数
 *           rag 检索次数 / ragHit 命中次数 / ragScore 命中条数累计
 *           ms 任务耗时累计 / llm LLM 调用次数 / demo 标记演示数据
 */
const DAY_KEY = 'sm.monitor.days.v1'
const blankDay = () => ({ t: 0, ok: 0, fail: 0, skip: 0, imp: 0, impOk: 0, rag: 0, ragHit: 0, ragScore: 0, ms: 0, llm: 0 })

function dayKey(ts){
  const d = new Date(ts)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function todayRec(){
  const k = dayKey(Date.now())
  if(!monitor.days[k]) monitor.days[k] = blankDay()
  return monitor.days[k]
}

/** 任务结束时计入当天记录 */
function recDayTask(type, status, ms, detail){
  const d = todayRec()
  d.t++
  d.ms += ms
  if(status === 'success') d.ok++
  else if(status === 'failed') d.fail++
  else d.skip++
  if(type === 'import'){
    d.imp++
    if(status === 'success') d.impOk++
  }
  if(type === 'search'){
    d.rag++
    const m = /(\d+)\s*条命中/.exec(detail || '')
    if(m){ d.ragHit++; d.ragScore += +m[1] }
  }
  saveDays()
}

/** LLM 调用计入当天记录 */
function recDayLlm(){
  todayRec().llm++
  saveDays()
}

let saveTimer = null
function saveDays(force){
  if(force){ writeDays(); return }
  if(saveTimer) return
  saveTimer = setTimeout(() => { saveTimer = null; writeDays() }, 800)
}
function writeDays(){
  try{ localStorage.setItem(DAY_KEY, JSON.stringify(monitor.days)) }catch{ /* 隐私模式等场景忽略 */ }
}

/** 生成过去 90 天的演示历史（仅首次访问，标记 demo: 1） */
function seedDemo(){
  const rnd = (s => () => (s = s * 16807 % 2147483647) / 2147483647)(20111114)
  const base = new Date(); base.setHours(12, 0, 0, 0)
  for(let i = 1; i <= 90; i++){
    const d = new Date(base - i * 86400000)
    const weekend = d.getDay() === 0 || d.getDay() === 6
    if(weekend && rnd() < 0.6) continue
    const t = weekend ? 4 + Math.floor(rnd() * 6) : 14 + Math.floor(rnd() * 18)
    const fail = Math.floor(t * (0.02 + rnd() * 0.1))
    const skip = Math.floor(t * rnd() * 0.08)
    const imp = Math.floor(rnd() * 4)
    const rag = Math.floor(t * (0.35 + rnd() * 0.25))
    const ragHit = Math.max(1, Math.floor(rag * (0.82 + rnd() * 0.15)))
    monitor.days[dayKey(d.getTime())] = {
      demo: 1, t, ok: t - fail - skip, fail, skip,
      imp, impOk: imp ? imp - (rnd() < 0.15 ? 1 : 0) : 0,
      rag, ragHit, ragScore: ragHit * (2 + Math.floor(rnd() * 6)),
      ms: t * (600 + Math.floor(rnd() * 1800)),
      llm: Math.floor(t * (0.5 + rnd() * 0.6))
    }
  }
}

/** 加载持久化的按日记录；首次访问生成演示历史；仅保留最近 120 天 */
export function loadDays(){
  try{
    const raw = localStorage.getItem(DAY_KEY)
    if(raw){ Object.assign(monitor.days, JSON.parse(raw)) }
    else { seedDemo(); saveDays(true) }
  }catch{ seedDemo() }
  const cutoff = dayKey(Date.now() - 120 * 86400000)
  for(const k of Object.keys(monitor.days)) if(k < cutoff) delete monitor.days[k]
}
