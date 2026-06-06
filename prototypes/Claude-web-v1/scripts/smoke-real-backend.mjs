#!/usr/bin/env node

import { spawn } from 'node:child_process';
import { randomUUID } from 'node:crypto';
import { setTimeout as delay } from 'node:timers/promises';
import { chromium } from 'playwright';

const apiOrigin = process.env.API_ORIGIN ?? 'http://localhost:8080';
const frontendHost = process.env.FRONTEND_HOST ?? '127.0.0.1';
const frontendPort = Number(process.env.FRONTEND_PORT ?? 5274);
const frontendUrl = `http://${frontendHost}:${frontendPort}`;
const mockUserId = process.env.MOCK_USER_ID ?? 'mock_user_001';
const headless = process.env.HEADLESS !== 'false';
const timeoutMs = Number(process.env.SMOKE_TIMEOUT_MS ?? 20_000);

const observedHttpErrors = [];
const observedConsoleErrors = [];

function log(message) {
  console.log(`[ui-smoke] ${message}`);
}

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

function idempotencyKey(scope) {
  return `ui-smoke-${scope}-${Date.now()}-${randomUUID()}`;
}

async function apiRequest(path, options = {}) {
  const headers = {
    Accept: 'application/json',
    'X-Mock-User-Id': mockUserId,
    ...options.headers,
  };
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json';
    headers['Idempotency-Key'] = options.idempotencyKey ?? idempotencyKey('api');
  }
  const response = await fetch(`${apiOrigin}${path}`, {
    method: options.method ?? (options.body === undefined ? 'GET' : 'POST'),
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  });
  const text = await response.text();
  const data = text ? JSON.parse(text) : null;
  return { response, data };
}

async function waitForHttpOk(url, label) {
  const deadline = Date.now() + timeoutMs;
  let lastError;
  while (Date.now() < deadline) {
    try {
      const response = await fetch(url);
      if (response.ok) return;
      lastError = new Error(`${label} returned HTTP ${response.status}`);
    } catch (error) {
      lastError = error;
    }
    await delay(500);
  }
  throw new Error(`${label} did not become ready: ${lastError?.message ?? 'timeout'}`);
}

function startFrontend() {
  const viteBin =
    process.platform === 'win32' ? 'node_modules/.bin/vite.cmd' : 'node_modules/.bin/vite';
  const child = spawn(viteBin, ['--host', frontendHost, '--port', String(frontendPort), '--strictPort'], {
    cwd: process.cwd(),
    env: {
      ...process.env,
      VITE_API_PROXY_TARGET: apiOrigin,
    },
    stdio: ['ignore', 'pipe', 'pipe'],
  });

  child.stdout.on('data', (chunk) => {
    const line = chunk.toString().trim();
    if (line) log(`vite ${line}`);
  });
  child.stderr.on('data', (chunk) => {
    const line = chunk.toString().trim();
    if (line) log(`vite ${line}`);
  });

  return child;
}

async function stopProcess(child) {
  if (!child || child.killed) return;
  child.kill('SIGINT');
  await Promise.race([
    new Promise((resolve) => child.once('exit', resolve)),
    delay(3_000).then(() => {
      if (!child.killed) child.kill('SIGTERM');
    }),
  ]);
}

async function expectNoHorizontalOverflow(page, label) {
  const result = await page.evaluate(() => ({
    scrollWidth: document.documentElement.scrollWidth,
    viewportWidth: window.innerWidth,
  }));
  assert(
    result.scrollWidth <= result.viewportWidth + 1,
    `${label} has horizontal overflow: scrollWidth=${result.scrollWidth}, viewport=${result.viewportWidth}`,
  );
}

async function waitForVisibleText(page, textOrRegex, label) {
  await page.getByText(textOrRegex).first().waitFor({ state: 'visible', timeout: timeoutMs });
  log(`verified ${label}`);
}

