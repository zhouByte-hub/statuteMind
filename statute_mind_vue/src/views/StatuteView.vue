<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { statuteDetail } from '@/data/mock'
import { store } from '@/store/app'
import { useNav } from '@/composables/useNav'

const route = useRoute()
const { openStatute, openJudgment, goHome } = useNav()

const detail = ref(null)
const notFound = ref(false)
const fav = ref(false)

/** 加载条文详情；未知 id 显示 404 态 */
function load(id){
  const d = statuteDetail(id)
  notFound.value = !d
  detail.value = d
  fav.value = d ? store.isFav(d.s.id) : false
  if(d) document.title = `${d.s.law}${d.s.art} · StatuteMind`
}

watch(() => route.params.id, id => { if(route.name === 'statute') load(id) }, { immediate: true })
watch(() => store.replay, r => { if(r?.type === 'statute') load(r.id) })

function toggleFav(){
  fav.value = store.toggleFav(detail.value.s.id)
}

const isExcerpt = computed(() => detail.value?.s.text.includes('节引'))
</script>

<template>
  <section class="sdet" aria-label="法条详情">
    <!-- 面包屑 -->
    <nav class="crumb" aria-label="面包屑">
      <button class="linklike" @click="goHome">法条检索</button>
      <span class="sep">/</span>
      <b>条文详情</b>
    </nav>

    <!-- 未知条文 -->
    <div v-if="notFound" class="missing">
      <h1>未找到该条文</h1>
      <p>链接指向的条文不在演示法条库中，可能已被移除或链接有误。</p>
      <button class="goSearch" @click="goHome">返回检索</button>
    </div>

    <template v-else-if="detail">
      <!-- 标题区 -->
      <header class="dhead">
        <div class="ttlRow">
          <h1>{{ detail.s.law }}<span class="art">{{ detail.s.art }}</span></h1>
          <button class="star" :class="{ on: fav }" @click="toggleFav"
            :aria-label="fav ? '取消收藏该法条' : '收藏该法条'">
            <svg width="20" height="20" viewBox="0 0 24 24" :fill="fav ? '#E8A23F' : 'none'" aria-hidden="true">
              <path d="M12 2.6l2.9 5.9 6.5.95-4.7 4.6 1.1 6.5L12 17.5l-5.8 3.05 1.1-6.5-4.7-4.6 6.5-.95L12 2.6z" :stroke="fav ? '#E8A23F' : '#8A93A3'" stroke-width="1.8" stroke-linejoin="round"/>
            </svg>
            <span>{{ fav ? '已收藏' : '收藏' }}</span>
          </button>
        </div>
        <div class="tags">
          <span class="tag cat">{{ detail.s.cat }}</span>
          <span class="tag" :class="detail.meta.status === '现行有效' ? 'ok' : 'warn'">{{ detail.meta.status }}</span>
          <span v-if="isExcerpt" class="tag exc">节引</span>
        </div>
      </header>

      <!-- 元信息 -->
      <div class="metaCard" role="table" aria-label="条文元信息">
        <div class="mrow"><span class="k">制定机关</span><b>{{ detail.meta.org }}</b></div>
        <div class="mrow"><span class="k">通过日期</span><b class="num-tab">{{ detail.meta.passed }}</b></div>
        <div class="mrow"><span class="k">施行日期</span><b class="num-tab">{{ detail.meta.effect }}</b></div>
        <div class="mrow"><span class="k">条文主旨</span><b>{{ detail.s.subj }}</b></div>
      </div>

      <!-- 条文全文 -->
      <div class="blk">
        <h2>条文内容</h2>
        <blockquote class="lawText">{{ detail.s.text }}</blockquote>
        <p v-if="isExcerpt" class="excNote">* 本条为演示库节引，非完整条文；实际产品中将展示官方公布全文。</p>
      </div>

      <!-- 版本沿革 -->
      <div v-if="detail.meta.versions.length" class="blk">
        <h2>版本沿革</h2>
        <ol class="vline">
          <li v-for="(v, i) in detail.meta.versions" :key="i" :class="{ cur: i === 0 }">
            <i class="dot" aria-hidden="true"></i>
            <div class="vbody">
              <b>{{ v.v }}</b><time class="num-tab">{{ v.date }}</time>
              <p>{{ v.note }}</p>
            </div>
          </li>
        </ol>
      </div>

      <!-- 关联法条 -->
      <div v-if="detail.rel.length" class="blk">
        <h2>关联法条 <em>{{ detail.rel.length }} 条 · 同部法律与关联行为</em></h2>
        <div class="relGrid">
          <button v-for="r in detail.rel" :key="r.id" class="relCard" @click="openStatute(r.id)">
            <b>{{ r.law }}{{ r.art }}</b>
            <span class="rsubj">{{ r.subj }} · {{ r.cat }}</span>
            <p>{{ r.text.length > 52 ? r.text.slice(0, 52) + '……' : r.text }}</p>
          </button>
        </div>
      </div>

      <!-- 行为入口 -->
      <div v-if="detail.s.jid" class="blk">
        <h2>场景联动</h2>
        <div class="linkRow">
          <button class="goJudge" @click="openJudgment(detail.s.jid)">用「行为判断」分析相关行为 →</button>
        </div>
      </div>

      <p class="disclaimer">本页内容为演示数据，仅供参考，不构成法律意见。</p>
    </template>
  </section>
