# Общая информация
Проект использует PostgresSQL для хранения загружаемых файлов и логин/hash пароля пользователей.

## Таблица users
Содержит login и hash пароля. Состоит из колонок: id(IDENTITY), username, password

При создании SpringContext по умолчанию создаются два пользователя:
admin:password
user:user

## Таблица cloud_files
Содержит полное описание загружаемого файла с информацией о пользователе-владельце файла.

Колонки:
id(IDENTITY)
filename
hash
size
storage_path
username

## Стуктура сохраняемых файлов
Файлы сохраняются по пути описанному в property.yml или ENV. Если переменная не задана, используется стандартный путь .uploads.

`upload-dir: ${APP_UPLOAD_DIR:./uploads}`

# Архитектура проекта

Проект полностью реализует файл спецификации CloudServiceSpecification.yaml.

Классы приложения расположены в следующих пакетах:
- config - класс конфигурации SpringSecurity и класс создания базовых пользователей(admin, user)
- controller - REST-контроллеры /login и /file
- dto - DTO-классы
- filter - TokenAuthFilter для проверки наличия поля auth-token в каждом запросе
- model - Entity для БД
- repository - JPA-репозитории
- service - бизнес-логика работы с файлами и логинами. TokenService - in-memory хранилище token: user

Проект покрыт unit-тестами с Mockito и интеграционными тестами с testcontainers.

# Запуск и доступ к информации

Для работы проекта нужно собрать bootJar, после чего запустить docker-compose.
```
./gradlew bootJar
docker-compose build --no-cache && docker-compose up -d
```

БД и директория загрузки файлов монтируются как volumes и сохраняют содержимое между перезапусками.

При запуске бекенда путь сохранения файлов совпадает с volume:
```
APP_UPLOAD_DIR: /app/uploads
    volumes:
      - uploaded_files:/app/uploads

```

Для авторизации в веб доступны два пользователя:
- user с паролем user
- admin с паролем password