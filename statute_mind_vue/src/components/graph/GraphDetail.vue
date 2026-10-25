<script setup>
import { useNav } from '@/composables/useNav'

defineProps({
  /** buildNodes 产出的图谱节点 */
  node: { type: Object, required: true }
})
const { openLibrary, openSearch } = useNav()
</script>

<template>
  <div class="gdetail">
    <div class="gdhead">
      <span class="gtag" :class="node.cls">{{ node.tag }}</span>
      <b>{{ node.title }}</b>
      <span v-if="node.lib" class="gdact">
        <button class="mini" @click="openLibrary(node.lib)">在知识库查看</button>
        <button class="mini" @click="openSearch(node.q)">去检索</button>
      </span>
    </div>
    <dl class="jrows">
      <div v-for="(r, i) in node.rows" :key="i" class="jrow">
        <dt>{{ r[0] }}</dt>
        <dd>{{ r[1] }}<small v-if="r[2]">{{ r[2] }}</small></dd>
      </div>
    </dl>
  </div>
</template>

<style scoped>
.gdetail{flex:none;margin:6px 28px 24px;border:1px solid var(--line);border-radius:12px;background:#fff;box-shadow:var(--shadow);padding:16px 20px;animation:rise .4s var(--ease)}
.gdhead{display:flex;align-items:center;gap:10px;flex-wrap:wrap;margin-bottom:4px}
.gdhead b{font-size:14.5px;font-weight:800}
.gdact{margin-left:auto;display:flex;gap:8px}
.jrows{margin-top:6px;border-top:1px dashed var(--line)}
.jrow{display:flex;gap:14px;padding:8px 0;border-bottom:1px dashed var(--line2);font-size:13.5px}
.jrow:last-child{border-bottom:0}
dt{flex:none;width:88px;color:var(--muted)}
dd{flex:1;font-weight:600;color:var(--ink)}
dd small{display:block;font-weight:500;font-size:12.5px;color:var(--muted);margin-top:2px}
@media (max-width:960px){.gdetail{margin:16px 16px 0}}
@media (max-width:760px){dt{width:76px}}
</style>
