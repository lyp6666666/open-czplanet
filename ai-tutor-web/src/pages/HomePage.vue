<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Autoplay, Pagination } from 'swiper/modules'
import { Swiper, SwiperSlide } from 'swiper/vue'

import { useCityStore } from '@/stores/city'
import { useHomeStore } from '@/stores/home'
import HomeFooter from '@/ui/home/HomeFooter.vue'

import 'swiper/css'
import 'swiper/css/pagination'

const router = useRouter()
const home = useHomeStore()
const cityStore = useCityStore()

const activeProofIndex = ref(0)
const activeHeroIndex = ref(0)
const teacherRail = ref<HTMLElement | null>(null)

const serviceHighlights = [
  {
    icon: '◉',
    title: '线上实时家教',
    desc: 'AI实时诊断课堂表现，课后自动生成进步画像与学情建议。',
    action: '立即体验流程',
    route: '/guide/student',
  },
  {
    icon: '◎',
    title: '线下本地辅导',
    desc: '匹配本地星级老师，围绕提分目标制定可执行的辅导节奏。',
    action: '查看邀约流程',
    route: '/student/post',
  },
] as const

const platformMetrics = [
  { value: '98%', label: '家长满意度', note: '真实反馈持续提升' },
  { value: '15-30分', label: '平均提分区间', note: '按阶段目标追踪' },
  { value: '10,000+', label: '学生正在学习', note: '覆盖多学段多城市' },
  { value: '6年+', label: '专注在线教育', note: '教研与产品持续投入' },
] as const

const aiSteps = [
  {
    index: '1.',
    title: '课前诊断',
    desc: 'AI分析孩子的薄弱项、目标分层与提分阻力，快速给出入门方案。',
    points: ['知识画像', '阶段定位', '学习习惯'],
  },
  {
    index: '2.',
    title: '课堂实时监控',
    desc: '监测专注度、互动频率、理解程度，让上课状态有迹可循。',
    points: ['专注波动', '互动次数', '理解偏差'],
  },
  {
    index: '3.',
    title: '课后精准巩固',
    desc: '结合课堂表现自动推送练习与复盘建议，减少无效刷题。',
    points: ['错因分析', '本节错题', '复练任务'],
  },
  {
    index: '4.',
    title: '长期成长追踪',
    desc: '把每次课程效果沉淀成长期成长曲线，帮助家长看见阶段变化。',
    points: ['成长曲线', '阶段对比', '策略调整'],
  },
] as const

const trustBadges = [
  '教师严格审核',
  '教学过程透明',
  '资金安全保障',
  '隐私安全保护',
  '售后无忧服务',
] as const

const heroAssurances = [
  {
    title: '严选名校教师',
    desc: '5重审核，履历纪实',
    icon: 'badge',
  },
  {
    title: 'AI数据驱动',
    desc: '课堂表现实时记录',
    icon: 'spark',
  },
  {
    title: '效果可视化',
    desc: '成长看得见，进步看得清',
    icon: 'shield',
  },
] as const

const parentProofs = [
  {
    quote: '孩子以前上课听着听着就走神，现在会主动总结，成绩也稳步提上来了。',
    author: '初二家长',
  },
  {
    quote: '平台把老师匹配、课堂记录、课后复盘串在一起，家长终于能看懂进步过程。',
    author: '高一家长',
  },
  {
    quote: '不只是找老师方便，而是每节课之后都知道下一步该怎么补，心里特别踏实。',
    author: '六年级家长',
  },
] as const

const fallbackTutors = [
  {
    userId: 101,
    displayName: '张老师',
    avatar: '/avatars/avatar-1.svg',
    city: '北京',
    education: '数学教育硕士',
    experienceYears: 8,
    ratePerHour: '320',
    subjectTags: ['数学', '初中数学'],
    highlights: ['擅长压轴题拆解', '阶段提分显著'],
  },
  {
    userId: 102,
    displayName: '李老师',
    avatar: '/avatars/avatar-2.svg',
    city: '上海',
    education: '英语硕士',
    experienceYears: 5,
    ratePerHour: '280',
    subjectTags: ['英语', '高中英语'],
    highlights: ['方法型教学', '口语与阅读兼顾'],
  },
  {
    userId: 103,
    displayName: '王老师',
    avatar: '/avatars/avatar-6.svg',
    city: '深圳',
    education: '物理师范',
    experienceYears: 6,
    ratePerHour: '300',
    subjectTags: ['物理', '高中物理'],
    highlights: ['模型化讲解', '培优冲刺'],
  },
  {
    userId: 104,
    displayName: '陈老师',
    avatar: '/avatars/avatar-2.svg',
    city: '杭州',
    education: '语文教育',
    experienceYears: 7,
    ratePerHour: '260',
    subjectTags: ['语文', '阅读写作'],
    highlights: ['作文提分', '课堂感染力强'],
  },
  {
    userId: 105,
    displayName: '刘老师',
    avatar: '/avatars/avatar-5.svg',
    city: '广州',
    education: '化学本科',
    experienceYears: 4,
    ratePerHour: '240',
    subjectTags: ['化学', '初中化学'],
    highlights: ['基础巩固', '薄弱点清零'],
  },
] as const

