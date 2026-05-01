<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import { getCustomerServiceConfig } from '@/api/customerService'
import type { CustomerServiceConfigResp } from '@/api/customerService'
import { customerServiceConfig } from '@/constants/customerService'
import { useToastStore } from '@/stores/toast'

const route = useRoute()
const toast = useToastStore()
const open = ref(false)
const serviceRoot = ref<HTMLElement | null>(null)
const config = ref<CustomerServiceConfigResp>({ ...customerServiceConfig })
const position = ref<{ x: number; y: number } | null>(null)
const dragging = ref(false)
const suppressNextClick = ref(false)

let dragStart:
  | {
      pointerId: number
      startX: number
      startY: number
      originX: number
      originY: number
    }
  | null = null

const isLifted = computed(() => route.path.startsWith('/chat') || route.path.startsWith('/pay/'))
const visible = computed(() => config.value.enabled !== false)
const channelLabel = computed(() => (config.value.channelType === 'WECHAT_WORK' ? '企业微信' : '微信'))
const copyWechatSuccessText = computed(() => `${channelLabel.value}已复制`)
const rootStyle = computed(() => {
  if (!position.value) return undefined
  return {
    left: `${position.value.x}px`,
    top: `${position.value.y}px`,
    right: 'auto',
    bottom: 'auto',
  }
})

async function copyText(text: string, successText: string) {
  const value = text.trim()
  if (!value) {
    toast.show('暂无可复制内容', 'info')
    return
  }

  try {
    await navigator.clipboard.writeText(value)
    toast.show(successText, 'success')
  } catch {
    toast.show('复制失败，请手动复制', 'error')
  }
}

async function loadConfig() {
  try {
    const remote = await getCustomerServiceConfig()
    config.value = {
      ...customerServiceConfig,
      ...remote,
      channelType: remote.channelType === 'WECHAT_PERSONAL' ? 'WECHAT_PERSONAL' : 'WECHAT_WORK',
    }
  } catch {
    config.value = { ...customerServiceConfig }
  }
}

function togglePanel() {
  if (suppressNextClick.value) {
    suppressNextClick.value = false
    return
  }
  open.value = !open.value
}

function closePanel() {
  open.value = false
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') closePanel()
}

function clampPosition(x: number, y: number) {
  const rect = serviceRoot.value?.getBoundingClientRect()
  const width = rect?.width || 92
  const height = rect?.height || 56
  const margin = 12
  const maxX = Math.max(margin, window.innerWidth - width - margin)
  const maxY = Math.max(margin, window.innerHeight - height - margin)

  return {
    x: Math.min(Math.max(margin, x), maxX),
    y: Math.min(Math.max(margin, y), maxY),
  }
}

function onDragStart(event: PointerEvent) {
  if (event.button !== 0) return

  const rect = serviceRoot.value?.getBoundingClientRect()
  if (!rect) return

  dragStart = {
    pointerId: event.pointerId,
    startX: event.clientX,
    startY: event.clientY,
    originX: rect.left,
    originY: rect.top,
  }
  dragging.value = false
  window.addEventListener('pointermove', onDragMove)
  window.addEventListener('pointerup', onDragEnd)
}

function onDragMove(event: PointerEvent) {
  if (!dragStart || event.pointerId !== dragStart.pointerId) return

  const deltaX = event.clientX - dragStart.startX
  const deltaY = event.clientY - dragStart.startY
  if (!dragging.value && Math.hypot(deltaX, deltaY) < 5) return

  dragging.value = true
  suppressNextClick.value = true
  position.value = clampPosition(dragStart.originX + deltaX, dragStart.originY + deltaY)
}

function onDragEnd(event: PointerEvent) {
  if (dragStart && event.pointerId === dragStart.pointerId) {
    dragStart = null
  }
  window.removeEventListener('pointermove', onDragMove)
  window.removeEventListener('pointerup', onDragEnd)

  window.setTimeout(() => {
    dragging.value = false
  }, 0)
}

function onResize() {
  if (!position.value) return
  position.value = clampPosition(position.value.x, position.value.y)
}

onMounted(() => {
  void loadConfig()
  window.addEventListener('keydown', onKeydown)
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
  window.removeEventListener('resize', onResize)
  window.removeEventListener('pointermove', onDragMove)
  window.removeEventListener('pointerup', onDragEnd)
})
</script>