async function runMainFlow(page) {
  log('running real-backend creation flow');
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto(frontendUrl, { waitUntil: 'networkidle' });
  await waitForVisibleText(page, '本地服务', 'local service mode');
  await expectNoHorizontalOverflow(page, 'mobile home');

  await page
    .getByRole('textbox', { name: /你的灵感/ })
    .fill('雁门关外风雪夜，少年与旧友重逢，想把江湖往事唱成一首温柔但有边塞鼓点的歌。');
  await page.getByRole('button', { name: '生成歌词' }).click();
  await page.waitForURL(/#\/work\/[0-9a-f-]+/, { timeout: timeoutMs });
  const workId = page.url().split('/work/')[1];
  assert(workId, 'work id was not present in route');
  await waitForVisibleText(page, '歌词待确认', 'lyrics confirmation page');
  await waitForVisibleText(page, '燕云意象', 'yanyun references');

  await page.getByRole('button', { name: 'AI 润色' }).click();
  await page.getByRole('button', { name: '开始润色' }).waitFor({ state: 'visible', timeout: timeoutMs });
  assert(await page.getByRole('button', { name: '开始润色' }).isDisabled(), 'empty polish submit should be disabled');
  await page.getByRole('textbox', { name: /想让 AI/ }).fill('让歌词更有边塞画面感，副歌更押韵。');
  await page.getByRole('button', { name: '开始润色' }).click();
  await waitForVisibleText(page, '第 2 版', 'polished draft');

  await page.getByRole('button', { name: 'AI 续写' }).click();
  await page.getByRole('button', { name: '开始续写' }).click();
  await waitForVisibleText(page, '第 3 版', 'continued draft');

  await page.getByRole('button', { name: 'AI 润色' }).click();
  await page.getByRole('textbox', { name: /想让 AI/ }).fill('再更热血一点。');
  await page.getByRole('button', { name: '开始润色' }).click();
  await page.getByRole('alert').waitFor({ state: 'visible', timeout: timeoutMs });
  const conflictText = await page.getByRole('alert').innerText();
  assert(conflictText.includes('改词次数已用完'), `quota conflict copy missing: ${conflictText}`);
  assert(conflictText.includes('请求编号：'), `quota conflict request id missing: ${conflictText}`);
  await page.getByRole('button', { name: '取消' }).click();

  await page.getByRole('button', { name: '确认出歌' }).click();
  await waitForVisibleText(page, '交给社区发布', 'finished handoff page');
  await waitForVisibleText(page, '交接下载链接', 'package URL');
  await waitForVisibleText(page, '视频地址', 'video URL');
  await waitForVisibleText(page, '歌词正文', 'lyrics text');

  await page.getByRole('button', { name: '刷新下载链接' }).click();
  await waitForVisibleText(page, '交接下载链接', 'refreshed package URL');
  await page.getByRole('button', { name: '标记已交接' }).click();
  await waitForVisibleText(page, '作品已交接给社区发布流程。', 'fetched package status');
  await page.getByRole('button', { name: '标记已交接' }).waitFor({ state: 'hidden', timeout: timeoutMs });

  await page.getByRole('button', { name: '我的作品' }).click();
  await waitForVisibleText(page, '创作记录', 'works list');
  await waitForVisibleText(page, 'Yanyun Mock Song', 'latest work in list');
  await expectNoHorizontalOverflow(page, 'mobile works list');

  await page.setViewportSize({ width: 1440, height: 900 });
  await expectNoHorizontalOverflow(page, 'desktop works list');

  const detail = await apiRequest(`/api/v1/works/${workId}`);
  assert(detail.data.status === 'GENERATED', `expected generated work, got ${detail.data.status}`);
  assert(detail.data.package_status === 'PACKAGE_FETCHED', `expected PACKAGE_FETCHED, got ${detail.data.package_status}`);
  log(`main flow PASS work_id=${workId}`);
}

async function createFailedWork() {
  const created = await apiRequest('/api/v1/works/lyrics', {
    body: {
      song_title: '失败重试 UI Smoke',
      lyrics_input: '雁门风起，试一次失败再重试',
      style_tags: ['smoke'],
      yanyun_reference: '雁门关',
    },
    idempotencyKey: idempotencyKey('failure-lyrics'),
  });
  assert(created.response.ok, `failed to create failure-smoke work: HTTP ${created.response.status}`);
  const workId = created.data.work_id;

  const confirmed = await apiRequest(`/api/v1/works/${workId}/confirm`, {
    body: { music_provider: 'suno' },
    idempotencyKey: idempotencyKey('failure-confirm'),
  });
  assert(confirmed.response.status === 409, `expected suno guarded confirm to return 409, got ${confirmed.response.status}`);

  const detail = await apiRequest(`/api/v1/works/${workId}`);
  assert(detail.data.status === 'FAILED', `expected failed work, got ${detail.data.status}`);
  assert(detail.data.available_actions.includes('RETRY_MUSIC'), 'failed work is missing RETRY_MUSIC');
  return workId;
}

async function runFailureRetryFlow(page) {
  log('running failure retry UI flow');
  const workId = await createFailedWork();

  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto(`${frontendUrl}/#/work/${workId}`, { waitUntil: 'networkidle' });
  await waitForVisibleText(page, '旋律生成遇到了问题', 'failure page title');
  await waitForVisibleText(page, '失败码：MUSIC_GENERATION_FAILED', 'failure code');
  await waitForVisibleText(page, '剩余重试次数：2', 'remaining retry count');

  await page.getByRole('button', { name: '重新生成' }).click();
  await waitForVisibleText(page, '交给社区发布', 'retry recovered finished page');
  const detail = await apiRequest(`/api/v1/works/${workId}`);
  assert(detail.data.status === 'GENERATED', `expected retry to recover generated work, got ${detail.data.status}`);
  assert(detail.data.package_status === 'PACKAGE_READY', `expected retry PACKAGE_READY, got ${detail.data.package_status}`);
  log(`failure retry flow PASS work_id=${workId}`);
}

function unexpectedHttpErrors() {
  return observedHttpErrors.filter((item) => {
    if (item.status === 409 && item.url.includes('/lyrics/polish')) return false;
    return true;
  });
}

async function main() {
  await waitForHttpOk(`${apiOrigin}/health`, 'music-api health');
  const frontend = startFrontend();
  let browser;
  try {
    await waitForHttpOk(frontendUrl, 'frontend dev server');
    browser = await chromium.launch({ headless });
    const context = await browser.newContext();
    const page = await context.newPage();
    page.on('response', (response) => {
      if (response.status() >= 400) {
        observedHttpErrors.push({ status: response.status(), url: response.url() });
      }
    });
    page.on('console', (message) => {
      if (message.type() === 'error') {
        observedConsoleErrors.push(message.text());
      }
    });

    await runMainFlow(page);
    await runFailureRetryFlow(page);

    const httpErrors = unexpectedHttpErrors();
    assert(httpErrors.length === 0, `unexpected HTTP errors: ${JSON.stringify(httpErrors, null, 2)}`);
    const consoleErrors = observedConsoleErrors.filter((message) => !message.includes('409'));
    assert(consoleErrors.length === 0, `unexpected console errors: ${JSON.stringify(consoleErrors, null, 2)}`);
    log('PASS real backend UI smoke');
  } finally {
    if (browser) await browser.close();
    await stopProcess(frontend);
  }
}

main().catch(async (error) => {
  console.error(`[ui-smoke] ERROR: ${error.stack ?? error.message}`);
  process.exitCode = 1;
});
