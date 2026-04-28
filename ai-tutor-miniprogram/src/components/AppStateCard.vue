<template>
  <view class="state-card" :class="variantClass">
    <text v-if="eyebrow" class="eyebrow">{{ eyebrow }}</text>
    <text class="title">{{ title }}</text>
    <text v-if="description" class="desc">{{ description }}</text>
    <button v-if="actionText" class="action-btn" :class="buttonClass" @click="$emit('action')">{{ actionText }}</button>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = withDefaults(defineProps<{
  title: string;
  description?: string;
  eyebrow?: string;
  actionText?: string;
  variant?: 'default' | 'error' | 'soft';
}>(), {
  description: '',
  eyebrow: '',
  actionText: '',
  variant: 'default',
});

defineEmits<{
  (e: 'action'): void;
}>();

const variantClass = computed(() => `variant-${props.variant}`);
const buttonClass = computed(() => (props.variant === 'error' ? 'btn-error' : 'btn-default'));
</script>

<style scoped lang="scss">
.state-card {
  padding: 24px 18px;
  border-radius: 18px;
  text-align: center;
  border: 1px solid rgba(31, 35, 41, 0.08);
  background: #ffffff;
  box-shadow: 0 10px 30px rgba(31, 35, 41, 0.06);
}

.variant-error {
  background: #fff8f7;
  border-color: rgba(221, 82, 77, 0.14);
}

.variant-soft {
  background: #f8fbfb;
}

.eyebrow,
.title,
.desc {
  display: block;
}

.eyebrow {
  font-size: 12px;
  color: #8f959e;
  margin-bottom: 8px;
}

.title {
  font-size: 16px;
  font-weight: 900;
  color: #1f2329;
}

.desc {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.7;
  color: #646a73;
}

.action-btn {
  margin-top: 16px;
  height: 40px;
  padding: 0 22px;
  border-radius: 999px;
  border: 0;
  font-size: 13px;
  font-weight: 900;
  line-height: 40px;
}

.action-btn::after {
  border: 0;
}

.btn-default {
  background: #00bebd;
  color: #ffffff;
}

.btn-error {
  background: #dd524d;
  color: #ffffff;
}
</style>
