<template>
  <div class="practice">
    <h1>연습하기</h1>
    <p class="subtitle">파트와 연습 모드를 선택하세요</p>

    <!-- 연습 모드 선택 -->
    <div class="mode-selector">
      <button
        v-for="m in modes"
        :key="m.id"
        class="mode-btn"
        :class="{ active: selectedMode === m.id }"
        @click="selectedMode = m.id"
      >
        <span class="mode-icon">{{ m.icon }}</span>
        <span class="mode-name">{{ m.name }}</span>
        <span class="mode-desc">{{ m.desc }}</span>
      </button>
    </div>

    <!-- 파트 목록 -->
    <div class="parts-list">
      <div
        v-for="part in store.parts"
        :key="part.id"
        class="card part-item"
        :class="{ disabled: selectedMode === 'korean' && part.id === 1 }"
        @click="goToPractice(part.id)"
      >
        <div class="part-header">
          <span class="part-badge">Part {{ part.id }}</span>
          <span v-if="selectedMode === 'korean' && part.id === 1" class="part-unavailable">이 모드 사용 불가</span>
          <span v-else class="question-count">{{ part.questions.length }}문제</span>
        </div>
        <h2>{{ part.title }}</h2>
        <p>{{ part.description }}</p>
        <div class="part-meta">
          <span v-if="selectedMode === 'real'">준비 {{ part.prepTime }}초 / 응답 {{ part.responseTime }}초</span>
          <span v-else-if="selectedMode === 'free' && part.id === 1">시간 제한 없음 | 음성만 가능 (발음 분석)</span>
          <span v-else-if="selectedMode === 'free'">시간 제한 없음 | 음성 또는 텍스트</span>
          <span v-else-if="selectedMode === 'korean' && part.id === 1">Part 1은 한글 연습 모드를 지원하지 않습니다</span>
          <span v-else>시간 제한 없음 | 한글 답변 → 영어 번역</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useSpeakingStore } from '../stores/speaking'

const store = useSpeakingStore()
const router = useRouter()

const selectedMode = ref('real')

const modes = [
  { id: 'real', icon: '🎯', name: '실전 연습', desc: '준비시간 + 답변시간 (음성 녹음)' },
  { id: 'free', icon: '📝', name: '자유 연습', desc: '시간 제한 없이 음성 또는 텍스트로 답변' },
  { id: 'korean', icon: '🇰🇷', name: '한글 연습', desc: '한글로 답변 → AI가 영어로 번역' }
]

function goToPractice(partId) {
  if (selectedMode.value === 'korean' && partId === 1) return
  router.push({ path: `/practice/${partId}`, query: { mode: selectedMode.value } })
}
</script>

<style scoped>
.practice h1 {
  font-size: 1.5rem;
  margin-bottom: 4px;
}

.subtitle {
  color: #888;
  margin-bottom: 24px;
}

/* 모드 선택 */
.mode-selector {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.mode-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 16px 12px;
  background: #fff;
  border: 2px solid #e0e0e0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.mode-btn:hover {
  border-color: #4A90D9;
  background: #f8faff;
}

.mode-btn.active {
  border-color: #4A90D9;
  background: #eef3fb;
  box-shadow: 0 2px 8px rgba(74,144,217,0.15);
}

.mode-icon {
  font-size: 1.5rem;
}

.mode-name {
  font-weight: 600;
  font-size: 0.95rem;
  color: #333;
}

.mode-desc {
  font-size: 0.78rem;
  color: #888;
  text-align: center;
  line-height: 1.3;
}

/* 파트 목록 */
.parts-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.part-item {
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
}

.part-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(0,0,0,0.1);
}

.part-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.part-badge {
  background: #4A90D9;
  color: #fff;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 0.8rem;
  font-weight: 600;
}

.question-count {
  color: #999;
  font-size: 0.85rem;
}

.part-item h2 {
  font-size: 1.15rem;
  margin-bottom: 4px;
}

.part-item p {
  color: #666;
  font-size: 0.95rem;
  margin-bottom: 12px;
}

.part-meta {
  color: #999;
  font-size: 0.85rem;
}

.part-item.disabled {
  opacity: 0.4;
  cursor: not-allowed;
  pointer-events: none;
}

.part-unavailable {
  color: #e74c3c;
  font-size: 0.8rem;
  font-weight: 600;
}

@media (max-width: 600px) {
  .mode-selector {
    flex-direction: column;
  }
}
</style>
