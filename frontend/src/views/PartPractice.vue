<template>
  <div class="part-practice" v-if="currentPart">
    <div class="practice-header">
      <router-link to="/practice" class="back-link">← 파트 목록</router-link>
      <h1>Part {{ currentPart.id }} - {{ currentPart.title }}</h1>
    </div>

    <!-- 문제 선택 -->
    <div v-if="!started" class="question-select card">
      <h2>문제를 선택하세요</h2>
      <div class="question-list">
        <button
          v-for="(q, idx) in currentPart.questions"
          :key="q.id"
          class="btn-secondary question-btn"
          @click="startQuestion(idx)"
        >
          문제 {{ idx + 1 }}
        </button>
      </div>
    </div>

    <!-- 연습 진행 중 -->
    <div v-else class="practice-area card">
      <!-- 준비 시간 -->
      <div v-if="phase === 'prep'" class="phase-prep">
        <div class="phase-label">준비 시간</div>
        <div class="timer">{{ timeLeft }}초</div>
        <div class="timer-bar">
          <div class="timer-fill" :style="{ width: timerPercent + '%' }"></div>
        </div>
        <div class="question-text">
          <p>{{ currentQuestion.text }}</p>
          <div v-if="currentQuestion.info" class="info-box">
            <pre>{{ currentQuestion.info }}</pre>
          </div>
          <p v-if="currentQuestion.hint" class="hint">💡 {{ currentQuestion.hint }}</p>
        </div>
      </div>

      <!-- 응답 시간 -->
      <div v-if="phase === 'response'" class="phase-response">
        <div class="phase-label recording">녹음 중</div>
        <div class="timer">{{ timeLeft }}초</div>
        <div class="timer-bar">
          <div class="timer-fill response-fill" :style="{ width: timerPercent + '%' }"></div>
        </div>
        <div class="question-text">
          <p>{{ currentQuestion.text }}</p>
          <div v-if="currentQuestion.subQuestions" class="sub-questions">
            <p v-for="(sq, i) in currentQuestion.subQuestions" :key="i" class="sub-q">
              Q{{ i + 1 }}. {{ sq }}
            </p>
          </div>
        </div>
        <div class="recording-indicator">
          <span class="rec-dot"></span> 말하기 시작하세요
        </div>
      </div>

      <!-- 완료 -->
      <div v-if="phase === 'done'" class="phase-done">
        <h2>연습 완료!</h2>
        <p>Part {{ currentPart.id }} - 문제 {{ currentQuestionIdx + 1 }}</p>

        <div v-if="audioUrl" class="playback">
          <h3>내 녹음 듣기</h3>
          <audio :src="audioUrl" controls></audio>
        </div>

        <div class="ai-section">
          <button class="btn-ai" @click="getSampleAnswer" :disabled="aiLoading">
            {{ aiLoading && aiMode === 'sample' ? '생성 중...' : 'AI 모범 답안 보기' }}
          </button>
          <button class="btn-ai feedback-btn" @click="showFeedbackInput = !showFeedbackInput" :disabled="aiLoading">
            AI 피드백 받기
          </button>

          <div v-if="showFeedbackInput" class="feedback-input">
            <textarea v-model="userAnswer" placeholder="본인의 답변을 영어로 입력하세요..." rows="4"></textarea>
            <button class="btn-primary" @click="getFeedback" :disabled="aiLoading || !userAnswer.trim()">
              {{ aiLoading && aiMode === 'feedback' ? '분석 중...' : '피드백 요청' }}
            </button>
          </div>

          <div v-if="aiResult" class="ai-result card">
            <div class="ai-result-content" v-html="renderMarkdown(aiResult)"></div>
          </div>
        </div>

        <div class="done-actions">
          <button class="btn-primary" @click="startQuestion(currentQuestionIdx)">다시 하기</button>
          <button class="btn-secondary" @click="nextQuestion" v-if="currentQuestionIdx < currentPart.questions.length - 1">다음 문제</button>
          <button class="btn-secondary" @click="reset">문제 목록으로</button>
        </div>
      </div>

      <button v-if="phase !== 'done'" class="btn-danger stop-btn" @click="stopPractice">중지</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { useSpeakingStore } from '../stores/speaking'

const props = defineProps({ part: String })
const store = useSpeakingStore()

const currentPart = computed(() => store.parts.find(p => p.id === Number(props.part)))

const started = ref(false)
const phase = ref('prep') // prep, response, done
const currentQuestionIdx = ref(0)
const timeLeft = ref(0)
const audioUrl = ref(null)

// AI 관련
const aiLoading = ref(false)
const aiMode = ref('')
const aiResult = ref('')
const userAnswer = ref('')
const showFeedbackInput = ref(false)

let timer = null
let mediaRecorder = null
let audioChunks = []

const currentQuestion = computed(() => currentPart.value?.questions[currentQuestionIdx.value])

const timerPercent = computed(() => {
  if (!currentPart.value) return 0
  const total = phase.value === 'prep' ? currentPart.value.prepTime : currentPart.value.responseTime
  return (timeLeft.value / total) * 100
})

function startQuestion(idx) {
  currentQuestionIdx.value = idx
  started.value = true
  audioUrl.value = null
  aiResult.value = ''
  userAnswer.value = ''
  showFeedbackInput.value = false
  startPrep()
}

