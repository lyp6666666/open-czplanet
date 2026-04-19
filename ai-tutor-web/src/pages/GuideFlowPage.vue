<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'

import { BRAND_NAME } from '@/constants/brand'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{
  audience: 'tutor' | 'student'
}>()

const auth = useAuthStore()

const isTutorGuide = computed(() => props.audience === 'tutor')

const hero = computed(() =>
  isTutorGuide.value
    ? {
        eyebrow: '家教教程',
        title: '三步进入高质量接单节奏',
        subtitle: '把资料、沟通和合作流程一次走顺，才能更快接到合适的课单。',
        cta: '/auth/tutor',
        ctaText: auth.isLoggedIn ? '去查看需求' : '去登录家教端',
      }
    : {
        eyebrow: '学生教程',
        title: '从发需求到约课成交更清晰',
        subtitle: '把需求写准确、筛选路径拉直，能明显提升匹配效率和沟通质量。',
        cta: '/auth/student',
        ctaText: auth.isLoggedIn ? '去发布需求' : '去登录学生端',
      },
)

const steps = computed(() =>
  isTutorGuide.value
    ? [
        { title: '完善资料', desc: '先补齐擅长科目、授课方式、时薪和亮点介绍，让家长第一眼就知道你适不适合。' },
        { title: '浏览需求', desc: '优先筛选与你学段、授课模式和城市匹配的需求，减少无效沟通。' },
        { title: '主动沟通', desc: '通过聊天入口快速说明你的优势、可授课时间和试听安排，提升回复率。' },
        { title: '确认合作', desc: '双方确认后再推进支付、试听或排课，避免信息不对称造成反复。' },
        { title: '开始授课', desc: '把第一次课目标、资料和时间都提前约定清楚，成交后更容易长期合作。' },
      ]
    : [
        { title: '发布需求', desc: '把年级、科目、时间、预算和授课方式写具体，系统更容易给你推荐对的人。' },
        { title: '挑选老师', desc: '先看老师的学科标签、教育背景、经验和报价，再决定是否发起沟通。' },
        { title: '发起聊天申请', desc: '学生可以主动向心仪教师发起开始聊天申请；教师通过后，信息费仍由教师支付，支付完成才会解锁聊天。' },
        { title: '沟通细节', desc: '聊天解锁后，把孩子基础、希望提升点和上课频次讲清楚，老师更容易给出针对性方案。' },
        { title: '试听确认', desc: '若支持试听，可先约一节试课验证风格是否合适；后续合作、试课退款等流程保持平台统一规则。' },
        { title: '安排课程', desc: '最终确认老师、课时和时间表后，再进入持续上课与后续复盘。' },
      ],
)

const tips = computed(() =>
  isTutorGuide.value
    ? [
        '标题和亮点不要空，家长通常先看你能解决什么问题。',
        '第一条消息要短、准、真，尽量说明科目经验与时间匹配度。',
        '若需求不匹配，及时跳过，比盲目海投更节省时间。',
      ]
    : [
        '需求越具体，老师给你的回复就越有针对性。',
        '先确认授课方式和时间，再谈预算与长期安排更高效。',
        '试课后及时反馈，能更快锁定合适老师。',
      ],
)
</script>

