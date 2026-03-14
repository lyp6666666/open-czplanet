<template>
  <view class="container" v-if="tutor">
    <view class="header">
      <image class="avatar" :src="tutor.user.avatar || '/static/logo.png'" mode="aspectFill"></image>
      <text class="name">{{ tutor.user.name }}</text>
    </view>
    <view class="section">
      <text class="label">Subject</text>
      <text class="value">{{ tutor.teacherProfile?.subject || 'N/A' }}</text>
    </view>
    <view class="section">
      <text class="label">Education</text>
      <text class="value">{{ tutor.teacherProfile?.education || 'N/A' }}</text>
    </view>
    <view class="section">
      <text class="label">Introduction</text>
      <text class="value">{{ tutor.teacherProfile?.introduction || 'No introduction provided.' }}</text>
    </view>
    <button type="default" class="contact-btn" @click="handleContact" style="margin-bottom: 10px;">Contact Tutor</button>
    <button type="primary" class="book-btn" @click="openBookingModal">Book Appointment</button>

    <!-- Booking Modal -->
    <view v-if="showBookingModal" class="modal-mask" @click="closeBookingModal">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <text class="modal-title">Book Appointment</text>
          <text class="close-btn" @click="closeBookingModal">×</text>
        </view>
        <view class="modal-body">
          <view class="form-item">
            <text class="label">Date</text>
            <picker mode="date" :value="bookingDate" start="2023-01-01" end="2030-12-31" @change="onDateChange">
              <view class="picker-value">{{ bookingDate || 'Select Date' }}</view>
            </picker>
          </view>
          <view class="form-item">
            <text class="label">Time</text>
            <picker mode="time" :value="bookingTime" start="00:00" end="23:59" @change="onTimeChange">
              <view class="picker-value">{{ bookingTime || 'Select Time' }}</view>
            </picker>
          </view>
          <view class="form-item">
            <text class="label">Remark</text>
            <input class="input" v-model="bookingRemark" placeholder="Enter remark" />
          </view>
        </view>
        <view class="modal-footer">
          <button type="primary" @click="handleBook">Confirm & Pay</button>
        </view>
      </view>
    </view>
  </view>
  <view v-else class="loading">
    <text>Loading...</text>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { request } from '@/utils/request';
import { chatApi } from '@/api/chat';
import { useUserStore } from '@/stores/user';

const userStore = useUserStore();
const tutor = ref<any>(null);
const showBookingModal = ref(false);
const bookingDate = ref('');
const bookingTime = ref('');
const bookingRemark = ref('');

onLoad(async (options: any) => {
  if (options.id) {
    await fetchDetail(options.id);
  }
});

const fetchDetail = async (id: string) => {
  try {
    const res: any = await request({
      url: `/user/card?uid=${id}`,
      loading: true
    });
    tutor.value = res;
  } catch (error) {
    console.error(error);
    uni.showToast({ title: 'Failed to load details', icon: 'none' });
  }
};

const handleContact = async () => {
    if (!tutor.value) return;
    try {
        const targetUid = tutor.value.user.id;
        const roomId: any = await chatApi.getOrCreateRoom(targetUid);
        if (roomId) {
            uni.navigateTo({ url: `/pages/chat/room?id=${roomId}` });
        }
    } catch (error) {
        console.error(error);
        uni.showToast({ title: 'Failed to start chat', icon: 'none' });
    }
};

