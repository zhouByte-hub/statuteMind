<script setup>
/** 指标数值卡片。status: ok|warn（超阈值红色高亮） */
defineProps({
  label: { type: String, required: true },
  value: { type: [Number, String], default: '—' },
  unit: { type: String, default: '' },
  sub: { type: String, default: '' },
  status: { type: String, default: 'ok' }
})
</script>

<template>
  <div class="mcard" :class="{ warn: status === 'warn' }" role="status">
    <span class="lb">{{ label }}</span>
    <span class="val num-tab">{{ value }}<small v-if="unit">{{ unit }}</small></span>
    <span class="sub">{{ sub }}</span>
  </div>
</template>

<style scoped>
.mcard{display:flex;flex-direction:column;gap:4px;background:#fff;border:1px solid var(--line);border-radius:12px;padding:14px 16px;min-width:0;transition:border-color .3s,box-shadow .3s}
.lb{font-size:12.5px;color:var(--muted)}
.val{font-size:24px;font-weight:800;color:var(--ink);line-height:1.2;white-space:nowrap}
.val small{font-size:12.5px;font-weight:600;color:var(--muted);margin-left:3px}
.sub{font-size:11.5px;color:var(--faint);white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.mcard.warn{border-color:var(--red);background:#FDF3F2;box-shadow:0 0 0 3px rgba(198,58,48,.08)}
.mcard.warn .val{color:var(--red)}
</style>