<template>
  <div class="page">
    <header v-if="!auth.isLoggedIn" class="guest-head">
      <div class="container guest-inner">
        <RouterLink class="brand" to="/">{{ BRAND_NAME }}</RouterLink>
        <RouterLink class="back-link" to="/">返回首页</RouterLink>
      </div>
    </header>

    <main class="container content">
      <section class="hero card">
        <div class="hero-copy">
          <div class="eyebrow">{{ hero.eyebrow }}</div>
          <h1>{{ hero.title }}</h1>
          <p>{{ hero.subtitle }}</p>
          <div class="hero-actions">
            <RouterLink class="btn btn-primary" :to="hero.cta">{{ hero.ctaText }}</RouterLink>
            <RouterLink class="btn btn-muted" to="/">返回首页</RouterLink>
          </div>
        </div>
        <div class="hero-panel">
          <div class="panel-card">
            <div class="panel-title">{{ isTutorGuide ? '接单节奏' : '找老师节奏' }}</div>
            <div class="panel-value">{{ isTutorGuide ? '资料 -> 需求 -> 沟通 -> 合作' : '需求 -> 老师 -> 沟通 -> 约课' }}</div>
            <div class="panel-sub">把流程看清，比盲目点击更快进入有效匹配。</div>
          </div>
        </div>
      </section>

      <section class="section">
        <div class="section-head">
          <div class="section-title">流程步骤</div>
          <div class="section-sub">建议按顺序完成，每一步都直接影响后续匹配效率。</div>
        </div>
        <div class="steps">
          <article v-for="(step, index) in steps" :key="step.title" class="step card">
            <div class="step-index">0{{ index + 1 }}</div>
            <div class="step-title">{{ step.title }}</div>
            <div class="step-desc">{{ step.desc }}</div>
          </article>
        </div>
      </section>

      <section class="section tips-wrap">
        <div class="section-head">
          <div class="section-title">实用建议</div>
          <div class="section-sub">这些细节通常比功能入口本身更影响最终转化。</div>
        </div>
        <div class="tips">
          <div v-for="tip in tips" :key="tip" class="tip card">{{ tip }}</div>
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background:
    radial-gradient(720px 320px at 10% 0%, rgba(0, 190, 189, 0.16), transparent 60%),
    radial-gradient(900px 380px at 90% 0%, rgba(30, 64, 175, 0.14), transparent 58%),
    #f6f8fc;
}

.guest-head {
  position: sticky;
  top: 0;
  z-index: 10;
  backdrop-filter: blur(10px);
  background: rgba(246, 248, 252, 0.82);
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
}

.guest-inner {
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.brand {
  font-size: 24px;
  font-weight: 900;
  letter-spacing: 0.02em;
  color: #0f172a;
}

.back-link {
  color: #0f766e;
  font-weight: 700;
}

.content {
  padding: 28px 0 40px;
  display: grid;
  gap: 24px;
}

.hero {
  padding: 28px;
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(260px, 0.8fr);
  gap: 18px;
  overflow: hidden;
  position: relative;
}

.hero::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.82), rgba(255, 255, 255, 0.62)),
    linear-gradient(135deg, rgba(0, 190, 189, 0.08), rgba(30, 64, 175, 0.08));
}

.hero-copy,
.hero-panel {
  position: relative;
  z-index: 1;
}

.eyebrow {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(0, 190, 189, 0.12);
  color: #0f766e;
  font-size: 13px;
  font-weight: 800;
}

h1 {
  margin: 14px 0 10px;
  font-size: clamp(30px, 4vw, 44px);
  line-height: 1.08;
}

p {
  margin: 0;
  max-width: 640px;
  color: #475569;
  font-size: 15px;
}

.hero-actions {
  margin-top: 20px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.hero-panel {
  display: flex;
  align-items: stretch;
}

.panel-card {
  width: 100%;
  border-radius: 24px;
  padding: 22px;
  background: linear-gradient(160deg, #0f172a, #1d4ed8);
  color: white;
  display: grid;
  align-content: center;
  gap: 10px;
}

.panel-title {
  font-size: 14px;
  font-weight: 800;
  color: rgba(255, 255, 255, 0.72);
}

.panel-value {
  font-size: 24px;
  font-weight: 900;
  line-height: 1.25;
}

.panel-sub {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.82);
}

.section {
  display: grid;
  gap: 14px;
}

.section-head {
  display: grid;
  gap: 4px;
}

.section-title {
  font-size: 24px;
  font-weight: 900;
}

.section-sub {
  color: #64748b;
  font-size: 14px;
}

.steps {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}

.step {
  padding: 18px;
  display: grid;
  gap: 10px;
  min-height: 188px;
}

.step-index {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: rgba(0, 190, 189, 0.12);
  color: #0f766e;
  font-weight: 900;
}

.step-title {
  font-size: 18px;
  font-weight: 800;
}

.step-desc {
  color: #475569;
  font-size: 14px;
  line-height: 1.7;
}

.tips {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.tip {
  padding: 18px;
  color: #334155;
  font-size: 14px;
  line-height: 1.7;
}

@media (max-width: 1100px) {
  .steps {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .tips {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 860px) {
  .hero {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .content {
    padding: 18px 0 28px;
  }

  .guest-inner {
    height: 64px;
  }

  .hero {
    padding: 20px;
  }

  .steps {
    grid-template-columns: 1fr;
  }
}
</style>
