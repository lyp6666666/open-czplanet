<template>
  <view class="page">
    <view v-if="loading" class="loading">
      <text class="loading-text">加载中...</text>
    </view>

    <view v-else-if="tutor" class="content">
      <view class="card profile">
        <view class="head">
          <image class="avatar" :src="resolveImageUrl(tutor.user.avatar)" mode="aspectFill"></image>
          <view class="head-main">
            <view class="name-row">
              <text class="name">{{ tutor.user.name || '老师' }}</text>
              <text class="role-tag">家教</text>
            </view>
            <view class="badges" v-if="badges.length">
              <text v-for="b in badges" :key="b" class="badge">{{ b }}</text>
            </view>
            <view class="tags" v-if="subjectTags.length">
              <text v-for="t in subjectTags" :key="t" class="tag">{{ t }}</text>
            </view>
          </view>
          <view class="price" v-if="priceText">
            <text class="price-num">{{ priceText }}</text>
          </view>
        </view>

        <view class="meta">
          <text v-if="tutor.teacherProfile?.city" class="meta-item">{{ tutor.teacherProfile.city }}</text>
          <text v-if="teachingModeText" class="meta-item">{{ teachingModeText }}</text>
          <text v-if="tutor.teacherProfile?.highestEduSchool" class="meta-item">{{ tutor.teacherProfile.highestEduSchool }}</text>
          <text v-if="tutor.teacherProfile?.education" class="meta-item">{{ tutor.teacherProfile.education }}</text>
          <text v-if="tutor.teacherProfile?.experienceYears != null" class="meta-item">{{ tutor.teacherProfile.experienceYears }} 年教龄</text>
        </view>
      </view>

      <view class="card sec">
        <text class="sec-title">基本信息</text>
        <view class="kv">
          <text class="k">擅长科目</text>
          <text class="v">{{ tutor.teacherProfile?.subject || '暂无' }}</text>
        </view>
        <view class="kv">
          <text class="k">学历</text>
          <text class="v">{{ tutor.teacherProfile?.education || '暂无' }}</text>
        </view>
        <view class="kv">
          <text class="k">所在城市</text>
          <text class="v">{{ tutor.teacherProfile?.city || '暂无' }}</text>
        </view>
        <view class="kv">
          <text class="k">授课方式</text>
          <text class="v">{{ teachingModeText || '暂无' }}</text>
        </view>
        <view class="kv">
          <text class="k">课时费用</text>
          <text class="v">{{ priceText || '暂无' }}</text>
        </view>
      </view>

      <view class="card sec">
        <text class="sec-title">个人简介</text>
        <text class="desc">{{ tutor.teacherProfile?.introduction || '暂无简介' }}</text>
      </view>

      <view class="ops">
        <u-button type="default" shape="circle" @click="handleContact">发起申请</u-button>
        <u-button type="primary" color="#00bebd" shape="circle" @click="openBookingModal">预约课程</u-button>
      </view>
    </view>

    <view v-else class="loading">
      <text class="loading-text">暂无数据</text>
    </view>

    <view v-if="showBookingModal" class="mask" @click="closeBookingModal">
      <view class="modal card" @click.stop>
        <view class="modal-head">
          <text class="modal-title">预约课程</text>
          <view class="modal-close" @click="closeBookingModal">
            <u-icon name="close" size="18" color="#646a73"></u-icon>
          </view>
        </view>

        <view class="modal-body">
          <view class="form-item">
            <text class="label">日期</text>
            <picker mode="date" :value="bookingDate" start="2023-01-01" end="2030-12-31" @change="onDateChange">
              <view class="picker-value">{{ bookingDate || '请选择日期' }}</view>
            </picker>
          </view>
          <view class="form-item">
            <text class="label">时间</text>
            <picker mode="time" :value="bookingTime" start="00:00" end="23:59" @change="onTimeChange">
              <view class="picker-value">{{ bookingTime || '请选择时间' }}</view>
            </picker>
          </view>
          <view class="form-item">
            <text class="label">备注</text>
            <input class="input" v-model="bookingRemark" placeholder="可填写上课需求/说明（选填）" />
          </view>
        </view>

        <view class="modal-footer">
          <u-button type="primary" color="#00bebd" shape="circle" @click="handleBook">确认并支付</u-button>
        </view>
      </view>
    </view>

    <view v-if="showApplyModal" class="mask" @click="closeApplyModal">
      <view class="modal card" @click.stop>
        <view class="modal-head">
          <text class="modal-title">向老师发起申请</text>
          <view class="modal-close" @click="closeApplyModal">
            <u-icon name="close" size="18" color="#646a73"></u-icon>
          </view>
        </view>

        <view class="modal-body">
          <view class="form-item">
            <text class="label">申请语</text>
            <textarea class="textarea" v-model="applyContent" placeholder="请简单描述你的需求，便于老师判断是否通过"></textarea>
          </view>
        </view>

        <view class="modal-footer">
          <u-button type="primary" color="#00bebd" shape="circle" :disabled="applyBusy" @click="handleApply">
            {{ applyBusy ? '发送中...' : '发送申请' }}
          </u-button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { request, resolveImageUrl } from '@/utils/request';
