<template>
  <div class="full-test">
    <!-- 시작 전 -->
    <div v-if="state === 'intro'" class="intro card">
      <h1>TOEIC Speaking 모의고사</h1>
      <p class="intro-desc">실제 시험과 동일한 구성으로 Part 1~5를 순서대로 진행합니다.</p>
      <div class="test-overview">
        <div class="overview-item" v-for="item in testFlow" :key="item.part">
          <span class="overview-part">Part {{ item.part }}</span>
          <span class="overview-title">{{ item.title }}</span>
          <span class="overview-count">{{ item.count }}문제</span>
        </div>
      </div>
      <p class="intro-total">총 {{ totalQuestions }}문제 | 약 20분 소요</p>
      <button class="btn-primary btn-start" @click="startTest">시험 시작</button>
    </div>

    <!-- 파트 전환 안내 -->
    <div v-else-if="state === 'part-intro'" class="part-intro card">
      <div class="part-intro-badge">Part {{ currentPartData.part }}</div>
      <h2>{{ currentPartData.title }}</h2>
      <p>{{ currentPartData.description }}</p>
      <p class="part-intro-meta">
        준비시간 {{ currentPartData.prepTime }}초 / 응답시간 {{ currentPartData.responseTime }}초
      </p>
      <div class="part-intro-timer">{{ partIntroCountdown }}초 후 시작...</div>
    </div>

    <!-- 문제 진행 -->
    <div v-else-if="state === 'prep' || state === 'response'" class="test-area card">
      <div class="test-progress">
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: overallProgress + '%' }"></div>
        </div>
        <span class="progress-text">{{ currentGlobalIdx + 1 }} / {{ totalQuestions }}</span>
      </div>

      <div class="test-part-label">Part {{ currentPartData.part }} - {{ currentPartData.title }}</div>

      <!-- Part 4: 정보 표 (준비/응답 시간 동안 항상 표시) -->
      <div v-if="currentQ.info && (state === 'prep' || state === 'response')" class="info-table-area">
        <div class="info-table-header">제공된 정보</div>
        <div class="info-table" v-html="formatInfoToTable(currentQ.info)"></div>
      </div>

      <!-- 준비 시간 -->
      <div v-if="state === 'prep'" class="phase-prep">
        <div class="phase-label">준비 시간</div>
        <div class="timer">{{ timeLeft }}초</div>
        <div class="timer-bar">
          <div class="timer-fill" :style="{ width: timerPercent + '%' }"></div>
        </div>
        <div class="question-content">
          <p class="question-text">{{ currentQ.text }}</p>
          <div v-if="currentQ.imageUrl" class="question-image">
            <img :src="currentQ.imageUrl" alt="사진" />
          </div>
          <p v-if="currentQ.hint && !currentQ.imageUrl" class="hint">{{ currentQ.hint }}</p>
        </div>
        <button class="btn-skip" @click="skipPrep">답변 시작 →</button>
      </div>

      <!-- 응답 시간 -->
      <div v-if="state === 'response'" class="phase-response">
        <div class="phase-label recording">녹음 중</div>
        <div class="timer">{{ timeLeft }}초</div>
        <div class="timer-bar">
          <div class="timer-fill response-fill" :style="{ width: timerPercent + '%' }"></div>
        </div>
        <div class="question-content">
          <p class="question-text">{{ currentQ.text }}</p>
          <div v-if="currentQ.imageUrl" class="question-image">
            <img :src="currentQ.imageUrl" alt="사진" />
          </div>
          <div v-if="currentQ.subQuestions" class="sub-questions">
            <p v-for="(sq, i) in currentQ.subQuestions" :key="i" class="sub-q">
              Q{{ i + 1 }}. {{ sq }}
            </p>
          </div>
        </div>
        <div class="recording-indicator">
          <span class="rec-dot"></span> 말하기 시작하세요
        </div>
        <button class="btn-skip btn-skip-response" @click="skipResponse">답변 완료 →</button>
      </div>
    </div>

    <!-- 시험 완료 -->
    <div v-else-if="state === 'done'" class="done card">
      <h1>모의고사 완료!</h1>
      <p class="done-desc">수고하셨습니다. 모든 파트를 완료했습니다.</p>

      <div class="result-summary">
        <div class="result-item" v-for="item in testFlow" :key="item.part">
          <span class="result-part">Part {{ item.part }}</span>
          <span class="result-title">{{ item.title }}</span>
          <span class="result-status">{{ item.count }}문제 완료</span>
        </div>
      </div>

      <div class="done-actions">
        <router-link to="/"><button class="btn-primary">홈으로</button></router-link>
        <button class="btn-secondary" @click="startTest">다시 응시</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { useSpeakingStore } from '../stores/speaking'