<template>
  <div
    v-if="visible"
    ref="serviceRoot"
    class="customer-service"
    :class="{ lifted: isLifted, open }"
    :style="rootStyle"
  >
    <Transition name="service-panel">
      <div
        v-if="open"
        class="service-panel"
        role="dialog"
        aria-label="联系客服"
      >
        <div class="panel-head">
          <div>
            <div class="panel-kicker">
              人工服务
            </div>
            <div class="panel-title">
              联系客服
            </div>
          </div>
          <button
            class="icon-btn"
            type="button"
            aria-label="关闭客服面板"
            @click="closePanel"
          >
            <svg
              viewBox="0 0 24 24"
              aria-hidden="true"
            >
              <path d="M6 6l12 12M18 6L6 18" />
            </svg>
          </button>
        </div>

        <div class="contact-list">
          <div class="contact-row">
            <div
              class="contact-icon wechat"
              aria-hidden="true"
            >
              <svg viewBox="0 0 24 24">
                <path d="M10.6 5.3c-4.1 0-7.4 2.6-7.4 5.9 0 1.9 1.1 3.6 2.8 4.7l-.6 2.1 2.4-1.2c.9.3 1.8.4 2.8.4 4.1 0 7.4-2.6 7.4-5.9s-3.3-6-7.4-6Z" />
                <path d="M14.4 12.9c3.4.4 6 2.6 6 5.3 0 1.4-.7 2.7-1.9 3.7l.5 1.7-2-1c-.8.3-1.7.4-2.6.4-2.9 0-5.4-1.6-6.1-3.8" />
              </svg>
            </div>
            <div class="contact-main">
              <div class="contact-label">
                {{ channelLabel }}
              </div>
              <div class="contact-value">
                {{ config.displayName }}
              </div>
              <div
                v-if="config.wechatNo"
                class="contact-note"
              >
                {{ config.wechatNo }}
              </div>
            </div>
            <button
              v-if="config.wechatNo"
              class="copy-btn"
              type="button"
              @click="copyText(config.wechatNo || '', copyWechatSuccessText)"
            >
              复制
            </button>
          </div>

          <div
            v-if="config.qrCodeUrl"
            class="qr-card"
          >
            <img
              class="qr-image"
              :src="config.qrCodeUrl"
              :alt="`${channelLabel}二维码`"
            >
            <div class="qr-copy">
              <div class="qr-title">
                扫码添加{{ channelLabel }}
              </div>
              <div class="qr-sub">
                {{ config.description || '添加客服时请备注身份与手机号' }}
              </div>
            </div>
          </div>

          <div
            v-if="config.qqNo"
            class="contact-row"
          >
            <div
              class="contact-icon qq"
              aria-hidden="true"
            >
              <svg viewBox="0 0 24 24">
                <path d="M12 3.2c-2.8 0-5 2.5-5 6 0 1.5-.5 2.7-1.2 3.9-.7 1-.9 2.1-.3 2.7.4.4 1.1.4 1.8.1.6 2.1 2.4 3.6 4.7 3.6s4.1-1.5 4.7-3.6c.7.3 1.4.3 1.8-.1.6-.6.4-1.7-.3-2.7-.7-1.2-1.2-2.4-1.2-3.9 0-3.5-2.2-6-5-6Z" />
                <path d="M8.9 20.1c-.7.4-1.3.9-1.1 1.5.2.5 1.9.7 4.2.7s4-.2 4.2-.7c.2-.6-.4-1.1-1.1-1.5" />
              </svg>
            </div>
            <div class="contact-main">
              <div class="contact-label">
                QQ
              </div>
              <div class="contact-value">
                {{ config.qqNo }}
              </div>
            </div>
            <button
              class="copy-btn"
              type="button"
              @click="copyText(config.qqNo || '', 'QQ号已复制')"
            >
              复制
            </button>
          </div>
        </div>

        <div class="service-time">
          服务时间：{{ config.serviceTime }}
        </div>
      </div>
    </Transition>

    <button
      class="float-btn"
      :class="{ dragging }"
      type="button"
      :aria-expanded="open"
      aria-label="联系客服"
      title="点击展开，按住可拖动"
      @pointerdown="onDragStart"
      @click="togglePanel"
    >
      <span
        class="float-icon"
        aria-hidden="true"
      >
        <svg viewBox="0 0 24 24">
          <path d="M4.5 12.5a7.5 7.5 0 0 1 15 0v3.2a2.8 2.8 0 0 1-2.8 2.8h-1.1" />
          <path d="M7.5 12.2h-1a2 2 0 0 0-2 2v.8a2 2 0 0 0 2 2h1v-4.8Z" />
          <path d="M16.5 12.2h1a2 2 0 0 1 2 2v.8a2 2 0 0 1-2 2h-1v-4.8Z" />
          <path d="M10.2 18.5h2.2a1.2 1.2 0 0 0 0-2.4h-1.1" />
        </svg>
      </span>
      <span class="float-text">客服</span>
    </button>
  </div>
