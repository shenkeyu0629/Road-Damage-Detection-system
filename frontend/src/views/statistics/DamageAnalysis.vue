<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>历史病害分析</span>
        </div>
      </template>
      <p style="color: #909399; margin-bottom: 20px;">
        通过对历史病害数据的统计分析，识别病害高发路段，为道路养护决策提供数据支持。
      </p>
    </el-card>
    
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-value">{{ overviewStats.totalRoads }}</div>
            <div class="stat-label">涉及道路数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-value danger">{{ overviewStats.totalDamages }}</div>
            <div class="stat-label">病害总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-value warning">{{ overviewStats.severeCount }}</div>
            <div class="stat-label">严重病害数</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-card style="margin-top: 20px">
      <template #header>
        <span>病害高频路段 TOP10</span>
      </template>
      <el-table :data="topSections" v-loading="loading" border stripe>
        <el-table-column type="index" label="排名" width="60" />
        <el-table-column prop="roadName" label="道路名称" min-width="150" />
        <el-table-column prop="sectionName" label="路段" min-width="120" />
        <el-table-column prop="totalDamages" label="病害总数" width="100">
          <template #default="{ row }">
            <el-tag type="danger">{{ row.totalDamages }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="recordCount" label="记录次数" width="100" />
        <el-table-column prop="severeCount" label="严重病害" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.severeCount > 0" type="danger">{{ row.severeCount }}</el-tag>
            <span v-else>0</span>
          </template>
        </el-table-column>
        <el-table-column label="风险等级" width="100">
          <template #default="{ row }">
            <el-tag :type="getRiskLevel(row.totalDamages, row.severeCount).type">
              {{ getRiskLevel(row.totalDamages, row.severeCount).label }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>按道路分类统计</span>
          </template>
          <div ref="roadChartRef" style="height: 350px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>病害等级分布</span>
          </template>
          <div ref="levelChartRef" style="height: 350px;"></div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-card style="margin-top: 20px">
      <template #header>
        <span>各路段病害详细分析</span>
      </template>
      <el-table :data="sectionAnalysis" v-loading="loading" border stripe max-height="400">
        <el-table-column prop="roadName" label="道路名称" min-width="120" />
        <el-table-column prop="sectionName" label="路段" min-width="100" />
        <el-table-column prop="recordCount" label="记录次数" width="90" />
        <el-table-column prop="totalDamages" label="病害总数" width="90">
          <template #default="{ row }">
            <el-tag type="danger">{{ row.totalDamages }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="severeCount" label="严重" width="70">
          <template #default="{ row }">
            <span v-if="row.severeCount > 0" style="color: #f56c6c; font-weight: bold;">{{ row.severeCount }}</span>
            <span v-else>0</span>
          </template>
        </el-table-column>
        <el-table-column prop="minorCount" label="轻微" width="70">
          <template #default="{ row }">
            <span v-if="row.minorCount > 0" style="color: #67c23a;">{{ row.minorCount }}</span>
            <span v-else>0</span>
          </template>
        </el-table-column>
        <el-table-column prop="damageTypes" label="病害类型" min-width="150" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import request from '@/utils/request'
import * as echarts from 'echarts'

const loading = ref(false)
const topSections = ref([])
const sectionAnalysis = ref([])
const roadAnalysis = ref([])

const roadChartRef = ref(null)
const levelChartRef = ref(null)
let roadChart = null
let levelChart = null

const overviewStats = ref({
  totalRoads: 0,
  totalDamages: 0,
  severeCount: 0
})

const getRiskLevel = (totalDamages, severeCount) => {
  if (severeCount >= 3 || totalDamages >= 10) {
    return { label: '高风险', type: 'danger' }
  } else if (severeCount >= 1 || totalDamages >= 5) {
    return { label: '中风险', type: 'warning' }
  } else {
    return { label: '低风险', type: 'success' }
  }
}

const loadTopSections = async () => {
  try {
    const res = await request.get('/damage-history/analysis/top-sections')
    topSections.value = res.data || []
  } catch (error) {
    console.error('Load top sections error:', error)
  }
}

const loadSectionAnalysis = async () => {
  try {
    const res = await request.get('/damage-history/analysis/by-section')
    sectionAnalysis.value = res.data || []
  } catch (error) {
    console.error('Load section analysis error:', error)
  }
}

const loadRoadAnalysis = async () => {
  try {
    const res = await request.get('/damage-history/analysis/by-road')
    roadAnalysis.value = res.data || []
    
    let totalDamages = 0
    let severeCount = 0
    
    roadAnalysis.value.forEach(item => {
      totalDamages += item.totalDamages || 0
      severeCount += item.severeCount || 0
    })
    
    overviewStats.value = {
      totalRoads: roadAnalysis.value.length,
      totalDamages: totalDamages,
      severeCount: severeCount
    }
    
    renderRoadChart()
    renderLevelChart()
  } catch (error) {
    console.error('Load road analysis error:', error)
  }
}

const renderRoadChart = () => {
  if (roadChart) {
    roadChart.dispose()
  }
  
  roadChart = echarts.init(roadChartRef.value)
  
  const displayData = roadAnalysis.value.slice(0, 10)
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
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
      data: displayData.map(item => item.roadName),
      axisLabel: {
        interval: 0,
        rotate: 30,
        fontSize: 10
      }
    },
    yAxis: {
      type: 'value',
      name: '病害数量',
      minInterval: 1
    },
    series: [
      {
        name: '病害总数',
        type: 'bar',
        barWidth: '60%',
        data: displayData.map(item => item.totalDamages),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#f56c6c' },
            { offset: 1, color: '#f89898' }
          ])
        }
      }
    ]
  }
  
  roadChart.setOption(option)
}

const renderLevelChart = () => {
  if (levelChart) {
    levelChart.dispose()
  }
  
  levelChart = echarts.init(levelChartRef.value)
  
  let severeTotal = 0
  let minorTotal = 0
  
  roadAnalysis.value.forEach(item => {
    severeTotal += item.severeCount || 0
    minorTotal += item.minorCount || 0
  })
  
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
        name: '病害等级',
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
        data: [
          { value: severeTotal, name: '严重', itemStyle: { color: '#f56c6c' } },
          { value: minorTotal, name: '轻微', itemStyle: { color: '#67c23a' } }
        ]
      }
    ]
  }
  
  levelChart.setOption(option)
}

const handleResize = () => {
  if (roadChart) {
    roadChart.resize()
  }
  if (levelChart) {
    levelChart.resize()
  }
}

onMounted(() => {
  loadTopSections()
  loadSectionAnalysis()
  loadRoadAnalysis()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (roadChart) {
    roadChart.dispose()
  }
  if (levelChart) {
    levelChart.dispose()
  }
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-card {
  border-radius: 12px;
}

.stat-item {
  text-align: center;
  padding: 20px;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #409eff;
}

.stat-value.danger {
  color: #f56c6c;
}

.stat-value.warning {
  color: #e6a23c;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 8px;
}
</style>
