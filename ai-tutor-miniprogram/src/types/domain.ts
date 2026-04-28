export type UserRole = 'student' | 'tutor';

export type TutorStatus = 'NONE' | 'INCOMPLETE' | 'PENDING' | 'APPROVED' | 'REJECTED';

export const USER_TYPE_TEACHER = 1;
export const USER_TYPE_STUDENT = 2;
export const USER_TYPE_ORG = 3;

export interface TeacherProfile {
  id?: number;
  userId?: number;
  realName?: string;
  education?: string;
  subject?: string;
  experienceYears?: number;
  ratePerHour?: number | string;
  introduction?: string;
  defaultGreeting?: string;
  certificateUrls?: string;
  basicCompleted?: number;
  resumeCompleted?: number;
  city?: string;
  highestEduSchool?: string;
  teachingMode?: string;
  realnameVerifyStatus?: number;
  realnameVerifyRejectReason?: string;
  realnameVerifyIdnoMasked?: string;
  realnameVerifyIdFrontUrl?: string;
  realnameVerifyIdBackUrl?: string;
  eduVerifyStatus?: number;
  eduVerifyRejectReason?: string;
  eduVerifyProofUrls?: string;
  status?: number;
}

export interface StudentProfile {
  id?: number;
  userId?: number;
  realName?: string;
  city?: string;
  childAge?: number;
  address?: string;
  demandDescription?: string;
  budget?: number;
  status?: number;
}

export interface LoginUser {
  id: number;
  name?: string;
  avatar?: string;
  phone?: string;
  sex?: number;
  userType?: number;
  isNew?: boolean;
  token?: string;
  openid?: string;
  redirectRoomId?: number;
  redirectOtherUid?: number;
}

export interface CurrentUser extends LoginUser {
  teacherProfile?: TeacherProfile | null;
  studentProfile?: StudentProfile | null;
  organizationProfile?: unknown;
}

export interface PageResult<T> {
  list?: T[];
  items?: T[];
  nextCursor?: number | string | null;
  total?: number;
}
