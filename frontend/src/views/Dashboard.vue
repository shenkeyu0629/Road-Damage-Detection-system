<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #409EFF">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalRecords }}</div>
              <div class="stat-label">巡检记录</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #E6A23C">
              <el-icon><Warning /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalDamages }}</div>
              <div class="stat-label">病害数量</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #F56C6C">
              <el-icon><Bell /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.pendingAlarms }}</div>
              <div class="stat-label">待处理告警</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #67C23A">
              <el-icon><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.resolvedAlarms }}</div>
              <div class="stat-label">已处理告警</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="16">
        <el-card>
          <template #header>
            <span>病害趋势</span>
          </template>
          <div ref="trendChartRef" class="chart-container" style="height: 350px"></div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>病害类型分布</span>
          </template>
          <div ref="pieChartRef" class="chart-container" style="height: 350px"></div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" style="margin-top: 5px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span>待处理告警</span>
              <el-button type="primary" link @click="goToAlarm">
                查看全部
              </el-button>
            </div>
          </template>
          <el-table :data="pendingAlarms" style="width: 100%" v-loading="loading">
            <el-table-column label="告警编号" width="150">
              <template #default="{ row }">
                {{ row.alarmCode }}
              </template>
            </el-table-column>
            <el-table-column label="告警原因">
              <template #default="{ row }">
                {{ formatReason(row.alarmReason) }}
              </template>
            </el-table-column>
            <el-table-column prop="alarmLevel" label="级别" width="80">
              <template #default="{ row }">
                <el-tag :type="getLevelType(row.alarmLevel)" size="small">{{ getLevelText(row.alarmLevel) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="时间" width="160" />
          </el-table>
          <el-empty v-if="!loading && pendingAlarms.length === 0" description="暂无待处理告警" />
        </el-card>
      </el-col>
      
      <el-col :span="12">
        <el-card>
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span>最新告警</span>
              <el-button type="primary" link @click="goToAlarm">
                查看全部
              </el-button>
            </div>
          </template>
          <el-table :data="recentAlarms" style="width: 100%" v-loading="loading">
            <el-table-column label="告警编号" width="150">
              <template #default="{ row }">
                {{ row.alarmCode }}
              </template>
            </el-table-column>
            <el-table-column label="告警原因">
              <template #default="{ row }">
                {{ formatReason(row.alarmReason) }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="时间" width="160" />
          </el-table>
          <el-empty v-if="!loading && recentAlarms.length === 0" description="暂无告警记录" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import request from '@/utils/request'

const router = useRouter()
const trendChartRef = ref()
const pieChartRef = ref()
let trendChart = null
let pieChart = null

const loading = ref(false)

const stats = ref({
  totalRecords: 0,
  totalDamages: 0,
  pendingAlarms: 0,
  resolvedAlarms: 0
})

const pendingAlarms = ref([])
const recentAlarms = ref([])

const statusMap = {
  pending: { text: '待处理', type: 'danger' },
  resolved: { text: '已处理', type: 'success' },
  ignored: { text: '已忽略', type: 'info' }
}

const levelMap = {
  minor: { text: '轻微', type: 'success' },
  moderate: { text: '中等', type: 'warning' },
  severe: { text: '严重', type: 'danger' }
}

const getStatusType = (status) => statusMap[status]?.type || 'info'
const getStatusText = (status) => statusMap[status]?.text || status
const getLevelType = (level) => levelMap[level]?.type || 'info'
const getLevelText = (level) => levelMap[level]?.text || level

const formatReason = (reason) => {
  if (!reason) return '-'
  if (reason.includes('检测到')) {
    return reason.length > 30 ? reason.substring(0, 30) + '...' : reason
  }
  
  try {
    const data = JSON.parse(reason)
    if (Array.isArray(data)) {
      return `检测到${data.length}处病害`
    }
  } catch (e) {}
  
  return reason.length > 30 ? reason.substring(0, 30) + '...' : reason
}

const goToAlarm = () => {
  router.push('/alarm')
}

const loadStats = async () => {
  try {
    const res = await request.get('/statistics/damage')
    const data = res.data || {}
    
    stats.value.totalDamages = data.totalCount || 0
    stats.value.pendingAlarms = data.byLevel?.severe || 0
  } catch (error) {
    console.error(error)
  }
}

const loadRecords = async () => {
  try {
    const res = await request.get('/inspection/record/list', { params: { size: 1 } })
    stats.value.totalRecords = res.data?.total || 0
  } catch (error) {
    console.error(error)
  }
}

const loadAlarms = async () => {
  loading.value = true
  try {
    const [pendingRes, allRes] = await Promise.all([
      request.get('/alarm/list', { params: { size: 5, status: 'pending' } }),
      request.get('/alarm/list', { params: { size: 5 } })
    ])
    
    pendingAlarms.value = pendingRes.data?.records || []
    recentAlarms.value = allRes.data?.records || []
    
    stats.value.pendingAlarms = pendingRes.data?.total || 0
    
    const resolvedRes = await request.get('/alarm/list', { params: { size: 1, status: 'resolved' } })
    stats.value.resolvedAlarms = resolvedRes.data?.total || 0
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const initTrendChart = async () => {
  if (!trendChartRef.value) return
  
  trendChart = echarts.init(trendChartRef.value)
  
  try {
    const res = await request.get('/statistics/damage')
    const trend = res.data?.trend || []
    
    const option = {
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: {
        type: 'category',
        data: trend.map(d => d.date),
        boundaryGap: false
      },
      yAxis: { type: 'value' },
      series: [{
        type: 'line',
        data: trend.map(d => d.count),
        smooth: true,
        areaStyle: { color: 'rgba(64, 158, 255, 0.2)' },
        lineStyle: { color: '#409eff', width: 2 },
        itemStyle: { color: '#409eff' }
      }]
    }
    
    trendChart.setOption(option)
  } catch (error) {
    console.error(error)
  }
}

const initPieChart = async () => {
  if (!pieChartRef.value) return
  
  pieChart = echarts.init(pieChartRef.value)
  
  try {
    const res = await request.get('/statistics/damage')
    const byType = res.data?.byType || {}
    
    const data = Object.entries(byType).map(([name, value]) => ({ name, value }))
    
    const option = {
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { orient: 'vertical', left: 'left' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
        labelLine: { show: false },
        data: data
      }]
    }
    
    pieChart.setOption(option)
  } catch (error) {
    console.error(error)
  }
}

const handleResize = () => {
  trendChart?.resize()
  pieChart?.resize()
}

const checkAuth = () => {
  const token = localStorage.getItem('token')
  if (!token) {
    router.push('/login')
    return false
  }
  return true
}

onMounted(async () => {
  if (!checkAuth()) return
  
  await Promise.all([
    loadStats(),
    loadRecords(),
    loadAlarms()
  ])
  
  await Promise.all([
    initTrendChart(),
    initPieChart()
  ])
  
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped>
.dashboard {
  padding: 0;
}

.stat-card {
  border-radius: 12px;
}

.stat-content {
  display: flex;
  align-items: center;
  padding: 10px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
}

.stat-icon .el-icon {
  font-size: 28px;
  color: #fff;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}

.chart-container {
  width: 100%;
}
</style>
