# Gemini 第 2 批前端任务包：Mock 作品生成闭环

文档日期：2026-06-05

## 1. 任务目标

基于 `docs/api/openapi-v0.1.yaml` 和当前本地后端 Mock API，完成移动端优先、兼容 PC Web 的前端业务页面原型。目标是让用户能在本地环境走通：

1. 灵感成歌创建作品。
2. 填词成歌创建作品。
3. 查看歌词草案并确认出歌。
4. 查看生成后的封面、视频、歌词和发布包状态。
5. 查看我的作品列表。
6. 对发布包执行刷新 URL、标记已交接等动作。

本任务包不要求高保真品牌视觉，不要求接入真实公司账号、审核、权益、发布、分享系统，不要求调用真实 DeepSeek、Suno、MiniMax、Image 2。

## 2. 技术与边界

- 工程位置：`apps/web`。
- 技术栈：React + Vite + TypeScript。
- API Base URL：默认 `http://localhost:8080`，通过环境变量配置。
- Mock 用户 Header：`X-Mock-User-Id: mock_user_001`。
- 所有 POST 请求必须带 `Idempotency-Key`，前端可用 `crypto.randomUUID()` 生成。
- 页面移动端优先，同时适配 PC Web。
- 视觉资产如需图片，后续优先用 Image 生成，不使用未经授权素材。

## 3. 页面范围

### 3.1 创作首页

核心入口：

- 灵感成歌。
- 填词成歌。
- 我的作品。

首页不做营销落地页，首屏直接进入创作工具。

### 3.2 灵感成歌页

字段：

- `story_input`，必填。
- `mood`，可选。
- `scene`，可选。
- `relationship`，可选。
- `music_style`，可选。
- `vocal_preference`，可选值：`AUTO`、`MALE`、`FEMALE`、`CHORUS`。

提交接口：

- `POST /api/v1/works/inspiration`

成功后跳转作品详情页。

### 3.3 填词成歌页

字段：

- `lyrics_input`，必填。
- `song_title`，可选。
- `music_style`，可选。
- `vocal_preference`，可选值同上。

提交接口：

- `POST /api/v1/works/lyrics`

成功后跳转作品详情页。

### 3.4 作品详情页

接口：

- `GET /api/v1/works/{work_id}`

需要显示：

- 作品标题。
- `status`。
- `generation_stage`。
- `package_status`。
- 歌词草案。
- `polish_remaining_count`。
- `quota_hint.message`。
- `publish_handoff_hint.message`。
- 失败原因 `failure.failure_message`。
- 当前 `available_actions` 对应的操作按钮。

动作映射：

| Action | 前端按钮 | 接口 |
|---|---|---|
| `POLISH_LYRICS` | 润色歌词 | `POST /api/v1/works/{work_id}/lyrics/polish` |
| `CONTINUE_LYRICS` | 续写歌词 | `POST /api/v1/works/{work_id}/lyrics/continue` |
| `CONFIRM_WORK` | 确认出歌 | `POST /api/v1/works/{work_id}/confirm` |
| `RETRY_COVER` | 重生封面 | `POST /api/v1/works/{work_id}/cover/regenerate` |
| `RERENDER_VIDEO` | 重渲视频 | `POST /api/v1/works/{work_id}/video/rerender` |
| `REFRESH_PACKAGE_URL` | 刷新下载链接 | `POST /api/v1/works/{work_id}/publish-package/refresh-url` |
| `MARK_PACKAGE_FETCHED` | 标记已交接 | `POST /api/v1/works/{work_id}/publish-package/mark-fetched` |
| `RETURN_TO_EDIT` | 返回编辑 | 本地路由处理 |
| `CONTACT_SUPPORT` | 联系支持 | 本地占位，不接真实客服 |

### 3.5 成品/发布包区域

接口：

- `GET /api/v1/works/{work_id}/publish-package`

当 `package_status = PACKAGE_READY` 时显示：

- `package_url`。
- `package_url_expires_at`。
- `package_json.video.url`。
- `package_json.cover.url`。
- `package_json.lyrics.text`。
- 刷新 URL、标记已交接按钮。

当 `PACKAGE_FETCHED` 时显示已交接状态，并保留刷新 URL。

当 `PACKAGE_NOT_READY` 或 `PACKAGE_BLOCKED` 时显示对应提示。

### 3.6 我的作品页

接口：

- `GET /api/v1/works?page=1&page_size=20`

列表字段：

- `work_code`。
- `song_title`。
- `status`。
- `generation_stage`。
- `package_status`。
- `cover_url`。
- `video_preview_url`。
- `updated_at`。

点击作品进入详情页。

## 4. 状态展示口径

关键状态：

- `LYRICS_READY`：展示歌词草案和确认出歌按钮。
- `GENERATING`：展示生成中状态，禁用重复提交。
- `GENERATED` + `PACKAGE_READY`：展示成品和发布包。
- `FAILED`：展示失败原因和 `available_actions`。
- `PACKAGE_FETCHED`：展示已交接。

生成阶段 `generation_stage` 应作为辅助提示，不要覆盖主状态。

## 5. 错误与加载

必须覆盖：

- 页面首次加载 skeleton 或 loading。
- POST 提交 loading，按钮防重复点击。
- 400/403/404/409/500 错误提示。
- API 不可达提示。
- 空作品列表。
- 发布包未就绪状态。

错误响应结构：

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Request failed",
    "request_id": "uuid",
    "timestamp": "2026-06-05T00:00:00Z"
  }
}
```

## 6. 禁区

- 不接真实账号登录。
- 不接真实公司审核、权益、发布、分享系统。
- 不调用真实 DeepSeek、Suno、MiniMax、Image 2。
- 不提交真实密钥、真实 Cookie、真实用户数据。
- 不把发布包文案暴露成过强技术语言，用户侧优先使用“成品已生成”“交接状态”等文案。

## 7. 验收

- 移动端 390px 宽度下主要页面无文字重叠、按钮溢出、横向滚动。
- PC Web 1440px 宽度下布局可读，不出现空洞的营销页。
- 灵感成歌和填词成歌均可创建作品。
- 作品详情能显示歌词草案。
- 确认出歌后能看到 `GENERATED`、`PACKAGE_READY` 和发布包信息。
- 标记已交接后能看到 `PACKAGE_FETCHED`。
- API 错误能显示 `error.message` 和 `request_id`。
