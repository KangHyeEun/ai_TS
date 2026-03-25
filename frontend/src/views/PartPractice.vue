<template>
  <div class="part-practice" v-if="currentPart">
    <div class="practice-header">
      <router-link to="/practice" class="back-link">← 파트 목록</router-link>
      <h1>Part {{ currentPart.id }} - {{ currentPart.title }}</h1>
    </div>

    <!-- 문제 로딩 중 -->
    <div v-if="!started && loadingQuestion" class="question-select card">
      <div class="loading-state">문제를 불러오는 중...</div>
    </div>

    <!-- 문제 선택 -->
    <div v-else-if="!started" class="question-select card">
      <div class="question-select-header">
        <h2>{{ isSetPart ? '세트를 선택하세요' : '문제를 선택하세요' }}</h2>
        <button class="btn-generate" @click="generateAiQuestion" :disabled="generating">
          {{ generating ? 'AI 문제 생성 중...' : 'AI 새 문제 생성' }}
        </button>
      </div>
      <div class="question-list">
        <button
          v-for="(q, idx) in allQuestions"
          :key="q.id || idx"
          class="btn-secondary question-btn"
          :class="{ 'ai-generated': q.aiGenerated }"
          @click="startQuestion(idx)"
        >
          {{ q.aiGenerated ? 'AI ' : '' }}{{ isSetPart ? '세트' : '문제' }} {{ idx + 1 }}
          <span v-if="q.setTitle" class="set-label">{{ q.setTitle }}</span>
        </button>
      </div>
    </div>

    <!-- 연습 진행 중 -->
    <div v-else class="practice-area card">
      <!-- 세트 진행 표시 -->
      <div v-if="isSetPart && currentSetQuestions.length > 1" class="set-progress">
        Q{{ subQuestionIdx + 1 }} / {{ currentSetQuestions.length }}
      </div>

      <!-- Part 4: 정보 표 (준비/응답 시간 동안 항상 표시) -->
      <div v-if="currentQuestion.hasSchedule && (phase === 'prep' || phase === 'response')" class="info-table-area">
        <div class="info-table-title">{{ currentQuestion.infoTitle }}</div>
        <div class="info-table-details">{{ currentQuestion.infoDetails }}</div>
        <table class="schedule-table">
          <thead>
            <tr>
              <th class="col-time">시간</th>
              <th class="col-content">내용</th>
              <th class="col-speaker">담당</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, i) in currentQuestion.infoSchedule" :key="i">
              <td class="time-cell">{{ row.time }}</td>
              <td v-html="formatContent(row.content)"></td>
              <td class="speaker-cell">{{ row.speaker }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <!-- 기존 텍스트 info (하위 호환) -->
      <div v-else-if="currentQuestion.info && (phase === 'prep' || phase === 'response')" class="info-table-area">
        <div class="info-table-header">제공된 정보</div>
        <div class="info-table" v-html="formatInfoToTable(currentQuestion.info)"></div>
      </div>

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
          <p v-if="currentQuestion.hint && !currentQuestion.imageUrl" class="hint">💡 {{ currentQuestion.hint }}</p>
        </div>
        <button class="btn-skip" @click="skipPrep">답변 시작 →</button>
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
        <button class="btn-skip btn-skip-response" @click="skipResponse">답변 완료 →</button>
      </div>

      <!-- 자유/한글 모드: 타이머 없이 답변 -->
      <div v-if="phase === 'free-answer'" class="phase-free">
        <div class="free-mode-label" v-if="practiceMode === 'free'">자유 연습 모드</div>
        <div class="free-mode-label korean-label" v-else>한글 연습 모드</div>

        <!-- Part 4 표 유지 -->
        <div v-if="currentQuestion.hasSchedule" class="info-table-area" style="margin-bottom:16px">
          <div class="info-table-title">{{ currentQuestion.infoTitle }}</div>
          <div class="info-table-details">{{ currentQuestion.infoDetails }}</div>
          <table class="schedule-table">
            <thead><tr><th class="col-time">시간</th><th class="col-content">내용</th><th class="col-speaker">담당</th></tr></thead>
            <tbody>
              <tr v-for="(row, i) in currentQuestion.infoSchedule" :key="i">
                <td class="time-cell">{{ row.time }}</td>
                <td v-html="formatContent(row.content)"></td>
                <td class="speaker-cell">{{ row.speaker }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 세트 진행 표시 -->
        <div v-if="isSetPart && currentSetQuestions.length > 1" class="set-progress">
          Q{{ subQuestionIdx + 1 }} / {{ currentSetQuestions.length }}
        </div>

        <div class="question-text" style="margin-bottom:20px">
          <p>{{ currentQuestion.text }}</p>
          <div v-if="currentQuestion.imageUrl" class="question-image">
            <img :src="currentQuestion.imageUrl" alt="사진" />
          </div>
          <p v-if="currentQuestion.hint && !currentQuestion.imageUrl" class="hint">{{ currentQuestion.hint }}</p>
        </div>

        <!-- 자유 모드: 음성/텍스트 선택 -->
        <div v-if="practiceMode === 'free'" class="free-answer-area">
          <!-- Part 1: 음성만 가능 -->
          <div v-if="currentPart.id !== 1" class="answer-type-toggle">
            <button :class="{ active: freeAnswerType === 'voice' }" @click="freeAnswerType = 'voice'">음성 답변</button>
            <button :class="{ active: freeAnswerType === 'text' }" @click="freeAnswerType = 'text'">텍스트 답변</button>
          </div>
          <p v-if="currentPart.id === 1" class="part1-voice-only">Part 1은 음성 녹음으로만 연습합니다. 지문을 소리내어 읽어주세요.</p>

          <div v-if="freeAnswerType === 'voice' || currentPart.id === 1" class="voice-area">
            <button
              class="btn-record-toggle"
              :class="{ recording: freeRecording }"
              @click="toggleFreeRecording"
            >
              <span class="record-icon" :class="{ active: freeRecording }"></span>
              <span class="record-label">{{ freeRecording ? '녹음 중지' : '녹음 시작' }}</span>
            </button>
            <div v-if="freeRecording" class="recording-status">
              <span class="rec-dot"></span>
              <span>녹음 중... {{ freeRecordTime }}초</span>
            </div>
            <div v-if="!freeRecording && audioUrl" class="playback" style="margin-top:16px">
              <h4>녹음 결과</h4>
              <audio :src="audioUrl" controls></audio>
              <button class="btn-primary" style="margin-top:12px" @click="finishFreeAnswer">답변 완료</button>
              <button class="btn-secondary" style="margin-top:8px" @click="retryRecording">다시 녹음</button>
            </div>
          </div>

          <div v-if="freeAnswerType === 'text' && currentPart.id !== 1" class="text-area">
            <textarea v-model="freeTextAnswer" placeholder="영어로 답변을 입력하세요..." rows="5"></textarea>
            <button class="btn-primary" @click="submitFreeTextAnswer" :disabled="!freeTextAnswer.trim()">답변 제출</button>
          </div>
        </div>

        <!-- 한글 모드 -->
        <div v-if="practiceMode === 'korean'" class="korean-answer-area">
          <textarea v-model="koreanAnswer" placeholder="한글로 답변을 입력하세요..." rows="5"></textarea>
          <button class="btn-primary btn-translate" @click="submitKoreanAnswer" :disabled="!koreanAnswer.trim() || translating">
            {{ translating ? '번역 중...' : '답변 제출 (영어 번역)' }}
          </button>

          <div v-if="translationResult" class="translation-result card">
            <div class="ai-result-content" v-html="renderMarkdown(translationResult)"></div>
          </div>

          <div v-if="translationResult" class="done-actions" style="margin-top:16px">
            <button class="btn-primary" @click="nextFreeQuestion">다음 문제</button>
            <button class="btn-secondary" @click="reset">문제 목록으로</button>
          </div>
        </div>
      </div>

      <!-- 완료 -->
      <div v-if="phase === 'done'" class="phase-done">
        <h2>연습 완료!</h2>
        <p>Part {{ currentPart.id }} - 문제 {{ currentQuestionIdx + 1 }}</p>

        <div v-if="audioUrl" class="playback">
          <h3>내 녹음 듣기</h3>
          <audio :src="audioUrl" controls></audio>
          <p v-if="savedAudioUrl" class="save-status">서버에 저장됨</p>
        </div>

        <!-- Part 1: 발음 분석 -->
        <div v-if="currentPart.id === 1" class="pronunciation-section">
          <div v-if="pronunciationLoading" class="pron-loading">발음 분석 중...</div>
          <div v-else-if="pronunciationResult" class="pron-result card">
            <div class="pron-score">
              <span class="pron-score-number" :class="pronScoreClass">{{ pronunciationResult.score }}점</span>
              <span class="pron-score-label">/ 100</span>
            </div>
            <p class="pron-feedback">{{ pronunciationResult.overallFeedback }}</p>

            <!-- 부정확한 단어 목록 -->
            <div v-if="pronunciationResult.incorrectWords && pronunciationResult.incorrectWords.length > 0" class="pron-words">
              <h4>교정이 필요한 단어</h4>
              <div class="pron-word-item" v-for="(w, i) in pronunciationResult.incorrectWords" :key="i">
                <div class="pron-word-main">
                  <span class="pron-original">{{ w.original }}</span>
                  <span class="pron-arrow">→</span>
                  <span class="pron-spoken">{{ w.spoken }}</span>
                  <button class="btn-listen-word" @click="speakWord(w.original)" title="발음 듣기">🔊</button>
                </div>
                <p class="pron-tip">{{ w.tip }}</p>
              </div>
            </div>
            <div v-else class="pron-perfect">모든 단어를 정확하게 발음했습니다!</div>

            <!-- 전체 지문 AI 음성 듣기 -->
            <div class="pron-full-listen">
              <button class="btn-primary btn-full-listen" @click="speakFullText" :disabled="isSpeaking">
                {{ isSpeaking ? '재생 중...' : '전체 지문 AI 음성 듣기' }}
              </button>
              <button v-if="isSpeaking" class="btn-secondary" @click="stopSpeaking">정지</button>
            </div>
          </div>
        </div>

        <div class="ai-section">
          <button class="btn-ai" @click="getSampleAnswer" :disabled="aiLoading">
            {{ aiLoading && aiMode === 'sample' ? '생성 중...' : 'AI 모범 답안 보기' }}
          </button>
          <button v-if="!showFeedbackArea" class="btn-ai feedback-btn" @click="requestFeedback" :disabled="aiLoading || sttProcessing">
            {{ sttProcessing ? '음성 인식 중...' : (aiLoading && aiMode === 'feedback' ? '피드백 분석 중...' : 'AI 피드백 받기') }}
          </button>

          <!-- 피드백 영역 -->
          <div v-if="showFeedbackArea" class="feedback-area">
            <!-- STT 진행 중 -->
            <div v-if="sttProcessing" class="stt-processing">
              <span class="stt-spinner"></span> 음성을 텍스트로 변환 중...
            </div>

            <!-- STT/텍스트 결과 표시 -->
            <div v-else-if="sttText" class="stt-result">
              <h4>{{ audioUrl ? '음성 인식 결과' : '제출한 답변' }}</h4>
              <p class="stt-text">{{ sttText }}</p>
              <button class="btn-primary" @click="getFeedback" :disabled="aiLoading" style="margin-top:10px; width:100%">
                {{ aiLoading && aiMode === 'feedback' ? '피드백 분석 중...' : '이 내용으로 피드백 받기' }}
              </button>
            </div>

            <!-- STT 실패 시 직접 입력 -->
            <div v-else class="stt-failed">
              <p class="stt-fail-msg">음성 인식에 실패했습니다. 답변을 직접 입력해주세요.</p>
              <textarea v-model="userAnswer" placeholder="본인의 답변을 영어로 입력하세요..." rows="4"></textarea>
              <button class="btn-primary" @click="getFeedback" :disabled="aiLoading || !userAnswer.trim()" style="width:100%">
                {{ aiLoading && aiMode === 'feedback' ? '분석 중...' : '피드백 요청' }}
              </button>
            </div>
          </div>

          <div v-if="aiResult" class="ai-result card">
            <div class="ai-result-content" v-html="renderMarkdown(aiResult)"></div>
          </div>
        </div>

        <div class="done-actions">
          <button class="btn-primary" @click="startQuestion(currentQuestionIdx)">다시 하기</button>
          <button class="btn-secondary" @click="nextQuestion" v-if="currentQuestionIdx < allQuestions.length - 1">다음 문제</button>
          <button class="btn-secondary" @click="reset">문제 목록으로</button>
        </div>
      </div>

      <button v-if="phase !== 'done'" class="btn-danger stop-btn" @click="stopPractice">중지</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useSpeakingStore } from '../stores/speaking'

const props = defineProps({ part: String })
const route = useRoute()
const store = useSpeakingStore()

// 연습 모드: real(실전), free(자유), korean(한글)
const practiceMode = computed(() => route.query.mode || 'real')
const practiceModeCode = computed(() => {
  const map = { real: 'REAL', free: 'FREE', korean: 'KOREAN' }
  return map[practiceMode.value] || 'REAL'
})

const currentPart = computed(() => store.parts.find(p => p.id === Number(props.part)))

// Part 3, 4는 세트 단위 진행
const isSetPart = computed(() => [3, 4].includes(currentPart.value?.id))

const started = ref(false)
const phase = ref('prep') // prep, response, done
const currentQuestionIdx = ref(0) // 세트/문제 인덱스
const subQuestionIdx = ref(0)     // 세트 내 서브 질문 인덱스
const timeLeft = ref(0)
const audioUrl = ref(null)
const generating = ref(false)
const savedAudioUrl = ref(null)
const loadingQuestion = ref(false)

// 서버에서 가져온 문제 목록
const serverQuestions = ref([])

// 서버 문제 + 기본 문제 합산
const allQuestions = computed(() => {
  if (serverQuestions.value.length > 0) {
    return serverQuestions.value
  }
  return currentPart.value?.questions || []
})

// 파트 진입 시 서버에서 문제 가져오기 (DB 랜덤 or AI 자동 생성)
async function fetchQuestion() {
  loadingQuestion.value = true
  try {
    const res = await fetch('/api/gemini/get-question', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ partNumber: currentPart.value.id })
    })
    const data = await res.json()
    if (data.error) {
      console.warn('서버 문제 조회 실패, 기본 문제 사용:', data.error)
      return
    }

    const partId = currentPart.value.id

    if (partId === 3 && data.subQuestions) {
      // Part 3: 세트 구조로 변환
      serverQuestions.value = [{
        id: 'srv-' + data.questionId,
        setTitle: data.source === 'db' ? 'DB 문제' : 'AI 생성',
        subQuestions: [
          { text: data.text, prepTime: 3, responseTime: 15 },
          ...(data.subQuestions || []).map((sq, i, arr) => ({
            text: sq,
            prepTime: 3,
            responseTime: i === arr.length - 1 ? 30 : 15
          }))
        ]
      }]
    } else if (partId === 4 && data.subQuestions) {
      // Part 4: 세트 구조로 변환 (3열 표 데이터 포함)
      serverQuestions.value = [{
        id: 'srv-' + data.questionId,
        setTitle: data.source === 'db' ? 'DB 문제' : 'AI 생성',
        info: data.info,
        infoTitle: data.infoTitle || null,
        infoDetails: data.infoDetails ? data.infoDetails.replace(/\\n/g, '\n') : null,
        infoSchedule: data.infoSchedule || null,
        subQuestions: (data.subQuestions || []).map((sq, i, arr) => ({
          text: sq,
          prepTime: 3,
          responseTime: i === arr.length - 1 ? 30 : 15
        }))
      }]
    } else {
      serverQuestions.value = [{
        id: 'srv-' + data.questionId,
        text: data.text,
        hint: data.hint || null,
        info: data.info || null,
        imageUrl: data.imageUrl || null,
        prepTime: data.prepTime,
        responseTime: data.responseTime
      }]
    }
  } catch (err) {
    console.warn('서버 문제 조회 실패, 기본 문제 사용:', err)
  } finally {
    loadingQuestion.value = false
  }
}

