<template>
  <div class="part-practice" v-if="currentPart">
    <div class="practice-header">
      <router-link to="/practice" class="back-link">&larr; 파트 목록</router-link>
      <h1>Part {{ currentPart.id }} - {{ currentPart.title }}</h1>
    </div>

    <!-- 문제 로딩 중 -->
    <div v-if="loadingQuestion" class="question-select card">
      <div class="loading-state">
        {{ generating ? 'AI가 새 문제를 생성하고 있습니다...' : '문제를 불러오는 중...' }}
      </div>
    </div>

    <!-- 연습 진행 중 -->
    <div v-else-if="started" class="practice-area card">
      <!-- 세트 진행 표시 (prep/response에서만, free-answer는 FreeAnswerPhase 내부에서 표시) -->
      <div v-if="isSetPart && currentSetQuestions.length > 1 && (phase === 'prep' || phase === 'response')" class="set-progress">
        Q{{ subQuestionIdx + 1 }} / {{ currentSetQuestions.length }}
      </div>

      <!-- Part 4: 정보 표 -->
      <InfoTable
        v-if="(currentQuestion.hasSchedule || currentQuestion.info) && (phase === 'prep' || phase === 'response')"
        :question="currentQuestion"
      />

      <!-- 준비 시간 -->
      <div v-if="phase === 'prep'" class="phase-prep">
        <div class="phase-label">준비 시간</div>
        <div class="timer">{{ timeLeft }}초</div>
        <div class="timer-bar">
          <div class="timer-fill" :style="{ width: timerPercent + '%' }"></div>
        </div>
        <div class="question-text">
          <p>{{ currentQuestion.text }}</p>
          <div v-if="currentQuestion.imageUrl" class="question-image">
            <img :src="currentQuestion.imageUrl" alt="사진 묘사 이미지" />
          </div>
          <p v-if="currentQuestion.hint && !currentQuestion.imageUrl" class="hint">&#x1f4a1; {{ currentQuestion.hint }}</p>
        </div>
        <button class="btn-skip" @click="skipPrep">답변 시작 &rarr;</button>
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
          <div v-if="currentQuestion.imageUrl" class="question-image">
            <img :src="currentQuestion.imageUrl" alt="사진 묘사 이미지" />
          </div>
        </div>
        <div class="recording-indicator">
          <span class="rec-dot"></span> 말하기 시작하세요
        </div>
        <button class="btn-skip btn-skip-response" @click="skipResponse">답변 완료 &rarr;</button>
      </div>

      <!-- 자유/한글 모드 -->
      <FreeAnswerPhase
        v-if="phase === 'free-answer'"
        :practiceMode="practiceMode"
        :currentQuestion="currentQuestion"
        :isSetPart="isSetPart"
        :sttText="recorder.sttText.value"
        :setQuestionsCount="currentSetQuestions.length"
        :subQuestionIdx="subQuestionIdx"
        :partId="currentPart.id"
        :freeRecording="freeRecording"
        :freeRecordTime="freeRecordTime"
        :audioUrl="recorder.audioUrl.value"
        :freeTextAnswer="freeTextAnswer"
        :koreanAnswer="koreanAnswer"
        :translationResult="ai.translationResult.value"
        :translationData="ai.translationData.value"
        :translating="ai.translating.value"
        @toggleFreeRecording="toggleFreeRecording"
        @finishFreeAnswer="finishFreeAnswer"
        @retryRecording="retryRecording"
        @submitFreeTextAnswer="submitFreeTextAnswer"
        @submitKoreanAnswer="handleSubmitKoreanAnswer"
        @nextFreeQuestion="nextFreeQuestion"
        @reset="reset"
        @update:freeTextAnswer="freeTextAnswer = $event"
        @update:koreanAnswer="koreanAnswer = $event"
      />

      <!-- 완료 -->
      <DonePhase
        v-if="phase === 'done'"
        :partId="currentPart.id"
        :questionIdx="currentQuestionIdx"
        :isSetPart="isSetPart"
        :subAnswers="subAnswers"
        :audioUrl="recorder.audioUrl.value"
        :savedAudioUrl="recorder.savedAudioUrl.value"
        :pronunciationResult="pron.pronunciationResult.value"
        :pronunciationLoading="pron.pronunciationLoading.value"
        :pronScoreClass="pron.pronScoreClass.value"
        :isSpeaking="pron.isSpeaking.value"
        :aiLoading="ai.aiLoading.value"
        :aiMode="ai.aiMode.value"
        :aiResult="ai.aiResult.value"
        :feedbackData="ai.feedbackData.value"
        :showFeedbackArea="ai.showFeedbackArea.value"
        :sttProcessing="recorder.sttProcessing.value"
        :sttText="recorder.sttText.value"
        :userAnswer="ai.userAnswer.value"
        :hasNext="hasNextSubOrQuestion"
        :nextLabel="nextButtonLabel"
        @speakWord="pron.speakWord"
        @speakFullText="pron.speakFullText(currentQuestion.text)"
        @stopSpeaking="pron.stopSpeaking"
        @requestFeedback="handleRequestFeedback"
        @getFeedback="handleGetFeedback"
        @update:userAnswer="ai.userAnswer.value = $event"
        @retry="startQuestion(currentQuestionIdx)"
        @goNext="goNextSubOrQuestion"
        @reset="reset"
      />

      <button v-if="phase !== 'done'" class="btn-danger stop-btn" @click="stopPractice">중지</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useSpeakingStore } from '../stores/speaking'
