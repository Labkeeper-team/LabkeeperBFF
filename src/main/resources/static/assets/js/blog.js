document.addEventListener('DOMContentLoaded', () => {
    const article = document.querySelector('.article-content');
    if (!article || typeof window.renderMathInElement !== 'function') return;

    window.renderMathInElement(article, {
        delimiters: [
            { left: '$$', right: '$$', display: true },
            { left: '\\[', right: '\\]', display: true },
            { left: '\\(', right: '\\)', display: false },
            { left: '$', right: '$', display: false }
        ],
        ignoredTags: ['script', 'noscript', 'style', 'textarea', 'pre', 'code', 'option'],
        trust: false,
        throwOnError: false,
        maxExpand: 1000
    });
    article.dataset.mathReady = 'true';
});
