import { createRouter, createWebHashHistory } from 'vue-router'

const HomePage = () => import('@/pages/HomePage.vue')
const AboutPage = () => import('@/pages/AboutPage.vue')
const AuthPage = () => import('@/pages/AuthPage.vue')
const MePage = () => import('@/pages/MePage.vue')
const PrivacyPolicyPage = () => import('@/pages/PrivacyPolicyPage.vue')
const SettingsPage = () => import('@/pages/SettingsPage.vue')
const UpdatePhonePage = () => import('@/pages/UpdatePhonePage.vue')
const EmailSettingsPage = () => import('@/pages/EmailSettingsPage.vue')
const StudentPostPage = () => import('@/pages/student/StudentPostPage.vue')
const StudentOnboardingFirstDemandPage = () => import('@/pages/student/StudentOnboardingFirstDemandPage.vue')
const StudentMineJobsPage = () => import('@/pages/student/StudentMineJobsPage.vue')
const StudentMineJobDetailPage = () => import('@/pages/student/StudentMineJobDetailPage.vue')
const StudentEditJobPage = () => import('@/pages/student/StudentEditJobPage.vue')
const StudentFavoritesPage = () => import('@/pages/student/StudentFavoritesPage.vue')
const StudentTutorsPage = () => import('@/pages/student/StudentTutorsPage.vue')
const TutorJobsPage = () => import('@/pages/tutor/TutorJobsPage.vue')
const TutorJobDetailPage = () => import('@/pages/tutor/TutorJobDetailPage.vue')
const TutorFavoritesPage = () => import('@/pages/tutor/TutorFavoritesPage.vue')
const TutorOnboardingBasicPage = () => import('@/pages/tutor/TutorOnboardingBasicPage.vue')
const TutorOnboardingProfilePage = () => import('@/pages/tutor/TutorOnboardingProfilePage.vue')
const ChatListPage = () => import('@/pages/chat/ChatListPage.vue')
const ChatRoomPage = () => import('@/pages/chat/ChatRoomPage.vue')
const SchedulePage = () => import('@/pages/schedule/SchedulePage.vue')
const BrokeragePayPage = () => import('@/pages/pay/BrokeragePayPage.vue')
const CashierPayPage = () => import('@/pages/pay/CashierPayPage.vue')
const MyCoursesPage = () => import('@/pages/course/MyCoursesPage.vue')
const CourseDetailPage = () => import('@/pages/course/CourseDetailPage.vue')
const LessonAiSummaryPage = () => import('@/pages/course/LessonAiSummaryPage.vue')
const LivePreparePage = () => import('@/pages/live/LivePreparePage.vue')
const LivePermissionGuidePage = () => import('@/pages/live/LivePermissionGuidePage.vue')
const LiveClassroomPage = () => import('@/pages/live/LiveClassroomPage.vue')
const OrgAuthPage = () => import('@/pages/org/OrgAuthPage.vue')
const OrgChangePasswordPage = () => import('@/pages/org/OrgChangePasswordPage.vue')
const OrgPostPage = () => import('@/pages/org/OrgPostPage.vue')
const OrgTutorsPage = () => import('@/pages/org/OrgTutorsPage.vue')
const OrgFavoritesPage = () => import('@/pages/org/OrgFavoritesPage.vue')
const OrgMineJobsPage = () => import('@/pages/org/OrgMineJobsPage.vue')
const OrgMineJobDetailPage = () => import('@/pages/org/OrgMineJobDetailPage.vue')
const OrgPublicProfilePage = () => import('@/pages/org/OrgPublicProfilePage.vue')
const GuideFlowPage = () => import('@/pages/GuideFlowPage.vue')
const InviteRewardPage = () => import('@/pages/InviteRewardPage.vue')

const STORAGE_TOKEN_KEY = 'ai_tutor_token'
const STORAGE_USER_KEY = 'ai_tutor_user'
const STORAGE_TUTOR_BASIC_COMPLETED_KEY = 'ai_tutor_tutor_basic_completed'

