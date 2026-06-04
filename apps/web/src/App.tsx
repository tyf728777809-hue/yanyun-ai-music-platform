const statusItems = [
  '创作入口',
  '歌词确认',
  '生成状态',
  '成品预览',
  '发布包交接',
];

export function App() {
  return (
    <main className="app-shell" aria-label="燕云 AI 作曲平台">
      <section className="workspace">
        <p className="eyebrow">Local scaffold</p>
        <h1>燕云 AI 作曲平台</h1>
        <p className="summary">
          移动端优先、兼容 PC Web 的用户侧工程骨架。当前只验证前端构建能力，
          完整页面实现将通过 Gemini 前端任务包推进。
        </p>
        <ul className="status-list" aria-label="后续页面范围">
          {statusItems.map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ul>
      </section>
    </main>
  );
}
