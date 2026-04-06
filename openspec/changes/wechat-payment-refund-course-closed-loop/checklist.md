---
title: 验收检查表（微信支付退款闭环 + 我的课程）
status: draft
date: 2026-04-04
---

# 数据库

- [ ] 迁移脚本可在空库与已有数据上执行成功（无破坏性失败）
- [ ] `refund_request` / `payment_refund` / `course_enrollment` 表存在且索引生效
- [ ] `brokerage_order` 状态扩展与代码枚举一致（无 DISPUTE/PAID 等不一致问题）

# 聊天退费闭环（100%）

- [ ] 教师支付信息费后进入聊天页，显示“申请退费”按钮且 hover 文案正确
- [ ] 点击“申请退费”后：立即关闭聊天（room 关闭且无法继续发消息）
- [ ] 产生 refund_request（type=CHAT_INFO_FEE，status=PENDING）且 brokerage_order.status=REFUND_REVIEW
- [ ] 管理端可在列表中看到该申请，并在详情中查看聊天记录
- [ ] 管理端审核通过后：payment_refund 记录生成，退款成功后 brokerage_order.status=REFUNDED、refund_request.status=APPROVED

# 合作达成与联系方式

- [ ] 任一方发起合作，另一方同意后状态变更为 ACCEPTED
- [ ] ACCEPTED 后双方都能看到对方联系方式（默认电话=注册手机号）
- [ ] ACCEPTED 后“申请退费”按钮禁用且 hover 文案为固定提示
- [ ] ACCEPTED 后聊天仍可继续发送消息（不会被后端拦截）

# 我的课程与试课退款（60%）

- [ ] 顶部导航可进入“我的课程”页面
- [ ] 课程按状态展示：申请中/待支付/沟通中/退费审批中/已退费/试课中/开课中/已结课/试课退费审批中
- [ ] 合作达成后课程进入试课中，试课截止时间=7 天后
- [ ] 教师在试课期内可发起“试课不通过”，必须填写说明且至少上传 1 张证据图
- [ ] 管理端可查看试课说明与证据图，并可审核通过/拒绝
- [ ] 审核通过后：按 60% 原路退款并落库；状态收敛到已退费

# 幂等与安全

- [ ] 重复点击申请退款不会产生重复申请（返回明确错误或幂等返回）
- [ ] 重复审批通过不会重复发起退款（payment_refund 幂等）
- [ ] 退款金额校验严格：不允许大于实付金额；试课退款固定 60%
- [ ] 服务间调用携带身份签名头（FeignIdentityRequestInterceptor 生效）

# 测试

- [ ] `payment-service` 单元/集成测试通过（覆盖全额/部分退款、失败分支、幂等）
- [ ] `videoCall-IM-service` 测试通过（退款申请关闭房间、合作后仍可聊天、试课期校验）
- [ ] `ai-tutor-admin` 测试通过（审核链路与状态机）

