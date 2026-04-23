<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>角色管理</span>
        </div>
      </template>
      
      <el-row :gutter="20">
        <el-col :span="8" v-for="role in roleList" :key="role.code">
          <el-card class="role-card" :class="'role-' + role.code">
            <template #header>
              <div class="role-header">
                <el-icon :size="24"><User /></el-icon>
                <span>{{ role.name }}</span>
                <el-tag v-if="role.code === 'admin'" type="danger" size="small" style="margin-left: 10px">系统角色</el-tag>
              </div>
            </template>
            <div class="role-desc">{{ role.description }}</div>
            <div class="role-permissions">
              <div class="permission-title">权限范围：</div>
              <el-tag v-for="perm in role.permissions" :key="perm" size="small" style="margin: 3px">
                {{ getPermissionName(perm) }}
              </el-tag>
            </div>
            <div class="role-users">
              <span class="users-count">关联用户：{{ role.userCount }} 人</span>
            </div>
            <el-button 
              type="primary" 
              size="small" 
              @click="editRole(role)" 
              style="margin-top: 10px"
              :disabled="role.code === 'admin'"
            >
              配置权限
            </el-button>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
    
    <el-dialog v-model="dialogVisible" title="配置角色权限" width="600px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="角色名称">
          <el-input v-model="form.name" disabled />
        </el-form-item>
        <el-form-item label="角色描述">
          <el-input v-model="form.description" type="textarea" rows="2" />
        </el-form-item>
        <el-form-item label="功能权限">
          <el-tree
            ref="treeRef"
            :data="permissionTree"
            :props="{ label: 'name', children: 'children' }"
            show-checkbox
            node-key="code"
            :default-checked-keys="form.checkedPermissions"
            style="max-height: 300px; overflow-y: auto"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRole" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const dialogVisible = ref(false)
const saving = ref(false)
const treeRef = ref()

const roleList = ref([
  {
    code: 'admin',
    name: '管理员',
    description: '拥有系统最高管理权限，负责用户账户的创建与禁用、角色权限的分配以及操作日志的审计。',
    permissions: ['road:manage', 'section:manage', 'monitor:view', 'monitor:detect', 'inspection:view', 'inspection:manage', 'record:view', 'damage:history', 'damage:disposal', 'damage:analysis', 'user:manage', 'role:manage', 'audit:manage', 'log:manage'],
    userCount: 1
  },
  {
    code: 'reviewer',
    name: '审核员',
    description: '承担病害标注的审核工作，对自动识别结果进行确认、修正或驳回，并可查看病害统计数据和处置记录。',
    permissions: ['monitor:view', 'inspection:view', 'record:view', 'damage:history', 'damage:disposal', 'damage:analysis', 'audit:manage'],
    userCount: 0
  },
  {
    code: 'inspector',
    name: '巡检员',
    description: '主要负责道路图像的采集与上传，可以查看巡检任务和记录，以及病害统计信息。',
    permissions: ['monitor:view', 'inspection:view', 'record:view', 'damage:history'],
    userCount: 0
  }
])

const form = reactive({
  code: '',
  name: '',
  description: '',
  checkedPermissions: []
})

const permissionTree = ref([
  {
    name: '基础数据',
    code: 'basic',
    children: [
      { name: '道路管理', code: 'road:manage' },
      { name: '路段管理', code: 'section:manage' }
    ]
  },
  {
    name: '实时监控',
    code: 'monitor',
    children: [
      { name: '查看监控', code: 'monitor:view' },
      { name: '执行检测', code: 'monitor:detect' }
    ]
  },
  {
    name: '自动巡检',
    code: 'inspection',
    children: [
      { name: '查看任务', code: 'inspection:view' },
      { name: '管理任务', code: 'inspection:manage' }
    ]
  },
  {
    name: '巡检记录',
    code: 'record',
    children: [
      { name: '查看记录', code: 'record:view' }
    ]
  },
  {
    name: '病害统计',
    code: 'damage',
    children: [
      { name: '历史病害统计', code: 'damage:history' },
      { name: '病害处置记录', code: 'damage:disposal' },
      { name: '历史病害分析', code: 'damage:analysis' }
    ]
  },
  {
    name: '系统管理',
    code: 'system',
    children: [
      { name: '用户管理', code: 'user:manage' },
      { name: '角色管理', code: 'role:manage' },
      { name: '审核流程', code: 'audit:manage' },
      { name: '操作日志', code: 'log:manage' }
    ]
  }
])

const permissionNames = {
  'road:manage': '道路管理',
  'section:manage': '路段管理',
  'monitor:view': '查看监控',
  'monitor:detect': '执行检测',
  'inspection:view': '查看任务',
  'inspection:manage': '管理任务',
  'record:view': '查看记录',
  'damage:history': '历史病害统计',
  'damage:disposal': '病害处置记录',
  'damage:analysis': '历史病害分析',
  'user:manage': '用户管理',
  'role:manage': '角色管理',
  'audit:manage': '审核流程',
  'log:manage': '操作日志'
}

const getPermissionName = (code) => permissionNames[code] || code

const editRole = (role) => {
  if (role.code === 'admin') {
    ElMessage.warning('管理员角色不可修改')
    return
  }
  form.code = role.code
  form.name = role.name
  form.description = role.description
  form.checkedPermissions = [...role.permissions]
  dialogVisible.value = true
}

const saveRole = async () => {
  saving.value = true
  try {
    const checkedKeys = treeRef.value?.getCheckedKeys() || []
    const role = roleList.value.find(r => r.code === form.code)
    if (role) {
      role.description = form.description
      role.permissions = checkedKeys.filter(k => !permissionTree.value.some(p => p.code === k))
    }
    
    await request.put(`/role/${form.code}`, {
      description: form.description,
      permissions: checkedKeys
    })
    
    ElMessage.success('保存成功')
    dialogVisible.value = false
  } catch (error) {
    console.error(error)
  } finally {
    saving.value = false
  }
}

const loadRoles = async () => {
  try {
    const res = await request.get('/role/list')
    if (res.data) {
      roleList.value = res.data
    }
  } catch (error) {
    console.error(error)
  }
}

onMounted(() => {
  loadRoles()
})
</script>

<style scoped>
.role-card {
  margin-bottom: 20px;
}

.role-admin{
  border-left: 4px solid #f56c6c;
}

.role-reviewer{
  border-left: 4px solid #e6a23c;
}

.role-inspector{
  border-left: 4px solid #67c23a;
}

.role-header{
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: bold;
}

.role-admin .role-header{
  color: #f56c6c;
}

.role-reviewer .role-header{
  color: #e6a23c;
}

.role-inspector .role-header{
  color: #67c23a;
}

.role-desc{
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
  margin-bottom: 15px;
}

.permission-title{
  font-weight: 500;
  margin-bottom: 8px;
  color: #303133;
}

.role-users{
  margin-top: 15px;
  padding-top: 10px;
  border-top: 1px solid #ebeef5;
}

.users-count{
  color: #909399;
  font-size: 13px;
}
</style>
