<template>
  <div class="layout-container">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="layout-aside">
      <div class="logo">
        <el-icon v-if="isCollapse"><DataAnalysis /></el-icon>
        <template v-else>
          <el-icon><DataAnalysis /></el-icon>
          <span>病害检测系统</span>
        </template>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        router
      >
        <el-sub-menu v-if="hasPermission('road:manage')" index="road">
          <template #title>
            <el-icon><Location /></el-icon>
            <span>基础数据</span>
          </template>
          <el-menu-item index="/road">道路管理</el-menu-item>
          <el-menu-item index="/road/section">路段管理</el-menu-item>
        </el-sub-menu>
        
        <el-menu-item v-if="hasPermission('monitor:view')" index="/monitor">
          <el-icon><Monitor /></el-icon>
          <span>实时监控中心</span>
        </el-menu-item>
        
        <el-menu-item v-if="hasPermission('inspection:view')" index="/inspection/task">
          <el-icon><Timer /></el-icon>
          <span>自动巡检</span>
        </el-menu-item>
        
        <el-menu-item v-if="hasPermission('record:view')" index="/record">
          <el-icon><Document /></el-icon>
          <span>巡检记录查询</span>
        </el-menu-item>
        
        <el-sub-menu v-if="hasPermission('damage:history')" index="statistics">
          <template #title>
            <el-icon><DataAnalysis /></el-icon>
            <span>病害统计</span>
          </template>
          <el-menu-item index="/damage-history">
            <el-icon><Document /></el-icon>
            <span>历史病害统计</span>
          </el-menu-item>
          <el-menu-item v-if="hasPermission('damage:disposal')" index="/damage-disposal">
            <el-icon><Checked /></el-icon>
            <span>病害处置记录</span>
          </el-menu-item>
          <el-menu-item v-if="hasPermission('damage:analysis')" index="/damage-analysis">
            <el-icon><TrendCharts /></el-icon>
            <span>历史病害分析</span>
          </el-menu-item>
        </el-sub-menu>
        
        <el-sub-menu v-if="hasAnyPermission(['user:manage', 'role:manage', 'audit:manage', 'log:manage'])" index="system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item v-if="hasPermission('user:manage')" index="/system/user">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item v-if="hasPermission('role:manage')" index="/system/role">
            <el-icon><Avatar /></el-icon>
            <span>角色管理</span>
          </el-menu-item>
          <el-menu-item v-if="hasPermission('audit:manage')" index="/system/audit">
            <el-icon><Checked /></el-icon>
            <span>审核流程</span>
          </el-menu-item>
          <el-menu-item v-if="hasPermission('log:manage')" index="/system/log">
            <el-icon><List /></el-icon>
            <span>操作日志</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    
    <el-container class="layout-main">
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="toggleCollapse">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        
        <div class="header-right">
          <el-dropdown v-if="isAdmin" @command="handleRoleSwitch" style="margin-right: 15px;">
            <el-button type="primary" size="small">
              <el-icon><Switch /></el-icon>
              <span style="margin-left: 5px;">{{ currentRoleName }}</span>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="admin" :disabled="currentRole === 'admin'">
                  管理员
                </el-dropdown-item>
                <el-dropdown-item command="reviewer" :disabled="currentRole === 'reviewer'">
                  审核员
                </el-dropdown-item>
                <el-dropdown-item command="inspector" :disabled="currentRole === 'inspector'">
                  巡检员
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="32" icon="UserFilled" />
              <span style="margin-left: 8px">{{ userStore.userInfo?.realName || '管理员' }}</span>
              <el-icon style="margin-left: 5px"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <el-main class="layout-content">
        <router-view />
      </el-main>
    </el-container>
    
    <ProfileDialog v-model="profileVisible" />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import ProfileDialog from '@/components/ProfileDialog.vue'

const isCollapse = ref(false)
const profileVisible = ref(false)

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || '')

const currentRole = computed(() => userStore.currentRole)

const currentRoleName = computed(() => {
  const names = {
    admin: '管理员',
    reviewer: '审核员',
    inspector: '巡检员'
  }
  return names[currentRole.value] || '管理员'
})

const isAdmin = computed(() => {
  const roles = userStore.userInfo?.roles
  if (Array.isArray(roles)) {
    return roles.includes('admin')
  }
  return roles === 'admin'
})

const permissions = computed(() => userStore.currentPermissions)

const hasPermission = (permission) => {
  return permissions.value.includes(permission)
}

const hasAnyPermission = (permissionList) => {
  return permissionList.some(p => permissions.value.includes(p))
}

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

const handleRoleSwitch = (role) => {
  if (role === currentRole.value) return
  
  if (role === userStore.userInfo?.roles) {
    userStore.clearSimulateRole()
  } else {
    userStore.setSimulateRole(role)
  }
  
  ElMessage.success(`已切换到${currentRoleName.value}视图`)
  
  const currentPath = route.path
  const allowedRoutes = {
    admin: ['/road', '/monitor', '/inspection', '/record', '/damage', '/system'],
    reviewer: ['/monitor', '/inspection', '/record', '/damage', '/system/audit'],
    inspector: ['/monitor', '/inspection', '/record', '/damage-history']
  }
  
  const allowed = allowedRoutes[currentRole.value] || allowedRoutes.admin
  const isAllowed = allowed.some(r => currentPath.startsWith(r))
  
  if (!isAllowed) {
    router.push('/monitor')
  }
}

const handleCommand = async (command) => {
  if (command === 'profile') {
    profileVisible.value = true
  } else if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.layout-aside {
  background-color: #304156;
  transition: width 0.3s;
  overflow: hidden;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  background-color: #263445;
}

.logo .el-icon {
  font-size: 24px;
  margin-right: 8px;
}

.logo span {
  white-space: nowrap;
}

.el-menu {
  border-right: none;
}

.layout-main {
  background-color: #f0f2f5;
}

.layout-header {
  background-color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.header-left {
  display: flex;
  align-items: center;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  margin-right: 15px;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.layout-content {
  padding: 20px;
  overflow-y: auto;
}
</style>
