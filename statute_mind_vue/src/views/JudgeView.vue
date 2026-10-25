<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ChipsRow from '@/components/ChipsRow.vue'
import NoticeBox from '@/components/NoticeBox.vue'
import AnalysisGraph from '@/components/graph/AnalysisGraph.vue'
import GraphDetail from '@/components/graph/GraphDetail.vue'
import { store } from '@/store/app'
import { useNav } from '@/composables/useNav'
import { JUDGMENTS, PRESET_KEYS, JUDGE_EXAMPLES } from '@/data/mock'
import { buildNodes } from '@/utils/graph'
import { prefersReduced } from '@/utils/env'

const route = useRoute()
const router = useRouter()
const { openJudgment } = useNav()

const inputEl = ref(null)
const desc = ref('')
const current = ref(null)   // 命中的判断对象
const notFound = ref('')    // 未收录时的行为描述
const selected = ref('def')

const hasResult = computed(() => !!(current.value || notFound.value))
const headName = computed(() => (current.value ? current.value.name : notFound.value))
const detailNode = computed(() => current.value && buildNodes(current.value)[selected.value])

const scrollTop = () => window.scrollTo({ top: 0, behavior: prefersReduced ? 'auto' : 'smooth' })

function runJudgment(jid){
  const J = JUDGMENTS[jid]
  if(!J) return
  current.value = J
  notFound.value = ''
  desc.value = J.name
  selected.value = 'def'
  scrollTop()
}

function submit(){
  const q = desc.value.trim()
  if(!q){ inputEl.value?.focus(); return }
  const hit = PRESET_KEYS.find(([re]) => new RegExp(re).test(q))
  if(hit){
    route.query.j === hit[1] ? runJudgment(hit[1]) : router.push({ name: 'judge', query: { j: hit[1] } })
    return
  }
  current.value = null
  notFound.value = q
  scrollTop()
}

async function reset(){
  current.value = null
  notFound.value = ''
  desc.value = ''
  if(route.query.j) await router.replace({ name: 'judge' })
  inputEl.value?.focus()
}

/** 路由 ?j= 是判断的唯一事实源；清除参数即回到空态 */
watch(() => route.query.j, j => {
  if(j && JUDGMENTS[j]) runJudgment(j)
  else if(!j){ current.value = null; notFound.value = '' }
}, { immediate: true })

watch(() => store.replay, r => { if(r?.type === 'judge') runJudgment(r.id) })
</script>

<template>
  <section class="vjudge" aria-label="行为判断">
    <div class="judgeWrap">
      <!-- 左：行为输入面板 -->
      <aside class="jleft" aria-label="行为输入面板">
        <h1 class="jleftTitle">行为判断</h1>
        <p class="jleftSub">描述一个行为，即刻生成「行为 → 定性 → 法条 → 处罚 → 风险」分析图谱</p>
        <label class="fieldLabel" for="judgeInput">行为描述</label>
        <textarea
          id="judgeInput" ref="inputEl" v-model="desc" rows="4"
          placeholder="例如：公司拖欠工资 3 个月&#10;也可以描述：网上辱骂他人、高空抛物……"
          aria-label="描述一个行为"
          @keydown="e => { if(e.key === 'Enter' && (e.ctrlKey || e.metaKey)) submit() }"
        ></textarea>
        <div class="jleftAct">
          <button class="goBtn" @click="submit">开始判断</button>
          <span class="kbdHint">Ctrl / ⌘ + Enter 也可提交</span>
        </div>
        <ChipsRow class="jchips" label="示例：" :items="JUDGE_EXAMPLES" align="start" @select="it => openJudgment(it.value)" />
        <p class="jnote">演示数据 · AI 判断仅供参考，不构成法律意见</p>
      </aside>

      <!-- 右：分析图谱 -->
      <section class="jright" aria-label="分析图谱">
        <div v-if="hasResult" class="ghead">
          <b>{{ headName }}</b>
          <span class="demoTag">分析图谱 · 演示数据</span>
          <button class="linklike" @click="reset">重新输入</button>
        </div>

        <AnalysisGraph
          v-if="current"
          :judgment="current"
          :selected="selected"
          @select="selected = $event"
        />

        <div v-if="!hasResult" class="gempty">
          <svg width="38" height="38" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <circle cx="11" cy="6" r="2.4" stroke="#3D5CE5" stroke-width="1.8"/>
            <circle cx="5" cy="18" r="2.4" stroke="#3D5CE5" stroke-width="1.8"/>
            <circle cx="17" cy="18" r="2.4" stroke="#3D5CE5" stroke-width="1.8"/>
            <path d="M9.8 8 6.2 15.8M12.2 8l3.6 7.8M7.4 18h7.2" stroke="#3D5CE5" stroke-width="1.6" stroke-linecap="round"/>
          </svg>
          <b>输入行为，生成分析图谱</b>
          <p>左侧描述一个行为（或点击示例），右侧将生成行为定性、匹配法条、处罚与风险的推理图谱；点击图谱节点可查看结构化详情。</p>
        </div>

        <NoticeBox
          v-else-if="notFound"
          class="jnotice"
          :title="`演示原型暂未收录「${notFound}」的判断路线`"
        >
          <template #text>试试下方示例行为，或先到 <RouterLink class="linklike" :to="{ name: 'search' }">法条检索</RouterLink> 查条文。</template>
          <ChipsRow :items="JUDGE_EXAMPLES" @select="it => openJudgment(it.value)" />
        </NoticeBox>

        <GraphDetail v-if="detailNode" :key="selected" :node="detailNode" aria-live="polite" />
      </section>
    </div>
  </section>
