import { defineConfig, devices } from '@playwright/test';

const baseURL = process.env.FRONTEND_URL ?? 'http://host.docker.internal:8080';

const desktop = { width: 1440, height: 900 };

export default defineConfig({
  testDir: '.',
  fullyParallel: true,
  workers: 2,
  retries: 0,
  timeout: 60_000,
  reporter: 'list',
  snapshotPathTemplate: '{testDir}/__screenshots__/{projectName}/{arg}{ext}',
  expect: {
    toHaveScreenshot: {
      animations: 'disabled',
      caret: 'hide',
      maxDiffPixelRatio: 0.01,
    },
  },
  use: {
    baseURL,
    reducedMotion: 'reduce',
  },
  // The Playwright image ships Chromium, Firefox, and WebKit. These projects apply the device
  // profile of each platform; they are not native Windows, macOS, or phone installations.
  projects: [
    { name: 'win-chrome', use: { ...devices['Desktop Chrome'], viewport: desktop } },
    { name: 'win-edge', use: { ...devices['Desktop Edge'], viewport: desktop } },
    { name: 'macos-safari', use: { ...devices['Desktop Safari'], viewport: desktop } },
    { name: 'iphone-safari', use: { ...devices['iPhone 14'] } },
    { name: 'android-chrome', use: { ...devices['Pixel 7'] } },
    { name: 'win-firefox', use: { ...devices['Desktop Firefox'], viewport: desktop } },
  ],
});
