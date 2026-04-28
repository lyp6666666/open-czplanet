import { request } from '@/utils/request';

export interface ParentTutorCard {
  userId: number;
  displayName: string;
  avatar?: string | null;
  city?: string | null;
  education?: string | null;
  experienceYears?: number | null;
  ratePerHour?: string | number | null;
  subjectTags?: string[];
  highlights?: string[];
  introduction?: string | null;
  highestEduSchool?: string | null;
  eduVerifyStatus?: number | null;
}

export interface ParentTutorPageParams {
  pageSize?: number;
  cursor?: number | null;
  q?: string;
  city?: string;
  mode?: string;
  subject?: string;
  rateMin?: number | null;
  rateMax?: number | null;
}

export interface CursorPageResponse<T> {
  nextCursor?: number | null;
  isLast?: boolean;
  list?: T[];
}

export const parentTutorsApi = {
  page(params: ParentTutorPageParams): Promise<CursorPageResponse<ParentTutorCard>> {
    return request({
      url: '/api/v1/parent/tutors/page',
      method: 'GET',
      data: params,
      silentError: true,
    });
  },
};
