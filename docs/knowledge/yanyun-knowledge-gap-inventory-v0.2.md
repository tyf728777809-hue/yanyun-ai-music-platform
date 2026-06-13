# 燕云创作知识库缺口盘点表 v0.2

盘点时间：2026-06-13

本表用于把当前商用知识库后续应补的人物、剧情、地域、势力、玩法、创作边界和 eval 缺口一次性盘清楚。它不是正式知识库正文，不会直接参与写词检索；后续补库应按本表逐行转成 `knowledge-base/commercial-final/` 下的分域卡片。

## 当前结论

- 当前正式库基线：`68` 个实体、`231` 个创作 chunk、`131` 条 eval case；盘点表新增的是后续补库作战清单，不代表正式库已经减少或替换。
- v0.2 专门修复人物漏项风险：新增外部候选人物池、程序化差集审计、普通 NPC 群像归类和未来预告人物边界。
- 第一批正式补库已回写状态：赵光义/晋中原、张彦霖、周蔷、薛丑、洪肆、魏仁浦、冯继昇、燕/应宁、蓬山四海豪客、四猫三鹅伙伴群像和部分普通 NPC 群像已标为 `已有`。
- 第二批正式补库已回写状态：姚药药、慧药、柴荣、王清、李祚、盈盈/温无缺、沈义伦、麻布袋、郭昕、张议潮、唐新词已转入正式人物库；鬼市子和天不收已补边界卡，不再标为 `需拆分` / `偏薄`。
- 第三批正式补库已回写状态：画睛兄、兰澳、贺然、黎蓁蓁、柳青衣、柏楚玉、福禄寿三姐妹、赵大哥、龟奶奶、史鸩、石守信、慕容延钊已转入正式人物库。
- 人物覆盖口径：只收对歌词创作有价值的主线/支线/势力代表/历史锚点/伙伴/强记忆 NPC；低叙事普通 NPC 按地域或市井群像纳入 P3。
- `pending_clues` 可以进入创作素材层，但只能写成暗线、传闻、旧事、情绪或意象，不得写成官方定论。
- 江南/杭州、陈子奚、玉山君、文津馆十相等截至 2026-06-13 属于未来/预告资料，不进当前 live 主库事实层。

## 覆盖统计

| 类型 | 总行数 | 已有 | 缺失 | 偏薄 | 需拆分 | 仅待核线索 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 人物 | 130 | 60 | 48 | 0 | 0 | 22 |
| 剧情/任务线 | 30 | 6 | 13 | 1 | 10 | 0 |
| 地域/场景 | 41 | 10 | 22 | 0 | 9 | 0 |
| 势力/门派/组织 | 29 | 10 | 6 | 0 | 11 | 2 |
| 玩法体验 | 27 | 7 | 11 | 2 | 6 | 1 |
| 创作边界 | 13 | 7 | 4 | 1 | 1 | 0 |
| 回归题库 | 8 | 0 | 8 | 0 | 0 | 0 |

## 人物补漏审计 v0.2

