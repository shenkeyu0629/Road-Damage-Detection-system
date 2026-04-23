<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>审核流程</span>
        </div>
      </template>
      
      <el-form :inline="true" :model="queryParams" class="query-form">
        <el-form-item label="审核状态">
          <el-select v-model="queryParams.auditStatus" placeholder="全部" clearable style="width: 120px">
            <el-option label="待审核" value="待审核" />
            <el-option label="审核通过" value="审核通过" />
            <el-option label="审核不通过" value="审核不通过" />
          </el-select>
        </el-form-item>
        <el-form-item label="道路名称">
          <el-input v-model="queryParams.roadName" placeholder="请输入道路名称" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadDisposals">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
      
      <el-table :data="disposalList" v-loading="loading" stripe>
        <el-table-column prop="roadName" label="道路名称" min-width="120" />
        <el-table-column prop="sectionName" label="路段名称" min-width="120" />
        <el-table-column prop="damageCount" label="病害数量" width="90" />
        <el-table-column prop="damageLevel" label="病害等级" width="90">
          <template #default="{ row }">
            <el-tag :type="levelMap[row.damageLevel] || 'info'">{{ row.damageLevel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="damageTypes" label="病害类型" min-width="120" show-overflow-tooltip />
        <el-table-column prop="disposalMethod" label="处置方式" min-width="120" show-overflow-tooltip />
        <el-table-column prop="disposalTime" label="处置时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.disposalTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="auditStatus" label="审核状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.auditStatus] || 'info'">{{ row.auditStatus || '待审核' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="row.auditStatus === '待审核'">
              <el-button type="success" size="small" @click="handleAudit(row, '审核通过')">通过</el-button>
              <el-button type="danger" size="small" @click="handleAudit(row, '审核不通过')">不通过</el-button>
            </template>
            <template v-else>
              <span style="color: #909399;">已审核</span>
            </template>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination
        v-model:current-page="queryParams.page"
        v-model:page-size="queryParams.size"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadDisposals"
        @current-change="loadDisposals"
        style="margin-top: 20px; justify-content: flex-end;"
      />
    </el-card>
    
    <el-dialog v-model="auditDialogVisible" title="审核" width="400px">
      <el-form :model="auditForm" label-width="80px">
        <el-form-item label="审核结果">
          <el-tag :type="auditForm.status === '审核通过' ? 'success' : 'danger'">{{ auditForm.status }}</el-tag>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input v-model="auditForm.remark" type="textarea" :rows="3" placeholder="请输入审核备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAudit" :loading="submitting">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const submitting = ref(false)
const disposalList = ref([])
const total = ref(0)
const auditDialogVisible = ref(false)

const queryParams = reactive({
  page: 1,
  size: 10,
  auditStatus: '',
  roadName: ''
})

const auditForm = reactive({
  id: null,
  status: '',
  remark: ''
})

const levelMap = {
  '轻微': 'success',
  '严重': 'danger'
}

const statusMap = {
  '待审核': 'warning',
  '审核通过': 'success',
  '审核不通过': 'danger'
}

const formatTime = (time) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 16)
}

const loadDisposals = async () => {
  loading.value = true
  try {
    const params = { ...queryParams }
    const res = await request.get('/damage-disposal/list', { params })
    if (res.data) {
      disposalList.value = res.data.records || res.data
      total.value = res.data.total || disposalList.value.length
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  queryParams.auditStatus = ''
  queryParams.roadName = ''
  queryParams.page = 1
  loadDisposals()
}

const handleAudit = (row, status) => {
  auditForm.id = row.id
  auditForm.status = status
  auditForm.remark = ''
  auditDialogVisible.value = true
}

const submitAudit = async () => {
  submitting.value = true
  try {
    await request.put(`/damage-disposal/${auditForm.id}/audit`, {
      auditStatus: auditForm.status,
      auditRemark: auditForm.remark
    })
    ElMessage.success('审核成功')
    auditDialogVisible.value = false
    loadDisposals()
  } catch (error) {
    console.error(error)
    ElMessage.error('审核失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadDisposals()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.query-form {
  margin-bottom: 20px;
}
</style>
