SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

USE ai_tutor;

-- QA seed for test host 117.72.111.39.
-- This script is idempotent and uses high ID ranges to avoid dev seed collisions.

INSERT INTO `user`
(id, name, phone, avatar, sex, open_id, active_status, last_opt_time, ip_info, item_id, status, user_type, ref_id, create_time, update_time)
VALUES
(910001, 'QA家长-北京王女士', '18611720001', '/avatars/qa-parent-910001.png', 2, NULL, 2, NOW(3), 'QA-117', NULL, 0, 2, 920001, NOW(3), NOW(3)),
(910002, 'QA家长-上海李先生', '18611720002', '/avatars/qa-parent-910002.png', 1, NULL, 2, NOW(3), 'QA-117', NULL, 0, 2, 920002, NOW(3), NOW(3)),
(910003, 'QA学生-广州陈同学', '18611720003', '/avatars/qa-student-910003.png', 1, NULL, 2, NOW(3), 'QA-117', NULL, 0, 2, 920003, NOW(3), NOW(3)),
(910004, 'QA机构-启航教育', '18611720004', '/avatars/qa-org-910004.png', NULL, NULL, 2, NOW(3), 'QA-117', NULL, 0, 3, 920004, NOW(3), NOW(3)),
(910101, 'QA教师-数学赵老师', '18611721001', '/avatars/qa-teacher-910101.png', 1, NULL, 2, NOW(3), 'QA-117', NULL, 0, 1, 930001, NOW(3), NOW(3)),
(910102, 'QA教师-英语周老师', '18611721002', '/avatars/qa-teacher-910102.png', 2, NULL, 2, NOW(3), 'QA-117', NULL, 0, 1, 930002, NOW(3), NOW(3)),
(910103, 'QA教师-物理孙老师', '18611721003', '/avatars/qa-teacher-910103.png', 1, NULL, 2, NOW(3), 'QA-117', NULL, 0, 1, 930003, NOW(3), NOW(3)),
(910104, 'QA教师-化学吴老师', '18611721004', '/avatars/qa-teacher-910104.png', 2, NULL, 2, NOW(3), 'QA-117', NULL, 0, 1, 930004, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
name=VALUES(name),
phone=VALUES(phone),
avatar=VALUES(avatar),
sex=VALUES(sex),
active_status=VALUES(active_status),
ip_info=VALUES(ip_info),
status=VALUES(status),
user_type=VALUES(user_type),
ref_id=VALUES(ref_id),
update_time=VALUES(update_time);

INSERT INTO `student_profile`
(id, user_id, real_name, age, address, demand_description, budget, status, create_time, update_time)
VALUES
(920001, 910001, '王女士', 14, '北京·海淀·五道口', 'QA：初二数学函数和几何薄弱，需要老师做诊断、错题复盘和阶段测评。', 260.00, 1, NOW(3), NOW(3)),
(920002, 910002, '李先生', 12, '上海·徐汇·漕河泾', 'QA：六年级英语自然拼读和口语薄弱，希望线上固定辅导。', 220.00, 1, NOW(3), NOW(3)),
(920003, 910003, '陈同学', 17, '广州·天河·珠江新城', 'QA：高二物理电磁学补基础，已进入聊天解锁场景。', 320.00, 1, NOW(3), NOW(3)),
(920004, 910004, '启航教育', NULL, '北京·朝阳·望京', 'QA：机构发布需求与机构身份流程验证。', NULL, 1, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
real_name=VALUES(real_name),
age=VALUES(age),
address=VALUES(address),
demand_description=VALUES(demand_description),
budget=VALUES(budget),
status=VALUES(status),
update_time=VALUES(update_time);

INSERT INTO `organization_profile`
(id, user_id, org_name, intro, contact_name, contact_phone, address, license_no, split_platform_percent, split_org_percent, status)
VALUES
(925001, 910004, 'QA启航教育', 'QA 测试机构：用于机构需求发布、机构资料、后台审核与机构单搜索。', 'QA机构运营', '18611720004', '北京·朝阳·望京', 'QA91110105MA11720004', 50, 50, 1)
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

INSERT INTO `organization_account`
(org_user_id, username, password_hash, must_change_password, status)
VALUES
(910004, 'qa_qihang_org', '$2y$10$LbNkUflDfYhPkKJyVJJsv.Hddizb59IKgpP3dlYYtvKA8BPQp7HTC', 1, 1)
ON DUPLICATE KEY UPDATE
password_hash=VALUES(password_hash),
must_change_password=VALUES(must_change_password),
status=VALUES(status),
update_time=NOW(3);

INSERT INTO `teacher_profile`
(id, user_id, real_name, education, subject, experience_years, rate_per_hour, introduction, city, highest_edu_school, teaching_mode, resume_completed, certificate_urls, basic_completed, realname_verify_status, realname_verify_method, realname_verify_idno_masked, realname_verify_reject_reason, edu_verify_status, edu_verify_reject_reason, home_star_teacher, status, create_time, update_time)
VALUES
(930001, 910101, 'QA赵明远', 'C985', '初中数学,高中数学', 8, 300.00, 'QA 数学老师：用于需求申请、信息费待支付、聊天解锁和课节支付链路。', '北京', '北京大学', 'BOTH', 1, JSON_ARRAY('/qa/cert/math-degree.png'), 1, 2, 'NAME_IDNO', '110***********001X', NULL, 2, NULL, 1, 1, NOW(3), NOW(3)),
(930002, 910102, 'QA周若晴', 'OVERSEAS', '小学英语,初中英语', 6, 260.00, 'QA 英语老师：用于线上需求、收藏、待处理申请和课程试听链路。', '上海', '香港中文大学', 'ONLINE', 1, JSON_ARRAY('/qa/cert/english-degree.png'), 1, 2, 'NAME_IDNO', '310***********002X', NULL, 2, NULL, 1, 1, NOW(3), NOW(3)),
(930003, 910103, 'QA孙启航', 'C211', '初中物理,高中物理', 7, 320.00, 'QA 物理老师：实名认证/学历认证审核中，用于后台审核列表。', '广州', '华南理工大学', 'BOTH', 1, JSON_ARRAY('/qa/cert/physics-degree.png'), 1, 1, 'ID_PHOTO', '440***********003X', NULL, 1, NULL, 0, 1, NOW(3), NOW(3)),
(930004, 910104, 'QA吴可欣', 'BACHELOR', '初中化学,高中化学', 5, 240.00, 'QA 化学老师：认证驳回，用于驳回态展示与重新提交。', '杭州', '浙江师范大学', 'ONLINE', 1, JSON_ARRAY('/qa/cert/chemistry-degree.png'), 1, 3, 'ID_PHOTO', '330***********004X', '身份证照片不清晰', 3, '学历证明缺少学校章', 0, 1, NOW(3), NOW(3))
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
basic_completed=VALUES(basic_completed),
realname_verify_status=VALUES(realname_verify_status),
realname_verify_method=VALUES(realname_verify_method),
realname_verify_idno_masked=VALUES(realname_verify_idno_masked),
realname_verify_reject_reason=VALUES(realname_verify_reject_reason),
edu_verify_status=VALUES(edu_verify_status),
edu_verify_reject_reason=VALUES(edu_verify_reject_reason),
home_star_teacher=VALUES(home_star_teacher),
status=VALUES(status),
update_time=VALUES(update_time);

INSERT INTO `teacher_job_posting`
(id, tutor_id, subject_id, title, description, price_per_hour, mode, city, available_time, max_students, status, create_time, update_time)
VALUES
(950001, 910101, 201, 'QA北京初中数学函数几何提分', 'QA：诊断小测、函数几何专题、错题复盘和阶段测评。', 300.00, 'both', '北京', JSON_ARRAY('Tue 19-21','Sat 10-12'), 1, 1, NOW(), NOW()),
(950002, 910102, 103, 'QA线上小学英语自然拼读口语', 'QA：自然拼读、分级阅读、口语跟读和课后打卡。', 240.00, 'online', '全国', JSON_ARRAY('Wed 19-20','Sun 10-11'), 1, 1, NOW(), NOW()),
(950003, 910103, 303, 'QA广州高中物理电磁学专题', 'QA：电磁学模型化讲解，适合试听、直播课堂和 AI 课后报告验证。', 320.00, 'both', '广州', JSON_ARRAY('Thu 20-22','Sat 14-16'), 1, 1, NOW(), NOW()),
(950004, 910104, 204, 'QA杭州初中化学预习课（下架）', 'QA：下架服务，用于验证下架数据不应出现在前台列表。', 220.00, 'online', '全国', JSON_ARRAY('Fri 19-21'), 1, 0, NOW(), NOW())
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
(id, parent_id, subject_id, subject_name, student_gender, grade_code, available_time, teacher_gender_preference, teacher_requirement_detail, title, description, child_age, class_mode, city, address, frequency_per_week, budget_min, budget_max, stage_code, education_requirement, publisher_identity, schedule, biz_status, reject_reason, status, create_time, update_time)
VALUES
(940001, 910001, 201, '初中数学', 'male', 'JUNIOR2', '周二/周四 19:00 后，周六上午', 'both', 'QA：希望老师有初中数学提分经验，课后提供错题复盘。', 'QA北京初二数学函数几何提分', 'QA 主流程需求：老师浏览、收藏、申请，学生接受后生成信息费订单。', 14, 'both', '北京', '海淀区五道口附近，可线上/线下', 2, 220.00, 320.00, 'JUNIOR', 'C985', 'PARENT', JSON_ARRAY('Tue 19-21','Thu 19-21','Sat 10-12'), 1, NULL, 1, NOW(3), NOW(3)),
(940002, 910002, 103, '小学英语', 'female', 'GRADE6', '周三晚、周日上午', 'female', 'QA：希望老师擅长自然拼读和口语陪练。', 'QA上海六年级英语自然拼读口语', 'QA 待支付链路需求：已有 accepted application 和 pending brokerage order。', 12, 'online', '全国', NULL, 2, 160.00, 260.00, 'PRIMARY', 'BACHELOR', 'PARENT', JSON_ARRAY('Wed 19-20','Sun 10-11'), 2, NULL, 1, NOW(3), NOW(3)),
(940003, 910003, 303, '高中物理', 'male', 'SENIOR2', '周四晚、周六下午', 'both', 'QA：希望老师把电磁学体系讲清楚，并安排试听课。', 'QA广州高二物理电磁学补基础', 'QA 已解锁聊天、课程、直播、AI 报告链路需求。', 17, 'online', '全国', NULL, 2, 260.00, 380.00, 'SENIOR', 'C211', 'STUDENT_SELF', JSON_ARRAY('Thu 20-22','Sat 14-16'), 4, NULL, 1, NOW(3), NOW(3)),
(940004, 910004, 201, '初中数学', 'female', 'JUNIOR3', '周末白天', 'both', 'QA：机构单，要求老师可配合机构教研反馈。', 'QA机构单｜初三数学冲刺老师招募', 'QA 机构发布需求，用于机构身份、机构单搜索和后台审核。', 15, 'offline', '北京', '朝阳区望京附近', 3, 240.00, 360.00, 'JUNIOR', 'DOUBLE_FIRST_CLASS', 'ORGANIZATION', JSON_ARRAY('Sat 09-12','Sun 09-12'), 1, NULL, 1, NOW(3), NOW(3)),
(940005, 910001, 302, '高中英语', 'female', 'SENIOR1', '周日晚', 'both', 'QA：待后台审核的需求。', 'QA待审核需求｜高一英语写作精批', 'QA 待审核状态，用于后台需求审核列表。', 16, 'online', '全国', NULL, 1, 180.00, 280.00, 'SENIOR', 'BACHELOR', 'PARENT', JSON_ARRAY('Sun 19-21'), 1, NULL, 0, NOW(3), NOW(3)),
(940006, 910002, 204, '初中化学', 'male', 'JUNIOR1', '周六上午', 'both', 'QA：已拒绝需求。', 'QA已拒绝需求｜初中化学提前预习', 'QA 后台审核拒绝态，用于前后台状态展示。', 13, 'online', '全国', NULL, 1, 120.00, 200.00, 'JUNIOR', 'BACHELOR', 'PARENT', JSON_ARRAY('Sat 10-12'), 1, '需求描述包含无效联系方式', 2, NOW(3), NOW(3)),
(940007, 910001, 101, '小学数学', 'male', 'GRADE5', '周一晚', 'both', 'QA：已关闭需求。', 'QA已关闭需求｜小学数学奥数启蒙', 'QA 关闭态需求，用于验证关闭后不可继续申请。', 11, 'offline', '北京', '海淀区', 1, 160.00, 240.00, 'PRIMARY', 'BACHELOR', 'PARENT', JSON_ARRAY('Mon 19-21'), 6, NULL, 3, NOW(3), NOW(3))
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
biz_status=VALUES(biz_status),
reject_reason=VALUES(reject_reason),
status=VALUES(status),
update_time=VALUES(update_time);

INSERT IGNORE INTO `tutor_favorite_demand` (tutor_id, demand_id, create_time)
VALUES
(910101, 940001, NOW()),
(910102, 940002, NOW()),
(910103, 940003, NOW());

INSERT IGNORE INTO `parent_favorite_tutor` (parent_id, tutor_id, create_time)
VALUES
(910001, 910101, NOW()),
(910002, 910102, NOW()),
(910003, 910103, NOW());

INSERT INTO `tutor_application`
(id, sender_uid, receiver_uid, sender_role, receiver_role, context_type, context_id, teaching_mode, content, client_request_id, status, chat_access_status, room_id, decided_at, receiver_read, receiver_read_time, create_time, update_time)
VALUES
(960001, 910101, 910001, 'TEACHER', 'STUDENT', 'DEMAND', 940001, 'OFFLINE', 'QA待处理申请：我擅长初中数学函数与几何，可以先做诊断小测。', 'qa-app-960001', 'PENDING', 'NONE', NULL, NULL, 0, NULL, DATE_SUB(NOW(3), INTERVAL 2 HOUR), DATE_SUB(NOW(3), INTERVAL 2 HOUR)),
(960002, 910102, 910002, 'TEACHER', 'STUDENT', 'DEMAND', 940002, 'ONLINE', 'QA已接受待支付申请：可按自然拼读和口语陪练方案推进。', 'qa-app-960002', 'ACCEPTED', 'PAYMENT_REQUIRED', NULL, DATE_SUB(NOW(3), INTERVAL 1 DAY), 1, DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 2 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY)),
(960003, 910103, 910003, 'TEACHER', 'STUDENT', 'DEMAND', 940003, 'ONLINE', 'QA已解锁申请：电磁学专题适合先试听再进入正式课。', 'qa-app-960003', 'ACCEPTED', 'CHAT_ENABLED', 970001, DATE_SUB(NOW(3), INTERVAL 5 DAY), 1, DATE_SUB(NOW(3), INTERVAL 5 DAY), DATE_SUB(NOW(3), INTERVAL 6 DAY), DATE_SUB(NOW(3), INTERVAL 5 DAY)),
(960004, 910104, 910001, 'TEACHER', 'STUDENT', 'DEMAND', 940001, 'ONLINE', 'QA已拒绝申请：用于拒绝态列表验证。', 'qa-app-960004', 'REJECTED', 'NONE', NULL, DATE_SUB(NOW(3), INTERVAL 1 DAY), 1, DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 2 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY))
ON DUPLICATE KEY UPDATE
teaching_mode=VALUES(teaching_mode),
content=VALUES(content),
status=VALUES(status),
chat_access_status=VALUES(chat_access_status),
room_id=VALUES(room_id),
decided_at=VALUES(decided_at),
receiver_read=VALUES(receiver_read),
receiver_read_time=VALUES(receiver_read_time),
update_time=VALUES(update_time);

