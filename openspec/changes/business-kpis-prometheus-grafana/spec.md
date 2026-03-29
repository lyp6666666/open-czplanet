---
title: 业务关键指标（KPI）Prometheus + Grafana 实时看板
status: draft
date: 2026-03-28
---

# 背景

当前需要在 Grafana 上实时可视化以下业务关键数据的趋势与按天聚合结果，并且尽量不依赖“直接查业务库聚合”的方式：

- 每日新注册用户数（教师/学生/机构）
- 每日申请沟通数量（教师主动/学生主动）
- 每日申请沟通通过/拒绝数量（教师主动/学生主动）
- 发送手机验证码次数
- 每日支付信息费总和
- 每日达成合作总次数
- 每日退款次数与退款总额

本方案采用 Prometheus 作为指标存储与聚合引擎，Grafana 作为可视化层。

# 目标

- 以“最少打点、低侵入”的方式，在关键业务状态变更点产出 Metrics。
- 在 Grafana 看板上直接展示上述指标的趋势与按天聚合数据。
- 每个指标明确：名称、类型、单位、标签（labels）、采集口径、幂等保障策略、PromQL。
- 提供 Prometheus 与 Grafana 基础配置说明（scrape、数据源、dashboard 查询示例）。
- 不引入高基数标签（禁止 user_id / order_no / phone 等）。
- 不影响正常业务：指标上报失败不得影响主流程（Metrics 仅内存计数 + scrape 导出）。

# 非目标

- 不在本次实现“精确 DAU/UV 去重”（distinct user_id/day）。本次的“每日登录用户数”不在需求范围内。
- 不做用户总存量（累计注册用户总量）的精确口径（存量需要状态校准，本次仅做每日新增与业务事件口径）。
- 不提供 Grafana Dashboard JSON 的最终 UI 布局审美定稿（但提供面板清单与 PromQL，后续可再固化 provisioning）。

# 指标设计

## 统一命名与约束

- 指标前缀：`ai_tutor_biz_`
- 仅使用低基数 labels（枚举维度）：`role / initiator / decision`
- 金额指标统一以“分（cents）”作为累加单位，PromQL 中转换为元。
- 全部计数/金额指标使用 Counter（单调递增），按天聚合使用 `increase(metric[1d])`。

## 指标清单

### 1) 每日新注册用户数（按角色）

- Metric: `ai_tutor_biz_user_register_total`
- Type: Counter
- Labels:
  - `role`: `teacher | student | org`
- 口径：用户注册成功（首次创建用户）时 +1。若“登录或注册”接口返回“已存在用户”，不计入。
- 幂等：仅在“插入用户记录成功”或“状态从未注册 -> 已注册”时递增。

PromQL：

- 每日新增注册（总计）：
  - `sum(increase(ai_tutor_biz_user_register_total[1d]))`
- 每日新增注册（按角色）：
  - `sum by (role) (increase(ai_tutor_biz_user_register_total[1d]))`

### 2) 每日申请沟通数量（按发起方）

- Metric: `ai_tutor_biz_comm_apply_total`
- Type: Counter
- Labels:
  - `initiator`: `teacher | student | org`
- 口径：申请沟通单创建成功（初始状态为 PENDING）时 +1。
- 幂等：仅在“创建申请记录成功”时递增；重复提交若命中幂等/复用旧申请，不重复计数。

PromQL：

- 每日申请沟通（总计）：
  - `sum(increase(ai_tutor_biz_comm_apply_total[1d]))`
- 每日申请沟通（按发起方）：
  - `sum by (initiator) (increase(ai_tutor_biz_comm_apply_total[1d]))`

### 3) 每日申请沟通通过/拒绝数量（按发起方与决策）

- Metric: `ai_tutor_biz_comm_apply_decision_total`
- Type: Counter
- Labels:
  - `initiator`: `teacher | student | org`
  - `decision`: `approved | rejected`
- 口径：申请沟通状态从 PENDING 变更为 APPROVED / REJECTED 时 +1。
- 幂等：仅在“状态变更成功”且前置状态为 PENDING 时递增（防止重复审批/重放）。

PromQL：

- 每日通过数量：
  - `sum(increase(ai_tutor_biz_comm_apply_decision_total{decision="approved"}[1d]))`
- 每日拒绝数量：
  - `sum(increase(ai_tutor_biz_comm_apply_decision_total{decision="rejected"}[1d]))`
- 每日通过/拒绝（按发起方）：
  - `sum by (initiator, decision) (increase(ai_tutor_biz_comm_apply_decision_total[1d]))`

### 4) 发送手机验证码次数

- Metric: `ai_tutor_biz_sms_code_send_total`
- Type: Counter
- Labels: 无（如后续需要区分场景，可新增 `scene` 且控制在固定枚举集合内）
- 口径：验证码发送接口“触发发送动作”时 +1（包括模拟发送）。
- 幂等：验证码限流命中（直接拒绝）不计数；真正执行发送逻辑时计数。

PromQL：

- 每日验证码发送次数：
  - `sum(increase(ai_tutor_biz_sms_code_send_total[1d]))`

### 5) 每日支付信息费总和

