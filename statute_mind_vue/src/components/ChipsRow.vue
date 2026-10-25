<script setup>
defineProps({
  label: { type: String, default: '' },
  /** [{ value, label, count?, active? }] */
  items: { type: Array, required: true },
  align: { type: String, default: 'center' }
})
defineEmits(['select'])
</script>

<template>
  <div class="chips" :class="{ start: align === 'start' }" :aria-label="label || undefined">
    <span v-if="label" class="chipLabel">{{ label }}</span>
    <button
      v-for="it in items" :key="it.value" class="chip"
      :class="{ on: it.active }" :aria-pressed="it.active"
      @click="$emit('select', it)"
    >{{ it.label }}<span v-if="it.count != null" class="n num-tab">{{ it.count }}</span></button>
  </div>
</template>

<style scoped>
.chips.start{justify-content:flex-start}
</style>
