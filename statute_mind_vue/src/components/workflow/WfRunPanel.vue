<script setup>
/** 底部运行追踪面板：运行输入 → 执行 → 节点日志（输入/输出 JSON 可展开）+ 回复流式预览 */
import { reactive, ref, watch } from 'vue'

const props = defineProps({
  open: Boolean,
  running: Boolean,
  error: { type: String, default: '' },
  ms: { type: Number, default: 0 },
  answer: { type: String, default: '' },
  /** 开始节点字段定义 [{ name, label }] */
  fields: { type: Array, default: () => [] },
  /** 运行输入（响应式对象，直接双向写） */
  input: { type: Object, required: true },
  /** 节点执行日志 [{ id,label,type,status,ms,input,output }] */
  logs: { type: Array, default: () => [] }
})
const emit = defineEmits(['close', 'run', 'stop'])

const expanded = reactive(new Set())
function toggle(id){
  expanded.has(id) ? expanded.delete(id) : expanded.add(id)
}
const pretty = v => JSON.stringify(v, null, 2)

const STATUS = { success: '成功', failed: '失败', skipped: '跳过' }
</script>

<template>
  <transition name="up">
    <section v-if="open" class="runpanel" aria-label="运行追踪">
      <header>
        <b>运行追踪</b>
        <span v-if="ms" class="ms">耗时 {{ (ms / 1000).toFixed(2) }} s</span>
        <span class="demoTag">演示数据</span>
        <button class="close" aria-label="关闭运行面板" @click="$emit('close')">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"><path d="M5 5l14 14M19 5 5 19"/></svg>
        </button>
      </header>

      <div class="body">
        <!-- 运行输入 -->
        <div class="inputForm" :class="{ done: running || logs.length }">
          <h4>运行输入</h4>
          <div v-for="f in fields" :key="f.name" class="frow">
            <label :for="`wf-in-${f.name}`">{{ f.label || f.name }}</label>
            <textarea
              :id="`wf-in-${f.name}`" v-model="input[f.name]" rows="2"
              placeholder="如：公司拖欠工资 3 个月" :disabled="running"
            />
          </div>
          <div class="btns">
            <button v-if="!running" class="go" @click="$emit('run')">开始运行</button>
            <button v-else class="stop" @click="$emit('stop')">停止</button>
          </div>
          <p v-if="error" class="err" role="alert">{{ error }}</p>
        </div>

        <div class="logsWrap">
          <!-- 回复流式预览 -->
          <div v-if="answer || running" class="answer">
            <h4>回复</h4>
            <p>{{ answer }}<i v-if="running" class="caret" /></p>
          </div>

          <!-- 节点时序日志 -->
          <ol v-if="logs.length">
            <li v-for="l in logs" :key="l.id + l.status" :class="l.status">
              <button class="lrow" @click="toggle(l.id + l.status)">
                <i class="ldot" />
                <b>{{ l.label }}</b>
                <em v-if="l.status === 'failed'" class="lfail">{{ STATUS[l.status] }}</em>
                <em v-else>{{ STATUS[l.status] }}</em>
                <span class="lms">{{ l.ms }} ms</span>
                <svg class="chev" :class="{ open: expanded.has(l.id + l.status) }" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><path d="m6 9 6 6 6-6"/></svg>
              </button>
              <div v-if="expanded.has(l.id + l.status)" class="ldetail">
                <div><h5>输入</h5><pre>{{ pretty(l.input) }}</pre></div>
                <div><h5>输出</h5><pre>{{ pretty(l.output) }}</pre></div>
              </div>
            </li>
          </ol>
          <p v-else-if="!running" class="empty">点击「开始运行」后，这里将按执行顺序展示每个节点的输入与输出。</p>
        </div>
      </div>
    </section>
  </transition>
</template>

