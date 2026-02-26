export interface BaseResponse<T> {
  code: number
  data: T
  message: string
}

export interface CursorPageRequest {
  pageSize?: number
  cursor?: number | null
}

export interface CursorPageResponse<T> {
  nextCursor: number | null
  isLast: boolean
  list: T[]
}

export interface SubjectTreeNode {
  id: number
  parentId: number
  name: string
  grade?: string
  description?: string
  children: SubjectTreeNode[]
}

export interface HomeConfigVO {
  defaultCity: string
  citySelectable: boolean
  search: null | {
    placeholder: string
    defaultMode: string
  }
  nav: Array<{
    key: string
    name: string
    link: string
  }>
  authEntry: null | {
    loginText: string
    link: string
  }
}

export interface GeoLocateVO {
  ip: string
  city: string
  province: string
  cityCode: string
  suggestCities: string[]
}

export interface HotWordsVO {
  updatedAt: string
  list: Array<{
    word: string
    type: string
  }>
}

export interface SearchSuggestVO {
  q: string
  list: Array<{
    type: string
    title: string
    subtitle: string
    payload: Record<string, unknown>
  }>
}

export interface BannersVO {
  carousel: Array<{
    id: string
    title: string
    subtitle: string
    imageUrl: string
    link?: {
      type: string
      url: string
    } | null
  }>
  cards: Array<{
    id: string
    title: string
    subtitle: string
    imageUrl: string
    link?: {
      type: string
      url: string
    } | null
  }>
}

export interface HotTabsVO {
  type: string
  tabs: Array<{
    tabId: string
    name: string
    params: Record<string, unknown>
  }>
}

export interface HotServiceCardVO {
  serviceId: number
  title: string
  subject: { id: number; name: string }
  pricePerHour: string
  mode: string
  city: string
  tutor: {
    userId: number
    displayName: string
    avatar: string
    education: string
    experienceYears: number
    ratePerHour: string
  }
  tags: string[]
}

export interface HotDemandCardVO {
  demandId: number
  title: string
  subject: { id: number; name: string }
  budget: { min: string; max: string; unit: string }
  classMode: string
  city: string
  addressSimple: string
  childAge: number
  scheduleText: string
  parent: { userId: number; displayName: string; avatar: string }
  tags: string[]
}

export interface HotTutorCardVO {
  userId: number
  displayName: string
  avatar: string
  city: string
  education: string
  experienceYears: number
  ratePerHour: string
  subjectTags: string[]
  highlights: string[]
  representativeServices: Array<{
    serviceId: number
    title: string
    pricePerHour: string
  }>
}

export interface FooterLinksVO {
  links: Array<{ name: string; url: string }>
}

export type UserRoleEnum = 'TEACHER' | 'STUDENT'

export interface LoginUserVO {
  id: number
  name: string
  phone: string
  avatar: string | null
  sex: number | null
  userType: number
  token: string
  isNew?: boolean
}

export interface TeacherProfile {
  id: number
  userId: number
  realName: string
  education: string | null
  subject: string | null
  experienceYears: number | null
  ratePerHour: string | null
  introduction: string | null
  city?: string | null
  highestEduSchool?: string | null
  teachingMode?: string | null
  defaultGreeting?: string | null
  certificateUrls: string | null
  basicCompleted?: number | null
  resumeCompleted?: number | null
  realnameVerifyStatus?: number | null
  realnameVerifyMethod?: string | null
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
  status: number
  createTime: string
  updateTime: string
}

export interface StudentProfile {
  id: number
  userId: number
  realName: string
  age: number | null
  childAge: number | null
  address: string | null
  demandDescription: string | null
  budget: string | null
  status: number
  createTime: string
  updateTime: string
}

export interface UserMeVO {
  id: number
  name: string
  phone: string
  avatar: string | null
  sex: number | null
  userType: number
  teacherProfile?: TeacherProfile | null
  studentProfile?: StudentProfile | null
}

export interface UserSettingsVO {
  applicationGreeting: string
}

export interface UserSimpleVO {
  id: number
  name: string
  avatar: string | null
  userType: number
}

export interface UserCardVO {
  user: UserSimpleVO
  teacherProfile: TeacherProfile | null
  studentProfile: StudentProfile | null
  jobPosting: StudentJobPosting | null
}