INSERT INTO `brokerage_order`
(id, proposal_id, application_id, room_id, payer_uid, amount_fen, original_amount_fen, discount_amount_fen, promotion_type, promotion_snapshot_json, pay_method, status, proof_url, proof_note, paid_at, refund_locked, refunded_amount_fen, create_time, update_time)
VALUES
(980001, NULL, 960002, NULL, 910102, 19900, 19900, 0, NULL, NULL, NULL, 'PENDING', NULL, NULL, NULL, 0, 0, DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY)),
(980002, NULL, 960003, 970001, 910103, 19900, 19900, 0, NULL, NULL, 'WECHAT', 'PAID', '/qa/pay/proof-980002.png', 'QA paid proof', DATE_SUB(NOW(3), INTERVAL 5 DAY), 0, 0, DATE_SUB(NOW(3), INTERVAL 6 DAY), DATE_SUB(NOW(3), INTERVAL 5 DAY)),
(980003, NULL, NULL, 970001, 910103, 19900, 19900, 0, NULL, NULL, 'WECHAT', 'REFUND_REVIEW', '/qa/pay/proof-980003.png', 'QA refund review', DATE_SUB(NOW(3), INTERVAL 4 DAY), 1, 0, DATE_SUB(NOW(3), INTERVAL 4 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY)),
(980004, NULL, NULL, 970001, 910103, 19900, 19900, 0, NULL, NULL, 'WECHAT', 'REFUNDED', '/qa/pay/proof-980004.png', 'QA refunded', DATE_SUB(NOW(3), INTERVAL 8 DAY), 1, 19900, DATE_SUB(NOW(3), INTERVAL 8 DAY), DATE_SUB(NOW(3), INTERVAL 7 DAY))
ON DUPLICATE KEY UPDATE
application_id=VALUES(application_id),
room_id=VALUES(room_id),
pay_method=VALUES(pay_method),
status=VALUES(status),
proof_url=VALUES(proof_url),
proof_note=VALUES(proof_note),
paid_at=VALUES(paid_at),
refund_locked=VALUES(refund_locked),
refunded_amount_fen=VALUES(refunded_amount_fen),
update_time=VALUES(update_time);

