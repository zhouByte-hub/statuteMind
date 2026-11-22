<script setup>
import { computed, onBeforeUnmount, onMounted, reactive } from 'vue'
import StatCard from '@/components/monitor/StatCard.vue'
import LineChart from '@/components/monitor/LineChart.vue'
import DonutChart from '@/components/monitor/DonutChart.vue'
import CalendarPanel from '@/components/monitor/CalendarPanel.vue'
import { monitor } from '@/monitor/metrics'
import headerImg from '@/assets/analysis_header.png'

const WINDOW = 60 // 图表展示最近 60s 窗口
let timer = null

/** 展示快照：仅按刷新节奏从采集层拷贝 */
const view = reactive({
  active: [], tasks: [], totals: { requests: 0, success: 0, failed: 0, skipped: 0, nodes: 0 },
  llm: { calls: 0, ttft: [], speed: [], outLen: [] },
  lastError: null,
  series: { latency: [], throughput: [], errorRate: [] }
})

const slice = k => monitor.series[k].slice(-WINDOW)

function load(){
  view.active = [...monitor.active]
  view.tasks = monitor.tasks.slice(0, 8)
  view.totals = { ...monitor.totals }
  view.llm = { calls: monitor.llm.calls, ttft: [...monitor.llm.ttft], speed: [...monitor.llm.speed], outLen: [...monitor.llm.outLen] }
  view.lastError = monitor.lastError ? { ...monitor.lastError } : null
  for(const k of Object.keys(view.series)) view.series[k] = slice(k)
}

onMounted(() => {
  load()
  timer = setInterval(load, 5000)
})
onBeforeUnmount(() => clearInterval(timer))

/* ===== 阈值与告警 ===== */
const TH = { latencyMax: 2000, errRateMax: 10, ttftMax: 3000 }

const latencyNow = computed(() => lastVal(view.series.latency))
const errRateNow = computed(() => lastVal(view.series.errorRate))
function lastVal(arr){ for(let i = arr.length - 1; i >= 0; i--) if(arr[i] != null) return arr[i]; return null }

/** 大模型推理聚合值（数组序：新在前） */
const llmAgg = computed(() => {
  const L = view.llm
  const avg = a => a.length ? Math.round(a.reduce((s, v) => s + v, 0) / a.length) : null
  return { calls: L.calls, ttft: avg(L.ttft), speed: avg(L.speed), outLen: avg(L.outLen), last: L.ttft[0] ?? null }
})
const llmTtftOk = computed(() => llmAgg.value.last == null || llmAgg.value.last <= TH.ttftMax)

const alerts = computed(() => {
  const list = []
  if(latencyNow.value != null && latencyNow.value > TH.latencyMax) list.push(`平均响应延迟 ${latencyNow.value}ms 超过 ${TH.latencyMax}ms 阈值`)
  if(errRateNow.value != null && errRateNow.value > TH.errRateMax) list.push(`错误率 ${errRateNow.value}% 超过 ${TH.errRateMax}% 阈值`)
  if(!llmTtftOk.value) list.push(`LLM 首 Token 延迟 ${llmAgg.value.last}ms 超过 ${TH.ttftMax}ms 阈值`)
  if(view.lastError && Date.now() - view.lastError.at < 8000) list.push(`发生未捕获异常：${view.lastError.msg}`)
  return list
})

const taskStatus = computed(() => [
  { label: '成功', value: view.totals.success, color: '#2E9E5B' },
  { label: '失败', value: view.totals.failed, color: '#C63A30' },
  { label: '跳过/中止', value: view.totals.skipped, color: '#B25E00' }
])
const runningNow = computed(() => view.active.length)
const TYPE_LABEL = { search: '法条检索', judge: '行为判断', import: '知识导入', chat: '小法对话', workflow: '工作流' }
const fmtTime = ts => new Date(ts).toLocaleTimeString('zh-CN', { hour12: false })
</script>

