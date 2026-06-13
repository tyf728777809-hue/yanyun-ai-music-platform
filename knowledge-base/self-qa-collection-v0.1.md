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

## 当前缺口

### P0 高频人物传

当前已完成首批：寒香寻升级为剧情整合人物传；郑鄂、容鸢/慕容源、鬼市子完成实体边界修正；江无浪、褚清泉、红线、伊刀、天不收已新增高频人物卡。以下人物仍主要停留在公开种子卡，需要继续补到 3-5 张创作卡：

- 寻心：弱水岸、心魔/身份试炼、与寒香寻/洛神关系、玩家如何写空灵角色歌。
- 千夜：绣金楼、夜行、毁灭欲、与寒香寻旧怨/亲缘暗示、火烧不羡仙。
- 阿依努尔：凉州、玉露为乡、身份与故乡、家族/地域情绪。
- 田英：清河压迫感、强敌、局中人、玩家对手歌。
- 叶万山：江湖强者、师承/对手关系、适合英雄迟暮或压迫感。
- 小十七：少年感、同行、轻快与成长。
- 道主 / 无相皇：势力核心、世界暗面、人物歌和反派歌角度。
- 江无浪、红线、褚清泉、伊刀、天不收：已完成首批实体卡；后续可继续补剧情阶段与关系图。

子 Agent A 盘点后的优先修正：

- 郑鄂：当前库中“记录者锚点”定位失真，应改为“常平使 / 救世初心 / 常平一炬 / 白衣折扇 / 霜枪”的人物线。推荐 chunk：`character-zhenge-changping-fall`。
- 容鸢/慕容源：存在命名冲突，应先核对官方中文名。推荐 chunk：`character-rongyuan-momen-revenge`。
- 鬼市子：实体边界不清，更像开封鬼市身份壳或地下场景，不宜直接写成稳定 NPC 传记。推荐改挂剧情/场景 chunk：`story-kaifeng-guishizi-underworld`。
- 江无浪：高频缺失角色，应新增人物卡，承接“江叔 / 养父 / 带婴儿逃亡 / 竹林抚养 / 忽然远行”。推荐 chunk：`character-jiangwulang-fosterfather-arc`。
- 褚清泉：寒香寻角色歌高频共现人物，应新增 standalone 角色卡，承接“边塞 / 家国私情 / 换脸拒绝 / 永别”。推荐 chunk：`character-chuqingquan-borderfarewell`。

### P1 剧情/任务线

当前剧情线多为 1 张摘要卡，缺起点、冲突、转折、结局、可写角度：

- 清河主线：神仙不渡、菱花尘满、为谁归去、梦境不羡仙。
- 弱水岸寻心：寻心、洛神、身份与记忆。
- 九流祸起 / 九流暗涌：局中局、势力操控、普通人卷入。
- 无名剑谱：失落传承、自成一派、鬼市子。
- 玉露为乡：凉州、阿依努尔、身份和归乡。
- 青州蓬山文津：书卷气、文津馆、旧案与求索。
- 开封夜市与鬼市：繁华、暗面、交易和孤独。

子 Agent B 盘点后的优先补线：

- 清河主线 / 神仙不渡：拆成 `又见新来燕`、`匹马映林嘶`、`菱花尘满`、`为谁归去` 和不羡仙状态变化。推荐 chunk：`story.qh.main.00-arc.v1`、`story.qh.main.30-linghuachenman.v1`、`story.qh.main.40-weisheguiqu.v1`。
- 弱水岸 / 寻心：拆成入井、机关、洛神笔迹、乐谱玉笛、思芳歌、幻境、寻心战、无面人。推荐 chunk：`story.qh.ruoshuian.00-arc.v1`、`story.qh.ruoshuian.40-xunxin.v1`。
- 九流线：补鬼市入口、阴兵伪装、朱佑生、朱小家/道主、九流门创派与叛变。推荐 chunk：`faction.jiuliu.00-overview.v1`、`story.kf.ghostmarket.20-yinbingjiedao.v1`。
- 玉露为乡：补玉露台、藤王村、月光酒、阿依奴、望帝乡、拼月回忆。推荐 chunk：`story.lz.yuluweixiang.00-arc.v1`。
- 青州文津：补文津馆有教无类、考试入门、读书晋升、同窗日常、经世之志。推荐 chunk：`region.qz.wenjinguan.00-overview.v1`。
- 雁门 / 长安：当前应先做 stub，不硬补完整剧情；雁门缺地理人物主线，长安先作为象征地域。

