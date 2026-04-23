<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>历史病害统计</span>
        </div>
      </template>
      
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="道路">
          <el-select v-model="queryParams.roadId" placeholder="全部" clearable style="width: 150px" @change="handleRoadChange">
            <el-option v-for="road in roadList" :key="road.id" :label="road.roadName" :value="road.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="路段">
          <el-select v-model="queryParams.sectionId" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="section in sectionList" :key="section.id" :label="section.sectionName" :value="section.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="病害等级">
          <el-select v-model="queryParams.damageLevel" placeholder="全部" clearable style="width: 120px">
            <el-option label="轻微" value="轻微" />
            <el-option label="严重" value="严重" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="未处理" value="未处理" />
            <el-option label="处理中" value="处理中" />
            <el-option label="已处理" value="已处理" />
          </el-select>
        </el-form-item>
        <el-form-item label="检测日期">
          <el-date-picker v-model="queryParams.dateRange" type="daterange" range-separator="-" start-placeholder="开始" end-placeholder="结束" style="width: 220px" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadRecords">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="resetQuery">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="4">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-value">{{ summaryStats.totalRecords }}</div>
            <div class="stat-label">病害记录数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-value">{{ summaryStats.totalDamages }}</div>
            <div class="stat-label">病害总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-value danger">{{ summaryStats.severeCount }}</div>
            <div class="stat-label">严重病害</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-value warning">{{ summaryStats.untreatedCount }}</div>
            <div class="stat-label">未处理</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-value info">{{ summaryStats.processingCount }}</div>
            <div class="stat-label">处理中</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-value success">{{ summaryStats.processedCount }}</div>
            <div class="stat-label">已处理</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-card style="margin-top: 20px">
      <el-table :data="recordList" v-loading="loading" border stripe>
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="roadName" label="道路名称" width="120" />
        <el-table-column prop="sectionName" label="路段" width="100" />
        <el-table-column prop="damageCount" label="病害数量" width="90">
          <template #default="{ row }">
            <el-tag type="danger">{{ row.damageCount || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="damageLevel" label="病害等级" width="90">
          <template #default="{ row }">
            <el-tag :type="getLevelType(row.damageLevel)" size="small">{{ row.damageLevel || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="damageTypes" label="病害类型" min-width="150" show-overflow-tooltip />
        <el-table-column prop="detectionTime" label="检测时间" width="160" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-select v-model="row.status" size="small" @change="handleStatusChange(row)" style="width: 90px">
              <el-option label="未处理" value="未处理" />
              <el-option label="处理中" value="处理中" />
              <el-option label="已处理" value="已处理" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDisposeDialog(row)">处置</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination
        v-model:current-page="queryParams.page"
        v-model:page-size="queryParams.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadRecords"
        @current-change="loadRecords"
        class="pagination"
      />
    </el-card>
    
    <el-dialog v-model="disposeVisible" title="病害处置" width="500px">
      <el-form :model="disposeForm" label-width="80px">
        <el-form-item label="道路">
          <span>{{ currentRecord?.roadName }}</span>
        </el-form-item>
        <el-form-item label="路段">
          <span>{{ currentRecord?.sectionName }}</span>
        </el-form-item>
        <el-form-item label="病害数量">
          <span>{{ currentRecord?.damageCount }}</span>
        </el-form-item>
        <el-form-item label="病害等级">
          <el-tag :type="getLevelType(currentRecord?.damageLevel)">{{ currentRecord?.damageLevel }}</el-tag>
        </el-form-item>
        <el-form-item label="病害类型">
          <span>{{ currentRecord?.damageTypes }}</span>
        </el-form-item>
        <el-form-item label="处置方法">
          <el-input v-model="disposeForm.disposalMethod" type="textarea" :rows="4" placeholder="请输入处置方法" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="disposeVisible = false">取消</el-button>
        <el-button type="primary" @click="submitDispose">确认处置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const recordList = ref([])
const total = ref(0)
const disposeVisible = ref(false)
const currentRecord = ref(null)

const roadList = ref([])
const sectionList = ref([])

const summaryStats = ref({
  totalRecords: 0,
  totalDamages: 0,
  severeCount: 0,
  untreatedCount: 0,
  processingCount: 0,
  processedCount: 0
})

const queryParams = reactive({
  page: 1,
  size: 10,
  roadId: null,
  sectionId: null,
  damageLevel: null,
  status: null,
  dateRange: null
})

const disposeForm = reactive({
  disposalMethod: ''
})

const levelMap = {
  '轻微': 'success',
  '严重': 'danger'
}

const getLevelType = (level) => levelMap[level] || 'info'

const loadStatistics = async () => {
  try {
    const params = {}
    if (queryParams.roadId) {
      params.roadId = queryParams.roadId
    }
    const res = await request.get('/damage-history/statistics', { params })
    summaryStats.value = res.data || {
      totalRecords: 0,
      totalDamages: 0,
      severeCount: 0,
      untreatedCount: 0,
      processingCount: 0,
      processedCount: 0
    }
  } catch (error) {
    console.error('Load statistics error:', error)
  }
}

const loadRoadList = async () => {
  try {
    const res = await request.get('/road/list', { params: { size: 100 } })
    roadList.value = res.data?.records || []
  } catch (error) {
    console.error(error)
  }
}

const handleRoadChange = async (roadId) => {
  queryParams.sectionId = null
  sectionList.value = []
  if (roadId) {
    try {
      const res = await request.get(`/road/${roadId}/sections`)
      sectionList.value = res.data || []
    } catch (error) {
      console.error(error)
    }
  }
}

const loadRecords = async () => {
  loading.value = true
  try {
    const params = { ...queryParams }
    if (params.dateRange && params.dateRange.length === 2) {
      params.startDate = params.dateRange[0]
      params.endDate = params.dateRange[1]
    }
    delete params.dateRange
    
    const res = await request.get('/damage-history/list', { params })
    recordList.value = res.data?.records || []
    total.value = res.data?.total || 0
    loadStatistics()
  } catch (error) {
    console.error(error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  queryParams.roadId = null
  queryParams.sectionId = null
  queryParams.damageLevel = null
  queryParams.status = null
  queryParams.dateRange = null
  sectionList.value = []
  loadRecords()
}

const handleStatusChange = async (row) => {
  try {
    await request.put(`/damage-history/${row.id}/status`, null, {
      params: { status: row.status }
    })
    ElMessage.success('状态更新成功')
    loadStatistics()
  } catch (error) {
    console.error(error)
    ElMessage.error('状态更新失败')
    loadRecords()
  }
}

const openDisposeDialog = (row) => {
  currentRecord.value = row
  disposeForm.disposalMethod = row.disposalMethod || ''
  disposeVisible.value = true
}

const submitDispose = async () => {
  if (!disposeForm.disposalMethod.trim()) {
    ElMessage.warning('请输入处置方法')
    return
  }
  
  try {
    await request.put(`/damage-history/${currentRecord.value.id}/dispose`, {
      disposalMethod: disposeForm.disposalMethod
    })
    ElMessage.success('处置成功')
    disposeVisible.value = false
    loadRecords()
    loadStatistics()
  } catch (error) {
    console.error(error)
    ElMessage.error('处置失败')
  }
}

onMounted(() => {
  loadRoadList()
  loadRecords()
  loadStatistics()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 0;
}

.stat-card {
  border-radius: 12px;
}

.stat-item {
  text-align: center;
  padding: 15px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #409eff;
}

.stat-value.danger {
  color: #f56c6c;
}

.stat-value.warning {
  color: #e6a23c;
}

.stat-value.success {
  color: #67c23a;
}

.stat-value.info {
  color: #909399;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}

.pagination {
  margin-top: 15px;
  justify-content: flex-end;
}
</style>
