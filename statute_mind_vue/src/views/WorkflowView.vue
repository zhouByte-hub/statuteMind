<script setup>
/**
 * 工作流编排视图：类 Dify 画布（Vue Flow）。
 * 左节点库 / 中画布 / 右配置抽屉 / 底运行追踪；localStorage 自动保存，可一键回到内置示例。
 */
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { MarkerType, useVueFlow, VueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/controls/dist/style.css'

import WfNode from '@/components/workflow/WfNode.vue'
import WfEdge from '@/components/workflow/WfEdge.vue'
import WfNodePanel from '@/components/workflow/WfNodePanel.vue'
import WfConfigDrawer from '@/components/workflow/WfConfigDrawer.vue'
import WfRunPanel from '@/components/workflow/WfRunPanel.vue'
import { NODE_TYPES, createNode } from '@/workflow/registry'
import { runFlow } from '@/workflow/executor'
import { loadFlow, resetToSample, saveFlow } from '@/workflow/storage'

const allTypes = Object.keys(NODE_TYPES)

// ---------- 流程数据（首屏：本地保存 > 内置示例） ----------
const initial = loadFlow() || resetToSample()
const flowName = ref(initial.name)
const nodes = ref(initial.nodes)
const edges = ref(initial.edges.map(e => ({
  data: {}, markerEnd: MarkerType.ArrowClosed, type: 'wf', ...e
})))

const canvasWrap = ref(null)
const { fitView, screenToFlowCoordinate } = useVueFlow()

// ---------- 自动保存 ----------
const savedTick = ref(false)
let saveTimer = 0
watch([nodes, edges, flowName], () => {
  clearTimeout(saveTimer)
  saveTimer = setTimeout(() => {
    saveFlow({ name: flowName.value, nodes: nodes.value, edges: edges.value })
    savedTick.value = true
    setTimeout(() => { savedTick.value = false }, 1400)
  }, 600)
}, { deep: true })

// ---------- 画布交互 ----------
const selectedId = ref(null)
const selected = computed(() => nodes.value.find(n => n.id === selectedId.value) || null)

/** 由入边反推上游节点的输出变量（配置抽屉里的「上游变量」chips） */
const upstreamVars = computed(() => {
  if(!selected.value) return []
  const res = []
  for(const e of edges.value){
    if(e.target !== selected.value.id) continue
    const src = nodes.value.find(n => n.id === e.source)
    if(!src) continue
    const meta = NODE_TYPES[src.type]
    if(!meta) continue
    for(const v of meta.outVars){
      res.push({ nodeId: src.id, nodeLabel: src.data.name || meta.label, varName: v.name, varLabel: v.label })
    }
  }
  return res
})

function onNodeClick({ node }){ selectedId.value = node.id }
function onPaneClick(){ selectedId.value = null }

function onConnect(conn){
  if(!conn.source || !conn.target || conn.source === conn.target) return
  const dup = edges.value.some(e =>
    e.source === conn.source && e.target === conn.target
    && (e.sourceHandle ?? null) === (conn.sourceHandle ?? null)
  )
  if(dup) return
  edges.value.push({
    id: `e_${Date.now().toString(36)}${Math.random().toString(36).slice(2, 5)}`,
    source: conn.source, target: conn.target,
    sourceHandle: conn.sourceHandle ?? undefined,
    targetHandle: conn.targetHandle ?? undefined,
    type: 'wf', markerEnd: MarkerType.ArrowClosed, data: {}
  })
}

function onDrop(e){
  const type = e.dataTransfer?.getData('application/wf-type')
  if(!type || !NODE_TYPES[type]) return
  const position = screenToFlowCoordinate({ x: e.clientX, y: e.clientY })
  addNodeAt(type, position)
}

function onAddFromPanel(type){
  const r = canvasWrap.value?.getBoundingClientRect()
  const jitter = () => (Math.random() - 0.5) * 60
  const position = screenToFlowCoordinate({
    x: (r?.left || 0) + (r?.width || 800) / 2 + jitter(),
    y: (r?.top || 0) + (r?.height || 500) / 2 + jitter()
  })
  addNodeAt(type, position)
  selectedId.value = null
}

function addNodeAt(type, position){
  const n = createNode(type, position)
  if(n) nodes.value.push(n)
}

function loadSample(){
  const f = resetToSample()
  flowName.value = f.name
  nodes.value = f.nodes
  edges.value = f.edges.map(e => ({ data: {}, markerEnd: MarkerType.ArrowClosed, type: 'wf', ...e }))
  selectedId.value = null
  nextTick(() => fitView({ padding: 0.18, maxZoom: 1.1 }))
}

function clearCanvas(){
  nodes.value = []
  edges.value = []
  selectedId.value = null
}

// ---------- 运行 ----------
const showRun = ref(false)
const running = ref(false)
const runLogs = ref([])
const runAnswer = ref('')
const runMs = ref(0)
const runErr = ref('')
const runInput = reactive({})
let abortCtl = null

const startNode = computed(() => nodes.value.find(n => n.type === 'start'))
const inputFields = computed(() => startNode.value?.data.fields || [])

function openRun(){
  for(const f of inputFields.value){
    if(!(f.name in runInput)) runInput[f.name] = ''
  }
  runErr.value = ''
  showRun.value = true
}

async function doRun(){
  if(!startNode.value){
    runErr.value = '画布缺少「开始」节点，请从左侧节点库添加后再运行。'
    return
  }
  abortCtl?.abort()
  abortCtl = new AbortController()
  for(const n of nodes.value){ n.data.status = 'idle'; n.data.result = '' }
  for(const e of edges.value){ e.data = { ...(e.data || {}), active: false, done: false } }
  runLogs.value = []
  runAnswer.value = ''
  runMs.value = 0
  runErr.value = ''
  running.value = true
  showRun.value = true

  const finished = runFlow(
    { name: flowName.value, nodes: nodes.value, edges: edges.value, input: { ...runInput } },
    {
      onEdgeState: (id, s) => {
        const e = edges.value.find(x => x.id === id)
        if(e) e.data = { ...(e.data || {}), active: s === 'active', done: s === 'done' }
      },
      onToken: t => { runAnswer.value += t }
    },
    abortCtl.signal
  )
  try{
    const r = await finished
    runLogs.value = r.logs
    runMs.value = r.ms
    if(r.answer) runAnswer.value = r.answer
  }catch(err){
    if(err?.name !== 'AbortError') runErr.value = err?.message || '运行出错'
  }finally{
    running.value = false
  }
}

function stopRun(){ abortCtl?.abort() }

onMounted(() => {
  nextTick(() => fitView({ padding: 0.18, maxZoom: 1.1 }))
})
</script>

<template>
  <section class="vflow" aria-label="工作流编排">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <div class="left">
        <svg class="ticon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 4v6a4 4 0 0 0 4 4h4a4 4 0 0 0 4-4V4M12 14v6M9 17l3 3 3-3"/></svg>
        <input v-model="flowName" class="fname" aria-label="流程名称" placeholder="未命名流程">
        <transition name="fade"><span v-if="savedTick" class="saved">已自动保存</span></transition>
      </div>
      <div class="right">
        <button class="mini" @click="loadSample">加载示例</button>
        <button class="mini" @click="clearCanvas">清空画布</button>
        <button class="mini pri run" @click="openRun">
          <svg width="11" height="11" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M7 4.5 19.5 12 7 19.5v-15Z"/></svg>
          运行
        </button>
      </div>
    </div>

    <div ref="canvasWrap" class="canvasWrap">
      <WfNodePanel @add="onAddFromPanel" />

      <div class="flowArea" @dragover.prevent @drop="onDrop">
        <VueFlow
          v-model:nodes="nodes"
          v-model:edges="edges"
          class="cflow"
          delete-key-code="Delete"
          :min-zoom="0.15"
          :max-zoom="2.2"
          :connection-radius="32"
          @connect="onConnect"
          @node-click="onNodeClick"
          @pane-click="onPaneClick"
        >
          <Background :gap="22" pattern-color="#D7DBE5" />
          <Controls position="bottom-left" />

          <template v-for="t in allTypes" :key="t" #[`node-${t}`]="p">
            <WfNode v-bind="p" />
          </template>
          <template #edge-wf="p">
            <WfEdge v-bind="p" />
          </template>
        </VueFlow>

        <!-- 空画布引导 -->
        <div v-if="!nodes.length" class="emptyGuide">
          <b>画布还是空的</b>
          <p>从左侧节点库拖入节点开始编排，或一键加载内置示例「欠薪维权助手」</p>
          <button class="mini pri" @click="loadSample">加载示例工作流</button>
        </div>

        <!-- 移动端提示 -->
        <p class="mobileTip">编排建议在桌面端进行，此处可预览与运行</p>
      </div>

      <WfConfigDrawer
        v-if="selected"
        :node="selected"
        :upstream="upstreamVars"
        @close="selectedId = null"
      />

      <WfRunPanel
        :open="showRun" :running="running" :error="runErr"
        :ms="runMs" :answer="runAnswer" :fields="inputFields"
        :input="runInput" :logs="runLogs"
        @close="showRun = false" @run="doRun" @stop="stopRun"
      />
    </div>
  </section>
</template>

<style scoped>
.vflow{display:flex;flex-direction:column;height:calc(100dvh - 60px);min-height:520px;animation:fadein .25s both}

/* 工具栏 */
.toolbar{flex:none;display:flex;align-items:center;justify-content:space-between;gap:12px;height:52px;padding:0 16px;background:#fff;border-bottom:1px solid var(--line)}
.left{display:flex;align-items:center;gap:9px;min-width:0}
.ticon{flex:none;color:var(--blue)}
.fname{border:1px solid transparent;border-radius:8px;padding:5px 9px;font:700 14.5px/1.4 inherit;color:var(--ink);background:none;min-width:0;width:280px;transition:border-color .15s,background .15s}
.fname:hover{background:var(--gray)}
.fname:focus{outline:none;border-color:var(--blue);background:#fff}
.saved{font-size:11.5px;color:#1E7B4F}
.right{display:flex;align-items:center;gap:9px;flex:none}
.run svg{margin-right:2px}

/* 画布区 */
.canvasWrap{flex:1;display:flex;min-height:0;position:relative;background:#F7F8FA}
.flowArea{flex:1;position:relative;min-width:0}
.cflow{width:100%;height:100%}
.cflow :deep(.vue-flow__pane){cursor:default}
.cflow :deep(.vue-flow__handle){width:9px;height:9px;background:#AEB5C2;border:2px solid #fff}
.cflow :deep(.vue-flow__handle:hover){background:var(--blue)}
.cflow :deep(.vue-flow__node){cursor:grab}
.cflow :deep(.vue-flow__node.dragging){cursor:grabbing}
.cflow :deep(.vue-flow__edge.selected .wf-edge){stroke:var(--blue)}
.cflow :deep(.vue-flow__controls){box-shadow:var(--shadow);border:1px solid var(--line);border-radius:10px;overflow:hidden}
.cflow :deep(.vue-flow__controls-button){background:#fff;border-bottom:1px solid var(--line2);color:var(--ink2)}
.cflow :deep(.vue-flow__controls-button:hover){background:#F5F7FF}
.cflow :deep(.vue-flow__controls-button svg){fill:currentColor}

/* 空画布引导 */
.emptyGuide{position:absolute;inset:0;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:8px;text-align:center;pointer-events:none}
.emptyGuide b{font-size:15.5px;color:var(--ink)}
.emptyGuide p{font-size:13px;color:var(--muted);max-width:340px;line-height:1.8}
.emptyGuide .mini{pointer-events:auto}

/* 移动端 */
.mobileTip{display:none;position:absolute;top:10px;left:50%;transform:translateX(-50%);font-size:11px;color:var(--faint);background:rgba(255,255,255,.9);border:1px solid var(--line);border-radius:999px;padding:3px 12px;pointer-events:none;white-space:nowrap}
.fade-enter-active,.fade-leave-active{transition:opacity .4s}
.fade-enter-from,.fade-leave-to{opacity:0}

@media (max-width:760px){
  .mobileTip{display:block}
  .fname{width:130px;font-size:13px}
  .canvasWrap > aside:first-child{display:none}
  .toolbar{padding:0 10px;gap:8px}
}
</style>
