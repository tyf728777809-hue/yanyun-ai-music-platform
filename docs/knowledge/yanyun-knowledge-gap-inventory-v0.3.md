# 燕云创作知识库缺口盘点表 v0.3（已补齐）

盘点时间：2026-06-13；最终补齐时间：2026-06-14

本表用于把当前商用知识库后续应补的人物、剧情、地域、势力、玩法、创作边界和 eval 缺口一次性盘清楚。它不是正式知识库正文，不会直接参与写词检索；后续补库应按本表逐行转成 `knowledge-base/commercial-final/` 下的分域卡片。

## 当前结论

- 当前正式库已完成缺口表一次性补齐：`knowledge-base/commercial-final/` 共 `280` 个实体、`917` 个创作 chunk、`336` 条 eval case。
- 分域覆盖：人物文件 `130` 个实体、剧情文件 `34` 个实体、地域文件 `41` 个实体、势力文件 `32` 个实体、玩法文件 `30` 个实体、创作边界 `13` 个实体；缺口盘点表 `278` 行均已回写为 `已有`。
- 本轮把剩余 `155` 条非 `present` 缺口全部转入正式库或 eval 题库：人物 `52`、剧情 `19`、地域 `31`、势力 `19`、玩法 `20`、创作边界 `6`、eval `8`。
- `pending_clues` 仍可进入创作素材层，但只能写成暗线、传闻、旧事、情绪或意象，不得写成官方定论。
- 这版是“商用创作知识库”补齐，不等同于公司内部完整剧情圣经；后续若公司提供官方资料包，应按同名实体覆盖或增厚。
- 江南/杭州、陈子奚、玉山君、文津馆十相等截至 2026-06-13 属于未来/预告资料，不进当前 live 主库事实层。

## 覆盖统计

| 类型 | 总行数 | 已有 | 缺失 | 偏薄 | 需拆分 | 仅待核线索 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 人物 | 130 | 130 | 0 | 0 | 0 | 0 |
| 剧情/任务线 | 30 | 30 | 0 | 0 | 0 | 0 |
| 地域/场景 | 41 | 41 | 0 | 0 | 0 | 0 |
| 势力/门派/组织 | 29 | 29 | 0 | 0 | 0 | 0 |
| 玩法体验 | 27 | 27 | 0 | 0 | 0 | 0 |
| 创作边界 | 13 | 13 | 0 | 0 | 0 | 0 |
| 回归题库 | 8 | 8 | 0 | 0 | 0 | 0 |

## 最终补齐审计 v0.3

- 本轮补齐前剩余非 `present` 缺口：`155` 条；补齐后剩余非 `present` 缺口：`0`。
- 正式库新增实体：人物 `52`、剧情 `19`、地域 `31`、势力 `19`、玩法 `20`、创作边界 `6`。
- 正式库新增创作卡：`441` 张；新增/补齐 eval case：`149` 条。
- 表格行的 `已有` 表示已经具备可检索、可注入、可用于作词的结构化卡片；不表示该条目已完成公司内部逐条官方复核。
- 对待核线索类条目，正式库使用 `pending_clues` 口径，运行时必须作为暗线/传闻/意象使用。

## 人物补漏审计 v0.2

- 本轮新增人物/群像缺口：`54` 条，当前人物盘点共 `130` 行。
- 已补入的核心漏项包括：赵光义/晋中原、张彦霖、周蔷、画睛兄、兰澳、薛丑、洪肆、魏仁浦、冯继昇、燕/应宁、蓬山四海豪客、四猫三鹅伙伴群像和墨山道代号人物。
- 第一批已转入正式人物库：`19` 条；第二批又转入/增厚 `13` 条；第三批转入 `12` 条；第四批转入 `12` 条；第五批转入 `6` 条，当前人物盘点 `已有` 为 `78` 条；P1 人物缺口已清空，这些条目仍可继续用实录和公司资料增厚。
- 四猫三鹅：官方确认首期伙伴群和系统上线；七个个体名多来自社区实机/攻略，先标 `pending_clues_only`，不写成官方命名定论。
- 普通 NPC：清河、开封、河西/凉州、青州/蓬山优先做群像卡；只有乡德美、宋五、方旭、靳春娘这类个人议题明显的普通 NPC 先列为 P3 单卡样本。
- 未来预告：陈子奚、玉山君、陈氏、钱王/杭州传说、文津馆十相只进 future/watchlist 或预告层，不作为当前已上线人物事实。

## 来源口径

- `official_public_seed`：官网、官方公告、商店页等可以支撑的公开事实。
- `accepted_story_synthesis`：攻略/维基/剧情实录/自问自答综合出的创作叙事层，可用于写词，不对外宣称官方定论。
- `pending_clues`：单源、争议、前瞻、玩家猜测，只能作为暗线或意象。
- `creative_guidance`：创作和质量门规则，不是剧情事实。

## 盘点表

### 人物

