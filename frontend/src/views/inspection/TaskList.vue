<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>巡检任务</span>
          <div>
            <el-button type="success" @click="handleImmediateInspection">
              <el-icon><VideoPlay /></el-icon>
              立即巡检
            </el-button>
            <el-button type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>
              新建任务
            </el-button>
          </div>
        </div>
      </template>
      
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="任务名称">
          <el-input v-model="queryParams.taskName" placeholder="请输入任务名称" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="道路">
          <el-select v-model="queryParams.roadId" placeholder="全部" clearable style="width: 150px" @change="handleQueryRoadChange">
            <el-option v-for="road in roadList" :key="road.id" :label="road.roadName" :value="road.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="路段">
          <el-select v-model="queryParams.sectionId" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="section in querySectionList" :key="section.id" :label="section.sectionName" :value="section.id" />
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
          <el-button type="primary" @click="loadTasks">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
      
      <el-table :data="taskList" v-loading="loading" border stripe>
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="taskName" label="任务名称" width="150" />
        <el-table-column prop="roadName" label="道路" width="120">
          <template #default="{ row }">
            {{ getRoadName(row.roadId) }}
          </template>
        </el-table-column>
        <el-table-column prop="sectionName" label="路段" width="100">
          <template #default="{ row }">
            {{ row.sectionId ? getSectionName(row.roadId, row.sectionId) : '全部' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="scheduledStartTime" label="计划开始时间" width="160" />
        <el-table-column prop="scheduledEndTime" label="计划结束时间" width="160" />
        <el-table-column prop="startTime" label="实际开始时间" width="160" />
        <el-table-column prop="endTime" label="实际结束时间" width="160" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDetail(row)">详情</el-button>
            <el-button 
              type="warning" 
              link 
              @click="cancelTask(row)" 
              v-if="row.status === 'running'"
            >取消</el-button>
            <el-button 
              type="danger" 
              link 
              @click="handleDelete(row)"
              v-if="row.status !== 'running'"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination
        v-model:current-page="queryParams.page"
        v-model:page-size="queryParams.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadTasks"
        @current-change="loadTasks"
        class="pagination"
      />
    </el-card>
    
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="form.taskName" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="选择道路" prop="roadId">
          <el-select v-model="form.roadId" placeholder="请选择道路" style="width: 100%" @change="handleRoadChange">
            <el-option v-for="road in roadList" :key="road.id" :label="road.roadName" :value="road.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择路段">
          <el-select v-model="form.sectionId" placeholder="全部路段" clearable style="width: 100%">
            <el-option v-for="section in sectionList" :key="section.id" :label="section.sectionName" :value="section.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="巡检开始时间" prop="scheduledStartTime">
          <el-date-picker 
            v-model="form.scheduledStartTime" 
            type="datetime" 
            placeholder="选择巡检开始时间"
            style="width: 100%"
            value-format="YYYY-MM-DDTHH:mm:ss"
            :disabled-date="disabledDate"
          />
        </el-form-item>
        <el-form-item label="巡检结束时间" prop="scheduledEndTime">
          <el-date-picker 
            v-model="form.scheduledEndTime" 
            type="datetime" 
            placeholder="选择巡检结束时间"
            style="width: 100%"
            value-format="YYYY-MM-DDTHH:mm:ss"
            :disabled-date="disabledDate"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
    
    <el-dialog v-model="immediateDialogVisible" title="立即巡检" width="500px">
      <el-form :model="immediateForm" :rules="immediateRules" ref="immediateFormRef" label-width="100px">
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="immediateForm.taskName" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="选择道路" prop="roadId">
          <el-select v-model="immediateForm.roadId" placeholder="请选择道路" style="width: 100%" @change="handleImmediateRoadChange">
            <el-option v-for="road in roadList" :key="road.id" :label="road.roadName" :value="road.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择路段">
          <el-select v-model="immediateForm.sectionId" placeholder="全部路段" clearable style="width: 100%">
            <el-option v-for="section in immediateSectionList" :key="section.id" :label="section.sectionName" :value="section.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="immediateDialogVisible = false">取消</el-button>
        <el-button type="success" @click="submitImmediateInspection" :loading="submitting">开始巡检</el-button>
      </template>
    </el-dialog>
    
    <el-dialog v-model="detailVisible" title="任务详情" width="700px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="任务名称">{{ currentTask?.taskName }}</el-descriptions-item>
        <el-descriptions-item label="任务编号">{{ currentTask?.taskCode }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentTask?.status)">{{ getStatusName(currentTask?.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="道路">{{ getRoadName(currentTask?.roadId) }}</el-descriptions-item>
        <el-descriptions-item label="路段">{{ currentTask?.sectionId ? getSectionName(currentTask?.roadId, currentTask?.sectionId) : '全部' }}</el-descriptions-item>
        <el-descriptions-item label="计划开始时间">{{ currentTask?.scheduledStartTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="计划结束时间">{{ currentTask?.scheduledEndTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="实际开始时间">{{ currentTask?.startTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="实际结束时间">{{ currentTask?.endTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentTask?.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const immediateDialogVisible = ref(false)
const detailVisible = ref(false)
const dialogTitle = ref('新建巡检任务')
const formRef = ref()
const immediateFormRef = ref()

const taskList = ref([])
const roadList = ref([])
const sectionList = ref([])
const querySectionList = ref([])
const immediateSectionList = ref([])
const allSections = ref({})
const total = ref(0)
const currentTask = ref(null)

const queryParams = reactive({
  page: 1,
  size: 10,
  taskName: '',
  roadId: null,
  sectionId: null,
  status: ''
})

const form = reactive({
  id: null,
  taskName: '',
  taskCode: '',
  roadId: null,
  sectionId: null,
  scheduledStartTime: '',
  scheduledEndTime: '',
  remark: ''
})

const immediateForm = reactive({
  taskName: '',
  roadId: null,
  sectionId: null
})

const rules = {
  taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  roadId: [{ required: true, message: '请选择道路', trigger: 'change' }],
  scheduledStartTime: [{ required: true, message: '请选择巡检开始时间', trigger: 'change' }],
  scheduledEndTime: [{ required: true, message: '请选择巡检结束时间', trigger: 'change' }]
}

const immediateRules = {
  taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  roadId: [{ required: true, message: '请选择道路', trigger: 'change' }]
}

const statusMap = {
  pending: { name: '待执行', type: 'info' },
  running: { name: '执行中', type: 'warning' },
  completed: { name: '已完成', type: 'success' },
  cancelled: { name: '已取消', type: 'danger' }
}

const getStatusName = (status) => statusMap[status]?.name || status
const getStatusType = (status) => statusMap[status]?.type || 'info'

const disabledDate = (time) => {
  return time.getTime() < Date.now() - 8.64e7
}

const getRoadName = (roadId) => {
  const road = roadList.value.find(r => r.id === roadId)
  return road ? road.roadName : '-'
}

const getSectionName = (roadId, sectionId) => {
  const sections = allSections.value[roadId] || []
  const section = sections.find(s => s.id === sectionId)
  return section ? section.sectionName : '-'
}

const loadTasks = async () => {
  loading.value = true
  try {
    const res = await request.get('/inspection/task/list', { params: queryParams })
    taskList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const loadRoads = async () => {
  try {
    const res = await request.get('/road/list', { params: { size: 100 } })
    roadList.value = res.data?.records || []
    
    for (const road of roadList.value) {
      const sectionRes = await request.get(`/road/${road.id}/sections`)
      allSections.value[road.id] = sectionRes.data || []
    }
  } catch (error) {
    console.error(error)
  }
}

const handleRoadChange = async (roadId) => {
  form.sectionId = null
  sectionList.value = []
  
  if (roadId) {
    sectionList.value = allSections.value[roadId] || []
  }
}

const handleImmediateRoadChange = async (roadId) => {
  immediateForm.sectionId = null
  immediateSectionList.value = []
  
  if (roadId) {
    immediateSectionList.value = allSections.value[roadId] || []
  }
}

const handleQueryRoadChange = async (roadId) => {
  queryParams.sectionId = null
  querySectionList.value = []
  
  if (roadId) {
    querySectionList.value = allSections.value[roadId] || []
  }
}

const resetQuery = () => {
  queryParams.taskName = ''
  queryParams.roadId = null
  queryParams.sectionId = null
  queryParams.status = ''
  querySectionList.value = []
  loadTasks()
}

const handleAdd = () => {
  Object.assign(form, {
    id: null,
    taskName: '',
    taskCode: '',
    roadId: null,
    sectionId: null,
    scheduledStartTime: '',
    scheduledEndTime: '',
    remark: ''
  })
  sectionList.value = []
  dialogTitle.value = '新建巡检任务'
  dialogVisible.value = true
}

const handleImmediateInspection = () => {
  Object.assign(immediateForm, {
    taskName: '',
    roadId: null,
    sectionId: null
  })
  immediateSectionList.value = []
  immediateDialogVisible.value = true
}

const submitForm = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    if (form.scheduledStartTime && form.scheduledEndTime) {
      if (new Date(form.scheduledStartTime) >= new Date(form.scheduledEndTime)) {
        ElMessage.error('结束时间必须大于开始时间')
        return
      }
    }
    
    submitting.value = true
    try {
      await request.post('/inspection/task', form)
      ElMessage.success('创建成功')
      dialogVisible.value = false
      loadTasks()
    } catch (error) {
      console.error(error)
    } finally {
      submitting.value = false
    }
  })
}

const submitImmediateInspection = async () => {
  if (!immediateFormRef.value) return
  
  await immediateFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitting.value = true
    try {
      await request.post('/inspection/task/immediate', immediateForm)
      ElMessage.success('巡检任务已开始执行')
      immediateDialogVisible.value = false
      loadTasks()
    } catch (error) {
      console.error(error)
    } finally {
      submitting.value = false
    }
  })
}

const cancelTask = async (row) => {
  try {
    await ElMessageBox.confirm('确定取消该巡检任务吗？', '提示')
    await request.post(`/inspection/task/${row.id}/cancel`)
    ElMessage.success('任务已取消')
    loadTasks()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

const viewDetail = (row) => {
  currentTask.value = row
  detailVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该任务吗？', '提示')
    await request.delete(`/inspection/task/${row.id}`)
    ElMessage.success('删除成功')
    loadTasks()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

onMounted(() => {
  loadTasks()
  loadRoads()
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
