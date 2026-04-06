export interface BaseResponse<T> {
  code: number
  data: T
  message: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
}

export interface AdminLoginResponse {
  token: string
  id: number
  nickname: string | null
}

export interface AdminOrganizationCreateRequest {
  orgName: string
  username?: string | null
  initialPassword?: string | null
  contactName?: string | null
  contactPhone: string
  address?: string | null
  intro?: string | null
  licenseNo?: string | null
  splitPlatformPercent?: number | null
  splitOrgPercent?: number | null
}

export interface AdminOrganizationCreateResponse {
  orgUserId: number
  username: string
  initialPassword: string
}

export interface AdminOrganizationRow {
  orgUserId: number
  orgName: string | null
  username: string | null
  contactPhone: string | null
  userStatus: number | null
  accountStatus: number | null
  mustChangePassword: number | null
  lastLoginTime: string | null
  createTime: string | null
  updateTime: string | null
}

export interface AdminOrganizationDetail {
  orgUserId: number
  orgName: string | null
  username: string | null
  accountStatus: number | null
  mustChangePassword: number | null
  lastLoginTime: string | null
  userStatus: number | null
  contactName: string | null
  contactPhone: string | null
  address: string | null
  intro: string | null
  licenseNo: string | null
  splitPlatformPercent: number | null
  splitOrgPercent: number | null
  createTime: string | null
  updateTime: string | null
}

export interface AdminOrganizationUpdateRequest {
  orgName?: string | null
  username?: string | null
  initialPassword?: string | null
  accountStatus?: number | null
  contactName?: string | null
  contactPhone?: string | null
  address?: string | null
  intro?: string | null
  licenseNo?: string | null
  splitPlatformPercent?: number | null
  splitOrgPercent?: number | null
}

export interface DashboardStatsResponse {
  totalUsers: number
  activeTeachers: number
  pendingJobs: number
  pendingVerifications: number
  pendingRefunds: number
}

export interface StudentJobPosting {
  id: number
  parentId: number
  subjectId: number
  subjectName?: string | null
  title?: string | null
  description?: string | null
  classMode?: string | null
  city?: string | null
  address?: string | null
  budgetMin?: string | number | null
  budgetMax?: string | number | null
  createTime?: string | null
  updateTime?: string | null
  status?: number | null
  rejectReason?: string | null
}

export interface TeacherProfile {
  userId: number
  realName?: string | null
  education?: string | null
  subject?: string | null
  city?: string | null
  highestEduSchool?: string | null

  realnameVerifyStatus?: number | null
  realnameVerifyIdFrontUrl?: string | null
  realnameVerifyIdBackUrl?: string | null
  realnameVerifyIdnoMasked?: string | null
  realnameVerifyRejectReason?: string | null
  realnameVerifySubmitTime?: string | null
  realnameVerifyTime?: string | null

  eduVerifyStatus?: number | null
  eduVerifyProofUrls?: string | null
  eduVerifyRejectReason?: string | null
  eduVerifySubmitTime?: string | null
  eduVerifyTime?: string | null
}

export interface StudentProfile {
  userId: number
  realName?: string | null
  age?: number | null
  address?: string | null
  demandDescription?: string | null
  budget?: number | string | null
  status?: number | null
  createTime?: string | null
  updateTime?: string | null
}

export interface User {
  id: number
  name?: string | null
  phone: string
  avatar?: string | null
  sex?: number | null
  activeStatus?: number | null
  status?: number | null
  userType: number
  createTime?: string | null
  updateTime?: string | null
}

export interface AdminUserRow {
  id: number
  name?: string | null
  phone: string
  avatar?: string | null
  sex?: number | null
  status?: number | null
  activeStatus?: number | null
  userType: number
  createTime?: string | null
  updateTime?: string | null

  teacherRealName?: string | null
  teacherEducation?: string | null
  teacherSubject?: string | null
  teacherCity?: string | null
  teacherRatePerHour?: number | string | null
  teacherRealnameVerifyStatus?: number | null
  teacherEduVerifyStatus?: number | null
  teacherProfileStatus?: number | null

  studentRealName?: string | null
  studentAge?: number | null
  studentAddress?: string | null
  studentDemandDescription?: string | null
  studentBudget?: number | string | null
  studentProfileStatus?: number | null
}

export interface AdminUserDetail {
  user: User
  teacherProfile?: TeacherProfile | null
  studentProfile?: StudentProfile | null
}

export interface BrokerageOrder {
  id: number
  proposalId?: number | null
  applicationId?: number | null
  roomId?: number | null
  payerUid?: number | null
  amountFen?: number | null
  payMethod?: string | null
  status?: string | null
  proofUrl?: string | null
  proofNote?: string | null
  paidAt?: string | null
  createTime?: string | null
  updateTime?: string | null
}

export interface Message {
  id: number
  roomId: number
  fromUid: number
  toUid: number
  content?: string | null
  type?: number | null
  status?: number | null
  createTime?: string | null
}

export interface RefundRequestRecord {
  id: number
  brokerageOrderId: number
  courseId?: number | null
  roomId?: number | null
  applicantUid: number
  applicantRole: string
  type: string
  status: string
  reason?: string | null
  evidenceImagesJson?: string | null
  refundPercent: number
  refundAmountFen: number
  adminUid?: number | null
  adminNote?: string | null
  decidedAt?: string | null
  createTime?: string | null
  updateTime?: string | null
}

export interface RefundRequestDetailResponse {
  refundRequest: RefundRequestRecord
  order: BrokerageOrder | null
  chatHistory: Message[] | null
}