import { useRecorder } from '../composables/useRecorder'
import { usePronunciation } from '../composables/usePronunciation'
import { useAiFeedback } from '../composables/useAiFeedback'
import InfoTable from '../components/practice/InfoTable.vue'
import FreeAnswerPhase from '../components/practice/FreeAnswerPhase.vue'
import DonePhase from '../components/practice/DonePhase.vue'

const props = defineProps({ part: String })
const route = useRoute()
const store = useSpeakingStore()

// 연습 모드
const practiceMode = computed(() => route.query.mode || 'real')
const practiceModeCode = computed(() => {
  const map = { real: 'REAL', free: 'FREE', korean: 'KOREAN' }
  return map[practiceMode.value] || 'REAL'
})

const currentPart = computed(() => store.parts.find(p => p.id === Number(props.part)))
const isSetPart = computed(() => [3, 4].includes(currentPart.value?.id))

// 상태
const started = ref(false)
const phase = ref('prep')
const currentQuestionIdx = ref(0)
const subQuestionIdx = ref(0)
const timeLeft = ref(0)
const generating = ref(false)
const subAnswers = ref([])
const loadingQuestion = ref(false)
const serverQuestions = ref([])

// 자유/한글 모드
const freeTextAnswer = ref('')
const koreanAnswer = ref('')
const freeRecording = ref(false)
const freeRecordTime = ref(0)
let freeStream = null
let freeTimer = null

// Composables
const recorder = useRecorder(store, currentPart, currentQuestionIdx)
const pron = usePronunciation()

let timer = null

// 문제 목록
const allQuestions = computed(() => {
  if (serverQuestions.value.length > 0) return serverQuestions.value
  return currentPart.value?.questions || []
})

// 현재 세트의 서브 질문 목록
const currentSetQuestions = computed(() => {
  const q = allQuestions.value[currentQuestionIdx.value]
  if (!q) return []
  if (q.subQuestions && Array.isArray(q.subQuestions)) {
    return q.subQuestions.map(sq => {
      if (typeof sq === 'string') {
        return { text: sq, prepTime: q.prepTime || currentPart.value.prepTime, responseTime: q.responseTime || currentPart.value.responseTime }
      }
      return sq
    })
  }
  return []
})

// 현재 표시할 질문
const currentQuestion = computed(() => {
  const q = allQuestions.value[currentQuestionIdx.value]
  if (!q) return {}
  if (isSetPart.value && currentSetQuestions.value.length > 0) {
    const sub = currentSetQuestions.value[subQuestionIdx.value]
    return {
      text: sub.text,
      questionId: q.questionId || null,
      info: q.info || null,
      infoTitle: q.infoTitle || null,
      infoDetails: q.infoDetails || null,
      infoSchedule: q.infoSchedule || null,
      hasSchedule: !!(q.infoSchedule && q.infoSchedule.length > 0),
      hint: q.hint || null,
      imageUrl: q.imageUrl || null,
      prepTime: sub.prepTime,
      responseTime: sub.responseTime
    }
  }
  return q
})

