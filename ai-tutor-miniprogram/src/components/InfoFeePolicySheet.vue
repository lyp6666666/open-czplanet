<template>
  <view v-if="open" class="sheet-mask" @click.self="$emit('close')">
    <view class="sheet-card">
      <view class="sheet-head">
        <text class="sheet-badge">平台说明</text>
        <button class="sheet-close" @click="$emit('close')">×</button>
      </view>
      <text class="sheet-title">{{ title }}</text>
      <text class="sheet-intro">{{ intro }}</text>

      <view class="section value">
        <text class="section-title">你会得到什么</text>
        <view v-for="item in valuePoints" :key="item" class="point">
          <text class="dot"></text>
          <text class="point-text">{{ item }}</text>
        </view>
      </view>

      <view class="section plain">
        <text class="section-title">{{ refundTitle }}</text>
        <text class="section-copy">{{ refundCopy }}</text>
      </view>

      <view class="section alert">
        <text class="section-title">重要提醒</text>
        <view v-for="item in compliancePoints" :key="item" class="point">
          <text class="dot alert-dot"></text>
          <text class="point-text">{{ item }}</text>
        </view>
      </view>

      <view class="section pledge">
        <text class="section-title">平台承诺</text>
        <view class="pill-row">
          <text class="pill">订单可追踪</text>
          <text class="pill">记录可核实</text>
          <text class="pill">符合规则可申请处理</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
  open: boolean;
  viewerRole: 'teacher' | 'student';
}>();

defineEmits<{
  (e: 'close'): void;
}>();

const title = computed(() =>
  props.viewerRole === 'teacher' ? '为什么要先支付信息费？' : '为什么平台要先收取信息费？',
);

const intro = computed(() =>
  props.viewerRole === 'teacher'
    ? '信息费用于确认真实合作意向，让你在投入更多时间前，先和真实需求进入受平台保障的沟通流程。'
    : '平台通过信息费机制确认真实合作意向，帮助双方更高效地进入后续沟通，并保留必要记录保障双方权益。',
);

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
);

const refundTitle = computed(() => (props.viewerRole === 'teacher' ? '如果沟通未成立' : '如果合作没有继续'));

const refundCopy = computed(() =>
  props.viewerRole === 'teacher'
    ? '支付后如双方在详细沟通后未能达成合作，教师可按平台规则申请退款；平台会结合沟通记录、合作状态及规则进行处理。'
    : '如果双方在详细沟通后未能继续合作，平台会结合沟通记录、合作状态与规则处理后续信息费问题。学生同样需要遵守平台规则，避免绕开平台私下成交。',
);

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
);
</script>

<style lang="scss" scoped>
.sheet-mask {
  position: fixed;
  inset: 0;
  z-index: 90;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding: 24px 12px calc(24px + env(safe-area-inset-bottom));
  background:
    radial-gradient(circle at top, rgba(199, 240, 226, 0.18), transparent 26%),
    rgba(16, 24, 32, 0.42);
  backdrop-filter: blur(8px);
}

.sheet-card {
  width: 100%;
  max-height: 82vh;
  padding: 20px 18px 22px;
  border-radius: 24px;
  background: linear-gradient(180deg, rgba(247, 252, 250, 0.98), rgba(255, 255, 255, 0.98));
  box-shadow: 0 24px 56px rgba(16, 24, 32, 0.18);
  overflow: auto;
  box-sizing: border-box;
}

.sheet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sheet-badge {
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(15, 118, 110, 0.1);
  color: #0f766e;
  font-size: 12px;
  line-height: 28px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.sheet-close {
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: 999px;
  background: rgba(16, 24, 32, 0.06);
  color: #12212a;
  font-size: 22px;
  line-height: 34px;
  padding: 0;
}

.sheet-close::after {
  display: none;
}

.sheet-title {
  display: block;
  margin-top: 14px;
  color: #12212a;
  font-size: 24px;
  line-height: 1.3;
  font-weight: 900;
}

.sheet-intro {
  display: block;
  margin-top: 10px;
  color: #52636d;
  font-size: 14px;
  line-height: 1.75;
}

.section {
  margin-top: 16px;
  padding: 16px;
  border-radius: 20px;
}

.section.value {
  background: linear-gradient(135deg, rgba(220, 244, 237, 0.95), rgba(244, 251, 248, 0.95));
}

.section.plain {
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(16, 24, 32, 0.08);
}

.section.alert {
  background: linear-gradient(180deg, rgba(255, 243, 234, 0.96), rgba(255, 250, 246, 0.96));
  border: 1px solid rgba(196, 92, 39, 0.16);
}

.section.pledge {
  background: linear-gradient(180deg, rgba(245, 247, 248, 0.94), rgba(255, 255, 255, 0.9));
}

.section-title {
  display: block;
  color: #12212a;
  font-size: 15px;
  font-weight: 900;
}

.section-copy {
  display: block;
  margin-top: 8px;
  color: #52636d;
  font-size: 13px;
  line-height: 1.7;
}

.point {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}

.dot {
  width: 10px;
  height: 10px;
  margin-top: 6px;
  border-radius: 999px;
  flex: 0 0 auto;
  background: #0f766e;
  box-shadow: 0 0 0 4px rgba(15, 118, 110, 0.14);
}

.alert-dot {
  background: #c76d2f;
  box-shadow: 0 0 0 4px rgba(199, 109, 47, 0.12);
}

.point-text {
  color: #21323a;
  font-size: 13px;
  line-height: 1.7;
}

.pill-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.pill {
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(18, 33, 42, 0.08);
  background: rgba(255, 255, 255, 0.92);
  color: #12212a;
  font-size: 12px;
  line-height: 34px;
  font-weight: 800;
}
</style>
