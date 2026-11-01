<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ChipsRow from '@/components/ChipsRow.vue'
import NoticeBox from '@/components/NoticeBox.vue'
import ResultItem from '@/components/ResultItem.vue'
import { store } from '@/store/app'
import { useNav } from '@/composables/useNav'
import { STATUTES, SEARCH_EXAMPLES, SEARCH_SUGGESTIONS, statuteDetail } from '@/data/mock'
import { prefersReduced } from '@/utils/env'
import { beginTask } from '@/monitor/metrics'

const route = useRoute()
const router = useRouter()
const { openSearch, openStatute } = useNav()

const inputEl = ref(null)
const input = ref('')
const searching = ref(false)
const elapsed = ref(0)
const results = ref([])
const emptyQ = ref('')
const runSeq = ref(0) // 递增序号：让同批结果的入场动画可重放

const exampleItems = SEARCH_EXAMPLES.map(k => ({ value: k, label: k }))
const suggestionItems = SEARCH_SUGGESTIONS.map(k => ({ value: k, label: k }))

/** 收藏条目 → { id, title, subj } */
const favItems = computed(() => store.favs
  .map(id => statuteDetail(id)?.s)
  .filter(Boolean)
  .map(s => ({ id: s.id, title: `${s.law}${s.art}`, subj: s.subj })))

const fmtTime = at => new Date(at).toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' })

let timer = null
let endPrev = null // 被新检索取代的任务，结束时标记 skipped

