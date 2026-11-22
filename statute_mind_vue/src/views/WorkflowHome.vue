<script setup>
/**
 * 工作流首页：左侧行式流程列表（右上角新建按钮），右侧动态与推理观测。
 * 不设页内标题与分类筛选（顶部导航已表明页面语境）；右栏只放时间维度的
 * 动态事件与运行指标，与行内静态信息零重复。≤1200px 隐藏右栏，≤760px 行简化。
 */
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { NODE_TYPES } from '@/workflow/registry'
import { loadStore, addFlow, removeFlow, setActive } from '@/workflow/storage'
import { monitor } from '@/monitor/metrics'

const router = useRouter()
const store = loadStore()

/** 行式列表：按最近编辑倒序 */
const flows = computed(() => [...store.flows].sort((a, b) => (b.updatedAt || 0) - (a.updatedAt || 0)))

/** 右栏最近动态：全部流程的编辑事件（运行明细单独进「运行记录」卡，避免同源重复） */
const events = computed(() => store.flows
  .map(f => ({ name: f.name, at: f.updatedAt || 0 }))
  .filter(e => e.at)
  .sort((a, b) => b.at - a.at)
  .slice(0, 8))

/** 右栏运行记录：工作流执行明细（状态/耗时/时间，最新在前） */
const runs = computed(() => monitor.tasks
  .filter(t => t.type === 'workflow')
  .slice(0, 8))

/** 右栏模型推理实测指标（与运行监控页同源：monitor.llm 滑动窗口） */
const llmStats = computed(() => {
  const L = monitor.llm
  if(!L.calls) return null
  const avg = a => Math.round(a.reduce((s, v) => s + v, 0) / a.length)
  return { calls: L.calls, ttft: avg(L.ttft), speed: avg(L.speed), outLen: avg(L.outLen) }
})

/** 行内链路摘要：前 5 个节点名用 → 连接 */
function chain(f){
  const names = f.nodes.map(n => n.data?.name || NODE_TYPES[n.type]?.label || '?')
  return names.slice(0, 5).join(' → ') + (names.length > 5 ? ' …' : '')
}

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

