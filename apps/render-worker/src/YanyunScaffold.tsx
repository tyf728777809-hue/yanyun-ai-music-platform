import {AbsoluteFill} from 'remotion';

export function YanyunScaffold() {
  return (
    <AbsoluteFill
      style={{
        alignItems: 'center',
        backgroundColor: '#101914',
        color: '#f2f7f3',
        display: 'flex',
        fontFamily: 'system-ui, sans-serif',
        justifyContent: 'center',
      }}
    >
      <div style={{fontSize: 72, letterSpacing: 0}}>燕云 AI 作曲平台</div>
    </AbsoluteFill>
  );
}
