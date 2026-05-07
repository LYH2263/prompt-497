-- Set Client Encoding
SET client_encoding = 'UTF8';

-- Create Users Table
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL
);

-- Equipment Table
CREATE TABLE IF NOT EXISTS equipment (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL, -- e.g., 'Normal', 'Warning', 'Maintenance'
    location VARCHAR(100),
    type VARCHAR(50),
    last_maintenance TIMESTAMP,
    installation_date DATE
);

-- Maintenance Records
CREATE TABLE IF NOT EXISTS maintenance_record (
    id SERIAL PRIMARY KEY,
    equipment_id INTEGER REFERENCES equipment(id),
    technician VARCHAR(100),
    description TEXT,
    maintenance_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) -- 'Completed', 'Pending'
);

-- Insert Users (Password: 123456)
INSERT INTO users (username, password, role)
VALUES ('admin', '123456', 'ADMIN')
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (username, password, role)
VALUES ('user', '123456', 'USER')
ON CONFLICT (username) DO NOTHING;

-- Seed Equipment Data (Chinese)
INSERT INTO equipment (name, status, location, type, last_maintenance, installation_date) VALUES
('主井提升机 #1', '正常运行', '一号矿井', 'JK-3x2.5', NOW(), '2023-01-15'),
('副井提升机 #2', '维护中', '一号矿井', 'JK-2.5x2', NOW() - INTERVAL '2 days', '2023-03-20'),
('通风机组 A', '正常运行', '通风井', 'FBCDZ-No20', NOW() - INTERVAL '10 days', '2022-11-05'),
('排水泵站 B-1', '故障报警', '井底车场', 'MD280-50', NOW() - INTERVAL '5 days', '2024-01-10');

-- Seed Maintenance Records
INSERT INTO maintenance_record (equipment_id, technician, description, status) VALUES
(1, '张三', '例行检查，钢丝绳润滑良好', '已完成'),
(2, '李四', '制动系统磨损更换', '进行中');
