import fs from 'fs';
import path from 'path';

const moduleDir = path.resolve(import.meta.dirname, '..');
const pagesSrc = path.join(moduleDir, 'src/main/pages/src');
const articlesDir = path.join(moduleDir, 'src/main/pages/scripts/blog-generator/articles-data');
const resources = path.join(moduleDir, 'src/main/resources');
const staticDir = path.join(resources, 'static');
const templatesDir = path.join(resources, 'templates');
const yamlDir = path.join(resources, 'blog/articles');

const PLACEHOLDERS = [
    ['IO_LABKEEPER_FRONTEND_YANDEX_METRIKA_KEY', '[(${yandexMetrikaKey})]'],
    ['IO_LABKEEPER_FRONTEND_API_VERSION', '[(${apiVersion})]'],
    ['IO_LABKEEPER_FRONTEND_OPENPANEL_CLIENT_ID', '[(${openpanelClientId})]'],
    ['IO_LABKEEPER_FRONTEND_OPENPANEL_API_URL', '[(${openpanelApiUrl})]'],
];

const SLIDER_BOOT = '<script th:inline="javascript">\n        window.LABKEEPER_BLOG_ARTICLES = [(${blogSliderJson})];\n    </script>\n    ';

function copyAssets() {
    const assetsSrc = path.join(pagesSrc, 'assets');
    const assetsDest = path.join(staticDir, 'assets');
    fs.rmSync(assetsDest, { recursive: true, force: true });
    fs.cpSync(assetsSrc, assetsDest, {
        recursive: true,
        filter: (source) => {
            const relative = path.relative(assetsSrc, source);
            return relative !== 'scss' && !relative.startsWith(`scss${path.sep}`);
        },
    });
    fs.copyFileSync(path.join(pagesSrc, 'robots.txt'), path.join(staticDir, 'robots.txt'));
}

function patchSlider() {
    const sliderPath = path.join(staticDir, 'assets/js/slider.js');
    const slider = fs.readFileSync(sliderPath, 'utf8');
    const start = slider.indexOf('const ALL_BLOG_ARTICLES = [');
    const end = slider.indexOf('window.ALL_BLOG_ARTICLES = ALL_BLOG_ARTICLES;');
    if (start < 0 || end < 0) {
        throw new Error('slider.js article registry was not found');
    }
    const patched = `${slider.slice(0, start)}const ALL_BLOG_ARTICLES = window.LABKEEPER_BLOG_ARTICLES || [];\n    ${slider.slice(end)}`;
    fs.writeFileSync(sliderPath, patched);
}

const BLOCK_WIDTH = 96;

function yamlQuote(value) {
    return JSON.stringify(value);
}

function emitString(value, keyIndent) {
    const text = value.replace(/\r\n/g, '\n');
    if (!text.includes('\n') && text.length <= BLOCK_WIDTH) {
        return yamlQuote(text);
    }
    const pad = '  '.repeat(keyIndent + 1);
    const lines = [];
    let inPre = false;
    for (const line of text.split('\n')) {
        lines.push(...wrapPlainLine(line, inPre));
        inPre = preStateAfter(line, inPre);
    }
    const indicator = text.endsWith('\n') ? '|2' : '|2-';
    const body = lines.map((line) => (line.length === 0 ? '' : pad + line)).join('\n');
    return `${indicator}\n${body}`;
}

function wrapPlainLine(line, inPre) {
    if (inPre || line.includes('<pre') || line.length <= BLOCK_WIDTH) {
        return [line];
    }
    const parts = [];
    let rest = line;
    while (rest.length > BLOCK_WIDTH) {
        const breakAt = rest.lastIndexOf(' ', BLOCK_WIDTH);
        if (breakAt <= 0) {
            break;
        }
        parts.push(rest.slice(0, breakAt));
        rest = rest.slice(breakAt + 1);
    }
    parts.push(rest);
    return parts;
}

function preStateAfter(line, inPre) {
    const tags = line.match(/<\/?pre\b[^>]*>/gi) || [];
    let state = inPre;
    for (const tag of tags) {
        state = !tag.startsWith('</');
    }
    return state;
}

function emit(value, indent) {
    if (Array.isArray(value)) {
        if (value.length === 0) {
            return '[]';
        }
        const pad = '  '.repeat(indent);
        return `\n${value.map((item) => `${pad}- ${emitListItem(item, indent + 1)}`).join('\n')}`;
    }
    if (value && typeof value === 'object') {
        return `\n${emitObject(value, indent)}`;
    }
    if (typeof value === 'string') {
        return emitString(value, indent);
    }
    if (typeof value === 'number' || typeof value === 'boolean') {
        return String(value);
    }
    if (value == null) {
        return 'null';
    }
    return yamlQuote(String(value));
}

function emitListItem(item, indent) {
    if (item && typeof item === 'object' && !Array.isArray(item)) {
        const body = emitObject(item, indent);
        const lines = body.split('\n');
        return [lines[0].trimStart(), ...lines.slice(1)].join('\n');
    }
    return emit(item, indent);
}

function emitObject(object, indent) {
    const pad = '  '.repeat(indent);
    return Object.entries(object).map(([key, value]) => {
        if (Array.isArray(value) || (value && typeof value === 'object')) {
            return `${pad}${key}:${emit(value, indent + 1)}`;
        }
        return `${pad}${key}: ${emit(value, indent)}`;
    }).join('\n');
}