// AI composable (currentQuestion 선언 이후에 초기화)
const ai = useAiFeedback(store, currentPart, computed(() => currentQuestion.value))

const timerPercent = computed(() => {
  if (!currentPart.value) return 0
  const q = currentQuestion.value
  const total = phase.value === 'prep'
    ? (q?.prepTime || currentPart.value.prepTime)
    : (q?.responseTime || currentPart.value.responseTime)
  return (timeLeft.value / total) * 100
})

// 서버 응답 데이터를 문제 객체로 변환
function parseServerQuestion(data) {
  const partId = currentPart.value.id

  if ((partId === 3 || partId === 4) && data.subQuestions) {
    const subs = (data.subQuestions || [])
    // subQuestion이 문자열이면 객체로 변환, 객체면 그대로 사용
    const parseSub = (sq, i, arr) => {
      if (typeof sq === 'object' && sq.text) {
        return { text: sq.text, prepTime: 3, responseTime: sq.responseTime || (i === arr.length - 1 ? 30 : 15) }
      }
      return { text: sq, prepTime: 3, responseTime: i === arr.length - 1 ? 30 : 15 }
    }
    // subQuestions에 3개가 있으면 그대로, 부족하면 text를 Q1으로 추가
    let allSubs
    if (subs.length >= 3) {
      allSubs = subs.map(parseSub)
    } else if (data.text && subs.length > 0) {
      allSubs = [
        { text: data.text, prepTime: 3, responseTime: 15 },
        ...subs.map(parseSub)
      ]
    } else {
      allSubs = subs.map(parseSub)
    }
    // 정확히 3개로 제한
    allSubs = allSubs.slice(0, 3)
    const result = {
      id: 'srv-' + data.questionId,
      questionId: data.questionId,
      setTitle: data.source === 'db' ? 'DB 문제' : 'AI 생성',
      subQuestions: allSubs
    }
    // Part 4: 정보 활용 추가 필드
    if (partId === 4) {
      result.info = data.info || null
      result.infoTitle = data.infoTitle || null
      result.infoDetails = data.infoDetails ? data.infoDetails.replace(/\\n/g, '\n') : null
      result.infoSchedule = data.infoSchedule || null
    }
    return result
  } else {
    return {
      id: 'srv-' + data.questionId,
      questionId: data.questionId,
      text: data.text,
      hint: data.hint || null,
      info: data.info || null,
      imageUrl: data.imageUrl || null,
      prepTime: data.prepTime,
      responseTime: data.responseTime
    }
  }
}

// 기본 내장 문제로 폴백 + DB 저장
function startWithFallback() {
  const fallbackQuestions = currentPart.value?.questions
  if (fallbackQuestions && fallbackQuestions.length > 0) {
    const randomIdx = Math.floor(Math.random() * fallbackQuestions.length)
    const picked = fallbackQuestions[randomIdx]
    serverQuestions.value = [picked]
    loadingQuestion.value = false
    startQuestion(0)

    // 백그라운드에서 DB에 저장 (중복 방지는 백엔드에서 처리)
    saveQuestionToDb(currentPart.value.id, picked)
    return true
  }
  return false
}

// 문제를 DB에 저장 (비동기, 실패해도 무시)
async function saveQuestionToDb(partNumber, questionData) {
  try {
    const typeMap = { 1: '지문 읽기', 2: '사진 묘사', 3: '질문 응답', 4: '정보 활용', 5: '의견 제시' }
    const content = { questionType: typeMap[partNumber] || '기타' }

    if (questionData.text) content.text = questionData.text
    if (questionData.hint) content.hint = questionData.hint

    // 세트형: subQuestions를 객체 배열로 저장 (표준 형식)
    if (questionData.subQuestions) {
      content.subQuestions = questionData.subQuestions.map(sq => {
        if (typeof sq === 'object' && sq.text) {
          return { text: sq.text, responseTime: sq.responseTime || 15 }
        }
        return { text: sq, responseTime: 15 }
      })
    }
    if (questionData.infoTitle) content.infoTitle = questionData.infoTitle
    if (questionData.infoDetails) content.infoDetails = questionData.infoDetails
    if (questionData.infoSchedule) content.infoSchedule = questionData.infoSchedule

    const res = await fetch('/api/gemini/save-question', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ partNumber, content })
    })
    if (res.ok) {
      const data = await res.json()
      console.log('문제 DB 저장:', data.questionId, data.source)
    }
  } catch (err) {
    console.warn('문제 DB 저장 실패 (무시):', err)
  }
}