function readAuthFromStorage(): { token: string | null; userType: number | null } {
  const rawToken = localStorage.getItem(STORAGE_TOKEN_KEY)
  const token = typeof rawToken === 'string' && rawToken.trim() ? rawToken.trim() : null
  const rawUser = localStorage.getItem(STORAGE_USER_KEY)
  if (!rawUser) return { token, userType: null }
  try {
    const u = JSON.parse(rawUser) as { userType?: unknown }
    const userType = typeof u.userType === 'number' ? u.userType : Number(u.userType)
    return { token, userType: Number.isFinite(userType) ? userType : null }
  } catch {
    return { token, userType: null }
  }
}

export const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomePage,
    },
    {
      path: '/about',
      name: 'about',
      component: AboutPage,
    },
    {
      path: '/privacy',
      name: 'privacyPolicy',
      component: PrivacyPolicyPage,
    },
    {
      path: '/auth/tutor',
      name: 'authTutor',
      component: AuthPage,
      props: { role: 'TEACHER' },
    },
    {
      path: '/auth/student',
      name: 'authStudent',
      component: AuthPage,
      props: { role: 'STUDENT' },
    },
    {
      path: '/auth/org',
      name: 'authOrg',
      component: OrgAuthPage,
    },
    {
      path: '/org/change-password',
      name: 'orgChangePassword',
      component: OrgChangePasswordPage,
    },
    {
      path: '/org/tutors',
      name: 'orgTutors',
      component: OrgTutorsPage,
    },
    {
      path: '/org/post',
      name: 'orgPost',
      component: OrgPostPage,
    },
    {
      path: '/org/jobs/mine',
      name: 'orgMineJobs',
      component: OrgMineJobsPage,
    },
    {
      path: '/org/favorites',
      name: 'orgFavorites',
      component: OrgFavoritesPage,
    },
    {
      path: '/org/jobs/:id',
      name: 'orgMineJobDetail',
      component: OrgMineJobDetailPage,
    },
    {
      path: '/org/jobs/:id/edit',
      name: 'orgEditJob',
      component: StudentEditJobPage,
    },
    {
      path: '/organization/:orgUserId',
      name: 'orgPublicProfile',
      component: OrgPublicProfilePage,
    },
    {
      path: '/guide/tutor',
      name: 'guideTutor',
      component: GuideFlowPage,
      props: { audience: 'tutor' },
    },
    {
      path: '/guide/student',
      name: 'guideStudent',
      component: GuideFlowPage,
      props: { audience: 'student' },
    },
    {
      path: '/me',
      name: 'me',
      component: MePage,
    },
    {
      path: '/settings',
      name: 'settings',
      component: SettingsPage,
    },
    {
      path: '/invite',
      name: 'inviteReward',
      component: InviteRewardPage,
    },
    {
      path: '/settings/phone',
      name: 'updatePhone',
      component: UpdatePhonePage,
    },
    {
      path: '/settings/email',
      name: 'emailSettings',
      component: EmailSettingsPage,
    },
    {
      path: '/student/post',
      name: 'studentPost',
      component: StudentPostPage,
    },
    {
      path: '/student/onboarding/first-demand',
      name: 'studentOnboardingFirstDemand',
      component: StudentOnboardingFirstDemandPage,
    },
    {
      path: '/student/jobs/mine',
      name: 'studentMineJobs',
      component: StudentMineJobsPage,
    },
    {
      path: '/student/jobs/:id',
      name: 'studentMineJobDetail',
      component: StudentMineJobDetailPage,
    },
    {
      path: '/student/jobs/:id/edit',
      name: 'studentEditJob',
      component: StudentEditJobPage,
    },
    {
      path: '/student/favorites',
      name: 'studentFavorites',
      component: StudentFavoritesPage,
    },
    {
      path: '/student/tutors',
      name: 'studentTutors',
      component: StudentTutorsPage,
    },
    {
      path: '/tutor/jobs',
      name: 'tutorJobs',
      component: TutorJobsPage,
    },
    {
      path: '/tutor/onboarding/basic',
      name: 'tutorOnboardingBasic',
      component: TutorOnboardingBasicPage,
    },
    {
      path: '/tutor/onboarding/profile',
      name: 'tutorOnboardingProfile',
      component: TutorOnboardingProfilePage,
    },
    {
      path: '/tutor/jobs/:id',
      name: 'tutorJobDetail',
      component: TutorJobDetailPage,
    },
    {
      path: '/tutor/favorites',
      name: 'tutorFavorites',
      component: TutorFavoritesPage,
    },
    {
      path: '/chat',
      name: 'chatList',
      component: ChatListPage,
      children: [
        {
          path: ':roomId',
          name: 'chatRoom',
          component: ChatRoomPage,
        },
      ],
    },
    {
      path: '/schedule',
      name: 'schedule',
      component: SchedulePage,
    },
    {
      path: '/pay/brokerage',
      name: 'brokeragePay',
      component: BrokeragePayPage,
    },
    {
      path: '/pay/cashier',
      name: 'cashierPay',
      component: CashierPayPage,
    },
    {
      path: '/courses/my',
      name: 'myCourses',
      component: MyCoursesPage,
    },
    {
      path: '/courses/:courseId',
      name: 'courseDetail',
      component: CourseDetailPage,
    },
    {
      path: '/courses/:courseId/ai-summary',
      name: 'lessonAiSummary',
      component: LessonAiSummaryPage,
    },
    {
      path: '/live/prepare/:courseId',
      name: 'livePrepare',
      component: LivePreparePage,
    },
    {
      path: '/live/permission-help',
      name: 'livePermissionHelp',
      component: LivePermissionGuidePage,
    },
    {
      path: '/live/classroom/:courseId',
      name: 'liveClassroom',
      component: LiveClassroomPage,
    },
  ],
})

