<script setup>
/** 工作流连线：贝塞尔曲线，运行时激活的边带流光动画 */
import { computed } from 'vue'
import { BaseEdge, getBezierPath } from '@vue-flow/core'

const props = defineProps({
  id: String,
  sourceX: Number,
  sourceY: Number,
  targetX: Number,
  targetY: Number,
  sourcePosition: String,
  targetPosition: String,
  markerEnd: String,
  data: Object
})

const path = computed(() => getBezierPath({
  sourceX: props.sourceX,
  sourceY: props.sourceY,
  targetX: props.targetX,
  targetY: props.targetY,
  sourcePosition: props.sourcePosition,
  targetPosition: props.targetPosition
}))
</script>

<template>
  <BaseEdge
    :id="id" :path="path" :marker-end="markerEnd"
    class="wf-edge" :class="{ active: data?.active, done: data?.done }"
  />
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
</style>
