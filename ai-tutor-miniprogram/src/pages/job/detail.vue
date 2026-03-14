<template>
  <view class="page">
    <view v-if="loading" class="loading">
      <text class="loading-text">加载中...</text>
    </view>

    <view v-else-if="job" class="content">
      <view class="card hero">
        <view class="hero-head">
          <text class="title">{{ job.title || job.subjectName || '需求详情' }}</text>
          <text class="price">{{ budgetText }}</text>
        </view>
        <view class="meta">
          <text v-if="placeText" class="meta-item">{{ placeText }}</text>
          <text v-if="classModeText" class="meta-item">{{ classModeText }}</text>
          <text v-if="job.frequencyPerWeek" class="meta-item">每周 {{ job.frequencyPerWeek }} 次</text>
          <text v-if="educationText" class="meta-item">{{ educationText }}</text>
          <text v-if="teacherGenderText" class="meta-item">{{ teacherGenderText }}</text>
          <text v-if="stageText" class="meta-item">{{ stageText }}</text>
          <text v-if="gradeText" class="meta-item">{{ gradeText }}</text>
        </view>
        <view v-if="job.publisherIdentity === 'ORGANIZATION'" class="org-tip">
          <text class="org-tip-text">机构单：请注意甄别信息，沟通确认上课安排与费用规则。</text>
        </view>
      </view>

      <view class="card sec">
        <text class="sec-title">需求描述</text>
        <text class="desc">{{ job.description || '暂无描述' }}</text>
      </view>

      <view v-if="job.teacherRequirementDetail" class="card sec">
        <text class="sec-title">对教员的要求</text>
        <text class="desc">{{ job.teacherRequirementDetail }}</text>
      </view>

      <view v-if="job.availableTime || scheduleText" class="card sec">
        <text class="sec-title">可上课时间</text>
        <text v-if="job.availableTime" class="desc">{{ job.availableTime }}</text>
        <text v-else class="desc">{{ scheduleText }}</text>
      </view>

      <view v-if="showAddress" class="card sec">
        <text class="sec-title">上课地点</text>
        <view class="kv">
          <text class="k">城市</text>
          <text class="v">{{ job.city || '暂无' }}</text>
        </view>
        <view class="kv">
          <text class="k">地址</text>
          <text class="v">{{ job.address || '暂无' }}</text>
        </view>
      </view>

      <view v-if="job.publisher" class="card publisher">
        <view class="pub-row">
          <image class="pub-avatar" :src="resolveImageUrl(job.publisher.avatar)" mode="aspectFill"></image>
          <view class="pub-main">
            <text class="pub-name">{{ job.publisher.displayName || '发布者' }}</text>
            <text class="pub-sub">{{ job.publisher.identityLabel || '发布者' }}</text>
          </view>
        </view>
        <u-button v-if="userStore.isTutor" type="primary" color="#00bebd" shape="circle" @click="handleContact">发起沟通</u-button>
      </view>
    </view>

    <view v-else class="loading">
      <text class="loading-text">暂无数据</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { jobsApi } from '@/api/jobs';
import { chatApi } from '@/api/chat';
import { useUserStore } from '@/stores/user';
import { resolveImageUrl } from '@/utils/request';

const userStore = useUserStore();
const job = ref<any>(null);
const loading = ref(false);

onLoad(async (options: any) => {
  if (options.id) {
    await fetchDetail(options.id);
  }
});

const fetchDetail = async (id: number) => {
  try {
    loading.value = true;
    const res: any = await jobsApi.getDemandView(id);
    job.value = res;
  } catch (error) {
    console.error(error);
    uni.showToast({ title: '加载失败', icon: 'none' });
  } finally {
    loading.value = false;
  }
};

const normalizeLower = (v: unknown) => String(v || '').trim().toLowerCase();

const classModeText = computed(() => {
  const s = normalizeLower(job.value?.classMode);
  if (!s) return '';
  if (s === 'online') return '线上';
  if (s === 'offline') return '线下';
  if (s === 'both') return '线上/线下';
  return '';
});

const placeText = computed(() => {
  const s = normalizeLower(job.value?.classMode);
  if (s === 'online') return '线上';
  const city = String(job.value?.city || '').trim();
  if (city) return city;
  if (s === 'offline') return '线下';
  if (s === 'both') return '线上/线下';
  return '';
});

const budgetText = computed(() => {
  const min = job.value?.budgetMin;
  const max = job.value?.budgetMax;
  if (min != null && max != null && String(min) !== '' && String(max) !== '') return `¥${min}-${max}/小时`;
  if (min != null && String(min) !== '') return `¥${min}/小时起`;
  if (max != null && String(max) !== '') return `≤¥${max}/小时`;
  return '面议';
});

const stageText = computed(() => {
  const s = String(job.value?.stageCode || '').trim().toUpperCase();
  if (!s) return '';
  if (s === 'PRESCHOOL') return '幼小';
  if (s === 'PRIMARY') return '小学';
  if (s === 'JUNIOR') return '初中';
  if (s === 'SENIOR') return '高中';
  if (s === 'OTHER') return '其他';
  return s;
});