| ID | 名称 | 状态 | 优先级 | 来源等级 | 建议补卡 | 写词影响 / 风险 |
| --- | --- | --- | --- | --- | --- | --- |
| `character-chai-rong` | 柴荣 | 已有 | P0 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第二批补入或增厚，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第二批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-guo-xin` | 郭昕 | 已有 | P0 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第二批补入或增厚，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第二批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-huiyao` | 慧药 | 已有 | P0 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第二批补入或增厚，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第二批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-li-zuo` | 李祚 | 已有 | P0 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第二批补入或增厚，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第二批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-ma-budai` | 麻布袋 | 已有 | P0 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第二批补入或增厚，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第二批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-shen-yilun` | 沈义伦 | 已有 | P0 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第二批补入或增厚，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第二批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-tang-xinci` | 唐新词 | 已有 | P0 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第二批补入或增厚，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第二批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-wang-qing` | 王清 | 已有 | P0 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第二批补入或增厚，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第二批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-yaoyaoyao` | 姚药药 | 已有 | P0 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第二批补入或增厚，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第二批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-yingying-wenwuque` | 盈盈/温无缺 | 已有 | P0 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第二批补入或增厚，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第二批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-zhang-yichao` | 张议潮 | 已有 | P0 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第二批补入或增厚，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第二批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-zhang-yanlin` | 张彦霖 | 已有 | P0 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：所属势力和完整经历仍薄，避免把玩家整理扩写成官方定论。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-zhao-guangyi-jinzhongyuan` | 赵光义/晋中原 | 已有 | P0 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：当前主要来自百科/攻略综合；具体人格、剧情走向和结局需用游戏内实录或公司资料复核。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-zhou-qiang` | 周蔷 | 已有 | P0 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：需区分游戏设定与历史借影，不写成官方已确认历史原型。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-an-liuli` | 安琉璃 | 已有 | P1 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第四批补入，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第四批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-bai-chuyu` | 柏楚玉 | 已有 | P1 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第三批补入，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第三批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-bailangzhu` | 白狼主 | 已有 | P1 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第五批补入，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第五批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-baoyin` | 宝音 | 已有 | P1 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第四批补入，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第四批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-fang-bai` | 方白 | 已有 | P1 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第四批补入，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第四批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-feng-yi` | 冯夷 | 已有 | P1 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第四批补入，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第四批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-fubao` | 福宝 | 已有 | P1 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第五批补入，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第五批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-fulushou-sisters` | 福禄寿三姐妹 | 已有 | P1 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第三批补入，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第三批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-gui-nainai` | 龟奶奶 | 已有 | P1 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第三批补入，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第三批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-heran` | 贺然 | 已有 | P1 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第三批补入，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第三批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-huajing-xiong` | 画睛兄 | 已有 | P1 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第三批补入，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第三批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-huakui` | 花魁 | 已有 | P1 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第五批补入，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第五批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-lan-ao` | 兰澳 | 已有 | P1 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第三批补入，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第三批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-lian-daozi` | 廉道子 | 已有 | P1 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第四批补入，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第四批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-liu-qingyi` | 柳青衣 | 已有 | P1 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第三批补入，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第三批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-lizhenzhen` | 黎蓁蓁 | 已有 | P1 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第三批补入，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第三批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-murong-yanzhao` | 慕容延钊 | 已有 | P1 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第三批补入，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第三批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-shi-jiu` | 史鸩 | 已有 | P1 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第三批补入，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第三批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-shi-shouxin` | 石守信 | 已有 | P1 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第三批补入，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第三批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-shi-yimo` | 时一墨 | 已有 | P1 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第五批补入，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第五批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-song-jiuwei` | 宋九薇 | 已有 | P1 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第四批补入，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第四批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-wangyue-chanyuan` | 望月婵媛 | 已有 | P1 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第四批补入，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第四批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-xiao-mao-youan` | 小猫佑安 | 已有 | P1 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第五批补入，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第五批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-yan-shiqi` | 燕十七 | 已有 | P1 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第四批补入，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第四批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-ye-mocheng` | 叶墨城 | 已有 | P1 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第五批补入，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第五批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-yingling` | 鹰铃 | 已有 | P1 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第四批补入，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第四批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-zhang-huaishen` | 张淮深 | 已有 | P1 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第四批补入，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第四批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-zhao-chengzong` | 赵承宗 | 已有 | P1 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第四批补入，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第四批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-zhao-dage` | 赵大哥 | 已有 | P1 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第三批补入，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第三批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-zhu-yu` | 朱鱼 | 已有 | P1 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第四批补入，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第四批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `current-character-017` | 天不收 | 已有 | P1 | current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第二批补入或增厚，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第二批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `current-character-011` | 鬼市子 | 已有 | P1 | current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第二批补入或增厚，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式库第二批仅做创作可用卡片，不代表人物传已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-companion-four-cats-three-geese` | 四猫三鹅伙伴群像 | 已有 | P1 | accepted_story_synthesis, current_kb, official_public_seed | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：官方确认群体和系统上线，但未在公开稿中逐一给出七个正式名字。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-feng-jisheng` | 冯继昇 | 已有 | P1 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：名字存在简化写法，完整剧情位置需后续补实录。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-hong-si` | 洪肆 | 已有 | P1 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：目前主要是名单级信息，不能过度扩写具体经历。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-ji-fuzhen` | 季浮真 | 已有 | P1 | current_kb, official_public_seed | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：官方公开为四海豪客/首领线，不要误写成伙伴。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-lin-xingyi` | 林行逸 | 已有 | P1 | current_kb, official_public_seed | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：官方公开为四海豪客/首领线，不要误写成伙伴。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-liu-yanru` | 柳晏如 | 已有 | P1 | current_kb, official_public_seed | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：官方公开为四海豪客/首领线，不要误写成伙伴。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-wei-renpu` | 魏仁浦 | 已有 | P1 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：公开资料多来自维基/玩家整理，入库需标 accepted_story_synthesis。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-xie-changgeng` | 谢长庚 | 已有 | P1 | current_kb, official_public_seed | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：官方公开为四海豪客/首领线，不要误写成伙伴。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-xue-chou` | 薛丑 | 已有 | P1 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：部分信息来自搜索摘要和攻略整合，正式人物传需二次核对。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-yan-yingning` | 燕/应宁 | 已有 | P1 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：“燕”同时是普通字，检索和别名匹配需避免误召。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-zhu-yinghan` | 祝英晗 | 已有 | P1 | current_kb, official_public_seed | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：官方公开为四海豪客/首领线，不要误写成伙伴。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-caojing-guanyin` | 曹敬观音 | 已有 | P2 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 河西/宗教与边地叙事线索，需谨慎。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 已在最终补齐批次中转入 characters.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `character-cheng-xin` | 程心 | 已有 | P2 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 青溪/弱水岸相关人物，短笛、焚焰、黑暗中守理想等意象适合抒情。 风险：资料多来自物件/关联摘要，人物主线需后续补齐。 已在最终补齐批次中转入 characters.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `character-diao-xu` | 翟煦 | 已有 | P2 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 势力代表人物补足。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 已在最终补齐批次中转入 characters.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `character-feng-ruzhi` | 冯如之 | 已有 | P2 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 冯夷相关人物补足，主要服务关系网。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 已在最终补齐批次中转入 characters.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `character-he-jin` | 贺津 | 已有 | P2 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 河西人物关系补足。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 已在最终补齐批次中转入 characters.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `character-he-wanchun` | 贺万春 | 已有 | P2 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 人物关系补足。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 已在最终补齐批次中转入 characters.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `character-he-youqu` | 贺又渠 | 已有 | P2 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 河西人物关系补足。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 已在最终补齐批次中转入 characters.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `character-lan-he` | 蓝何 | 已有 | P2 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 人物关系补足。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 已在最终补齐批次中转入 characters.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `character-liu-duo` | 刘铎 | 已有 | P2 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 青州人物补足。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 已在最终补齐批次中转入 characters.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `character-lv-xingshi` | 吕行世 | 已有 | P2 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 青州/蓬山人物补足。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 已在最终补齐批次中转入 characters.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `character-nanzhu-gongzi` | 南烛公子 | 已有 | P2 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 开封/支线风格化人物，可作奇人线。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 已在最终补齐批次中转入 characters.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `character-pu-xiansheng` | 蒲先生 | 已有 | P2 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 开封民间人物，补市井与小人物故事。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 已在最终补齐批次中转入 characters.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `character-qin-ruolan` | 秦弱兰 | 已有 | P2 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 势力/人物关系补足。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 v0.2 补充：存在“秦弱兰/秦若兰”异写，正式入库时需统一 canonical 与别名。 已在最终补齐批次中转入 characters.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `character-qing-moshandao` | 青 | 已有 | P2 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 墨山道前代人物，可补前代师承、山中传承和隐秘旧事。 风险：更多来自手札/物件线索，不要写成完整官方传记。 已在最终补齐批次中转入 characters.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `character-shi-zhen` | 石贞 | 已有 | P2 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 开封人物关系补足项，优先级低于主线人物。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 已在最终补齐批次中转入 characters.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `character-sinan-swordsman` | 司南剑客 | 已有 | P2 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 剑客/门派风味素材，需确认具体归属。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 已在最终补齐批次中转入 characters.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `character-sun-buqi` | 孙不弃 | 已有 | P2 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 势力代表人物补足。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 已在最终补齐批次中转入 characters.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `character-sun-yuan` | 孙愿 | 已有 | P2 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 势力代表人物补足。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 已在最终补齐批次中转入 characters.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `character-wang-wenli` | 王文礼 | 已有 | P2 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 青州/文津馆人物补足。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 已在最终补齐批次中转入 characters.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `character-wei-zhixi` | 魏芷昔 | 已有 | P2 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 开封女性人物补足，避免女性角色只剩寒香寻/红线。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 已在最终补齐批次中转入 characters.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `character-zhang-cuo` | 张错 | 已有 | P2 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 开封/势力人物补足。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 已在最终补齐批次中转入 characters.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `character-zhang-wanshi` | 张万师 | 已有 | P2 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 墨山道人物，适合师承、规训、工匠伦理和技艺传递。 风险：目前只有名单级公开信息，优先级低于冯继昇/燕。 已在最终补齐批次中转入 characters.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `character-companion-daili-cat` | 呆狸猫 | 已有 | P2 | pending_clues, current_kb_pending | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 笨拙可爱和反差萌明显，适合生活流、轻喜剧和暖心陪伴。 风险：非官方命名页，先作为待官方命名确认层。 已在最终补齐批次中转入 characters.json，运行时必须按 pending_clues 口径使用。 |
| `character-companion-daoba-cat` | 刀疤猫 | 已有 | P2 | pending_clues, current_kb_pending | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 旧伤、流浪和野性意象强，适合江湖旧创与陪伴主题。 风险：非官方命名页；伙伴等同某既有人物的映射只能作为社区猜测。 已在最终补齐批次中转入 characters.json，运行时必须按 pending_clues 口径使用。 |
| `character-companion-dianmo-goose` | 典墨鹅 | 已有 | P2 | pending_clues, current_kb_pending | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 典墨与文津馆书卷气适配，适合墨色、典籍和儒雅陪伴。 风险：来源夹带社区身份猜测，不能作为事实层。 已在最终补齐批次中转入 characters.json，运行时必须按 pending_clues 口径使用。 |
| `character-companion-duxia-cat` | 独侠猫 | 已有 | P2 | pending_clues, current_kb_pending | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 名字自带独行侠气质，适合孤行江湖、外冷内热和陪伴反差。 风险：非官方命名页；伙伴等同某既有人物的映射只能作为社区猜测。 已在最终补齐批次中转入 characters.json，运行时必须按 pending_clues 口径使用。 |
| `character-companion-linge-cat` | 灵娥猫 | 已有 | P2 | pending_clues, current_kb_pending | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 轻灵、月色和梦幻感明显，可服务温柔陪伴类歌词。 风险：存在“灵娥/灵蛾”写法分歧，且官方公开口径对其上线状态不够稳定。 已在最终补齐批次中转入 characters.json，运行时必须按 pending_clues 口径使用。 |
| `character-companion-yangfeng-goose` | 扬风鹅 | 已有 | P2 | pending_clues, current_kb_pending | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 扬风适合海风、轻狂、冲撞感和远行陪伴。 风险：非官方命名页；社区对其身份也无定论。 已在最终补齐批次中转入 characters.json，运行时必须按 pending_clues 口径使用。 |
| `character-companion-zhujin-goose` | 逐金鹅 | 已有 | P2 | pending_clues, current_kb_pending | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 逐金可延展为奔忙、财气、世俗愿望和旅途小心思。 风险：来源夹带社区身份猜测，不能作为事实层。 已在最终补齐批次中转入 characters.json，运行时必须按 pending_clues 口径使用。 |
| `character-saodi-mentong` | 扫地门童 | 已有 | P2 | current_kb, official_public_seed | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：官方未给正式姓名，先按描述性名称记录。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `current-character-001` | 寒香寻 | 已有 | P2 | current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-character-002` | 寻心 | 已有 | P2 | current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-character-003` | 阿依努尔 | 已有 | P2 | current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-character-004` | 田英 | 已有 | P2 | current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-character-005` | 叶万山 | 已有 | P2 | current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-character-006` | 小十七 | 已有 | P2 | current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-character-007` | 道主 | 已有 | P2 | current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-character-008` | 无相皇 | 已有 | P2 | current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-character-009` | 容鸢 | 已有 | P2 | current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-character-010` | 千夜 | 已有 | P2 | current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-character-012` | 郑鄂 | 已有 | P2 | current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-character-013` | 江无浪 | 已有 | P2 | current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-character-014` | 褚清泉 | 已有 | P2 | current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-character-015` | 红线 | 已有 | P2 | current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-character-016` | 伊刀 | 已有 | P2 | current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-character-018` | 玩家游侠 | 已有 | P2 | current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `character-group-kaifeng-fanlou-goldstreet` | 开封樊楼金街群像 | 已有 | P3 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 承接樊楼、金街、宴饮、侍宴和风月市井，可写繁华与虚浮。 风险：颜楚楚、江离等若后续有独立剧情再拆单卡。 已在最终补齐批次中转入 characters.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `character-group-kaifeng-underclass-ghostmarket` | 开封底层/鬼市边民群像 | 已有 | P3 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 承接金街与贫民窟、鬼市之间的强反差，适合暗面开封和底层求生。 风险：不要把所有底层 NPC 具体身份写死；多用群像表达。 已在最终补齐批次中转入 characters.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `character-group-qinghe-family-neighbors` | 清河村居亲缘群像 | 已有 | P3 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 承接母子、村邻、宗族和日常安置，适合普通玩家故事。 风险：靳春娘/靳小宝、汤瘸佬/汤驴宝可视素材量升级为双人关系卡。 已在最终补齐批次中转入 characters.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `character-group-qingzhou-neighbor-hospitality` | 青州好客老乡群像 | 已有 | P3 | official_public_seed, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 承接齐鲁风土、热汤招待、送路人、送礼人和好客日常。 风险：青州普通 NPC 名单公开不足，先按系统和区域气质做群像。 已在最终补齐批次中转入 characters.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `character-qinghe-fangxu` | 方旭 | 已有 | P3 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 普通 NPC 中记忆点较强，可补清河小人物和日常抉择。 风险：仅作“可升单卡样本”，事实细节需二次核对。 已在最终补齐批次中转入 characters.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `character-qinghe-jin-chunniang` | 靳春娘 | 已有 | P3 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 与村居亲缘关系强，适合母子安置、乡邻照看和乱世日常。 风险：也可并入清河村居亲缘群像，正式补库时按素材量决定是否拆单卡。 已在最终补齐批次中转入 characters.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `character-qinghe-songwu` | 宋五 | 已有 | P3 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 普通 NPC 中诉求明确，可服务旅行愿望、小人物自我实现等歌词。 风险：仅作“可升单卡样本”，事实细节需二次核对。 已在最终补齐批次中转入 characters.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `character-qinghe-xiangdemei` | 乡德美 | 已有 | P3 | accepted_story_synthesis, current_kb | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 普通 NPC 中个人议题明显，可补自我认同、乡村表达和烟火气。 风险：仅作“可升单卡样本”，不要由此把所有好感 NPC 都逐个建卡。 已在最终补齐批次中转入 characters.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `character-li-jiaojiao` | 李蕉蕉 | 已有 | P3 | pending_clues, current_kb_pending | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 醉花阴“艳湖七美”线索，可补轻灵、弈棋、风月雅趣。 风险：偏社区维基趣味条目，是否进正式人物库待确认。 已在最终补齐批次中转入 characters.json，运行时必须按 pending_clues 口径使用。 |
| `character-moshandao-bird-bei` | 鵯 | 已有 | P3 | pending_clues, current_kb_pending | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 墨山道鸟名代号，适合微小、群鸣和山林细节。 风险：仅有代号式名单或极薄公开信息，适合作为待补采样位，不应扩写具体经历。 已在最终补齐批次中转入 characters.json，运行时必须按 pending_clues 口径使用。 |
| `character-moshandao-bird-guan` | 鹳 | 已有 | P3 | pending_clues, current_kb_pending | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 墨山道鸟名代号，适合远行、迁徙和山外消息。 风险：仅有代号式名单或极薄公开信息，适合作为待补采样位，不应扩写具体经历。 已在最终补齐批次中转入 characters.json，运行时必须按 pending_clues 口径使用。 |
| `character-moshandao-bird-he` | 鹤 | 已有 | P3 | pending_clues, current_kb_pending | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 墨山道鸟名代号，适合清冷、长寿、山门秩序。 风险：仅有代号式名单或极薄公开信息，适合作为待补采样位，不应扩写具体经历。 已在最终补齐批次中转入 characters.json，运行时必须按 pending_clues 口径使用。 |
| `character-moshandao-bird-jiu` | 鹫 | 已有 | P3 | pending_clues, current_kb_pending | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 墨山道鸟名代号，适合荒山、残酷和俯瞰死亡。 风险：仅有代号式名单或极薄公开信息，适合作为待补采样位，不应扩写具体经历。 已在最终补齐批次中转入 characters.json，运行时必须按 pending_clues 口径使用。 |
| `character-moshandao-bird-ju` | 鵙 | 已有 | P3 | pending_clues, current_kb_pending | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 墨山道鸟名代号，适合尖锐、截断和机关杀机。 风险：仅有代号式名单或极薄公开信息，适合作为待补采样位，不应扩写具体经历。 已在最终补齐批次中转入 characters.json，运行时必须按 pending_clues 口径使用。 |
| `character-moshandao-bird-lu` | 鹭 | 已有 | P3 | pending_clues, current_kb_pending | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 墨山道鸟名代号，适合山雾、机巧、冷色氛围。 风险：仅有代号式名单或极薄公开信息，适合作为待补采样位，不应扩写具体经历。 已在最终补齐批次中转入 characters.json，运行时必须按 pending_clues 口径使用。 |
| `character-moshandao-bird-peng` | 鹏 | 已有 | P3 | pending_clues, current_kb_pending | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 墨山道鸟名代号，适合高处、远志、巨构机关。 风险：仅有代号式名单或极薄公开信息，适合作为待补采样位，不应扩写具体经历。 已在最终补齐批次中转入 characters.json，运行时必须按 pending_clues 口径使用。 |
| `character-moshandao-bird-sun` | 隼 | 已有 | P3 | pending_clues, current_kb_pending | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 墨山道鸟名代号，偏锐利、俯冲、狩猎和精准机关感。 风险：仅有代号式名单或极薄公开信息，适合作为待补采样位，不应扩写具体经历。 已在最终补齐批次中转入 characters.json，运行时必须按 pending_clues 口径使用。 |
| `character-moshandao-bird-ya` | 鸦 | 已有 | P3 | pending_clues, current_kb_pending | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 墨山道鸟名代号，适合阴影、传信、山中旧案。 风险：仅有代号式名单或极薄公开信息，适合作为待补采样位，不应扩写具体经历。 已在最终补齐批次中转入 characters.json，运行时必须按 pending_clues 口径使用。 |
| `character-moshandao-bird-yao` | 鹞 | 已有 | P3 | pending_clues, current_kb_pending | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 墨山道鸟名代号，适合疾行、风中侦察和危险预兆。 风险：仅有代号式名单或极薄公开信息，适合作为待补采样位，不应扩写具体经历。 已在最终补齐批次中转入 characters.json，运行时必须按 pending_clues 口径使用。 |
| `character-moshandao-bird-yu` | 鹬 | 已有 | P3 | pending_clues, current_kb_pending | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 墨山道鸟名代号，适合水岸、细密观察和轻巧动作。 风险：仅有代号式名单或极薄公开信息，适合作为待补采样位，不应扩写具体经历。 已在最终补齐批次中转入 characters.json，运行时必须按 pending_clues 口径使用。 |
| `character-ni-laoshan` | 倪老山 | 已有 | P3 | pending_clues, current_kb_pending | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 九流门长老/门规线索，可作为江湖规矩和黑色幽默素材。 风险：主要来自门派攻略整合，未见独立人物页。 已在最终补齐批次中转入 characters.json，运行时必须按 pending_clues 口径使用。 |
| `character-qi-youyou` | 戚柚柚 | 已有 | P3 | pending_clues, current_kb_pending | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 醉花阴“艳湖七美”线索，可补美食、美人、池塘和钓者成群。 风险：别名更像文内称呼，事实层不要写死。 已在最终补齐批次中转入 characters.json，运行时必须按 pending_clues 口径使用。 |
| `character-yuan-boshi` | 袁博士 | 已有 | P3 | pending_clues, current_kb_pending | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 青溪入门接引/考验线索，可支撑问诊、诊金、医德选择。 风险：主要见于入门攻略整合页，先作待核线索。 已在最终补齐批次中转入 characters.json，运行时必须按 pending_clues 口径使用。 |
| `character-group-hexi-liangzhou-travelers` | 河西/凉州边塞行旅群像 | 已有 | P3 | accepted_story_synthesis, current_kb, official_public_seed | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：河西普通 NPC 摘要偏零散，先群像后升卡。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-group-kaifeng-baijie` | 开封坊巷百业群像 | 已有 | P3 | accepted_story_synthesis, current_kb, official_public_seed | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：开封 NPC 量极大，默认群像，不按人头铺卡。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-group-pengshan-wenjinguan-students` | 蓬山文津馆学子/舍友群像 | 已有 | P3 | accepted_story_synthesis, current_kb, official_public_seed | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：公开具名普通 NPC 少，不宜大规模建单卡。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-group-qinghe-rural-livelihood` | 清河乡野营生群像 | 已有 | P3 | accepted_story_synthesis, current_kb, official_public_seed | 已转入 `characters.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：不要逐个拆朱八碗、王多鲈等低叙事 NPC；后续有独立长支线再升级。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |

