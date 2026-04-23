<template>
  <div class="monitor-container">
    <div class="monitor-header">
      <div class="header-left">
        <h2>实时监控中心</h2>
        <el-tag :type="isConnected ? 'success' : 'danger'">
          {{ isConnected ? '在线' : '离线' }}
        </el-tag>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="startAllDetection" :disabled="!isConnected || cameras.length === 0">
          <el-icon><video-play /></el-icon>
          开始检测
        </el-button>
        <el-button @click="stopAllDetection" :disabled="!isConnected">
          <el-icon><video-pause /></el-icon>
          停止检测
        </el-button>
      </div>
    </div>
    
    <div class="monitor-body">
      <div class="video-grid grid-9">
        <div 
          v-for="camera in displayCameras" 
          :key="camera.id"
          class="video-cell"
          :class="{ 'selected': selectedCamera?.id === camera.id }"
          @click="selectCamera(camera)"
        >
          <div class="video-header">
            <span class="camera-info">
              {{ camera.roadName }} - {{ camera.sectionName }}
            </span>
            <div class="video-controls">
              <el-button-group size="small">
                <el-button @click.stop="toggleFullscreen(camera)" icon="full-screen" />
                <el-button @click.stop="togglePause(camera)" :icon="camera.paused ? 'video-play' : 'video-pause'" />
                <el-button @click.stop="removeCamera(camera)" icon="delete" type="danger" />
              </el-button-group>
            </div>
          </div>
          
          <div class="video-content">
            <video 
              :ref="el => videoRefs[camera.id] = el"
              :src="camera.videoUrl"
              autoplay 
              muted 
              loop
              @loadeddata="onVideoLoaded(camera)"
              @error="onVideoError(camera)"
            />
            <canvas 
              :ref="el => canvasRefs[camera.id] = el"
              class="overlay-canvas"
            />
            
            <div class="video-label">
              <el-icon><Location /></el-icon>
              <span>{{ camera.roadName }} - {{ camera.sectionName }}</span>
            </div>
            
            <div v-if="camera.paused" class="paused-overlay">
              <el-icon :size="40"><video-pause /></el-icon>
              <span>已暂停</span>
            </div>
            
            <div v-if="!camera.connected" class="disconnected-overlay">
              <el-icon :size="40"><warning /></el-icon>
              <span>连接中断</span>
            </div>
            
            <div v-if="camera.detections?.length" class="detection-count">
              <el-badge :value="camera.detections.length" type="danger">
                <el-button size="small" type="danger" circle>
                  <el-icon><warning /></el-icon>
                </el-button>
              </el-badge>
            </div>
          </div>
          
          <div class="video-footer">
            <el-tag v-if="camera.status === 'normal'" type="success" size="small">正常</el-tag>
            <el-tag v-else-if="camera.status === 'warning'" type="warning" size="small">告警</el-tag>
            <el-tag v-else type="danger" size="small">异常</el-tag>
            <span class="detection-info" v-if="camera.detectionEnabled">
              检测中 | 延迟: {{ camera.latency || 0 }}ms
            </span>
          </div>
        </div>
        
        <div v-for="i in emptySlots" :key="`empty-${i}`" class="video-cell empty">
          <div class="empty-content">
            <el-icon :size="40"><video-camera /></el-icon>
            <span>暂无视频源</span>
            <span class="empty-tip">请在路段管理中配置视频文件夹</span>
          </div>
        </div>
      </div>
    </div>
    
    <el-dialog v-model="fullscreenDialog" :title="selectedCamera?.roadName + ' - 实时监控'" width="90%" top="5vh" :close-on-click-modal="false">
      <div class="fullscreen-video" v-if="selectedCamera">
        <video 
          ref="fullscreenVideoRef"
          :src="selectedCamera.videoUrl"
          autoplay 
          muted 
          loop
          @timeupdate="onVideoTimeUpdate"
        />
        <canvas ref="fullscreenCanvasRef" class="overlay-canvas" />
        
        <div class="video-timeline">
          <el-slider v-model="videoProgress" :max="100" @change="seekVideo" />
          <div class="timeline-info">
            <span>{{ formatTime(currentTime) }} / {{ formatTime(duration) }}</span>
          </div>
        </div>
        
        <div class="fullscreen-controls">
          <el-button-group>
            <el-button @click="togglePause(selectedCamera)" :icon="selectedCamera.paused ? 'video-play' : 'video-pause'" />
            <el-button @click="seekBackward" icon="d-arrow-left" />
            <el-button @click="seekForward" icon="d-arrow-right" />
          </el-button-group>
          
          <div class="detection-toggle">
            <span>实时检测：</span>
            <el-switch v-model="selectedCamera.detectionEnabled" @change="toggleDetection(selectedCamera)" />
          </div>
        </div>
        
        <div class="detection-panel">
          <h4>检测信息 <el-tag v-if="selectedCamera.detections?.length" type="danger">{{ selectedCamera.detections.length }}处病害</el-tag></h4>
          <el-table :data="selectedCamera.detections || []" border size="small" max-height="200">
            <el-table-column prop="class_name" label="病害类型" width="120" />
            <el-table-column prop="level" label="等级" width="80">
              <template #default="{ row }">
                <el-tag :type="row.level === 'severe' ? 'danger' : 'warning'" size="small">
                  {{ row.level === 'severe' ? '高危' : '普通' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="confidence" label="置信度" width="100">
              <template #default="{ row }">{{ (row.confidence * 100).toFixed(1) }}%</template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'
import axios from 'axios'
import request from '@/utils/request'

const isConnected = ref(true)
const cameras = ref([])
const selectedCamera = ref(null)
const fullscreenDialog = ref(false)
const videoRefs = ref({})
const canvasRefs = ref({})
const fullscreenVideoRef = ref(null)
const fullscreenCanvasRef = ref(null)
const videoProgress = ref(0)
const currentTime = ref(0)
const duration = ref(0)

let detectionIntervals = {}
let isProcessing = {}
let hasAlarmed = {}
let canvasInitialized = {}

const displayCameras = computed(() => {
  return cameras.value.slice(0, 9)
})

const emptySlots = computed(() => {
  return Math.max(0, 9 - displayCameras.value.length)
})

const selectCamera = (camera) => {
  selectedCamera.value = camera
}

const toggleFullscreen = (camera) => {
  selectedCamera.value = camera
  fullscreenDialog.value = true
}

const togglePause = (camera) => {
  camera.paused = !camera.paused
  const video = videoRefs.value[camera.id]
  if (video) {
    if (camera.paused) {
      video.pause()
    } else {
      video.play()
    }
  }
}

const loadAllSections = async () => {
  try {
    const res = await request.get('/road/all-sections')
    const allSections = res.data || []
    
    cameras.value = allSections.map((section) => ({
      id: `section-${section.id}`,
      roadId: section.roadId,
      sectionId: section.id,
      roadName: section.roadName || '未知道路',
      sectionName: section.sectionName || '未知路段',
      videoFolderPath: section.videoFolderPath,
      videoUrl: null,
      videoFiles: [],
      status: 'normal',
      connected: false,
      detectionEnabled: false,
      detections: [],
      paused: false,
      latency: 0
    }))
    
    for (const camera of cameras.value) {
      if (camera.videoFolderPath) {
        try {
          await loadVideoFilesForSection(camera)
        } catch (e) {
          console.error('Load video files for section error:', e)
        }
      }
    }
  } catch (error) {
    console.error('Load all sections error:', error)
    cameras.value = []
  }
}

const loadVideoFilesForSection = async (camera) => {
  try {
    const res = await request.get('storage/list-videos', {
      params: { path: camera.videoFolderPath }
    })
    
    if (res.data && res.data.files && res.data.files.length > 0) {
      camera.videoFiles = res.data.files
      camera.videoUrl = '/api' + res.data.files[0].url
      camera.connected = true
    }
  } catch (error) {
    console.error('Load video files error:', error)
  }
}

const startAllDetection = () => {
  displayCameras.value.forEach(camera => {
    camera.detectionEnabled = true
    startDetection(camera)
  })
  ElMessage.success('已开始全部检测')
}

const stopAllDetection = () => {
  displayCameras.value.forEach(camera => {
    camera.detectionEnabled = false
    stopDetection(camera)
  })
  ElMessage.success('已停止全部检测')
}

const toggleDetection = (camera) => {
  if (camera.detectionEnabled) {
    startDetection(camera)
  } else {
    stopDetection(camera)
  }
}

const startDetection = (camera) => {
  if (detectionIntervals[camera.id]) return
  isProcessing[camera.id] = false
  hasAlarmed[camera.id] = false
  
  const displayCanvas = canvasRefs.value[camera.id]
  if (displayCanvas && displayCanvas.parentElement) {
    displayCanvas.width = displayCanvas.parentElement.clientWidth
    displayCanvas.height = displayCanvas.parentElement.clientHeight
    canvasInitialized[camera.id] = true
  }
  
  const detectFrame = async () => {
    const video = videoRefs.value[camera.id]
    const displayCanvas = canvasRefs.value[camera.id]
    
    if (!video || !displayCanvas || video.paused || video.ended) return
    if (isProcessing[camera.id]) return
    
    isProcessing[camera.id] = true
    
    const videoWidth = video.videoWidth || 640
    const videoHeight = video.videoHeight || 480
    const scale = 0.5
    const scaledWidth = videoWidth * scale
    const scaledHeight = videoHeight * scale
    
    const captureCanvas = document.createElement('canvas')
    captureCanvas.width = scaledWidth
    captureCanvas.height = scaledHeight
    const captureCtx = captureCanvas.getContext('2d')
    captureCtx.drawImage(video, 0, 0, scaledWidth, scaledHeight)
    
    captureCanvas.toBlob(async (blob) => {
      if (!blob) {
        isProcessing[camera.id] = false
        return
      }
      
      const formData = new FormData()
      formData.append('file', blob, 'frame.jpg')
      formData.append('conf_threshold', 0.3)
      
      try {
        const startTime = performance.now()
        const res = await axios.post('/ai-api/api/v1/detection/detect-upload', formData, {
          headers: { 'Content-Type': 'multipart/form-data' },
          timeout: 3000
        })
        const endTime = performance.now()
        
        camera.latency = Math.round(endTime - startTime)
        
        const scaledDetections = (res.data.detections || []).map(det => ({
          ...det,
          bbox: {
            x1: Math.round(det.bbox.x1 / scale),
            y1: Math.round(det.bbox.y1 / scale),
            x2: Math.round(det.bbox.x2 / scale),
            y2: Math.round(det.bbox.y2 / scale)
          }
        }))
        
        camera.detections = scaledDetections
        
        if (canvasInitialized[camera.id]) {
          drawDetections(displayCanvas, scaledDetections, video)
        }
        
        checkAlarm(camera, scaledDetections)
        
        if (scaledDetections.length > 0) {
          saveDetectionsToHistory(camera, scaledDetections)
        }
      } catch (error) {
        console.error('Detection error:', error)
      } finally {
        isProcessing[camera.id] = false
      }
    }, 'image/jpeg', 0.6)
  }
  
  detectFrame()
  detectionIntervals[camera.id] = setInterval(detectFrame, 500)
}

const saveDetectionsToHistory = async (camera, detections) => {
  try {
    const severeDamages = detections.filter(d => d.level === 'severe')
    const moderateDamages = detections.filter(d => d.level === 'moderate')
    const minorDamages = detections.filter(d => d.level === 'minor' || !d.level)
    
    const damageLevel = severeDamages.length > 0 ? '严重' : 
                        (moderateDamages.length > 0 ? '中等' : '轻微')
    
    const damageTypes = [...new Set(detections.map(d => d.class_name))].join('、')
    
    await request.post('/damage-history', {
      roadId: camera.roadId || null,
      sectionId: camera.sectionId || null,
      roadName: camera.roadName || '未知道路',
      sectionName: camera.sectionName || '未知路段',
      damageCount: detections.length,
      damageLevel: damageLevel,
      damageTypes: damageTypes,
      videoSource: camera.roadName + ' - ' + camera.sectionName,
      status: '未处理'
    })
  } catch (error) {
    console.error('Save detections error:', error)
  }
}

const stopDetection = (camera) => {
  if (detectionIntervals[camera.id]) {
    clearInterval(detectionIntervals[camera.id])
    delete detectionIntervals[camera.id]
  }
  
  if (hasAlarmed[camera.id] !== undefined) {
    delete hasAlarmed[camera.id]
  }
  
  if (canvasInitialized[camera.id] !== undefined) {
    delete canvasInitialized[camera.id]
  }
  
  const canvas = canvasRefs.value[camera.id]
  if (canvas) {
    const ctx = canvas.getContext('2d')
    ctx.clearRect(0, 0, canvas.width, canvas.height)
  }
  
  camera.detections = []
}

const drawDetections = (canvas, detections, videoElement) => {
  const ctx = canvas.getContext('2d')
  ctx.clearRect(0, 0, canvas.width, canvas.height)
  
  if (!videoElement || detections.length === 0) return
  
  const containerWidth = canvas.width
  const containerHeight = canvas.height
  const videoWidth = videoElement.videoWidth || 640
  const videoHeight = videoElement.videoHeight || 480
  
  const videoAspect = videoWidth / videoHeight
  const containerAspect = containerWidth / containerHeight
  
  let displayWidth, displayHeight, offsetX, offsetY
  
  if (videoAspect > containerAspect) {
    displayWidth = containerWidth
    displayHeight = containerWidth / videoAspect
    offsetX = 0
    offsetY = (containerHeight - displayHeight) / 2
  } else {
    displayHeight = containerHeight
    displayWidth = containerHeight * videoAspect
    offsetX = (containerWidth - displayWidth) / 2
    offsetY = 0
  }
  
  const scaleX = displayWidth / videoWidth
  const scaleY = displayHeight / videoHeight
  
  const damageColors = {
    '坑洞': { fill: 'rgba(255, 0, 0, 0.4)', stroke: '#ff0000', name: '坑洞' },
    '裂缝': { fill: 'rgba(255, 165, 0, 0.3)', stroke: '#ffa500', name: '裂缝' },
    'pothole': { fill: 'rgba(255, 0, 0, 0.4)', stroke: '#ff0000', name: '坑洞' },
    'crack': { fill: 'rgba(255, 165, 0, 0.3)', stroke: '#ffa500', name: '裂缝' },
    'default': { fill: 'rgba(255, 170, 0, 0.3)', stroke: '#ffaa00', name: '病害' }
  }
  
  const levelNames = {
    'severe': '高危',
    'moderate': '中等',
    'minor': '轻微',
    'none': '无'
  }
  
  detections.forEach(det => {
    const bbox = det.bbox
    const className = det.class_name || 'default'
    const colorInfo = damageColors[className] || damageColors['default']
    const isSevere = det.level === 'severe'
    
    const x1 = offsetX + bbox.x1 * scaleX
    const y1 = offsetY + bbox.y1 * scaleY
    const x2 = offsetX + bbox.x2 * scaleX
    const y2 = offsetY + bbox.y2 * scaleY
    const width = x2 - x1
    const height = y2 - y1
    
    ctx.fillStyle = colorInfo.fill
    ctx.fillRect(x1, y1, width, height)
    
    ctx.strokeStyle = isSevere ? '#ff0000' : colorInfo.stroke
    ctx.lineWidth = isSevere ? 4 : 3
    ctx.strokeRect(x1, y1, width, height)
    
    const typeName = colorInfo.name
    const levelText = `[${levelNames[det.level] || '轻微'}]`
    const label = `${typeName}${levelText} ${(det.confidence * 100).toFixed(0)}%`
    ctx.font = 'bold 16px Arial'
    const textWidth = ctx.measureText(label).width
    
    ctx.fillStyle = isSevere ? '#ff0000' : colorInfo.stroke
    ctx.fillRect(x1, y1 - 26, textWidth + 10, 26)
    
    ctx.fillStyle = '#ffffff'
    ctx.fillText(label, x1 + 5, y1 - 7)
  })
}

const checkAlarm = (camera, detections) => {
  const severeDamages = detections.filter(d => d.level === 'severe')
  
  if (severeDamages.length > 0) {
    camera.status = 'warning'
    
    if (!hasAlarmed[camera.id]) {
      hasAlarmed[camera.id] = true
      const locationInfo = `${camera.roadName}-${camera.sectionName}`
      
      ElNotification({
        title: `高危病害告警 - ${locationInfo}`,
        message: `检测到 ${severeDamages.length} 处高危病害：${severeDamages.map(d => d.class_name).join('、')}`,
        type: 'error',
        duration: 3000
      })
    }
  } else {
    camera.status = 'normal'
  }
}

const onVideoLoaded = (camera) => {
  camera.connected = true
}

const onVideoError = (camera) => {
  camera.connected = false
}

const onVideoTimeUpdate = () => {
  if (fullscreenVideoRef.value) {
    currentTime.value = fullscreenVideoRef.value.currentTime
    duration.value = fullscreenVideoRef.value.duration
    videoProgress.value = (currentTime.value / duration.value) * 100 || 0
  }
}

const seekVideo = (value) => {
  if (fullscreenVideoRef.value && duration.value) {
    fullscreenVideoRef.value.currentTime = (value / 100) * duration.value
  }
}

const seekBackward = () => {
  if (fullscreenVideoRef.value) {
    fullscreenVideoRef.value.currentTime = Math.max(0, fullscreenVideoRef.value.currentTime - 5)
  }
}

const seekForward = () => {
  if (fullscreenVideoRef.value) {
    fullscreenVideoRef.value.currentTime = Math.min(duration.value, fullscreenVideoRef.value.currentTime + 5)
  }
}

const formatTime = (seconds) => {
  if (!seconds || isNaN(seconds)) return '00:00'
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

onMounted(async () => {
  isConnected.value = true
  try {
    await loadAllSections()
    checkRunningInspectionTasks()
  } catch (error) {
    console.error('Init error:', error)
    cameras.value = []
  }
})

const checkRunningInspectionTasks = async () => {
  try {
    const res = await request.get('inspection/task/running')
    const runningTasks = res.data || []
    
    if (runningTasks.length > 0) {
      for (const task of runningTasks) {
        const camera = cameras.value.find(c => 
          c.roadId === task.roadId && 
          (task.sectionId === null || c.sectionId === task.sectionId)
        )
        
        if (camera && !camera.detectionEnabled) {
          camera.detectionEnabled = true
          camera.taskId = task.id
          startDetection(camera)
          console.log(`巡检任务 ${task.taskName} 已自动开始检测`)
        }
      }
    }
  } catch (error) {
    console.error('Check running inspection tasks error:', error)
  }
}

onUnmounted(() => {
  Object.keys(detectionIntervals).forEach(id => {
    clearInterval(detectionIntervals[id])
  })
})
</script>

<style scoped>
.monitor-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #0a0a0a;
}

.monitor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  border-bottom: 1px solid #2a2a4a;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 15px;
}

.header-left h2 {
  color: #fff;
  margin: 0;
  font-size: 20px;
}

.header-center {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.monitor-body {
  flex: 1;
  padding: 15px;
  overflow: auto;
}

.video-grid {
  display: grid;
  gap: 10px;
  height: 100%;
  grid-template-rows: repeat(3, 1fr);
}

.grid-1 {
  grid-template-columns: 1fr;
  grid-template-rows: 1fr;
}

.grid-4 {
  grid-template-columns: repeat(2, 1fr);
  grid-template-rows: repeat(2, 1fr);
}

.grid-9 {
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: repeat(3, 1fr);
}

.grid-16 {
  grid-template-columns: repeat(4, 1fr);
  grid-template-rows: repeat(4, 1fr);
}

.video-cell {
  background: #1a1a2e;
  border-radius: 8px;
  overflow: hidden;
  border: 2px solid #2a2a4a;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.video-cell:hover {
  border-color: #4a9eff;
}

.video-cell.selected {
  border-color: #4a9eff;
  box-shadow: 0 0 15px rgba(74, 158, 255, 0.3);
}

.video-cell.empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 0;
  aspect-ratio: 16 / 9;
}

.empty-content {
  text-align: center;
  color: #666;
}

.empty-content .el-icon {
  margin-bottom: 10px;
  color: #444;
}

.empty-content span {
  display: block;
  margin-bottom: 15px;
}

.video-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 12px;
}

