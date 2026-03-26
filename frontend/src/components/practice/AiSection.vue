<template>
  <div class="ai-section">
    <!-- 단일 버튼: AI 피드백 받기 -->
    <button
      v-if="!feedbackData && !showFeedbackArea && !(aiMode === 'feedback' && aiLoading)"
      class="btn-ai feedback-btn"
      @click="$emit('requestFeedback')"
      :disabled="aiLoading || sttProcessing"
    >
      {{ sttProcessing ? '음성 인식 중...' : (aiLoading && aiMode === 'feedback' ? '피드백 분석 중...' : 'AI 피드백 받기') }}
    </button>

    <div v-if="aiLoading && aiMode === 'feedback'" class="feedback-loading">피드백 분석 중...</div>

    <!-- 피드백 입력 영역 (비세트형에서 STT 결과 확인) -->
    <div v-if="showFeedbackArea" class="feedback-area">
      <div v-if="sttProcessing" class="stt-processing">
        <span class="stt-spinner"></span> 음성을 텍스트로 변환 중...
      </div>

      <div v-else-if="sttText" class="stt-result">
        <h4>{{ audioUrl ? '음성 인식 결과' : '제출한 답변' }}</h4>
        <p class="stt-text">{{ sttText }}</p>
        <button class="btn-primary" @click="$emit('getFeedback')" :disabled="aiLoading" style="margin-top:10px; width:100%">
          {{ aiLoading && aiMode === 'feedback' ? '피드백 분석 중...' : '이 내용으로 피드백 받기' }}
        </button>
      </div>

      <div v-else class="stt-failed">
        <p class="stt-fail-msg">음성 인식에 실패했습니다. 답변을 직접 입력해주세요.</p>
        <textarea :value="userAnswer" @input="$emit('update:userAnswer', $event.target.value)" placeholder="본인의 답변을 영어로 입력하세요..." rows="4"></textarea>
        <button class="btn-primary" @click="$emit('getFeedback')" :disabled="aiLoading || !userAnswer.trim()" style="width:100%">
          {{ aiLoading && aiMode === 'feedback' ? '분석 중...' : '피드백 요청' }}
        </button>
      </div>
    </div>

    <!-- 구조화된 피드백 탭 UI -->
    <div v-if="feedbackData" class="feedback-tabs-container card">
      <div class="feedback-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="feedback-tab"
          :class="{ active: activeTab === tab.key }"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </div>

      <div class="feedback-tab-content">
        <!-- 현재 예상 점수 -->
        <div v-if="activeTab === 'score'" class="tab-score">
          <div class="score-display">
            <div class="score-big" :class="scoreClass">{{ feedbackData.estimatedScore }}</div>
            <div class="score-unit">/ 200</div>
          </div>
          <p class="score-comment">{{ feedbackData.scoreComment }}</p>
        </div>

        <!-- 목표 분석 + 달성 팁 -->
        <div v-if="activeTab === 'analysis'" class="tab-analysis">
          <p class="analysis-text">{{ feedbackData.targetAnalysis }}</p>

          <div v-if="feedbackData.targetTips && feedbackData.targetTips.length > 0" class="target-tips">
            <h4 class="tips-title">목표 점수 달성 팁</h4>
            <ul class="tips-list">
              <li v-for="(tip, i) in feedbackData.targetTips" :key="i">{{ tip }}</li>
            </ul>
          </div>
        </div>

        <!-- 잘한 점 -->
        <div v-if="activeTab === 'strengths'" class="tab-strengths">
          <div v-if="!feedbackData.strengths || feedbackData.strengths.length === 0" class="empty-feedback">
            분석할 수 있는 답변이 부족합니다. 영어로 답변을 시도해보세요.
          </div>
          <div v-for="(s, i) in feedbackData.strengths" :key="i" class="feedback-item strength-item">
            <div class="feedback-item-header">
              <span class="feedback-badge good">&#10003;</span>
              <strong>{{ s.point }}</strong>
            </div>
            <p class="feedback-item-detail">{{ s.detail }}</p>
            <div v-if="s.quote" class="feedback-quote">
              <span class="quote-label">내 답변:</span> "{{ s.quote }}"
            </div>
          </div>
        </div>

        <!-- 개선할 점 -->
        <div v-if="activeTab === 'improvements'" class="tab-improvements">
          <div v-for="(imp, i) in feedbackData.improvements" :key="i" class="feedback-item improve-item">
            <div class="feedback-item-header">
              <span class="feedback-badge improve">!</span>
              <strong>{{ imp.point }}</strong>
            </div>
            <p class="feedback-item-detail">{{ imp.detail }}</p>
            <div v-if="imp.studentSaid || imp.betterWay" class="feedback-compare">
              <div v-if="imp.studentSaid" class="compare-row before">
                <span class="compare-label">내 답변</span>
                <span class="compare-text">{{ imp.studentSaid }}</span>
              </div>
              <div v-if="imp.studentSaid && imp.betterWay" class="compare-arrow">&darr;</div>
              <div v-if="imp.betterWay" class="compare-row after">
                <span class="compare-label">개선안</span>
                <span class="compare-text">{{ imp.betterWay }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 수정 답변 (문제별) -->
        <div v-if="activeTab === 'corrected'" class="tab-corrected">
          <div
            v-for="(ca, i) in feedbackData.correctedAnswers"
            :key="i"
            class="corrected-item"
            :class="{ 'multi-item': feedbackData.correctedAnswers.length > 1 }"
          >
            <div v-if="feedbackData.correctedAnswers.length > 1" class="corrected-q-header">
              <span class="corrected-q-badge">Q{{ i + 1 }}</span>
              <span class="corrected-q-text">{{ ca.question }}</span>
              <span class="corrected-q-time">{{ ca.responseTime }}초</span>
            </div>
            <div class="corrected-en">
              <h4>English</h4>
              <p>{{ ca.answer }}</p>
            </div>
            <div v-if="ca.answerKo" class="corrected-ko">
              <h4>한국어 해석</h4>
              <p>{{ ca.answerKo }}</p>
            </div>
          </div>
        </div>

        <!-- 핵심 표현 학습 -->
        <div v-if="activeTab === 'expressions'" class="tab-expressions">
          <div v-for="(expr, i) in feedbackData.keyExpressions" :key="i" class="expression-item">
            <div class="expression-en">{{ expr.expression }}</div>
            <div class="expression-ko">{{ expr.meaning }}</div>
            <div v-if="expr.example" class="expression-example">
              <span class="example-label">Ex.</span> {{ expr.example }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- fallback 텍스트 (파싱 실패 시) -->
    <div v-if="aiResult" class="ai-result card">
      <div class="ai-result-content" v-html="renderMarkdown(aiResult)"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  aiLoading: Boolean,
  aiMode: String,
  aiResult: String,
  feedbackData: Object,
  showFeedbackArea: Boolean,
  sttProcessing: Boolean,
  sttText: String,
  audioUrl: String,
  userAnswer: String
})

defineEmits(['requestFeedback', 'getFeedback', 'update:userAnswer'])

const activeTab = ref('score')

const tabs = [
  { key: 'score', label: '예상 점수' },
  { key: 'analysis', label: '목표 분석' },
  { key: 'strengths', label: '잘한 점' },
  { key: 'improvements', label: '개선할 점' },
  { key: 'corrected', label: '수정 답변' },
  { key: 'expressions', label: '핵심 표현' }
]

const scoreClass = computed(() => {
  const s = props.feedbackData?.estimatedScore || 0
  if (s >= 160) return 'score-high'
  if (s >= 110) return 'score-mid'
  return 'score-low'
})

function renderMarkdown(text) {
  return text
    .replace(/## (.+)/g, '<h3>$1</h3>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
}
</script>

<style src="../../styles/practice.css"></style>
