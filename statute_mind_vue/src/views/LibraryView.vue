<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ChipsRow from '@/components/ChipsRow.vue'
import LibRow from '@/components/LibRow.vue'
import { store } from '@/store/app'
import { useNav } from '@/composables/useNav'
import { CATS, STATUTES, statuteDetail } from '@/data/mock'
import { prefersReduced } from '@/utils/env'
import { beginTask } from '@/monitor/metrics'

const route = useRoute()
const router = useRouter()
const { openSearch, openStatute } = useNav()

const over = ref(false)
const uploading = ref(false)
const upName = ref('')
const upP = ref(0)
const upTxt = ref('0%')
const fileEl = ref(null)
const flashSeqs = reactive({}) // 条目 id → 定位高亮触发序号

const all = computed(() => [...store.uploaded, ...STATUTES])
const counts = computed(() => {
  const c = {}
  all.value.forEach(s => { c[s.cat] = (c[s.cat] || 0) + 1 })
  return c
})
const catItems = computed(() => CATS
  .filter(c => c === '全部' || counts.value[c])
  .map(c => ({
    value: c, label: c,
    count: c === '全部' ? all.value.length : counts.value[c],
    active: store.catFilter === c
  })))
const filtered = computed(() => all.value.filter(s => store.catFilter === '全部' || s.cat === store.catFilter))

/** 右栏分类分布：内置分类按 CATS 序排列，导入类垫后 */
const distItems = computed(() => [
  ...CATS.slice(1).filter(c => counts.value[c]).map(c => ({ name: c, n: counts.value[c] })),
  ...(counts.value['导入'] ? [{ name: '导入', n: counts.value['导入'] }] : [])
])

/** 时间轴：每部法律按其在库中的首条条文取现行版本时间，倒序（避免同法多条挤占年份） */
const tlItems = computed(() => {
  const seen = new Set()
  const items = []
  for (const s of STATUTES) {
    if (seen.has(s.law)) continue
    seen.add(s.law)
    items.push({ ...s, date: statuteDetail(s.id)?.meta.versions[0]?.date || '' })
  }
  return items.sort((a, b) => b.date.localeCompare(a.date))
})

/** 时间轴按年份分组（tlItems 已倒序） */
const tlGroups = computed(() => {
  const map = new Map()
  for (const s of tlItems.value) {
    const y = s.date.slice(0, 4)
    if (!map.has(y)) map.set(y, [])
    map.get(y).push(s)
  }
  return [...map.entries()].map(([year, items]) => ({ year, items }))
})
const rows = computed(() => {
  const order = store.catFilter === '全部' ? ['导入', ...CATS.slice(1)].filter(g => filtered.value.some(s => s.cat === g)) : [store.catFilter]
  let i = 0
  return order.map(g => ({ name: g, items: filtered.value.filter(s => s.cat === g).map(s => ({ s, i: i++ })) }))
})

function flashRow(id){
  flashSeqs[id] = (flashSeqs[id] || 0) + 1
}

/** 模拟上传：进度条 → 入库（计数 +2）→ 定位高亮新条目 */
let upTimer = null
function doUpload(name){
  if(uploading.value) return
  const end = beginTask('import', name)
  uploading.value = true
  upName.value = name
  upP.value = 0
  upTxt.value = '0%'
  upTimer = setInterval(() => {
    upP.value = Math.min(100, upP.value + 7 + Math.random() * 9)
    upTxt.value = `${Math.floor(upP.value)}% · ${upP.value < 55 ? '解析中…' : '切分入库…'}`
    if(upP.value >= 100){
      clearInterval(upTimer)
      upTxt.value = '已入库 · 新增 2 条（演示）'
      store.applyUpload()
      if(store.catFilter !== '全部' && store.catFilter !== '导入') store.setFilter('全部')
      setTimeout(() => {
        store.uploaded.forEach(u => flashRow(u.id))
        uploading.value = false
        end('success', '入库 2 条')
      }, 500)
    }
  }, prefersReduced ? 10 : 110)
}
onBeforeUnmount(() => clearInterval(upTimer))

function onDrop(e){
  over.value = false
  const f = e.dataTransfer?.files?.[0]
  doUpload(f ? f.name : '演示文件_劳动争议案例汇编.pdf')
}

/** 点击上传：打开系统文件选择框（选择真实文件名，解析仍为演示流程） */
function pickFile(){ fileEl.value?.click() }
function onPicked(e){
  const f = e.target.files?.[0]
  if(f) doUpload(f.name)
  e.target.value = '' // 允许重复选择同一文件
}