### P2 地域/场景

当前地域多为氛围卡，需要补“玩家在这里会发生什么”和“适合写什么歌”：

- 清河、神仙渡、不羡仙：归家、童年、梨花、酒、失家。
- 弱水岸：记忆、倒影、身份、地下旧事。
- 开封：夜市、樊楼、鬼市、繁华与孤独。
- 雁门：风雪、守城、边塞小人物。
- 凉州：风沙、远方、归属。
- 青州/蓬山/文津馆：书卷、山雨、求索。
- 长安：大城、旧都、盛世余响。

### P3 势力/门派

当前势力多为 1 张方向卡，缺组织气质、代表人物、冲突和禁写误区：

- 九流门、不老仙、醉花阴、文津馆、无心谷、三更天、赤龙堂、天泉、梨园、青溪、狂澜、墨山道、归义军。

子 Agent C 盘点后的优先补派：

- 缺口最大：赤龙堂、归义军、不老仙、寻声。先做占位与证据清单，不写硬设定。
- 最适合先提升歌词质量：九流门、醉花阴、狂澜、青溪、梨园。
- 势力卡必须写“组织气质 + 玩家会怎么唱 + 禁写误区”，不要先补百科历史。
- 推荐 chunk：`kb.faction.jiuliumen.lyric_brief.v1`、`kb.faction.zuihuayin.lyric_brief.v1`、`kb.faction.kuanglan.lyric_brief.v1`、`kb.faction.qingxi.lyric_brief.v1`、`kb.faction.liyuan.lyric_brief.v1`。

### P4 玩法体验

当前玩法已覆盖但偏薄，需要转成玩家故事素材：

- 偷师百家：从笨拙模仿到自成一派。
- 奇术：点穴、摄星拿月等如何变成歌词意象。
- 寻声：标题核心概念，声音、回声、故事、记忆。
- 营生 / 不平事：普通玩家故事，不宏大救世。
- 镇守 / 多人同行：失败、重来、并肩、过关后的情绪。
- 侠迹残章：碎片化故事和玩家补完感。

子 Agent C 盘点后的玩法补法：

- 奇术：写成奇人异技与江湖巧劲，不写仙侠法宝。推荐 chunk：`kb.gameplay.qishu.lyric_brief.v1`。
- 偷师：写观察、模仿、失手和借火，不写修仙顿悟。推荐 chunk：`kb.gameplay.toushi.experience.v1`。
- 寻声：当前资料最不稳，先做“声音寻缘 / 风声回响”占位，不要把端外活动硬当主玩法。推荐 chunk：`kb.gameplay.xunsheng.placeholder.v1`。
- 镇守：写地方记忆和玩家并肩，不堆机制词。推荐 chunk：`kb.gameplay.zhenshou.theme.v1`。
- 多人同行：写江湖同路，不写公会战口号。推荐 chunk：`kb.gameplay.duorentongxing.emotion.v1`。

## 子 Agent 输出使用规则

- 子 Agent 可以直接输出“可贴进 JSON 的草案”，但主 Agent 必须统一命名、别名、`source_type`、`fact_level` 和 chunk id。
- 子 Agent 的长篇人物传不直接进 Prompt；进入 Prompt 的只能是短摘要、情绪弧线、意象和禁写误区。
- 如果子 Agent 发现实体边界错误，优先修实体边界，再补内容，例如鬼市子应先判断是人物、场景还是身份壳。

## 下一批建议顺序

1. 补 P0 高频人物传，先让角色歌质量明显提升。
2. 补清河主线与弱水岸寻心，因为它们和寒香寻/寻心/主角童年强绑定。
3. 补九流、无名剑谱、玉露为乡、开封鬼市、青州文津等高频剧情。
4. 补门派/势力，支持玩家点名门派写歌。
5. 补玩法体验，支持普通玩家故事和社交分享场景。
