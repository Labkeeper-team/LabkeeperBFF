/* global Swiper */
document.addEventListener('DOMContentLoaded', () => {
    // 2. Реестр всех статей блога
    // =========================================================================
    const ALL_BLOG_ARTICLES = window.LABKEEPER_BLOG_ARTICLES || [];
    window.ALL_BLOG_ARTICLES = ALL_BLOG_ARTICLES;

    // =========================================================================
    // 3. Слайдер статей (внутренние страницы статей)
    // =========================================================================
    const audienceSwiperEl = document.querySelector('.audience__swiper');
    if (audienceSwiperEl) {
        const currentPath = window.location.pathname
            .replace(/\/+$/, '')
            .replace(/\.html$/, '');

        const eligibleArticles = ALL_BLOG_ARTICLES.filter((article) => {
            const articleNormalized = article.url
                .replace(/\/+$/, '')
                .replace(/\.html$/, '');
            return !currentPath.endsWith(articleNormalized);
        });

        const shuffle = (array) => {
            const arr = [...array];
            for (let i = arr.length - 1; i > 0; i--) {
                const j = Math.floor(Math.random() * (i + 1));
                [arr[i], arr[j]] = [arr[j], arr[i]];
            }
            return arr;
        };

        const randomArticles = shuffle(eligibleArticles).slice(0, 30);

        const swiperWrapper = audienceSwiperEl.querySelector('.swiper-wrapper');
        const hasServerSlides =
            swiperWrapper && swiperWrapper.querySelector('.swiper-slide');
        if (swiperWrapper && !hasServerSlides && randomArticles.length > 0) {
            swiperWrapper.innerHTML = randomArticles
                .map(
                    (article) => `
                <div class="swiper-slide">
                    <a href="${article.url}" class="audience-card">
                        <p class="audience-card__title">${article.title}</p>
                        <p class="audience-card__text">${article.text}</p>
                    </a>
                </div>
            `
                )
                .join('');
        }

        new Swiper(audienceSwiperEl, {
            slidesPerView: 1.18,
            spaceBetween: 16,
            loop: false,
            grabCursor: true,
            navigation: {
                nextEl: '.audience__arrow--next',
                prevEl: '.audience__arrow--prev',
            },
            breakpoints: {
                768: {
                    slidesPerView: 2,
                    spaceBetween: 20,
                },
                1024: {
                    slidesPerView: 3,
                    spaceBetween: 24,
                    allowTouchMove: true,
                },
            },
        });
    }

    // =========================================================================
    // 4. Бегущая строка блога (если присутствует в DOM)
    // =========================================================================
    const blogTickerTrack = document.querySelector('.js-blog-ticker-track');
    const blogTickerViewport = document.querySelector('.blog-ticker__viewport');

    if (blogTickerTrack) {
        let totalCount = 0;
        const existingItems =
            blogTickerTrack.querySelectorAll('.blog-ticker__item');
        if (existingItems.length > 0) {
            const itemsHtml = blogTickerTrack.innerHTML;
            blogTickerTrack.innerHTML = itemsHtml + itemsHtml;
            totalCount = existingItems.length * 2;
        } else if (
            Array.isArray(ALL_BLOG_ARTICLES) &&
            ALL_BLOG_ARTICLES.length > 0
        ) {
            const latestArticles = ALL_BLOG_ARTICLES.slice(-50);
            const itemsHtml = latestArticles
                .map(
                    (article) => `
                <a href="${article.url}" class="blog-ticker__item">
                    <span class="blog-ticker__text">${article.title}</span>
                    <span class="blog-ticker__arrow">→</span>
                </a>
            `
                )
                .join('');

            const repeatCount = latestArticles.length < 10 ? 4 : 2;
            let trackHtml = '';
            for (let i = 0; i < repeatCount; i++) {
                trackHtml += itemsHtml;
            }

            blogTickerTrack.innerHTML = trackHtml;
            totalCount = latestArticles.length * repeatCount;
        }

        const duration = Math.max(35, Math.round(totalCount * 2.2));
        blogTickerTrack.style.animationDuration = `${duration}s`;

        if (blogTickerViewport) {
            let isDragging = false;
            let startX = 0;
            let startY = 0;
            let initialTranslateX = 0;
            let isHorizontalSwipe = false;
            let dragDistance = 0;

            const getCurrentTranslateX = () => {
                const style = window.getComputedStyle(blogTickerTrack);
                const transform = style.transform || style.webkitTransform;
                if (!transform || transform === 'none') {
                    return 0;
                }
                try {
                    const matrix = new window.DOMMatrixReadOnly(transform);
                    return matrix.m41;
                    // eslint-disable-next-line no-unused-vars
                } catch (err) {
                    const values = transform.match(/matrix.*\((.+)\)/);
                    if (values && values[1]) {
                        const parts = values[1].split(', ');
                        return parseFloat(parts[4]) || 0;
                    }
                    return 0;
                }
            };

            const applyAnimationAtPosition = (currentX) => {
                const halfWidth = blogTickerTrack.scrollWidth / 2;
                if (!halfWidth) {
                    return;
                }
                let normalizedX = currentX % halfWidth;
                if (normalizedX > 0) {
                    normalizedX -= halfWidth;
                }

                const fraction = (normalizedX + halfWidth) / halfWidth;
                const delay = -(fraction * duration);

                blogTickerTrack.style.animation = 'none';
                void blogTickerTrack.offsetWidth;
                blogTickerTrack.style.animation = `blogTickerLtr ${duration}s linear infinite`;
                blogTickerTrack.style.animationDelay = `${delay}s`;
                blogTickerTrack.style.transform = '';
            };

            blogTickerViewport.addEventListener(
                'touchstart',
                (e) => {
                    if (e.touches.length !== 1) {
                        return;
                    }
                    isDragging = true;
                    isHorizontalSwipe = false;
                    dragDistance = 0;
                    startX = e.touches[0].clientX;
                    startY = e.touches[0].clientY;
                    initialTranslateX = getCurrentTranslateX();

                    blogTickerTrack.style.animation = 'none';
                    blogTickerTrack.style.transform = `translateX(${initialTranslateX}px)`;
                },
                { passive: true }
            );

            blogTickerViewport.addEventListener(
                'touchmove',
                (e) => {
                    if (!isDragging || e.touches.length !== 1) {
                        return;
                    }
                    const currentX = e.touches[0].clientX;
                    const currentY = e.touches[0].clientY;
                    const diffX = currentX - startX;
                    const diffY = currentY - startY;

                    if (!isHorizontalSwipe) {
                        if (
                            Math.abs(diffX) > 8 &&
                            Math.abs(diffX) > Math.abs(diffY)
                        ) {
                            isHorizontalSwipe = true;
                        } else if (Math.abs(diffY) > 8) {
                            isDragging = false;
                            applyAnimationAtPosition(initialTranslateX);
                            return;
                        }
                    }

                    if (isHorizontalSwipe) {
                        dragDistance = diffX;
                        const halfWidth = blogTickerTrack.scrollWidth / 2;
                        let targetX = initialTranslateX + diffX;
                        if (halfWidth > 0) {
                            while (targetX > 0) {
                                targetX -= halfWidth;
                            }
                            while (targetX < -halfWidth) {
                                targetX += halfWidth;
                            }
                        }
                        blogTickerTrack.style.transform = `translateX(${targetX}px)`;
                    }
                },
                { passive: true }
            );

            const handleTouchEnd = () => {
                if (!isDragging) {
                    return;
                }
                isDragging = false;
                const finalX = getCurrentTranslateX();
                applyAnimationAtPosition(finalX);
            };

            blogTickerViewport.addEventListener('touchend', handleTouchEnd, {
                passive: true,
            });
            blogTickerViewport.addEventListener('touchcancel', handleTouchEnd, {
                passive: true,
            });

            blogTickerTrack.addEventListener(
                'click',
                (e) => {
                    if (Math.abs(dragDistance) > 10) {
                        e.preventDefault();
                        e.stopPropagation();
                    }
                },
                true
            );
        }
    }

    // =========================================================================
    // 5. Логика табов Преимуществ (Плавный Cross-fade)
    // =========================================================================
    const tabButtons = document.querySelectorAll('.features__tab-btn');
    const tabSlides = document.querySelectorAll('.features__slide');

    if (tabButtons.length > 0 && tabSlides.length > 0) {
        const setActiveTab = (index) => {
            tabButtons.forEach((btn, i) => {
                const isActive = i === index;
                btn.classList.toggle('is-active', isActive);
                btn.setAttribute('aria-selected', isActive ? 'true' : 'false');
            });

            tabSlides.forEach((slide, i) => {
                slide.classList.toggle('is-active', i === index);
            });
        };

        tabButtons.forEach((btn, index) => {
            btn.addEventListener('click', () => {
                setActiveTab(index);
            });
        });

        setActiveTab(0);
    }

    // =========================================================================
    // 6. Слайдеры «Шаблоны» и «База знаний»
    // =========================================================================
    function initCatalogSwiper(sectionId) {
        const section = document.getElementById(sectionId);
        if (!section) {
            return;
        }
        const twoRows = sectionId === 'templates';
        const swiper = new Swiper(`#${sectionId} .knowledge__swiper`, {
            slidesPerView: twoRows ? 1 : 1.18,
            spaceBetween: 16,
            loop: false,
            grabCursor: true,
            observer: true,
            observeParents: true,
            ...(twoRows ? { grid: { rows: 2, fill: 'row' } } : {}),
            navigation: {
                nextEl: `#${sectionId} .knowledge__arrow--next`,
                prevEl: `#${sectionId} .knowledge__arrow--prev`,
            },
            breakpoints: twoRows
                ? {
                      768: {
                          slidesPerView: 2,
                          spaceBetween: 20,
                          grid: { rows: 2, fill: 'row' },
                      },
                      1024: {
                          slidesPerView: 3,
                          spaceBetween: 24,
                          allowTouchMove: true,
                          grid: { rows: 2, fill: 'row' },
                      },
                  }
                : {
                      768: {
                          slidesPerView: 2,
                          spaceBetween: 20,
                      },
                      1024: {
                          slidesPerView: 3,
                          spaceBetween: 24,
                          allowTouchMove: true,
                      },
                  },
        });

        const update = () => {
            swiper.update();
            if (swiper.navigation) {
                swiper.navigation.update();
            }
        };
        window.addEventListener('load', update);
        setTimeout(update, 300);
    }

    initCatalogSwiper('templates');
    initCatalogSwiper('knowledge');
});
