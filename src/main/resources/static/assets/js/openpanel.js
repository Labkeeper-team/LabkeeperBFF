// Контракт вкладки общий с редактором: те же ключи sessionStorage.
(function () {
  var OPENPANEL_PROFILE_KEY = 'labkeeper.openpanel.profileId';
  var OPENPANEL_SESSION_STARTED_KEY = 'labkeeper.openpanel.sessionStarted';
  var OPENPANEL_ATTRIBUTION_KEY = 'labkeeper.openpanel.attribution';
  var OPENPANEL_FIRST_NAME_KEY = 'labkeeper.openpanel.firstName';

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
    'region'
  ];

  function configuredSecret(value) {
    if (typeof value !== 'string' || !value || value.indexOf('IO_LABKEEPER_FRONTEND_') === 0) {
      return '';
    }
    return value.trim();
  }

  function profileIdFromUuid(uuid) {
    return uuid.replace(/[^0-9A-Za-z]/g, '');
  }

  function readStoredProfileId() {
    try {
      var stored = sessionStorage.getItem(OPENPANEL_PROFILE_KEY);
      if (stored && /^[0-9A-Za-z]+$/.test(stored)) {
        return stored;
      }
    } catch {
      return '';
    }
    return '';
  }

  function writeStorage(key, value) {
    try {
      sessionStorage.setItem(key, value);
    } catch {
      // без хранилища редактор в этой вкладке не увидит профиль
    }
  }

  function newAnonymousDisplayName() {
    var suffix = crypto.getRandomValues(new Uint32Array(1))[0] % 1000000;
    return 'anonymous' + String(suffix).padStart(6, '0');
  }

  function readOrCreateGuestDisplayName() {
    try {
      var stored = sessionStorage.getItem(OPENPANEL_FIRST_NAME_KEY);
      if (stored && /^anonymous\d{6}$/.test(stored)) {
        return stored;
      }
    } catch {
      return newAnonymousDisplayName();
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
      var raw = sessionStorage.getItem(OPENPANEL_ATTRIBUTION_KEY);
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

  function sessionAlreadyStarted() {
    try {
      return sessionStorage.getItem(OPENPANEL_SESSION_STARTED_KEY) === '1';
    } catch {
      return false;
    }
  }

  function postOpenPanel(apiUrl, clientId, body) {
    var base = apiUrl.replace(/\/$/, '');
    fetch(base + '/track', {
      method: 'POST',
      headers: {
        'content-type': 'application/json',
        'openpanel-client-id': clientId,
        'openpanel-sdk-name': 'web',
        'openpanel-sdk-version': '1.4.1'
      },
      body: JSON.stringify(body),
      keepalive: true
    }).catch(function () {
      // отказ аналитики не должен всплывать на страницу
    });
  }

  function startOpenPanelSession() {
    var clientId = configuredSecret(window.__LABKEEPER_OPENPANEL_CLIENT_ID);
    var apiUrl = configuredSecret(window.__LABKEEPER_OPENPANEL_API_URL);
    if (!clientId || !apiUrl) {
      return;
    }

    var profileId = readStoredProfileId();
    if (!profileId) {
      profileId = profileIdFromUuid(crypto.randomUUID());
      writeStorage(OPENPANEL_PROFILE_KEY, profileId);
    }

    var fromUrl = campaignFromUrl();
    if (fromUrl) {
      writeStorage(OPENPANEL_ATTRIBUTION_KEY, JSON.stringify(fromUrl));
    }
    var attribution = fromUrl || readStoredAttribution();

    var identifyPayload = {
      profileId: profileId,
      firstName: readOrCreateGuestDisplayName()
    };
    if (attribution) {
      identifyPayload.properties = attribution;
    }
    postOpenPanel(apiUrl, clientId, {
      type: 'identify',
      payload: identifyPayload
    });

    if (sessionAlreadyStarted()) {
      return;
    }

    var properties = {
      __path: window.location.pathname,
      __title: document.title,
      __referrer: document.referrer
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
        properties: properties
      }
    });
    writeStorage(OPENPANEL_SESSION_STARTED_KEY, '1');
  }

  startOpenPanelSession();
})();
