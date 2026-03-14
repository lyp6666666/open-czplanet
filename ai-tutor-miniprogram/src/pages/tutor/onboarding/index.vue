<template>
  <view class="container">
    <view class="steps-header">
        <view class="step-item" :class="{ active: currentStep >= 0 }">
            <text class="num">1</text>
            <text class="text">Basic</text>
        </view>
        <view class="line"></view>
        <view class="step-item" :class="{ active: currentStep >= 1 }">
            <text class="num">2</text>
            <text class="text">Teaching</text>
        </view>
        <view class="line"></view>
        <view class="step-item" :class="{ active: currentStep >= 2 }">
            <text class="num">3</text>
            <text class="text">Edu</text>
        </view>
    </view>

    <view class="form-content">
        <!-- Step 1: Basic Info -->
        <view v-show="currentStep === 0">
            <view class="form-item">
                <text class="label">Avatar</text>
                <view class="avatar-uploader" @click="chooseAvatar">
                    <image v-if="form.avatar" :src="form.avatar" mode="aspectFill" class="avatar-img"></image>
                    <view v-else class="placeholder">+</view>
                </view>
            </view>
            <view class="form-item">
                <text class="label">Real Name</text>
                <input class="input" v-model="form.realName" placeholder="Your real name" />
            </view>
            <view class="form-item">
                <text class="label">City</text>
                <input class="input" v-model="form.city" placeholder="Current city" />
            </view>
            <view class="form-item">
                <text class="label">Introduction</text>
                <textarea class="textarea" v-model="form.introduction" placeholder="Self introduction..." />
            </view>
        </view>

        <!-- Step 2: Teaching Info -->
        <view v-show="currentStep === 1">
            <view class="form-item">
                <text class="label">Subject</text>
                <input class="input" v-model="form.subject" placeholder="e.g. Math, English" />
            </view>
            <view class="form-item">
                <text class="label">Experience (Years)</text>
                <input class="input" type="number" v-model.number="form.experienceYears" />
            </view>
            <view class="form-item">
                <text class="label">Hourly Rate (¥)</text>
                <input class="input" type="number" v-model="form.ratePerHour" />
            </view>
            <view class="form-item">
                <text class="label">Teaching Mode</text>
                <radio-group @change="onModeChange">
                    <label class="radio"><radio value="ONLINE" :checked="form.teachingMode === 'ONLINE'" />Online</label>
                    <label class="radio"><radio value="OFFLINE" :checked="form.teachingMode === 'OFFLINE'" />Offline</label>
                    <label class="radio"><radio value="BOTH" :checked="form.teachingMode === 'BOTH'" />Both</label>
                </radio-group>
            </view>
        </view>

        <!-- Step 3: Education -->
        <view v-show="currentStep === 2">
            <view class="form-item">
                <text class="label">School</text>
                <input class="input" v-model="form.highestEduSchool" placeholder="University name" />
            </view>
            <view class="form-item">
                <text class="label">Degree</text>
                <input class="input" v-model="form.education" placeholder="e.g. Bachelor, Master" />
            </view>
            <view class="form-item">
                <text class="label">Certificates (Optional)</text>
                <view class="cert-uploader" @click="uploadCert">
                    <text v-if="!certUrl">Upload Image</text>
                    <text v-else>Uploaded (1 file)</text>
                </view>
            </view>
        </view>
    </view>

    <view class="footer">
        <button class="btn prev" v-if="currentStep > 0" @click="prev">Previous</button>
        <button class="btn next" type="primary" v-if="currentStep < 2" @click="next">Next</button>
        <button class="btn submit" type="primary" v-if="currentStep === 2" @click="submit">Submit</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { userApi } from '@/api/user';
import { tutorApi } from '@/api/tutor';
import { useUserStore } from '@/stores/user';

const userStore = useUserStore();
const currentStep = ref(0);
const certUrl = ref('');

const form = reactive({
    avatar: userStore.userInfo?.avatar || '',
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
            uploadFile(filePath, (url) => {
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
            uploadFile(filePath, (url) => {
                certUrl.value = url;
                form.certificateUrls = JSON.stringify([url]);
            });
        }
    });
};