router.beforeEach((to) => {
  const { token, userType } = readAuthFromStorage()
  const loggedIn = typeof token === 'string' && token.length > 0

  if (loggedIn && to.path.startsWith('/auth/')) {
    if (userType === 1) return { path: '/tutor/jobs' }
    if (userType === 2) return { path: '/student/post' }
    if (userType === 3) return { path: '/org/jobs/mine' }
    return { path: '/' }
  }

  const needAuth =
    to.path === '/me' ||
    to.path === '/invite' ||
    to.path.startsWith('/settings') ||
    to.path === '/schedule' ||
    to.path.startsWith('/courses') ||
    to.path.startsWith('/live/') ||
    to.path.startsWith('/pay/') ||
    to.path.startsWith('/student/') ||
    to.path.startsWith('/tutor/') ||
    to.path.startsWith('/org/') ||
    to.path.startsWith('/chat')

  if (needAuth && !loggedIn) {
    if (to.path.startsWith('/tutor/')) return { path: '/auth/tutor' }
    if (to.path.startsWith('/org/')) return { path: '/auth/org' }
    return { path: '/auth/student' }
  }

  if (loggedIn && userType != null) {
    if (to.path.startsWith('/tutor/') && userType !== 1) return { path: '/student/post' }
    if (to.path.startsWith('/student/') && userType !== 2) return { path: '/tutor/jobs' }
    if (to.path.startsWith('/org/') && userType !== 3) return { path: '/' }
  }

  if (loggedIn && userType === 1 && to.path.startsWith('/tutor/')) {
    const completed = localStorage.getItem(STORAGE_TUTOR_BASIC_COMPLETED_KEY)
    if (to.path.startsWith('/tutor/onboarding')) {
      if (to.path.startsWith('/tutor/onboarding/basic') && completed === '1') return { path: '/tutor/jobs' }
      if (to.path.startsWith('/tutor/onboarding/profile') && completed === '0') return { path: '/tutor/onboarding/basic' }
    } else {
      if (completed === '0') return { path: '/tutor/onboarding/basic' }
    }
  }

  return true
})
