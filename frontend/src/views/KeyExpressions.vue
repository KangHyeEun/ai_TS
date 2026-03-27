<template>
  <div class="key-expressions-page">
    <div class="page-header">
      <h1>핵심 표현 모아보기</h1>
      <p class="page-desc">AI 피드백에서 추천받은 핵심 표현들을 한곳에서 복습하세요.</p>
    </div>

    <div v-if="loading" class="loading-state">불러오는 중...</div>

    <div v-else-if="expressions.length === 0" class="card empty-state">
      <p>아직 핵심 표현이 없습니다.</p>
      <p class="empty-hint">연습 후 AI 피드백을 받으면 핵심 표현이 자동으로 수집됩니다.</p>
      <router-link to="/practice">
        <button class="btn-primary">연습 시작하기</button>
      </router-link>
    </div>

    <div v-else>
      <!-- 검색 -->
      <div class="search-bar">
        <input v-model="searchQuery" type="text" placeholder="표현 또는 뜻 검색..." class="search-input" />
        <span class="result-count">{{ filteredExpressions.length }}개 표현</span>
      </div>

      <!-- 표현 카드 목록 -->
      <div class="expr-list">
        <div v-for="(expr, i) in filteredExpressions" :key="i" class="card expr-card">
          <div class="expr-main">
            <div class="expr-en">{{ expr.expression }}</div>
            <div class="expr-ko">{{ expr.meaning }}</div>
          </div>
          <div v-if="expr.example" class="expr-example">
            <span class="example-label">Ex.</span> {{ expr.example }}
          </div>
          <div class="expr-meta">
            <span class="meta-date">{{ formatDate(expr.evaluatedAt) }}</span>
            <span v-if="expr.score" class="meta-score">{{ expr.score }}점</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useSpeakingStore } from '../stores/speaking'

const store = useSpeakingStore()
const expressions = ref([])
const loading = ref(true)
const searchQuery = ref('')

const filteredExpressions = computed(() => {
  if (!searchQuery.value.trim()) return expressions.value
  const q = searchQuery.value.toLowerCase()
  return expressions.value.filter(e =>
    e.expression?.toLowerCase().includes(q) ||
    e.meaning?.toLowerCase().includes(q) ||
    e.example?.toLowerCase().includes(q)
  )
})

onMounted(async () => {
  try {
    const userId = store.currentUser?.userId || 1
    const res = await fetch(`/api/evaluations/key-expressions/${userId}`)
    if (res.ok) {
      expressions.value = await res.json()
    }
  } catch (err) {
    console.error('핵심 표현 조회 실패:', err)
  } finally {
    loading.value = false
  }
})

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const month = d.getMonth() + 1
  const day = d.getDate()
  return `${month}/${day}`
}
</script>

<style scoped>
.page-header { margin-bottom: 24px; }
.page-header h1 { font-size: 1.5rem; margin-bottom: 4px; }
.page-desc { color: #888; font-size: 0.9rem; }

.loading-state { text-align: center; padding: 40px; color: #4A90D9; font-weight: 500; }

.empty-state { text-align: center; padding: 48px 24px; }
.empty-state p { color: #888; margin-bottom: 8px; }
.empty-hint { font-size: 0.85rem; margin-bottom: 16px; }

.search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.search-input {
  flex: 1;
  padding: 10px 16px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 0.9rem;
  outline: none;
  transition: border-color 0.2s;
}

.search-input:focus { border-color: #4A90D9; }

.result-count {
  flex-shrink: 0;
  font-size: 0.85rem;
  color: #888;
}

.expr-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.expr-card {
  padding: 16px 20px;
  transition: box-shadow 0.2s;
}

.expr-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.08); }

.expr-main { margin-bottom: 8px; }

.expr-en {
  font-size: 1.05rem;
  font-weight: 600;
  color: #2c3e50;
  line-height: 1.5;
}

.expr-ko {
  font-size: 0.9rem;
  color: #666;
  margin-top: 2px;
  line-height: 1.5;
}

.expr-example {
  padding: 8px 12px;
  background: #f5f8fc;
  border-radius: 6px;
  font-size: 0.88rem;
  color: #555;
  line-height: 1.6;
  margin-bottom: 8px;
}

.example-label {
  font-weight: 700;
  color: #4A90D9;
  margin-right: 4px;
}

.expr-meta {
  display: flex;
  gap: 10px;
  align-items: center;
}

.meta-date {
  font-size: 0.78rem;
  color: #aaa;
}

.meta-score {
  font-size: 0.75rem;
  background: #4A90D9;
  color: #fff;
  padding: 1px 8px;
  border-radius: 10px;
  font-weight: 600;
}
</style>
