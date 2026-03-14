/*
 Navicat Premium Data Transfer

 Source Server         : localhost_3301 (8.0)
 Source Server Type    : MySQL
 Source Server Version : 80030
 Source Host           : localhost:3301
 Source Schema         : easy-agent

 Target Server Type    : MySQL
 Target Server Version : 80030
 File Encoding         : 65001

 Date: 14/03/2026
*/

SET NAMES utf8mb4;
SET
FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ea_iam_user
-- ----------------------------
DROP TABLE IF EXISTS `ea_iam_user`;
CREATE TABLE `ea_iam_user`
(
    `id`         bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username`   varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci  NOT NULL COMMENT '用户名',
    `password`   varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码（BCrypt加密）',
    `email`      varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '邮箱',
    `phone`      varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '手机号',
    `status`     tinyint(0) NULL DEFAULT 1 COMMENT '状态：1=正常，0=禁用',
    `created_at` timestamp(0)                                                  NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `updated_at` timestamp(0)                                                  NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_username`(`username`) USING BTREE,
    INDEX        `idx_email`(`email`) USING BTREE,
    INDEX        `idx_status`(`status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ea_iam_role
-- ----------------------------
DROP TABLE IF EXISTS `ea_iam_role`;
CREATE TABLE `ea_iam_role`
(
    `id`          bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_code`   varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci  NOT NULL COMMENT '角色编码（如ADMIN、USER）',
    `role_name`   varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称',
    `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '角色描述',
    `created_at`  timestamp(0)                                                  NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `updated_at`  timestamp(0)                                                  NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_role_code`(`role_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ea_iam_permission
-- ----------------------------
DROP TABLE IF EXISTS `ea_iam_permission`;
CREATE TABLE `ea_iam_permission`
(
    `id`              bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `permission_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限编码（如agent:read、agent:write）',
    `permission_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限名称',
    `resource_type`   varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '资源类型：menu/button/api',
    `resource_url`    varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '资源路径',
    `description`     varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '权限描述',
    `created_at`      timestamp(0)                                                  NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `updated_at`      timestamp(0)                                                  NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_permission_code`(`permission_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '权限表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ea_iam_user_role
-- ----------------------------
DROP TABLE IF EXISTS `ea_iam_user_role`;
CREATE TABLE `ea_iam_user_role`
(
    `id`         bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`    bigint(0) NOT NULL COMMENT '用户ID',
    `role_id`    bigint(0) NOT NULL COMMENT '角色ID',
    `created_at` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_user_role`(`user_id`, `role_id`) USING BTREE,
    INDEX        `idx_user_id`(`user_id`) USING BTREE,
    INDEX        `idx_role_id`(`role_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户角色关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ea_iam_role_permission
-- ----------------------------
DROP TABLE IF EXISTS `ea_iam_role_permission`;
CREATE TABLE `ea_iam_role_permission`
(
    `id`            bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_id`       bigint(0) NOT NULL COMMENT '角色ID',
    `permission_id` bigint(0) NOT NULL COMMENT '权限ID',
    `created_at`    timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_role_permission`(`role_id`, `permission_id`) USING BTREE,
    INDEX           `idx_role_id`(`role_id`) USING BTREE,
    INDEX           `idx_permission_id`(`permission_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '角色权限关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 初始角色数据
-- ----------------------------
INSERT INTO `ea_iam_role` (`id`, `role_code`, `role_name`, `description`)
VALUES (1, 'ADMIN', '管理员', '拥有所有权限'),
       (2, 'USER', '普通用户', '拥有基本权限');

-- ----------------------------
-- 初始权限数据
-- ----------------------------
INSERT INTO `ea_iam_permission` (`id`, `permission_code`, `permission_name`, `resource_type`, `resource_url`,
                                 `description`)
VALUES (1, 'agent:read', '查看Agent', 'api', '/eaAgent/**', '查看Agent相关接口'),
       (2, 'agent:write', '编辑Agent', 'api', '/eaAgent/**', '编辑Agent相关接口'),
       (3, 'agent:delete', '删除Agent', 'api', '/eaAgent/**', '删除Agent相关接口'),
       (4, 'tool:read', '查看工具', 'api', '/toolManager/**', '查看工具相关接口'),
       (5, 'tool:write', '编辑工具', 'api', '/toolManager/**', '编辑工具相关接口'),
       (6, 'tool:delete', '删除工具', 'api', '/toolManager/**', '删除工具相关接口'),
       (7, 'knowledge:read', '查看知识库', 'api', '/knowledge/**', '查看知识库相关接口'),
       (8, 'knowledge:write', '编辑知识库', 'api', '/knowledge/**', '编辑知识库相关接口'),
       (9, 'knowledge:delete', '删除知识库', 'api', '/knowledge/**', '删除知识库相关接口'),
       (10, 'chat:read', '查看聊天', 'api', '/chat/**', '查看聊天相关接口'),
       (11, 'chat:write', '发送消息', 'api', '/chat/**', '发送消息相关接口'),
       (12, 'user:read', '查看用户', 'api', '/auth/**', '查看用户相关接口'),
       (13, 'user:write', '编辑用户', 'api', '/auth/**', '编辑用户相关接口'),
       (14, 'user:delete', '删除用户', 'api', '/auth/**', '删除用户相关接口');

-- ----------------------------
-- 初始角色权限关联数据（管理员拥有所有权限）
-- ----------------------------
INSERT INTO `ea_iam_role_permission` (`role_id`, `permission_id`)
VALUES (1, 1),
       (1, 2),
       (1, 3),
       (1, 4),
       (1, 5),
       (1, 6),
       (1, 7),
       (1, 8),
       (1, 9),
       (1, 10),
       (1, 11),
       (1, 12),
       (1, 13),
       (1, 14);

-- ----------------------------
-- 初始普通用户权限（仅查看和基本操作）
-- ----------------------------
INSERT INTO `ea_iam_role_permission` (`role_id`, `permission_id`)
VALUES (2, 1),
       (2, 4),
       (2, 7),
       (2, 10),
       (2, 11);

-- ----------------------------
-- 初始管理员用户（密码：admin123）
-- ----------------------------
INSERT INTO `ea_iam_user` (`id`, `username`, `password`, `email`, `phone`, `status`)
VALUES (1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'admin@example.com', '13800138000',
        1);

-- ----------------------------
-- 初始用户角色关联数据
-- ----------------------------
INSERT INTO `ea_iam_user_role` (`user_id`, `role_id`)
VALUES (1, 1);

SET
FOREIGN_KEY_CHECKS = 1;
