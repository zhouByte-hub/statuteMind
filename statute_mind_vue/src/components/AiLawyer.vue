<script setup>
/** AI 小法：全局法律咨询对话助手。默认悬浮球，点击展开对话面板（纯前端演示问答）。 */
import { ref, reactive, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const open = ref(false)
const typing = ref(false)
const draft = ref('')
const listEl = ref(null)
/** 消息：{ role: 'ai' | 'user', text, acts?: [{ label, to }] } */
const msgs = reactive([
  { role: 'ai', text: '您好，我是 AI 小法 🧑‍⚖️ 有法律问题可以随时问我，也可以帮您跳转到对应的分析工具。' }
])

/** 建议问题（点击即发送） */
const chips = ['拖欠工资怎么办', '高空抛物违法吗', '租房押金不退']

/** 演示问答规则：关键词 → 回复 + 快捷跳转 */
const RULES = [
  {
    kws: ['工资', '欠薪', '拖欠', '劳动报酬', '劳动'],
    text: '您咨询的是劳动报酬类问题。根据《劳动合同法》第三十条，用人单位应当按照约定及时足额支付劳动报酬；拖欠的，劳动者可依法向法院申请支付令。依据第八十五条，逾期不支付的还可按应付金额 50%–100% 加付赔偿金。建议保留工资条、考勤记录等证据，仲裁时效为一年，也可拨打 12333 求助。',
    acts: [
      { label: '查看行为判断', to: { name: 'judge', query: { j: 'j1' } } },
      { label: '检索相关法条', to: { name: 'search', query: { q: '拖欠工资' } } }
    ]
  },
  {
    kws: ['骂', '辱', '诽谤', '网暴', '名誉'],
    text: '在网上辱骂他人可能构成公然侮辱、诽谤。依据《治安管理处罚法》第四十二条，可处五日以下拘留或五百元以下罚款，情节较重的处五至十日拘留；《民法典》第一千零二十四条同时保护名誉权，可主张停止侵害、赔礼道歉与赔偿。点击/浏览达 5,000 次或转发 500 次以上还可能构成诽谤罪，建议立即删除内容并保存截图证据。',
    acts: [
      { label: '查看行为判断', to: { name: 'judge', query: { j: 'j2' } } },
      { label: '检索相关法条', to: { name: 'search', query: { q: '网上辱骂' } } }
    ]
  },
  {
    kws: ['高空', '抛物', '坠物'],
    text: '高空抛物是法律明文禁止的行为。《民法典》第一千二百五十四条规定禁止从建筑物中抛掷物品，造成损害的由侵权人依法承担赔偿责任；难以确定具体侵权人的，由可能加害的建筑物使用人补偿。《刑法》第二百九十一条之二进一步规定，情节严重的可处一年以下有期徒刑、拘役或者管制。抛掷行为本身即违法，无须等到造成损害。',
    acts: [
      { label: '查看行为判断', to: { name: 'judge', query: { j: 'j3' } } },
      { label: '检索相关法条', to: { name: 'search', query: { q: '高空抛物' } } }
    ]
  },
  {
    kws: ['狗', '犬', '宠物', '咬'],
    text: '饲养动物致人损害适用无过错责任。《民法典》第一千二百四十五条规定，饲养的动物造成他人损害的，饲养人或管理人应当承担侵权责任，但能证明损害因被侵权人故意或重大过失造成的可以不承担或减轻责任。《治安管理处罚法》第七十五条规定，放任动物恐吓他人的可处二百元以上五百元以下罚款。',
    acts: [{ label: '检索相关法条', to: { name: 'search', query: { q: '狗咬人' } } }]
  },
  {
    kws: ['噪音', '噪声', '扰民', '广场舞'],
    text: '制造噪声干扰他人正常生活的，依据《治安管理处罚法》第五十八条，处警告；警告后不改正的，处二百元以上五百元以下罚款。可以先与对方沟通、请物业协调，或拨打 110、12345 反映。',
    acts: [{ label: '检索相关法条', to: { name: 'search', query: { q: '噪音扰民' } } }]
  },
  {
    kws: ['租房', '押金', '租金', '租赁', '租'],
    text: '租赁相关权益受《民法典》合同编保护：第七百零三条界定租赁合同，第七百二十一条规定承租人应按约定期限支付租金。押金返还、扣减规则以合同约定为准，房东无正当理由拒退押金的，可协商、投诉或起诉维权，注意保留合同、付款凭证与沟通记录。',
    acts: [{ label: '检索相关法条', to: { name: 'search', query: { q: '租房押金' } } }]
  },
  {
    kws: ['工作流', '流程'],
    text: '工作流编排可以把「问题分类 → 知识检索 → 条件分支 → 生成回复」等步骤串成自动化流程，支持可视化拖拽与一键试运行。要不要去看看内置的「欠薪维权助手」示例？',
    acts: [{ label: '打开工作流', to: { name: 'workflow' } }]
  },
  {
    kws: ['你好', '您好', '在吗', 'hi', 'hello', '介绍'],
    text: '您好！我是 AI 小法，可以为您：解答常见法律问题、指引使用「法条检索 / 行为判断 / 知识库 / 工作流」工具。您可以试试下面的快捷问题。'
  }
]

const FALLBACK = '抱歉，演示版暂只内置了常见法律问题的解答。您可以问我：拖欠工资、网上辱骂、高空抛物、噪音扰民、租房押金等，或直接使用顶部导航的工具。'

function reply(q){
  const t = q.toLowerCase()
  return RULES.find(r => r.kws.some(k => t.includes(k)))
}

function push(m){
  msgs.push(m)
  nextTick(() => { if(listEl.value) listEl.value.scrollTop = listEl.value.scrollHeight })
}

function send(text){
  const q = (text ?? draft.value).trim()
  if(!q || typing.value) return
  draft.value = ''
  push({ role: 'user', text: q })
  typing.value = true
  const rule = reply(q)
  const full = rule ? rule.text : FALLBACK
  const acts = rule?.acts
  // 打字机效果：逐字呈现
  const echo = { role: 'ai', text: '' }
  push(echo)
  let i = 0
  const tick = () => {
    i = Math.min(full.length, i + 2)
    echo.text = full.slice(0, i)
    if(listEl.value) listEl.value.scrollTop = listEl.value.scrollHeight
    if(i < full.length){ setTimeout(tick, 16) }
    else{
      if(acts) echo.acts = acts
      typing.value = false
    }
  }
  setTimeout(tick, 260)
}

function go(to){
  router.push(to)
  open.value = false
}

watch(open, v => {
  if(v) nextTick(() => { if(listEl.value) listEl.value.scrollTop = listEl.value.scrollHeight })
})
</script>

<template>
  <div class="ailaw" :class="{ on: open }">
    <!-- 对话面板 -->
    <transition name="pop">
      <section v-if="open" class="panel" role="dialog" aria-label="AI 小法对话">
        <header>
          <span class="ava" aria-hidden="true">法</span>
          <div class="tt">
            <b>AI 小法</b>
            <i>在线 · 演示问答</i>
          </div>
          <button class="x" aria-label="收起对话" @click="open = false">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M6 6l12 12M18 6 6 18"/></svg>
          </button>
        </header>

        <div ref="listEl" class="list">
          <div v-for="(m, i) in msgs" :key="i" class="row" :class="m.role">
            <div class="bubble">
              <p>{{ m.text }}</p>
              <div v-if="m.acts?.length" class="acts">
                <button v-for="a in m.acts" :key="a.label" @click="go(a.to)">{{ a.label }}</button>
              </div>
            </div>
          </div>
          <div v-if="typing" class="dots" aria-label="正在输入"><i /><i /><i /></div>
        </div>

        <div class="chips">
          <button v-for="c in chips" :key="c" :disabled="typing" @click="send(c)">{{ c }}</button>
        </div>

        <footer>
          <input
            v-model="draft" class="ask" placeholder="输入您的法律问题…"
            aria-label="输入您的法律问题" @keydown.enter="send()"
          >
          <button class="go" aria-label="发送" :disabled="typing || !draft.trim()" @click="send()">
            <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M3.4 20.4 21 12 3.4 3.6 3.4 10l12.6 2-12.6 2z"/></svg>
          </button>
        </footer>
      </section>
    </transition>

    <!-- 悬浮球 -->
    <button class="fab" :aria-label="open ? '收起 AI 小法' : '打开 AI 小法'" :title="open ? '收起' : 'AI 小法'" @click="open = !open">
      <transition name="swap" mode="out-in">
        <svg v-if="!open" key="s" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M12 3v2.5M5.5 8.5 4 6.2M18.5 8.5 20 6.2"/>
          <path d="M12 5.5c-4.1 0-7 2.6-7 6 0 2 1 3.7 2.6 4.8L6.8 20l3.1-1.6c.7.1 1.4.2 2.1.2 4.1 0 7-2.6 7-6s-2.9-7.1-7-7.1Z"/>
          <path d="M9.2 10.4h.01M14.8 10.4h.01M9.5 13.4c1.5 1 3.5 1 5-.01"/>
        </svg>
        <svg v-else key="x" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" aria-hidden="true"><path d="M6 6l12 12M18 6 6 18"/></svg>
      </transition>
    </button>
  </div>
</template>

<style scoped>
.ailaw{position:fixed;right:22px;bottom:22px;z-index:70;display:flex;flex-direction:column;align-items:flex-end;gap:12px}

/* ---------- 悬浮球 ---------- */
.fab{position:relative;width:56px;height:56px;border-radius:50%;border:none;cursor:pointer;color:#fff;
  background:linear-gradient(145deg,#4C6CF0,var(--blue) 55%,#3249C0);
  display:flex;align-items:center;justify-content:center;
  box-shadow:0 8px 22px rgba(61,92,229,.38),0 2px 6px rgba(31,41,72,.18);
  transition:transform .22s cubic-bezier(.34,1.56,.64,1),box-shadow .22s}
.fab svg{width:27px;height:27px}
.fab:hover{transform:translateY(-3px) scale(1.05);box-shadow:0 12px 28px rgba(61,92,229,.46),0 3px 8px rgba(31,41,72,.2)}
.fab::after{content:'';position:absolute;inset:-5px;border-radius:50%;border:2px solid rgba(61,92,229,.35);animation:pulse 2.6s ease-out infinite;pointer-events:none}
@keyframes pulse{0%{transform:scale(.9);opacity:.9}70%,100%{transform:scale(1.32);opacity:0}}

/* ---------- 面板 ---------- */
.panel{width:364px;max-width:calc(100vw - 32px);height:472px;max-height:calc(100dvh - 130px);
  display:flex;flex-direction:column;overflow:hidden;
  background:#fff;border:1px solid var(--line,#E7EAF0);border-radius:18px;
  box-shadow:0 24px 60px rgba(23,32,64,.18),0 4px 14px rgba(23,32,64,.08)}
.panel header{flex:none;display:flex;align-items:center;gap:10px;padding:13px 14px;
  background:linear-gradient(135deg,#F5F7FF,#EEF2FE);border-bottom:1px solid var(--line,#E7EAF0)}
.ava{width:34px;height:34px;border-radius:10px;background:linear-gradient(145deg,#4C6CF0,var(--blue));
  color:#fff;font-size:16px;font-weight:700;display:flex;align-items:center;justify-content:center;flex:none}
.tt{flex:1;display:flex;flex-direction:column;gap:1px;min-width:0}
.tt b{font-size:14.5px;color:var(--ink,#1F2937)}
.tt i{font-style:normal;font-size:11.5px;color:#12A150}
.tt i::before{content:'';display:inline-block;width:6px;height:6px;border-radius:50%;background:#12A150;margin-right:5px;vertical-align:1px}
.x{width:30px;height:30px;border:none;background:transparent;border-radius:8px;color:#7A8296;cursor:pointer;
  display:flex;align-items:center;justify-content:center;flex:none}
.x svg{width:17px;height:17px}
.x:hover{background:rgba(31,41,72,.07);color:var(--ink,#1F2937)}

/* 消息列表 */
.list{flex:1;overflow-y:auto;padding:14px 14px 6px;display:flex;flex-direction:column;gap:11px;background:#FAFBFD}
.row{display:flex}
.row.user{justify-content:flex-end}
.bubble{max-width:86%;padding:9px 12px;border-radius:13px;font-size:13.2px;line-height:1.75}
.row.ai .bubble{background:#fff;border:1px solid var(--line,#E7EAF0);border-top-left-radius:4px;color:var(--ink,#1F2937)}
.row.user .bubble{background:var(--blue);color:#fff;border-top-right-radius:4px}
.bubble p{margin:0;white-space:pre-wrap;word-break:break-word}
.acts{display:flex;flex-wrap:wrap;gap:6px;margin-top:9px}
.acts button{border:1px solid rgba(61,92,229,.4);background:#F3F6FE;color:var(--blue);font-size:12px;font-weight:600;
  padding:5px 10px;border-radius:999px;cursor:pointer;transition:background .15s}
.acts button:hover{background:var(--blue);color:#fff}

/* 输入中三点 */
.dots{display:flex;gap:4px;padding:4px 2px 6px}
.dots i{width:6px;height:6px;border-radius:50%;background:#A9B1BE;animation:dot 1.1s ease-in-out infinite}
.dots i:nth-child(2){animation-delay:.16s}
.dots i:nth-child(3){animation-delay:.32s}
@keyframes dot{0%,100%{opacity:.35;transform:translateY(0)}50%{opacity:1;transform:translateY(-3px)}}

/* 快捷问题 */
.chips{flex:none;display:flex;flex-wrap:wrap;gap:6px;padding:9px 14px;border-top:1px solid var(--line,#E7EAF0);background:#fff}
.chips button{border:1px solid var(--line,#E7EAF0);background:#F7F8FA;color:#4B5563;font-size:11.8px;
  padding:4.5px 10px;border-radius:999px;cursor:pointer;transition:all .15s}
.chips button:hover:not(:disabled){border-color:rgba(61,92,229,.5);color:var(--blue);background:#F3F6FE}
.chips button:disabled{opacity:.5;cursor:not-allowed}

/* 输入区 */
footer{flex:none;display:flex;gap:8px;padding:11px 14px 13px;background:#fff}
.ask{flex:1;min-width:0;height:38px;border:1px solid var(--line,#E7EAF0);border-radius:11px;padding:0 12px;
  font-size:13.2px;color:var(--ink,#1F2937);background:#F7F8FA;outline:none;transition:border-color .15s,background .15s}
.ask:focus{border-color:rgba(61,92,229,.55);background:#fff}
.ask::placeholder{color:#9AA1AD}
.go{width:38px;height:38px;border:none;border-radius:11px;background:var(--blue);color:#fff;cursor:pointer;flex:none;
  display:flex;align-items:center;justify-content:center;transition:background .15s,opacity .15s}
.go svg{width:17px;height:17px}
.go:hover:not(:disabled){background:#3348C7}
.go:disabled{opacity:.45;cursor:not-allowed}

/* ---------- 过渡动画 ---------- */
.pop-enter-active,.pop-leave-active{transition:opacity .2s ease,transform .22s cubic-bezier(.34,1.4,.64,1);transform-origin:right bottom}
.pop-enter-from,.pop-leave-to{opacity:0;transform:scale(.86) translateY(14px)}
.swap-enter-active,.swap-leave-active{transition:opacity .13s,transform .13s}
.swap-enter-from,.swap-leave-to{opacity:0;transform:scale(.5) rotate(-40deg)}

@media (max-width:760px){
  .ailaw{right:14px;bottom:14px}
  .panel{height:min(430px,calc(100dvh - 110px))}
  .fab{width:52px;height:52px}
  .fab svg{width:24px;height:24px}
}

@media (prefers-reduced-motion:reduce){
  .fab::after{animation:none;opacity:0}
  .pop-enter-active,.pop-leave-active,.swap-enter-active,.swap-leave-active{transition:opacity .15s;transform:none}
}
</style>
