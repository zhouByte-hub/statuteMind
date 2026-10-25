<script setup>
defineProps({
  size: { type: Number, default: 24 },
  /** 首页大 logo 变体：更细描边 + 顶部圆点 */
  hero: { type: Boolean, default: false }
})
</script>

<template>
  <svg :width="size" :height="size" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <circle v-if="hero" cx="12" cy="3.4" r="1.3" fill="#4E6EF2"/>
    <path d="M12 4v15M9 20h6" stroke="#4E6EF2" :stroke-width="hero ? 1.8 : 2" stroke-linecap="round"/>
    <path d="M5 6h14" stroke="#4E6EF2" :stroke-width="hero ? 1.8 : 2" stroke-linecap="round"/>
    <path d="M7 6l-2.8 6a3.2 3.2 0 0 0 5.6 0L7 6z" stroke="#4E6EF2" :stroke-width="hero ? 1.5 : 1.7" stroke-linejoin="round"/>
    <path d="M17 6l-2.8 6a3.2 3.2 0 0 0 5.6 0L17 6z" stroke="#4E6EF2" :stroke-width="hero ? 1.5 : 1.7" stroke-linejoin="round"/>
  </svg>
</template>
