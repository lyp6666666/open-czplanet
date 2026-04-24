# 支付远程测试

当任务涉及支付回调可达性、收银台测试、支付后聊天解锁，或共享远程测试拓扑时，请使用这份参考。

## 当前拓扑

### 笔记本

- 主工作副本在笔记本本地。
- VS Code SFTP 会自动把仓库改动同步到：
  `root@111.228.20.88:/opt/ai-platform`
- 远程测试期间，推荐通过托管 SSH 隧道在本地浏览器访问：
  `http://localhost:5173`
  `http://localhost:5174`
  `http://localhost:18080`

### 业务服务器

- 主应用服务器：
  `111.228.20.88`
- 这台机器运行真正的仓库副本和所有应用进程：
  gateway `18080`
  appointment `18081`
  IM `18082`
  payment `18083`
  admin `18084`
  用户端 Web `5173`
  管理端 Web `5174`
- 共享中间件默认常驻在这台主机上：
  MySQL、Redis、RabbitMQ、MinIO、Prometheus、Grafana、Nacos
- 日常远程启动这台机器时，通常不应去动中间件。

### 域名 / 回调代理服务器

- 域名服务器：
  `111.229.64.41`
- 域名：
  `huoyue.online`
- 这台机器只运行 `nginx`。
- 它不是主应用宿主机。
- 它存在的原因是：支付平台需要一个带域名的公网回调 URL，而业务服务器本身的公网入口会受到云厂商或支付侧拦截限制。
- 当前职责：
  把 `/payment/notify/*` 和 `/payment/return/*` 代理到 `111.228.20.88`
- 它不提供完整前端站点。
- `http://huoyue.online/` 会刻意返回：
  `ai-tutor payment callback proxy ok`

## 为什么要拆成两台服务器

- 支付平台要求公网可访问的回调 URL。
- `111.228.20.88` 才是真正运行应用的业务服务器，但直连域名到它时，会受到支付侧域名拦截问题影响。
- `111.229.64.41` 作为支付回调路径的公网入口。
- 域名服务器在转发时会把上游 `Host` 改写为 `111.228.20.88`。
- 实际回调路径：
  支付平台 -> `111.229.64.41` 上的 `huoyue.online` -> `nginx` 代理 -> `111.228.20.88` 的 gateway / payment-service

## 当前支付测试流程

### 1. 同步代码

- 本地代码是编辑时的主事实来源。
- 本地保存文件后，让 SFTP 自动同步到：
  `111.228.20.88:/opt/ai-platform`
- 如果服务器表现看起来像旧代码，先怀疑 SFTP 同步漂移，并直接检查远程文件内容。

### 2. 启动业务服务器

在 `111.228.20.88` 上，当前支付测试的标准启动方式是：

```bash
cd /opt/ai-platform
MANAGE_INFRA=never FRONTEND_HOST=0.0.0.0 NACOS_SERVER_ADDR=127.0.0.1:8848 sh scripts/dev_local_up.sh
```

为什么要这么启动：

- 这台服务器上的中间件本来就长期运行
- `MANAGE_INFRA=never` 可以避免误操作 Docker 中间件
- `NACOS_SERVER_ADDR=127.0.0.1:8848` 表示使用同机 Nacos
- `FRONTEND_HOST=0.0.0.0` 既支持远程直接访问，也支持本地 SSH 隧道访问

停止命令：

```bash
cd /opt/ai-platform
STOP_INFRA=0 sh scripts/dev_local_down.sh
```

### 3. 在笔记本上打开本地隧道

推荐本地入口：

```bash
bash scripts/ssh_tunnel.sh start
```

查看状态 / 关闭：

```bash
bash scripts/ssh_tunnel.sh status
bash scripts/ssh_tunnel.sh stop
```

隧道目标：

- `localhost:5173` -> 远程 `127.0.0.1:5173`
- `localhost:5174` -> 远程 `127.0.0.1:5174`
- `localhost:18080` -> 远程 `127.0.0.1:18080`

### 4. 测试时使用正确的 URL

- 笔记本访问用户前端：
  `http://localhost:5173`
- 笔记本访问管理前端：
  `http://localhost:5174`
- 笔记本访问网关/API：
  `http://localhost:18080`
- 支付回调公网域名：
  `http://huoyue.online/payment/notify/yungouos`
- 支付 return 公网域名：
  `http://huoyue.online/payment/return/yungouos`