</template>

<style scoped>
.vjudge{flex:1;display:flex;animation:fadein .25s both}
.judgeWrap{flex:1;display:flex;min-width:0}
.jleft{flex:none;width:400px;border-right:1px solid var(--line);padding:30px 26px;display:flex;flex-direction:column}
.jleftTitle{font-size:22px;font-weight:900}
.jleftSub{margin-top:8px;font-size:13px;line-height:1.8;color:var(--muted)}
.fieldLabel{margin-top:26px;font-size:12.5px;font-weight:600;color:var(--muted)}
textarea{margin-top:8px;width:100%;resize:vertical;min-height:96px;border:1.5px solid #C9CED8;border-radius:10px;padding:12px 14px;font:400 14px/1.7 'Noto Sans SC',sans-serif;color:var(--ink);background:#fff;transition:border-color .2s,box-shadow .2s}
textarea:focus{outline:none;border-color:var(--blue);box-shadow:0 0 0 3px rgba(61,92,229,.14)}
textarea::placeholder{color:var(--faint)}
.jleftAct{margin-top:12px;display:flex;align-items:center;gap:12px;flex-wrap:wrap}
.goBtn{flex:none;height:42px;padding:0 26px;background:var(--blue);color:#fff;border-radius:9px;font:700 14.5px 'Noto Sans SC',sans-serif;letter-spacing:.06em;transition:background .2s,transform .15s var(--ease)}
.goBtn:hover{background:var(--blue-h)}
.goBtn:active{transform:scale(.97)}
.kbdHint{font-size:12px;color:var(--faint)}
.jchips{justify-content:flex-start;margin-top:24px}
.jnote{margin-top:auto;padding-top:22px;font-size:12px;color:var(--muted);line-height:1.8}

.jright{flex:1;min-width:0;display:flex;flex-direction:column}
.ghead{display:flex;align-items:center;gap:12px;padding:18px 28px 0;flex-wrap:wrap;animation:rise .4s var(--ease)}
.ghead b{font-size:15.5px;font-weight:800}
.jright .jnotice{margin:24px 28px}
.gempty{margin:auto;max-width:440px;text-align:center;padding:40px 20px;animation:rise .45s var(--ease)}
.gempty svg{margin-bottom:12px}
.gempty b{display:block;font-size:16px;margin-bottom:8px}
.gempty p{font-size:13px;color:var(--muted);line-height:1.9}

@media (max-width:960px){
  .judgeWrap{flex-direction:column}
  .jleft{width:auto;border-right:0;border-bottom:1px solid var(--line);padding:22px 16px}
  .fieldLabel{margin-top:20px}
  .jnote{margin-top:18px;padding-top:0}
  .jright{padding-bottom:26px}
  .ghead{padding:16px 16px 0}
  .jright .jnotice{margin:16px}
}
</style>
