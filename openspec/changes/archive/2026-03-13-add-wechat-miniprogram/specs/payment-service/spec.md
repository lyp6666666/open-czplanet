## 新增需求（ADDED Requirements）

### 需求：微信 JSAPI 支付
系统应支持小程序交易的微信支付 JSAPI 策略。

#### 场景：支付初始化
- **当** 收到 `trade_type=JSAPI` 的支付请求时
- **那么** 系统应验证 `openid` 是否存在
- **那么** 系统应调用微信支付 V3 API 创建预支付订单
- **那么** 系统应返回 `uni.requestPayment` 所需的签名支付参数（timeStamp, nonceStr, package, signType, paySign）

#### 场景：支付通知
- **当** 微信支付发送支付成功通知时
- **那么** 系统应验证签名
- **那么** 系统应将订单状态更新为“已支付”