async function getSampleAnswer() {
  aiLoading.value = true
  aiMode.value = 'sample'
  aiResult.value = ''
  try {
    const res = await fetch('/api/gemini/sample-answer', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        partId: currentPart.value.id,
        questionText: currentQuestion.value.text,
        info: currentQuestion.value.info || null,
        subQuestions: currentQuestion.value.subQuestions || null
      })
    })
    const data = await res.json()
    aiResult.value = data.answer
  } catch (err) {
    aiResult.value = 'AI 응답을 가져오지 못했습니다.'
  } finally {
    aiLoading.value = false
  }
}

async function getFeedback() {
  aiLoading.value = true
  aiMode.value = 'feedback'
  aiResult.value = ''
  try {
    const res = await fetch('/api/gemini/feedback', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        partId: currentPart.value.id,
        questionText: currentQuestion.value.text,
        userAnswer: userAnswer.value
      })
    })
    const data = await res.json()
    aiResult.value = data.feedback
  } catch (err) {
    aiResult.value = 'AI 피드백을 가져오지 못했습니다.'
  } finally {
    aiLoading.value = false
  }
}

function renderMarkdown(text) {
  return text
    .replace(/## (.+)/g, '<h3>$1</h3>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
}

function startPrep() {
  phase.value = 'prep'
  timeLeft.value = currentPart.value.prepTime
  timer = setInterval(() => {
    timeLeft.value--
    if (timeLeft.value <= 0) {
      clearInterval(timer)
      startResponse()
    }
  }, 1000)
}

async function startResponse() {
  phase.value = 'response'
  timeLeft.value = currentPart.value.responseTime
  audioChunks = []

  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    mediaRecorder = new MediaRecorder(stream)
    mediaRecorder.ondataavailable = (e) => audioChunks.push(e.data)
    mediaRecorder.onstop = () => {
      const blob = new Blob(audioChunks, { type: 'audio/webm' })
      audioUrl.value = URL.createObjectURL(blob)
      stream.getTracks().forEach(t => t.stop())
    }
    mediaRecorder.start()
  } catch (err) {
    console.warn('마이크 접근 불가:', err)
  }

  timer = setInterval(() => {
    timeLeft.value--
    if (timeLeft.value <= 0) {
      clearInterval(timer)
      finishResponse()
    }
  }, 1000)
}

function finishResponse() {
  if (mediaRecorder && mediaRecorder.state === 'recording') {
    mediaRecorder.stop()
  }
  phase.value = 'done'

  store.addRecord({
    partId: currentPart.value.id,
    questionIdx: currentQuestionIdx.value,
    partTitle: currentPart.value.title
  })
}

function stopPractice() {
  clearInterval(timer)
  if (mediaRecorder && mediaRecorder.state === 'recording') {
    mediaRecorder.stop()
  }
  phase.value = 'done'
}

function nextQuestion() {
  if (currentQuestionIdx.value < currentPart.value.questions.length - 1) {
    startQuestion(currentQuestionIdx.value + 1)
  }
}

function reset() {
  clearInterval(timer)
  started.value = false
  phase.value = 'prep'
}

onUnmounted(() => {
  clearInterval(timer)
  if (mediaRecorder && mediaRecorder.state === 'recording') {
    mediaRecorder.stop()
  }
})
</script>

<style scoped>
.back-link {
  color: #4A90D9;
  text-decoration: none;
  font-size: 0.9rem;
}

.practice-header h1 {
  font-size: 1.3rem;
  margin: 8px 0 20px;
}

.question-select h2 {
  margin-bottom: 16px;
}

.question-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.question-btn {
  min-width: 100px;
}

.practice-area {
  position: relative;
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
  color: #333;
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

.question-text {
  font-size: 1.1rem;
  line-height: 1.8;
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
}

.info-box {
  margin-top: 12px;
  padding: 12px;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 6px;
}

.info-box pre {
  white-space: pre-wrap;
  font-size: 0.95rem;
}

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

.phase-done {
  text-align: center;
}

.phase-done h2 {
  color: #27ae60;
  margin-bottom: 8px;
}

.playback {
  margin: 20px 0;
}

.playback h3 {
  margin-bottom: 8px;
  font-size: 1rem;
}

.playback audio {
  width: 100%;
  max-width: 400px;
}

.done-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 20px;
  flex-wrap: wrap;
}

.stop-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  padding: 6px 14px;
  font-size: 0.85rem;
}

.ai-section {
  margin: 24px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.btn-ai {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  padding: 10px 24px;
  font-size: 0.95rem;
  font-weight: 600;
}

.btn-ai.feedback-btn {
  background: linear-gradient(135deg, #f093fb, #f5576c);
}

.btn-ai:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.feedback-input {
  width: 100%;
  max-width: 500px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.feedback-input textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 0.95rem;
  resize: vertical;
  font-family: inherit;
}

.ai-result {
  width: 100%;
  text-align: left;
  margin-top: 8px;
  background: #f8f7ff;
  border: 1px solid #e0ddf5;
}

.ai-result-content {
  font-size: 0.95rem;
  line-height: 1.8;
}

.ai-result-content h3 {
  color: #5b4ea0;
  margin: 16px 0 8px;
  font-size: 1.05rem;
}

.ai-result-content h3:first-child {
  margin-top: 0;
}

@media (max-width: 600px) {
  .timer {
    font-size: 2.5rem;
  }
}
</style>