onMounted(() => {
  fetchQuestion()
})

// 현재 세트의 서브 질문 목록 (Part 3, 4)
const currentSetQuestions = computed(() => {
  const q = allQuestions.value[currentQuestionIdx.value]
  if (!q) return []
  if (q.subQuestions && Array.isArray(q.subQuestions)) {
    // Part 3: subQuestions가 객체 배열 { text, prepTime, responseTime }
    // Part 4: subQuestions가 문자열 배열
    return q.subQuestions.map(sq => {
      if (typeof sq === 'string') {
        return { text: sq, prepTime: q.prepTime || currentPart.value.prepTime, responseTime: q.responseTime || currentPart.value.responseTime }
      }
      return sq
    })
  }
  return []
})

// 현재 표시할 질문 (세트면 서브 질문, 아니면 메인 질문)
const currentQuestion = computed(() => {
  const q = allQuestions.value[currentQuestionIdx.value]
  if (!q) return {}
  if (isSetPart.value && currentSetQuestions.value.length > 0) {
    const sub = currentSetQuestions.value[subQuestionIdx.value]
    return {
      text: sub.text,
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

// AI 관련
const aiLoading = ref(false)
const aiMode = ref('')
const aiResult = ref('')
const cachedSampleAnswer = ref('')
const cachedFeedback = ref('')
const userAnswer = ref('')
const showFeedbackInput = ref(false)
const showFeedbackArea = ref(false)
const sttProcessing = ref(false)
const lastAudioBlob = ref(null)

// 자유/한글 모드 관련
const freeAnswerType = ref('voice') // voice 또는 text
const freeTextAnswer = ref('')
const koreanAnswer = ref('')
const translationResult = ref('')
const translating = ref(false)

// Part 1: STT + 발음 분석 관련
const sttText = ref('')
const pronunciationResult = ref(null)
const pronunciationLoading = ref(false)
const isSpeaking = ref(false)
const pronScoreClass = computed(() => {
  if (!pronunciationResult.value) return ''
  const s = pronunciationResult.value.score
  if (s >= 90) return 'score-great'
  if (s >= 70) return 'score-good'
  if (s >= 50) return 'score-fair'
  return 'score-poor'
})

let timer = null
let mediaRecorder = null
let audioChunks = []

const timerPercent = computed(() => {
  if (!currentPart.value) return 0
  const q = currentQuestion.value
  const total = phase.value === 'prep'
    ? (q?.prepTime || currentPart.value.prepTime)
    : (q?.responseTime || currentPart.value.responseTime)
  return (timeLeft.value / total) * 100
})

async function generateAiQuestion() {
  generating.value = true
  try {
    const res = await fetch('/api/gemini/generate-question', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ partNumber: currentPart.value.id })
    })
    const data = await res.json()
    if (data.error) {
      alert(data.error)
      return
    }

    const partId = currentPart.value.id
    let newQ

    if (partId === 3 && data.subQuestions) {
      newQ = {
        id: 'ai-' + Date.now(),
        setTitle: 'AI 생성',
        subQuestions: [
          { text: data.text, prepTime: 3, responseTime: 15 },
          ...(data.subQuestions || []).map((sq, i, arr) => ({
            text: sq,
            prepTime: 3,
            responseTime: i === arr.length - 1 ? 30 : 15
          }))
        ],
        aiGenerated: true
      }
    } else if (partId === 4 && data.subQuestions) {
      newQ = {
        id: 'ai-' + Date.now(),
        setTitle: 'AI 생성',
        info: data.info,
        infoTitle: data.infoTitle || null,
        infoDetails: data.infoDetails ? data.infoDetails.replace(/\\n/g, '\n') : null,
        infoSchedule: data.infoSchedule || null,
        subQuestions: (data.subQuestions || []).map((sq, i, arr) => ({
          text: sq,
          prepTime: 3,
          responseTime: i === arr.length - 1 ? 30 : 15
        })),
        aiGenerated: true
      }
    } else {
      newQ = {
        id: 'ai-' + Date.now(),
        text: data.text,
        hint: data.hint || null,
        info: data.info || null,
        imageUrl: data.imageUrl || null,
        prepTime: data.prepTime,
        responseTime: data.responseTime,
        aiGenerated: true
      }
    }

    serverQuestions.value.push(newQ)
  } catch (err) {
    alert('AI 문제 생성에 실패했습니다.')
  } finally {
    generating.value = false
  }
}

function startQuestion(idx) {
  currentQuestionIdx.value = idx
  subQuestionIdx.value = 0
  started.value = true
  audioUrl.value = null
  savedAudioUrl.value = null
  aiResult.value = ''
  userAnswer.value = ''
  freeTextAnswer.value = ''
  koreanAnswer.value = ''
  translationResult.value = ''
  showFeedbackInput.value = false
  showFeedbackArea.value = false
  sttProcessing.value = false
  sttText.value = ''
  lastAudioBlob.value = null
  pronunciationResult.value = null
  cachedSampleAnswer.value = ''
  cachedFeedback.value = ''

  if (practiceMode.value === 'real') {
    startPrep()
  } else {
    // 자유/한글 모드: 타이머 없이 바로 문제 표시
    phase.value = 'free-answer'
  }
}

async function submitKoreanAnswer() {
  if (!koreanAnswer.value.trim()) return
  translating.value = true
  translationResult.value = ''
  try {
    const res = await fetch('/api/gemini/translate-to-english', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        koreanText: koreanAnswer.value,
        questionText: currentQuestion.value.text,
        partId: String(currentPart.value.id)
      })
    })
    const data = await res.json()
    translationResult.value = data.translation
    // user_responses 테이블에 저장 (한글 답변 → textAnswer)
    const q = currentQuestion.value
    store.saveUserResponse({
      questionId: q?.questionId,
      practiceMode: practiceModeCode.value,
      audioFilePath: null,
      sttText: null,
      textAnswer: koreanAnswer.value || null
    })
  } catch (err) {
    translationResult.value = '번역에 실패했습니다.'
  } finally {
    translating.value = false
  }
}

