import { createRouter, createWebHashHistory } from 'vue-router'

import { useAdminAuthStore } from '@/stores/auth'

const LoginPage = () => import('@/pages/LoginPage.vue')
const DashboardPage = () => import('@/pages/DashboardPage.vue')
const JobsPage = () => import('@/pages/JobsPage.vue')
const VerificationPage = () => import('@/pages/VerificationPage.vue')
const VerificationDetailPage = () => import('@/pages/VerificationDetailPage.vue')
const RefundsPage = () => import('@/pages/RefundsPage.vue')
const RefundDetailPage = () => import('@/pages/RefundDetailPage.vue')
const UsersPage = () => import('@/pages/UsersPage.vue')
const PaymentOrdersPage = () => import('@/pages/PaymentOrdersPage.vue')
const PaymentOrderDetailPage = () => import('@/pages/PaymentOrderDetailPage.vue')
const OrganizationsPage = () => import('@/pages/OrganizationsPage.vue')
const HomeCarouselPage = () => import('@/pages/HomeCarouselPage.vue')

export const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', redirect: '/dashboard' },
    { path: '/login', name: 'login', component: LoginPage },
    { path: '/dashboard', name: 'dashboard', component: DashboardPage, meta: { auth: true } },
    { path: '/jobs', name: 'jobs', component: JobsPage, meta: { auth: true } },
    { path: '/verification', name: 'verification', component: VerificationPage, meta: { auth: true } },
    {
      path: '/verification/:userId',
      name: 'verificationDetail',
      component: VerificationDetailPage,
      meta: { auth: true },
    },
    { path: '/refunds', name: 'refunds', component: RefundsPage, meta: { auth: true } },
    { path: '/refunds/:requestId', name: 'refundDetail', component: RefundDetailPage, meta: { auth: true } },
    { path: '/users', name: 'users', component: UsersPage, meta: { auth: true } },
    { path: '/organizations', name: 'organizations', component: OrganizationsPage, meta: { auth: true } },
    { path: '/home-carousel', name: 'homeCarousel', component: HomeCarouselPage, meta: { auth: true } },
    { path: '/payments', name: 'paymentOrders', component: PaymentOrdersPage, meta: { auth: true } },
    { path: '/payments/:orderNo', name: 'paymentOrderDetail', component: PaymentOrderDetailPage, meta: { auth: true } },
  ],
})

router.beforeEach((to) => {
  const auth = useAdminAuthStore()
  if (auth.token === null && auth.user === null) {
    auth.loadFromStorage()
  }
  const needAuth = Boolean(to.meta.auth)
  if (needAuth && !auth.isAuthed) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && auth.isAuthed) {
    return { name: 'dashboard' }
  }
  return true
})