// 서버에서 랜덤 문제 가져오기 → 바로 연습 시작
async function fetchAndStart() {
  loadingQuestion.value = true
  started.value = false
  generating.value = false
  try {
    const res = await fetch('/api/gemini/get-question', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ partNumber: currentPart.value.id })
    })
    if (!res.ok) throw new Error(`서버 오류: ${res.status}`)
    const data = await res.json()
    if (data.error) throw new Error(data.error)

    const parsed = parseServerQuestion(data)
    // 세트형인데 subQuestions가 비어있으면 실패
    if (isSetPart.value && (!parsed.subQuestions || parsed.subQuestions.length === 0)) {
      throw new Error('세트 문제 데이터 부족')
    }
    // 비세트형인데 text가 비어있으면 실패
    if (!isSetPart.value && !parsed.text) {
      throw new Error('문제 텍스트 없음')
    }

    serverQuestions.value = [parsed]
    loadingQuestion.value = false
    startQuestion(0)
  } catch (err) {
    console.warn('서버 문제 조회 실패:', err.message)
    // 기본 문제로 폴백
    if (!startWithFallback()) {
      loadingQuestion.value = false
      alert('문제를 가져올 수 없습니다. 파트 목록으로 돌아갑니다.')
    }
  }
}

onMounted(() => { fetchAndStart() })

// 문제 시작
function startQuestion(idx) {
  currentQuestionIdx.value = idx
  subQuestionIdx.value = 0
  subAnswers.value = []
  started.value = true
  freeTextAnswer.value = ''
  koreanAnswer.value = ''
  recorder.resetRecording()
  pron.resetPronunciation()
  ai.resetAi()

  if (practiceMode.value === 'real') {
    startPrep()
  } else {
    phase.value = 'free-answer'
  }
}

// 타이머 관련
function startPrep() {
  phase.value = 'prep'
  const q = currentQuestion.value
  timeLeft.value = q?.prepTime || currentPart.value.prepTime
  timer = setInterval(() => {
    timeLeft.value--
    if (timeLeft.value <= 0) { clearInterval(timer); startResponse() }
  }, 1000)
}

function skipPrep() { clearInterval(timer); startResponse() }
function skipResponse() { clearInterval(timer); finishResponse() }

async function startResponse() {
  phase.value = 'response'
  const q = currentQuestion.value
  timeLeft.value = q?.responseTime || currentPart.value.responseTime

  await recorder.startRecording()

  timer = setInterval(() => {
    timeLeft.value--
    if (timeLeft.value <= 0) { clearInterval(timer); finishResponse() }
  }, 1000)
}

function finishResponse() {
  recorder.stopRecording()

  // 세트형: 현재 서브 질문 답변 저장
  if (isSetPart.value) {
    subAnswers.value[subQuestionIdx.value] = {
      audioUrl: recorder.audioUrl.value,
      savedAudioUrl: recorder.savedAudioUrl.value,
      sttText: recorder.sttText.value || '',
      text: currentQuestion.value?.text || ''
    }
  }

  // 세트 내 다음 질문
  if (isSetPart.value && subQuestionIdx.value < currentSetQuestions.value.length - 1) {
    subQuestionIdx.value++
    recorder.resetRecording()
    startPrep()
    return
  }

  // 완료
  phase.value = 'done'

  if (currentPart.value.id === 1) {
    pron.startPronunciationAnalysis(currentQuestion.value.text, recorder.sttText.value, recorder.stopSTT)
  }

  store.addRecord({
    partId: currentPart.value.id,
    questionId: getCurrentQuestionId(),
    partTitle: currentPart.value.title,
    practiceMode: practiceModeCode.value
  })

  store.saveUserResponse({
    questionId: currentQuestion.value?.questionId,
    practiceMode: practiceModeCode.value,
    audioFilePath: recorder.savedAudioUrl.value || null,
    sttText: recorder.sttText.value || null,
    textAnswer: null
  }).then(responseId => {
    if (responseId) ai.setResponseId(responseId)
    // responseId 설정 후 자동 피드백 요청
    autoRequestFeedback()
  })
}

