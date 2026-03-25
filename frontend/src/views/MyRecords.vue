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

    <div v-else class="records-list">
      <div class="card record-item" v-for="(record, idx) in store.records" :key="idx">
        <div class="record-info">
          <span class="record-part">Part {{ record.partId }}</span>
          <span class="record-title">{{ record.partTitle }}</span>
          <span class="record-q">문제 {{ record.questionIdx + 1 }}</span>
        </div>
        <div class="record-date">{{ formatDate(record.createdAt) }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useSpeakingStore } from '../stores/speaking'

const store = useSpeakingStore()

function formatDate(dateStr) {
  const d = new Date(dateStr)
  const month = d.getMonth() + 1
  const day = d.getDate()
  const hours = d.getHours().toString().padStart(2, '0')
  const minutes = d.getMinutes().toString().padStart(2, '0')
  return `${month}/${day} ${hours}:${minutes}`
}

function confirmClear() {
  if (confirm('모든 연습 기록을 삭제하시겠습니까?')) {
    store.clearRecords()
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

.records-header h1 {
  font-size: 1.5rem;
}

.empty {
  text-align: center;
  padding: 48px 24px;
}

.empty p {
  color: #888;
  margin-bottom: 16px;
}

.records-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.record-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
}

.record-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.record-part {
  background: #4A90D9;
  color: #fff;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 0.8rem;
  font-weight: 600;
}

.record-title {
  font-weight: 500;
}

.record-q {
  color: #999;
  font-size: 0.85rem;
}

.record-date {
  color: #999;
  font-size: 0.85rem;
}

@media (max-width: 600px) {
  .record-info {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
}
</style>
