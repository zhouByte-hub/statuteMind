<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const VIEW_NAMES = { search: '法条检索', judge: '行为判断', library: '知识库' }
const current = computed(() => VIEW_NAMES[route.name] || '法条检索')
</script>

<template>
  <footer>
    <p><b>演示数据，仅供参考，不构成法律意见</b> · statuteMind 原型 · 当前：{{ current }}模式</p>
  </footer>
</template>

<style scoped>
footer{flex:none;padding:26px 20px 36px;text-align:center;font-size:12.5px;color:var(--muted);line-height:1.9}
footer b{color:var(--ink);font-weight:600}
</style>
