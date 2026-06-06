# 燕云乐坊 · 创作工作台原型（Claude-web-v1）

移动端优先、兼容 PC Web 的 AI 作曲创作工作台前端原型。基于现有后端 **OpenAPI v0.1** 构建，
第一屏即为实际产品功能（无营销落地页）。

> 这是独立原型，只读地调用后端，不修改 `apps/web`、后端、Java 模块或数据库。

## 技术栈

与仓库内 `apps/web` 保持一致：React 19 + TypeScript + Vite 8 + Vitest，纯 CSS（无 UI 框架）。

## 运行

```bash
cd prototypes/Claude-web-v1
npm install
npm run dev      # 本地 http://localhost:5273
```

### 两种运行模式

顶栏右上角可切换：

- **本地后端**（默认）：调用真实后端 `http://localhost:8080/api/v1`。
  - 由 Vite dev proxy 把 `/api` 转发到 8080，规避后端未配置 CORS 的问题。
  - 指向其它后端：`VITE_API_PROXY_TARGET=http://其它地址:端口 npm run dev`。
  - 启动后端见 `docs/runbook/local-development.md`（默认 `MUSIC_PROVIDER=mock`）。
- **演示模式**：纯前端内存模拟，**不联网、不调用任何真实模型**（DeepSeek / Suno / MiniMax / Image 2）。
  - 直接用 `http://localhost:5273/?mock=1` 打开即进入演示模式，便于离线演示与验收。
  - 忠实复刻后端状态机与 JSON 契约：歌词生成 → 确认 → 出歌进度 → 成品 / 失败重试。
  - 在灵感 / 歌词里写「失败」「fail」可触发一次旋律生成失败，演示失败页与重试。

## 实现的页面与流程

1. **创作首页** — 灵感成歌（`POST /works/inspiration`）、填词成歌（`POST /works/lyrics`）。
2. **歌词确认 / 作品详情** — `GET /works/{work_id}`；展示歌词、标题、摘要、编曲方向、燕云意象、
   剩余改词次数；AI 润色（`/lyrics/polish`）、AI 续写（`/lyrics/continue`）、确认出歌（`/confirm`）。
3. **生成中** — 状态为 `GENERATING` 或处于 `MUSIC_GENERATING / COVER_GENERATING / VIDEO_RENDERING /
   PACKAGE_BUILDING` 等阶段时展示进度，轮询 `GET /works/{work_id}` 更新。
4. **失败页** — 展示友好失败原因、建议动作；按 `available_actions` 渲染按钮，支持
   `POST /works/{work_id}/music/retry` 与返回编辑。
5. **成品页** — `status=GENERATED` 且 `package_status=PACKAGE_READY` 时调用
   `GET /works/{work_id}/publish-package`；媒体优先用 `media_assets`；支持标记已交接
   （`/publish-package/mark-fetched`）与刷新下载链接（`/publish-package/refresh-url`）。

## 关键设计原则

- **不猜状态**：UI 阶段由后端 `status` / `generation_stage` / `package_status` 派生
  （见 [src/api/workState.ts](src/api/workState.ts)），按钮完全由 `available_actions` 驱动。
- **幂等**：每个 POST 自动带唯一 `Idempotency-Key`（`web-<时间戳>-<uuid>`，见
  [src/api/client.ts](src/api/client.ts)）；本地请求头带 `X-Mock-User-Id: mock_user_001`。
- **错误友好**：解析后端统一错误信封 `{ error: { code, message, ... } }`；改词第 3 次的
  HTTP 409 会提示「改词次数已用完」。
- **状态完备**：所有按钮具备 loading / disabled / error 态。
- **面向普通用户的文案**：不出现「发布包 JSON」等技术词；成品交接写作「作品已准备好，可交给社区发布」。
- **响应式**：移动端 390px 与桌面 1440px 均不重叠、不溢出。

## 目录结构

```
src/
  api/         # 类型、HTTP 客户端、works 接口封装、状态派生与文案
  components/  # AppShell / Button / Field / Modal / Toast / Banner / StatusPill / Spinner
  hooks/       # useWorkDetail（轮询）、useAction（动作 + loading + 错误）
  mock/        # 演示模式内存后端（不联网）+ 本地媒体生成 + 服务路由
  pages/       # HomePage / WorkPage 及 work/ 下五个阶段子视图
  styles/      # tokens.css / base.css / app.css
  __tests__/   # 状态派生与改词限额（409）单测
```

## 测试

```bash
npm test          # vitest
npm run typecheck # tsc 类型检查
npm run build     # 构建产物
```
