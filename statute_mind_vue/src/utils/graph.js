/** 分析图谱：四列节点（行为 → 定性 → 主/关联条文 → 处罚&风险）与连线拓扑 */
export const GRAPH_COLS = [['act'], ['def'], ['s0', 's1'], ['pun', 'risk']]
export const GRAPH_EDGES = [
  ['act', 'def'], ['def', 's0'], ['def', 's1'],
  ['s0', 'pun'], ['s1', 'pun'], ['pun', 'risk']
]

/** 由判断数据（cps 四段 card）推导图谱节点 */
export function buildNodes(J){
  const c = (i, k) => (J.cps[i].card.find(r => r[0] === k) || ['', ''])[1]
  const sec = J.cps[1].card.find(r => ['配套条文', '民事层面', '刑事衔接'].includes(r[0]))
  return {
    act:  { tag: '行为', cls: 't-act',  title: J.name, sub: c(0, '行为名称'), root: true, rows: J.cps[0].card },
    def:  { tag: '定性', cls: '',       title: c(0, '定性结论'), sub: J.cps[0].d, rows: J.cps[0].card },
    s0:   { tag: '主条文', cls: 't-law', title: c(1, '法律依据'), sub: c(1, '条文摘引'), rows: J.cps[1].card, lib: J.lib, q: J.q },
    s1:   { tag: sec ? sec[0] : '关联条文', cls: 't-law', title: sec ? sec[1] : '', sub: sec ? sec[0] : '', rows: J.cps[1].card, lib: J.lib2, q: J.q },
    pun:  { tag: '处罚', cls: 't-pun',  title: '处罚与责任', sub: J.cps[2].d, rows: J.cps[2].card },
    risk: { tag: '风险', cls: 't-risk', title: '风险提示', sub: J.cps[3].d, rows: J.cps[3].card }
  }
}
