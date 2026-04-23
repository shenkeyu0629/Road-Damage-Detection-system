<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>巡检记录查询与统计</span>
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
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="待执行" value="pending" />
            <el-option label="执行中" value="running" />
            <el-option label="已完成" value="completed" />
            <el-option label="已取消" value="cancelled" />
          </el-select>
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
      <el-col :span="12">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-value">{{ summaryStats.totalRecords }}</div>
            <div class="stat-label">巡检记录数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-value">{{ summaryStats.roadsInspected }}</div>
            <div class="stat-label">巡检道路数</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-card style="margin-top: 20px">
      <el-table :data="recordList" v-loading="loading" border stripe>
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="taskName" label="任务名称" min-width="180" />
        <el-table-column prop="roadName" label="道路" min-width="150" />
        <el-table-column prop="sectionName" label="路段" min-width="120">
          <template #default="{ row }">
            {{ row.sectionName || '全部' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="scheduledStartTime" label="计划开始时间" min-width="170" />
        <el-table-column prop="scheduledEndTime" label="计划结束时间" min-width="170" />
        <el-table-column prop="startTime" label="实际开始时间" min-width="170" />
        <el-table-column prop="endTime" label="实际结束时间" min-width="170" />
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
    
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>按道路分类统计</span>
          </template>
          <div ref="roadChartRef" style="height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>按路段分类统计</span>
          </template>
          <div ref="sectionChartRef" style="height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import * as echarts from 'echarts'

const loading = ref(false)
const recordList = ref([])
const total = ref(0)

const roadList = ref([])
const sectionList = ref([])

const roadChartRef = ref(null)
const sectionChartRef = ref(null)
let roadChart = null
let sectionChart = null

const summaryStats = ref({
  totalRecords: 0,
  roadsInspected: 0
})

const queryParams = reactive({
  page: 1,
  size: 10,
  roadId: null,
  sectionId: null,
  status: null
})

const statusMap = {
  pending: { name: '待执行', type: 'info' },
  running: { name: '执行中', type: 'warning' },
  completed: { name: '已完成', type: 'success' },
  cancelled: { name: '已取消', type: 'danger' }
}

const getStatusName = (status) => statusMap[status]?.name || status
const getStatusType = (status) => statusMap[status]?.type || 'info'

const loadStatistics = async () => {
  try {
    const params = {}
    if (queryParams.roadId) {
      params.roadId = queryParams.roadId
    }
    const res = await request.get('/inspection/task/statistics', { params })
    summaryStats.value = res.data || {
      totalRecords: 0,
      roadsInspected: 0
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
    const res = await request.get('/inspection/task/list', { params: queryParams })
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
  queryParams.status = null
  sectionList.value = []
  loadRecords()
}

const loadRoadChartData = async () => {
  try {
    const res = await request.get('/inspection/task/statistics/by-road')
    const data = res.data || []
    
    if (roadChart) {
      roadChart.dispose()
    }
    
    roadChart = echarts.init(roadChartRef.value)
    
    const option = {
      tooltip: {
        trigger: 'item',
        formatter: '{b}: {c}次 ({d}%)'
      },
      legend: {
        orient: 'vertical',
        left: 'left',
        top: 'center'
      },
      series: [
        {
          name: '巡检次数',
          type: 'pie',
          radius: ['40%', '70%'],
          center: ['60%', '50%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: {
            show: false,
            position: 'center'
          },
          emphasis: {
            label: {
              show: true,
              fontSize: 16,
              fontWeight: 'bold'
            }
          },
          labelLine: {
            show: false
          },
          data: data.map(item => ({
            name: item.roadName,
            value: item.count
          }))
        }
      ]
    }
    
    roadChart.setOption(option)
  } catch (error) {
    console.error('Load road chart error:', error)
  }
}

const loadSectionChartData = async () => {
  try {
    const res = await request.get('/inspection/task/statistics/by-section')
    const data = res.data || []
    
    if (sectionChart) {
      sectionChart.dispose()
    }
    
    sectionChart = echarts.init(sectionChartRef.value)
    
    const displayData = data.slice(0, 10)
    
    const option = {
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow'
        },
        formatter: function(params) {
          const item = params[0]
          const fullData = data.find(d => d.sectionName === item.name)
          if (fullData) {
            return `${fullData.roadName} - ${fullData.sectionName}<br/>巡检次数: ${fullData.count}次`
          }
          return `${item.name}<br/>巡检次数: ${item.value}次`
        }
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: displayData.map(item => item.sectionName),
        axisLabel: {
          interval: 0,
          rotate: 30,
          fontSize: 10
        }
      },
      yAxis: {
        type: 'value',
        name: '巡检次数',
        minInterval: 1
      },
      series: [
        {
          name: '巡检次数',
          type: 'bar',
          barWidth: '60%',
          data: displayData.map(item => item.count),
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#83bff6' },
              { offset: 0.5, color: '#188df0' },
              { offset: 1, color: '#188df0' }
            ])
          },
          emphasis: {
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#2378f7' },
                { offset: 0.7, color: '#2378f7' },
                { offset: 1, color: '#83bff6' }
              ])
            }
          }
        }
      ]
    }
    
    sectionChart.setOption(option)
  } catch (error) {
    console.error('Load section chart error:', error)
  }
}

const handleResize = () => {
  if (roadChart) {
    roadChart.resize()
  }
  if (sectionChart) {
    sectionChart.resize()
  }
}

onMounted(() => {
  loadRoadList()
  loadRecords()
  loadStatistics()
  loadRoadChartData()
  loadSectionChartData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (roadChart) {
    roadChart.dispose()
  }
  if (sectionChart) {
    sectionChart.dispose()
  }
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
