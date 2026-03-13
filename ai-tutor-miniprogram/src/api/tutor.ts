import { request } from '@/utils/request';

export const tutorApi = {
  // 提交学历认证
  submitEducation(proofUrls: string[]) {
    return request({
      url: '/teacher/verification/education/submit',
      method: 'POST',
      data: { proofUrls }
    });
  },
  // 提交实名认证
  submitRealname(data: any) {
    return request({
      url: '/teacher/verification/realname/submit',
      method: 'POST',
      data
    });
  }
};
