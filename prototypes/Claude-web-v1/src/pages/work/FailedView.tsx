import { useState } from 'react';
import type { WorkViewProps } from './types';
import type { AvailableAction } from '../../api/types';
import { Button } from '../../components/Button';
import { Banner } from '../../components/Banner';
import { Modal } from '../../components/Modal';
import { useAction } from '../../hooks/useAction';
import { actionLabel } from '../../api/actions';
import { FAILURE_COPY, hasAction } from '../../api/workState';
import { service } from '../../mock/service';

// 失败页：展示友好失败原因 + 建议动作，按钮全部由 available_actions 驱动。
export function FailedView({ work, refresh, onBackToHome }: WorkViewProps) {
  const { run, busyKey } = useAction(refresh);
  const [supportOpen, setSupportOpen] = useState(false);
  const failure = work.failure;
  const copy = failure ? FAILURE_COPY[failure.failure_code] : null;

  const retryInfo =
    failure && failure.retry_limit != null
      ? `已重试 ${failure.retry_count ?? 0} / ${failure.retry_limit} 次`
      : null;

  async function handleRetryMusic() {
    await run('RETRY_MUSIC', () => service.retryMusic(work.work_id, {}), {
      successMsg: '已重新开始生成',
    });
  }

  async function handleRetryCover() {
    await run('RETRY_COVER', () => service.regenerateCover(work.work_id), {
      successMsg: '已重新生成封面',
    });
  }

  async function handleRerenderVideo() {
    await run('RERENDER_VIDEO', () => service.rerenderVideo(work.work_id), {
      successMsg: '已重新渲染画面',
    });
  }

  function handleReturnToEdit() {
    onBackToHome();
  }

  // 渲染除主重试外的次级动作。
  const secondaryActions = work.available_actions.filter(
    (a): a is AvailableAction => a !== 'RETRY_MUSIC',
  );

  return (
    <div className="work-stage failed-view">
      <header className="stage-head stage-head--center">
        <div className="fail-mark" aria-hidden="true">
          ⚠
        </div>
        <h1 className="stage-title">{copy?.title ?? '生成遇到了问题'}</h1>
        <p className="stage-lead">{copy?.hint ?? '可以重试，或返回编辑。'}</p>
      </header>

      {failure && (
        <Banner tone="danger" title="发生了什么">
          <span>{copy?.hint ?? failure.failure_message}</span>
          {failure.remaining_retry_count != null && (
            <span className="banner-line">剩余重试次数：{failure.remaining_retry_count}</span>
          )}
          {retryInfo && <span className="fail-retry banner-line">{retryInfo}</span>}
        </Banner>
      )}

      {failure?.recommended_action && (
        <p className="recommend-line">
          建议操作：<strong>{actionLabel(failure.recommended_action)}</strong>
        </p>
      )}

      <div className="action-bar action-bar--stack">
        {hasAction(work, 'RETRY_MUSIC') && (
          <Button
            tone="primary"
            size="lg"
            block
            loading={busyKey === 'RETRY_MUSIC'}
            disabled={busyKey !== null}
            onClick={handleRetryMusic}
          >
            重新生成
          </Button>
        )}

        {secondaryActions.map((action) => {
          if (action === 'RETURN_TO_EDIT') {
            return (
              <Button
                key={action}
                tone="secondary"
                block
                disabled={busyKey !== null}
                onClick={handleReturnToEdit}
              >
                返回重新创作
              </Button>
            );
          }
          if (action === 'RETRY_LYRICS') {
            return (
              <Button
                key={action}
                tone="secondary"
                block
                loading={busyKey === action}
                disabled={busyKey !== null}
                onClick={() => void refresh()}
              >
                {actionLabel(action)}
              </Button>
            );
          }
          if (action === 'RETRY_COVER') {
            return (
              <Button
                key={action}
                tone="secondary"
                block
                loading={busyKey === action}
                disabled={busyKey !== null}
                onClick={handleRetryCover}
              >
                {actionLabel(action)}
              </Button>
            );
          }
          if (action === 'RERENDER_VIDEO') {
            return (
              <Button
                key={action}
                tone="secondary"
                block
                loading={busyKey === action}
                disabled={busyKey !== null}
                onClick={handleRerenderVideo}
              >
                {actionLabel(action)}
              </Button>
            );
          }
          if (action === 'CONTACT_SUPPORT') {
            return (
              <Button
                key={action}
                tone="ghost"
                block
                onClick={() => setSupportOpen(true)}
                disabled={busyKey !== null}
              >
                联系平台协助
              </Button>
            );
          }
          return null;
        })}

        <button className="textlink" onClick={onBackToHome}>
          返回创作首页
        </button>
      </div>
      <Modal
        open={supportOpen}
        title="联系平台协助"
        subtitle="把这些信息发给平台或开发同事，方便定位问题。"
        onClose={() => setSupportOpen(false)}
        footer={
          <Button tone="primary" onClick={() => setSupportOpen(false)}>
            我知道了
          </Button>
        }
      >
        <div className="support-panel">
          <p>作品编号：{work.work_code}</p>
          <p>作品 ID：{work.work_id}</p>
          {failure && <p>失败原因：{failure.failure_code}</p>}
          {failure?.failure_message && <p>提示：{failure.failure_message}</p>}
        </div>
      </Modal>
    </div>
  );
}
