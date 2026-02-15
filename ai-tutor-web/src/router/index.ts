import { createRouter, createWebHashHistory } from 'vue-router'

const HomePage = () => import('@/pages/HomePage.vue')
const AuthPage = () => import('@/pages/AuthPage.vue')

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
  ],
})
