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
            <text class="text">认证资料</text>
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
                <text class="label">学历证明</text>
                <view class="cert-uploader" :class="{ done: certUrl }" @click="uploadCert">
                    <text class="upload-mark">{{ certUrl ? '✓' : '+' }}</text>
                    <text>{{ certUrl ? '已上传学历证明' : '上传学信网/学生证/毕业证截图' }}</text>
                </view>
            </view>
            <view class="form-item">
                <text class="label">身份证人像面</text>
                <view class="proof-box" :class="{ done: idFrontUrl }" @click="uploadIdFront">
                    <image v-if="idFrontUrl" class="proof-img" :src="resolveImageUrl(idFrontUrl)" mode="aspectFill"></image>
                    <view class="proof-overlay">
                        <text class="upload-mark">{{ idFrontUrl ? '✓' : '+' }}</text>
                        <text>{{ idFrontUrl ? '已上传人像面' : '上传人像面' }}</text>
                    </view>
                </view>
            </view>
            <view class="form-item">
                <text class="label">身份证国徽面</text>
                <view class="proof-box" :class="{ done: idBackUrl }" @click="uploadIdBack">
                    <image v-if="idBackUrl" class="proof-img" :src="resolveImageUrl(idBackUrl)" mode="aspectFill"></image>
                    <view class="proof-overlay">
                        <text class="upload-mark">{{ idBackUrl ? '✓' : '+' }}</text>
                        <text>{{ idBackUrl ? '已上传国徽面' : '上传国徽面' }}</text>
                    </view>
                </view>
            </view>
            <view class="verify-note">
                <text>平台仅用于教师身份与学历审核，审核中不会公开证件图片。</text>
            </view>
        </view>
    </view>

    <view class="footer">
        <button class="action-btn secondary" v-if="currentStep > 0" @click="prev">上一步</button>
        <button class="action-btn primary" v-if="currentStep < 2" @click="next">下一步</button>
        <button class="action-btn primary" v-if="currentStep === 2" :disabled="submitBusy" @click="submit">
            {{ submitBusy ? '提交中...' : '提交审核' }}
        </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { userApi } from '@/api/user';
import { tutorApi } from '@/api/tutor';
import { assetsApi } from '@/api/assets';
import { useUserStore } from '@/stores/user';
import { resolveImageUrl } from '@/utils/request';

const userStore = useUserStore();
const currentStep = ref(0);
const certUrl = ref('');
const idFrontUrl = ref('');
const idBackUrl = ref('');
const submitBusy = ref(false);

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

const uploadIdFront = () => {
    chooseAndUpload('other', (url) => {
        idFrontUrl.value = url;
    });
};

const uploadIdBack = () => {
    chooseAndUpload('other', (url) => {
        idBackUrl.value = url;
    });
};

const chooseAndUpload = (biz: 'avatar' | 'other', cb: (url: string) => void) => {
    uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
            const filePath = res.tempFilePaths[0];
            void uploadFile(filePath, biz, cb);
        }
    });
};

const uploadFile = async (filePath: string, biz: 'avatar' | 'other', cb: (url: string) => void) => {
    uni.showLoading({ title: '上传中...', mask: true });
    try {
        const uploaded = await assetsApi.uploadImage(filePath, biz);
        cb(uploaded.url);
    } catch (err: any) {
        console.error(err);
        uni.showToast({ title: err?.message || '上传失败', icon: 'none' });
    } finally {
        uni.hideLoading();
    }
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
    if (!form.certificateUrls) {
        uni.showToast({ title: '请上传学历证明', icon: 'none' });
        return;
    }
    if (!idFrontUrl.value || !idBackUrl.value) {
        uni.showToast({ title: '请上传身份证正反面', icon: 'none' });
        return;
    }
    if (submitBusy.value) return;
    submitBusy.value = true;

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
        
        const urls = JSON.parse(form.certificateUrls);
        await tutorApi.submitEducation(urls);
        await tutorApi.submitRealname({
            method: 'ID_PHOTO',
            idFrontUrl: idFrontUrl.value,
            idBackUrl: idBackUrl.value
        });

        uni.showToast({ title: '已提交审核', icon: 'success' });
        
        // Refresh user info to update status
        await userStore.refreshUserInfo();
        
        setTimeout(() => {
            uni.navigateBack();
        }, 1500);

    } catch (error: any) {
        console.error(error);
        uni.showToast({ title: error.message || '提交失败', icon: 'none' });
    } finally {
        submitBusy.value = false;
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
    height: 48px;
    border-radius: 12px;
    border: 1px dashed rgba(31, 35, 41, 0.18);
    background: rgba(31, 35, 41, 0.03);
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    font-size: 13px;
    color: var(--muted);
    font-weight: 800;
    &.done {
        border-color: rgba(0, 190, 189, 0.42);
        background: rgba(0, 190, 189, 0.08);
        color: #007f7e;
    }
}

.proof-box {
    height: 118px;
    border-radius: 14px;
    border: 1px dashed rgba(31, 35, 41, 0.18);
    background: rgba(31, 35, 41, 0.03);
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--muted);
    font-size: 13px;
    font-weight: 800;
    overflow: hidden;
    position: relative;
    &.done {
        border-style: solid;
        border-color: rgba(0, 190, 189, 0.35);
        background: rgba(0, 190, 189, 0.08);
    }
}

.proof-img {
    width: 100%;
    height: 118px;
}

.proof-overlay {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    background: rgba(255, 255, 255, 0.76);
    color: #007f7e;
}

.proof-box:not(.done) .proof-overlay {
    background: transparent;
    color: var(--muted);
}

.upload-mark {
    width: 22px;
    height: 22px;
    border-radius: 50%;
    background: #00bebd;
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    font-weight: 900;
    line-height: 22px;
}

.proof-box:not(.done) .upload-mark,
.cert-uploader:not(.done) .upload-mark {
    background: rgba(31, 35, 41, 0.14);
    color: rgba(31, 35, 41, 0.5);
}

.verify-note {
    margin-top: 12px;
    padding: 10px 12px;
    border-radius: 12px;
    background: rgba(15, 118, 110, 0.08);
    color: #0f766e;
    font-size: 12px;
    line-height: 1.6;
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

.action-btn {
    flex: 1;
    height: 48px;
    border-radius: 999px;
    border: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 15px;
    font-weight: 900;
    line-height: 48px;
    &::after {
        border: 0;
    }
    &.primary {
        background: #00bebd;
        color: #fff;
        box-shadow: 0 10px 20px rgba(0, 190, 189, 0.22);
    }
    &.secondary {
        background: #fff;
        color: var(--text);
        border: 1px solid rgba(31, 35, 41, 0.12);
    }
    &[disabled] {
        opacity: 0.62;
    }
}
</style>
