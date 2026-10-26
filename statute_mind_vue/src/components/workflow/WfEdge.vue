<script setup>
/** 工作流连线：贝塞尔曲线，运行时激活的边带流光动画；选中时中点显示删除按钮 */
import { computed } from 'vue'
import { BaseEdge, EdgeLabelRenderer, getBezierPath } from '@vue-flow/core'

const props = defineProps({
  id: String,
  sourceX: Number,
  sourceY: Number,
  targetX: Number,
  targetY: Number,
  sourcePosition: String,
  targetPosition: String,
  markerEnd: String,
  data: Object,
  selected: Boolean
})

const emit = defineEmits(['remove'])

const p = computed(() => getBezierPath({
  sourceX: props.sourceX,
  sourceY: props.sourceY,
  targetX: props.targetX,
  targetY: props.targetY,
  sourcePosition: props.sourcePosition,
  targetPosition: props.targetPosition
}))
const path = computed(() => p.value[0])
const mid = computed(() => ({ x: p.value[1], y: p.value[2] }))
</script>

<template>
  <BaseEdge
    :id="id" :path="path" :marker-end="markerEnd"
    class="wf-edge" :class="{ active: data?.active, done: data?.done }"
  />
  <EdgeLabelRenderer>
    <button
      v-if="selected"
      class="edel" aria-label="删除连线" title="删除连线"
      :style="{ transform: `translate(-50%,-50%) translate(${mid.x}px,${mid.y}px)` }"
      @click="emit('remove')"
    >
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"><path d="M6 6l12 12M18 6L6 18" /></svg>
    </button>
  </EdgeLabelRenderer>
</template>

<style scoped>
.wf-edge{stroke:#A9B1BE;stroke-width:1.8;transition:stroke .3s}
.wf-edge.done{stroke:var(--blue)}
.wf-edge.active{
  stroke:var(--blue);stroke-width:2.2;
  stroke-dasharray:7 5;
  animation:wfFlow .45s linear infinite;
}
@keyframes wfFlow{to{stroke-dashoffset:-12}}

.edel{
  position:absolute;z-index:1;width:20px;height:20px;border-radius:50%;
  display:flex;align-items:center;justify-content:center;
  background:#fff;border:1px solid var(--line);color:var(--red);
  cursor:pointer;box-shadow:var(--shadow);pointer-events:all;
}
.edel svg{width:10px;height:10px}
.edel:hover{border-color:var(--red)}
</style>