</template>

<style scoped>
.customer-service {
  position: fixed;
  right: max(18px, env(safe-area-inset-right));
  bottom: max(22px, env(safe-area-inset-bottom));
  z-index: 1200;
  width: max-content;
}

.customer-service.lifted {
  bottom: max(92px, calc(env(safe-area-inset-bottom) + 92px));
}

.service-panel {
  position: absolute;
  right: 0;
  bottom: calc(100% + 10px);
  width: min(300px, calc(100vw - 32px));
  padding: 14px;
  border: 1px solid rgba(45, 98, 242, 0.14);
  border-radius: 16px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 251, 255, 0.98)),
    #fff;
  box-shadow: 0 24px 60px rgba(34, 64, 132, 0.18);
}

.panel-head,
.contact-row {
  display: flex;
  align-items: center;
}

.panel-head {
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.panel-kicker {
  color: #6f81a8;
  font-size: 12px;
  line-height: 1.2;
}

.panel-title {
  margin-top: 2px;
  color: #13295f;
  font-size: 17px;
  font-weight: 800;
  line-height: 1.2;
}

.icon-btn,
.copy-btn,
.float-btn {
  cursor: pointer;
}

.icon-btn {
  width: 30px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  color: #6f81a8;
  background: rgba(45, 98, 242, 0.08);
}

.icon-btn svg,
.float-icon svg,
.contact-icon svg {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.9;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.contact-list {
  display: grid;
  gap: 9px;
}

.contact-row {
  gap: 10px;
  min-height: 58px;
  padding: 10px;
  border: 1px solid rgba(80, 112, 195, 0.12);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.82);
}

.contact-icon {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  border-radius: 10px;
}

.contact-icon.wechat {
  color: #0f9f7a;
  background: rgba(15, 159, 122, 0.1);
}

.contact-icon.qq {
  color: #2d62f2;
  background: rgba(45, 98, 242, 0.1);
}

.contact-main {
  min-width: 0;
  flex: 1;
}

.contact-label {
  color: #6f81a8;
  font-size: 12px;
  line-height: 1.2;
}

.contact-value {
  margin-top: 3px;
  color: #13295f;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.2;
  overflow-wrap: anywhere;
}

.contact-note {
  margin-top: 3px;
  color: #6f81a8;
  font-size: 12px;
  line-height: 1.2;
  overflow-wrap: anywhere;
}

.qr-card {
  display: grid;
  grid-template-columns: 92px 1fr;
  gap: 10px;
  align-items: center;
  padding: 10px;
  border: 1px solid rgba(80, 112, 195, 0.12);
  border-radius: 12px;
  background: rgba(45, 98, 242, 0.05);
}

.qr-image {
  width: 92px;
  height: 92px;
  object-fit: cover;
  border-radius: 10px;
  background: #fff;
}

.qr-copy {
  min-width: 0;
}

.qr-title {
  color: #13295f;
  font-size: 14px;
  font-weight: 800;
}

.qr-sub {
  margin-top: 5px;
  color: #6f81a8;
  font-size: 12px;
  line-height: 1.45;
}

.copy-btn {
  min-width: 48px;
  height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  color: #2d62f2;
  background: rgba(45, 98, 242, 0.1);
  font-size: 13px;
  font-weight: 700;
  line-height: 30px;
  text-align: center;
}

.service-time {
  margin-top: 12px;
  padding: 8px 10px;
  border-radius: 10px;
  color: #6f81a8;
  background: rgba(45, 98, 242, 0.07);
  font-size: 12px;
}

.float-btn {
  height: 46px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 0 14px 0 12px;
  border-radius: 999px;
  color: #fff;
  background: linear-gradient(135deg, #16b7ad, #2d62f2);
  box-shadow: 0 12px 30px rgba(45, 98, 242, 0.28);
}

.float-icon {
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.float-icon svg {
  width: 22px;
  height: 22px;
  stroke-width: 2;
}

.float-text {
  font-size: 14px;
  font-weight: 800;
  line-height: 1;
}

.service-panel-enter-active,
.service-panel-leave-active {
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;
}

.service-panel-enter-from,
.service-panel-leave-to {
  opacity: 0;
  transform: translateY(8px) scale(0.98);
}

@media (max-width: 720px) {
  .customer-service {
    right: max(12px, env(safe-area-inset-right));
    bottom: max(76px, calc(env(safe-area-inset-bottom) + 76px));
  }

  .customer-service.lifted {
    bottom: max(104px, calc(env(safe-area-inset-bottom) + 104px));
  }

  .float-btn {
    height: 42px;
    padding: 0 12px;
  }
}
</style>
