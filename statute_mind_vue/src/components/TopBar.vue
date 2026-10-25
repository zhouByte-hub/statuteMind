<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import BrandMark from '@/components/BrandMark.vue'
import { store } from '@/store/app'

const route = useRoute()
const NAVS = [
  { name: 'search', label: '法条检索' },
  { name: 'judge', label: '行为判断' },
  { name: 'library', label: '知识库' }
]

/** SERP 形态（检索结果态）下顶栏出现小品牌 */
const showBrand = computed(() => route.name === 'search' && !!route.query.q)

/** 上传入库后徽标跳动一次 */
const ticked = ref(false)
watch(() => store.statuteCount, () => {
  ticked.value = false
  requestAnimationFrame(() => { ticked.value = true })
})
</script>

<template>
  <header class="top">
    <RouterLink class="brand" :class="{ show: showBrand }" :to="{ name: 'search' }" aria-label="statuteMind 首页">
      <BrandMark :size="24" />
      statute<em>Mind</em>
    </RouterLink>
    <nav aria-label="模式导航">
      <RouterLink v-for="n in NAVS" :key="n.name" :to="{ name: n.name }" :class="{ on: route.name === n.name }">{{ n.label }}</RouterLink>
    </nav>
    <span class="badge" :class="{ tick: ticked }">
      <span class="dot" aria-hidden="true"></span>
      演示数据 · 内置 <b class="num-tab">{{ store.statuteCount.toLocaleString() }}</b> 条法条
    </span>
  </header>
</template>

<style scoped>
.top{flex:none;height:60px;display:flex;align-items:center;gap:28px;padding:0 28px;border-bottom:1px solid var(--line2)}
.brand{display:none;align-items:center;gap:8px;text-decoration:none;color:var(--ink);font-weight:900;font-size:19px;white-space:nowrap}
.brand.show{display:inline-flex}
.brand em{color:var(--blue);font-style:normal}
nav{display:flex;gap:26px}
nav a{font-size:14px;color:var(--ink2);text-decoration:none;padding:19px 2px 17px;border-bottom:2px solid transparent;transition:color .2s,border-color .2s;white-space:nowrap}
nav a:hover{color:var(--blue)}
nav a.on{color:var(--blue);font-weight:700;border-color:var(--blue)}
.badge{margin-left:auto;display:inline-flex;align-items:center;gap:7px;font-size:12px;color:var(--muted);border:1px solid var(--line);border-radius:999px;padding:4px 12px;background:var(--gray);white-space:nowrap}
.badge .dot{width:6px;height:6px;border-radius:50%;background:var(--blue)}
.badge b{color:var(--blue)}
.badge.tick{animation:tick .5s var(--ease)}
@media (max-width:760px){
  .top{padding:0 14px;gap:14px}
  nav{gap:14px}
  nav a{padding:19px 0 17px;font-size:13px}
  .badge{display:none}
}
</style>
