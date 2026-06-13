# 燕云创作知识库 P5 证据包 v0.1

生成时间：2026-06-14

## 总体结论

- 当前知识库版本：`yanyun-commercial-kb-2026-06-13-v1`，内容边界截至 `2026-06-13`。
- 正式库规模：`280` 个实体、`917` 个创作 chunk、`336` 条 eval case。
- 缺口盘点表：`278` 行，剩余非 `present` 为 `0`。
- 污染审计：实体问题 `0`，chunk 问题 `0`，eval 问题 `0`，敏感信息问题 `0`。
- 本报告为离线证据包，不调用真实 DeepSeek、Yunwu、WellAPI、DreamMaker，不联网，不导入数据库。

## 分域覆盖

| 类型 | 数量 |
| --- | ---: |
| `boundary` | 13 |
| `character` | 129 |
| `faction` | 32 |
| `gameplay` | 30 |
| `place` | 1 |
| `region` | 40 |
| `story` | 35 |

## 来源与事实等级

| source_type | 实体数 |
| --- | ---: |
| `community` | 4 |
| `guide_wiki_synthesis` | 204 |
| `official_public` | 51 |
| `official_public_curated` | 21 |

| fact_level | 实体数 | 使用口径 |
| --- | ---: | --- |
| `accepted_story_synthesis` | 183 | 攻略/维基/剧情整理综合，可服务创作，不对外宣称官方定论。 |
| `creative_boundary` | 13 | 创作/审核/交付边界。 |
| `creative_guidance` | 6 | 创作指导，不是剧情事实。 |
| `needs_ingame_recording` | 2 | 需游戏内实录补证。 |
| `official_public_seed` | 51 | 官方公开资料种子，可作为基础事实。 |
| `pending_clues` | 25 | 待核线索，只能作暗线、传闻、旧事、情绪或意象。 |

## A/B 评测方案

本阶段不直接跑真实模型，先固定评测协议。后续执行时对同一批输入分别使用：

- A 组：`KNOWLEDGE_RETRIEVAL_MODE=disabled`。
- B 组：`KNOWLEDGE_RETRIEVAL_MODE=pgvector`，知识库版本为当前 `kb_version`。

评分维度：

| 维度 | 通过标准 |
| --- | --- |
| 世界归属感 | 歌词像发生在燕云大世界里，不靠硬塞“燕云/十六声”关键词。 |
| 用户故事保真 | 玩家原始角色、情绪和故事没有被知识库强行改写。 |
| 角色/剧情准确性 | 点名角色、地域、门派、剧情线时不写错核心经历和关系。 |
| 可唱性 | 结构可唱，有副歌记忆点，不像百科或散文。 |
| 泛古风污染 | 不堆砌红尘、宿命、刀光剑影等空泛词。 |
| 边界安全 | 其他 IP、现实歌手仿唱、待核线索和商用发布边界处理正确。 |

建议判定：B 组在世界归属感、角色/剧情准确性上应显著高于 A 组；用户故事保真和可唱性不能下降。

## 抽样 Eval Case

| id | 输入 | 期望实体 |
| --- | --- | --- |
| `eval-character-hanxiangxun-001` | 以寒香寻的人生经历写一首歌，想要少年游和不羡仙的感觉 | `character-hanxiangxun`, `place-buxianxian`, `place-shenxiandu` |
| `eval-character-ayinuer-001` | 写一首阿依努尔的角色歌，主题是故乡和身份 | `character-ayinuer`, `region-liangzhou`, `story-yulu-weixiang` |
| `eval-character-xunxin-001` | 帮我写寻心线的歌，空灵一点 | `character-xunxin`, `story-ruoshui-xunxin`, `place-ruoshui-an` |
| `eval-character-tianying-001` | 田英这个角色写成对手感很强的歌 | `character-tianying` |
| `eval-character-qianye-001` | 千夜主题，夜行和孤独 | `character-qianye` |
| `eval-character-ayinuer-alias-001` | 阿依奴的歌，写玉露台和月光酒，不要写得猎奇 | `character-ayinuer`, `story-yulu-weixiang` |
| `eval-character-ye-wanshan-001` | 叶万山，北地守城名将那种沉默背负 | `character-ye-wanshan`, `region-yanmen` |
| `eval-character-lucky-seventeen-001` | 小十七，少年感但不要太幼稚 | `character-lucky-seventeen` |
| `eval-character-dao-lord-001` | 道主和九流门地下规矩，想要神秘一点 | `character-dao-lord`, `faction-jiuliumen` |
| `eval-character-void-king-001` | 无相皇，写突破血脉限制却被力量吞掉 | `character-void-king` |
| `eval-character-jiangwulang-001` | 写江叔带着少东家在清河长大的感觉 | `character-jiangwulang`, `region-qinghe`, `place-buxianxian` |
| `eval-character-chuqingquan-001` | 写褚清泉和寒姨那种家国私情被撕开的歌 | `character-chuqingquan`, `character-hanxiangxun` |
| `eval-character-hongxian-001` | 红线和少东家少年同行，想要轻快但后劲酸 | `character-hongxian`, `place-shenxiandu` |
| `eval-character-yidao-001` | 伊刀作为寒姨故人出现，写旧案回潮 | `character-yidao`, `character-hanxiangxun` |
| `eval-character-tianbushou-001` | 天不收和寒姨旧信，写一首低声叙事歌 | `character-tianbushou`, `character-hanxiangxun` |
| `eval-character-zheng-e-001` | 郑鄂常平一炬，写一个从救世到厌世的人 | `character-zheng-e` |
| `eval-story-guishizi-001` | 鬼市子和开封地下鬼市，想写悬疑一点 | `story-kaifeng-guishizi-underworld`, `story-kaifeng-night-market` |
| `eval-region-kaifeng-001` | 开封夜市很热闹但我觉得很孤独，写首歌 | `region-kaifeng`, `story-kaifeng-night-market` |
| `eval-region-yanmen-001` | 雁门风雪，守城的小兵 | `region-yanmen`, `faction-guiyijun` |
| `eval-region-qingzhou-001` | 青州文津馆，书卷气和旧案 | `region-qingzhou`, `faction-wenjinguan`, `story-qingzhou-pengshan` |
| `eval-story-faction-fall-001` | 我想写一个天赋平平的弟子，师门没了，但他还是想行侠 | `story-faction-fall`, `character-player-wanderer` |
| `eval-story-small-justice-001` | 写一个路见不平的小故事，不要太宏大 | `story-small-injustice`, `gameplay-injustice` |
| `eval-gameplay-steal-001` | 偷师百家，从笨拙到自成一派 | `gameplay-steal-learning`, `gameplay-martial-mix` |
| `eval-gameplay-qishu-001` | 用点穴和摄星拿月写一点武侠浪漫 | `gameplay-qishu` |

## 污染审计结论

- 未发现阻塞级污染、缺失实体引用、敏感信息或未完成缺口。

## 交付文件

- `knowledge-base/commercial-final-provenance-manifest.json`：chunk/entity 级来源与事实等级清单。
- `knowledge-base/commercial-final-pollution-audit.json`：污染、缺失引用和敏感信息审计摘要。
- `docs/knowledge/yanyun-knowledge-p5-evidence-v0.1.md`：本报告。

## 剩余工作

- 执行真实 A/B 样本生成与人工评分，确认 B 组实际歌词质量提升。
- 公司提供官方资料包后，对 `accepted_story_synthesis` 和 `pending_clues` 逐条升降级。
- 公司确认品牌禁区、剧透口径和社区发布审核规则。
- 将 pgvector 导入真实本地数据库，跑检索召回延迟 P50/P95。
