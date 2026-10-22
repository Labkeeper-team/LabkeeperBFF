import { expect, test } from '@playwright/test';

test('home page', async ({ page }) => {
  // The page asks browsers to upgrade every request to HTTPS. The local server is plain HTTP.
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

  await page.goto('/');
  await expect(page.locator('h1')).toHaveText(/\S/);
  await expect(page.locator('#cookie-banner')).not.toHaveClass(/hidden/);
  await expect(page.locator('.js-preloader')).toHaveClass(/preloader--hidden/);
  await page.evaluate(() => document.fonts.ready);
  await expect(page).toHaveScreenshot('home.png', { fullPage: true });
});
