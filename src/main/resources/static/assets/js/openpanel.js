// Контракт гостя общий с редактором (src/web/session.ts в LabkeeperEditor): те же ключи
// localStorage и то же окно сессии. Пока с последнего события прошло не больше 30 минут,
// лендинг, редактор и все вкладки устройства идут одним гостем и одним стартом сессии.
(function () {
    var OPENPANEL_PROFILE_KEY = 'labkeeper.openpanel.profileId';
    var OPENPANEL_SESSION_STARTED_KEY = 'labkeeper.openpanel.sessionStarted';
    var OPENPANEL_ATTRIBUTION_KEY = 'labkeeper.openpanel.attribution';
    var OPENPANEL_FIRST_NAME_KEY = 'labkeeper.openpanel.firstName';
    var OPENPANEL_LAST_ACTIVITY_KEY = 'labkeeper.openpanel.lastActivity';
    // столько OpenPanel держит сессию без событий: после такой паузы гость начинается заново
    var SESSION_IDLE_MS = 30 * 60 * 1000;
    var GUEST_KEYS = [
        OPENPANEL_PROFILE_KEY,
        OPENPANEL_SESSION_STARTED_KEY,
        OPENPANEL_ATTRIBUTION_KEY,
        OPENPANEL_FIRST_NAME_KEY,
        OPENPANEL_LAST_ACTIVITY_KEY,
    ];

    var CAMPAIGN_PARAMS = [
        'utm_source',
        'utm_medium',
        'utm_campaign',
        'utm_content',
        'utm_term',
        'yclid',
        'campaign_id',
        'ad_id',
        'banner_id',
        'phrase_id',
        'source',
        'device',
        'region',
    ];

    function configuredSecret(value) {
        if (
            typeof value !== 'string' ||
            !value ||
            value.indexOf('IO_LABKEEPER_FRONTEND_') === 0
        ) {
            return '';
        }
        return value.trim();
    }

    function profileIdFromUuid(uuid) {
        return uuid.replace(/[^0-9A-Za-z]/g, '');
    }

    // Хранилище гостя, из которого уже убрано всё от истёкшей сессии. Время из будущего
    // тоже считается истёкшим: после перевода часов назад иначе гость жил бы лишние часы.
    function guestStorage() {
        try {
            var storage = window.localStorage;
            var lastActivity = Number(
                storage.getItem(OPENPANEL_LAST_ACTIVITY_KEY)
            );
            var idle = Date.now() - lastActivity;
            if (!(lastActivity > 0 && idle >= 0 && idle <= SESSION_IDLE_MS)) {
                GUEST_KEYS.forEach(function (key) {
                    storage.removeItem(key);
                });
            }
            return storage;
        } catch {
            // приватный режим не должен ронять страницу: гость останется только в этом запросе
            return null;
        }
    }

    function readStorage(key) {
        try {
            var storage = guestStorage();
            return storage ? storage.getItem(key) : null;
        } catch {
            return null;
        }
    }

    function writeStorage(key, value) {
        var storage = guestStorage();
        if (!storage) {
            return;
        }
        try {
            storage.setItem(key, value);
            // запись тоже продлевает окно: иначе следующее чтение сочло бы её истёкшей
            storage.setItem(OPENPANEL_LAST_ACTIVITY_KEY, String(Date.now()));
        } catch {
            // без хранилища редактор и другие вкладки не увидят профиль
        }
    }

    // каждое событие в OpenPanel продлевает окно гостя для всех вкладок и редактора
    function markActivity() {
        var storage = guestStorage();
        if (!storage) {
            return;
        }
        try {
            storage.setItem(OPENPANEL_LAST_ACTIVITY_KEY, String(Date.now()));
        } catch {
            // без хранилища окно не продлится, а событие всё равно уйдёт
        }
    }

    function readStoredProfileId() {
        var stored = readStorage(OPENPANEL_PROFILE_KEY);
        return stored && /^[0-9A-Za-z]+$/.test(stored) ? stored : '';
    }

    function newAnonymousDisplayName() {
        var suffix = crypto.getRandomValues(new Uint32Array(1))[0] % 1000000;
        return 'anonymous' + String(suffix).padStart(6, '0');
    }

    function readOrCreateGuestDisplayName() {
        var stored = readStorage(OPENPANEL_FIRST_NAME_KEY);
        if (stored && /^anonymous\d{6}$/.test(stored)) {
            return stored;
        }
        var name = newAnonymousDisplayName();
        writeStorage(OPENPANEL_FIRST_NAME_KEY, name);
        return name;
    }

    function campaignFromUrl() {
        var params = new URLSearchParams(window.location.search);
        var found = {};
        var any = false;
        CAMPAIGN_PARAMS.forEach(function (key) {
            var value = (params.get(key) || '').trim();
            if (!value) {
                return;
            }
            found[key] = value;
            any = true;
        });
        return any ? found : null;
    }

    function readStoredAttribution() {
        try {
            var raw = readStorage(OPENPANEL_ATTRIBUTION_KEY);
            if (!raw) {
                return null;
            }
            var parsed = JSON.parse(raw);
            if (!parsed || typeof parsed !== 'object') {
                return null;
            }
            var found = {};
            var any = false;
            CAMPAIGN_PARAMS.forEach(function (key) {
                var value = parsed[key];
                if (typeof value !== 'string' || !value.trim()) {
                    return;
                }
                found[key] = value.trim();
                any = true;
            });
            return any ? found : null;
        } catch {
            return null;
        }
    }

    // флаг ставит тот, кто первым отправил старт сессии гостя: лендинг или редактор
    function sessionAlreadyStarted() {
        return readStorage(OPENPANEL_SESSION_STARTED_KEY) === '1';
    }

    function postOpenPanel(apiUrl, clientId, body) {
        var base = apiUrl.replace(/\/$/, '');
        markActivity();
        fetch(base + '/track', {
            method: 'POST',
            headers: {
                'content-type': 'application/json',
                'openpanel-client-id': clientId,
                'openpanel-sdk-name': 'web',
                'openpanel-sdk-version': '1.4.1',
            },
            body: JSON.stringify(body),
            keepalive: true,
        }).catch(function () {
            // отказ аналитики не должен всплывать на страницу
        });
    }

    // Гость и метки кампании заводятся и без ключей OpenPanel: редактор подхватит их,
    // даже если на этом стенде аналитика лендинга не настроена.
    function prepareGuest() {
        var profileId = readStoredProfileId();
        if (!profileId) {
            profileId = profileIdFromUuid(crypto.randomUUID());
            writeStorage(OPENPANEL_PROFILE_KEY, profileId);
        }
        var firstName = readOrCreateGuestDisplayName();

        var fromUrl = campaignFromUrl();
        if (fromUrl) {
            writeStorage(OPENPANEL_ATTRIBUTION_KEY, JSON.stringify(fromUrl));
        }
        return {
            profileId: profileId,
            firstName: firstName,
            attribution: fromUrl || readStoredAttribution(),
        };
    }

    function startOpenPanelSession() {
        var guest = prepareGuest();
        var clientId = configuredSecret(window.__LABKEEPER_OPENPANEL_CLIENT_ID);
        var apiUrl = configuredSecret(window.__LABKEEPER_OPENPANEL_API_URL);
        if (!clientId || !apiUrl) {
            return;
        }

        var profileId = guest.profileId;
        var attribution = guest.attribution;
        var identifyPayload = {
            profileId: profileId,
            firstName: guest.firstName,
        };
        if (attribution) {
            identifyPayload.properties = attribution;
        }
        postOpenPanel(apiUrl, clientId, {
            type: 'identify',
            payload: identifyPayload,
        });

        if (sessionAlreadyStarted()) {
            return;
        }

        var properties = {
            __path: window.location.pathname,
            __title: document.title,
            __referrer: document.referrer,
        };
        if (attribution) {
            CAMPAIGN_PARAMS.forEach(function (key) {
                if (attribution[key]) {
                    properties[key] = attribution[key];
                }
            });
        }
        postOpenPanel(apiUrl, clientId, {
            type: 'track',
            payload: {
                name: '[L] Session started',
                profileId: profileId,
                properties: properties,
            },
        });
        writeStorage(OPENPANEL_SESSION_STARTED_KEY, '1');
    }

    startOpenPanelSession();
})();
