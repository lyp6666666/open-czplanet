SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

USE ai_tutor;

INSERT INTO `user` (id, name, phone, avatar, sex, open_id, active_status, last_opt_time, ip_info, item_id, status, user_type, ref_id, create_time, update_time)
VALUES
(101, '家长-林女士', '15268836901', '/avatars/u101.png', 2, NULL, 2, NOW(3), NULL, NULL, 0, 2, NULL, NOW(3), NOW(3)),
(102, '家长-王先生', '15268836902', '/avatars/u102.png', 1, NULL, 2, NOW(3), NULL, NULL, 0, 2, NULL, NOW(3), NOW(3)),
(103, '家长-张女士', '15268836903', '/avatars/u103.png', 2, NULL, 2, NOW(3), NULL, NULL, 0, 2, NULL, NOW(3), NOW(3)),
(104, '学生-陈同学', '15268836904', '/avatars/u104.png', 1, NULL, 2, NOW(3), NULL, NULL, 0, 2, NULL, NOW(3), NOW(3)),
(105, '学生-刘同学', '15268836905', '/avatars/u105.png', 2, NULL, 2, NOW(3), NULL, NULL, 0, 2, NULL, NOW(3), NOW(3)),
(106, '家长-赵女士', '15268836906', '/avatars/u106.png', 2, NULL, 2, NOW(3), NULL, NULL, 0, 2, NULL, NOW(3), NOW(3)),
(107, '家长-周先生', '15268836907', '/avatars/u107.png', 1, NULL, 2, NOW(3), NULL, NULL, 0, 2, NULL, NOW(3), NOW(3)),
(108, '家长-吴女士', '15268836908', '/avatars/u108.png', 2, NULL, 2, NOW(3), NULL, NULL, 0, 2, NULL, NOW(3), NOW(3)),
(109, '学生-孙同学', '15268836909', '/avatars/u109.png', 1, NULL, 2, NOW(3), NULL, NULL, 0, 2, NULL, NOW(3), NOW(3)),
(110, '家长-郑女士', '15268836910', '/avatars/u110.png', 2, NULL, 2, NOW(3), NULL, NULL, 0, 2, NULL, NOW(3), NOW(3)),
(201, '教师-李老师', '13812345001', '/avatars/t201.png', 1, NULL, 2, NOW(3), NULL, NULL, 0, 1, NULL, NOW(3), NOW(3)),
(202, '教师-周老师', '13812345002', '/avatars/t202.png', 2, NULL, 2, NOW(3), NULL, NULL, 0, 1, NULL, NOW(3), NOW(3)),
(203, '教师-王老师', '13812345003', '/avatars/t203.png', 1, NULL, 2, NOW(3), NULL, NULL, 0, 1, NULL, NOW(3), NOW(3)),
(204, '教师-陈老师', '13812345004', '/avatars/t204.png', 2, NULL, 2, NOW(3), NULL, NULL, 0, 1, NULL, NOW(3), NOW(3)),
(205, '教师-张老师', '13812345005', '/avatars/t205.png', 1, NULL, 2, NOW(3), NULL, NULL, 0, 1, NULL, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
name=VALUES(name),
avatar=VALUES(avatar),
sex=VALUES(sex),
status=VALUES(status),
user_type=VALUES(user_type),
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

INSERT INTO `teacher_profile` (id, user_id, real_name, education, subject, experience_years, rate_per_hour, introduction, certificate_urls, status, create_time, update_time)
VALUES
(2001, 201, '李老师', 'C985', '数学', 6, 260.00, '擅长小学到初中数学提分，注重方法与错题复盘。', NULL, 1, NOW(3), NOW(3)),
(2002, 202, '周老师', 'BACHELOR', '英语', 4, 220.00, '口语陪练+语法梳理，课堂节奏快但耐心。', NULL, 1, NOW(3), NOW(3)),
(2003, 203, '王老师', 'C211', '物理', 5, 280.00, '重点突破模型与题型，适合中高考备考。', NULL, 1, NOW(3), NOW(3)),
(2004, 204, '陈老师', 'OVERSEAS', '语文', 3, 240.00, '阅读理解与写作提分，善于引导表达。', NULL, 1, NOW(3), NOW(3)),
(2005, 205, '张老师', 'QS50', '奥数', 7, 320.00, '奥数启蒙到竞赛进阶，强调思维训练。', NULL, 1, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
real_name=VALUES(real_name),
education=VALUES(education),
subject=VALUES(subject),
experience_years=VALUES(experience_years),
rate_per_hour=VALUES(rate_per_hour),
introduction=VALUES(introduction),
status=VALUES(status),
update_time=VALUES(update_time);

INSERT INTO `student_job_posting`
(id, parent_id, subject_id, title, description, child_age, class_mode, city, address, frequency_per_week, budget_min, budget_max, stage_code, education_requirement, publisher_identity, schedule, status, create_time, update_time)
VALUES
(3001, 101, 201, '初中数学一对一提分（函数/几何）', '孩子初二，基础还可以但做题不稳。希望老师能带着建立解题框架，每次课后有作业并讲评。', 14, 'offline', '北京', '海淀区中关村附近，地铁可达', 2, 180.00, 240.00, 'JUNIOR', 'BACHELOR', 'PARENT', '["Tue 19-21","Sat 10-12"]', 1, NOW(3), NOW(3)),
(3002, 102, 202, '初中英语阅读理解+语法巩固', '想系统梳理时态/从句，提升阅读速度与准确率。最好能每周固定两次，课后给词汇计划。', 14, 'online', NULL, NULL, 2, 160.00, 220.00, 'JUNIOR', 'C211', 'PARENT', '["Wed 20-21","Sun 10-11"]', 1, NOW(3), NOW(3)),
(3003, 103, 205, '小学语文阅读写作提升', '三年级，作文不知道怎么写。希望老师能带着做阅读积累和写作结构训练。', 10, 'offline', '杭州', '西湖区文三路附近', 3, 120.00, 180.00, 'PRIMARY', 'BACHELOR', 'PARENT', '["Mon 19-20","Thu 19-20","Sat 9-10"]', 1, NOW(3), NOW(3)),
(3004, 104, 303, '高二物理电磁学补基础+刷题', '我自己是学生，高二电磁学听不太懂，想从基础概念到典型题系统补。', 17, 'offline', '广州', '天河区体育西路附近', 2, 220.00, 320.00, 'SENIOR', 'C985', 'STUDENT_SELF', '["Tue 20-22","Sat 14-16"]', 1, NOW(3), NOW(3)),
(3005, 105, 301, '高一数学函数与导数（重点突破）', '高一，函数与导数概念不清，题目一变就不会。希望老师讲清思路并配套练习。', 16, 'both', '深圳', '南山区科技园附近，可线上/线下', 3, 240.00, 360.00, 'SENIOR', 'DOUBLE_FIRST_CLASS', 'STUDENT_SELF', '["Mon 20-21","Wed 20-21","Sun 10-12"]', 1, NOW(3), NOW(3)),
(3006, 106, 204, '初中化学启蒙（提前预习）', '孩子初一，想提前预习化学建立兴趣，课堂以实验现象和概念理解为主。', 13, 'online', NULL, NULL, 1, 160.00, 220.00, 'JUNIOR', 'BACHELOR', 'PARENT', '["Sat 10-12"]', 1, NOW(3), NOW(3)),
(3007, 107, 103, '小学英语自然拼读+口语陪练', '四年级，单词记不住。希望老师有体系地带着练自然拼读，兼顾口语表达。', 9, 'offline', '上海', '杨浦区五角场附近', 2, 120.00, 180.00, 'PRIMARY', 'UNLIMITED', 'PARENT', '["Wed 19-20","Sun 19-20"]', 1, NOW(3), NOW(3)),
(3008, 108, 201, '初三数学冲刺（压轴题专项）', '初三，想重点突破几何与函数压轴题。希望老师能按题型拆解训练。', 15, 'offline', '杭州', '滨江区江陵路附近', 5, 220.00, 320.00, 'JUNIOR', 'C985', 'PARENT', '["Mon 19-21","Tue 19-21","Thu 19-21","Sat 9-12","Sun 9-12"]', 1, NOW(3), NOW(3)),
(3009, 109, 103, '小学英语词汇+听力习惯养成', '五年级，听力跟不上，词汇量也少。希望老师带着制定学习计划，逐步提升。', 12, 'online', NULL, NULL, 2, 100.00, 160.00, 'PRIMARY', 'BACHELOR', 'STUDENT_SELF', '["Tue 20-21","Thu 20-21"]', 1, NOW(3), NOW(3)),
(3010, 110, 101, '小学数学奥数启蒙（思维训练）', '四年级，想做奥数启蒙。希望老师有趣、能引导孩子思考，不刷题为主。', 11, 'offline', '深圳', '福田区会展中心附近', 2, 180.00, 260.00, 'PRIMARY', 'OVERSEAS', 'PARENT', '["Sat 10-12","Sun 10-12"]', 1, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
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

SET FOREIGN_KEY_CHECKS = 1;

