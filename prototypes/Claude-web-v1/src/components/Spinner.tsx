// 旋转的水墨环：用于全屏 / 区块加载态。
export function Spinner({ size = 28, label }: { size?: number; label?: string }) {
  return (
    <span className="spinner-wrap">
      <span
        className="spinner"
        style={{ width: size, height: size }}
        role="progressbar"
        aria-label={label ?? '加载中'}
      />
      {label && <span className="spinner-label">{label}</span>}
    </span>
  );
}