const openBookingModal = () => {
    if (!userStore.isLoggedIn) {
        uni.showToast({ title: 'Please login first', icon: 'none' });
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
      uni.showToast({ title: 'Please select date and time', icon: 'none' });
      return;
  }

  try {
    const startTime = `${bookingDate.value}T${bookingTime.value}:00`; // ISO format partially

    // 1. Create Appointment
    // Note: Assuming API expects this format. If not, adjust.
    // Also hardcoding some required fields for MVP
    const apptRes: any = await request({
        url: '/appointment/create',
        method: 'POST',
        data: {
            targetUid: tutor.value.user.id,
            subjectId: 1, // Hardcoded for MVP as we don't have subject list
            startTime: startTime, // Backend expects LocalDateTime, string in ISO format usually works
            durationMinutes: 60,
            classMode: 'ONLINE',
            city: 'Online',
            address: 'Online',
            remark: bookingRemark.value
        },
        loading: true
    });

    const appointmentId = apptRes; // Assuming returns Long id directly or {id: ...} - check service

    // 2. Create Direct Order
    const orderRes: any = await request({
      url: '/chat/brokerage/order/direct',
      method: 'POST',
      data: {
        amountFen: 1, // Test amount: 0.01 CNY
        subject: `Book Tutor ${tutor.value?.user?.name || ''}`,
        appointmentId: appointmentId
      },
      loading: true
    });

    if (!orderRes || !orderRes.id) {
        throw new Error('Create order failed');
    }

    // 3. Create Payment
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

    // 4. Request Payment
    if (payRes && payRes.payParams) {
        const params = payRes.payParams;
        
        // Mock 支付处理
        if (params.mock) {
            uni.showModal({
                title: 'Mock Payment',
                content: 'Simulate successful payment?',
                success: (res) => {
                    if (res.confirm) {
                        uni.showToast({ title: 'Payment Success (Mock)', icon: 'success' });
                        closeBookingModal();
                        // 可以在这里调用后端 Mock 回调接口（如果需要）
                    } else {
                        uni.showToast({ title: 'Payment Cancelled (Mock)', icon: 'none' });
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
                uni.showToast({ title: 'Payment Success', icon: 'success' });
                closeBookingModal();
            },
            fail: function (err: any) {
                console.log('fail:' + JSON.stringify(err));
                uni.showToast({ title: 'Payment Failed', icon: 'none' });
            }
        } as any);
    } else {
        throw new Error('No payment params returned');
    }

  } catch (error: any) {
    console.error(error);
    uni.showToast({ title: error.message || 'Booking Failed', icon: 'none' });
  }
};
</script>

<style lang="scss" scoped>
.container {
  padding: 20px;
  background-color: #fff;
  min-height: 100vh;
}
/* ... existing styles ... */
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0,0,0,0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 999;
}
.modal-content {
  width: 80%;
  background-color: #fff;
  border-radius: 10px;
  padding: 20px;
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.modal-title {
  font-size: 18px;
  font-weight: bold;
}
.close-btn {
  font-size: 24px;
  color: #999;
}
.form-item {
  margin-bottom: 15px;
  .label {
    display: block;
    margin-bottom: 5px;
    font-size: 14px;
    color: #666;
  }
  .picker-value {
    border: 1px solid #ddd;
    padding: 10px;
    border-radius: 5px;
  }
  .input {
    border: 1px solid #ddd;
    padding: 10px;
    border-radius: 5px;
  }
}
.modal-footer {
  margin-top: 20px;
}
/* Ensure existing styles are preserved or re-declared if overwritten */
.header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
  
  .avatar {
    width: 100px;
    height: 100px;
    border-radius: 50%;
    margin-bottom: 15px;
    background-color: #f0f0f0;
    border: 2px solid #fff;
    box-shadow: 0 2px 10px rgba(0,0,0,0.1);
  }
  .name {
    font-size: 22px;
    font-weight: bold;
    color: #333;
  }
}
.section {
  margin-bottom: 20px;
  .label {
    font-size: 14px;
    color: #999;
    margin-bottom: 8px;
    display: block;
    text-transform: uppercase;
    letter-spacing: 1px;
  }
  .value {
    font-size: 16px;
    color: #333;
    line-height: 1.6;
  }
}
.book-btn {
  margin-top: 10px;
  width: 100%;
  border-radius: 25px;
}
.contact-btn {
  margin-top: 30px;
  width: 100%;
  border-radius: 25px;
  background-color: #f0f0f0;
  color: #333;
}
.loading {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  color: #999;
}
</style>
