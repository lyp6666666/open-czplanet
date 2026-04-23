SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

USE ai_tutor;

INSERT INTO `sys_admin_user` (`username`, `password`, `nickname`, `status`)
VALUES ('admin', '$2a$10$Qe6N8DbgmYojC4dWNsCSX.7sAFBwq/.zF4WyTgl0HGOIDFZZA0SS2', '超级管理员', 1)
ON DUPLICATE KEY UPDATE
`password`=VALUES(`password`),
`nickname`=VALUES(`nickname`),
`status`=VALUES(`status`),
`update_time`=NOW(3);

INSERT INTO `user` (id, name, phone, avatar, sex, open_id, active_status, last_opt_time, ip_info, item_id, status, user_type, ref_id, create_time, update_time)
VALUES
(101, '家长-林女士', '15268836901', '/avatars/u101.png', 2, NULL, 2, NOW(3), NULL, NULL, 0, 2, 1001, NOW(3), NOW(3)),
(102, '家长-王先生', '15268836902', '/avatars/u102.png', 1, NULL, 2, NOW(3), NULL, NULL, 0, 2, 1002, NOW(3), NOW(3)),
(103, '家长-张女士', '15268836903', '/avatars/u103.png', 2, NULL, 2, NOW(3), NULL, NULL, 0, 2, 1003, NOW(3), NOW(3)),
(104, '学生-陈同学', '15268836904', '/avatars/u104.png', 1, NULL, 2, NOW(3), NULL, NULL, 0, 2, 1004, NOW(3), NOW(3)),
(105, '学生-刘同学', '15268836905', '/avatars/u105.png', 2, NULL, 2, NOW(3), NULL, NULL, 0, 2, 1005, NOW(3), NOW(3)),
(106, '家长-赵女士', '15268836906', '/avatars/u106.png', 2, NULL, 2, NOW(3), NULL, NULL, 0, 2, 1006, NOW(3), NOW(3)),
(107, '家长-周先生', '15268836907', '/avatars/u107.png', 1, NULL, 2, NOW(3), NULL, NULL, 0, 2, 1007, NOW(3), NOW(3)),
(108, '家长-吴女士', '15268836908', '/avatars/u108.png', 2, NULL, 2, NOW(3), NULL, NULL, 0, 2, 1008, NOW(3), NOW(3)),
(109, '学生-孙同学', '15268836909', '/avatars/u109.png', 1, NULL, 2, NOW(3), NULL, NULL, 0, 2, 1009, NOW(3), NOW(3)),
(110, '家长-郑女士', '15268836910', '/avatars/u110.png', 2, NULL, 2, NOW(3), NULL, NULL, 0, 2, 1010, NOW(3), NOW(3)),
(201, '教师-李老师', '13812345001', '/avatars/t201.png', 1, NULL, 2, NOW(3), NULL, NULL, 0, 1, 2001, NOW(3), NOW(3)),
(202, '教师-周老师', '13812345002', '/avatars/t202.png', 2, NULL, 2, NOW(3), NULL, NULL, 0, 1, 2002, NOW(3), NOW(3)),
(203, '教师-王老师', '13812345003', '/avatars/t203.png', 1, NULL, 2, NOW(3), NULL, NULL, 0, 1, 2003, NOW(3), NOW(3)),
(204, '教师-陈老师', '13812345004', '/avatars/t204.png', 2, NULL, 2, NOW(3), NULL, NULL, 0, 1, 2004, NOW(3), NOW(3)),
(205, '教师-张老师', '13812345005', '/avatars/t205.png', 1, NULL, 2, NOW(3), NULL, NULL, 0, 1, 2005, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
name=VALUES(name),
phone=VALUES(phone),
avatar=VALUES(avatar),
sex=VALUES(sex),
active_status=VALUES(active_status),
last_opt_time=VALUES(last_opt_time),
status=VALUES(status),
user_type=VALUES(user_type),
ref_id=VALUES(ref_id),
update_time=VALUES(update_time);

INSERT INTO `user` (id, name, phone, avatar, sex, open_id, active_status, last_opt_time, ip_info, item_id, status, user_type, ref_id, create_time, update_time)
VALUES
(301, '默默机构', '19900000001', '/avatars/org301.png', NULL, NULL, 2, NOW(3), NULL, NULL, 0, 3, 1301, NOW(3), NOW(3)),
(20019, '示例机构20019', '19900020019', '/avatars/org20019.png', NULL, NULL, 2, NOW(3), NULL, NULL, 0, 3, 62019, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
name=VALUES(name),
phone=VALUES(phone),
avatar=VALUES(avatar),
sex=VALUES(sex),
active_status=VALUES(active_status),
last_opt_time=VALUES(last_opt_time),
status=VALUES(status),
user_type=VALUES(user_type),
ref_id=VALUES(ref_id),
update_time=VALUES(update_time);

INSERT INTO `user` (id, name, phone, avatar, sex, open_id, active_status, last_opt_time, ip_info, item_id, status, user_type, ref_id, create_time, update_time)
VALUES
(1, '用户5678', '13812345678', NULL, NULL, NULL, 2, NULL, NULL, NULL, 0, 1, NULL, '2025-11-30 12:06:31.429', '2025-11-30 12:06:31.429'),
(3, '用户6909', '15268836999', NULL, NULL, NULL, 2, NULL, NULL, NULL, 0, 2, NULL, '2025-11-30 22:00:46.260', '2025-11-30 22:00:46.260')
ON DUPLICATE KEY UPDATE
name=VALUES(name),
phone=VALUES(phone),
avatar=VALUES(avatar),
sex=VALUES(sex),
active_status=VALUES(active_status),
last_opt_time=VALUES(last_opt_time),
status=VALUES(status),
user_type=VALUES(user_type),
ref_id=VALUES(ref_id),
update_time=VALUES(update_time);

INSERT INTO `student_profile` (id, user_id, real_name, age, address, demand_description, budget, status, create_time, update_time)
VALUES
(1001, 101, '林女士', 12, '北京·海淀·中关村', '孩子五年级，数学计算基础一般，想提升应用题思路。', 180.00, 1, NOW(3), NOW(3)),
(1002, 102, '王先生', 14, '上海·浦东·世纪大道', '初二英语阅读理解提分，偏薄弱语法与词汇。', 220.00, 1, NOW(3), NOW(3)),
(1003, 103, '张女士', 10, '杭州·西湖·文三路', '三年级语文阅读与写作，提升表达与作文结构。', 160.00, 1, NOW(3), NOW(3)),
(1004, 104, '陈同学', 17, '广州·天河·体育西', '高二物理电磁学吃力，想系统补基础并刷题。', 260.00, 1, NOW(3), NOW(3)),
(1005, 105, '刘同学', 16, '深圳·南山·科技园', '高一数学函数与导数基础薄弱，需要一对一讲解。', 280.00, 1, NOW(3), NOW(3)),
(1006, 106, '赵女士', 13, '北京·朝阳·国贸', '初一化学启蒙，想提前建立概念与兴趣。', 200.00, 1, NOW(3), NOW(3)),
(1007, 107, '周先生', 9, '上海·杨浦·五角场', '四年级英语自然拼读与口语陪练。', 150.00, 1, NOW(3), NOW(3)),
(1008, 108, '吴女士', 15, '杭州·滨江·江陵路', '初三数学冲刺，重难点专项突破。', 240.00, 1, NOW(3), NOW(3)),
(1009, 109, '孙同学', 12, '广州·越秀·北京路', '小学英语词汇+听力，想养成学习习惯。', 140.00, 1, NOW(3), NOW(3)),
(1010, 110, '郑女士', 11, '深圳·福田·会展中心', '小学奥数启蒙，培养思维与兴趣。', 200.00, 1, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
real_name=VALUES(real_name),
age=VALUES(age),
address=VALUES(address),
demand_description=VALUES(demand_description),
budget=VALUES(budget),
status=VALUES(status),
update_time=VALUES(update_time);

INSERT INTO `student_profile` (id, user_id, real_name, age, address, demand_description, budget, status, create_time, update_time)
VALUES
(1301, 301, '默默机构', NULL, '北京·海淀', '机构介绍：专注中小学一对一家教与学习规划。', NULL, 1, NOW(3), NOW(3)),
(62019, 20019, '示例机构20019', NULL, '北京·朝阳', '机构介绍：用于本地联调的机构账号（兼容聊天/申请）。', NULL, 1, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
real_name=VALUES(real_name),
age=VALUES(age),
address=VALUES(address),
demand_description=VALUES(demand_description),
budget=VALUES(budget),
status=VALUES(status),
update_time=VALUES(update_time);

INSERT INTO `organization_profile` (id, user_id, org_name, intro, contact_name, contact_phone, address, license_no, split_platform_percent, split_org_percent, status)
VALUES
(4001, 301, '默默机构', '专注中小学一对一辅导，提供学习规划与阶段测评。', '客服小默', '19900000001', '北京·海淀', '91110108MA0000000X', 50, 50, 1),
(42019, 20019, '示例机构20019', '用于本地联调的机构资料。', '联系人20019', '19900020019', '北京·朝阳', '91110108MA0020019X', 50, 50, 1)
ON DUPLICATE KEY UPDATE
org_name=VALUES(org_name),
intro=VALUES(intro),
contact_name=VALUES(contact_name),
contact_phone=VALUES(contact_phone),
address=VALUES(address),
license_no=VALUES(license_no),
split_platform_percent=VALUES(split_platform_percent),
split_org_percent=VALUES(split_org_percent),
status=VALUES(status),
update_time=NOW(3);

INSERT INTO `organization_account` (org_user_id, username, password_hash, must_change_password, status)
VALUES
(301, 'momo_org', '$2y$10$LbNkUflDfYhPkKJyVJJsv.Hddizb59IKgpP3dlYYtvKA8BPQp7HTC', 1, 1)
ON DUPLICATE KEY UPDATE
password_hash=VALUES(password_hash),
must_change_password=VALUES(must_change_password),
status=VALUES(status),
update_time=NOW(3);

INSERT INTO `teacher_profile` (id, user_id, real_name, education, subject, experience_years, rate_per_hour, introduction, city, highest_edu_school, teaching_mode, resume_completed, certificate_urls, status, create_time, update_time)
VALUES
(2001, 201, '李老师', 'C985', '数学', 6, 260.00, '擅长小学到初中数学提分，注重方法与错题复盘。', '北京', '北京大学', 'ONLINE', 1, NULL, 1, NOW(3), NOW(3)),
(2002, 202, '周老师', 'BACHELOR', '英语', 4, 220.00, '口语陪练+语法梳理，课堂节奏快但耐心。', '北京', '北京外国语大学', 'ONLINE', 1, NULL, 1, NOW(3), NOW(3)),
(2003, 203, '王老师', 'C211', '物理', 5, 280.00, '重点突破模型与题型，适合中高考备考。', '北京', '北京理工大学', 'OFFLINE', 1, NULL, 1, NOW(3), NOW(3)),
(2004, 204, '陈老师', 'OVERSEAS', '语文', 3, 240.00, '阅读理解与写作提分，善于引导表达。', '北京', '北京师范大学', 'BOTH', 1, NULL, 1, NOW(3), NOW(3)),
(2005, 205, '张老师', 'QS50', '奥数', 7, 320.00, '奥数启蒙到竞赛进阶，强调思维训练。', '北京', '清华大学', 'ONLINE', 1, NULL, 1, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
real_name=VALUES(real_name),
education=VALUES(education),
subject=VALUES(subject),
experience_years=VALUES(experience_years),
rate_per_hour=VALUES(rate_per_hour),
introduction=VALUES(introduction),
city=VALUES(city),
highest_edu_school=VALUES(highest_edu_school),
teaching_mode=VALUES(teaching_mode),
resume_completed=VALUES(resume_completed),
status=VALUES(status),
update_time=VALUES(update_time);

INSERT INTO `student_job_posting`
(id, parent_id, subject_id, subject_name, student_gender, title, description, child_age, class_mode, city, address, frequency_per_week, budget_min, budget_max, stage_code, education_requirement, publisher_identity, schedule, status, create_time, update_time)
VALUES
(3001, 101, 201, '初中数学', 'male', '初中数学一对一提分（函数/几何）', '孩子初二，基础还可以但做题不稳。希望老师能带着建立解题框架，每次课后有作业并讲评。', 14, 'offline', '北京', '海淀区中关村附近，地铁可达', 2, 180.00, 240.00, 'JUNIOR', 'BACHELOR', 'PARENT', '["Tue 19-21","Sat 10-12"]', 1, NOW(3), NOW(3)),
(3002, 102, 202, '初中英语', 'female', '初中英语阅读理解+语法巩固', '想系统梳理时态/从句，提升阅读速度与准确率。最好能每周固定两次，课后给词汇计划。', 14, 'online', '全国', NULL, 2, 160.00, 220.00, 'JUNIOR', 'C211', 'PARENT', '["Wed 20-21","Sun 10-11"]', 1, NOW(3), NOW(3)),
(3003, 103, 102, '小学语文', 'male', '小学语文阅读写作提升', '三年级，作文不知道怎么写。希望老师能带着做阅读积累和写作结构训练。', 10, 'offline', '杭州', '西湖区文三路附近', 3, 120.00, 180.00, 'PRIMARY', 'BACHELOR', 'PARENT', '["Mon 19-20","Thu 19-20","Sat 9-10"]', 1, NOW(3), NOW(3)),
(3004, 104, 303, '高中物理', 'female', '高二物理电磁学补基础+刷题', '我自己是学生，高二电磁学听不太懂，想从基础概念到典型题系统补。', 17, 'offline', '广州', '天河区体育西路附近', 2, 220.00, 320.00, 'SENIOR', 'C985', 'STUDENT_SELF', '["Tue 20-22","Sat 14-16"]', 1, NOW(3), NOW(3)),
(3005, 105, 301, '高中数学', 'male', '高一数学函数与导数（重点突破）', '高一，函数与导数概念不清，题目一变就不会。希望老师讲清思路并配套练习。', 16, 'both', '深圳', '南山区科技园附近，可线上/线下', 3, 240.00, 360.00, 'SENIOR', 'DOUBLE_FIRST_CLASS', 'STUDENT_SELF', '["Mon 20-21","Wed 20-21","Sun 10-12"]', 1, NOW(3), NOW(3)),
(3006, 106, 204, '初中化学', 'female', '初中化学启蒙（提前预习）', '孩子初一，想提前预习化学建立兴趣，课堂以实验现象和概念理解为主。', 13, 'online', '全国', NULL, 1, 160.00, 220.00, 'JUNIOR', 'BACHELOR', 'PARENT', '["Sat 10-12"]', 1, NOW(3), NOW(3)),
(3007, 107, 103, '小学英语', 'male', '小学英语自然拼读+口语陪练', '四年级，单词记不住。希望老师有体系地带着练自然拼读，兼顾口语表达。', 9, 'offline', '上海', '杨浦区五角场附近', 2, 120.00, 180.00, 'PRIMARY', 'UNLIMITED', 'PARENT', '["Wed 19-20","Sun 19-20"]', 1, NOW(3), NOW(3)),
(3008, 108, 201, '初中数学', 'female', '初三数学冲刺（压轴题专项）', '初三，想重点突破几何与函数压轴题。希望老师能按题型拆解训练。', 15, 'offline', '杭州', '滨江区江陵路附近', 5, 220.00, 320.00, 'JUNIOR', 'C985', 'PARENT', '["Mon 19-21","Tue 19-21","Thu 19-21","Sat 9-12","Sun 9-12"]', 1, NOW(3), NOW(3)),
(3009, 109, 103, '小学英语', 'male', '小学英语词汇+听力习惯养成', '五年级，听力跟不上，词汇量也少。希望老师带着制定学习计划，逐步提升。', 12, 'online', '全国', NULL, 2, 100.00, 160.00, 'PRIMARY', 'BACHELOR', 'STUDENT_SELF', '["Tue 20-21","Thu 20-21"]', 1, NOW(3), NOW(3)),
(3010, 110, 101, '小学数学', 'female', '小学数学奥数启蒙（思维训练）', '四年级，想做奥数启蒙。希望老师有趣、能引导孩子思考，不刷题为主。', 11, 'offline', '深圳', '福田区会展中心附近', 2, 180.00, 260.00, 'PRIMARY', 'OVERSEAS', 'PARENT', '["Sat 10-12","Sun 10-12"]', 1, NOW(3), NOW(3)),

(3011, 101, 201, '初中数学', 'male', '成都初二数学补弱（函数基础）', '孩子初二，函数题型不熟练，想从基础到专项训练。', 14, 'offline', '成都', '锦江区春熙路附近', 2, 160.00, 240.00, 'JUNIOR', 'BACHELOR', 'PARENT', '["Tue 19-21","Sat 10-12"]', 1, NOW(3), NOW(3)),
(3012, 102, 202, '初中英语', 'female', '西安初中英语口语+听力', '想提升口语表达与听力理解，课堂多互动。', 13, 'both', '西安', '雁塔区小寨附近', 2, 140.00, 220.00, 'JUNIOR', 'BACHELOR', 'PARENT', '["Wed 20-21","Sun 10-11"]', 1, NOW(3), NOW(3)),
(3013, 103, 102, '小学语文', 'male', '南京小学语文阅读积累', '提升阅读理解，培养写作素材积累习惯。', 10, 'offline', '南京', '鼓楼区新街口附近', 2, 120.00, 180.00, 'PRIMARY', 'BACHELOR', 'PARENT', '["Mon 19-20","Thu 19-20"]', 1, NOW(3), NOW(3)),
(3014, 104, 303, '高中物理', 'female', '武汉高二物理力学系统复习', '从受力分析到能量守恒，配套习题讲解。', 17, 'offline', '武汉', '洪山区光谷附近', 2, 220.00, 320.00, 'SENIOR', 'C211', 'STUDENT_SELF', '["Tue 20-22","Sat 14-16"]', 1, NOW(3), NOW(3)),
(3015, 105, 301, '高中数学', 'male', '苏州高一数学补基础（必修）', '希望梳理知识点，提升做题稳定性。', 16, 'offline', '苏州', '工业园区湖东附近', 2, 220.00, 320.00, 'SENIOR', 'BACHELOR', 'STUDENT_SELF', '["Mon 20-21","Wed 20-21"]', 1, NOW(3), NOW(3)),
(3016, 106, 204, '初中化学', 'female', '郑州初中化学预习（线上）', '提前预习初中化学，建立概念与兴趣。', 13, 'online', '全国', NULL, 1, 140.00, 220.00, 'JUNIOR', 'BACHELOR', 'PARENT', '["Sat 10-12"]', 1, NOW(3), NOW(3)),
(3017, 107, 103, '小学英语', 'male', '青岛小学英语自然拼读', '提升自然拼读与朗读能力，建立语感。', 9, 'offline', '青岛', '市南区五四广场附近', 2, 120.00, 180.00, 'PRIMARY', 'UNLIMITED', 'PARENT', '["Wed 19-20","Sun 19-20"]', 1, NOW(3), NOW(3)),
(3018, 108, 201, '初中数学', 'female', '济南初三数学冲刺（函数压轴）', '冲刺阶段希望专项突破函数压轴题。', 15, 'both', '济南', '历下区泉城广场附近', 3, 220.00, 320.00, 'JUNIOR', 'C985', 'PARENT', '["Thu 19-21","Sat 9-12","Sun 9-12"]', 1, NOW(3), NOW(3)),
(3019, 109, 103, '小学英语', 'male', '福州小学英语词汇听力提升（线上）', '制定学习计划，坚持打卡与复盘。', 12, 'online', '全国', NULL, 2, 100.00, 160.00, 'PRIMARY', 'BACHELOR', 'STUDENT_SELF', '["Tue 20-21","Thu 20-21"]', 1, NOW(3), NOW(3)),
(3020, 110, 101, '小学数学', 'female', '广州小学奥数启蒙', '培养思维与兴趣，注重过程与方法。', 11, 'offline', '广州', '天河区体育西附近', 2, 160.00, 240.00, 'PRIMARY', 'OVERSEAS', 'PARENT', '["Sat 10-12","Sun 10-12"]', 1, NOW(3), NOW(3)),
(3021, 20019, 201, '初中数学', 'male', '机构单｜初二数学提分（函数/几何）', '机构需求：希望老师具备带班提分经验，能制定阶段计划。', 14, 'offline', '北京', '朝阳区望京附近', 2, 180.00, 260.00, 'JUNIOR', 'BACHELOR', 'ORGANIZATION', '["Tue 19-21","Sat 10-12"]', 1, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
subject_name=VALUES(subject_name),
student_gender=VALUES(student_gender),
title=VALUES(title),
description=VALUES(description),
child_age=VALUES(child_age),
class_mode=VALUES(class_mode),
city=VALUES(city),
address=VALUES(address),
frequency_per_week=VALUES(frequency_per_week),
budget_min=VALUES(budget_min),
budget_max=VALUES(budget_max),
stage_code=VALUES(stage_code),
education_requirement=VALUES(education_requirement),
publisher_identity=VALUES(publisher_identity),
schedule=VALUES(schedule),
status=VALUES(status),
update_time=VALUES(update_time);

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
(id, user_id, real_name, education, subject, experience_years, rate_per_hour, introduction, city, highest_edu_school, teaching_mode, resume_completed, certificate_urls, status, create_time, update_time)
VALUES
(50001, 10001, '张晨', '北大 本科', '初中数学,高中数学', 4, 180.00, '擅长提分与错题体系化，注重解题思路与复盘。', '北京', '北京大学', 'ONLINE', 1, '["https://example.com/cert/teacher/50001-1.jpg"]', 1, DATE_SUB(NOW(), INTERVAL 30 DAY), NOW()),
(50002, 10002, '李薇', '师范 硕士', '初中英语,高中英语', 6, 220.00, '阅读写作双线提升，强调词汇与语法在语境中掌握。', '北京', '北京师范大学', 'ONLINE', 1, '["https://example.com/cert/teacher/50002-1.jpg"]', 1, DATE_SUB(NOW(), INTERVAL 18 DAY), NOW()),
(50003, 10003, '王磊', '985 本科', '初中物理,高中物理', 5, 240.00, '物理模型化讲解，善于把复杂题拆解成步骤。', '北京', '北京大学', 'OFFLINE', 1, '["https://example.com/cert/teacher/50003-1.jpg"]', 1, DATE_SUB(NOW(), INTERVAL 12 DAY), NOW()),
(50004, 10004, '赵婷', '师范 本科', '初中化学,高中化学', 3, 200.00, '化学方程式与题型训练结合，重视基础与规范书写。', '北京', '华东师范大学', 'BOTH', 1, '["https://example.com/cert/teacher/50004-1.jpg"]', 1, DATE_SUB(NOW(), INTERVAL 25 DAY), NOW()),
(50005, 10005, '陈浩', '北邮 本科', '编程(Python)', 2, 160.00, '从零到一做小项目，培养算法思维与代码习惯。', '北京', '北京邮电大学', 'ONLINE', 1, '["https://example.com/cert/teacher/50005-1.jpg"]', 1, DATE_SUB(NOW(), INTERVAL 8 DAY), NOW()),
(50006, 10006, '刘敏', '央音 本科', '钢琴', 7, 260.00, '钢琴启蒙与考级，强调节奏与手型基本功。', '北京', '中央音乐学院', 'OFFLINE', 1, '["https://example.com/cert/teacher/50006-1.jpg"]', 1, DATE_SUB(NOW(), INTERVAL 16 DAY), NOW()),
(50007, 10007, '周航', '师范 本科', '小学数学,初中数学', 4, 150.00, '耐心细致，善于帮助孩子建立自信与学习习惯。', '北京', '北京师范大学', 'BOTH', 1, '["https://example.com/cert/teacher/50007-1.jpg"]', 1, DATE_SUB(NOW(), INTERVAL 20 DAY), NOW()),
(50008, 10008, '吴楠', '外语 硕士', '小学英语,初中英语', 5, 190.00, '自然拼读+分级阅读，口语表达循序渐进。', '北京', '北京外国语大学', 'ONLINE', 1, '["https://example.com/cert/teacher/50008-1.jpg"]', 1, DATE_SUB(NOW(), INTERVAL 6 DAY), NOW()),
(50009, 10009, '郑凯', '美院 本科', '美术', 3, 180.00, '素描造型基础训练，兼顾创意与观察力。', '北京', '中央美术学院', 'OFFLINE', 1, '["https://example.com/cert/teacher/50009-1.jpg"]', 1, DATE_SUB(NOW(), INTERVAL 10 DAY), NOW()),
(50010, 10010, '孙颖', '师范 本科', '小学语文,初中语文', 6, 210.00, '阅读理解与作文专项，积累素材与表达结构。', '北京', '北京师范大学', 'BOTH', 1, '["https://example.com/cert/teacher/50010-1.jpg"]', 1, DATE_SUB(NOW(), INTERVAL 28 DAY), NOW()),
(50011, 10011, '马超', '理工 本科', '高中数学', 5, 260.00, '高考数学提分，擅长压轴题思路梳理。', '北京', '北京大学', 'ONLINE', 1, '["https://example.com/cert/teacher/50011-1.jpg"]', 1, DATE_SUB(NOW(), INTERVAL 14 DAY), NOW()),
(50012, 10012, '胡静', '书法协会', '书法', 8, 200.00, '硬笔规范与软笔入门，结构与章法训练。', '北京', '中国书法家协会', 'OFFLINE', 1, '["https://example.com/cert/teacher/50012-1.jpg"]', 1, DATE_SUB(NOW(), INTERVAL 9 DAY), NOW())
ON DUPLICATE KEY UPDATE
real_name=VALUES(real_name),
education=VALUES(education),
subject=VALUES(subject),
experience_years=VALUES(experience_years),
rate_per_hour=VALUES(rate_per_hour),
introduction=VALUES(introduction),
city=VALUES(city),
highest_edu_school=VALUES(highest_edu_school),
teaching_mode=VALUES(teaching_mode),
resume_completed=VALUES(resume_completed),
certificate_urls=VALUES(certificate_urls),
status=VALUES(status),
update_time=VALUES(update_time);

INSERT INTO student_profile
(id, user_id, real_name, age, address, demand_description, budget, status, create_time, update_time)
VALUES
(60001, 20001, '王女士', 10, '北京市海淀区', '孩子基础一般，希望提升应用题与计算准确率。', 150.00, 1, DATE_SUB(NOW(3), INTERVAL 40 DAY), NOW(3)),
(60002, 20002, '李先生', 13, '北京市朝阳区', '想系统补英语词汇语法，目标期末提升。', 180.00, 1, DATE_SUB(NOW(3), INTERVAL 33 DAY), NOW(3)),
(60003, 20003, '赵女士', 15, '北京市西城区', '物理电学薄弱，需要针对性练题。', 220.00, 1, DATE_SUB(NOW(3), INTERVAL 22 DAY), NOW(3)),
(60004, 20004, '周先生', 8, '北京市丰台区', '语文阅读理解差，想提高总结与表达。', 140.00, 1, DATE_SUB(NOW(3), INTERVAL 19 DAY), NOW(3)),
(60005, 20005, '陈女士', 12, '北京市昌平区', '数学几何不牢，想建立知识框架。', 180.00, 1, DATE_SUB(NOW(3), INTERVAL 12 DAY), NOW(3)),
(60006, 20006, '刘先生', 9, '北京市通州区', '孩子学钢琴准备考级，需要每周固定练习指导。', 260.00, 1, DATE_SUB(NOW(3), INTERVAL 10 DAY), NOW(3)),
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

INSERT INTO teacher_job_posting
(id, tutor_id, subject_id, title, description, price_per_hour, mode, city, available_time, max_students, status, create_time, update_time)
VALUES
(70001, 10001, 201, '初中数学一对一提分（函数/几何）', '按章节+题型训练，配套错题本与周测。', 180.00, 'online', '北京', '["Tue 19-21","Sat 10-12"]', 1, 1, DATE_SUB(NOW(), INTERVAL 12 DAY), NOW()),
(70002, 10001, 301, '高中数学系统复盘（导数/圆锥曲线）', '梳理知识网络，专项突破压轴题。', 260.00, 'online', '北京', '["Wed 19-21","Sun 14-16"]', 1, 1, DATE_SUB(NOW(), INTERVAL 9 DAY), NOW()),
(70003, 10002, 202, '初中英语阅读写作提升', '词汇语法在语境中掌握，写作模板+真题演练。', 220.00, 'online', '北京', '["Mon 19-21","Thu 19-21"]', 1, 1, DATE_SUB(NOW(), INTERVAL 10 DAY), NOW()),
(70004, 10002, 302, '高中英语写作专项（提分）', '审题-结构-表达三步法，改作文到位。', 240.00, 'online', '北京', '["Sat 19-21"]', 1, 1, DATE_SUB(NOW(), INTERVAL 7 DAY), NOW()),
(70005, 10003, 203, '初中物理电学专项', '电路分析与题型归纳，做题更稳。', 240.00, 'online', '北京', '["Tue 20-22","Sun 10-12"]', 1, 1, DATE_SUB(NOW(), INTERVAL 11 DAY), NOW()),
(70006, 10003, 303, '高中物理力学综合', '受力分析+能量守恒体系化训练。', 260.00, 'online', '北京', '["Wed 20-22"]', 1, 1, DATE_SUB(NOW(), INTERVAL 8 DAY), NOW()),
(70007, 10004, 204, '初中化学基础夯实', '酸碱盐与化学方程式规范训练。', 200.00, 'online', '北京', '["Mon 20-22"]', 1, 1, DATE_SUB(NOW(), INTERVAL 13 DAY), NOW()),
(70008, 10004, 304, '高中化学有机入门', '结构-性质-反应路线，配套练习。', 230.00, 'online', '北京', '["Fri 19-21"]', 1, 1, DATE_SUB(NOW(), INTERVAL 6 DAY), NOW()),
(70009, 10005, 403, 'Python入门+小项目（零基础）', '从语法到项目：爬虫/数据处理/小游戏。', 160.00, 'online', '北京', '["Sat 14-16","Sun 14-16"]', 1, 1, DATE_SUB(NOW(), INTERVAL 6 DAY), NOW()),
(70010, 10005, 403, 'Python算法与刷题入门', '循环/递归/搜索/排序，培养代码思维。', 180.00, 'online', '北京', '["Wed 19-21"]', 1, 1, DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()),
(70011, 10006, 401, '钢琴启蒙（节奏/手型/识谱）', '以兴趣为导向，打好基本功。', 260.00, 'offline', '北京', '["Sat 10-12","Sun 10-12"]', 1, 1, DATE_SUB(NOW(), INTERVAL 16 DAY), NOW()),
(70012, 10006, 401, '钢琴考级辅导（1-6级）', '曲目+视奏+乐理，制定练琴计划。', 320.00, 'offline', '北京', '["Wed 18-20"]', 1, 1, DATE_SUB(NOW(), INTERVAL 4 DAY), NOW()),
(70013, 10007, 101, '小学数学计算与应用题', '夯实基础，提升正确率与解题步骤。', 150.00, 'both', '北京', '["Tue 18-20","Thu 18-20"]', 1, 1, DATE_SUB(NOW(), INTERVAL 20 DAY), NOW()),
(70014, 10007, 201, '初中数学培优（中等偏上）', '专题训练+错题归纳，冲刺更高分。', 180.00, 'online', '北京', '["Sat 09-11"]', 1, 1, DATE_SUB(NOW(), INTERVAL 5 DAY), NOW()),
(70015, 10008, 103, '小学英语自然拼读+口语', '从拼读规则到阅读表达，轻松开口。', 160.00, 'online', '北京', '["Mon 18-20"]', 1, 1, DATE_SUB(NOW(), INTERVAL 6 DAY), NOW()),
(70016, 10008, 202, '初中英语词汇语法专项', '高频词+核心语法，搭配真题练习。', 190.00, 'online', '北京', '["Thu 20-22"]', 1, 1, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
(70017, 10009, 405, '美术素描基础（静物/结构）', '从线条到结构，提升观察与表达。', 180.00, 'offline', '北京', '["Sat 14-16"]', 1, 1, DATE_SUB(NOW(), INTERVAL 10 DAY), NOW()),
(70018, 10010, 102, '小学语文阅读与作文', '素材积累+结构表达，写作不再难。', 180.00, 'online', '北京', '["Wed 18-20"]', 1, 1, DATE_SUB(NOW(), INTERVAL 28 DAY), NOW()),
(70019, 10010, 205, '初中语文阅读理解提升', '方法论+题型训练，提升答题规范。', 210.00, 'online', '北京', '["Sun 19-21"]', 1, 1, DATE_SUB(NOW(), INTERVAL 8 DAY), NOW()),
(70020, 10011, 301, '高中数学冲刺（压轴题思路）', '分类突破，训练思维链路与书写规范。', 280.00, 'online', '北京', '["Sat 19-21","Sun 19-21"]', 1, 1, DATE_SUB(NOW(), INTERVAL 14 DAY), NOW()),
(70021, 10012, 404, '硬笔书法规范训练', '字形结构与章法，纠正握笔姿势。', 200.00, 'both', '北京', '["Tue 19-20","Fri 19-20"]', 1, 1, DATE_SUB(NOW(), INTERVAL 9 DAY), NOW()),
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

INSERT INTO student_job_posting
(id, parent_id, subject_id, subject_name, student_gender, title, description, child_age, class_mode, city, address, budget_min, budget_max, schedule, status, create_time, update_time)
VALUES
(80001, 20001, 101, '小学数学', 'male', '小学三年级数学家教（补基础）', '主要是计算和应用题，想把错误率降下来。', 10, 'online', '北京', '北京市海淀区', 120.00, 160.00, '["Tue 19-21","Sat 10-12"]', 1, DATE_SUB(NOW(), INTERVAL 9 DAY), NOW()),
(80002, 20002, 202, '初中英语', 'female', '初二英语词汇语法提升', '阅读理解做题慢，词汇量不够，希望系统补齐。', 13, 'online', '北京', '北京市朝阳区', 160.00, 220.00, '["Mon 19-21","Thu 19-21"]', 1, DATE_SUB(NOW(), INTERVAL 8 DAY), NOW()),
(80003, 20003, 203, '初中物理', 'male', '初三物理电学专项冲刺', '电路题总丢分，希望针对题型训练。', 15, 'online', '北京', '北京市西城区', 180.00, 260.00, '["Wed 19-21","Sun 10-12"]', 1, DATE_SUB(NOW(), INTERVAL 7 DAY), NOW()),
(80004, 20004, 102, '小学语文', 'female', '小学语文阅读理解与作文', '阅读理解抓不住重点，作文结构混乱。', 8, 'online', '北京', '北京市丰台区', 120.00, 180.00, '["Sat 14-16"]', 1, DATE_SUB(NOW(), INTERVAL 6 DAY), NOW()),
(80005, 20005, 201, '初中数学', 'male', '初一数学几何补弱', '几何证明题无从下手，需要方法。', 12, 'online', '北京', '北京市昌平区', 160.00, 220.00, '["Tue 20-22"]', 1, DATE_SUB(NOW(), INTERVAL 5 DAY), NOW()),
(80006, 20006, 401, '钢琴', 'female', '钢琴考级辅导（每周固定）', '准备考级，希望老师能制定练琴计划。', 9, 'offline', '北京', '北京市通州区', 240.00, 360.00, '["Sat 10-12"]', 1, DATE_SUB(NOW(), INTERVAL 5 DAY), NOW()),
(80007, 20007, 204, '初中化学', 'male', '初中化学基础夯实', '方程式和计算题薄弱，需要系统练习。', 14, 'online', '北京', '北京市海淀区', 160.00, 240.00, '["Sun 14-16"]', 1, DATE_SUB(NOW(), INTERVAL 4 DAY), NOW()),
(80008, 20008, 301, '高中数学', 'female', '高二数学专题突破（导数/数列）', '冲刺高分，希望做专题体系化训练。', 16, 'online', '北京', '北京市朝阳区', 220.00, 320.00, '["Sat 19-21","Sun 19-21"]', 1, DATE_SUB(NOW(), INTERVAL 4 DAY), NOW()),
(80009, 20009, 403, '编程(Python)', 'male', 'Python入门（兴趣+项目）', '希望做小项目培养兴趣，最好能有作业反馈。', 11, 'online', '北京', '北京市石景山区', 140.00, 220.00, '["Wed 19-21","Sat 14-16"]', 1, DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()),
(80010, 20010, 404, '书法', 'female', '硬笔书法纠正（字形结构）', '握笔姿势不对，写字不工整，需要纠正。', 10, 'both', '北京', '北京市东城区', 160.00, 240.00, '["Fri 19-20","Sun 10-11"]', 1, DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()),
(80011, 20001, 103, '小学英语', 'male', '小学英语口语提升', '不敢开口，希望多对话练习。', 10, 'online', '北京', '北京市海淀区', 120.00, 180.00, '["Mon 18-19","Thu 18-19"]', 1, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
(80012, 20002, 205, '初中语文', 'female', '初中语文阅读理解提高', '答题不规范，希望掌握方法。', 13, 'online', '北京', '北京市朝阳区', 150.00, 220.00, '["Sun 14-16"]', 1, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
(80013, 20003, 304, '高中化学', 'male', '高中化学有机专项', '有机反应路线记不住，需要梳理。', 15, 'online', '北京', '北京市西城区', 200.00, 280.00, '["Sat 10-12"]', 1, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
(80014, 20008, 302, '高中英语', 'female', '高中英语写作提分', '写作得分低，希望系统提升。', 16, 'online', '北京', '北京市朝阳区', 200.00, 300.00, '["Thu 19-21"]', 1, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(80015, 20009, 405, '美术', 'male', '美术素描基础训练', '想系统学素描，提升造型能力。', 11, 'offline', '北京', '北京市石景山区', 160.00, 240.00, '["Sat 14-16"]', 1, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW())
ON DUPLICATE KEY UPDATE
parent_id=VALUES(parent_id),
subject_id=VALUES(subject_id),
subject_name=VALUES(subject_name),
student_gender=VALUES(student_gender),
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

INSERT INTO `student_job_posting`
(id, parent_id, subject_id, subject_name, student_gender, title, description, child_age, class_mode, city, address, frequency_per_week, budget_min, budget_max, stage_code, education_requirement, publisher_identity, schedule, status, create_time, update_time)
VALUES
(3101, 301, 201, '初中数学', 'male', '机构单｜初二数学系统提分（函数/几何）', '机构发布：匹配擅长函数与几何的老师，需可做阶段测评与学习规划。', 14, 'offline', '北京', '海淀区中关村附近', 2, 200.00, 280.00, 'JUNIOR', 'BACHELOR', 'ORGANIZATION', '["Tue 19-21","Sat 10-12"]', 1, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
title=VALUES(title),
description=VALUES(description),
class_mode=VALUES(class_mode),
city=VALUES(city),
address=VALUES(address),
frequency_per_week=VALUES(frequency_per_week),
budget_min=VALUES(budget_min),
budget_max=VALUES(budget_max),
publisher_identity=VALUES(publisher_identity),
status=VALUES(status),
update_time=VALUES(update_time);

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

INSERT INTO tutor_appointment
(id, parent_id, tutor_id, parent_job_posting_id, tutor_job_posting_id, subject_id, class_mode, city, address, start_time, duration_minutes, status, created_by, proposed_start_time, proposed_by, cancel_by, remark, create_time, update_time)
VALUES
(92001, 20001, 10001, 80001, 70001, 101, 'online', '北京', '北京市海淀区', DATE_ADD(NOW(3), INTERVAL 2 DAY), 60, 1, 20001, NULL, NULL, NULL, '想先做一次试课', DATE_SUB(NOW(3), INTERVAL 1 DAY), NOW(3)),
(92002, 20002, 10002, 80002, 70003, 202, 'online', '北京', '北京市朝阳区', DATE_ADD(NOW(3), INTERVAL 3 DAY), 60, 2, 20002, NULL, NULL, NULL, '阅读写作同步提升', DATE_SUB(NOW(3), INTERVAL 2 DAY), NOW(3)),
(92003, 20006, 10006, 80006, 70011, 401, 'offline', '北京', '北京市通州区', DATE_ADD(NOW(3), INTERVAL 4 DAY), 60, 1, 20006, NULL, NULL, NULL, '考级辅导', DATE_SUB(NOW(3), INTERVAL 2 DAY), NOW(3)),
(92004, 20009, 10005, 80009, 70009, 403, 'online', '北京', '北京市石景山区', DATE_ADD(NOW(3), INTERVAL 1 DAY), 60, 5, 20009, NULL, NULL, NULL, '已完成一次课', DATE_SUB(NOW(3), INTERVAL 6 DAY), NOW(3)),
(92005, 20003, 10003, 80003, 70005, 203, 'online', '北京', '北京市西城区', DATE_ADD(NOW(3), INTERVAL 5 DAY), 60, 4, 20003, NULL, NULL, 20003, '时间冲突先取消', DATE_SUB(NOW(3), INTERVAL 3 DAY), NOW(3))
ON DUPLICATE KEY UPDATE
status=VALUES(status),
start_time=VALUES(start_time),
duration_minutes=VALUES(duration_minutes),
remark=VALUES(remark),
update_time=VALUES(update_time);

INSERT INTO `user` (id, name, phone, avatar, sex, open_id, active_status, last_opt_time, ip_info, item_id, status, user_type, ref_id, create_time, update_time)
VALUES
(111, '家长-唐女士', '15268836911', '/avatars/u111.png', 2, NULL, 2, NOW(3), NULL, NULL, 0, 2, 1011, NOW(3), NOW(3)),
(112, '家长-韩先生', '15268836912', '/avatars/u112.png', 1, NULL, 2, NOW(3), NULL, NULL, 0, 2, 1012, NOW(3), NOW(3)),
(113, '学生-许同学', '15268836913', '/avatars/u113.png', 2, NULL, 2, NOW(3), NULL, NULL, 0, 2, 1013, NOW(3), NOW(3)),
(114, '家长-邓女士', '15268836914', '/avatars/u114.png', 2, NULL, 2, NOW(3), NULL, NULL, 0, 2, 1014, NOW(3), NOW(3)),
(115, '家长-何先生', '15268836915', '/avatars/u115.png', 1, NULL, 2, NOW(3), NULL, NULL, 0, 2, 1015, NOW(3), NOW(3)),
(116, '学生-沈同学', '15268836916', '/avatars/u116.png', 1, NULL, 2, NOW(3), NULL, NULL, 0, 2, 1016, NOW(3), NOW(3)),
(206, '教师-徐老师', '13812345006', '/avatars/t206.png', 1, NULL, 2, NOW(3), NULL, NULL, 0, 1, 2006, NOW(3), NOW(3)),
(207, '教师-孙老师', '13812345007', '/avatars/t207.png', 2, NULL, 2, NOW(3), NULL, NULL, 0, 1, 2007, NOW(3), NOW(3)),
(208, '教师-罗老师', '13812345008', '/avatars/t208.png', 1, NULL, 2, NOW(3), NULL, NULL, 0, 1, 2008, NOW(3), NOW(3)),
(209, '教师-冯老师', '13812345009', '/avatars/t209.png', 2, NULL, 2, NOW(3), NULL, NULL, 0, 1, 2009, NOW(3), NOW(3)),
(210, '教师-高老师', '13812345010', '/avatars/t210.png', 1, NULL, 2, NOW(3), NULL, NULL, 0, 1, 2010, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
name=VALUES(name),
phone=VALUES(phone),
avatar=VALUES(avatar),
sex=VALUES(sex),
active_status=VALUES(active_status),
last_opt_time=VALUES(last_opt_time),
status=VALUES(status),
user_type=VALUES(user_type),
ref_id=VALUES(ref_id),
update_time=VALUES(update_time);

INSERT INTO `student_profile` (id, user_id, real_name, age, address, demand_description, budget, status, create_time, update_time)
VALUES
(1011, 111, '唐女士', 11, '天津·和平·小白楼', '五年级，数学应用题经常漏条件，想系统训练审题与列式；希望每次课后有作业、错题整理与家长反馈。', 190.00, 1, NOW(3), NOW(3)),
(1012, 112, '韩先生', 15, '重庆·渝中·解放碑', '初三英语：阅读理解速度慢、完形正确率低；希望老师能给词汇背诵计划、每周两次固定训练并阶段测评。', 240.00, 1, NOW(3), NOW(3)),
(1013, 113, '许同学', 17, '长沙·岳麓·梅溪湖', '高二物理：电磁学概念混乱，作业经常写不完；希望从基础概念到典型模型逐步补齐，并配套真题训练。', 300.00, 1, NOW(3), NOW(3)),
(1014, 114, '邓女士', 9, '厦门·思明·软件园', '四年级语文：阅读理解抓不住中心，作文缺素材；希望提升阅读方法、好词好句积累与写作结构。', 180.00, 1, NOW(3), NOW(3)),
(1015, 115, '何先生', 12, '昆明·五华·南屏街', '六年级英语：自然拼读薄弱，口语不敢开口；希望加强发音纠正、分级阅读与情景对话。', 170.00, 1, NOW(3), NOW(3)),
(1016, 116, '沈同学', 13, '沈阳·和平·太原街', '初二数学：函数与几何综合题不稳定；希望专题训练+错题复盘，提升解题步骤与书写规范。', 220.00, 1, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
real_name=VALUES(real_name),
age=VALUES(age),
address=VALUES(address),
demand_description=VALUES(demand_description),
budget=VALUES(budget),
status=VALUES(status),
update_time=VALUES(update_time);

INSERT INTO `teacher_profile` (id, user_id, real_name, education, subject, experience_years, rate_per_hour, introduction, city, highest_edu_school, teaching_mode, resume_completed, certificate_urls, status, create_time, update_time)
VALUES
(2006, 206, '徐一鸣', 'DOUBLE_FIRST_CLASS', '初中数学,高中数学', 8, 320.00, '擅长中考/高考数学提分，讲解注重“方法-训练-复盘”闭环；可提供阶段测评、错题本与学习计划。', '北京', '中国人民大学', 'ONLINE', 1, NULL, 1, NOW(3), NOW(3)),
(2007, 207, '孙雨晴', 'OVERSEAS', '初中英语,高中英语', 6, 280.00, '阅读写作双线提升，擅长把语法融入语境；课堂互动多，适合口语与写作提升。', '北京', '对外经济贸易大学', 'ONLINE', 1, NULL, 1, NOW(3), NOW(3)),
(2008, 208, '罗子涵', 'C985', '初中物理,高中物理', 7, 340.00, '模型化讲解+典型题归纳，带学生建立知识网络；课后提供讲义与练习讲评。', '北京', '北京航空航天大学', 'OFFLINE', 1, NULL, 1, NOW(3), NOW(3)),
(2009, 209, '冯若兰', 'BACHELOR', '小学语文,初中语文', 5, 230.00, '阅读理解方法训练+写作素材积累，重视表达结构与写作思维。', '北京', '首都师范大学', 'BOTH', 1, NULL, 1, NOW(3), NOW(3)),
(2010, 210, '高天', 'QS50', '小学英语,初中英语', 4, 240.00, '自然拼读+分级阅读+口语表达循序渐进，注重发音纠正与习惯养成。', '北京', '北京语言大学', 'ONLINE', 1, NULL, 1, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
real_name=VALUES(real_name),
education=VALUES(education),
subject=VALUES(subject),
experience_years=VALUES(experience_years),
rate_per_hour=VALUES(rate_per_hour),
introduction=VALUES(introduction),
city=VALUES(city),
highest_edu_school=VALUES(highest_edu_school),
teaching_mode=VALUES(teaching_mode),
resume_completed=VALUES(resume_completed),
certificate_urls=VALUES(certificate_urls),
status=VALUES(status),
update_time=VALUES(update_time);

INSERT INTO `teacher_job_posting`
(id, tutor_id, subject_id, title, description, price_per_hour, mode, city, available_time, max_students, status, create_time, update_time)
VALUES
(70101, 206, 201, '天津初中数学提分（函数/几何/压轴）', '按章节诊断+题型训练，输出错题本与周测；适合想系统提升与冲刺高分的学生。', 260.00, 'offline', '天津', '["Tue 19-21","Sat 10-12"]', 1, 1, NOW(), NOW()),
(70102, 206, 301, '线上高中数学（导数/圆锥曲线）', '梳理知识网络，压轴题拆解到步骤；课后练习+讲评。', 280.00, 'online', '全国', '["Wed 20-22","Sun 10-12"]', 1, 1, NOW(), NOW()),
(70103, 207, 202, '重庆初中英语阅读写作提升', '阅读速度+语法巩固+写作结构训练，配套词汇计划与阶段测评。', 220.00, 'offline', '重庆', '["Mon 19-21","Thu 19-21"]', 1, 1, NOW(), NOW()),
(70104, 207, 302, '线上高中英语写作提分', '审题-结构-表达三步法；逐篇精批作文，建立模板与素材库。', 240.00, 'online', '全国', '["Sat 19-21"]', 1, 1, NOW(), NOW()),
(70105, 208, 303, '长沙高中物理电磁学系统补强', '从场强/磁场到电磁感应，模型化讲解+真题训练；适合基础薄弱到冲刺阶段。', 300.00, 'offline', '长沙', '["Tue 20-22","Sat 14-16"]', 1, 1, NOW(), NOW()),
(70106, 209, 102, '厦门小学语文阅读与作文（方法+积累）', '阅读理解四步法+写作结构训练；每周阅读任务+素材积累与复盘。', 180.00, 'offline', '厦门', '["Wed 18-20","Sun 10-12"]', 1, 1, NOW(), NOW()),
(70107, 210, 103, '昆明小学英语自然拼读+口语陪练', '发音纠正+分级阅读+情景对话；适合零基础或不敢开口的孩子。', 170.00, 'offline', '昆明', '["Tue 18-20","Thu 18-20"]', 1, 1, NOW(), NOW()),
(70108, 210, 202, '沈阳初中英语词汇语法专项', '高频词+核心语法，配套真题训练与错题复盘；提升阅读与完形正确率。', 190.00, 'offline', '沈阳', '["Sat 09-11"]', 1, 0, NOW(), NOW())
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

INSERT INTO `student_job_posting`
(id, parent_id, subject_id, subject_name, student_gender, grade_code, available_time, teacher_gender_preference, teacher_requirement_detail, title, description, child_age, class_mode, city, address, frequency_per_week, budget_min, budget_max, stage_code, education_requirement, publisher_identity, schedule, status, create_time, update_time)
VALUES
(3031, 111, 101, '小学数学', 'female', 'GRADE5', '周二/周四晚 19:00-21:00；周末上午可协调', 'both', '希望老师能先做诊断小测，给出阶段计划；能布置并批改作业，愿意和家长沟通学习进展；最好有带过小升初衔接经验。', '天津五年级数学应用题系统训练（审题+列式）', '目前薄弱点：①题目条件多，容易漏看；②列式步骤不规范；③检查意识弱。期望目标：形成稳定的审题流程，提升应用题正确率与速度。授课形式：以题型为主线，配合错题本与每周小测。', 11, 'offline', '天津', '和平区小白楼附近（地铁可达）', 2, 160.00, 220.00, 'PRIMARY', 'BACHELOR', 'PARENT', '["Tue 19-21","Thu 19-21"]', 1, NOW(), NOW()),
(3032, 112, 202, '初中英语', 'male', 'JUNIOR3', '工作日晚上 19:30 后；周日白天', 'female', '偏好有体系的词汇教学与阅读方法训练；希望老师能给出可执行的背诵/复习计划；需要阶段测评与错题复盘。', '重庆初三英语阅读+完形提分（计划+复盘）', '当前问题：阅读速度慢、长难句理解差、完形常错。目标：提升阅读定位能力与语法理解，建立词汇复习节奏。希望：每节课有明确目标，课后作业量适中但必须讲评。', 15, 'offline', '重庆', '渝中区解放碑附近（可上门或附近自习室）', 2, 180.00, 260.00, 'JUNIOR', 'C211', 'PARENT', '["Wed 20-21","Sun 10-12"]', 1, NOW(), NOW()),
(3033, 113, 303, '高中物理', 'female', 'SENIOR2', '周二/周六 20:00-22:00；可加一节线上答疑', 'both', '希望老师能从概念到模型系统讲解，不要直接刷题；需要把公式推导与常见陷阱讲透；最好能提供讲义。', '长沙高二物理电磁学补基础+真题训练', '学习情况：课堂能听懂一部分但做题无从下手，公式不会用。目标：建立电场/磁场/电磁感应知识框架，掌握典型模型并能独立完成中档题。授课方式：讲解+例题+当堂训练+课后作业讲评。', 17, 'offline', '长沙', '岳麓区梅溪湖附近', 2, 240.00, 340.00, 'SENIOR', 'C985', 'STUDENT_SELF', '["Tue 20-22","Sat 20-22"]', 1, NOW(), NOW()),
(3034, 114, 102, '小学语文', 'male', 'GRADE4', '周三晚 18:00-20:00；周日 10:00-12:00', 'both', '希望老师耐心，擅长引导孩子表达；希望每周有阅读任务与积累清单；作文要有结构训练。', '厦门四年级语文阅读理解+作文结构训练', '主要诉求：阅读理解抓不住重点、作文流水账。目标：掌握段落中心提炼方法、积累素材并能按“开头-过程-结尾”写完整作文。课堂希望多互动，多鼓励。', 9, 'offline', '厦门', '思明区软件园附近', 2, 140.00, 220.00, 'PRIMARY', 'BACHELOR', 'PARENT', '["Wed 18-20","Sun 10-12"]', 1, NOW(), NOW()),
(3035, 115, 103, '小学英语', 'female', 'GRADE6', '周二/周四 18:00-20:00；周六上午可选', 'both', '希望老师能纠正发音、带读、并建立背单词和听力打卡机制；偏好有分级阅读材料。', '昆明六年级英语自然拼读+口语陪练（发音纠正）', '现状：单词记不牢、发音不准、不敢开口。目标：掌握常见拼读规则，敢于开口表达；形成每周学习节奏（听力/阅读/口语）。授课方式：拼读规则讲解+带读+情景对话+课后打卡。', 12, 'offline', '昆明', '五华区南屏街附近', 2, 120.00, 190.00, 'PRIMARY', 'UNLIMITED', 'PARENT', '["Tue 18-20","Thu 18-20"]', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
subject_name=VALUES(subject_name),
student_gender=VALUES(student_gender),
grade_code=VALUES(grade_code),
available_time=VALUES(available_time),
teacher_gender_preference=VALUES(teacher_gender_preference),
teacher_requirement_detail=VALUES(teacher_requirement_detail),
title=VALUES(title),
description=VALUES(description),
child_age=VALUES(child_age),
class_mode=VALUES(class_mode),
city=VALUES(city),
address=VALUES(address),
frequency_per_week=VALUES(frequency_per_week),
budget_min=VALUES(budget_min),
budget_max=VALUES(budget_max),
stage_code=VALUES(stage_code),
education_requirement=VALUES(education_requirement),
publisher_identity=VALUES(publisher_identity),
schedule=VALUES(schedule),
status=VALUES(status),
update_time=VALUES(update_time);

INSERT IGNORE INTO tutor_favorite_demand (tutor_id, demand_id, create_time)
VALUES
(206, 3031, NOW()),
(207, 3032, NOW()),
(208, 3033, NOW());

INSERT IGNORE INTO parent_favorite_tutor (parent_id, tutor_id, create_time)
VALUES
(111, 206, NOW()),
(112, 207, NOW()),
(114, 209, NOW());

INSERT INTO tutor_application
(id, sender_uid, receiver_uid, sender_role, receiver_role, context_type, context_id, content, client_request_id, status, chat_access_status, room_id, decided_at, receiver_read, receiver_read_time, create_time, update_time)
VALUES
(51001, 206, 111, 'TEACHER', 'STUDENT', 'DEMAND', 3031, '您好，我看了您的需求。我擅长应用题审题与列式训练，会先做诊断小测并制定每周计划，课后作业会批改并和家长同步进度。', 'seed-51001', 'PENDING', 'NONE', NULL, NULL, 0, NULL, DATE_SUB(NOW(3), INTERVAL 3 HOUR), DATE_SUB(NOW(3), INTERVAL 3 HOUR)),
(51002, 113, 207, 'STUDENT', 'TEACHER', 'TUTOR', 2007, '老师您好，我想提升英语阅读速度和写作结构。希望每周两次固定上课，课后有词汇计划和作文批改。', 'seed-51002', 'PENDING', 'NONE', NULL, NULL, 0, NULL, DATE_SUB(NOW(3), INTERVAL 2 HOUR), DATE_SUB(NOW(3), INTERVAL 2 HOUR)),
(51003, 206, 112, 'TEACHER', 'STUDENT', 'DEMAND', 3032, '您好，我可以从长难句分析与阅读定位方法入手，同时配套词汇计划与阶段测评。若同意我会先安排一次诊断课。', 'seed-51003', 'ACCEPTED', 'PAYMENT_REQUIRED', NULL, DATE_SUB(NOW(3), INTERVAL 1 DAY), 1, DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 2 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY)),
(51004, 208, 113, 'TEACHER', 'STUDENT', 'DEMAND', 3033, '您好，我会用模型化方式讲电磁学，从基础概念到典型题系统训练，并提供讲义与作业讲评。', 'seed-51004', 'ACCEPTED', 'CHAT_ENABLED', 90101, DATE_SUB(NOW(3), INTERVAL 3 DAY), 1, DATE_SUB(NOW(3), INTERVAL 3 DAY), DATE_SUB(NOW(3), INTERVAL 5 DAY), DATE_SUB(NOW(3), INTERVAL 3 DAY))
ON DUPLICATE KEY UPDATE
content=VALUES(content),
status=VALUES(status),
chat_access_status=VALUES(chat_access_status),
room_id=VALUES(room_id),
decided_at=VALUES(decided_at),
receiver_read=VALUES(receiver_read),
receiver_read_time=VALUES(receiver_read_time),
update_time=VALUES(update_time);

INSERT INTO brokerage_order
(id, proposal_id, application_id, room_id, payer_uid, amount_fen, pay_method, status, proof_url, proof_note, paid_at, create_time, update_time)
VALUES
(98001, NULL, 51003, NULL, 206, 19900, NULL, 'PENDING', NULL, NULL, NULL, DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY)),
(98002, NULL, 51004, 90101, 208, 19900, 'WECHAT', 'PAID', 'https://example.com/pay/proof/98002.png', 'seed paid', DATE_SUB(NOW(3), INTERVAL 3 DAY), DATE_SUB(NOW(3), INTERVAL 4 DAY), DATE_SUB(NOW(3), INTERVAL 3 DAY))
ON DUPLICATE KEY UPDATE
application_id=VALUES(application_id),
room_id=VALUES(room_id),
pay_method=VALUES(pay_method),
status=VALUES(status),
proof_url=VALUES(proof_url),
proof_note=VALUES(proof_note),
paid_at=VALUES(paid_at),
update_time=VALUES(update_time);

INSERT INTO application_brokerage_order
(id, application_id, order_id, create_time, update_time)
VALUES
(96001, 51003, 98001, DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY)),
(96002, 51004, 98002, DATE_SUB(NOW(3), INTERVAL 4 DAY), DATE_SUB(NOW(3), INTERVAL 3 DAY))
ON DUPLICATE KEY UPDATE
order_id=VALUES(order_id),
update_time=VALUES(update_time);

INSERT INTO room
(id, teacher_profile_id, student_profile_id, active_time, last_msg_id, status, create_time, update_time)
VALUES
(90101, 2008, 1013, NOW(3), 91103, 1, DATE_SUB(NOW(3), INTERVAL 3 DAY), NOW(3))
ON DUPLICATE KEY UPDATE
active_time=VALUES(active_time),
last_msg_id=VALUES(last_msg_id),
status=VALUES(status),
update_time=VALUES(update_time);

INSERT INTO message
(id, room_id, from_uid, to_uid, content, reply_msg_id, status, gap_count, type, extra, create_time, update_time)
VALUES
(91101, 90101, 113, 208, '老师您好，我电磁学有点跟不上，尤其是电磁感应这块。', NULL, 0, NULL, 1, NULL, DATE_SUB(NOW(3), INTERVAL 3 DAY), DATE_SUB(NOW(3), INTERVAL 3 DAY)),
(91102, 90101, 208, 113, '好的，我们先从磁通量与楞次定律的直观理解开始，再做典型题。', NULL, 0, NULL, 1, NULL, DATE_SUB(NOW(3), INTERVAL 3 DAY), DATE_SUB(NOW(3), INTERVAL 3 DAY)),
(91103, 90101, 113, 208, '明白了，我今晚把作业题发您看一下。', NULL, 0, NULL, 1, NULL, DATE_SUB(NOW(3), INTERVAL 2 DAY), DATE_SUB(NOW(3), INTERVAL 2 DAY))
ON DUPLICATE KEY UPDATE
content=VALUES(content),
status=VALUES(status),
type=VALUES(type),
update_time=VALUES(update_time);

INSERT INTO user_settings
(user_id, application_greeting, settings_json, create_time, update_time)
VALUES
(206, '您好，我看了您的需求，和我的授课方向非常匹配，我们可以进一步详细沟通吗？', JSON_OBJECT('version', 1), NOW(3), NOW(3)),
(207, '您好，我希望了解下您的学习目标与当前水平，我们可以详细聊聊吗？', JSON_OBJECT('version', 1), NOW(3), NOW(3)),
(208, '您好，我和岗位的匹配度很高，可以通过详细聊聊吗', JSON_OBJECT('version', 1), NOW(3), NOW(3)),
(111, '您好，我想进一步了解老师的授课方式与排课时间，可以详细聊聊吗？', JSON_OBJECT('version', 1), NOW(3), NOW(3)),
(113, '老师您好，我想提升薄弱项并制定学习计划，方便详细聊聊吗？', JSON_OBJECT('version', 1), NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
application_greeting=VALUES(application_greeting),
settings_json=VALUES(settings_json),
update_time=VALUES(update_time);

INSERT INTO `user`
(id, name, phone, avatar, sex, open_id, active_status, last_opt_time, ip_info, item_id, status, user_type, ref_id, create_time, update_time)
VALUES
(117, '家长-郭女士', '15268836917', '/avatars/u117.png', 2, NULL, 2, NOW(3), NULL, NULL, 0, 2, 1017, NOW(3), NOW(3)),
(118, '家长-蒋先生', '15268836918', '/avatars/u118.png', 1, NULL, 2, NOW(3), NULL, NULL, 0, 2, 1018, NOW(3), NOW(3)),
(119, '学生-袁同学', '15268836919', '/avatars/u119.png', 1, NULL, 2, NOW(3), NULL, NULL, 0, 2, 1019, NOW(3), NOW(3)),
(120, '家长-彭女士', '15268836920', '/avatars/u120.png', 2, NULL, 2, NOW(3), NULL, NULL, 0, 2, 1020, NOW(3), NOW(3)),
(121, '家长-谢先生', '15268836921', '/avatars/u121.png', 1, NULL, 2, NOW(3), NULL, NULL, 0, 2, 1021, NOW(3), NOW(3)),
(122, '学生-曹同学', '15268836922', '/avatars/u122.png', 2, NULL, 2, NOW(3), NULL, NULL, 0, 2, 1022, NOW(3), NOW(3)),
(123, '家长-赖女士', '15268836923', '/avatars/u123.png', 2, NULL, 2, NOW(3), NULL, NULL, 0, 2, 1023, NOW(3), NOW(3)),
(124, '家长-宋先生', '15268836924', '/avatars/u124.png', 1, NULL, 2, NOW(3), NULL, NULL, 0, 2, 1024, NOW(3), NOW(3)),
(211, '教师-顾老师', '13812345011', '/avatars/t211.png', 1, NULL, 2, NOW(3), NULL, NULL, 0, 1, 2011, NOW(3), NOW(3)),
(212, '教师-梁老师', '13812345012', '/avatars/t212.png', 2, NULL, 2, NOW(3), NULL, NULL, 0, 1, 2012, NOW(3), NOW(3)),
(213, '教师-唐老师', '13812345013', '/avatars/t213.png', 1, NULL, 2, NOW(3), NULL, NULL, 0, 1, 2013, NOW(3), NOW(3)),
(214, '教师-许老师', '13812345014', '/avatars/t214.png', 2, NULL, 2, NOW(3), NULL, NULL, 0, 1, 2014, NOW(3), NOW(3)),
(215, '教师-贺老师', '13812345015', '/avatars/t215.png', 1, NULL, 2, NOW(3), NULL, NULL, 0, 1, 2015, NOW(3), NOW(3)),
(216, '教师-程老师', '13812345016', '/avatars/t216.png', 2, NULL, 2, NOW(3), NULL, NULL, 0, 1, 2016, NOW(3), NOW(3)),
(217, '教师-苏老师', '13812345017', '/avatars/t217.png', 1, NULL, 2, NOW(3), NULL, NULL, 0, 1, 2017, NOW(3), NOW(3)),
(218, '教师-林老师', '13812345018', '/avatars/t218.png', 2, NULL, 2, NOW(3), NULL, NULL, 0, 1, 2018, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
name=VALUES(name),
phone=VALUES(phone),
avatar=VALUES(avatar),
sex=VALUES(sex),
active_status=VALUES(active_status),
last_opt_time=VALUES(last_opt_time),
status=VALUES(status),
user_type=VALUES(user_type),
ref_id=VALUES(ref_id),
update_time=VALUES(update_time);

INSERT INTO `student_profile`
(id, user_id, real_name, age, address, demand_description, budget, status, create_time, update_time)
VALUES
(1017, 117, '郭女士', 11, '上海·徐汇·漕河泾', '孩子五年级，应用题和几何题都不稳定，希望老师能先测评再做阶段训练。', 210.00, 1, NOW(3), NOW(3)),
(1018, 118, '蒋先生', 16, '广州·天河·珠江新城', '高一英语写作和阅读偏弱，想提升考试得分并养成复盘习惯。', 260.00, 1, NOW(3), NOW(3)),
(1019, 119, '袁同学', 17, '成都·高新·金融城', '高二化学有机部分容易混淆，希望老师能系统梳理反应路线。', 300.00, 1, NOW(3), NOW(3)),
(1020, 120, '彭女士', 8, '南京·鼓楼·龙江', '二年级语文识字量和阅读兴趣不足，想找耐心的老师打基础。', 150.00, 1, NOW(3), NOW(3)),
(1021, 121, '谢先生', 13, '杭州·拱墅·武林广场', '初二物理力学、电学都不稳，希望老师能结合错题做专题突破。', 240.00, 1, NOW(3), NOW(3)),
(1022, 122, '曹同学', 10, '武汉·武昌·徐东', '想学 Python 做小游戏和简单动画，培养编程兴趣。', 190.00, 1, NOW(3), NOW(3)),
(1023, 123, '赖女士', 12, '苏州·工业园区·湖西', '六年级英语自然拼读一般，口语不敢开口，想配合阅读和打卡。', 180.00, 1, NOW(3), NOW(3)),
(1024, 124, '宋先生', 14, '西安·雁塔·高新路', '初三数学压轴题失分严重，希望老师有中考冲刺经验。', 280.00, 1, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
real_name=VALUES(real_name),
age=VALUES(age),
address=VALUES(address),
demand_description=VALUES(demand_description),
budget=VALUES(budget),
status=VALUES(status),
update_time=VALUES(update_time);

INSERT INTO `teacher_profile`
(id, user_id, real_name, education, subject, experience_years, rate_per_hour, introduction, city, highest_edu_school, teaching_mode, resume_completed, certificate_urls, status, create_time, update_time)
VALUES
(2011, 211, '顾承宇', 'C985', '小学数学,初中数学', 7, 260.00, '擅长诊断薄弱点并制定提升节奏，课堂重视方法拆解与错题复盘。', '上海', '复旦大学', 'BOTH', 1, NULL, 1, NOW(3), NOW(3)),
(2012, 212, '梁知夏', 'OVERSEAS', '初中英语,高中英语', 6, 280.00, '阅读与写作双线提升，善于把语法融入真实语境，适合提分与表达训练。', '广州', '香港中文大学', 'ONLINE', 1, NULL, 1, NOW(3), NOW(3)),
(2013, 213, '唐哲远', 'DOUBLE_FIRST_CLASS', '高中化学,初中化学', 8, 320.00, '讲解逻辑清晰，擅长把知识点串成体系，适合基础补齐与冲刺提分。', '成都', '四川大学', 'OFFLINE', 1, NULL, 1, NOW(3), NOW(3)),
(2014, 214, '许若宁', 'BACHELOR', '小学语文,初中语文', 5, 210.00, '阅读理解与作文结构训练经验丰富，课堂互动足，适合低龄孩子建立兴趣。', '南京', '南京师范大学', 'OFFLINE', 1, NULL, 1, NOW(3), NOW(3)),
(2015, 215, '贺青川', 'C211', '初中物理,高中物理', 6, 290.00, '擅长把力学、电学模型讲透，配套练习与阶段讲评，帮助学生建立知识网络。', '杭州', '浙江大学', 'BOTH', 1, NULL, 1, NOW(3), NOW(3)),
(2016, 216, '程可欣', 'BACHELOR', '编程(Python),小学数学', 4, 220.00, '从兴趣切入做项目，注重鼓励式教学，让学生边学边看到作品成果。', '武汉', '华中科技大学', 'ONLINE', 1, NULL, 1, NOW(3), NOW(3)),
(2017, 217, '苏晚秋', 'QS50', '小学英语,初中英语', 7, 240.00, '自然拼读、口语陪练和分级阅读经验丰富，擅长帮助孩子建立开口自信。', '苏州', '上海外国语大学', 'ONLINE', 1, NULL, 1, NOW(3), NOW(3)),
(2018, 218, '林若岚', 'C985', '初中数学,高中数学', 9, 340.00, '专注中考数学冲刺，擅长压轴题拆解、规范书写与阶段测评。', '西安', '西安交通大学', 'OFFLINE', 1, NULL, 1, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
real_name=VALUES(real_name),
education=VALUES(education),
subject=VALUES(subject),
experience_years=VALUES(experience_years),
rate_per_hour=VALUES(rate_per_hour),
introduction=VALUES(introduction),
city=VALUES(city),
highest_edu_school=VALUES(highest_edu_school),
teaching_mode=VALUES(teaching_mode),
resume_completed=VALUES(resume_completed),
certificate_urls=VALUES(certificate_urls),
status=VALUES(status),
update_time=VALUES(update_time);

INSERT INTO `teacher_job_posting`
(id, tutor_id, subject_id, title, description, price_per_hour, mode, city, available_time, max_students, status, create_time, update_time)
VALUES
(70109, 211, 101, '上海小学数学应用题专项（诊断+提升）', '先做知识点诊断，再按题型分层训练，适合想提升正确率和审题能力的学生。', 220.00, 'offline', '上海', '["Tue 19-21","Sat 09-11"]', 1, 1, NOW(), NOW()),
(70110, 211, 201, '上海初中数学同步补弱（函数/几何）', '同步巩固+错题复盘，帮助学生把步骤和思路真正讲清楚。', 260.00, 'both', '上海', '["Wed 19-21","Sun 10-12"]', 1, 1, NOW(), NOW()),
(70111, 212, 202, '广州初中英语阅读提分（词汇+长难句）', '阅读定位、词汇复习和长难句拆解结合，适合阶段提分。', 230.00, 'online', '全国', '["Mon 19-21","Thu 19-21"]', 1, 1, NOW(), NOW()),
(70112, 212, 302, '线上高中英语写作精批', '每次课聚焦一个写作主题，讲模板、改表达、做复盘。', 280.00, 'online', '全国', '["Sat 19-21","Sun 14-16"]', 1, 1, NOW(), NOW()),
(70113, 213, 204, '成都初中化学基础夯实（方程式/计算）', '从核心概念到方程式规范书写，配套练习与课后讲评。', 240.00, 'offline', '成都', '["Tue 18-20","Fri 19-21"]', 1, 1, NOW(), NOW()),
(70114, 213, 304, '成都高中化学有机反应路线系统课', '梳理官能团、反应条件和常见题型，适合高一高二补体系。', 320.00, 'offline', '成都', '["Wed 20-22","Sat 14-16"]', 1, 1, NOW(), NOW()),
(70115, 214, 102, '南京小学语文阅读启蒙与表达训练', '通过绘本、短文和复述训练提升阅读兴趣与表达能力。', 180.00, 'offline', '南京', '["Tue 18-20","Sun 10-12"]', 1, 1, NOW(), NOW()),
(70116, 214, 205, '南京初中语文阅读理解与作文结构', '题型方法+素材积累双线推进，适合基础一般但想稳定提分的学生。', 220.00, 'offline', '南京', '["Thu 19-21","Sat 10-12"]', 1, 1, NOW(), NOW()),
(70117, 215, 203, '杭州初中物理力学模型课', '把受力分析、牛顿定律和典型题型串成完整框架。', 260.00, 'both', '杭州', '["Tue 20-22","Sun 09-11"]', 1, 1, NOW(), NOW()),
(70118, 215, 303, '杭州高中物理电学冲刺（诊断+专项）', '适合高一高二学生查漏补缺，也适合高三专题强化。', 300.00, 'both', '杭州', '["Thu 20-22","Sat 19-21"]', 1, 1, NOW(), NOW()),
(70119, 216, 403, '线上 Python 启蒙（小游戏/小项目）', '从零开始写出看得见的小作品，建立逻辑和成就感。', 200.00, 'online', '全国', '["Wed 19-21","Sat 14-16"]', 1, 1, NOW(), NOW()),
(70120, 216, 101, '线上小学数学思维启蒙', '用趣味题和互动方式培养数学兴趣，适合低年级孩子。', 180.00, 'online', '全国', '["Sun 10-12"]', 1, 1, NOW(), NOW()),
(70121, 217, 103, '苏州小学英语自然拼读+分级阅读', '发音纠正、拼读规则和阅读复述结合，帮助孩子敢开口。', 190.00, 'online', '全国', '["Mon 18-20","Thu 18-20"]', 1, 1, NOW(), NOW()),
(70122, 217, 202, '苏州初中英语口语与听力训练', '场景对话+听力跟读+课后打卡，适合想快速提升语感的学生。', 220.00, 'online', '全国', '["Sat 09-11","Sun 09-11"]', 1, 1, NOW(), NOW()),
(70123, 218, 201, '西安初中数学中考冲刺（压轴拆解）', '把压轴题拆成步骤和模型，适合冲刺阶段稳定提分。', 300.00, 'offline', '西安', '["Tue 19-21","Fri 19-21"]', 1, 1, NOW(), NOW()),
(70124, 218, 301, '西安高中数学函数导数专项', '专题讲解+高频题型演练，帮助学生把思路真正落到书写上。', 340.00, 'offline', '西安', '["Sat 14-16","Sun 14-16"]', 1, 1, NOW(), NOW())
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

INSERT INTO `student_job_posting`
(id, parent_id, subject_id, subject_name, student_gender, grade_code, available_time, teacher_gender_preference, teacher_requirement_detail, title, description, child_age, class_mode, city, address, frequency_per_week, budget_min, budget_max, stage_code, education_requirement, publisher_identity, schedule, status, create_time, update_time)
VALUES
(3036, 117, 101, '小学数学', 'female', 'GRADE5', '周二、周四晚上 19:00 后；周六上午优先', 'both', '希望老师能先做能力诊断，课后有简短练习和反馈；孩子比较敏感，希望老师有耐心。', '上海五年级数学应用题专项提升', '目前主要问题是审题不完整、列式不稳定。希望老师能围绕应用题、几何和错题整理做系统训练，目标是提升校内测试稳定性。', 11, 'offline', '上海', '徐汇区漕河泾附近', 2, 180.00, 260.00, 'PRIMARY', 'BACHELOR', 'PARENT', '["Tue 19-21","Thu 19-21","Sat 09-11"]', 1, NOW(), NOW()),
(3037, 118, 302, '高中英语', 'male', 'SENIOR1', '工作日晚上 20:00 后；周日下午可协调', 'female', '偏好擅长写作精批和阅读定位的老师，希望老师能给出每周背诵/复习计划。', '广州高一英语阅读写作提分（目标期中明显提升）', '孩子基础不差，但写作得分不稳定、阅读速度偏慢。希望老师从句型表达、词汇复盘和真题阅读方法入手，逐步形成自己的答题节奏。', 16, 'online', '全国', NULL, 2, 220.00, 320.00, 'SENIOR', 'C211', 'PARENT', '["Wed 20-22","Sun 15-17"]', 1, NOW(), NOW()),
(3038, 119, 304, '高中化学', 'male', 'SENIOR2', '周二、周六 20:00-22:00；可额外线上答疑', 'both', '希望老师把有机反应路线和常考题型讲透，不希望只刷题；最好能提供讲义。', '成都高二化学有机体系梳理+真题训练', '我自己是学生，目前有机部分知识点容易串不起来，做题时反应条件和产物判断常出错。希望先补体系再做专题训练。', 17, 'offline', '成都', '高新区金融城附近', 2, 240.00, 360.00, 'SENIOR', 'DOUBLE_FIRST_CLASS', 'STUDENT_SELF', '["Tue 20-22","Sat 20-22"]', 1, NOW(), NOW()),
(3039, 120, 102, '小学语文', 'female', 'GRADE2', '周三 18:00-20:00；周日 10:00-12:00', 'both', '希望老师温和、有互动感，能培养阅读兴趣，不要一开始就太偏应试。', '南京二年级语文识字阅读启蒙', '孩子现在识字量有限，阅读时容易分神。想先把阅读兴趣和复述表达带起来，慢慢过渡到看图写话和基础写作。', 8, 'offline', '南京', '鼓楼区龙江附近', 2, 120.00, 180.00, 'PRIMARY', 'BACHELOR', 'PARENT', '["Wed 18-20","Sun 10-12"]', 1, NOW(), NOW()),
(3040, 121, 203, '初中物理', 'male', 'JUNIOR2', '周二/周四 19:30 后；周末白天可商量', 'male', '希望老师擅长力学和电学专题训练，最好能结合错题讲方法，课后有简洁作业。', '杭州初二物理专题补弱（力学/电学）', '目前班内成绩中等偏下，受力分析和电路判断总出错。希望通过 6-8 周的系统训练把基础题和中档题稳定下来。', 13, 'both', '杭州', '拱墅区武林广场附近', 2, 180.00, 280.00, 'JUNIOR', 'C211', 'PARENT', '["Tue 20-22","Thu 20-22","Sun 10-12"]', 1, NOW(), NOW()),
(3041, 122, 403, '编程(Python)', 'female', 'GRADE4', '周三晚和周六下午最合适', 'both', '希望老师能够循序渐进、鼓励式教学，最好每几节课就能做一个小作品。', '武汉四年级 Python 兴趣启蒙（小游戏方向）', '我想学编程做小游戏和动画，现在完全零基础。希望老师上课有趣一点，能带我一点点完成自己的作品。', 10, 'online', '全国', NULL, 2, 150.00, 240.00, 'PRIMARY', 'UNLIMITED', 'STUDENT_SELF', '["Wed 19-20","Sat 14-16"]', 1, NOW(), NOW()),
(3042, 123, 103, '小学英语', 'female', 'GRADE6', '工作日 18:30 后；周六上午', 'female', '希望老师有自然拼读和口语陪练经验，能布置阅读打卡并及时反馈。', '苏州六年级英语自然拼读+口语提升', '孩子词汇积累一般、发音不够准，也不太愿意开口。希望老师帮忙建立朗读和背词习惯，同时通过阅读和对话提升自信。', 12, 'online', '全国', NULL, 2, 140.00, 220.00, 'PRIMARY', 'BACHELOR', 'PARENT', '["Tue 18-20","Thu 18-20","Sat 09-11"]', 1, NOW(), NOW()),
(3043, 124, 201, '初中数学', 'male', 'JUNIOR3', '周一/周五晚上，周日下午也可以', 'both', '希望老师非常熟悉中考压轴题，能讲透函数和几何综合，最好有阶段测评。', '西安初三数学冲刺（函数/几何压轴题）', '孩子基础题还可以，但综合题和压轴题拿分少。目标是中考前把大题步骤和思路稳定下来，提升 10-15 分。', 14, 'offline', '西安', '雁塔区高新路附近', 2, 240.00, 360.00, 'JUNIOR', 'C985', 'PARENT', '["Mon 19-21","Fri 19-21","Sun 14-16"]', 1, NOW(), NOW()),
(3044, 117, 202, '初中英语', 'female', 'JUNIOR1', '周末下午优先，工作日晚间可协调', 'both', '想找发音和阅读都比较强的老师，孩子需要建立学习节奏，不希望课堂太沉闷。', '上海初一英语阅读理解+口语表达', '想趁着初一尽快把词汇、发音和阅读方法补上，后面进入初二初三会更轻松。希望老师能多鼓励孩子开口。', 12, 'both', '上海', '徐汇区漕河泾附近，可线上/线下', 2, 160.00, 240.00, 'JUNIOR', 'BACHELOR', 'PARENT', '["Sat 14-16","Sun 10-12"]', 1, NOW(), NOW()),
(3045, 121, 301, '高中数学', 'male', 'SENIOR1', '周末晚上为主，平时可安排一节答疑', 'both', '希望老师能够把函数、数列讲得有体系，配合阶段性小测和错题整理。', '杭州高一数学函数数列同步拔高', '孩子课内内容能跟上，但综合题一难就不会，想找老师从知识网络和题型方法两方面一起带。', 16, 'online', '全国', NULL, 2, 220.00, 320.00, 'SENIOR', 'DOUBLE_FIRST_CLASS', 'PARENT', '["Sat 19-21","Sun 19-21"]', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
subject_name=VALUES(subject_name),
student_gender=VALUES(student_gender),
grade_code=VALUES(grade_code),
available_time=VALUES(available_time),
teacher_gender_preference=VALUES(teacher_gender_preference),
teacher_requirement_detail=VALUES(teacher_requirement_detail),
title=VALUES(title),
description=VALUES(description),
child_age=VALUES(child_age),
class_mode=VALUES(class_mode),
city=VALUES(city),
address=VALUES(address),
frequency_per_week=VALUES(frequency_per_week),
budget_min=VALUES(budget_min),
budget_max=VALUES(budget_max),
stage_code=VALUES(stage_code),
education_requirement=VALUES(education_requirement),
publisher_identity=VALUES(publisher_identity),
schedule=VALUES(schedule),
status=VALUES(status),
update_time=VALUES(update_time);

INSERT IGNORE INTO tutor_favorite_demand (tutor_id, demand_id, create_time)
VALUES
(211, 3036, NOW()),
(212, 3037, NOW()),
(213, 3038, NOW()),
(215, 3040, NOW()),
(217, 3042, NOW()),
(218, 3043, NOW());

INSERT IGNORE INTO parent_favorite_tutor (parent_id, tutor_id, create_time)
VALUES
(117, 211, NOW()),
(118, 212, NOW()),
(120, 214, NOW()),
(121, 215, NOW()),
(123, 217, NOW()),
(124, 218, NOW());

INSERT INTO user_settings
(user_id, application_greeting, settings_json, create_time, update_time)
VALUES
(211, '您好，我会先帮您做一次学习情况诊断，再给出阶段提升计划，我们可以详细聊聊吗？', JSON_OBJECT('version', 1), NOW(3), NOW(3)),
(212, '您好，我看了您的需求，阅读和写作部分正好是我的强项，方便沟通下孩子目前情况吗？', JSON_OBJECT('version', 1), NOW(3), NOW(3)),
(215, '您好，我擅长力学和电学专题训练，可以先帮您定位薄弱点，再安排针对性课程。', JSON_OBJECT('version', 1), NOW(3), NOW(3)),
(217, '您好，我这边可以提供自然拼读、口语陪练和阅读打卡方案，想先了解下孩子现在的学习状态。', JSON_OBJECT('version', 1), NOW(3), NOW(3)),
(117, '您好，我想先了解老师的上课节奏、作业反馈方式和是否方便做阶段测评。', JSON_OBJECT('version', 1), NOW(3), NOW(3)),
(121, '老师您好，我比较看重讲题是否清晰、课后是否能有错题复盘，方便详细聊聊吗？', JSON_OBJECT('version', 1), NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
application_greeting=VALUES(application_greeting),
settings_json=VALUES(settings_json),
update_time=VALUES(update_time);

SET FOREIGN_KEY_CHECKS = 1;
