import { request } from '@/utils/request';

export const jobsApi = {
  // 发布需求
  createDemand(data: any) {
    return request({
      url: '/api/v1/parent/jobs',
      method: 'POST',
      data
    });
  },
  // 更新需求
  updateDemand(id: number, data: any) {
    return request({
      url: `/api/v1/parent/jobs/${id}`,
      method: 'PUT',
      data
    });
  },
  // 我的需求
  mineDemands(params: any) {
    return request({
      url: '/api/v1/parent/jobs/mine',
      method: 'GET',
      data: params
    });
  },
  // 需求广场 (Feed)
  feedDemands(params: any) {
    return request({
      url: '/api/v1/parent/jobs/feed',
      method: 'GET',
      data: params
    });
  },
  // 需求详情
  getDemand(id: number) {
    return request({
      url: `/api/v1/parent/jobs/${id}`,
      method: 'GET'
    });
  },
  // 需求详情（教师端视图，包含发布者信息）
  getDemandView(id: number) {
    return request({
      url: `/api/v1/parent/jobs/${id}/view`,
      method: 'GET'
    });
  }
};