const gradeText = computed(() => {
  const s = String(job.value?.gradeCode || '').trim().toUpperCase();
  if (!s) return '';
  if (s === 'PRESCHOOL') return '学前';
  if (s.startsWith('GRADE')) return `小学${s.replace('GRADE', '')}年级`;
  if (s.startsWith('JUNIOR')) return `初${s.replace('JUNIOR', '')}`;
  if (s.startsWith('SENIOR')) return `高${s.replace('SENIOR', '')}`;
  if (s === 'SELF_EXAM') return '自考';
  if (s.startsWith('COLLEGE')) return `大学${s.replace('COLLEGE', '')}`;
  if (s === 'ADULT') return '成人';
  return s;
});

const educationText = computed(() => {
  const s = String(job.value?.educationRequirement || '').trim().toUpperCase();
  if (!s) return '';
  if (s === 'TOP2') return 'TOP2';
  if (s === 'C985') return '985';
  if (s === 'C211') return '211';
  if (s === 'DOUBLE_FIRST_CLASS') return '双一流';
  if (s === 'FIRST_TIER') return '一本';
  if (s === 'BACHELOR') return '本科及以上';
  if (s === 'OVERSEAS') return '海外';
  if (s === 'QS50') return 'QS50';
  return s;
});

const teacherGenderText = computed(() => {
  const s = normalizeLower(job.value?.teacherGenderPreference);
  if (!s) return '';
  if (s === 'male') return '偏好男老师';
  if (s === 'female') return '偏好女老师';
  if (s === 'both') return '性别不限';
  return '';
});

const scheduleText = computed(() => {
  const raw = String(job.value?.schedule || '').trim();
  if (!raw) return '';
  try {
    const obj = JSON.parse(raw);
    return JSON.stringify(obj, null, 2);
  } catch {
    return raw;
  }
});

const showAddress = computed(() => {
  const s = normalizeLower(job.value?.classMode);
  return s === 'offline' || s === 'both';
});

const handleContact = async () => {
    if (!job.value) return;
    try {
        const targetUid = job.value?.publisher?.uid;
        if (!targetUid) {
            uni.showToast({ title: '缺少发布者信息', icon: 'none' });
            return;
        }
        
        const roomId: any = await chatApi.getOrCreateRoom(targetUid);
        if (roomId) {
            uni.navigateTo({ url: `/pages/chat/room?id=${roomId}` });
        } else {
            uni.showToast({ title: '进入会话失败', icon: 'none' });
        }
    } catch (error) {
        console.error(error);
        uni.showToast({ title: '发起聊天失败', icon: 'none' });
    }
};
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: var(--bg);
}

.content {
  padding: 16px;
  display: grid;
  gap: 12px;
}

.card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(31, 35, 41, 0.08);
}

.hero {
  padding: 16px;
  display: grid;
  gap: 12px;
}

.hero-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.title {
  font-size: 16px;
  font-weight: 900;
  color: var(--text);
  flex: 1;
  min-width: 0;
}

.price {
  font-size: 13px;
  font-weight: 900;
  color: #ff4d4f;
  white-space: nowrap;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  color: var(--muted);
  font-size: 12px;
}

.meta-item {
  padding: 3px 10px;
  border-radius: 999px;
  border: 1px solid rgba(31, 35, 41, 0.08);
  background: rgba(255, 255, 255, 0.7);
}

.org-tip {
  background: rgba(10, 120, 255, 0.06);
  border: 1px solid rgba(10, 120, 255, 0.16);
  border-radius: 12px;
  padding: 10px 12px;
}

.org-tip-text {
  color: #0a78ff;
  font-size: 12px;
  line-height: 1.6;
}

.sec {
  padding: 16px;
}

.sec-title {
  font-size: 14px;
  font-weight: 900;
  color: var(--text);
  margin-bottom: 12px;
  display: block;
}

.desc {
  background: rgba(31, 35, 41, 0.04);
  border: 1px solid rgba(31, 35, 41, 0.08);
  border-radius: 12px;
  padding: 12px;
  font-size: 13px;
  color: var(--text);
  line-height: 1.7;
  white-space: pre-wrap;
}

.kv {
  display: flex;
  gap: 12px;
  padding: 10px 0;
  border-top: 1px solid rgba(31, 35, 41, 0.08);
}

.kv:first-of-type {
  border-top: none;
  padding-top: 0;
}

.k {
  width: 84px;
  color: var(--muted);
  font-size: 12px;
  font-weight: 900;
  flex-shrink: 0;
}

.v {
  color: var(--text);
  font-size: 13px;
  line-height: 1.7;
  flex: 1;
}

.publisher {
  padding: 16px;
  display: grid;
  gap: 12px;
}

.pub-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.pub-avatar {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  border: 1px solid rgba(31, 35, 41, 0.12);
  background: #f0f0f0;
  flex-shrink: 0;
}

.pub-main {
  flex: 1;
  min-width: 0;
  display: grid;
  gap: 2px;
}

.pub-name {
  font-size: 14px;
  font-weight: 900;
  color: var(--text);
}

.pub-sub {
  font-size: 12px;
  color: var(--muted);
}

.loading {
  padding-top: 120px;
  display: flex;
  justify-content: center;
}

.loading-text {
  color: var(--muted);
  font-size: 14px;
}
</style>
