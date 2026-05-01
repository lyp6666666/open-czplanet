import { request } from '@/utils/request';
import type { CurrentUser } from '@/types/domain';

export type UserSimple = {
  id: number;
  uid?: number;
  name?: string;
  realName?: string;
  nickname?: string;
  avatar?: string;
  phone?: string;
  userType?: number;
};

export const userApi = {
  // 获取当前用户信息（包含扩展资料）
  getUserInfo(): Promise<CurrentUser> {
    return request({
      url: '/user/me',
      method: 'GET'
    });
  },
  // 更新用户信息
  updateUserInfo(data: any) {
    return request({
      url: '/user/updateUserInfo',
      method: 'POST',
      data
    });
  },
  emailStatus() {
    return request({
      url: '/user/email',
      method: 'GET'
    });
  },
  sendEmailCode(data: { email: string; emailType: 'PRIMARY' | 'SUMMARY_ONLY'; scene?: string }) {
    return request({
      url: '/user/email/code',
      method: 'POST',
      data
    });
  },
  verifyEmail(data: { email: string; emailType: 'PRIMARY' | 'SUMMARY_ONLY'; code: string; scene?: string; bindSource?: string }) {
    return request({
      url: '/user/email/verify',
      method: 'POST',
      data
    });
  },
  deleteSummaryEmail() {
    return request({
      url: '/user/email/summary',
      method: 'DELETE'
    });
  },
  batch(ids: number[]) {
    return request({
      url: '/user/batch',
      method: 'GET',
      data: { ids: ids.join(',') },
      silentError: true,
    }) as Promise<UserSimple[]>;
  }
};
