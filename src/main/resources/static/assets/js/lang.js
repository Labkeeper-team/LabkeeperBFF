/**
 * Тексты страниц сервер отдаёт сам, на языке из cookie labkeeper_lang (без неё на русском).
 * Скрипт переключает язык и держит cookie вровень с localStorage: под тем же ключом
 * язык хранит редактор, и выбранный там язык должен доходить до сайта (GH-175)
 */
(function () {
    const KEY = 'labkeeper_lang';
    const LANGUAGES = ['ru', 'en', 'zh'];
    const YEAR_SECONDS = 60 * 60 * 24 * 365;

    const readStored = () => {
        try {
            return window.localStorage.getItem(KEY);
        } catch (e) {
            return null;
        }
    };

    const store = (lang) => {
        try {
            window.localStorage.setItem(KEY, lang);
        } catch (e) {
            // без хранилища редактор возьмёт свой язык
        }
    };

    const readCookie = () => {
        const match = document.cookie.match(/(?:^|;\s*)labkeeper_lang=([^;]*)/);
        return match ? match[1] : null;
    };

    /**
     * Перезагружает страницу на языке lang. Если cookie не сохранилась (их запретили),
     * сервер языка не узнает: тогда страница остаётся как есть и возвращается false
     */
    const switchTo = (lang) => {
        document.cookie = `${KEY}=${lang}; path=/; max-age=${YEAR_SECONDS}; SameSite=Lax`;
        if (readCookie() !== lang) {
            return false;
        }
        store(lang);
        window.location.reload();
        return true;
    };

    // Поисковые роботы исполняют скрипты с английским браузером (Googlebot — en-US).
    // Им сайт отдаёт русский, иначе в выдачу попал бы перевод вместо оригинала
    const ROBOT = /bot|crawl|spider|slurp|lighthouse/i;

    /**
     * Язык сайта по первому языку из настроек браузера: китайский и английский узнаём,
     * для остальных и для роботов — null, страница остаётся на русском
     */
    const browserLanguage = () => {
        const navigator = window.navigator || {};
        if (ROBOT.test(navigator.userAgent || '')) {
            return null;
        }
        const first =
            (navigator.languages && navigator.languages[0]) ||
            navigator.language;
        const code = String(first || '')
            .toLowerCase()
            .split('-')[0];
        return code === 'zh' || code === 'en' ? code : null;
    };

    const page = document.documentElement.lang;
    const stored = readStored();

    // Язык сменили в редакторе, а cookie ещё старая. После перезагрузки cookie совпадёт, второй не будет
    if (
        LANGUAGES.includes(stored) &&
        stored !== page &&
        readCookie() !== stored &&
        switchTo(stored)
    ) {
        return;
    }
    // Первый заход из китайского или английского браузера: язык ещё никто не выбирал,
    // поэтому страница переходит на язык браузера
    const detected = browserLanguage();
    if (
        !LANGUAGES.includes(stored) &&
        readCookie() === null &&
        detected !== null &&
        page !== detected &&
        switchTo(detected)
    ) {
        return;
    }
    // Свой язык сайт отдаёт редактору, только если тот ещё ничего не выбрал: без cookie
    // страница остаётся на русском, но выбор человека в редакторе не теряется
    if (!LANGUAGES.includes(stored)) {
        store(page);
    }

    document.addEventListener('DOMContentLoaded', () => {
        document.querySelectorAll('.js-lang-switcher button').forEach((btn) => {
            btn.addEventListener('click', () => {
                const lang = btn.getAttribute('data-lang');
                if (LANGUAGES.includes(lang) && lang !== page) {
                    switchTo(lang);
                }
            });
        });
    });
})();
