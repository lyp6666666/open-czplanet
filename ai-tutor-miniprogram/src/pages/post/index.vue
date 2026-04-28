<template>
  <view class="page">
    <view class="hero">
      <text class="eyebrow">{{ isEdit ? '编辑需求' : '发布需求' }}</text>
      <text class="title">{{ isEdit ? '更新孩子的上课计划' : '把需求说清楚，老师更好判断' }}</text>
      <text class="subtitle">授课方式、预算、时间和老师要求会影响匹配质量。</text>
    </view>

    <view v-if="loading" class="state">加载中...</view>

    <template v-else>
      <view class="panel">
        <text class="section-title">学生信息</text>
        <view class="field">
          <text class="label">科目</text>
          <input class="input" v-model="form.subjectName" placeholder="如：数学、英语" />
        </view>
        <view class="field">
          <text class="label">年级</text>
          <picker :range="gradeOptions" range-key="label" :value="gradeIndex" @change="onGradeChange">
            <view class="picker-value">{{ gradeOptions[gradeIndex].label }}</view>
          </picker>
        </view>
        <view class="field">
          <text class="label">学员性别</text>
          <view class="seg">
            <view class="seg-item" :class="{ active: form.studentGender === 'male' }" @click="form.studentGender = 'male'">男</view>
            <view class="seg-item" :class="{ active: form.studentGender === 'female' }" @click="form.studentGender = 'female'">女</view>
            <view class="seg-item" :class="{ active: form.studentGender === 'UNKNOWN' }" @click="form.studentGender = 'UNKNOWN'">不限</view>
          </view>
        </view>
      </view>

      <view class="panel">
        <text class="section-title">上课安排</text>
        <view class="field">
          <text class="label">授课方式</text>
          <view class="seg">
            <view class="seg-item" :class="{ active: form.classMode === 'online' }" @click="setClassMode('online')">线上</view>
            <view class="seg-item" :class="{ active: form.classMode === 'offline' }" @click="setClassMode('offline')">线下</view>
            <view class="seg-item" :class="{ active: form.classMode === 'both' }" @click="setClassMode('both')">均可</view>
          </view>
        </view>
        <view v-if="form.classMode !== 'online'" class="field">
          <text class="label">城市</text>
          <input class="input" v-model="form.city" placeholder="如：上海" />
        </view>
        <view v-if="form.classMode !== 'online'" class="field">
          <text class="label">上课地址</text>
          <input class="input" v-model="form.address" placeholder="填写大致区域即可" />
        </view>
        <view class="field">
          <text class="label">每周频次</text>
          <slider :value="form.frequencyPerWeek" min="1" max="7" activeColor="#0f766e" @change="form.frequencyPerWeek = Number($event.detail.value)" />
          <text class="hint">每周 {{ form.frequencyPerWeek }} 次</text>
        </view>
        <view class="field">
          <text class="label">可上课时间</text>
          <input class="input" v-model="form.availableTime" placeholder="如：周二/周四 19:00 后" />
        </view>
      </view>

      <view class="panel">
        <text class="section-title">预算与要求</text>
        <view class="budget-row">
          <view class="field">
            <text class="label">预算下限</text>
            <input class="input" type="digit" v-model="form.budgetMin" placeholder="元/小时" />
          </view>
          <view class="field">
            <text class="label">预算上限</text>
            <input class="input" type="digit" v-model="form.budgetMax" placeholder="元/小时" />
          </view>
        </view>
        <view class="field">
          <text class="label">老师性别偏好</text>
          <view class="seg">
            <view class="seg-item" :class="{ active: form.teacherGenderPreference === 'both' }" @click="form.teacherGenderPreference = 'both'">不限</view>
            <view class="seg-item" :class="{ active: form.teacherGenderPreference === 'male' }" @click="form.teacherGenderPreference = 'male'">男老师</view>
            <view class="seg-item" :class="{ active: form.teacherGenderPreference === 'female' }" @click="form.teacherGenderPreference = 'female'">女老师</view>
          </view>
        </view>
        <view class="field">
          <text class="label">需求描述</text>
          <textarea class="textarea" v-model="form.description" placeholder="孩子当前情况、目标、希望老师怎么辅导" maxlength="800" />
        </view>
        <view class="field">
          <text class="label">对教员的要求</text>
          <textarea class="textarea small" v-model="form.teacherRequirementDetail" placeholder="如：有高中数学经验，擅长引导思路" maxlength="500" />
        </view>
      </view>

      <view v-if="error" class="error">{{ error }}</view>

      <view class="bottom-bar">
        <button class="submit" :disabled="saving" @click="submit">{{ saving ? '保存中...' : isEdit ? '保存修改' : '发布需求' }}</button>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { jobsApi } from '@/api/jobs';
