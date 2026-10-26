import { createRouter, createWebHashHistory } from 'vue-router'

// 三视图 hash 路由；深链 ?q=（检索）与 ?j=（判断）由各视图监听 query 消费
const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', redirect: { name: 'search' } },
    { path: '/search', name: 'search', component: () => import('@/views/SearchView.vue') },
    { path: '/judge', name: 'judge', component: () => import('@/views/JudgeView.vue') },
    { path: '/library', name: 'library', component: () => import('@/views/LibraryView.vue') },
    { path: '/workflow', name: 'workflow', component: () => import('@/views/WorkflowView.vue') },
    { path: '/:pathMatch(.*)*', redirect: { name: 'search' } }
  ],
  scrollBehavior: () => ({ top: 0 })
})

export default router
