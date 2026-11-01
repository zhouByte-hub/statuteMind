<script setup>
import { computed } from 'vue'

/** 状态分布环形图（SVG 手绘） props: items [{label,value,color}] */
const props = defineProps({
  items: { type: Array, default: () => [] },
  size: { type: Number, default: 150 },
  centerLabel: { type: String, default: '总计' }
})

const total = computed(() => props.items.reduce((a, b) => a + b.value, 0))
const R = 52
const C = 2 * Math.PI * R

const arcs = computed(() => {
  let acc = 0
  return props.items.filter(i => i.value > 0).map(i => {
    const frac = total.value ? i.value / total.value : 0
    const a = { ...i, dash: frac * C, offset: -acc * C }
    acc += frac
    return a
  })
})
</script>

<template>
  <div class="donut">
    <svg :width="size" :height="size" viewBox="0 0 130 130" role="img" aria-label="状态分布环形图">
      <circle cx="65" cy="65" :r="R" fill="none" stroke="var(--gray)" stroke-width="15" />
      <circle
        v-for="a in arcs" :key="a.label" cx="65" cy="65" :r="R" fill="none"
        :stroke="a.color" stroke-width="15" stroke-linecap="butt"
        :stroke-dasharray="`${a.dash} ${C - a.dash}`" :stroke-dashoffset="a.offset"
        transform="rotate(-90 65 65)"
      />
      <text x="65" y="62" text-anchor="middle" class="cval num-tab">{{ total }}</text>
      <text x="65" y="78" text-anchor="middle" class="clabel">{{ centerLabel }}</text>
    </svg>
    <ul class="legend">
      <li v-for="i in items" :key="i.label">
        <i :style="{ background: i.color }" />
        <span>{{ i.label }}</span>
        <b class="num-tab">{{ i.value }}</b>
        <em class="num-tab">{{ total ? Math.round(i.value / total * 100) : 0 }}%</em>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.donut{display:flex;align-items:center;gap:22px;flex-wrap:wrap}
svg{flex:none}
.cval{font-size:23px;font-weight:800;fill:var(--ink)}
.clabel{font-size:10.5px;fill:var(--muted)}
.legend{list-style:none;display:flex;flex-direction:column;gap:9px;font-size:13px;min-width:150px;flex:1}
.legend li{display:flex;align-items:center;gap:8px;color:var(--ink2)}
.legend i{width:10px;height:10px;border-radius:3px;flex:none}
.legend b{margin-left:auto;color:var(--ink)}
.legend em{font-style:normal;color:var(--muted);font-size:12px;width:38px;text-align:right}
</style>
