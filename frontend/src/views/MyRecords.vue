<template>
  <div class="my-records">
    <div class="records-header">
      <h1>내 연습 기록</h1>
      <button v-if="store.records.length" class="btn-danger" @click="confirmClear">기록 삭제</button>
    </div>

    <div v-if="store.records.length === 0" class="card empty">
      <p>아직 연습 기록이 없습니다.</p>
      <router-link to="/practice">
        <button class="btn-primary">연습 시작하기</button>
      </router-link>
    </div>

    <div v-else>
      <!-- 파트 탭 -->
      <div class="part-tabs">
        <button
          class="part-tab"
          :class="{ active: selectedPart === 0 }"
          @click="selectedPart = 0"
        >
          전체 <span class="tab-count">{{ store.records.length }}</span>
        </button>
        <button
          v-for="p in availableParts"
          :key="p.id"
          class="part-tab"
          :class="{ active: selectedPart === p.id }"
          @click="selectedPart = p.id"
        >
          Part {{ p.id }} <span class="tab-count">{{ p.count }}</span>
        </button>
      </div>

      <!-- 연습 모드 필터 -->
      <div class="mode-filters">
        <button
          class="mode-btn"
          :class="{ active: selectedMode === '' }"
          @click="selectedMode = ''"
        >전체</button>
        <button
          v-for="m in modes"
          :key="m.code"
          class="mode-btn"
          :class="{ active: selectedMode === m.code }"
          @click="selectedMode = m.code"
        >{{ m.label }}</button>
      </div>

      <!-- 기록 목록 -->
      <div class="records-list">
        <div
          class="card record-card"
          v-for="(record, idx) in filteredRecords"
          :key="idx"
        >
          <div class="record-item" @click="toggleDetail(record)">
            <div class="record-info">
              <span class="record-part" :class="'part-' + record.partId">Part {{ record.partId }}</span>
              <span class="record-title">{{ record.partTitle }}</span>
              <span class="record-q">{{ modeLabel(record.practiceMode) }}</span>
            </div>
            <div class="record-right">
              <div class="record-date">{{ formatDate(record.createdAt) }}</div>
              <span class="record-arrow" :class="{ open: expandedId === record.id }">&#9662;</span>
            </div>
          </div>

          <!-- 상세 내용 -->
          <div v-if="expandedId === record.id" class="record-detail">
            <div v-if="detailLoading" class="detail-loading">불러오는 중...</div>
            <div v-else-if="detailData">
              <!-- 세트 문항: 문항별 질문+답변+수정답변 카드 -->
              <div v-if="detailData.questionContent?.subQuestions" class="qa-cards">
                <div v-for="(sq, i) in detailData.questionContent.subQuestions" :key="i" class="qa-card">
                  <div class="qa-question">
                    <span class="sub-badge">Q{{ i + 1 }}</span>
                    <span>{{ typeof sq === 'object' ? sq.text : sq }}</span>
                    <span v-if="typeof sq === 'object' && sq.responseTime" class="response-time">{{ sq.responseTime }}초</span>
                  </div>
                  <div class="qa-answer" :class="{ 'no-answer': !parsedSubAnswers(detailData)?.[i]?.answer }">
                    <span class="answer-label">내 답변</span>
                    {{ parsedSubAnswers(detailData)?.[i]?.answer || '답변 없음' }}
                  </div>
                  <div v-if="parsedCorrectedAnswers(detailData)?.[i]" class="qa-corrected">
                    <div class="corrected-en">
                      <span class="answer-label corrected-label">수정 답변</span>
                      {{ parsedCorrectedAnswers(detailData)[i].answer }}
                    </div>
                    <div v-if="parsedCorrectedAnswers(detailData)[i].answerKo" class="corrected-ko">
                      {{ parsedCorrectedAnswers(detailData)[i].answerKo }}
                    </div>
                  </div>
                </div>
              </div>

              <!-- 일반 문항: 질문+답변+수정답변 -->
              <div v-else>
                <div v-if="detailData.questionContent?.text" class="detail-section">
                  <div class="detail-label">문제</div>
                  <div class="detail-question">{{ detailData.questionContent.text }}</div>
                </div>
                <div class="detail-section">
                  <div class="detail-label">내 답변</div>
                  <div class="detail-answer">
                    <p v-if="detailData.sttText">{{ detailData.sttText }}</p>
                    <p v-else-if="detailData.textAnswer">{{ detailData.textAnswer }}</p>
                    <p v-else class="no-answer">답변 없음</p>
                  </div>
                </div>
                <div v-if="parsedCorrectedAnswers(detailData)?.[0]" class="detail-section">
                  <div class="detail-label">수정 답변</div>
                  <div class="corrected-block">
                    <p class="corrected-en-text">{{ parsedCorrectedAnswers(detailData)[0].answer }}</p>
                    <p v-if="parsedCorrectedAnswers(detailData)[0].answerKo" class="corrected-ko-text">{{ parsedCorrectedAnswers(detailData)[0].answerKo }}</p>
                  </div>
                </div>
              </div>

              <!-- 녹음 파일 -->
              <div v-if="detailData.audioFilePath" class="detail-section">
                <div class="detail-label">녹음</div>
                <audio :src="'/uploads/' + detailData.audioFilePath" controls class="detail-audio"></audio>
              </div>

            </div>
            <div v-else class="detail-empty">상세 정보를 불러올 수 없습니다.</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useSpeakingStore } from '../stores/speaking'

