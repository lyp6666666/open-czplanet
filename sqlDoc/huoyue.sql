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

 Date: 30/11/2025 22:15:38
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
-- Records of position_post
-- ----------------------------

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
-- Records of student_job_posting
-- ----------------------------

-- ----------------------------
-- Table structure for student_profile
-- ----------------------------
DROP TABLE IF EXISTS `student_profile`;
CREATE TABLE `student_profile`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '家长资料id',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '用户id（逻辑外键）',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '家长姓名',
  `age` int(11) NULL DEFAULT NULL COMMENT '孩子年龄',
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
-- Records of student_profile
-- ----------------------------

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
-- Records of teacher_job_posting
-- ----------------------------

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
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '教师资料表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of teacher_profile
-- ----------------------------

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
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表' ROW_FORMAT = DYNAMIC;


DROP TABLE IF EXISTS `room`;
CREATE TABLE `room` (
            `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '会话id',
            `teacher_profile_id` bigint(20) NOT NULL COMMENT '教师资料id（逻辑外键）',
            `student_profile_id` bigint(20) NOT NULL COMMENT '学生资料id（逻辑外键）',
            `active_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '最后消息时间',
            `last_msg_id` bigint(20) DEFAULT NULL COMMENT '最后一条消息id',
            `status` tinyint(4) DEFAULT 1 COMMENT '状态 1正常 0关闭/归档',
            `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
            `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
            PRIMARY KEY (`id`),
            UNIQUE KEY `uniq_teacher_student` (`teacher_profile_id`, `student_profile_id`),
            KEY `idx_teacher_profile_id` (`teacher_profile_id`),
            KEY `idx_student_profile_id` (`student_profile_id`),
            KEY `idx_active_time` (`active_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生与教师之间的1对1会话表';


DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
            `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '消息id',
            `room_id` bigint(20) NOT NULL COMMENT '会话id',
            `from_uid` bigint(20) NOT NULL COMMENT '发送者 user_id',
            `to_uid` bigint(20) NOT NULL COMMENT '接收者 user_id',
            `content` varchar(1024) DEFAULT NULL COMMENT '消息内容',
            `reply_msg_id` bigint(20) DEFAULT NULL COMMENT '被回复的消息id',
            `status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '状态 0正常 1删除',
            `gap_count` int DEFAULT NULL COMMENT '与被回复消息的间隔数',
            `type` tinyint(4) NOT NULL DEFAULT 1 COMMENT '消息类型 1文本 2撤回',
            `extra` json DEFAULT NULL COMMENT '扩展信息',
            `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
            `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
            PRIMARY KEY (`id`),
            KEY `idx_room_id` (`room_id`),
            KEY `idx_from_uid` (`from_uid`),
            KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

DROP TABLE IF EXISTS `tutor_appointment`;
CREATE TABLE `tutor_appointment` (
            `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '预约id',
            `parent_id` bigint(20) NOT NULL COMMENT '家长 user_id',
            `tutor_id` bigint(20) NOT NULL COMMENT '教师 user_id',
            `parent_job_posting_id` bigint(20) DEFAULT NULL COMMENT '家长需求贴id',
            `tutor_job_posting_id` bigint(20) DEFAULT NULL COMMENT '教师服务贴id',
            `subject_id` bigint(20) NOT NULL COMMENT '科目id（position_post.id）',
            `class_mode` varchar(50) DEFAULT NULL COMMENT '授课方式：online/offline/both',
            `city` varchar(100) DEFAULT NULL COMMENT '城市（线下）',
            `address` varchar(255) DEFAULT NULL COMMENT '地址（线下）',
            `start_time` datetime(3) NOT NULL COMMENT '开始时间',
            `duration_minutes` int NOT NULL DEFAULT 60 COMMENT '时长（分钟）',
            `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态：1待确认 2已确认 3待改期确认 4已取消 5已完成',
            `created_by` bigint(20) NOT NULL COMMENT '发起人 user_id',
            `proposed_start_time` datetime(3) DEFAULT NULL COMMENT '改期提议时间',
            `proposed_by` bigint(20) DEFAULT NULL COMMENT '改期发起人 user_id',
            `cancel_by` bigint(20) DEFAULT NULL COMMENT '取消人 user_id',
            `remark` varchar(255) DEFAULT NULL COMMENT '备注/取消原因',
            `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
            `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
            PRIMARY KEY (`id`),
            KEY `idx_parent_id` (`parent_id`),
            KEY `idx_tutor_id` (`tutor_id`),
            KEY `idx_status` (`status`),
            KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约/邀约表';


-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1, '用户5678', '13812345678', NULL, NULL, NULL, 2, NULL, NULL, NULL, 0, 1, NULL, '2025-11-30 12:06:31.429', '2025-11-30 12:06:31.429');
INSERT INTO `user` VALUES (3, '用户6909', '15268836909', NULL, NULL, NULL, 2, NULL, NULL, NULL, 0, 2, NULL, '2025-11-30 22:00:46.260', '2025-11-30 22:00:46.260');

SET FOREIGN_KEY_CHECKS = 1;


SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
USE ai_tutor;

-- =========================
-- 1) 科目树（position_post）
-- parent_id=0 视为根节点；子科目挂在年级/类别下
-- =========================
INSERT INTO position_post
(id, parent_id, name, grade, description, sort, enable_status, create_time, update_time)
VALUES
(100, 0, '小学', '小学', '小学段科目', 1, 1, NOW(), NOW()),
(200, 0, '初中', '初中', '初中段科目', 2, 1, NOW(), NOW()),
(300, 0, '高中', '高中', '高中段科目', 3, 1, NOW(), NOW()),
(400, 0, '兴趣', '兴趣', '素质/兴趣类课程', 4, 1, NOW(), NOW()),

(101, 100, '小学数学', '小学', '计算、应用题、奥数启蒙', 11, 1, NOW(), NOW()),
(102, 100, '小学语文', '小学', '阅读理解、作文、基础字词', 12, 1, NOW(), NOW()),
(103, 100, '小学英语', '小学', '自然拼读、单词、口语', 13, 1, NOW(), NOW()),

(201, 200, '初中数学', '初中', '代数、几何、函数', 21, 1, NOW(), NOW()),
(202, 200, '初中英语', '初中', '词汇语法、阅读、写作', 22, 1, NOW(), NOW()),
(203, 200, '初中物理', '初中', '力学、电学基础', 23, 1, NOW(), NOW()),
(204, 200, '初中化学', '初中', '酸碱盐、化学方程式', 24, 1, NOW(), NOW()),
(205, 200, '初中语文', '初中', '阅读、作文、文言文基础', 25, 1, NOW(), NOW()),

(301, 300, '高中数学', '高中', '函数、导数、圆锥曲线', 31, 1, NOW(), NOW()),
(302, 300, '高中英语', '高中', '阅读、完形、写作提分', 32, 1, NOW(), NOW()),
(303, 300, '高中物理', '高中', '电磁学、力学综合', 33, 1, NOW(), NOW()),
(304, 300, '高中化学', '高中', '有机、化学平衡', 34, 1, NOW(), NOW()),
(305, 300, '高中语文', '高中', '现代文阅读、作文', 35, 1, NOW(), NOW()),

(401, 400, '钢琴', '兴趣', '启蒙到考级，基本功/曲目', 41, 1, NOW(), NOW()),
(402, 400, '吉他', '兴趣', '民谣弹唱、和弦与节奏', 42, 1, NOW(), NOW()),
(403, 400, '编程(Python)', '兴趣', '入门语法、算法思维、项目', 43, 1, NOW(), NOW()),
(404, 400, '书法', '兴趣', '硬笔/软笔，结构与章法', 44, 1, NOW(), NOW()),
(405, 400, '美术', '兴趣', '素描/色彩，基础造型', 45, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
parent_id=VALUES(parent_id),
name=VALUES(name),
grade=VALUES(grade),
description=VALUES(description),
sort=VALUES(sort),
enable_status=VALUES(enable_status),
update_time=VALUES(update_time);

-- =========================
-- 2) 用户（user）+ 老师资料（teacher_profile）+ 家长资料（student_profile）
-- user.user_type: 1教师 2家长；ref_id 指向对应 profile.id（逻辑外键）
-- =========================
INSERT INTO `user`
(id, name, phone, avatar, sex, open_id, active_status, last_opt_time, ip_info, item_id, status, user_type, ref_id, create_time, update_time)
VALUES
(10001, '张老师10001', '13900010001', 'https://i.pravatar.cc/150?img=12', 1, NULL, 2, NOW(3), NULL, NULL, 0, 1, 50001, DATE_SUB(NOW(3), INTERVAL 30 DAY), NOW(3)),
(10002, '李老师10002', '13900010002', 'https://i.pravatar.cc/150?img=32', 2, NULL, 2, NOW(3), NULL, NULL, 0, 1, 50002, DATE_SUB(NOW(3), INTERVAL 18 DAY), NOW(3)),
(10003, '王老师10003', '13900010003', 'https://i.pravatar.cc/150?img=22', 1, NULL, 2, NOW(3), NULL, NULL, 0, 1, 50003, DATE_SUB(NOW(3), INTERVAL 12 DAY), NOW(3)),
(10004, '赵老师10004', '13900010004', 'https://i.pravatar.cc/150?img=45', 2, NULL, 2, NOW(3), NULL, NULL, 0, 1, 50004, DATE_SUB(NOW(3), INTERVAL 25 DAY), NOW(3)),
(10005, '陈老师10005', '13900010005', 'https://i.pravatar.cc/150?img=55', 1, NULL, 2, NOW(3), NULL, NULL, 0, 1, 50005, DATE_SUB(NOW(3), INTERVAL 8 DAY), NOW(3)),
(10006, '刘老师10006', '13900010006', 'https://i.pravatar.cc/150?img=14', 2, NULL, 2, NOW(3), NULL, NULL, 0, 1, 50006, DATE_SUB(NOW(3), INTERVAL 16 DAY), NOW(3)),
(10007, '周老师10007', '13900010007', 'https://i.pravatar.cc/150?img=7',  1, NULL, 2, NOW(3), NULL, NULL, 0, 1, 50007, DATE_SUB(NOW(3), INTERVAL 20 DAY), NOW(3)),
(10008, '吴老师10008', '13900010008', 'https://i.pravatar.cc/150?img=9',  2, NULL, 2, NOW(3), NULL, NULL, 0, 1, 50008, DATE_SUB(NOW(3), INTERVAL 6 DAY), NOW(3)),
(10009, '郑老师10009', '13900010009', 'https://i.pravatar.cc/150?img=16', 1, NULL, 2, NOW(3), NULL, NULL, 0, 1, 50009, DATE_SUB(NOW(3), INTERVAL 10 DAY), NOW(3)),
(10010, '孙老师10010', '13900010010', 'https://i.pravatar.cc/150?img=18', 2, NULL, 2, NOW(3), NULL, NULL, 0, 1, 50010, DATE_SUB(NOW(3), INTERVAL 28 DAY), NOW(3)),
(10011, '马老师10011', '13900010011', 'https://i.pravatar.cc/150?img=20', 1, NULL, 2, NOW(3), NULL, NULL, 0, 1, 50011, DATE_SUB(NOW(3), INTERVAL 14 DAY), NOW(3)),
(10012, '胡老师10012', '13900010012', 'https://i.pravatar.cc/150?img=24', 2, NULL, 2, NOW(3), NULL, NULL, 0, 1, 50012, DATE_SUB(NOW(3), INTERVAL 9 DAY), NOW(3)),

(20001, '家长王20001', '13800020001', 'https://i.pravatar.cc/150?img=61', 2, NULL, 2, NOW(3), NULL, NULL, 0, 2, 60001, DATE_SUB(NOW(3), INTERVAL 40 DAY), NOW(3)),
(20002, '家长李20002', '13800020002', 'https://i.pravatar.cc/150?img=62', 1, NULL, 2, NOW(3), NULL, NULL, 0, 2, 60002, DATE_SUB(NOW(3), INTERVAL 33 DAY), NOW(3)),
(20003, '家长赵20003', '13800020003', 'https://i.pravatar.cc/150?img=63', 2, NULL, 2, NOW(3), NULL, NULL, 0, 2, 60003, DATE_SUB(NOW(3), INTERVAL 22 DAY), NOW(3)),
(20004, '家长周20004', '13800020004', 'https://i.pravatar.cc/150?img=64', 1, NULL, 2, NOW(3), NULL, NULL, 0, 2, 60004, DATE_SUB(NOW(3), INTERVAL 19 DAY), NOW(3)),
(20005, '家长陈20005', '13800020005', 'https://i.pravatar.cc/150?img=65', 2, NULL, 2, NOW(3), NULL, NULL, 0, 2, 60005, DATE_SUB(NOW(3), INTERVAL 12 DAY), NOW(3)),
(20006, '家长刘20006', '13800020006', 'https://i.pravatar.cc/150?img=66', 1, NULL, 2, NOW(3), NULL, NULL, 0, 2, 60006, DATE_SUB(NOW(3), INTERVAL 10 DAY), NOW(3)),
(20007, '家长吴20007', '13800020007', 'https://i.pravatar.cc/150?img=67', 2, NULL, 2, NOW(3), NULL, NULL, 0, 2, 60007, DATE_SUB(NOW(3), INTERVAL 9 DAY), NOW(3)),
(20008, '家长郑20008', '13800020008', 'https://i.pravatar.cc/150?img=68', 1, NULL, 2, NOW(3), NULL, NULL, 0, 2, 60008, DATE_SUB(NOW(3), INTERVAL 7 DAY), NOW(3)),
(20009, '家长孙20009', '13800020009', 'https://i.pravatar.cc/150?img=69', 2, NULL, 2, NOW(3), NULL, NULL, 0, 2, 60009, DATE_SUB(NOW(3), INTERVAL 6 DAY), NOW(3)),
(20010, '家长马20010', '13800020010', 'https://i.pravatar.cc/150?img=70', 1, NULL, 2, NOW(3), NULL, NULL, 0, 2, 60010, DATE_SUB(NOW(3), INTERVAL 5 DAY), NOW(3))
ON DUPLICATE KEY UPDATE
phone=VALUES(phone),
avatar=VALUES(avatar),
sex=VALUES(sex),
active_status=VALUES(active_status),
last_opt_time=VALUES(last_opt_time),
status=VALUES(status),
user_type=VALUES(user_type),
ref_id=VALUES(ref_id),
update_time=VALUES(update_time);

INSERT INTO teacher_profile
(id, user_id, real_name, education, subject, experience_years, rate_per_hour, introduction, certificate_urls, status, create_time, update_time)
VALUES
(50001, 10001, '张晨', '北大 本科', '初中数学,高中数学', 4, 180.00, '擅长提分与错题体系化，注重解题思路与复盘。', '["https://example.com/cert/teacher/50001-1.jpg"]', 1, DATE_SUB(NOW(), INTERVAL 30 DAY), NOW()),
(50002, 10002, '李薇', '师范 硕士', '初中英语,高中英语', 6, 220.00, '阅读写作双线提升，强调词汇与语法在语境中掌握。', '["https://example.com/cert/teacher/50002-1.jpg"]', 1, DATE_SUB(NOW(), INTERVAL 18 DAY), NOW()),
(50003, 10003, '王磊', '985 本科', '初中物理,高中物理', 5, 240.00, '物理模型化讲解，善于把复杂题拆解成步骤。', '["https://example.com/cert/teacher/50003-1.jpg"]', 1, DATE_SUB(NOW(), INTERVAL 12 DAY), NOW()),
(50004, 10004, '赵婷', '师范 本科', '初中化学,高中化学', 3, 200.00, '化学方程式与题型训练结合，重视基础与规范书写。', '["https://example.com/cert/teacher/50004-1.jpg"]', 1, DATE_SUB(NOW(), INTERVAL 25 DAY), NOW()),
(50005, 10005, '陈浩', '北邮 本科', '编程(Python)', 2, 160.00, '从零到一做小项目，培养算法思维与代码习惯。', '["https://example.com/cert/teacher/50005-1.jpg"]', 1, DATE_SUB(NOW(), INTERVAL 8 DAY), NOW()),
(50006, 10006, '刘敏', '央音 本科', '钢琴', 7, 260.00, '钢琴启蒙与考级，强调节奏与手型基本功。', '["https://example.com/cert/teacher/50006-1.jpg"]', 1, DATE_SUB(NOW(), INTERVAL 16 DAY), NOW()),
(50007, 10007, '周航', '师范 本科', '小学数学,初中数学', 4, 150.00, '耐心细致，善于帮助孩子建立自信与学习习惯。', '["https://example.com/cert/teacher/50007-1.jpg"]', 1, DATE_SUB(NOW(), INTERVAL 20 DAY), NOW()),
(50008, 10008, '吴楠', '外语 硕士', '小学英语,初中英语', 5, 190.00, '自然拼读+分级阅读，口语表达循序渐进。', '["https://example.com/cert/teacher/50008-1.jpg"]', 1, DATE_SUB(NOW(), INTERVAL 6 DAY), NOW()),
(50009, 10009, '郑凯', '美院 本科', '美术', 3, 180.00, '素描造型基础训练，兼顾创意与观察力。', '["https://example.com/cert/teacher/50009-1.jpg"]', 1, DATE_SUB(NOW(), INTERVAL 10 DAY), NOW()),
(50010, 10010, '孙颖', '师范 本科', '小学语文,初中语文', 6, 210.00, '阅读理解与作文专项，积累素材与表达结构。', '["https://example.com/cert/teacher/50010-1.jpg"]', 1, DATE_SUB(NOW(), INTERVAL 28 DAY), NOW()),
(50011, 10011, '马超', '理工 本科', '高中数学', 5, 260.00, '高考数学提分，擅长压轴题思路梳理。', '["https://example.com/cert/teacher/50011-1.jpg"]', 1, DATE_SUB(NOW(), INTERVAL 14 DAY), NOW()),
(50012, 10012, '胡静', '书法协会', '书法', 8, 200.00, '硬笔规范与软笔入门，结构与章法训练。', '["https://example.com/cert/teacher/50012-1.jpg"]', 1, DATE_SUB(NOW(), INTERVAL 9 DAY), NOW())
ON DUPLICATE KEY UPDATE
real_name=VALUES(real_name),
education=VALUES(education),
subject=VALUES(subject),
experience_years=VALUES(experience_years),
rate_per_hour=VALUES(rate_per_hour),
introduction=VALUES(introduction),
certificate_urls=VALUES(certificate_urls),
status=VALUES(status),
update_time=VALUES(update_time);

INSERT INTO student_profile
(id, user_id, real_name, age, address, demand_description, budget, status, create_time, update_time)
VALUES
(60001, 20001, '王女士', 10, '北京市海淀区', '孩子基础一般，希望提升应用题与计算准确率。', 150.00, 1, DATE_SUB(NOW(3), INTERVAL 40 DAY), NOW(3)),
(60002, 20002, '李先生', 13, '北京市朝阳区', '想系统补英语词汇语法，目标期末提升。', 180.00, 1, DATE_SUB(NOW(3), INTERVAL 33 DAY), NOW(3)),
(60003, 20003, '赵女士', 15, '北京市西城区', '物理电学薄弱，需要针对性练题。', 220.00, 1, DATE_SUB(NOW(3), INTERVAL 22 DAY), NOW(3)),
(60004, 20004, '周先生', 8,  '北京市丰台区', '语文阅读理解差，想提高总结与表达。', 140.00, 1, DATE_SUB(NOW(3), INTERVAL 19 DAY), NOW(3)),
(60005, 20005, '陈女士', 12, '北京市昌平区', '数学几何不牢，想建立知识框架。', 180.00, 1, DATE_SUB(NOW(3), INTERVAL 12 DAY), NOW(3)),
(60006, 20006, '刘先生', 9,  '北京市通州区', '孩子学钢琴准备考级，需要每周固定练习指导。', 260.00, 1, DATE_SUB(NOW(3), INTERVAL 10 DAY), NOW(3)),
(60007, 20007, '吴女士', 14, '北京市海淀区', '化学方程式不熟，希望夯实基础。', 200.00, 1, DATE_SUB(NOW(3), INTERVAL 9 DAY), NOW(3)),
(60008, 20008, '郑先生', 16, '北京市朝阳区', '冲刺高考数学，想做专题突破。', 280.00, 1, DATE_SUB(NOW(3), INTERVAL 7 DAY), NOW(3)),
(60009, 20009, '孙女士', 11, '北京市石景山区', '想学Python做项目，培养兴趣与逻辑。', 160.00, 1, DATE_SUB(NOW(3), INTERVAL 6 DAY), NOW(3)),
(60010, 20010, '马先生', 10, '北京市东城区', '孩子写字不规范，想练硬笔书法。', 200.00, 1, DATE_SUB(NOW(3), INTERVAL 5 DAY), NOW(3))
ON DUPLICATE KEY UPDATE
real_name=VALUES(real_name),
age=VALUES(age),
address=VALUES(address),
demand_description=VALUES(demand_description),
budget=VALUES(budget),
status=VALUES(status),
update_time=VALUES(update_time);

-- =========================
-- 3) 老师服务贴（teacher_job_posting） status=1 上架
-- mode: online/offline/both；city 用北京为主（方便首页筛 city）
-- =========================
INSERT INTO teacher_job_posting
(id, tutor_id, subject_id, title, description, price_per_hour, mode, city, available_time, max_students, status, create_time, update_time)
VALUES
(70001, 10001, 201, '初中数学一对一提分（函数/几何）', '按章节+题型训练，配套错题本与周测。', 180.00, 'online',  '北京', '["Tue 19-21","Sat 10-12"]', 1, 1, DATE_SUB(NOW(), INTERVAL 12 DAY), NOW()),
(70002, 10001, 301, '高中数学系统复盘（导数/圆锥曲线）', '梳理知识网络，专项突破压轴题。', 260.00, 'online',  '北京', '["Wed 19-21","Sun 14-16"]', 1, 1, DATE_SUB(NOW(), INTERVAL 9 DAY), NOW()),
(70003, 10002, 202, '初中英语阅读写作提升', '词汇语法在语境中掌握，写作模板+真题演练。', 220.00, 'online',  '北京', '["Mon 19-21","Thu 19-21"]', 1, 1, DATE_SUB(NOW(), INTERVAL 10 DAY), NOW()),
(70004, 10002, 302, '高中英语写作专项（提分）', '审题-结构-表达三步法，改作文到位。', 240.00, 'online',  '北京', '["Sat 19-21"]', 1, 1, DATE_SUB(NOW(), INTERVAL 7 DAY), NOW()),
(70005, 10003, 203, '初中物理电学专项', '电路分析与题型归纳，做题更稳。', 240.00, 'online',  '北京', '["Tue 20-22","Sun 10-12"]', 1, 1, DATE_SUB(NOW(), INTERVAL 11 DAY), NOW()),
(70006, 10003, 303, '高中物理力学综合', '受力分析+能量守恒体系化训练。', 260.00, 'online',  '北京', '["Wed 20-22"]', 1, 1, DATE_SUB(NOW(), INTERVAL 8 DAY), NOW()),
(70007, 10004, 204, '初中化学基础夯实', '酸碱盐与化学方程式规范训练。', 200.00, 'online',  '北京', '["Mon 20-22"]', 1, 1, DATE_SUB(NOW(), INTERVAL 13 DAY), NOW()),
(70008, 10004, 304, '高中化学有机入门', '结构-性质-反应路线，配套练习。', 230.00, 'online',  '北京', '["Fri 19-21"]', 1, 1, DATE_SUB(NOW(), INTERVAL 6 DAY), NOW()),
(70009, 10005, 403, 'Python入门+小项目（零基础）', '从语法到项目：爬虫/数据处理/小游戏。', 160.00, 'online',  '北京', '["Sat 14-16","Sun 14-16"]', 1, 1, DATE_SUB(NOW(), INTERVAL 6 DAY), NOW()),
(70010, 10005, 403, 'Python算法与刷题入门', '循环/递归/搜索/排序，培养代码思维。', 180.00, 'online',  '北京', '["Wed 19-21"]', 1, 1, DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()),
(70011, 10006, 401, '钢琴启蒙（节奏/手型/识谱）', '以兴趣为导向，打好基本功。', 260.00, 'offline', '北京', '["Sat 10-12","Sun 10-12"]', 1, 1, DATE_SUB(NOW(), INTERVAL 16 DAY), NOW()),
(70012, 10006, 401, '钢琴考级辅导（1-6级）', '曲目+视奏+乐理，制定练琴计划。', 320.00, 'offline', '北京', '["Wed 18-20"]', 1, 1, DATE_SUB(NOW(), INTERVAL 4 DAY), NOW()),
(70013, 10007, 101, '小学数学计算与应用题', '夯实基础，提升正确率与解题步骤。', 150.00, 'both',   '北京', '["Tue 18-20","Thu 18-20"]', 1, 1, DATE_SUB(NOW(), INTERVAL 20 DAY), NOW()),
(70014, 10007, 201, '初中数学培优（中等偏上）', '专题训练+错题归纳，冲刺更高分。', 180.00, 'online', '北京', '["Sat 09-11"]', 1, 1, DATE_SUB(NOW(), INTERVAL 5 DAY), NOW()),
(70015, 10008, 103, '小学英语自然拼读+口语', '从拼读规则到阅读表达，轻松开口。', 160.00, 'online', '北京', '["Mon 18-20"]', 1, 1, DATE_SUB(NOW(), INTERVAL 6 DAY), NOW()),
(70016, 10008, 202, '初中英语词汇语法专项', '高频词+核心语法，搭配真题练习。', 190.00, 'online', '北京', '["Thu 20-22"]', 1, 1, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
(70017, 10009, 405, '美术素描基础（静物/结构）', '从线条到结构，提升观察与表达。', 180.00, 'offline', '北京', '["Sat 14-16"]', 1, 1, DATE_SUB(NOW(), INTERVAL 10 DAY), NOW()),
(70018, 10010, 102, '小学语文阅读与作文', '素材积累+结构表达，写作不再难。', 180.00, 'online', '北京', '["Wed 18-20"]', 1, 1, DATE_SUB(NOW(), INTERVAL 28 DAY), NOW()),
(70019, 10010, 205, '初中语文阅读理解提升', '方法论+题型训练，提升答题规范。', 210.00, 'online', '北京', '["Sun 19-21"]', 1, 1, DATE_SUB(NOW(), INTERVAL 8 DAY), NOW()),
(70020, 10011, 301, '高中数学冲刺（压轴题思路）', '分类突破，训练思维链路与书写规范。', 280.00, 'online', '北京', '["Sat 19-21","Sun 19-21"]', 1, 1, DATE_SUB(NOW(), INTERVAL 14 DAY), NOW()),
(70021, 10012, 404, '硬笔书法规范训练', '字形结构与章法，纠正握笔姿势。', 200.00, 'both',  '北京', '["Tue 19-20","Fri 19-20"]', 1, 1, DATE_SUB(NOW(), INTERVAL 9 DAY), NOW()),

-- 再补一些服务贴，让首页更“热闹”
(70022, 10001, 201, '初中数学基础补弱（同步+练习）', '同步巩固+错题复盘，稳步提升。', 170.00, 'online', '北京', '["Mon 19-21"]', 1, 1, DATE_SUB(NOW(), INTERVAL 4 DAY), NOW()),
(70023, 10002, 202, '初中英语口语陪练（发音/对话）', '场景化对话，提升表达与自信。', 180.00, 'online', '北京', '["Wed 18-19","Fri 18-19"]', 1, 1, DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()),
(70024, 10003, 203, '初中物理力学入门', '从概念到题型，打好基础。', 200.00, 'online', '北京', '["Thu 19-21"]', 1, 1, DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()),
(70025, 10004, 204, '初中化学计算题专项', '方程式配平+计算题规范步骤。', 210.00, 'online', '北京', '["Sun 14-16"]', 1, 1, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
(70026, 10007, 101, '小学数学思维启蒙', '趣味题训练思维，培养数学兴趣。', 160.00, 'online', '北京', '["Sat 10-12"]', 1, 1, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
(70027, 10008, 103, '小学英语分级阅读', '分级阅读+复述表达，提升语感。', 170.00, 'online', '北京', '["Sun 10-12"]', 1, 1, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
(70028, 10010, 205, '初中语文作文专项（审题立意）', '审题-结构-素材，写作提升快。', 220.00, 'online', '北京', '["Tue 20-22"]', 1, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(70029, 10011, 301, '高中数学基础巩固（必修）', '知识点清单化复盘+练习巩固。', 240.00, 'online', '北京', '["Thu 20-22"]', 1, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(70030, 10005, 403, 'Python数据分析入门', 'pandas+可视化，做小数据项目。', 200.00, 'online', '北京', '["Sat 16-18"]', 1, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW())
ON DUPLICATE KEY UPDATE
tutor_id=VALUES(tutor_id),
subject_id=VALUES(subject_id),
title=VALUES(title),
description=VALUES(description),
price_per_hour=VALUES(price_per_hour),
mode=VALUES(mode),
city=VALUES(city),
available_time=VALUES(available_time),
max_students=VALUES(max_students),
status=VALUES(status),
update_time=VALUES(update_time);

-- =========================
-- 4) 家长需求贴（student_job_posting） status=1 发布中
-- =========================
INSERT INTO student_job_posting
(id, parent_id, subject_id, title, description, child_age, class_mode, city, address, budget_min, budget_max, schedule, status, create_time, update_time)
VALUES
(80001, 20001, 101, '小学三年级数学家教（补基础）', '主要是计算和应用题，想把错误率降下来。', 10, 'online',  '北京', '北京市海淀区', 120.00, 160.00, '["Tue 19-21","Sat 10-12"]', 1, DATE_SUB(NOW(), INTERVAL 9 DAY), NOW()),
(80002, 20002, 202, '初二英语词汇语法提升', '阅读理解做题慢，词汇量不够，希望系统补齐。', 13, 'online', '北京', '北京市朝阳区', 160.00, 220.00, '["Mon 19-21","Thu 19-21"]', 1, DATE_SUB(NOW(), INTERVAL 8 DAY), NOW()),
(80003, 20003, 203, '初三物理电学专项冲刺', '电路题总丢分，希望针对题型训练。', 15, 'online', '北京', '北京市西城区', 180.00, 260.00, '["Wed 19-21","Sun 10-12"]', 1, DATE_SUB(NOW(), INTERVAL 7 DAY), NOW()),
(80004, 20004, 102, '小学语文阅读理解与作文', '阅读理解抓不住重点，作文结构混乱。', 8, 'online',  '北京', '北京市丰台区', 120.00, 180.00, '["Sat 14-16"]', 1, DATE_SUB(NOW(), INTERVAL 6 DAY), NOW()),
(80005, 20005, 201, '初一数学几何补弱', '几何证明题无从下手，需要方法。', 12, 'online',  '北京', '北京市昌平区', 160.00, 220.00, '["Tue 20-22"]', 1, DATE_SUB(NOW(), INTERVAL 5 DAY), NOW()),
(80006, 20006, 401, '钢琴考级辅导（每周固定）', '准备考级，希望老师能制定练琴计划。', 9, 'offline', '北京', '北京市通州区', 240.00, 360.00, '["Sat 10-12"]', 1, DATE_SUB(NOW(), INTERVAL 5 DAY), NOW()),
(80007, 20007, 204, '初中化学基础夯实', '方程式和计算题薄弱，需要系统练习。', 14, 'online', '北京', '北京市海淀区', 160.00, 240.00, '["Sun 14-16"]', 1, DATE_SUB(NOW(), INTERVAL 4 DAY), NOW()),
(80008, 20008, 301, '高二数学专题突破（导数/数列）', '冲刺高分，希望做专题体系化训练。', 16, 'online', '北京', '北京市朝阳区', 220.00, 320.00, '["Sat 19-21","Sun 19-21"]', 1, DATE_SUB(NOW(), INTERVAL 4 DAY), NOW()),
(80009, 20009, 403, 'Python入门（兴趣+项目）', '希望做小项目培养兴趣，最好能有作业反馈。', 11, 'online', '北京', '北京市石景山区', 140.00, 220.00, '["Wed 19-21","Sat 14-16"]', 1, DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()),
(80010, 20010, 404, '硬笔书法纠正（字形结构）', '握笔姿势不对，写字不工整，需要纠正。', 10, 'both', '北京', '北京市东城区', 160.00, 240.00, '["Fri 19-20","Sun 10-11"]', 1, DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()),

-- 再补一些需求贴
(80011, 20001, 103, '小学英语口语提升', '不敢开口，希望多对话练习。', 10, 'online', '北京', '北京市海淀区', 120.00, 180.00, '["Mon 18-19","Thu 18-19"]', 1, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
(80012, 20002, 205, '初中语文阅读理解提高', '答题不规范，希望掌握方法。', 13, 'online', '北京', '北京市朝阳区', 150.00, 220.00, '["Sun 14-16"]', 1, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
(80013, 20003, 304, '高中化学有机专项', '有机反应路线记不住，需要梳理。', 15, 'online', '北京', '北京市西城区', 200.00, 280.00, '["Sat 10-12"]', 1, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
(80014, 20008, 302, '高中英语写作提分', '写作得分低，希望系统提升。', 16, 'online', '北京', '北京市朝阳区', 200.00, 300.00, '["Thu 19-21"]', 1, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(80015, 20009, 405, '美术素描基础训练', '想系统学素描，提升造型能力。', 11, 'offline', '北京', '北京市石景山区', 160.00, 240.00, '["Sat 14-16"]', 1, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW())
ON DUPLICATE KEY UPDATE
parent_id=VALUES(parent_id),
subject_id=VALUES(subject_id),
title=VALUES(title),
description=VALUES(description),
child_age=VALUES(child_age),
class_mode=VALUES(class_mode),
city=VALUES(city),
address=VALUES(address),
budget_min=VALUES(budget_min),
budget_max=VALUES(budget_max),
schedule=VALUES(schedule),
status=VALUES(status),
update_time=VALUES(update_time);

-- =========================
-- 5) 少量会话与消息（room/message）用于 IM 联调
-- =========================
INSERT INTO room
(id, teacher_profile_id, student_profile_id, active_time, last_msg_id, status, create_time, update_time)
VALUES
(90001, 50001, 60001, NOW(3), 91003, 1, DATE_SUB(NOW(3), INTERVAL 6 DAY), NOW(3)),
(90002, 50002, 60002, NOW(3), 91006, 1, DATE_SUB(NOW(3), INTERVAL 5 DAY), NOW(3)),
(90003, 50003, 60003, NOW(3), 91009, 1, DATE_SUB(NOW(3), INTERVAL 4 DAY), NOW(3)),
(90004, 50006, 60006, NOW(3), 91012, 1, DATE_SUB(NOW(3), INTERVAL 3 DAY), NOW(3)),
(90005, 50005, 60009, NOW(3), 91015, 1, DATE_SUB(NOW(3), INTERVAL 2 DAY), NOW(3))
ON DUPLICATE KEY UPDATE
active_time=VALUES(active_time),
last_msg_id=VALUES(last_msg_id),
status=VALUES(status),
update_time=VALUES(update_time);

INSERT INTO message
(id, room_id, from_uid, to_uid, content, reply_msg_id, status, gap_count, type, extra, create_time, update_time)
VALUES
(91001, 90001, 20001, 10001, '老师您好，孩子最近应用题总出错，想从基础开始补。', NULL, 0, NULL, 1, NULL, DATE_SUB(NOW(3), INTERVAL 6 DAY), NOW(3)),
(91002, 90001, 10001, 20001, '好的，我建议先做一次小测，定位薄弱点，再按题型训练。', NULL, 0, NULL, 1, NULL, DATE_SUB(NOW(3), INTERVAL 6 DAY), NOW(3)),
(91003, 90001, 20001, 10001, '可以的，那我们先约一次试课吗？', NULL, 0, NULL, 1, NULL, DATE_SUB(NOW(3), INTERVAL 6 DAY), NOW(3)),

(91004, 90002, 20002, 10002, '孩子英语阅读慢，词汇量不够，想系统提升。', NULL, 0, NULL, 1, NULL, DATE_SUB(NOW(3), INTERVAL 5 DAY), NOW(3)),
(91005, 90002, 10002, 20002, '没问题，我这边会按词族+语法点安排，配套阅读材料。', NULL, 0, NULL, 1, NULL, DATE_SUB(NOW(3), INTERVAL 5 DAY), NOW(3)),
(91006, 90002, 20002, 10002, '太好了，我们下周一晚上可以上课。', NULL, 0, NULL, 1, NULL, DATE_SUB(NOW(3), INTERVAL 5 DAY), NOW(3)),

(91007, 90003, 20003, 10003, '电学电路题总错，感觉没思路。', NULL, 0, NULL, 1, NULL, DATE_SUB(NOW(3), INTERVAL 4 DAY), NOW(3)),
(91008, 90003, 10003, 20003, '先把串并联与等效电阻打牢，再做典型题型。', NULL, 0, NULL, 1, NULL, DATE_SUB(NOW(3), INTERVAL 4 DAY), NOW(3)),
(91009, 90003, 20003, 10003, '好的，那先按您说的来。', NULL, 0, NULL, 1, NULL, DATE_SUB(NOW(3), INTERVAL 4 DAY), NOW(3)),

(91010, 90004, 20006, 10006, '孩子钢琴考级想冲一冲，练琴效率不高。', NULL, 0, NULL, 1, NULL, DATE_SUB(NOW(3), INTERVAL 3 DAY), NOW(3)),
(91011, 90004, 10006, 20006, '我会给练琴计划和节奏训练方法，每周复盘。', NULL, 0, NULL, 1, NULL, DATE_SUB(NOW(3), INTERVAL 3 DAY), NOW(3)),
(91012, 90004, 20006, 10006, '好的，周六上午可以上课。', NULL, 0, NULL, 1, NULL, DATE_SUB(NOW(3), INTERVAL 3 DAY), NOW(3)),

(91013, 90005, 20009, 10005, '想学Python做点小项目，有推荐的路线吗？', NULL, 0, NULL, 1, NULL, DATE_SUB(NOW(3), INTERVAL 2 DAY), NOW(3)),
(91014, 90005, 10005, 20009, '可以从语法+小项目开始，比如小游戏/爬虫/数据处理。', NULL, 0, NULL, 1, NULL, DATE_SUB(NOW(3), INTERVAL 2 DAY), NOW(3)),
(91015, 90005, 20009, 10005, '好的，那我们先从小游戏开始。', NULL, 0, NULL, 1, NULL, DATE_SUB(NOW(3), INTERVAL 2 DAY), NOW(3))
ON DUPLICATE KEY UPDATE
content=VALUES(content),
status=VALUES(status),
type=VALUES(type),
update_time=VALUES(update_time);

-- =========================
-- 6) 少量预约（tutor_appointment）用于闭环联调
-- status: 1待确认 2已确认 4已取消 5已完成
-- =========================
INSERT INTO tutor_appointment
(id, parent_id, tutor_id, parent_job_posting_id, tutor_job_posting_id, subject_id, class_mode, city, address, start_time, duration_minutes, status, created_by, proposed_start_time, proposed_by, cancel_by, remark, create_time, update_time)
VALUES
(92001, 20001, 10001, 80001, 70001, 101, 'online',  '北京', '北京市海淀区', DATE_ADD(NOW(3), INTERVAL 2 DAY), 60, 1, 20001, NULL, NULL, NULL, '想先做一次试课', DATE_SUB(NOW(3), INTERVAL 1 DAY), NOW(3)),
(92002, 20002, 10002, 80002, 70003, 202, 'online',  '北京', '北京市朝阳区', DATE_ADD(NOW(3), INTERVAL 3 DAY), 60, 2, 20002, NULL, NULL, NULL, '阅读写作同步提升', DATE_SUB(NOW(3), INTERVAL 2 DAY), NOW(3)),
(92003, 20006, 10006, 80006, 70011, 401, 'offline', '北京', '北京市通州区', DATE_ADD(NOW(3), INTERVAL 4 DAY), 60, 1, 20006, NULL, NULL, NULL, '考级辅导', DATE_SUB(NOW(3), INTERVAL 2 DAY), NOW(3)),
(92004, 20009, 10005, 80009, 70009, 403, 'online',  '北京', '北京市石景山区', DATE_ADD(NOW(3), INTERVAL 1 DAY), 60, 5, 20009, NULL, NULL, NULL, '已完成一次课', DATE_SUB(NOW(3), INTERVAL 6 DAY), NOW(3)),
(92005, 20003, 10003, 80003, 70005, 203, 'online',  '北京', '北京市西城区', DATE_ADD(NOW(3), INTERVAL 5 DAY), 60, 4, 20003, NULL, NULL, 20003, '时间冲突先取消', DATE_SUB(NOW(3), INTERVAL 3 DAY), NOW(3))
ON DUPLICATE KEY UPDATE
status=VALUES(status),
start_time=VALUES(start_time),
duration_minutes=VALUES(duration_minutes),
remark=VALUES(remark),
update_time=VALUES(update_time);

SET FOREIGN_KEY_CHECKS = 1;