/** 演示检索：关键词 × 内置法条关键词表做双向包含匹配并计分 */
function runSearch(q){
  q = (q || '').trim()
  if(!q){ inputEl.value?.focus(); return }
  clearTimeout(timer)
  if(endPrev) endPrev('skipped', '被新检索取代')
  const end = beginTask('search', `「${q}」`)
  endPrev = end
  input.value = q
  store.addHistory(q)
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
    end(hits.length ? 'success' : 'failed', hits.length ? `${hits.length} 条命中` : '未命中')
    endPrev = null
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
      <h1 class="tagline">把生活语言翻译成法律语言 · 条文出处可查</h1>
      <div class="sboxRow">
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
      <div class="panels">
        <!-- 检索历史（持久化） -->
        <div class="panel">
          <h2>检索历史
            <button v-if="store.history.length" class="linklike danger" @click="store.clearHistory()">清空</button>
          </h2>
          <template v-if="store.history.length">
            <div v-for="h in store.history.slice(0, 6)" :key="h.q" class="hrow">
              <button class="hmain" @click="openSearch(h.q)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true"><circle cx="12" cy="12" r="9" stroke="#5F6672" stroke-width="2"/><path d="M12 7v5l3 2" stroke="#5F6672" stroke-width="2" stroke-linecap="round"/></svg>
                <b>{{ h.q }}</b>
              </button>
              <time class="num-tab">{{ fmtTime(h.at) }}</time>
              <button class="del" aria-label="删除该条历史" @click="store.removeHistory(h.q)">×</button>
            </div>
          </template>
          <p v-else class="none">暂无检索记录，试试上方示例关键词</p>
        </div>
        <!-- 我的收藏 -->
        <div class="panel">
          <h2>我的收藏</h2>
          <template v-if="favItems.length">
            <button v-for="f in favItems" :key="f.id" class="frow" @click="openStatute(f.id)">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="#E8A23F" aria-hidden="true"><path d="M12 2.6l2.9 5.9 6.5.95-4.7 4.6 1.1 6.5L12 17.5l-5.8 3.05 1.1-6.5-4.7-4.6 6.5-.95L12 2.6z"/></svg>
              <b>{{ f.title }}</b><em>{{ f.subj }}</em>
            </button>
          </template>
          <p v-else class="none">检索后点击条目右侧 ☆ 即可收藏法条</p>
        </div>
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
.tagline{animation-delay:.02s;margin-top:0;font-weight:500;font-size:14.5px;color:var(--muted);letter-spacing:.05em}
.sboxRow{animation-delay:.08s}
.heroWrap .chips{animation-delay:.14s}
.panels{animation-delay:.2s}

/* 搜索框行：首页居中大框；检索后吸顶窄框（百度 SERP 形态） */
.sboxRow{display:flex;justify-content:center;align-items:center;gap:16px;margin-top:30px}
.sbox{display:flex;width:min(620px,100%)}
.sbox input{flex:1;min-width:0;height:48px;border:2px solid var(--blue);border-right:0;border-radius:10px 0 0 10px;padding:0 16px;font:400 15px 'Noto Sans SC',sans-serif;color:var(--ink);background:#fff;transition:box-shadow .2s}
.sbox input::placeholder{color:var(--faint)}
.sbox input:focus{outline:none;box-shadow:0 0 0 4px rgba(78,110,242,.14)}
.sbox .go{flex:none;width:110px;height:48px;background:var(--blue);color:#fff;font:700 15px 'Noto Sans SC',sans-serif;border-radius:0 10px 10px 0;letter-spacing:.1em;transition:background .2s,transform .15s var(--ease)}
.sbox .go:hover{background:var(--blue-h)}
.sbox .go:active{transform:scale(.97)}

/* 检索后：收起主视觉，搜索框吸顶 */
.compact .heroWrap{padding:0 0 6px}
.compact .tagline,.compact .panels{display:none}
.compact .heroWrap .chips{display:none}
.compact .sboxRow{position:sticky;top:0;z-index:20;background:#fff;padding:12px 20px;border-bottom:1px solid var(--line)}
.compact .sbox input,.compact .sbox .go{height:42px}

/* 检索中加载条（有限进度，非无限循环） */
.loadbar{max-width:620px;margin:16px auto 0;height:4px;border-radius:99px;background:var(--gray);overflow:hidden}
.loadbar i{display:block;height:100%;width:100%;background:var(--blue);border-radius:99px;transform-origin:left;animation:loadgrow .7s var(--ease) forwards}

/* 结果区 */
.serp{display:none;max-width:720px;margin:0 auto;padding:18px 20px 80px}
.compact .serp{display:block}

/* 历史与收藏双面板 */
.panels{max-width:760px;margin:36px auto 0;display:grid;grid-template-columns:1fr 1fr;gap:20px;text-align:left;border-top:1px dashed var(--line);padding-top:18px}
.panel h2{display:flex;align-items:center;font-size:13px;font-weight:600;color:var(--muted);margin-bottom:6px}
.panel h2 .linklike{margin-left:auto;font-size:12px}
.panel h2 .linklike.danger{color:#C63A30}
.hrow{display:flex;align-items:center;gap:6px;padding:2px 0}
.hmain{display:flex;align-items:center;gap:9px;flex:1;min-width:0;text-align:left;font-size:13.5px;padding:7px 10px;border-radius:8px;color:var(--ink2);transition:background .15s}
.hmain:hover{background:var(--gray)}
.hmain svg{flex:none;opacity:.75}
.hmain b{font-weight:500;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.hrow time{flex:none;font-size:11.5px;color:var(--faint)}
.del{flex:none;width:22px;height:22px;border-radius:6px;color:var(--faint);font-size:15px;line-height:1;transition:background .15s,color .15s}
.del:hover{background:rgba(198,58,48,.1);color:#C63A30}
.frow{display:flex;align-items:center;gap:9px;width:100%;text-align:left;font-size:13.5px;padding:8px 10px;border-radius:8px;color:var(--ink2);transition:background .15s}
.frow:hover{background:var(--gray)}
.frow b{font-weight:500}
.frow em{font-style:normal;margin-left:auto;font-size:11.5px;color:var(--faint);overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.none{font-size:13px;color:var(--muted);padding:6px 0}

@media (max-width:760px){
  .heroWrap{padding-top:7vh}
  .tagline{font-size:13px}
  .panels{grid-template-columns:1fr}
  .sbox input{height:44px;font-size:14px;padding:0 12px}
  .sbox .go{width:88px;height:44px;font-size:14px}
  .compact .sbox input,.compact .sbox .go{height:40px}
  .serp{padding:14px 16px 60px}
}
</style>
