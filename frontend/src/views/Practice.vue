<template>
  <div class="practice">
    <h1>파트 선택</h1>
    <p class="subtitle">연습할 파트를 선택하세요</p>

    <div class="parts-list">
      <router-link
        v-for="part in store.parts"
        :key="part.id"
        :to="`/practice/${part.id}`"
        class="card part-item"
      >
        <div class="part-header">
          <span class="part-badge">Part {{ part.id }}</span>
          <span class="question-count">{{ part.questions.length }}문제</span>
        </div>
        <h2>{{ part.title }}</h2>
        <p>{{ part.description }}</p>
        <div class="part-meta">
          <span>준비시간: {{ part.prepTime }}초</span>
          <span>응답시간: {{ part.responseTime }}초</span>
        </div>
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { useSpeakingStore } from '../stores/speaking'
const store = useSpeakingStore()
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

.parts-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.part-item {
  text-decoration: none;
  color: inherit;
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
  display: flex;
  gap: 16px;
  color: #999;
  font-size: 0.85rem;
}
</style>
