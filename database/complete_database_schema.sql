-- =====================================================
-- DialTestCenter 数据库完整结构初始化脚本
-- =====================================================
-- 创建时间: 2025-01-24
-- 描述: 拨测控制中心完整数据库结构，包含所有表、索引、约束和初始数据
-- 数据库: PostgreSQL 16+
-- 字符集: UTF-8
-- =====================================================

-- 设置数据库参数
SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

-- =====================================================
-- 1. 删除已存在的表（按依赖关系顺序删除）
-- =====================================================
DROP TABLE IF EXISTS test_case CASCADE;
DROP TABLE IF EXISTS app_type CASCADE;
DROP TABLE IF EXISTS test_case_set CASCADE;
DROP TABLE IF EXISTS software_package CASCADE;
DROP TABLE IF EXISTS operation_logs CASCADE;
DROP TABLE IF EXISTS operation_log CASCADE;
DROP TABLE IF EXISTS user_role CASCADE;
DROP TABLE IF EXISTS user_roles CASCADE;
DROP TABLE IF EXISTS roles CASCADE;
DROP TABLE IF EXISTS operation_targets CASCADE;
DROP TABLE IF EXISTS operation_types CASCADE;
DROP TABLE IF EXISTS dial_user CASCADE;
DROP TABLE IF EXISTS dial_users CASCADE;

-- =====================================================
-- 2. 创建函数
-- =====================================================

-- 更新时间戳函数
CREATE OR REPLACE FUNCTION public.update_updated_time_column() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;

-- =====================================================
-- 3. 创建序列
-- =====================================================

-- 应用类型序列
CREATE SEQUENCE public.app_type_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- 用户序列
CREATE SEQUENCE public.dial_users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- 操作记录序列
CREATE SEQUENCE public.operation_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- 操作记录序列（新版本）
CREATE SEQUENCE public.operation_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- 操作目标序列
CREATE SEQUENCE public.operation_targets_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- 操作类型序列
CREATE SEQUENCE public.operation_types_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- 角色序列
CREATE SEQUENCE public.roles_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- 软件包序列
CREATE SEQUENCE public.software_package_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- 测试用例序列
CREATE SEQUENCE public.test_case_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- 测试用例集序列
CREATE SEQUENCE public.test_case_set_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- 用户角色序列
CREATE SEQUENCE public.user_role_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- 用户角色序列（新版本）
CREATE SEQUENCE public.user_roles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- =====================================================
-- 4. 创建表结构
-- =====================================================

-- 4.1 应用类型表
CREATE TABLE public.app_type (
    id bigint NOT NULL DEFAULT nextval('public.app_type_id_seq'::regclass),
    business_category character varying(200) NOT NULL,
    app_name character varying(200) NOT NULL,
    description character varying(500),
    CONSTRAINT app_type_pkey PRIMARY KEY (id),
    CONSTRAINT uk_business_app UNIQUE (business_category, app_name)
);

-- 4.2 用户表
CREATE TABLE public.dial_users (
    id bigint NOT NULL DEFAULT nextval('public.dial_users_id_seq'::regclass),
    username character varying(50) NOT NULL,
    password character varying(255) NOT NULL,
    last_login_time timestamp without time zone,
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_time timestamp without time zone,
    CONSTRAINT dial_users_pkey PRIMARY KEY (id),
    CONSTRAINT dial_users_username_key UNIQUE (username)
);

-- 4.3 操作记录表（旧版本）
CREATE TABLE public.operation_log (
    id bigint NOT NULL DEFAULT nextval('public.operation_log_id_seq'::regclass),
    username character varying(100) NOT NULL,
    operation_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    operation_type character varying(50) NOT NULL,
    target character varying(200) NOT NULL,
    description text,
    CONSTRAINT operation_log_pkey PRIMARY KEY (id)
);

