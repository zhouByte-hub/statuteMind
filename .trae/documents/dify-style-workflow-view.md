# 「工作流」视图实施计划（类 Dify 编排界面）

## Context

statuteMind 前端（Vue 3.5 + vue-router 4 + Vite 6，纯前端演示、全 mock 数据、零额外依赖）现有法条检索/行为判断/知识库三个主视图。用户要求新增第 4 个主视图：**类 Dify 的工作流编排界面**。已确认两项决策：画布采用 **@vue-flow/core** 库；节点体系采用 **通用 Dify 节点全套**（无后端时 LLM/HTTP 等做合理模拟，可真实计算的部分尽量真实计算并复用 mock.js 数据）。

## 1. 依赖安装

在 statute_mind_vue 下安装（Vue 3.5 兼容的 1.x 稳定线）：

- `@vue-flow/core@^1.42.0` — 画布引擎（节点/边/连线/平移缩放/框选）
- `@vue-flow/background@^1.3.2` — 点阵背景
- `@vue-flow/controls@^1.1.2` — 缩放/适应控件
- `@vue-flow/minimap@^1.5.0` — 小地图

视图内引入 `@vue-flow/core/dist/style.css` 与 `theme-default.css`，并用 `--vf-*` 变量对齐项目设计令牌（--blue:#3D5CE5 白底风格）。

## 2. 新增文件（目录 src/workflow/ + src/components/workflow/）

| 文件 | 职责 |
|---|---|
| `src/views/WorkflowView.vue` | 第 4 视图壳：顶部工具栏（流程名/运行/停止/重置/加载示例）+ 左节点库面板 + 中间 VueFlow 画布 + 右配置抽屉 + 底运行追踪面板 |
| `src/components/workflow/WfNode.vue` | 自定义节点：图标/标题/配置摘要/状态徽标（idle/running/success/failed/skipped）/Handles；ifelse 多输出锚点（每 case 一个 + else，sourceHandle 区分） |
| `src/components/workflow/WfEdge.vue` | 自定义贝塞尔边；运行中该边加流光动画（stroke-dasharray） |
| `src/components/workflow/WfNodePanel.vue` | 左侧节点库，按 Dify 分组（开始/结束、AI、检索、逻辑、转换），draggable 到画布（dataTransfer 带 type） |
| `src/components/workflow/WfConfigDrawer.vue` | 右侧抽屉：节点名 + 按 type 分发表单 + 「可用上游变量」chips（点击插入 `{{#nodeId.var#}}`） |
| `src/components/workflow/ConfigForms.vue` | 10 类节点表单集中导出 `{ type: 组件 }` |
| `src/components/workflow/WfRunPanel.vue` | 运行追踪：节点时序列表、每节点输入/输出 JSON 折叠、answer 打字机文本、总耗时 |
| `src/workflow/registry.js` | NODE_TYPES 注册表：type → { label, icon, color, group, defaults, outVars } |
| `src/workflow/executor.js` | `runFlow({nodes, edges, input}, hooks)` 模拟执行引擎（见 §5） |
| `src/workflow/sampleFlow.js` | 示例「欠薪维权助手」：start→classifier→ifelse→knowledge→llm→answer，坐标预排 |
| `src/workflow/storage.js` | localStorage（key `statuteMind.wf.v1`）：loadFlow/saveFlow/resetToSample |

### 节点注册表（10 类，通用 Dify 全套）

| type | 名称 | 关键配置项 | outVars |
|---|---|---|---|
| start | 开始 | 输入字段（默认 query） | query |
| answer | 直接回复 | 回复模板 | — |
| llm | LLM | model/prompt/temperature | text |
| knowledge | 知识检索 | topK | chunks |
| classifier | 问题分类 | classes 列表 | class_name |
| ifelse | 条件分支 | cases[{id,keyword}] + else | — |
| code | 代码执行 | language/code | output |
| http | HTTP 请求 | method/url/mockResponse | body/status |
| template | 模板转换 | 模板文本 | output |
| aggregator | 变量聚合 | 变量引用列表 | output |

## 3. 修改现有文件

