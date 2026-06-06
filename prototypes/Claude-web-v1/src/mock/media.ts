// 纯本地生成的演示媒体资源，不发起任何网络请求，也不调用真实模型。
// 仅用于内置「演示模式」，让封面、音频在原型里可见可听。

// 燕云风味的水墨远山封面（内联 SVG → data URI）。
export function makeCoverDataUri(seed: string): string {
  const hues = hashHues(seed);
  const svg = `
<svg xmlns="http://www.w3.org/2000/svg" width="640" height="640" viewBox="0 0 640 640">
  <defs>
    <linearGradient id="sky" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="hsl(${hues.a},38%,16%)"/>
      <stop offset="0.55" stop-color="hsl(${hues.b},34%,24%)"/>
      <stop offset="1" stop-color="hsl(${hues.c},30%,34%)"/>
    </linearGradient>
    <radialGradient id="moon" cx="0.74" cy="0.26" r="0.18">
      <stop offset="0" stop-color="#f4ecd6" stop-opacity="0.95"/>
      <stop offset="1" stop-color="#f4ecd6" stop-opacity="0"/>
    </radialGradient>
  </defs>
  <rect width="640" height="640" fill="url(#sky)"/>
  <circle cx="474" cy="166" r="58" fill="#f4ecd6" opacity="0.92"/>
  <rect width="640" height="640" fill="url(#moon)"/>
  <g opacity="0.30" fill="#0d130f">
    <path d="M0 360 L120 286 L228 350 L330 268 L452 356 L548 300 L640 352 L640 640 L0 640 Z"/>
  </g>
  <g opacity="0.55" fill="#0a0f0c">
    <path d="M0 452 L96 392 L200 452 L300 388 L408 458 L520 404 L640 456 L640 640 L0 640 Z"/>
  </g>
  <g opacity="0.85" fill="#070b08">
    <path d="M0 540 L130 494 L250 544 L372 500 L500 548 L640 512 L640 640 L0 640 Z"/>
  </g>
  <g stroke="#f4ecd6" stroke-opacity="0.5" stroke-width="2" fill="none">
    <path d="M150 232 q40 -28 86 -8"/>
    <path d="M360 206 q44 -30 96 -6"/>
  </g>
</svg>`.trim();
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
}

function hashHues(seed: string): { a: number; b: number; c: number } {
  let h = 0;
  for (let i = 0; i < seed.length; i += 1) {
    h = (h * 31 + seed.charCodeAt(i)) >>> 0;
  }
  const base = 150 + (h % 40); // 偏青绿色系，贴合燕云山水。
  return { a: base, b: (base + 14) % 360, c: (base + 28) % 360 };
}

// 生成一小段温柔的五声音阶琶音（WAV data URI），让演示里的播放器可发声。
export function makeToneWavDataUri(seed: string): string {
  const sampleRate = 11025;
  const seconds = 3;
  const total = sampleRate * seconds;
  // 宫调五声音阶（C D E G A），随种子换个起调。
  const pentatonic = [261.63, 293.66, 329.63, 392.0, 440.0, 523.25];
  let s = 0;
  for (let i = 0; i < seed.length; i += 1) s += seed.charCodeAt(i);
  const data = new Int16Array(total);
  const noteLen = Math.floor(sampleRate * 0.5);
  for (let i = 0; i < total; i += 1) {
    const noteIndex = Math.floor(i / noteLen);
    const freq = pentatonic[(noteIndex + s) % pentatonic.length];
    const tInNote = (i % noteLen) / sampleRate;
    // 简单的拨弦包络：快起慢落。
    const env = Math.exp(-4 * tInNote);
    const sample =
      Math.sin(2 * Math.PI * freq * (i / sampleRate)) * 0.5 * env +
      Math.sin(2 * Math.PI * freq * 2 * (i / sampleRate)) * 0.15 * env;
    data[i] = Math.max(-1, Math.min(1, sample)) * 32767;
  }
  return encodeWav(data, sampleRate);
}

function encodeWav(samples: Int16Array, sampleRate: number): string {
  const bytesPerSample = 2;
  const blockAlign = bytesPerSample;
  const dataSize = samples.length * bytesPerSample;
  const buffer = new ArrayBuffer(44 + dataSize);
  const view = new DataView(buffer);
  const writeStr = (offset: number, str: string) => {
    for (let i = 0; i < str.length; i += 1) view.setUint8(offset + i, str.charCodeAt(i));
  };
  writeStr(0, 'RIFF');
  view.setUint32(4, 36 + dataSize, true);
  writeStr(8, 'WAVE');
  writeStr(12, 'fmt ');
  view.setUint32(16, 16, true);
  view.setUint16(20, 1, true); // PCM
  view.setUint16(22, 1, true); // mono
  view.setUint32(24, sampleRate, true);
  view.setUint32(28, sampleRate * blockAlign, true);
  view.setUint16(32, blockAlign, true);
  view.setUint16(34, 8 * bytesPerSample, true);
  writeStr(36, 'data');
  view.setUint32(40, dataSize, true);
  for (let i = 0; i < samples.length; i += 1) {
    view.setInt16(44 + i * bytesPerSample, samples[i], true);
  }
  let binary = '';
  const bytes = new Uint8Array(buffer);
  for (let i = 0; i < bytes.length; i += 1) binary += String.fromCharCode(bytes[i]);
  return `data:audio/wav;base64,${btoa(binary)}`;
}
