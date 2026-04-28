import { request } from '@/utils/request';

export const favoritesApi = {
  favoriteDemand(demandId: number) {
    return request({
      url: `/api/v1/tutor/favorites/demands/${demandId}`,
      method: 'POST'
    });
  },
  unfavoriteDemand(demandId: number) {
    return request({
      url: `/api/v1/tutor/favorites/demands/${demandId}`,
      method: 'DELETE'
    });
  },
  checkDemandFavorites(ids: number[]) {
    return request({
      url: '/api/v1/tutor/favorites/demands/check',
      method: 'GET',
      data: { ids: ids.join(',') }
    }) as Promise<number[]>;
  },
  pageDemandFavorites(params: { pageSize?: number; cursor?: number | null }) {
    return request({
      url: '/api/v1/tutor/favorites/demands/page',
      method: 'GET',
      data: params,
      silentError: true,
    });
  },
  favoriteTutor(tutorId: number) {
    return request({
      url: `/api/v1/parent/favorites/tutors/${tutorId}`,
      method: 'POST'
    });
  },
  unfavoriteTutor(tutorId: number) {
    return request({
      url: `/api/v1/parent/favorites/tutors/${tutorId}`,
      method: 'DELETE'
    });
  },
  checkTutorFavorites(ids: number[]) {
    return request({
      url: '/api/v1/parent/favorites/tutors/check',
      method: 'GET',
      data: { ids: ids.join(',') }
    }) as Promise<number[]>;
  },
  pageTutorFavorites(params: { pageSize?: number; cursor?: number | null }) {
    return request({
      url: '/api/v1/parent/favorites/tutors/page',
      method: 'GET',
      data: params,
      silentError: true,
    });
  }
};
