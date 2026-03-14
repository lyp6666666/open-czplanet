<template>
  <view class="container">
    <view class="hero">
      <text class="hero-title">家教入驻</text>
      <text class="hero-sub">完善资料后可浏览需求并发起沟通</text>
    </view>

    <view class="steps-header">
        <view class="step-item" :class="{ active: currentStep >= 0 }">
            <text class="num">1</text>
            <text class="text">基本信息</text>
        </view>
        <view class="line"></view>
        <view class="step-item" :class="{ active: currentStep >= 1 }">
            <text class="num">2</text>
            <text class="text">授课信息</text>
        </view>
        <view class="line"></view>
        <view class="step-item" :class="{ active: currentStep >= 2 }">
            <text class="num">3</text>
            <text class="text">教育信息</text>
        </view>
    </view>

    <view class="card form-content">
        <view v-show="currentStep === 0">
            <view class="form-item">
                <text class="label">头像</text>
                <view class="avatar-uploader" @click="chooseAvatar">
                    <image v-if="form.avatar" :src="resolveImageUrl(form.avatar)" mode="aspectFill" class="avatar-img"></image>
                    <view v-else class="placeholder">+</view>
                </view>
            </view>
            <view class="form-item">
                <text class="label">真实姓名</text>
                <input class="input" v-model="form.realName" placeholder="请输入真实姓名" />
            </view>
            <view class="form-item">
                <text class="label">所在城市</text>
                <input class="input" v-model="form.city" placeholder="请输入所在城市" />
            </view>
            <view class="form-item">
                <text class="label">自我介绍</text>
                <textarea class="textarea" v-model="form.introduction" placeholder="请简单介绍一下自己..." />
            </view>
        </view>

        <view v-show="currentStep === 1">
            <view class="form-item">
                <text class="label">擅长科目</text>
                <input class="input" v-model="form.subject" placeholder="例如：数学、英语" />
            </view>
            <view class="form-item">
                <text class="label">授课年限（年）</text>
                <input class="input" type="number" v-model.number="form.experienceYears" />
            </view>
            <view class="form-item">
                <text class="label">课时费（元/小时）</text>
                <input class="input" type="number" v-model="form.ratePerHour" />
            </view>
            <view class="form-item">
                <text class="label">授课方式</text>
                <radio-group @change="onModeChange">
                    <label class="radio"><radio value="ONLINE" :checked="form.teachingMode === 'ONLINE'" />线上</label>
                    <label class="radio"><radio value="OFFLINE" :checked="form.teachingMode === 'OFFLINE'" />线下</label>
                    <label class="radio"><radio value="BOTH" :checked="form.teachingMode === 'BOTH'" />均可</label>
                </radio-group>
            </view>
        </view>

        <view v-show="currentStep === 2">
            <view class="form-item">
                <text class="label">毕业院校</text>
                <input class="input" v-model="form.highestEduSchool" placeholder="请输入学校名称" />
            </view>
            <view class="form-item">
                <text class="label">学历</text>
                <input class="input" v-model="form.education" placeholder="例如：本科、硕士" />
            </view>
            <view class="form-item">
                <text class="label">证书（选填）</text>
                <view class="cert-uploader" @click="uploadCert">
                    <text v-if="!certUrl">上传图片</text>
                    <text v-else>已上传（1 张）</text>
                </view>
            </view>
        </view>
    </view>

    <view class="footer">
        <u-button class="btn prev" v-if="currentStep > 0" shape="circle" @click="prev">上一步</u-button>
        <u-button class="btn next" type="primary" color="#00bebd" shape="circle" v-if="currentStep < 2" @click="next">下一步</u-button>
        <u-button class="btn submit" type="primary" color="#00bebd" shape="circle" v-if="currentStep === 2" @click="submit">提交</u-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { userApi } from '@/api/user';
import { tutorApi } from '@/api/tutor';
import { useUserStore } from '@/stores/user';
import { BASE_URL, resolveImageUrl } from '@/utils/request';

const userStore = useUserStore();
const currentStep = ref(0);
const certUrl = ref('');

const form = reactive({
    avatar: (userStore.userInfo?.avatar && !String(userStore.userInfo.avatar).includes('pravatar.cc') && !String(userStore.userInfo.avatar).endsWith('.svg')) ? userStore.userInfo.avatar : '',
    realName: '',
    city: '',
    introduction: '',
    subject: '',
    experienceYears: 0,
    ratePerHour: '',
    teachingMode: 'ONLINE',
    highestEduSchool: '',
    education: '',
    certificateUrls: ''
});

const onModeChange = (e: any) => {
    form.teachingMode = e.detail.value;
};

const chooseAvatar = () => {
    uni.chooseImage({
        count: 1,
        success: (res) => {
            const filePath = res.tempFilePaths[0];
            uploadFile(filePath, 'avatar', (url) => {
                form.avatar = url;
            });
        }
    });
};

const uploadCert = () => {
    uni.chooseImage({
        count: 1,
        success: (res) => {
            const filePath = res.tempFilePaths[0];
            uploadFile(filePath, 'other', (url) => {
                certUrl.value = url;
                form.certificateUrls = JSON.stringify([url]);
            });
        }
    });
};

