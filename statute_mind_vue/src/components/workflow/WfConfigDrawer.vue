<script setup>
/**
 * 右侧配置抽屉：节点改名 + 按 type 的专属表单 + 上游变量引用（点击复制）。
 * 直接修改选中节点的 data（响应式对象），关闭即生效。
 */
import { computed, ref } from 'vue'
import { NODE_TYPES } from '@/workflow/registry'

const props = defineProps({
  /** 选中的画布节点（响应式对象） */
  node: { type: Object, required: true },
  /** 上游变量：[{ nodeId, nodeLabel, varName, varLabel }] */
  upstream: { type: Array, default: () => [] }
})
const emit = defineEmits(['close', 'remove'])

const meta = computed(() => NODE_TYPES[props.node.type] || {})
const d = computed(() => props.node.data)

const copied = ref('')
async function copyRef(u){
  const ref = `{{#${u.nodeId}.${u.varName}#}}`
  try{ await navigator.clipboard.writeText(ref) }catch{
    const ta = document.createElement('textarea')
    ta.value = ref
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    ta.remove()
  }
  copied.value = ref
  setTimeout(() => { if(copied.value === ref) copied.value = '' }, 1600)
}

function addField(){ d.value.fields.push({ name: `field_${d.value.fields.length + 1}`, label: '新字段' }) }
function addClass(){ d.value.classes.push('新分类') }
function addCase(){
  d.value.cases.push({ id: `c${Date.now().toString(36)}`, label: `分支 ${d.value.cases.length + 1}`, keyword: '' })
}
function addRef(){ d.value.refs.push('{{#') }
</script>

<template>
  <aside class="drawer" aria-label="节点配置">
    <header>
      <i class="ico" :style="{ color: meta.color, background: meta.color + '1A' }">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" v-html="meta.icon" />
      </i>
      <div class="tt">
        <input v-model="d.name" class="ninput" :placeholder="meta.label" :aria-label="'节点名称'">
        <span class="tlabel">{{ meta.label }} · {{ node.id }}</span>
      </div>
      <button class="close" aria-label="关闭配置面板" @click="$emit('close')">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"><path d="M5 5l14 14M19 5 5 19"/></svg>
      </button>
    </header>
    <p class="desc">{{ meta.desc }}</p>

    <div class="body">
      <!-- 开始：输入字段 -->
      <template v-if="node.type === 'start'">
        <h4>输入字段</h4>
        <div v-for="(f, i) in d.fields" :key="i" class="row2">
          <input v-model="f.name" placeholder="变量名" aria-label="变量名">
          <input v-model="f.label" placeholder="显示名" aria-label="显示名">
          <button class="del" aria-label="删除字段" @click="d.fields.splice(i, 1)">×</button>
        </div>
        <button class="add" @click="addField">+ 添加字段</button>
      </template>

      <!-- 直接回复 -->
      <template v-else-if="node.type === 'answer'">
        <h4>回复内容（支持变量引用）</h4>
        <textarea v-model="d.text" rows="6" />
      </template>

      <!-- LLM -->
      <template v-else-if="node.type === 'llm'">
        <h4>模型</h4>
        <input v-model="d.model" placeholder="模型名（演示）">
        <h4>温度 {{ d.temperature }}</h4>
        <input v-model.number="d.temperature" type="range" min="0" max="1" step="0.1" class="range">
        <h4>Prompt（支持变量引用）</h4>
        <textarea v-model="d.prompt" rows="7" />
      </template>

      <!-- 问题分类 -->
      <template v-else-if="node.type === 'classifier'">
        <h4>分类输入（变量引用，留空取上游文本）</h4>
        <input v-model="d.input" placeholder="{{#start_1.query#}}">
        <h4>分类列表（按序匹配）</h4>
        <div v-for="(c, i) in d.classes" :key="i" class="row1">
          <input v-model="d.classes[i]" aria-label="分类名">
          <button class="del" aria-label="删除分类" @click="d.classes.splice(i, 1)">×</button>
        </div>
        <button class="add" @click="addClass">+ 添加分类</button>
      </template>

      <!-- 知识检索 -->
      <template v-else-if="node.type === 'knowledge'">
        <h4>检索输入（变量引用，留空取上游文本）</h4>
        <input v-model="d.input" placeholder="{{#start_1.query#}}">
        <h4>Top K</h4>
        <input v-model.number="d.topK" type="number" min="1" max="8">
      </template>

      <!-- 条件分支 -->
      <template v-else-if="node.type === 'ifelse'">
        <h4>判断输入（变量引用，留空取上游文本）</h4>
        <input v-model="d.input" placeholder="{{#classifier_1.class_name#}}">
        <h4>分支条件（输入包含关键词即命中，按序匹配）</h4>
        <div v-for="(c, i) in d.cases" :key="c.id" class="row2">
          <input v-model="c.label" placeholder="分支名" aria-label="分支名">
          <input v-model="c.keyword" placeholder="关键词" aria-label="关键词">
          <button class="del" aria-label="删除分支" @click="d.cases.splice(i, 1)">×</button>
        </div>
        <button class="add" @click="addCase">+ 添加分支</button>
        <p class="note">未命中任何分支时走「否则」出口。</p>
      </template>

      <!-- 代码执行 -->
      <template v-else-if="node.type === 'code'">
        <h4>JavaScript 代码（inputs 为上游输出合集）</h4>
        <textarea v-model="d.code" rows="9" class="mono" spellcheck="false" />
        <p class="note">演示环境直接在浏览器执行，请勿写死循环。</p>
      </template>

      <!-- HTTP 请求 -->
      <template v-else-if="node.type === 'http'">
        <h4>请求方法</h4>
        <select v-model="d.method">
          <option>GET</option><option>POST</option><option>PUT</option><option>DELETE</option>
        </select>
        <h4>URL</h4>
        <input v-model="d.url">
        <h4>模拟响应 JSON（演示环境不实际发请求）</h4>
        <textarea v-model="d.mockResponse" rows="6" class="mono" spellcheck="false" />
      </template>

      <!-- 模板转换 -->
      <template v-else-if="node.type === 'template'">
        <h4>模板（支持变量引用）</h4>
        <textarea v-model="d.template" rows="7" />
      </template>

      <!-- 变量聚合 -->
      <template v-else-if="node.type === 'aggregator'">
        <h4>聚合的变量引用</h4>
        <div v-for="(r, i) in d.refs" :key="i" class="row1">
          <input v-model="d.refs[i]" aria-label="变量引用">
          <button class="del" aria-label="删除变量" @click="d.refs.splice(i, 1)">×</button>
        </div>
        <button class="add" @click="addRef">+ 添加变量</button>
      </template>
    </div>

    <footer>
      <div v-if="upstream.length">
        <h4>上游变量（点击复制引用）</h4>
        <div class="vars">
          <button v-for="u in upstream" :key="u.nodeId + u.varName" class="vchip" @click="copyRef(u)">
            <b>{{ u.nodeLabel }}</b>{{ u.varName }}
            <em v-if="copied === `{{#${u.nodeId}.${u.varName}#}}`">已复制</em>
          </button>
        </div>
      </div>
      <button class="danger" @click="emit('remove')">
        <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18M8 6V4h8v2m-9 0 1 14h8l1-14" /></svg>
        删除该节点
      </button>
    </footer>
  </aside>