INSERT INTO `application_brokerage_order`
(id, application_id, order_id, create_time, update_time)
VALUES
(986001, 960002, 980001, DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY)),
(986002, 960003, 980002, DATE_SUB(NOW(3), INTERVAL 6 DAY), DATE_SUB(NOW(3), INTERVAL 5 DAY))
ON DUPLICATE KEY UPDATE
order_id=VALUES(order_id),
update_time=VALUES(update_time);

INSERT INTO `payment_order`
(id, order_no, user_id, amount, currency, channel, provider, status, transaction_id, provider_order_no, context_id, context_type, subject, body, client_ip, extra_params, pay_data, notify_count, last_notify_time, notify_verified, event_sent, event_sent_time, event_send_fail_reason, success_time, expire_time, create_time, update_time)
VALUES
(981001, 'QA_PAY_981001_PENDING', 910102, 19900, 'CNY', 'WECHAT', 'YUNGOUOS', 'PENDING', NULL, NULL, 980001, 'BROKERAGE_ORDER', 'QA信息费待支付订单', 'QA accepted application pending payment', '117.72.111.39', JSON_OBJECT('qa', true), JSON_OBJECT('mockQr', 'qa://pay/981001'), 0, NULL, 0, 0, NULL, NULL, NULL, DATE_ADD(NOW(3), INTERVAL 2 HOUR), DATE_SUB(NOW(3), INTERVAL 1 HOUR), NOW(3)),
(981002, 'QA_PAY_981002_SUCCESS', 910103, 19900, 'CNY', 'WECHAT', 'YUNGOUOS', 'SUCCESS', 'QA_TX_981002', 'QA_PROVIDER_981002', 980002, 'BROKERAGE_ORDER', 'QA信息费支付成功订单', 'QA paid application chat enabled', '117.72.111.39', JSON_OBJECT('qa', true), JSON_OBJECT('mockQr', 'qa://pay/981002'), 2, DATE_SUB(NOW(3), INTERVAL 5 DAY), 1, 1, DATE_SUB(NOW(3), INTERVAL 5 DAY), NULL, DATE_SUB(NOW(3), INTERVAL 5 DAY), DATE_ADD(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 6 DAY), DATE_SUB(NOW(3), INTERVAL 5 DAY)),
(981003, 'QA_PAY_981003_FAILED', 910101, 19900, 'CNY', 'WECHAT', 'YUNGOUOS', 'FAILED', NULL, NULL, 980003, 'BROKERAGE_ORDER', 'QA信息费失败订单', 'QA failed payment for negative branch', '117.72.111.39', JSON_OBJECT('qa', true), NULL, 1, DATE_SUB(NOW(3), INTERVAL 1 DAY), 0, 0, NULL, 'SIGN_VERIFY_FAILED', NULL, DATE_SUB(NOW(3), INTERVAL 1 HOUR), DATE_SUB(NOW(3), INTERVAL 2 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY))
ON DUPLICATE KEY UPDATE
user_id=VALUES(user_id),
amount=VALUES(amount),
status=VALUES(status),
transaction_id=VALUES(transaction_id),
provider_order_no=VALUES(provider_order_no),
context_id=VALUES(context_id),
context_type=VALUES(context_type),
subject=VALUES(subject),
body=VALUES(body),
extra_params=VALUES(extra_params),
pay_data=VALUES(pay_data),
notify_count=VALUES(notify_count),
last_notify_time=VALUES(last_notify_time),
notify_verified=VALUES(notify_verified),
event_sent=VALUES(event_sent),
event_sent_time=VALUES(event_sent_time),
event_send_fail_reason=VALUES(event_send_fail_reason),
success_time=VALUES(success_time),
expire_time=VALUES(expire_time),
update_time=VALUES(update_time);

