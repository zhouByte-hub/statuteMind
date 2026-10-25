import { reactive } from 'vue'
import { UPLOADED } from '@/data/mock'

let seq = 0

/** 轻量全局状态：跨视图共享的检索记录、徽标计数、导入条目与回放信号 */
export const store = reactive({
  /** 最近检索（最多 4 条） */
  recent: [],
  /** 顶栏徽标展示的内置法条数 */
  statuteCount: 1286,
  /** 模拟导入的条目 */
  uploaded: UPLOADED,
  /** 知识库当前分类筛选 */
  catFilter: '全部',
  /** 跨视图回放信号：同参重复跳转时通知目标视图重放检索/判断/定位 */
  replay: null,

  addRecent(q){
    this.recent = [q, ...this.recent.filter(r => r !== q)].slice(0, 4)
  },
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
