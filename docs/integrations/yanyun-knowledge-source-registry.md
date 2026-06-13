# 燕云创作知识库来源登记 v0.1

## Summary

本文记录燕云创作知识库二阶段的资料来源治理。知识库只在构建期使用搜索和人工整理；用户实时写词链路不联网搜索，避免生成速度和稳定性被外部网页影响。

当前登记时间：2026-06-13。
当前商用知识版本：`yanyun-commercial-kb-2026-06-13-v1`。
历史工程种子版本：`yanyun-official-kb-2026-06-13-v1`。

## Source Policy

资料优先级：

1. `P0` 公司/官方内部资料包：角色设定、剧情梗概、任务线、世界观手册、宣发禁区。当前暂未接入。
2. `P1` 公开官方资料：官网、官方公告、官方开发计划、官方视频/图文。可进入事实层，但只提炼为短摘要。
3. `P2` 游戏内实录整理：由项目组或公司同事截图/录屏/文字整理后入库。可进入事实层，需要记录采集版本和采集人。
4. `P3` 攻略站、玩家维基、剧情实录整理：可进入创作叙事层，用于串联人物传、剧情线、情绪弧线和可用意象，来源等级标记为 `guide_wiki_synthesis` / `accepted_story_synthesis`。
5. `P4` 原始社区散帖、评论、二创讨论：只能作为发现线索，不直接入库。

入库规则：

- 不大段复制官方原文。
- 每条资料必须标注来源、更新时间、实体类型、事实等级和禁写项。
- 只向 Agent 注入短摘要、情绪弧线、可用意象和禁写项。
- 找不到任何可解释来源时，标记为资料缺口，不编造。
- 如果资料来自攻略站、玩家维基或剧情整理，可先进入创作叙事层；如果未来要作为对外剧情百科或公司审核依据，再回到官方资料或游戏内实录二次确认。

## Guide / Wiki Story Synthesis

创作知识库允许通过“自问自答 + 攻略/维基/剧情整理综合”补齐人物传。该层不是对外官方事实声明，而是给 `CreativeBriefAgent` / `LyricsAgent` 使用的创作素材层。

入库要求：

- 每条资料必须转成短摘要、情绪弧线、可用意象和禁写误区，不大段复制网页原文。
- `source_type` 使用 `guide_wiki_synthesis`，`fact_level` 使用 `accepted_story_synthesis`。
- 可以把多处攻略、玩家维基和剧情实录串成角色人生线，例如“寒香寻：乱世孤女 -> 洛神换脸者 -> 不羡仙老板娘 -> 少东家的寒姨 -> 绣金楼旧怨”。
- 用户侧歌词可以使用该叙事层，但不能在产品文案里宣称“官方已确认完整人物传”。
- 后续公司如提供内部资料包，以公司资料包覆盖同名实体的叙事层摘要。

## Approved Public Official Sources