const heroCarouselItems = computed(() => {
  const rows = home.banners?.carousel || []
  if (rows.length) return rows
  return [
    {
      id: 'fallback-1',
      title: '在线 1对1，AI实时监控',
      subtitle: 'AI助教全程陪伴',
      imageUrl: '/banners/carousel-1.svg',
      link: { type: 'ROUTE', url: '/guide/student' },
    },
  ]
})

const currentHeroSlide = computed(() => heroCarouselItems.value[activeHeroIndex.value] || heroCarouselItems.value[0] || null)

const heroFallbackBanners = ['/banners/carousel-1.svg', '/banners/carousel-2.svg', '/banners/carousel-3.svg'] as const

const displayTutors = computed(() => {
  const source = home.hotTutorsPool.length ? home.hotTutorsPool : home.hotTutors.list
  const list = source.map((item, index) => ({
    userId: item.userId,
    displayName: item.displayName,
    avatar: item.avatar || fallbackTutors[index % fallbackTutors.length]!.avatar,
    city: item.city,
    education: item.education,
    experienceYears: item.experienceYears,
    ratePerHour: item.ratePerHour,
    subjectTags: [...(item.subjectTags?.length ? item.subjectTags : fallbackTutors[index % fallbackTutors.length]!.subjectTags)],
    highlights: [...(item.highlights?.length ? item.highlights : fallbackTutors[index % fallbackTutors.length]!.highlights)],
  }))

  if (!list.length) return [...fallbackTutors]

  return [...list]
    .sort((a, b) => Number(b.highlights.includes('星级教师')) - Number(a.highlights.includes('星级教师')))
    .slice(0, 12)
})

function goRoute(path: string) {
  void router.push(path)
}

function goHeroLink() {
  const url = currentHeroSlide.value?.link?.url?.trim()
  if (!url) {
    void router.push('/guide/student')
    return
  }
  if (currentHeroSlide.value?.link?.type === 'ROUTE' || url.startsWith('/')) {
    void router.push(url)
    return
  }
  window.open(url, '_blank', 'noreferrer')
}

function fallbackAvatar(index = 0) {
  return fallbackTutors[index % fallbackTutors.length]!.avatar
}

function fallbackHeroBanner(index = 0) {
  return heroFallbackBanners[index % heroFallbackBanners.length]!
}

function handleTutorImageError(event: Event, index = 0) {
  const target = event.target as HTMLImageElement | null
  if (!target) return
  const next = fallbackAvatar(index)
  if (target.src.endsWith(next)) return
  target.src = next
}

function handleHeroImageError(event: Event, index = 0) {
  const target = event.target as HTMLImageElement | null
  if (!target) return
  const next = fallbackHeroBanner(index)
  if (target.src.endsWith(next)) return
  target.src = next
}

function handleHeroFloatImageError(event: Event) {
  const target = event.target as HTMLImageElement | null
  if (!target) return
  const next = fallbackHeroBanner(activeHeroIndex.value)
  if (target.src.endsWith(next)) return
  target.src = next
}

function isStarTutor(tutor: { highlights: readonly string[] }) {
  return tutor.highlights.includes('星级教师')
}

function onHeroSlideChange(swiper: { realIndex?: number; activeIndex?: number }) {
  activeHeroIndex.value = swiper.realIndex ?? swiper.activeIndex ?? 0
}

function scrollTeacherRail(direction: 'prev' | 'next') {
  const el = teacherRail.value
  if (!el) return
  const distance = Math.max(320, Math.floor(el.clientWidth * 0.82))
  el.scrollBy({ left: direction === 'next' ? distance : -distance, behavior: 'smooth' })
}

function nextProof() {
  activeProofIndex.value = (activeProofIndex.value + 1) % parentProofs.length
}

let proofTimer: number | null = null

watch(
  () => cityStore.city,
  (value) => {
    if (value && value !== home.city) void home.setCity(value)
  },
)

onMounted(() => {
  if (cityStore.city && cityStore.city !== home.city) {
    home.city = cityStore.city
  }
  void home.initHome()
  proofTimer = window.setInterval(nextProof, 5000)
})