### 剧情/任务线

| ID | 名称 | 状态 | 优先级 | 来源等级 | 建议补卡 | 写词影响 / 风险 |
| --- | --- | --- | --- | --- | --- | --- |
| `story-buxianxian-fire-return` | 不羡仙 | 已有 | P0 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `storylines.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式剧情库第一批补入，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式剧情库第一批仅做创作可用卡片，不代表剧情线已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `story-hexi-wenyue-changan` | 河西主章·问月长安 | 已有 | P0 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `storylines.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式剧情库第一批补入，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式剧情库第一批仅做创作可用卡片，不代表剧情线已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `story-kaifeng-tiandi-ronglu` | 开封主章·天地熔炉 | 已有 | P0 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `storylines.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式剧情库第一批补入，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式剧情库第一批仅做创作可用卡片，不代表剧情线已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `story-qinghe-shenxian-budu` | 清河主章·神仙不渡 | 已有 | P0 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `storylines.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式剧情库第一批补入，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式剧情库第一批仅做创作可用卡片，不代表剧情线已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `story-wuyou-dong` | 无忧洞 | 已有 | P0 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `storylines.json`，含资料补齐、创作转译和禁写误区卡 | 已在正式剧情库第一批补入，可被检索和用于写词；后续仍可按实录和公司资料继续增强。 风险：正式剧情库第一批仅做创作可用卡片，不代表剧情线已完全终局；accepted_story_synthesis 不应对外宣称官方定论。 |
| `story-baitou-cheng` | 白头城 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `storylines.json`，含资料补齐、创作转译和禁写误区卡 | 非常适合战后余烬、家书、边军未归。 风险：完整梗概多数依赖攻略/实录综合；结局、身份反转和真相需标 accepted_story_synthesis 或 pending_clues。 已在最终补齐批次中转入 storylines.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `story-fengxue-daishan` | 风雪岱山 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `storylines.json`，含资料补齐、创作转译和禁写误区卡 | 适合悬疑、雪夜、众声纷纭、真相迟到。 风险：完整梗概多数依赖攻略/实录综合；结局、身份反转和真相需标 accepted_story_synthesis 或 pending_clues。 已在最终补齐批次中转入 storylines.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `story-jiangjun-ci` | 将军祠 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `storylines.json`，含资料补齐、创作转译和禁写误区卡 | 承接旧将未冷、乱世忠骨。 风险：完整梗概多数依赖攻略/实录综合；结局、身份反转和真相需标 accepted_story_synthesis 或 pending_clues。 已在最终补齐批次中转入 storylines.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `story-puti-kuhai` | 菩提苦海 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `storylines.json`，含资料补齐、创作转译和禁写误区卡 | 适合压抑、病相、慈悲反转和伪善。 风险：完整梗概多数依赖攻略/实录综合；结局、身份反转和真相需标 accepted_story_synthesis 或 pending_clues。 已在最终补齐批次中转入 storylines.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `story-qingzhou-dongyue` | 青州主线前段·东岳 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `storylines.json`，含资料补齐、创作转译和禁写误区卡 | 当前青州蓬山文津直接跳过东岳。 风险：完整梗概多数依赖攻略/实录综合；结局、身份反转和真相需标 accepted_story_synthesis 或 pending_clues。 已在最终补齐批次中转入 storylines.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `story-shenxiandu` | 神仙渡 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `storylines.json`，含资料补齐、创作转译和禁写误区卡 | 适合回家、渡口、少年离乡后再回望。 风险：完整梗概多数依赖攻略/实录综合；结局、身份反转和真相需标 accepted_story_synthesis 或 pending_clues。 已在最终补齐批次中转入 storylines.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `story-yumen-guan` | 玉门关 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `storylines.json`，含资料补齐、创作转译和禁写误区卡 | 适合边塞长路、初入河西、关门与月色。 风险：完整梗概多数依赖攻略/实录综合；结局、身份反转和真相需标 accepted_story_synthesis 或 pending_clues。 已在最终补齐批次中转入 storylines.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `story-liangzhou-guiyijun` | 凉州与归义军旧事 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `storylines.json`，含资料补齐、创作转译和禁写误区卡 | 承接归唐、边地清宁、盛宴与伤兵并存。 风险：完整梗概多数依赖攻略/实录综合；结局、身份反转和真相需标 accepted_story_synthesis 或 pending_clues。 已在最终补齐批次中转入 storylines.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `current-storyline-021` | 九流祸起 | 已有 | P1 | current_kb | 已转入 `storylines.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 已在最终补齐批次中转入 storylines.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `current-storyline-023` | 开封夜市与鬼市 | 已有 | P1 | current_kb | 已转入 `storylines.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 已在最终补齐批次中转入 storylines.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `current-storyline-024` | 青州蓬山文津 | 已有 | P1 | current_kb | 已转入 `storylines.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 已在最终补齐批次中转入 storylines.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `current-storyline-028` | 同行伙伴 | 已有 | P1 | current_kb | 已转入 `storylines.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 已在最终补齐批次中转入 storylines.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `story-companion-first-season` | 伙伴系统首期剧情 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `storylines.json`，含资料补齐、创作转译和禁写误区卡 | 现有同行伙伴过抽象，不能支撑真实系统创作。 风险：完整梗概多数依赖攻略/实录综合；结局、身份反转和真相需标 accepted_story_synthesis 或 pending_clues。 已在最终补齐批次中转入 storylines.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `story-guishizi-split` | 鬼市子 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `storylines.json`，含资料补齐、创作转译和禁写误区卡 | 黑市、假名、狂欢与阴气并置的强素材。 风险：完整梗概多数依赖攻略/实录综合；结局、身份反转和真相需标 accepted_story_synthesis 或 pending_clues。 已在最终补齐批次中转入 storylines.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `story-jiuliu-split` | 九流线·奇谋/暗涌/祸起 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `storylines.json`，含资料补齐、创作转译和禁写误区卡 | 现有一条九流祸起承载太多内容，不利检索。 风险：完整梗概多数依赖攻略/实录综合；结局、身份反转和真相需标 accepted_story_synthesis 或 pending_clues。 已在最终补齐批次中转入 storylines.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `story-wenjinguan-pengshan-split` | 文津馆与蓬山 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `storylines.json`，含资料补齐、创作转译和禁写误区卡 | 现有粗合并影响检索和情绪精度。 风险：完整梗概多数依赖攻略/实录综合；结局、身份反转和真相需标 accepted_story_synthesis 或 pending_clues。 已在最终补齐批次中转入 storylines.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `story-zhulin-jiuju` | 竹林旧居 | 已有 | P2 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `storylines.json`，含资料补齐、创作转译和禁写误区卡 | 适合初见江湖、未成侠先成伤的新手篇。 风险：完整梗概多数依赖攻略/实录综合；结局、身份反转和真相需标 accepted_story_synthesis 或 pending_clues。 已在最终补齐批次中转入 storylines.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `story-boss-old-cases-index` | 镇守旧事总线 | 已有 | P2 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `storylines.json`，含资料补齐、创作转译和禁写误区卡 | 便于命中旧案、镇守、前尘类用户意图。 风险：完整梗概多数依赖攻略/实录综合；结局、身份反转和真相需标 accepted_story_synthesis 或 pending_clues。 已在最终补齐批次中转入 storylines.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `story-xiaji-canzhang-index` | 侠迹与残章索引线 | 已有 | P2 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `storylines.json`，含资料补齐、创作转译和禁写误区卡 | 补全点名某卷侠迹/残章的检索能力。 风险：完整梗概多数依赖攻略/实录综合；结局、身份反转和真相需标 accepted_story_synthesis 或 pending_clues。 已在最终补齐批次中转入 storylines.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `current-storyline-019` | 玉露为乡 | 已有 | P2 | current_kb | 已转入 `storylines.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-storyline-020` | 弱水岸寻心 | 已有 | P2 | current_kb | 已转入 `storylines.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-storyline-022` | 无名剑谱 | 已有 | P2 | current_kb | 已转入 `storylines.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-storyline-025` | 清河初入江湖 | 已有 | P2 | current_kb | 已转入 `storylines.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-storyline-026` | 门派倾覆 | 已有 | P2 | current_kb | 已转入 `storylines.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-storyline-027` | 小人物不平事 | 已有 | P2 | current_kb | 已转入 `storylines.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |

### 地域/场景

| ID | 名称 | 状态 | 优先级 | 来源等级 | 建议补卡 | 写词影响 / 风险 |
| --- | --- | --- | --- | --- | --- | --- |
| `region-bujianshan` | 不见山 | 已有 | P0 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 补足求真、机关、云峰、最后桃源的独特审美。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 已在最终补齐批次中转入 regions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `region-hexi` | 河西 | 已有 | P0 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 把玉门关-凉州-秦川串成完整远行/归途主轴。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 已在最终补齐批次中转入 regions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `region-qinchuan` | 秦川 | 已有 | P0 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 把河西从诗意边塞推进到画卷归途。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 已在最终补齐批次中转入 regions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `region-yumen-guan` | 玉门关 | 已有 | P0 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 补足大漠、边关、旅人、残忍与温柔并置。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 已在最终补齐批次中转入 regions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `region-fanlou-golden-street` | 樊楼金街 | 已有 | P0 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 开封夜生活和繁华暗面的最强入口词。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 已在最终补齐批次中转入 regions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `region-palace-city` | 宫城 | 已有 | P0 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 补皇城、朝野、宫墙、温情秘闻与禁地冒险。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 已在最终补齐批次中转入 regions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `region-wenjinguan-school` | 文津馆学斋/校舍/藏书阁 | 已有 | P0 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 直接补求学、同窗、舍友情、书卷烟火素材。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 已在最终补齐批次中转入 regions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `region-yulutai` | 玉露台 | 已有 | P0 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 阿依努尔线和普通人故乡记忆的高频入口。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 已在最终补齐批次中转入 regions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `region-baitou-cheng` | 白头城 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 强补家国、孤忠、旧军旅线素材。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 已在最终补齐批次中转入 regions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `region-dasanguan` | 大散关 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 适合写过关与命运转场。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 已在最终补齐批次中转入 regions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `region-dongyue` | 东岳 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 把青州从泛书卷气落到山海相接。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 已在最终补齐批次中转入 regions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `region-tengwang-village` | 藤王村 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 适合衰败村落、守藤人、旧酿与亡魂执念。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 已在最终补齐批次中转入 regions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `region-tianshanglai` | 天上来 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 补河道、吊机、渡口、奇局类意象。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 已在最终补齐批次中转入 regions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `region-wugou-ta` | 无垢塔 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 补佛塔、战后余烬、清宁重建。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 已在最终补齐批次中转入 regions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `region-wuyou-dong` | 无忧洞 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 承接中式微恐、未尽人言、开封暗面。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 已在最终补齐批次中转入 regions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `region-coffin-shop-ghost-market` | 棺材铺/鬼市入口 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 九流门、鬼市子、开封暗面常用开笔入口。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 已在最终补齐批次中转入 regions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `region-huoren-yiguan` | 活人医馆 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 弱水岸从抽象水岸感落到医馆、井、伤与秘密。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 已在最终补齐批次中转入 regions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `region-seaside` | 海滨 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 补海风、潮声、滩涂、拾鲜等稀缺意象。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 已在最终补齐批次中转入 regions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `region-wangdi-xiang` | 望帝乡 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 强适配回望、归乡不能、旧名未忘。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 已在最终补齐批次中转入 regions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `region-zhulin-jiuju` | 竹林旧居 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 少东家童年、养父线、归家感高频必用。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 已在最终补齐批次中转入 regions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `region-baicao-ye` | 百草野 | 已有 | P2 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 给清河补荒野旧迹面。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 已在最终补齐批次中转入 regions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `region-baima-yi` | 白马驿 | 已有 | P2 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 适合驿路、送别、途中旧事。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 已在最终补齐批次中转入 regions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `region-chunqiu-bieguan` | 春秋别馆 | 已有 | P2 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 补旧馆、暗道、别后无人回的气质。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 已在最终补齐批次中转入 regions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `region-daishan` | 岱山 | 已有 | P2 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 适合悬疑、雪夜、群像困局。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 已在最终补齐批次中转入 regions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `region-fengqiu-village` | 封丘村 | 已有 | P2 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 适合朴素村落、沿河日常、人情线。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 已在最终补齐批次中转入 regions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `region-heshile` | 河市乐 | 已有 | P2 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 补戏台、流言、沿河市声、民间热闹。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 已在最终补齐批次中转入 regions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `region-jiangjun-ci` | 将军祠 | 已有 | P2 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 适合碑刻、旧将、稚子江湖、残响。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 已在最终补齐批次中转入 regions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `region-poor-quarter` | 贫民窟 | 已有 | P2 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 把开封从繁华单一面拉回人间切片。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 已在最终补齐批次中转入 regions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `region-puti-kuhai` | 菩提苦海 | 已有 | P2 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 补佛性、苦海、鼓声召引的意象层。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 已在最终补齐批次中转入 regions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `region-shanmiaozhou` | 善妙洲 | 已有 | P2 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 补清河北向延展和继续上路空间感。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 已在最终补齐批次中转入 regions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `region-yinyue-shan` | 隐月山 | 已有 | P2 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 补山场、雾、试炼、奇遇式叙事。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 已在最终补齐批次中转入 regions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `current-region-029` | 清河 | 已有 | P2 | current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-region-030` | 开封 | 已有 | P2 | current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-region-031` | 雁门 | 已有 | P2 | current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-region-032` | 凉州 | 已有 | P2 | current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-region-033` | 长安 | 已有 | P2 | current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-region-034` | 青州 | 已有 | P2 | current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-region-035` | 蓬山 | 已有 | P2 | current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-region-036` | 神仙渡 | 已有 | P2 | current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-region-037` | 弱水岸 | 已有 | P2 | current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-region-038` | 不羡仙 | 已有 | P2 | current_kb | 已转入 `regions.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |

### 势力/门派/组织

| ID | 名称 | 状态 | 优先级 | 来源等级 | 建议补卡 | 写词影响 / 风险 |
| --- | --- | --- | --- | --- | --- | --- |
| `faction-xiujinlou` | 绣金楼 | 已有 | P0 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `factions.json`，含资料补齐、创作转译和禁写误区卡 | 人物歌大量会碰到，缺主势力卡会导致角色线失根。 风险：组织宗旨、内部位阶、完整谱系很多不在官方公开资料中；入库需分清官方存在、综合叙事和待核线索。 已在最终补齐批次中转入 factions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `faction-xuanjian` | 悬剑 | 已有 | P0 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `factions.json`，含资料补齐、创作转译和禁写误区卡 | 补清河主线、旧朝遗绪、理想主义与权术的门派气质。 风险：组织宗旨、内部位阶、完整谱系很多不在官方公开资料中；入库需分清官方存在、综合叙事和待核线索。 已在最终补齐批次中转入 factions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `faction-yanbeimeng` | 燕北盟 | 已有 | P0 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `factions.json`，含资料补齐、创作转译和禁写误区卡 | 家国、遗民、北伐未竟、英雄被遗忘等题材核心来源。 风险：组织宗旨、内部位阶、完整谱系很多不在官方公开资料中；入库需分清官方存在、综合叙事和待核线索。 已在最终补齐批次中转入 factions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `faction-guyun` | 孤云 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `factions.json`，含资料补齐、创作转译和禁写误区卡 | 作为门派/势力线索，后续需确认上线与玩法边界。 风险：组织宗旨、内部位阶、完整谱系很多不在官方公开资料中；入库需分清官方存在、综合叙事和待核线索。 已在最终补齐批次中转入 factions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `faction-pojie-foye` | 破戒佛爷 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `factions.json`，含资料补齐、创作转译和禁写误区卡 | 补清河阴郁面、伪善/信仰崩塌/寺庙变寨。 风险：组织宗旨、内部位阶、完整谱系很多不在官方公开资料中；入库需分清官方存在、综合叙事和待核线索。 已在最终补齐批次中转入 factions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `faction-tianhu-jun` | 天虎军 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `factions.json`，含资料补齐、创作转译和禁写误区卡 | 把叶万山从单人反派扩到整支军队的悲剧感。 风险：组织宗旨、内部位阶、完整谱系很多不在官方公开资料中；入库需分清官方存在、综合叙事和待核线索。 已在最终补齐批次中转入 factions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `current-faction-042` | 文津馆 | 已有 | P1 | current_kb | 已转入 `factions.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 已在最终补齐批次中转入 factions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `current-faction-045` | 赤龙堂 | 已有 | P1 | current_kb | 已转入 `factions.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 已在最终补齐批次中转入 factions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `current-faction-051` | 归义军 | 已有 | P1 | current_kb | 已转入 `factions.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 已在最终补齐批次中转入 factions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `faction-ghost-market-order` | 鬼市地下秩序 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `factions.json`，含资料补齐、创作转译和禁写误区卡 | 现有角色域和剧情域有散点，但缺势力/秩序壳。 风险：组织宗旨、内部位阶、完整谱系很多不在官方公开资料中；入库需分清官方存在、综合叙事和待核线索。 已在最终补齐批次中转入 factions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `faction-hexi-border-army` | 河西边军/凉州守军 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `factions.json`，含资料补齐、创作转译和禁写误区卡 | 归义军过于大历史，缺凉州现场军旅质感。 风险：组织宗旨、内部位阶、完整谱系很多不在官方公开资料中；入库需分清官方存在、综合叙事和待核线索。 已在最终补齐批次中转入 factions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `faction-kaifeng-government` | 开封官府/武德司 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `factions.json`，含资料补齐、创作转译和禁写误区卡 | 开封歌、鬼市歌、庙堂线都需要白天秩序对照面。 风险：组织宗旨、内部位阶、完整谱系很多不在官方公开资料中；入库需分清官方存在、综合叙事和待核线索。 已在最终补齐批次中转入 factions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `faction-khitan` | 契丹相关势力 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `factions.json`，含资料补齐、创作转译和禁写误区卡 | 北地、燕北盟、天虎军、河西线都会回流到这层背景势力。 风险：组织宗旨、内部位阶、完整谱系很多不在官方公开资料中；入库需分清官方存在、综合叙事和待核线索。 已在最终补齐批次中转入 factions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `faction-qingjiao-tang` | 青蛟堂 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `factions.json`，含资料补齐、创作转译和禁写误区卡 | 当前赤龙堂已入库，但缺对手与前身，河运江湖叙事是断的。 风险：组织宗旨、内部位阶、完整谱系很多不在官方公开资料中；入库需分清官方存在、综合叙事和待核线索。 已在最终补齐批次中转入 factions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `faction-no-sect` | 无门无派体系 | 已有 | P2 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `factions.json`，含资料补齐、创作转译和禁写误区卡 | 玩家自我投射很强。 风险：组织宗旨、内部位阶、完整谱系很多不在官方公开资料中；入库需分清官方存在、综合叙事和待核线索。 已在最终补齐批次中转入 factions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `faction-qingzhou-literati` | 青州文脉关联组织 | 已有 | P2 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `factions.json`，含资料补齐、创作转译和禁写误区卡 | 把文津馆从书卷气升级成可写人群与秩序。 风险：组织宗旨、内部位阶、完整谱系很多不在官方公开资料中；入库需分清官方存在、综合叙事和待核线索。 已在最终补齐批次中转入 factions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `faction-wumianren` | 无面人 | 已有 | P2 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `factions.json`，含资料补齐、创作转译和禁写误区卡 | 适合氛围创作，但不一定是稳定组织。 风险：组织宗旨、内部位阶、完整谱系很多不在官方公开资料中；入库需分清官方存在、综合叙事和待核线索。 已在最终补齐批次中转入 factions.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `faction-palace-institutions` | 开封宫城机构 | 已有 | P2 | pending_clues, current_kb_pending | 已转入 `factions.json`，含资料补齐、创作转译和禁写误区卡 | 对宫廷题材有用，但更像机构/场景。 风险：组织宗旨、内部位阶、完整谱系很多不在官方公开资料中；入库需分清官方存在、综合叙事和待核线索。 已在最终补齐批次中转入 factions.json，运行时必须按 pending_clues 口径使用。 |
| `current-faction-039` | 九流门 | 已有 | P2 | current_kb | 已转入 `factions.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-faction-040` | 不老仙 | 已有 | P2 | current_kb | 已转入 `factions.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-faction-041` | 醉花阴 | 已有 | P2 | current_kb | 已转入 `factions.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-faction-043` | 无心谷 | 已有 | P2 | current_kb | 已转入 `factions.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-faction-044` | 三更天 | 已有 | P2 | current_kb | 已转入 `factions.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-faction-046` | 天泉 | 已有 | P2 | current_kb | 已转入 `factions.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-faction-047` | 梨园 | 已有 | P2 | current_kb | 已转入 `factions.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-faction-048` | 青溪 | 已有 | P2 | current_kb | 已转入 `factions.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-faction-049` | 狂澜 | 已有 | P2 | current_kb | 已转入 `factions.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-faction-050` | 墨山道 | 已有 | P2 | current_kb | 已转入 `factions.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `faction-qiongqi-shi` | 穷奇师 | 已有 | P3 | pending_clues, current_kb_pending | 已转入 `factions.json`，含资料补齐、创作转译和禁写误区卡 | 可补敌营感，但暂不稳。 风险：组织宗旨、内部位阶、完整谱系很多不在官方公开资料中；入库需分清官方存在、综合叙事和待核线索。 已在最终补齐批次中转入 factions.json，运行时必须按 pending_clues 口径使用。 |

### 玩法体验

| ID | 名称 | 状态 | 优先级 | 来源等级 | 建议补卡 | 写词影响 / 风险 |
| --- | --- | --- | --- | --- | --- | --- |
| `gameplay-coop-pve` | 多人副本与协作 PVE | 已有 | P0 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `gameplay.json`，含资料补齐、创作转译和禁写误区卡 | 中强；并肩、翻车、复盘、再战，但避免 MMO 套词。 风险：玩法公开资料常混合前瞻、优化和正式上线；必须按 2026-06-13 切分已上线与预告。 已在最终补齐批次中转入 gameplay.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `gameplay-homestead-buxianxian` | 家业与不羡仙重建 | 已有 | P0 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `gameplay.json`，含资料补齐、创作转译和禁写误区卡 | 极强；归乡、修屋、种地、认人、旧景重见。 风险：玩法公开资料常混合前瞻、优化和正式上线；必须按 2026-06-13 切分已上线与预告。 已在最终补齐批次中转入 gameplay.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `gameplay-music-system` | 丝竹雅韵与乐器系统 | 已有 | P0 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `gameplay.json`，含资料补齐、创作转译和禁写误区卡 | 极强；平台作词链路和燕云世界感最直接桥梁之一。 风险：玩法公开资料常混合前瞻、优化和正式上线；必须按 2026-06-13 切分已上线与预告。 已在最终补齐批次中转入 gameplay.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `gameplay-open-world-compendium` | 开放世界探索与博物志 | 已有 | P0 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `gameplay.json`，含资料补齐、创作转译和禁写误区卡 | 强；普通玩家故事大底盘，补走路见世界。 风险：玩法公开资料常混合前瞻、优化和正式上线；必须按 2026-06-13 切分已上线与预告。 已在最终补齐批次中转入 gameplay.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `gameplay-pvp-zhige` | PvP 与止戈竞技 | 已有 | P0 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `gameplay.json`，含资料补齐、创作转译和禁写误区卡 | 中；更偏 eval 和用户意图理解。 风险：玩法公开资料常混合前瞻、优化和正式上线；必须按 2026-06-13 切分已上线与预告。 已在最终补齐批次中转入 gameplay.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `gameplay-qinggong-exploration` | 轻功与立体探索 | 已有 | P0 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `gameplay.json`，含资料补齐、创作转译和禁写误区卡 | 强；轻功天然带风、檐、山、逃、赴约、少年气。 风险：玩法公开资料常混合前瞻、优化和正式上线；必须按 2026-06-13 切分已上线与预告。 已在最终补齐批次中转入 gameplay.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `gameplay-companion-split` | 伙伴系统细分 | 已有 | P0 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `gameplay.json`，含资料补齐、创作转译和禁写误区卡 | 极强；温柔、陪伴、玩闹、路感。 风险：玩法公开资料常混合前瞻、优化和正式上线；必须按 2026-06-13 切分已上线与预告。 已在最终补齐批次中转入 gameplay.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `gameplay-baiye` | 百业与百业战 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `gameplay.json`，含资料补齐、创作转译和禁写误区卡 | 中强；同伴结社、商战荒诞、群体荣誉、驻地热闹。 风险：玩法公开资料常混合前瞻、优化和正式上线；必须按 2026-06-13 切分已上线与预告。 已在最终补齐批次中转入 gameplay.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `gameplay-social-qingyilin` | 社交关系与情义林 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `gameplay.json`，含资料补齐、创作转译和禁写误区卡 | 中强；同游、拜师、结义、暧昧未满。 风险：玩法公开资料常混合前瞻、优化和正式上线；必须按 2026-06-13 切分已上线与预告。 已在最终补齐批次中转入 gameplay.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `gameplay-stronghold-infiltration` | 据点渗透与战斗据点 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `gameplay.json`，含资料补齐、创作转译和禁写误区卡 | 中强；夜探、误入、引敌、摸营、撤离。 风险：玩法公开资料常混合前瞻、优化和正式上线；必须按 2026-06-13 切分已上线与预告。 已在最终补齐批次中转入 gameplay.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `gameplay-tianggong-diku` | 天工地窟与地牢秘境 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `gameplay.json`，含资料补齐、创作转译和禁写误区卡 | 中；险、暗、机关、旧城、同行探坑。 风险：玩法公开资料常混合前瞻、优化和正式上线；必须按 2026-06-13 切分已上线与预告。 已在最终补齐批次中转入 gameplay.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `gameplay-crafting-cooking` | 采集制作与美食烹饪 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `gameplay.json`，含资料补齐、创作转译和禁写误区卡 | 强；食物、烟火气、赶海、采药、做菜。 风险：玩法公开资料常混合前瞻、优化和正式上线；必须按 2026-06-13 切分已上线与预告。 已在最终补齐批次中转入 gameplay.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `current-gameplay-059` | 伙伴同行 | 已有 | P1 | current_kb | 已转入 `gameplay.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 已在最终补齐批次中转入 gameplay.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `current-gameplay-060` | 镇守首领 | 已有 | P1 | current_kb | 已转入 `gameplay.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 已在最终补齐批次中转入 gameplay.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `current-gameplay-061` | 侠迹残章 | 已有 | P1 | current_kb | 已转入 `gameplay.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 已在最终补齐批次中转入 gameplay.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `gameplay-faction-prestige` | 门派地位与晋升 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `gameplay.json`，含资料补齐、创作转译和禁写误区卡 | 中；角色歌和门派代入有效。 风险：玩法公开资料常混合前瞻、优化和正式上线；必须按 2026-06-13 切分已上线与预告。 已在最终补齐批次中转入 gameplay.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `gameplay-life-qishu-split` | 潜行、点穴、妙手空空与兽语等生活奇术 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `gameplay.json`，含资料补齐、创作转译和禁写误区卡 | 中强；巧、偷、听、逗、骗的江湖感。 风险：玩法公开资料常混合前瞻、优化和正式上线；必须按 2026-06-13 切分已上线与预告。 已在最终补齐批次中转入 gameplay.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `gameplay-wild-boss-split` | 野外首领细分 | 已有 | P1 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `gameplay.json`，含资料补齐、创作转译和禁写误区卡 | 中；路上撞见强敌、败走、再回头。 风险：玩法公开资料常混合前瞻、优化和正式上线；必须按 2026-06-13 切分已上线与预告。 已在最终补齐批次中转入 gameplay.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `gameplay-appearance-wanxiang` | 外观、万相集与角色塑造 | 已有 | P2 | official_public_seed, accepted_story_synthesis, current_kb | 已转入 `gameplay.json`，含资料补齐、创作转译和禁写误区卡 | 弱到中；用户意图理解有帮助，通常只是背景。 风险：玩法公开资料常混合前瞻、优化和正式上线；必须按 2026-06-13 切分已上线与预告。 已在最终补齐批次中转入 gameplay.json，运行时必须按 accepted_story_synthesis 口径使用。 |
| `gameplay-boardgames-future` | 打马棋与侠客偶棋 | 已有 | P2 | pending_clues, current_kb_pending | 已转入 `gameplay.json`，含资料补齐、创作转译和禁写误区卡 | 中；可做未来创作暗线，但不进入当前 live facts。 风险：玩法公开资料常混合前瞻、优化和正式上线；必须按 2026-06-13 切分已上线与预告。 已在最终补齐批次中转入 gameplay.json，运行时必须按 pending_clues 口径使用。 |
| `current-gameplay-052` | 奇术 | 已有 | P2 | current_kb | 已转入 `gameplay.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-gameplay-053` | 偷师百家 | 已有 | P2 | current_kb | 已转入 `gameplay.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-gameplay-054` | 寻声 | 已有 | P2 | current_kb | 已转入 `gameplay.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-gameplay-055` | 中式解谜 | 已有 | P2 | current_kb | 已转入 `gameplay.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-gameplay-056` | 武学自由搭配 | 已有 | P2 | current_kb | 已转入 `gameplay.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-gameplay-057` | 营生 | 已有 | P2 | current_kb | 已转入 `gameplay.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-gameplay-058` | 不平事 | 已有 | P2 | current_kb | 已转入 `gameplay.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |

### 创作边界

| ID | 名称 | 状态 | 优先级 | 来源等级 | 建议补卡 | 写词影响 / 风险 |
| --- | --- | --- | --- | --- | --- | --- |
| `boundary-spoiler-policy` | 剧透等级与用户意图边界 | 已有 | P0 | creative_guidance, current_kb | 已转入 `creative-boundaries.json`，含资料补齐、创作转译和禁写误区卡 | 避免角色歌误剧透，也避免过度保守导致写不深。 风险：边界类不直接提供剧情事实，而是约束 Agent 表达与质量门。 已在最终补齐批次中转入 creative-boundaries.json，运行时必须按 creative_boundary 口径使用。 |
| `boundary-user-oc-vs-canon` | 玩家原创角色与官方人物边界 | 已有 | P0 | creative_guidance, current_kb | 已转入 `creative-boundaries.json`，含资料补齐、创作转译和禁写误区卡 | 用户不一定想写官方剧情，知识库不能吞掉玩家创意。 风险：边界类不直接提供剧情事实，而是约束 Agent 表达与质量门。 已在最终补齐批次中转入 creative-boundaries.json，运行时必须按 creative_boundary 口径使用。 |
| `boundary-unconfirmed-relationship` | 未确认人物关系表达规则 | 已有 | P0 | creative_guidance, current_kb | 已转入 `creative-boundaries.json`，含资料补齐、创作转译和禁写误区卡 | 寒香寻/千夜/寻心等人物线很容易把推测写成定论。 风险：边界类不直接提供剧情事实，而是约束 Agent 表达与质量门。 已在最终补齐批次中转入 creative-boundaries.json，运行时必须按 creative_boundary 口径使用。 |
| `boundary-ethnic-history` | 民族/历史映射表达边界 | 已有 | P1 | creative_guidance, current_kb | 已转入 `creative-boundaries.json`，含资料补齐、创作转译和禁写误区卡 | 家国歌很需要，但商业风险也高。 风险：边界类不直接提供剧情事实，而是约束 Agent 表达与质量门。 已在最终补齐批次中转入 creative-boundaries.json，运行时必须按 creative_boundary 口径使用。 |
| `boundary-cover-title-policy` | 封面标题文字边界 | 已有 | P1 | creative_guidance, current_kb | 已转入 `creative-boundaries.json`，含资料补齐、创作转译和禁写误区卡 | 封面允许作品名，但不能出现假歌手/厂牌/版权。 风险：边界类不直接提供剧情事实，而是约束 Agent 表达与质量门。 已在最终补齐批次中转入 creative-boundaries.json，运行时必须按 creative_boundary 口径使用。 |
| `boundary-commercial-publish` | 社区发布交接边界 | 已有 | P2 | creative_guidance, current_kb | 已转入 `creative-boundaries.json`，含资料补齐、创作转译和禁写误区卡 | 避免知识库把公司社区系统写成本平台能力承诺。 风险：边界类不直接提供剧情事实，而是约束 Agent 表达与质量门。 已在最终补齐批次中转入 creative-boundaries.json，运行时必须按 creative_boundary 口径使用。 |
| `current-boundary-062` | 其他 IP 题材拒绝 | 已有 | P2 | current_kb | 已转入 `creative-boundaries.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-boundary-063` | 现实歌手风格安全改写 | 已有 | P2 | current_kb | 已转入 `creative-boundaries.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-boundary-064` | 剧情整合与官方口径分层 | 已有 | P2 | current_kb | 已转入 `creative-boundaries.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-boundary-065` | 玩家故事保真 | 已有 | P2 | current_kb | 已转入 `creative-boundaries.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-boundary-066` | 商业权益不承诺 | 已有 | P2 | current_kb | 已转入 `creative-boundaries.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-boundary-067` | 封面文字受控 | 已有 | P2 | current_kb | 已转入 `creative-boundaries.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-boundary-068` | 暴力血腥与低俗限制 | 已有 | P2 | current_kb | 已转入 `creative-boundaries.json`，含资料补齐、创作转译和禁写误区卡 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |

### 回归题库

| ID | 名称 | 状态 | 优先级 | 来源等级 | 建议补卡 | 写词影响 / 风险 |
| --- | --- | --- | --- | --- | --- | --- |
| `eval-character-depth` | eval-角色歌深度与不跑题 | 已有 | P0 | creative_guidance, accepted_story_synthesis, current_kb_eval | 已补正式 eval case | 直接验证知识库对作词质量的核心收益。 风险：eval 不是知识事实正文，但必须覆盖真实玩家输入、边界拒绝和日期敏感问题。 已转为正式 eval case；后续 A/B 评测可继续扩写。 |
| `eval-companion-boundary` | eval-伙伴陪伴边界 | 已有 | P0 | creative_guidance, accepted_story_synthesis, current_kb_eval | 已补正式 eval case | 伙伴容易被写偏成萌宠文或恋爱陪伴文。 风险：eval 不是知识事实正文，但必须覆盖真实玩家输入、边界拒绝和日期敏感问题。 已转为正式 eval case；后续 A/B 评测可继续扩写。 |
| `eval-homestead-version` | eval-家业重建情感与版本差异 | 已有 | P0 | creative_guidance, accepted_story_synthesis, current_kb_eval | 已补正式 eval case | 防止模型把理想中的家园写成当前已完全实现。 风险：eval 不是知识事实正文，但必须覆盖真实玩家输入、边界拒绝和日期敏感问题。 已转为正式 eval case；后续 A/B 评测可继续扩写。 |
| `eval-live-vs-preview` | eval-已上线与预告玩法隔离 | 已有 | P0 | creative_guidance, accepted_story_synthesis, current_kb_eval | 已补正式 eval case | 日期敏感边界，防止未来内容污染当前主库。 风险：eval 不是知识事实正文，但必须覆盖真实玩家输入、边界拒绝和日期敏感问题。 已转为正式 eval case；后续 A/B 评测可继续扩写。 |
| `eval-other-ip-rewrite` | eval-其他 IP 拒绝与情绪转译 | 已有 | P0 | creative_guidance, accepted_story_synthesis, current_kb_eval | 已补正式 eval case | 产品边界核心验收。 风险：eval 不是知识事实正文，但必须覆盖真实玩家输入、边界拒绝和日期敏感问题。 已转为正式 eval case；后续 A/B 评测可继续扩写。 |
| `eval-pvp-pve-routing` | eval-PvP/PvE术语分流 | 已有 | P0 | creative_guidance, accepted_story_synthesis, current_kb_eval | 已补正式 eval case | 防止试剑写成 PVP，或止戈误写成单一玩法。 风险：eval 不是知识事实正文，但必须覆盖真实玩家输入、边界拒绝和日期敏感问题。 已转为正式 eval case；后续 A/B 评测可继续扩写。 |
| `eval-storyline-recall` | eval-剧情线召回与事实降级 | 已有 | P0 | creative_guidance, accepted_story_synthesis, current_kb_eval | 已补正式 eval case | 防止长剧情线误召回和编造。 风险：eval 不是知识事实正文，但必须覆盖真实玩家输入、边界拒绝和日期敏感问题。 已转为正式 eval case；后续 A/B 评测可继续扩写。 |
| `eval-weak-official-downgrade` | eval-弱官方玩法的事实层降级 | 已有 | P1 | creative_guidance, accepted_story_synthesis, current_kb_eval | 已补正式 eval case | 关系知识库输出可信度和商业风险。 风险：eval 不是知识事实正文，但必须覆盖真实玩家输入、边界拒绝和日期敏感问题。 已转为正式 eval case；后续 A/B 评测可继续扩写。 |

## 未来资料 / 暂不进入 live 主库

- 江南/杭州：截至 2026-06-13 仍是 6 月 26 日暑期版本预告，不进主库 live facts；后续上线后再转入人物/地域/剧情缺口。 来源：official-hangzhou-preview, official-jiangnan-preview, official-site
- 打马棋/侠客偶棋正式上线后资料：可保留未来素材，但当前只能作 roadmap guardrail。 来源：official-future-boardgame
- 陈子奚：江南引路人/陈氏家主已被官方预告提及，但截至 2026-06-13 尚未作为 live 内容入主库人物缺口。 来源：official-jiangnan-preview
- 玉山君：仅在江南预告中被提及，身份未明，不进入 live 人物缺口。 来源：official-jiangnan-preview
- 陈氏：江南预告中的家族势力线索，仅作未来资料。 来源：official-jiangnan-preview
- 钱王/杭州传说：杭州预告中的历史传说素材，不能当成已上线可交互角色。 来源：official-hangzhou-preview
- 文津馆十相：官方预告/剧情钩子，只确认集体称呼，不公开成员名单。 来源：official-pengshan-live

## 普通 NPC 排除与群像规则

- 纯职业泛称 NPC：无稳定个人剧情，默认进入玩法/地域群像，不做人物卡。 示例：行商, 猎户, 山民, 钓鱼佬
- 功能型服务称谓：更适合樊楼/金街服务业群像，不逐个建卡。 示例：沽酒侍女, 侍宴侍女
- 伙伴等同既有人物的社区映射：社区发布者也标注仅供娱乐，不能作为事实层。 示例：独侠猫=江晏, 刀疤猫=伊刀
- 泛称动物/生态点位：除伙伴系统具名对象外，不作为人物缺口。 示例：狗狗
