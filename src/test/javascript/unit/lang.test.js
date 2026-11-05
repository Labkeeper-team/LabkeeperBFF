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

/** Страница на языке page. cookie и stored — что уже лежит в браузере, cookiesBlocked — cookie не сохраняются */
function openPage({ page, cookie = null, stored = null, cookiesBlocked = false }) {
    const storage = new Map(stored ? [[KEY, stored]] : []);
    const jar = new Map(cookie ? [[KEY, cookie]] : []);
    const listeners = {};
    const buttons = ['ru', 'en'].map((lang) => ({
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