import { ensureStudentMode } from '@/utils/studentGuard';

const id = ref<number | null>(null);
const loading = ref(false);
const saving = ref(false);
const error = ref('');
const isEdit = computed(() => !!id.value);

const gradeOptions = [
  { label: '小学', value: 'GRADE1', stage: 'PRIMARY' },
  { label: '初一', value: 'JUNIOR1', stage: 'JUNIOR' },
  { label: '初二', value: 'JUNIOR2', stage: 'JUNIOR' },
  { label: '初三', value: 'JUNIOR3', stage: 'JUNIOR' },
  { label: '高一', value: 'SENIOR1', stage: 'SENIOR' },
  { label: '高二', value: 'SENIOR2', stage: 'SENIOR' },
  { label: '高三', value: 'SENIOR3', stage: 'SENIOR' },
  { label: '其他', value: 'ADULT', stage: 'OTHER' }
];
const gradeIndex = ref(4);

const form = reactive<any>({
  subjectName: '',
  subjectOther: false,
  title: '',
  description: '',
  studentGender: 'UNKNOWN',
  gradeCode: gradeOptions[gradeIndex.value].value,
  teacherGenderPreference: 'both',
  availableTime: '',
  teacherRequirementDetail: '',
  classMode: 'online',
  city: '',
  address: '',
  budgetMin: '',
  budgetMax: '',
  frequencyPerWeek: 2,
  stageCode: gradeOptions[gradeIndex.value].stage,
  educationRequirement: 'UNLIMITED',
  publisherIdentity: 'PARENT'
});

function onGradeChange(e: any) {
  gradeIndex.value = Number(e.detail.value || 0);
  form.gradeCode = gradeOptions[gradeIndex.value].value;
  form.stageCode = gradeOptions[gradeIndex.value].stage;
}

function setClassMode(mode: 'online' | 'offline' | 'both') {
  form.classMode = mode;
  if (mode === 'online') {
    form.city = '';
    form.address = '';
  }
}

function hydrate(data: any) {
  Object.assign(form, {
    subjectName: data.subjectName || '',
    subjectOther: !!data.subjectOther,
    title: data.title || '',
    description: data.description || '',
    studentGender: data.studentGender || 'UNKNOWN',
    gradeCode: data.gradeCode || 'SENIOR1',
    teacherGenderPreference: data.teacherGenderPreference || 'both',
    availableTime: data.availableTime || '',
    teacherRequirementDetail: data.teacherRequirementDetail || '',
    classMode: String(data.classMode || 'online').toLowerCase(),
    city: data.city || '',
    address: data.address || '',
    budgetMin: data.budgetMin ?? '',
    budgetMax: data.budgetMax ?? '',
    frequencyPerWeek: Number(data.frequencyPerWeek || 2),
    stageCode: data.stageCode || 'SENIOR',
    educationRequirement: data.educationRequirement || 'UNLIMITED',
    publisherIdentity: data.publisherIdentity || 'PARENT',
    status: data.status
  });
  const idx = gradeOptions.findIndex((it) => it.value === form.gradeCode);
  gradeIndex.value = idx >= 0 ? idx : 0;
}

async function loadDetail() {
  if (!id.value) return;
  loading.value = true;
  try {
    hydrate(await jobsApi.getDemand(id.value));
  } catch (e: any) {
    error.value = e?.message || e?.msg || '加载需求失败';
  } finally {
    loading.value = false;
  }
}

function buildPayload() {
  const subjectName = String(form.subjectName || '').trim();
  const desc = String(form.description || '').trim();
  const req = String(form.teacherRequirementDetail || '').trim();
  const min = Number(form.budgetMin);
  const max = Number(form.budgetMax);
  if (!subjectName) throw new Error('请填写科目');
  if (!desc) throw new Error('请填写需求描述');
  if (!Number.isFinite(min) || !Number.isFinite(max) || min <= 0 || max <= 0) throw new Error('请填写有效预算');
  if (min > max) throw new Error('预算下限不能大于上限');
  if (form.classMode !== 'online' && (!String(form.city || '').trim() || !String(form.address || '').trim())) {
    throw new Error('线下或均可授课需要填写城市和上课地址');
  }
  return {
    subjectName,
    subjectOther: false,
    title: `${gradeOptions[gradeIndex.value].label}${subjectName}家教`,
    description: desc,
    studentGender: form.studentGender,
    gradeCode: form.gradeCode,
    teacherGenderPreference: form.teacherGenderPreference,
    availableTime: String(form.availableTime || '').trim() || undefined,
    teacherRequirementDetail: req,
    classMode: form.classMode,
    city: form.classMode === 'online' ? undefined : String(form.city || '').trim(),
    address: form.classMode === 'online' ? undefined : String(form.address || '').trim(),
    budgetMin: min,
    budgetMax: max,
    frequencyPerWeek: Number(form.frequencyPerWeek || 1),
    stageCode: form.stageCode,
    educationRequirement: 'UNLIMITED',
    publisherIdentity: 'PARENT',
    status: form.status
  };
}

