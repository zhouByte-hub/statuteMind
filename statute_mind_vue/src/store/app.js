import { reactive } from 'vue'
import { UPLOADED } from '@/data/mock'

let seq = 0

const LS_HIST = 'statuteMind.history'
const LS_FAV = 'statuteMind.favs'

/** localStorage 读取（损坏或不存在时回落默认值） */
function loadLS(key, dflt){
  try{
    const v = JSON.parse(localStorage.getItem(key))
    return Array.isArray(v) ? v : dflt
  }catch{ return dflt }
}
const saveLS = (key, val) => { try{ localStorage.setItem(key, JSON.stringify(val)) }catch{ /* 隐私模式等场景静默降级 */ } }

/** 轻量全局状态：跨视图共享的检索记录、徽标计数、导入条目与回放信号 */
export const store = reactive({
  /** 检索历史（最多 20 条，localStorage 持久化，新在前） */
  history: loadLS(LS_HIST, []),
  /** 收藏的法条 id（localStorage 持久化，新在前） */
  favs: loadLS(LS_FAV, []),
  /** 顶栏徽标展示的内置法条数 */
  statuteCount: 1286,
  /** 模拟导入的条目 */
  uploaded: UPLOADED,
  /** 知识库当前分类筛选 */
  catFilter: '全部',
  /** 跨视图回放信号：同参重复跳转时通知目标视图重放检索/判断/定位 */
  replay: null,

  /** 记录一次检索（去重置顶，最多 20 条） */
  addHistory(q){
    this.history = [{ q, at: Date.now() }, ...this.history.filter(h => h.q !== q)].slice(0, 20)
    saveLS(LS_HIST, this.history)
  },
  removeHistory(q){
    this.history = this.history.filter(h => h.q !== q)
    saveLS(LS_HIST, this.history)
  },
  clearHistory(){
    this.history = []
    saveLS(LS_HIST, this.history)
  },
  /** 收藏/取消收藏法条，返回是否已收藏 */
  toggleFav(id){
    const on = !this.favs.includes(id)
    this.favs = on ? [id, ...this.favs] : this.favs.filter(f => f !== id)
    saveLS(LS_FAV, this.favs)
    return on
  },
  isFav(id){ return this.favs.includes(id) },
  applyUpload(){
    this.statuteCount += 2
  },
  setFilter(c){
    this.catFilter = c
  },
  requestReplay(type, id){
    this.replay = { type, id, seq: ++seq }
  }
})