const uploadFile = (filePath: string, biz: 'avatar' | 'other', cb: (url: string) => void) => {
    const token = uni.getStorageSync('token');
    uni.showLoading({ title: '上传中...', mask: true });
    uni.uploadFile({
        url: `${BASE_URL}/api/v1/assets/upload`,
        filePath: filePath,
        name: 'file',
        formData: { biz },
        header: {
            'Authorization': 'Bearer ' + token
        },
        success: (uploadFileRes) => {
            const data = JSON.parse(uploadFileRes.data);
            if (data.code === 0) {
                cb(data.data?.url);
            } else {
                uni.showToast({ title: data.msg || data.message || '上传失败', icon: 'none' });
            }
        },
        fail: (err) => {
            console.error(err);
            uni.showToast({ title: '上传失败', icon: 'none' });
        },
        complete: () => {
            uni.hideLoading();
        }
    });
};

const next = () => {
    if (currentStep.value === 0) {
        if (!form.avatar || !form.realName || !form.city) {
            uni.showToast({ title: '请完善头像、姓名与城市', icon: 'none' });
            return;
        }
    } else if (currentStep.value === 1) {
        if (!form.subject || !form.ratePerHour) {
            uni.showToast({ title: '请完善科目与课时费', icon: 'none' });
            return;
        }
    }
    currentStep.value++;
};

const prev = () => {
    currentStep.value--;
};

const submit = async () => {
    if (!form.highestEduSchool || !form.education) {
        uni.showToast({ title: '请完善学校与学历', icon: 'none' });
        return;
    }

    try {
        const updateData = {
            baseUserInfo: {
                avatar: form.avatar,
                name: form.realName // Sync real name to nickname for now, or just update avatar
            },
            teacherExtInfo: {
                realName: form.realName,
                city: form.city,
                introduction: form.introduction,
                subject: form.subject,
                experienceYears: form.experienceYears,
                ratePerHour: Number(form.ratePerHour),
                teachingMode: form.teachingMode,
                highestEduSchool: form.highestEduSchool,
                education: form.education,
                certificateUrls: form.certificateUrls
            }
        };

        await userApi.updateUserInfo(updateData);
        
        // Also submit verification if cert exists (optional flow)
        if (form.certificateUrls) {
            try {
                const urls = JSON.parse(form.certificateUrls);
                await tutorApi.submitEducation(urls);
            } catch (e) {
                console.warn('学历认证提交失败', e);
            }
        }

        uni.showToast({ title: '已提交', icon: 'success' });
        
        // Refresh user info to update status
        await userStore.refreshUserInfo();
        
        setTimeout(() => {
            uni.navigateBack();
        }, 1500);

    } catch (error: any) {
        console.error(error);
        uni.showToast({ title: error.message || '提交失败', icon: 'none' });
    }
};
</script>

<style lang="scss" scoped>
.container {
    padding: 16px;
    background-color: var(--bg);
    min-height: 100vh;
    display: flex;
    flex-direction: column;
}

.hero {
    padding: 6px 4px 14px;
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.hero-title {
    font-size: 20px;
    font-weight: 900;
    color: var(--text);
}

.hero-sub {
    font-size: 12px;
    color: var(--muted);
}

.card {
    background: var(--card);
    border: 1px solid var(--border);
    border-radius: 16px;
    box-shadow: 0 10px 30px rgba(31, 35, 41, 0.08);
}

.steps-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
    padding: 10px 12px;
    background: rgba(255, 255, 255, 0.7);
    border: 1px solid rgba(31, 35, 41, 0.08);
    border-radius: 16px;
    
    .step-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        opacity: 0.5;
        &.active {
            opacity: 1;
            .num {
                background-color: var(--primary);
                color: #fff;
                border-color: var(--primary);
            }
            .text {
                color: var(--primary);
            }
        }
        .num {
            width: 30px;
            height: 30px;
            border-radius: 50%;
            border: 1px solid rgba(31, 35, 41, 0.22);
            display: flex;
            justify-content: center;
            align-items: center;
            margin-bottom: 5px;
            font-size: 14px;
            font-weight: 900;
            background: #fff;
        }
        .text {
            font-size: 12px;
            color: var(--muted);
            font-weight: 800;
        }
    }
    .line {
        flex: 1;
        height: 1px;
        background-color: rgba(31, 35, 41, 0.12);
        margin: 0 10px;
    }
}
.form-content {
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 12px;
}
.form-item {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.label {
    font-size: 12px;
    color: var(--muted);
    font-weight: 900;
}

.input {
    height: 44px;
    background: #ffffff;
    border: 1px solid rgba(31, 35, 41, 0.12);
    border-radius: 12px;
    padding: 0 12px;
    font-size: 14px;
    color: var(--text);
}

.textarea {
    min-height: 96px;
    background: #ffffff;
    border: 1px solid rgba(31, 35, 41, 0.12);
    border-radius: 12px;
    padding: 10px 12px;
    font-size: 14px;
    color: var(--text);
    line-height: 1.6;
}

.avatar-uploader {
    width: 72px;
    height: 72px;
    border-radius: 18px;
    background: rgba(31, 35, 41, 0.04);
    border: 1px dashed rgba(31, 35, 41, 0.18);
    display: flex;
    align-items: center;
    justify-content: center;
}

.avatar-img {
    width: 72px;
    height: 72px;
    border-radius: 18px;
}

.placeholder {
    font-size: 28px;
    color: rgba(31, 35, 41, 0.28);
    font-weight: 900;
}

.cert-uploader {
    height: 44px;
    border-radius: 12px;
    border: 1px dashed rgba(31, 35, 41, 0.18);
    background: rgba(31, 35, 41, 0.03);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 13px;
    color: var(--muted);
    font-weight: 800;
}

.radio {
    font-size: 14px;
    color: var(--text);
    margin-right: 12px;
}

.footer {
    margin-top: 12px;
    display: flex;
    gap: 10px;
}

.btn {
    flex: 1;
}
</style>