INSERT INTO `room`
(id, teacher_profile_id, student_profile_id, active_time, last_msg_id, status, create_time, update_time)
VALUES
(970001, 930003, 920003, NOW(3), 971004, 1, DATE_SUB(NOW(3), INTERVAL 5 DAY), NOW(3)),
(970002, 930001, 920001, DATE_SUB(NOW(3), INTERVAL 2 DAY), 971006, 0, DATE_SUB(NOW(3), INTERVAL 3 DAY), DATE_SUB(NOW(3), INTERVAL 2 DAY))
ON DUPLICATE KEY UPDATE
active_time=VALUES(active_time),
last_msg_id=VALUES(last_msg_id),
status=VALUES(status),
update_time=VALUES(update_time);

INSERT INTO `message`
(id, room_id, from_uid, to_uid, content, is_masked, reply_msg_id, status, gap_count, type, extra, create_time, update_time)
VALUES
(971001, 970001, 910003, 910103, 'QA：老师您好，我主要想补电磁感应和磁场这块。', 0, NULL, 0, NULL, 1, NULL, DATE_SUB(NOW(3), INTERVAL 5 DAY), DATE_SUB(NOW(3), INTERVAL 5 DAY)),
(971002, 970001, 910103, 910003, 'QA：可以，我们先做一次诊断，然后按模型专题推进。', 0, NULL, 0, NULL, 1, NULL, DATE_SUB(NOW(3), INTERVAL 5 DAY), DATE_SUB(NOW(3), INTERVAL 5 DAY)),
(971003, 970001, 910003, 910103, 'QA：周六下午可以先试听吗？', 0, NULL, 0, NULL, 1, NULL, DATE_SUB(NOW(3), INTERVAL 4 DAY), DATE_SUB(NOW(3), INTERVAL 4 DAY)),
(971004, 970001, 910103, 910003, 'QA：可以，我会带一套电磁学诊断题。', 0, NULL, 0, NULL, 1, JSON_OBJECT('qa', true), DATE_SUB(NOW(3), INTERVAL 4 DAY), DATE_SUB(NOW(3), INTERVAL 4 DAY)),
(971005, 970002, 910001, 910101, 'QA：关闭房间历史消息。', 0, NULL, 0, NULL, 1, NULL, DATE_SUB(NOW(3), INTERVAL 2 DAY), DATE_SUB(NOW(3), INTERVAL 2 DAY)),
(971006, 970002, 910101, 910001, 'QA：该房间已关闭，用于验证不可继续发消息。', 0, NULL, 0, NULL, 1, NULL, DATE_SUB(NOW(3), INTERVAL 2 DAY), DATE_SUB(NOW(3), INTERVAL 2 DAY))
ON DUPLICATE KEY UPDATE
content=VALUES(content),
is_masked=VALUES(is_masked),
status=VALUES(status),
type=VALUES(type),
extra=VALUES(extra),
update_time=VALUES(update_time);