onUnmounted(() => {
  if (proofTimer) {
    window.clearInterval(proofTimer)
  }
})
</script>

<template>
  <div class="home-shell">
    <main class="page-main">
      <div class="container landing">
        <section class="hero-panel surface-card">
          <div class="hero-copy">
            <div class="hero-badge">
              <span>只做 1对1 · 专注提分</span>
              <span class="hero-divider"></span>
              <span>AI助教全程陪伴</span>
            </div>

            <h1 class="hero-title">
              <span>名校学子1对1陪伴</span>
              <span>学习更高效，<em class="hero-accent">成长看得见</em></span>
            </h1>
            <p class="hero-subtitle">
              课堂表现实时分析、错题精准定位、学习路径动态优化，让每一次上课都成为进步的阶梯。
            </p>

            <div class="service-grid">
              <button
                v-for="item in serviceHighlights"
                :key="item.title"
                class="service-card"
                type="button"
                @click="goRoute(item.route)"
              >
                <div class="service-icon">{{ item.icon }}</div>
                <div class="service-text">
                  <div class="service-title">{{ item.title }}</div>
                  <div class="service-desc">{{ item.desc }}</div>
                  <div class="service-link">{{ item.action }}</div>
                </div>
              </button>
            </div>

            <div class="hero-bottom">
              <article v-for="item in heroAssurances" :key="item.title" class="hero-bottom-item">
                <div class="hero-bottom-icon" :class="`hero-bottom-icon-${item.icon}`">
                  <span></span>
                </div>
                <div class="hero-bottom-copy">
                  <div class="hero-bottom-title">{{ item.title }}</div>
                  <div class="hero-bottom-desc">{{ item.desc }}</div>
                </div>
              </article>
            </div>
          </div>

          <div class="hero-visual">
            <div class="visual-frame">
              <div class="visual-head">
                <div>
                  <div class="visual-title">{{ currentHeroSlide?.title || '在线 1对1，AI实时监控' }}</div>
                  <div class="visual-kicker">{{ currentHeroSlide?.subtitle || 'AI助教全程陪伴' }}</div>
                </div>
                <span class="visual-chip">AI助教已接入</span>
              </div>

              <div class="classroom-board">
                <div class="teacher-stage">
                  <Swiper
                    class="teacher-stage-swiper"
                    :modules="[Autoplay, Pagination]"
                    :loop="heroCarouselItems.length > 1"
                    :autoplay="heroCarouselItems.length > 1 ? { delay: 3600, disableOnInteraction: false } : false"
                    :pagination="heroCarouselItems.length > 1 ? { clickable: true, el: '.teacher-stage-pagination', bulletClass: 'hero-pagination-bullet', bulletActiveClass: 'hero-pagination-bullet-active' } : false"
                    @slideChange="onHeroSlideChange"
                  >
                    <SwiperSlide v-for="(item, index) in heroCarouselItems" :key="item.id">
                      <button class="hero-slide-button" type="button" @click="goHeroLink">
                        <img
                          class="teacher-photo"
                          :src="item.imageUrl || fallbackHeroBanner(index)"
                          :alt="item.title || `首页轮播图-${index + 1}`"
                          @error="handleHeroImageError($event, index)"
                        />
                      </button>
                    </SwiperSlide>
                  </Swiper>
                  <div class="teacher-float">
                    <img
                      class="teacher-float-avatar"
                      :src="currentHeroSlide?.imageUrl || fallbackHeroBanner(activeHeroIndex)"
                      alt="assistant"
                      @error="handleHeroFloatImageError"
                    />
                  </div>
                  <div class="video-toolbar">
                    <span>视频</span>
                    <span>语音</span>
                    <span>互动</span>
                    <span>00:36:28</span>
                  </div>
                  <div class="teacher-stage-pagination"></div>
                </div>
              </div>

            </div>
          </div>
        </section>

        <section class="metrics-bar surface-card">
          <article v-for="item in platformMetrics" :key="item.label" class="metric-card">
            <div class="metric-value">{{ item.value }}</div>
            <div class="metric-label">{{ item.label }}</div>
            <div class="metric-note">{{ item.note }}</div>
          </article>
        </section>

        <section class="ai-section surface-card">
          <div class="section-intro">
            <span class="section-tag">平台自研最新AI技术</span>
            <h2>免费赋能在线一对一教育</h2>
            <p>科技让好老师更懂学生，让学习更高效，也让家长真正看见进步依据。</p>
          </div>

          <div class="ai-grid">
            <article v-for="step in aiSteps" :key="step.title" class="ai-card">
              <div class="ai-index">{{ step.index }}</div>
              <h3>{{ step.title }}</h3>
              <p>{{ step.desc }}</p>
              <div class="ai-points">
                <span v-for="point in step.points" :key="point" class="ai-point">{{ point }}</span>
              </div>
              <div class="mini-chart" :class="`chart-${step.index[0]}`">
                <span></span>
              </div>
            </article>
          </div>
        </section>

        <section class="teachers-section">
          <div class="section-head">
            <div>
              <span class="section-tag">平台星级认证教师</span>
              <h2>名校背景 + 教学经验 + 增长提分</h2>
            </div>
            <div class="section-actions">
              <button class="rail-btn" type="button" @click="scrollTeacherRail('prev')">←</button>
              <button class="rail-btn" type="button" @click="scrollTeacherRail('next')">→</button>
              <button class="text-action" type="button" @click="goRoute('/student/tutors')">查看全部教师</button>
            </div>
          </div>

          <div ref="teacherRail" class="teacher-rail">
            <article v-for="(tutor, index) in displayTutors" :key="tutor.userId" class="teacher-card surface-card">
              <div class="teacher-cover">
                <img :src="tutor.avatar" :alt="tutor.displayName" @error="handleTutorImageError($event, index)" />
                <span class="teacher-badge">{{ tutor.ratePerHour || '认证' }} /时</span>
                <span v-if="isStarTutor(tutor)" class="teacher-star">星级教师</span>
              </div>
              <div class="teacher-body">
                <div class="teacher-name-row">
                  <h3>{{ tutor.displayName }}</h3>
                  <span class="teacher-city">{{ tutor.city || home.city }}</span>
                </div>
                <p class="teacher-meta">{{ tutor.subjectTags.join(' / ') }} · {{ tutor.experienceYears }}年教龄</p>
                <p class="teacher-edu">{{ tutor.education }}</p>
                <div class="teacher-highlights">
                  <span v-for="item in tutor.highlights.slice(0, 2)" :key="item" class="teacher-pill">{{ item }}</span>
                </div>
                <button class="teacher-btn" type="button" @click="goRoute('/student/tutors')">查看详情</button>
              </div>
            </article>
          </div>
        </section>

        <section class="trust-section">
          <div class="trust-panel">
            <div class="trust-copy">
              <span class="section-tag section-tag-light">创智星球 · 值得信赖的家教平台</span>
              <h2>多重保障，让每一次选择都更安心</h2>
              <p>从老师审核、课堂记录到账户安全和售后服务，我们把家长最关心的环节做成可追踪的服务链路。</p>
              <div class="trust-badges">
                <span v-for="badge in trustBadges" :key="badge" class="trust-badge">{{ badge }}</span>
              </div>
            </div>

            <div class="proof-card">
              <div class="proof-kicker">家长真实反馈</div>
              <p class="proof-quote">“{{ parentProofs[activeProofIndex]!.quote }}”</p>
              <div class="proof-author">来自 {{ parentProofs[activeProofIndex]!.author }}</div>
              <div class="proof-dots">
                <span
                  v-for="(_, index) in parentProofs"
                  :key="index"
                  :class="{ active: index === activeProofIndex }"
                ></span>
              </div>
            </div>
          </div>
        </section>

        <section class="bottom-metrics surface-card">
          <article v-for="item in platformMetrics" :key="`${item.label}-footer`" class="bottom-metric">
            <strong>{{ item.value }}</strong>
            <span>{{ item.label }}</span>
          </article>
        </section>
      </div>
    </main>

    <HomeFooter :links="home.footerLinks" />
  </div>