<style scoped>
.runpanel{position:absolute;left:12px;right:12px;bottom:12px;max-height:46%;background:#fff;border:1px solid var(--line);border-radius:14px;box-shadow:0 8px 32px rgba(30,35,50,.14);display:flex;flex-direction:column;z-index:30;overflow:hidden}
.up-enter-active,.up-leave-active{transition:transform .3s var(--ease),opacity .3s}
.up-enter-from,.up-leave-to{transform:translateY(24px);opacity:0}

header{flex:none;display:flex;align-items:center;gap:10px;padding:11px 14px;border-bottom:1px solid var(--line2)}
header b{font-size:13px;color:var(--ink)}
.ms{font-size:12px;color:#1E7B4F;font-weight:600;font-variant-numeric:tabular-nums}
.close{margin-left:auto;display:inline-flex;width:26px;height:26px;align-items:center;justify-content:center;border-radius:7px;color:var(--muted);transition:background .15s,color .15s}
.close:hover{background:var(--gray);color:var(--ink)}

.body{flex:1;display:flex;min-height:0}
.inputForm{flex:none;width:250px;border-right:1px solid var(--line2);padding:12px 14px;overflow-y:auto;transition:opacity .3s}
.inputForm.done{opacity:.55}
h4{font-size:11.5px;font-weight:600;color:var(--muted);margin-bottom:7px}
.frow{margin-bottom:8px}
.frow label{display:block;font-size:11.5px;color:var(--ink2);margin-bottom:4px}
textarea{width:100%;border:1px solid var(--line);border-radius:8px;padding:6px 9px;font:400 12.5px/1.6 inherit;color:var(--ink);resize:vertical;transition:border-color .15s,box-shadow .15s}
textarea:focus{outline:none;border-color:var(--blue);box-shadow:0 0 0 3px rgba(61,92,229,.12)}
textarea:disabled{background:var(--gray)}
.btns{margin-top:6px}
.go,.stop{width:100%;height:34px;border-radius:9px;font:700 13px inherit;letter-spacing:.06em;transition:background .2s,transform .15s var(--ease)}
.go{background:var(--blue);color:#fff}
.go:hover{background:var(--blue-h)}
.go:active{transform:scale(.97)}
.stop{background:#FBEDEC;color:var(--red)}
.stop:hover{background:#F6DDDB}
.err{margin-top:8px;font-size:11.5px;color:var(--red);line-height:1.6}

.logsWrap{flex:1;overflow-y:auto;padding:12px 16px 14px;min-width:0}
.answer{margin-bottom:10px}
.answer p{font-size:12.5px;color:var(--ink);line-height:1.8;white-space:pre-wrap;background:#F7F9FF;border:1px solid #E3E9FC;border-radius:10px;padding:9px 12px}
.caret{display:inline-block;width:2px;height:13px;background:var(--blue);vertical-align:-2px;margin-left:2px;animation:blink .8s steps(1) infinite}
@keyframes blink{50%{opacity:0}}

ol{list-style:none}
li{border:1px solid var(--line2);border-radius:10px;margin-bottom:7px;overflow:hidden}
li.failed{border-color:#F0D2CF}
.lrow{display:flex;align-items:center;gap:8px;width:100%;text-align:left;padding:8px 11px;font-size:12.5px;transition:background .15s}
.lrow:hover{background:var(--gray)}
.ldot{flex:none;width:8px;height:8px;border-radius:50%}
li.success .ldot{background:#1E7B4F}
li.failed .ldot{background:var(--red)}
li.skipped .ldot{background:var(--faint)}
.lrow b{font-weight:600;color:var(--ink)}
.lrow em{font-style:normal;font-size:11px;color:var(--muted)}
.lrow em.lfail{color:var(--red);font-weight:600}
.lms{margin-left:auto;font-size:11px;color:var(--faint);font-variant-numeric:tabular-nums}
.chev{color:var(--faint);transition:transform .2s}
.chev.open{transform:rotate(180deg)}
.ldetail{display:grid;grid-template-columns:1fr 1fr;gap:10px;padding:2px 11px 10px;border-top:1px dashed var(--line2)}
.ldetail h5{font-size:11px;font-weight:600;color:var(--faint);margin:8px 0 4px}
.ldetail pre{margin:0;font:400 11px/1.6 ui-monospace,Menlo,Consolas,monospace;color:var(--ink2);background:var(--gray);border-radius:8px;padding:8px 10px;max-height:180px;overflow:auto;white-space:pre-wrap;word-break:break-all}
.empty{font-size:12px;color:var(--faint);padding:10px 2px}

@media (max-width:760px){
  .body{flex-direction:column;overflow-y:auto}
  .inputForm{width:100%;border-right:0;border-bottom:1px solid var(--line2)}
  .logsWrap{flex:none}
  .ldetail{grid-template-columns:1fr}
}
</style>
