<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BrandMark from '@/components/BrandMark.vue'
import ChipsRow from '@/components/ChipsRow.vue'
import NoticeBox from '@/components/NoticeBox.vue'
import ResultItem from '@/components/ResultItem.vue'
import { store } from '@/store/app'
import { useNav } from '@/composables/useNav'
import { STATUTES, SEARCH_EXAMPLES, SEARCH_SUGGESTIONS } from '@/data/mock'
import { prefersReduced } from '@/utils/env'

const route = useRoute()
const router = useRouter()
const { openSearch } = useNav()

const inputEl = ref(null)
const input = ref('')
const searching = ref(false)
const elapsed = ref(0)
const results = ref([])
const emptyQ = ref('')
const runSeq = ref(0) // 递增序号：让同批结果的入场动画可重放

const exampleItems = SEARCH_EXAMPLES.map(k => ({ value: k, label: k }))
const suggestionItems = SEARCH_SUGGESTIONS.map(k => ({ value: k, label: k }))

let timer = null

/** 演示检索：关键词 × 内置法条关键词表做双向包含匹配并计分 */
function runSearch(q){
  q = (q || '').trim()
  if(!q){ inputEl.value?.focus(); return }
  input.value = q
  store.addRecent(q)
  searching.value = true
  results.value = []
  emptyQ.value = ''
  elapsed.value = (q.length * 37 % 280 + 140) / 1000
  runSeq.value++
  const hits = STATUTES
    .map(s => {
      const mk = s.kws.filter(k => q.includes(k) || k.includes(q))
      return mk.length
        ? { s, mk, score: Math.min(97, 84 + 6 * Math.min(mk.length, 2) + (q.includes(s.subj.slice(0, 2)) ? 4 : 0)) }
        : null
    })
    .filter(Boolean)
    .sort((a, b) => b.score - a.score)
  clearTimeout(timer)
  timer = setTimeout(() => {
    searching.value = false
    if(hits.length) results.value = hits
    else emptyQ.value = q
  }, prefersReduced ? 0 : 700)
}

function submit(){
  const q = input.value.trim()
  if(!q){ inputEl.value?.focus(); return }
  // 同词重查时路由不变，直接重放
  route.query.q === q ? runSearch(q) : router.push({ name: 'search', query: { q } })
}

async function resetSearch(){
  await router.replace({ name: 'search' })
  input.value = ''
  window.scrollTo({ top: 0, behavior: prefersReduced ? 'auto' : 'smooth' })
  inputEl.value?.focus()
}

/** 路由 ?q= 是检索的唯一事实源（深链 / 返回 / 重放共用） */
watch(() => route.query.q, q => {
  if(q) runSearch(q)
  else {
    clearTimeout(timer)
    searching.value = false
    results.value = []
    emptyQ.value = ''
  }
}, { immediate: true })

watch(() => store.replay, r => { if(r?.type === 'search') runSearch(r.id) })
</script>

<template>
  <section class="vsearch" :class="{ compact: !!route.query.q }" aria-label="法条检索">
    <div class="heroWrap">
      <div class="brandBig">
        <BrandMark :size="72" hero />
        <h1>statute<em>Mind</em></h1>
        <p class="tagline">把生活语言翻译成法律语言 · 条文出处可查</p>
      </div>
      <div class="modeTabs" aria-label="检索模式">
        <button class="on" aria-current="true">搜法条</button>
        <button @click="router.push({ name: 'judge' })">判行为</button>
      </div>
      <div class="sboxRow">
        <RouterLink class="serpLogo" :to="{ name: 'search' }" aria-label="返回 statuteMind 首页">
          <BrandMark :size="20" />
          <span>statute<em>Mind</em></span>
        </RouterLink>
        <div class="sbox">
          <input
            ref="inputEl" v-model="input" type="text"
            placeholder="输入关键词，如：拖欠工资" aria-label="法条关键词"
            @keydown.enter="submit"
          >
          <button class="go" @click="submit">搜法条</button>
        </div>
      </div>
      <ChipsRow label="示例：" :items="exampleItems" @select="it => openSearch(it.value)" />
      <div class="recent">
        <h2>最近检索</h2>
        <template v-if="store.recent.length">
          <button v-for="r in store.recent" :key="r" class="rrow" @click="openSearch(r)">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true"><circle cx="12" cy="12" r="9" stroke="#5F6672" stroke-width="2"/><path d="M12 7v5l3 2" stroke="#5F6672" stroke-width="2" stroke-linecap="round"/></svg>
            <b>{{ r }}</b><em>重新检索</em>
          </button>
        </template>
        <p v-else class="none">暂无检索记录，试试上方示例关键词</p>
      </div>
    </div>

    <div class="loadbar" v-show="searching" aria-hidden="true"><i></i></div>

    <div class="serp">
      <p class="meta" role="status">
        <template v-if="searching">关键词「{{ input }}」 · 正在检索演示法条库…</template>
        <template v-else-if="emptyQ">
          关键词「{{ emptyQ }}」 · 演示数据
          <button class="linklike" @click="resetSearch">重新输入</button>
        </template>
        <template v-else-if="results.length">关键词「{{ input }}」 · 命中 {{ results.length }} 条 · 演示检索 {{ elapsed.toFixed(2) }} s · 演示数据</template>
      </p>
      <NoticeBox
        v-if="emptyQ"
        :title="`演示库未收录「${emptyQ}」`"
        text="原型只内置部分常见场景法条。换个更具体的关键词，或试试："
      >
        <ChipsRow :items="suggestionItems" @select="it => openSearch(it.value)" />
      </NoticeBox>
      <ResultItem v-for="h in results" :key="`${runSeq}-${h.s.id}`" :hit="h" :index="results.indexOf(h)" />
    </div>
  </section>