INSERT INTO `room_read_state`
(id, room_id, uid, last_read_msg_id, last_read_time, create_time, update_time)
VALUES
(972001, 970001, 910003, 971004, DATE_SUB(NOW(3), INTERVAL 4 DAY), NOW(3), NOW(3)),
(972002, 970001, 910103, 971003, DATE_SUB(NOW(3), INTERVAL 4 DAY), NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
last_read_msg_id=VALUES(last_read_msg_id),
last_read_time=VALUES(last_read_time),
update_time=VALUES(update_time);

INSERT INTO `chat_realtime_event`
(event_id, target_uid, event_type, biz_type, room_id, msg_id, occurred_at, payload_json, create_time)
VALUES
(973001, 910103, 'MESSAGE_CREATED', 'CHAT', 970001, 971001, DATE_SUB(NOW(3), INTERVAL 5 DAY), JSON_OBJECT('qa', true, 'msgId', 971001), NOW(3)),
(973002, 910003, 'MESSAGE_CREATED', 'CHAT', 970001, 971002, DATE_SUB(NOW(3), INTERVAL 5 DAY), JSON_OBJECT('qa', true, 'msgId', 971002), NOW(3))
ON DUPLICATE KEY UPDATE
target_uid=VALUES(target_uid),
event_type=VALUES(event_type),
biz_type=VALUES(biz_type),
room_id=VALUES(room_id),
msg_id=VALUES(msg_id),
occurred_at=VALUES(occurred_at),
payload_json=VALUES(payload_json);

INSERT INTO `collaboration_proposal`
(id, room_id, from_uid, to_uid, price_per_hour, class_time, frequency_per_week, trial_start_at, trial_end_at, remark, expire_at, client_request_id, status, actor_uid, action_time, create_time, update_time)
VALUES
(974001, 970001, 910103, 910003, '320元/小时', '每周六 14:00-16:00', 1, DATE_ADD(NOW(3), INTERVAL 2 DAY), DATE_ADD(NOW(3), INTERVAL 9 DAY), 'QA试听合作提案，待学生接受。', DATE_ADD(NOW(3), INTERVAL 1 DAY), 'qa-proposal-974001', 'PENDING', NULL, NULL, NOW(3), NOW(3)),
(974002, 970001, 910003, 910103, '300元/小时', '每周四 20:00-22:00', 1, DATE_SUB(NOW(3), INTERVAL 5 DAY), DATE_ADD(NOW(3), INTERVAL 2 DAY), 'QA已接受合作提案。', DATE_ADD(NOW(3), INTERVAL 1 DAY), 'qa-proposal-974002', 'ACCEPTED', 910103, DATE_SUB(NOW(3), INTERVAL 5 DAY), DATE_SUB(NOW(3), INTERVAL 6 DAY), DATE_SUB(NOW(3), INTERVAL 5 DAY))
ON DUPLICATE KEY UPDATE
price_per_hour=VALUES(price_per_hour),
class_time=VALUES(class_time),
frequency_per_week=VALUES(frequency_per_week),
trial_start_at=VALUES(trial_start_at),
trial_end_at=VALUES(trial_end_at),
remark=VALUES(remark),
expire_at=VALUES(expire_at),
status=VALUES(status),
actor_uid=VALUES(actor_uid),
action_time=VALUES(action_time),
update_time=VALUES(update_time);

INSERT INTO `course_enrollment`
(id, application_id, room_id, proposal_id, teacher_uid, student_uid, teaching_mode, course_name, class_time, frequency_per_week, lesson_price, status, trial_start_at, trial_end_at, weekly_schedule_deadline_at, weekly_schedule_submitted_at, create_time, update_time)
VALUES
(982001, 960003, 970001, 974002, 910103, 910003, 'ONLINE', 'QA高二物理电磁学试听课', '每周四 20:00-22:00', 1, '300元/小时', 'TRIALING', DATE_SUB(NOW(3), INTERVAL 5 DAY), DATE_ADD(NOW(3), INTERVAL 2 DAY), DATE_ADD(NOW(3), INTERVAL 3 DAY), NULL, DATE_SUB(NOW(3), INTERVAL 5 DAY), NOW(3)),
(982002, 960002, NULL, NULL, 910102, 910002, 'ONLINE', 'QA小学英语自然拼读课程', NULL, NULL, '240元/小时', 'WAIT_PAY', NULL, NULL, NULL, NULL, DATE_SUB(NOW(3), INTERVAL 1 DAY), NOW(3))
ON DUPLICATE KEY UPDATE
room_id=VALUES(room_id),
proposal_id=VALUES(proposal_id),
teacher_uid=VALUES(teacher_uid),
student_uid=VALUES(student_uid),
teaching_mode=VALUES(teaching_mode),
course_name=VALUES(course_name),
class_time=VALUES(class_time),
frequency_per_week=VALUES(frequency_per_week),
lesson_price=VALUES(lesson_price),
status=VALUES(status),
trial_start_at=VALUES(trial_start_at),
trial_end_at=VALUES(trial_end_at),
weekly_schedule_deadline_at=VALUES(weekly_schedule_deadline_at),
weekly_schedule_submitted_at=VALUES(weekly_schedule_submitted_at),
update_time=VALUES(update_time);

INSERT INTO `tutor_appointment`
(id, course_id, parent_id, tutor_id, parent_job_posting_id, tutor_job_posting_id, title, lesson_type, lesson_price_fen, trial_price_percent, payable_amount_fen, subject_id, class_mode, city, address, start_time, duration_minutes, status, created_by, room_id, proposed_start_time, proposed_by, cancel_by, remark, create_time, update_time)
VALUES
(983001, 982001, 910003, 910103, 940003, 950003, 'QA高二物理电磁学试听课', 'TRIAL', 30000, 50, 15000, 303, 'online', '全国', NULL, DATE_ADD(NOW(3), INTERVAL 2 DAY), 60, 2, 910003, 970001, NULL, NULL, NULL, 'QA已确认试听课', DATE_SUB(NOW(3), INTERVAL 3 DAY), NOW(3)),
(983002, 982001, 910003, 910103, 940003, 950003, 'QA高二物理正式课-已完成', 'NORMAL', 30000, 50, 30000, 303, 'online', '全国', NULL, DATE_SUB(NOW(3), INTERVAL 1 DAY), 60, 5, 910103, 970001, NULL, NULL, NULL, 'QA已完成课节，用于总结/结算', DATE_SUB(NOW(3), INTERVAL 3 DAY), NOW(3)),
(983003, NULL, 910001, 910101, 940001, 950001, 'QA初中数学待确认预约', 'NORMAL', 30000, 50, 30000, 201, 'offline', '北京', '海淀区五道口', DATE_ADD(NOW(3), INTERVAL 3 DAY), 60, 1, 910001, NULL, NULL, NULL, NULL, 'QA待老师确认', NOW(3), NOW(3)),
(983004, NULL, 910002, 910102, 940002, 950002, 'QA英语已取消预约', 'NORMAL', 24000, 50, 24000, 103, 'online', '全国', NULL, DATE_ADD(NOW(3), INTERVAL 4 DAY), 60, 4, 910002, NULL, NULL, NULL, 910002, 'QA用户取消', DATE_SUB(NOW(3), INTERVAL 1 DAY), NOW(3))
ON DUPLICATE KEY UPDATE
course_id=VALUES(course_id),
title=VALUES(title),
lesson_type=VALUES(lesson_type),
lesson_price_fen=VALUES(lesson_price_fen),
payable_amount_fen=VALUES(payable_amount_fen),
status=VALUES(status),
start_time=VALUES(start_time),
room_id=VALUES(room_id),
remark=VALUES(remark),
update_time=VALUES(update_time);

INSERT INTO `lesson_payment_order`
(id, lesson_id, course_id, student_uid, teacher_uid, lesson_type, total_amount_fen, platform_fee_rate, platform_fee_amount_fen, teacher_income_amount_fen, status, payment_order_no, paid_at, create_time, update_time)
VALUES
(987001, 983001, 982001, 910003, 910103, 'TRIAL', 15000, 10, 1500, 13500, 'PENDING', NULL, NULL, NOW(3), NOW(3)),
(987002, 983002, 982001, 910003, 910103, 'NORMAL', 30000, 10, 3000, 27000, 'PAID', 'QA_LESSON_PAY_987002', DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 2 DAY), NOW(3))
ON DUPLICATE KEY UPDATE
course_id=VALUES(course_id),
student_uid=VALUES(student_uid),
teacher_uid=VALUES(teacher_uid),
status=VALUES(status),
payment_order_no=VALUES(payment_order_no),
paid_at=VALUES(paid_at),
update_time=VALUES(update_time);

