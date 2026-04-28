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
  `subject_id` bigint(20) NULL DEFAULT NULL COMMENT '需求科目ID（position_post.id）',
  `subject_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '科目名称（不区分年级）',
  `subject_is_other` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否为其他自定义科目：1是 0否',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '需求标题，如：小学三年级数学家教',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '课程/需求详情描述',
  `student_gender` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学员性别：male/female',
  `grade_code` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '学生年级编码：PRESCHOOL/GRADE1~6/JUNIOR1~3/SENIOR1~3/SELF_EXAM/COLLEGE1~4/ADULT',
  `available_time` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '可上课时间（自由文本）',
  `teacher_gender_preference` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'both' COMMENT '教师性别偏好：male/female/both',
  `teacher_requirement_detail` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '对教员的详细要求（自由文本）',
  `child_age` int(11) NULL DEFAULT NULL COMMENT '孩子年龄',
  `class_mode` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'online' COMMENT '授课方式：online/offline',
  `city` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '线下授课所在城市（offline 时必填）',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '具体上课地址（offline 时可选）',
  `frequency_per_week` int(11) NOT NULL DEFAULT 2 COMMENT '授课频次（每周几次）',
  `budget_min` decimal(10, 2) NULL DEFAULT NULL COMMENT '预算下限（每小时）',
  `budget_max` decimal(10, 2) NULL DEFAULT NULL COMMENT '预算上限（每小时）',
  `stage_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '授课学段：PRESCHOOL/PRIMARY/JUNIOR/SENIOR/OTHER',
  `education_requirement` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '学历要求：TOP2/C985/C211/DOUBLE_FIRST_CLASS/FIRST_TIER/BACHELOR/OVERSEAS/QS50 等',
  `publisher_identity` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PARENT' COMMENT '发布者身份：PARENT/STUDENT_SELF/ORGANIZATION',
  `schedule` json NULL COMMENT '期望上课时间，例如：[\"Tue 19-21\",\"Sat 10-12\"]',
  `biz_status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '业务状态：1匹配中 2待支付解锁 3沟通中 4合作中 5已结课 6已关闭',
  `reject_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '审核拒绝原因',
  `status` tinyint(4) NULL DEFAULT 0 COMMENT '状态：0-待审核 1-发布中 2-已拒绝 3-已关闭',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_parent_id`(`parent_id`) USING BTREE,
  INDEX `idx_subject_id`(`subject_id`) USING BTREE,
  INDEX `idx_biz_status`(`biz_status`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '家长发布的家教岗位需求表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of student_job_posting
-- ----------------------------

-- ----------------------------
-- Table structure for tutor_favorite_demand
-- ----------------------------
DROP TABLE IF EXISTS `tutor_favorite_demand`;
CREATE TABLE `tutor_favorite_demand`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `tutor_id` bigint(20) NOT NULL COMMENT '教师用户ID（user.id）',
  `demand_id` bigint(20) NOT NULL COMMENT '需求贴ID（student_job_posting.id）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tutor_demand`(`tutor_id`, `demand_id`) USING BTREE,
  INDEX `idx_tutor_id`(`tutor_id`) USING BTREE,
  INDEX `idx_demand_id`(`demand_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '教师收藏需求贴表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for parent_favorite_tutor
-- ----------------------------
DROP TABLE IF EXISTS `parent_favorite_tutor`;
CREATE TABLE `parent_favorite_tutor`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `parent_id` bigint(20) NOT NULL COMMENT '家长用户ID（user.id）',
  `tutor_id` bigint(20) NOT NULL COMMENT '教师用户ID（user.id）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_parent_tutor`(`parent_id`, `tutor_id`) USING BTREE,
  INDEX `idx_parent_id`(`parent_id`) USING BTREE,
  INDEX `idx_tutor_id`(`tutor_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '家长收藏教师表' ROW_FORMAT = Dynamic;

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
  `default_greeting` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '默认打招呼语',
  `certificate_urls` json NULL COMMENT '教师证书或资格证明文件链接',
  `basic_completed` tinyint(1) NOT NULL DEFAULT 0 COMMENT '基础信息是否已补全 0否 1是',
  `resume_completed` tinyint(1) NOT NULL DEFAULT 0 COMMENT '简历是否已补全 0否 1是',
  `city` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '所在城市',
  `highest_edu_school` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最高学历学校',
  `teaching_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '历史字段（已弃用，不再作为授课匹配依据）',
  `realname_verify_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '实名认证状态 0未提交 1审核中 2通过 3驳回',
  `realname_verify_method` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '实名认证提交方式 ID_PHOTO/NAME_IDNO',
  `realname_verify_id_front_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '身份证人像面截图',
  `realname_verify_id_back_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '身份证国徽面截图',
  `realname_verify_idno_cipher` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '身份证号密文/Hash',
  `realname_verify_idno_masked` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '身份证号脱敏展示',
  `realname_verify_reject_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '实名认证驳回原因',
  `realname_verify_submit_time` datetime NULL DEFAULT NULL COMMENT '实名认证提交时间',
  `realname_verify_time` datetime NULL DEFAULT NULL COMMENT '实名认证通过时间',
  `edu_verify_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '学籍/学历认证状态 0未提交 1审核中 2通过 3驳回',
  `edu_verify_proof_urls` json NULL COMMENT '学籍/学历认证材料截图（JSON数组）',
  `edu_verify_reject_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '学籍/学历认证驳回原因',
  `edu_verify_submit_time` datetime NULL DEFAULT NULL COMMENT '学籍/学历认证提交时间',
  `edu_verify_time` datetime NULL DEFAULT NULL COMMENT '学籍/学历认证通过时间',
  `home_star_teacher` tinyint(4) NOT NULL DEFAULT 0 COMMENT '首页星级教师标记 0否 1是',
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
  `user_type` tinyint(1) NOT NULL COMMENT '用户类型 1教师 2家长 3机构',
  `ref_id` bigint(20) NULL DEFAULT NULL COMMENT '逻辑外键，指向教师表或家长表id',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniq_open_id`(`open_id`) USING BTREE,
  UNIQUE INDEX `uniq_phone`(`phone`) USING BTREE,
  INDEX `idx_create_time`(`create_time`) USING BTREE,
  INDEX `idx_update_time`(`update_time`) USING BTREE,
  INDEX `idx_active_status_last_opt_time`(`active_status`, `last_opt_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表' ROW_FORMAT = DYNAMIC;

DROP TABLE IF EXISTS `user_settings`;
CREATE TABLE `user_settings` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '设置id',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '用户id',
  `application_greeting` varchar(500) DEFAULT NULL COMMENT '默认申请问候语',
  `settings_json` json DEFAULT NULL COMMENT '扩展设置JSON',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户设置表';

DROP TABLE IF EXISTS `organization_profile`;
CREATE TABLE `organization_profile` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '机构资料id',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '机构主账号 user_id（逻辑外键）',
  `org_name` varchar(100) NOT NULL COMMENT '机构名称',
  `intro` varchar(2000) DEFAULT NULL COMMENT '机构介绍',
  `contact_name` varchar(50) DEFAULT NULL COMMENT '联系人姓名',
  `contact_phone` varchar(32) DEFAULT NULL COMMENT '联系人电话',
  `address` varchar(255) DEFAULT NULL COMMENT '机构地址',
  `license_no` varchar(64) DEFAULT NULL COMMENT '营业执照号/统一社会信用代码',
  `split_platform_percent` int NOT NULL DEFAULT 50 COMMENT '平台分成比例（百分比）',
  `split_org_percent` int NOT NULL DEFAULT 50 COMMENT '机构分成比例（百分比）',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态 1正常 0禁用',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_org_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机构资料表';

DROP TABLE IF EXISTS `organization_account`;
CREATE TABLE `organization_account` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '机构账号id',
  `org_user_id` bigint(20) UNSIGNED NOT NULL COMMENT '机构主账号 user_id（逻辑外键）',
  `username` varchar(50) NOT NULL COMMENT '登录账号（由管理端创建发放）',
  `password_hash` varchar(128) NOT NULL COMMENT 'BCrypt 密码哈希',
  `must_change_password` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否首次登录强制改密 1是 0否',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态 1正常 0禁用',
  `last_login_time` datetime(3) DEFAULT NULL COMMENT '最后登录时间',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_username` (`username`),
  UNIQUE KEY `uniq_org_user_id` (`org_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机构登录账号表';


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
            `is_masked` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否命中屏蔽规则 0否 1是',
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

DROP TABLE IF EXISTS `chat_realtime_event`;
CREATE TABLE `chat_realtime_event` (
            `event_id` bigint(20) UNSIGNED NOT NULL COMMENT '统一实时事件id',
            `target_uid` bigint(20) NOT NULL COMMENT '事件目标用户id',
            `event_type` varchar(64) NOT NULL COMMENT '统一事件类型',
            `biz_type` varchar(32) NOT NULL COMMENT '业务域类型',
            `room_id` bigint(20) DEFAULT NULL COMMENT '关联会话id',
            `msg_id` bigint(20) DEFAULT NULL COMMENT '关联消息id',
            `occurred_at` datetime(3) NOT NULL COMMENT '事件发生时间',
            `payload_json` longtext NOT NULL COMMENT '事件负载json',
            `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
            PRIMARY KEY (`event_id`),
            KEY `idx_target_uid_event_id` (`target_uid`, `event_id`),
            KEY `idx_target_uid_occurred_at` (`target_uid`, `occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IM实时事件补偿表';

DROP TABLE IF EXISTS `collaboration_proposal`;
CREATE TABLE `collaboration_proposal` (
            `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '合作提案id',
            `room_id` bigint(20) NOT NULL COMMENT '会话id',
            `from_uid` bigint(20) NOT NULL COMMENT '发起人 user_id',
            `to_uid` bigint(20) NOT NULL COMMENT '接收人 user_id',
            `price_per_hour` varchar(64) NOT NULL COMMENT '收费标准（每小时）',
            `class_time` varchar(255) NOT NULL COMMENT '上课时间（自由文本）',
            `frequency_per_week` int NOT NULL COMMENT '上课频次（每周次数）',
            `trial_start_at` datetime(3) DEFAULT NULL COMMENT '试课开始时间',
            `trial_end_at` datetime(3) DEFAULT NULL COMMENT '试课结束时间',
            `remark` varchar(1024) DEFAULT NULL COMMENT '试课合作备注',
            `expire_at` datetime(3) DEFAULT NULL COMMENT '提案过期时间',
            `client_request_id` varchar(128) DEFAULT NULL COMMENT '客户端幂等键',
            `status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/ACCEPTED/REJECTED',
            `actor_uid` bigint(20) DEFAULT NULL COMMENT '操作人 user_id（同意/拒绝）',
            `action_time` datetime(3) DEFAULT NULL COMMENT '操作时间',
            `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
            `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
            PRIMARY KEY (`id`),
            KEY `idx_room_id` (`room_id`),
            KEY `idx_to_uid_status` (`to_uid`, `status`),
            KEY `idx_collab_room_status_expire` (`room_id`, `status`, `expire_at`),
            KEY `idx_collab_from_client_req` (`from_uid`, `client_request_id`),
            KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天合作提案表';

DROP TABLE IF EXISTS `brokerage_order`;
CREATE TABLE `brokerage_order` (
            `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '中介费订单id',
            `proposal_id` bigint(20) UNSIGNED DEFAULT NULL COMMENT '合作提案id',
            `application_id` bigint(20) UNSIGNED DEFAULT NULL COMMENT '申请id',
            `room_id` bigint(20) UNSIGNED DEFAULT NULL COMMENT '会话id',
            `payer_uid` bigint(20) UNSIGNED NOT NULL COMMENT '付款人uid（教师）',
            `amount_fen` bigint(20) UNSIGNED NOT NULL COMMENT '中介费金额（分）',
            `original_amount_fen` bigint(20) UNSIGNED DEFAULT NULL COMMENT '优惠前信息费金额（分）',
            `discount_amount_fen` bigint(20) UNSIGNED NOT NULL DEFAULT 0 COMMENT '优惠金额（分）',
            `promotion_type` varchar(64) DEFAULT NULL COMMENT '促销类型，如 SYSTEM_INVITE',
            `promotion_snapshot_json` json DEFAULT NULL COMMENT '促销配置快照',
            `pay_method` varchar(32) DEFAULT NULL COMMENT '支付方式 WECHAT/ALIPAY',
            `status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '订单状态 PENDING/PROOF_SUBMITTED/PAID/REFUND_REVIEW/TRIAL_REFUND_REVIEW/REFUNDED/REJECTED/CANCELED',
            `proof_url` varchar(1024) DEFAULT NULL COMMENT '支付凭证URL',
            `proof_note` varchar(512) DEFAULT NULL COMMENT '支付备注',
            `paid_at` datetime(3) DEFAULT NULL COMMENT '确认到账时间',
            `refund_locked` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否进入退款流程（0否 1是）',
            `refunded_amount_fen` bigint(20) UNSIGNED NOT NULL DEFAULT 0 COMMENT '已退款金额（分），支持部分退款追溯',
            `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
            `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
            PRIMARY KEY (`id`),
            UNIQUE KEY `uniq_proposal_id` (`proposal_id`),
            UNIQUE KEY `uniq_application_id` (`application_id`),
            KEY `idx_room_id` (`room_id`),
            KEY `idx_payer_uid` (`payer_uid`),
            KEY `idx_status` (`status`),
            KEY `idx_brokerage_order_refund_locked` (`refund_locked`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='中介费订单表';

DROP TABLE IF EXISTS `payment_order`;
CREATE TABLE `payment_order` (
            `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '支付订单ID',
            `order_no` varchar(64) NOT NULL COMMENT '商户订单号（唯一）',
            `user_id` bigint(20) NOT NULL COMMENT '支付用户ID',
            `amount` bigint(20) NOT NULL COMMENT '支付金额（单位：分）',
            `currency` varchar(8) NOT NULL DEFAULT 'CNY' COMMENT '币种',
            `channel` varchar(32) NOT NULL COMMENT '支付渠道：ALIPAY, WECHAT',
            `provider` varchar(32) NOT NULL DEFAULT 'YUNGOUOS' COMMENT '支付提供方：YUNGOUOS',
            `status` varchar(32) NOT NULL COMMENT '订单状态：PENDING, SUCCESS, FAILED, CLOSED',
            `transaction_id` varchar(64) DEFAULT NULL COMMENT '第三方交易流水号',
            `provider_order_no` varchar(64) DEFAULT NULL COMMENT '第三方系统单号（如 YunGouOS orderNo）',
            `context_id` bigint(20) NOT NULL COMMENT '业务上下文ID',
            `context_type` varchar(32) NOT NULL COMMENT '业务上下文类型',
            `subject` varchar(256) NOT NULL COMMENT '订单标题',
            `body` varchar(1024) DEFAULT NULL COMMENT '订单描述',
            `client_ip` varchar(64) DEFAULT NULL COMMENT '客户端IP',
            `extra_params` text COMMENT '附加参数（JSON格式）',
            `pay_data` text COMMENT '支付要素数据（JSON：二维码图片地址/支付链接等）',
            `notify_count` int(11) NOT NULL DEFAULT 0 COMMENT '回调接收次数',
            `last_notify_time` datetime(3) DEFAULT NULL COMMENT '最后一次回调接收时间',
            `notify_verified` tinyint(1) NOT NULL DEFAULT 0 COMMENT '回调验签是否通过：0否 1是',
            `event_sent` tinyint(1) NOT NULL DEFAULT 0 COMMENT '支付成功事件是否已投递：0否 1是',
            `event_sent_time` datetime(3) DEFAULT NULL COMMENT '支付成功事件投递时间',
            `event_send_fail_reason` varchar(256) DEFAULT NULL COMMENT '事件投递失败原因（用于排障）',
            `success_time` datetime(3) DEFAULT NULL COMMENT '支付成功时间',
            `expire_time` datetime(3) DEFAULT NULL COMMENT '订单过期时间',
            `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
            `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
            PRIMARY KEY (`id`),
            UNIQUE KEY `uk_order_no` (`order_no`),
            KEY `idx_user_id` (`user_id`),
            KEY `idx_context` (`context_id`, `context_type`),
            KEY `idx_create_time` (`create_time`),
            KEY `idx_status_create_time` (`status`, `create_time`),
            KEY `idx_event_sent` (`status`, `event_sent`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付订单表';

DROP TABLE IF EXISTS `payment_refund`;
CREATE TABLE `payment_refund` (
            `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '退款记录id',
            `refund_no` varchar(64) NOT NULL COMMENT '平台退款单号（唯一）',
            `payment_order_no` varchar(64) NOT NULL COMMENT '支付订单号',
            `provider` varchar(32) NOT NULL DEFAULT 'YUNGOUOS' COMMENT '支付提供方（YUNGOUOS）',
            `provider_refund_no` varchar(64) DEFAULT NULL COMMENT '第三方退款单号',
            `refund_amount_fen` bigint(20) UNSIGNED NOT NULL COMMENT '退款金额（分）',
            `status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '退款状态 PENDING/SUCCESS/FAILED',
            `request_id` bigint(20) UNSIGNED NOT NULL COMMENT '业务幂等键（refund_request.id）',
            `fail_reason` varchar(1024) DEFAULT NULL COMMENT '失败原因（排障用）',
            `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
            `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
            PRIMARY KEY (`id`),
            UNIQUE KEY `uniq_refund_no` (`refund_no`),
            UNIQUE KEY `uniq_request_id` (`request_id`),
            KEY `idx_payment_refund_payment_order_status` (`payment_order_no`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付退款表（原路退款审计与幂等）';

DROP TABLE IF EXISTS `tutor_application`;
CREATE TABLE `tutor_application` (
            `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '申请id',
            `sender_uid` bigint(20) NOT NULL COMMENT '发起方用户id',
            `receiver_uid` bigint(20) NOT NULL COMMENT '接收方用户id',
            `sender_role` varchar(16) NOT NULL COMMENT '发起方角色',
            `receiver_role` varchar(16) NOT NULL COMMENT '接收方角色',
            `context_type` varchar(16) NOT NULL COMMENT '上下文类型',
            `context_id` bigint(20) NOT NULL COMMENT '上下文id',
            `teaching_mode` varchar(20) DEFAULT NULL COMMENT '申请锁定的授课形式：ONLINE/OFFLINE',
            `content` varchar(500) NOT NULL COMMENT '申请内容',
            `client_request_id` varchar(64) DEFAULT NULL COMMENT '幂等键',
            `status` varchar(16) NOT NULL COMMENT '状态',
            `chat_access_status` varchar(32) NOT NULL COMMENT '聊天准入状态',
            `room_id` bigint(20) DEFAULT NULL COMMENT '关联roomId',
            `decided_at` datetime(3) DEFAULT NULL COMMENT '处理时间',
            `receiver_read` tinyint(1) NOT NULL DEFAULT 0 COMMENT '接收方是否已读',
            `receiver_read_time` datetime(3) DEFAULT NULL COMMENT '接收方已读时间',
            `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
            `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
            PRIMARY KEY (`id`),
            UNIQUE KEY `uniq_sender_client_req` (`sender_uid`, `client_request_id`),
            KEY `idx_sender` (`sender_uid`),
            KEY `idx_receiver` (`receiver_uid`),
            KEY `idx_context` (`context_type`, `context_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='找家教申请表';

DROP TABLE IF EXISTS `application_brokerage_order`;
CREATE TABLE `application_brokerage_order` (
            `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '关联id',
            `application_id` bigint(20) NOT NULL COMMENT '申请id',
            `order_id` bigint(20) NOT NULL COMMENT '中介费订单id',
            `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
            `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
            PRIMARY KEY (`id`),
            UNIQUE KEY `uniq_application` (`application_id`),
            KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='申请与中介费订单关联表';

DROP TABLE IF EXISTS `course_enrollment`;
CREATE TABLE `course_enrollment` (
            `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '课程视图id',
            `application_id` bigint(20) UNSIGNED NOT NULL COMMENT '申请单id（唯一）',
            `room_id` bigint(20) UNSIGNED DEFAULT NULL COMMENT '会话id',
            `proposal_id` bigint(20) UNSIGNED DEFAULT NULL COMMENT '合作提案id',
            `teacher_uid` bigint(20) UNSIGNED NOT NULL COMMENT '教师uid',
            `student_uid` bigint(20) UNSIGNED NOT NULL COMMENT '学生uid',
            `teaching_mode` varchar(20) DEFAULT NULL COMMENT '授课形式 ONLINE/OFFLINE',
            `course_name` varchar(255) DEFAULT NULL COMMENT '长期课程名称',
            `class_time` varchar(255) DEFAULT NULL COMMENT '每周固定上课时间描述',
            `frequency_per_week` int DEFAULT NULL COMMENT '每周课次数',
            `lesson_price` varchar(64) DEFAULT NULL COMMENT '单节课价格文案',
            `status` varchar(32) NOT NULL COMMENT '课程状态 APPLYING/WAIT_PAY/COMMUNICATING/REFUND_REVIEW/REFUNDED/TRIALING/TRIAL_REFUND_REVIEW/TEACHING/FINISHED',
            `trial_start_at` datetime(3) DEFAULT NULL COMMENT '试课开始时间',
            `trial_end_at` datetime(3) DEFAULT NULL COMMENT '试课结束时间（开始+7天）',
            `weekly_schedule_deadline_at` datetime(3) DEFAULT NULL COMMENT '试课通过后正式课表提交截止时间',
            `weekly_schedule_submitted_at` datetime(3) DEFAULT NULL COMMENT '正式课表提交时间',
            `weekly_reminder_12h_sent_at` datetime(3) DEFAULT NULL COMMENT '正式课表 12h 提醒发送时间',
            `weekly_reminder_6h_sent_at` datetime(3) DEFAULT NULL COMMENT '正式课表 6h 提醒发送时间',
            `weekly_reminder_1h_sent_at` datetime(3) DEFAULT NULL COMMENT '正式课表 1h 提醒发送时间',
            `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
            `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
            PRIMARY KEY (`id`),
            UNIQUE KEY `uniq_course_application` (`application_id`),
            KEY `idx_course_teacher_status` (`teacher_uid`, `status`),
            KEY `idx_course_student_status` (`student_uid`, `status`),
            KEY `idx_course_room` (`room_id`),
            KEY `idx_course_proposal` (`proposal_id`),
            KEY `idx_course_weekly_schedule_deadline` (`status`, `weekly_schedule_deadline_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='我的课程视图表（聚合申请/支付/合作/退款/试课状态）';

DROP TABLE IF EXISTS `refund_request`;
CREATE TABLE `refund_request` (
            `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '退款申请id',
            `brokerage_order_id` bigint(20) UNSIGNED NOT NULL COMMENT '信息费订单id',
            `course_id` bigint(20) UNSIGNED DEFAULT NULL COMMENT '课程视图id（试课退款使用）',
            `room_id` bigint(20) UNSIGNED DEFAULT NULL COMMENT '会话id（聊天退款使用）',
            `applicant_uid` bigint(20) UNSIGNED NOT NULL COMMENT '申请人uid',
            `applicant_role` varchar(32) NOT NULL COMMENT '申请人角色 TEACHER/STUDENT',
            `type` varchar(32) NOT NULL COMMENT '退款类型 CHAT_INFO_FEE/TRIAL_INFO_FEE',
            `status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '申请状态 PENDING/APPROVED/REJECTED',
            `reason` varchar(1024) DEFAULT NULL COMMENT '退款原因/说明',
            `evidence_images_json` text COMMENT '证据图片URL数组（JSON）',
            `evidence_video_url` varchar(1024) DEFAULT NULL COMMENT '试课不通过微信聊天录屏URL',
            `evidence_video_duration_seconds` int(11) DEFAULT NULL COMMENT '录屏时长（秒，最长60秒）',
            `evidence_video_delete_status` varchar(32) DEFAULT NULL COMMENT '录屏删除状态 PENDING_DELETE/DELETED/KEEP',
            `evidence_video_deleted_at` datetime(3) DEFAULT NULL COMMENT '录屏删除标记时间',
            `refund_percent` int(11) NOT NULL COMMENT '退款比例（100/80）',
            `refund_amount_fen` bigint(20) UNSIGNED NOT NULL COMMENT '申请退款金额（分）',
            `admin_uid` bigint(20) UNSIGNED DEFAULT NULL COMMENT '审核管理员uid',
            `admin_note` varchar(1024) DEFAULT NULL COMMENT '审核备注/拒绝原因',
            `decided_at` datetime(3) DEFAULT NULL COMMENT '审核时间',
            `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
            `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
            PRIMARY KEY (`id`),
            KEY `idx_refund_request_status_type_time` (`status`, `type`, `create_time`),
            KEY `idx_refund_request_brokerage_status` (`brokerage_order_id`, `status`),
            KEY `idx_refund_request_room` (`room_id`),
            KEY `idx_refund_request_course` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款申请表（聊天退款/试课退款）';

DROP TABLE IF EXISTS `user_email`;
CREATE TABLE `user_email` (
            `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '邮箱绑定记录id',
            `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '用户id',
            `email_type` varchar(32) NOT NULL COMMENT '邮箱类型 PRIMARY主邮箱 SUMMARY_ONLY课后总结专用邮箱',
            `email` varchar(255) NOT NULL COMMENT '邮箱地址',
            `email_masked` varchar(255) DEFAULT NULL COMMENT '脱敏邮箱',
            `verify_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '验证状态 PENDING待验证 VERIFIED已验证 INVALID已失效',
            `verified_at` datetime(3) DEFAULT NULL COMMENT '验证通过时间',
            `bind_source` varchar(32) DEFAULT NULL COMMENT '绑定来源 MY_PAGE/COURSE_PAGE/CHAT_PAGE/SUMMARY_PAGE',
            `bounce_status` varchar(32) NOT NULL DEFAULT 'NORMAL' COMMENT '退信状态 NORMAL正常 SOFT_BOUNCE软退信 HARD_BOUNCE硬退信',
            `last_notify_at` datetime(3) DEFAULT NULL COMMENT '最近一次业务通知发送时间',
            `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态 1有效 0删除',
            `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
            `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
            PRIMARY KEY (`id`),
            UNIQUE KEY `uniq_user_email_type_active` (`user_id`, `email_type`, `status`),
            KEY `idx_email` (`email`),
            KEY `idx_user_type_status` (`user_id`, `email_type`, `verify_status`),
            KEY `idx_bounce_status` (`bounce_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户邮箱绑定表';

DROP TABLE IF EXISTS `email_verify_code`;
CREATE TABLE `email_verify_code` (
            `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '邮箱验证码id',
            `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '用户id',
            `email` varchar(255) NOT NULL COMMENT '目标邮箱',
            `email_type` varchar(32) NOT NULL COMMENT '邮箱类型 PRIMARY/SUMMARY_ONLY',
            `code_hash` varchar(128) NOT NULL COMMENT '验证码hash',
            `scene` varchar(32) NOT NULL COMMENT '场景 BIND/CHANGE/REBIND',
            `expire_at` datetime(3) NOT NULL COMMENT '过期时间',
            `verified_at` datetime(3) DEFAULT NULL COMMENT '验证成功时间',
            `try_count` int(11) NOT NULL DEFAULT 0 COMMENT '验证尝试次数',
            `send_ip` varchar(64) DEFAULT NULL COMMENT '发送请求ip',
            `status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/VERIFIED/EXPIRED/CANCELED',
            `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
            `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
            PRIMARY KEY (`id`),
            KEY `idx_user_email_type_status` (`user_id`, `email`, `email_type`, `status`),
            KEY `idx_expire_at` (`expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮箱验证码表';

DROP TABLE IF EXISTS `email_notification_task`;
CREATE TABLE `email_notification_task` (
            `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '邮件通知任务id',
            `task_key` varchar(128) NOT NULL COMMENT '业务幂等key',
            `template_code` varchar(64) NOT NULL COMMENT '模板编码',
            `biz_type` varchar(64) NOT NULL COMMENT '业务类型 EMAIL_VERIFY/UNREAD_MESSAGE/LESSON_START/LESSON_SUMMARY',
            `biz_id` bigint(20) UNSIGNED DEFAULT NULL COMMENT '业务id，如msgId或lessonId',
            `receiver_uid` bigint(20) UNSIGNED DEFAULT NULL COMMENT '收件用户id',
            `receiver_role` varchar(32) DEFAULT NULL COMMENT '收件角色 TEACHER/STUDENT/PARENT_SUMMARY',
            `email_type` varchar(32) DEFAULT NULL COMMENT '邮箱类型 PRIMARY/SUMMARY_ONLY',
            `email` varchar(255) NOT NULL COMMENT '收件邮箱',
            `subject` varchar(255) DEFAULT NULL COMMENT '渲染后标题',
            `payload_json` json DEFAULT NULL COMMENT '模板变量与业务上下文',
            `scheduled_at` datetime(3) NOT NULL COMMENT '计划发送时间',
            `status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/VALIDATING/SENDING/SENT/FAILED/CANCELED/EXPIRED',
            `retry_count` int(11) NOT NULL DEFAULT 0 COMMENT '重试次数',
            `max_retry_count` int(11) NOT NULL DEFAULT 3 COMMENT '最大重试次数',
            `last_error` varchar(1024) DEFAULT NULL COMMENT '最近失败原因',
            `sent_at` datetime(3) DEFAULT NULL COMMENT '发送成功时间',
            `opened_at` datetime(3) DEFAULT NULL COMMENT '打开时间',
            `clicked_at` datetime(3) DEFAULT NULL COMMENT '点击时间',
            `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
            `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
            PRIMARY KEY (`id`),
            UNIQUE KEY `uniq_task_key` (`task_key`),
            KEY `idx_status_scheduled` (`status`, `scheduled_at`),
            KEY `idx_receiver_uid` (`receiver_uid`),
            KEY `idx_biz` (`biz_type`, `biz_id`),
            KEY `idx_template_status` (`template_code`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮件通知任务表';

DROP TABLE IF EXISTS `email_send_log`;
CREATE TABLE `email_send_log` (
            `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '发送日志id',
            `task_id` bigint(20) UNSIGNED NOT NULL COMMENT '任务id',
            `provider` varchar(64) DEFAULT NULL COMMENT '邮件服务商',
            `provider_message_id` varchar(128) DEFAULT NULL COMMENT '服务商消息id',
            `email` varchar(255) NOT NULL COMMENT '收件邮箱',
            `send_status` varchar(32) NOT NULL COMMENT '发送状态 SUCCESS/FAIL',
            `error_code` varchar(64) DEFAULT NULL COMMENT '错误码',
            `error_message` varchar(1024) DEFAULT NULL COMMENT '错误信息',
            `request_id` varchar(128) DEFAULT NULL COMMENT '请求id',
            `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
            PRIMARY KEY (`id`),
            KEY `idx_task_id` (`task_id`),
            KEY `idx_provider_message_id` (`provider_message_id`),
            KEY `idx_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮件发送日志表';

DROP TABLE IF EXISTS `lesson_summary`;
CREATE TABLE `lesson_summary` (
            `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '课后总结id',
            `lesson_id` bigint(20) UNSIGNED NOT NULL COMMENT '课节id，对应tutor_appointment.id',
            `course_id` bigint(20) UNSIGNED DEFAULT NULL COMMENT '长期课程id',
            `teacher_uid` bigint(20) UNSIGNED NOT NULL COMMENT '教师uid',
            `student_uid` bigint(20) UNSIGNED NOT NULL COMMENT '学生uid',
            `title` varchar(255) DEFAULT NULL COMMENT '总结标题',
            `summary_status` varchar(32) NOT NULL DEFAULT 'GENERATING' COMMENT '状态 GENERATING/READY/FAILED',
            `summary_brief` varchar(1000) DEFAULT NULL COMMENT '邮件摘要',
            `summary_content` mediumtext DEFAULT NULL COMMENT '完整总结内容',
            `homework` varchar(2000) DEFAULT NULL COMMENT '作业/建议',
            `ready_at` datetime(3) DEFAULT NULL COMMENT '生成完成时间',
            `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
            `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
            PRIMARY KEY (`id`),
            UNIQUE KEY `uniq_lesson_summary` (`lesson_id`),
            KEY `idx_course_id` (`course_id`),
            KEY `idx_student_ready` (`student_uid`, `summary_status`, `ready_at`),
            KEY `idx_teacher_ready` (`teacher_uid`, `summary_status`, `ready_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课后总结表';

DROP TABLE IF EXISTS `home_carousel_config`;
CREATE TABLE `home_carousel_config` (
            `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '首页轮播图配置id',
            `title` varchar(80) NOT NULL COMMENT '主标题',
            `subtitle` varchar(160) DEFAULT NULL COMMENT '副标题',
            `image_object_key` varchar(255) NOT NULL COMMENT 'MinIO 对象 key',
            `link_type` varchar(16) DEFAULT 'NONE' COMMENT '跳转类型 NONE/ROUTE/URL',
            `link_url` varchar(255) DEFAULT NULL COMMENT '跳转地址',
            `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
            `create_admin_id` bigint(20) UNSIGNED DEFAULT NULL COMMENT '创建管理员id',
            `update_admin_id` bigint(20) UNSIGNED DEFAULT NULL COMMENT '更新管理员id',
            `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
            `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
            PRIMARY KEY (`id`),
            KEY `idx_home_carousel_sort` (`sort_order`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页轮播图配置表';

DROP TABLE IF EXISTS `live_class_session`;
CREATE TABLE `live_class_session` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `course_id` bigint(20) NOT NULL,
  `schedule_event_id` bigint(20) DEFAULT NULL,
  `room_id` bigint(20) DEFAULT NULL,
  `provider` varchar(32) NOT NULL DEFAULT 'LIVEKIT',
  `provider_room_name` varchar(128) NOT NULL,
  `teacher_uid` bigint(20) NOT NULL,
  `student_uid` bigint(20) NOT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'CREATED',
  `join_open_at` datetime(3) DEFAULT NULL,
  `scheduled_start_at` datetime(3) NOT NULL,
  `scheduled_end_at` datetime(3) NOT NULL,
  `actual_start_at` datetime(3) DEFAULT NULL,
  `actual_end_at` datetime(3) DEFAULT NULL,
  `host_joined_at` datetime(3) DEFAULT NULL,
  `peer_joined_at` datetime(3) DEFAULT NULL,
  `ended_by_uid` bigint(20) DEFAULT NULL,
  `end_reason` varchar(64) DEFAULT NULL,
  `record_policy` varchar(32) NOT NULL DEFAULT 'OFF',
  `ai_policy` varchar(32) NOT NULL DEFAULT 'OFF',
  `extra_json` json DEFAULT NULL,
  `version` int(11) NOT NULL DEFAULT 0,
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_live_class_course_id` (`course_id`),
  UNIQUE KEY `uk_live_class_provider_room_name` (`provider_room_name`),
  KEY `idx_live_class_teacher_status_start` (`teacher_uid`,`status`,`scheduled_start_at`),
  KEY `idx_live_class_student_status_start` (`student_uid`,`status`,`scheduled_start_at`),
  KEY `idx_live_class_room_id` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实时课堂主表';

DROP TABLE IF EXISTS `live_class_participant`;
CREATE TABLE `live_class_participant` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `session_id` bigint(20) NOT NULL,
  `uid` bigint(20) NOT NULL,
  `role` varchar(32) NOT NULL,
  `identity_type` varchar(32) NOT NULL DEFAULT 'HUMAN',
  `join_count` int(11) NOT NULL DEFAULT 0,
  `first_join_at` datetime(3) DEFAULT NULL,
  `last_join_at` datetime(3) DEFAULT NULL,
  `last_leave_at` datetime(3) DEFAULT NULL,
  `online_status` varchar(32) NOT NULL DEFAULT 'NOT_JOINED',
  `camera_enabled` tinyint(1) NOT NULL DEFAULT 0,
  `mic_enabled` tinyint(1) NOT NULL DEFAULT 0,
  `device_info_json` json DEFAULT NULL,
  `network_score` int(11) DEFAULT NULL,
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_live_class_session_uid` (`session_id`,`uid`),
  KEY `idx_live_class_participant_uid_status` (`uid`,`online_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实时课堂参与者状态表';

DROP TABLE IF EXISTS `live_class_event`;
CREATE TABLE `live_class_event` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `session_id` bigint(20) NOT NULL,
  `event_type` varchar(64) NOT NULL,
  `event_source` varchar(32) NOT NULL,
  `operator_uid` bigint(20) DEFAULT NULL,
  `payload_json` json DEFAULT NULL,
  `occurred_at` datetime(3) NOT NULL,
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_live_class_event_session_occurred` (`session_id`,`occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实时课堂事件流水表';

DROP TABLE IF EXISTS `live_class_device_report`;
CREATE TABLE `live_class_device_report` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `session_id` bigint(20) NOT NULL,
  `uid` bigint(20) NOT NULL,
  `report_stage` varchar(32) NOT NULL,
  `camera_status` varchar(32) DEFAULT NULL,
  `mic_status` varchar(32) DEFAULT NULL,
  `speaker_status` varchar(32) DEFAULT NULL,
  `network_level` varchar(32) DEFAULT NULL,
  `browser_info` varchar(256) DEFAULT NULL,
  `os_info` varchar(256) DEFAULT NULL,
  `device_json` json DEFAULT NULL,
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_live_class_device_report_session_uid_stage` (`session_id`,`uid`,`report_stage`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实时课堂设备检测上报表';

DROP TABLE IF EXISTS `live_class_webhook_event`;
CREATE TABLE `live_class_webhook_event` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `provider` varchar(32) NOT NULL,
  `provider_event_id` varchar(128) NOT NULL,
  `session_id` bigint(20) DEFAULT NULL,
  `provider_room_name` varchar(128) DEFAULT NULL,
  `event_type` varchar(64) NOT NULL,
  `payload_json` json NOT NULL,
  `processed` tinyint(1) NOT NULL DEFAULT 0,
  `processed_at` datetime(3) DEFAULT NULL,
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_live_class_webhook_provider_event` (`provider`,`provider_event_id`),
  KEY `idx_live_class_webhook_room_name` (`provider_room_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实时课堂媒体 webhook 事件表';

DROP TABLE IF EXISTS `room_read_state`;
CREATE TABLE `room_read_state` (
            `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '已读状态id',
            `room_id` bigint(20) NOT NULL COMMENT '会话id',
            `uid` bigint(20) NOT NULL COMMENT '用户id',
            `last_read_msg_id` bigint(20) DEFAULT NULL COMMENT '最后已读消息id',
            `last_read_time` datetime(3) DEFAULT NULL COMMENT '最后已读时间',
            `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
            `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
            PRIMARY KEY (`id`),
            UNIQUE KEY `uniq_room_uid` (`room_id`, `uid`),
            KEY `idx_uid` (`uid`),
            KEY `idx_room_id` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话已读状态表';

DROP TABLE IF EXISTS `tutor_appointment`;
CREATE TABLE `tutor_appointment` (
            `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '预约id',
            `course_id` bigint(20) UNSIGNED DEFAULT NULL COMMENT '所属长期课程 id',
            `parent_id` bigint(20) NOT NULL COMMENT '家长 user_id',
            `tutor_id` bigint(20) NOT NULL COMMENT '教师 user_id',
            `parent_job_posting_id` bigint(20) DEFAULT NULL COMMENT '家长需求贴id',
            `tutor_job_posting_id` bigint(20) DEFAULT NULL COMMENT '教师服务贴id',
            `title` varchar(100) DEFAULT NULL COMMENT '课程名称/标题',
            `lesson_type` varchar(20) NOT NULL DEFAULT 'NORMAL' COMMENT '课节类型 TRIAL/NORMAL',
            `lesson_price_fen` bigint(20) UNSIGNED DEFAULT NULL COMMENT '单节标准课价（分）',
            `trial_price_percent` int(11) NOT NULL DEFAULT 50 COMMENT '试课收费比例，默认50表示半节课',
            `payable_amount_fen` bigint(20) UNSIGNED DEFAULT NULL COMMENT '当前课节应付金额（分）',
            `subject_id` bigint(20) NOT NULL COMMENT '科目id（position_post.id）',
            `class_mode` varchar(50) DEFAULT NULL COMMENT '授课方式：online/offline',
            `city` varchar(100) DEFAULT NULL COMMENT '城市（线下）',
            `address` varchar(255) DEFAULT NULL COMMENT '地址（线下）',
            `start_time` datetime(3) NOT NULL COMMENT '开始时间',
            `duration_minutes` int NOT NULL DEFAULT 60 COMMENT '时长（分钟）',
            `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态：1待确认 2已确认 3待改期确认 4已取消 5已完成 6已拒绝',
            `created_by` bigint(20) NOT NULL COMMENT '发起人 user_id',
            `room_id` bigint(20) DEFAULT NULL COMMENT '关联聊天会话id（用于快速跳转）',
            `proposed_start_time` datetime(3) DEFAULT NULL COMMENT '改期提议时间',
            `proposed_by` bigint(20) DEFAULT NULL COMMENT '改期发起人 user_id',
            `cancel_by` bigint(20) DEFAULT NULL COMMENT '取消人 user_id',
            `remark` varchar(255) DEFAULT NULL COMMENT '备注/取消原因',
            `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
            `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
            PRIMARY KEY (`id`),
            KEY `idx_course_id` (`course_id`),
            KEY `idx_parent_id` (`parent_id`),
            KEY `idx_tutor_id` (`tutor_id`),
            KEY `idx_status` (`status`),
            KEY `idx_start_time` (`start_time`),
            KEY `idx_room_id` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约/邀约表';

DROP TABLE IF EXISTS `lesson_payment_order`;
CREATE TABLE `lesson_payment_order` (
            `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '课节支付订单id',
            `lesson_id` bigint(20) UNSIGNED NOT NULL COMMENT '短期课节/预约id',
            `course_id` bigint(20) UNSIGNED DEFAULT NULL COMMENT '所属长期课程id',
            `student_uid` bigint(20) UNSIGNED NOT NULL COMMENT '学生/家长uid',
            `teacher_uid` bigint(20) UNSIGNED NOT NULL COMMENT '教师uid',
            `lesson_type` varchar(20) NOT NULL DEFAULT 'NORMAL' COMMENT '课节类型 TRIAL/NORMAL',
            `total_amount_fen` bigint(20) UNSIGNED NOT NULL COMMENT '学生支付金额（分）',
            `platform_fee_rate` int(11) NOT NULL DEFAULT 10 COMMENT '平台服务费率百分比',
            `platform_fee_amount_fen` bigint(20) UNSIGNED NOT NULL COMMENT '平台服务费金额（分）',
            `teacher_income_amount_fen` bigint(20) UNSIGNED NOT NULL COMMENT '教师预计到账金额（分）',
            `status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/PAYING/PAID/CANCELED',
            `payment_order_no` varchar(64) DEFAULT NULL COMMENT '支付域商户订单号',
            `paid_at` datetime(3) DEFAULT NULL COMMENT '支付完成时间',
            `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
            `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
            PRIMARY KEY (`id`),
            UNIQUE KEY `uniq_lesson_payment_lesson` (`lesson_id`),
            KEY `idx_lesson_payment_course_status` (`course_id`, `status`),
            KEY `idx_lesson_payment_student_status` (`student_uid`, `status`),
            KEY `idx_lesson_payment_teacher_status` (`teacher_uid`, `status`),
            KEY `idx_lesson_payment_order_no` (`payment_order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='线上课节支付订单表';

DROP TABLE IF EXISTS `teacher_settlement`;
CREATE TABLE `teacher_settlement` (
            `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '教师结算记录id',
            `lesson_payment_order_id` bigint(20) UNSIGNED NOT NULL COMMENT '课节支付订单id',
            `teacher_uid` bigint(20) UNSIGNED NOT NULL COMMENT '教师uid',
            `settlement_amount_fen` bigint(20) UNSIGNED NOT NULL COMMENT '教师应结算金额（分）',
            `platform_fee_amount_fen` bigint(20) UNSIGNED NOT NULL COMMENT '平台服务费金额（分）',
            `status` varchar(32) NOT NULL DEFAULT 'SETTLEABLE' COMMENT '状态 SETTLEABLE/PAID',
            `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
            `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
            PRIMARY KEY (`id`),
            UNIQUE KEY `uniq_teacher_settlement_lesson_payment` (`lesson_payment_order_id`),
            KEY `idx_teacher_settlement_teacher_status` (`teacher_uid`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师课节结算快照表';

DROP TABLE IF EXISTS `tutor_review`;
CREATE TABLE `tutor_review` (
            `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评价id',
            `appointment_id` bigint(20) UNSIGNED NOT NULL COMMENT '预约id（逻辑外键）',
            `parent_id` bigint(20) UNSIGNED NOT NULL COMMENT '评价方 user_id（学生/家长/机构）',
            `tutor_id` bigint(20) UNSIGNED NOT NULL COMMENT '被评价教师 user_id',
            `rating` tinyint(4) NOT NULL COMMENT '评分 1~5',
            `content` varchar(1000) DEFAULT NULL COMMENT '评价内容',
            `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态 1正常 0删除',
            `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
            `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
            PRIMARY KEY (`id`),
            UNIQUE KEY `uniq_appointment` (`appointment_id`),
            KEY `idx_tutor_id` (`tutor_id`),
            KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师评价表（预约完成后评价）';

DROP TABLE IF EXISTS `sys_admin_user`;
CREATE TABLE `sys_admin_user` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '加密密码',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态 1正常 0禁用',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台管理员表';

DROP TABLE IF EXISTS `invite_code`;
CREATE TABLE `invite_code` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '邀请码记录id',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '用户id',
  `invite_code` varchar(16) NOT NULL COMMENT '邀请码',
  `status` varchar(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态 ACTIVE/FROZEN/INVALID',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_user_id` (`user_id`),
  UNIQUE KEY `uniq_invite_code` (`invite_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户邀请码表';

DROP TABLE IF EXISTS `invite_system_config`;
CREATE TABLE `invite_system_config` (
  `id` bigint(20) UNSIGNED NOT NULL COMMENT '配置id，固定为1',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用系统邀请码',
  `system_invite_code` varchar(16) NOT NULL COMMENT '系统邀请码',
  `system_invite_link` varchar(512) DEFAULT NULL COMMENT '系统邀请链接',
  `tutor_info_fee_discount_rate` decimal(10,4) NOT NULL DEFAULT 0.5000 COMMENT '教师信息费折扣比例',
  `student_reward_rate` decimal(10,4) NOT NULL DEFAULT 0.1300 COMMENT '学生返现比例',
  `promo_title` varchar(128) DEFAULT NULL COMMENT '推广标题',
  `promo_desc` varchar(512) DEFAULT NULL COMMENT '推广说明',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_system_invite_code` (`system_invite_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统邀请码配置表';

INSERT INTO `invite_system_config`
(`id`, `enabled`, `system_invite_code`, `system_invite_link`, `tutor_info_fee_discount_rate`, `student_reward_rate`, `promo_title`, `promo_desc`, `create_time`, `update_time`)
VALUES
(1, 1, 'CHUANGZHI', 'http://localhost:5173/auth/student?inviteCode=CHUANGZHI', 0.5000, 0.1300, '创智推广专属福利', '使用创智推广码注册后，教师信息费享受推广期减半，学生可按教师实付信息费获得返现。', NOW(3), NOW(3));

DROP TABLE IF EXISTS `invite_relation`;
CREATE TABLE `invite_relation` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '邀请关系id',
  `inviter_uid` bigint(20) UNSIGNED NOT NULL COMMENT '邀请人uid',
  `invitee_uid` bigint(20) UNSIGNED NOT NULL COMMENT '被邀请人uid',
  `invite_code` varchar(16) NOT NULL COMMENT '填写的邀请码',
  `bind_source` varchar(32) NOT NULL DEFAULT 'REGISTER' COMMENT '绑定来源',
  `status` varchar(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态 ACTIVE/FROZEN/INVALID',
  `risk_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否命中风控',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `bind_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_invitee_uid` (`invitee_uid`),
  KEY `idx_inviter_uid` (`inviter_uid`),
  KEY `idx_invite_code` (`invite_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邀请关系表';

DROP TABLE IF EXISTS `invite_receiver_account`;
CREATE TABLE `invite_receiver_account` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '收款信息id',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '用户id',
  `receiver_name` varchar(64) DEFAULT NULL COMMENT '收款人姓名',
  `wechat_no` varchar(64) DEFAULT NULL COMMENT '微信号',
  `phone` varchar(32) DEFAULT NULL COMMENT '手机号',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `status` varchar(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态 ACTIVE/INVALID',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邀请返利收款信息表';

DROP TABLE IF EXISTS `invite_reward_record`;
CREATE TABLE `invite_reward_record` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '返利记录id',
  `inviter_uid` bigint(20) UNSIGNED NOT NULL COMMENT '邀请人uid',
  `invitee_uid` bigint(20) UNSIGNED NOT NULL COMMENT '被邀请人uid',
  `reward_scene` varchar(64) NOT NULL COMMENT '返利场景',
  `biz_order_type` varchar(64) NOT NULL COMMENT '业务订单类型',
  `biz_order_id` bigint(20) UNSIGNED NOT NULL COMMENT '业务订单id',
  `payment_order_id` bigint(20) UNSIGNED DEFAULT NULL COMMENT '支付订单id',
  `base_amount_fen` bigint(20) UNSIGNED NOT NULL DEFAULT 0 COMMENT '返利基数（分）',
  `reward_rate` decimal(10,4) NOT NULL DEFAULT 0.0000 COMMENT '返利比例',
  `reward_amount_fen` bigint(20) UNSIGNED NOT NULL DEFAULT 0 COMMENT '返利金额（分）',
  `status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/FROZEN/SETTLEABLE/SETTLEMENT_PENDING/PAID/FAILED/REVERSED',
  `freeze_reason` varchar(255) DEFAULT NULL COMMENT '冻结原因',
  `settlement_month` varchar(16) DEFAULT NULL COMMENT '结算月份 yyyy-MM',
  `config_snapshot_json` json DEFAULT NULL COMMENT '配置快照',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_scene_biz_order` (`reward_scene`, `biz_order_type`, `biz_order_id`),
  KEY `idx_inviter_uid_status` (`inviter_uid`, `status`),
  KEY `idx_invitee_uid` (`invitee_uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邀请返利记录表';

DROP TABLE IF EXISTS `invite_settlement_order`;
CREATE TABLE `invite_settlement_order` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '结算单id',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '用户id',
  `settlement_month` varchar(16) NOT NULL COMMENT '结算月份 yyyy-MM',
  `total_amount_fen` bigint(20) UNSIGNED NOT NULL DEFAULT 0 COMMENT '结算总金额（分）',
  `paid_amount_fen` bigint(20) UNSIGNED NOT NULL DEFAULT 0 COMMENT '实际打款金额（分）',
  `status` varchar(16) NOT NULL DEFAULT 'CREATED' COMMENT '状态 CREATED/PAYING/PAID/FAILED/CANCELED',
  `receiver_snapshot_json` json DEFAULT NULL COMMENT '收款信息快照',
  `fail_reason` varchar(255) DEFAULT NULL COMMENT '失败原因',
  `pay_time` datetime(3) DEFAULT NULL COMMENT '打款时间',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_user_month` (`user_id`, `settlement_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邀请返利结算单表';

DROP TABLE IF EXISTS `ai_task`;
CREATE TABLE `ai_task` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `task_id` varchar(64) NOT NULL COMMENT '任务唯一ID',
  `task_type` varchar(64) NOT NULL COMMENT '任务类型 LESSON_REPORT/CHAT_SUMMARY',
  `biz_id` varchar(64) NOT NULL COMMENT '业务主键，例如 lessonId/roomId',
  `status` varchar(32) NOT NULL COMMENT '任务状态',
  `progress` int(11) NOT NULL DEFAULT 0 COMMENT '任务进度 0-100',
  `message` varchar(255) DEFAULT NULL COMMENT '进度说明',
  `input_json` json DEFAULT NULL COMMENT '任务输入快照',
  `output_json` json DEFAULT NULL COMMENT '任务输出结果',
  `error_message` text DEFAULT NULL COMMENT '错误信息',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_task_task_id` (`task_id`),
  KEY `idx_ai_task_biz` (`biz_id`),
  KEY `idx_ai_task_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI Agent 任务表';

DROP TABLE IF EXISTS `ai_lesson_report`;
CREATE TABLE `ai_lesson_report` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `lesson_id` bigint(20) NOT NULL COMMENT '课程ID',
  `task_id` varchar(64) NOT NULL COMMENT '关联任务ID',
  `teacher_id` bigint(20) DEFAULT NULL COMMENT '教师ID',
  `student_id` bigint(20) DEFAULT NULL COMMENT '学生ID',
  `status` varchar(32) NOT NULL COMMENT '报告状态',
  `report_json` json DEFAULT NULL COMMENT '课后报告草稿/最终稿',
  `teacher_edited_json` json DEFAULT NULL COMMENT '教师编辑后的报告',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_lesson_report_lesson_id` (`lesson_id`),
  UNIQUE KEY `uk_ai_lesson_report_task_id` (`task_id`),
  KEY `idx_ai_lesson_report_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 课后报告表';

DROP TABLE IF EXISTS `ai_chat_summary`;
CREATE TABLE `ai_chat_summary` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `room_id` bigint(20) NOT NULL COMMENT '聊天室ID',
  `task_id` varchar(64) NOT NULL COMMENT '关联任务ID',
  `summary_json` json DEFAULT NULL COMMENT 'IM 摘要结果',
  `message_start_id` bigint(20) DEFAULT NULL COMMENT '摘要覆盖的起始消息ID',
  `message_end_id` bigint(20) DEFAULT NULL COMMENT '摘要覆盖的结束消息ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_chat_summary_room_id` (`room_id`),
  UNIQUE KEY `uk_ai_chat_summary_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 聊天摘要表';

DROP TABLE IF EXISTS `ai_live_lesson_session`;
CREATE TABLE `ai_live_lesson_session` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `lesson_id` bigint(20) NOT NULL COMMENT '课程ID',
  `session_id` varchar(64) NOT NULL COMMENT '课堂AI会话ID',
  `teacher_id` bigint(20) DEFAULT NULL COMMENT '教师ID',
  `student_id` bigint(20) DEFAULT NULL COMMENT '学生ID',
  `subject` varchar(64) DEFAULT NULL COMMENT '科目',
  `grade` varchar(64) DEFAULT NULL COMMENT '年级',
  `course_type` varchar(32) NOT NULL COMMENT '课型 ONLINE_FORMAL/ONLINE_TRIAL/TEXT_ONLY/LOW_COST',
  `mode` varchar(32) NOT NULL COMMENT '实时AI模式',
  `asr_enabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否启用ASR',
  `llm_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用LLM阶段摘要',
  `status` varchar(32) NOT NULL COMMENT '状态 ACTIVE/FINALIZED/FAILED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_live_lesson_lesson_id` (`lesson_id`),
  UNIQUE KEY `uk_ai_live_lesson_session_id` (`session_id`),
  KEY `idx_ai_live_lesson_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实时课堂AI会话表';

DROP TABLE IF EXISTS `ai_lesson_transcript`;
CREATE TABLE `ai_lesson_transcript` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `lesson_id` bigint(20) NOT NULL COMMENT '课程ID',
  `segment_index` int(11) NOT NULL COMMENT '分段序号',
  `speaker` varchar(32) NOT NULL COMMENT '说话人 teacher/student/unknown',
  `start_ms` bigint(20) NOT NULL DEFAULT 0 COMMENT '起始时间毫秒',
  `end_ms` bigint(20) NOT NULL DEFAULT 0 COMMENT '结束时间毫秒',
  `text` text NOT NULL COMMENT '转写文本',
  `is_final` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否最终文本',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ai_lesson_transcript_lesson` (`lesson_id`),
  KEY `idx_ai_lesson_transcript_segment` (`lesson_id`, `segment_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课堂实时转写表';

DROP TABLE IF EXISTS `ai_lesson_stage_summary`;
CREATE TABLE `ai_lesson_stage_summary` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `lesson_id` bigint(20) NOT NULL COMMENT '课程ID',
  `stage_index` int(11) NOT NULL COMMENT '阶段索引',
  `summary_json` json DEFAULT NULL COMMENT '阶段摘要结果',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ai_lesson_stage_summary_lesson` (`lesson_id`),
  KEY `idx_ai_lesson_stage_summary_stage` (`lesson_id`, `stage_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课堂阶段摘要表';

SET FOREIGN_KEY_CHECKS = 1;
