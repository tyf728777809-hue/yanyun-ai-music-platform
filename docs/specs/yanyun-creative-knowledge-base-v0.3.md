# 燕云创作知识库 v0.3

## Summary

燕云创作知识库用于补足 DeepSeek 对《燕云十六声》公测后角色、剧情、地域、玩法和玩家语境的缺口。它不是联网搜索、百科问答或剧情审稿器，而是受控的创作素材库：帮助 Agent 理解“这个故事如何属于燕云世界”，但不限制玩家创意，不要求歌词必须出现“燕云”“十六声”等关键词。

## Source Policy

- 首版资料来源优先级：公司/官方资料包、公开官方资料、游戏内实录整理、攻略站/玩家维基/剧情整理综合资料。
- 本知识库服务 AI 作词与创作，不是对外剧情百科。攻略站、玩家维基、剧情实录整理和自问自答结果可以进入“创作叙事层”，用于串联人物传、剧情线和可写意象。
- 未经整理的原始社区散帖不直接入库；经过交叉整理后的资料使用 `source_type=guide_wiki_synthesis` 或 `community` + `fact_level=accepted_story_synthesis`。
- 公开来源登记见 `docs/integrations/yanyun-knowledge-source-registry.md`。
- 每个知识版本必须记录 `kb_version`、`content_as_of`、来源策略和导入时间。
- 每条资料分事实层与创作层：事实层防止角色/剧情写错；创作层提供情绪、意象、歌词角度和禁写误区。

## Self-QA Collection

允许使用自问自答作为资料扩充方法。标准流程：

1. 先模拟玩家真实提问，例如“以寒香寻的一生写一首歌”“写千夜的孤独和夜行”“写九流门局中局”。
2. 让模型输出该问题需要的资料结构：人物经历、关系、剧情阶段、地域、情绪弧线、可用意象、禁写误区。
3. 再让模型按知识库字段直接输出候选卡片：`summary_for_prompt`、`usable_imagery`、`emotional_arc`、`avoid_claims`。
4. 主 Agent 负责收敛和去重，标记来源等级为 `accepted_story_synthesis`，并避免写成百科复述。
5. 如果后续公司需要对外剧情准确性，再把对应卡片回查官方/实录来源；作词链路可以先使用创作叙事层。

## Runtime Behavior

- 自动化测试和 CI 默认 `KNOWLEDGE_RETRIEVAL_MODE=disabled/mock`；本地真实写词体验和 `public-real-full-experience` smoke 默认使用 `pgvector`。
- 点名角色、地域、门派、任务线时，先走实体别名命中，再取对应 chunk。
- 真实用户常用说法可以作为关系桥接别名维护，例如“九流门局中局”“普通小摊贩”命中 `九流祸起` 剧情线，避免只召回门派百科卡。
- 未点名实体时，使用 pgvector 按主题和情绪检索。
- 运行时允许注入 `confirmed_facts`、`creative_materials` 和 `pending_clues`；其中 `pending_clues` 必须带内部口径提示，只能作为暗线、传闻、情绪或意象使用，不写成官方定论。
- 每次注入 3-6 条短上下文，只包含摘要、情绪弧线、可用意象和禁写项，不塞官方原文大段文本。
- 检索失败降级为空知识上下文，不阻断创作。

## Commercial Knowledge Package

商用默认入口为 `knowledge-base/commercial-final/`，导入时合并为 `yanyun-commercial-kb-2026-06-13-v1`。当前已按 7 个文件分域维护：

- `characters.json`：角色、玩家游侠承接位。
- `storylines.json`：剧情/任务线和普通玩家故事模板。
- `regions.json`：地域、场景、城市气质。
- `factions.json`：势力/门派/江湖组织。
- `gameplay.json`：玩法体验与玩家行为素材。
- `creative-boundaries.json`：创作禁区、版权和商业边界。
- `eval-cases.json`：商用回归题库。

当前商用包包含 280 个实体、917 个创作 chunk 和 336 条 eval case，缺口盘点表已全部回写为 present，并已生成 P5 离线证据包：A/B 评测协议、chunk/entity 级 provenance manifest 和污染审计报告。它仍不是对外官方剧情百科：公司内部禁写边界、版本级剧透口径、实录逐字证据和真实模型 A/B 人工评分仍需后续补齐。

旧 `knowledge-base/yanyun-official-kb-v1.json` 仅作为历史单文件工程种子保留。

## Import

本地导入：

```bash
STRICT_COMMERCIAL=1 python3 scripts/knowledge/validate-yanyun-knowledge.py knowledge-base/commercial-final
python3 scripts/knowledge/import-yanyun-knowledge.py knowledge-base/commercial-final
```

需要 PostgreSQL 已支持 `vector` 扩展。Docker Compose 已将本地 PostgreSQL 镜像切到 `pgvector/pgvector:pg16`；已有容器如未重建，需重建 postgres 服务后再执行 migration 和导入。

导入脚本优先使用本机 `psql`；如果本机没有安装 PostgreSQL 客户端，会自动 fallback 到：

```bash
docker exec -i ${POSTGRES_CONTAINER:-yanyun-postgres} psql -U ${POSTGRES_USER:-postgres} -d ${POSTGRES_DB:-yanyun_music}
```

本地验证命令：

```bash
KNOWLEDGE_RETRIEVAL_MODE=pgvector \
KNOWLEDGE_KB_VERSION=yanyun-commercial-kb-2026-06-13-v1 \
java -jar apps/music-api/build/libs/music-api-0.1.0-SNAPSHOT.jar
```

然后创建一条点名角色或剧情的作品，检查 `agent_runs` 中是否存在 `KnowledgeRetrieve / SUCCEEDED`，并检查 `lyrics_drafts.knowledge_base_version` 和 `lyrics_drafts.yanyun_references_json`。

## Agent Rules

- `CreativeBriefAgent` 判断创作类型和世界观归属，不替玩家选题。
- `LyricsAgent` 使用知识摘要作为素材，不照抄资料，不写剧情百科。
- `QualityEvaluationAgent` 检查世界归属感、角色/剧情错误、用户故事保真、泛古风套词和关键词堆砌。
- 普通玩家故事不强绑官方角色；角色歌才使用对应人物资料。

## Acceptance

- “以寒香寻的人生经历写歌”命中寒香寻剧情整合人物传、不羡仙、神仙渡和洛神/寻心相关创作意象。
- “天赋平平的善良剑客，门派覆灭”命中小人物侠义和门派倾覆资料，不强行绑定官方角色。
- “高达主题歌”仍拒绝；“高达那种孤独热血感但改成燕云”可转译但不能保留其他 IP 专有名词。
- 检索耗时目标：P50 < 500ms，P95 < 1.5s。
- 日志只记录 `kb_version`、chunk id、entity id、耗时和 hash，不记录完整 prompt、官方原文或供应商响应。
