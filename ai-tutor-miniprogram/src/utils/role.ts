import {
  type CurrentUser,
  type TeacherProfile,
  type TutorStatus,
  type UserRole,
  USER_TYPE_STUDENT,
  USER_TYPE_TEACHER,
} from '@/types/domain';

const hasText = (v: unknown) => typeof v === 'string' && v.trim().length > 0;

export function roleFromUserType(userType?: number): UserRole {
  return userType === USER_TYPE_TEACHER ? 'tutor' : 'student';
}

export function isTeacherUser(user?: Pick<CurrentUser, 'userType'> | null): boolean {
  return user?.userType === USER_TYPE_TEACHER;
}

export function isStudentUser(user?: Pick<CurrentUser, 'userType'> | null): boolean {
  return user?.userType === USER_TYPE_STUDENT;
}

export function deriveTutorStatus(profile?: TeacherProfile | null): TutorStatus {
  if (!profile) return 'NONE';

  const realname = Number(profile.realnameVerifyStatus ?? 0);
  const education = Number(profile.eduVerifyStatus ?? 0);
  if (realname === 3 || education === 3) return 'REJECTED';

  const basicReady =
    Number(profile.basicCompleted ?? 0) === 1 ||
    (hasText(profile.realName) && hasText(profile.city) && hasText(profile.introduction));
  const resumeReady =
    Number(profile.resumeCompleted ?? 0) === 1 ||
    (hasText(profile.subject) && hasText(profile.education) && hasText(profile.highestEduSchool));

  if (!basicReady || !resumeReady) return 'INCOMPLETE';
  if (realname === 2 && education === 2) return 'APPROVED';
  if (realname === 1 || education === 1) return 'PENDING';
  return 'INCOMPLETE';
}

export function tutorRejectReason(profile?: TeacherProfile | null): string {
  return (
    profile?.realnameVerifyRejectReason?.trim() ||
    profile?.eduVerifyRejectReason?.trim() ||
    '资料不完整或认证未通过，请检查后重新提交。'
  );
}