// 자유 모드 녹음
function retryRecording() {
  recorder.resetRecording()
  freeRecordTime.value = 0
}

async function toggleFreeRecording() {
  if (freeRecording.value) {
    recorder.stopRecording()
    freeRecording.value = false
    clearInterval(freeTimer)
  } else {
    recorder.resetRecording()
    freeRecordTime.value = 0
    freeTimer = setInterval(() => { freeRecordTime.value++ }, 1000)
    await recorder.startRecording()
    freeRecording.value = true
  }
}

function finishFreeAnswer() {
  if (isSetPart.value) {
    subAnswers.value[subQuestionIdx.value] = {
      audioUrl: recorder.audioUrl.value,
      savedAudioUrl: recorder.savedAudioUrl.value,
      sttText: recorder.sttText.value || freeTextAnswer.value || '',
      text: currentQuestion.value?.text || ''
    }
    // 세트 문항이 남아있으면 다음 문항으로 이동
    if (subQuestionIdx.value < currentSetQuestions.value.length - 1) {
      subQuestionIdx.value++
      freeTextAnswer.value = ''
      recorder.resetRecording()
      phase.value = 'free-answer'
      return
    }
  }
  phase.value = 'done'
  if (currentPart.value.id === 1) {
    pron.startPronunciationAnalysis(currentQuestion.value.text, recorder.sttText.value, recorder.stopSTT)
  }
  store.addRecord({
    partId: currentPart.value.id,
    questionId: getCurrentQuestionId(),
    partTitle: currentPart.value.title,
    practiceMode: practiceModeCode.value
  })
  // 세트 문항: 모든 sub-answer를 JSON으로 저장
  const allAnswersText = isSetPart.value
    ? JSON.stringify(subAnswers.value.map(a => ({ question: a.text, answer: a.sttText || '' })))
    : (recorder.sttText.value || null)
  store.saveUserResponse({
    questionId: getCurrentQuestionId(),
    practiceMode: practiceModeCode.value,
    audioFilePath: recorder.savedAudioUrl.value || null,
    sttText: allAnswersText,
    textAnswer: null
  }).then(responseId => {
    if (responseId) ai.setResponseId(responseId)
    autoRequestFeedback()
  })
}

function submitFreeTextAnswer() {
  recorder.sttText.value = freeTextAnswer.value

  if (isSetPart.value) {
    subAnswers.value[subQuestionIdx.value] = {
      audioUrl: null, savedAudioUrl: null,
      sttText: freeTextAnswer.value || '',
      text: currentQuestion.value?.text || ''
    }
    if (subQuestionIdx.value < currentSetQuestions.value.length - 1) {
      subQuestionIdx.value++
      freeTextAnswer.value = ''
      recorder.sttText.value = ''
      recorder.audioUrl.value = null
      phase.value = 'free-answer'
      return
    }
  }

  phase.value = 'done'
  store.addRecord({
    partId: currentPart.value.id,
    questionId: getCurrentQuestionId(),
    partTitle: currentPart.value.title,
    practiceMode: practiceModeCode.value
  })
  // 세트 문항: 모든 sub-answer를 JSON으로 저장
  const allAnswersText = isSetPart.value
    ? JSON.stringify(subAnswers.value.map(a => ({ question: a.text, answer: a.sttText || '' })))
    : null
  store.saveUserResponse({
    questionId: getCurrentQuestionId(),
    practiceMode: practiceModeCode.value,
    audioFilePath: null,
    sttText: allAnswersText,
    textAnswer: isSetPart.value ? null : (freeTextAnswer.value || null)
  }).then(responseId => {
    if (responseId) ai.setResponseId(responseId)
    autoRequestFeedback()
  })
}

