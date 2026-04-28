import { defineStore } from 'pinia';
import { request } from '@/utils/request';
import { userApi } from '@/api/user';
import type { CurrentUser, LoginUser, TutorStatus, UserRole } from '@/types/domain';
import { deriveTutorStatus, isTeacherUser, roleFromUserType, tutorRejectReason } from '@/utils/role';

const MOCK_LOGIN_ENABLED =
  Boolean((import.meta as any).env?.DEV) &&
  String((import.meta as any).env?.VITE_ENABLE_MP_MOCK_LOGIN || '').toLowerCase() === 'true';

export const useUserStore = defineStore('user', {
  state: () => ({
    token: uni.getStorageSync('token') || '',
    userInfo: (uni.getStorageSync('userInfo') || null) as CurrentUser | null,
    currentRole: (uni.getStorageSync('currentRole') || 'student') as UserRole,
    tutorStatus: (uni.getStorageSync('tutorStatus') || 'NONE') as TutorStatus,
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    isTutor: (state) => state.currentRole === 'tutor',
    isTeacherAccount: (state) => isTeacherUser(state.userInfo),
    canUseTutorMode: (state) => state.tutorStatus === 'APPROVED',
    tutorRejectReason: (state) => tutorRejectReason(state.userInfo?.teacherProfile),
  },
  actions: {
    setToken(token: string) {
      this.token = token;
      uni.setStorageSync('token', token);
    },
    setUserInfo(userInfo: CurrentUser | LoginUser) {
      this.userInfo = { ...this.userInfo, ...userInfo };
      uni.setStorageSync('userInfo', this.userInfo);
      this.tutorStatus = deriveTutorStatus(this.userInfo.teacherProfile);
      uni.setStorageSync('tutorStatus', this.tutorStatus);

      const cachedRole = uni.getStorageSync('currentRole') as UserRole | '';
      if (!cachedRole) this.setCurrentRole(roleFromUserType(this.userInfo.userType));
    },
    setCurrentRole(role: UserRole) {
      this.currentRole = role;
      uni.setStorageSync('currentRole', role);
    },
    async refreshUserInfo() {
        try {
            const res = await userApi.getUserInfo();
            if (res) {
                this.setUserInfo(res);
            }
            return res;
        } catch (e) {
            console.error('Failed to refresh user info', e);
            throw e;
        }
    },
    switchRole(role: UserRole) {
      if (role === 'tutor') {
          if (this.tutorStatus === 'NONE' || this.tutorStatus === 'INCOMPLETE') {
              uni.showModal({
                  title: '家教入驻',
                  content: '需要先完善家教资料并提交认证。',
                  confirmText: '去入驻',
                  success: (res) => {
                      if (res.confirm) {
                          uni.navigateTo({ url: '/pages/tutor/onboarding/index' });
                      }
                  }
              });
              return;
          } else if (this.tutorStatus === 'PENDING') {
              uni.navigateTo({ url: '/pages/tutor/status?status=PENDING' });
              return;
          } else if (this.tutorStatus === 'REJECTED') {
              uni.navigateTo({ url: `/pages/tutor/status?status=REJECTED&reason=${encodeURIComponent(this.tutorRejectReason)}` });
              return;
          }
      }
      
      this.setCurrentRole(role);
      uni.reLaunch({ url: '/pages/home/index' });
    },
    logout() {
      this.token = '';
      this.userInfo = null;
      this.currentRole = 'student';
      uni.removeStorageSync('token');
      uni.removeStorageSync('userInfo');
      uni.removeStorageSync('currentRole');
      uni.removeStorageSync('tutorStatus');
    },
    sendSmsCode(phone: string) {
      return request({
        url: '/user/sendcode',
        method: 'POST',
        data: { phone },
        loading: true
      });
    },
    async loginBySms(phone: string, code: string, role: UserRole = 'student') {
      const userRoleEnum = role === 'tutor' ? 'TEACHER' : 'STUDENT';
      const res: any = await request({
        url: '/user/loginOrRegister',
        method: 'POST',
        data: { phone, code, userRoleEnum },
        loading: true
      });
      if (res && res.token) {
        this.setToken(res.token);
        this.setUserInfo(res);
        this.setCurrentRole(role);
        await this.refreshUserInfo();
        return res;
      }
      throw new Error('No token returned');
    },
    login(preferredRole: UserRole = 'student') {
      return new Promise((resolve, reject) => {
        uni.login({
          provider: 'weixin',
          success: async (loginRes) => {
            if (loginRes.code) {
              try {
                const res: any = await request({
                  url: '/user/wechatLogin',
                  method: 'POST',
                  data: { code: loginRes.code },
                  loading: true
                });
                // Assuming res is LoginUserVO which contains token
                if (res && res.token) {
                    this.setToken(res.token);
                    this.setUserInfo(res);
                    this.setCurrentRole(preferredRole);
                    await this.refreshUserInfo();
                    resolve(res);
                } else {
                    reject(new Error('No token returned'));
                }
              } catch (error) {
                reject(error);
              }
            } else {
              reject(new Error('Login failed: ' + loginRes.errMsg));
            }
          },
          fail: async (err) => {
            if (!MOCK_LOGIN_ENABLED) {
              reject(err);
              return;
            }
            try {
                const mockCode = 'mock_user_' + Math.floor(Math.random() * 1000);
                const res: any = await request({
                    url: '/user/wechatLogin',
                    method: 'POST',
                    data: { code: mockCode },
                    loading: true
                });
                if (res && res.token) {
                    this.setToken(res.token);
                    this.setUserInfo(res);
                    this.setCurrentRole(preferredRole);
                    await this.refreshUserInfo();
                    resolve(res);
                } else {
                    reject(err);
                }
            } catch (e) {
                reject(err);
            }
          }
        });
      });
    }
  }
});
