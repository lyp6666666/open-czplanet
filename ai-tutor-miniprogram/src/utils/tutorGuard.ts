import { useUserStore } from '@/stores/user';
import type { TutorStatus } from '@/types/domain';
import { currentPageUrl, goLoginWithRedirect } from '@/utils/authRedirect';

export function tutorStatusUrl(status: TutorStatus, reason = '') {
  const query = reason ? `&reason=${encodeURIComponent(reason)}` : '';
  return `/pages/tutor/status?status=${status}${query}`;
}

export function goTutorStatus(status: TutorStatus, reason = '') {
  uni.navigateTo({ url: tutorStatusUrl(status, reason) });
}

export function ensureTutorApproved(message = '教师资料审核通过后才能使用该功能。', intent?: string) {
  const userStore = useUserStore();
  if (!userStore.isLoggedIn) {
    uni.showToast({ title: '请先登录', icon: 'none' });
    setTimeout(() => {
      goLoginWithRedirect(currentPageUrl(), 'tutor', intent);
    }, 500);
    return false;
  }

  const status = userStore.tutorStatus;
  if (status === 'APPROVED') return true;

  if (status === 'NONE' || status === 'INCOMPLETE') {
    uni.showModal({
      title: '家教入驻',
      content: '需要先完善家教资料并提交认证。',
      confirmText: '去入驻',
      success: (res) => {
        if (res.confirm) uni.navigateTo({ url: '/pages/tutor/onboarding/index' });
      }
    });
    return false;
  }

  uni.showToast({ title: message, icon: 'none' });
  setTimeout(() => {
    goTutorStatus(status, userStore.tutorRejectReason);
  }, 500);
  return false;
}
