<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>操作日志</span>
          <div>
            <el-button type="danger" @click="batchDelete" v-if="isAdmin">
              <el-icon><Delete /></el-icon>
              批量清理
            </el-button>
          </div>
        </div>
      </template>
      
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="queryParams.dateRange"
            type="daterange"
            range-separator="-"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="queryParams.username" placeholder="请输入用户名" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="queryParams.operationType" placeholder="全部" clearable style="width: 140px">
            <el-option label="新增" value="新增" />
            <el-option label="修改" value="修改" />
            <el-option label="删除" value="删除" />
            <el-option label="启用" value="启用" />
            <el-option label="禁用" value="禁用" />
            <el-option label="审核通过" value="审核通过" />
            <el-option label="审核不通过" value="审核不通过" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadLogs">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
      
      <el-table :data="logList" v-loading="loading" border stripe>
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="username" label="操作人" width="120" />
        <el-table-column prop="operationType" label="操作类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getOperationTagType(row.operationType)" size="small">
              {{ row.operationType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operationDesc" label="操作描述" min-width="250" show-overflow-tooltip />
        <el-table-column prop="operationTime" label="操作时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.operationTime) }}
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination
        v-model:current-page="queryParams.page"
        v-model:page-size="queryParams.size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadLogs"
        @current-change="loadLogs"
        class="pagination"
      />
    </el-card>
    
    <el-dialog v-model="deleteDialogVisible" title="批量清理日志" width="450px">
      <el-form :model="deleteForm" label-width="100px">
        <el-form-item label="清理方式">
          <el-radio-group v-model="deleteForm.type">
            <el-radio value="before">指定时间之前</el-radio>
            <el-radio value="range">指定时间范围</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="截止时间" v-if="deleteForm.type === 'before'">
          <el-date-picker v-model="deleteForm.beforeDate" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="时间范围" v-else>
          <el-date-picker v-model="deleteForm.dateRange" type="daterange" style="width: 100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item>
          <el-alert type="warning" :closable="false" show-icon>
            <template #title>
              <span>清理操作将永久删除日志，且此操作不可恢复</span>
            </template>
          </el-alert>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deleteDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmDelete" :loading="deleting">确认清理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const deleting = ref(false)
const deleteDialogVisible = ref(false)

const logList = ref([])
const total = ref(0)

const isAdmin = computed(() => {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  return userInfo.roles?.includes('admin')
})

const queryParams = reactive({
  page: 1,
  size: 20,
  dateRange: null,
  username: '',
  operationType: ''
})

const deleteForm = reactive({
  type: 'before',
  beforeDate: '',
  dateRange: null
})

const getOperationTagType = (type) => {
  const map = {
    '新增': 'success',
    '修改': 'warning',
    '删除': 'danger',
    '启用': 'success',
    '禁用': 'info',
    '审核通过': 'success',
    '审核不通过': 'danger'
  }
  return map[type] || 'info'
}

const formatTime = (time) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 19)
}

const loadLogs = async () => {
  loading.value = true
  try {
    const params = { ...queryParams }
    if (params.dateRange && params.dateRange.length === 2) {
      params.startDate = params.dateRange[0]
      params.endDate = params.dateRange[1]
    }
    delete params.dateRange
    
    const res = await request.get('/log/list', { params })
    logList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  queryParams.dateRange = null
  queryParams.username = ''
  queryParams.operationType = ''
  loadLogs()
}

const batchDelete = () => {
  deleteForm.type = 'before'
  deleteForm.beforeDate = ''
  deleteForm.dateRange = null
  deleteDialogVisible.value = true
}

const confirmDelete = async () => {
  if (deleteForm.type === 'before' && !deleteForm.beforeDate) {
    ElMessage.warning('请选择截止时间')
    return
  }
  if (deleteForm.type === 'range' && (!deleteForm.dateRange || deleteForm.dateRange.length !== 2)) {
    ElMessage.warning('请选择时间范围')
    return
  }
  
  try {
    await ElMessageBox.confirm('确定要清理日志吗？此操作不可恢复！', '警告', { type: 'warning' })
    
    deleting.value = true
    const params = {}
    if (deleteForm.type === 'before') {
      params.beforeDate = deleteForm.beforeDate
    } else if (deleteForm.dateRange) {
      params.startDate = deleteForm.dateRange[0]
      params.endDate = deleteForm.dateRange[1]
    }
    
    const res = await request.delete('/log/clean', { params })
    ElMessage.success(`清理完成，共删除 ${res.data || 0} 条记录`)
    deleteDialogVisible.value = false
    loadLogs()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  } finally {
    deleting.value = false
  }
}

onMounted(() => {
  loadLogs()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 15px;
}

.pagination {
  margin-top: 15px;
  justify-content: flex-end;
}
</style>
