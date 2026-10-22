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
            const normalized = relative.split(path.sep).join('/');
            return normalized !== 'scss'
                && !normalized.startsWith('scss/')
                && normalized !== 'vendor/katex'
                && !normalized.startsWith('vendor/katex/');
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
        const yaml = `${emitObject(document, 0)}\n`
            .replaceAll('https://labkeeper.io', '{{publicUrl}}')
            .replaceAll('labkeeper.io', '{{publicHost}}');
        const category = article.categories?.[0];
        if (!category) {
            throw new Error(`Article ${article.slug} has no category`);
        }
        const dir = path.join(yamlDir, category);
        fs.mkdirSync(dir, { recursive: true });
        fs.writeFileSync(path.join(dir, `${article.slug}.yaml`), yaml);
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

function usePublicSite(text) {
    return text
        .replaceAll('labkeeper.io@gmail.com', '[(${contactEmail})]')
        .replaceAll('+7 (963) 768-55-90', '[(${contactPhone})]')
        .replaceAll('+7(963)768-55-90', '[(${contactPhone})]')
        .replaceAll('https://labkulture.design', '[(${publicUrl})]')
        .replaceAll('https://labkeeper.io', '[(${publicUrl})]');
}

function applyCommon(html) {
    let result = usePublicSite(html);
    for (const [token, expression] of PLACEHOLDERS) {
        result = result.split(token).join(expression);
    }
    if (!result.includes('__LABKEEPER_PUBLIC_URL')) {
        result = result.replace(
            /([ \t]*)window\.__LABKEEPER_OPENPANEL_API_URL = "\[\(\$\{openpanelApiUrl\}\)\]";/g,
            (match, indent) => [
                match,
                `${indent}window.__LABKEEPER_PUBLIC_URL = "[(\${publicUrl})]";`,
                `${indent}window.__LABKEEPER_CONTACT_EMAIL = "[(\${contactEmail})]";`,
                `${indent}window.__LABKEEPER_CONTACT_PHONE = "[(\${contactPhone})]";`,
            ].join('\n'),
        );
    }
    result = result.replace(
        /(<a\b[^>]*class="[^"]*header__login"[^>]*)(>)/g,
        (match, start, end) => (
            start.includes('th:style')
                ? match
                : `${start} th:style="\${viewerAuthenticated} ? 'display: none' : null"${end}`
        ),
    );
    result = result.replace(
        /(<a\b[^>]*class="[^"]*header__projects"[^>]*?)\sstyle="display:\s*none;?"/g,
        `$1 th:style="\${viewerAuthenticated} ? null : 'display: none'"`,
    );
    result = result
        .replaceAll('assets/css/swiper-bundle.min.css', 'assets/css/swiper.css')
        .replaceAll('assets/js/swiper-bundle.min.js', 'assets/js/swiper.js');
    result = result.replace(
        /<script src="(?:\.\.\/)?assets\/js\/slider\.js" defer><\/script>\s*<script src="(?:\.\.\/)?assets\/js\/swiper\.js"><\/script>/g,
        `${SLIDER_BOOT}<script src="assets/js/swiper.js" defer></script>\n    <script src="assets/js/slider.js" defer></script>`,
    );
    return result.replace(
        /<script src="(?:\.\.\/)?assets\/js\/slider\.js" defer><\/script>/g,
        `${SLIDER_BOOT}<script src="assets/js/swiper.js" defer></script>\n    <script src="assets/js/slider.js" defer></script>`,
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

const projectSlides = `                            <div class="swiper-slide" th:each="project : \${previewProjects}"
                                th:data-category="\${project.category}"
                                th:attr="data-name-ru=\${project.nameRu},data-name-en=\${project.nameEn},data-category-ru=\${project.categoryRu},data-category-en=\${project.categoryEn}">
                                <a class="example-card" target="_blank" th:href="@{/project/{id}(id=\${project.id})}">
                                    <div class="example-card__visual">
                                        <img class="example-card__image" loading="lazy"
                                            th:src="\${project.logoUrl != null ? project.logoUrl : 'assets/img/logo.svg'}"
                                            th:alt="\${project.nameRu}"
                                            th:classappend="\${project.logoUrl == null} ? 'example-card__image--placeholder'">
                                        <img src="assets/img/target.svg" alt="open" class="example-card__external-icon" width="16" height="16">
                                    </div>
                                    <div class="example-card__content">
                                        <h3 class="example-card__title" th:text="\${project.nameRu}">Проект</h3>
                                        <p class="example-card__text" th:text="\${project.categoryRu}">Категория</p>
                                    </div>
                                </a>
                            </div>
                            <div class="loading-placeholder" th:if="\${previewProjects.isEmpty()}">Нет проектов в данной категории</div>`;

function patchLandingScripts() {
    fs.writeFileSync(path.join(staticDir, 'assets/js/auth.js'), `document.addEventListener('DOMContentLoaded', () => {
  // Login and project buttons are rendered by the server for the current viewer.
});
`);
    const sliderPath = path.join(staticDir, 'assets/js/slider.js');
    const slider = fs.readFileSync(sliderPath, 'utf8');
    const start = slider.indexOf('const projectsContainer = document.getElementById(\'projects-container\');');
    const end = slider.indexOf('// 2. Реестр всех статей блога');
    if (start < 0 || end < 0) {
        throw new Error('slider.js preview block was not found');
    }
    const replacement = `const projectsContainer = document.getElementById('projects-container');
    if (projectsContainer) {
        const categoryLinks = document.querySelectorAll('#project-categories .header__nav-link');
        const allSlides = Array.from(projectsContainer.querySelectorAll('.swiper-slide'));
        let swiperInstance = null;

        const activeLangBtn = document.querySelector('.js-lang-switcher .header__lang-btn--active');
        let currentLang = activeLangBtn ? activeLangBtn.getAttribute('data-lang') : (document.documentElement.lang || 'ru');

        function initSwiper() {
            if (swiperInstance) {
                swiperInstance.destroy(true, true);
            }

            swiperInstance = new Swiper('#examples .examples__swiper', {
                slidesPerView: 'auto',
                spaceBetween: 16,
                loop: false,
                grabCursor: true,
                navigation: {
                    nextEl: '#examples .examples__arrow--next',
                    prevEl: '#examples .examples__arrow--prev'
                },
                breakpoints: {
                    1024: {
                        slidesPerView: 4,
                        spaceBetween: 30,
                        allowTouchMove: true
                    }
                }
            });
        }

        function getActiveCategory() {
            const activeTab = document.querySelector('#project-categories .header__nav-link--active');
            return activeTab ? activeTab.getAttribute('data-category') : 'all';
        }

        function applyLanguage() {
            const nameKey = currentLang === 'en' ? 'nameEn' : 'nameRu';
            const categoryKey = currentLang === 'en' ? 'categoryEn' : 'categoryRu';
            allSlides.forEach((slide) => {
                const title = slide.querySelector('.example-card__title');
                const text = slide.querySelector('.example-card__text');
                if (title) {
                    title.textContent = slide.dataset[nameKey] || '';
                }
                if (text) {
                    text.textContent = slide.dataset[categoryKey] || '';
                }
            });
        }

        function renderProjects(category) {
            projectsContainer.innerHTML = '';
            const visible = category === 'all'
                ? allSlides
                : allSlides.filter((slide) => slide.dataset.category === category);
            if (visible.length === 0) {
                const noProjectsText = currentLang === 'ru'
                    ? 'Нет проектов в данной категории'
                    : 'No projects in this category';
                projectsContainer.innerHTML = \`<div class="loading-placeholder">\${noProjectsText}</div>\`;
                if (swiperInstance) {
                    swiperInstance.destroy(true, true);
                    swiperInstance = null;
                }
                return;
            }
            visible.forEach((slide) => projectsContainer.appendChild(slide));
            initSwiper();
        }

        categoryLinks.forEach((link) => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                categoryLinks.forEach((item) => item.classList.remove('header__nav-link--active'));
                link.classList.add('header__nav-link--active');
                renderProjects(getActiveCategory());
            });
        });

        document.querySelectorAll('.js-lang-switcher').forEach((switcher) => {
            switcher.addEventListener('click', (e) => {
                const btn = e.target.closest('.header__lang-btn');
                if (!btn) {
                    return;
                }
                const newLang = btn.getAttribute('data-lang');
                if (newLang && newLang !== currentLang) {
                    currentLang = newLang;
                    applyLanguage();
                    renderProjects(getActiveCategory());
                }
            });
        });

        applyLanguage();
        renderProjects(getActiveCategory());
    }

    `;
    fs.writeFileSync(sliderPath, `${slider.slice(0, start)}${replacement}${slider.slice(end)}`);
}

function patchLang() {
    const file = path.join(staticDir, 'assets/js/lang.js');
    let text = fs.readFileSync(file, 'utf8')
        .replaceAll('https://labkulture.design', '__PUBLIC_URL__')
        .replaceAll('https://labkeeper.io', '__PUBLIC_URL__')
        .replaceAll('labkeeper.io@gmail.com', '__CONTACT_EMAIL__');
    text = text.replace(
        'el.innerHTML = i18n[lang][key]; // используем innerHTML, так как некоторые строки содержат HTML-теги, например <br> или <span>',
        `el.innerHTML = i18n[lang][key]
                                    .replaceAll('__PUBLIC_URL__', window.__LABKEEPER_PUBLIC_URL || '')
                                    .replaceAll('__CONTACT_EMAIL__', window.__LABKEEPER_CONTACT_EMAIL || '')
                                    .replaceAll('__CONTACT_PHONE__', window.__LABKEEPER_CONTACT_PHONE || '');`,
    );
    fs.writeFileSync(file, text);
}

function patchSiteFiles() {
    const generator = path.join(staticDir, 'assets/js/generate-sitemap.js');
    if (fs.existsSync(generator)) {
        const text = fs.readFileSync(generator, 'utf8').replace(
            "const DOMAIN = 'https://labkeeper.io';",
            `const DOMAIN = process.env.LABKEEPER_PUBLIC_URL;
if (!DOMAIN) {
    throw new Error('LABKEEPER_PUBLIC_URL is not set');
}`,
        );
        fs.writeFileSync(generator, text);
    }
    const robots = path.join(staticDir, 'robots.txt');
    if (fs.existsSync(robots)) {
        const text = fs.readFileSync(robots, 'utf8')
            .replace(/\r?\nSitemap: https:\/\/labkeeper\.io\/sitemap\.xml\r?\n?/, '\n');
        fs.writeFileSync(robots, text);
    }
}

copyAssets();
patchSlider();
patchLandingScripts();
patchLang();
patchSiteFiles();
const articleCount = writeArticles();
writePage('index.html', (html) => {
    const marker = html.indexOf('<div class="swiper knowledge__swiper">');
    const wrapper = html.indexOf('<div class="swiper-wrapper">', marker);
    const withKnowledge = replaceInnerDiv(html, wrapper, knowledgeSlides);
    const projects = withKnowledge.indexOf('<div class="swiper-wrapper" id="projects-container">');
    return replaceInnerDiv(withKnowledge, projects, projectSlides);
});
writePage('blog.html', (html) => {
    const grid = html.indexOf('<div class="blog-grid" id="blog-grid">');
    return replaceInnerDiv(html, grid, blogCards);
});
for (const page of ['about.html', 'privacy.html', 'soglas.html', 'sogl_adv.html', 'sogl_ds.html', 'sogl_yam.html', 'oferta.html']) {
    writePage(page, (html) => html);
}

console.log(`Imported ${articleCount} articles and page templates into resources`);
