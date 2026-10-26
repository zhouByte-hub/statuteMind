<script setup>
/**
 * 工作流首页：展示全部流程的预览卡片（程序化 SVG 缩略图），
 * 点击卡片进入编排界面（/workflow/:id），支持新建与删除。
 */
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { NODE_TYPES } from '@/workflow/registry'
import { loadStore, addFlow, removeFlow, setActive } from '@/workflow/storage'

const router = useRouter()
const store = loadStore()

const flows = computed(() =>
  [...store.flows].sort((a, b) => (b.updatedAt || 0) - (a.updatedAt || 0))
)

function open(id){
  setActive(id)
  router.push({ name: 'workflow-edit', params: { id } })
}

function onAdd(){
  const f = addFlow()
  router.push({ name: 'workflow-edit', params: { id: f.id } })
}

function onDelete(flow, ev){
  ev.stopPropagation()
  removeFlow(flow.id)
}

/** 相对时间 */
function ago(ts){
  if(!ts) return ''
  const d = Date.now() - ts
  if(d < 60e3) return '刚刚'
  if(d < 3600e3) return Math.floor(d / 60e3) + ' 分钟前'
  if(d < 86400e3) return Math.floor(d / 3600e3) + ' 小时前'
  return Math.floor(d / 86400e3) + ' 天前'
}

/** 节点近似尺寸（持久化数据带渲染后的 dimensions，未渲染过用回退值） */
const sizeOf = n => [
  n.dimensions?.width || 236,
  n.dimensions?.height || (n.type === 'ifelse' ? 128 : 90)
]

/** 把流程画成 SVG 缩略图：白色节点卡 + 类型色圆点，连线为水平贝塞尔 */
function preview(flow){
  const ns = flow.nodes
  if(!ns.length) return null
  const boxes = ns.map(n => {
    const [w, h] = sizeOf(n)
    return { n, x: n.position?.x || 0, y: n.position?.y || 0, w, h }
  })
  const PAD = 30
  const minX = Math.min(...boxes.map(b => b.x)) - PAD
  const minY = Math.min(...boxes.map(b => b.y)) - PAD
  const maxX = Math.max(...boxes.map(b => b.x + b.w)) + PAD
  const maxY = Math.max(...boxes.map(b => b.y + b.h)) + PAD
  const W = Math.max(maxX - minX, 120)
  const H = Math.max(maxY - minY, 90)

  const byId = Object.fromEntries(boxes.map(b => [b.n.id, b]))
  const lines = []
  for(const e of flow.edges || []){
    const s = byId[e.source], t = byId[e.target]
    if(!s || !t) continue
    const x1 = s.x + s.w - minX, y1 = s.y + s.h / 2 - minY
    const x2 = t.x - minX, y2 = t.y + t.h / 2 - minY
    const dx = Math.max(40, Math.abs(x2 - x1) / 2)
    lines.push(`M${x1},${y1} C${x1 + dx},${y1} ${x2 - dx},${y2} ${x2},${y2}`)
  }

  const rects = boxes.map(b => {
    const meta = NODE_TYPES[b.n.type]
    const c = meta?.color || '#9AA1AD'
    return {
      x: b.x - minX, y: b.y - minY, w: b.w, h: b.h, c,
      label: (b.n.data?.name || meta?.label || '?').slice(0, 6)
    }
  })
  return { viewBox: `0 0 ${W} ${H}`, lines, rects }
}
</script>

<template>
  <section class="whome" aria-label="工作流首页">

    <div class="grid">
      <article
        v-for="f in flows" :key="f.id"
        class="card" tabindex="0" role="button"
        :aria-label="`打开流程 ${f.name}`"
        @click="open(f.id)" @keydown.enter="open(f.id)"
      >
        <div class="thumb">
          <svg v-if="preview(f)" class="pv" :viewBox="preview(f).viewBox" preserveAspectRatio="xMidYMid meet" aria-hidden="true">
            <defs>
              <filter id="pvShadow" x="-30%" y="-30%" width="160%" height="160%">
                <feDropShadow dx="0" dy="5" stdDeviation="7" flood-color="#24304E" flood-opacity="0.13" />
              </filter>
            </defs>
            <path
              v-for="(d, i) in preview(f).lines" :key="i"
              :d="d" fill="none" stroke="#C6CCDA" stroke-width="7" stroke-linecap="round"
            />
            <g v-for="(r, i) in preview(f).rects" :key="'n' + i" filter="url(#pvShadow)">
              <rect :x="r.x" :y="r.y" :width="r.w" :height="r.h" rx="14" fill="#fff" :stroke="r.c" stroke-width="5" stroke-opacity=".5" />
              <circle :cx="r.x + 32" :cy="r.y + r.h / 2" r="14" :fill="r.c" />
              <text :x="r.x + 56" :y="r.y + r.h / 2 + 11" fill="#2A3346" font-size="28" font-weight="600">{{ r.label }}</text>
            </g>
          </svg>
          <span v-else class="blank">空白流程 · 进去添加节点</span>
          <button class="del" aria-label="删除流程" title="删除流程" @click="onDelete(f, $event)">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"><path d="M6 6l12 12M18 6L6 18"/></svg>
          </button>
        </div>
        <div class="meta">
          <b class="nm">{{ f.name }}</b>
          <span class="sub">{{ f.nodes.length }} 节点 · {{ ago(f.updatedAt) || '未编辑' }}</span>
        </div>
      </article>

      <button class="card addCard" aria-label="新建空白流程" @click="onAdd">
        <i>
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M12 5v14M5 12h14"/></svg>
        </i>
        <b>新建空白流程</b>
        <span>从「开始」节点开始编排</span>
      </button>
    </div>
  </section>
