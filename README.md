# Labkeeper

Команды запускаются из корня репозитория.

## Сервер

```shell
gradlew bootRun
```

Слушает порт 80. Запросы, которые фронтенд сам не обслуживает, уходят на `https://labkeeper.io`. Другой хост:

```shell
gradlew bootRun --args="--io.labkeeper.frontend.proxy-host=http://localhost:9090"
```

Пустое значение отключает прокси.

## Проверки

```shell
gradlew test
gradlew check
gradlew detekt
gradlew detekt detektMain detektTest "-Pdetekt.autoCorrect=true"
```

`check` включает тесты и detekt. Последняя команда ещё и применяет автоисправления.

## Форматирование

HTML, JS и YAML в `src/main/resources`.

```shell
npm --prefix src/main/libs ci
npm --prefix src/main/libs run format
npm --prefix src/main/libs run format:check
```

`npm ci` нужен один раз, пока нет `src/main/libs/node_modules`.

## JavaScript

```shell
npm --prefix src/test/javascript ci
npm --prefix src/test/javascript test
```

Проверяют ссылки примеров и разбор YAML-статей. В CI это шаг `Run JavaScript unit tests` в проверке JavaScript.

## Браузерные тесты

Нужны запущенный сервер и Docker. Сравнивают снимок главной и проверяют статьи блога: вёрстку, формулы и ссылки на пример редактора.

```shell
$env:FRONTEND_URL = "http://localhost"
npm --prefix src/test/javascript run snapshots
```
