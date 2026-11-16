# OmniAsset API

REST API для Linux-десктопа и Android-клиента. Клиенты обращаются к Ktor, а Ktor подключается к PostgreSQL.

## Запуск

Переменные окружения:

    DATABASE_JDBC_URL=jdbc:postgresql://<database-host>:5432/<database-name>?currentSchema=inventory
    DATABASE_USERNAME=postgres
    DATABASE_PASSWORD=your-password
    JWT_SECRET=long-random-secret

Проверка сервера: GET /health. Ответ: ok.

## Аутентификация

POST /api/auth/login

Тело: { username, password }.

Ответ содержит token, userId, username, fullName и role. Для защищенных запросов используется заголовок Authorization: Bearer <token>.

Роли: admin, operator, viewer.

## Техника

GET /api/equipment — список техники.

Поддерживаемые параметры: search, status, categoryId, locationId, employeeId, limit, offset. Поиск выполняется по инвентарному номеру, серийному номеру, модели и производителю. Максимальный размер страницы — 100.

GET /api/equipment/{id} — карточка техники.

POST /api/equipment — создание техники. Требует роль admin. Тело содержит inventoryNumber, serialNumber, modelId, statusId, locationId, departmentId, purchaseDate, commissioningDate, purchasePrice, warrantyUntil, specifications и notes.

PUT /api/equipment/{id} — изменение техники. Требует роль admin. Без поля specifications или при значении null сохраняются текущие характеристики. При конфликте инвентарного или серийного номера API возвращает 409.

POST /api/equipment/{id}/move — перемещение и смена ответственного. Тело: locationId, departmentId, employeeId, comment. Требует роль admin или operator. При смене ответственного операция закрывает старое закрепление и создаёт новое, если передан employeeId. Перемещение без смены сотрудника сохраняет текущее закрепление. Списанная техника возвращает 409.

POST /api/equipment/{id}/status — смена статуса. Тело: statusCode, comment.

POST /api/equipment/{id}/archive — архивирование техники администратором через статус written_off.

GET /api/equipment/{id}/history — история перемещений и смены статуса.

## Справочники

GET /api/references/{type}

Поддерживаются categories, manufacturers, locations, departments, employees, statuses и models. Для models возвращаются id модели и name, включающее производителя и название модели.

## Администратор

Административные маршруты требуют роль admin:

GET /api/admin/references/{type} — список для редактирования с дополнительными полями.

POST /api/admin/references/{type} — создание записи. Поддерживаются categories, manufacturers, locations, departments и models. Поле name обязательно. Категории, подразделения, места и модели принимают description. Для места доступны building и roomNumber, для модели обязательны categoryId и manufacturerId.

PUT /api/admin/references/{type}/{id} — изменение записи. Для building и roomNumber пустая строка очищает поле, отсутствие значения сохраняет текущее.

POST /api/admin/users — создание пользователя. Тело: username, password, fullName, email, role.

POST /api/admin/users/{id}/block — блокировка пользователя.

## Отчеты

GET /api/summary/statuses — количество техники по статусам.

GET /api/equipment.csv — CSV-экспорт списка техники с фильтрами.

GET /api/reports/equipment — ограниченный по параллельности отчетный список.

## Коды ответа

200 — успех; 201 — запись создана; 400 — неверные данные; 401 — нужна авторизация; 403 — недостаточно прав; 404 — запись не найдена; 500 — ошибка сервера.

## Покрытие ТЗ

API покрывает авторизацию, поиск, фильтрацию, пагинацию, карточку техники, создание и изменение, перемещения, статусы, историю, справочники, пользователей, сводку и CSV.

Ограничения текущей версии: полный CRUD есть не для всех справочников; ошибки пока не унифицированы в JSON; TLS требует настройки.

Для дипломной версии основной API-контур сформирован. Перед сдачей нужно проверить роли, ошибки API и клиентские сценарии.

Администратор также может управлять сотрудниками и просматривать пользователей:

GET /api/admin/users
GET /api/admin/employees
POST /api/admin/employees
PUT /api/admin/employees/{id}
POST /api/admin/employees/{id}/block

Тело создания или изменения сотрудника содержит fullName, position, department, phone и email.