重要区分：

- 当前用户并不是通过 `huoyue.online` 浏览主站应用。
- `huoyue.online` 只是公网支付回调 / return 入口。
- 如果 `huoyue.online/` 显示 `ai-tutor payment callback proxy ok`，说明这个入口是健康的。

## 当前真实 E2E 里的关键约束

- 信息费阶段的真实链路必须区分两类订单：
  - IM 侧 `brokerage orderId`
  - payment-service 侧 `payment orderNo`
- `chat/application/{id}/enter-chat` 返回的 `orderId` 是 IM 中介费订单 id，不是 payment-service 的 `orderNo`
- 想用 `/payment/dev/orders/{orderNo}/mock-success` 做真实 E2E 时，必须先调用 `/payment/prepay`
- 推荐顺序：
  1. 申请被同意
  2. 调用 `/chat/application/{applicationId}/enter-chat` 确认需要支付，并拿到 `brokerage orderId`
  3. 调用 `/payment/prepay`，参数使用 `contextType=BROKERAGE_ORDER` 与 `contextId=<brokerage orderId>`
  4. 从 `/payment/prepay` 响应中拿到真正的 `payment orderNo`
  5. 调用 `/payment/dev/orders/{payment orderNo}/mock-success`
  6. 轮询 `/payment/orders/{payment orderNo}` 直到 `SUCCESS`
  7. 再验证 IM 是否出现聊天解锁日志和 `CHAT_ENABLED`
- 如果直接把 `brokerage orderId` 传给 `/payment/dev/orders/{orderNo}/mock-success`，通常不会命中正确支付单

## 当前远程环境额外注意事项

- 远端云购收商户当前存在金额上限约束；如果日志出现“支付金额超出商户号最大限额：10元”，说明真实 `/payment/prepay` 下单已失败
- 遇到该错误时，不要继续误判为前端或聊天解锁逻辑异常，应先确认：
  - 当前信息费金额配置
  - 当前测试环境商户额度
  - 是否需要切换到 mock/降额配置后再继续 E2E
- 对真实前端 E2E 来说，只要 `/payment/prepay` 没成功返回 `orderNo`，后续 `mock-success` 和聊天解锁链路都不成立

## 线上回调验证流程

做真实支付测试时，保持下面三个日志窗口同时打开。

### 1. 域名服务器上的 `nginx`

证明支付平台已经打到公网回调入口：

```bash
ssh root@111.229.64.41 "tail -f /var/log/nginx/ai-tutor-payment-domain.access.log | grep --line-buffered -E 'payment/notify/yungouos|payment/return/yungouos'"
```

期望看到：

- `POST /payment/notify/yungouos`
- 类似 `YunGouOS-Notify-CallBack` 的 user-agent

### 2. 业务服务器上的 Payment Service

证明支付回调和最终落库已经进入应用：

```bash
ssh root@111.228.20.88 "cd /opt/ai-platform && tail -f .logs/payment-service.log | grep --line-buffered -E 'PAY_NOTIFY|PAY_FINALIZE|updated to SUCCESS|YunGouOS 回调|YunGouOS 回调验签|YunGouOS 回调订单不存在'"
```

最关键的信号：

- `PAY_NOTIFY`
- `updated to SUCCESS`
- `PAY_FINALIZE success`

在 `2026-04-16` 观察到的真实成功序列：

- 支付平台回调命中了 `huoyue.online`
- 出现了 `PAY_NOTIFY` 日志，但重复通知里 `orderNo` 为空
- 系统仍然通过支付平台查询确认了支付状态
- 出现 `Payment order ... updated to SUCCESS by provider query`
- 出现 `PAY_FINALIZE success`

这个仓库里一个很重要的经验：

- 在当前这套接入下，云购收的 notify 请求可能不会直接带来可用的 `orderNo`
- 即便如此，系统仍然可能通过 provider query + finalize 走到成功
- 所以不要把 `PAY_NOTIFY failed reason=missing_order_no` 直接理解成整笔支付失败

### 3. 业务服务器上的 IM Service

证明支付成功已经解锁后续业务/聊天状态：

```bash
ssh root@111.228.20.88 "cd /opt/ai-platform && tail -f .logs/videoCall-IM-service.log | grep --line-buffered -E 'payment_success_received|brokerage_payment_success|tutor_application_paid'"
```

最关键的信号：

- `brokerage_payment_success`
- `tutor_application_paid`

