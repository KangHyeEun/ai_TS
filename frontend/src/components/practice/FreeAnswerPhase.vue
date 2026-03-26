<template>
  <div class="phase-free">
    <div class="free-mode-label" v-if="practiceMode === 'free'">자유 연습 모드</div>
    <div class="free-mode-label korean-label" v-else>한글 연습 모드</div>

    <!-- 세트 진행 표시 (모드 라벨 바로 아래) -->
    <div v-if="isSetPart && setQuestionsCount > 1" class="set-progress">
      Q{{ subQuestionIdx + 1 }} / {{ setQuestionsCount }}
    </div>

    <!-- Part 4 표 -->
    <InfoTable v-if="currentQuestion.hasSchedule || currentQuestion.info" :question="currentQuestion" />

    <div class="question-text" style="margin-bottom:20px">
      <p>{{ currentQuestion.text }}</p>
      <div v-if="currentQuestion.imageUrl" class="question-image">
        <img :src="currentQuestion.imageUrl" alt="사진" />
      </div>
      <p v-if="currentQuestion.hint && !currentQuestion.imageUrl" class="hint">{{ currentQuestion.hint }}</p>
    </div>

    <!-- 자유 모드: 음성/텍스트 선택 -->
    <div v-if="practiceMode === 'free'" class="free-answer-area">
      <div v-if="partId !== 1" class="answer-type-toggle">
        <button :class="{ active: freeAnswerType === 'voice' }" @click="freeAnswerType = 'voice'">음성 답변</button>
        <button :class="{ active: freeAnswerType === 'text' }" @click="freeAnswerType = 'text'">텍스트 답변</button>
      </div>
      <p v-if="partId === 1" class="part1-voice-only">Part 1은 음성 녹음으로만 연습합니다. 지문을 소리내어 읽어주세요.</p>

      <div v-if="freeAnswerType === 'voice' || partId === 1" class="voice-area">
        <button
          class="btn-record-toggle"
          :class="{ recording: freeRecording }"
          @click="$emit('toggleFreeRecording')"
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
          <button class="btn-primary" style="margin-top:12px" @click="$emit('finishFreeAnswer')">답변 완료</button>
          <button class="btn-secondary" style="margin-top:8px" @click="$emit('retryRecording')">다시 녹음</button>
        </div>
      </div>

      <div v-if="freeAnswerType === 'text' && partId !== 1" class="text-area">
        <textarea :value="freeTextAnswer" @input="$emit('update:freeTextAnswer', $event.target.value)" placeholder="영어로 답변을 입력하세요..." rows="5"></textarea>
        <button class="btn-primary" @click="$emit('submitFreeTextAnswer')" :disabled="!freeTextAnswer.trim()">답변 제출</button>
      </div>
    </div>

    <!-- 한글 모드 -->
    <div v-if="practiceMode === 'korean'" class="korean-answer-area">
      <textarea :value="koreanAnswer" @input="$emit('update:koreanAnswer', $event.target.value)" placeholder="한글로 답변을 입력하세요..." rows="5"></textarea>
      <button class="btn-primary btn-translate-sm" @click="$emit('submitKoreanAnswer')" :disabled="!koreanAnswer.trim() || translating">
        {{ translating ? '번역 중...' : '답변 제출' }}
      </button>

      <!-- 구조화된 번역 탭 UI -->
      <div v-if="translationData" class="translation-tabs-container card">
        <div class="feedback-tabs">
          <button v-for="tab in transTabs" :key="tab.key" class="feedback-tab" :class="{ active: activeTransTab === tab.key }" @click="activeTransTab = tab.key">
            {{ tab.label }}
          </button>
        </div>
        <div class="feedback-tab-content">
          <!-- 영어 번역 -->
          <div v-if="activeTransTab === 'translation'">
            <div class="corrected-en">
              <h4>내 답변의 영어 번역</h4>
              <p>{{ translationData.translation }}</p>
            </div>
          </div>
          <!-- 개선된 표현 -->
          <div v-if="activeTransTab === 'improved'">
            <div v-if="translationData.improved" class="improved-section">
              <h4 class="improved-label">English</h4>
              <p class="improved-en">{{ translationData.improved }}</p>
            </div>
            <div v-if="translationData.improvedKo" class="improved-section">
              <h4 class="improved-label" style="color:#555">한국어 해석</h4>
              <p class="improved-ko">{{ translationData.improvedKo }}</p>
            </div>
          </div>
          <!-- 개선 설명 -->
          <div v-if="activeTransTab === 'explanation'">
            <div v-if="translationData.improvements && translationData.improvements.length" class="improve-list">
              <div v-for="(item, i) in translationData.improvements" :key="i" class="improve-detail-item">
                <div class="improve-detail-header">{{ i + 1 }}. {{ item.reason }}</div>
                <div class="feedback-compare">
                  <div class="compare-row before">
                    <span class="compare-label">원래</span>
                    <span class="compare-text">{{ item.before }}</span>
                  </div>
                  <div class="compare-arrow">&darr;</div>
                  <div class="compare-row after">
                    <span class="compare-label">개선</span>
                    <span class="compare-text">{{ item.after }}</span>
                  </div>
                </div>
              </div>
            </div>
            <!-- fallback: 기존 문자열 설명 -->
            <div v-else-if="translationData.improvementExplanation" class="improve-explanation-text">
              {{ translationData.improvementExplanation }}
            </div>
          </div>
          <!-- 핵심 표현 -->
          <div v-if="activeTransTab === 'expressions'">
            <div v-for="(expr, i) in translationData.keyExpressions" :key="i" class="expression-item">
              <div class="expression-en">{{ expr.expression }}</div>
              <div class="expression-ko">{{ expr.meaning }}</div>
              <div v-if="expr.example" class="expression-example"><span class="example-label">Ex.</span> {{ expr.example }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- fallback: 원본 텍스트 -->
      <div v-else-if="translationResult" class="translation-result card">
        <div class="ai-result-content" v-html="renderMarkdown(translationResult)"></div>
      </div>

      <div v-if="translationResult || translationData" class="done-actions" style="margin-top:16px">
        <button class="btn-primary" @click="$emit('nextFreeQuestion')">다음 문제</button>
        <button class="btn-secondary" @click="$emit('reset')">다른 문제</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import InfoTable from './InfoTable.vue'

defineProps({
  practiceMode: String,
  currentQuestion: Object,
  isSetPart: Boolean,
  setQuestionsCount: Number,
  subQuestionIdx: Number,
  partId: Number,
  freeRecording: Boolean,
  freeRecordTime: Number,
  audioUrl: String,
  freeTextAnswer: String,
  koreanAnswer: String,
  translationResult: String,
  translationData: Object,
  translating: Boolean
})

defineEmits([
  'toggleFreeRecording', 'finishFreeAnswer', 'retryRecording',
  'submitFreeTextAnswer', 'submitKoreanAnswer', 'nextFreeQuestion', 'reset',
  'update:freeTextAnswer', 'update:koreanAnswer'
])

const freeAnswerType = ref('voice')
const activeTransTab = ref('translation')

const transTabs = [
  { key: 'translation', label: '영어 번역' },
  { key: 'improved', label: '개선 표현' },
  { key: 'explanation', label: '개선 설명' },
  { key: 'expressions', label: '핵심 표현' }
]

function renderMarkdown(text) {
  return text
    .replace(/## (.+)/g, '<h3>$1</h3>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
}
</script>

<style src="../../styles/practice.css"></style>
