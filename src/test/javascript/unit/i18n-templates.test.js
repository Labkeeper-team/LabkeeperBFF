import assert from 'node:assert/strict';
import { readFileSync, readdirSync } from 'node:fs';
import { test } from 'node:test';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import vm from 'node:vm';

const resources = join(dirname(fileURLToPath(import.meta.url)), '../../../main/resources');
const templatesDir = join(resources, 'templates');

/** Elements whose markup differs from the dictionary on purpose (an icon in HTML, a label in the dictionary). */
const MARKUP_DIFFERS = new Set(['hero-btn-ai']);

function loadRu() {
    const source = readFileSync(join(resources, 'static/assets/js/lang.js'), 'utf8');
    const context = {};
    vm.runInNewContext(`${source.split('document.addEventListener')[0]};this.i18n = i18n;`, context);
    return context.i18n.ru;
}

const text = (html) =>
    html
        .replace(/<[^>]+>/g, ' ')
        .replace(/&nbsp;/g, ' ')
        .replace(/\s+/g, ' ')
        .trim();

/** Elements with a data-i18n attribute and their inner HTML, nested same-name tags excluded. */
function translatable(html) {
    const pattern =
        /<([a-z0-9]+)\b(?:[^>"']|"[^"]*"|'[^']*')*\sdata-i18n="([^"]+)"(?:[^>"']|"[^"]*"|'[^']*')*>((?:(?!<\1[\s>])[\s\S])*?)<\/\1\s*>/g;
    return [...html.matchAll(pattern)].map((match) => ({ key: match[2], inner: match[3] }));
}

const ru = loadRu();
const templates = readdirSync(templatesDir).filter((name) => name.endsWith('.html'));

test('templates carry the Russian text in HTML, so it is visible without JavaScript', () => {
    assert.ok(templates.length > 0);
    for (const name of templates) {
        const html = readFileSync(join(templatesDir, name), 'utf8');
        for (const { key, inner } of translatable(html)) {
            assert.notEqual(inner.trim(), '', `${name}: data-i18n="${key}" is empty`);
        }
    }
});

test('Russian text in templates matches the lang.js dictionary', () => {
    for (const name of templates) {
        const html = readFileSync(join(templatesDir, name), 'utf8');
        for (const { key, inner } of translatable(html)) {
            if (MARKUP_DIFFERS.has(key) || !(key in ru) || /__[A-Z_]+__/.test(ru[key])) continue;
            assert.equal(text(inner), text(ru[key]), `${name}: data-i18n="${key}" differs from lang.js`);
        }
    }
});