const uploadFile = (filePath: string, cb: (url: string) => void) => {
    const token = uni.getStorageSync('token');
    uni.showLoading({ title: 'Uploading...' });
    uni.uploadFile({
        url: 'http://localhost:8080/api/v1/common/upload', // TODO: Use config or environment variable
        filePath: filePath,
        name: 'file',
        header: {
            'Authorization': 'Bearer ' + token
        },
        success: (uploadFileRes) => {
            const data = JSON.parse(uploadFileRes.data);
            if (data.code === 0) {
                cb(data.data); // Assuming data.data is the URL
            } else {
                uni.showToast({ title: 'Upload failed', icon: 'none' });
            }
        },
        fail: (err) => {
            console.error(err);
            uni.showToast({ title: 'Upload error', icon: 'none' });
        },
        complete: () => {
            uni.hideLoading();
        }
    });
};

const next = () => {
    if (currentStep.value === 0) {
        if (!form.realName || !form.city) {
            uni.showToast({ title: 'Please fill required fields', icon: 'none' });
            return;
        }
    } else if (currentStep.value === 1) {
        if (!form.subject || !form.ratePerHour) {
            uni.showToast({ title: 'Please fill required fields', icon: 'none' });
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
        uni.showToast({ title: 'Please fill required fields', icon: 'none' });
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
                console.warn('Verification submit failed', e);
            }
        }

        uni.showToast({ title: 'Submitted Successfully', icon: 'success' });
        
        // Refresh user info to update status
        await userStore.refreshUserInfo();
        
        setTimeout(() => {
            uni.navigateBack();
        }, 1500);

    } catch (error: any) {
        console.error(error);
        uni.showToast({ title: error.message || 'Submit Failed', icon: 'none' });
    }
};
</script>

<style lang="scss" scoped>
.container {
    padding: 20px;
    background-color: #fff;
    min-height: 100vh;
    display: flex;
    flex-direction: column;
}
.steps-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 30px;
    padding: 0 10px;
    
    .step-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        opacity: 0.5;
        &.active {
            opacity: 1;
            .num {
                background-color: #007aff;
                color: #fff;
                border-color: #007aff;
            }
            .text {
                color: #007aff;
            }
        }
        .num {
            width: 30px;
            height: 30px;
            border-radius: 50%;
            border: 1px solid #999;
            display: flex;
            justify-content: center;
            align-items: center;
            margin-bottom: 5px;
            font-size: 14px;
        }
        .text {
            font-size: 12px;
        }
    }
    .line {
        flex: 1;
        height: 1px;
        background-color: #ddd;
        margin: 0 10px;
        margin-bottom: 15px;
    }
}
.form-content {
    flex: 1;
}
.form-item {
    margin-bottom: 20px;
    .label {
        display: block;
        margin-bottom: 8px;
        font-size: 14px;
        color: #333;
        font-weight: bold;
    }
    .input {
        border: 1px solid #eee;
        padding: 10px;
        border-radius: 5px;
        font-size: 14px;
    }
    .textarea {
        border: 1px solid #eee;
        padding: 10px;
        border-radius: 5px;
        font-size: 14px;
        width: 100%;
        height: 100px;
    }
    .avatar-uploader {
        width: 80px;
        height: 80px;
        background-color: #f5f5f5;
        border-radius: 50%;
        display: flex;
        justify-content: center;
        align-items: center;
        overflow: hidden;
        .avatar-img {
            width: 100%;
            height: 100%;
        }
        .placeholder {
            font-size: 30px;
            color: #ccc;
        }
    }
    .cert-uploader {
        padding: 20px;
        background-color: #f5f5f5;
        text-align: center;
        border-radius: 5px;
        color: #666;
    }
    .radio {
        margin-right: 15px;
        font-size: 14px;
    }
}
.footer {
    display: flex;
    justify-content: space-between;
    margin-top: 20px;
    .btn {
        width: 45%;
        border-radius: 25px;
        &.submit {
            width: 100%;
        }
    }
}
</style>
