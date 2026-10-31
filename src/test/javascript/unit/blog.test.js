import assert from 'node:assert/strict';
import { readdir, readFile } from 'node:fs/promises';
import { test } from 'node:test';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { parse } from 'yaml';
import { buildExampleUrl, EXAMPLE_ORIGIN, EXAMPLE_TYPES } from './example-url.js';

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

test('an example link carries the slug and leaves the program out of the query', () => {
    const example = {
        latex: String.raw`$x+y$ & 50% #1 \frac{a}{b}`,
        markdown: '# Пример\n<sup>2</sup> + $$',
        compute: 'a = 1\nb = a + 2',
    };
    const url = new URL(buildExampleUrl(example, 'formulas'), EXAMPLE_ORIGIN);
    assert.equal(url.pathname, '/project/default');
    assert.equal(url.searchParams.get('example'), 'formulas');
    assert.equal(url.searchParams.has('open'), false);
    for (const type of EXAMPLE_TYPES) assert.equal(url.searchParams.has(type), false);
    assert.equal(url.href.includes(example.latex), false);
});

test('a segment list keeps parameters in YAML and the slug in the link', () => {
    const example = [
        { type: 'compute', text: 'a = 1', parameters: { hideValue: true, visible: true } },
        { type: 'latex', text: 'first block' },
        { type: 'latex', text: 'second block', parameters: { visible: false } },
        { type: 'md', text: '# Notes' },
    ];
    const url = new URL(buildExampleUrl(example, 'many-segments'), EXAMPLE_ORIGIN);
    assert.equal(url.searchParams.get('example'), 'many-segments');
    assert.equal(url.href.includes('second block'), false);
    assert.equal(example[2].parameters.visible, false);
});

test('a long program stays in YAML and does not grow the link', () => {
    const text = 'я'.repeat(4000);
    const url = buildExampleUrl({ latex: text }, 'long-example');
    assert.equal(url, '/project/default?example=long-example');
    assert.equal(url.includes(text), false);
});

for (const example of [undefined, null, [], {}, { latex: '' }, { markdown: '  ' }, { latex: 12 }, { unknown: 'text' }]) {
    test(`invalid example is rejected: ${JSON.stringify(example)}`, () => {
        assert.throws(() => buildExampleUrl(example));
    });
}

test('every article has a distinct example link and the program stays in the article', () => {
    assert.ok(articles.length > 0);
    assert.equal(new Set(articles.map((article) => article.slug)).size, articles.length);
    const links = articles.map((article) => {
        const link = buildExampleUrl(article.editorExample, article.slug);
        const query = new URL(link, EXAMPLE_ORIGIN).searchParams;
        assert.equal(query.get('example'), article.slug);
        assert.equal(query.has('open'), false);
        for (const type of EXAMPLE_TYPES) {
            assert.equal(query.has(type), false);
            const text = article.editorExample[type];
            if (typeof text === 'string' && text.length > 12) assert.equal(link.includes(text.slice(0, 12)), false);
        }
        return link;
    });
    assert.equal(new Set(links).size, articles.length);
});

for (const slug of [
    'student-confidence-intervals-error-calculation',
    'indirect-measurement-error-calculation-lab-report',
]) {
    test(`${slug}: calculation example keeps compute and result substitutions in YAML`, () => {
        const example = examples[slug];
        assert.ok(example.compute.trim());
        assert.match(example.latex, /\$\{[a-z]+\}/);
        assert.doesNotMatch(example.latex, /\\(?:documentclass|begin\{document\}|end\{document\})/);
        const query = new URL(buildExampleUrl(example, slug), EXAMPLE_ORIGIN).searchParams;
        assert.equal(query.get('example'), slug);
        assert.equal(query.has('compute'), false);
        assert.equal(query.has('latex'), false);
    });
}