.camera-info {
  font-weight: 500;
}

.video-content {
  flex: 1;
  position: relative;
  background: #000;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.video-content video {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.overlay-canvas {
  position: absolute;
  top: 0;
  left: 0;
  pointer-events: none;
}

.video-label {
  position: absolute;
  top: 10px;
  left: 10px;
  background: rgba(0, 0, 0, 0.7);
  color: #fff;
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
  z-index: 10;
  backdrop-filter: blur(4px);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.video-label .el-icon {
  font-size: 14px;
  color: #4a9eff;
}

.paused-overlay,
.disconnected-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.paused-overlay span,
.disconnected-overlay span {
  margin-top: 10px;
}

.detection-count {
  position: absolute;
  top: 10px;
  right: 10px;
}

.video-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 12px;
  background: rgba(0, 0, 0, 0.5);
  font-size: 11px;
  color: #aaa;
}

.detection-info {
  color: #4a9eff;
}

.fullscreen-video {
  position: relative;
  width: 100%;
  height: 60vh;
  background: #000;
}

.fullscreen-video video {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.video-timeline {
  padding: 10px;
  background: rgba(0, 0, 0, 0.8);
}

.timeline-info {
  text-align: center;
  color: #fff;
  margin-top: 5px;
  font-size: 12px;
}

.fullscreen-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  background: rgba(0, 0, 0, 0.8);
}

.detection-toggle {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #fff;
}

.detection-panel {
  padding: 15px;
  background: #1a1a2e;
}

.detection-panel h4 {
  color: #fff;
  margin-bottom: 10px;
}

.empty-tip {
  font-size: 12px;
  color: #666;
  margin-top: 8px;
}
</style>
