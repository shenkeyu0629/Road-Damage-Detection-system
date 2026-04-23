<template>
  <el-dialog v-model="visible" title="个人中心" width="500px" @close="handleClose">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="基本信息" name="info">
        <el-form :model="userInfo" label-width="80px">
          <el-form-item label="用户名">
            <el-input v-model="userInfo.username" disabled />
          </el-form-item>
          <el-form-item label="姓名">
            <el-input v-model="userInfo.realName" />
          </el-form-item>
          <el-form-item label="角色">
            <el-tag v-for="role in userInfo.roles" :key="role" style="margin-right: 5px">
              {{ getRoleName(role) }}
            </el-tag>
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="userInfo.email" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="userInfo.phone" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="saveUserInfo" :loading="saving">保存修改</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
      
      <el-tab-pane label="修改密码" name="password">
        <el-form :model="passwordForm" label-width="100px" :rules="passwordRules" ref="passwordFormRef">
          <el-form-item label="当前密码" prop="oldPassword">
            <el-input v-model="passwordForm.oldPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="passwordForm.newPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="changePassword" :loading="changingPassword">修改密码</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue'])

const visible = ref(props.modelValue)
const activeTab = ref('info')
const saving = ref(false)
const changingPassword = ref(false)
const passwordFormRef = ref()

const userStore = useUserStore()

const userInfo = reactive({
  id: null,
  username: '',
  realName: '',
  email: '',
  phone: '',
  roles: []
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const roleMap = {
  admin: '管理员',
  reviewer: '审核员',
  inspector: '巡检员'
}

const getRoleName = (role) => roleMap[role] || role

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val) {
    loadUserInfo()
  }
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

const loadUserInfo = () => {
  const storedUser = localStorage.getItem('userInfo')
  if (storedUser) {
    const user = JSON.parse(storedUser)
    userInfo.id = user.id
    userInfo.username = user.username
    userInfo.realName = user.realName || ''
    userInfo.email = user.email || ''
    userInfo.phone = user.phone || ''
    userInfo.roles = user.roles || ['inspector']
  }
}

const saveUserInfo = async () => {
  saving.value = true
  try {
    await request.put(`/user/${userInfo.id}`, {
      realName: userInfo.realName,
      email: userInfo.email,
      phone: userInfo.phone
    })
    
    const storedUser = JSON.parse(localStorage.getItem('userInfo') || '{}')
    storedUser.realName = userInfo.realName
    storedUser.email = userInfo.email
    storedUser.phone = userInfo.phone
    localStorage.setItem('userInfo', JSON.stringify(storedUser))
    
    ElMessage.success('保存成功')
  } catch (error) {
    console.error(error)
  } finally {
    saving.value = false
  }
}

const changePassword = async () => {
  if (!passwordFormRef.value) return
  
  await passwordFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    changingPassword.value = true
    try {
      await request.put(`/user/${userInfo.id}/password`, {
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword
      })
      
      ElMessage.success('密码修改成功')
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
    } catch (error) {
      console.error(error)
    } finally {
      changingPassword.value = false
    }
  })
}

const handleClose = () => {
  activeTab.value = 'info'
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}
</script>