/** 「在知识库查看」深链：定位 + 蓝底高亮；随后清参便于同条目重复触发 */
watch(() => route.query.lib, async id => {
  if(!id) return
  await Promise.resolve()
  flashRow(id)
  router.replace({ name: 'library' })
}, { immediate: true })

watch(() => store.replay, r => { if(r?.type === 'library') flashRow(r.id) })
</script>

<template>
  <section class="vlib" aria-label="知识库">
    <div class="libWrap">
      <div class="libHeadRow">
        <h1>知识库</h1>
        <button
          class="drop" :class="{ over }" aria-label="上传文档扩充知识库（模拟解析流程）"
          @click="pickFile"
          @dragover.prevent="over = true" @dragenter.prevent="over = true"
          @dragleave.prevent="over = false" @drop.prevent="onDrop"
        >
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M12 16V5m0 0 4 4m-4-4-4 4" stroke="#4E6EF2" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M4 15v3a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-3" stroke="#4E6EF2" stroke-width="2" stroke-linecap="round"/>
          </svg>
          <span>上传文档，扩充知识库<span>点击选择或拖入文件 · 解析为演示流程</span></span>
        </button>
        <!-- 隐藏的文件选择框：点击上传按钮时触发 -->
        <input ref="fileEl" type="file" accept=".pdf,.doc,.docx,.txt,.md" class="filePick" aria-hidden="true" tabindex="-1" @change="onPicked">
      </div>
      <p class="libMeta">
        精选内置 <b class="num-tab">{{ STATUTES.length }}</b> 条真实条文节引 + 导入 <b class="num-tab">{{ store.uploaded.length }}</b> 条演示条目，可被「法条检索」与「行为判断」引用；上传后新条目将入库（此原型不真正解析文件）。
      </p>
      <div v-if="upName" class="uplog" role="status">
        <span>{{ upName }}</span>
        <div class="upbar"><b :style="{ width: upP + '%' }"></b></div>
        <span class="num-tab">{{ upTxt }}</span>
      </div>
      <ChipsRow class="catChips" :items="catItems" align="start" @select="it => store.setFilter(it.value)" />
      <p class="meta">
        {{ store.catFilter === '全部' ? `按法规分类 · ${rows.length} 组` : store.catFilter }} · 共 {{ filtered.length }} 条 · 演示数据
      </p>
      <div v-for="g in rows" :key="g.name" class="lgroup">
        <h2>{{ g.name }}<span class="num-tab">{{ g.items.length }} 条</span></h2>
        <LibRow v-for="r in g.items" :key="r.s.id" :s="r.s" :index="r.i" :flash-seq="flashSeqs[r.s.id] || 0" />
      </div>
    </div>

    <!-- 右侧栏（宽屏显示）：分类分布 + 重点条文 + 上传指引 -->
    <aside class="rail" aria-label="知识库分类与指引">
      <div class="railIn">
        <div class="card">
          <h2>分类分布</h2>
          <button v-for="c in distItems" :key="c.name" class="navrow" @click="store.setFilter(c.name)">
            <b>{{ c.name }}</b><span class="cnt num-tab">{{ c.n }} 条</span>
          </button>
        </div>
        <div class="card">
          <h2>最新法规条文</h2>
          <div class="tl">
            <div v-for="g in tlGroups" :key="g.year" class="tlyear">
              <span class="tly num-tab">{{ g.year }}</span>
              <button v-for="s in g.items" :key="s.id" class="tlrow" @click="openStatute(s.id)">
                <b>{{ s.law }}{{ s.art }}</b>
                <em class="num-tab">{{ s.date }}</em>
              </button>
            </div>
          </div>
        </div>
        <div class="card">
          <h2>上传指引</h2>
          <p class="gr"><b>支持格式</b>PDF · Word · TXT · Markdown</p>
          <p class="gr"><b>上传方式</b>点击选择，或直接拖入文件</p>
          <p class="gr"><b>入库流程</b>解析 → 切分 → 可被检索与判断引用</p>
          <p class="gcap">当前 内置 {{ STATUTES.length }} 条 · 导入 {{ store.uploaded.length }} 条 · 演示流程不真正解析文件</p>
        </div>
      </div>
    </aside>
  </section>
</template>