</template>

<style scoped>
:global(body),
:global(#app) {
  background:
    radial-gradient(circle at 0% 18%, rgba(99, 145, 255, 0.22), transparent 30%),
    radial-gradient(circle at 100% 14%, rgba(39, 201, 183, 0.18), transparent 26%),
    linear-gradient(180deg, #f7faff 0%, #eef4ff 42%, #f6fbff 100%);
}

.home-shell {
  position: relative;
  min-height: 100vh;
  margin-top: -18px;
  padding-top: 18px;
  background:
    radial-gradient(circle at 0% 18%, rgba(99, 145, 255, 0.22), transparent 30%),
    radial-gradient(circle at 100% 14%, rgba(39, 201, 183, 0.18), transparent 26%),
    linear-gradient(180deg, #f7faff 0%, #eef4ff 42%, #f6fbff 100%);
}

.page-main {
  position: relative;
  overflow: hidden;
  padding: 0 0 56px;
}

.landing {
  width: min(1520px, calc(100% - 72px));
  position: relative;
  display: grid;
  gap: 32px;
  padding-top: 18px;
}

.surface-card {
  position: relative;
  border-radius: 30px;
  border: 1px solid rgba(80, 112, 195, 0.12);
  background: rgba(255, 255, 255, 0.92);
  box-shadow:
    0 24px 60px rgba(51, 78, 146, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.7);
}

.hero-panel {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(0, 1.08fr);
  gap: 22px;
  padding: 40px;
}

.hero-copy {
  display: grid;
  align-content: start;
  gap: 26px;
  padding-right: 12px;
}

.hero-badge,
.hero-bottom,
.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  align-items: center;
}

.hero-badge {
  width: fit-content;
  padding: 10px 16px;
  border-radius: 999px;
  background: rgba(41, 99, 235, 0.08);
  color: #4163ab;
  font-size: 13px;
  font-weight: 700;
}

.hero-divider {
  width: 1px;
  height: 14px;
  background: rgba(65, 99, 171, 0.22);
}

.hero-title {
  margin: 0;
  max-width: 9.2em;
  display: grid;
  gap: 12px;
  color: #0d1b48;
  font-size: clamp(40px, 4.2vw, 72px);
  line-height: 1.02;
  letter-spacing: -0.05em;
}

.hero-title span {
  display: block;
}

.hero-title span:last-child {
  font-size: 0.82em;
  white-space: nowrap;
}

.hero-accent {
  color: #3b82f6;
  font-style: normal;
}

.hero-subtitle {
  margin: 0;
  max-width: 560px;
  color: #617299;
  font-size: 18px;
  line-height: 1.9;
}

.hero-primary,
.hero-secondary,
.link-btn,
.teacher-btn,
.text-action {
  cursor: pointer;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    background 0.2s ease,
    color 0.2s ease;
}

.hero-primary,
.hero-secondary,
.teacher-btn {
  height: 52px;
  min-width: 168px;
  padding: 0 28px;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 800;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.hero-primary {
  border: 0;
  color: #fff;
  background: linear-gradient(135deg, #19c2bf, #2d62f2);
  box-shadow: 0 20px 38px rgba(43, 98, 234, 0.24);
}

.hero-secondary {
  border: 1px solid rgba(63, 96, 176, 0.14);
  color: #2f4f9f;
  background: #fff;
}

.hero-primary:hover,
.hero-secondary:hover,
.teacher-btn:hover,
.text-action:hover {
  transform: translateY(-1px);
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.service-card {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 14px;
  padding: 20px;
  border: 1px solid rgba(76, 109, 190, 0.12);
  border-radius: 24px;
  background: linear-gradient(180deg, #ffffff, #f8fbff);
  text-align: left;
  cursor: pointer;
}

.service-icon {
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  border-radius: 18px;
  background: linear-gradient(135deg, #edf4ff, #e6fffb);
  color: #2e61f0;
  font-size: 20px;
  font-weight: 900;
}

.service-text {
  display: grid;
  gap: 6px;
}

.service-title {
  color: #18306d;
  font-size: 18px;
  font-weight: 800;
}

.service-desc {
  color: #6b7da6;
  font-size: 13px;
  line-height: 1.7;
}

.service-link {
  color: #19a7c0;
  font-size: 13px;
  font-weight: 700;
}

.hero-bottom {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.hero-bottom-item {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 12px;
  align-items: start;
  padding: 14px 0;
}

.hero-bottom-icon {
  position: relative;
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 12px;
  background: linear-gradient(135deg, #eef5ff, #eafcff);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.75);
}

.hero-bottom-icon span,
.hero-bottom-icon::before,
.hero-bottom-icon::after {
  content: '';
  position: absolute;
}

.hero-bottom-icon-badge::before {
  width: 18px;
  height: 18px;
  border: 2px solid #3a74f2;
  border-radius: 50%;
}

.hero-bottom-icon-badge::after {
  bottom: 6px;
  width: 10px;
  height: 8px;
  background: #3a74f2;
  clip-path: polygon(50% 100%, 0 0, 100% 0);
}

.hero-bottom-icon-spark::before {
  width: 16px;
  height: 16px;
  border-radius: 5px;
  background: linear-gradient(135deg, #19c2bf, #2d62f2);
  transform: rotate(45deg);
}

.hero-bottom-icon-spark span {
  width: 10px;
  height: 10px;
  background: #fff;
  clip-path: polygon(50% 0, 62% 36%, 100% 50%, 62% 64%, 50% 100%, 38% 64%, 0 50%, 38% 36%);
}

.hero-bottom-icon-shield::before {
  width: 18px;
  height: 20px;
  background: linear-gradient(135deg, #5c8dff, #2d62f2);
  clip-path: polygon(50% 0, 90% 14%, 90% 54%, 50% 100%, 10% 54%, 10% 14%);
}

.hero-bottom-icon-shield::after {
  width: 7px;
  height: 4px;
  border-left: 2px solid #fff;
  border-bottom: 2px solid #fff;
  transform: rotate(-45deg);
  margin-top: 1px;
}

.hero-bottom-copy {
  display: grid;
  gap: 4px;
}

.hero-bottom-title {
  color: #142a61;
  font-size: 15px;
  font-weight: 800;
  line-height: 1.25;
}

.hero-bottom-desc {
  color: #7d8cad;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.5;
}

.hero-visual {
  display: grid;
  align-self: start;
  min-width: 0;
}

.visual-frame {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  align-self: start;
  gap: 20px;
  min-width: 0;
  padding: 26px;
  border-radius: 30px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(245, 249, 255, 0.96)),
    linear-gradient(135deg, rgba(36, 99, 235, 0.08), rgba(25, 194, 191, 0.08));
  border: 1px solid rgba(80, 112, 195, 0.1);
}

.visual-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.visual-title {
  color: #12265f;
  font-size: 30px;
  font-weight: 800;
}

.visual-kicker {
  margin-top: 4px;
  color: #6e7ea6;
  font-size: 13px;
}

.visual-chip {
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(53, 111, 235, 0.09);
  color: #2e61f0;
  font-size: 12px;
  font-weight: 800;
}

.classroom-board {
  display: block;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  overflow: hidden;
}

.teacher-stage {
  position: relative;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  overflow: hidden;
  border-radius: 28px;
  background: linear-gradient(180deg, #d9e7ff 0%, #f3f7ff 100%);
  height: clamp(360px, 30vw, 500px);
  min-height: 0;
}

.teacher-stage-swiper,
.teacher-stage :deep(.swiper),
.teacher-stage :deep(.swiper-wrapper),
.teacher-stage :deep(.swiper-slide),
.hero-slide-button {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  height: 100%;
}

.hero-slide-button {
  display: block;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
}

.teacher-photo {
  display: block;
  width: 100%;
  max-width: 100%;
  height: 100%;
  min-height: 0;
  object-fit: contain;
  object-position: center;
  background: linear-gradient(180deg, #e3edff 0%, #f6f9ff 100%);
}

.teacher-float {
  position: absolute;
  top: 24px;
  right: 24px;
  width: 110px;
  height: 146px;
  padding: 8px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 20px 32px rgba(33, 54, 112, 0.18);
}

.teacher-float-avatar {
  width: 100%;
  height: 100%;
  border-radius: 16px;
  object-fit: cover;
}

.video-toolbar {
  position: absolute;
  left: 18px;
  right: 18px;
  bottom: 56px;
  display: flex;
  justify-content: space-around;
  align-items: center;
  padding: 14px 18px;
  border-radius: 18px;
  background: rgba(16, 27, 59, 0.82);
  color: #fff;
  font-size: 13px;
}

.teacher-stage-pagination {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 18px;
  z-index: 3;
  display: flex;
  justify-content: center;
  gap: 8px;
}

.teacher-stage :deep(.hero-pagination-bullet) {
  width: 9px;
  height: 9px;
  margin: 0 !important;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 6px 16px rgba(17, 32, 78, 0.16);
  opacity: 1;
  transition: width 0.2s ease, background-color 0.2s ease, transform 0.2s ease;
}

.teacher-stage :deep(.hero-pagination-bullet-active) {
  width: 26px;
  background: #ffffff;
  transform: translateY(-1px);
}

.link-btn,
.text-action {
  padding: 0;
  border: 0;
  background: transparent;
  color: #2d62f2;
  font-size: 14px;
  font-weight: 800;
}

.metrics-bar,
.bottom-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0;
  padding: 14px 10px;
}

.metric-card,
.bottom-metric {
  display: grid;
  gap: 6px;
  justify-items: center;
  padding: 18px 12px;
}

.metric-card + .metric-card,
.bottom-metric + .bottom-metric {
  border-left: 1px solid rgba(80, 112, 195, 0.1);
}

.metric-value,
.bottom-metric strong {
  color: #1b4dcf;
  font-size: 34px;
  font-weight: 800;
  letter-spacing: -0.04em;
}

.metric-label,
.bottom-metric span {
  color: #17326c;
  font-size: 16px;
  font-weight: 700;
}

.metric-note {
  color: #7f8fb4;
  font-size: 13px;
}

.ai-section {
  display: grid;
  gap: 26px;
  padding: 30px;
}

.section-intro,
.section-head {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
}

.section-intro {
  align-items: start;
  flex-direction: column;
}

.section-tag {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  width: fit-content;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.08);
  color: #2d62f2;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.section-tag-light {
  background: rgba(255, 255, 255, 0.12);
  color: #d7e3ff;
}

.section-intro h2,
.section-head h2,
.trust-copy h2 {
  margin: 10px 0 0;
  color: #101f50;
  font-size: clamp(28px, 3vw, 42px);
  line-height: 1.15;
  letter-spacing: -0.04em;
}

.section-intro p,
.trust-copy p {
  margin: 12px 0 0;
  color: #6a7ca3;
  font-size: 16px;
  line-height: 1.85;
  max-width: 780px;
}

.ai-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.ai-card {
  display: grid;
  gap: 14px;
  min-height: 320px;
  padding: 22px;
  border-radius: 24px;
  background: linear-gradient(180deg, #ffffff, #f8fbff);
  border: 1px solid rgba(80, 112, 195, 0.1);
}

.ai-index {
  color: #2d62f2;
  font-size: 24px;
  font-weight: 900;
}

.ai-card h3 {
  margin: 0;
  color: #12255b;
  font-size: 22px;
}

.ai-card p {
  margin: 0;
  color: #7282a7;
  font-size: 14px;
  line-height: 1.75;
}

.ai-points {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.ai-point {
  padding: 8px 12px;
  border-radius: 999px;
  background: #edf3ff;
  color: #3a5eaa;
  font-size: 12px;
  font-weight: 700;
}

.mini-chart {
  margin-top: auto;
  height: 120px;
  border-radius: 20px;
  background:
    linear-gradient(180deg, rgba(59, 130, 246, 0.06), rgba(59, 130, 246, 0)),
    linear-gradient(0deg, rgba(64, 113, 216, 0.08) 1px, transparent 1px);
  background-size: 100% 100%, 100% 22px;
  position: relative;
  overflow: hidden;
}

.mini-chart span {
  position: absolute;
  inset: 0;
}

.mini-chart span::before {
  content: '';
  position: absolute;
  inset: 18px 14px;
  background: linear-gradient(90deg, transparent 0%, transparent 5%, #2d62f2 5%, #2d62f2 10%, transparent 10%);
}

.chart-1 span::before {
  clip-path: polygon(0% 72%, 18% 66%, 35% 76%, 54% 38%, 74% 48%, 100% 22%, 100% 34%, 74% 60%, 54% 50%, 35% 88%, 18% 78%, 0% 84%);
}

.chart-2 span::before {
  clip-path: polygon(0% 62%, 20% 64%, 40% 58%, 60% 48%, 80% 36%, 100% 28%, 100% 40%, 80% 48%, 60% 60%, 40% 72%, 20% 78%, 0% 74%);
}

.chart-3 span::before {
  clip-path: polygon(0% 70%, 20% 68%, 38% 62%, 58% 54%, 76% 46%, 100% 34%, 100% 46%, 76% 58%, 58% 66%, 38% 74%, 20% 80%, 0% 82%);
}

.chart-4 span::before {
  clip-path: polygon(0% 82%, 18% 74%, 36% 68%, 54% 54%, 72% 46%, 100% 18%, 100% 30%, 72% 58%, 54% 66%, 36% 80%, 18% 86%, 0% 92%);
}

.teachers-section {
  display: grid;
  gap: 20px;
}

.section-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.text-action {
  align-self: center;
}

.rail-btn {
  width: 40px;
  height: 40px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(80, 112, 195, 0.14);
  color: #2d62f2;
  font-size: 18px;
  font-weight: 800;
  box-shadow: 0 12px 28px rgba(51, 78, 146, 0.08);
}

.teacher-rail {
  display: flex;
  gap: 16px;
  overflow-x: auto;
  padding-bottom: 8px;
  scroll-snap-type: x proximity;
  scrollbar-width: thin;
  scrollbar-color: rgba(45, 98, 242, 0.35) transparent;
}

.teacher-rail::-webkit-scrollbar {
  height: 8px;
}

.teacher-rail::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(45, 98, 242, 0.28);
}

.teacher-card {
  overflow: hidden;
  flex: 0 0 280px;
  scroll-snap-align: start;
}

.teacher-cover {
  position: relative;
  aspect-ratio: 0.88;
  overflow: hidden;
  background: linear-gradient(180deg, #e6efff, #f7fbff);
}

.teacher-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.45s ease;
}

.teacher-card:hover .teacher-cover img {
  transform: scale(1.04);
}

.teacher-badge {
  position: absolute;
  left: 14px;
  bottom: 14px;
  padding: 7px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: #2d62f2;
  font-size: 12px;
  font-weight: 800;
}

.teacher-star {
  position: absolute;
  top: 14px;
  left: 14px;
  padding: 7px 10px;
  border-radius: 999px;
  background: rgba(29, 78, 216, 0.92);
  color: #fff;
  font-size: 12px;
  font-weight: 800;
}

.teacher-body {
  display: grid;
  gap: 10px;
  padding: 18px;
}

.teacher-name-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.teacher-name-row h3 {
  margin: 0;
  color: #13295f;
  font-size: 22px;
}

.teacher-city,
.teacher-meta,
.teacher-edu {
  color: #7485aa;
  font-size: 13px;
}

.teacher-meta,
.teacher-edu {
  margin: 0;
  line-height: 1.7;
}

.teacher-highlights {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.teacher-pill {
  padding: 7px 10px;
  border-radius: 999px;
  background: #edf3ff;
  color: #3c5faa;
  font-size: 12px;
  font-weight: 700;
}

.teacher-btn {
  justify-self: start;
  border: 1px solid rgba(63, 96, 176, 0.12);
  color: #2d62f2;
  background: #fff;
}

.trust-section {
  margin-top: 4px;
}

.trust-panel {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(320px, 420px);
  gap: 20px;
  padding: 34px;
  border-radius: 34px;
  background: linear-gradient(120deg, #1d4ed8 0%, #2148a7 48%, #1f3d95 100%);
  box-shadow: 0 28px 60px rgba(29, 78, 216, 0.24);
}

.trust-copy h2,
.trust-copy p {
  color: #fff;
}

.trust-copy p {
  color: rgba(232, 240, 255, 0.88);
}

.trust-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 22px;
}

.trust-badge {
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  color: #f3f7ff;
  font-size: 13px;
  font-weight: 700;
}

.proof-card {
  display: grid;
  align-content: start;
  gap: 14px;
  padding: 26px;
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.96);
  color: #162a61;
}

.proof-kicker {
  color: #2d62f2;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.proof-quote {
  margin: 0;
  min-height: 120px;
  color: #17306b;
  font-size: 24px;
  line-height: 1.6;
}

.proof-author {
  color: #7f8fb4;
  font-size: 14px;
}

.proof-dots {
  display: flex;
  gap: 8px;
}

.proof-dots span {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: rgba(45, 98, 242, 0.18);
}

.proof-dots span.active {
  width: 24px;
  background: #2d62f2;
}

@media (max-width: 1280px) {
  .hero-panel,
  .trust-panel {
    grid-template-columns: 1fr;
  }

  .hero-copy {
    padding-right: 0;
  }

  .ai-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .page-main {
    padding-top: 0;
  }

  .hero-panel,
  .ai-section,
  .trust-panel {
    padding: 24px;
  }

  .service-grid,
  .metrics-bar,
  .bottom-metrics,
  .ai-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .metric-card:nth-child(3),
  .bottom-metric:nth-child(3) {
    border-left: 0;
  }
}

@media (max-width: 720px) {
  .landing {
    width: min(100%, calc(100% - 24px));
    gap: 20px;
    padding-top: 18px;
  }

  .hero-panel {
    padding: 18px;
    border-radius: 24px;
  }

  .hero-title {
    max-width: none;
    font-size: 34px;
  }

  .hero-title span:last-child {
    white-space: normal;
  }

  .hero-subtitle {
    font-size: 15px;
  }

  .service-grid,
  .metrics-bar,
  .bottom-metrics,
  .ai-grid,
  .hero-bottom {
    grid-template-columns: 1fr;
  }

  .visual-title,
  .teacher-name-row h3 {
    font-size: 22px;
  }

  .visual-head,
  .section-head,
  .teacher-name-row {
    align-items: start;
    flex-direction: column;
  }

  .teacher-stage {
    height: 300px;
  }

  .teacher-float {
    top: 16px;
    right: 16px;
    width: 88px;
    height: 118px;
  }

  .video-toolbar {
    left: 12px;
    right: 12px;
    bottom: 44px;
    padding: 12px 14px;
    font-size: 12px;
  }

  .teacher-stage-pagination {
    bottom: 14px;
  }

  .teacher-card {
    flex-basis: 84vw;
  }

  .proof-quote {
    min-height: auto;
    font-size: 19px;
  }

  .metric-card + .metric-card,
  .bottom-metric + .bottom-metric {
    border-left: 0;
    border-top: 1px solid rgba(80, 112, 195, 0.1);
  }
}
</style>
