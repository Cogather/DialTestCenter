-- Executor Management V2 schema

-- dial_users table: unified user management for frontend and executor CHAP auth
-- password field stores NTLM hash for executors, bcrypt for frontend users
CREATE TABLE IF NOT EXISTS dial_users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(256) NOT NULL,
    last_login_time TIMESTAMP NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_dial_users_username ON dial_users (username);

-- executor table: stores registered executors
CREATE TABLE IF NOT EXISTS executor (
    name VARCHAR(40) PRIMARY KEY,
    ip VARCHAR(40),
    token VARCHAR(256),
    proxy VARCHAR(256),
    description TEXT,
    status SMALLINT,
    last_online_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_executor_status_last_online_time ON executor (status, last_online_time DESC);

-- ue table: stores UE devices bound to executors
CREATE TABLE IF NOT EXISTS ue (
    msisdn VARCHAR(15) PRIMARY KEY,
    executor_name VARCHAR(40) REFERENCES executor(name) ON UPDATE CASCADE ON DELETE SET NULL,
    vendor VARCHAR(128),
    os VARCHAR(128),
    info TEXT,
    task_info TEXT
);
CREATE INDEX IF NOT EXISTS idx_ue_executor_name ON ue (executor_name);
CREATE INDEX IF NOT EXISTS idx_ue_vendor ON ue (vendor);


