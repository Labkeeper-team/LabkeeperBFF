// openpanel.js исполняется в отдельном контексте vm: так у каждой «страницы» своё окно,
// а общий localStorage изображает одно устройство с несколькими вкладками.
import assert from 'node:assert/strict';
import { webcrypto } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { test } from 'node:test';
import { fileURLToPath } from 'node:url';
import vm from 'node:vm';

const SOURCE = readFileSync(
    join(dirname(fileURLToPath(import.meta.url)), '../../../main/resources/static/assets/js/openpanel.js'),
    'utf8'
);

const PROFILE_KEY = 'labkeeper.openpanel.profileId';
const SESSION_STARTED_KEY = 'labkeeper.openpanel.sessionStarted';
const ATTRIBUTION_KEY = 'labkeeper.openpanel.attribution';
const FIRST_NAME_KEY = 'labkeeper.openpanel.firstName';
const LAST_ACTIVITY_KEY = 'labkeeper.openpanel.lastActivity';
const MINUTE = 60 * 1000;

class MemoryStorage {
    constructor(values = {}) {
        this.values = new Map(Object.entries(values));
    }
    getItem(key) {
        return this.values.has(key) ? this.values.get(key) : null;
    }
    setItem(key, value) {
        this.values.set(key, String(value));
    }
    removeItem(key) {
        this.values.delete(key);
    }
}

/** То, что оставляет редактор или соседняя вкладка лендинга после последнего события */
function deviceWithGuest(idleMs, extra = {}) {
    return new MemoryStorage({
        [PROFILE_KEY]: 'editorguest',
        [FIRST_NAME_KEY]: 'anonymous000042',
        [SESSION_STARTED_KEY]: '1',
        [LAST_ACTIVITY_KEY]: String(Date.now() - idleMs),
        ...extra
    });
}

function openPage({ storage = new MemoryStorage(), tabValues = {}, search = '', pathname = '/', configured = true, windowOverrides = {}, pageCrypto = webcrypto } = {}) {
    const requests = [];
    const listeners = [];
    const sessionStorage = new MemoryStorage(tabValues);
    const window = {
        localStorage: storage,
        sessionStorage,
        location: { search, pathname, origin: 'https://labkeeper.io', href: `https://labkeeper.io${pathname}${search}` },
        __LABKEEPER_OPENPANEL_CLIENT_ID: configured ? 'client-1' : 'IO_LABKEEPER_FRONTEND_OPENPANEL_CLIENT_ID',
        __LABKEEPER_OPENPANEL_API_URL: configured ? 'https://openpanel.example/api/' : 'IO_LABKEEPER_FRONTEND_OPENPANEL_API_URL'
    };
    // геттеры переносим как есть: spread вызвал бы их ещё до запуска страницы
    Object.defineProperties(window, Object.getOwnPropertyDescriptors(windowOverrides));
    vm.runInNewContext(SOURCE, {
        window,
        sessionStorage,
        document: {
            title: 'Labkeeper',
            referrer: '',
            addEventListener(type, listener) {
                listeners.push({ type, listener });
            }
        },
        crypto: pageCrypto,
        URL,
        URLSearchParams,
        fetch(url, init) {
            requests.push({ url, body: JSON.parse(init.body) });
            return Promise.resolve({});
        }
    });
    return {
        requests,
        // щелчок по ссылке страницы: attributes как у тега, button как у мыши
        click(href, { type = 'click', button = 0, attributes = [] } = {}) {
            const link = {
                getAttribute: name => (name === 'href' ? href : null),
                hasAttribute: name => attributes.includes(name)
            };
            const target = { closest: () => link };
            listeners.filter(entry => entry.type === type).forEach(entry => entry.listener({ type, button, target }));
        },
        fromArticle: () => requests.filter(request => request.body.type === 'track' && request.body.payload.name === '[L] Editor opened from article').map(request => request.body.payload),
        identify: requests.filter(request => request.body.type === 'identify').map(request => request.body.payload),
        starts: requests.filter(request => request.body.type === 'track' && request.body.payload.name === '[L] Session started').map(request => request.body.payload)
    };
}

test('a first visit creates a guest, names it and starts the session', () => {
    const storage = new MemoryStorage();

    const page = openPage({ storage });

    const profileId = storage.getItem(PROFILE_KEY);
    assert.match(profileId, /^[0-9A-Za-z]+$/);
    assert.deepEqual(page.identify, [{ profileId, firstName: storage.getItem(FIRST_NAME_KEY) }]);
    assert.match(storage.getItem(FIRST_NAME_KEY), /^anonymous\d{6}$/);
    assert.equal(page.starts.length, 1);
    assert.equal(page.starts[0].profileId, profileId);
    assert.equal(storage.getItem(SESSION_STARTED_KEY), '1');
    assert.equal(page.requests[0].url, 'https://openpanel.example/api/track');
});