INSERT INTO `teacher_settlement`
(id, lesson_payment_order_id, teacher_uid, settlement_amount_fen, platform_fee_amount_fen, status, create_time, update_time)
VALUES
(988001, 987002, 910103, 27000, 3000, 'SETTLEABLE', NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
teacher_uid=VALUES(teacher_uid),
settlement_amount_fen=VALUES(settlement_amount_fen),
platform_fee_amount_fen=VALUES(platform_fee_amount_fen),
status=VALUES(status),
update_time=VALUES(update_time);

INSERT INTO `refund_request`
(id, brokerage_order_id, course_id, room_id, applicant_uid, applicant_role, type, status, reason, evidence_images_json, evidence_video_url, evidence_video_duration_seconds, evidence_video_delete_status, refund_percent, refund_amount_fen, admin_uid, admin_note, decided_at, create_time, update_time)
VALUES
(985001, 980003, NULL, 970001, 910003, 'STUDENT', 'CHAT_INFO_FEE', 'PENDING', 'QA：聊天后发现老师时间不匹配，申请信息费退款。', JSON_ARRAY('/qa/refund/evidence-985001.png'), NULL, NULL, NULL, 100, 19900, NULL, NULL, NULL, DATE_SUB(NOW(3), INTERVAL 1 DAY), NOW(3)),
(985002, 980004, 982001, 970001, 910003, 'STUDENT', 'TRIAL_INFO_FEE', 'APPROVED', 'QA：试听不通过，申请 80% 退款。', JSON_ARRAY('/qa/refund/evidence-985002.png'), '/qa/refund/video-985002.mp4', 45, 'PENDING_DELETE', 80, 15920, 1, 'QA审核通过', DATE_SUB(NOW(3), INTERVAL 7 DAY), DATE_SUB(NOW(3), INTERVAL 8 DAY), NOW(3))
ON DUPLICATE KEY UPDATE
course_id=VALUES(course_id),
room_id=VALUES(room_id),
applicant_uid=VALUES(applicant_uid),
applicant_role=VALUES(applicant_role),
type=VALUES(type),
status=VALUES(status),
reason=VALUES(reason),
evidence_images_json=VALUES(evidence_images_json),
evidence_video_url=VALUES(evidence_video_url),
evidence_video_duration_seconds=VALUES(evidence_video_duration_seconds),
evidence_video_delete_status=VALUES(evidence_video_delete_status),
refund_percent=VALUES(refund_percent),
refund_amount_fen=VALUES(refund_amount_fen),
admin_uid=VALUES(admin_uid),
admin_note=VALUES(admin_note),
decided_at=VALUES(decided_at),
update_time=VALUES(update_time);

INSERT INTO `payment_refund`
(id, refund_no, payment_order_no, provider, provider_refund_no, refund_amount_fen, status, request_id, fail_reason, create_time, update_time)
VALUES
(985101, 'QA_REFUND_985101_PENDING', 'QA_PAY_981002_SUCCESS', 'YUNGOUOS', NULL, 19900, 'PENDING', 985001, NULL, NOW(3), NOW(3)),
(985102, 'QA_REFUND_985102_SUCCESS', 'QA_PAY_981002_SUCCESS', 'YUNGOUOS', 'QA_PROVIDER_REFUND_985102', 15920, 'SUCCESS', 985002, NULL, DATE_SUB(NOW(3), INTERVAL 7 DAY), NOW(3))
ON DUPLICATE KEY UPDATE
payment_order_no=VALUES(payment_order_no),
provider_refund_no=VALUES(provider_refund_no),
refund_amount_fen=VALUES(refund_amount_fen),
status=VALUES(status),
request_id=VALUES(request_id),
fail_reason=VALUES(fail_reason),
update_time=VALUES(update_time);

INSERT INTO `live_class_session`
(id, course_id, schedule_event_id, room_id, provider, provider_room_name, teacher_uid, student_uid, status, join_open_at, scheduled_start_at, scheduled_end_at, actual_start_at, actual_end_at, host_joined_at, peer_joined_at, ended_by_uid, end_reason, record_policy, ai_policy, extra_json, version, create_time, update_time)
VALUES
(984001, 982001, 983001, 970001, 'LIVEKIT', 'qa-live-984001-upcoming', 910103, 910003, 'CREATED', DATE_ADD(NOW(3), INTERVAL 1 DAY), DATE_ADD(NOW(3), INTERVAL 2 DAY), DATE_ADD(DATE_ADD(NOW(3), INTERVAL 2 DAY), INTERVAL 1 HOUR), NULL, NULL, NULL, NULL, NULL, NULL, 'OFF', 'SUMMARY', JSON_OBJECT('qa', true), 0, NOW(3), NOW(3)),
(984002, 982001, 983002, 970001, 'LIVEKIT', 'qa-live-984002-ended', 910103, 910003, 'ENDED', DATE_SUB(NOW(3), INTERVAL 2 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 23 HOUR), DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 23 HOUR), DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY), 910103, 'NORMAL_END', 'OFF', 'SUMMARY', JSON_OBJECT('qa', true), 1, DATE_SUB(NOW(3), INTERVAL 2 DAY), NOW(3))
ON DUPLICATE KEY UPDATE
schedule_event_id=VALUES(schedule_event_id),
room_id=VALUES(room_id),
provider_room_name=VALUES(provider_room_name),
teacher_uid=VALUES(teacher_uid),
student_uid=VALUES(student_uid),
status=VALUES(status),
join_open_at=VALUES(join_open_at),
scheduled_start_at=VALUES(scheduled_start_at),
scheduled_end_at=VALUES(scheduled_end_at),
actual_start_at=VALUES(actual_start_at),
actual_end_at=VALUES(actual_end_at),
host_joined_at=VALUES(host_joined_at),
peer_joined_at=VALUES(peer_joined_at),
ended_by_uid=VALUES(ended_by_uid),
end_reason=VALUES(end_reason),
record_policy=VALUES(record_policy),
ai_policy=VALUES(ai_policy),
extra_json=VALUES(extra_json),
version=VALUES(version),
update_time=VALUES(update_time);

