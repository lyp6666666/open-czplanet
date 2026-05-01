<template>
  <div class="page">
    <section class="card hero">
      <div>
        <div class="title">
          客服配置
        </div>
        <div class="sub">
          用户端右下角客服浮窗会读取这里的联系方式；支持个人微信和企业微信二维码。
        </div>
      </div>
      <div
        class="status"
        :class="{ off: !form.enabled }"
      >
        {{ form.enabled ? '已启用' : '已关闭' }}
      </div>
    </section>

    <section class="card form-card">
      <div class="section-title">
        联系方式
      </div>
      <div class="form-grid">
        <label class="field switch-field">
          <span class="label">启用客服浮窗</span>
          <input
            v-model="form.enabled"
            type="checkbox"
          >
        </label>

        <label class="field">
          <span class="label">微信类型</span>
          <select
            v-model="form.channelType"
            class="input"
          >
            <option value="WECHAT_WORK">企业微信</option>
            <option value="WECHAT_PERSONAL">个人微信</option>
          </select>
        </label>

        <label class="field">
          <span class="label">展示名称</span>
          <input
            v-model.trim="form.displayName"
            class="input"
            placeholder="例如：创智星球客服"
          >
        </label>

        <label class="field">
          <span class="label">{{ form.channelType === 'WECHAT_WORK' ? '企业微信号/名称' : '微信号' }}</span>
          <input
            v-model.trim="form.wechatNo"
            class="input"
            placeholder="例如：ai_tutor_service"
          >
        </label>

        <label class="field">
          <span class="label">QQ 号</span>
          <input
            v-model.trim="form.qqNo"
            class="input"
            placeholder="例如：123456789"
          >
        </label>

        <label class="field">
          <span class="label">服务时间</span>
          <input
            v-model.trim="form.serviceTime"
            class="input"
            placeholder="例如：09:00 - 22:00"
          >
        </label>

        <label class="field field-wide">
          <span class="label">备注文案</span>
          <input
            v-model.trim="form.description"
            class="input"
            placeholder="例如：添加客服时请备注：家长/老师 + 手机号"
          >
        </label>
      </div>

      <div class="upload-box">
        <div>
          <div class="section-title small">
            二维码
          </div>
          <div class="sub">
            建议上传企业微信外部联系人二维码或客服二维码，用户端会直接展示。
          </div>
        </div>
        <div class="qr-preview">
          <img
            v-if="form.qrCodeUrl"
            class="qr-image"
            :src="form.qrCodeUrl"
            alt="客服二维码"
          >
          <div
            v-else
            class="qr-empty"
          >
            暂无二维码
          </div>
        </div>
        <label class="btn btn-muted upload-btn">
          <input
            type="file"
            accept="image/*"
            @change="onPickQrCode"
          >
          选择二维码
        </label>
        <button
          class="btn"
          type="button"
          :disabled="uploading || !pickedFile"
          @click="uploadQrCode"
        >
          {{ uploading ? '上传中...' : '上传二维码' }}
        </button>
      </div>

      <div class="actions">
        <button
          class="btn btn-primary"
          type="button"
          :disabled="saving"
          @click="save"
        >
          {{ saving ? '保存中...' : '保存客服配置' }}
        </button>
        <button
          class="btn btn-muted"
          type="button"
          :disabled="loading"
          @click="load"
        >
          重新加载
        </button>
      </div>

      <div
        v-if="hint"
        class="hint ok"
      >
        {{ hint }}
      </div>
      <div
        v-if="errorText"
        class="hint error"
      >
        {{ errorText }}
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'

import { getCustomerServiceConfig, saveCustomerServiceConfig, uploadCustomerServiceQrCode } from '@/api/customerService'
import type { AdminCustomerServiceConfig } from '@/api/types'

const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const errorText = ref<string | null>(null)
const hint = ref<string | null>(null)
const pickedFile = ref<File | null>(null)
const MAX_UPLOAD_BYTES = 20 * 1024 * 1024

const form = reactive<AdminCustomerServiceConfig>({
  enabled: true,
  channelType: 'WECHAT_WORK',
  displayName: '创智星球客服',
  wechatNo: 'ai_tutor_service',
  qqNo: '123456789',
  qrCodeUrl: null,
  qrCodeObjectKey: null,
  serviceTime: '09:00 - 22:00',
  description: '添加客服时请备注：家长/老师 + 手机号',
})