-- 4.4 操作记录表（新版本）
CREATE TABLE public.operation_logs (
    id bigint NOT NULL DEFAULT nextval('public.operation_logs_id_seq'::regclass),
    username character varying(50) NOT NULL,
    operation_type character varying(20) NOT NULL,
    operation_target character varying(50) NOT NULL,
    operation_description_zh text,
    operation_description_en text,
    operation_data jsonb,
    operation_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT operation_logs_pkey PRIMARY KEY (id),
    CONSTRAINT chk_description_not_empty CHECK (((operation_description_zh IS NOT NULL) OR (operation_description_en IS NOT NULL)))
);

-- 4.5 操作目标表
CREATE TABLE public.operation_targets (
    id integer NOT NULL DEFAULT nextval('public.operation_targets_id_seq'::regclass),
    code character varying(50) NOT NULL,
    name_zh character varying(50) NOT NULL,
    name_en character varying(50) NOT NULL,
    description_zh text,
    description_en text,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT operation_targets_pkey PRIMARY KEY (id),
    CONSTRAINT operation_targets_code_key UNIQUE (code)
);

-- 4.6 操作类型表
CREATE TABLE public.operation_types (
    id integer NOT NULL DEFAULT nextval('public.operation_types_id_seq'::regclass),
    code character varying(20) NOT NULL,
    name_zh character varying(50) NOT NULL,
    name_en character varying(50) NOT NULL,
    description_zh text,
    description_en text,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT operation_types_pkey PRIMARY KEY (id),
    CONSTRAINT operation_types_code_key UNIQUE (code)
);

-- 4.7 角色表
CREATE TABLE public.roles (
    id integer NOT NULL DEFAULT nextval('public.roles_id_seq'::regclass),
    code character varying(20) NOT NULL,
    name_zh character varying(50) NOT NULL,
    name_en character varying(50) NOT NULL,
    description_zh text,
    description_en text,
    CONSTRAINT roles_pkey PRIMARY KEY (id),
    CONSTRAINT roles_code_key UNIQUE (code)
);

-- 4.8 软件包表
CREATE TABLE public.software_package (
    id bigint NOT NULL DEFAULT nextval('public.software_package_id_seq'::regclass),
    software_name character varying(255) NOT NULL,
    file_content bytea NOT NULL,
    file_format character varying(10) NOT NULL,
    sha512 character varying(128),
    platform character varying(20) NOT NULL,
    creator character varying(100) NOT NULL,
    file_size bigint NOT NULL,
    description character varying(1000),
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT software_package_pkey PRIMARY KEY (id),
    CONSTRAINT uk_software_package_sha512 UNIQUE (sha512),
    CONSTRAINT uk_software_package_software_name UNIQUE (software_name)
);

-- 4.9 测试用例表
CREATE TABLE public.test_case (
    id bigint NOT NULL DEFAULT nextval('public.test_case_id_seq'::regclass),
    test_case_set_id bigint NOT NULL,
    case_name character varying(200) NOT NULL,
    case_number character varying(100) NOT NULL,
    test_steps text,
    expected_result text,
    business_category character varying(200),
    app_name character varying(200),
    dependencies_package text,
    dependencies_rule text,
    environment_config text,
    script_exists boolean DEFAULT false NOT NULL,
    CONSTRAINT test_case_pkey PRIMARY KEY (id),
    CONSTRAINT uk_test_case_set_case_number UNIQUE (test_case_set_id, case_number)
);

-- 4.10 测试用例集表
CREATE TABLE public.test_case_set (
    id bigint NOT NULL DEFAULT nextval('public.test_case_set_id_seq'::regclass),
    name character varying(200) NOT NULL,
    version character varying(50) NOT NULL,
    file_content bytea NOT NULL,
    file_size bigint NOT NULL,
    description character varying(1000),
    sha256 character varying(64),
    business_zh character varying(50) DEFAULT 'VPN阻断'::character varying,
    business_en character varying(50) DEFAULT 'VPN_BLOCK'::character varying,
    CONSTRAINT test_case_set_pkey PRIMARY KEY (id),
    CONSTRAINT uk_name_version UNIQUE (name, version)
);