async function submitFreeTextAnswer() {
  // 텍스트 답변을 sttText에 저장 → 피드백 시 이 텍스트를 사용
  sttText.value = freeTextAnswer.value

  phase.value = 'done'
  store.addRecord({
    partId: currentPart.value.id,
    questionIdx: currentQuestionIdx.value,
    partTitle: currentPart.value.title
  })

  // user_responses 테이블에 저장 (텍스트 답변 → textAnswer)
  const q = currentQuestion.value
  store.saveUserResponse({
    questionId: q?.questionId,
    practiceMode: practiceModeCode.value,
    audioFilePath: null,
    sttText: null,
    textAnswer: freeTextAnswer.value || null
  })
}

// 자유 모드 녹음
const freeRecording = ref(false)
const freeRecordTime = ref(0)
let freeStream = null
let freeTimer = null

function retryRecording() {
  audioUrl.value = null
  savedAudioUrl.value = null
  freeRecordTime.value = 0
}

async function toggleFreeRecording() {
  if (freeRecording.value) {
    // 녹음 중지
    if (mediaRecorder && mediaRecorder.state === 'recording') {
      mediaRecorder.stop()
    }
    freeRecording.value = false
    clearInterval(freeTimer)
    stopSTT()
  } else {
    // 녹음 시작
    audioChunks = []
    audioUrl.value = null
    freeRecordTime.value = 0
    freeTimer = setInterval(() => { freeRecordTime.value++ }, 1000)
    try {
      freeStream = await navigator.mediaDevices.getUserMedia({ audio: true })
      mediaRecorder = new MediaRecorder(freeStream)
      mediaRecorder.ondataavailable = (e) => audioChunks.push(e.data)
      mediaRecorder.onstop = async () => {
        const blob = new Blob(audioChunks, { type: 'audio/webm' })
        audioUrl.value = URL.createObjectURL(blob)
        lastAudioBlob.value = blob
        freeStream.getTracks().forEach(t => t.stop())
        // 서버 업로드
        try {
          const formData = new FormData()
          formData.append('file', blob, 'recording.webm')
          formData.append('userId', store.currentUser?.userId || 1)
          formData.append('partNumber', currentPart.value.id)
          formData.append('questionIdx', currentQuestionIdx.value)
          await fetch('/api/files/audio', { method: 'POST', body: formData })
        } catch (err) { console.warn('업로드 실패:', err) }
      }
      mediaRecorder.start()
      freeRecording.value = true
      // 자유 모드: STT 동시 시작 (모든 파트)
      startSTT()
    } catch (err) {
      console.warn('마이크 접근 불가:', err)
    }
  }
}

