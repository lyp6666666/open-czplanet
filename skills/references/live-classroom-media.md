# 实时课堂媒体链路

这份说明只记录本仓库和当前共享 DEV 环境下，实时课堂从业务入会到真实音视频建立的关键事实。

## 当前线上拓扑

- 公网域名机：`111.229.64.41`
  - 只负责 `huoyue.online` 的 `80/443`
  - 负责把 `/livekit` 的 HTTPS/WSS 信令反代到应用机
- 应用机：`111.228.20.88`
  - 实际运行网关、业务服务、LiveKit、MySQL、Redis、Nacos
  - 浏览器真实音视频流不会走公网域名机 nginx，而是直接打到这台机器的 LiveKit 媒体端口

## 当前有效端口要求

- `111.229.64.41`
  - 仅需保留 `80/443`
- `111.228.20.88`
  - `80/443`
  - `TCP 7881`
  - `UDP 50000-50100`

仓库当前 LiveKit 配置见：

- `Dockerfile/livekit/livekit.yaml`
- `Dockerfile/docker-compose.yml`

其中：

- `rtc.port_range_start=50000`
- `rtc.port_range_end=50100`
- `rtc.tcp_port=7881`
- `rtc.use_external_ip=true`

只要仍然使用这组配置，云安全组/云防火墙就必须完整放通这整段 UDP 和 `7881/tcp`。

## 原来为什么会出现“双方都在等待对方”

- 课堂业务层其实已经让双方进入了同一个 `LiveKit room`
- `/livekit` 的 WebSocket 信令链路是通的，所以页面能进入课堂壳子
- 但浏览器无法真正回到 `111.228.20.88` 的媒体端口时，WebRTC 的 ICE 无法完成
- 结果就是：
  - 页面一直 `connecting/reconnecting/disconnected`
  - 双方都显示“已进入课堂，正在等待对方加入”
  - 看不到远端视频，也听不到远端声音

## 这类问题的最准排查方法

### 1. 先确认应用机 TCP 回退端口可达

```bash
nc -vz -w 4 111.228.20.88 7881
```

如果这里直接超时，说明应用机的公网入口还没打通，不要先怀疑前端代码。

### 2. 线上抓包看媒体是否双向

在应用机执行：

```bash
ssh root@111.228.20.88 'timeout 90 tcpdump -n -i any "(udp portrange 50000-50100) or tcp port 7881" -vv -c 220'
```

判断标准：

- 只有服务器向浏览器公网地址发包，没有浏览器回包：
  仍然是安全组/云防火墙/公网链路问题
- 已经看到浏览器公网地址持续回包到 `50000-50100/udp`：
  说明真实媒体链路已通

### 3. 再跑浏览器 E2E

```bash
cd ai-tutor-web
PLAYWRIGHT_BASE_URL=https://huoyue.online \
PLAYWRIGHT_API_BASE_URL=https://huoyue.online \
OPS_VERIFY_TOKEN=DevOpsVerifyTokenForE2E \
npx playwright test e2e/live-classroom.spec.ts --project=chromium
```

关键成功证据：

- `classroom-connection-state` 进入 `connected`
- 浏览器日志出现：
  - `participant:connected`
  - `track:subscribed {kind: video}`
  - `track:subscribed {kind: audio}`
- 测试 `teacher and student can join same livekit room with media permissions` 通过

## 当前已验证通过的线上链路

### 完整业务链路

`ai-tutor-web/e2e/live-classroom.spec.ts` 已在线上 DEV 验证通过：

- 学生发起沟通
- 教师同意
- 信息费支付成功
- 教师发起合作提案
- 学生接受
- 双方进入同一试课课堂
- 双方真实建立远端视频和远端音频轨

### 最短课堂链路

如果只是验证课堂本身，不必每次都从申请/支付整条链走完。
可以直接在数据库中构造：

- `user`
- `teacher_profile`
- `student_profile`
- `room`
- `course_enrollment`
- `live_class_session`
- `live_class_participant`

然后使用 `/api/v1/public/dev/sms/login` 给双方拿 token，直接打开：

- `/#/live/prepare/{courseId}`

这条最短链路的目标是快速验证：

- 准备页可进入
- 双方都可 join
- `LiveKit` 真正连通
- 不再出现“两边都在等待对方”

## 回归建议

凡是下面任一项发生变化，都至少要重跑一次 `ai-tutor-web/e2e/live-classroom.spec.ts`：

- `Dockerfile/livekit/livekit.yaml`
- `Dockerfile/docker-compose.yml`
- 应用机云安全组
- `/livekit` nginx 转发
- `live-class-service`
- `ai-tutor-web/src/modules/live/livekit.ts`
- `ai-tutor-web/src/pages/live/LivePreparePage.vue`
- `ai-tutor-web/src/pages/live/LiveClassroomPage.vue`
