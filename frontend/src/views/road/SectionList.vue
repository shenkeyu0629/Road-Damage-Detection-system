<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>路段管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增路段
          </el-button>
        </div>
      </template>
      
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="所属道路">
          <el-select v-model="searchForm.roadId" placeholder="请选择道路" clearable>
            <el-option
              v-for="road in roadList"
              :key="road.id"
              :label="road.roadName"
              :value="road.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="roadName" label="所属道路" width="150">
          <template #default="{ row }">
            <el-tag type="primary">{{ row.roadName || getRoadName(row.roadId) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sectionName" label="路段名称" min-width="120" />
        <el-table-column prop="sectionCode" label="路段编码" width="120" />
        <el-table-column prop="startStake" label="起始桩号" width="100" />
        <el-table-column prop="endStake" label="终止桩号" width="100" />
        <el-table-column prop="length" label="长度(km)" width="100" />
        <el-table-column prop="laneCount" label="车道数" width="80" />
        <el-table-column prop="pavementType" label="路面类型" width="100">
          <template #default="{ row }">
            {{ getPavementType(row.pavementType) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="warning" link @click="openFolder(row)">打开文件夹</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>
    
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="所属道路" prop="roadId">
          <el-select v-model="form.roadId" placeholder="请选择道路" @change="handleRoadChange">
            <el-option
              v-for="road in roadList"
              :key="road.id"
              :label="road.roadName"
              :value="road.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="路段名称" prop="sectionName">
          <el-input v-model="form.sectionName" placeholder="请输入路段名称" />
        </el-form-item>
        <el-form-item label="路段编码" prop="sectionCode">
          <el-input v-model="form.sectionCode" placeholder="请输入路段编码" />
        </el-form-item>
        <el-form-item label="起始桩号" prop="startStake">
          <el-input v-model="form.startStake" placeholder="如: K0+000" />
        </el-form-item>
        <el-form-item label="终止桩号" prop="endStake">
          <el-input v-model="form.endStake" placeholder="如: K1+000" />
        </el-form-item>
        <el-form-item label="长度(km)" prop="length">
          <el-input-number v-model="form.length" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="车道数" prop="laneCount">
          <el-input-number v-model="form.laneCount" :min="1" :max="20" />
        </el-form-item>
        <el-form-item label="路面类型" prop="pavementType">
          <el-select v-model="form.pavementType" placeholder="请选择路面类型">
            <el-option label="沥青路面" value="asphalt" />
            <el-option label="水泥路面" value="concrete" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSectionList, createSection, updateSection, deleteSection } from '@/api/road'
import { getRoadList } from '@/api/road'
import request from '@/utils/request'

const route = useRoute()
const loading = ref(false)
const tableData = ref([])
const roadList = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增路段')
const formRef = ref()

const searchForm = reactive({
  roadId: null
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const form = reactive({
  id: null,
  roadId: null,
  sectionName: '',
  sectionCode: '',
  startStake: '',
  endStake: '',
  length: 0,
  laneCount: 2,
  pavementType: 'asphalt',
  status: 1
})

const rules = {
  roadId: [{ required: true, message: '请选择道路', trigger: 'change' }],
  sectionName: [{ required: true, message: '请输入路段名称', trigger: 'blur' }]
}

const pavementMap = {
  asphalt: '沥青路面',
  concrete: '水泥路面',
  other: '其他'
}

const getPavementType = (type) => pavementMap[type] || type

const getRoadName = (roadId) => {
  const road = roadList.value.find(r => r.id === roadId)
  return road ? road.roadName : '-'
}

const loadRoadList = async () => {
  const res = await getRoadList({ page: 1, size: 100 })
  roadList.value = res.data?.records || []
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getSectionList({
      page: pagination.page,
      size: pagination.size,
      ...searchForm
    })
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadData()
}

const handleReset = () => {
  searchForm.roadId = null
  handleSearch()
}

const handleAdd = () => {
  dialogTitle.value = '新增路段'
  Object.assign(form, {
    id: null,
    roadId: searchForm.roadId,
    sectionName: '',
    sectionCode: '',
    startStake: '',
    endStake: '',
    length: 0,
    laneCount: 2,
    pavementType: 'asphalt',
    status: 1
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑路段'
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleRoadChange = () => {
  form.sectionName = ''
  form.sectionCode = ''
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定要删除该路段吗？', '提示', { type: 'warning' })
  await deleteSection(row.id)
  ElMessage.success('删除成功')
  loadData()
}

const handleSubmit = async () => {
  await formRef.value.validate()
  
  if (form.id) {
    await updateSection(form)
    ElMessage.success('更新成功')
  } else {
    await createSection(form)
    ElMessage.success('创建成功')
  }
  
  dialogVisible.value = false
  loadData()
}

const openFolder = async (row) => {
  try {
    const road = roadList.value.find(r => r.id === row.roadId)
    if (!road) {
      ElMessage.warning('找不到对应的道路信息')
      return
    }
    
    await request.post('/storage/open-folder', null, {
      params: {
        roadId: row.roadId,
        roadName: road.roadName,
        roadCode: road.roadCode,
        sectionId: row.id,
        sectionName: row.sectionName,
        sectionCode: row.sectionCode
      }
    })
    ElMessage.success('已打开文件夹')
  } catch (error) {
    ElMessage.error('打开文件夹失败')
    console.error(error)
  }
}

onMounted(async () => {
  await loadRoadList()
  if (route.query.roadId) {
    searchForm.roadId = Number(route.query.roadId)
  }
  loadData()
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

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