- Metric: `ai_tutor_biz_payment_info_fee_amount_cents_total`
- Type: Counter
- Labels: 无（如需要可后续加 `channel`，但必须保持低基数）
- 单位：分（cents）
- 口径：信息费支付成功且状态从 PENDING -> SUCCESS 时，按订单金额（分）累加。
- 幂等：仅在支付订单首次成功入库更新成功时累加（基于状态机更新返回值或 eventSent 标记）。

PromQL：

- 每日支付信息费总额（元）：
  - `sum(increase(ai_tutor_biz_payment_info_fee_amount_cents_total[1d])) / 100`

### 6) 每日达成合作总次数

- Metric: `ai_tutor_biz_collaboration_success_total`
- Type: Counter
- Labels:
  - `initiator`: `teacher | student`
- 口径：当“合作达成”这一业务状态首次成立时 +1（本项目中以“信息费订单支付成功并解锁聊天/联系方式”为合作达成口径）。
- 幂等：仅在状态从“未达成”变为“已达成”的首次成功更新时计数。

PromQL：

- 每日达成合作次数（总计）：
  - `sum(increase(ai_tutor_biz_collaboration_success_total[1d]))`
- 每日达成合作次数（按发起方）：
  - `sum by (initiator) (increase(ai_tutor_biz_collaboration_success_total[1d]))`

### 7) 每日退款次数与退款总额

- Metric（次数）: `ai_tutor_biz_refund_total`
- Type: Counter
- Labels: 无（如后续需要可加 `reason`，但必须是固定枚举集合）
- 口径：退款成功（状态首次变为 SUCCESS）时 +1。
- 幂等：仅在首次成功退款入库状态变更时计数。

- Metric（金额）: `ai_tutor_biz_refund_amount_cents_total`
- Type: Counter
- 单位：分（cents）
- 口径：退款成功时按退款金额累加。

PromQL：

- 每日退款次数：
  - `sum(increase(ai_tutor_biz_refund_total[1d]))`
- 每日退款总额（元）：
  - `sum(increase(ai_tutor_biz_refund_amount_cents_total[1d])) / 100`

# Grafana 看板（面板建议）

- 新注册用户（按角色）趋势：time series（1d bucket 或 1h bucket + 日聚合）
- 申请沟通（按发起方）趋势：time series
- 申请沟通通过/拒绝（按发起方、decision）趋势：stacked time series / bar
- 验证码发送次数趋势：time series
- 信息费支付总额（元）趋势：time series（带当日累计对比可选）
- 达成合作次数趋势：time series
- 退款次数 & 退款金额趋势：双轴或拆分两图

# 实现策略（低侵入/稳定性）

## 技术选型

- Spring Boot 3.x + Micrometer + Prometheus registry
- 在每个需要暴露指标的服务中启用 Actuator Prometheus endpoint：
  - `/actuator/prometheus`

## 最少打点策略

- 不在 Controller 层打点（避免计入失败请求/参数校验失败），尽量在“状态变更成功”的 Service 层打点。
- 对支付/退款等幂等敏感动作，只在数据库更新成功路径做计数与金额累加。
- 尽量复用现有“状态机更新返回值/幂等字段”，避免额外查询。

## 可靠性与性能

- 指标仅在进程内内存累加，导出由 Prometheus scrape 拉取；不会阻塞主流程。
- 禁止高基数 label，避免 Prometheus 存储爆炸。
- 金额类 Counter 使用 double 累加（Micrometer），传入值必须为非负且来源为“分”整型转换。

# 配置说明

## 依赖配置（每个相关服务）

在需要暴露业务指标的 Spring Boot 服务中增加依赖：

- `org.springframework.boot:spring-boot-starter-actuator`
- `io.micrometer:micrometer-registry-prometheus`

## 应用配置（application.yml / Nacos）

建议最小配置：

- 暴露 endpoint：
  - `management.endpoints.web.exposure.include=health,info,prometheus`
- Prometheus endpoint：
  - `management.endpoint.prometheus.enabled=true`
- 端口与安全策略：
  - `/actuator/prometheus` 仅允许内网或通过网关/白名单访问（具体由部署侧控制）

## Prometheus 配置（scrape）

示例（按服务实例抓取，实际 target 以部署环境为准）：

```yaml
scrape_configs:
  - job_name: ai-tutor
    metrics_path: /actuator/prometheus
    static_configs:
      - targets:
          - tutor-appointment-service:8080
          - payment-service:8080
          - videoCall-IM-service:8080
```

## Grafana 配置（数据源）

- 添加 Prometheus 数据源指向 Prometheus HTTP 地址
- 在面板中直接使用本规范提供的 PromQL

# 迁移与回滚

- 仅新增 Metrics 暴露与少量打点代码，不影响业务存储结构。
- 回滚策略：关闭 `management.endpoints.web.exposure.include` 中 prometheus 或回滚依赖与打点代码。

# 涉及模块（预计）

- `tutor-appointment-service`：注册成功、验证码发送
- `videoCall-IM-service`：申请沟通创建/决策、信息费支付成功（MQ 消费后落地）、合作达成（同信息费支付成功口径）
- `payment-service`：退款成功（若退款在支付域落地），或由业务域在退款完结点产出指标
