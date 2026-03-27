import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useSpeakingStore = defineStore('speaking', () => {
  // 토익스피킹 파트별 문제 데이터
  // 파트별 문제 데이터 (DB 표준 형식과 동일)
  // 서버에서 문제를 가져오지 못할 때 폴백으로 사용
  const parts = ref([
    {
      id: 1,
      title: 'Read a Text Aloud',
      description: '지문을 소리 내어 읽기',
      prepTime: 45,
      responseTime: 45,
      questions: [
        {
          id: 'p1-1',
          text: 'Welcome to the annual technology conference. We are pleased to announce that this year\'s event will feature over fifty speakers from around the world. Sessions will cover topics ranging from artificial intelligence to sustainable energy solutions. Please check the schedule posted near the registration desk for room assignments and times.'
        }
      ]
    },
    {
      id: 2,
      title: 'Describe a Picture',
      description: '사진 묘사하기',
      prepTime: 45,
      responseTime: 30,
      questions: [
        {
          id: 'p2-1',
          text: 'Describe the picture below.',
          hint: 'A bright, modern co-working space with large windows letting in natural light. In the foreground, a woman wearing a blue blazer is typing on a laptop while talking on the phone. Behind her, two men in casual shirts are standing near a whiteboard, pointing at a chart and discussing. On the right side, a young man with headphones is sitting on a bean bag, reading something on a tablet. Several potted plants are placed around the room, and coffee cups are visible on the desks.',
          imageUrl: 'https://images.pexels.com/photos/3184292/pexels-photo-3184292.jpeg?auto=compress&cs=tinysrgb&w=800'
        }
      ]
    },
    {
      id: 3,
      title: 'Respond to Questions',
      description: '듣고, 질문에 답하기',
      prepTime: 3,
      responseTime: 15,
      questions: [
        {
          id: 'p3-set1',
          setTitle: '대중교통 이용',
          subQuestions: [
            { text: 'How often do you use public transportation?', prepTime: 3, responseTime: 15 },
            { text: 'What do you think are the main advantages of using public transportation compared to driving?', prepTime: 3, responseTime: 15 },
            { text: 'Some cities are investing heavily in improving public transportation systems. Do you think this is a good use of public funds? Why or why not?', prepTime: 3, responseTime: 30 }
          ]
        }
      ]
    },
    {
      id: 4,
      title: 'Respond Using Information',
      description: '제공된 정보를 사용하여 질문에 답하기',
      prepTime: 45,
      responseTime: 30,
      questions: [
        {
          id: 'p4-1',
          text: 'You will see a schedule for a company workshop. Answer the questions using the information provided.',
          infoTitle: 'Annual Marketing Workshop',
          infoDetails: 'Date: March 15\nTime: 9:00 AM - 4:00 PM\nLocation: Conference Room A, 3rd Floor',
          infoSchedule: [
            { time: '9:00 - 10:30', content: 'Lecture: Brand Strategy Overview', speaker: 'Sarah Kim' },
            { time: '10:30 - 10:45', content: 'Break (Coffee & Refreshments)', speaker: '-' },
            { time: '10:45 - 12:00', content: 'Presentation: Social Media Trends', speaker: 'David Park' },
            { time: '12:00 - 1:00', content: 'Lunch (included in registration)', speaker: '-' },
            { time: '1:00 - 2:30', content: 'Workshop: Customer Engagement', speaker: 'Emily Chen' },
            { time: '2:30 - 4:00', content: 'Group Discussion: Final Project', speaker: '-' }
          ],
          subQuestions: [
            { text: 'What time does the workshop begin and where is it being held?', prepTime: 3, responseTime: 15 },
            { text: 'What topic is covered right after the coffee break?', prepTime: 3, responseTime: 15 },
            { text: 'I heard the group project is in the morning. Is that correct?', prepTime: 3, responseTime: 30 }
          ]
        }
      ]
    },
    {
      id: 5,
      title: 'Express an Opinion',
      description: '의견 제시하기',
      prepTime: 30,
      responseTime: 60,
      questions: [
        {
          id: 'p5-1',
          text: 'Some people believe that employees should be required to work in the office every day, while others think that remote work should be an option. Which do you prefer and why? Give specific reasons and examples to support your opinion.'
        }
      ]
    }
  ])

  // 현재 로그인 사용자
  const currentUser = ref(null)

  async function fetchCurrentUser() {
    try {
      const res = await fetch('/api/user/current')
      currentUser.value = await res.json()
    } catch (err) {
      console.error('사용자 정보 조회 실패:', err)
    }
  }

  // 연습 기록
  const records = ref([])

  const totalPracticeCount = computed(() => records.value.length)

  const todayPracticeCount = computed(() => {
    const today = new Date().toDateString()
    return records.value.filter(r => new Date(r.createdAt).toDateString() === today).length
  })

  async function fetchRecords() {
    try {
      const res = await fetch('/api/records')
      records.value = await res.json()
    } catch (err) {
      console.error('기록 조회 실패:', err)
    }
  }

  async function addRecord(record) {
    try {
      await fetch('/api/records', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          userId: currentUser.value?.userId || 1,
          questionId: record.questionId || null,
          partId: record.partId,
          partTitle: record.partTitle,
          practiceMode: record.practiceMode || 'REAL'
        })
      })
      await fetchRecords()
    } catch (err) {
      console.error('기록 추가 실패:', err)
    }
  }

  async function clearRecords() {
    try {
      const res = await fetch('/api/records', { method: 'DELETE' })
      if (res.ok) {
        records.value = []
        // 학습 통계도 리셋
        todayStats.value = { completedQuestionsCount: 0, averageScore: 0 }
        weeklyStats.value = []
      } else {
        console.error('기록 삭제 실패:', res.status)
      }
    } catch (err) {
      console.error('기록 삭제 실패:', err)
    }
  }

  // 사용자 응답 저장 (user_responses 테이블)
  async function saveUserResponse({ questionId, practiceMode, audioFilePath, sttText, textAnswer }) {
    try {
      const res = await fetch('/api/responses', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          userId: currentUser.value?.userId || 1,
          questionId: questionId,
          practiceMode: practiceMode || 'REAL',
          audioFilePath: audioFilePath || null,
          sttText: sttText || null,
          textAnswer: textAnswer || null
        })
      })
      const data = await res.json()
      // 학습 통계 업데이트 (문제 완료 수 증가)
      incrementStudyStats()
      return data.responseId
    } catch (err) {
      console.error('응답 저장 실패:', err)
      return null
    }
  }

  // ========== Evaluation (AI 평가 저장) ==========
  async function saveEvaluation(evalData) {
    try {
      const res = await fetch('/api/evaluations', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(evalData)
      })
      if (!res.ok) return null
      const data = await res.json()
      if (evalData.score != null) {
        updateStudyScore(evalData.score)
      }
      return data.evaluationId
    } catch (err) {
      console.error('평가 저장 실패:', err)
      return null
    }
  }

  async function getEvaluationsByResponse(responseId) {
    try {
      const res = await fetch(`/api/evaluations/response/${responseId}`)
      if (!res.ok) return []
      return await res.json()
    } catch (err) {
      console.error('평가 조회 실패:', err)
      return []
    }
  }

  // ========== StudyStats (학습 통계) ==========
  const todayStats = ref({ completedQuestionsCount: 0, averageScore: 0 })
  const weeklyStats = ref([])

  async function fetchTodayStats() {
    try {
      const userId = currentUser.value?.userId || 1
      const res = await fetch(`/api/stats/${userId}/today`)
      if (!res.ok) return
      todayStats.value = await res.json()
    } catch (err) {
      console.error('오늘 통계 조회 실패:', err)
    }
  }

  async function fetchWeeklyStats() {
    try {
      const userId = currentUser.value?.userId || 1
      const end = new Date()
      const start = new Date()
      start.setDate(start.getDate() - 6)
      const startStr = start.toISOString().split('T')[0]
      const endStr = end.toISOString().split('T')[0]
      const res = await fetch(`/api/stats/${userId}/range?startDate=${startStr}&endDate=${endStr}`)
      if (!res.ok) return
      weeklyStats.value = await res.json()
    } catch (err) {
      console.error('주간 통계 조회 실패:', err)
    }
  }

  async function incrementStudyStats() {
    try {
      const userId = currentUser.value?.userId || 1
      const res = await fetch(`/api/stats/${userId}/increment`, { method: 'POST' })
      if (res.ok) await fetchTodayStats()
    } catch (err) {
      console.error('통계 증가 실패:', err)
    }
  }

  async function updateStudyScore(score) {
    try {
      const userId = currentUser.value?.userId || 1
      const res = await fetch(`/api/stats/${userId}/score`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ score })
      })
      if (res.ok) await fetchTodayStats()
    } catch (err) {
      console.error('점수 업데이트 실패:', err)
    }
  }

  // 앱 시작 시 사용자 정보 + 기록 + 통계 로드
  fetchCurrentUser().then(() => {
    fetchTodayStats()
    fetchWeeklyStats()
  })
  fetchRecords()

  return {
    parts, currentUser, records,
    totalPracticeCount, todayPracticeCount,
    todayStats, weeklyStats,
    fetchCurrentUser, fetchRecords, addRecord, clearRecords, saveUserResponse,
    saveEvaluation, getEvaluationsByResponse,
    fetchTodayStats, fetchWeeklyStats, incrementStudyStats, updateStudyScore
  }
})
