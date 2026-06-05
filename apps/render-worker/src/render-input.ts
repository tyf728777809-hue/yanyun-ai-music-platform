export type LyricLine = {
  readonly startFrame: number;
  readonly endFrame: number;
  readonly text: string;
};

export type LyricVideoProps = {
  readonly workId: string;
  readonly songTitle: string;
  readonly songSummary: string;
  readonly coverLabel: string;
  readonly lyrics: readonly LyricLine[];
};

export const sampleLyricVideoProps: LyricVideoProps = {
  workId: 'sample-work',
  songTitle: '燕云月下',
  songSummary: '一段关于边城月色、旧约与重逢的燕云歌。',
  coverLabel: 'YANYUN',
  lyrics: [
    {
      startFrame: 0,
      endFrame: 72,
      text: '风过边城，灯火照见旧誓',
    },
    {
      startFrame: 73,
      endFrame: 144,
      text: '月落长街，故人踏雪而来',
    },
    {
      startFrame: 145,
      endFrame: 239,
      text: '这一曲，把江湖唱给你听',
    },
  ],
};
