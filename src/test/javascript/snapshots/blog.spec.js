import { readdirSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test, expect } from '@playwright/test';
import { parse } from 'yaml';
import { buildExampleUrl } from '../unit/example-url.js';

const articlesDir = process.env.BLOG_ARTICLES_DIR
    ?? join(dirname(fileURLToPath(import.meta.url)), '../../../main/resources/blog/articles');

function loadArticles() {
    const articles = [];
    for (const file of readdirSync(articlesDir, { recursive: true })) {
        if (!String(file).endsWith('.yaml')) continue;
        const data = parse(readFileSync(join(articlesDir, file), 'utf8'));
        articles.push(...(Array.isArray(data) ? data : [data]));
    }
    return articles;
}

const articles = loadArticles();

test.beforeEach(async ({ page }) => {
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
});

async function setLanguage(page, language) {
    const burger = page.locator('.js-burger-btn');
    if (
        (await burger.isVisible()) &&
        !(await page.locator('.js-nav').evaluate((nav) => nav.classList.contains('header__nav--open')))
    ) {
        await burger.click();
    }
    await page.locator(`.js-lang-switcher [data-lang="${language}"]:visible`).click();
}

async function expectExampleLabel(link, language) {
    const label = language === 'en' ? 'View example' : 'Просмотреть пример';
    const hint = language === 'en' ? 'in editor' : 'в редакторе';
    await expect(link.locator('[data-i18n="blog-example"]')).toHaveText(label);
    await expect(link.locator('[data-i18n="blog-example-context"]')).toHaveText(hint);
    if (!(await link.isVisible())) return;
    expect(
        await link.evaluate((element) => {
            const bounds = element.getBoundingClientRect();
            return [...element.querySelectorAll('.article-example__label > span, .button__arrow')].every((child) => {
                const rect = child.getBoundingClientRect();
                return (
                    rect.left >= bounds.left &&
                    rect.right <= bounds.right + 1 &&
                    rect.top >= bounds.top &&
                    rect.bottom <= bounds.bottom + 1 &&
                    child.scrollWidth <= child.clientWidth + 1
                );
            });
        }),
    ).toBe(true);
}

for (const { slug, editorExample: example } of articles) {
    test(`${slug}: layout, math and localized example links`, async ({ page }) => {
        const errors = [];
        page.on('pageerror', (error) => errors.push(error.message));
        await page.goto(`/blog/${slug}`);
        await expect(page.locator('.article-content')).toHaveAttribute('data-math-ready', 'true');
        await page.evaluate(() => document.fonts.ready);
        await expect(page.locator('.katex-error')).toHaveCount(0);
        expect(await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth)).toBeLessThanOrEqual(1);
        await expect(page.locator('.article-content pre code *')).toHaveCount(0);
        const links = page.locator('[data-blog-example]');
        await expect(links).toHaveCount(2);
        for (const link of await links.all()) {
            await expect(link).toHaveAttribute('href', buildExampleUrl(example));
            await expectExampleLabel(link, 'ru');
        }
        await setLanguage(page, 'en');
        for (const link of await links.all()) await expectExampleLabel(link, 'en');
        expect(await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth)).toBeLessThanOrEqual(1);
        await setLanguage(page, 'ru');
        for (const link of await links.all()) await expectExampleLabel(link, 'ru');
        expect(errors).toEqual([]);
    });
}

test('math is rendered, while the original code remains copyable', async ({ page }) => {
    await page.goto('/blog/indirect-measurement-error-calculation-lab-report');
    await expect(page.locator('.article-section .katex').first()).toBeVisible();
    await expect(page.locator('.article-section pre code').first()).toContainText('\\Delta');
    await expect(page.locator('pre .katex, code .katex')).toHaveCount(0);
});

test('the long uncertainty formula fits a narrow mobile page', async ({ page }) => {
    await page.setViewportSize({ width: 320, height: 800 });
    await page.goto('/blog/indirect-measurement-error-calculation-lab-report');
    await expect(page.locator('.article-content')).toHaveAttribute('data-math-ready', 'true');
    await page.evaluate(() => document.fonts.ready);
    const formula = page.locator('.article-quote .katex-display');
    await expect(formula).toHaveCount(1);
    expect(await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth)).toBeLessThanOrEqual(1);
    expect(
        await formula.evaluate((element) => {
            if (element.scrollWidth <= element.clientWidth + 1) return true;
            element.scrollLeft = element.scrollWidth;
            return element.scrollLeft > 0;
        }),
    ).toBe(true);
});

test.describe('without JavaScript', () => {
    test.use({ javaScriptEnabled: false });

    test('example links work', async ({ page }) => {
        const example = articles.find((article) => article.slug === 'latex-formulas').editorExample;
        await page.goto('/blog/latex-formulas');
        await expect(page.locator('[data-blog-example]').last()).toHaveAttribute('href', buildExampleUrl(example));
        await page.locator('[data-blog-example]').last().click();
        expect(new URL(page.url()).searchParams.get('latex')).toBe(example.latex);
    });
});