import { chatApi } from '@/api/chat';
import { useUserStore } from '@/stores/user';

const userStore = useUserStore();
const tutor = ref<any>(null);
const loading = ref(false);
const showBookingModal = ref(false);
const bookingDate = ref('');
const bookingTime = ref('');
const bookingRemark = ref('');
const showApplyModal = ref(false);
const applyContent = ref('您好老师，我这边有一个家教需求，方便聊聊吗？');
const applyBusy = ref(false);

const subjectTags = computed(() => {
  const raw = String(tutor.value?.teacherProfile?.subject || '').trim();
  if (!raw) return [];
  return raw
    .split(/[,，、/|\\s]+/g)
    .map(s => s.trim())
    .filter(Boolean)
    .slice(0, 6);
});

const teachingModeText = computed(() => {
  const v = String(tutor.value?.teacherProfile?.teachingMode || '').trim().toUpperCase();
  if (!v) return '';
  if (v === 'ONLINE') return '线上';
  if (v === 'OFFLINE') return '线下';
  if (v === 'BOTH') return '线上/线下';
  return '';
});

const priceText = computed(() => {
  const v = tutor.value?.teacherProfile?.ratePerHour;
  if (v === null || v === undefined || v === '') return '';
  return `${v} 元/小时`;
});

const badges = computed(() => {
  const arr: string[] = [];
  const rn = Number(tutor.value?.realnameVerifyStatus);
  const edu = Number(tutor.value?.eduVerifyStatus);
  if (rn === 2) arr.push('实名认证');
  if (edu === 2) arr.push('学历认证');
  return arr;
});

onLoad(async (options: any) => {
  if (options.id) {
    await fetchDetail(options.id);
  }
});

const fetchDetail = async (id: string) => {
  try {
    loading.value = true;
    const res: any = await request({
      url: `/user/card?uid=${id}`
    });
    tutor.value = res;
  } catch (error) {
    console.error(error);
    uni.showToast({ title: '加载失败', icon: 'none' });
  } finally {
    loading.value = false;
  }
};

const handleContact = async () => {
  if (!userStore.isLoggedIn) {
    uni.showToast({ title: '请先登录', icon: 'none' });
    setTimeout(() => {
      uni.switchTab({ url: '/pages/me/index' });
    }, 800);
    return;
  }
  if (!tutor.value) return;
  showApplyModal.value = true;
};

const closeApplyModal = () => {
  if (applyBusy.value) return;
  showApplyModal.value = false;
};

const handleApply = async () => {
  if (!tutor.value) return;
  if (applyBusy.value) return;
  const targetUid = tutor.value.user?.id;
  const tutorId = tutor.value.teacherProfile?.id;
  const content = String(applyContent.value || '').trim();
  if (!targetUid || !tutorId) return;
  if (!content) {
    uni.showToast({ title: '请填写申请语', icon: 'none' });
    return;
  }
  applyBusy.value = true;
  try {
    const clientRequestId = `mp-${Date.now()}-${Math.random().toString(16).slice(2)}`;
    const msg: any = await chatApi.startChatByApplication({
      receiverUid: targetUid,
      contextType: 'TUTOR',
      contextId: tutorId,
      content,
      clientRequestId,
    });
    const roomId = msg?.message?.roomId;
    if (roomId) {
      showApplyModal.value = false;
      uni.navigateTo({ url: `/pages/chat/room?id=${roomId}` });
      return;
    }
    uni.showToast({ title: '申请已发送', icon: 'success' });
    showApplyModal.value = false;
  } catch (error) {
    console.error(error);
    uni.showToast({ title: '发送申请失败', icon: 'none' });
  } finally {
    applyBusy.value = false;
  }
};

const openBookingModal = () => {
    if (!userStore.isLoggedIn) {
        uni.showToast({ title: '请先登录', icon: 'none' });
        setTimeout(() => {
            uni.switchTab({ url: '/pages/me/index' });
        }, 1500);
        return;
    }
    showBookingModal.value = true;
};

const closeBookingModal = () => {
    showBookingModal.value = false;
};

const onDateChange = (e: any) => {
    bookingDate.value = e.detail.value;
};

const onTimeChange = (e: any) => {
    bookingTime.value = e.detail.value;
};

