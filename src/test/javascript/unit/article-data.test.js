import assert from 'node:assert/strict';
import { mkdtempSync, readFileSync, readdirSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { test } from 'node:test';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { parse, stringify } from 'yaml';
import { buildExampleUrl } from './example-url.js';

const resources = join(dirname(fileURLToPath(import.meta.url)), '../../../main/resources');
const articlesDir = join(resources, 'blog/articles');
const articleTemplate = join(resources, 'templates/article.html');
const SLUG = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;

const sample = {
    slug: 'sample',
    title: 'Sample',
    metaDescription: 'Article description',
    datePublished: '2026-10-08',
    batch: 1,
    sections: [{ id: 'example', title: 'Example', html: '<p>Source text</p>' }],
    editorExample: { latex: '$x$' },
};

function temporaryDirectory(t) {
    const directory = mkdtempSync(join(tmpdir(), 'labkeeper-article-data-'));
    t.after(() => rmSync(directory, { recursive: true, force: true }));
    return directory;
}

function validateArticle(article) {
    if (!article || typeof article.slug !== 'string' || !SLUG.test(article.slug)) {
        throw new Error('Article slug must contain lowercase letters, numbers and hyphens');
    }
    buildExampleUrl(article.editorExample, article.slug);
}

function loadArticles(files) {
    const articles = [];
    const slugs = new Set();
    for (const file of files) {
        try {
            const data = parse(readFileSync(file, 'utf8'));
            for (const article of Array.isArray(data) ? data : [data]) {
                validateArticle(article);
                if (slugs.has(article.slug)) throw new Error(`Duplicate article: ${article.slug}`);
                slugs.add(article.slug);
                articles.push(article);
            }
        } catch (error) {
            throw new Error(`${file}: ${error.message}`, { cause: error });
        }
    }
    if (!articles.length) throw new Error('No articles found in YAML sources');
    return articles;
}

function scriptJson(value) {
    return JSON.stringify(value).replaceAll('<', '\\u003c');
}

test('YAML batches and single articles retain every example character', (t) => {
    const directory = temporaryDirectory(t);
    const first = {
        ...sample,
        editorExample: { latex: '\\[x = ${value}\\]', compute: 'value = 2\n// вычисление' },
    };
    const second = { ...sample, slug: 'notes', editorExample: { markdown: '# Заметки\n\n$ & <tag> \\' } };
    const files = [join(directory, 'batch.yaml'), join(directory, 'article.yaml')];
    writeFileSync(files[0], stringify([first]));
    writeFileSync(files[1], stringify(second));

    assert.deepEqual(loadArticles(files), [first, second]);
    for (const article of loadArticles(files)) {
        const query = new URL(buildExampleUrl(article.editorExample), 'https://labkeeper.io').searchParams;
        for (const [type, text] of Object.entries(article.editorExample)) assert.equal(query.get(type), text);
    }
});

for (const data of [
    [sample, sample],
    [sample, { ...sample, editorExample: { markdown: '# Different' } }],
]) {
    test('duplicate slugs in a batch are rejected regardless of example type: ' + JSON.stringify(data[1].editorExample), (t) => {
        const file = join(temporaryDirectory(t), 'duplicate.yaml');
        writeFileSync(file, stringify(data));
        assert.throws(() => loadArticles([file]), /duplicate\.yaml: Duplicate article: sample/);
    });
}

test('duplicate slugs across batches are rejected', (t) => {
    const directory = temporaryDirectory(t);
    const files = ['first.yaml', 'second.yaml'].map((name) => join(directory, name));
    for (const file of files) writeFileSync(file, stringify([sample]));
    assert.throws(() => loadArticles(files), /second\.yaml: Duplicate article: sample/);
});

test('invalid YAML and empty sources produce actionable errors', (t) => {
    const directory = temporaryDirectory(t);
    const file = join(directory, 'broken.yaml');
    writeFileSync(file, '{');
    assert.throws(() => loadArticles([file]), /broken\.yaml:/);
    writeFileSync(file, '[]');
    assert.throws(() => loadArticles([file]), /No articles/);
    assert.throws(() => loadArticles([]), /No articles/);
    writeFileSync(file, 'null');
    assert.throws(() => loadArticles([file]), /broken\.yaml: Article slug/);
});

for (const [label, editorExample] of [
    ['missing', undefined],
    ['empty', {}],
    ['blank text', { latex: '' }],
    ['non-string text', { latex: 42 }],
    ['unknown segment', { unknown: 'text' }],
    ['oversized URL', { latex: 'я'.repeat(400) }],
]) {
    test('article validation rejects an invalid editorExample: ' + label, (t) => {
        const article = { ...sample, editorExample };
        const file = join(temporaryDirectory(t), 'invalid-example.yaml');
        writeFileSync(file, stringify(article));
        assert.throws(() => loadArticles([file]), /invalid-example\.yaml: sample:/);
    });
}

test('invalid slugs cannot escape the output directory', () => {
    for (const slug of [undefined, null, '', '../outside', 'a/b', 'A', 'a.html']) {
        assert.throws(() => validateArticle({ ...sample, slug }), /slug/);
    }
});

test('article metadata stays textual and JSON-LD cannot close the script tag', () => {
    const template = readFileSync(articleTemplate, 'utf8');
    for (const binding of [
        'th:text="${article.pageTitle}"',
        'th:content="${article.metaDescription}"',
        'th:content="${article.ogTitle}"',
        'th:content="${article.ogDescription}"',
        'th:content="${article.twitterTitle}"',
        'th:content="${article.twitterDescription}"',
        'th:alt="${article.sidebarImageAlt}"',
        'th:utext="${article.articleJsonLd}"',
        'th:utext="${article.breadcrumbJsonLd}"',
    ]) {
        assert.ok(template.includes(binding), binding);
    }

    const article = {
        ...sample,
        schemaDescription: 'Structured description </script>',
        schemaBreadcrumbTitle: 'Structured breadcrumb </script>',
    };
    const articleJson = scriptJson({ '@type': 'Article', description: article.schemaDescription });
    const breadcrumbJson = scriptJson({
        '@type': 'BreadcrumbList',
        itemListElement: [{ name: article.schemaBreadcrumbTitle }],
    });
    assert.equal(articleJson.includes('<'), false);
    assert.equal(breadcrumbJson.includes('<'), false);
    assert.equal(JSON.parse(articleJson).description, article.schemaDescription);
    assert.equal(JSON.parse(breadcrumbJson).itemListElement[0].name, article.schemaBreadcrumbTitle);
});

test('published YAML articles pass validation', () => {
    const files = readdirSync(articlesDir, { recursive: true })
        .filter((file) => file.endsWith('.yaml'))
        .map((file) => join(articlesDir, file));
    const articles = loadArticles(files);
    assert.ok(articles.length > 0);
    assert.equal(new Set(articles.map((article) => article.slug)).size, articles.length);
});