<template>
  <section class="mon" aria-label="Agent 运行监控">
    <!-- 顶部横幅 -->
    <img class="hero" :src="headerImg" alt="运行监控" />

    <!-- 异常告警条 -->
    <div v-if="alerts.length" class="alarms" role="alert">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M12 2 1 21h22L12 2Zm0 7a1 1 0 0 1 1 1v4a1 1 0 1 1-2 0v-4a1 1 0 0 1 1-1Zm0 9.2a1.2 1.2 0 1 1 0-2.4 1.2 1.2 0 0 1 0 2.4Z"/></svg>
      <ul><li v-for="a in alerts" :key="a">{{ a }}</li></ul>
    </div>

    <!-- 一、任务执行状态 -->
    <section class="grp">
      <h2><i class="gi" />任务执行状态</h2>
      <div class="cards">
        <StatCard label="运行中任务" :value="runningNow" unit="个" :sub="runningNow ? view.active.map(t => TYPE_LABEL[t.type]).join('、') : '当前空闲'" :status="runningNow ? 'warn' : 'ok'" />
        <StatCard label="累计请求" :value="view.totals.requests" unit="次" sub="Agent 任务总发起数" />
        <StatCard label="工作流节点" :value="view.totals.nodes" unit="个" sub="已执行节点累计" />
        <StatCard label="未捕获异常" :value="view.lastError ? '有' : '无'" :status="view.lastError ? 'warn' : 'ok'" :sub="view.lastError ? view.lastError.msg : '近 8s 无异常'" />
      </div>
      <div class="charts two">
        <div class="chartbox">
          <h3>任务状态分布 <em>累计</em></h3>
          <DonutChart :items="taskStatus" center-label="任务" />
        </div>
        <div class="chartbox">
          <h3>最近任务 <em>新在前</em></h3>
          <ul class="tasks">
            <li v-for="t in view.tasks" :key="t.id">
              <span class="tk" :class="t.status">{{ t.status === 'success' ? '成功' : t.status === 'failed' ? '失败' : '跳过' }}</span>
              <span class="tn">{{ TYPE_LABEL[t.type] || t.type }}<em>{{ t.label }}</em></span>
              <span class="tm num-tab">{{ fmtTime(t.at) }}</span>
              <span class="tms num-tab">{{ t.ms }}ms</span>
            </li>
            <li v-if="!view.tasks.length" class="empty">暂无任务记录 —— 去发起一次法条检索或运行工作流试试</li>
          </ul>
        </div>
      </div>
    </section>

    <!-- 三、大模型推理 + 运行日历（各占 1/2） -->
    <div class="duo">
      <section class="grp">
        <h2><i class="gi" />大模型推理 <em class="gtag">工作流 LLM 节点实测</em></h2>
        <div class="cards four">
          <StatCard label="LLM 调用次数" :value="llmAgg.calls" unit="次" sub="最近 60 次滑动窗口" />
          <StatCard label="首 Token 延迟" :value="llmAgg.ttft ?? '—'" unit="ms" :status="llmTtftOk ? 'ok' : 'warn'" :sub="`最近一次 ${llmAgg.last ?? '—'}ms · 阈值 ≤ ${TH.ttftMax}ms`" />
          <StatCard label="平均生成速度" :value="llmAgg.speed ?? '—'" unit="字/s" sub="输出字符 / 生成耗时" />
          <StatCard label="平均输出长度" :value="llmAgg.outLen ?? '—'" unit="字" sub="单次生成字符数" />
        </div>
        <div class="charts two">
          <div class="chartbox">
            <h3>首 Token 延迟 <em>ms · 按调用</em></h3>
            <LineChart :series="[{ name: 'TTFT', color: '#C63A30', points: [...view.llm.ttft].reverse() }]" unit="ms" />
          </div>
          <div class="chartbox">
            <h3>生成速度 <em>字/s · 按调用</em></h3>
            <LineChart :series="[{ name: '生成速度', color: '#7A5AF8', points: [...view.llm.speed].reverse() }]" unit="字/s" />
          </div>
        </div>
      </section>

      <section class="grp">
        <h2><i class="gi" />运行日历 <em class="gtag">按日聚合 · 悬浮查看当日指标</em></h2>
        <div class="chartbox calbox">
          <CalendarPanel />
        </div>
      </section>
    </div>

    <!-- 四、性能与流量 -->
    <section class="grp">
      <h2><i class="gi" />性能与流量</h2>
      <div class="charts three">
        <div class="chartbox">
          <h3>响应延迟 <em>ms · 任务均值</em></h3>
          <LineChart :series="[{ name: '平均延迟', color: '#C63A30', points: view.series.latency }]" unit="ms" />
        </div>
        <div class="chartbox">
          <h3>请求吞吐量 <em>次/秒</em></h3>
          <LineChart :series="[{ name: '吞吐', color: '#2E9E5B', points: view.series.throughput }]" unit="次/s" />
        </div>
        <div class="chartbox">
          <h3>错误率 <em>%</em></h3>
          <LineChart :series="[{ name: '错误率', color: '#B25E00', points: view.series.errorRate }]" :max="100" unit="%" />
        </div>
      </div>
    </section>

  </section>
