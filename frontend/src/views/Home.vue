<template>
  <div class="home">
    <section class="hero card">
      <h1>TOEIC Speaking 연습</h1>
      <p>토익스피킹 시험을 효과적으로 준비하세요.<br/>파트별 연습과 음성 녹음으로 실전처럼 학습할 수 있습니다.</p>
      <div class="hero-actions">
        <router-link to="/practice">
          <button class="btn-primary btn-large">파트별 연습</button>
        </router-link>
        <router-link to="/full-test">
          <button class="btn-primary btn-large btn-test">모의고사 응시</button>
        </router-link>
      </div>
    </section>

    <section class="stats">
      <div class="card stat-card">
        <div class="stat-number">{{ store.totalPracticeCount }}</div>
        <div class="stat-label">총 연습 횟수</div>
      </div>
      <div class="card stat-card">
        <div class="stat-number">{{ store.todayPracticeCount }}</div>
        <div class="stat-label">오늘 연습</div>
      </div>
      <div class="card stat-card">
        <div class="stat-number">5</div>
        <div class="stat-label">파트 구성</div>
      </div>
    </section>

    <section class="parts-overview">
      <h2>파트별 구성</h2>
      <div class="parts-grid">
        <div class="card part-card" v-for="part in store.parts" :key="part.id">
          <div class="part-number">Part {{ part.id }}</div>
          <h3>{{ part.title }}</h3>
          <p>{{ part.description }}</p>
          <p class="part-time">준비 {{ part.prepTime }}초 / 응답 {{ part.responseTime }}초</p>
          <router-link :to="`/practice/${part.id}`">
            <button class="btn-secondary">연습하기</button>
          </router-link>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { useSpeakingStore } from '../stores/speaking'
const store = useSpeakingStore()
</script>

<style scoped>
.hero {
  text-align: center;
  padding: 48px 24px;
  margin-bottom: 24px;
  background: linear-gradient(135deg, #4A90D9, #357ABD);
  color: #fff;
}

.hero h1 {
  font-size: 2rem;
  margin-bottom: 12px;
}

.hero p {
  font-size: 1.1rem;
  opacity: 0.9;
  margin-bottom: 24px;
}

.hero-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  flex-wrap: wrap;
}

.btn-large {
  padding: 14px 36px;
  font-size: 1.1rem;
  background: #fff;
  color: #4A90D9;
  font-weight: 600;
}

.btn-test {
  background: linear-gradient(135deg, #e74c3c, #c0392b);
  color: #fff;
}

.stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

.stat-card {
  text-align: center;
  padding: 20px;
}

.stat-number {
  font-size: 2rem;
  font-weight: 700;
  color: #4A90D9;
}

.stat-label {
  color: #888;
  margin-top: 4px;
}

.parts-overview h2 {
  margin-bottom: 16px;
  font-size: 1.3rem;
}

.parts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 16px;
}

.part-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.part-number {
  font-size: 0.85rem;
  font-weight: 600;
  color: #4A90D9;
  text-transform: uppercase;
}

.part-card h3 {
  font-size: 1.1rem;
}

.part-card p {
  color: #666;
  font-size: 0.95rem;
}

.part-time {
  font-size: 0.85rem !important;
  color: #999 !important;
}

.part-card button {
  align-self: flex-start;
  margin-top: auto;
}

@media (max-width: 600px) {
  .stats {
    grid-template-columns: 1fr;
  }
  .parts-grid {
    grid-template-columns: 1fr;
  }
}
</style>