- 本轮新增人物/群像缺口：`54` 条，当前人物盘点共 `130` 行。
- 已补入的核心漏项包括：赵光义/晋中原、张彦霖、周蔷、画睛兄、兰澳、薛丑、洪肆、魏仁浦、冯继昇、燕/应宁、蓬山四海豪客、四猫三鹅伙伴群像和墨山道代号人物。
- 第一批已转入正式人物库：`19` 条；第二批又转入/增厚 `13` 条；第三批转入 `12` 条，当前人物盘点 `已有` 为 `60` 条；这些条目仍可继续用实录和公司资料增厚。
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
| `character-chai-rong` | 柴荣 | 已有 | P0 | accepted_story_synthesis, current_kb | 已补 5 张正式人物卡 | 历史人物与悬剑/春秋别馆/旧朝理想相关，是家国线的重要锚点。 风险：正式库第二批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-guo-xin` | 郭昕 | 已有 | P0 | accepted_story_synthesis, current_kb | 已补 5 张正式人物卡 | 归义军/边关家国线核心历史锚点。 风险：正式库第二批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-huiyao` | 慧药 | 已有 | P0 | accepted_story_synthesis, current_kb | 已补 5 张正式人物卡 | 破戒佛爷/善妙洲/佛门失序线关键人物，支撑信仰崩塌类歌词。 风险：正式库第二批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-li-zuo` | 李祚 | 已有 | P0 | accepted_story_synthesis, current_kb | 已补 5 张正式人物卡 | 绣金楼/寒香寻/开封暗线常见关联人物，需要边界化。 风险：正式库第二批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-ma-budai` | 麻布袋 | 已有 | P0 | accepted_story_synthesis, current_kb | 已补 5 张正式人物卡 | 河西主章高频人物，关系到问月长安和河西旅程。 风险：正式库第二批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-shen-yilun` | 沈义伦 | 已有 | P0 | accepted_story_synthesis, current_kb | 已补 5 张正式人物卡 | 九流暗涌/开封鬼市线关键人物，影响九流门歌准确性。 风险：正式库第二批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-tang-xinci` | 唐新词 | 已有 | P0 | accepted_story_synthesis, current_kb | 已补 5 张正式人物卡 | 青州/文津馆核心人物，支撑书院与文字主题。 风险：正式库第二批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-wang-qing` | 王清 | 已有 | P0 | accepted_story_synthesis, current_kb | 已补 5 张正式人物卡 | 燕北盟、天泉和北地遗民情绪的高价值人物。 风险：正式库第二批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-yaoyaoyao` | 姚药药 | 已有 | P0 | accepted_story_synthesis, current_kb | 已补 5 张正式人物卡 | 清河/开封早期玩家记忆人物，适合补普通人、医药、陪伴和成长线。 风险：正式库第二批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-yingying-wenwuque` | 盈盈/温无缺 | 已有 | P0 | accepted_story_synthesis, current_kb | 已补 5 张正式人物卡 | 开封高频人物线，可承接身份、伪装、情感与市井张力。 风险：正式库第二批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-zhang-yichao` | 张议潮 | 已有 | P0 | accepted_story_synthesis, current_kb | 已补 5 张正式人物卡 | 归义军与归唐情绪核心人物，适合家国大歌。 风险：正式库第二批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-zhang-yanlin` | 张彦霖 | 已有 | P0 | accepted_story_synthesis, current_kb | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：所属势力和完整经历仍薄，避免把玩家整理扩写成官方定论。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-zhao-guangyi-jinzhongyuan` | 赵光义/晋中原 | 已有 | P0 | accepted_story_synthesis, current_kb | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：当前主要来自百科/攻略综合；具体人格、剧情走向和结局需用游戏内实录或公司资料复核。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-zhou-qiang` | 周蔷 | 已有 | P0 | accepted_story_synthesis, current_kb | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：需区分游戏设定与历史借影，不写成官方已确认历史原型。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-an-liuli` | 安琉璃 | 缺失 | P1 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 凉州/河西人物，补异域与身份线但需防猎奇。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `character-bai-chuyu` | 柏楚玉 | 已有 | P1 | accepted_story_synthesis, current_kb | 已补 4 张正式人物卡 | 势力/门派代表人物，适合补组织内人物关系。 风险：正式库第三批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-bailangzhu` | 白狼主 | 缺失 | P1 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 官方/社区高频人物或伙伴线索，需确认实体属性。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `character-baoyin` | 宝音 | 缺失 | P1 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 河西人物补足，适合故乡、声音、童年意象。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `character-fang-bai` | 方白 | 缺失 | P1 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 青州/文津馆人物，补同窗和求学关系。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `character-feng-yi` | 冯夷 | 缺失 | P1 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 开封/河道/怪异线索人物，适合水与旧事意象。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `character-fubao` | 福宝 | 缺失 | P1 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 伙伴/人物线索，适合陪伴与福气意象。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `character-fulushou-sisters` | 福禄寿三姐妹 | 已有 | P1 | accepted_story_synthesis, current_kb | 已补 4 张正式人物卡 | 开封市井/任务线群像，可补热闹与荒诞感。 风险：正式库第三批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-gui-nainai` | 龟奶奶 | 已有 | P1 | accepted_story_synthesis, current_kb | 已补 4 张正式人物卡 | 开封记忆点 NPC，补民间奇人和温情/怪诞。 风险：正式库第三批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-heran` | 贺然 | 已有 | P1 | accepted_story_synthesis, current_kb | 已补 4 张正式人物卡 | 清河支线与旧人关系素材，适合写江湖旧识和身份转折。 风险：正式库第三批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-huajing-xiong` | 画睛兄 | 已有 | P1 | accepted_story_synthesis, current_kb | 已补 4 张正式人物卡 | 文津馆人物，名字自带点睛、书画、题卷和真假成像的歌词意象。 风险：正式库第三批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-huakui` | 花魁 | 缺失 | P1 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 青州/开封审美人物线索，需防泛化。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `character-lan-ao` | 兰澳 | 已有 | P1 | accepted_story_synthesis, current_kb | 已补 4 张正式人物卡 | 无心谷/太行山人物，可承接隐谷、断念、避世、山风与旧信等情绪。 风险：正式库第三批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-lian-daozi` | 廉道子 | 缺失 | P1 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 河西/边地人物，补道义与荒漠行路。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `character-liu-qingyi` | 柳青衣 | 已有 | P1 | accepted_story_synthesis, current_kb | 已补 4 张正式人物卡 | 势力代表人物，补江湖组织人物层。 风险：正式库第三批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-lizhenzhen` | 黎蓁蓁 | 已有 | P1 | accepted_story_synthesis, current_kb | 已补 4 张正式人物卡 | 清河/开封女性人物线索，补情感关系和市井人物厚度。 风险：正式库第三批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-murong-yanzhao` | 慕容延钊 | 已有 | P1 | accepted_story_synthesis, current_kb | 已补 4 张正式人物卡 | 开封/庙堂线关键历史人物素材。 风险：正式库第三批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-shi-jiu` | 史鸩 | 已有 | P1 | accepted_story_synthesis, current_kb | 已补 4 张正式人物卡 | 开封暗线/人物冲突素材，适合阴谋与代价。 风险：正式库第三批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-shi-shouxin` | 石守信 | 已有 | P1 | accepted_story_synthesis, current_kb | 已补 4 张正式人物卡 | 庙堂/开封权力场人物，补朝野线。 风险：正式库第三批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-shi-yimo` | 时一墨 | 缺失 | P1 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 墨山道/机关或门派代表人物，补不见山线。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `character-song-jiuwei` | 宋九薇 | 缺失 | P1 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 青州/伙伴或人物线，高频官方公告出现。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `character-wangyue-chanyuan` | 望月婵媛 | 缺失 | P1 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 河西/凉州女性人物与月意象素材。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `character-xiao-mao-youan` | 小猫佑安 | 缺失 | P1 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 伙伴系统代表，支撑温柔陪伴类玩家故事。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `character-yan-shiqi` | 燕十七 | 缺失 | P1 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 青州/伙伴或人物线，易与小十七混淆，需要别名边界。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `character-ye-mocheng` | 叶墨城 | 缺失 | P1 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 青州/伙伴系统公告相关人物线索。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `character-yingling` | 鹰铃 | 缺失 | P1 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 河西/伙伴或人物线，高频创作意象。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `character-zhang-huaishen` | 张淮深 | 缺失 | P1 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 归义军后续历史人物，补家族/守土延续感。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `character-zhao-chengzong` | 赵承宗 | 缺失 | P1 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 宫城/庙堂人物，补皇城权力线。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `character-zhao-dage` | 赵大哥 | 已有 | P1 | accepted_story_synthesis, current_kb | 已补 4 张正式人物卡 | 开封普通人/市井关系线，适合玩家故事和日常烟火。 风险：正式库第三批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-zhu-yu` | 朱鱼 | 缺失 | P1 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 开封/九流关联人物，补市井暗线。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `current-character-017` | 天不收 | 已有 | P1 | current_kb | 已追加旧友边界与歌曲结构卡 | 已在第二批正式人物库中增厚，可被检索和用于写词；后续仍可按实录继续增厚。 风险：accepted_story_synthesis 不应对外宣称官方定论。 |
| `current-character-011` | 鬼市子 | 已有 | P1 | current_kb | 已追加身份壳边界与歌曲结构卡 | 已在第二批正式人物库中增厚，可被检索和用于写词；后续仍可按实录继续增厚。 风险：accepted_story_synthesis 不应对外宣称官方定论。 |
| `character-companion-four-cats-three-geese` | 四猫三鹅伙伴群像 | 已有 | P1 | accepted_story_synthesis, current_kb, official_public_seed | 伙伴系统总览卡；同行情绪卡；首期伙伴群像卡；禁写误区卡 | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：官方确认群体和系统上线，但未在公开稿中逐一给出七个正式名字。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-feng-jisheng` | 冯继昇 | 已有 | P1 | accepted_story_synthesis, current_kb | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：名字存在简化写法，完整剧情位置需后续补实录。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-hong-si` | 洪肆 | 已有 | P1 | accepted_story_synthesis, current_kb | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：目前主要是名单级信息，不能过度扩写具体经历。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-ji-fuzhen` | 季浮真 | 已有 | P1 | current_kb, official_public_seed | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：官方公开为四海豪客/首领线，不要误写成伙伴。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-lin-xingyi` | 林行逸 | 已有 | P1 | current_kb, official_public_seed | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：官方公开为四海豪客/首领线，不要误写成伙伴。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-liu-yanru` | 柳晏如 | 已有 | P1 | current_kb, official_public_seed | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：官方公开为四海豪客/首领线，不要误写成伙伴。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-wei-renpu` | 魏仁浦 | 已有 | P1 | accepted_story_synthesis, current_kb | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：公开资料多来自维基/玩家整理，入库需标 accepted_story_synthesis。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-xie-changgeng` | 谢长庚 | 已有 | P1 | current_kb, official_public_seed | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：官方公开为四海豪客/首领线，不要误写成伙伴。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-xue-chou` | 薛丑 | 已有 | P1 | accepted_story_synthesis, current_kb | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：部分信息来自搜索摘要和攻略整合，正式人物传需二次核对。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-yan-yingning` | 燕/应宁 | 已有 | P1 | accepted_story_synthesis, current_kb | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：“燕”同时是普通字，检索和别名匹配需避免误召。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-zhu-yinghan` | 祝英晗 | 已有 | P1 | current_kb, official_public_seed | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：官方公开为四海豪客/首领线，不要误写成伙伴。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-caojing-guanyin` | 曹敬观音 | 缺失 | P2 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 河西/宗教与边地叙事线索，需谨慎。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `character-cheng-xin` | 程心 | 缺失 | P2 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 青溪/弱水岸相关人物，短笛、焚焰、黑暗中守理想等意象适合抒情。 风险：资料多来自物件/关联摘要，人物主线需后续补齐。 |
| `character-diao-xu` | 翟煦 | 缺失 | P2 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 势力代表人物补足。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `character-feng-ruzhi` | 冯如之 | 缺失 | P2 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 冯夷相关人物补足，主要服务关系网。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `character-he-jin` | 贺津 | 缺失 | P2 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 河西人物关系补足。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `character-he-wanchun` | 贺万春 | 缺失 | P2 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 人物关系补足。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `character-he-youqu` | 贺又渠 | 缺失 | P2 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 河西人物关系补足。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `character-lan-he` | 蓝何 | 缺失 | P2 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 人物关系补足。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `character-liu-duo` | 刘铎 | 缺失 | P2 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 青州人物补足。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `character-lv-xingshi` | 吕行世 | 缺失 | P2 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 青州/蓬山人物补足。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `character-nanzhu-gongzi` | 南烛公子 | 缺失 | P2 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 开封/支线风格化人物，可作奇人线。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `character-pu-xiansheng` | 蒲先生 | 缺失 | P2 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 开封民间人物，补市井与小人物故事。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `character-qin-ruolan` | 秦弱兰 | 缺失 | P2 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 势力/人物关系补足。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 v0.2 补充：存在“秦弱兰/秦若兰”异写，正式入库时需统一 canonical 与别名。 |
| `character-qing-moshandao` | 青 | 缺失 | P2 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 墨山道前代人物，可补前代师承、山中传承和隐秘旧事。 风险：更多来自手札/物件线索，不要写成完整官方传记。 |
| `character-shi-zhen` | 石贞 | 缺失 | P2 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 开封人物关系补足项，优先级低于主线人物。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `character-sinan-swordsman` | 司南剑客 | 缺失 | P2 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 剑客/门派风味素材，需确认具体归属。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `character-sun-buqi` | 孙不弃 | 缺失 | P2 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 势力代表人物补足。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `character-sun-yuan` | 孙愿 | 缺失 | P2 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 势力代表人物补足。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `character-wang-wenli` | 王文礼 | 缺失 | P2 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 青州/文津馆人物补足。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `character-wei-zhixi` | 魏芷昔 | 缺失 | P2 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 开封女性人物补足，避免女性角色只剩寒香寻/红线。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `character-zhang-cuo` | 张错 | 缺失 | P2 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 开封/势力人物补足。 风险：多依赖攻略/维基/剧情整理和自问自答，入库时应标 accepted_story_synthesis；具体结局、亲缘、阵营结论需实录或公司资料复核。 |
| `character-zhang-wanshi` | 张万师 | 缺失 | P2 | accepted_story_synthesis | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 墨山道人物，适合师承、规训、工匠伦理和技艺传递。 风险：目前只有名单级公开信息，优先级低于冯继昇/燕。 |
| `character-companion-daili-cat` | 呆狸猫 | 仅待核线索 | P2 | pending_clues | 伙伴候选卡；同行情绪卡；社区猜测边界卡 | 笨拙可爱和反差萌明显，适合生活流、轻喜剧和暖心陪伴。 风险：非官方命名页，先作为待官方命名确认层。 |
| `character-companion-daoba-cat` | 刀疤猫 | 仅待核线索 | P2 | pending_clues | 伙伴候选卡；同行情绪卡；社区猜测边界卡 | 旧伤、流浪和野性意象强，适合江湖旧创与陪伴主题。 风险：非官方命名页；伙伴等同某既有人物的映射只能作为社区猜测。 |
| `character-companion-dianmo-goose` | 典墨鹅 | 仅待核线索 | P2 | pending_clues | 伙伴候选卡；同行情绪卡；社区猜测边界卡 | 典墨与文津馆书卷气适配，适合墨色、典籍和儒雅陪伴。 风险：来源夹带社区身份猜测，不能作为事实层。 |
| `character-companion-duxia-cat` | 独侠猫 | 仅待核线索 | P2 | pending_clues | 伙伴候选卡；同行情绪卡；社区猜测边界卡 | 名字自带独行侠气质，适合孤行江湖、外冷内热和陪伴反差。 风险：非官方命名页；伙伴等同某既有人物的映射只能作为社区猜测。 |
| `character-companion-linge-cat` | 灵娥猫 | 仅待核线索 | P2 | pending_clues | 伙伴候选卡；命名冲突卡；同行情绪卡 | 轻灵、月色和梦幻感明显，可服务温柔陪伴类歌词。 风险：存在“灵娥/灵蛾”写法分歧，且官方公开口径对其上线状态不够稳定。 |
| `character-companion-yangfeng-goose` | 扬风鹅 | 仅待核线索 | P2 | pending_clues | 伙伴候选卡；同行情绪卡；社区猜测边界卡 | 扬风适合海风、轻狂、冲撞感和远行陪伴。 风险：非官方命名页；社区对其身份也无定论。 |
| `character-companion-zhujin-goose` | 逐金鹅 | 仅待核线索 | P2 | pending_clues | 伙伴候选卡；同行情绪卡；社区猜测边界卡 | 逐金可延展为奔忙、财气、世俗愿望和旅途小心思。 风险：来源夹带社区身份猜测，不能作为事实层。 |
| `character-saodi-mentong` | 扫地门童 | 已有 | P2 | current_kb, official_public_seed | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：官方未给正式姓名，先按描述性名称记录。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `current-character-001` | 寒香寻 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-character-002` | 寻心 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-character-003` | 阿依努尔 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-character-004` | 田英 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-character-005` | 叶万山 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-character-006` | 小十七 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-character-007` | 道主 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-character-008` | 无相皇 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-character-009` | 容鸢 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-character-010` | 千夜 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-character-012` | 郑鄂 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-character-013` | 江无浪 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-character-014` | 褚清泉 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-character-015` | 红线 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-character-016` | 伊刀 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-character-018` | 玩家游侠 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `character-group-kaifeng-fanlou-goldstreet` | 开封樊楼金街群像 | 缺失 | P3 | official_public_seed, accepted_story_synthesis | 群像定位卡；场景/关系意象卡；可唱视角卡；不拆单卡边界 | 承接樊楼、金街、宴饮、侍宴和风月市井，可写繁华与虚浮。 风险：颜楚楚、江离等若后续有独立剧情再拆单卡。 |
| `character-group-kaifeng-underclass-ghostmarket` | 开封底层/鬼市边民群像 | 缺失 | P3 | official_public_seed, accepted_story_synthesis | 群像定位卡；场景/关系意象卡；可唱视角卡；不拆单卡边界 | 承接金街与贫民窟、鬼市之间的强反差，适合暗面开封和底层求生。 风险：不要把所有底层 NPC 具体身份写死；多用群像表达。 |
| `character-group-qinghe-family-neighbors` | 清河村居亲缘群像 | 缺失 | P3 | accepted_story_synthesis | 群像定位卡；场景/关系意象卡；可唱视角卡；不拆单卡边界 | 承接母子、村邻、宗族和日常安置，适合普通玩家故事。 风险：靳春娘/靳小宝、汤瘸佬/汤驴宝可视素材量升级为双人关系卡。 |
| `character-group-qingzhou-neighbor-hospitality` | 青州好客老乡群像 | 缺失 | P3 | official_public_seed | 群像定位卡；场景/关系意象卡；可唱视角卡；不拆单卡边界 | 承接齐鲁风土、热汤招待、送路人、送礼人和好客日常。 风险：青州普通 NPC 名单公开不足，先按系统和区域气质做群像。 |
| `character-qinghe-fangxu` | 方旭 | 缺失 | P3 | accepted_story_synthesis | 普通人单卡样本；个人议题卡；乡野烟火卡 | 普通 NPC 中记忆点较强，可补清河小人物和日常抉择。 风险：仅作“可升单卡样本”，事实细节需二次核对。 |
| `character-qinghe-jin-chunniang` | 靳春娘 | 缺失 | P3 | accepted_story_synthesis | 普通人单卡样本；亲缘关系卡；村居烟火卡 | 与村居亲缘关系强，适合母子安置、乡邻照看和乱世日常。 风险：也可并入清河村居亲缘群像，正式补库时按素材量决定是否拆单卡。 |
| `character-qinghe-songwu` | 宋五 | 缺失 | P3 | accepted_story_synthesis | 普通人单卡样本；个人议题卡；乡野烟火卡 | 普通 NPC 中诉求明确，可服务旅行愿望、小人物自我实现等歌词。 风险：仅作“可升单卡样本”，事实细节需二次核对。 |
| `character-qinghe-xiangdemei` | 乡德美 | 缺失 | P3 | accepted_story_synthesis | 普通人单卡样本；个人议题卡；乡野烟火卡 | 普通 NPC 中个人议题明显，可补自我认同、乡村表达和烟火气。 风险：仅作“可升单卡样本”，不要由此把所有好感 NPC 都逐个建卡。 |
| `character-li-jiaojiao` | 李蕉蕉 | 仅待核线索 | P3 | pending_clues | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 醉花阴“艳湖七美”线索，可补轻灵、弈棋、风月雅趣。 风险：偏社区维基趣味条目，是否进正式人物库待确认。 |
| `character-moshandao-bird-bei` | 鵯 | 仅待核线索 | P3 | pending_clues | 代号基础卡；墨山道群像关系卡；可用意象卡；禁写误区卡 | 墨山道鸟名代号，适合微小、群鸣和山林细节。 风险：仅有代号式名单或极薄公开信息，适合作为待补采样位，不应扩写具体经历。 |
| `character-moshandao-bird-guan` | 鹳 | 仅待核线索 | P3 | pending_clues | 代号基础卡；墨山道群像关系卡；可用意象卡；禁写误区卡 | 墨山道鸟名代号，适合远行、迁徙和山外消息。 风险：仅有代号式名单或极薄公开信息，适合作为待补采样位，不应扩写具体经历。 |
| `character-moshandao-bird-he` | 鹤 | 仅待核线索 | P3 | pending_clues | 代号基础卡；墨山道群像关系卡；可用意象卡；禁写误区卡 | 墨山道鸟名代号，适合清冷、长寿、山门秩序。 风险：仅有代号式名单或极薄公开信息，适合作为待补采样位，不应扩写具体经历。 |
| `character-moshandao-bird-jiu` | 鹫 | 仅待核线索 | P3 | pending_clues | 代号基础卡；墨山道群像关系卡；可用意象卡；禁写误区卡 | 墨山道鸟名代号，适合荒山、残酷和俯瞰死亡。 风险：仅有代号式名单或极薄公开信息，适合作为待补采样位，不应扩写具体经历。 |
| `character-moshandao-bird-ju` | 鵙 | 仅待核线索 | P3 | pending_clues | 代号基础卡；墨山道群像关系卡；可用意象卡；禁写误区卡 | 墨山道鸟名代号，适合尖锐、截断和机关杀机。 风险：仅有代号式名单或极薄公开信息，适合作为待补采样位，不应扩写具体经历。 |
| `character-moshandao-bird-lu` | 鹭 | 仅待核线索 | P3 | pending_clues | 代号基础卡；墨山道群像关系卡；可用意象卡；禁写误区卡 | 墨山道鸟名代号，适合山雾、机巧、冷色氛围。 风险：仅有代号式名单或极薄公开信息，适合作为待补采样位，不应扩写具体经历。 |
| `character-moshandao-bird-peng` | 鹏 | 仅待核线索 | P3 | pending_clues | 代号基础卡；墨山道群像关系卡；可用意象卡；禁写误区卡 | 墨山道鸟名代号，适合高处、远志、巨构机关。 风险：仅有代号式名单或极薄公开信息，适合作为待补采样位，不应扩写具体经历。 |
| `character-moshandao-bird-sun` | 隼 | 仅待核线索 | P3 | pending_clues | 代号基础卡；墨山道群像关系卡；可用意象卡；禁写误区卡 | 墨山道鸟名代号，偏锐利、俯冲、狩猎和精准机关感。 风险：仅有代号式名单或极薄公开信息，适合作为待补采样位，不应扩写具体经历。 |
| `character-moshandao-bird-ya` | 鸦 | 仅待核线索 | P3 | pending_clues | 代号基础卡；墨山道群像关系卡；可用意象卡；禁写误区卡 | 墨山道鸟名代号，适合阴影、传信、山中旧案。 风险：仅有代号式名单或极薄公开信息，适合作为待补采样位，不应扩写具体经历。 |
| `character-moshandao-bird-yao` | 鹞 | 仅待核线索 | P3 | pending_clues | 代号基础卡；墨山道群像关系卡；可用意象卡；禁写误区卡 | 墨山道鸟名代号，适合疾行、风中侦察和危险预兆。 风险：仅有代号式名单或极薄公开信息，适合作为待补采样位，不应扩写具体经历。 |
| `character-moshandao-bird-yu` | 鹬 | 仅待核线索 | P3 | pending_clues | 代号基础卡；墨山道群像关系卡；可用意象卡；禁写误区卡 | 墨山道鸟名代号，适合水岸、细密观察和轻巧动作。 风险：仅有代号式名单或极薄公开信息，适合作为待补采样位，不应扩写具体经历。 |
| `character-ni-laoshan` | 倪老山 | 仅待核线索 | P3 | pending_clues | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 九流门长老/门规线索，可作为江湖规矩和黑色幽默素材。 风险：主要来自门派攻略整合，未见独立人物页。 |
| `character-qi-youyou` | 戚柚柚 | 仅待核线索 | P3 | pending_clues | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 醉花阴“艳湖七美”线索，可补美食、美人、池塘和钓者成群。 风险：别名更像文内称呼，事实层不要写死。 |
| `character-yuan-boshi` | 袁博士 | 仅待核线索 | P3 | pending_clues | 人物基础身份卡；人生时间线卡；关系网络卡；情绪弧线卡；... | 青溪入门接引/考验线索，可支撑问诊、诊金、医德选择。 风险：主要见于入门攻略整合页，先作待核线索。 |
| `character-group-hexi-liangzhou-travelers` | 河西/凉州边塞行旅群像 | 已有 | P3 | accepted_story_synthesis, current_kb, official_public_seed | 群像定位卡；场景/关系意象卡；可唱视角卡；不拆单卡边界 | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：河西普通 NPC 摘要偏零散，先群像后升卡。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-group-kaifeng-baijie` | 开封坊巷百业群像 | 已有 | P3 | accepted_story_synthesis, current_kb, official_public_seed | 群像定位卡；场景/关系意象卡；可唱视角卡；不拆单卡边界 | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：开封 NPC 量极大，默认群像，不按人头铺卡。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-group-pengshan-wenjinguan-students` | 蓬山文津馆学子/舍友群像 | 已有 | P3 | accepted_story_synthesis, current_kb, official_public_seed | 群像定位卡；场景/关系意象卡；可唱视角卡；不拆单卡边界 | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：公开具名普通 NPC 少，不宜大规模建单卡。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |
| `character-group-qinghe-rural-livelihood` | 清河乡野营生群像 | 已有 | P3 | accepted_story_synthesis, current_kb, official_public_seed | 群像定位卡；场景/关系意象卡；可唱视角卡；不拆单卡边界 | 已在正式人物库第一批补入，可被检索和用于写词；后续仍可按实录继续增厚。 风险：不要逐个拆朱八碗、王多鲈等低叙事 NPC；后续有独立长支线再升级。 正式库第一批仅做创作可用卡片，不代表人物传已完全终局。 |