| 文件 | 改动 |
|---|---|
| `src/router/index.js` | routes 增 `{ path:'/workflow', name:'workflow', component:()=>import('@/views/WorkflowView.vue') }` |
| `src/components/TopBar.vue` | NAVS（L8-12）增 `{ name:'workflow', label:'工作流' }` |
| `src/components/SiteFooter.vue` | VIEW_NAMES（L6）增 `workflow:'工作流编排'` |
| `src/composables/useNav.js` | 增 `openWorkflow()` 并导出（与现有 open* 风格一致） |
| `src/pet/scenarios.js` + `src/pet/pet.js` | 增 workflow 场景（pose:'ponder'，anchor:50，bubble「正在编排…」）与 `pet.workflow(done)` API |
| `src/views/WorkflowView.vue` 运行时 | 点「运行」时调 `pet.workflow(finished)`，与其余三视图桌宠惯例一致 |

## 4. 画布编辑交互

- 拖拽：从左面板拖入 → `screenToFlowCoordinate` 落点添加节点；节点拖动、平移、滚轮缩放由 VueFlow 内建
- 连线：输出锚点拉到输入锚点（`onConnect` → `addEdges`）；点击边/节点选中后 Delete 删除（设 `delete-key-code="Delete"`，抽屉表单聚焦时不误删）
- 框选：Shift+拖拽（VueFlow `selectionKeyCode` 默认）
- 画布高度：`calc(100dvh - 60px)`（TopBar 高 60）；SiteFooter 被推到折叠线下属画布应用惯例，可接受

## 5. 模拟执行引擎（executor.js）

- **拓扑**：Kahn 入度排序，环检测报错终止；`ctx[nodeId]` 存各节点输出；模板统一 `{{#nodeId.var#}}` 语法 `resolve()` 替换
- **真实计算**：classifier 用 mock.js `PRESET_KEYS` 正则分类；knowledge 对 `STATUTES` kws 关键词命中计分取 topK；template 真实做 `{{var}}` 替换；code 用 `new Function` 受限真实执行（try/catch，演示可控）；llm 命中 `JUDGMENTS`（经 PRESET_KEYS 映射）则拼装其 cps 四段文本并逐字回调模拟流式
- **模拟**：http 延迟后返回 mockResponse（不发真实请求规避 CORS）；每节点 sleep 400–900ms 营造执行感
- **状态与传播**：hooks `onNodeStart/onNodeFinish/onEdgeActive`；节点抛错→failed，下游全部 skipped；AbortController 支持停止
- 运行时桌宠 ponder 入场，`finished` resolve 后离场

## 6. UI 融合与降级

- 样式：全部走 main.css 设计令牌；覆盖 vue-flow 内部 DOM 用 `:deep()`；对比度满足 WCAG AA；字号梯度与现有页面一致
- 层级：PetLayer 为 fixed z-index:60（pointer-events:none）；配置抽屉/运行面板若 fixed 需 z-index ≥70
- 移动端 ≤760px：隐藏左面板编辑能力，保留画布预览 + 运行 + 底部日志（touch 连线体验差，规避）
- 持久化：localStorage 仅存可序列化纯数据，key 带版本号

## 7. 实施顺序与验证

1. 装依赖 → router/TopBar/SiteFooter/useNav 接线 → 空画布可达（验证 `/#/workflow` 与导航高亮）
2. registry + WfNode/WfEdge/WfNodePanel：拖放添加、拖动、连线、删除（验证拖 2 节点连线成功）
3. WfConfigDrawer + ConfigForms + storage.js（验证改配置刷新后恢复）
4. executor + 状态徽标 + 边流光 + WfRunPanel（验证示例工作流跑通、失败分支传播正确）
5. sampleFlow 一键加载 + 移动端降级 + 桌宠场景 → `npm run build` 通过；用 TRAE-browseruse 对 `http://localhost:5172/#/workflow`（dev 端口以实际为准）做桌面端实拍走查：拖节点→连线→配置→运行→日志全链路，另验移动端降级与桌宠出场

## 8. 风险点

- vue-flow theme-default 为全局类，自定义须 `:deep()`，避免与现有原子类冲突
- Delete 键误删：聚焦表单时需校验
- `new Function` 执行 code 节点：演示项目无 CSP 限制，界面注明「演示环境，请勿写死循环」
- localStorage 数据结构变更：key 带 v1 便于迁移
