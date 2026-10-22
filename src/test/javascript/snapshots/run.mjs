import { spawn } from 'node:child_process';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const requested = process.env.FRONTEND_URL ?? 'http://localhost:8080';
const frontendUrl = requested.replace(/\/\/(localhost|127\.0\.0\.1)(?=[:/]|$)/, '//host.docker.internal');
const image = 'mcr.microsoft.com/playwright:v1.50.0-jammy';
const mount = root.replace(/\\/g, '/');

const response = await fetch(requested, { signal: AbortSignal.timeout(5000) }).catch(() => null);
if (!response?.ok) {
  console.error(`Frontend is not reachable at ${requested}. Start it first, then run this script.`);
  process.exit(1);
}

const docker = spawn('docker', [
  'run', '--rm',
  '--add-host', 'host.docker.internal:host-gateway',
  '-e', `FRONTEND_URL=${frontendUrl}`,
  '-e', 'PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1',
  '-v', `${mount}:/work`,
  '-v', '/work/node_modules',
  '-w', '/work',
  image,
  'bash', '-lc', 'npm ci && npx playwright test --config=snapshots/playwright.config.ts "$@"',
  'bash',
  ...process.argv.slice(2),
], { stdio: 'inherit' });

docker.on('exit', (code) => process.exit(code ?? 1));
