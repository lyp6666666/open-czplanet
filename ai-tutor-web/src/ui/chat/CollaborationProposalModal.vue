<script setup lang="ts">
import { computed, ref, watch } from 'vue'

const props = defineProps<{
  open: boolean
  busy?: boolean
  error?: string | null
  title?: string
  submitText?: string
  initial?: { pricePerHour: string; classTime: string; frequencyPerWeek: number } | null
}>()

const emit = defineEmits<{
  close: []
  submit: [{ pricePerHour: string; classTime: string; frequencyPerWeek: number }]
}>()

const pricePerHour = ref('')
const classTime = ref('')
const frequencyPerWeek = ref<number | null>(null)

watch(
  () => [props.open, props.initial] as const,
  ([open]) => {
    if (!open) return
    const init = props.initial
    if (init) {
      pricePerHour.value = init.pricePerHour || ''
      classTime.value = init.classTime || ''
      frequencyPerWeek.value = typeof init.frequencyPerWeek === 'number' ? init.frequencyPerWeek : 1
      return
    }
    pricePerHour.value = ''
    classTime.value = ''
    frequencyPerWeek.value = 1
  },
  { immediate: true },
)

const canSubmit = computed(() => {
  if (props.busy) return false
  const p = pricePerHour.value.trim()
  const t = classTime.value.trim()
  const f = frequencyPerWeek.value
  return p.length > 0 && t.length > 0 && typeof f === 'number' && Number.isFinite(f) && f >= 1
})

function close() {
  if (props.busy) return
  emit('close')
}

function submit() {
  if (!canSubmit.value) return
  emit('submit', {
    pricePerHour: pricePerHour.value.trim(),
    classTime: classTime.value.trim(),
    frequencyPerWeek: Number(frequencyPerWeek.value),
  })
}
</script>

<template>
  <div v-if="open" class="mask" @click.self="close">
    <div class="modal card">
      <div class="m-head">
        <div class="title">{{ title || '发起合作' }}</div>
        <button class="icon-btn" type="button" :disabled="busy" @click="close">×</button>
      </div>

      <div class="form">
        <label class="field">
          <div class="k">收费标准</div>
          <input v-model="pricePerHour" class="input" placeholder="例如：200 元/小时" :disabled="busy" />
        </label>
        <label class="field">
          <div class="k">上课时间</div>
          <textarea v-model="classTime" class="textarea" placeholder="例如：周一、周三 19:00-21:00" :disabled="busy" />
        </label>
        <label class="field">
          <div class="k">每周频次</div>
          <input v-model.number="frequencyPerWeek" class="input" type="number" min="1" step="1" :disabled="busy" />
        </label>
      </div>

      <div v-if="busy" class="hint">提交中...</div>
      <div v-else-if="error" class="hint error">{{ error }}</div>

      <div class="ops">
        <button class="btn" type="button" :disabled="busy" @click="close">取消</button>
        <button class="btn btn-primary" type="button" :disabled="!canSubmit" @click="submit">{{ submitText || '发送提案' }}</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: grid;
  place-items: center;
  padding: 16px;
  z-index: 60;
}

.modal {
  width: min(520px, 100%);
  padding: 18px;
  border-radius: 16px;
  display: grid;
  gap: 12px;
}

.m-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.title {
  font-weight: 900;
  font-size: 16px;
}

.icon-btn {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: #fff;
  cursor: pointer;
  font-size: 18px;
  line-height: 1;
}

.form {
  display: grid;
  gap: 10px;
}

.field {
  display: grid;
  gap: 6px;
}

.k {
  font-size: 12px;
  color: var(--muted);
  font-weight: 700;
}

.input {
  height: 40px;
  border-radius: 12px;
  border: 1px solid var(--border);
  padding: 0 12px;
  outline: none;
  background: #fff;
}

.textarea {
  min-height: 92px;
  border-radius: 12px;
  border: 1px solid var(--border);
  padding: 10px 12px;
  outline: none;
  background: #fff;
  resize: vertical;
}

.input:focus,
.textarea:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 4px var(--primary-weak);
}

.ops {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
