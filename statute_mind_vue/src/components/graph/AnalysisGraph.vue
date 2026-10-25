<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { GRAPH_COLS, GRAPH_EDGES, buildNodes } from '@/utils/graph'

const props = defineProps({
  judgment: { type: Object, required: true },
  selected: { type: String, default: '' }
})
defineEmits(['select'])

const canvasEl = ref(null)
const svgEl = ref(null)
const nodeEls = {}
const edges = ref([]) // [{ d, delay }]
const vb = ref('0 0 0 0')

const nodes = computed(() => buildNodes(props.judgment))
const node = id => nodes.value[id]

/** 量测节点锚点，生成 SVG 贝塞尔连线（列间水平距离过小时跳过） */
function draw(){
  const canvas = canvasEl.value, svg = svgEl.value
  if(!canvas || !svg) return
  const w = canvas.offsetWidth, h = canvas.offsetHeight
  if(!w || !h){ edges.value = []; return }
  vb.value = `0 0 ${w} ${h}`
  const cr = canvas.getBoundingClientRect()
  const pt = (el, side) => {
    const r = el.getBoundingClientRect()
    return { x: (side === 'r' ? r.right : r.left) - cr.left, y: r.top + r.height / 2 - cr.top }
  }
  const list = []
  GRAPH_EDGES.forEach(([a, b], i) => {
    const ea = nodeEls[a], eb = nodeEls[b]
    if(!ea || !eb) return
    const p1 = pt(ea, 'r'), p2 = pt(eb, 'l')
    if(p2.x - p1.x < 8) return
    const dx = Math.max(28, (p2.x - p1.x) / 2)
    list.push({
      d: `M${p1.x},${p1.y} C${p1.x + dx},${p1.y} ${p2.x - dx},${p2.y} ${p2.x - 4},${p2.y}`,
      delay: .12 + i * .09
    })
  })
  edges.value = list
}

const queueDraw = () => nextTick(() => requestAnimationFrame(draw))

let rsz = null
const onResize = () => { clearTimeout(rsz); rsz = setTimeout(draw, 120) }

onMounted(() => {
  queueDraw()
  window.addEventListener('resize', onResize)
  if(document.fonts?.ready) document.fonts.ready.then(() => { if(canvasEl.value?.isConnected) draw() })
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  clearTimeout(rsz)
})

// 判定切换：:key 重建子树重放入场动画后重算连线
watch(() => props.judgment, queueDraw)
</script>

<template>
  <div class="gwrap">
    <div :key="judgment.q" ref="canvasEl" class="gcanvas">
      <svg ref="svgEl" class="gedges" aria-hidden="true" :viewBox="vb">
        <defs>
          <marker id="garr" viewBox="0 0 8 8" refX="6.5" refY="4" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
            <path d="M0,0L8,4L0,8z" fill="#3D5CE5" opacity=".5"/>
          </marker>
        </defs>
        <path
          v-for="(e, i) in edges" :key="i"
          :d="e.d" pathLength="1" marker-end="url(#garr)"
          :style="{ '--d': e.delay + 's' }"
        />
      </svg>
      <div class="gcols">
        <div v-for="(col, ci) in GRAPH_COLS" :key="ci" class="gcol">
          <button
            v-for="id in col" :key="id"
            :ref="el => (nodeEls[id] = el)"
            class="gnode"
            :class="{ root: node(id).root, sel: selected === id }"
            :style="{ '--i': ci }"
            :aria-label="`查看${node(id).tag}节点：${node(id).title}`"
            @click="$emit('select', id)"
          >
            <span class="gtag" :class="node(id).cls">{{ node(id).tag }}</span>
            <b class="gtitle">{{ node(id).title }}</b>
            <span class="gsub">{{ node(id).sub }}</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.gwrap{flex:1;overflow-x:auto;padding:10px 28px 6px}
.gcanvas{position:relative;min-width:940px;padding:14px 0}
.gedges{position:absolute;inset:0;width:100%;height:100%}
.gedges path{fill:none;stroke:var(--blue);stroke-opacity:.45;stroke-width:2;stroke-dasharray:1;stroke-dashoffset:1;animation:edgedraw .55s var(--ease) forwards;animation-delay:var(--d,0s)}
.gcols{position:relative;display:flex;align-items:center;gap:44px;min-height:420px}
.gcol{display:flex;flex-direction:column;gap:40px;justify-content:center}
.gnode{flex:none;width:200px;text-align:left;background:#fff;border:1px solid var(--line);border-radius:12px;box-shadow:var(--shadow);padding:13px 15px;animation:rise .5s var(--ease) both;animation-delay:calc(var(--i)*120ms);transition:border-color .2s,box-shadow .2s,transform .15s var(--ease)}
.gnode:hover{border-color:#B9C4F2;transform:translateY(-1px)}
.gnode.sel{border-color:var(--blue);box-shadow:0 0 0 3px rgba(61,92,229,.13)}
.gtag{display:inline-block;font-size:10.5px;font-weight:700;letter-spacing:.06em;padding:1.5px 8px;border-radius:4px;background:#EEF1FE;color:#3B55C9}
.gtag.t-act{background:rgba(255,255,255,.22);color:#fff}
.gtag.t-law{background:#EEF1FE;color:#2440B1}
.gtag.t-pun{background:#FDF0E6;color:var(--amber)}
.gtag.t-risk{background:#FCEDEA;color:#A8332A}
.gtitle{display:block;margin-top:8px;font-size:14px;font-weight:800;line-height:1.5;color:var(--ink)}
.gsub{display:-webkit-box;-webkit-line-clamp:2;line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;margin-top:5px;font-size:12px;line-height:1.7;color:var(--muted)}
.gnode.root{background:var(--blue);border-color:var(--blue)}
.gnode.root .gtitle{color:#fff}
.gnode.root .gsub{color:rgba(255,255,255,.85)}

@media (max-width:960px){
  .gwrap{overflow-x:visible;padding:14px 16px 0}
  .gcanvas{min-width:0}
  .gedges{display:none}
  .gcols{flex-direction:column;align-items:stretch;gap:14px;min-height:0}
  .gcol{flex-direction:column;gap:14px}
  .gnode{width:auto}
}
</style>
