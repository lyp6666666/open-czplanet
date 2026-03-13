import { request } from '@/utils/request';

export const userApi = {
  // 获取当前用户信息（包含扩展资料）
  getUserInfo() {
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
  }
};