let recognition = null
let sttRestarting = false

function startSTT() {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!SpeechRecognition) return

  sttText.value = ''
  sttRestarting = true
  recognition = new SpeechRecognition()
  recognition.lang = 'en-US'
  recognition.continuous = true
  recognition.interimResults = true
  recognition.maxAlternatives = 1

  let finalTranscript = ''

  recognition.onresult = (event) => {
    let interim = ''
    for (let i = event.resultIndex; i < event.results.length; i++) {
      if (event.results[i].isFinal) {
        finalTranscript += event.results[i][0].transcript + ' '
      } else {
        interim += event.results[i][0].transcript
      }
    }
    sttText.value = (finalTranscript + interim).trim()
  }

  recognition.onerror = (e) => {
    console.warn('STT 오류:', e.error)
    // 'no-speech' 에러 시 자동 재시작
    if (e.error === 'no-speech' && sttRestarting) {
      try { recognition.start() } catch(err) {}
    }
  }

  // Chrome에서 continuous가 중간에 끊길 수 있으므로 자동 재시작
  recognition.onend = () => {
    if (sttRestarting) {
      try { recognition.start() } catch(e) {}
    }
  }

  recognition.start()
}

function stopSTT() {
  sttRestarting = false
  if (recognition) {
    try { recognition.stop() } catch (e) {}
  }
}

