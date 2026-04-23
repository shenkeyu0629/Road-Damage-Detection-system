<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>道路管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            添加道路
          </el-button>
        </div>
      </template>
      
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="roadName" label="道路名称" />
        <el-table-column prop="roadCode" label="道路编码" width="120" />
        <el-table-column prop="roadLevel" label="道路等级" width="100">
          <template #default="{ row }">
            <el-tag>{{ getLevelName(row.roadLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalLength" label="总长度(km)" width="100" />
        <el-table-column prop="region" label="所属区域" width="120" />
        <el-table-column prop="manageUnit" label="管理单位" />
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
            <el-button type="primary" link @click="handleViewSections(row)">路段</el-button>
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
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="道路名称" prop="roadName">
          <el-input v-model="form.roadName" placeholder="请输入道路名称" />
        </el-form-item>
        <el-form-item label="道路编码" prop="roadCode">
          <el-input v-model="form.roadCode" placeholder="请输入道路编码" />
        </el-form-item>
        <el-form-item label="道路等级" prop="roadLevel">
          <el-select v-model="form.roadLevel" placeholder="请选择道路等级" style="width: 100%">
            <el-option v-for="item in levelOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="总长度">
          <el-input-number v-model="form.totalLength" :min="0" :precision="2" placeholder="请输入总长度(km)" style="width: 100%" />
        </el-form-item>
        <el-form-item label="起点">
          <el-input v-model="form.startPoint" placeholder="请输入起点" />
        </el-form-item>
        <el-form-item label="终点">
          <el-input v-model="form.endPoint" placeholder="请输入终点" />
        </el-form-item>
        <el-form-item label="所属区域">
          <el-input v-model="form.region" placeholder="请输入所属区域" />
        </el-form-item>
        <el-form-item label="管理单位">
          <el-input v-model="form.manageUnit" placeholder="请输入管理单位" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" rows="3" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRoadList, createRoad, updateRoad, deleteRoad } from '@/api/road'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const tableData = ref([])
const formRef = ref()

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const form = reactive({
  id: null,
  roadName: '',
  roadCode: '',
  roadLevel: '',
  totalLength: null,
  startPoint: '',
  endPoint: '',
  region: '',
  manageUnit: '',
  status: 1,
  description: ''
})

const rules = {
  roadName: [{ required: true, message: '请输入道路名称', trigger: 'blur' }],
  roadCode: [{ required: true, message: '请输入道路编码', trigger: 'blur' }],
  roadLevel: [{ required: true, message: '请选择道路等级', trigger: 'change' }]
}

const levelOptions = [
  { label: '高速公路', value: 'highway' },
  { label: '国道', value: 'national' },
  { label: '省道', value: 'provincial' },
  { label: '县道', value: 'county' },
  { label: '乡道', value: 'township' },
  { label: '城市道路', value: 'urban' }
]

const getLevelName = (level) => {
  const option = levelOptions.find(opt => opt.value === level)
  return option ? option.label : level
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getRoadList({ page: pagination.page, size: pagination.size })
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增道路'
  Object.assign(form, {
    id: null,
    roadName: '',
    roadCode: '',
    roadLevel: '',
    totalLength: null,
    startPoint: '',
    endPoint: '',
    region: '',
    manageUnit: '',
    status: 1,
    description: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑道路'
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitting.value = true
    try {
      if (form.id) {
        await updateRoad(form)
      } else {
        await createRoad(form)
      }
      ElMessage.success('保存成功')
      dialogVisible.value = false
      loadData()
    } catch (error) {
      console.error(error)
    } finally {
      submitting.value = false
    }
  })
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该道路吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteRoad(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

const handleViewSections = (row) => {
  router.push({ path: '/road/section', query: { roadId: row.id } })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