### 剧情/任务线

| ID | 名称 | 状态 | 优先级 | 来源等级 | 建议补卡 | 写词影响 / 风险 |
| --- | --- | --- | --- | --- | --- | --- |
| `story-buxianxian-fire-return` | 不羡仙 | 缺失 | P0 | official_public_seed, accepted_story_synthesis | 酒馆与寒香寻；火烧前后反差；梦中不羡仙；重建前情绪边界 | 清河最强情绪节点之一，用户常点名。 风险：完整梗概多数依赖攻略/实录综合；结局、身份反转和真相需标 accepted_story_synthesis 或 pending_clues。 |
| `story-hexi-wenyue-changan` | 河西主章·问月长安 | 缺失 | P0 | official_public_seed, accepted_story_synthesis | 河西五段主章；玉门至凉州再至秦川；故乡/归唐/边塞/梦月意象 | 当前凉州只剩玉露为乡，不能代替河西主章。 风险：完整梗概多数依赖攻略/实录综合；结局、身份反转和真相需标 accepted_story_synthesis 或 pending_clues。 |
| `story-kaifeng-tiandi-ronglu` | 开封主章·天地熔炉 | 缺失 | P0 | official_public_seed, accepted_story_synthesis | 五段主章摘要；汴梁繁华与炉火；故人/权势/局中局；黑财神/郑鄂分流禁混 | 现有开封夜市与鬼市无法承接开封主线创作。 风险：完整梗概多数依赖攻略/实录综合；结局、身份反转和真相需标 accepted_story_synthesis 或 pending_clues。 |
| `story-qinghe-shenxian-budu` | 清河主章·神仙不渡 | 缺失 | P0 | official_public_seed, accepted_story_synthesis | 主章四段摘要；清河主情绪弧线；火烧前后状态变化；可用意象与禁写项 | 清河初入江湖现有卡过泛，难支撑少东家归家、灾前灾后反差。 风险：完整梗概多数依赖攻略/实录综合；结局、身份反转和真相需标 accepted_story_synthesis 或 pending_clues。 |
| `story-wuyou-dong` | 无忧洞 | 缺失 | P0 | official_public_seed, accepted_story_synthesis | 地宫/鬼樊楼/人市；道主与小福身份反转；九流门弟子救援；禁写过度定论 | 用户点名无忧洞/道主/阴间开封时需要独立命中。 风险：完整梗概多数依赖攻略/实录综合；结局、身份反转和真相需标 accepted_story_synthesis 或 pending_clues。 |
| `story-baitou-cheng` | 白头城 | 缺失 | P1 | official_public_seed, accepted_story_synthesis | 军旗回忆；城门难入；家书/旧军/白头城旧事 | 非常适合战后余烬、家书、边军未归。 风险：完整梗概多数依赖攻略/实录综合；结局、身份反转和真相需标 accepted_story_synthesis 或 pending_clues。 |
| `story-fengxue-daishan` | 风雪岱山 | 缺失 | P1 | official_public_seed, accepted_story_synthesis | 雪夜群困客栈；迷案推理；魔头传闻；旧案翻起 | 适合悬疑、雪夜、众声纷纭、真相迟到。 风险：完整梗概多数依赖攻略/实录综合；结局、身份反转和真相需标 accepted_story_synthesis 或 pending_clues。 |
| `story-jiangjun-ci` | 将军祠 | 缺失 | P1 | official_public_seed, accepted_story_synthesis | 荒祠旧将；忠勇余烬；将军像/祠火/甲胄意象 | 承接旧将未冷、乱世忠骨。 风险：完整梗概多数依赖攻略/实录综合；结局、身份反转和真相需标 accepted_story_synthesis 或 pending_clues。 |
| `story-puti-kuhai` | 菩提苦海 | 缺失 | P1 | official_public_seed, accepted_story_synthesis | 佛花与怪病；慈心表象/内里崩坏；镇守旧事留白 | 适合压抑、病相、慈悲反转和伪善。 风险：完整梗概多数依赖攻略/实录综合；结局、身份反转和真相需标 accepted_story_synthesis 或 pending_clues。 |
| `story-qingzhou-dongyue` | 青州主线前段·东岳 | 缺失 | P1 | official_public_seed, accepted_story_synthesis | 青州入境；岱岳/海风/市井；蓬山前置 | 当前青州蓬山文津直接跳过东岳。 风险：完整梗概多数依赖攻略/实录综合；结局、身份反转和真相需标 accepted_story_synthesis 或 pending_clues。 |
| `story-shenxiandu` | 神仙渡 | 缺失 | P1 | official_public_seed, accepted_story_synthesis | 归家入口；少东家身份感；渡口/酒肆/离人意象 | 适合回家、渡口、少年离乡后再回望。 风险：完整梗概多数依赖攻略/实录综合；结局、身份反转和真相需标 accepted_story_synthesis 或 pending_clues。 |
| `story-yumen-guan` | 玉门关 | 缺失 | P1 | official_public_seed, accepted_story_synthesis | 入关启程；风沙/思乡/驻边军；河西开场 | 适合边塞长路、初入河西、关门与月色。 风险：完整梗概多数依赖攻略/实录综合；结局、身份反转和真相需标 accepted_story_synthesis 或 pending_clues。 |
| `story-liangzhou-guiyijun` | 凉州与归义军旧事 | 偏薄 | P1 | official_public_seed, accepted_story_synthesis | 酒泉镇/无垢塔/玉露台；归义军伤兵与佛塔；归唐/他乡/庆典背后的创伤 | 承接归唐、边地清宁、盛宴与伤兵并存。 风险：完整梗概多数依赖攻略/实录综合；结局、身份反转和真相需标 accepted_story_synthesis 或 pending_clues。 |
| `current-storyline-021` | 九流祸起 | 需拆分 | P1 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-storyline-023` | 开封夜市与鬼市 | 需拆分 | P1 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-storyline-024` | 青州蓬山文津 | 需拆分 | P1 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-storyline-028` | 同行伙伴 | 需拆分 | P1 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `story-companion-first-season` | 伙伴系统首期剧情 | 需拆分 | P1 | official_public_seed, accepted_story_synthesis | 结伴考验；同行反馈；陪伴/不掉队；伙伴解锁边界 | 现有同行伙伴过抽象，不能支撑真实系统创作。 风险：完整梗概多数依赖攻略/实录综合；结局、身份反转和真相需标 accepted_story_synthesis 或 pending_clues。 |
| `story-guishizi-split` | 鬼市子 | 需拆分 | P1 | official_public_seed, accepted_story_synthesis | 地下鬼市气质；棺材入市/面具交易；阴兵借道；官财坊与主剧情分拆 | 黑市、假名、狂欢与阴气并置的强素材。 风险：完整梗概多数依赖攻略/实录综合；结局、身份反转和真相需标 accepted_story_synthesis 或 pending_clues。 |
| `story-jiuliu-split` | 九流线·奇谋/暗涌/祸起 | 需拆分 | P1 | official_public_seed, accepted_story_synthesis | 九流门众生视角；开封暗面；奇谋/暗涌/祸起三级拆分；朱佑生等线索降级 | 现有一条九流祸起承载太多内容，不利检索。 风险：完整梗概多数依赖攻略/实录综合；结局、身份反转和真相需标 accepted_story_synthesis 或 pending_clues。 |
| `story-wenjinguan-pengshan-split` | 文津馆与蓬山 | 需拆分 | P1 | official_public_seed, accepted_story_synthesis | 求学生活；门派晋升；同窗/舍友；藏书楼与文脉；... | 现有粗合并影响检索和情绪精度。 风险：完整梗概多数依赖攻略/实录综合；结局、身份反转和真相需标 accepted_story_synthesis 或 pending_clues。 |
| `story-zhulin-jiuju` | 竹林旧居 | 缺失 | P2 | official_public_seed, accepted_story_synthesis | 初入江湖；教学段不写教程化；竹林/断桥/旧居意象 | 适合初见江湖、未成侠先成伤的新手篇。 风险：完整梗概多数依赖攻略/实录综合；结局、身份反转和真相需标 accepted_story_synthesis 或 pending_clues。 |
| `story-boss-old-cases-index` | 镇守旧事总线 | 需拆分 | P2 | official_public_seed, accepted_story_synthesis | 按区域拆分索引卡；旧案/前尘/回忆机制；禁止跨案混写 | 便于命中旧案、镇守、前尘类用户意图。 风险：完整梗概多数依赖攻略/实录综合；结局、身份反转和真相需标 accepted_story_synthesis 或 pending_clues。 |
| `story-xiaji-canzhang-index` | 侠迹与残章索引线 | 需拆分 | P2 | official_public_seed, accepted_story_synthesis | 按区域卷次索引；官方命名侠迹单列；攻略命名残章降级 | 补全点名某卷侠迹/残章的检索能力。 风险：完整梗概多数依赖攻略/实录综合；结局、身份反转和真相需标 accepted_story_synthesis 或 pending_clues。 |
| `current-storyline-019` | 玉露为乡 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-storyline-020` | 弱水岸寻心 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-storyline-022` | 无名剑谱 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-storyline-025` | 清河初入江湖 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-storyline-026` | 门派倾覆 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-storyline-027` | 小人物不平事 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |

### 地域/场景

| ID | 名称 | 状态 | 优先级 | 来源等级 | 建议补卡 | 写词影响 / 风险 |
| --- | --- | --- | --- | --- | --- | --- |
| `region-bujianshan` | 不见山 | 缺失 | P0 | official_public_seed, accepted_story_synthesis | 地域/不见山/隐岳求真；地域/不见山/机关桃源与入山 | 补足求真、机关、云峰、最后桃源的独特审美。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 |
| `region-hexi` | 河西 | 缺失 | P0 | official_public_seed, accepted_story_synthesis | 地域/河西/三行诗总纲；地域/河西/黄沙-风雪-画卷递进 | 把玉门关-凉州-秦川串成完整远行/归途主轴。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 |
| `region-qinchuan` | 秦川 | 缺失 | P0 | official_public_seed, accepted_story_synthesis | 地域/秦川/画卷与四时；地域/秦川/归途将尽 | 把河西从诗意边塞推进到画卷归途。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 |
| `region-yumen-guan` | 玉门关 | 缺失 | P0 | official_public_seed, accepted_story_synthesis | 地域/玉门关/风沙与诗意；地域/玉门关/坎尔孜-城前隘-英烈冢 | 补足大漠、边关、旅人、残忍与温柔并置。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 |
| `region-fanlou-golden-street` | 樊楼金街 | 需拆分 | P0 | official_public_seed, accepted_story_synthesis | 场景/樊楼金街/纸醉金迷；场景/樊楼金街/热闹中孤单 | 开封夜生活和繁华暗面的最强入口词。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 |
| `region-palace-city` | 宫城 | 需拆分 | P0 | official_public_seed, accepted_story_synthesis | 地域/宫城/前朝后寝内诸司；地域/宫城/宫阙深处的人间 | 补皇城、朝野、宫墙、温情秘闻与禁地冒险。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 |
| `region-wenjinguan-school` | 文津馆学斋/校舍/藏书阁 | 需拆分 | P0 | official_public_seed, accepted_story_synthesis | 场景/文津馆/晨昏课业；场景/文津馆/同窗共居；场景/文津馆/藏书与传承 | 直接补求学、同窗、舍友情、书卷烟火素材。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 |
| `region-yulutai` | 玉露台 | 需拆分 | P0 | official_public_seed, accepted_story_synthesis | 场景/玉露台/葡萄园旧日；场景/玉露台/月光酒与离散 | 阿依努尔线和普通人故乡记忆的高频入口。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 |
| `region-baitou-cheng` | 白头城 | 缺失 | P1 | official_public_seed, accepted_story_synthesis | 场景/白头城/孤城白发；场景/白头城/安西军执念 | 强补家国、孤忠、旧军旅线素材。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 |
| `region-dasanguan` | 大散关 | 缺失 | P1 | official_public_seed, accepted_story_synthesis | 场景/大散关/越关入画；场景/大散关/险关与归途 | 适合写过关与命运转场。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 |
| `region-dongyue` | 东岳 | 缺失 | P1 | official_public_seed, accepted_story_synthesis | 地域/东岳/岱岳登临；地域/东岳/山海初见 | 把青州从泛书卷气落到山海相接。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 |
| `region-tengwang-village` | 藤王村 | 缺失 | P1 | official_public_seed, accepted_story_synthesis | 场景/藤王村/残藤与旧业；场景/藤王村/夜禁与朔雪 | 适合衰败村落、守藤人、旧酿与亡魂执念。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 |
| `region-tianshanglai` | 天上来 | 缺失 | P1 | official_public_seed, accepted_story_synthesis | 场景/天上来/河上奇局；场景/天上来/渡口与龙王传闻 | 补河道、吊机、渡口、奇局类意象。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 |
| `region-wugou-ta` | 无垢塔 | 缺失 | P1 | official_public_seed, accepted_story_synthesis | 场景/无垢塔/战火后静待重光；场景/无垢塔/护伤兵旧事 | 补佛塔、战后余烬、清宁重建。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 |
| `region-wuyou-dong` | 无忧洞 | 缺失 | P1 | official_public_seed, accepted_story_synthesis | 场景/无忧洞/细思极恐；场景/无忧洞/阴森不是鬼神 | 承接中式微恐、未尽人言、开封暗面。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 |
| `region-coffin-shop-ghost-market` | 棺材铺/鬼市入口 | 需拆分 | P1 | official_public_seed, accepted_story_synthesis | 场景/鬼市入口/棺材铺门缝；场景/鬼市入口/入局前一脚 | 九流门、鬼市子、开封暗面常用开笔入口。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 |
| `region-huoren-yiguan` | 活人医馆 | 需拆分 | P1 | official_public_seed, accepted_story_synthesis | 场景/活人医馆/医馆与枯井；场景/活人医馆/救治与禁入后院 | 弱水岸从抽象水岸感落到医馆、井、伤与秘密。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 |
| `region-seaside` | 海滨 | 需拆分 | P1 | official_public_seed, accepted_story_synthesis | 场景/海滨/黄河入海；场景/海滨/潮退拾鲜 | 补海风、潮声、滩涂、拾鲜等稀缺意象。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 |
| `region-wangdi-xiang` | 望帝乡 | 需拆分 | P1 | official_public_seed, accepted_story_synthesis | 场景/望帝乡/登塔回望；场景/望帝乡/故乡已非 | 强适配回望、归乡不能、旧名未忘。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 |
| `region-zhulin-jiuju` | 竹林旧居 | 需拆分 | P1 | official_public_seed, accepted_story_synthesis | 场景/竹林旧居/养育与临时的家；场景/竹林旧居/远行后的空屋 | 少东家童年、养父线、归家感高频必用。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 |
| `region-baicao-ye` | 百草野 | 缺失 | P2 | official_public_seed, accepted_story_synthesis | 地域/百草野/清河荒野；地域/百草野/遗址与残章 | 给清河补荒野旧迹面。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 |
| `region-baima-yi` | 白马驿 | 缺失 | P2 | official_public_seed, accepted_story_synthesis | 场景/白马驿/驿路与丧事村；场景/白马驿/路上人与未竟事 | 适合驿路、送别、途中旧事。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 |
| `region-chunqiu-bieguan` | 春秋别馆 | 缺失 | P2 | official_public_seed, accepted_story_synthesis | 场景/春秋别馆/旧馆与暗道；场景/春秋别馆/书架尘灰与北去之龙 | 补旧馆、暗道、别后无人回的气质。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 |
| `region-daishan` | 岱山 | 缺失 | P2 | official_public_seed, accepted_story_synthesis | 场景/岱山/风雪旧案；场景/岱山/众说纷纭 | 适合悬疑、雪夜、群像困局。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 |
| `region-fengqiu-village` | 封丘村 | 缺失 | P2 | official_public_seed, accepted_story_synthesis | 场景/封丘村/黄河沿岸村落；场景/封丘村/小人物往来 | 适合朴素村落、沿河日常、人情线。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 |
| `region-heshile` | 河市乐 | 缺失 | P2 | official_public_seed, accepted_story_synthesis | 场景/河市乐/戏台与偷听；场景/河市乐/沿河人声 | 补戏台、流言、沿河市声、民间热闹。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 |
| `region-jiangjun-ci` | 将军祠 | 缺失 | P2 | official_public_seed, accepted_story_synthesis | 场景/将军祠/碑与旧名；场景/将军祠/稚子与旧战 | 适合碑刻、旧将、稚子江湖、残响。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 |
| `region-poor-quarter` | 贫民窟 | 缺失 | P2 | official_public_seed, accepted_story_synthesis | 场景/贫民窟/热城背阴面；场景/贫民窟/乱世穷困 | 把开封从繁华单一面拉回人间切片。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 |
| `region-puti-kuhai` | 菩提苦海 | 缺失 | P2 | official_public_seed, accepted_story_synthesis | 场景/菩提苦海/鼓声与幻境；场景/菩提苦海/佛意与苦海 | 补佛性、苦海、鼓声召引的意象层。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 |
| `region-shanmiaozhou` | 善妙洲 | 缺失 | P2 | official_public_seed, accepted_story_synthesis | 地域/善妙洲/北向解锁；地域/善妙洲/佛爷寨与新地 | 补清河北向延展和继续上路空间感。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 |
| `region-yinyue-shan` | 隐月山 | 缺失 | P2 | official_public_seed, accepted_story_synthesis | 场景/隐月山/山顶试炼；场景/隐月山/雾里旧钟 | 补山场、雾、试炼、奇遇式叙事。 风险：地域事实层可用官方上线/场景资料；细剧情和支线氛围需降级为 accepted_story_synthesis。 |
| `current-region-029` | 清河 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-region-030` | 开封 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-region-031` | 雁门 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-region-032` | 凉州 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-region-033` | 长安 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-region-034` | 青州 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-region-035` | 蓬山 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-region-036` | 神仙渡 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-region-037` | 弱水岸 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-region-038` | 不羡仙 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |

### 势力/门派/组织

| ID | 名称 | 状态 | 优先级 | 来源等级 | 建议补卡 | 写词影响 / 风险 |
| --- | --- | --- | --- | --- | --- | --- |
| `faction-xiujinlou` | 绣金楼 | 缺失 | P0 | official_public_seed, accepted_story_synthesis | 绣金楼组织气质卡；梦傀/长生实验卡；寒香寻旧怨卡；千夜/无相皇挂钩卡 | 人物歌大量会碰到，缺主势力卡会导致角色线失根。 风险：组织宗旨、内部位阶、完整谱系很多不在官方公开资料中；入库需分清官方存在、综合叙事和待核线索。 |
| `faction-xuanjian` | 悬剑 | 缺失 | P0 | official_public_seed, accepted_story_synthesis | 春秋别馆据点卡；悬剑与柴荣旧事卡；褚清泉/田英/小十七关系卡 | 补清河主线、旧朝遗绪、理想主义与权术的门派气质。 风险：组织宗旨、内部位阶、完整谱系很多不在官方公开资料中；入库需分清官方存在、综合叙事和待核线索。 |
| `faction-yanbeimeng` | 燕北盟 | 缺失 | P0 | official_public_seed, accepted_story_synthesis | 王清与燕北盟起誓卡；将军祠民间记忆卡；中渡桥败局与遗民心气卡 | 家国、遗民、北伐未竟、英雄被遗忘等题材核心来源。 风险：组织宗旨、内部位阶、完整谱系很多不在官方公开资料中；入库需分清官方存在、综合叙事和待核线索。 |
| `faction-guyun` | 孤云 | 缺失 | P1 | official_public_seed, accepted_story_synthesis | 孤云门派气质卡；算学与天象卡；门规/门派地位开放节奏卡 | 作为门派/势力线索，后续需确认上线与玩法边界。 风险：组织宗旨、内部位阶、完整谱系很多不在官方公开资料中；入库需分清官方存在、综合叙事和待核线索。 |
| `faction-pojie-foye` | 破戒佛爷 | 缺失 | P1 | official_public_seed, accepted_story_synthesis | 善妙洲占寺为王卡；慧药与僧团失序卡；佛门外壳与匪气反差卡 | 补清河阴郁面、伪善/信仰崩塌/寺庙变寨。 风险：组织宗旨、内部位阶、完整谱系很多不在官方公开资料中；入库需分清官方存在、综合叙事和待核线索。 |
| `faction-tianhu-jun` | 天虎军 | 缺失 | P1 | official_public_seed, accepted_story_synthesis | 叶万山残部卡；菩提苦海赎罪与疯魔卡；北地守军转执念军卡 | 把叶万山从单人反派扩到整支军队的悲剧感。 风险：组织宗旨、内部位阶、完整谱系很多不在官方公开资料中；入库需分清官方存在、综合叙事和待核线索。 |
| `current-faction-042` | 文津馆 | 需拆分 | P1 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-faction-045` | 赤龙堂 | 需拆分 | P1 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-faction-051` | 归义军 | 需拆分 | P1 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `faction-ghost-market-order` | 鬼市地下秩序 | 需拆分 | P1 | official_public_seed, accepted_story_synthesis | 地下交易秩序卡；面具/假名/棺材入口卡；白天开封与夜间鬼市对照卡 | 现有角色域和剧情域有散点，但缺势力/秩序壳。 风险：组织宗旨、内部位阶、完整谱系很多不在官方公开资料中；入库需分清官方存在、综合叙事和待核线索。 |
| `faction-hexi-border-army` | 河西边军/凉州守军 | 需拆分 | P1 | official_public_seed, accepted_story_synthesis | 凉州城防卡；普通军士卡；归义军与地方守军差异卡 | 归义军过于大历史，缺凉州现场军旅质感。 风险：组织宗旨、内部位阶、完整谱系很多不在官方公开资料中；入库需分清官方存在、综合叙事和待核线索。 |
| `faction-kaifeng-government` | 开封官府/武德司 | 需拆分 | P1 | official_public_seed, accepted_story_synthesis | 开封府衙民政面卡；武德司监控/缉捕卡；官与江湖边界卡 | 开封歌、鬼市歌、庙堂线都需要白天秩序对照面。 风险：组织宗旨、内部位阶、完整谱系很多不在官方公开资料中；入库需分清官方存在、综合叙事和待核线索。 |
| `faction-khitan` | 契丹相关势力 | 需拆分 | P1 | official_public_seed, accepted_story_synthesis | 契丹南侵压迫卡；耶律阿不里祭祀/王族卡；边关对抗与民族他者视角卡 | 北地、燕北盟、天虎军、河西线都会回流到这层背景势力。 风险：组织宗旨、内部位阶、完整谱系很多不在官方公开资料中；入库需分清官方存在、综合叙事和待核线索。 |
| `faction-qingjiao-tang` | 青蛟堂 | 需拆分 | P1 | official_public_seed, accepted_story_synthesis | 龙蛟帮分裂卡；青蛟堂与赤龙堂对照卡；汪大洋/汴河渡口地面势力卡 | 当前赤龙堂已入库，但缺对手与前身，河运江湖叙事是断的。 风险：组织宗旨、内部位阶、完整谱系很多不在官方公开资料中；入库需分清官方存在、综合叙事和待核线索。 |
| `faction-no-sect` | 无门无派体系 | 需拆分 | P2 | official_public_seed, accepted_story_synthesis | 散修生存卡；偷师百家卡；不入门派也能成宗师卡 | 玩家自我投射很强。 风险：组织宗旨、内部位阶、完整谱系很多不在官方公开资料中；入库需分清官方存在、综合叙事和待核线索。 |
| `faction-qingzhou-literati` | 青州文脉关联组织 | 需拆分 | P2 | official_public_seed, accepted_story_synthesis | 文津馆正式制度卡；同窗/舍友/讲师生态卡；青州文脉与书院生活卡 | 把文津馆从书卷气升级成可写人群与秩序。 风险：组织宗旨、内部位阶、完整谱系很多不在官方公开资料中；入库需分清官方存在、综合叙事和待核线索。 |
| `faction-wumianren` | 无面人 | 需拆分 | P2 | official_public_seed, accepted_story_synthesis | 弱水岸群像卡；换脸失败/失身份意象卡 | 适合氛围创作，但不一定是稳定组织。 风险：组织宗旨、内部位阶、完整谱系很多不在官方公开资料中；入库需分清官方存在、综合叙事和待核线索。 |
| `faction-palace-institutions` | 开封宫城机构 | 仅待核线索 | P2 | pending_clues | 前朝/后寝/内诸司结构卡；宫墙罗网与秘库卡 | 对宫廷题材有用，但更像机构/场景。 风险：组织宗旨、内部位阶、完整谱系很多不在官方公开资料中；入库需分清官方存在、综合叙事和待核线索。 |
| `current-faction-039` | 九流门 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-faction-040` | 不老仙 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-faction-041` | 醉花阴 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-faction-043` | 无心谷 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-faction-044` | 三更天 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-faction-046` | 天泉 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-faction-047` | 梨园 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-faction-048` | 青溪 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-faction-049` | 狂澜 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-faction-050` | 墨山道 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `faction-qiongqi-shi` | 穷奇师 | 仅待核线索 | P3 | pending_clues | 伪装据点卡；敌营/奇袭氛围卡 | 可补敌营感，但暂不稳。 风险：组织宗旨、内部位阶、完整谱系很多不在官方公开资料中；入库需分清官方存在、综合叙事和待核线索。 |