function applyConfig(config: AdminCustomerServiceConfig) {
  form.enabled = config.enabled !== false
  form.channelType = config.channelType === 'WECHAT_PERSONAL' ? 'WECHAT_PERSONAL' : 'WECHAT_WORK'
  form.displayName = config.displayName || '创智星球客服'
  form.wechatNo = config.wechatNo || ''
  form.qqNo = config.qqNo || ''
  form.qrCodeUrl = config.qrCodeUrl || null
  form.qrCodeObjectKey = config.qrCodeObjectKey || null
  form.serviceTime = config.serviceTime || '09:00 - 22:00'
  form.description = config.description || ''
  form.updateTime = config.updateTime || null
}

function requestErrorText(e: unknown, fallback: string) {
  if (e && typeof e === 'object' && 'message' in e) {
    const msg = String((e as { message?: unknown }).message || '').trim()
    if (msg) return msg
  }
  return fallback
}

async function load() {
  if (loading.value) return
  loading.value = true
  errorText.value = null
  try {
    applyConfig(await getCustomerServiceConfig())
  } catch (e) {
    errorText.value = requestErrorText(e, '客服配置加载失败')
  } finally {
    loading.value = false
  }
}

function validateForm() {
  if (!form.displayName.trim()) return '请输入展示名称'
  if (!form.wechatNo?.trim() && !form.qqNo?.trim() && !form.qrCodeObjectKey) return '请至少配置微信、QQ 或二维码中的一种联系方式'
  if (!form.serviceTime.trim()) return '请输入服务时间'
  return null
}

async function save() {
  if (saving.value) return
  hint.value = null
  errorText.value = null
  const err = validateForm()
  if (err) {
    errorText.value = err
    return
  }
  saving.value = true
  try {
    applyConfig(await saveCustomerServiceConfig(form))
    hint.value = '客服配置已保存'
  } catch (e) {
    errorText.value = requestErrorText(e, '客服配置保存失败')
  } finally {
    saving.value = false
  }
}

function onPickQrCode(e: Event) {
  const input = e.target as HTMLInputElement | null
  pickedFile.value = input?.files?.[0] || null
  hint.value = null
  errorText.value = null
}

async function uploadQrCode() {
  if (uploading.value || !pickedFile.value) return
  if (pickedFile.value.size > MAX_UPLOAD_BYTES) {
    errorText.value = '二维码图片不能超过 20MB'
    return
  }
  uploading.value = true
  hint.value = null
  errorText.value = null
  try {
    applyConfig(await uploadCustomerServiceQrCode(pickedFile.value))
    pickedFile.value = null
    hint.value = '二维码已上传'
  } catch (e) {
    errorText.value = requestErrorText(e, '二维码上传失败')
  } finally {
    uploading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page {
  display: grid;
  gap: 12px;
}

.hero,
.form-card {
  padding: 16px;
}

.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.title {
  font-size: 22px;
  font-weight: 800;
}

.sub {
  margin-top: 6px;
  color: var(--muted);
  font-size: 14px;
}

.status {
  min-width: 86px;
  height: 40px;
  display: grid;
  place-items: center;
  border-radius: 999px;
  color: var(--primary);
  background: rgba(0, 190, 189, 0.1);
  font-weight: 800;
}

.status.off {
  color: var(--muted);
  background: rgba(100, 116, 139, 0.12);
}

.section-title {
  font-size: 16px;
  font-weight: 800;
}

.section-title.small {
  font-size: 14px;
}

.form-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.field {
  display: grid;
  gap: 8px;
}

.switch-field {
  align-content: start;
}

.switch-field input {
  width: 20px;
  height: 20px;
}

.field-wide {
  grid-column: 1 / -1;
}

.label {
  color: var(--muted);
  font-size: 13px;
}

.upload-box {
  margin-top: 16px;
  display: grid;
  grid-template-columns: 1fr 112px auto auto;
  gap: 12px;
  align-items: center;
  padding: 12px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 14px;
}

.qr-preview {
  width: 112px;
  height: 112px;
}

.qr-image,
.qr-empty {
  width: 112px;
  height: 112px;
  border-radius: 12px;
}

.qr-image {
  object-fit: cover;
  border: 1px solid var(--border);
}

.qr-empty {
  display: grid;
  place-items: center;
  color: var(--muted);
  background: rgba(15, 23, 42, 0.04);
  font-size: 13px;
}

.upload-btn {
  position: relative;
  overflow: hidden;
}

.upload-btn input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.actions {
  margin-top: 16px;
  display: flex;
  gap: 10px;
}

.hint {
  margin-top: 12px;
  font-size: 13px;
}

.hint.ok {
  color: var(--primary);
}

.hint.error {
  color: var(--danger);
}

@media (max-width: 980px) {
  .hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .form-grid,
  .upload-box {
    grid-template-columns: 1fr;
  }
}
</style>