// стенд браузерных тестов открывает лендинг по http, а там у crypto нет randomUUID
test('a page outside a secure context still creates the guest', () => {
    const insecureCrypto = { getRandomValues: array => webcrypto.getRandomValues(array) };

    for (const configured of [false, true]) {
        const storage = new MemoryStorage();

        const page = openPage({ storage, configured, pageCrypto: insecureCrypto });

        assert.match(storage.getItem(PROFILE_KEY), /^[0-9a-f]{12}4[0-9a-f]{3}[89ab][0-9a-f]{15}$/);
        assert.equal(page.starts.length, configured ? 1 : 0);
    }
});

// случай 4 из GH-197: возврат из редактора на лендинг в той же или другой вкладке
test('the guest left by the editor is continued without a second start', () => {
    const storage = deviceWithGuest(5 * MINUTE);

    const page = openPage({ storage });

    assert.deepEqual(page.identify, [{ profileId: 'editorguest', firstName: 'anonymous000042' }]);
    assert.equal(page.starts.length, 0);
});

// случаи 1 и 3: вкладки одного устройства делят гостя, sessionStorage для этого не нужен
test('another tab of the device keeps the guest and does not start again', () => {
    const storage = new MemoryStorage();
    const first = openPage({ storage });

    const second = openPage({ storage });

    assert.equal(first.starts.length, 1);
    assert.equal(second.starts.length, 0);
    assert.deepEqual(second.identify, first.identify);
});

// прежний контракт держал гостя в sessionStorage вкладки, переносить его не нужно
test('a guest from the old tab storage is not picked up', () => {
    const page = openPage({ tabValues: { [PROFILE_KEY]: 'oldtabguest', [SESSION_STARTED_KEY]: '1' } });

    assert.notEqual(page.identify[0].profileId, 'oldtabguest');
    assert.equal(page.starts.length, 1);
});

test('within thirty minutes of the last event the guest is the same', () => {
    const storage = deviceWithGuest(29 * MINUTE);

    const page = openPage({ storage });

    assert.equal(page.identify[0].profileId, 'editorguest');
    assert.equal(page.starts.length, 0);
});

// после 30 минут тишины OpenPanel начинает новую сессию, и гость с ней новый
test('after a longer pause everything of the guest expires', () => {
    const storage = deviceWithGuest(31 * MINUTE, {
        [ATTRIBUTION_KEY]: JSON.stringify({ utm_campaign: 'spring' })
    });

    const page = openPage({ storage });

    const profileId = storage.getItem(PROFILE_KEY);
    assert.notEqual(profileId, 'editorguest');
    assert.notEqual(storage.getItem(FIRST_NAME_KEY), 'anonymous000042');
    assert.equal(storage.getItem(ATTRIBUTION_KEY), null);
    assert.deepEqual(page.identify, [{ profileId, firstName: storage.getItem(FIRST_NAME_KEY) }]);
    assert.equal(page.starts.length, 1);
    assert.equal(page.starts[0].profileId, profileId);
    assert.equal(page.starts[0].properties.utm_campaign, undefined);
});

test('a guest without the activity time is not reused', () => {
    const storage = new MemoryStorage({ [PROFILE_KEY]: 'editorguest', [SESSION_STARTED_KEY]: '1' });

    const page = openPage({ storage });

    assert.notEqual(page.identify[0].profileId, 'editorguest');
    assert.equal(page.starts.length, 1);
});

test('activity from the future counts as expired', () => {
    const storage = deviceWithGuest(-5 * MINUTE);

    const page = openPage({ storage });

    assert.notEqual(page.identify[0].profileId, 'editorguest');
});

test('every request moves the window forward', () => {
    const storage = deviceWithGuest(29 * MINUTE);
    const before = Date.now();

    openPage({ storage });

    assert.ok(Number(storage.getItem(LAST_ACTIVITY_KEY)) >= before);
});

test('the campaign from the address replaces the stored one and reaches the events', () => {
    const storage = deviceWithGuest(MINUTE, {
        [ATTRIBUTION_KEY]: JSON.stringify({ utm_campaign: 'old' })
    });
    storage.removeItem(SESSION_STARTED_KEY);

    const page = openPage({ storage, search: '?utm_source=yandex&utm_campaign=spring&ignored=1' });

    const campaign = { utm_source: 'yandex', utm_campaign: 'spring' };
    assert.deepEqual(JSON.parse(storage.getItem(ATTRIBUTION_KEY)), campaign);
    assert.deepEqual(page.identify[0].properties, campaign);
    assert.equal(page.starts[0].properties.utm_campaign, 'spring');
    assert.equal(page.starts[0].properties.ignored, undefined);
});

