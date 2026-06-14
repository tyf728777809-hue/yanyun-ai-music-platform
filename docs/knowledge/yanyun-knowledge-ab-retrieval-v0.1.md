# 燕云创作知识库 A/B 检索评测 v0.1

生成时间：2026-06-14

## 结论

- 知识库版本：`yanyun-commercial-kb-2026-06-13-v1`。
- 样本数：`48`。
- A 组：`KNOWLEDGE_RETRIEVAL_MODE=disabled`，知识上下文数量恒为 `0`。
- B 组：`KNOWLEDGE_RETRIEVAL_MODE=pgvector`，从本地 PostgreSQL/pgvector 召回上下文。
- B 组 any-hit 召回率：`1.0`。
- B 组 all-hit 召回率：`0.7083`。
- B 组延迟：P50 `249.48ms`，P95 `447.53ms`，max `608.74ms`。
- 本轮没有调用真实 DeepSeek；这是检索/上下文层 A/B，不是最终歌词质量 A/B。

## 判定

- 通过：知识库已经足够进入真实模型 A/B。

## 失败与部分命中

- any-hit 失败样本：`0`。
- 部分命中样本：`14`。

| id | 输入 | 期望实体 | B 组召回实体 |
| --- | --- | --- | --- |
| `eval-character-hanxiangxun-001` | 以寒香寻的人生经历写一首歌，想要少年游和不羡仙的感觉 | `character-hanxiangxun`, `place-buxianxian`, `place-shenxiandu` | `character-hanxiangxun`, `character-hanxiangxun`, `character-hanxiangxun`, `place-buxianxian`, `place-buxianxian`, `place-buxianxian` |
| `eval-character-ayinuer-001` | 写一首阿依努尔的角色歌，主题是故乡和身份 | `character-ayinuer`, `region-liangzhou`, `story-yulu-weixiang` | `character-ayinuer`, `character-ayinuer`, `character-ayinuer`, `character-ayinuer`, `character-ayinuer` |
| `eval-character-xunxin-001` | 帮我写寻心线的歌，空灵一点 | `character-xunxin`, `story-ruoshui-xunxin`, `place-ruoshui-an` | `story-ruoshui-xunxin`, `story-ruoshui-xunxin`, `story-ruoshui-xunxin`, `character-xunxin`, `character-xunxin`, `character-xunxin` |
| `eval-character-ayinuer-alias-001` | 阿依奴的歌，写玉露台和月光酒，不要写得猎奇 | `character-ayinuer`, `story-yulu-weixiang` | `region-yulutai`, `region-yulutai`, `character-ayinuer`, `character-ayinuer`, `boundary-sensitive-content`, `character-li-jiaojiao` |
| `eval-character-ye-wanshan-001` | 叶万山，北地守城名将那种沉默背负 | `character-ye-wanshan`, `region-yanmen` | `character-ye-wanshan`, `character-ye-wanshan`, `character-ye-wanshan`, `character-ye-wanshan`, `character-ye-wanshan` |
| `eval-character-jiangwulang-001` | 写江叔带着少东家在清河长大的感觉 | `character-jiangwulang`, `region-qinghe`, `place-buxianxian` | `gameplay-homestead-buxianxian`, `gameplay-homestead-buxianxian`, `region-qinghe`, `region-qinghe`, `character-jiangwulang`, `character-jiangwulang` |
| `eval-character-hongxian-001` | 红线和少东家少年同行，想要轻快但后劲酸 | `character-hongxian`, `place-shenxiandu` | `character-hongxian`, `character-hongxian`, `character-hongxian`, `gameplay-homestead-buxianxian`, `gameplay-homestead-buxianxian`, `gameplay-homestead-buxianxian` |
| `eval-region-yanmen-001` | 雁门风雪，守城的小兵 | `region-yanmen`, `faction-guiyijun` | `region-yanmen`, `region-yanmen`, `region-yanmen`, `character-companion-yangfeng-goose`, `faction-palace-institutions` |
| `eval-region-qingzhou-001` | 青州文津馆，书卷气和旧案 | `region-qingzhou`, `faction-wenjinguan`, `story-qingzhou-pengshan` | `faction-wenjinguan`, `faction-wenjinguan`, `region-qingzhou`, `region-qingzhou`, `character-qing-moshandao`, `character-qing-moshandao` |
| `eval-gameplay-steal-001` | 偷师百家，从笨拙到自成一派 | `gameplay-steal-learning`, `gameplay-martial-mix` | `gameplay-steal-learning`, `gameplay-steal-learning`, `gameplay-steal-learning`, `story-faction-fall` |
| `eval-heavy-boundary-008-plot-certain` | 千夜一定是寒香寻亲人，就这么写 | `boundary-unconfirmed-plot`, `character-qianye`, `character-hanxiangxun` | `character-hanxiangxun`, `character-hanxiangxun`, `character-hanxiangxun`, `character-qianye`, `character-qianye`, `character-qianye` |
| `eval-character-zhang-yanlin-001` | 以张彦霖和河神祠写一首悼亡感的歌 | `character-zhang-yanlin`, `region-kaifeng` | `character-zhang-yanlin`, `character-zhang-yanlin`, `character-zhang-yanlin`, `character-jiangwulang` |
| `eval-character-zhang-huaishen-001` | 写张淮深的歌，要有归义军后续历史的感觉 | `character-zhang-huaishen`, `region-liangzhou`, `faction-guiyijun` | `character-zhang-huaishen`, `character-zhang-huaishen`, `character-zhang-huaishen`, `faction-guiyijun`, `faction-guiyijun`, `faction-guiyijun` |
| `eval-character-fubao-001` | 写福宝的歌，要有伙伴/福气意象的感觉 | `character-fubao`, `character-companion-four-cats-three-geese` | `gameplay-companion-split`, `gameplay-companion-split`, `gameplay-companion-split`, `character-fubao`, `character-fubao`, `character-fubao` |

## 后续真实模型 A/B

当前 shell 没有 `DEEPSEEK_API_KEY`，所以本轮不执行真实写词 A/B。拿到可用 key 后，建议使用同一批样本：

- A 组：禁用知识库生成歌词。
- B 组：启用 `pgvector` 生成歌词。
- 人工或 QualityEvaluationAgent 对世界归属感、用户故事保真、角色/剧情准确性、可唱性和泛古风污染打分。