在 `2026-04-16` 观察到的真实成功序列：

- `brokerage_payment_success start`
- `tutor_application_paid`
- `brokerage_payment_success done`

这个序列和前端表现一致：

- 支付成功
- 页面能跳回消息区域
- 聊天已解锁

## 本项目里的支付业务流程

当前设计中的主流程：

1. 教师发起申请。
2. 学生接受申请。
3. 教师支付信息费。
4. 聊天解锁。
5. 教师和学生在聊天里沟通细节。
6. 任一方都可以进入合作流程。
7. 合作有一周试课期。
8. 如果试课失败，学生可以发起退款。
9. 当前默认试课退款比例是 80%。
10. 如果刚买联系方式后发现不合适，可以直接在聊天里发起退款。
11. 这个早期聊天退款目标上是 100%，但要经过管理员审核。
12. 如果管理员在配置时限内没有处理，退款流程应自动推进。

补充当前线上需求口径：

13. 教师支付信息费前，双方不能继续沟通，也不能发合作、排试课或提交授课申请。
14. 试课合作达成后，先只生成试课，不直接生成正式固定课表。
15. 试课结束后由学生决定是否继续；如果继续，正式固定课表必须在试课结束后 24 小时内完成提交。
16. 如果试课失败或学生不继续，聊天关闭，教师按当前规则可申请退回 80% 信息费。

排查这个流程时，要记住：支付成功只是中点。
如果 UI 还是不对，要继续追到 IM 侧的合作状态和退款状态。

## 支付测试里关键的 Nacos 值

共享 Nacos 主机：

- `111.228.20.88:8848`

Namespaces：

- `dev=481e4376-4576-4b18-ac19-f61e170ca3ae`
- `prod=c3476048-10f6-4cc3-b3f1-90135d736a73`

共享 dev 环境主要使用的支付配置 DataId：

- `ai-tutor-payment-dev.yaml`

当前关键的线上值：

- `payment.enabled: true`
- `yungouos.notify-url: "http://huoyue.online/payment/notify/yungouos"`
- `yungouos.return-url: "http://huoyue.online/payment/return/yungouos"`
- `yungouos.return-page-url: "http://111.228.20.88:5173/"`

解释：

- notify / return 回调必须走公网域名服务器
- return-page 当前仍然指向业务服务器前端，而不是 `huoyue.online`
- 判断支付是否真正完成时，异步回调正确性比浏览器最终跳回哪个页面更重要

快速查询方式：

```bash
ssh root@111.228.20.88 "curl -s 'http://127.0.0.1:8848/nacos/v1/cs/configs?tenant=481e4376-4576-4b18-ac19-f61e170ca3ae&dataId=ai-tutor-payment-dev.yaml&group=DEFAULT_GROUP' | grep -nE 'notifyUrl:|notify-url:|return-url:|return-page-url:'"
```

## 快速健康检查

确认域名解析：

```bash
dig +short huoyue.online
```

期望结果：

- `111.229.64.41`

确认域名服务器根路径健康：

```bash
curl -i http://huoyue.online/
```

期望响应体：

- `ai-tutor payment callback proxy ok`

确认业务服务器端口：

```bash
ssh root@111.228.20.88 "cd /opt/ai-platform && ss -ltnp | grep -E ':18080|:18082|:18083|:5173|:5174 ' || true"
```

## 真实测试中的判断规则

- 如果域名服务器 `nginx` 日志里有回调记录，说明支付平台已经打到了公网入口。
- 如果随后 `payment-service` 记录了 `updated to SUCCESS` 和 `PAY_FINALIZE success`，说明应用端已经接受了这笔支付。
- 如果 IM 服务记录了 `tutor_application_paid`，说明业务解锁已经完成。
- 如果这三者都成立，但前端看起来仍不对，优先怀疑前端状态刷新或页面路由问题，而不是回调可达性本身。
- 如果 `payment-service` 已成功，但 IM 侧没有 `payment_success_received` / `brokerage_payment_success` / `tutor_application_paid`，优先怀疑 MQ 事件、IM 消费、或 application chat gating 同步问题。
- 如果合作提案接口仍报“教师支付信息费后，双方才能继续沟通并发起合作或授课申请”，说明申请仍停留在 `PAYMENT_REQUIRED`，需要回查支付单是否真正走完到 IM 解锁，而不是只看前端收银台显示。