const store = useSpeakingStore()
const selectedPart = ref(0)
const selectedMode = ref('')
const modes = [
  { code: 'REAL', label: '실전 연습' },
  { code: 'FREE', label: '자유 연습' },
  { code: 'KOREAN', label: '한글 연습' },
  { code: 'MOCK', label: '모의고사' }
]
const expandedId = ref(null)
const detailLoading = ref(false)
const detailData = ref(null)

// 사용자 응답 상세 캐시
const detailCache = {}

const availableParts = computed(() => {
  const countMap = {}
  for (const r of store.records) {
    countMap[r.partId] = (countMap[r.partId] || 0) + 1
  }
  return [1, 2, 3, 4, 5].map(id => ({ id, count: countMap[id] || 0 }))
})

const filteredRecords = computed(() => {
  let records = store.records
  if (selectedPart.value !== 0) {
    records = records.filter(r => r.partId === selectedPart.value)
  }
  if (selectedMode.value) {
    records = records.filter(r => r.practiceMode === selectedMode.value)
  }
  return records
})

async function toggleDetail(record) {
  // 닫기
  if (expandedId.value === record.id) {
    expandedId.value = null
    detailData.value = null
    return
  }

  expandedId.value = record.id
  detailData.value = null

  // record에 이미 questionContent가 있으면 바로 사용
  if (record.questionContent) {
    detailData.value = record
    return
  }

  // 캐시 확인
  if (detailCache[record.id]) {
    detailData.value = detailCache[record.id]
    return
  }

  // 상세가 없으면 전체 상세 조회
  detailLoading.value = true
  try {
    const userId = store.currentUser?.userId || 1
    const res = await fetch(`/api/records/${userId}/detail`)
    if (!res.ok) throw new Error()
    const allDetails = await res.json()

    // 캐시에 모두 저장
    for (const d of allDetails) {
      detailCache[d.id] = d
    }

    detailData.value = detailCache[record.id] || null
  } catch (err) {
    console.error('상세 조회 실패:', err)
    detailData.value = null
  } finally {
    detailLoading.value = false
  }
}

function formatDate(dateStr) {
  const d = new Date(dateStr)
  const month = d.getMonth() + 1
  const day = d.getDate()
  const hours = d.getHours().toString().padStart(2, '0')
  const minutes = d.getMinutes().toString().padStart(2, '0')
  return `${month}/${day} ${hours}:${minutes}`
}

function parsedSubAnswers(detail) {
  if (!detail?.sttText) return null
  try {
    const parsed = JSON.parse(detail.sttText)
    if (Array.isArray(parsed) && parsed.length > 0 && parsed[0].question) {
      return parsed
    }
  } catch (e) { /* not JSON, single answer */ }
  return null
}

function parsedCorrectedAnswers(detail) {
  if (!detail?.correctedAnswers) return null
  try {
    const parsed = JSON.parse(detail.correctedAnswers)
    if (Array.isArray(parsed)) return parsed
  } catch (e) { /* not JSON */ }
  return null
}

function modeLabel(mode) {
  const map = { REAL: '실전 연습', FREE: '자유 연습', KOREAN: '한글 연습', MOCK: '모의고사' }
  return map[mode] || mode
}

function confirmClear() {
  if (confirm('모든 연습 기록을 삭제하시겠습니까?')) {
    store.clearRecords()
    expandedId.value = null
  }
}
</script>

<style scoped>
.records-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.records-header h1 { font-size: 1.5rem; }