// Part 1: STT → 발음 분석
function startPronunciationAnalysis() {
  stopSTT()
  pronunciationResult.value = null
  pronunciationLoading.value = true

  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!SpeechRecognition) {
    pronunciationLoading.value = false
    pronunciationResult.value = { score: 0, overallFeedback: '이 브라우저는 음성 인식을 지원하지 않습니다. Chrome을 사용해주세요.', incorrectWords: [] }
    return
  }

  // audioUrl에서 blob을 다시 가져와서 STT 실행
  // Web Speech API는 실시간만 지원하므로 녹음된 텍스트를 직접 인식할 수 없음
  // 대안: 녹음 시 동시에 STT 실행
  // 이미 녹음이 끝났으므로 사용자에게 다시 읽어달라고 하지 않고,
  // 간단히 STT를 시뮬레이션: 녹음 중 실시간 STT를 사용하는 방식으로 변경
  // 여기서는 녹음 시 동시에 실행된 sttText를 사용
  if (sttText.value) {
    sendPronunciationAnalysis(sttText.value)
  } else {
    pronunciationLoading.value = false
    pronunciationResult.value = { score: 0, overallFeedback: '음성 인식 결과가 없습니다. Chrome 브라우저에서 다시 시도해주세요.', incorrectWords: [] }
  }
}

async function sendPronunciationAnalysis(recognizedText) {
  try {
    const res = await fetch('/api/gemini/analyze-pronunciation', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        originalText: currentQuestion.value.text,
        sttText: recognizedText
      })
    })
    const data = await res.json()
    pronunciationResult.value = data
  } catch (err) {
    pronunciationResult.value = { score: 0, overallFeedback: '발음 분석에 실패했습니다.', incorrectWords: [] }
  } finally {
    pronunciationLoading.value = false
  }
}

// TTS: 개별 단어 듣기
function speakWord(word) {
  const utterance = new SpeechSynthesisUtterance(word)
  utterance.lang = 'en-US'
  utterance.rate = 0.8
  speechSynthesis.speak(utterance)
}

// TTS: 전체 지문 듣기
function speakFullText() {
  const text = currentQuestion.value.text
  if (!text) return
  speechSynthesis.cancel()
  const utterance = new SpeechSynthesisUtterance(text)
  utterance.lang = 'en-US'
  utterance.rate = 0.9
  utterance.onstart = () => { isSpeaking.value = true }
  utterance.onend = () => { isSpeaking.value = false }
  speechSynthesis.speak(utterance)
}

function stopSpeaking() {
  speechSynthesis.cancel()
  isSpeaking.value = false
}

function finishFreeAnswer() {
  phase.value = 'done'
  if (currentPart.value.id === 1) {
    startPronunciationAnalysis()
  }
  store.addRecord({
    partId: currentPart.value.id,
    questionIdx: currentQuestionIdx.value,
    partTitle: currentPart.value.title
  })

  // user_responses 테이블에 저장 (자유모드 음성)
  const q = currentQuestion.value
  store.saveUserResponse({
    questionId: q?.questionId,
    practiceMode: practiceModeCode.value,
    audioFilePath: savedAudioUrl.value || null,
    sttText: sttText.value || null,
    textAnswer: null
  })
}

