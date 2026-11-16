<script setup>
import { computed, ref } from 'vue'
import { monitor } from '@/monitor/metrics'

/** 运行日历：按月展示每日任务热度，悬浮/聚焦查看当日完成率、失败率、文档解析与 RAG 指标 */
const WEEK = ['一', '二', '三', '四', '五', '六', '日']
const offset = ref(0) // 0 = 本月，负数 = 往前翻

const base = new Date(); base.setHours(0, 0, 0, 0)
const todayKey = `${base.getFullYear()}-${String(base.getMonth() + 1).padStart(2, '0')}-${String(base.getDate()).padStart(2, '0')}`

const title = computed(() => {
  const d = new Date(base.getFullYear(), base.getMonth() + offset.value, 1)
  return `${d.getFullYear()} 年 ${d.getMonth() + 1} 月`
})

/** 热度等级：按当日任务数分 5 档 */
const level = t => !t ? 0 : t < 6 ? 1 : t < 14 ? 2 : t < 24 ? 3 : 4

const cells = computed(() => {
  const first = new Date(base.getFullYear(), base.getMonth() + offset.value, 1)
  const y = first.getFullYear(), m = first.getMonth()
  const days = new Date(y, m + 1, 0).getDate()
  const arr = []
  for(let i = 0, lead = (first.getDay() + 6) % 7; i < lead; i++) arr.push(null)
  for(let d = 1; d <= days; d++){
    const key = `${y}-${String(m + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    const rec = monitor.days[key] || null
    arr.push({ d, m: m + 1, key, rec, today: key === todayKey, future: key > todayKey, level: level(rec?.t) })
  }
  return arr
})

/** 本月汇总（页脚） */
const summary = computed(() => {
  let t = 0, ok = 0
  for(const c of cells.value) if(c?.rec){ t += c.rec.t; ok += c.rec.ok }
  return t ? `本月 ${t} 个任务 · 完成率 ${Math.round(ok / t * 100)}%` : '本月暂无运行记录'
})

const pct = (a, b) => b ? `${Math.round(a / b * 100)}%` : '—'
const avgMs = r => r.t ? `${Math.round(r.ms / r.t / 100) / 10}s` : '—'
const avgHits = r => r.ragHit ? `${Math.round(r.ragScore / r.ragHit * 10) / 10} 条` : '—'
const fmtDate = c => `${c.m} 月 ${c.d} 日 · 周${'日一二三四五六'[new Date(c.key.replace(/-/g, '/')).getDay()]}`
</script>

<template>
  <div class="cal">
    <header class="chead">
      <button class="nav" :disabled="offset <= -3" aria-label="上一月" @click="offset--">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" aria-hidden="true"><path d="M15 5l-7 7 7 7"/></svg>
      </button>
      <span class="mtitle">{{ title }}</span>
      <button class="nav" :disabled="offset >= 0" aria-label="下一月" @click="offset++">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" aria-hidden="true"><path d="M9 5l7 7-7 7"/></svg>
      </button>
    </header>

    <div class="grid" role="grid" aria-label="按日运行热力">
      <span v-for="w in WEEK" :key="w" class="wd">{{ w }}</span>
      <template v-for="(c, i) in cells" :key="c?.key ?? `b${i}`">
        <span v-if="!c" class="cell blank" />
        <span
          v-else
          class="cell day"
          :class="[`lv${c.level}`, { today: c.today, future: c.future, 'edge-l': i % 7 < 2, 'edge-r': i % 7 > 4 }]"
          :tabindex="c.rec ? 0 : -1"
        >
          <i class="num-tab">{{ c.d }}</i>
          <span v-if="c.rec" class="tip" role="tooltip">
            <b>{{ fmtDate(c) }}<em v-if="c.rec.demo">演示数据</em></b>
            <span class="row">任务完成率 <em>{{ pct(c.rec.ok, c.rec.t) }}</em><small>{{ c.rec.t }} 个任务</small></span>
            <span class="row">任务失败率 <em>{{ pct(c.rec.fail, c.rec.t) }}</em><small>跳过 {{ c.rec.skip }}</small></span>
            <span class="row">文档解析成功率 <em>{{ c.rec.imp ? pct(c.rec.impOk, c.rec.imp) : '—' }}</em><small>{{ c.rec.imp }} 次导入</small></span>
            <span class="row">RAG 检索 <em>{{ c.rec.rag }} 次</em><small>命中率 {{ pct(c.rec.ragHit, c.rec.rag) }}</small></span>
            <span class="row">平均命中 <em>{{ avgHits(c.rec) }}</em><small>共 {{ c.rec.ragScore }} 条</small></span>
            <span class="row">平均任务耗时 <em>{{ avgMs(c.rec) }}</em><small>LLM 调用 {{ c.rec.llm }} 次</small></span>
          </span>
          <span v-else-if="!c.future" class="tip"><b>{{ fmtDate(c) }}</b><span class="none">当日无运行记录</span></span>
        </span>
      </template>
    </div>

    <footer class="cfoot">
      <span class="legend">少 <i class="lv0" /><i class="lv1" /><i class="lv2" /><i class="lv3" /><i class="lv4" /> 多</span>
      <span class="sum">{{ summary }}</span>
    </footer>
  </div>
</template>

<style scoped>
.cal{display:flex;flex-direction:column;gap:10px;height:100%;min-height:0}

/* 月份导航 */
.chead{display:flex;align-items:center;justify-content:space-between}
.mtitle{font-size:13.5px;font-weight:800;color:var(--ink)}
.nav{display:grid;place-items:center;width:24px;height:24px;border:1px solid var(--line);border-radius:6px;background:#fff;color:var(--muted);cursor:pointer}
.nav:hover:not(:disabled){border-color:var(--blue);color:var(--blue)}
.nav:disabled{opacity:.35;cursor:default}

/* 日期网格：行高随容器拉伸，保证与左栏等高 */
.grid{flex:1;display:grid;grid-template-columns:repeat(7,1fr);grid-template-rows:auto;grid-auto-rows:1fr;gap:4px;min-height:0}
.wd{font-size:11px;color:var(--faint);text-align:center;padding-bottom:2px}
.cell{position:relative;border-radius:6px;display:grid;place-items:center;min-width:0;min-height:34px}
.cell i{font-style:normal;font-size:11.5px;font-weight:600;color:var(--muted)}
.day{cursor:default;transition:transform .15s,box-shadow .15s}
.day:not(.future):hover,.day:focus-visible{transform:scale(1.12);box-shadow:0 2px 8px rgba(34,34,34,.15);z-index:2;outline:none}
.blank{background:transparent}
.future i{color:var(--line)}
.lv0{background:var(--line2)}
.lv1{background:#E3EBFC}
.lv2{background:#BFD0F8}
.lv3{background:#7F9FF0}
.lv3 i,.lv4 i{color:#fff}
.lv4{background:var(--blue)}
.today{box-shadow:inset 0 0 0 2px var(--ink)}

/* 悬浮详情卡 */
.tip{position:absolute;bottom:calc(100% + 8px);left:50%;transform:translateX(-50%);display:none;z-index:30;width:228px;background:#fff;border:1px solid var(--line);border-radius:10px;padding:12px 14px;box-shadow:0 8px 24px rgba(34,34,34,.14);pointer-events:none;text-align:left;cursor:default}
.day:hover .tip,.day:focus-visible .tip{display:block}
.edge-l .tip{left:0;transform:none}
.edge-r .tip{left:auto;right:0;transform:none}
.tip b{display:flex;align-items:center;gap:6px;font-size:12.5px;font-weight:800;color:var(--ink);padding-bottom:8px;margin-bottom:8px;border-bottom:1px solid var(--line2)}
.tip b em{font-style:normal;font-size:10px;font-weight:600;color:#8A5A00;background:#FDF0E6;border-radius:4px;padding:1px 6px}
.tip .row{display:flex;align-items:baseline;gap:6px;font-size:12px;color:var(--muted);line-height:2;white-space:nowrap}
.tip .row em{font-style:normal;font-weight:800;color:var(--ink);margin-left:auto}
.tip .row small{flex:none;width:64px;text-align:right;font-size:10.5px;color:var(--faint)}
.tip .none{display:block;font-size:12px;color:var(--faint);padding:2px 0}

/* 页脚 */
.cfoot{display:flex;align-items:center;justify-content:space-between;font-size:11px;color:var(--faint)}
.legend i{display:inline-block;width:11px;height:11px;border-radius:3px;margin-left:3px;vertical-align:-1.5px}
.sum{color:var(--muted);font-weight:600}

@media (max-width:560px){
  .tip{width:200px}
  .tip .row small{width:auto}
}
</style>
