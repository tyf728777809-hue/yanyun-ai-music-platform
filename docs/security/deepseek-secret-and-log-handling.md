# DeepSeek 凭据与日志处理规则

## 敏感信息范围

以下内容一律视为敏感信息：

- DeepSeek API Key。
- DeepSeek 鉴权 header、Bearer token、签名、临时 token。
- 私有 API base URL，如果供应商或公司要求不公开。
- 完整 Prompt、完整模型请求、完整模型响应。
- 用户输入中可能包含的个人信息、账号信息、联系方式或公司内部资料。
- Cookie、私钥、生产配置、公司用户 token。

## 禁止事项

- 不把真实凭据写入 `.env.example`、Markdown、测试、fixture、截图、提交信息或 issue。
- 不把 `Authorization`、API Key、token 或签名参数打印到日志。
- 不提交完整 Prompt、完整请求 payload、完整响应 payload。
- 不在自动化测试中调用真实 DeepSeek。
- 不把 DeepSeek 失败原文直接写入 `agent_runs.error_message`、作品失败信息或用户可见错误。
- 不在未设成本止损和回滚命令时打开真实调用。

## 允许记录的字段

- `work_id`。
- Agent 名称与版本。
- Operation：`INSPIRATION` / `LYRICS` / `POLISH` / `CONTINUE`。
- 模型名。
- Prompt 模板 key 与版本。
- 输入 hash、输出 hash。
- 脱敏后的错误码和短错误消息。
- 耗时、状态、是否成功、是否可重试。

## 代码侧保护

- `DEEPSEEK_REAL_CALLS_ENABLED=false` 时，真实 DeepSeek 客户端必须在外部 HTTP 请求前失败。
- 缺失 `DEEPSEEK_API_KEY`、模型名或 `AGENT_REAL_CALLS_ENABLED=true` 总开关时，必须在外部 HTTP 请求前失败。
- 真实客户端必须设置超时和最大尝试次数；默认不得无限重试。
- `agent_runs` 只记录 hash、模型名、模板版本、状态、耗时和脱敏失败信息，不记录完整 Prompt 或完整输出。
- 日志必须禁用请求/响应 body 原文输出。
- 错误文本入库前必须截断，并脱敏 `Authorization`、`Bearer`、`api_key`、`token`、`secret`、`password` 等字段。
- 自动化测试必须使用 fake HTTP 或 Mock client，不能依赖真实供应商可用性。

## 凭据注入与清理

只允许使用当前 shell、部署 Secret、CI Secret 或公司配置中心注入真实凭据：

```bash
export DEEPSEEK_BASE_URL="<from-secure-channel>"
export DEEPSEEK_API_KEY="<from-secure-channel>"
export AGENT_REAL_CALLS_ENABLED=true
export DEEPSEEK_REAL_CALLS_ENABLED=true
```

联调结束后：

```bash
unset DEEPSEEK_BASE_URL DEEPSEEK_API_KEY DEEPSEEK_MODEL_NAME
unset AGENT_REAL_CALLS_ENABLED DEEPSEEK_REAL_CALLS_ENABLED
```

提交前至少执行：

```bash
git diff --check
rg -l "api_key|apikey|authorization|bearer|token|secret|password|DEEPSEEK_API_KEY" . \
  -g '!build/**' -g '!apps/**/build/**' -g '!node_modules/**' -g '!prototypes/**/node_modules/**'
git status --short
```

如使用本机密钥扫描脚本，应只输出命中的文件名，不在终端粘贴真实密钥原文。
