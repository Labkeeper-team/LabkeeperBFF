/* global Swiper */
document.addEventListener('DOMContentLoaded', () => {
    // =========================================================================
    // 1. Слайдер проектов на главной странице (#examples)
    // =========================================================================
    const projectsContainer = document.getElementById('projects-container');
    if (projectsContainer) {
        const API_URL = '/api/v4/public/preview';
        const categoryLinks = document.querySelectorAll('#project-categories .header__nav-link');

        let allProjects = [];
        let swiperInstance = null;

        const activeLangBtn = document.querySelector('.js-lang-switcher .header__lang-btn--active');
        let currentLang = activeLangBtn ? activeLangBtn.getAttribute('data-lang') : (document.documentElement.lang || 'ru');

        const backupData = {
            projects: [
                { id: '092bacc5-c98a-4709-b67d-7455fd4588e4', name: { ru: 'Измерение интенсивности радиационного фона', en: 'Radiation background intensity measurement' }, category: 'lab', projectType: 'markdown', logoUrl: 'https://labkeeper.io/assets/img/case1.png' },
                { id: '1acb4436-d4d6-4f40-9fc2-92039adff180', name: { ru: 'Статистическая обработка результатов многократных измерений', en: 'Statistical processing of multiple measurement results' }, category: 'lab', projectType: 'markdown', logoUrl: 'https://labkeeper.io/assets/img/case2.png' },
                { id: 'c4c71322-14da-4394-b546-2ed0ace1ebfa', name: { ru: 'Определение систематических и случайных погрешностей', en: 'Determination of systematic and random errors' }, category: 'lab', projectType: 'markdown', logoUrl: 'https://labkeeper.io/assets/img/case4.png' },
                { id: 'd24a814f-ff8f-4b8c-8267-f824cf8c7b6b', name: { ru: 'Изучение линейной временной модальной логики', en: 'Study of linear temporal modal logic' }, category: 'diploma', projectType: 'latex', logoUrl: 'https://labkeeper.io/assets/img/case3.png' }
            ]
        };

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

        function renderProjects(projects) {
            projectsContainer.innerHTML = '';

            if (!projects || projects.length === 0) {
                const noProjectsText = currentLang === 'ru' ? 'Нет проектов в данной категории' : 'No projects in this category';
                projectsContainer.innerHTML = `<div class="loading-placeholder">${noProjectsText}</div>`;
                return;
            }

            projects.forEach((project) => {
                const projectName = project.name?.[currentLang] || project.name?.ru || project.name?.en || 'Untitled';

                const hasLogo = Boolean(project.logoUrl);
                const imgSrc = hasLogo ? project.logoUrl : 'assets/img/logo.svg';
                const imgClass = hasLogo ? 'example-card__image' : 'example-card__image example-card__image--placeholder';

                let categoryText = '';
                if (project.category === 'lab') {
                    categoryText = currentLang === 'ru' ? 'Лабораторная работа' : 'Laboratory work';
                } else if (project.category === 'diploma') {
                    categoryText = currentLang === 'ru' ? 'Дипломный проект' : 'Diploma project';
                } else {
                    categoryText = project.category || '';
                }

                const cardHtml = `
                    <div class="swiper-slide">
                        <a href="https://labkeeper.io/project/${project.id}" class="example-card" target="_blank">
                            <div class="example-card__visual">
                                <img src="${imgSrc}" alt="${projectName}" class="${imgClass}" loading="lazy">
                                <img src="assets/img/target.svg" alt="open" class="example-card__external-icon" width="16" height="16">
                            </div>
                            <div class="example-card__content">
                                <h3 class="example-card__title">${projectName}</h3>
                                <p class="example-card__text">${categoryText}</p>
                            </div>
                        </a>
                    </div>
                `;
                projectsContainer.insertAdjacentHTML('beforeend', cardHtml);
            });

            initSwiper();
        }

        function updateSliderDisplay() {
            const selectedCategory = getActiveCategory();
            if (selectedCategory === 'all') {
                renderProjects(allProjects);
            } else {
                const filtered = allProjects.filter((project) => project.category === selectedCategory);
                renderProjects(filtered);
            }
        }

        async function fetchProjects() {
            try {
                const response = await fetch(API_URL);
                if (!response.ok) {
                    throw new Error(`Status: ${response.status}`);
                }

                const data = await response.json();
                allProjects = data.projects || [];
                renderProjects(allProjects);
            } catch (error) {
                console.warn('Локальный тест (или ошибка CORS). Используются резервные данные:', error);
                allProjects = backupData.projects;
                renderProjects(allProjects);
            }
        }

        categoryLinks.forEach((link) => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                categoryLinks.forEach((l) => l.classList.remove('header__nav-link--active'));
                link.classList.add('header__nav-link--active');
                updateSliderDisplay();
            });
        });

        const langSwitchers = document.querySelectorAll('.js-lang-switcher');
        langSwitchers.forEach((switcher) => {
            switcher.addEventListener('click', (e) => {
                const btn = e.target.closest('.header__lang-btn');
                if (!btn) {
                    return;
                }

                const newLang = btn.getAttribute('data-lang');
                if (newLang && newLang !== currentLang) {
                    currentLang = newLang;
                    updateSliderDisplay();
                }
            });
        });

        fetchProjects();
    }

    // =========================================================================
    // 2. Реестр всех статей блога
    // =========================================================================
    const ALL_BLOG_ARTICLES = window.LABKEEPER_BLOG_ARTICLES || [];
    window.ALL_BLOG_ARTICLES = ALL_BLOG_ARTICLES;

    // =========================================================================
    // 3. Слайдер статей (внутренние страницы статей)
    // =========================================================================
    const audienceSwiperEl = document.querySelector('.audience__swiper');
    if (audienceSwiperEl) {
        const currentPath = window.location.pathname.replace(/\/+$/, '').replace(/\.html$/, '');

        const eligibleArticles = ALL_BLOG_ARTICLES.filter((article) => {
            const articleNormalized = article.url.replace(/\/+$/, '').replace(/\.html$/, '');
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
        if (swiperWrapper && randomArticles.length > 0) {
            swiperWrapper.innerHTML = randomArticles.map((article) => `
                <div class="swiper-slide">
                    <a href="${article.url}" class="audience-card">
                        <p class="audience-card__title">${article.title}</p>
                        <p class="audience-card__text">${article.text}</p>
                    </a>
                </div>
            `).join('');
        }

        new Swiper(audienceSwiperEl, {
            slidesPerView: 1.18,
            spaceBetween: 16,
            loop: false,
            grabCursor: true,
            navigation: {
                nextEl: '.audience__arrow--next',
                prevEl: '.audience__arrow--prev'
            },
            breakpoints: {
                768: {
                    slidesPerView: 2,
                    spaceBetween: 20
                },
                1024: {
                    slidesPerView: 3,
                    spaceBetween: 24,
                    allowTouchMove: true
                }
            }
        });
    }

    // =========================================================================
    // 4. Бегущая строка блога (если присутствует в DOM)
    // =========================================================================
    const blogTickerTrack = document.querySelector('.js-blog-ticker-track');
    const blogTickerViewport = document.querySelector('.blog-ticker__viewport');

    if (blogTickerTrack) {
        let totalCount = 0;
        const existingItems = blogTickerTrack.querySelectorAll('.blog-ticker__item');
        if (existingItems.length > 0) {
            const itemsHtml = blogTickerTrack.innerHTML;
            blogTickerTrack.innerHTML = itemsHtml + itemsHtml;
            totalCount = existingItems.length * 2;
        } else if (Array.isArray(ALL_BLOG_ARTICLES) && ALL_BLOG_ARTICLES.length > 0) {
            const latestArticles = ALL_BLOG_ARTICLES.slice(-50);
            const itemsHtml = latestArticles.map((article) => `
                <a href="${article.url}" class="blog-ticker__item">
                    <span class="blog-ticker__text">${article.title}</span>
                    <span class="blog-ticker__arrow">→</span>
                </a>
            `).join('');

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

            blogTickerViewport.addEventListener('touchstart', (e) => {
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
            }, { passive: true });

            blogTickerViewport.addEventListener('touchmove', (e) => {
                if (!isDragging || e.touches.length !== 1) {
                    return;
                }
                const currentX = e.touches[0].clientX;
                const currentY = e.touches[0].clientY;
                const diffX = currentX - startX;
                const diffY = currentY - startY;

                if (!isHorizontalSwipe) {
                    if (Math.abs(diffX) > 8 && Math.abs(diffX) > Math.abs(diffY)) {
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
            }, { passive: true });

            const handleTouchEnd = () => {
                if (!isDragging) {
                    return;
                }
                isDragging = false;
                const finalX = getCurrentTranslateX();
                applyAnimationAtPosition(finalX);
            };

            blogTickerViewport.addEventListener('touchend', handleTouchEnd, { passive: true });
            blogTickerViewport.addEventListener('touchcancel', handleTouchEnd, { passive: true });

            blogTickerTrack.addEventListener('click', (e) => {
                if (Math.abs(dragDistance) > 10) {
                    e.preventDefault();
                    e.stopPropagation();
                }
            }, true);
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
    // 6. Инициализация слайдера «База знаний» (#knowledge)
    // =========================================================================
    const knowledgeSection = document.getElementById('knowledge');
    if (knowledgeSection) {
        const knowledgeSwiper = new Swiper('#knowledge .knowledge__swiper', {
            slidesPerView: 1.18,
            spaceBetween: 16,
            loop: false,
            grabCursor: true,
            observer: true,
            observeParents: true,
            navigation: {
                nextEl: '#knowledge .knowledge__arrow--next',
                prevEl: '#knowledge .knowledge__arrow--prev'
            },
            breakpoints: {
                768: {
                    slidesPerView: 2,
                    spaceBetween: 20
                },
                1024: {
                    slidesPerView: 3,
                    spaceBetween: 24,
                    allowTouchMove: true
                }
            }
        });

        window.addEventListener('load', () => {
            knowledgeSwiper.update();
            if (knowledgeSwiper.navigation) {
                knowledgeSwiper.navigation.update();
            }
        });

        setTimeout(() => {
            knowledgeSwiper.update();
            if (knowledgeSwiper.navigation) {
                knowledgeSwiper.navigation.update();
            }
        }, 300);
    }
});