export interface StudentJobPosting {
  id: number
  parentId: number
  subjectId: number | null
  subjectName: string | null
  subjectIsOther: number | null
  title: string
  description: string | null
  studentGender: string | null
  gradeCode: string | null
  availableTime: string | null
  teacherGenderPreference: string | null
  teacherRequirementDetail: string | null
  childAge: number | null
  classMode: string | null
  city: string | null
  address: string | null
  frequencyPerWeek: number | null
  budgetMin: string | null
  budgetMax: string | null
  stageCode: string | null
  educationRequirement: string | null
  publisherIdentity: string | null
  schedule: string | null
  status: number
  createTime: string
  updateTime: string
}

export interface DemandPublisherVO {
  uid: number
  displayName: string
  avatar: string | null
  identityLabel: string
}

export interface DemandViewVO extends StudentJobPosting {
  publisher: DemandPublisherVO
}

export interface ChatRoomItemResp {
  roomId: number
  otherUid: number
  lastMsgId: number | null
  lastMsgBody: unknown
  unreadCount: number
  activeTime: string
}

export interface CursorPageResp<T> {
  cursor: number | null
  isLast: boolean
  list: T[]
}

export type TutorApplicationStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED'
export type TutorApplicationChatAccessStatus = 'NONE' | 'PAYMENT_REQUIRED' | 'CHAT_ENABLED'

export interface TutorApplicationVO {
  id: number
  senderUid: number
  receiverUid: number
  senderRole: 'TEACHER' | 'STUDENT'
  receiverRole: 'TEACHER' | 'STUDENT'
  contextType: 'DEMAND' | 'TUTOR'
  contextId: number
  content: string
  status: TutorApplicationStatus
  chatAccessStatus: TutorApplicationChatAccessStatus
  paymentPayerRole: 'TEACHER'
  orderId: number | null
  roomId: number | null
  receiverRead: boolean | null
  decidedAt: string | null
  createTime: string
}

export interface TutorApplicationUnreadResp {
  unreadCount: number
}

export interface TutorApplicationEnterResp {
  paymentRequired: boolean
  waitingForTeacherPayment: boolean
  orderId: number | null
  roomId: number | null
}

export interface ChatMessageResp {
  fromUser: { uid: number }
  message: { id: number; roomId: number; sendTime: string; body: unknown }
}

export type ScheduleEventStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'CANCELED' | 'UNKNOWN'
export type CollaborationProposalStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'UNKNOWN'
export type TutorApplicationCardStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED'

export interface ScheduleEventVO {
  id: number
  title: string
  description: string | null
  startAt: number
  endAt: number
  status: ScheduleEventStatus
  creatorUserId: number
  participant: UserSimpleVO | null
  chatRoomId: number | null
}

export type ChatMessageBody =
  | { type: 'text'; content: string }
  | {
      type: 'tutor_application'
      applicationId: number
      content: string
      status: TutorApplicationCardStatus
      creatorUserId: number
      contextType: 'DEMAND' | 'TUTOR'
      contextId: number
    }
  | { type: 'tutor_application_status'; applicationId: number; status: TutorApplicationCardStatus; actorUserId: number }
  | {
      type: 'lesson_request'
      eventId: number
      title: string
      startAt: number
      endAt: number
      status: ScheduleEventStatus
      creatorUserId: number
    }
  | {
      type: 'lesson_status'
      eventId: number
      title: string
      startAt: number
      endAt: number
      status: ScheduleEventStatus
      actorUserId: number
    }
  | {
      type: 'collaboration_proposal'
      proposalId: number
      pricePerHour: string
      classTime: string
      frequencyPerWeek: number
      status: CollaborationProposalStatus
      creatorUserId: number
    }
  | {
      type: 'collaboration_status'
      proposalId: number
      status: CollaborationProposalStatus
      actorUserId: number
    }
  | {
      type: 'brokerage_required'
      orderId: number
      proposalId: number | null
      amountFen: number | null
      status: string | null
      payerUserId: number | null
    }
  | {
      type: 'contact_unlocked'
      proposalId: number | null
      orderId: number | null
      status: string | null
    }
  | {
      type: 'brokerage_refund_request'
      requestId: number | null
      status: string | null
      creatorUserId: number | null
    }
  | {
      type: 'brokerage_refund_status'
      requestId: number | null
      status: string | null
      actorUserId: number | null
    }
  | { type: 'system'; content?: string; [k: string]: unknown }

export interface CursorPageBaseResp<T> {
  cursor: string | null
  isLast: boolean
  list: T[]
}