function loadArticles() {
    const articles = [];
    for (const file of fs.readdirSync(articlesDir).filter((name) => name.endsWith('.json')).sort()) {
        const data = JSON.parse(fs.readFileSync(path.join(articlesDir, file), 'utf8'));
        for (const article of Array.isArray(data) ? data : [data]) {
            articles.push(article);
        }
    }
    return articles;
}

function catalogOrder() {
    const blog = fs.readFileSync(path.join(pagesSrc, 'blog.html'), 'utf8');
    const start = blog.indexOf('id="blog-grid"');
    const grid = blog.slice(start);
    const slugs = [];
    const seen = new Set();
    for (const match of grid.matchAll(/href="\/blog\/([a-z0-9-]+)"/g)) {
        if (!seen.has(match[1])) {
            seen.add(match[1]);
            slugs.push(match[1]);
        }
    }
    return new Map(slugs.map((slug, index) => [slug, index]));
}

function writeArticles() {
    fs.rmSync(yamlDir, { recursive: true, force: true });
    fs.mkdirSync(yamlDir, { recursive: true });
    const order = catalogOrder();
    const slugs = new Set();
    for (const article of loadArticles()) {
        if (slugs.has(article.slug)) {
            throw new Error(`Duplicate slug ${article.slug}`);
        }
        slugs.add(article.slug);
        const document = {
            catalogIndex: order.get(article.slug) ?? 10000,
            ...article,
        };
        fs.writeFileSync(path.join(yamlDir, `${article.slug}.yaml`), `${emitObject(document, 0)}\n`);
    }
    return slugs.size;
}

function replaceInnerDiv(html, openTagStart, replacement) {
    const openEnd = html.indexOf('>', openTagStart);
    let depth = 1;
    const tag = /<\/?div\b[^>]*>/gi;
    tag.lastIndex = openEnd + 1;
    let match;
    while ((match = tag.exec(html))) {
        if (match[0].startsWith('</')) {
            depth -= 1;
        } else if (!match[0].endsWith('/>')) {
            depth += 1;
        }
        if (depth === 0) {
            return `${html.slice(0, openEnd + 1)}\n${replacement}\n${html.slice(match.index)}`;
        }
    }
    throw new Error('Unbalanced div');
}

function applyCommon(html) {
    let result = html;
    for (const [token, expression] of PLACEHOLDERS) {
        result = result.split(token).join(expression);
    }
    return result.replace(
        /<script src="(?:\.\.\/)?assets\/js\/slider\.js" defer><\/script>/g,
        `${SLIDER_BOOT}<script src="assets/js/slider.js" defer></script>`,
    );
}

function writePage(name, transform) {
    const source = fs.readFileSync(path.join(pagesSrc, name), 'utf8');
    fs.mkdirSync(templatesDir, { recursive: true });
    fs.writeFileSync(path.join(templatesDir, name), transform(applyCommon(source)));
}

const knowledgeSlides = `                            <div class="swiper-slide" th:each="article : \${articles}">
                                <article class="blog-card">
                                    <div class="blog-card__top">
                                        <span class="blog-card__tag" th:each="tag : \${article.categoryNames}" th:text="\${tag}">Категория</span>
                                    </div>
                                    <h3 class="blog-card__title" th:text="\${article.cardTitle}">Статья</h3>
                                    <p class="blog-card__text" th:text="\${article.cardDescription}">Описание</p>
                                    <a class="blog-card__footer" th:href="@{/blog/{slug}(slug=\${article.slug})}">
                                        <span>Читать статью</span>
                                        <span class="blog-card__arrow">→</span>
                                    </a>
                                </article>
                            </div>`;

const blogCards = `            <article class="blog-item" th:each="article : \${articles}" th:data-category="\${article.categoryAttr}">
                <a class="blog-card" th:href="@{/blog/{slug}(slug=\${article.slug})}">
                    <div class="blog-card__top">
                        <span class="blog-card__tag" th:each="tag : \${article.categoryNames}" th:text="\${tag}">Категория</span>
                    </div>
                    <h2 class="blog-card__title" th:text="\${article.cardTitle}">Статья</h2>
                    <p class="blog-card__text" th:text="\${article.cardDescription}">Описание</p>
                    <div class="blog-card__footer">
                        <span>Читать статью</span>
                        <span class="blog-card__arrow">→</span>
                    </div>
                </a>
            </article>`;

copyAssets();
patchSlider();
const articleCount = writeArticles();
writePage('index.html', (html) => {
    const marker = html.indexOf('<div class="swiper knowledge__swiper">');
    const wrapper = html.indexOf('<div class="swiper-wrapper">', marker);
    return replaceInnerDiv(html, wrapper, knowledgeSlides);
});
writePage('blog.html', (html) => {
    const grid = html.indexOf('<div class="blog-grid" id="blog-grid">');
    return replaceInnerDiv(html, grid, blogCards);
});
for (const page of ['about.html', 'privacy.html', 'soglas.html', 'sogl_adv.html', 'sogl_ds.html', 'sogl_yam.html', 'oferta.html']) {
    writePage(page, (html) => html);
}

console.log(`Imported ${articleCount} articles and page templates into resources`);
