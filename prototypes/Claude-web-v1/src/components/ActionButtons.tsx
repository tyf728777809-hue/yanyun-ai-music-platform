import { ACTION_META } from '../api/actions';
import type { AvailableAction } from '../api/types';

interface ActionButtonsProps {
  actions: AvailableAction[];
  loadingAction: AvailableAction | null;
  onAction: (action: AvailableAction) => void;
  disabled?: boolean;
  className?: string;
}

export function ActionButtons({
  actions,
  loadingAction,
  onAction,
  disabled = false,
  className,
}: ActionButtonsProps) {
  if (!actions.length) return null;

  return (
    <div className={['action-row', className].filter(Boolean).join(' ')}>
      {actions.map((action) => {
        const meta = ACTION_META[action];
        const isLoading = loadingAction === action;
        return (
          <button
            key={action}
            className={`btn btn--${meta.tone}`}
            type="button"
            disabled={disabled || loadingAction !== null}
            aria-busy={isLoading}
            onClick={() => onAction(action)}
          >
            {isLoading ? '处理中…' : meta.label}
          </button>
        );
      })}
    </div>
  );
}
