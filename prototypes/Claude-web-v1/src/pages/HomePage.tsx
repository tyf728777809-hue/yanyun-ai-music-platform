import { useState } from 'react';
import { Button } from '../components/Button';
import { ChoiceField, TextAreaField, TextField } from '../components/Field';
import { Banner } from '../components/Banner';
import { useToast } from '../components/Toast';
import { service } from '../mock/service';
import { ApiError } from '../api/client';
import { requestIdLine } from '../api/friendlyError';

type Mode = 'inspiration' | 'lyrics';

const VOCAL_OPTIONS = [
  { value: 'AUTO', label: '智能匹配' },
  { value: 'MALE_LEAD', label: '男声主唱' },
  { value: 'FEMALE_LEAD', label: '女声主唱' },
  { value: 'DUET', label: '双人对唱' },
  { value: 'CHORUS_ACCENT', label: '副歌和声点缀' },
];

const STYLE_PRESETS = [
  '流行抒情',
  'R&B',
  '摇滚',
  '民谣叙事',
  '说唱',
  '电子',
  '影视OST',
  '史诗战歌',
  'City Pop',
  '国风融合',
];

interface HomePageProps {
  onWorkCreated: (workId: string) => void;
  onOpenWorks: () => void;
}

export function HomePage({ onWorkCreated, onOpenWorks }: HomePageProps) {
  const toast = useToast();
  const [mode, setMode] = useState<Mode>('inspiration');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<{ message: string; requestId?: string | null } | null>(null);

  // 灵感成歌字段
  const [story, setStory] = useState('');
  const [mood, setMood] = useState('');

  // 填词成歌字段
  const [lyrics, setLyrics] = useState('');
  const [songTitle, setSongTitle] = useState('');

  // 共用字段
  const [musicStyle, setMusicStyle] = useState('');
  const [vocal, setVocal] = useState('AUTO');

  const canSubmit =
    mode === 'inspiration' ? story.trim().length > 0 : lyrics.trim().length > 0;

  async function handleSubmit() {
    if (!canSubmit || submitting) return;
    setSubmitting(true);
    setError(null);
    try {
      const created =
        mode === 'inspiration'
          ? await service.createFromInspiration({
              story_input: story.trim(),
              mood: mood.trim() || undefined,
              music_style: musicStyle.trim() || undefined,
              vocal_preference: vocal,
            })
          : await service.createFromLyrics({
              lyrics_input: lyrics.trim(),
              song_title: songTitle.trim() || undefined,
              music_style: musicStyle.trim() || undefined,
              vocal_preference: vocal,
            });
      toast.success('已开始创作歌词');
      onWorkCreated(created.work_id);
    } catch (err) {
      const msg = err instanceof ApiError ? err.message : '创建失败，请稍后重试';
      setError({ message: msg, requestId: requestIdLine(err) });
      toast.error('创建失败');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="home">
      <section className="hero">
        <p className="hero__eyebrow">燕云十六声 · 听见你的故事</p>
        <h1 className="hero__title">把心里的故事，谱成一首歌</h1>
        <p className="hero__lead">
          写下一段灵感，或直接填入你的词。剩下的交给燕云乐坊，山河与故人都会在旋律里。
        </p>
        <button className="textlink hero__works-link" onClick={onOpenWorks}>
          查看我的作品
        </button>
      </section>

      <div className="segmented" aria-label="创作方式">
        <button
          aria-pressed={mode === 'inspiration'}
          className={`segmented__tab ${mode === 'inspiration' ? 'is-active' : ''}`}
          onClick={() => setMode('inspiration')}
        >
          <span className="segmented__icon" aria-hidden="true">✦</span>
          <span className="segmented__label">灵感成歌</span>
          <span className="segmented__desc">说说故事，AI 写词谱曲</span>
        </button>
        <button
          aria-pressed={mode === 'lyrics'}
          className={`segmented__tab ${mode === 'lyrics' ? 'is-active' : ''}`}
          onClick={() => setMode('lyrics')}
        >
          <span className="segmented__icon" aria-hidden="true">✎</span>
          <span className="segmented__label">填词成歌</span>
          <span className="segmented__desc">已有词，直接谱成曲</span>
        </button>
      </div>

      <div className="card creation-card">
        {mode === 'inspiration' ? (
          <div className="form-stack">
            <TextAreaField
              label="你的灵感"
              hint="把人物、地点、关系、画面都直接写在这里就好"
              placeholder="例如：我想写一个燕云里的普通游侠，夜里回到清河，发现旧友还守着当年的酒摊……"
              rows={5}
              maxLength={3000}
              value={story}
              onChange={(e) => setStory(e.target.value)}
            />
            <TextField
              label="心情"
              optional
              placeholder="如 释然、思念、热血、孤独"
              maxLength={128}
              value={mood}
              onChange={(e) => setMood(e.target.value)}
            />
          </div>
        ) : (
          <div className="form-stack">
            <TextField
              label="歌曲标题"
              optional
              placeholder="不填则由 AI 拟定"
              maxLength={128}
              value={songTitle}
              onChange={(e) => setSongTitle(e.target.value)}
            />
            <TextAreaField
              label="你的歌词"
              hint="可分段落，支持 [主歌] [副歌] 等标记"
              placeholder={'[主歌]\n提灯夜行过远山，风过檐角铃声轻\n……'}
              rows={8}
              maxLength={5000}
              value={lyrics}
              onChange={(e) => setLyrics(e.target.value)}
            />
          </div>
        )}

        <div className="form-divider" />

        <div className="form-stack">
          <div className="field">
            <span className="field__label">
              曲风<span className="field__optional">选填</span>
            </span>
            <div className="chips">
              {STYLE_PRESETS.map((preset) => (
                <button
                  key={preset}
                  type="button"
                  className={`chip ${musicStyle === preset ? 'is-selected' : ''}`}
                  onClick={() => setMusicStyle(musicStyle === preset ? '' : preset)}
                >
                  {preset}
                </button>
              ))}
            </div>
            <input
              className="field__input"
              placeholder="可自由填写喜欢的曲风"
              maxLength={512}
              value={musicStyle}
              onChange={(e) => setMusicStyle(e.target.value)}
            />
          </div>

          <ChoiceField
            label="声线偏好"
            optional
            options={VOCAL_OPTIONS}
            value={vocal}
            onChange={setVocal}
          />
        </div>

        {error && (
          <Banner tone="danger" title="没能开始创作">
            <span>{error.message}</span>
            {error.requestId && <span className="request-id">{error.requestId}</span>}
          </Banner>
        )}

        <Button
          tone="primary"
          size="lg"
          block
          loading={submitting}
          disabled={!canSubmit}
          onClick={handleSubmit}
        >
          {mode === 'inspiration' ? '生成歌词' : '开始谱曲'}
        </Button>
        {!canSubmit && (
          <p className="form-foot-hint">
            {mode === 'inspiration' ? '先写下一点灵感吧' : '先填入你的歌词吧'}
          </p>
        )}
      </div>
    </div>
  );
}