INSERT INTO `live_class_participant`
(id, session_id, uid, role, identity_type, join_count, first_join_at, last_join_at, last_leave_at, online_status, camera_enabled, mic_enabled, device_info_json, network_score, create_time, update_time)
VALUES
(984101, 984002, 910103, 'TEACHER', 'HUMAN', 1, DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 23 HOUR), 'LEFT', 1, 1, JSON_OBJECT('browser', 'Chrome', 'qa', true), 92, NOW(3), NOW(3)),
(984102, 984002, 910003, 'STUDENT', 'HUMAN', 1, DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 23 HOUR), 'LEFT', 1, 1, JSON_OBJECT('browser', 'Chrome', 'qa', true), 88, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
join_count=VALUES(join_count),
first_join_at=VALUES(first_join_at),
last_join_at=VALUES(last_join_at),
last_leave_at=VALUES(last_leave_at),
online_status=VALUES(online_status),
camera_enabled=VALUES(camera_enabled),
mic_enabled=VALUES(mic_enabled),
device_info_json=VALUES(device_info_json),
network_score=VALUES(network_score),
update_time=VALUES(update_time);

INSERT INTO `live_class_event`
(id, session_id, event_type, event_source, operator_uid, payload_json, occurred_at, create_time)
VALUES
(984201, 984002, 'ROOM_STARTED', 'APP', 910103, JSON_OBJECT('qa', true), DATE_SUB(NOW(3), INTERVAL 1 DAY), NOW(3)),
(984202, 984002, 'PARTICIPANT_JOINED', 'LIVEKIT', 910003, JSON_OBJECT('qa', true, 'role', 'STUDENT'), DATE_SUB(NOW(3), INTERVAL 1 DAY), NOW(3)),
(984203, 984002, 'ROOM_ENDED', 'APP', 910103, JSON_OBJECT('qa', true, 'reason', 'NORMAL_END'), DATE_SUB(NOW(3), INTERVAL 23 HOUR), NOW(3))
ON DUPLICATE KEY UPDATE
event_type=VALUES(event_type),
event_source=VALUES(event_source),
operator_uid=VALUES(operator_uid),
payload_json=VALUES(payload_json),
occurred_at=VALUES(occurred_at);

INSERT INTO `ai_task`
(id, task_id, task_type, biz_id, status, progress, message, input_json, output_json, error_message, created_at, updated_at)
VALUES
(989001, 'qa-ai-task-989001', 'LESSON_REPORT', '983002', 'SUCCESS', 100, 'QA lesson report ready', JSON_OBJECT('lessonId', 983002), JSON_OBJECT('summary', 'QA：本节课完成电磁学基础诊断。'), NULL, NOW(), NOW()),
(989002, 'qa-ai-task-989002', 'CHAT_SUMMARY', '970001', 'RUNNING', 60, 'QA chat summary running', JSON_OBJECT('roomId', 970001), NULL, NULL, NOW(), NOW()),
(989003, 'qa-ai-task-989003', 'LESSON_REPORT', '983001', 'FAILED', 100, 'QA lesson report failed', JSON_OBJECT('lessonId', 983001), NULL, 'QA simulated model timeout', NOW(), NOW())
ON DUPLICATE KEY UPDATE
task_type=VALUES(task_type),
biz_id=VALUES(biz_id),
status=VALUES(status),
progress=VALUES(progress),
message=VALUES(message),
input_json=VALUES(input_json),
output_json=VALUES(output_json),
error_message=VALUES(error_message),
updated_at=VALUES(updated_at);

