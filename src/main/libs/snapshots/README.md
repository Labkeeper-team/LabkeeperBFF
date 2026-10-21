# Снимки главной страницы

Нужны запущенный Frontend и Docker.

```shell
gradlew bootRun
```

Из `src/main/libs`:

```shell
npm run snapshots
```

Команда сравнивает главную с картинками в `snapshots/__screenshots__/`. Шесть профилей: Windows Chrome, Windows Edge, macOS Safari, iPhone Safari, Android Chrome, Windows Firefox. Их рисует образ Playwright (Chromium, Firefox и WebKit), не установленные системы и телефоны.

Адрес по умолчанию — `http://localhost:8080`. Другой сервер:

```shell
$env:FRONTEND_URL = "http://localhost:9090"
npm run snapshots
```

Записать новые эталоны:

```shell
node snapshots/run.mjs --update-snapshots
```
