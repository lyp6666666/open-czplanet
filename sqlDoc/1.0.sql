/*
 Navicat Premium Data Transfer

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 50718 (5.7.18-log)
 Source Host           : localhost:3306
 Source Schema         : ai_tutor

 Target Server Type    : MySQL
 Target Server Version : 50718 (5.7.18-log)
 File Encoding         : 65001

 Date: 25/11/2025 01:57:00
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for position_post
-- ----------------------------
DROP TABLE IF EXISTS `position_post`;
CREATE TABLE `position_post`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '科目ID',
  `parent_id` bigint(20) NULL DEFAULT NULL COMMENT '父科目ID（例如：小学 -> 数学）',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '科目名称，如数学、英语、钢琴',
  `grade` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '年级段：小学/初中/高中/通用',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '科目描述或教学范围',
  `sort` int(11) NULL DEFAULT 0 COMMENT '排序',
  `enable_status` tinyint(4) NULL DEFAULT 1 COMMENT '启用状态：1-启用 0-禁用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_parent_id`(`parent_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '家教科目/分类表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for student_job_posting
-- ----------------------------
DROP TABLE IF EXISTS `student_job_posting`;
CREATE TABLE `student_job_posting`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '岗位需求ID',
  `parent_id` bigint(20) NOT NULL COMMENT '家长ID（对应 parent_profile.user_id 或 user 表）',
  `subject_id` bigint(20) NOT NULL COMMENT '需求科目ID（position_post.id）',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '需求标题，如：小学三年级数学家教',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '课程/需求详情描述',
  `child_age` int(11) NULL DEFAULT NULL COMMENT '孩子年龄',
  `class_mode` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'online' COMMENT '授课方式：online/offline/both',
  `city` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '线下授课所在城市（offline 时必填）',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '具体上课地址（offline 时可选）',
  `budget_min` decimal(10, 2) NULL DEFAULT NULL COMMENT '预算下限（每小时）',
  `budget_max` decimal(10, 2) NULL DEFAULT NULL COMMENT '预算上限（每小时）',
  `schedule` json NULL COMMENT '期望上课时间，例如：[\"Tue 19-21\",\"Sat 10-12\"]',
  `status` tinyint(4) NULL DEFAULT 1 COMMENT '状态：1发布中 0关闭',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_parent_id`(`parent_id`) USING BTREE,
  INDEX `idx_subject_id`(`subject_id`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '家长发布的家教岗位需求表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for student_profile
-- ----------------------------
DROP TABLE IF EXISTS `student_profile`;
CREATE TABLE `student_profile`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '家长资料id',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '用户id（逻辑外键）',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '家长姓名',
  `child_age` int(11) NULL DEFAULT NULL COMMENT '孩子年龄',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '上课地址',
  `demand_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '家教需求描述',
  `budget` decimal(10, 2) NULL DEFAULT NULL COMMENT '预算（每小时或每次）',
  `status` int(11) NULL DEFAULT 1 COMMENT '状态 1正常 0禁用',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '家长资料表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for teacher_job_posting
-- ----------------------------
DROP TABLE IF EXISTS `teacher_job_posting`;
CREATE TABLE `teacher_job_posting`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '发布ID',
  `tutor_id` bigint(20) NOT NULL COMMENT '家教老师ID',
  `subject_id` bigint(20) NOT NULL COMMENT '授课科目ID（position_post.id）',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '服务标题，例如：初中数学一对一辅导',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '课程详细说明',
  `price_per_hour` decimal(10, 2) NOT NULL COMMENT '每小时价格',
  `mode` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'online' COMMENT '授课方式：online/offline/both',
  `city` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '线下授课城市（当 mode=offline 时需要）',
  `available_time` json NULL COMMENT '可授课时间段，例如：[\"Mon 18-20\", \"Wed 19-21\"]',
  `max_students` int(11) NULL DEFAULT 1 COMMENT '最大可同时授课人数（默认1对1）',
  `status` tinyint(4) NULL DEFAULT 1 COMMENT '状态：1-上架 0-下架',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tutor_id`(`tutor_id`) USING BTREE,
  INDEX `idx_subject_id`(`subject_id`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '老师发布的授课服务/可预约家教岗位' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for teacher_profile
-- ----------------------------
DROP TABLE IF EXISTS `teacher_profile`;
CREATE TABLE `teacher_profile`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '教师资料id',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '用户id（逻辑外键）',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '教师真实姓名',
  `education` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '学历',
  `subject` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '教授科目',
  `experience_years` int(11) NULL DEFAULT NULL COMMENT '教学经验（年数）',
  `rate_per_hour` decimal(10, 2) NULL DEFAULT NULL COMMENT '每小时收费',
  `introduction` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '教师简介',
  `certificate_urls` json NULL COMMENT '教师证书或资格证明文件链接',
  `status` int(11) NULL DEFAULT 1 COMMENT '状态 1正常 0禁用',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '教师资料表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户id',
  `name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户昵称',
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户手机号',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户头像',
  `sex` int(11) NULL DEFAULT NULL COMMENT '性别 1为男性，2为女性',
  `open_id` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '微信openid用户标识',
  `active_status` int(11) NULL DEFAULT 2 COMMENT '在线状态 1在线 2离线',
  `last_opt_time` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '最后上下线时间',
  `ip_info` json NULL COMMENT 'ip信息',
  `item_id` bigint(20) NULL DEFAULT NULL COMMENT '佩戴的徽章id',
  `status` int(11) NULL DEFAULT 0 COMMENT '使用状态 0正常 1拉黑',
  `user_type` tinyint(1) NOT NULL COMMENT '用户类型 1教师 2家长',
  `ref_id` bigint(20) NULL DEFAULT NULL COMMENT '逻辑外键，指向教师表或家长表id',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniq_open_id`(`open_id`) USING BTREE,
  UNIQUE INDEX `uniq_name`(`name`) USING BTREE,
  INDEX `idx_create_time`(`create_time`) USING BTREE,
  INDEX `idx_update_time`(`update_time`) USING BTREE,
  INDEX `idx_active_status_last_opt_time`(`active_status`, `last_opt_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表' ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
