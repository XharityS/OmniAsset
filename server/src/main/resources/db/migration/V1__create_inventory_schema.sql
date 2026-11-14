BEGIN;

CREATE SCHEMA IF NOT EXISTS inventory;

SET search_path TO inventory, public;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Пользователи системы
CREATE TABLE app_users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username        VARCHAR(50) NOT NULL UNIQUE,
    password_hash   TEXT NOT NULL,
    full_name       VARCHAR(150) NOT NULL,
    email           VARCHAR(150) UNIQUE,
    role            VARCHAR(30) NOT NULL DEFAULT 'operator',
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT app_users_role_check
        CHECK (role IN ('admin', 'operator', 'viewer'))
);

-- Сотрудники, которым может выдаваться техника
CREATE TABLE employees (
    id              BIGSERIAL PRIMARY KEY,
    full_name       VARCHAR(150) NOT NULL,
    position        VARCHAR(100),
    department      VARCHAR(100),
    phone           VARCHAR(30),
    email           VARCHAR(150),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Подразделения организации
CREATE TABLE departments (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL UNIQUE,
    description     TEXT
);

-- Кабинеты и места размещения
CREATE TABLE locations (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    building        VARCHAR(100),
    room_number     VARCHAR(30),
    description     TEXT,

    CONSTRAINT locations_unique_place
        UNIQUE (building, room_number)
);

-- Категории техники
CREATE TABLE equipment_categories (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL UNIQUE,
    description     TEXT
);

-- Производители
CREATE TABLE manufacturers (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL UNIQUE
);

-- Модели техники
CREATE TABLE equipment_models (
    id              BIGSERIAL PRIMARY KEY,
    category_id     BIGINT NOT NULL,
    manufacturer_id BIGINT NOT NULL,
    name            VARCHAR(150) NOT NULL,
    description     TEXT,

    CONSTRAINT fk_models_category
        FOREIGN KEY (category_id)
        REFERENCES equipment_categories(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_models_manufacturer
        FOREIGN KEY (manufacturer_id)
        REFERENCES manufacturers(id)
        ON DELETE RESTRICT,

    CONSTRAINT equipment_models_unique
        UNIQUE (manufacturer_id, name)
);

-- Статусы техники
CREATE TABLE equipment_statuses (
    id              SMALLSERIAL PRIMARY KEY,
    code            VARCHAR(30) NOT NULL UNIQUE,
    name            VARCHAR(100) NOT NULL UNIQUE,
    description     TEXT
);

-- Начальные статусы
INSERT INTO equipment_statuses (code, name, description)
VALUES
    ('available', 'Доступно', 'Техника находится на складе и может быть выдана'),
    ('assigned', 'Выдано', 'Техника закреплена за сотрудником'),
    ('repair', 'На ремонте', 'Техника находится на ремонте'),
    ('written_off', 'Списано', 'Техника списана'),
    ('lost', 'Утеряно', 'Техника утеряна'),
    ('reserved', 'Зарезервировано', 'Техника зарезервирована')
ON CONFLICT (code) DO NOTHING;

-- Основная таблица техники
CREATE TABLE equipment (
    id                  BIGSERIAL PRIMARY KEY,
    inventory_number    VARCHAR(50) NOT NULL UNIQUE,
    serial_number      VARCHAR(100) UNIQUE,
    model_id           BIGINT NOT NULL,
    status_id          SMALLINT NOT NULL,
    location_id        BIGINT,
    department_id      BIGINT,
    purchase_date      DATE,
    commissioning_date DATE,
    purchase_price     NUMERIC(12, 2),
    warranty_until     DATE,
    specifications     JSONB NOT NULL DEFAULT '{}'::JSONB,
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_equipment_model
        FOREIGN KEY (model_id)
        REFERENCES equipment_models(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_equipment_status
        FOREIGN KEY (status_id)
        REFERENCES equipment_statuses(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_equipment_location
        FOREIGN KEY (location_id)
        REFERENCES locations(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_equipment_department
        FOREIGN KEY (department_id)
        REFERENCES departments(id)
        ON DELETE SET NULL,

    CONSTRAINT equipment_price_check
        CHECK (purchase_price IS NULL OR purchase_price >= 0),

    CONSTRAINT equipment_dates_check
        CHECK (
            commissioning_date IS NULL
            OR purchase_date IS NULL
            OR commissioning_date >= purchase_date
        )
);

-- История закрепления техники за сотрудниками
CREATE TABLE equipment_assignments (
    id              BIGSERIAL PRIMARY KEY,
    equipment_id    BIGINT NOT NULL,
    employee_id     BIGINT NOT NULL,
    assigned_by     UUID,
    assigned_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    returned_at     TIMESTAMPTZ,
    return_comment  TEXT,

    CONSTRAINT fk_assignments_equipment
        FOREIGN KEY (equipment_id)
        REFERENCES equipment(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_assignments_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_assignments_user
        FOREIGN KEY (assigned_by)
        REFERENCES app_users(id)
        ON DELETE SET NULL,

    CONSTRAINT assignments_dates_check
        CHECK (returned_at IS NULL OR returned_at >= assigned_at)
);

-- Ремонт и техническое обслуживание
CREATE TABLE maintenance_records (
    id              BIGSERIAL PRIMARY KEY,
    equipment_id    BIGINT NOT NULL,
    maintenance_type VARCHAR(30) NOT NULL,
    description     TEXT NOT NULL,
    service_company VARCHAR(150),
    started_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMPTZ,
    cost            NUMERIC(12, 2),
    result          TEXT,
    created_by      UUID,

    CONSTRAINT fk_maintenance_equipment
        FOREIGN KEY (equipment_id)
        REFERENCES equipment(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_maintenance_user
        FOREIGN KEY (created_by)
        REFERENCES app_users(id)
        ON DELETE SET NULL,

    CONSTRAINT maintenance_type_check
        CHECK (maintenance_type IN ('repair', 'inspection', 'upgrade', 'service')),

    CONSTRAINT maintenance_cost_check
        CHECK (cost IS NULL OR cost >= 0),

    CONSTRAINT maintenance_dates_check
        CHECK (completed_at IS NULL OR completed_at >= started_at)
);

-- Перемещения техники между кабинетами и подразделениями
CREATE TABLE equipment_movements (
    id              BIGSERIAL PRIMARY KEY,
    equipment_id    BIGINT NOT NULL,
    from_location_id BIGINT,
    to_location_id   BIGINT,
    from_department_id BIGINT,
    to_department_id   BIGINT,
    moved_by        UUID,
    moved_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reason          TEXT,

    CONSTRAINT fk_movements_equipment
        FOREIGN KEY (equipment_id)
        REFERENCES equipment(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_movements_from_location
        FOREIGN KEY (from_location_id)
        REFERENCES locations(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_movements_to_location
        FOREIGN KEY (to_location_id)
        REFERENCES locations(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_movements_from_department
        FOREIGN KEY (from_department_id)
        REFERENCES departments(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_movements_to_department
        FOREIGN KEY (to_department_id)
        REFERENCES departments(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_movements_user
        FOREIGN KEY (moved_by)
        REFERENCES app_users(id)
        ON DELETE SET NULL
);

-- Списание техники
CREATE TABLE write_off_records (
    id              BIGSERIAL PRIMARY KEY,
    equipment_id    BIGINT NOT NULL UNIQUE,
    reason          TEXT NOT NULL,
    write_off_date  DATE NOT NULL DEFAULT CURRENT_DATE,
    residual_value  NUMERIC(12, 2),
    approved_by     UUID,

    CONSTRAINT fk_write_off_equipment
        FOREIGN KEY (equipment_id)
        REFERENCES equipment(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_write_off_user
        FOREIGN KEY (approved_by)
        REFERENCES app_users(id)
        ON DELETE SET NULL,

    CONSTRAINT residual_value_check
        CHECK (residual_value IS NULL OR residual_value >= 0)
);

-- Журнал действий пользователей
CREATE TABLE audit_log (
    id              BIGSERIAL PRIMARY KEY,
    user_id         UUID,
    action          VARCHAR(30) NOT NULL,
    entity_name     VARCHAR(100) NOT NULL,
    entity_id       VARCHAR(100),
    old_data        JSONB,
    new_data        JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_audit_user
        FOREIGN KEY (user_id)
        REFERENCES app_users(id)
        ON DELETE SET NULL
);

-- Индексы для быстрого поиска

CREATE INDEX idx_equipment_model
    ON equipment(model_id);

CREATE INDEX idx_equipment_status
    ON equipment(status_id);

CREATE INDEX idx_equipment_location
    ON equipment(location_id);

CREATE INDEX idx_equipment_department
    ON equipment(department_id);

CREATE INDEX idx_equipment_serial_number
    ON equipment(serial_number);

CREATE INDEX idx_equipment_purchase_date
    ON equipment(purchase_date);

CREATE INDEX idx_equipment_warranty_until
    ON equipment(warranty_until);

CREATE INDEX idx_equipment_specifications
    ON equipment USING GIN(specifications);

CREATE INDEX idx_models_category
    ON equipment_models(category_id);

CREATE INDEX idx_models_manufacturer
    ON equipment_models(manufacturer_id);

CREATE INDEX idx_assignments_equipment
    ON equipment_assignments(equipment_id);

CREATE INDEX idx_assignments_employee
    ON equipment_assignments(employee_id);

CREATE INDEX idx_assignments_active
    ON equipment_assignments(equipment_id)
    WHERE returned_at IS NULL;

CREATE INDEX idx_maintenance_equipment
    ON maintenance_records(equipment_id);

CREATE INDEX idx_maintenance_started_at
    ON maintenance_records(started_at);

CREATE INDEX idx_movements_equipment
    ON equipment_movements(equipment_id);

CREATE INDEX idx_movements_moved_at
    ON equipment_movements(moved_at);

CREATE INDEX idx_audit_entity
    ON audit_log(entity_name, entity_id);

CREATE INDEX idx_audit_created_at
    ON audit_log(created_at);

-- Проверка: у одной единицы техники не может быть
-- двух активных закреплений одновременно
CREATE UNIQUE INDEX ux_one_active_assignment_per_equipment
    ON equipment_assignments(equipment_id)
    WHERE returned_at IS NULL;

-- Автоматическое обновление updated_at

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_equipment_updated_at
BEFORE UPDATE ON equipment
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

COMMIT;