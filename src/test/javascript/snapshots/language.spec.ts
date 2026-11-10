import { expect, test, type Page } from '@playwright/test';

// The page asks browsers to upgrade every request to HTTPS. The local server is plain HTTP.
async function allowPlainHttp(page: Page) {
  await page.route('**/*', async (route) => {
    if (route.request().resourceType() !== 'document') {
      await route.continue();
      return;
    }
    const response = await route.fetch();
    const headers = { ...response.headers() };
    delete headers['content-length'];
    const body = (await response.text())
      .replace(/upgrade-insecure-requests;?\s*/g, '')
      .replace("script-src 'self' 'unsafe-inline'", "script-src 'self' 'unsafe-inline' 'unsafe-eval'");
    await route.fulfill({ response, headers, body });
  });
}

test.describe('Chinese browser', () => {
  // A fresh context: no labkeeper_lang cookie and nothing in localStorage, as on the first visit
  test.use({ locale: 'zh-CN' });

  test('opens the home page in Chinese', async ({ page, context }) => {
    await allowPlainHttp(page);

    await page.goto('/');

    // lang.js switches the language and reloads the page once
    await expect(page.locator('html')).toHaveAttribute('lang', 'zh');
    await expect(page.locator('.hero__title')).toContainText('与 AI 一起');
    await expect(page.locator('.hero__label').nth(2)).toHaveText('图表');
    await expect(page.locator('#cookie-accept')).toHaveText('接受');
    await expect(page.locator('#cookie-banner')).toContainText('我们使用 Cookie');
    await expect(page.locator('.header__lang-btn--active').first()).toHaveAttribute('data-lang', 'zh');
    await expect(page.locator('body')).not.toContainText('Создавайте документы');

    const cookies = await context.cookies();
    expect(cookies.find((cookie) => cookie.name === 'labkeeper_lang')?.value).toBe('zh');
    expect(await page.evaluate(() => localStorage.getItem('labkeeper_lang'))).toBe('zh');
  });

  test('keeps the language the visitor picked', async ({ page }) => {
    await allowPlainHttp(page);
    await page.goto('/');
    await expect(page.locator('html')).toHaveAttribute('lang', 'zh');

    // Runs in desktop Chrome only (see playwright.config.ts), where the switcher sits in the header
    await Promise.all([
      page.waitForEvent('load'),
      page.locator('.header__lang .header__lang-btn[data-lang="ru"]').click(),
    ]);
    await expect(page.locator('html')).toHaveAttribute('lang', 'ru');

    // Next visit: the Chinese browser no longer overrides the choice
    await page.goto('/');
    await expect(page.locator('html')).toHaveAttribute('lang', 'ru');
    await expect(page.locator('.hero__title')).toContainText('Создавайте документы');
  });
});

test.describe('English browser', () => {
  test.use({ locale: 'en-US' });

  test('opens the home page in English', async ({ page, context }) => {
    await allowPlainHttp(page);

    await page.goto('/');

    await expect(page.locator('html')).toHaveAttribute('lang', 'en');
    await expect(page.locator('.hero__title')).toContainText('together with AI');
    await expect(page.locator('.hero__label').nth(2)).toHaveText('Diagrams');
    await expect(page.locator('#cookie-accept')).toHaveText('Accept');
    await expect(page.locator('.header__lang-btn--active').first()).toHaveAttribute('data-lang', 'en');
    await expect(page.locator('body')).not.toContainText('Создавайте документы');

    const cookies = await context.cookies();
    expect(cookies.find((cookie) => cookie.name === 'labkeeper_lang')?.value).toBe('en');
  });
});

test.describe('Search robot with an English browser', () => {
  test.use({
    locale: 'en-US',
    userAgent: 'Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)',
  });

  test('sees the Russian home page', async ({ page, context }) => {
    await allowPlainHttp(page);

    await page.goto('/');
    await page.waitForLoadState('load');

    // lang.js sets the cookie before it reloads, so no cookie means no switch
    const cookies = await context.cookies();
    expect(cookies.find((cookie) => cookie.name === 'labkeeper_lang')).toBeUndefined();
    await expect(page.locator('html')).toHaveAttribute('lang', 'ru');
    await expect(page.locator('.hero__title')).toContainText('Создавайте документы');
  });
});

test.describe('Russian browser', () => {
  test.use({ locale: 'ru-RU' });

  test('opens the home page in Russian', async ({ page }) => {
    await allowPlainHttp(page);

    await page.goto('/');

    await expect(page.locator('html')).toHaveAttribute('lang', 'ru');
    await expect(page.locator('.hero__title')).toContainText('Создавайте документы');
    await expect(page.locator('#cookie-accept')).toHaveText('Принять');
  });
});
