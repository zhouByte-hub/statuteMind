<script setup>
import TopBar from '@/components/TopBar.vue'
import SiteFooter from '@/components/SiteFooter.vue'
import AiLawyer from '@/components/AiLawyer.vue'
</script>

<template>
  <TopBar />
  <main><RouterView /></main>
  <SiteFooter />
  <AiLawyer />
</template>

<style scoped>
main{flex:1;display:flex;flex-direction:column;min-width:0}
</style>