-- 4.11 用户角色表（旧版本）
CREATE TABLE public.user_role (
    id bigint NOT NULL DEFAULT nextval('public.user_role_id_seq'::regclass),
    username character varying(100) NOT NULL,
    role character varying(50) NOT NULL,
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT user_role_pkey PRIMARY KEY (id),
    CONSTRAINT uk_username_role UNIQUE (username, role)
);

-- 4.12 用户角色表（新版本）
CREATE TABLE public.user_roles (
    id bigint NOT NULL DEFAULT nextval('public.user_roles_id_seq'::regclass),
    username character varying(50) NOT NULL,
    role character varying(20) NOT NULL,
    CONSTRAINT user_roles_pkey PRIMARY KEY (id),
    CONSTRAINT user_roles_username_role_key UNIQUE (username, role)
);

-- 4.13 执行机表
CREATE TABLE public.executor (
    name character varying(40) NOT NULL,
    ip character varying(40),
    token character varying(256),
    proxy character varying(256),
    description text,
    status smallint,
    last_online_time timestamp without time zone,
    CONSTRAINT executor_pkey PRIMARY KEY (name)
);

-- 4.14 UE设备表
CREATE TABLE public.ue (
    msisdn character varying(15) NOT NULL,
    executor_name character varying(40),
    vendor character varying(128),
    os character varying(128),
    info text,
    task_info text,
    CONSTRAINT ue_pkey PRIMARY KEY (msisdn),
    CONSTRAINT fk_ue_executor_name FOREIGN KEY (executor_name) REFERENCES public.executor(name) ON UPDATE CASCADE ON DELETE SET NULL
);

-- =====================================================
-- 5. 创建外键约束
-- =====================================================

-- 测试用例表外键
ALTER TABLE ONLY public.test_case
    ADD CONSTRAINT fk_test_case_set_id FOREIGN KEY (test_case_set_id) REFERENCES public.test_case_set(id) ON DELETE CASCADE;

-- =====================================================
-- 6. 创建索引
-- =====================================================

-- 应用类型表索引
CREATE INDEX idx_app_type_app ON public.app_type USING btree (app_name);
CREATE INDEX idx_app_type_category ON public.app_type USING btree (business_category);

-- 用户表索引
CREATE INDEX idx_dial_users_created_time ON public.dial_users USING btree (created_time);
CREATE INDEX idx_dial_users_last_login_time ON public.dial_users USING btree (last_login_time);
CREATE INDEX idx_dial_users_username ON public.dial_users USING btree (username);

-- 操作记录表索引
CREATE INDEX idx_operation_log_operation_time ON public.operation_log USING btree (operation_time);
CREATE INDEX idx_operation_log_operation_type ON public.operation_log USING btree (operation_type);
CREATE INDEX idx_operation_log_target ON public.operation_log USING btree (target);
CREATE INDEX idx_operation_log_username ON public.operation_log USING btree (username);

-- 操作记录表索引（新版本）
CREATE INDEX idx_operation_logs_operation_target ON public.operation_logs USING btree (operation_target);
CREATE INDEX idx_operation_logs_operation_time ON public.operation_logs USING btree (operation_time);
CREATE INDEX idx_operation_logs_operation_type ON public.operation_logs USING btree (operation_type);
CREATE INDEX idx_operation_logs_type_time ON public.operation_logs USING btree (operation_type, operation_time);
CREATE INDEX idx_operation_logs_username ON public.operation_logs USING btree (username);
CREATE INDEX idx_operation_logs_username_time ON public.operation_logs USING btree (username, operation_time);

