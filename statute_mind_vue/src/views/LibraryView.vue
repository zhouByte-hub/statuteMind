<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ChipsRow from '@/components/ChipsRow.vue'
import LibRow from '@/components/LibRow.vue'
import { store } from '@/store/app'
import { useNav } from '@/composables/useNav'
import { CATS, STATUTES } from '@/data/mock'
import { prefersReduced } from '@/utils/env'

const route = useRoute()
const router = useRouter()
const { openSearch } = useNav()

const over = ref(false)
const uploading = ref(false)
const upName = ref('')
const upP = ref(0)
const upTxt = ref('0%')
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
        <h1>知识库 <span class="demoTag">演示数据</span></h1>
        <button
          class="drop" :class="{ over }" aria-label="上传文档扩充知识库（模拟流程）"
          @click="doUpload('演示文件_劳动争议案例汇编.pdf')"
          @dragover.prevent="over = true" @dragenter.prevent="over = true"
          @dragleave.prevent="over = false" @drop.prevent="onDrop"
        >
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M12 16V5m0 0 4 4m-4-4-4 4" stroke="#4E6EF2" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M4 15v3a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-3" stroke="#4E6EF2" stroke-width="2" stroke-linecap="round"/>
          </svg>
          <span>上传文档，扩充知识库<span>点击选择或拖入文件 · 模拟流程</span></span>
        </button>
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
  </section>
</template>

<style scoped>
.vlib{display:block;animation:fadein .25s both}
.libWrap{max-width:760px;margin:0 auto;padding:34px 20px 80px}
.libHeadRow{display:flex;align-items:center;gap:14px;flex-wrap:wrap;animation:rise .5s var(--ease) both}
.libHeadRow h1{font-size:24px;font-weight:900}
.drop{margin-left:auto;display:inline-flex;align-items:center;gap:10px;border:1.5px dashed #C9CED8;border-radius:10px;background:#fff;padding:9px 16px;font:600 13px 'Noto Sans SC',sans-serif;color:var(--ink2);text-align:left;transition:border-color .2s,background .2s}
.drop:hover,.drop.over{border-color:var(--blue);background:#F5F7FF}
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
