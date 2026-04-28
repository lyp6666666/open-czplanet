<script setup lang="ts">
import { computed } from 'vue'

type ViewerRole = 'teacher' | 'student'

const props = defineProps<{
  open: boolean
  viewerRole: ViewerRole
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const heading = computed(() =>
  props.viewerRole === 'teacher' ? '为什么要先支付信息费？' : '为什么平台要先收取信息费？',
)

const intro = computed(() =>
  props.viewerRole === 'teacher'
    ? '信息费用于确认真实合作意向，让你在投入更多时间前，先和真实需求进入受平台保障的沟通流程。'
    : '平台通过信息费机制确认真实合作意向，帮助双方更高效地进入后续沟通，并保留必要记录保障双方权益。',
)

const valuePoints = computed(() =>
  props.viewerRole === 'teacher'
    ? [
        '先预付信息费，再继续沟通详细需求与合作安排',
        '减少无效对接、虚假试探和反复索要联系方式',
        '沟通记录和订单记录可追踪，出现争议时平台可协助核实',
      ]
    : [
        '帮助真实需求更快进入后续沟通，减少无效打扰',
        '教师支付后，双方才能继续确认详细安排与合作方式',
        '平台保留必要记录，便于后续核实争议与保障双方权益',
      ],
)

const refundTitle = computed(() => (props.viewerRole === 'teacher' ? '如果沟通未成立' : '如果合作没有继续'))

const refundCopy = computed(() =>
  props.viewerRole === 'teacher'
    ? '支付后如双方在详细沟通后未能达成合作，教师可按平台规则申请退款；平台会结合沟通记录、合作状态及规则进行处理。'
    : '如果双方在详细沟通后未能继续合作，平台会结合沟通记录、合作状态与规则处理后续信息费问题。学生同样需要遵守平台规则，避免绕开平台私下成交。',
)

const compliancePoints = computed(() =>
  props.viewerRole === 'teacher'
    ? [
        '合作通过前，双方不得私下索要、交换或泄露微信、电话等联系方式。',
        '若提前泄露联系方式、绕过平台私下成交，平台将不予退还信息费。',
      ]
    : [
        '合作通过前，双方不得交换或泄露微信、电话等联系方式。',
        '若存在绕过平台私下联系、私下成交等行为，平台将按规则处理，相关退款与保障权益可能受影响。',
      ],
)
</script>

<template>
  <div v-if="open" class="overlay" @click.self="emit('close')">
    <section class="sheet card">
      <button class="close-btn" type="button" aria-label="关闭说明" @click="emit('close')">×</button>
      <div class="hero-mark">平台说明</div>
      <h2 class="hero-title">{{ heading }}</h2>
      <p class="hero-copy">{{ intro }}</p>

      <section class="section value-panel">
        <div class="section-title">你会得到什么</div>
        <div class="point-list">
          <div v-for="item in valuePoints" :key="item" class="point">
            <span class="dot" />
            <span>{{ item }}</span>
          </div>
        </div>
      </section>

      <section class="section neutral-panel">
        <div class="section-title">{{ refundTitle }}</div>
        <p class="section-copy">{{ refundCopy }}</p>
      </section>

      <section class="section alert-panel">
        <div class="section-title">重要提醒</div>
        <div class="point-list compact">
          <div v-for="item in compliancePoints" :key="item" class="point">
            <span class="dot alert" />
            <span>{{ item }}</span>
          </div>
        </div>
      </section>

      <section class="section pledge-panel">
        <div class="section-title">平台承诺</div>
        <div class="pledges">
          <div class="pledge">订单可追踪</div>
          <div class="pledge">沟通记录可核实</div>
          <div class="pledge">符合规则可申请处理</div>
        </div>
      </section>
    </section>
  </div>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: grid;
  place-items: center;
  padding: 28px 16px;
  background:
    radial-gradient(circle at top, rgba(192, 230, 218, 0.22), transparent 30%),
    rgba(17, 24, 39, 0.5);
  backdrop-filter: blur(8px);
}

.sheet {
  position: relative;
  width: min(720px, calc(100vw - 24px));
  max-height: min(88vh, 900px);
  overflow: auto;
  padding: 28px;
  border-radius: 28px;
  border: 1px solid rgba(16, 24, 32, 0.08);
  background:
    linear-gradient(180deg, rgba(246, 252, 249, 0.98), rgba(255, 255, 255, 0.98));
  box-shadow: 0 30px 70px rgba(17, 24, 39, 0.18);
}

.close-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 36px;
  height: 36px;
  border: 0;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.06);
  color: #12212a;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
}

.hero-mark {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(15, 118, 110, 0.1);
  color: #0f766e;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.hero-title {
  margin: 14px 0 10px;
  color: #12212a;
  font-size: 30px;
  line-height: 1.18;
  font-weight: 900;
}

.hero-copy {
  margin: 0;
  color: #52636d;
  font-size: 15px;
  line-height: 1.75;
}

.section {
  margin-top: 18px;
  padding: 18px;
  border-radius: 22px;
}

.value-panel {
  background: linear-gradient(135deg, rgba(220, 244, 237, 0.95), rgba(244, 251, 248, 0.95));
}

.neutral-panel {
  border: 1px solid rgba(18, 33, 42, 0.08);
  background: rgba(255, 255, 255, 0.86);
}

.alert-panel {
  border: 1px solid rgba(191, 90, 36, 0.16);
  background: linear-gradient(180deg, rgba(255, 243, 234, 0.96), rgba(255, 250, 246, 0.96));
}

.pledge-panel {
  background: linear-gradient(180deg, rgba(245, 247, 248, 0.94), rgba(255, 255, 255, 0.9));
}

.section-title {
  color: #12212a;
  font-size: 16px;
  font-weight: 900;
}

.section-copy {
  margin: 8px 0 0;
  color: #52636d;
  font-size: 14px;
  line-height: 1.7;
}

.point-list {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.point-list.compact {
  gap: 8px;
}

.point {
  display: grid;
  grid-template-columns: 10px 1fr;
  gap: 10px;
  align-items: start;
  color: #21323a;
  font-size: 14px;
  line-height: 1.7;
}

.dot {
  width: 10px;
  height: 10px;
  margin-top: 8px;
  border-radius: 999px;
  background: #0f766e;
  box-shadow: 0 0 0 4px rgba(15, 118, 110, 0.14);
}

.dot.alert {
  background: #c76d2f;
  box-shadow: 0 0 0 4px rgba(199, 109, 47, 0.12);
}

.pledges {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.pledge {
  display: inline-flex;
  align-items: center;
  min-height: 38px;
  padding: 0 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(18, 33, 42, 0.08);
  color: #12212a;
  font-size: 13px;
  font-weight: 800;
}

@media (max-width: 720px) {
  .sheet {
    padding: 22px 18px;
    border-radius: 24px;
  }

  .hero-title {
    font-size: 24px;
  }
}
</style>
