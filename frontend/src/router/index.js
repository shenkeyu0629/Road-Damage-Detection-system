import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/components/Layout.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/monitor',
    children: [
      {
        path: 'road',
        name: 'Road',
        component: () => import('@/views/road/RoadList.vue'),
        meta: { title: '道路管理' }
      },
      {
        path: 'road/section',
        name: 'RoadSection',
        component: () => import('@/views/road/SectionList.vue'),
        meta: { title: '路段管理' }
      },
      {
        path: 'monitor',
        name: 'MonitorCenter',
        component: () => import('@/views/monitor/MonitorCenter.vue'),
        meta: { title: '实时监控中心' }
      },
      {
        path: 'inspection/task',
        name: 'InspectionTask',
        component: () => import('@/views/inspection/TaskList.vue'),
        meta: { title: '自动巡检' }
      },
      {
        path: 'record',
        name: 'InspectionRecord',
        component: () => import('@/views/inspection/RecordList.vue'),
        meta: { title: '巡检记录查询' }
      },
      {
        path: 'damage-history',
        name: 'DamageHistory',
        component: () => import('@/views/statistics/DamageHistory.vue'),
        meta: { title: '历史病害统计' }
      },
      {
        path: 'damage-disposal',
        name: 'DamageDisposal',
        component: () => import('@/views/statistics/DamageDisposal.vue'),
        meta: { title: '病害处置记录' }
      },
      {
        path: 'damage-analysis',
        name: 'DamageAnalysis',
        component: () => import('@/views/statistics/DamageAnalysis.vue'),
        meta: { title: '历史病害分析' }
      },
      {
        path: 'system/user',
        name: 'UserManage',
        component: () => import('@/views/system/UserManage.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'system/role',
        name: 'RoleManage',
        component: () => import('@/views/system/RoleManage.vue'),
        meta: { title: '角色管理' }
      },
      {
        path: 'system/audit',
        name: 'AuditManage',
        component: () => import('@/views/system/AuditManage.vue'),
        meta: { title: '审核流程' }
      },
      {
        path: 'system/log',
        name: 'LogManage',
        component: () => import('@/views/system/LogManage.vue'),
        meta: { title: '操作日志' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 道路病害检测系统` : '道路病害检测系统'
  
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
