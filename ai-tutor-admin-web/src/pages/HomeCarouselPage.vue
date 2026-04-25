<template>
  <div class="page">
    <section class="card hero">
      <div>
        <div class="title">首页轮播图</div>
        <div class="sub">中间大图从 MinIO 读取，最多保留 5 张；单张图不自动轮播，单文件上限 20MB。</div>
      </div>
      <div class="count">{{ rows.length }}/5</div>
    </section>

    <section class="card form-card">
      <div class="section-title">上传新轮播图</div>
      <div class="form-grid">
        <label class="field">
          <span class="label">主标题</span>
          <input v-model="form.title" class="input" placeholder="例如：春季提分计划" />
        </label>
        <label class="field">
          <span class="label">副标题</span>
          <input v-model="form.subtitle" class="input" placeholder="例如：覆盖数学、英语、物理等热门科目" />
        </label>
        <label class="field field-wide">
          <span class="label">跳转地址</span>
          <input v-model="form.linkUrl" class="input" placeholder="可选；支持 /guide/tutor 或 https://example.com" />
        </label>
        <label class="field field-wide">
          <span class="label">图片文件</span>
          <input class="input file-input" type="file" accept="image/*" @change="onPickFile" />
        </label>
      </div>
      <div class="upload-row">
        <div class="file-name">{{ fileName }}</div>
        <button class="btn btn-primary" type="button" :disabled="uploading || rows.length >= 5" @click="submit">
          {{ uploading ? '上传中...' : rows.length >= 5 ? '已达上限 5 张' : '上传并加入轮播' }}
        </button>
      </div>
      <div v-if="hint" class="hint ok">{{ hint }}</div>
      <div v-if="errorText" class="hint error">{{ errorText }}</div>
    </section>

    <section class="card list-card">
      <div class="section-title">当前配置</div>
      <div v-if="loading" class="empty">加载中...</div>
      <div v-else-if="rows.length === 0" class="empty">暂未配置轮播图，上传后会显示在首页中间轮播位。</div>
      <div v-else class="list">
        <article v-for="row in rows" :key="row.id" class="item">
          <img class="thumb" :src="row.imageUrl" :alt="row.title" />
          <div class="meta">
            <div class="meta-top">
              <div class="badge">第 {{ row.sortOrder }} 张</div>
              <div class="time">{{ fmtTime(row.updateTime || row.createTime) }}</div>
            </div>
            <div class="meta-title">{{ row.title }}</div>
            <div class="meta-sub">{{ row.subtitle || '未填写副标题' }}</div>
            <div class="meta-link">{{ row.linkUrl || '未配置跳转地址' }}</div>
          </div>
          <div class="actions">
            <button class="btn btn-danger" type="button" :disabled="busyId === row.id" @click="onDelete(row.id)">
              {{ busyId === row.id ? '删除中...' : '删除' }}
            </button>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import { createHomeCarousel, deleteHomeCarousel, listHomeCarousel } from '@/api/homeCarousel'
import type { AdminHomeCarouselItem } from '@/api/types'

const rows = ref<AdminHomeCarouselItem[]>([])
const loading = ref(false)
const uploading = ref(false)
const busyId = ref<number | null>(null)
const errorText = ref<string | null>(null)
const hint = ref<string | null>(null)
const pickedFile = ref<File | null>(null)
const MAX_UPLOAD_BYTES = 20 * 1024 * 1024

const form = reactive({
  title: '',
  subtitle: '',
  linkUrl: '',
})

const fileName = computed(() => pickedFile.value?.name || '尚未选择文件')

function fmtTime(v: string | null | undefined) {
  if (!v) return '-'
  return String(v).replace('T', ' ').slice(0, 16)
}

function resetForm() {
  form.title = ''
  form.subtitle = ''
  form.linkUrl = ''
  pickedFile.value = null
}

function onPickFile(e: Event) {
  const input = e.target as HTMLInputElement | null
  pickedFile.value = input?.files?.[0] || null
  hint.value = null
  errorText.value = null
}