-- 软件包表索引
CREATE INDEX idx_software_package_created_time ON public.software_package USING btree (created_time);
CREATE INDEX idx_software_package_creator ON public.software_package USING btree (creator);
CREATE INDEX idx_software_package_file_format ON public.software_package USING btree (file_format);
CREATE INDEX idx_software_package_platform ON public.software_package USING btree (platform);
CREATE INDEX idx_software_package_sha512 ON public.software_package USING btree (sha512);
CREATE INDEX idx_software_package_software_name ON public.software_package USING btree (software_name);

-- 测试用例表索引
CREATE INDEX idx_case_number ON public.test_case USING btree (case_number);
CREATE INDEX idx_script_exists ON public.test_case USING btree (script_exists);
CREATE INDEX idx_test_case_set_id ON public.test_case USING btree (test_case_set_id);

-- 测试用例集表索引
CREATE INDEX idx_test_case_set_name ON public.test_case_set USING btree (name);

-- 用户角色表索引
CREATE INDEX idx_role ON public.user_role USING btree (role);
CREATE INDEX idx_username ON public.user_role USING btree (username);
CREATE INDEX idx_user_roles_role ON public.user_roles USING btree (role);
CREATE INDEX idx_user_roles_username ON public.user_roles USING btree (username);

-- 执行机表索引
CREATE INDEX idx_executor_status_last_online_time ON public.executor USING btree (status, last_online_time DESC);

-- UE表索引
CREATE INDEX idx_ue_executor_name ON public.ue USING btree (executor_name);
CREATE INDEX idx_ue_vendor ON public.ue USING btree (vendor);

-- =====================================================
-- 7. 创建触发器
-- =====================================================

-- 软件包更新时间触发器
CREATE TRIGGER update_software_package_updated_time 
    BEFORE UPDATE ON public.software_package 
    FOR EACH ROW EXECUTE FUNCTION public.update_updated_time_column();

-- 用户角色更新时间触发器
CREATE TRIGGER update_user_role_updated_time 
    BEFORE UPDATE ON public.user_role 
    FOR EACH ROW EXECUTE FUNCTION public.update_updated_time_column();

-- =====================================================
-- 8. 添加表注释
-- =====================================================

COMMENT ON TABLE public.app_type IS 'Application type table';
COMMENT ON TABLE public.dial_users IS '用户表，存储用户基本信息';
COMMENT ON TABLE public.operation_log IS '操作记录表，存储用户所有操作记录';
COMMENT ON TABLE public.operation_logs IS '操作记录表（新版本），支持中英文描述';
COMMENT ON TABLE public.operation_targets IS '操作目标表，定义可操作的对象类型';
COMMENT ON TABLE public.operation_types IS '操作类型表，定义操作类型';
COMMENT ON TABLE public.roles IS 'Role enumeration table';
COMMENT ON TABLE public.software_package IS '软件包管理表';
COMMENT ON TABLE public.test_case IS 'Test case table';
COMMENT ON TABLE public.test_case_set IS 'Test case set table';
COMMENT ON TABLE public.user_role IS '用户角色关系表';
COMMENT ON TABLE public.user_roles IS 'User role relationship table';
COMMENT ON TABLE public.executor IS '执行机表，存储注册的执行机信息';
COMMENT ON TABLE public.ue IS 'UE设备表，存储与执行机关联的手机信息';

-- =====================================================
-- 9. 添加列注释
-- =====================================================

-- 应用类型表列注释
COMMENT ON COLUMN public.app_type.id IS 'Primary key ID, auto increment';
COMMENT ON COLUMN public.app_type.business_category IS 'Business category';
COMMENT ON COLUMN public.app_type.app_name IS 'Application name';
COMMENT ON COLUMN public.app_type.description IS 'Description';

