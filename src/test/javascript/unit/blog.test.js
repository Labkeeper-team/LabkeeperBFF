import assert from 'node:assert/strict';
import { readdir, readFile } from 'node:fs/promises';
import { test } from 'node:test';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { parse } from 'yaml';
import { buildExampleUrl, EXAMPLE_ORIGIN, EXAMPLE_TYPES, MAX_EXAMPLE_URL_LENGTH } from './example-url.js';

const articlesDir = join(dirname(fileURLToPath(import.meta.url)), '../../../main/resources/blog/articles');

async function loadArticles() {
    const files = (await readdir(articlesDir, { recursive: true })).filter((file) => file.endsWith('.yaml'));
    return Promise.all(
        files.map(async (file) => {
            const text = (await readFile(join(articlesDir, file), 'utf8'))
                .replaceAll('{{publicUrl}}', EXAMPLE_ORIGIN)
                .replaceAll('{{publicHost}}', EXAMPLE_ORIGIN);
            return parse(text);
        })
    );
}

const articles = await loadArticles();
const examples = Object.fromEntries(articles.map((article) => [article.slug, article.editorExample]));

test('example links round-trip all segment types without changing source text', () => {
    const example = {
        latex: String.raw`$x+y$ & 50% #1 \frac{a}{b}`,
        markdown: '# Пример\n<sup>2</sup> + $$',
        compute: 'a = 1\nb = a + 2',
    };
    const url = new URL(buildExampleUrl(example), EXAMPLE_ORIGIN);
    assert.equal(url.pathname, '/project/default');
    assert.equal(url.searchParams.has('example'), false);
    assert.equal(url.searchParams.get('open'), 'latex');
    for (const [type, value] of Object.entries(example)) assert.equal(url.searchParams.get(type), value);
});

test('Markdown examples explicitly select Markdown mode', () => {
    assert.equal(
        new URL(buildExampleUrl({ markdown: '# Notes' }), EXAMPLE_ORIGIN).searchParams.get('open'),
        'markdown'
    );
});

test('the URL budget includes percent encoding and the origin', () => {
    const prefix = `${EXAMPLE_ORIGIN}/project/default?open=latex&latex=`;
    const text = 'a'.repeat(MAX_EXAMPLE_URL_LENGTH - prefix.length);
    assert.equal(EXAMPLE_ORIGIN.length + buildExampleUrl({ latex: text }).length, MAX_EXAMPLE_URL_LENGTH);
    assert.throws(() => buildExampleUrl({ latex: text + 'a' }), /limit/);
    assert.throws(() => buildExampleUrl({ latex: 'я'.repeat(400) }), /limit/);
});

for (const example of [undefined, null, [], {}, { latex: '' }, { markdown: '  ' }, { latex: 12 }, { unknown: 'text' }]) {
    test(`invalid example is rejected: ${JSON.stringify(example)}`, () => {
        assert.throws(() => buildExampleUrl(example));
    });
}

test('every article has a distinct, bounded example and no orphan examples exist', () => {
    assert.ok(articles.length > 0);
    assert.equal(new Set(articles.map((article) => article.slug)).size, articles.length);
    const links = articles.map((article) => {
        const link = buildExampleUrl(article.editorExample, article.slug);
        const query = new URL(link, EXAMPLE_ORIGIN).searchParams;
        assert.equal(query.has('example'), false);
        const open = 'latex' in article.editorExample || 'compute' in article.editorExample ? 'latex' : 'markdown';
        assert.equal(query.get('open'), open);
        for (const type of EXAMPLE_TYPES) {
            assert.equal(query.get(type), article.editorExample[type] ?? null);
        }
        return link;
    });
    assert.equal(new Set(links).size, articles.length);
});

for (const slug of [
    'student-confidence-intervals-error-calculation',
    'indirect-measurement-error-calculation-lab-report',
]) {
    test(`${slug}: calculation example passes compute and result substitutions together`, () => {
        const example = examples[slug];
        assert.ok(example.compute.trim());
        assert.match(example.latex, /\$\{[a-z]+\}/);
        assert.doesNotMatch(example.latex, /\\(?:documentclass|begin\{document\}|end\{document\})/);
        const query = new URL(buildExampleUrl(example, slug), EXAMPLE_ORIGIN).searchParams;
        assert.equal(query.get('compute'), example.compute);
        assert.equal(query.get('latex'), example.latex);
        assert.equal(query.get('open'), 'latex');
    });
}
