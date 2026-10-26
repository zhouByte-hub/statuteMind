/**
 * 工作流本地持久化：多流程列表（localStorage，key 带版本号，仅存可序列化纯数据）。
 * 数据结构：{ activeId, flows: [{ id, name, nodes, edges, updatedAt }] }
 */
import { reactive } from 'vue'
import { makeSampleFlow } from './sampleFlow'
import { createNode, syncSeq } from './registry'

const KEY = 'statuteMind.wf.v2'
const OLD_KEY = 'statuteMind.wf.v1'

/** 运行期瞬态字段（不入持久化：刷新后不应残留上次运行的徽标） */
const TRANSIENT = ['status', 'result']

const stripRunState = nodes => nodes.map(n => ({ ...n, data: Object.fromEntries(
  Object.entries(n.data || {}).filter(([k]) => !TRANSIENT.includes(k))
) }))

const uid = () => `f_${Date.now().toString(36)}${Math.random().toString(36).slice(2, 6)}`

/** 校验并规整一条流程（无效返回 null） */
function sanitizeFlow(f){
  if(!f || !Array.isArray(f.nodes) || !f.nodes.length) return null
  syncSeq(f.nodes)
  return {
    id: f.id || uid(),
    name: f.name || '未命名流程',
    nodes: stripRunState(f.nodes),
    edges: Array.isArray(f.edges) ? f.edges : [],
    updatedAt: f.updatedAt || Date.now()
  }
}

/** 内置示例包装为一条可存储流程 */
export function sampleFlow(){
  return { ...sanitizeFlow(makeSampleFlow()), id: uid() }
}

/** 空白新流程：仅一个「开始」节点 */
export function blankFlow(){
  return {
    id: uid(),
    name: '未命名流程',
    nodes: [createNode('start', { x: 140, y: 240 })],
    edges: [],
    updatedAt: Date.now()
  }
}

export function persist(store){
  cache = store
  try{ localStorage.setItem(KEY, JSON.stringify(store)) }catch{ /* 隐私模式等场景下静默失败 */ }
}

/** 模块级单例：以 reactive 包装，storage 操作与视图共享同一代理，保证响应式 */
let cache = null

/** 读取全部流程（含 v1 单流程迁移；无数据时自动种入内置示例） */
export function loadStore(){
  if(cache) return cache
  try{
    const raw = localStorage.getItem(KEY)
    if(raw){
      const s = JSON.parse(raw)
      const flows = (s?.flows || []).map(sanitizeFlow).filter(Boolean)
      if(flows.length){
        cache = reactive({
          activeId: flows.some(f => f.id === s.activeId) ? s.activeId : flows[0].id,
          flows
        })
        return cache
      }
    }
    // v1 单流程 → 迁移为多流程结构
    const old = localStorage.getItem(OLD_KEY)
    if(old){
      const f = sanitizeFlow(JSON.parse(old))
      if(f){
        cache = reactive({ activeId: f.id, flows: [f] })
        persist(cache)
        try{ localStorage.removeItem(OLD_KEY) }catch{ /* 同上 */ }
        return cache
      }
    }
  }catch{ /* 数据损坏时回退到示例 */ }
  const f = sampleFlow()
  cache = reactive({ activeId: f.id, flows: [f] })
  persist(cache)
  return cache
}

/** 保存某条流程的内容（自动保存入口） */
export function saveFlow(id, data){
  const store = loadStore()
  const f = store.flows.find(x => x.id === id)
  if(f) Object.assign(f, data, { updatedAt: Date.now() })
  persist(store)
}

/** 新建流程并设为当前，返回新流程 */
export function addFlow(){
  const store = loadStore()
  const f = blankFlow()
  store.flows.push(f)
  store.activeId = f.id
  persist(store)
  return f
}

/** 删除流程（删空自动补示例；删除当前流程时自动落到第一条） */
export function removeFlow(id){
  const store = loadStore()
  store.flows = store.flows.filter(f => f.id !== id)
  if(!store.flows.length) store.flows.push(sampleFlow())
  if(!store.flows.some(f => f.id === store.activeId)) store.activeId = store.flows[0].id
  persist(store)
  return store
}

/** 切换当前流程 */
export function setActive(id){
  const store = loadStore()
  if(store.flows.some(f => f.id === id)){
    store.activeId = id
    persist(store)
  }
  return store
}
