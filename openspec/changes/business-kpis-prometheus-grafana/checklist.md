---
title: 验收检查表（业务 KPI Prometheus + Grafana）
status: draft
date: 2026-03-28
---

# 构建与回归

- [ ] `sh ./mvnw test` 通过
- [ ] 原有核心接口回归通过（登录/注册、申请沟通、支付回调、退款流程、验证码发送）

# 指标暴露

- [ ] 相关服务已启用 `/actuator/prometheus`（返回 200）
- [ ] `/actuator/prometheus` 输出中包含以下指标名（至少出现 HELP/TYPE 或样本）：
  - [ ] `ai_tutor_biz_user_register_total`
  - [ ] `ai_tutor_biz_comm_apply_total`
  - [ ] `ai_tutor_biz_comm_apply_decision_total`
  - [ ] `ai_tutor_biz_sms_code_send_total`
  - [ ] `ai_tutor_biz_payment_info_fee_amount_cents_total`
  - [ ] `ai_tutor_biz_collaboration_success_total`
  - [ ] `ai_tutor_biz_refund_total`
  - [ ] `ai_tutor_biz_refund_amount_cents_total`

# 口径与幂等

- [ ] 注册：同一手机号重复登录不重复计入“新增注册”
- [ ] 申请沟通：同一申请的重复提交/重试不重复计入
- [ ] 审批：重复审批/重放不会重复计入通过/拒绝
- [ ] 支付成功：状态已为 SUCCESS 的重复回调不重复累加金额
- [ ] 退款成功：重复回调不重复计数/累加金额

# Prometheus 验证

- [ ] Prometheus scrape 成功（Targets 状态 UP）
- [ ] PromQL 在 Prometheus 侧可查询出非空数据（至少在测试动作后）：
  - [ ] `sum(increase(ai_tutor_biz_user_register_total[1d]))`
  - [ ] `sum(increase(ai_tutor_biz_comm_apply_total[1d]))`
  - [ ] `sum(increase(ai_tutor_biz_comm_apply_decision_total[1d]))`
  - [ ] `sum(increase(ai_tutor_biz_sms_code_send_total[1d]))`
  - [ ] `sum(increase(ai_tutor_biz_payment_info_fee_amount_cents_total[1d])) / 100`
  - [ ] `sum(increase(ai_tutor_biz_collaboration_success_total[1d]))`
  - [ ] `sum(increase(ai_tutor_biz_refund_total[1d]))`
  - [ ] `sum(increase(ai_tutor_biz_refund_amount_cents_total[1d])) / 100`

# Grafana 验证

- [ ] Grafana Prometheus 数据源配置可用
- [ ] KPI 看板至少包含本规范的 7 组面板并可渲染趋势

# 文档

- [ ] `skills/SKILL.md` 已更新并包含：指标列表、PromQL、Prometheus/Grafana 配置要点
