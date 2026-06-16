# 写歌词 Agent v0.8 金曲奖级调优方案

## 目标

v0.8 只调写词质量，不改前端、OpenAPI、数据库、音乐、封面或视频链路。目标是把 LyricsAgent 从“能写燕云相关歌词”推进到“能写有作品感、能被听懂、值得继续打磨的歌”。

本轮保持现有知识库检索和注入方式不变。知识库仍提供燕云事实、人物关系、场景气质和可用意象，但不能变成歌词里的资料清单。

## 生产链路变化

- `CreativeBriefAgent` 从 v0.7 升级到 v0.8。
- 新增核心判断字段：
  - `song_core`
  - `singer_voice`
  - `listener_target`
  - `emotional_engine`
  - `chorus_job`
  - `avoid_direction`
- 旧字段如 `creative_core`、`song_thesis`、`chorus_function` 继续保留，但只作为辅助。
- `LyricsAgent` 优先完成一首歌，而不是逐条满足规则。
- `QualityEvaluationAgent` 增加 Prompt + 歌词联评口径，但不作为最终评委。

## 评审原则

评审必须一起看：

1. 用户原始输入。
2. 知识库召回。
3. CreativeBrief。
4. LyricsAgent system/user prompt。
5. 最终歌词。

不能只看歌词孤立评价，也不能只看 Prompt 是否写得漂亮。

## 100 分评分表

| 维度 | 分值 |
| --- | ---: |
| 歌曲命题 | 12 |
| 叙事视角 | 10 |
| 情感真实度 | 12 |
| 独特入口 | 10 |
| 语言准确度 | 10 |
| 副歌功能 | 10 |
| 声音记忆点 | 10 |
| 韵律可唱性 | 10 |
| 段落推进 | 8 |
| 意象系统 | 6 |
| 燕云归属感 | 8 |
| 原创高级感 | 4 |

## 一票否决

- 听不懂这首歌在唱什么。
- 像散文、剧情简介、设定介绍。
- 副歌没有功能。
- 只是堆燕云名词。
- 漂亮但空。
- 用户故事被丢失。
- 完全不可唱。

## 结论标签

- `金曲潜力`：有明确作品核，值得继续打磨。
- `可用但不够顶`：用户可能接受，但不能作为调优成功样本。
- `不通过`：方向失败，需要重写。

## 失败归因

- `用户输入太模糊`
- `知识库召回干扰`
- `CreativeBrief 方向错误`
- `LyricsAgent Prompt 规则过载`
- `LyricsAgent 执行弱`
- `QualityEvaluation 没拦住`

## A/B 流程

首批只跑 6 条：

```bash
MODE=plan python3 scripts/knowledge/run-yanyun-lyrics-agent-v08-ab.py
MODE=preflight python3 scripts/knowledge/run-yanyun-lyrics-agent-v08-ab.py
ALLOW_REAL_MODEL_SMOKE=1 ALLOW_DEEPSEEK_REAL_AB=1 MODE=execute_manual_review python3 scripts/knowledge/run-yanyun-lyrics-agent-v08-ab.py
```

完整 Prompt 和歌词输出到：

```text
build/reports/lyrics-agent/
```

该目录被 Git 忽略，不得提交。

如果 v0.8 首批 6 条少于 4 条明显优于 v0.7，不扩大到 24 条，先继续改 Prompt。

## 首批 A/B 结论

首批 6 条真实 DeepSeek A/B 已完成，完整 Prompt 与完整歌词只保存在 ignored 的 `build/reports/lyrics-agent/`。
可提交的脱敏评审摘要保存在 `knowledge-base/lyrics-agent-v0.8-ab-review-summary.json`。

结论：

- v0.8 初版只赢 1/6，不进入 24 条扩大评测。
- v0.8 的优势是歌曲命题更清楚，副歌功能更明确。
- v0.8 的主要问题是把 `song_core` 直接唱成显眼口号，削弱了真实细节、暧昧空间和人物质感。
- v0.7 在多条样本中胜出，是因为它保留了更奇异、更生活化、更像真实玩家记忆的声音装置。

v0.8.1 调整方向：

- `song_core` 作为暗中统领，不直接改写成反复喊的主题句。
- `chorus_job` 通过具体声音、动作、物件、句式变奏或意象回环完成，而不是只重复题目或结论。
- 每首歌至少保留一个不那么标准但真实的细节，来自用户输入或知识库。
- 轻量、亲密的输入避免过度解释；粗粝题材避免空喊、泛化反叛和无必要粗口。

## 通过标准

第二批 24 条扩大评测时：

- v0.8 至少 16/24 优于 v0.7。
- 至少 6/24 达到 `金曲潜力`。
- 至少 6 类输入都有胜出样本。
- 不允许所有歌同一种结构、口吻或悲情套路。
- 不牺牲燕云归属感和用户故事保真。