</template>

<style scoped>
.vsearch{display:block;animation:fadein .25s both}
.heroWrap{max-width:880px;margin:0 auto;padding:clamp(48px,10vh,110px) 20px 26px;text-align:center}
.heroWrap>*{animation:rise .55s var(--ease) both}
.brandBig{animation-delay:.02s}
.modeTabs{animation-delay:.08s}
.sboxRow{animation-delay:.14s}
.heroWrap .chips{animation-delay:.2s}
.recent{animation-delay:.26s}
.brandBig svg{width:72px;height:72px}
.brandBig h1{font-size:50px;font-weight:900;letter-spacing:.01em;margin-top:14px;color:var(--ink)}
.brandBig h1 em{font-style:normal;color:var(--blue)}
.tagline{margin-top:12px;font-size:14.5px;color:var(--muted);letter-spacing:.05em}
.modeTabs{display:inline-flex;gap:30px;margin:36px auto 16px}
.modeTabs button{font-size:15px;color:var(--muted);padding:4px 2px;border-bottom:2px solid transparent;transition:color .2s}
.modeTabs button:hover{color:var(--ink)}
.modeTabs button.on{color:var(--blue);font-weight:700;border-color:var(--blue)}

/* 搜索框行：首页居中大框；检索后吸顶窄框（百度 SERP 形态） */
.sboxRow{display:flex;justify-content:center;align-items:center;gap:16px}
.serpLogo{display:none;align-items:center;gap:7px;text-decoration:none;color:var(--ink);font-weight:900;font-size:17px;white-space:nowrap}
.serpLogo em{color:var(--blue);font-style:normal}
.sbox{display:flex;width:min(620px,100%)}
.sbox input{flex:1;min-width:0;height:48px;border:2px solid var(--blue);border-right:0;border-radius:10px 0 0 10px;padding:0 16px;font:400 15px 'Noto Sans SC',sans-serif;color:var(--ink);background:#fff;transition:box-shadow .2s}
.sbox input::placeholder{color:var(--faint)}
.sbox input:focus{outline:none;box-shadow:0 0 0 4px rgba(78,110,242,.14)}
.sbox .go{flex:none;width:110px;height:48px;background:var(--blue);color:#fff;font:700 15px 'Noto Sans SC',sans-serif;border-radius:0 10px 10px 0;letter-spacing:.1em;transition:background .2s,transform .15s var(--ease)}
.sbox .go:hover{background:var(--blue-h)}
.sbox .go:active{transform:scale(.97)}

/* 检索后：收起主视觉，搜索框吸顶 */
.compact .heroWrap{padding:0 0 6px}
.compact .brandBig,.compact .modeTabs,.compact .recent{display:none}
.compact .heroWrap .chips{display:none}
.compact .sboxRow{position:sticky;top:0;z-index:20;background:#fff;padding:12px 20px;border-bottom:1px solid var(--line)}
.compact .serpLogo{display:inline-flex}
.compact .sbox input,.compact .sbox .go{height:42px}

/* 检索中加载条（有限进度，非无限循环） */
.loadbar{max-width:620px;margin:16px auto 0;height:4px;border-radius:99px;background:var(--gray);overflow:hidden}
.loadbar i{display:block;height:100%;width:100%;background:var(--blue);border-radius:99px;transform-origin:left;animation:loadgrow .7s var(--ease) forwards}

/* 结果区 */
.serp{display:none;max-width:720px;margin:0 auto;padding:18px 20px 80px}
.compact .serp{display:block}

/* 最近检索 */
.recent{max-width:620px;margin:36px auto 0;text-align:left;border-top:1px dashed var(--line);padding-top:18px}
.recent h2{font-size:13px;font-weight:600;color:var(--muted);margin-bottom:6px}
.rrow{display:flex;align-items:center;gap:9px;width:100%;text-align:left;font-size:13.5px;padding:8px 10px;border-radius:8px;color:var(--ink2);transition:background .15s}
.rrow:hover{background:var(--gray)}
.rrow svg{flex:none;opacity:.75}
.rrow b{font-weight:500}
.rrow em{font-style:normal;margin-left:auto;font-size:11.5px;color:var(--faint)}
.none{font-size:13px;color:var(--muted);padding:6px 0}

@media (max-width:760px){
  .heroWrap{padding-top:7vh}
  .brandBig svg{width:54px;height:54px}
  .brandBig h1{font-size:38px}
  .tagline{font-size:13px}
  .sbox input{height:44px;font-size:14px;padding:0 12px}
  .sbox .go{width:88px;height:44px;font-size:14px}
  .compact .sbox input,.compact .sbox .go{height:40px}
  .serpLogo span{display:none}
  .serp{padding:14px 16px 60px}
}
</style>