async function handleSubmitKoreanAnswer() {
  await ai.submitKoreanAnswer(
    koreanAnswer.value,
    currentQuestion.value.text,
    currentPart.value.id,
    practiceModeCode.value,
    store.currentUser?.targetScore || 130,
    currentQuestion.value?.responseTime || currentPart.value.responseTime
  )
  store.saveUserResponse({
    questionId: currentQuestion.value?.questionId,
    practiceMode: practiceModeCode.value,
    audioFilePath: null, sttText: null,
    textAnswer: koreanAnswer.value || null
  }).then(responseId => { if (responseId) ai.setResponseId(responseId) })
}

function nextFreeQuestion() {
  if (isSetPart.value && subQuestionIdx.value < currentSetQuestions.value.length - 1) {
    subAnswers.value[subQuestionIdx.value] = {
      audioUrl: recorder.audioUrl.value,
      savedAudioUrl: recorder.savedAudioUrl.value,
      sttText: recorder.sttText.value || freeTextAnswer.value || '',
      text: currentQuestion.value?.text || ''
    }
    subQuestionIdx.value++
    koreanAnswer.value = ''
    freeTextAnswer.value = ''
    ai.translationResult.value = ''
    recorder.resetRecording()
    phase.value = 'free-answer'
    return
  }
  if (currentQuestionIdx.value < allQuestions.value.length - 1) {
    startQuestion(currentQuestionIdx.value + 1)
  } else {
    reset()
  }
}

// AI 피드백 핸들러
// 현재 문제의 DB question_id 추출
function getCurrentQuestionId() {
  const q = allQuestions.value[currentQuestionIdx.value]
  if (!q) return null
  // 서버에서 가져온 문제: id가 'srv-123' 형태
  const idStr = String(q.id || '')
  if (idStr.startsWith('srv-')) return parseInt(idStr.replace('srv-', ''))
  if (idStr.startsWith('ai-')) return null // 프론트 내장 문제는 DB ID 없음
  return q.questionId || null
}

function getQuestionInfo() {
  const q = allQuestions.value[currentQuestionIdx.value]
  if (!q) return null
  // Part 4: 정보 텍스트
  let info = ''
  if (q.infoTitle) info += q.infoTitle + '\n'
  if (q.infoDetails) info += q.infoDetails + '\n'
  if (q.infoSchedule) {
    info += q.infoSchedule.map(r => `${r.time} | ${r.content} | ${r.speaker}`).join('\n')
  }
  return info || q.info || null
}

function handleRequestFeedback() {
  ai.requestFeedback(
    isSetPart.value, subAnswers.value, recorder.sttText.value,
    currentSetQuestions.value, getQuestionInfo()
  )
}

function handleGetFeedback() {
  ai.getFeedback(
    isSetPart.value, subAnswers.value, recorder.sttText.value,
    currentSetQuestions.value, getQuestionInfo()
  )
}

// 연습 완료 시 자동 피드백 요청 (확인 화면 없이 바로 실행)
function autoRequestFeedback() {
  handleGetFeedback()
}

// 네비게이션
const hasNextSubOrQuestion = computed(() => {
  if (isSetPart.value && subQuestionIdx.value < currentSetQuestions.value.length - 1) return true
  return currentQuestionIdx.value < allQuestions.value.length - 1
})

const nextButtonLabel = computed(() => {
  if (isSetPart.value && subQuestionIdx.value < currentSetQuestions.value.length - 1) {
    return `다음 질문 (Q${subQuestionIdx.value + 2}/${currentSetQuestions.value.length})`
  }
  return '다음 문제'
})

function goNextSubOrQuestion() {
  if (isSetPart.value && subQuestionIdx.value < currentSetQuestions.value.length - 1) {
    subQuestionIdx.value++
    recorder.resetRecording()
    ai.resetAi()
    if (practiceMode.value === 'real') { startPrep() } else { phase.value = 'free-answer' }
    return
  }
  if (currentQuestionIdx.value < allQuestions.value.length - 1) {
    startQuestion(currentQuestionIdx.value + 1)
  }
}

function stopPractice() {
  clearInterval(timer)
  recorder.stopRecording()
  phase.value = 'done'
}

function reset() {
  clearInterval(timer)
  started.value = false
  subQuestionIdx.value = 0
  phase.value = 'prep'
  // 새 랜덤 문제 가져와서 바로 시작
  fetchAndStart()
}

onUnmounted(() => {
  clearInterval(timer)
  recorder.cleanup()
})
</script>

<style src="../styles/practice.css"></style>
