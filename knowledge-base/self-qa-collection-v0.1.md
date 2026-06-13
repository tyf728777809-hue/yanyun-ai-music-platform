# 燕云创作知识库自问自答补库工作单 v0.1

## 目标

用“玩家真实提问 -> 自问自答 -> 知识库字段化”的方式持续扩充燕云创作知识库。该方法服务歌词创作质量，不要求所有内容先达到对外官方百科级证据；攻略站、玩家维基、剧情实录整理和模型自问自答综合结果可进入创作叙事层。

## 入库口径

- 官方资料 / 游戏内实录：可进入事实层，`fact_level=official_public_seed` 或 `in_game_recording`。
- 攻略站 / 玩家维基 / 剧情实录整理 / 自问自答综合：可进入创作叙事层，`source_type=guide_wiki_synthesis`，`fact_level=accepted_story_synthesis`。
- 原始社区散帖 / 评论 / 二创猜测：只做线索，不直接入库。
- 写词 Agent 可以使用创作叙事层；产品对外说明不能宣称这些都是官方实锤。

## 标准自问自答 Prompt

```text
你是《燕云十六声》创作知识库资料整理 Agent。

目标实体/主题：{entity_or_topic}
用户可能会这样创作：{player_prompt_examples}

请用攻略站、玩家维基、剧情实录整理、官方公开资料和合理自问自答，输出可直接进入创作知识库的资料卡。
不要写长篇百科，不要输出 Markdown 解释，只输出 JSON object。

字段：
{
  "entity_key": "",
  "canonical_name": "",
  "source_type": "guide_wiki_synthesis",
  "fact_level": "accepted_story_synthesis",
  "summary": "100-180 字人物传/剧情线/势力摘要",
  "cards": [
    {
      "id": "",
      "heading_path": "",
      "theme_tags": [],
      "story_phase": "spoiler_synthesis",
      "summary_for_prompt": "给 LyricsAgent 的短摘要，强调怎么写，而不是复述百科",
      "usable_imagery": [],
      "emotional_arc": "",
      "avoid_claims": [],
      "content": ""
    }
  ],
  "eval_cases": [
    {
      "input": "",
      "expected_behavior": ""
    }
  ],
  "open_questions": []
}
```

## 当前状态

2026-06-13 已完成 P0-P4 重型百科创作版补齐。商用包 `knowledge-base/commercial-final/` 当前为 68 个实体、231 个创作 chunk、131 条 eval case：

- P0 人物：高频角色已扩到多张创作卡，覆盖人生时间线、关系网络、情绪弧线、可唱视角和禁写误区。
- P1 剧情：核心剧情线已拆成起点、冲突、转折、结局/留白、创作素材和禁写项。
- P2 地域：地域/场景已从风景氛围扩展到玩家事件、世界归属感和可写歌方向。
- P3 势力：门派/势力已补组织气质、代表冲突、玩家视角和禁写误区。
- P4 玩法：玩法体验已转成普通玩家故事素材，重点服务偷师、奇术、寻声、营生、不平事、镇守、同行、侠迹残章等创作场景。

`pending_clues` 允许进入写词 prompt，但必须被表达为暗线、传闻、旧事、情绪或意象，不写成官方定论。

## 后续精修队列

- P5 评测证据：补 A/B eval 报告、chunk 级 provenance manifest、污染审计报告和公司交付证据包。
- 公司资料覆盖：如果拿到官方内部角色设定、剧情梗概、任务线或宣发禁区，应覆盖同名实体的叙事层摘要。
- 实录精修：后续用游戏内录屏/截图/OCR/ASR 对关键角色和任务线做逐条核验，提升事实层置信度。
- 品牌边界：需要公司确认不可写、不可误写、不可剧透、不可引导的内容，继续补充 `creative-boundaries.json`。
- 质量回归：用真实玩家题库持续对比 `KNOWLEDGE_RETRIEVAL_MODE=disabled` 与 `pgvector`，确认知识库提升燕云归属感但不压制玩家创意。

## 子 Agent 输出使用规则

- 子 Agent 可以直接输出“可贴进 JSON 的草案”，但主 Agent 必须统一命名、别名、`source_type`、`fact_level` 和 chunk id。
- 子 Agent 的长篇人物传不直接进 Prompt；进入 Prompt 的只能是短摘要、情绪弧线、意象和禁写误区。
- 如果子 Agent 发现实体边界错误，优先修实体边界，再补内容，例如鬼市子应先判断是人物、场景还是身份壳。

## 下一批建议顺序

1. 做 P5 评测证据包，验证重型知识库对歌词质量、燕云归属感和用户故事保真的提升。
2. 用公司/官方资料包覆盖高风险人物、剧情和品牌禁区。
3. 对最常用的角色歌、剧情歌和普通玩家故事做人工审稿样本集。
4. 再决定是否扩展更多低频 NPC、活动剧情和版本新增内容。