-- 用户表列注释
COMMENT ON COLUMN public.dial_users.id IS '用户ID，主键';
COMMENT ON COLUMN public.dial_users.username IS '用户名，唯一标识';
COMMENT ON COLUMN public.dial_users.password IS '用户密码，加密存储';
COMMENT ON COLUMN public.dial_users.last_login_time IS '最后登录时间';
COMMENT ON COLUMN public.dial_users.created_time IS '创建时间';
COMMENT ON COLUMN public.dial_users.updated_time IS '更新时间';

-- 操作记录表列注释
COMMENT ON COLUMN public.operation_log.id IS '操作记录ID，主键';
COMMENT ON COLUMN public.operation_log.username IS '操作用户名';
COMMENT ON COLUMN public.operation_log.operation_time IS '操作时间';
COMMENT ON COLUMN public.operation_log.operation_type IS '操作类型：CREATE/UPDATE/DELETE/LOGIN/LOGOUT等';
COMMENT ON COLUMN public.operation_log.target IS '操作对象：如用户、角色、测试用例集等';
COMMENT ON COLUMN public.operation_log.description IS '操作描述：详细的操作说明';

-- 测试用例表列注释
COMMENT ON COLUMN public.test_case.id IS 'Primary key ID, auto increment';
COMMENT ON COLUMN public.test_case.test_case_set_id IS 'Associated test case set ID';
COMMENT ON COLUMN public.test_case.case_name IS 'Test case name';
COMMENT ON COLUMN public.test_case.case_number IS 'Test case number';
COMMENT ON COLUMN public.test_case.test_steps IS 'Test steps';
COMMENT ON COLUMN public.test_case.expected_result IS 'Expected result';
COMMENT ON COLUMN public.test_case.business_category IS 'Business category';
COMMENT ON COLUMN public.test_case.app_name IS 'Application name';
COMMENT ON COLUMN public.test_case.dependencies_package IS 'Dependencies package';
COMMENT ON COLUMN public.test_case.dependencies_rule IS 'Dependencies rule';
COMMENT ON COLUMN public.test_case.environment_config IS 'Environment configuration (text format, supports JSON string)';
COMMENT ON COLUMN public.test_case.script_exists IS 'Script exists flag';

-- 测试用例集表列注释
COMMENT ON COLUMN public.test_case_set.id IS 'Primary key ID, auto increment';
COMMENT ON COLUMN public.test_case_set.name IS 'Test case set name';
COMMENT ON COLUMN public.test_case_set.version IS 'Test case set version';
COMMENT ON COLUMN public.test_case_set.file_content IS 'File content (binary)';
COMMENT ON COLUMN public.test_case_set.file_size IS 'File size (bytes)';
COMMENT ON COLUMN public.test_case_set.description IS 'Description';
COMMENT ON COLUMN public.test_case_set.sha256 IS 'File SHA256 hash value';
COMMENT ON COLUMN public.test_case_set.business_zh IS '业务类型（中文）';
COMMENT ON COLUMN public.test_case_set.business_en IS 'Business type (English)';

-- 软件包表列注释
COMMENT ON COLUMN public.software_package.id IS '主键ID';
COMMENT ON COLUMN public.software_package.software_name IS '软件名称（完整文件名，带后缀）';
COMMENT ON COLUMN public.software_package.file_content IS '文件内容（二进制数据）';
COMMENT ON COLUMN public.software_package.file_format IS '文件格式：apk 或 ipa';
COMMENT ON COLUMN public.software_package.sha512 IS '文件内容的SHA512哈希值';
COMMENT ON COLUMN public.software_package.platform IS '平台：android 或 ios';
COMMENT ON COLUMN public.software_package.creator IS '创建者';
COMMENT ON COLUMN public.software_package.file_size IS '文件大小（字节）';
COMMENT ON COLUMN public.software_package.description IS '描述信息';
COMMENT ON COLUMN public.software_package.created_time IS '创建时间';
COMMENT ON COLUMN public.software_package.updated_time IS '更新时间';

