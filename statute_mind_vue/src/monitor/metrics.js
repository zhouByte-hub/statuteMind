/**
 * Agent 系统运行指标采集层。
 * 全部为真实埋点数据：
 * - 任务执行：beginTask 包装各视图的检索/判断/导入/对话/工作流运行，记录耗时与状态
 * - 响应延迟 / 吞吐量 / 错误率：按 1s 桶从任务完成事件聚合
 * - 大模型推理：recordLlm 记录 LLM 节点的 TTFT / 生成速度 / 输出长度
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
}
