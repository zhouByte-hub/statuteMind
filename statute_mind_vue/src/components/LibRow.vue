<script setup>
import { ref, watch } from 'vue'
import { useNav } from '@/composables/useNav'
import { prefersReduced } from '@/utils/env'

const props = defineProps({
  s: { type: Object, required: true },
  index: { type: Number, default: 0 },
  /** 每次递增触发一次「定位 + 蓝底高亮」 */
  flashSeq: { type: Number, default: 0 }
})
const { openSearch, openJudgment } = useNav()
const el = ref(null)

watch(() => props.flashSeq, () => {
  const node = el.value
  if(!node) return
  // 同步移除再立即加回（不经历绘制），保证 flashbg 动画可重复触发且不重放入场动画
  node.classList.remove('flash')
  void node.offsetWidth
  node.classList.add('flash')
  node.scrollIntoView({ block: 'center', behavior: prefersReduced ? 'auto' : 'smooth' })
})
</script>

<template>
  <article ref="el" class="lrow" :style="{ '--i': index }">
    <div class="lt">
      <b>{{ s.law }}{{ s.art }}</b>
      <span class="subj">{{ s.subj }}</span>
      <span class="tag">{{ s.cat }}</span>
      <span v-if="s.tagNew" class="tag amber">新导入 · 演示</span>
    </div>
    <p class="q">{{ s.text }}</p>
    <div class="ract">
      <button v-if="s.jid" class="mini pri" @click="openJudgment(s.jid)">判相关行为</button>
      <button class="mini" @click="openSearch(s.subj)">去检索</button>
    </div>
  </article>
</template>

<style scoped>
.lrow{padding:16px 2px;border-bottom:1px solid var(--line2);animation:rise .45s var(--ease) both;animation-delay:calc(var(--i)*35ms)}
.lrow.flash{animation:flashbg 1.2s var(--ease)}
.lt{display:flex;align-items:center;gap:10px;flex-wrap:wrap}
.lt b{font-size:15.5px;font-weight:700}
.subj{font-size:12.5px;color:var(--muted)}
.q{margin-top:8px;font-size:13.5px;line-height:1.9;color:var(--ink2)}
.ract{display:flex;gap:8px;margin-top:9px}
</style>
