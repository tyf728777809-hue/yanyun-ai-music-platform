import type { WorkDetail } from '../../api/types';

// 所有作品子视图共享的 props。
export interface WorkViewProps {
  work: WorkDetail;
  refresh: () => Promise<void>;
  onBackToHome: () => void;
}
