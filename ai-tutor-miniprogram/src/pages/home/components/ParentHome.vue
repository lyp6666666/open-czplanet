<template>
  <view class="container">
    <view class="search-bar">
      <input class="search-input" type="text" v-model="keyword" placeholder="Search tutors..." @confirm="onSearch" />
    </view>
    <view class="tutor-list">
      <view v-for="tutor in tutorList" :key="tutor.userId" class="tutor-card" @click="goToDetail(tutor.userId)">
        <image class="avatar" :src="tutor.avatar || '/static/logo.png'" mode="aspectFill"></image>
        <view class="info">
          <text class="name">{{ tutor.displayName }}</text>
          <view class="tags">
            <text class="tag" v-for="(tag, index) in (tutor.subjectTags || [])" :key="index">{{ tag }}</text>
          </view>
          <text class="intro">{{ tutor.introduction }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { request } from '@/utils/request';

const keyword = ref('');
const tutorList = ref<any[]>([]);
const cursor = ref<string | null>(null);

const fetchTutors = async () => {
  try {
    const res: any = await request({
      url: '/api/v1/parent/tutors/page',
      data: {
        q: keyword.value,
        cursor: cursor.value,
        limit: 10
      },
      loading: true
    });
    // Response structure: { items: [], nextCursor: '...' }
    if (res && res.items) {
      if (cursor.value) {
          tutorList.value = [...tutorList.value, ...res.items];
      } else {
          tutorList.value = res.items;
      }
      cursor.value = res.nextCursor;
    }
  } catch (error) {
    console.error(error);
  }
};

const onSearch = () => {
  cursor.value = null;
  tutorList.value = [];
  fetchTutors();
};

const goToDetail = (id: number) => {
  uni.navigateTo({ url: `/pages/tutor/detail?id=${id}` });
};

onMounted(() => {
  fetchTutors();
});
</script>

<style lang="scss" scoped>
.container {
  padding: 10px;
}
.search-bar {
  margin-bottom: 15px;
  .search-input {
    background-color: #f5f5f5;
    padding: 10px;
    border-radius: 20px;
    font-size: 14px;
  }
}
.tutor-card {
  display: flex;
  padding: 15px;
  background-color: #fff;
  border-radius: 8px;
  margin-bottom: 10px;
  box-shadow: 0 2px 5px rgba(0,0,0,0.05);
  
  .avatar {
    width: 60px;
    height: 60px;
    border-radius: 50%;
    margin-right: 15px;
    background-color: #f0f0f0;
    flex-shrink: 0;
  }
  .info {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    
    .name {
      font-weight: bold;
      font-size: 16px;
      margin-bottom: 5px;
      color: #333;
    }
    .tags {
      display: flex;
      flex-wrap: wrap;
      margin-bottom: 5px;
      .tag {
        font-size: 12px;
        color: #007aff;
        background-color: #e6f2ff;
        padding: 2px 6px;
        border-radius: 4px;
        margin-right: 5px;
        margin-bottom: 5px;
      }
    }
    .intro {
      font-size: 12px;
      color: #999;
      display: -webkit-box;
      -webkit-box-orient: vertical;
      -webkit-line-clamp: 2;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }
}
</style>
