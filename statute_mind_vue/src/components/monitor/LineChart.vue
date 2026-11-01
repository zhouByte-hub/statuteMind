<script setup>
import { computed } from 'vue'

/**
 * 轻量实时折线图（SVG 手绘，支持多序列与 null 断点）。
 * props:
 *  - series: [{ name, color, points: number[] }]，null 值断线
 *  - unit / height / max: 纵轴单位、高度、纵轴上限（缺省自适应）
 */
const props = defineProps({
  series: { type: Array, default: () => [] },
  unit: { type: String, default: '' },
  height: { type: Number, default: 130 },
  max: { type: Number, default: 0 }
})

const W = 520
const H = 130
const PAD_L = 40
const PAD_R = 10
const PAD_T = 12
const PAD_B = 18

const n = computed(() => Math.max(...props.series.map(s => s.points.length), 2))

const yMax = computed(() => {
  if(props.max) return props.max
  let m = 0
  for(const s of props.series) for(const v of s.points) if(v != null && v > m) m = v
  return m <= 0 ? 1 : m * 1.15
})

function x(i){ return PAD_L + i / (n.value - 1) * (W - PAD_L - PAD_R) }
function y(v){ return PAD_T + (1 - v / yMax.value) * (H - PAD_T - PAD_B) }

/** 一条序列的折线路径（null 断开成多段）+ 末端点 */
const lines = computed(() => props.series.map(s => {
  const segs = []
  let d = ''
  let prev = null
  s.points.forEach((v, i) => {
    if(v == null){ prev = null; return }
    d += (prev === null ? 'M' : 'L') + x(i).toFixed(1) + ' ' + y(v).toFixed(1)
    prev = v
  })
  if(d) segs.push(d)
  let dot = null
  for(let i = s.points.length - 1; i >= 0; i--){
    if(s.points[i] != null){ dot = { x: x(i), y: y(s.points[i]), v: s.points[i] }; break }
  }
  return { name: s.name, color: s.color, segs, dot }
}))

const yTicks = computed(() => [1, .5, 0].map(f => ({ y: y(yMax.value * f), label: fmt(yMax.value * f) })))

function fmt(v){
  if(!props.unit) return Math.round(v)
  if(v >= 1e6) return (v / 1e6).toFixed(1) + 'M'
  if(v >= 1e3) return (v / 1e3).toFixed(1) + 'k'
  return +v.toFixed(1)
}
const fmtDot = v => v >= 1e3 ? (v / 1e3).toFixed(1) + 'k' : +v.toFixed(1)
</script>

<template>
  <figure class="lchart">
    <svg :viewBox="`0 0 ${W} ${H}`" preserveAspectRatio="none" :style="{ height: height + 'px' }" role="img" :aria-label="series.map(s => s.name).join('、') + '趋势图'">
      <line v-for="(t, i) in yTicks" :key="i" :x1="PAD_L" :x2="W - PAD_R" :y1="t.y" :y2="t.y" class="grid" />
      <text v-for="(t, i) in yTicks" :key="'t' + i" :x="PAD_L - 5" :y="t.y + 3.5" class="ylabel">{{ t.label }}</text>
      <g v-for="l in lines" :key="l.name">
        <path v-for="(d, i) in l.segs" :key="i" :d="d" fill="none" :stroke="l.color" stroke-width="1.8" stroke-linejoin="round" stroke-linecap="round" vector-effect="non-scaling-stroke" />
        <circle v-if="l.dot" :cx="l.dot.x" :cy="l.dot.y" r="3" :fill="l.color" :stroke="'#fff'" stroke-width="1.2" />
      </g>
    </svg>
    <figcaption class="legend">
      <span v-for="l in lines" :key="l.name" class="li">
        <i :style="{ background: l.color }" />{{ l.name }}<b v-if="l.dot" class="num-tab">{{ fmtDot(l.dot.v) }}{{ unit }}</b>
      </span>
    </figcaption>
  </figure>
</template>

<style scoped>
.lchart{width:100%}
svg{display:block;width:100%}
.grid{stroke:var(--line2);stroke-width:1}
.ylabel{font-size:9.5px;fill:var(--faint);text-anchor:end;font-variant-numeric:tabular-nums}
.legend{display:flex;gap:16px;flex-wrap:wrap;margin-top:8px;font-size:12px;color:var(--muted)}
.legend .li{display:inline-flex;align-items:center;gap:6px}
.legend i{width:9px;height:9px;border-radius:2.5px;flex:none}
.legend b{color:var(--ink);font-size:12.5px;margin-left:2px}
</style>
