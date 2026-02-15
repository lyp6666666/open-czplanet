import { createRouter, createWebHashHistory } from 'vue-router'

const HomePage = () => import('@/pages/HomePage.vue')

export const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomePage,
    },
  ],
})