function validateUploadForm() {
  if (!form.title.trim()) {
    return '请输入主标题'
  }
  if (!pickedFile.value) {
    return '请先选择图片文件'
  }
  if (pickedFile.value.size > MAX_UPLOAD_BYTES) {
    return '图片不能超过 20MB，请压缩后重新上传'
  }
  return null
}

function resolveRequestErrorMessage(e: unknown) {
  if (!e || typeof e !== 'object') {
    return '上传失败'
  }
  const error = e as {
    message?: unknown
    response?: {
      status?: number
      data?: {
        message?: unknown
      }
    }
  }
  const apiMessage = error.response?.data?.message
  if (typeof apiMessage === 'string' && apiMessage.trim()) {
    return apiMessage.trim()
  }
  if (error.response?.status === 413) {
    return '上传图片过大，代理层已拒绝请求。请先部署最新 Nginx 配置，再重新上传。'
  }
  if (typeof error.message === 'string' && error.message.trim()) {
    return error.message.trim()
  }
  return '上传失败'
}

async function load() {
  if (loading.value) return
  loading.value = true
  errorText.value = null
  try {
    rows.value = await listHomeCarousel()
  } catch (e) {
    errorText.value = e && typeof e === 'object' && 'message' in e ? String((e as { message?: unknown }).message) : '加载失败'
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (uploading.value) return
  hint.value = null
  errorText.value = null
  const validationError = validateUploadForm()
  if (validationError) {
    errorText.value = validationError
    return
  }
  uploading.value = true
  try {
    const created = await createHomeCarousel({
      title: form.title,
      subtitle: form.subtitle,
      linkUrl: form.linkUrl,
      file: pickedFile.value!,
    })
    rows.value = [...rows.value, created].sort((a, b) => a.sortOrder - b.sortOrder || a.id - b.id)
    hint.value = '轮播图已上传并加入首页轮播'
    resetForm()
  } catch (e) {
    errorText.value = resolveRequestErrorMessage(e)
  } finally {
    uploading.value = false
  }
}

async function onDelete(id: number) {
  if (busyId.value != null) return
  const ok = window.confirm('删除后首页将不再展示这张轮播图，是否继续？')
  if (!ok) return
  busyId.value = id
  hint.value = null
  errorText.value = null
  try {
    await deleteHomeCarousel(id)
    await load()
    hint.value = '轮播图已删除'
  } catch (e) {
    errorText.value = e && typeof e === 'object' && 'message' in e ? String((e as { message?: unknown }).message) : '删除失败'
  } finally {
    busyId.value = null
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
.form-card,
.list-card {
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

.count {
  min-width: 86px;
  height: 48px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  background: rgba(0, 190, 189, 0.1);
  color: var(--primary);
  font-weight: 900;
  font-size: 18px;
}

.section-title {
  font-size: 16px;
  font-weight: 800;
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

.field-wide {
  grid-column: 1 / -1;
}

.label {
  font-size: 13px;
  color: var(--muted);
}

.file-input {
  padding: 9px 12px;
}

.upload-row {
  margin-top: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.file-name {
  color: var(--muted);
  font-size: 13px;
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

.empty {
  margin-top: 14px;
  color: var(--muted);
  font-size: 14px;
}

.list {
  margin-top: 14px;
  display: grid;
  gap: 12px;
}

.item {
  display: grid;
  grid-template-columns: 240px 1fr auto;
  gap: 14px;
  align-items: center;
  padding: 12px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 16px;
}

.thumb {
  width: 100%;
  height: 132px;
  object-fit: cover;
  border-radius: 12px;
  background: rgba(15, 23, 42, 0.04);
}

.meta {
  display: grid;
  gap: 8px;
}

.meta-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.time {
  color: var(--muted);
  font-size: 12px;
}

.meta-title {
  font-size: 18px;
  font-weight: 800;
}

.meta-sub,
.meta-link {
  color: var(--muted);
  font-size: 13px;
  word-break: break-all;
}

.actions {
  display: flex;
  align-items: center;
}

@media (max-width: 980px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .item {
    grid-template-columns: 1fr;
  }

  .hero {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
