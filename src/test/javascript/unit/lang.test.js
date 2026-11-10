// lang.js исполняется в отдельном контексте vm с поддельными document, cookie и localStorage.
// Тексты страниц приходят с сервера, скрипт только держит cookie labkeeper_lang вровень с языком редактора.
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { test } from 'node:test';
import { fileURLToPath } from 'node:url';
import vm from 'node:vm';

const SOURCE = readFileSync(
    join(dirname(fileURLToPath(import.meta.url)), '../../../main/resources/static/assets/js/lang.js'),
    'utf8'
);

const KEY = 'labkeeper_lang';

/**
 * Страница на языке page. cookie и stored — что уже лежит в браузере, cookiesBlocked — cookie не сохраняются,
 * browser — языки из настроек браузера
 */
function openPage({ page, cookie = null, stored = null, cookiesBlocked = false, browser = ['ru-RU'], userAgent = 'Mozilla/5.0' }) {
    const storage = new Map(stored ? [[KEY, stored]] : []);
    const jar = new Map(cookie ? [[KEY, cookie]] : []);
    const listeners = {};
    const buttons = ['ru', 'en', 'zh'].map((lang) => ({
        lang,
        onClick: null,
        getAttribute: () => lang,
        addEventListener: (_, listener) => {
            buttons.find((b) => b.lang === lang).onClick = listener;
        },
    }));
    const result = { reloads: 0, storage, jar, buttons };
    const document = {
        documentElement: { lang: page },
        get cookie() {
            return [...jar].map(([name, value]) => `${name}=${value}`).join('; ');
        },
        set cookie(value) {
            if (cookiesBlocked) return;
            const [pair] = value.split(';');
            const [name, cookieValue] = pair.split('=');
            jar.set(name.trim(), cookieValue);
        },
        addEventListener: (type, listener) => {
            listeners[type] = listener;
        },
        querySelectorAll: () => buttons,
    };
    const window = {
        navigator: { languages: browser, language: browser[0], userAgent },
        localStorage: {
            getItem: (key) => (storage.has(key) ? storage.get(key) : null),
            setItem: (key, value) => storage.set(key, String(value)),
        },
        location: {
            reload: () => {
                result.reloads += 1;
            },
        },
    };
    vm.runInNewContext(SOURCE, { window, document });
    listeners.DOMContentLoaded?.();
    result.click = (lang) => buttons.find((b) => b.lang === lang).onClick();
    return result;
}

test('без сохранённого языка страница остаётся как есть и отдаёт свой язык редактору', () => {
    const page = openPage({ page: 'ru' });
    assert.equal(page.reloads, 0);
    assert.equal(page.storage.get(KEY), 'ru');
});

test('кнопка другого языка ставит cookie и перезагружает страницу', () => {
    const page = openPage({ page: 'ru' });
    page.click('en');
    assert.equal(page.jar.get(KEY), 'en');
    assert.equal(page.storage.get(KEY), 'en');
    assert.equal(page.reloads, 1);
});

test('кнопка китайского ставит cookie zh и перезагружает страницу', () => {
    const page = openPage({ page: 'en', cookie: 'en' });
    page.click('zh');
    assert.equal(page.jar.get(KEY), 'zh');
    assert.equal(page.storage.get(KEY), 'zh');
    assert.equal(page.reloads, 1);
});

test('китайский, выбранный в редакторе, доходит до сайта', () => {
    const page = openPage({ page: 'ru', stored: 'zh' });
    assert.equal(page.reloads, 1);
    assert.equal(page.jar.get(KEY), 'zh');
});

test('кнопка текущего языка ничего не делает', () => {
    const page = openPage({ page: 'en', cookie: 'en' });
    page.click('en');
    assert.equal(page.reloads, 0);
});

test('язык, выбранный в редакторе, доходит до сайта одной перезагрузкой', () => {
    const before = openPage({ page: 'ru', stored: 'en' });
    assert.equal(before.reloads, 1);
    assert.equal(before.jar.get(KEY), 'en');

    const after = openPage({ page: 'en', stored: 'en', cookie: 'en' });
    assert.equal(after.reloads, 0);
});

test('если сервер не принял cookie, страница не перезагружается по кругу', () => {
    const page = openPage({ page: 'ru', stored: 'en', cookie: 'en' });
    assert.equal(page.reloads, 0);
    assert.equal(page.storage.get(KEY), 'en');
});

test('без cookie язык не переключить, перезагрузок нет, а язык редактора не теряется', () => {
    const stale = openPage({ page: 'ru', stored: 'en', cookiesBlocked: true });
    assert.equal(stale.reloads, 0);
    assert.equal(stale.storage.get(KEY), 'en');

    stale.click('en');
    assert.equal(stale.reloads, 0);
});

test('чужое значение в localStorage не трогает страницу', () => {
    const page = openPage({ page: 'ru', stored: 'de' });
    assert.equal(page.reloads, 0);
    assert.equal(page.storage.get(KEY), 'ru');
});

test('первый заход из китайского браузера переключает сайт на китайский', () => {
    const page = openPage({ page: 'ru', browser: ['zh-CN', 'zh'] });
    assert.equal(page.reloads, 1);
    assert.equal(page.jar.get(KEY), 'zh');
    assert.equal(page.storage.get(KEY), 'zh');

    const after = openPage({ page: 'zh', cookie: 'zh', stored: 'zh', browser: ['zh-CN'] });
    assert.equal(after.reloads, 0);
});

test('китайский браузер не отменяет язык, который человек уже выбрал', () => {
    const chosen = openPage({ page: 'ru', cookie: 'ru', browser: ['zh-TW'] });
    assert.equal(chosen.reloads, 0);

    const fromEditor = openPage({ page: 'en', cookie: 'en', stored: 'en', browser: ['zh-CN'] });
    assert.equal(fromEditor.reloads, 0);
});

test('другие языки браузера оставляют сайт на русском', () => {
    const page = openPage({ page: 'ru', browser: ['de-DE', 'zh-CN'] });
    assert.equal(page.reloads, 0);
    assert.equal(page.storage.get(KEY), 'ru');
});

test('китайский браузер без cookie не перезагружает страницу по кругу', () => {
    const page = openPage({ page: 'ru', browser: ['zh-CN'], cookiesBlocked: true });
    assert.equal(page.reloads, 0);
});

test('первый заход из английского браузера переключает сайт на английский', () => {
    const page = openPage({ page: 'ru', browser: ['en-US', 'en'] });
    assert.equal(page.reloads, 1);
    assert.equal(page.jar.get(KEY), 'en');
    assert.equal(page.storage.get(KEY), 'en');
});

test('русский браузер остаётся на русском', () => {
    const page = openPage({ page: 'ru', browser: ['ru-RU', 'en-US'] });
    assert.equal(page.reloads, 0);
    assert.equal(page.storage.get(KEY), 'ru');
});

test('поисковые роботы с английским браузером видят русский', () => {
    const googlebot = openPage({
        page: 'ru',
        browser: ['en-US'],
        userAgent: 'Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)',
    });
    assert.equal(googlebot.reloads, 0);
    assert.equal(googlebot.jar.get(KEY), undefined);
});