.empty { text-align: center; padding: 48px 24px; }
.empty p { color: #888; margin-bottom: 16px; }

.part-tabs {
  display: flex;
  gap: 0;
  overflow-x: auto;
  border-bottom: 2px solid #e8ecf1;
  margin-bottom: 16px;
  -webkit-overflow-scrolling: touch;
}

.part-tab {
  flex: 0 0 auto;
  padding: 10px 18px;
  background: none;
  border: none;
  border-bottom: 3px solid transparent;
  font-size: 0.9rem;
  font-weight: 600;
  color: #888;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}

.part-tab:hover { color: #4A90D9; background: #f5f8fc; }
.part-tab.active { color: #4A90D9; border-bottom-color: #4A90D9; }

.tab-count {
  display: inline-block;
  background: #e8ecf1;
  color: #666;
  font-size: 0.75rem;
  padding: 1px 7px;
  border-radius: 10px;
  margin-left: 4px;
}

.part-tab.active .tab-count { background: #4A90D9; color: #fff; }

.mode-filters {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.mode-btn {
  padding: 6px 14px;
  border: 1px solid #ddd;
  border-radius: 20px;
  background: #fff;
  font-size: 0.82rem;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.mode-btn:hover { border-color: #4A90D9; color: #4A90D9; }
.mode-btn.active { background: #4A90D9; color: #fff; border-color: #4A90D9; }

.records-list { display: flex; flex-direction: column; gap: 8px; }

.record-card { overflow: hidden; }

.record-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  cursor: pointer;
  transition: background 0.15s;
}

.record-item:hover { background: #f8fafc; }

.record-info { display: flex; align-items: center; gap: 12px; }

.record-part {
  color: #fff;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 0.8rem;
  font-weight: 600;
}

.part-1 { background: #4A90D9; }
.part-2 { background: #27ae60; }
.part-3 { background: #e67e22; }
.part-4 { background: #8e44ad; }
.part-5 { background: #e74c3c; }

.record-title { font-weight: 500; }
.record-q { color: #999; font-size: 0.85rem; }

.record-right { display: flex; align-items: center; gap: 10px; }
.record-date { color: #999; font-size: 0.85rem; }
.record-arrow {
  color: #bbb;
  font-size: 0.8rem;
  transition: transform 0.2s;
}
.record-arrow.open { transform: rotate(180deg); }

/* 상세 영역 */
.record-detail {
  padding: 0 20px 20px;
  border-top: 1px solid #f0f0f0;
}

.detail-loading {
  text-align: center;
  padding: 20px;
  color: #4A90D9;
  font-weight: 500;
}

.detail-empty {
  text-align: center;
  padding: 20px;
  color: #999;
}

.qa-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 14px;
}

.qa-card {
  border: 1px solid #e8ecf1;
  border-radius: 10px;
  overflow: hidden;
}

.qa-question {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 12px 16px;
  background: #f5f8fc;
  font-size: 0.92rem;
  line-height: 1.6;
  color: #333;
}

.qa-question .response-time {
  flex-shrink: 0;
  font-size: 0.75rem;
  color: #999;
  margin-left: auto;
  padding-left: 8px;
}

.qa-answer {
  padding: 12px 16px;
  font-size: 0.92rem;
  line-height: 1.6;
  color: #333;
  border-top: 1px solid #e8ecf1;
}

.qa-answer .answer-label {
  display: inline-block;
  background: #27ae60;
  color: #fff;
  font-size: 0.7rem;
  font-weight: 700;
  padding: 1px 8px;
  border-radius: 8px;
  margin-right: 8px;
}

.qa-answer.no-answer {
  color: #999;
  font-style: italic;
}

.qa-answer.no-answer .answer-label {
  background: #ccc;
}

.qa-corrected {
  padding: 12px 16px;
  background: #f0faf0;
  border-top: 1px dashed #c8e6c9;
}

.corrected-en {
  font-size: 0.92rem;
  line-height: 1.6;
  color: #2e7d32;
}

.corrected-label {
  background: #8e44ad !important;
}

.corrected-ko {
  margin-top: 6px;
  font-size: 0.85rem;
  line-height: 1.6;
  color: #666;
  padding-left: 4px;
  border-left: 2px solid #ddd;
}

.corrected-block {
  padding: 12px 16px;
  background: #f0faf0;
  border-radius: 8px;
  border-left: 3px solid #27ae60;
}

.corrected-en-text {
  font-size: 0.95rem;
  line-height: 1.7;
  color: #2e7d32;
  margin: 0;
}

.corrected-ko-text {
  margin: 8px 0 0;
  font-size: 0.85rem;
  line-height: 1.6;
  color: #666;
}

.detail-section {
  margin-top: 14px;
}

.detail-label {
  font-size: 0.8rem;
  font-weight: 700;
  color: #4A90D9;
  margin-bottom: 6px;
  text-transform: uppercase;
}

.detail-question {
  padding: 12px 16px;
  background: #f5f8fc;
  border-radius: 8px;
  font-size: 0.95rem;
  line-height: 1.7;
  color: #333;
}

.detail-subs {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.detail-sub-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 0.9rem;
}

.sub-badge {
  flex-shrink: 0;
  background: #4A90D9;
  color: #fff;
  font-size: 0.7rem;
  font-weight: 700;
  padding: 1px 8px;
  border-radius: 8px;
  margin-top: 2px;
}

.detail-answer {
  padding: 12px 16px;
  background: #f9faf5;
  border-radius: 8px;
  border-left: 3px solid #27ae60;
  font-size: 0.95rem;
  line-height: 1.7;
  color: #333;
}

.no-answer {
  color: #999;
  font-style: italic;
}

.detail-audio {
  width: 100%;
  max-width: 400px;
  margin-top: 4px;
}

.detail-meta {
  margin-top: 14px;
  display: flex;
  gap: 8px;
}

.meta-tag {
  background: #f0f0f0;
  color: #666;
  font-size: 0.75rem;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 12px;
}

@media (max-width: 600px) {
  .record-info {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
  .part-tab { padding: 8px 14px; font-size: 0.85rem; }
}
</style>
