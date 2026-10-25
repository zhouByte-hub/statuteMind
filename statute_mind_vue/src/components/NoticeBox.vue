<script setup>
defineProps({
  title: { type: String, required: true },
  text: { type: String, default: '' }
})
</script>

<template>
  <div class="notice">
    <slot name="icon">
      <svg width="34" height="34" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <circle cx="10.5" cy="10.5" r="6.5" stroke="#3D5CE5" stroke-width="2"/>
        <path d="M15.5 15.5 21 21" stroke="#3D5CE5" stroke-width="2" stroke-linecap="round"/>
        <path d="M7.5 10.5h6" stroke="#3D5CE5" stroke-width="1.8" stroke-linecap="round"/>
      </svg>
    </slot>
    <b>{{ title }}</b>
    <p v-if="text || $slots.text"><slot name="text">{{ text }}</slot></p>
    <slot />
  </div>
</template>
