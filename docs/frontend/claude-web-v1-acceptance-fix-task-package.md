# Claude Web v1 前端原型验收修复任务包

版本：v0.1
状态：待前端实现者修复
更新时间：2026-06-06
适用目录：`prototypes/Claude-web-v1`

## 背景

`prototypes/Claude-web-v1` 已交付 React + TypeScript + Vite 前端原型，并通过本地 `npm test`、
`npm run typecheck`、`npm run build` 和 390px / 1440px Playwright smoke。该原型已经具备
灵感成歌、填词成歌、歌词确认、生成中、失败页和成品页的基础流程。

本任务包只要求修复 OpenAPI v0.1、前端任务包和验收口径不一致的问题。不要修改 `apps/web`、
后端 Java 模块、数据库、配置文件、`docs` 或 Git 提交记录。

## 执行边界

- 只修改 `prototypes/Claude-web-v1`。
- 不调用真实 DeepSeek、Suno、MiniMax、Image 2 或公司系统。
- 真实后端默认地址仍为 `http://localhost:8080/api/v1`。
- 浏览器侧本地请求继续使用 `X-Mock-User-Id: mock_user_001`。
- 所有 POST 必须继续带唯一 `Idempotency-Key`。
- UI 按钮必须继续由后端 `available_actions` 驱动，不允许按前端猜状态硬编码。
- 用户侧文案不要出现“发布包 JSON”等技术词；可以使用“作品已准备好，可交给社区发布”。

## 必须修复

### 1. 补齐我的作品列表页

当前缺少 `GET /api/v1/works` 接入和“我的作品”页面，不满足原前端任务包验收。

修复要求：

- 新增作品列表路由和入口。
- 新增 `listWorks(page, page_size, status?)` API 封装。
- 列表至少展示 `work_code`、`song_title`、`status`、`generation_stage`、`package_status`、
  `cover_url`、`video_preview_url`、`updated_at`。
- 支持空列表、加载中、错误态和点击进入作品详情。
- 移动端 390px 和桌面 1440px 不允许横向溢出。

### 2. 成品页补齐发布交接信息

当前成品页只展示媒体资产和部分按钮，发布交接区缺少任务包要求的关键信息。

修复要求：

- 当 `status=GENERATED` 且 `package_status=PACKAGE_READY` 时调用
  `GET /works/{work_id}/publish-package`。
- 发布交接区展示用户可理解的下载链接和有效期。
- 展示发布包返回的 `package_url`、`package_json.video.url`、`package_json.cover.url`、
  `package_json.lyrics.text`。
- 音频、封面、视频展示优先使用 `media_assets`；交接区使用 `publish-package` 返回值。
- 保留并验证“刷新下载链接”和“标记已交接”。
- 补 `PACKAGE_READY` 和 `PACKAGE_FETCHED` 的回归测试。

### 3. 正确处理 `PACKAGE_BLOCKED`

当前 `PACKAGE_BLOCKED` 可能被错误派生到生成中或确认页，用户会看到假进度。

修复要求：

- 在状态派生中单独处理 `package_status=PACKAGE_BLOCKED`。
- 展示阻断原因 `blocked_reason` 和 `publish_handoff_hint.message`。
- 不显示生成进度。
- 不暴露技术错误；文案面向普通用户解释“作品暂不能交给社区发布”。

### 4. 润色请求体必须符合 OpenAPI

OpenAPI v0.1 要求 `LyricsPolishRequest.instruction` 必填且最少 1 个字符。当前前端允许空值。

修复要求：

- `LyricsPolishRequest.instruction` 类型改为必填。
- 润色弹窗为空时禁用提交并显示用户可理解的校验提示。
- 删除或改写“不填也可以”文案。
- 补“空 instruction 不发请求”的测试。

### 5. 补齐 `RETRY_COVER` 和 `RERENDER_VIDEO`

当前动作文案存在，但 API 封装和失败页动作执行缺失。

修复要求：

- 新增 `regenerateCover(workId)`，调用 `POST /works/{work_id}/cover/regenerate`。
- 新增 `rerenderVideo(workId)`，调用 `POST /works/{work_id}/video/rerender`。
- 失败页或动作组件在 `available_actions` 包含 `RETRY_COVER` / `RERENDER_VIDEO` 时显示按钮。
- loading、disabled、error 状态完整。
- 执行成功后刷新作品详情并进入后端返回状态对应视图。

### 6. 错误态展示 `request_id`

当前客户端已解析 `request_id`，但 UI 没有展示。

修复要求：

- 首页错误、作品页错误、动作错误 toast 都应展示或允许复制 `request_id`。
- 对 409 润色/续写次数耗尽，继续使用友好文案，同时保留请求编号。
- 补 404、409、500 的显示测试。

### 7. 作品详情补齐关键状态信息

当前详情页缺少多个验收字段。

修复要求：

- 详情头部展示 `status`、`generation_stage`、`package_status`。
- 展示 `quota_hint.message`。
- 展示 `publish_handoff_hint.message`。
- 失败态展示 `failure.failure_code`、`failure.failure_message`、`recommended_action`、
  `retry_count`、`retry_limit`、`remaining_retry_count`。
- 使用已有 `StatusPill` / `StagePill` 或等价组件，但不要牺牲移动端可读性。

## 可访问性与交互修复

- 弹窗需要焦点陷阱和关闭后焦点恢复。
- 伪 tab / radio 控件如果使用 `role=tab`、`role=radio`，需要补齐键盘行为；否则改成普通按钮或原生控件。
- 检查低对比文本颜色，尤其是深色背景上的次级文字。
- 所有按钮保留 loading / disabled 状态，避免重复提交。

## 验收命令

```bash
cd prototypes/Claude-web-v1
npm test
npm run typecheck
npm run build
```

建议补 Playwright smoke：

- 390px 宽度：灵感成歌、润色 2 次、第三次 409、确认出歌、成品页、刷新链接、标记交接。
- 390px 宽度：输入“失败”触发失败页，验证失败码、推荐动作、重试按钮。
- 1440px 宽度：首页、我的作品列表、作品详情、成品页无横向溢出。
- 真实后端模式：不加 `?mock=1`，请求走 `http://localhost:8080/api/v1`。

## 已知非阻断项

- 视觉审美本轮不做泛泛重做，先修契约、流程、状态、错误和响应式硬问题。
- `apps/web` 仍是正式工程 scaffold，本任务包不要求迁移到 `apps/web`。
- 账号、审核、权益、社区发布、分享均不在前端真实实现，只展示后端返回状态和提示。