</template>

<style scoped>
.drawer{position:absolute;top:12px;right:12px;bottom:12px;width:316px;background:#fff;border:1px solid var(--line);border-radius:14px;box-shadow:0 8px 32px rgba(30,35,50,.12);display:flex;flex-direction:column;z-index:30;overflow:hidden}
header{display:flex;align-items:center;gap:9px;padding:13px 14px 0}
.tt{flex:1;min-width:0}
.ninput{width:100%;border:1px solid transparent;border-radius:6px;padding:3px 7px;font:700 14px/1.5 inherit;color:var(--ink);background:none;transition:border-color .15s,background .15s}
.ninput:hover{background:var(--gray)}
.ninput:focus{outline:none;border-color:var(--blue);background:#fff}
.tlabel{display:block;font-size:11px;color:var(--faint);padding-left:7px}
.close{flex:none;display:inline-flex;width:26px;height:26px;align-items:center;justify-content:center;border-radius:7px;color:var(--muted);transition:background .15s,color .15s}
.close:hover{background:var(--gray);color:var(--ink)}
.ico{flex:none;display:inline-flex;width:30px;height:30px;border-radius:8px;align-items:center;justify-content:center}
.ico svg{width:17px;height:17px}
.desc{padding:6px 14px 10px;font-size:11.5px;color:var(--muted);line-height:1.6;border-bottom:1px solid var(--line2)}

.body{flex:1;overflow-y:auto;padding:12px 14px 16px}
h4{font-size:11.5px;font-weight:600;color:var(--muted);margin:12px 0 6px}
h4:first-child{margin-top:0}
input,select,textarea{width:100%;border:1px solid var(--line);border-radius:8px;padding:7px 10px;font:400 12.5px/1.6 inherit;color:var(--ink);background:#fff;transition:border-color .15s,box-shadow .15s}
input:focus,select:focus,textarea:focus{outline:none;border-color:var(--blue);box-shadow:0 0 0 3px rgba(61,92,229,.12)}
textarea{resize:vertical;line-height:1.7}
textarea.mono, .mono{font:400 11.5px/1.7 ui-monospace,Menlo,Consolas,monospace}
.range{padding:0;height:22px;accent-color:var(--blue)}
input[type=number]{max-width:120px}

.row2{display:grid;grid-template-columns:1fr 1fr 26px;gap:6px;margin-bottom:6px;align-items:center}
.row1{display:grid;grid-template-columns:1fr 26px;gap:6px;margin-bottom:6px;align-items:center}
.del{width:26px;height:30px;border-radius:7px;color:var(--faint);font-size:16px;transition:background .15s,color .15s}
.del:hover{background:#FBEDEC;color:var(--red)}
.add{margin-top:2px;font-size:12px;font-weight:600;color:var(--blue);padding:5px 8px;border-radius:7px;transition:background .15s}
.add:hover{background:#F0F3FE}
.note{margin-top:8px;font-size:11px;color:var(--faint);line-height:1.6}

footer{flex:none;border-top:1px solid var(--line2);padding:10px 14px 12px;background:#FBFCFE;display:flex;flex-direction:column;gap:10px}
.vars{display:flex;flex-wrap:wrap;gap:6px}
.danger{align-self:flex-start;display:inline-flex;align-items:center;gap:6px;font:600 12px/1 inherit;color:var(--red);border:1px solid #E5B6B2;border-radius:8px;padding:7px 12px;background:#fff;cursor:pointer;transition:background .15s,border-color .15s}
.danger:hover{background:#FDF3F2;border-color:var(--red)}
.vchip{display:inline-flex;align-items:center;gap:5px;font-size:11px;color:var(--ink2);border:1px solid var(--line);border-radius:999px;padding:3.5px 10px;background:#fff;transition:border-color .15s,color .15s}
.vchip b{font-weight:600;color:var(--blue)}
.vchip:hover{border-color:var(--blue)}
.vchip em{font-style:normal;color:#1E7B4F;font-weight:600}
</style>
