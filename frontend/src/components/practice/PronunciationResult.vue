<template>
  <div class="pronunciation-section">
    <div v-if="loading" class="pron-loading">발음 분석 중...</div>
    <div v-else-if="result" class="pron-result card">
      <div class="pron-score">
        <span class="pron-score-number" :class="scoreClass">{{ result.score }}점</span>
        <span class="pron-score-label">/ 100</span>
      </div>
      <p class="pron-feedback">{{ result.overallFeedback }}</p>

      <div v-if="result.incorrectWords && result.incorrectWords.length > 0" class="pron-words">
        <h4>교정이 필요한 단어</h4>
        <div class="pron-word-item" v-for="(w, i) in result.incorrectWords" :key="i">
          <div class="pron-word-main">
            <span class="pron-original">{{ w.original }}</span>
            <span class="pron-arrow">&rarr;</span>
            <span class="pron-spoken">{{ w.spoken }}</span>
            <button class="btn-listen-word" @click="$emit('speakWord', w.original)" title="발음 듣기">&#x1f50a;</button>
          </div>
          <p class="pron-tip">{{ w.tip }}</p>
        </div>
      </div>
      <div v-else class="pron-perfect">모든 단어를 정확하게 발음했습니다!</div>

      <div class="pron-full-listen">
        <button class="btn-primary btn-full-listen" @click="$emit('speakFullText')" :disabled="speaking">
          {{ speaking ? '재생 중...' : '전체 지문 AI 음성 듣기' }}
        </button>
        <button v-if="speaking" class="btn-secondary" @click="$emit('stopSpeaking')">정지</button>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  result: { type: Object, default: null },
  loading: { type: Boolean, default: false },
  scoreClass: { type: String, default: '' },
  speaking: { type: Boolean, default: false }
})

defineEmits(['speakWord', 'speakFullText', 'stopSpeaking'])
</script>

<style src="../../styles/practice.css"></style>
