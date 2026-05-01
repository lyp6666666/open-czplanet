<template>
  <view class="page">
    <web-view v-if="url" class="webview" :src="url" />
    <AppStateCard
      v-else
      title="课堂地址缺失"
      description="当前没有可打开的课堂页面地址。"
      variant="error"
    />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onHide, onLoad, onUnload } from '@dcloudio/uni-app';
import AppStateCard from '@/components/AppStateCard.vue';
import { markLiveReturnRefresh } from '@/utils/liveRefresh';

const url = ref('');
const courseId = ref<number | null>(null);
const sessionId = ref<number | null>(null);
const eventId = ref<number | null>(null);

function markReturned() {
  markLiveReturnRefresh({
    courseId: courseId.value,
    sessionId: sessionId.value,
    eventId: eventId.value,
    source: 'live_webview',
  });
}

onLoad((options: any) => {
  url.value = String(options?.url || '').trim();
  const cid = Number(options?.courseId);
  const sid = Number(options?.sessionId);
  const eid = Number(options?.eventId);
  courseId.value = Number.isFinite(cid) ? cid : null;
  sessionId.value = Number.isFinite(sid) ? sid : null;
  eventId.value = Number.isFinite(eid) ? eid : null;
  const title = String(options?.title || '').trim();
  if (title) {
    uni.setNavigationBarTitle({ title });
  }
});

onHide(() => {
  markReturned();
});

onUnload(() => {
  markReturned();
});
</script>

<style scoped lang="scss">
.page,
.webview {
  width: 100%;
  height: 100vh;
}

.page {
  background: #f4f7f7;
}
</style>
