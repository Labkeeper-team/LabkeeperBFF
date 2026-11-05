/**
 * Тексты страниц сервер отдаёт сам, на языке из cookie labkeeper_lang (без неё на русском).
 * Скрипт переключает язык и держит cookie вровень с localStorage: под тем же ключом
 * язык хранит редактор, и выбранный там язык должен доходить до сайта (GH-175)
 */
(function () {
    const KEY = 'labkeeper_lang';
    const LANGUAGES = ['ru', 'en'];
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
