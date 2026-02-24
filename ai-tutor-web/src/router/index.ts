import { createRouter, createWebHashHistory } from 'vue-router'

const HomePage = () => import('@/pages/HomePage.vue')
const AuthPage = () => import('@/pages/AuthPage.vue')
const MePage = () => import('@/pages/MePage.vue')
const StudentPostPage = () => import('@/pages/student/StudentPostPage.vue')
const StudentOnboardingFirstDemandPage = () => import('@/pages/student/StudentOnboardingFirstDemandPage.vue')
const StudentMineJobsPage = () => import('@/pages/student/StudentMineJobsPage.vue')
const StudentEditJobPage = () => import('@/pages/student/StudentEditJobPage.vue')
const StudentFavoritesPage = () => import('@/pages/student/StudentFavoritesPage.vue')
const TutorJobsPage = () => import('@/pages/tutor/TutorJobsPage.vue')
const TutorJobDetailPage = () => import('@/pages/tutor/TutorJobDetailPage.vue')
const TutorFavoritesPage = () => import('@/pages/tutor/TutorFavoritesPage.vue')
const TutorOnboardingBasicPage = () => import('@/pages/tutor/TutorOnboardingBasicPage.vue')
const ChatListPage = () => import('@/pages/chat/ChatListPage.vue')
const ChatRoomPage = () => import('@/pages/chat/ChatRoomPage.vue')
const SchedulePage = () => import('@/pages/schedule/SchedulePage.vue')
const BrokeragePayPage = () => import('@/pages/pay/BrokeragePayPage.vue')

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
      path: '/me',
      name: 'me',
      component: MePage,
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
  ],
})

router.beforeEach((to) => {
  const { token, userType } = readAuthFromStorage()
  const loggedIn = typeof token === 'string' && token.length > 0

  if (loggedIn && to.path.startsWith('/auth/')) {
    if (userType === 1) return { path: '/tutor/jobs' }
    if (userType === 2) return { path: '/student/post' }
    return { path: '/' }
  }

  const needAuth =
    to.path === '/me' ||
    to.path === '/schedule' ||
    to.path.startsWith('/pay/') ||
    to.path.startsWith('/student/') ||
    to.path.startsWith('/tutor/') ||
    to.path.startsWith('/chat')

  if (needAuth && !loggedIn) {
    if (to.path.startsWith('/tutor/')) return { path: '/auth/tutor' }
    return { path: '/auth/student' }
  }

  if (loggedIn && userType != null) {
    if (to.path.startsWith('/tutor/') && userType !== 1) return { path: '/student/post' }
    if (to.path.startsWith('/student/') && userType !== 2) return { path: '/tutor/jobs' }
  }

  if (loggedIn && userType === 1 && to.path.startsWith('/tutor/')) {
    const completed = localStorage.getItem(STORAGE_TUTOR_BASIC_COMPLETED_KEY)
    if (to.path.startsWith('/tutor/onboarding')) {
      if (completed === '1') return { path: '/tutor/jobs' }
    } else {
      if (completed === '0') return { path: '/tutor/onboarding/basic' }
    }
  }

  return true
})