const store = useSpeakingStore()

// 시험 흐름 정의: 실제 TOEIC Speaking 구성
// 문제 1-2: Part1, 문제 3-4: Part2, 문제 5-7: Part3, 문제 8-10: Part4, 문제 11: Part5
const testFlow = [
  { part: 1, title: 'Read a Text Aloud', description: '문장 읽기', count: 2 },
  { part: 2, title: 'Describe a Picture', description: '사진 묘사', count: 2 },
  { part: 3, title: 'Respond to Questions', description: '듣고, 질문에 답하기', count: 3 },
  { part: 4, title: 'Respond Using Information', description: '제공된 정보를 사용하여 질문에 답하기', count: 3 },
  { part: 5, title: 'Express an Opinion', description: '의견 제시하기', count: 1 }
]

const totalQuestions = computed(() => testFlow.reduce((sum, p) => sum + p.count, 0))

// 문항별 정확한 시간 (문제번호 1~11)
// 문제1-2: prep 45, resp 45 | 문제3-4: prep 45, resp 30
// 문제5-6: prep 3, resp 15 | 문제7: prep 3, resp 30
// 문제8-9: prep 3, resp 15 | 문제10: prep 3, resp 30
// 문제11: prep 45, resp 60
const questionTimings = [
  { prepTime: 45, responseTime: 45 },  // 문제 1
  { prepTime: 45, responseTime: 45 },  // 문제 2
  { prepTime: 45, responseTime: 30 },  // 문제 3
  { prepTime: 45, responseTime: 30 },  // 문제 4
  { prepTime: 3,  responseTime: 15 },  // 문제 5
  { prepTime: 3,  responseTime: 15 },  // 문제 6
  { prepTime: 3,  responseTime: 30 },  // 문제 7
  { prepTime: 3,  responseTime: 15 },  // 문제 8
  { prepTime: 3,  responseTime: 15 },  // 문제 9
  { prepTime: 3,  responseTime: 30 },  // 문제 10
  { prepTime: 45, responseTime: 60 },  // 문제 11
]

// 시험 문제 목록 (플랫화)
const testQuestions = ref([])

function buildTestQuestions() {
  const questions = []
  let globalIdx = 0

  // Part 1: 문제 1-2
  const part1 = store.parts.find(p => p.id === 1)
  if (part1) {
    for (let i = 0; i < 2 && i < part1.questions.length; i++) {
      questions.push({ ...part1.questions[i], part: 1, ...questionTimings[globalIdx] })
      globalIdx++
    }
  }

  // Part 2: 문제 3-4
  const part2 = store.parts.find(p => p.id === 2)
  if (part2) {
    for (let i = 0; i < 2 && i < part2.questions.length; i++) {
      questions.push({ ...part2.questions[i], part: 2, ...questionTimings[globalIdx] })
      globalIdx++
    }
  }

  // Part 3: 문제 5-7 (첫 번째 세트의 서브 질문 3개)
  const part3 = store.parts.find(p => p.id === 3)
  if (part3 && part3.questions.length > 0) {
    const set = part3.questions[0]
    if (set.subQuestions) {
      for (let i = 0; i < set.subQuestions.length && i < 3; i++) {
        const sq = set.subQuestions[i]
        questions.push({
          id: set.id + '-q' + i,
          text: typeof sq === 'string' ? sq : sq.text,
          part: 3,
          ...questionTimings[globalIdx]
        })
        globalIdx++
      }
    }
  }

  // Part 4: 문제 8-10 (info 기반 서브 질문 3개)
  const part4 = store.parts.find(p => p.id === 4)
  if (part4 && part4.questions.length > 0) {
    const q = part4.questions[0]
    if (q.subQuestions) {
      for (let i = 0; i < q.subQuestions.length && i < 3; i++) {
        questions.push({
          id: q.id + '-sub' + i,
          text: q.subQuestions[i],
          info: q.info,
          part: 4,
          ...questionTimings[globalIdx]
        })
        globalIdx++
      }
    }
  }

  // Part 5: 문제 11
  const part5 = store.parts.find(p => p.id === 5)
  if (part5 && part5.questions.length > 0) {
    questions.push({ ...part5.questions[0], part: 5, ...questionTimings[globalIdx] })
    globalIdx++
  }

  testQuestions.value = questions
}