-- 执行机表列注释
COMMENT ON COLUMN public.executor.name IS '执行机名称，主键';
COMMENT ON COLUMN public.executor.ip IS '执行机IP地址';
COMMENT ON COLUMN public.executor.token IS '认证Token';
COMMENT ON COLUMN public.executor.proxy IS '代理地址';
COMMENT ON COLUMN public.executor.description IS '描述信息';
COMMENT ON COLUMN public.executor.status IS '状态：0-OFFLINE, 1-ONLINE, 2-INVALID';
COMMENT ON COLUMN public.executor.last_online_time IS '最后在线时间';

-- UE表列注释
COMMENT ON COLUMN public.ue.msisdn IS '手机号/序列号，主键';
COMMENT ON COLUMN public.ue.executor_name IS '关联的执行机名称';
COMMENT ON COLUMN public.ue.vendor IS '厂商';
COMMENT ON COLUMN public.ue.os IS '操作系统及版本';
COMMENT ON COLUMN public.ue.info IS '详细信息（JSON格式）';
COMMENT ON COLUMN public.ue.task_info IS '任务信息';

-- =====================================================
-- 10. 插入初始数据
-- =====================================================

-- 插入角色定义
INSERT INTO public.roles (code, name_zh, name_en, description_zh, description_en) VALUES
('ADMIN', '管理员', 'Administrator', '拥有所有权限', 'Has all permissions'),
('OPERATOR', '操作员', 'Operator', '可以执行所有拨测相关操作', 'Can execute all dial test related operations'),
('BROWSER', '浏览者', 'Browser', '只能查看', 'View only'),
('EXECUTOR', '执行者', 'Executor', '用于执行者注册', 'For executor registration')
ON CONFLICT (code) DO NOTHING;

-- 插入操作类型
INSERT INTO public.operation_types (code, name_zh, name_en, description_zh, description_en) VALUES
('CREATE', '创建', 'Create', '创建操作', 'Create operation'),
('UPDATE', '更新', 'Update', '更新操作', 'Update operation'),
('DELETE', '删除', 'Delete', '删除操作', 'Delete operation'),
('VIEW', '查看', 'View', '查看操作', 'View operation'),
('LOGIN', '登录', 'Login', '用户登录', 'User login'),
('LOGOUT', '登出', 'Logout', '用户登出', 'User logout'),
('DOWNLOAD', '下载', 'Download', '文件下载', 'File download'),
('UPLOAD', '上传', 'Upload', '文件上传', 'File upload')
ON CONFLICT (code) DO NOTHING;

-- 插入操作目标
INSERT INTO public.operation_targets (code, name_zh, name_en, description_zh, description_en) VALUES
('USER', '用户', 'User', '用户管理', 'User management'),
('ROLE', '角色', 'Role', '角色管理', 'Role management'),
('TEST_CASE_SET', '测试用例集', 'Test Case Set', '测试用例集管理', 'Test case set management'),
('TEST_CASE', '测试用例', 'Test Case', '测试用例管理', 'Test case management'),
('SOFTWARE_PACKAGE', '软件包', 'Software Package', '软件包管理', 'Software package management'),
('SYSTEM', '系统', 'System', '系统操作', 'System operation')
ON CONFLICT (code) DO NOTHING;

-- 插入默认管理员用户
INSERT INTO public.user_roles (username, role) VALUES ('admin', 'ADMIN')
ON CONFLICT (username, role) DO NOTHING;

-- =====================================================
-- 11. 验证数据插入
-- =====================================================

-- 显示插入的数据统计
SELECT 'Roles inserted:' as status, count(*) as count FROM public.roles;
SELECT 'Operation types inserted:' as status, count(*) as count FROM public.operation_types;
SELECT 'Operation targets inserted:' as status, count(*) as count FROM public.operation_targets;
SELECT 'Admin user inserted:' as status, count(*) as count FROM public.user_roles WHERE username = 'admin' AND role = 'ADMIN';

-- =====================================================
-- 脚本执行完成
-- =====================================================
