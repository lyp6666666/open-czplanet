<template>
  <div class="permission-guide-page">
    <section class="hero card">
      <div class="eyebrow">浏览器权限帮助</div>
      <h1>开启摄像头、麦克风和扬声器权限</h1>
      <p>
        如果课堂准备页没有弹出浏览器授权框，或者页面提示“当前浏览器不支持音视频采集”，可以按照下面的步骤逐项排查。
      </p>
    </section>

    <section class="grid">
      <div class="card step-card">
        <h2>1. 先确认是不是安全连接</h2>
        <p>浏览器只会在 `https`、`localhost` 或受信任环境里开放摄像头和麦克风权限。</p>
        <ul>
          <li>如果地址栏显示“不安全”，先切换到带证书的 `https` 域名。</li>
          <li>如果是本地调试，优先使用 `localhost` 访问。</li>
          <li>切回安全连接后，重新进入课堂准备页并点击“一键检测设备权限”。</li>
        </ul>
      </div>

      <div class="card step-card">
        <h2>2. 在浏览器站点设置中允许权限</h2>
        <p>如果你之前点过“拒绝”，浏览器通常不会再次自动弹窗，需要手动改回“允许”。</p>
        <ul>
          <li>Chrome / Edge：点击地址栏左侧站点图标，在“摄像头”“麦克风”里改成“允许”。</li>
          <li>Safari：打开“Safari - 设置 - 网站 - 摄像头 / 麦克风”，将当前站点改为“允许”。</li>
          <li>改完权限后刷新课堂准备页，再重新检测。</li>
        </ul>
      </div>

      <div class="card step-card">
        <h2>3. 检查设备是否被占用</h2>
        <p>当会议软件、直播工具或录屏应用占用设备时，浏览器即使有权限也无法正常采集。</p>
        <ul>
          <li>先关闭 Zoom、腾讯会议、飞书会议、OBS、录屏工具等应用。</li>
          <li>外接摄像头、耳机或麦克风重新插拔一次。</li>
          <li>返回课堂准备页重新做设备自检。</li>
        </ul>
      </div>

      <div class="card step-card">
        <h2>4. 试听扬声器</h2>
        <p>扬声器通常不需要单独授权，但浏览器不一定支持切换输出设备。</p>
        <ul>
          <li>在课堂准备页点击“开始试听”，确认自己能听到提示音。</li>
          <li>如果没有声音，先检查系统音量和输出设备。</li>
          <li>如浏览器不支持切换扬声器设备，网站会继续使用系统默认输出。</li>
        </ul>
      </div>
    </section>

    <section class="card footer-card">
      <h2>仍然无法解决？</h2>
      <p>请把课堂准备页中的错误提示、当前浏览器名称和地址栏状态一并反馈给平台，我们可以更快判断是权限、证书还是设备兼容性问题。</p>
      <div class="actions">
        <button class="btn" type="button" @click="$router.back()">返回上一页</button>
        <button class="btn btn-primary" type="button" @click="$router.push({ name: 'home' })">回到首页</button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.permission-guide-page {
  display: grid;
  gap: 18px;
}

.hero {
  padding: 24px;
  border-radius: 28px;
  background:
    radial-gradient(circle at top left, rgba(16, 185, 129, 0.18), transparent 32%),
    linear-gradient(135deg, #f8fcff 0%, #eefbf5 100%);
}

.eyebrow {
  font-size: 12px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(0, 0, 0, 0.48);
}

h1 {
  margin: 8px 0 10px;
  font-size: 34px;
  line-height: 1.15;
}

p,
li {
  color: rgba(15, 23, 42, 0.72);
  line-height: 1.7;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.step-card,
.footer-card {
  padding: 20px;
}

.step-card h2,
.footer-card h2 {
  margin: 0 0 10px;
  font-size: 22px;
}

ul {
  margin: 12px 0 0;
  padding-left: 20px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}

@media (max-width: 900px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