<style scoped>
/* 宽屏两列：主列 + 右侧统计栏，整组居中；外缘留白吸收超宽屏 */
.vlib{display:grid;grid-template-columns:minmax(460px,760px) 320px;column-gap:24px;justify-content:center;padding:0 20px;animation:fadein .25s both}
.vlib>.libWrap{grid-column:1;min-width:0}
.vlib>.rail{grid-column:2;min-width:0}
.railIn{position:sticky;top:84px;display:flex;flex-direction:column;gap:16px;animation:rise .5s var(--ease) both}
.card{background:#fff;border:1px solid var(--line);border-radius:12px;padding:14px 16px}
.card h2{margin:0 0 6px;font-size:13px;font-weight:700;color:var(--ink2)}
.navrow{display:flex;align-items:center;gap:8px;width:100%;padding:7px 8px;border-radius:8px;font-size:13.5px;color:var(--ink2);transition:background .15s}
.navrow b{font-weight:500;flex:none}
.navrow .cnt{margin-left:auto;font-size:11.5px;color:var(--faint);white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.navrow:hover{background:var(--gray)}
/* 最新法规条文垂直时间轴 */
.tl{position:relative;margin-top:8px;padding-left:20px}
.tl::before{content:"";position:absolute;left:6px;top:10px;bottom:10px;width:2px;background:var(--line)}
.tly{display:inline-block;margin:10px 0 2px;padding:1px 8px;border-radius:99px;background:var(--gray);font-size:11px;font-weight:700;color:var(--ink2);position:relative}
.tly::before{content:"";position:absolute;left:-19px;top:50%;transform:translateY(-50%);width:8px;height:8px;border-radius:99px;background:var(--blue);border:2px solid #fff;box-sizing:content-box}
.tlrow{display:flex;align-items:center;gap:8px;width:100%;padding:5px 6px;border-radius:8px;font-size:12.5px;color:var(--ink2);transition:background .15s}
.tlrow b{font-weight:600;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.tlrow em{font-style:normal;margin-left:auto;flex:none;font-size:11px;color:var(--faint)}
.tlrow:hover{background:var(--gray)}
.gr{display:flex;gap:10px;margin:0;padding:4px 0;font-size:12.5px;line-height:1.7;color:var(--muted)}
.gr b{flex:none;font-weight:600;color:var(--ink2)}
.gcap{margin:0 0 4px;font-size:11.5px;line-height:1.7;color:var(--faint)}

/* ≤1200px 降级：侧栏隐藏，恢复主列居中的原始布局 */
@media (max-width:1200px){
  .vlib{display:block;padding:0}
  .rail{display:none}
}

.libWrap{max-width:760px;margin:0 auto;padding:34px 20px 80px}
.libHeadRow{display:flex;align-items:center;gap:14px;flex-wrap:wrap;animation:rise .5s var(--ease) both}
.libHeadRow h1{font-size:24px;font-weight:900}
.drop{margin-left:auto;display:inline-flex;align-items:center;gap:10px;border:1.5px dashed #C9CED8;border-radius:10px;background:#fff;padding:9px 16px;font:600 13px 'Noto Sans SC',sans-serif;color:var(--ink2);text-align:left;transition:border-color .2s,background .2s}
.drop:hover,.drop.over{border-color:var(--blue);background:#F5F7FF}
.filePick{position:absolute;width:1px;height:1px;opacity:0;pointer-events:none}
.drop span{display:block;font-size:11px;font-weight:400;color:var(--muted);margin-top:1px}
.libMeta{margin-top:12px;font-size:13px;color:var(--muted);line-height:1.8;animation:rise .5s .06s var(--ease) both}
.uplog{margin-top:14px;font-size:12.5px;color:var(--muted);animation:rise .4s var(--ease)}
.upbar{height:6px;background:var(--gray);border-radius:99px;overflow:hidden;margin-top:7px}
.upbar b{display:block;height:100%;width:0;background:var(--blue);transition:width .18s linear}
.catChips{justify-content:flex-start;margin:20px 0 6px;border-bottom:1px solid var(--line);padding-bottom:14px}
.lgroup h2{font-size:14px;font-weight:800;margin:24px 0 2px;display:flex;align-items:center;gap:8px}
.lgroup h2::before{content:"";width:4px;height:14px;border-radius:2px;background:var(--blue)}
.lgroup h2 span{font-size:12px;color:var(--muted);font-weight:500}
@media (max-width:760px){.libHeadRow .drop{margin-left:0}}
</style>