/** 绝对日期：创建时间用 */
function fmtDate(ts){
  if(!ts) return '—'
  const d = new Date(ts)
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
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
  <section class="whome" aria-label="工作流">

    <div class="main">
      <div class="mnew">
        <button class="new" @click="onAdd">
          <i class="ic"><svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" aria-hidden="true"><path d="M12 5v14M5 12h14"/></svg></i>
          新建流程
        </button>
      </div>
      <div class="rows">
        <div v-if="!flows.length" class="empty">
          还没有流程，点右上「新建流程」开始编排
        </div>
        <article
          v-for="f in flows" :key="f.id"
          class="row" tabindex="0" role="button"
          :aria-label="`打开流程 ${f.name}`"
          @click="open(f.id)" @keydown.enter="open(f.id)"
        >
          <div class="rthumb" aria-hidden="true">
            <svg v-if="preview(f)" class="pv" :viewBox="preview(f).viewBox" preserveAspectRatio="xMidYMid meet">
              <path
                v-for="(d, i) in preview(f).lines" :key="i"
                :d="d" fill="none" stroke="#C6CCDA" stroke-width="7" stroke-linecap="round"
              />
              <g v-for="(r, i) in preview(f).rects" :key="'n' + i">
                <rect :x="r.x" :y="r.y" :width="r.w" :height="r.h" rx="14" fill="#fff" :stroke="r.c" stroke-width="5" stroke-opacity=".55" />
                <circle :cx="r.x + 32" :cy="r.y + r.h / 2" r="14" :fill="r.c" />
                <text :x="r.x + 56" :y="r.y + r.h / 2 + 11" fill="#2A3346" font-size="28" font-weight="600">{{ r.label }}</text>
              </g>
            </svg>
            <span v-else class="blank">空白流程</span>
          </div>
          <div class="rmain">
            <b class="rname">{{ f.name }}</b>
            <span class="rchain">{{ chain(f) || '尚无节点，进入编排添加' }}</span>
          </div>
          <div class="rmeta">
            <span class="num-tab">{{ f.nodes.length }} 节点 · {{ f.edges.length }} 连线 · 编辑于{{ ago(f.updatedAt) || '未知' }}</span>
            <span class="num-tab">创建 {{ fmtDate(f.createdAt) }} · 上次运行 {{ f.lastRunAt ? ago(f.lastRunAt) : '未运行' }}</span>
          </div>
          <button class="del" aria-label="删除流程" title="删除流程" @click="onDelete(f, $event)">
            <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"><path d="M6 6l12 12M18 6L6 18"/></svg>
          </button>
          <svg class="chev" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><path d="M9 6l6 6-6 6"/></svg>
        </article>
      </div>
    </div>

    <!-- 右栏：顶部独立新建按钮 + 时间维度的动态与运行观测（与行内静态信息不重复） -->
    <aside class="panel" aria-label="工作流动态与运行观测">
      <div class="pnew">
        <button class="new" @click="onAdd">
          <i class="ic"><svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" aria-hidden="true"><path d="M12 5v14M5 12h14"/></svg></i>
          新建流程
        </button>
      </div>
      <div class="panelIn">
        <p class="psec">最近动态</p>
        <div v-if="events.length" class="evtl">
          <div v-for="(e, i) in events" :key="i" class="evrow">
            <i class="edit"></i>
            <div class="evtxt">
              <b>编辑了「{{ e.name }}」</b>
              <span class="num-tab">{{ ago(e.at) }}</span>
            </div>
          </div>
        </div>
        <p v-else class="ecap">暂无动态，编辑流程后显示</p>

        <p class="psec">运行记录</p>
        <div v-if="runs.length" class="runrows">
          <div v-for="t in runs" :key="t.id" class="runrow">
            <i :class="t.status"></i>
            <div class="rinfo">
              <b>{{ t.label }}</b>
              <span class="num-tab">{{ ago(t.at) }} · {{ t.status === 'success' ? '成功' : t.status === 'failed' ? '失败' : '跳过' }}</span>
            </div>
            <em class="num-tab">{{ t.ms }} ms</em>
          </div>
        </div>
        <p v-else class="ecap">进入编排页运行流程后，这里记录每次执行的耗时与状态</p>

        <p class="psec">模型推理</p>
        <template v-if="llmStats">
          <div class="mstats">
            <div><b class="num-tab">{{ llmStats.ttft }}</b><span>ms 首字</span></div>
            <div><b class="num-tab">{{ llmStats.speed }}</b><span>字/秒</span></div>
            <div><b class="num-tab">{{ llmStats.outLen }}</b><span>字输出</span></div>
          </div>
          <p class="ecap num-tab">近 {{ llmStats.calls }} 次调用实测</p>
        </template>
        <p v-else class="ecap">运行含大模型节点的流程后，这里显示实测指标</p>

        <RouterLink class="mlink" :to="{ name: 'monitor' }">查看运行监控 →</RouterLink>
      </div>
    </aside>
  </section>
</template>

<style scoped>
/* 两列与知识库页同宽：主列 minmax(460,760) + 右栏 320，整组居中 */
.whome{width:100%;padding:34px 20px 60px;display:grid;grid-template-columns:minmax(460px,760px) 320px;column-gap:24px;justify-content:center;animation:fadein .25s both}
.whome>.main{grid-column:1;min-width:0}
.whome>.panel{grid-column:2;min-width:0}
.mnew{display:none;justify-content:flex-end;margin-bottom:12px}

/* 主按钮：右栏满宽、渐变底 + 声呐脉冲光环；hover 浮起 + 渐变位移 + 图标旋转 */
.new{position:relative;display:flex;align-items:center;justify-content:center;gap:8px;width:100%;border:0;border-radius:11px;padding:12px 16px;background:linear-gradient(120deg,#3D5CE5,#5B82F5 55%,#6E9BFF);background-size:160% 100%;background-position:0 0;color:#fff;font:700 13.5px/1 'Noto Sans SC',sans-serif;letter-spacing:.02em;cursor:pointer;box-shadow:0 6px 16px rgba(61,92,229,.32),inset 0 1px 0 rgba(255,255,255,.22);transition:background-position .35s var(--ease),transform .18s,box-shadow .18s}
.new:hover{background-position:100% 0;transform:translateY(-2px);box-shadow:0 10px 22px rgba(61,92,229,.4),inset 0 1px 0 rgba(255,255,255,.25)}
.new:active{transform:translateY(0) scale(.985);box-shadow:0 4px 12px rgba(61,92,229,.3)}
.new:focus-visible{outline:2px solid var(--blue);outline-offset:2px}
.ic{display:flex;width:20px;height:20px;border-radius:99px;background:rgba(255,255,255,.24);align-items:center;justify-content:center;transition:transform .3s var(--ease)}
.new:hover .ic{transform:rotate(90deg) scale(1.06)}
.mnew .new{width:auto}
@keyframes sonar{
  0%{box-shadow:0 6px 16px rgba(61,92,229,.32),0 0 0 0 rgba(61,92,229,.30)}
  70%{box-shadow:0 6px 16px rgba(61,92,229,.32),0 0 0 9px rgba(61,92,229,0)}
  100%{box-shadow:0 6px 16px rgba(61,92,229,.32),0 0 0 0 rgba(61,92,229,0)}
}
@media (prefers-reduced-motion:no-preference){
  .pnew{animation:rise .5s var(--ease) both}
  .pnew .new{animation:sonar 3s var(--ease) infinite}
}

.rows{display:flex;flex-direction:column;gap:10px}
.empty{border:1.5px dashed var(--line);border-radius:14px;padding:44px 20px;text-align:center;font-size:13px;color:var(--muted)}

.row{position:relative;display:flex;align-items:center;gap:16px;background:#fff;border:1px solid var(--line);border-radius:12px;padding:12px 14px 12px 12px;cursor:pointer;transition:border-color .15s,box-shadow .15s,transform .15s;font:inherit;text-align:left;width:100%}
.row:hover,.row:focus-visible{border-color:var(--blue);box-shadow:0 6px 18px rgba(61,92,229,.10);transform:translateY(-1px);outline:none}
.rthumb{flex:none;width:138px;height:78px;border-radius:8px;background:linear-gradient(180deg,#FAFBFD,#F2F4F9);border:1px solid var(--line);overflow:hidden;display:flex;align-items:center;justify-content:center}
.rthumb .pv{width:100%;height:100%;padding:3px 5px;box-sizing:border-box}
.blank{font-size:11px;color:var(--faint)}
.rmain{flex:1;min-width:0;display:flex;flex-direction:column;gap:5px}
.rname{font-size:14.5px;font-weight:700;color:var(--ink);overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.rchain{font-size:12px;color:var(--muted);overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.rmeta{flex:none;display:flex;flex-direction:column;gap:3px;align-items:flex-end}
.rmeta span{font-size:11.5px;color:var(--faint);white-space:nowrap}
.chev{flex:none;width:16px;height:16px;color:var(--faint)}
.del{position:absolute;top:8px;right:30px;width:24px;height:24px;border-radius:7px;display:flex;align-items:center;justify-content:center;background:rgba(255,255,255,.95);border:1px solid var(--line);color:var(--faint);cursor:pointer;opacity:0;transition:opacity .15s,color .15s,border-color .15s}
.row:hover .del,.row:focus-visible .del{opacity:1}
.del:hover{color:var(--red);border-color:var(--red)}

/* 右侧观测栏：顶部独立新建按钮（不与面板内容同卡），下面是观测面板 */
.panel{min-width:0}
.pnew{display:flex;justify-content:flex-end;margin-bottom:12px}
.panelIn{position:sticky;top:84px;background:#fff;border:1px solid var(--line);border-radius:14px;padding:16px 18px 18px;animation:rise .5s var(--ease) both}
.psec{display:flex;align-items:center;gap:8px;margin:0 0 8px;font-size:12px;font-weight:700;color:var(--faint);letter-spacing:.04em}
.psec::before{content:"";width:4px;height:12px;border-radius:2px;background:var(--blue);flex:none}
.psec:not(:first-child){margin-top:16px}
.evtl{position:relative;padding-left:18px}
.evtl::before{content:"";position:absolute;left:5px;top:8px;bottom:8px;width:2px;background:var(--line)}
.evrow{position:relative;display:flex;gap:8px;padding:4px 0}
.evrow>i{position:absolute;left:-18px;top:50%;transform:translateY(-50%);width:8px;height:8px;border-radius:99px;border:2px solid #fff;box-sizing:content-box;background:var(--blue)}
.evrow>i.success{background:#2E9E6B}
.evrow>i.failed{background:var(--red)}
.evrow>i.skipped{background:#9AA1AD}
.evtxt b{font-size:12.5px;font-weight:500;color:var(--ink2);overflow:hidden;text-overflow:ellipsis;white-space:nowrap;display:inline-block;max-width:170px;vertical-align:bottom}
.evtxt span{margin-left:6px;font-size:11px;color:var(--faint)}
.runrows{display:flex;flex-direction:column}
.runrow{display:flex;align-items:center;gap:8px;padding:5px 0}
.runrow>i{flex:none;width:8px;height:8px;border-radius:99px;background:#2E9E6B}
.runrow>i.failed{background:var(--red)}
.runrow>i.skipped{background:#9AA1AD}
.rinfo{flex:1;min-width:0}
.rinfo b{display:block;font-size:12.5px;font-weight:600;color:var(--ink2);overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.rinfo span{display:block;margin-top:1px;font-size:11px;color:var(--faint)}
.runrow>em{flex:none;font-style:normal;font-size:11.5px;color:var(--muted)}
.runrow>em.err{color:var(--red)}
.mstats{display:flex;gap:8px}
.mstats>div{flex:1;background:#F7F8FB;border-radius:10px;padding:8px 4px;text-align:center}
.mstats b{display:block;font:800 15px/1.2 'Noto Sans SC',sans-serif;color:var(--ink)}
.mstats span{display:block;margin-top:2px;font-size:10.5px;color:var(--muted)}
.ecap{margin:6px 0 0;font-size:11.5px;line-height:1.7;color:var(--faint)}
.mlink{display:inline-block;margin-top:12px;font-size:12.5px;font-weight:600;color:var(--blue);text-decoration:none}
.mlink:hover{text-decoration:underline}

/* ≤1200px：右栏隐藏，单列；≤760px 行式列表再简化 */
@media (max-width:1200px){
  .whome{display:block;max-width:1120px}
  .panel{display:none}
  .mnew{display:flex}
}
@media (max-width:760px){
  .whome{padding:22px 14px 46px}
  .row{gap:12px}
  .rthumb,.rmeta,.chev{display:none}
}
</style>
