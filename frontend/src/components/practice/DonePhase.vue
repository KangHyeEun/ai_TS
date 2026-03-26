<template>
  <div class="phase-done">
    <h2>연습 완료!</h2>
    <p>Part {{ partId }} - 문제 {{ questionIdx + 1 }}</p>

    <!-- 세트형: 각 질문별 답변 표시 -->
    <div v-if="isSetPart && subAnswers.length > 0" class="set-answers">
      <div v-for="(ans, i) in subAnswers" :key="i" class="set-answer-item">
        <h4>Q{{ i + 1 }}: {{ ans.text }}</h4>
        <audio v-if="ans.audioUrl" :src="ans.audioUrl" controls></audio>
        <p v-if="ans.sttText" class="stt-text-small">{{ ans.sttText }}</p>
      </div>
    </div>

    <!-- 일반 문제: 단일 녹음 -->
    <div v-else-if="audioUrl" class="playback">
      <h3>내 녹음 듣기</h3>
      <audio :src="audioUrl" controls></audio>
      <p v-if="savedAudioUrl" class="save-status">서버에 저장됨</p>
    </div>

    <!-- Part 1: 발음 분석 -->
    <PronunciationResult
      v-if="partId === 1"
      :result="pronunciationResult"
      :loading="pronunciationLoading"
      :scoreClass="pronScoreClass"
      :speaking="isSpeaking"
      @speakWord="$emit('speakWord', $event)"
      @speakFullText="$emit('speakFullText')"
      @stopSpeaking="$emit('stopSpeaking')"
    />

    <AiSection
      :aiLoading="aiLoading"
      :aiMode="aiMode"
      :aiResult="aiResult"
      :feedbackData="feedbackData"
      :showFeedbackArea="showFeedbackArea"
      :sttProcessing="sttProcessing"
      :sttText="sttText"
      :audioUrl="audioUrl"
      :userAnswer="userAnswer"
      @requestFeedback="$emit('requestFeedback')"
      @getFeedback="$emit('getFeedback')"
      @update:userAnswer="$emit('update:userAnswer', $event)"
    />

    <div class="done-actions">
      <button class="btn-primary" @click="$emit('retry')">다시 하기</button>
      <button class="btn-next" @click="$emit('goNext')" v-if="hasNext">
        {{ nextLabel }}
      </button>
      <button class="btn-secondary" @click="$emit('reset')">다른 문제</button>
    </div>
  </div>
</template>

<script setup>
import PronunciationResult from './PronunciationResult.vue'
import AiSection from './AiSection.vue'

defineProps({
  partId: Number,
  questionIdx: Number,
  isSetPart: Boolean,
  subAnswers: Array,
  audioUrl: String,
  savedAudioUrl: String,
  // pronunciation
  pronunciationResult: Object,
  pronunciationLoading: Boolean,
  pronScoreClass: String,
  isSpeaking: Boolean,
  // ai
  aiLoading: Boolean,
  aiMode: String,
  aiResult: String,
  feedbackData: Object,
  showFeedbackArea: Boolean,
  sttProcessing: Boolean,
  sttText: String,
  userAnswer: String,
  // navigation
  hasNext: Boolean,
  nextLabel: String
})

defineEmits([
  'speakWord', 'speakFullText', 'stopSpeaking',
  'requestFeedback', 'getFeedback', 'update:userAnswer',
  'retry', 'goNext', 'reset'
])
</script>

<style src="../../styles/practice.css"></style>
