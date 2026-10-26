<script setup>
/** 左侧节点库：按分组列出节点类型，支持拖拽入画布 / 点击添加 */
import { NODE_GROUPS, NODE_TYPES } from '@/workflow/registry'

defineEmits(['add'])

function onDragStart(e, type){
  e.dataTransfer.setData('application/wf-type', type)
  e.dataTransfer.effectAllowed = 'copy'
}
</script>

<template>
  <aside class="panel" aria-label="节点库">
    <h2>节点库</h2>
    <p class="hint">拖入画布或点击添加</p>
    <section v-for="g in NODE_GROUPS" :key="g.key">
      <h3>{{ g.label }}</h3>
      <button
        v-for="t in g.types" :key="t" class="item" draggable="true"
        @dragstart="e => onDragStart(e, t)" @click="$emit('add', t)"
        :title="NODE_TYPES[t].desc"
      >
        <i class="ico" :style="{ color: NODE_TYPES[t].color, background: NODE_TYPES[t].color + '1A' }">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" v-html="NODE_TYPES[t].icon" />
        </i>
        <span class="txt">
          <b>{{ NODE_TYPES[t].label }}</b>
          <em>{{ NODE_TYPES[t].desc }}</em>
        </span>
      </button>
    </section>
  </aside>
</template>

<style scoped>
.panel{flex:none;width:212px;border-right:1px solid var(--line);background:#fff;padding:14px 12px 20px;overflow-y:auto}
h2{font-size:13px;font-weight:700;color:var(--ink)}
.hint{font-size:11.5px;color:var(--faint);margin:3px 0 4px}
section{margin-top:12px}
h3{font-size:11px;font-weight:600;color:var(--faint);letter-spacing:.08em;margin-bottom:6px}
.item{display:flex;align-items:center;gap:9px;width:100%;text-align:left;border:1px solid transparent;border-radius:9px;padding:7px 8px;transition:background .15s,border-color .15s;cursor:grab}
.item:hover{background:#F5F7FF;border-color:var(--line)}
.item:active{cursor:grabbing}
.ico{flex:none;display:inline-flex;width:28px;height:28px;border-radius:8px;align-items:center;justify-content:center}
.ico svg{width:16px;height:16px}
.txt{min-width:0}
.txt b{display:block;font-size:12.5px;font-weight:600;color:var(--ink)}
.txt em{display:block;font-style:normal;font-size:10.5px;color:var(--faint);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;margin-top:1px}
</style>