async function submit() {
  if (saving.value) return;
  if (!ensureStudentMode('学生/家长身份可以发布或编辑需求。')) return;
  error.value = '';
  let payload: any;
  try {
    payload = buildPayload();
  } catch (e: any) {
    error.value = e?.message || '请检查表单';
    return;
  }
  saving.value = true;
  try {
    if (id.value) await jobsApi.updateDemand(id.value, payload);
    else id.value = Number(await jobsApi.createDemand(payload));
    uni.showToast({ title: id.value ? '已保存' : '已发布', icon: 'success' });
    setTimeout(() => uni.navigateBack(), 500);
  } catch (e: any) {
    error.value = e?.message || e?.msg || '保存失败';
  } finally {
    saving.value = false;
  }
}

onLoad((options: any) => {
  if (!ensureStudentMode('学生/家长身份可以发布或编辑需求。')) return;
  const n = Number(options?.id);
  id.value = Number.isFinite(n) && n > 0 ? n : null;
  void loadDetail();
});
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  padding: 16px 16px 96px;
  background: #f4f7f7;
  box-sizing: border-box;
}

.hero {
  padding: 20px;
  border-radius: 20px;
  background: linear-gradient(135deg, #102326 0%, #14524d 70%, #d49f52 160%);
  color: #fff;
  box-shadow: 0 18px 38px rgba(16, 35, 38, 0.2);
}

.eyebrow,
.title,
.subtitle,
.section-title,
.label,
.hint {
  display: block;
}

.eyebrow {
  font-size: 12px;
  opacity: 0.72;
  margin-bottom: 6px;
}

.title {
  font-size: 22px;
  font-weight: 900;
  margin-bottom: 8px;
}

.subtitle {
  font-size: 13px;
  line-height: 1.6;
  opacity: 0.82;
}

.panel {
  margin-top: 12px;
  padding: 16px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid rgba(18, 37, 41, 0.08);
  box-shadow: 0 12px 28px rgba(18, 37, 41, 0.06);
}

.section-title {
  margin-bottom: 12px;
  color: #152326;
  font-size: 16px;
  font-weight: 900;
}

.field {
  margin-top: 12px;
}

.label {
  color: #68757f;
  font-size: 13px;
  font-weight: 800;
  margin-bottom: 8px;
}

.input,
.picker-value {
  width: 100%;
  padding: 12px;
  border-radius: 12px;
  background: #f4f7f7;
  color: #172326;
  box-sizing: border-box;
  font-size: 14px;
}

.input,
.picker-value {
  height: 44px;
  line-height: 20px;
}

.textarea {
  width: 100%;
  padding: 12px;
  border-radius: 12px;
  background: #f4f7f7;
  color: #172326;
  box-sizing: border-box;
  font-size: 14px;
}

.textarea {
  height: 112px;
  line-height: 1.6;
}

.textarea.small {
  height: 88px;
}

.seg {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.seg-item {
  height: 38px;
  line-height: 38px;
  text-align: center;
  border-radius: 12px;
  background: #f0f3f3;
  color: #65717a;
  font-size: 13px;
}

.seg-item.active {
  background: #0f766e;
  color: #fff;
  font-weight: 900;
}

.hint {
  color: #0f766e;
  font-size: 12px;
  text-align: right;
}

.budget-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 10px 16px calc(10px + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.96);
  border-top: 1px solid rgba(18, 37, 41, 0.08);
}

.submit {
  width: 100%;
  height: 44px;
  line-height: 44px;
  border: 0;
  border-radius: 999px;
  background: #0f766e;
  color: #fff;
  font-size: 15px;
  font-weight: 900;
}

.error {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 12px;
  background: #fff0f0;
  color: #c24141;
  font-size: 13px;
}

.state {
  margin-top: 16px;
  padding: 24px;
  border-radius: 18px;
  background: #fff;
  color: #66727c;
  text-align: center;
}
</style>
