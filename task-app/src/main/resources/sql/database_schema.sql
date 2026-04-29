-- Database schema for Fourth Generation Time Management App
-- PostgreSQL

-- 1. roles テーブル
CREATE TABLE roles (
    role_id SERIAL PRIMARY KEY,
    role_name VARCHAR(255) NOT NULL,
    is_expanded BOOLEAN DEFAULT true,
    color VARCHAR(7) DEFAULT '#4a90d9',
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. tasks テーブル
DROP TABLE IF EXISTS tasks;

CREATE TABLE tasks (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    role_id INTEGER NOT NULL REFERENCES roles(role_id) ON DELETE CASCADE,
    is_permanent BOOLEAN DEFAULT false,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. sharpen_the_saw_areas テーブル
CREATE TABLE sharpen_the_saw_areas (
    id VARCHAR(50) PRIMARY KEY, -- 'Body', 'Intelligence', 'Social・Emotional', 'Mental'
    name VARCHAR(255) NOT NULL,
    icon VARCHAR(10) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. sharpen_the_saw_tasks テーブル
CREATE TABLE sharpen_the_saw_tasks (
    id SERIAL PRIMARY KEY,
    area_id VARCHAR(50) NOT NULL REFERENCES sharpen_the_saw_areas(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. week_data テーブル
CREATE TABLE week_data (
    id SERIAL PRIMARY KEY,
    week_start DATE NOT NULL,
    weekly_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(week_start)
);

-- 6. scheduled_tasks テーブル
CREATE TABLE scheduled_tasks (
    id SERIAL PRIMARY KEY,
    week_data_id INTEGER NOT NULL REFERENCES week_data(id) ON DELETE CASCADE,
    task_id INTEGER NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    day INTEGER NOT NULL CHECK (day >= 0 AND day <= 6), -- 0-6 (Monday-Sunday)
    start_time TIME NOT NULL, -- HH:MM format
    duration INTEGER NOT NULL, -- in minutes (default 60)
    title VARCHAR(255) NOT NULL,
    role_id INTEGER NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 7. day_notes テーブル
CREATE TABLE day_notes (
    id SERIAL PRIMARY KEY,
    week_data_id INTEGER NOT NULL REFERENCES week_data(id) ON DELETE CASCADE,
    day INTEGER NOT NULL CHECK (day >= 0 AND day <= 6), -- 0-6 (Monday-Sunday)
    notes TEXT,
    sleep_start TIME, -- HH:MM
    sleep_end TIME, -- HH:MM
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(week_data_id, day)
);

-- 初期データの挿入

-- sharpen_the_saw_areas の初期データ
INSERT INTO sharpen_the_saw_areas (id, name, icon) VALUES
('Body', 'Body', '💪'),
('Intelligence', 'Intelligence', '🧠'),
('Social・Emotional', 'Social・Emotional', '❤️'),
('Mental', 'Mental', '🙏');

-- サンプルのrolesデータ
INSERT INTO roles (role_name, is_expanded, color, sort_order) VALUES
('Professional', true, '#4a90d9', 1),
('Family', true, '#e67e22', 2);

-- パフォーマンス向上のためのインデックス
CREATE INDEX idx_tasks_role_id ON tasks(role_id);
CREATE INDEX idx_scheduled_tasks_week_data_id ON scheduled_tasks(week_data_id);
CREATE INDEX idx_scheduled_tasks_day ON scheduled_tasks(day);
CREATE INDEX idx_day_notes_week_data_id ON day_notes(week_data_id);
CREATE INDEX idx_day_notes_day ON day_notes(day); 