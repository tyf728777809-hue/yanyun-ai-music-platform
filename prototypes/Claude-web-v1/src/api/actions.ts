import type { AvailableAction } from './types';

// available_actions 驱动按钮渲染。这里只定义“某动作长什么样”，
// 是否出现完全由后端返回的列表决定。
export interface ActionMeta {
  label: string;
  // primary 动作在 UI 中作为主按钮高亮；其余作为次按钮。
  tone: 'primary' | 'secondary' | 'ghost';
}

export const ACTION_META: Record<AvailableAction, ActionMeta> = {
  POLISH_LYRICS: { label: 'AI 润色', tone: 'secondary' },
  CONTINUE_LYRICS: { label: 'AI 续写', tone: 'secondary' },
  CONFIRM_WORK: { label: '确认出歌', tone: 'primary' },
  RETRY_LYRICS: { label: '重新生成歌词', tone: 'primary' },
  RETRY_MUSIC: { label: '重新生成', tone: 'primary' },
  RETRY_COVER: { label: '重新生成封面', tone: 'secondary' },
  RERENDER_VIDEO: { label: '重新渲染画面', tone: 'secondary' },
  REFRESH_PACKAGE_URL: { label: '刷新下载链接', tone: 'secondary' },
  MARK_PACKAGE_FETCHED: { label: '标记已交接', tone: 'primary' },
  RETURN_TO_EDIT: { label: '返回编辑', tone: 'ghost' },
  CONTACT_SUPPORT: { label: '联系平台协助', tone: 'ghost' },
};

export function actionLabel(action: AvailableAction): string {
  return ACTION_META[action]?.label ?? action;
}