function nextFreeQuestion() {
  // 세트 내 다음 질문
  if (isSetPart.value && subQuestionIdx.value < currentSetQuestions.value.length - 1) {
    subQuestionIdx.value++
    koreanAnswer.value = ''
    freeTextAnswer.value = ''
    translationResult.value = ''
    audioUrl.value = null
    phase.value = 'free-answer'
    return
  }
  // 다음 세트/문제
  if (currentQuestionIdx.value < allQuestions.value.length - 1) {
    startQuestion(currentQuestionIdx.value + 1)
  } else {
    reset()
  }
}

async function getSampleAnswer() {
  // 캐시된 결과가 있으면 바로 표시
  if (cachedSampleAnswer.value) {
    aiMode.value = 'sample'
    aiResult.value = cachedSampleAnswer.value
    return
  }

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
        subQuestions: currentQuestion.value.subQuestions || null,
        targetScore: store.currentUser?.targetScore || 130,
        responseTime: currentQuestion.value.responseTime || currentPart.value.responseTime
      })
    })
    const data = await res.json()
    aiResult.value = data.answer
    cachedSampleAnswer.value = data.answer
  } catch (err) {
    aiResult.value = 'AI 응답을 가져오지 못했습니다.'
  } finally {
    aiLoading.value = false
  }
}

// 피드백 버튼 클릭 시: STT 결과가 있으면 바로 피드백 요청, 없으면 입력창 표시
function requestFeedback() {
  // 캐시된 피드백이 있으면 바로 표시
  if (cachedFeedback.value) {
    showFeedbackArea.value = true
    aiMode.value = 'feedback'
    aiResult.value = cachedFeedback.value
    return
  }

  showFeedbackArea.value = true

  // STT/텍스트 결과가 있으면 바로 표시
  if (sttText.value) {
    return
  }
  // 없으면 텍스트 입력 fallback
}

