## ADDED Requirements

### Requirement: 已读 ACK 必须持久化且幂等
系统 MUST 提供会话维度的已读 ACK 能力，并将“用户在某会话中最后已读消息 id”持久化到数据库中；对同一会话的多次 ACK MUST 幂等且单调递增（不能回退到更小的消息 id）。

#### Scenario: 重复 ACK 不会回退
- **WHEN** 用户对同一 room 连续上报两次已读（先上报 lastReadMsgId=100，再上报 lastReadMsgId=80）
- **THEN** 服务端最终持久化的 lastReadMsgId MUST 为 100

### Requirement: 刷新与跨页面后未读数必须一致
系统 MUST 在会话列表接口中返回基于“已读持久化状态”计算得到的未读数；前端在刷新页面或跨页面重新进入后，未读红点 MUST 与服务端返回保持一致。

#### Scenario: 已读后刷新不再出现红点
- **WHEN** 用户在会话 A 中阅读到最新消息并成功 ACK，随后切换到其他页面并刷新浏览器
- **THEN** 会话 A 在会话列表中的 unreadCount MUST 为 0，顶部“消息”入口红点 MUST 不再显示

### Requirement: ACK 失败必须可观测且可恢复
当服务端 ACK 写入失败（例如表不存在、权限不足、数据库异常）时，接口 MUST 返回明确错误；前端 MUST 维持未读状态并支持自动重试或在下次进入会话时再次上报。

#### Scenario: ACK 失败不会导致“假消红点”
- **WHEN** 用户进入会话并触发 ACK，但服务端返回错误
- **THEN** 前端 MUST 不将该会话永久视为已读，刷新后红点 MUST 仍按服务端未读数展示，并在后续成功 ACK 后消失
