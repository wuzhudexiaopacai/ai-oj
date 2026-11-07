-- =============================================================
-- AI-OJ 认证中心种子数据：admin 用户 + 管理员角色 + 全部权限
-- @author wuzhudexiaopacai
-- =============================================================
USE ai_oj_auth;
SET NAMES utf8mb4;

-- 管理员用户（密码 admin123，BCrypt）
INSERT INTO sys_user (id, username, nickname, password, email, avatar, status, intro, rating)
VALUES (1, 'admin', '系统管理员', '$2a$10$gTc7gzRhhNXyoX7z0ob.rOJuphBXCC/SLxEMvWDhxNHkCMJurGS5K',
        'admin@ai-oj.local', '', 1, '超级管理员', 1500);

-- 管理员角色
INSERT INTO sys_role (id, code, name, description, status)
VALUES (1, 'ROLE_ADMIN', '超级管理员', '拥有全部权限', 1);

-- 普通用户角色
INSERT INTO sys_role (id, code, name, description, status)
VALUES (2, 'ROLE_USER', '普通用户', '默认角色', 1);

-- 权限（与代码 @PreAuthorize 一一对应）
INSERT INTO sys_permission (id, parent_id, code, name, type, sort) VALUES
-- 用户管理
(100, 0, 'user:list',        '用户列表',   3, 1),
(101, 0, 'user:create',      '创建用户',   3, 2),
(102, 0, 'user:update',      '更新用户',   3, 3),
(103, 0, 'user:delete',      '删除用户',   3, 4),
(104, 0, 'user:assign-role', '分配角色',   3, 5),
-- 角色管理
(110, 0, 'role:list',        '角色列表',   3, 11),
(111, 0, 'role:create',      '创建角色',   3, 12),
(112, 0, 'role:update',      '更新角色',   3, 13),
(113, 0, 'role:delete',      '删除角色',   3, 14),
-- 权限管理
(120, 0, 'permission:list',   '权限列表',  3, 21),
(121, 0, 'permission:create', '创建权限',  3, 22),
(122, 0, 'permission:update', '更新权限',  3, 23),
(123, 0, 'permission:delete', '删除权限',  3, 24),
-- 题目管理
(200, 0, 'problem:create',   '创建题目',   3, 31),
(201, 0, 'problem:update',   '更新题目',   3, 32),
(202, 0, 'problem:delete',   '删除题目',   3, 33),
(203, 0, 'problem:import',   '导入题目',   3, 34),
(204, 0, 'problem:export',   '导出题目',   3, 35),
(205, 0, 'problem:testcase', '管理用例',   3, 36),
-- 比赛管理
(300, 0, 'contest:manage',   '比赛管理',   3, 41),
-- 文件管理
(400, 0, 'file:manage',      '文件管理',   3, 51),
-- AI 管理
(500, 0, 'ai:template',      'AI模板管理', 3, 61),
-- 日志管理
(600, 0, 'log:operation:list', '操作日志查询', 3, 71),
-- 死信管理
(601, 0, 'judge:deadletter:list',   '死信列表', 3, 81),
(602, 0, 'judge:deadletter:handle', '死信处理', 3, 82);

-- 管理员拥有全部权限
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT ROW_NUMBER() OVER (ORDER BY id) + 1000, 1, id FROM sys_permission;

-- admin 绑定管理员角色
INSERT INTO sys_user_role (id, user_id, role_id) VALUES (1, 1, 1);