### 玩法体验

| ID | 名称 | 状态 | 优先级 | 来源等级 | 建议补卡 | 写词影响 / 风险 |
| --- | --- | --- | --- | --- | --- | --- |
| `gameplay-coop-pve` | 多人副本与协作 PVE | 缺失 | P0 | official_public_seed, accepted_story_synthesis | feature_overview；fight_rolefeel；failure_story；eval_guardrail | 中强；并肩、翻车、复盘、再战，但避免 MMO 套词。 风险：玩法公开资料常混合前瞻、优化和正式上线；必须按 2026-06-13 切分已上线与预告。 |
| `gameplay-homestead-buxianxian` | 家业与不羡仙重建 | 缺失 | P0 | official_public_seed, accepted_story_synthesis | feature_overview；rebuild_emotion_arc；ordinary_player_story；usable_imagery；... | 极强；归乡、修屋、种地、认人、旧景重见。 风险：玩法公开资料常混合前瞻、优化和正式上线；必须按 2026-06-13 切分已上线与预告。 |
| `gameplay-music-system` | 丝竹雅韵与乐器系统 | 缺失 | P0 | official_public_seed, accepted_story_synthesis | feature_overview；cultural_context；usable_imagery；lyric_seed；... | 极强；平台作词链路和燕云世界感最直接桥梁之一。 风险：玩法公开资料常混合前瞻、优化和正式上线；必须按 2026-06-13 切分已上线与预告。 |
| `gameplay-open-world-compendium` | 开放世界探索与博物志 | 缺失 | P0 | official_public_seed, accepted_story_synthesis | exploration_loop；ordinary_player_story；collectible_flavor；usable_imagery；... | 强；普通玩家故事大底盘，补走路见世界。 风险：玩法公开资料常混合前瞻、优化和正式上线；必须按 2026-06-13 切分已上线与预告。 |
| `gameplay-pvp-zhige` | PvP 与止戈竞技 | 缺失 | P0 | official_public_seed, accepted_story_synthesis | mode_split；competitive_emotion；terminology_guardrail；eval_guardrail | 中；更偏 eval 和用户意图理解。 风险：玩法公开资料常混合前瞻、优化和正式上线；必须按 2026-06-13 切分已上线与预告。 |
| `gameplay-qinggong-exploration` | 轻功与立体探索 | 缺失 | P0 | official_public_seed, accepted_story_synthesis | mechanic_shell；player_moment；ordinary_player_story；usable_imagery；... | 强；轻功天然带风、檐、山、逃、赴约、少年气。 风险：玩法公开资料常混合前瞻、优化和正式上线；必须按 2026-06-13 切分已上线与预告。 |
| `gameplay-companion-split` | 伙伴系统细分 | 偏薄 | P0 | official_public_seed, accepted_story_synthesis | feature_overview；interaction_matrix；ordinary_player_story；usable_imagery；... | 极强；温柔、陪伴、玩闹、路感。 风险：玩法公开资料常混合前瞻、优化和正式上线；必须按 2026-06-13 切分已上线与预告。 |
| `gameplay-baiye` | 百业与百业战 | 缺失 | P1 | official_public_seed, accepted_story_synthesis | feature_overview；social_structure；ordinary_player_story；eval_guardrail；... | 中强；同伴结社、商战荒诞、群体荣誉、驻地热闹。 风险：玩法公开资料常混合前瞻、优化和正式上线；必须按 2026-06-13 切分已上线与预告。 |
| `gameplay-social-qingyilin` | 社交关系与情义林 | 缺失 | P1 | official_public_seed, accepted_story_synthesis | social_structure；ordinary_player_story；reward_loop；avoid_claims | 中强；同游、拜师、结义、暧昧未满。 风险：玩法公开资料常混合前瞻、优化和正式上线；必须按 2026-06-13 切分已上线与预告。 |
| `gameplay-stronghold-infiltration` | 据点渗透与战斗据点 | 缺失 | P1 | official_public_seed, accepted_story_synthesis | mechanic_shell；encounter_types；ordinary_player_story；avoid_claims | 中强；夜探、误入、引敌、摸营、撤离。 风险：玩法公开资料常混合前瞻、优化和正式上线；必须按 2026-06-13 切分已上线与预告。 |
| `gameplay-tianggong-diku` | 天工地窟与地牢秘境 | 缺失 | P1 | official_public_seed, accepted_story_synthesis | feature_overview；exploration_loop；coop_boundary；avoid_claims | 中；险、暗、机关、旧城、同行探坑。 风险：玩法公开资料常混合前瞻、优化和正式上线；必须按 2026-06-13 切分已上线与预告。 |
| `gameplay-crafting-cooking` | 采集制作与美食烹饪 | 偏薄 | P1 | official_public_seed, accepted_story_synthesis | ordinary_player_story；life_flavor；usable_imagery；weak_fact_guardrail | 强；食物、烟火气、赶海、采药、做菜。 风险：玩法公开资料常混合前瞻、优化和正式上线；必须按 2026-06-13 切分已上线与预告。 |
| `current-gameplay-059` | 伙伴同行 | 需拆分 | P1 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-gameplay-060` | 镇守首领 | 需拆分 | P1 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-gameplay-061` | 侠迹残章 | 需拆分 | P1 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `gameplay-faction-prestige` | 门派地位与晋升 | 需拆分 | P1 | official_public_seed, accepted_story_synthesis | feature_overview；faction_flavor；roleplay_loop；avoid_claims | 中；角色歌和门派代入有效。 风险：玩法公开资料常混合前瞻、优化和正式上线；必须按 2026-06-13 切分已上线与预告。 |
| `gameplay-life-qishu-split` | 潜行、点穴、妙手空空与兽语等生活奇术 | 需拆分 | P1 | official_public_seed, accepted_story_synthesis | subability_split；mechanic_shell；ordinary_player_story；avoid_claims | 中强；巧、偷、听、逗、骗的江湖感。 风险：玩法公开资料常混合前瞻、优化和正式上线；必须按 2026-06-13 切分已上线与预告。 |
| `gameplay-wild-boss-split` | 野外首领细分 | 需拆分 | P1 | official_public_seed, accepted_story_synthesis | boss_story_seed；encounter_types；ordinary_player_story；avoid_claims | 中；路上撞见强敌、败走、再回头。 风险：玩法公开资料常混合前瞻、优化和正式上线；必须按 2026-06-13 切分已上线与预告。 |
| `gameplay-appearance-wanxiang` | 外观、万相集与角色塑造 | 缺失 | P2 | official_public_seed, accepted_story_synthesis | feature_overview；identity_expression；weak_background_card | 弱到中；用户意图理解有帮助，通常只是背景。 风险：玩法公开资料常混合前瞻、优化和正式上线；必须按 2026-06-13 切分已上线与预告。 |
| `gameplay-boardgames-future` | 打马棋与侠客偶棋 | 仅待核线索 | P2 | pending_clues | roadmap_guardrail；future_hook；avoid_claims | 中；可做未来创作暗线，但不进入当前 live facts。 风险：玩法公开资料常混合前瞻、优化和正式上线；必须按 2026-06-13 切分已上线与预告。 |
| `current-gameplay-052` | 奇术 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-gameplay-053` | 偷师百家 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-gameplay-054` | 寻声 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-gameplay-055` | 中式解谜 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-gameplay-056` | 武学自由搭配 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-gameplay-057` | 营生 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-gameplay-058` | 不平事 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |

### 创作边界

| ID | 名称 | 状态 | 优先级 | 来源等级 | 建议补卡 | 写词影响 / 风险 |
| --- | --- | --- | --- | --- | --- | --- |
| `boundary-spoiler-policy` | 剧透等级与用户意图边界 | 缺失 | P0 | creative_guidance | 剧透等级矩阵；用户主动点名时的放开规则；默认写意不剧透规则 | 避免角色歌误剧透，也避免过度保守导致写不深。 风险：边界类不直接提供剧情事实，而是约束 Agent 表达与质量门。 |
| `boundary-user-oc-vs-canon` | 玩家原创角色与官方人物边界 | 缺失 | P0 | creative_guidance | 玩家故事保真升级卡；不强绑官方角色规则；可转译燕云世界观规则 | 用户不一定想写官方剧情，知识库不能吞掉玩家创意。 风险：边界类不直接提供剧情事实，而是约束 Agent 表达与质量门。 |
| `boundary-unconfirmed-relationship` | 未确认人物关系表达规则 | 需拆分 | P0 | creative_guidance | 关系置信等级表；可写/不可写措辞模板；pending_clues 使用样例 | 寒香寻/千夜/寻心等人物线很容易把推测写成定论。 风险：边界类不直接提供剧情事实，而是约束 Agent 表达与质量门。 |
| `boundary-ethnic-history` | 民族/历史映射表达边界 | 缺失 | P1 | creative_guidance | 历史映射降口径卡；民族他者描写禁区；家国线推荐措辞 | 家国歌很需要，但商业风险也高。 风险：边界类不直接提供剧情事实，而是约束 Agent 表达与质量门。 |
| `boundary-cover-title-policy` | 封面标题文字边界 | 偏薄 | P1 | creative_guidance | 受控歌名可用规则；假署名阻断规则；标题进入视觉 Prompt 的边界 | 封面允许作品名，但不能出现假歌手/厂牌/版权。 风险：边界类不直接提供剧情事实，而是约束 Agent 表达与质量门。 |
| `boundary-commercial-publish` | 社区发布交接边界 | 缺失 | P2 | creative_guidance | 本平台/公司系统职责卡；用户侧文案卡；交接状态卡 | 避免知识库把公司社区系统写成本平台能力承诺。 风险：边界类不直接提供剧情事实，而是约束 Agent 表达与质量门。 |
| `current-boundary-062` | 其他 IP 题材拒绝 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-boundary-063` | 现实歌手风格安全改写 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-boundary-064` | 剧情整合与官方口径分层 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-boundary-065` | 玩家故事保真 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-boundary-066` | 商业权益不承诺 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-boundary-067` | 封面文字受控 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |
| `current-boundary-068` | 暴力血腥与低俗限制 | 已有 | P2 | current_kb | 保留现有卡片；若状态为 thin/needs_fact_split，则按同名缺口或相邻缺口增厚/拆分。 | 当前已可被检索，但仍需根据缺口表判断是否增厚到商用百科级。 风险：不要把当前已有覆盖误认为已经完整；present 表示可用，非最终完备。 |

### 回归题库

| ID | 名称 | 状态 | 优先级 | 来源等级 | 建议补卡 | 写词影响 / 风险 |
| --- | --- | --- | --- | --- | --- | --- |
| `eval-character-depth` | eval-角色歌深度与不跑题 | 缺失 | P0 | creative_guidance, accepted_story_synthesis | 点名角色必须召回人物传/关系/地域，不写成泛古风；普通玩家故事不强塞官方人物 | 直接验证知识库对作词质量的核心收益。 风险：eval 不是知识事实正文，但必须覆盖真实玩家输入、边界拒绝和日期敏感问题。 |
| `eval-companion-boundary` | eval-伙伴陪伴边界 | 缺失 | P0 | creative_guidance, accepted_story_synthesis | 伙伴=同行者，不擅自写成人形宠物/恋爱对象/已上线后续狗系内容 | 伙伴容易被写偏成萌宠文或恋爱陪伴文。 风险：eval 不是知识事实正文，但必须覆盖真实玩家输入、边界拒绝和日期敏感问题。 |
| `eval-homestead-version` | eval-家业重建情感与版本差异 | 缺失 | P0 | creative_guidance, accepted_story_synthesis | 区分已上线家业、已承认缺陷、后续重做承诺 | 防止模型把理想中的家园写成当前已完全实现。 风险：eval 不是知识事实正文，但必须覆盖真实玩家输入、边界拒绝和日期敏感问题。 |
| `eval-live-vs-preview` | eval-已上线与预告玩法隔离 | 缺失 | P0 | creative_guidance, accepted_story_synthesis | 打马棋/侠客偶棋/新多人 PVE/身份博弈玩法不得写成已上线事实 | 日期敏感边界，防止未来内容污染当前主库。 风险：eval 不是知识事实正文，但必须覆盖真实玩家输入、边界拒绝和日期敏感问题。 |
| `eval-other-ip-rewrite` | eval-其他 IP 拒绝与情绪转译 | 缺失 | P0 | creative_guidance, accepted_story_synthesis | 其他 IP 主题歌拒绝；“某种情绪但改成燕云”允许转译且不保留专有名词 | 产品边界核心验收。 风险：eval 不是知识事实正文，但必须覆盖真实玩家输入、边界拒绝和日期敏感问题。 |
| `eval-pvp-pve-routing` | eval-PvP/PvE术语分流 | 缺失 | P0 | creative_guidance, accepted_story_synthesis | 区分试剑/侠境/止戈/觉障林/论剑/争锋/演武 | 防止试剑写成 PVP，或止戈误写成单一玩法。 风险：eval 不是知识事实正文，但必须覆盖真实玩家输入、边界拒绝和日期敏感问题。 |
| `eval-storyline-recall` | eval-剧情线召回与事实降级 | 缺失 | P0 | creative_guidance, accepted_story_synthesis | 点名剧情线必须召回正确起点/冲突/转折；待核结局不得写死 | 防止长剧情线误召回和编造。 风险：eval 不是知识事实正文，但必须覆盖真实玩家输入、边界拒绝和日期敏感问题。 |
| `eval-weak-official-downgrade` | eval-弱官方玩法的事实层降级 | 缺失 | P1 | creative_guidance, accepted_story_synthesis | 官方薄、攻略厚时自动降口径，不宣称“官方明确写过” | 关系知识库输出可信度和商业风险。 风险：eval 不是知识事实正文，但必须覆盖真实玩家输入、边界拒绝和日期敏感问题。 |

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
