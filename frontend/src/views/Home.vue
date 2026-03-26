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
        <div class="stat-number">{{ store.todayStats.completedQuestionsCount || 0 }}</div>
        <div class="stat-label">오늘 완료 문제</div>
      </div>
      <div class="card stat-card">
        <div class="stat-number avg-score" :class="scoreColorClass">
          {{ displayAvgScore }}
        </div>
        <div class="stat-label">오늘 평균 점수</div>
      </div>
    </section>

    <!-- 주간 학습 현황 -->
    <section v-if="store.weeklyStats.length > 0" class="weekly-section">
      <h2>주간 학습 현황</h2>
      <div class="weekly-chart">
        <div class="chart-bar-group" v-for="day in weeklyChartData" :key="day.date">
          <div class="chart-bar-wrapper">
            <div class="chart-bar" :style="{ height: day.barHeight + '%' }">
              <span class="chart-count" v-if="day.count > 0">{{ day.count }}</span>
            </div>
          </div>
          <div class="chart-label">{{ day.label }}</div>
        </div>
      </div>
    </section>

  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useSpeakingStore } from '../stores/speaking'
const store = useSpeakingStore()

const displayAvgScore = computed(() => {
  const score = store.todayStats.averageScore
  if (!score || score === 0) return '-'
  return typeof score === 'number' ? score.toFixed(0) : parseFloat(score).toFixed(0)
})

const scoreColorClass = computed(() => {
  const score = parseFloat(store.todayStats.averageScore) || 0
  if (score === 0) return ''
  if (score >= 160) return 'score-high'
  if (score >= 110) return 'score-mid'
  return 'score-low'
})

const dayNames = ['일', '월', '화', '수', '목', '금', '토']

const weeklyChartData = computed(() => {
  const data = []
  const statsMap = {}
  for (const s of store.weeklyStats) {
    statsMap[s.studyDate] = s.completedQuestionsCount || 0
  }

  const maxCount = Math.max(1, ...Object.values(statsMap))

  for (let i = 6; i >= 0; i--) {
    const d = new Date()
    d.setDate(d.getDate() - i)
    const dateStr = d.toISOString().split('T')[0]
    const count = statsMap[dateStr] || 0
    data.push({
      date: dateStr,
      label: dayNames[d.getDay()],
      count,
      barHeight: Math.max(count > 0 ? 8 : 0, (count / maxCount) * 100)
    })
  }
  return data
})
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


.avg-score { font-size: 1.8rem; }
.score-high { color: #27ae60; }
.score-mid { color: #e67e22; }
.score-low { color: #e74c3c; }

.weekly-section {
  margin-bottom: 32px;
}

.weekly-section h2 {
  margin-bottom: 16px;
  font-size: 1.3rem;
}

.weekly-chart {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  padding: 24px 16px 16px;
  height: 180px;
}

.chart-bar-group {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.chart-bar-wrapper {
  height: 120px;
  width: 100%;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.chart-bar {
  width: 28px;
  background: linear-gradient(180deg, #4A90D9, #357ABD);
  border-radius: 4px 4px 0 0;
  min-height: 0;
  transition: height 0.5s ease;
  position: relative;
  display: flex;
  justify-content: center;
}

.chart-count {
  position: absolute;
  top: -20px;
  font-size: 0.75rem;
  font-weight: 700;
  color: #4A90D9;
}

.chart-label {
  font-size: 0.8rem;
  color: #888;
  font-weight: 500;
}

@media (max-width: 600px) {
  .stats {
    grid-template-columns: 1fr;
  }
  .parts-grid {
    grid-template-columns: 1fr;
  }
  .chart-bar { width: 20px; }
}
</style>
