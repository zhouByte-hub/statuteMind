<script setup>
/** 工作流通用节点卡片：图标 + 名称 + 摘要 + 状态徽标 + 连接锚点（条件分支多输出） */
import { computed } from 'vue'
import { Handle, Position } from '@vue-flow/core'
import { NODE_TYPES, ELSE_HANDLE } from '@/workflow/registry'

const props = defineProps({
  id: { type: String, required: true },
  type: { type: String, required: true },
  data: { type: Object, required: true },
  selected: Boolean
})

const meta = computed(() => NODE_TYPES[props.type] || {})

const STATUS = {
  idle: null,
  running: { label: '运行中' },
  success: { label: '成功' },
  failed: { label: '失败' },
  skipped: { label: '跳过' }
}
const st = computed(() => STATUS[props.data.status] || null)

/** 直接回复卡片里的模板预览 */
const preview = computed(() => {
  const t = String(props.data.text || '').replace(/\s+/g, ' ').trim()
  return t.length > 46 ? t.slice(0, 46) + '…' : t
})
</script>

<template>
  <div class="wfnode" :class="{ sel: selected, [data.status || '']: !!data.status }">
    <Handle v-if="meta.hasTarget" type="target" :position="Position.Left" />

    <header>
      <i class="ico" :style="{ color: meta.color, background: meta.color + '1A' }">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" v-html="meta.icon" />
      </i>
      <b class="name">{{ data.name || meta.label }}</b>
      <span v-if="st" class="st" :class="data.status">
        <i class="dot" />{{ st.label }}
      </span>
    </header>

    <p class="sum">{{ data.result || meta.summary(data) }}</p>

    <!-- 开始节点：列出输入字段 -->
    <ul v-if="type === 'start'" class="fields">
      <li v-for="f in data.fields || []" :key="f.name">
        <code>{{ f.name }}</code><span>{{ f.label }}</span>
      </li>
    </ul>

    <!-- 直接回复：模板预览 -->
    <p v-else-if="type === 'answer'" class="preview">{{ preview || '未配置回复内容' }}</p>

    <!-- LLM：模型行 -->
    <p v-else-if="type === 'llm'" class="preview">{{ data.model }}</p>

    <!-- 条件分支：每分支一个输出锚点 -->
    <template v-if="type === 'ifelse'">
      <div v-for="c in data.cases || []" :key="c.id" class="branch">
        <span class="blabel"><i class="bdot" />{{ c.label || '分支' }}<em v-if="c.keyword">含「{{ c.keyword }}」</em></span>
        <Handle type="source" :id="c.id" :position="Position.Right" />
      </div>
      <div class="branch else">
        <span class="blabel"><i class="bdot" />否则</span>
        <Handle type="source" :id="ELSE_HANDLE" :position="Position.Right" />
      </div>
    </template>

    <Handle v-else-if="meta.outputs !== 0" type="source" :position="Position.Right" />
  </div>
</template>

<style scoped>
.wfnode{
  position:relative;width:236px;background:#fff;border:1px solid var(--line);border-radius:12px;
  padding:11px 13px 12px;box-shadow:var(--shadow);font-size:12.5px;
  transition:box-shadow .2s,border-color .2s;
}
.wfnode.sel,.wfnode:hover{border-color:var(--blue);box-shadow:0 4px 16px rgba(61,92,229,.14)}
.wfnode.running{border-color:var(--blue)}
.wfnode.failed{border-color:var(--red)}
.wfnode.skipped{opacity:.55}

header{display:flex;align-items:center;gap:8px;min-width:0}
.ico{flex:none;display:inline-flex;width:26px;height:26px;border-radius:7px;align-items:center;justify-content:center}
.ico svg{width:15px;height:15px}
.name{font-size:13px;font-weight:700;color:var(--ink);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;flex:1}
.st{flex:none;display:inline-flex;align-items:center;gap:4px;font-size:11px;color:var(--muted)}
.st .dot{width:7px;height:7px;border-radius:50%}
.st.running{color:var(--blue)}
.st.running .dot{background:var(--blue);animation:wfPulse 1s ease-in-out infinite}
.st.success{color:#1E7B4F}
.st.success .dot{background:#1E7B4F}
.st.failed{color:var(--red)}
.st.failed .dot{background:var(--red)}
.st.skipped{color:var(--faint)}
.st.skipped .dot{background:var(--faint)}
@keyframes wfPulse{50%{opacity:.35;transform:scale(.8)}}

.sum{margin-top:7px;font-size:11.5px;color:var(--muted);line-height:1.5;word-break:break-all}
.wfnode.success .sum{color:#1E7B4F}
.wfnode.failed .sum{color:var(--red)}
.preview{margin-top:6px;font-size:11.5px;color:var(--faint);line-height:1.55;word-break:break-all;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}

.fields{list-style:none;margin-top:7px;display:flex;flex-direction:column;gap:4px}
.fields li{display:flex;align-items:center;gap:8px;background:var(--gray);border-radius:6px;padding:4px 8px}
.fields code{font:600 11px ui-monospace,Menlo,monospace;color:var(--link)}
.fields span{font-size:11.5px;color:var(--muted)}

.branch{position:relative;display:flex;align-items:center;margin-top:7px;border-top:1px dashed var(--line2);padding-top:7px}
.blabel{display:inline-flex;align-items:center;gap:6px;font-size:11.5px;color:var(--ink2);font-weight:500}
.blabel em{font-style:normal;color:var(--faint);font-size:11px}
.bdot{width:6px;height:6px;border-radius:50%;background:#C63A30;opacity:.75}
.branch.else .bdot{background:var(--faint)}
</style>
