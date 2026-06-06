import type { ReactNode } from 'react';

type BannerTone = 'info' | 'danger' | 'gold' | 'success';

interface BannerProps {
  tone?: BannerTone;
  title?: string;
  children: ReactNode;
  action?: ReactNode;
}

// 信息条：用于错误、限额、引导等可读提示。
export function Banner({ tone = 'info', title, children, action }: BannerProps) {
  return (
    <div className={`banner banner--${tone}`} role={tone === 'danger' ? 'alert' : 'status'}>
      <span className="banner__rail" aria-hidden="true" />
      <div className="banner__body">
        {title && <p className="banner__title">{title}</p>}
        <div className="banner__text">{children}</div>
      </div>
      {action && <div className="banner__action">{action}</div>}
    </div>
  );
}
