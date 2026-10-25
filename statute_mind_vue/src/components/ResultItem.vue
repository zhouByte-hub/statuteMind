<script setup>
import { computed } from 'vue'
import { highlight } from '@/utils/format'
import { useNav } from '@/composables/useNav'

const props = defineProps({
  /** { s: 法条, mk: 命中关键词, score: 相关度 } */
  hit: { type: Object, required: true },
  index: { type: Number, default: 0 }
})
const { openLibrary, openJudgment } = useNav()
const quoteHtml = computed(() => highlight(props.hit.s.text, props.hit.mk))
</script>

<template>
  <article class="r" :style="{ '--i': index }">
    <h3>
      <button class="rtitle" :aria-label="`在知识库查看 ${hit.s.law}${hit.s.art}`" @click="openLibrary(hit.s.id)">
        {{ hit.s.law }}{{ hit.s.art }}<span class="rsubj">（{{ hit.s.subj }}）</span>
      </button>
    </h3>
    <div class="rsrc">
      <span class="tag">{{ hit.s.cat }}</span>
      <span>演示法条库 · 精选节引</span>
      <span class="rscore num-tab">相关度 {{ hit.score }}%</span>
    </div>
    <!-- 条文摘引：highlight 内部已转义，受控 HTML -->
    <p class="rquote" v-html="quoteHtml"></p>
    <div class="ract">
      <button v-if="hit.s.jid" class="mini pri" @click="openJudgment(hit.s.jid)">判相关行为</button>
      <button class="mini" @click="openLibrary(hit.s.id)">在知识库查看</button>
    </div>
  </article>
</template>

<style scoped>
.r{margin-bottom:30px;animation:rise .5s var(--ease) both;animation-delay:calc(var(--i)*90ms)}
h3{font-size:16.5px;font-weight:600;line-height:1.5}
.rtitle{color:var(--link);text-align:left;font-size:inherit;font-weight:inherit;line-height:inherit}
.rtitle:hover{text-decoration:underline}
.rsubj{color:var(--muted);font-size:13.5px;font-weight:500}
.rsrc{display:flex;align-items:center;gap:10px;margin-top:5px;font-size:12.5px;color:var(--muted);flex-wrap:wrap}
.rscore{margin-left:auto}
.rquote{margin-top:8px;font-size:14px;line-height:1.85;color:var(--ink2)}
.ract{display:flex;gap:8px;margin-top:9px}
</style>