// 상태
const state = ref('intro') // intro, part-intro, prep, response, done
const currentIdx = ref(0)
const timeLeft = ref(0)
const partIntroCountdown = ref(3)

let timer = null
let mediaRecorder = null
let audioChunks = []

const currentQ = computed(() => testQuestions.value[currentIdx.value] || {})
const currentPartData = computed(() => testFlow.find(f => f.part === currentQ.value?.part) || testFlow[0])
const currentGlobalIdx = computed(() => currentIdx.value)
const overallProgress = computed(() => ((currentIdx.value) / testQuestions.value.length) * 100)

const timerPercent = computed(() => {
  const q = currentQ.value
  if (!q) return 0
  const total = state.value === 'prep' ? q.prepTime : q.responseTime
  return (timeLeft.value / total) * 100
})

function startTest() {
  buildTestQuestions()
  currentIdx.value = 0
  showPartIntro()
}

function showPartIntro() {
  state.value = 'part-intro'
  partIntroCountdown.value = 3

  // 같은 파트의 연속 문제면 파트 인트로 건너뛰기
  if (currentIdx.value > 0) {
    const prev = testQuestions.value[currentIdx.value - 1]
    const curr = testQuestions.value[currentIdx.value]
    if (prev && curr && prev.part === curr.part) {
      startPrep()
      return
    }
  }

  timer = setInterval(() => {
    partIntroCountdown.value--
    if (partIntroCountdown.value <= 0) {
      clearInterval(timer)
      startPrep()
    }
  }, 1000)
}

function startPrep() {
  state.value = 'prep'
  timeLeft.value = currentQ.value.prepTime
  timer = setInterval(() => {
    timeLeft.value--
    if (timeLeft.value <= 0) {
      clearInterval(timer)
      startResponse()
    }
  }, 1000)
}

function skipPrep() {
  clearInterval(timer)
  startResponse()
}

function skipResponse() {
  clearInterval(timer)
  finishQuestion()
}

function formatInfoToTable(info) {
  if (!info) return ''
  const lines = info.split('\n').map(l => l.trim()).filter(l => l)
  const headerLines = []
  const scheduleLines = []
  for (const line of lines) {
    if (/\d{1,2}:\d{2}/.test(line) && /[-–~]/.test(line)) {
      scheduleLines.push(line)
    } else {
      headerLines.push(line)
    }
  }
  let html = ''
  if (headerLines.length > 0) {
    html += '<div class="info-header-section">'
    for (const h of headerLines) {
      const colonIdx = h.indexOf(':')
      if (colonIdx > 0 && colonIdx < 30) {
        const key = h.substring(0, colonIdx).trim()
        const val = h.substring(colonIdx + 1).trim()
        html += `<div class="info-meta"><span class="info-key">${key}</span><span class="info-val">${val}</span></div>`
      } else {
        html += `<div class="info-title">${h}</div>`
      }
    }
    html += '</div>'
  }
  if (scheduleLines.length > 0) {
    html += '<table class="schedule-table"><thead><tr><th>시간</th><th>내용</th></tr></thead><tbody>'
    for (const line of scheduleLines) {
      const match = line.match(/^(\d{1,2}:\d{2}[\s]*(?:AM|PM)?[\s]*[-–~][\s]*\d{1,2}:\d{2}[\s]*(?:AM|PM)?)\s+(.+)$/i)
      if (match) {
        html += `<tr><td class="time-cell">${match[1].trim()}</td><td>${match[2].trim()}</td></tr>`
      } else {
        const simpleMatch = line.match(/^([\d:]+[\s]*(?:AM|PM)?[\s]*[-–~]?[\s]*[\d:]*[\s]*(?:AM|PM)?)\s{2,}(.+)$/i)
        if (simpleMatch) {
          html += `<tr><td class="time-cell">${simpleMatch[1].trim()}</td><td>${simpleMatch[2].trim()}</td></tr>`
        } else {
          html += `<tr><td colspan="2">${line}</td></tr>`
        }
      }
    }
    html += '</tbody></table>'
  }
  if (scheduleLines.length === 0 && headerLines.length === 0) {
    html = `<pre class="info-raw">${info}</pre>`
  }
  return html
}

