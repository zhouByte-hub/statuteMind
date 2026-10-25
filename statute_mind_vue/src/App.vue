<script setup>
import TopBar from '@/components/TopBar.vue'
import SiteFooter from '@/components/SiteFooter.vue'
</script>

<template>
  <TopBar />
  <main><RouterView /></main>
  <SiteFooter />
</template>

<style scoped>
main{flex:1;display:flex;flex-direction:column;min-width:0}
</style>
