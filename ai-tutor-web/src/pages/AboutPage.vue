<script setup lang="ts">
import { BRAND_NAME } from '@/constants/brand'

const milestones = [
  '围绕家长、教师与机构三方撮合，提供发布需求、浏览信息与在线沟通能力。',
  '支持课程预约、排课协同、在线上课准备与课后总结等教学服务流程。',
  '通过站内消息、邮箱提醒与必要的订单流程，帮助用户更稳定地完成约课与交付。',
]
</script>

<template>
  <div class="page">
    <section class="hero">
      <div class="eyebrow">
        ABOUT
      </div>
      <h1>关于{{ BRAND_NAME }}</h1>
      <p>
        {{ BRAND_NAME }}是一家面向家教与课程服务场景的平台，连接家长、教师与机构，帮助用户更高效地完成需求发布、匹配沟通、课程协同与教学交付。
      </p>
    </section>

    <section class="card section">
      <h2>我们在做什么</h2>
      <ul>
        <li
          v-for="item in milestones"
          :key="item"
        >
          {{ item }}
        </li>
      </ul>
    </section>

    <section class="card section">
      <h2>我们的原则</h2>
      <div class="grid">
        <article>
          <h3>清晰撮合</h3>
          <p>尽量用结构化信息展示需求、课时、预算、科目与时间安排，降低沟通成本。</p>
        </article>
        <article>
          <h3>可信服务</h3>
          <p>围绕身份资料、沟通记录、课程安排与订单流程建设更稳定的服务体验。</p>
        </article>
        <article>
          <h3>隐私优先</h3>
          <p>对联系方式、学习记录与账户资料采取分级管理，只在必要业务场景中处理。</p>
        </article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 40px 20px 56px;
  background:
    radial-gradient(circle at top left, rgba(0, 190, 189, 0.12), transparent 32%),
    linear-gradient(180deg, #f8fbfb 0%, #f5f7fb 42%, #f6f7fb 100%);
}

.hero,
.section {
  width: min(920px, 100%);
  margin: 0 auto;
}

.hero {
  padding: 32px 8px 28px;
}

.eyebrow {
  color: var(--primary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.2em;
}

h1 {
  margin: 10px 0 14px;
  font-size: clamp(32px, 4vw, 48px);
  line-height: 1.1;
}

.hero p {
  max-width: 720px;
  margin: 0;
  color: var(--muted);
  font-size: 16px;
  line-height: 1.8;
}

.section {
  padding: 28px 30px;
  margin-top: 18px;
  border-radius: 20px;
}

h2 {
  margin: 0 0 14px;
  font-size: 22px;
}

ul {
  margin: 0;
  padding-left: 20px;
  color: var(--muted);
  line-height: 1.9;
}

.grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

article {
  padding: 18px;
  border-radius: 16px;
  background: rgba(0, 190, 189, 0.06);
}

h3 {
  margin: 0 0 8px;
  font-size: 16px;
}

article p {
  margin: 0;
  color: var(--muted);
  line-height: 1.8;
}

@media (max-width: 768px) {
  .page {
    padding: 24px 14px 40px;
  }

  .section {
    padding: 22px 18px;
  }

  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
