<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import TopBar from '@/components/TopBar.vue'
import SiteFooter from '@/components/SiteFooter.vue'
import AiLawyer from '@/components/AiLawyer.vue'
import { prefersReduced } from '@/utils/env'

/** 顶部导航顺序：向右切换新页从右滑入，向左切换从左滑入（PPT 式） */
const ORDER = ['search', 'judge', 'library', 'workflow', 'workflow-edit', 'monitor']
const idx = n => (n === 'workflow-edit' ? 3 : ORDER.indexOf(n))

const route = useRoute()
const tname = ref('')
watch(() => route.name, (n, o) => {
  const a = idx(n), b = idx(o)
  if(a < 0 || b < 0 || a === b || prefersReduced){ tname.value = ''; return }
  tname.value = a > b ? 'slide-next' : 'slide-prev'
})
</script>

<template>
  <TopBar />
  <main>
    <RouterView v-slot="{ Component }">
      <Transition :name="tname">
        <component :is="Component" :key="route.name" />
      </Transition>
    </RouterView>
  </main>
  <SiteFooter />
  <AiLawyer />
</template>

<style scoped>
main{flex:1;display:flex;flex-direction:column;min-width:0;position:relative;overflow-x:clip}
</style>

<!-- 全局作用域：过渡类挂在各视图根元素上，scoped 会因作用域属性不匹配而失效 -->
<style>
/* PPT 推拉式滑动：新旧两页同时位移（旧页滑出、新页同步滑入），连贯无空档 */
.slide-next-enter-active,.slide-next-leave-active,
.slide-prev-enter-active,.slide-prev-leave-active{transition:transform .42s cubic-bezier(.45,.05,.25,1);will-change:transform}
.slide-next-enter-from{transform:translateX(100%)}
.slide-next-leave-to{transform:translateX(-100%)}
.slide-prev-enter-from{transform:translateX(-100%)}
.slide-prev-leave-to{transform:translateX(100%)}
/* 离场页脱离文档流并铺满 main，与新页并排滑动 */
.slide-next-leave-active,.slide-prev-leave-active{position:absolute;top:0;left:0;right:0}
</style>
