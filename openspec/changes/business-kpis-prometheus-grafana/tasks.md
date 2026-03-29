## 1. 依赖与基础能力

- [ ] 1.1 为相关服务引入 Actuator 与 Prometheus registry 依赖
- [ ] 1.2 暴露 `/actuator/prometheus` 并配置 exposure include
- [ ] 1.3 提供统一的业务指标封装（metrics 名称、labels 枚举、写入方法）

## 2. 业务指标打点

- [ ] 2.1 用户注册成功：新增注册 Counter（按 role）
- [ ] 2.2 申请沟通创建：申请数量 Counter（按 initiator）
- [ ] 2.3 申请沟通审批：通过/拒绝 Counter（按 initiator 与 decision）
- [ ] 2.4 验证码发送：发送次数 Counter
- [ ] 2.5 信息费支付成功：金额 Counter（分）与必要的幂等保护
- [ ] 2.6 合作达成：合作次数 Counter（按 initiator）与幂等保护
- [ ] 2.7 退款成功：次数 Counter 与金额 Counter（分）与幂等保护

## 3. 文档与说明

- [ ] 3.1 更新 `skills/SKILL.md`：说明 Grafana KPI 看板与指标列表
- [ ] 3.2 补充 Prometheus/Grafana 配置说明与 PromQL 查询清单

## 4. 测试与验证

- [ ] 4.1 单测：关键打点路径幂等性与计数准确性
- [ ] 4.2 集成验证：启动服务后 `/actuator/prometheus` 可采集到指标
- [ ] 4.3 回归：全量测试通过，原有功能不受影响
