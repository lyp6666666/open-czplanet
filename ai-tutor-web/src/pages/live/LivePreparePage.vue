<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { liveApi, type PrepareLiveSessionResp } from '@/api/live'

const route = useRoute()
const router = useRouter()

const courseId = computed(() => Number(route.params.courseId))
const loading = ref(false)
const joining = ref(false)
const error = ref<string | null>(null)
const prepareData = ref<PrepareLiveSessionResp | null>(null)

const cameraEnabled = ref(true)
const micEnabled = ref(true)
const speakerChecked = ref(false)
const networkLevel = ref<'GOOD' | 'NORMAL' | 'POOR'>('GOOD')

async function load() {
  loading.value = true
  error.value = null
  try {
    prepareData.value = await liveApi.prepare(courseId.value, { clientType: 'WEB', sourcePage: 'LIVE_PREPARE' })
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载课堂失败'
  } finally {
    loading.value = false
  }
}

async function enterClassroom() {
  if (!prepareData.value?.sessionId || joining.value) return
  joining.value = true
  error.value = null
  try {
    await liveApi.reportDevice(prepareData.value.sessionId, {
      reportStage: 'PREPARE',
      cameraStatus: cameraEnabled.value ? 'READY' : 'OFF',
      micStatus: micEnabled.value ? 'READY' : 'OFF',
      speakerStatus: speakerChecked.value ? 'READY' : 'UNTESTED',
      networkLevel: networkLevel.value,
      browserInfo: navigator.userAgent,
      osInfo: navigator.platform,
      deviceInfo: { cameraEnabled: cameraEnabled.value, micEnabled: micEnabled.value },
    })
    await router.push({ name: 'liveClassroom', params: { courseId: String(courseId.value) } })
  } catch (e) {
    error.value = e instanceof Error ? e.message : '进入课堂失败'
  } finally {
    joining.value = false
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="live-prepare">
    <section class="hero card">
      <div class="hero-copy">
        <div class="eyebrow">实时课程准备</div>
        <h1>进入课堂前，先确认设备状态</h1>
        <p>
          <template v-if="prepareData">
            你即将和 {{ prepareData.peerDisplayName || '对方' }} 进入实时课堂，建议先确认摄像头、麦克风和网络状态。
          </template>
          <template v-else>正在加载课堂信息。</template>
        </p>
      </div>
      <div class="preview">
        <div class="preview-frame">
          <div class="preview-badge">本地预览</div>
          <div class="preview-surface">
            <div class="avatar-orb">你</div>
          </div>
        </div>
      </div>
    </section>

    <section class="panel-grid">
      <div class="card device-panel">
        <div class="section-title">设备自检</div>
        <label class="device-row">
          <span>摄像头</span>
          <input v-model="cameraEnabled" type="checkbox" />
        </label>
        <label class="device-row">
          <span>麦克风</span>
          <input v-model="micEnabled" type="checkbox" />
        </label>
        <label class="device-row">
          <span>扬声器试听</span>
          <input v-model="speakerChecked" type="checkbox" />
        </label>
        <label class="device-row">
          <span>网络状态</span>
          <select v-model="networkLevel" class="select">
            <option value="GOOD">良好</option>
            <option value="NORMAL">一般</option>
            <option value="POOR">较差</option>
          </select>
        </label>
      </div>

      <div class="card summary-panel">
        <div class="section-title">课堂信息</div>
        <div class="summary-row">
          <span>课程状态</span>
          <strong>{{ prepareData?.status || '加载中' }}</strong>
        </div>
        <div class="summary-row">
          <span>对方</span>
          <strong>{{ prepareData?.peerDisplayName || '—' }}</strong>
        </div>
        <div class="summary-row">
          <span>允许入会</span>
          <strong>{{ prepareData?.joinableNow ? '现在可进入' : '暂未开放' }}</strong>
        </div>
        <div v-if="error" class="hint error">{{ error }}</div>
        <div class="actions">
          <button class="btn" type="button" @click="$router.back()">稍后再说</button>
          <button class="btn btn-primary" type="button" :disabled="loading || joining || !prepareData?.canJoin" @click="enterClassroom">
            {{ joining ? '进入中...' : '进入课堂' }}
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.live-prepare {
  display: grid;
  gap: 16px;
}

.hero {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 18px;
  padding: 24px;
  border-radius: 24px;
  background:
    radial-gradient(circle at top left, rgba(0, 190, 189, 0.18), transparent 35%),
    linear-gradient(135deg, #f7fbff 0%, #eef6f5 100%);
}

.eyebrow {
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(0, 0, 0, 0.48);
}

h1 {
  margin: 8px 0 10px;
  font-size: 30px;
  line-height: 1.1;
}

p {
  margin: 0;
  color: rgba(0, 0, 0, 0.62);
}

.preview-frame {
  border-radius: 22px;
  padding: 18px;
  background: linear-gradient(160deg, #0f2c2f 0%, #17393a 100%);
  color: #fff;
}

.preview-badge {
  font-size: 12px;
  opacity: 0.82;
}

.preview-surface {
  margin-top: 14px;
  min-height: 220px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.06);
}

.avatar-orb {
  width: 88px;
  height: 88px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  font-weight: 700;
  background: rgba(255, 255, 255, 0.14);
}

.panel-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.device-panel,
.summary-panel {
  padding: 18px;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 12px;
}

.device-row,
.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.select {
  min-width: 120px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}

@media (max-width: 960px) {
  .hero,
  .panel-grid {
    grid-template-columns: 1fr;
  }
}
</style>