async function startResponse() {
  state.value = 'response'
  timeLeft.value = currentQ.value.responseTime
  audioChunks = []

  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    mediaRecorder = new MediaRecorder(stream)
    mediaRecorder.ondataavailable = (e) => audioChunks.push(e.data)
    mediaRecorder.onstop = async () => {
      const blob = new Blob(audioChunks, { type: 'audio/webm' })
      stream.getTracks().forEach(t => t.stop())

      // 서버에 오디오 업로드
      try {
        const formData = new FormData()
        formData.append('file', blob, 'recording.webm')
        formData.append('userId', store.currentUser?.userId || 1)
        formData.append('partNumber', currentQ.value.part)
        formData.append('questionIdx', currentIdx.value)
        await fetch('/api/files/audio', { method: 'POST', body: formData })
      } catch (err) {
        console.warn('오디오 업로드 실패:', err)
      }
    }
    mediaRecorder.start()
  } catch (err) {
    console.warn('마이크 접근 불가:', err)
  }

  timer = setInterval(() => {
    timeLeft.value--
    if (timeLeft.value <= 0) {
      clearInterval(timer)
      finishQuestion()
    }
  }, 1000)
}

function finishQuestion() {
  if (mediaRecorder && mediaRecorder.state === 'recording') {
    mediaRecorder.stop()
  }

  store.addRecord({
    partId: currentQ.value.part,
    questionIdx: currentIdx.value,
    partTitle: currentPartData.value.title
  })

  // user_responses에 모의고사 응답 저장
  store.saveUserResponse({
    questionId: currentQ.value.questionId || null,
    practiceMode: 'MOCK',
    audioFilePath: null,
    sttText: null
  })

  // 다음 문제 또는 종료
  if (currentIdx.value < testQuestions.value.length - 1) {
    currentIdx.value++
    showPartIntro()
  } else {
    state.value = 'done'
  }
}

onUnmounted(() => {
  clearInterval(timer)
  if (mediaRecorder && mediaRecorder.state === 'recording') {
    mediaRecorder.stop()
  }
})
</script>

<style scoped>
.full-test {
  max-width: 800px;
  margin: 0 auto;
}

/* 인트로 */
.intro {
  text-align: center;
  padding: 48px 32px;
}

.intro h1 {
  font-size: 1.8rem;
  margin-bottom: 12px;
}

.intro-desc {
  color: #666;
  margin-bottom: 32px;
  font-size: 1.05rem;
}

.test-overview {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-width: 450px;
  margin: 0 auto 24px;
  text-align: left;
}

.overview-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.overview-part {
  background: #4A90D9;
  color: #fff;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 0.8rem;
  font-weight: 600;
  white-space: nowrap;
}

.overview-title {
  flex: 1;
  font-weight: 500;
}

.overview-count {
  color: #999;
  font-size: 0.85rem;
}

.intro-total {
  color: #888;
  margin-bottom: 24px;
}

.btn-start {
  padding: 14px 48px;
  font-size: 1.1rem;
  font-weight: 600;
}

/* 파트 인트로 */
.part-intro {
  text-align: center;
  padding: 60px 32px;
}

.part-intro-badge {
  display: inline-block;
  background: #4A90D9;
  color: #fff;
  padding: 6px 20px;
  border-radius: 20px;
  font-size: 1rem;
  font-weight: 600;
  margin-bottom: 16px;
}

.part-intro h2 {
  font-size: 1.5rem;
  margin-bottom: 8px;
}

.part-intro p {
  color: #666;
}

.part-intro-meta {
  color: #999 !important;
  font-size: 0.9rem;
  margin-top: 8px;
}

.part-intro-timer {
  margin-top: 32px;
  font-size: 1.5rem;
  font-weight: 700;
  color: #4A90D9;
}

/* 진행 중 */
.test-area {
  position: relative;
}