async function getFeedback() {
  // 캐시된 결과가 있으면 바로 표시
  if (cachedFeedback.value) {
    aiMode.value = 'feedback'
    aiResult.value = cachedFeedback.value
    return
  }

  const answer = sttText.value || userAnswer.value
  if (!answer.trim()) return

  aiLoading.value = true
  aiMode.value = 'feedback'
  aiResult.value = ''
  try {
    const questionText = currentQuestion.value?.text || allQuestions.value[currentQuestionIdx.value]?.text || '질문 없음'
    const payload = {
      partId: currentPart.value.id,
      questionText: questionText,
      userAnswer: answer,
      targetScore: store.currentUser?.targetScore || 130,
      responseTime: currentQuestion.value?.responseTime || currentPart.value.responseTime
    }
    const res = await fetch('/api/gemini/feedback', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    })
    const data = await res.json()
    if (data.error) {
      aiResult.value = '피드백 생성 실패: ' + data.error
    } else {
      aiResult.value = data.feedback
      cachedFeedback.value = data.feedback
    }
  } catch (err) {
    console.error('피드백 요청 실패:', err)
    aiResult.value = 'AI 피드백을 가져오지 못했습니다. 서버가 실행 중인지 확인해주세요.'
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

function formatContent(text) {
  if (!text) return ''
  // "Lecture:", "Presentation:", "Group discussion:", "Workshop:", "Lunch" 등을 굵게 표시
  return text.replace(/^((?:Lecture|Presentation|Group discussion|Workshop|Keynote|Panel|Lunch|Break|Welcome|Opening|Closing)[^:]*:?)/i,
    '<strong>$1</strong>')
}

function formatInfoToTable(info) {
  if (!info) return ''
  const lines = info.split('\n').map(l => l.trim()).filter(l => l)

  // 제목/헤더 줄과 시간표 줄 분리
  const headerLines = []
  const scheduleLines = []

  for (const line of lines) {
    // 시간 패턴이 포함된 줄은 시간표로 간주 (예: "9:00", "10:30 AM", "9:00 -")
    if (/\d{1,2}:\d{2}/.test(line) && /[-–~]/.test(line)) {
      scheduleLines.push(line)
    } else {
      headerLines.push(line)
    }
  }

  let html = ''

  // 헤더 정보 (제목, 날짜, 장소 등)
  if (headerLines.length > 0) {
    html += '<div class="info-header-section">'
    for (const h of headerLines) {
      // "Key: Value" 형식이면 강조 처리
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

  // 시간표를 테이블로 변환
  if (scheduleLines.length > 0) {
    html += '<table class="schedule-table"><thead><tr><th>시간</th><th>내용</th></tr></thead><tbody>'
    for (const line of scheduleLines) {
      // "9:00 - 10:30  Brand Strategy" 또는 "9:00 AM - 10:30 AM  Brand Strategy" 패턴
      const match = line.match(/^(\d{1,2}:\d{2}[\s]*(?:AM|PM)?[\s]*[-–~][\s]*\d{1,2}:\d{2}[\s]*(?:AM|PM)?)\s+(.+)$/i)
      if (match) {
        html += `<tr><td class="time-cell">${match[1].trim()}</td><td>${match[2].trim()}</td></tr>`
      } else {
        // 단순 분리 시도: 첫 시간 부분과 나머지
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

  // 시간표가 없으면 원본 텍스트 표시
  if (scheduleLines.length === 0 && headerLines.length === 0) {
    html = `<pre class="info-raw">${info}</pre>`
  }

  return html
}

function startPrep() {
  phase.value = 'prep'
  const q = currentQuestion.value
  timeLeft.value = q?.prepTime || currentPart.value.prepTime
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
  finishResponse()
}

async function startResponse() {
  phase.value = 'response'
  const q = currentQuestion.value
  timeLeft.value = q?.responseTime || currentPart.value.responseTime
  audioChunks = []

  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    mediaRecorder = new MediaRecorder(stream)
    mediaRecorder.ondataavailable = (e) => audioChunks.push(e.data)
    mediaRecorder.onstop = async () => {
      const blob = new Blob(audioChunks, { type: 'audio/webm' })
      audioUrl.value = URL.createObjectURL(blob)
      lastAudioBlob.value = blob
      stream.getTracks().forEach(t => t.stop())

      // 서버에 오디오 업로드
      try {
        const formData = new FormData()
        formData.append('file', blob, 'recording.webm')
        formData.append('userId', store.currentUser?.userId || 1)
        formData.append('partNumber', currentPart.value.id)
        formData.append('questionIdx', currentQuestionIdx.value)
        const res = await fetch('/api/files/audio', { method: 'POST', body: formData })
        const data = await res.json()
        savedAudioUrl.value = data.url
      } catch (err) {
        console.warn('오디오 업로드 실패:', err)
      }
    }
    mediaRecorder.start()

    // 녹음과 동시에 STT 시작 (모든 파트)
    startSTT()
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
  stopSTT()

  // 세트 내 다음 질문이 있으면 자동 진행
  if (isSetPart.value && subQuestionIdx.value < currentSetQuestions.value.length - 1) {
    subQuestionIdx.value++
    audioUrl.value = null
    savedAudioUrl.value = null
    startPrep()
    return
  }

  // 세트 완료 또는 일반 문제 완료
  phase.value = 'done'

  // Part 1: 자동 발음 분석 시작
  if (currentPart.value.id === 1) {
    startPronunciationAnalysis()
  }

  store.addRecord({
    partId: currentPart.value.id,
    questionIdx: currentQuestionIdx.value,
    partTitle: currentPart.value.title
  })

  // user_responses 테이블에 저장 (실전모드 음성)
  const q = currentQuestion.value
  store.saveUserResponse({
    questionId: q?.questionId,
    practiceMode: practiceModeCode.value,
    audioFilePath: savedAudioUrl.value || null,
    sttText: sttText.value || null,
    textAnswer: null
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
  if (currentQuestionIdx.value < allQuestions.value.length - 1) {
    startQuestion(currentQuestionIdx.value + 1)
  }
}

function reset() {
  clearInterval(timer)
  started.value = false
  subQuestionIdx.value = 0
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

.loading-state {
  text-align: center;
  padding: 48px 24px;
  color: #4A90D9;
  font-size: 1.1rem;
  font-weight: 600;
}

.set-label {
  display: block;
  font-size: 0.8rem;
  color: #888;
  margin-top: 2px;
}

.set-progress {
  text-align: center;
  font-size: 0.9rem;
  font-weight: 600;
  color: #4A90D9;
  background: #eef3fb;
  padding: 8px;
  border-radius: 6px;
  margin-bottom: 16px;
}

.practice-header h1 {
  font-size: 1.3rem;
  margin: 8px 0 20px;
}

.question-select-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.question-select-header h2 {
  margin: 0;
}

.btn-generate {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  padding: 8px 20px;
  font-size: 0.9rem;
  font-weight: 600;
}

.btn-generate:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.question-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.question-btn {
  min-width: 100px;
}

.question-btn.ai-generated {
  border: 2px solid #764ba2;
  color: #764ba2;
  font-weight: 600;
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
  border: 1px solid #ddd;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.info-table-title {
  background: #f5f0e8;
  padding: 20px 16px 8px;
  font-size: 1.4rem;
  font-weight: 700;
  text-align: center;
  color: #333;
}

.info-table-details {
  background: #f5f0e8;
  padding: 4px 16px 16px;
  text-align: center;
  color: #555;
  font-size: 0.95rem;
  line-height: 1.6;
  white-space: pre-line;
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

.schedule-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.95rem;
}

.schedule-table thead {
  background: #e8e0d0;
}

.schedule-table th {
  padding: 10px 12px;
  text-align: left;
  font-weight: 600;
  color: #555;
  border-bottom: 2px solid #ccc;
}

.schedule-table th.col-time { width: 25%; }
.schedule-table th.col-content { width: 50%; }
.schedule-table th.col-speaker { width: 25%; text-align: right; }

.schedule-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #eee;
  vertical-align: top;
}

.schedule-table tr:last-child td {
  border-bottom: none;
}

.schedule-table tr:nth-child(even) {
  background: #fafafa;
}

.time-cell {
  white-space: nowrap;
  font-weight: 500;
  color: #555;
}

.speaker-cell {
  text-align: right;
  color: #666;
}

:deep(.info-header-section) {
  margin-bottom: 12px;
}

:deep(.info-title) {
  font-size: 1.1rem;
  font-weight: 700;
  color: #333;
  margin-bottom: 6px;
}

:deep(.info-meta) {
  display: flex;
  gap: 8px;
  padding: 3px 0;
  font-size: 0.95rem;
}

:deep(.info-key) {
  font-weight: 600;
  color: #4A90D9;
  min-width: 80px;
}

:deep(.info-key::after) {
  content: ':';
}

:deep(.info-val) {
  color: #333;
}

:deep(.schedule-table) {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.95rem;
}

:deep(.schedule-table thead) {
  background: #f0f4fa;
}

:deep(.schedule-table th) {
  padding: 8px 12px;
  text-align: left;
  font-weight: 600;
  color: #4A90D9;
  border-bottom: 2px solid #d0d8e8;
}

:deep(.schedule-table td) {
  padding: 8px 12px;
  border-bottom: 1px solid #eee;
}

:deep(.schedule-table tr:last-child td) {
  border-bottom: none;
}

:deep(.schedule-table tr:hover) {
  background: #f8fafd;
}

:deep(.time-cell) {
  white-space: nowrap;
  font-weight: 500;
  color: #555;
  min-width: 140px;
}

:deep(.info-raw) {
  white-space: pre-wrap;
  font-size: 0.95rem;
  margin: 0;
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

.save-status {
  color: #27ae60;
  font-size: 0.85rem;
  margin-top: 6px;
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

/* 자유/한글 모드 */
.phase-free {
  padding: 8px 0;
}

.free-mode-label {
  text-align: center;
  font-size: 1rem;
  font-weight: 600;
  color: #4A90D9;
  background: #eef3fb;
  padding: 8px;
  border-radius: 6px;
  margin-bottom: 16px;
}

.free-mode-label.korean-label {
  color: #e67e22;
  background: #fef5e7;
}

.answer-type-toggle {
  display: flex;
  gap: 0;
  margin-bottom: 16px;
  border-radius: 8px;
  overflow: hidden;
  border: 2px solid #4A90D9;
}

.answer-type-toggle button {
  flex: 1;
  padding: 10px;
  background: #fff;
  color: #4A90D9;
  font-weight: 600;
  border: none;
  cursor: pointer;
  font-size: 0.95rem;
}

.answer-type-toggle button.active {
  background: #4A90D9;
  color: #fff;
}

.btn-record {
  display: block;
  margin: 0 auto;
  padding: 12px 32px;
  font-size: 1rem;
}

.btn-record-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin: 0 auto;
  padding: 16px 40px;
  background: #4A90D9;
  color: #fff;
  border: none;
  border-radius: 50px;
  font-size: 1.05rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-record-toggle.recording {
  background: #e74c3c;
  animation: pulse-shadow 1.5s infinite;
}

.record-icon {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: rgba(255,255,255,0.9);
  transition: all 0.3s;
}

.record-icon.active {
  width: 14px;
  height: 14px;
  border-radius: 3px;
  background: #fff;
}

.recording-status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 12px;
  font-size: 1rem;
  color: #e74c3c;
  font-weight: 500;
}

@keyframes pulse-shadow {
  0% { box-shadow: 0 0 0 0 rgba(231,76,60,0.4); }
  70% { box-shadow: 0 0 0 12px rgba(231,76,60,0); }
  100% { box-shadow: 0 0 0 0 rgba(231,76,60,0); }
}

.text-area textarea,
.korean-answer-area textarea {
  width: 100%;
  padding: 14px;
  border: 2px solid #ddd;
  border-radius: 8px;
  font-size: 1rem;
  font-family: inherit;
  resize: vertical;
  margin-bottom: 12px;
  line-height: 1.6;
}

.text-area textarea:focus,
.korean-answer-area textarea:focus {
  border-color: #4A90D9;
  outline: none;
}

.btn-translate {
  display: block;
  width: 100%;
  padding: 12px;
  font-size: 1rem;
  background: linear-gradient(135deg, #e67e22, #d35400);
}

.translation-result {
  margin-top: 16px;
  text-align: left;
  background: #fffbf0;
  border: 1px solid #f0d9a8;
}

.part1-voice-only {
  text-align: center;
  color: #4A90D9;
  font-weight: 500;
  margin-bottom: 16px;
  font-size: 0.95rem;
}

/* 발음 분석 */
.pronunciation-section {
  margin: 20px 0;
  text-align: left;
}

.feedback-area {
  width: 100%;
  margin-top: 12px;
}

.stt-processing {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 20px;
  color: #4A90D9;
  font-weight: 600;
}

.stt-spinner {
  width: 20px;
  height: 20px;
  border: 3px solid #e0e0e0;
  border-top-color: #4A90D9;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.stt-result {
  background: #f0f7ff;
  border: 1px solid #c8ddf5;
  border-radius: 8px;
  padding: 16px;
  text-align: left;
}

.stt-result h4 {
  font-size: 0.85rem;
  color: #4A90D9;
  margin-bottom: 8px;
}

.stt-text {
  font-size: 1rem;
  line-height: 1.7;
  color: #333;
  background: #fff;
  padding: 12px;
  border-radius: 6px;
  border: 1px solid #e0e8f0;
}

.stt-failed textarea {
  width: 100%;
  padding: 14px;
  border: 2px solid #ddd;
  border-radius: 8px;
  font-size: 1rem;
  font-family: inherit;
  resize: vertical;
  margin-bottom: 10px;
}

.stt-fail-msg {
  color: #e67e22;
  font-size: 0.9rem;
  margin-bottom: 10px;
  text-align: center;
}

.pron-loading {
  text-align: center;
  color: #4A90D9;
  font-weight: 600;
  padding: 20px;
}

.pron-result {
  background: #f8faff;
  border: 1px solid #d0dff0;
}

.pron-score {
  text-align: center;
  margin-bottom: 12px;
}

.pron-score-number {
  font-size: 2.5rem;
  font-weight: 700;
}

.pron-score-number.score-great { color: #27ae60; }
.pron-score-number.score-good { color: #4A90D9; }
.pron-score-number.score-fair { color: #e67e22; }
.pron-score-number.score-poor { color: #e74c3c; }

.pron-score-label {
  font-size: 1rem;
  color: #999;
  margin-left: 4px;
}

.pron-feedback {
  text-align: center;
  color: #555;
  margin-bottom: 16px;
  line-height: 1.6;
}

.pron-words h4 {
  color: #e74c3c;
  margin-bottom: 10px;
  font-size: 1rem;
}

.pron-word-item {
  padding: 10px 12px;
  border-bottom: 1px solid #eee;
}

.pron-word-item:last-child {
  border-bottom: none;
}

.pron-word-main {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.pron-original {
  font-weight: 700;
  color: #27ae60;
  font-size: 1.05rem;
}

.pron-arrow {
  color: #999;
}

.pron-spoken {
  color: #e74c3c;
  font-weight: 500;
  text-decoration: line-through;
}

.btn-listen-word {
  background: none;
  border: 1px solid #4A90D9;
  border-radius: 50%;
  width: 30px;
  height: 30px;
  font-size: 0.9rem;
  cursor: pointer;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pron-tip {
  color: #888;
  font-size: 0.85rem;
  margin-left: 4px;
}

.pron-perfect {
  text-align: center;
  color: #27ae60;
  font-weight: 600;
  padding: 16px;
}

.pron-full-listen {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 16px;
}

.btn-full-listen {
  padding: 10px 24px;
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
