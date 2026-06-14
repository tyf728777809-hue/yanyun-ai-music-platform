# 燕云创作知识库真实歌词 A/B v0.1

生成时间：`2026-06-14T04:29:57.090160+00:00`

## 结论

- 知识库版本：`yanyun-commercial-kb-2026-06-13-v1`。
- 模型：`deepseek-v4-pro`。
- 样本数：`4`。
- A 组：禁用知识库上下文。
- B 组：注入本地 PostgreSQL/pgvector 召回的知识库上下文。
- 胜负：B 胜 `3`，A 胜 `1`，平局 `0`。
- 平均 overall：A `6.83`，B `8.45`。
- 平均燕云世界归属感：A `6.25`，B `8.0`。
- 本报告不保存完整歌词、完整 prompt、供应商原始响应、密钥或媒体 URL；歌词仅保存 hash、标题、摘要和结构统计。

## 样本明细

| id | 期望实体 | B 召回实体 | winner | A overall | B overall | 评审摘要 |
| --- | --- | --- | --- | ---: | ---: | --- |
| `eval-character-hanxiangxun-001` | `character-hanxiangxun`, `place-buxianxian`, `place-shenxiandu` | `character-hanxiangxun`, `character-hanxiangxun`, `character-hanxiangxun`, `place-buxianxian`, `place-buxianxian`, `place-buxianxian` | `B` | 4 | 9 | A组歌词泛古风堆砌严重，缺乏寒香寻专属故事细节; B组精准融入易容、少东家、不羡仙、神仙渡等核心设定，世界归属感强 |
| `eval-character-ayinuer-001` | `character-ayinuer`, `region-liangzhou`, `story-yulu-weixiang` | `character-ayinuer`, `character-ayinuer`, `character-ayinuer`, `character-ayinuer`, `character-ayinuer` | `B` | 6.5 | 8.3 | A组天山意象偏泛西域，未明显锚定凉州或燕云具体地标；B组通过‘凉州城墙’‘玉露台’‘葡萄根’准确呼应角色背景。; B组用‘离散的人’‘葡萄酿’等细节更贴近阿依努尔的身世与回忆，故事保真度更高。 |
| `eval-story-faction-fall-001` | `story-faction-fall`, `character-player-wanderer` | `story-faction-fall`, `story-faction-fall`, `story-faction-fall`, `character-player-wanderer`, `character-player-wanderer`, `character-player-wanderer` | `B` | 8 | 9 | B 的歌词用具体场景（挨打、剑脱手、师父教握剑）更生动刻画‘天赋平平’，比 A 的抽象隐喻更贴合用户所求。; B 的 hook 记忆点虽不如 A 的‘无锋剑’意象鲜明，但情感真实度更高，易引发共鸣。 |
| `eval-heavy-story-003-jiuliu-common` | `story-jiuliu-huoqi`, `faction-jiuliumen` | `faction-jiuliumen`, `faction-jiuliumen`, `faction-jiuliumen`, `character-player-wanderer`, `character-player-wanderer`, `character-player-wanderer` | `A` | 8.8 | 7.5 | A组的市井细节（说书人醒木、玉佩暗号、护城河）更贴近燕云十六声的江湖日常，B组偏泛武侠; A组副歌‘我卖炊饼也卖了一阵心跳’记忆点强，B组‘棋子还是执棋的手’较直白 |

## 后续建议

- 如果 B 组优势稳定，下一步把 `KNOWLEDGE_RETRIEVAL_MODE=pgvector` 纳入本地真实用户测试默认配置。
- all-hit 未覆盖的多实体题，后续补实体关系扩展检索：角色 -> 地域/剧情/势力的轻量关联召回。
- 真实生产前仍需要公司确认官方资料包、品牌禁区和可剧透边界。
