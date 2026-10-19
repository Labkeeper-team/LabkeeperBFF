/* eslint-disable */
const fs = require('fs');
const path = require('path');

const DOMAIN = 'https://labkeeper.io';
// Путь к src: файл лежит в src/assets/js -> поднимаемся на 2 уровня выше до src
const SRC_DIR = path.resolve(__dirname, '..', '..');
const BLOG_DIR = path.join(SRC_DIR, 'blog');
const SITEMAP_PATH = path.join(SRC_DIR, 'sitemap.xml');

// Основные страницы и их приоритеты
const STATIC_PAGES = [
    { url: '/', priority: '1.0', changefreq: 'weekly' },
    { url: '/about', priority: '0.8', changefreq: 'monthly' },
    { url: '/blog', priority: '0.9', changefreq: 'daily' },
];

const LEGAL_PAGES = [
    { url: '/privacy', priority: '0.3', changefreq: 'monthly' },
    { url: '/oferta', priority: '0.3', changefreq: 'monthly' },
    { url: '/soglas', priority: '0.3', changefreq: 'monthly' },
    { url: '/sogl_adv', priority: '0.3', changefreq: 'monthly' },
    { url: '/sogl_yam', priority: '0.3', changefreq: 'monthly' },
    { url: '/sogl_ds', priority: '0.3', changefreq: 'monthly' },
];

function generateSitemap() {
    const urls = [];

    // 1. Статические страницы
    STATIC_PAGES.forEach(page => urls.push(page));

    // 2. Все статьи из папки src/blog/
    if (fs.existsSync(BLOG_DIR)) {
        const blogFiles = fs.readdirSync(BLOG_DIR)
            .filter(file => file.endsWith('.html'))
            .sort();

        blogFiles.forEach(file => {
            const slug = file.replace('.html', '');
            urls.push({
                url: `/blog/${slug}`,
                priority: '0.8',
                changefreq: 'monthly'
            });
        });
    }

    // 3. Юридические страницы
    LEGAL_PAGES.forEach(page => urls.push(page));

    // Генерация XML
    let xml = '<?xml version="1.0" encoding="UTF-8"?>\n';
    xml += '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n';

    urls.forEach(item => {
        xml += '  <url>\n';
        xml += `    <loc>${DOMAIN}${item.url}</loc>\n`;
        xml += `    <changefreq>${item.changefreq}</changefreq>\n`;
        xml += `    <priority>${item.priority}</priority>\n`;
        xml += '  </url>\n';
    });

    xml += '</urlset>\n';

    fs.writeFileSync(SITEMAP_PATH, xml, 'utf8');
    console.log(`[Sitemap] Успешно сгенерирован ${SITEMAP_PATH} (${urls.length} страниц)`);
}

generateSitemap();