</template>

<style scoped>
.whome{max-width:1120px;margin:0 auto;padding:34px 24px 60px;animation:fadein .25s both}

.hero{display:flex;align-items:flex-end;justify-content:space-between;gap:16px;margin-bottom:22px}
.ht h1{margin:0;font-size:22px;color:var(--ink)}
.ht p{margin:7px 0 0;font-size:13px;color:var(--muted)}
.new{display:inline-flex;align-items:center;gap:6px;flex:none;border:0;border-radius:9px;padding:9px 15px;background:var(--blue);color:#fff;font:600 13px/1 inherit;cursor:pointer;box-shadow:0 3px 10px rgba(61,92,229,.28);transition:filter .15s,transform .15s}
.new:hover{filter:brightness(1.06);transform:translateY(-1px)}

.grid{display:grid;grid-template-columns:repeat(3,1fr);gap:18px}

.card{position:relative;background:#fff;border:1px solid var(--line);border-radius:14px;overflow:hidden;cursor:pointer;box-shadow:var(--shadow);transition:transform .18s,box-shadow .18s,border-color .18s;text-align:left;padding:0;font:inherit}
.card:hover,.card:focus-visible{transform:translateY(-3px);border-color:var(--blue);box-shadow:0 10px 26px rgba(61,92,229,.16);outline:none}

.thumb{position:relative;height:152px;background:linear-gradient(180deg,#FAFBFD,#F2F4F9);display:flex;align-items:center;justify-content:center;overflow:hidden}
.thumb::before{content:'';position:absolute;inset:0;background-image:radial-gradient(#DFE3EC 1.2px,transparent 1.2px);background-size:18px 18px;opacity:.55;mask-image:linear-gradient(180deg,rgba(0,0,0,.9),rgba(0,0,0,.35));-webkit-mask-image:linear-gradient(180deg,rgba(0,0,0,.9),rgba(0,0,0,.35))}
.pv{position:relative;width:100%;height:100%;padding:6px 10px;box-sizing:border-box}
.blank{font-size:12px;color:var(--faint)}
.del{position:absolute;top:9px;right:9px;width:26px;height:26px;border-radius:8px;display:flex;align-items:center;justify-content:center;background:rgba(255,255,255,.94);border:1px solid var(--line);color:var(--faint);cursor:pointer;opacity:0;transition:opacity .15s,color .15s,border-color .15s}
.del svg{width:12px;height:12px}
.card:hover .del,.card:focus-visible .del{opacity:1}
.del:hover{color:var(--red);border-color:var(--red)}

.meta{padding:12px 15px 14px;display:flex;flex-direction:column;gap:4px}
.nm{font-size:14.5px;font-weight:700;color:var(--ink);white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.sub{font-size:12px;color:var(--faint)}

.addCard{display:flex;flex-direction:column;align-items:center;justify-content:center;gap:8px;min-height:210px;border-style:dashed;border-color:var(--line);background:rgba(255,255,255,.6);box-shadow:none;color:var(--muted)}
.addCard i{display:flex;width:44px;height:44px;border-radius:12px;align-items:center;justify-content:center;background:#F0F3FE;color:var(--blue)}
.addCard b{font-size:13.5px;font-weight:600;color:var(--ink2)}
.addCard span{font-size:11.5px;color:var(--faint)}
.addCard:hover{border-color:var(--blue);background:#FAFBFF;transform:translateY(-3px)}

@media (max-width:760px){
  .whome{padding:22px 14px 46px}
  .hero{flex-direction:column;align-items:flex-start;gap:12px}
  .grid{grid-template-columns:1fr;gap:12px}
}
</style>