// без ключей OpenPanel на стенде гость всё равно нужен редактору
test('without the keys the guest is prepared for the editor and nothing is sent', () => {
    const storage = new MemoryStorage();

    const page = openPage({ storage, configured: false, search: '?yclid=click-1' });

    assert.equal(page.requests.length, 0);
    assert.match(storage.getItem(PROFILE_KEY), /^[0-9A-Za-z]+$/);
    assert.match(storage.getItem(FIRST_NAME_KEY), /^anonymous\d{6}$/);
    assert.deepEqual(JSON.parse(storage.getItem(ATTRIBUTION_KEY)), { yclid: 'click-1' });
    assert.equal(storage.getItem(SESSION_STARTED_KEY), null);
});

test('an inaccessible storage does not break the page', () => {
    const page = openPage({
        windowOverrides: {
            get localStorage() {
                throw new Error('SecurityError');
            }
        }
    });

    assert.match(page.identify[0].profileId, /^[0-9A-Za-z]+$/);
    assert.match(page.identify[0].firstName, /^anonymous\d{6}$/);
    assert.equal(page.starts.length, 1);
});

test('a storage that refuses writes does not break the page', () => {
    const storage = new MemoryStorage();
    storage.setItem = () => {
        throw new Error('QuotaExceededError');
    };

    const page = openPage({ storage });

    assert.match(page.identify[0].profileId, /^[0-9A-Za-z]+$/);
    assert.equal(page.starts.length, 1);
});

// контракт с редактором: id и имя проверяются одинаково с обеих сторон
test('a broken guest id or name is replaced, not sent', () => {
    const storage = deviceWithGuest(MINUTE, {
        [PROFILE_KEY]: 'bad id!',
        [FIRST_NAME_KEY]: 'Anonymous'
    });

    const page = openPage({ storage });

    const profileId = storage.getItem(PROFILE_KEY);
    assert.notEqual(profileId, 'bad id!');
    assert.match(profileId, /^[0-9A-Za-z]+$/);
    assert.match(page.identify[0].firstName, /^anonymous\d{6}$/);
});

// GH-181: статья отдаётся с no-referrer, и переход в редактор отмечает она сама
const ARTICLE = '/blog/indirect-measurement-error-calculation-lab-report';
const EXAMPLE = '/project/default?open=markdown&markdown=%23+Lab';

test('a click on the example of an article tells which article led to the editor', () => {
    const storage = new MemoryStorage();
    const page = openPage({ storage, pathname: ARTICLE });

    page.click(EXAMPLE, { attributes: ['data-blog-example'] });

    assert.deepEqual(page.fromArticle(), [
        {
            name: '[L] Editor opened from article',
            profileId: storage.getItem(PROFILE_KEY),
            properties: {
                __path: ARTICLE,
                article: 'indirect-measurement-error-calculation-lab-report',
                link: 'example'
            }
        }
    ]);
    assert.equal(page.requests.at(-1).url, 'https://openpanel.example/api/track');
});

test('other editor links of an article are told apart', () => {
    const page = openPage({ pathname: ARTICLE });

    page.click('/project/default');
    page.click('/project/default?open=login');
    page.click('/projects');

    assert.deepEqual(
        page.fromArticle().map(event => event.properties.link),
        ['editor', 'login', 'projects']
    );
});

test('a middle click opens a tab and counts, a right click does not', () => {
    const page = openPage({ pathname: ARTICLE });

    page.click(EXAMPLE, { type: 'auxclick', button: 1 });
    page.click(EXAMPLE, { type: 'auxclick', button: 2 });

    assert.equal(page.fromArticle().length, 1);
});

test('links outside the editor and pages outside the blog send nothing', () => {
    const article = openPage({ pathname: ARTICLE });
    article.click('/blog');
    article.click('/tokens');
    article.click('https://example.com/project/default');

    const home = openPage({ pathname: '/' });
    home.click(EXAMPLE);

    const blog = openPage({ pathname: '/blog' });
    blog.click(EXAMPLE);

    assert.equal(article.fromArticle().length, 0);
    assert.equal(home.fromArticle().length, 0);
    assert.equal(blog.fromArticle().length, 0);
});

test('without the keys a transition from an article sends nothing', () => {
    const page = openPage({ pathname: ARTICLE, configured: false });

    page.click(EXAMPLE, { attributes: ['data-blog-example'] });

    assert.equal(page.requests.length, 0);
});

