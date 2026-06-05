# DreamMaker 凭据与日志处理规则

## 敏感信息范围

以下内容一律视为敏感信息：

- DreamMaker AccessKey。
- DreamMaker SecretKey。
- DreamMaker JWT。
- `DREAMMAKER_USER_ACCESS_TOKEN` / `X-Access-Token`。
- 供应商返回的带签名下载 URL。
- Cookie、私钥、生产配置、公司用户 token。

## 禁止事项

- 不把真实凭据写入 `.env.example`、Markdown、测试、fixture、截图、提交信息或 issue。
- 不把 `Authorization` / `X-Access-Token` 打印到日志。
- 不提交供应商完整请求或完整响应 payload。
- 不在自动化测试中调用真实 DreamMaker。
- 不在默认 `sync` 模式下跑真实 Suno/MiniMax。

## 允许记录的字段

- `work_id`。
- `job_id`。
- Provider：`SUNO` / `MINIMAX`。
- DreamMaker task id。
- 脱敏后的错误码和短错误消息。
- 作品状态、生成阶段、发布包状态。
- 平台对象存储 key 或平台 URL。

## 代码侧保护

- `DREAMMAKER_REAL_CALLS_ENABLED=false` 时，`DreamMakerHttpClient` 必须在外部 HTTP 请求前失败。
- 缺失 `DREAMMAKER_ACCESS_KEY` / `DREAMMAKER_SECRET_KEY` 时，必须在外部 HTTP 请求前失败。
- `provider_calls.error_message` 入库前必须截断并脱敏 Bearer token、JWT、`access_key`、`secret_key`、`token` 等字段。
- 作品失败信息也必须使用脱敏后的短文本。

## 联调后清理

```bash
unset DREAMMAKER_ACCESS_KEY DREAMMAKER_SECRET_KEY DREAMMAKER_USER_ACCESS_TOKEN
export DREAMMAKER_REAL_CALLS_ENABLED=false
```

提交前执行：

```bash
python3 /Users/tongyifeng/.codex/skills/env-secrets-manager/scripts/env_auditor.py /Users/tongyifeng/Desktop/ai项目/燕云乐坊
git diff --check
git status --short
```
