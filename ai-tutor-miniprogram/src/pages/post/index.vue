<template>
  <view class="container">
    <view class="form-group">
      <view class="form-item">
        <text class="label">Subject</text>
        <input class="input" v-model="form.subjectName" placeholder="e.g. Math" />
      </view>
      <view class="form-item">
        <text class="label">Grade</text>
        <input class="input" v-model="form.gradeCode" placeholder="e.g. Grade 10" />
      </view>
      <view class="form-item">
        <text class="label">City</text>
        <input class="input" v-model="form.city" placeholder="e.g. Shanghai" />
      </view>
      <view class="form-item">
        <text class="label">Class Mode</text>
        <input class="input" v-model="form.classMode" placeholder="ONLINE / OFFLINE" />
      </view>
      <view class="form-item">
        <text class="label">Budget Min</text>
        <input class="input" type="number" v-model.number="form.budgetMin" />
      </view>
      <view class="form-item">
        <text class="label">Budget Max</text>
        <input class="input" type="number" v-model.number="form.budgetMax" />
      </view>
      <view class="form-item">
        <text class="label">Description</text>
        <textarea class="textarea" v-model="form.description" placeholder="Requirements..." />
      </view>
    </view>
    <button type="primary" class="submit-btn" @click="submit">Post Demand</button>
  </view>
</template>

<script setup lang="ts">
import { reactive } from 'vue';
import { jobsApi } from '@/api/jobs';

const form = reactive({
  subjectName: '',
  subjectOther: false,
  title: 'Looking for Tutor',
  description: '',
  studentGender: 'UNKNOWN',
  gradeCode: '',
  teacherRequirementDetail: '',
  classMode: 'ONLINE',
  city: '',
  budgetMin: 0,
  budgetMax: 0,
  frequencyPerWeek: 1,
  stageCode: 'PRIMARY',
  educationRequirement: 'UNDERGRADUATE',
  publisherIdentity: 'PARENT'
});

const submit = async () => {
  if (!form.subjectName || !form.description) {
      uni.showToast({ title: 'Please fill required fields', icon: 'none' });
      return;
  }
  try {
    form.title = `Looking for ${form.subjectName} Tutor`;
    
    await jobsApi.createDemand(form);
    uni.showToast({ title: 'Posted Successfully', icon: 'success' });
    setTimeout(() => {
        uni.navigateBack();
    }, 1500);
  } catch (error: any) {
    console.error(error);
    uni.showToast({ title: error.message || 'Post Failed', icon: 'none' });
  }
};
</script>

<style lang="scss" scoped>
.container {
  padding: 20px;
  background-color: #f8f8f8;
  min-height: 100vh;
}
.form-group {
  background-color: #fff;
  border-radius: 10px;
  padding: 15px;
  margin-bottom: 20px;
}
.form-item {
  margin-bottom: 15px;
  .label {
    display: block;
    font-size: 14px;
    color: #666;
    margin-bottom: 5px;
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
}
.submit-btn {
  width: 100%;
  border-radius: 25px;
}
</style>
