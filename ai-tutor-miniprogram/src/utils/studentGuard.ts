import { useUserStore } from '@/stores/user';
import { currentPageUrl, goLoginWithRedirect, setPendingRedirect } from '@/utils/authRedirect';

export function ensureStudentMode(message = '请切换到学生/家长身份后使用该功能。', intent?: string) {
  const userStore = useUserStore();
  if (!userStore.isLoggedIn) {
    uni.showToast({ title: '请先登录', icon: 'none' });
    setTimeout(() => {
      goLoginWithRedirect(currentPageUrl(), 'student', intent);
    }, 500);
    return false;
  }

  if (userStore.currentRole === 'student') return true;

  uni.showModal({
    title: '切换到学生端',
    content: message,
    confirmText: '切换',
    success: (res) => {
      if (res.confirm) {
        setPendingRedirect(currentPageUrl(), 'student', intent);
        userStore.setCurrentRole('student');
        uni.reLaunch({ url: '/pages/home/index' });
      }
    }
  });
  return false;
}