.test-progress {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.progress-bar {
  flex: 1;
  height: 6px;
  background: #e8ecf1;
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #4A90D9;
  border-radius: 3px;
  transition: width 0.3s;
}

.progress-text {
  font-size: 0.85rem;
  color: #888;
  white-space: nowrap;
}

.test-part-label {
  font-size: 0.9rem;
  font-weight: 600;
  color: #4A90D9;
  margin-bottom: 12px;
}

.phase-label {
  font-size: 1rem;
  font-weight: 600;
  color: #4A90D9;
  margin-bottom: 8px;
}

.phase-label.recording {
  color: #e74c3c;
}

.timer {
  font-size: 3rem;
  font-weight: 700;
  text-align: center;
  margin: 8px 0;
}

.timer-bar {
  height: 8px;
  background: #e8ecf1;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 24px;
}

.timer-fill {
  height: 100%;
  background: #4A90D9;
  border-radius: 4px;
  transition: width 1s linear;
}

.timer-fill.response-fill {
  background: #e74c3c;
}

.question-content {
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
}

.question-text {
  font-size: 1.1rem;
  line-height: 1.8;
}

.question-image {
  margin: 16px 0;
  text-align: center;
}

.question-image img {
  max-width: 100%;
  max-height: 400px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.12);
}

.info-table-area {
  margin-bottom: 16px;
  background: #fff;
  border: 2px solid #4A90D9;
  border-radius: 10px;
  overflow: hidden;
}

.info-table-header {
  background: #4A90D9;
  color: #fff;
  padding: 8px 16px;
  font-weight: 600;
  font-size: 0.9rem;
}

.info-table {
  padding: 16px;
}

:deep(.info-title) { font-size: 1.1rem; font-weight: 700; margin-bottom: 6px; }
:deep(.info-header-section) { margin-bottom: 12px; }
:deep(.info-meta) { display: flex; gap: 8px; padding: 3px 0; font-size: 0.95rem; }
:deep(.info-key) { font-weight: 600; color: #4A90D9; min-width: 80px; }
:deep(.info-key::after) { content: ':'; }
:deep(.info-val) { color: #333; }
:deep(.schedule-table) { width: 100%; border-collapse: collapse; font-size: 0.95rem; }
:deep(.schedule-table thead) { background: #f0f4fa; }
:deep(.schedule-table th) { padding: 8px 12px; text-align: left; font-weight: 600; color: #4A90D9; border-bottom: 2px solid #d0d8e8; }
:deep(.schedule-table td) { padding: 8px 12px; border-bottom: 1px solid #eee; }
:deep(.schedule-table tr:last-child td) { border-bottom: none; }
:deep(.schedule-table tr:hover) { background: #f8fafd; }
:deep(.time-cell) { white-space: nowrap; font-weight: 500; color: #555; min-width: 140px; }
:deep(.info-raw) { white-space: pre-wrap; font-size: 0.95rem; margin: 0; }

.hint {
  margin-top: 12px;
  color: #888;
  font-size: 0.9rem;
}

.sub-questions {
  margin-top: 12px;
}

.sub-q {
  padding: 8px 0;
  border-bottom: 1px solid #eee;
  font-size: 1rem;
}

.recording-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 20px;
  font-size: 1.1rem;
  color: #e74c3c;
}

.rec-dot {
  width: 12px;
  height: 12px;
  background: #e74c3c;
  border-radius: 50%;
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

/* 완료 */
.done {
  text-align: center;
  padding: 48px 32px;
}

.done h1 {
  color: #27ae60;
  margin-bottom: 8px;
}

.done-desc {
  color: #666;
  margin-bottom: 32px;
}

.result-summary {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: 450px;
  margin: 0 auto 32px;
  text-align: left;
}

.result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #f0faf0;
  border-radius: 8px;
}

.result-part {
  background: #27ae60;
  color: #fff;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 0.8rem;
  font-weight: 600;
}

.result-title {
  flex: 1;
  font-weight: 500;
}

.result-status {
  color: #27ae60;
  font-size: 0.85rem;
  font-weight: 600;
}

.done-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.btn-skip {
  display: block;
  margin: 16px auto 0;
  padding: 10px 28px;
  background: #4A90D9;
  color: #fff;
  font-weight: 600;
  font-size: 0.95rem;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

.btn-skip-response {
  background: #e74c3c;
}

@media (max-width: 600px) {
  .timer {
    font-size: 2.5rem;
  }
  .intro {
    padding: 32px 16px;
  }
}
</style>