INSERT INTO `ai_lesson_report`
(id, lesson_id, task_id, teacher_id, student_id, status, report_json, teacher_edited_json, created_at, updated_at)
VALUES
(989101, 983002, 'qa-ai-task-989001', 910103, 910003, 'READY', JSON_OBJECT('brief', 'QA课后报告：电磁学诊断完成'), NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE
task_id=VALUES(task_id),
teacher_id=VALUES(teacher_id),
student_id=VALUES(student_id),
status=VALUES(status),
report_json=VALUES(report_json),
teacher_edited_json=VALUES(teacher_edited_json),
updated_at=VALUES(updated_at);

INSERT INTO `ai_chat_summary`
(id, room_id, task_id, summary_json, message_start_id, message_end_id, created_at, updated_at)
VALUES
(989201, 970001, 'qa-ai-task-989002', JSON_OBJECT('brief', 'QA聊天摘要：学生咨询电磁学试听安排。'), 971001, 971004, NOW(), NOW())
ON DUPLICATE KEY UPDATE
task_id=VALUES(task_id),
summary_json=VALUES(summary_json),
message_start_id=VALUES(message_start_id),
message_end_id=VALUES(message_end_id),
updated_at=VALUES(updated_at);

INSERT INTO `lesson_summary`
(id, lesson_id, course_id, teacher_uid, student_uid, title, summary_status, summary_brief, summary_content, homework, ready_at, create_time, update_time)
VALUES
(989301, 983002, 982001, 910103, 910003, 'QA高二物理课后总结', 'READY', '本节课完成电磁感应基础诊断。', 'QA：学生对磁通量和楞次定律理解初步建立，需要继续训练方向判断。', '完成 10 道电磁感应基础题，并整理错题原因。', DATE_SUB(NOW(3), INTERVAL 20 HOUR), NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
course_id=VALUES(course_id),
teacher_uid=VALUES(teacher_uid),
student_uid=VALUES(student_uid),
title=VALUES(title),
summary_status=VALUES(summary_status),
summary_brief=VALUES(summary_brief),
summary_content=VALUES(summary_content),
homework=VALUES(homework),
ready_at=VALUES(ready_at),
update_time=VALUES(update_time);

INSERT INTO `user_email`
(id, user_id, email_type, email, email_masked, verify_status, verified_at, bind_source, bounce_status, last_notify_at, status, create_time, update_time)
VALUES
(989401, 910003, 'PRIMARY', 'qa-student-910003@example.com', 'qa-s******003@example.com', 'VERIFIED', NOW(3), 'COURSE_PAGE', 'NORMAL', DATE_SUB(NOW(3), INTERVAL 20 HOUR), 1, NOW(3), NOW(3)),
(989402, 910103, 'PRIMARY', 'qa-teacher-910103@example.com', 'qa-t******103@example.com', 'VERIFIED', NOW(3), 'MY_PAGE', 'NORMAL', NULL, 1, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
email=VALUES(email),
email_masked=VALUES(email_masked),
verify_status=VALUES(verify_status),
verified_at=VALUES(verified_at),
bind_source=VALUES(bind_source),
bounce_status=VALUES(bounce_status),
last_notify_at=VALUES(last_notify_at),
status=VALUES(status),
update_time=VALUES(update_time);

INSERT INTO `email_notification_task`
(id, task_key, template_code, biz_type, biz_id, receiver_uid, receiver_role, email_type, email, subject, payload_json, scheduled_at, status, retry_count, max_retry_count, last_error, sent_at, opened_at, clicked_at, create_time, update_time)
VALUES
(989501, 'qa-lesson-summary-983002-student', 'LESSON_SUMMARY', 'LESSON_SUMMARY', 983002, 910003, 'STUDENT', 'PRIMARY', 'qa-student-910003@example.com', 'QA课后总结已生成', JSON_OBJECT('lessonId', 983002), DATE_SUB(NOW(3), INTERVAL 20 HOUR), 'SENT', 0, 3, NULL, DATE_SUB(NOW(3), INTERVAL 20 HOUR), NULL, NULL, NOW(3), NOW(3)),
(989502, 'qa-unread-message-970001-teacher', 'UNREAD_MESSAGE', 'UNREAD_MESSAGE', 971004, 910103, 'TEACHER', 'PRIMARY', 'qa-teacher-910103@example.com', 'QA你有新的未读消息', JSON_OBJECT('roomId', 970001), NOW(3), 'PENDING', 0, 3, NULL, NULL, NULL, NULL, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
template_code=VALUES(template_code),
biz_type=VALUES(biz_type),
biz_id=VALUES(biz_id),
receiver_uid=VALUES(receiver_uid),
receiver_role=VALUES(receiver_role),
email_type=VALUES(email_type),
email=VALUES(email),
subject=VALUES(subject),
payload_json=VALUES(payload_json),
scheduled_at=VALUES(scheduled_at),
status=VALUES(status),
retry_count=VALUES(retry_count),
last_error=VALUES(last_error),
sent_at=VALUES(sent_at),
opened_at=VALUES(opened_at),
clicked_at=VALUES(clicked_at),
update_time=VALUES(update_time);

INSERT INTO `home_carousel_config`
(id, title, subtitle, image_object_key, link_type, link_url, sort_order, create_admin_id, update_admin_id, create_time, update_time)
VALUES
(990001, 'QA 首页 Banner - 数学提分', '用于验证首页轮播配置、排序和跳转', 'qa/banners/math-upgrade.png', 'ROUTE', '/demand/940001', 10, 1, 1, NOW(3), NOW(3)),
(990002, 'QA 首页 Banner - 直播课堂', '用于验证直播课堂入口配置', 'qa/banners/live-class.png', 'ROUTE', '/live/984001', 20, 1, 1, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
title=VALUES(title),
subtitle=VALUES(subtitle),
image_object_key=VALUES(image_object_key),
link_type=VALUES(link_type),
link_url=VALUES(link_url),
sort_order=VALUES(sort_order),
update_admin_id=VALUES(update_admin_id),
update_time=VALUES(update_time);

INSERT INTO `invite_code`
(id, user_id, invite_code, status, create_time, update_time)
VALUES
(991001, 910001, 'QA117P1', 'ACTIVE', NOW(3), NOW(3)),
(991002, 910101, 'QA117T1', 'ACTIVE', NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
invite_code=VALUES(invite_code),
status=VALUES(status),
update_time=VALUES(update_time);

INSERT INTO `invite_relation`
(id, inviter_uid, invitee_uid, invite_code, bind_source, status, risk_flag, remark, bind_time, create_time, update_time)
VALUES
(991101, 910001, 910102, 'QA117P1', 'REGISTER', 'ACTIVE', 0, 'QA邀请关系：家长邀请老师注册', DATE_SUB(NOW(3), INTERVAL 10 DAY), NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
inviter_uid=VALUES(inviter_uid),
invite_code=VALUES(invite_code),
bind_source=VALUES(bind_source),
status=VALUES(status),
risk_flag=VALUES(risk_flag),
remark=VALUES(remark),
update_time=VALUES(update_time);

INSERT INTO `invite_receiver_account`
(id, user_id, receiver_name, wechat_no, phone, remark, status, create_time, update_time)
VALUES
(991201, 910001, 'QA王女士', 'qa_parent_910001', '18611720001', 'QA返利收款账号', 'ACTIVE', NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
receiver_name=VALUES(receiver_name),
wechat_no=VALUES(wechat_no),
phone=VALUES(phone),
remark=VALUES(remark),
status=VALUES(status),
update_time=VALUES(update_time);

INSERT INTO `invite_reward_record`
(id, inviter_uid, invitee_uid, reward_scene, biz_order_type, biz_order_id, payment_order_id, base_amount_fen, reward_rate, reward_amount_fen, status, freeze_reason, settlement_month, config_snapshot_json, create_time, update_time)
VALUES
(991301, 910001, 910102, 'INFO_FEE_PAID', 'BROKERAGE_ORDER', 980002, 981002, 19900, 0.1300, 2587, 'SETTLEABLE', NULL, DATE_FORMAT(NOW(), '%Y-%m'), JSON_OBJECT('qa', true, 'rate', 0.13), NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
payment_order_id=VALUES(payment_order_id),
base_amount_fen=VALUES(base_amount_fen),
reward_rate=VALUES(reward_rate),
reward_amount_fen=VALUES(reward_amount_fen),
status=VALUES(status),
freeze_reason=VALUES(freeze_reason),
settlement_month=VALUES(settlement_month),
config_snapshot_json=VALUES(config_snapshot_json),
update_time=VALUES(update_time);

SET FOREIGN_KEY_CHECKS = 1;
