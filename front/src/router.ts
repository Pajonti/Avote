import { createRouter, createWebHistory } from 'vue-router'
import DetailPanel from './components/DetailPanel.vue'
import HomeView from './views/HomeView.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: HomeView,
      children: [{ path: 'p/:id(\\d+)', name: 'detail', component: DetailPanel }],
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})
