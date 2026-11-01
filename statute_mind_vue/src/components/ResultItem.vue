<script setup>
import { computed } from 'vue'
import { highlight } from '@/utils/format'
import { useNav } from '@/composables/useNav'
import { store } from '@/store/app'

const props = defineProps({
  /** { s: 法条, mk: 命中关键词, score: 相关度 } */
  hit: { type: Object, required: true },
  index: { type: Number, default: 0 }
})
const { openStatute, openJudgment } = useNav()
const quoteHtml = computed(() => highlight(props.hit.s.text, props.hit.mk))
const fav = computed(() => store.isFav(props.hit.s.id))
</script>

<template>
  <article class="r" :style="{ '--i': index }">
    <h3>
      <button class="rtitle" :aria-label="`查看 ${hit.s.law}${hit.s.art} 条文详情`" @click="openStatute(hit.s.id)">
        {{ hit.s.law }}{{ hit.s.art }}<span class="rsubj">（{{ hit.s.subj }}）</span>
      </button>
      <button class="star" :class="{ on: fav }" :aria-label="fav ? '取消收藏' : '收藏该法条'"
        :title="fav ? '取消收藏' : '收藏'" @click="store.toggleFav(hit.s.id)">
        <svg width="16" height="16" viewBox="0 0 24 24" :fill="fav ? '#E8A23F' : 'none'" aria-hidden="true">
          <path d="M12 2.6l2.9 5.9 6.5.95-4.7 4.6 1.1 6.5L12 17.5l-5.8 3.05 1.1-6.5-4.7-4.6 6.5-.95L12 2.6z" :stroke="fav ? '#E8A23F' : '#8A93A3'" stroke-width="1.8" stroke-linejoin="round"/>
        </svg>
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
      <button class="mini" @click="openStatute(hit.s.id)">查看条文详情</button>
    </div>
  </article>
</template>

<style scoped>
.r{margin-bottom:30px;animation:rise .5s var(--ease) both;animation-delay:calc(var(--i)*90ms)}
h3{font-size:16.5px;font-weight:600;line-height:1.5;display:flex;align-items:flex-start;gap:6px}
.rtitle{flex:1;color:var(--link);text-align:left;font-size:inherit;font-weight:inherit;line-height:inherit}
.rtitle:hover{text-decoration:underline}
.rsubj{color:var(--muted);font-size:13.5px;font-weight:500}
.star{flex:none;padding:2px;border-radius:6px;line-height:0;transition:transform .15s var(--ease)}
.star:hover{transform:scale(1.15)}
.star.on svg{filter:drop-shadow(0 1px 2px rgba(232,162,63,.4))}
.rsrc{display:flex;align-items:center;gap:10px;margin-top:5px;font-size:12.5px;color:var(--muted);flex-wrap:wrap}
.rscore{margin-left:auto}
.rquote{margin-top:8px;font-size:14px;line-height:1.85;color:var(--ink2)}
.ract{display:flex;gap:8px;margin-top:9px}
</style>