</template>

<style scoped>
.mon{width:min(1180px,100%);margin:0 auto;padding:26px 28px 48px;display:flex;flex-direction:column;gap:22px}

/* 顶部横幅图 */
.hero{width:100%;display:block;border-radius:16px;object-fit:cover;aspect-ratio:20/3}

/* 告警条 */
.alarms{display:flex;gap:10px;align-items:flex-start;background:#FDF3F2;border:1px solid #E7B7B2;border-radius:12px;padding:12px 16px;color:var(--red)}
.alarms svg{flex:none;margin-top:2px}
.alarms ul{list-style:none;display:flex;flex-direction:column;gap:3px;font-size:13px;font-weight:600}

/* 分组面板 */
.grp{display:flex;flex-direction:column;gap:14px;background:var(--gray);border:1px solid var(--line2);border-radius:16px;padding:18px}
.grp h2{display:flex;align-items:center;gap:9px;font-size:15.5px;font-weight:800;color:var(--ink)}
.gtag{font-style:normal;font-size:11px;font-weight:500;color:var(--faint)}
.gi{width:4px;height:15px;border-radius:2px;background:var(--blue)}
.cards{display:grid;grid-template-columns:repeat(4,1fr);gap:12px}
.cards:only-child{grid-template-columns:repeat(2,1fr)}

/* 双栏：大模型推理 2/3 + 运行日历 1/3，等高 */
.duo{display:grid;grid-template-columns:2fr 1fr;gap:22px;align-items:stretch}
.duo .cards.four{grid-template-columns:repeat(2,1fr)}
.duo .grp{min-width:0}
.calbox{flex:1;display:flex;flex-direction:column;min-height:0}

/* 图表区 */
.charts{display:grid;gap:12px}
.charts.two{grid-template-columns:1fr 1fr}
.charts.three{grid-template-columns:repeat(3,1fr)}
.chartbox{background:#fff;border:1px solid var(--line);border-radius:12px;padding:14px 16px;min-width:0}
.chartbox h3{font-size:13.5px;font-weight:700;color:var(--ink);margin-bottom:10px;display:flex;align-items:baseline;gap:8px}
.chartbox h3 em{font-style:normal;font-size:11px;font-weight:500;color:var(--faint)}

/* 最近任务列表 */
.tasks{list-style:none;display:flex;flex-direction:column;gap:8px;max-height:172px;overflow:auto}
.tasks li{display:flex;align-items:center;gap:10px;font-size:12.5px;min-width:0}
.tasks .empty{color:var(--muted);justify-content:center;padding:26px 0}
.tk{flex:none;font-size:11px;font-weight:700;border-radius:4px;padding:1.5px 7px}
.tk.success{background:#E7F4EC;color:#1E7A44}
.tk.failed{background:#FBEAE8;color:var(--red)}
.tk.skipped{background:#FDF0E6;color:var(--amber)}
.tn{color:var(--ink2);font-weight:600;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.tn em{font-style:normal;font-weight:400;color:var(--faint);margin-left:6px}
.tm{margin-left:auto;color:var(--faint);flex:none}
.tms{flex:none;width:52px;text-align:right;color:var(--muted)}

/* 响应式：平板两列卡片、手机单列 */
@media (max-width:1000px){
  .cards{grid-template-columns:repeat(2,1fr)}
  .charts.three{grid-template-columns:1fr 1fr}
  .duo{grid-template-columns:1fr}
}
@media (max-width:760px){
  .mon{padding:18px 14px 36px;gap:16px}
  .charts.two,.charts.three{grid-template-columns:1fr}
  .cards,.cards:only-child{grid-template-columns:repeat(2,1fr)}
  .grp{padding:14px}
}
@media (max-width:420px){
  .cards,.cards:only-child,.duo .cards.four{grid-template-columns:1fr}
}
</style>