</template>

<style scoped>
.sdet{max-width:820px;margin:0 auto;padding:26px 20px 80px;display:block;animation:fadein .25s both}

/* 面包屑 */
.crumb{display:flex;align-items:center;gap:8px;font-size:13px;color:var(--muted);margin-bottom:18px}
.crumb .linklike{font-size:13px}
.crumb .sep{color:var(--faint)}
.crumb b{color:var(--ink2);font-weight:600}

/* 标题 */
.dhead .ttlRow{display:flex;align-items:center;gap:14px;flex-wrap:wrap}
.dhead h1{margin:0;font-size:24px;font-weight:800;color:var(--ink);line-height:1.4}
.dhead .art{margin-left:4px}
.tags{display:flex;gap:8px;margin-top:10px;flex-wrap:wrap}
.tag{font-size:12px;padding:3px 10px;border-radius:999px;background:var(--gray);color:var(--ink2);border:1px solid var(--line)}
.tag.cat{color:var(--blue);border-color:rgba(78,110,242,.35);background:rgba(78,110,242,.08)}
.tag.ok{color:#2E9E5B;border-color:rgba(46,158,91,.4);background:rgba(46,158,91,.08)}
.tag.warn{color:#C63A30;border-color:rgba(198,58,48,.4);background:rgba(198,58,48,.08)}
.tag.exc{color:#B07514;border-color:rgba(176,117,20,.4);background:rgba(176,117,20,.08)}

/* 收藏 */
.star{display:inline-flex;align-items:center;gap:6px;font-size:13px;color:var(--ink2);border:1px solid var(--line);border-radius:999px;padding:6px 14px;background:#fff;transition:border-color .2s,transform .15s var(--ease)}
.star:hover{border-color:#E8A23F;transform:scale(1.04)}
.star.on{border-color:#E8A23F;color:#B07514;background:rgba(232,162,63,.08)}

/* 元信息卡 */
.metaCard{margin-top:18px;border:1px solid var(--line);border-radius:12px;background:#fff;overflow:hidden}
.mrow{display:flex;gap:16px;padding:10px 18px;font-size:13.5px}
.mrow + .mrow{border-top:1px dashed var(--line)}
.mrow .k{flex:none;width:72px;color:var(--muted)}
.mrow b{font-weight:500;color:var(--ink)}

/* 区块 */
.blk{margin-top:26px}
.blk h2{display:flex;align-items:baseline;gap:8px;font-size:15.5px;font-weight:800;color:var(--ink);margin-bottom:12px}
.blk h2 em{font-style:normal;font-size:12px;font-weight:500;color:var(--faint)}

/* 条文正文 */
.lawText{margin:0;padding:20px 22px;border-left:4px solid var(--blue);background:var(--gray);border-radius:0 12px 12px 0;font-size:15px;line-height:2;color:var(--ink)}
.excNote{margin-top:8px;font-size:12px;color:var(--faint)}

/* 版本沿革时间线 */
.vline{list-style:none;margin:0;padding:0}
.vline li{position:relative;display:flex;gap:14px;padding:0 0 20px 4px}
.vline .dot{flex:none;width:11px;height:11px;border-radius:50%;background:#fff;border:3px solid var(--faint);margin-top:4px}
.vline li.cur .dot{border-color:var(--blue)}
.vline .vbody b{font-size:13.5px;font-weight:700;color:var(--ink)}
.vline li.cur .vbody b{color:var(--blue)}
.vline .vbody time{margin-left:10px;font-size:12px;color:var(--muted)}
.vline .vbody p{margin:4px 0 0;font-size:12.5px;color:var(--muted);line-height:1.7}

/* 关联法条 */
.relGrid{display:grid;grid-template-columns:repeat(2,1fr);gap:12px}
.relCard{text-align:left;border:1px solid var(--line);border-radius:12px;background:#fff;padding:14px 16px;transition:border-color .2s,box-shadow .2s,transform .15s var(--ease)}
.relCard:hover{border-color:var(--blue);box-shadow:0 3px 12px rgba(78,110,242,.1);transform:translateY(-1px)}
.relCard b{font-size:13.5px;font-weight:700;color:var(--link)}
.relCard .rsubj{display:block;margin-top:3px;font-size:12px;color:var(--muted)}
.relCard p{margin:7px 0 0;font-size:12.5px;line-height:1.7;color:var(--ink2)}

/* 场景联动 */
.goJudge{width:100%;padding:13px;border:1.5px dashed rgba(78,110,242,.45);border-radius:12px;background:rgba(78,110,242,.05);color:var(--blue);font-size:14px;font-weight:600;transition:background .2s}
.goJudge:hover{background:rgba(78,110,242,.12)}

.disclaimer{margin-top:34px;font-size:12px;color:var(--faint);text-align:center}

.missing{text-align:center;padding:70px 20px}
.missing h1{font-size:20px;font-weight:800;color:var(--ink)}
.missing p{margin:10px 0 22px;font-size:13.5px;color:var(--muted)}
.goSearch{padding:10px 26px;background:var(--blue);color:#fff;border-radius:10px;font-size:14px;font-weight:600;transition:background .2s}
.goSearch:hover{background:var(--blue-h)}

@media (max-width:640px){
  .relGrid{grid-template-columns:1fr}
  .dhead h1{font-size:20px}
  .mrow{padding:9px 14px}
}
</style>
