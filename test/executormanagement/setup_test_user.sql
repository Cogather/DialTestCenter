-- Setup test user for executor management integration tests
-- 
-- According to "Executor Management Implementation Design" (V4):
-- - dial_users.password field stores SHA256 Hash (64-bit hexadecimal)
-- - CHAP authentication: Response = SHA256(SHA256-Hash + Challenge)
--
-- Usage:
-- 1. Connect to PostgreSQL database
-- 2. Execute this script to create test user
-- 3. Record the password value (SHA256 Hash) for EXEC_AGENT_SHA256_HASH environment variable
--
-- Test user password: test123
-- SHA256 Hash: ECD71870D1963316A97E3AC3408C9835AD8CF0F3C1BC703527C30265534F75AE
-- 
-- Generate SHA256 Hash (Python):
-- import hashlib
-- password = "test123"
-- sha256_hash = hashlib.sha256(password.encode('utf-8')).hexdigest().upper()
-- print(sha256_hash)

-- Clean up existing test users (compatible with old table names)
DELETE FROM agent_user WHERE username = 'test_agent';
DELETE FROM dial_users WHERE username = 'test_agent';

-- Insert test user into dial_users table
-- username: test_agent
-- password: ECD71870D1963316A97E3AC3408C9835AD8CF0F3C1BC703527C30265534F75AE (SHA256 Hash of test123)
INSERT INTO dial_users (username, password)
VALUES ('test_agent', 'ECD71870D1963316A97E3AC3408C9835AD8CF0F3C1BC703527C30265534F75AE')
ON CONFLICT (username) DO UPDATE
SET password = EXCLUDED.password;

-- Verify insertion
SELECT id, username, password, last_login_time
FROM dial_users 
WHERE username = 'test_agent';

-- Status message
SELECT 'Success' as status,
       'test_agent' as username,
       'test123' as raw_password,
       'ECD71870D1963316A97E3AC3408C9835AD8CF0F3C1BC703527C30265534F75AE' as sha256_hash,
       'Set environment variable: export EXEC_AGENT_SHA256_HASH=ECD71870D1963316A97E3AC3408C9835AD8CF0F3C1BC703527C30265534F75AE' as instruction;

