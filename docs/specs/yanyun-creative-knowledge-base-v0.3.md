# 燕云创作知识库 v0.3

## Summary

燕云创作知识库用于补足 DeepSeek 对《燕云十六声》公测后角色、剧情、地域、玩法和玩家语境的缺口。它不是联网搜索、百科问答或剧情审稿器，而是受控的创作素材库：帮助 Agent 理解“这个故事如何属于燕云世界”，但不限制玩家创意，不要求歌词必须出现“燕云”“十六声”等关键词。

## Source Policy

- 首版资料来源优先级：公司/官方资料包、公开官方资料、游戏内实录整理。
- 社区资料只可作为发现线索，不直接进入事实层。
- 公开来源登记见 `docs/integrations/yanyun-knowledge-source-registry.md`。
- 每个知识版本必须记录 `kb_version`、`content_as_of`、来源策略和导入时间。
- 每条资料分事实层与创作层：事实层防止角色/剧情写错；创作层提供情绪、意象、歌词角度和禁写误区。

## Runtime Behavior

- 默认 `KNOWLEDGE_RETRIEVAL_MODE=disabled`；显式设置为 `pgvector` 才启用真实知识检索。
- 点名角色、地域、门派、任务线时，先走实体别名命中，再取对应 chunk。
- 未点名实体时，使用 pgvector 按主题和情绪检索。
- 每次注入 3-6 条短上下文，只包含摘要、情绪弧线、可用意象和禁写项，不塞官方原文大段文本。
- 检索失败降级为空知识上下文，不阻断创作。

## First Seed

首版种子文件为 `knowledge-base/yanyun-official-kb-v1.json`，覆盖：

- 人物样例：寒香寻、寻心。
- 地域样例：清河、开封、雁门、凉州、长安、青州 / 蓬山 / 文津馆。
- 场景样例：不羡仙 / 神仙渡、开封夜色。
- 剧情/线索样例：玉露为乡、弱水岸寻心、九流线索、无名剑谱。
- 主题样例：小人物侠义、门派倾覆。
- 玩法样例：奇术、偷师百家、寻声、中式解谜、武学自由搭配、营生与不平事。

该种子只是工程打通和检索行为验证样本，不代表完整官方知识库。后续需要公司/官方资料包和游戏内实录继续补齐高频角色、剧情/任务线、地域、势力/门派和玩法体验。

## Import

本地导入：

```bash
python3 scripts/knowledge/import-yanyun-knowledge.py knowledge-base/yanyun-official-kb-v1.json
```

需要 PostgreSQL 已支持 `vector` 扩展。Docker Compose 已将本地 PostgreSQL 镜像切到 `pgvector/pgvector:pg16`；已有容器如未重建，需重建 postgres 服务后再执行 migration 和导入。

导入脚本优先使用本机 `psql`；如果本机没有安装 PostgreSQL 客户端，会自动 fallback 到：

```bash
docker exec -i ${POSTGRES_CONTAINER:-yanyun-postgres} psql -U ${POSTGRES_USER:-postgres} -d ${POSTGRES_DB:-yanyun_music}
```

本地验证命令：

```bash
KNOWLEDGE_RETRIEVAL_MODE=pgvector \
KNOWLEDGE_KB_VERSION=yanyun-official-kb-2026-06-13-v1 \
java -jar apps/music-api/build/libs/music-api-0.1.0-SNAPSHOT.jar
```

然后创建一条点名角色或剧情的作品，检查 `agent_runs` 中是否存在 `KnowledgeRetrieve / SUCCEEDED`，并检查 `lyrics_drafts.knowledge_base_version` 和 `lyrics_drafts.yanyun_references_json`。

## Agent Rules

- `CreativeBriefAgent` 判断创作类型和世界观归属，不替玩家选题。
- `LyricsAgent` 使用知识摘要作为素材，不照抄资料，不写剧情百科。
- `QualityEvaluationAgent` 检查世界归属感、角色/剧情错误、用户故事保真、泛古风套词和关键词堆砌。
- 普通玩家故事不强绑官方角色；角色歌才使用对应人物资料。

## Acceptance

- “以寒香寻的人生经历写歌”命中寒香寻和不羡仙相关资料。
- “天赋平平的善良剑客，门派覆灭”命中小人物侠义和门派倾覆资料，不强行绑定官方角色。
- “高达主题歌”仍拒绝；“高达那种孤独热血感但改成燕云”可转译但不能保留其他 IP 专有名词。
- 检索耗时目标：P50 < 500ms，P95 < 1.5s。
- 日志只记录 `kb_version`、chunk id、entity id、耗时和 hash，不记录完整 prompt、官方原文或供应商响应。
