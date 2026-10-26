/** 工作流本地持久化：localStorage（key 带版本号，仅存可序列化纯数据） */
import { makeSampleFlow } from './sampleFlow'
import { syncSeq } from './registry'

const KEY = 'statuteMind.wf.v1'

/** 运行期瞬态字段（不入持久化：刷新后不应残留上次运行的徽标） */
const TRANSIENT = ['status', 'result']

function stripRunState(nodes){
  return nodes.map(n => ({ ...n, data: Object.fromEntries(
    Object.entries(n.data || {}).filter(([k]) => !TRANSIENT.includes(k))
  ) }))
}

/** 读取本地保存的流程；无有效数据时返回 null */
export function loadFlow(){
  try{
    const raw = localStorage.getItem(KEY)
    if(!raw) return null
    const flow = JSON.parse(raw)
    if(flow && Array.isArray(flow.nodes) && flow.nodes.length){
      flow.nodes = stripRunState(flow.nodes)
      syncSeq(flow.nodes)
      return flow
    }
  }catch{ /* 数据损坏时回退到示例 */ }
  return null
}

/** 保存流程（画布变更后自动调用） */
export function saveFlow(flow){
  try{
    localStorage.setItem(KEY, JSON.stringify({ name: flow.name, nodes: stripRunState(flow.nodes), edges: flow.edges }))
  }catch{ /* 隐私模式等场景下静默失败 */ }
}

/** 清空本地流程并回到内置示例 */
export function resetToSample(){
  try{ localStorage.removeItem(KEY) }catch{ /* 同上 */ }
  const flow = makeSampleFlow()
  syncSeq(flow.nodes)
  return flow
}