const handleBook = async () => {
  if (!bookingDate.value || !bookingTime.value) {
      uni.showToast({ title: '请选择日期和时间', icon: 'none' });
      return;
  }

  try {
    const startTime = `${bookingDate.value}T${bookingTime.value}:00`;
    const apptRes: any = await request({
        url: '/appointment/create',
        method: 'POST',
        data: {
            targetUid: tutor.value.user.id,
            subjectId: 1,
            startTime: startTime,
            durationMinutes: 60,
            classMode: 'ONLINE',
            city: '线上',
            address: '线上',
            remark: bookingRemark.value
        },
        loading: true
    });

    const appointmentId = apptRes;
    const orderRes: any = await request({
      url: '/chat/brokerage/order/direct',
      method: 'POST',
      data: {
        amountFen: 1,
        subject: `预约家教 ${tutor.value?.user?.name || ''}`,
        appointmentId: appointmentId
      },
      loading: true
    });

    if (!orderRes || !orderRes.id) {
        throw new Error('创建订单失败');
    }

    const payRes: any = await request({
      url: '/payment/create',
      method: 'POST',
      data: {
        channel: 'WECHAT',
        contextId: orderRes.id,
        contextType: 'BROKERAGE_ORDER',
        openid: userStore.userInfo?.openid
      },
      loading: true
    });

    if (payRes && payRes.payParams) {
        const params = payRes.payParams;
        
        if (params.mock) {
            uni.showModal({
                title: '模拟支付',
                content: '是否模拟支付成功？',
                success: (res) => {
                    if (res.confirm) {
                        uni.showToast({ title: '支付成功（模拟）', icon: 'success' });
                        closeBookingModal();
                    } else {
                        uni.showToast({ title: '已取消（模拟）', icon: 'none' });
                    }
                }
            });
            return;
        }

        uni.requestPayment({
            provider: 'wxpay',
            timeStamp: params.timeStamp,
            nonceStr: params.nonceStr,
            package: params.package,
            signType: params.signType,
            paySign: params.paySign,
            success: function (res: any) {
                console.log('success:' + JSON.stringify(res));
                uni.showToast({ title: '支付成功', icon: 'success' });
                closeBookingModal();
            },
            fail: function (err: any) {
                console.log('fail:' + JSON.stringify(err));
                uni.showToast({ title: '支付失败', icon: 'none' });
            }
        } as any);
    } else {
        throw new Error('支付参数缺失');
    }

  } catch (error: any) {
    console.error(error);
    uni.showToast({ title: error.message || '预约失败', icon: 'none' });
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

.profile {
  padding: 18px 16px;
  display: grid;
  gap: 12px;
}

.head {
  display: flex;
  gap: 12px;
  align-items: center;
}

.head-main {
  flex: 1;
  min-width: 0;
}

.avatar {
  width: 64px;
  height: 64px;
  border-radius: 18px;
  background: #f0f0f0;
  border: 2px solid #fff;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
  flex-shrink: 0;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.name {
  font-size: 18px;
  font-weight: 900;
  color: var(--text);
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-tag {
  font-size: 12px;
  color: var(--primary);
  background: rgba(0, 190, 189, 0.12);
  padding: 2px 10px;
  border-radius: 999px;
  font-weight: 900;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--muted);
  font-size: 12px;
}

.meta-item {
  padding: 4px 10px;
  border: 1px solid rgba(31, 35, 41, 0.08);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.7);
}

.badges {
  display: flex;
  gap: 6px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.badge {
  font-size: 10px;
  line-height: 16px;
  color: #0a78ff;
  background: rgba(10, 120, 255, 0.1);
  border: 1px solid rgba(10, 120, 255, 0.25);
  padding: 0 8px;
  border-radius: 999px;
  font-weight: 900;
}

.tags {
  display: flex;
  gap: 6px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.tag {
  font-size: 10px;
  line-height: 16px;
  color: var(--primary);
  background: rgba(0, 190, 189, 0.12);
  border: 1px solid rgba(0, 190, 189, 0.25);
  padding: 0 8px;
  border-radius: 999px;
  font-weight: 900;
}

.price {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  flex-shrink: 0;
}

.price-num {
  color: #ff4d4f;
  font-weight: 900;
  font-size: 13px;
  white-space: nowrap;
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

.ops {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
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
  width: min(560px, 100%);
  padding: 16px;
  border-radius: 16px;
  display: grid;
  gap: 12px;
}

.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.modal-title {
  font-size: 16px;
  font-weight: 900;
  color: var(--text);
}

.modal-close {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  border: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
}

.form-item {
  display: grid;
  gap: 8px;
  margin-bottom: 10px;
}

.label {
  font-size: 12px;
  color: var(--muted);
  font-weight: 900;
}

.picker-value {
  height: 40px;
  border-radius: 12px;
  border: 1px solid var(--border);
  padding: 0 12px;
  display: flex;
  align-items: center;
  color: var(--text);
  background: #fff;
}

.input {
  height: 40px;
  border-radius: 12px;
  border: 1px solid var(--border);
  padding: 0 12px;
  background: #fff;
  color: var(--text);
}

.textarea {
  min-height: 88px;
  border-radius: 12px;
  border: 1px solid var(--border);
  padding: 10px 12px;
  background: #fff;
  color: var(--text);
  box-sizing: border-box;
  width: 100%;
}

.modal-footer {
  margin-top: 6px;
}
</style>
