# Surface brief — statuteMind 原型（index.html）

## Scope & mode

单文件 HTML 原型（index.html），含三个可互跳视图：法条检索 / 行为判断 / 知识库。Operate 模式。无后端，全部检索与判断为前端模拟数据。

## Audience, job, constraints

- 受众：需要快速查法、判断行为后果的普通人与初级法务/HR。
- 任务：①搜法条（关键词→带出处的条文结果）；②判行为（描述行为→四段判断：引用条文+违定性+处罚+风险提示）；③知识库（法条库浏览+模拟上传导入）。
- 约束：中文界面；模拟数据须标「演示数据」；免责声明常驻；键盘可达；动效尊重 prefers-reduced-motion；单文件、无构建、无本地资源引用，字体仅限 Google Fonts。

## Chosen direction

「定向推理图」（challenger-course，用户在决策轮直接钦定，覆盖骰子指派）。memorable moment：判断推理链作为紫色赛程在等高线地形上按编号检查点逐段描绘，落点即结论。组合参考：.impeccable/mocks/decision/challenger-course.html（代码主导构建，其构图即构建所配）。

## Unresolved

未来接真实 LLM 的接口位仅作注释级预留；「调准」理解为页面间导航互跳。

## Direction contract

THESIS: 法律判断是一段有编号的越野赛程——地形（法条库）固定不动，紫色路线带着你从「行为」走到「后果」；品类默认的蓝色数据库布局被拒绝。

OWN-WORLD: 米白 #f5f4ee 地面、墨绿黑 #26251f 正文、赛程紫 #6d4fa1（只给活跃路线/当前检查点/主操作）、开阔地黄 #d9cf9a 与橄榄 #a8a06a 只做地形色块；1px 等高线纹理为底层；粗壮紧凑显示字体做方位标，检查点=紫描边圆圈+粗编号；无渐变玻璃感，纸面制图质感。

STORY: 访客明白——这是一个把「我遇到的事」翻译成「违反了哪条、后果是什么」的判断引擎；每一步推理都发生在地图上，可回看、可跳步；结论可溯及法条原文。

FIRST VIEWPORT: 顶部细导航带（statuteMind·法规智能引擎 + 三视图导航 + 「演示数据·内置1,286条法条」徽标）；主区左侧图例/检索台（大搜索框，示例「公司拖欠工资 3 个月怎么办」，搜法条/判行为切换），右侧为推理地图（SVG 等高线地形 + 紫色路线连 4 检查点：1行为定性→2法条匹配→3处罚与责任→4风险提示，当前=3）；右缘当前检查点详情卡（编号、名称、关键内容、GO按钮）；底部免责小字。主操作（搜索/重新判断）位于检索台底部偏上。

FORM: 定向推理图（我自己接地候选表第4位，challenger-course 胜出），seed key 9c74758d。

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance.