| ID | Source | Type | Accessibility | Date / As Of | Intended Use |
| --- | --- | --- | --- | --- | --- |
| `S1` | [燕云十六声官网](https://www.yysls.cn/) | official | public | as of 2026-06-13 | 世界观、基础地域、玩法气质总览 |
| `S2` | [燕云十六声移动官网](https://www.yysls.cn/m/) | official | public | as of 2026-06-13 | 移动端官网公开设定与产品定位核对 |
| `S3` | [《燕云十六声》公测开启相关官方公告](https://www.yysls.cn/news/official/20241227/37780_1202836.html) | official | public | 2024-12-27 | 不羡仙、寒香寻、寻心、神仙渡、弱水岸、奇术、江湖烟火 |
| `S4` | [凉州主线剧情相关官方公告](https://www.yysls.cn/news/official/20250718/37780_1247023.html) | official | public | 2025-07-18 | 凉州、长安、阿依努尔、玉露为乡、身份追问 |
| `S5` | [中式解谜开发计划官方文章](https://www.yysls.cn/news/official/20251022/37780_1264523.html) | official | public | 2025-10-22 | 中式解谜、点穴、摄星拿月、清河、神仙渡、机关鸟、叶子戏 |
| `S6` | [武学架构调整和设计思路官方文章](https://www.yysls.cn/news/official/20251014/37780_1262963.html) | official | public | 2025-10-14 | 武学自由搭配、任意武器、流派、心法、守势技、特殊技、易武技 |
| `S7` | [青州、蓬山、文津馆相关官方公告](https://www.yysls.cn/news/official/20250823/37780_1254195.html) | official | public | 2025-08-23 | 青州、蓬山、文津馆、书卷气、求索与情感线索 |
| `S8` | [无名剑谱相关官方公告](https://www.yysls.cn/news/official/20250529/37780_1236417.html) | official | public | 2025-05-29 | 无名剑谱、空谷古冢、鬼市子、江湖万象 |
| `S9` | [九流相关官方公告](https://www.yysls.cn/news/official/20250627/37780_1242827.html) | official | public | 2025-06-27 | 九流奇谋、九流祸起、九流暗涌、逐鹿中原 |
| `S10` | [营生、不平事相关官方公告](https://www.yysls.cn/news/official/20250520/37780_1233919.html) | official | public | 2025-05-20 | 营生、不平事、市井任务、玩法生活感 |
| `S11` | [开封夜市与鬼市相关官方公告](https://www.yysls.cn/news/official/20241017/37780_1189576.html) | official | public | 2024-10-17 | 开封夜色、樊楼、鬼市、市井繁华与暗面 |
| `S12` | [Where Winds Meet 全球官网](https://www.wherewindsmeetgame.com/m/hmt/index.html?from=nav) | official | public | as of 2026-06-13 | 全球版公开人物、势力、世界气质核对 |
| `S13` | [Where Winds Meet Google Play 页面](https://play.google.com/store/apps/details?hl=zh&id=com.netease.yysls) | official | public | as of 2026-06-13 | 开放世界、武侠体验、跨端产品定位核对 |

## Self-QA Collection Method

针对用户点名但资料不足的实体，采用“自问自答”整理，不直接幻想结论：

1. 用户可能怎么问？
   例如：`以寒香寻的人生经历写一首歌`、`写阿依努尔的身份感`、`写九流门里的普通人`。
2. 这个问题需要哪些事实？
   人物经历、关系、剧情阶段、地域、情绪弧线、禁写结论。
3. 官方公开资料是否足够？
   足够则入事实层；不足则进入创作叙事层，用攻略/维基/剧情整理和自问自答串联人物传。
4. 能否用游戏内实录补齐？
   可以则进入后续采集任务；不能则保持 `accepted_story_synthesis`，允许服务作词但不宣称官方实锤。
5. 是否会限制玩家创意？
   如果是普通玩家故事，只注入世界气质；如果是角色/剧情歌，才注入对应事实卡。

## Current Coverage

已进入 `knowledge-base/commercial-final/` 的商用分域资料：

- 总量：`280` 个实体、`917` 个创作 chunk、`336` 条 eval case；`commercial-final-gap-inventory.json` 中 `278` 行缺口均已转为 `present`。
- 人物：`characters.json` 包含 `130` 条人物/人物群像/人物相关实体，覆盖主线人物、支线人物、势力代表、历史锚点、伙伴系统角色、强记忆 NPC 和地域群像。
- 剧情/任务线：`storylines.json` 包含 `34` 条剧情实体，覆盖清河、开封、河西/凉州、青州/蓬山、伙伴、镇守旧事、侠迹残章等创作高频路径。
- 地域/场景：`regions.json` 包含 `41` 条地域/场景实体，覆盖大区域、城市场景、支线地点、镇守场景和可写歌的细分空间。
- 势力/门派/组织：`factions.json` 包含 `32` 条势力实体，覆盖门派、组织、敌对势力、庙堂/边军/市井秩序和待核组织线索。
- 玩法体验：`gameplay.json` 包含 `30` 条玩法实体，覆盖奇术、偷师、寻声、轻功探索、营生、不平事、镇守、同行、多人、家业和外观等玩家故事素材。
- 创作禁区：`creative-boundaries.json` 包含 `13` 条边界实体，覆盖其他 IP 拒绝、风格安全改写、未确认剧情、玩家故事保真、封面标题、商用发布等。
- 回归题库：`eval-cases.json` 包含 `336` 条真实玩家输入样例，覆盖角色歌、剧情歌、地域歌、门派歌、玩法体验、其他 IP 转译/拒绝、待核线索谨慎使用和知识库召回。

## Known Gaps

- `commercial-final-gap-inventory.json` 当前已无非 `present` 缺口；后续不再按“补缺口表”推进，而应进入质量评测和公司资料复核。
- 公司/官方内部资料包仍未接入；若公司提供角色设定、剧情梗概、任务线或宣发禁区，需要覆盖同名实体的叙事层摘要。
- 官方设定中不可写、不可误写、不可剧透的品牌边界仍需公司确认。
- P5 尚未完成：A/B 评测报告、chunk 级 provenance manifest、污染审计报告和公司交付证据包仍需单独产出。
- 当前商用包服务创作，不应宣称为完整官方剧情百科或玩家攻略百科。

## Runtime Boundary

- 用户写词请求不会触发网页搜索。
- `pending_clues` 可以进入写词 prompt，但必须作为暗线、传闻、情绪或意象使用，不在用户侧宣称为官方定论。
- 检索失败不阻断创作，只降级为空知识上下文。
- 日志只记录 `kb_version`、chunk id、entity id、耗时和 hash，不记录完整 prompt、官方原文、供应商原始响应或用户敏感信息。
