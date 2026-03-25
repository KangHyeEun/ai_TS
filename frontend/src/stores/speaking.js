import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useSpeakingStore = defineStore('speaking', () => {
  // 토익스피킹 파트별 문제 데이터
  const parts = ref([
    {
      id: 1,
      title: 'Read a Text Aloud',
      description: '지문을 소리 내어 읽기',
      prepTime: 45,
      responseTime: 45,
      questions: [
        {
          id: 'p1-1', questionId: 1001,
          text: 'Welcome to the annual technology conference. We are pleased to announce that this year\'s event will feature over fifty speakers from around the world. Sessions will cover topics ranging from artificial intelligence to sustainable energy solutions. Please check the schedule posted near the registration desk for room assignments and times.'
        },
        {
          id: 'p1-2', questionId: 1002,
          text: 'Thank you for calling Greenfield Medical Center. Our office hours are Monday through Friday, from eight A.M. to six P.M. If you need to schedule an appointment, please press one. For prescription refills, press two. To speak with a nurse, press three. For all other inquiries, please stay on the line.'
        },
        {
          id: 'p1-3', questionId: 1003,
          text: 'Attention all passengers. Flight 472 to London Heathrow has been delayed by approximately thirty minutes due to weather conditions. We expect to begin boarding at gate B12 at three forty-five P.M. We apologize for the inconvenience and thank you for your patience.'
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
          id: 'p2-1', questionId: 1004,
          text: '사진을 보고 묘사해 주세요.',
          hint: '장소, 사람, 행동, 배경 등을 묘사하세요. "In this picture, I can see..." 로 시작하면 좋습니다.'
        },
        {
          id: 'p2-2', questionId: 1005,
          text: '사진을 보고 묘사해 주세요.',
          hint: '현재진행형 시제를 사용하세요. 위치 표현(on the left, in the background)을 활용하세요.'
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
          id: 'p3-set1', questionId: 1006,
          setTitle: '대중교통 이용',
          subQuestions: [
            { text: 'How often do you use public transportation?', prepTime: 3, responseTime: 15 },
            { text: 'What do you think are the main advantages of using public transportation compared to driving?', prepTime: 3, responseTime: 15 },
            { text: 'Some cities are investing heavily in improving public transportation systems. Do you think this is a good use of public funds? Why or why not?', prepTime: 3, responseTime: 30 }
          ]
        },
        {
          id: 'p3-set2', questionId: 1007,
          setTitle: '외식 습관',
          subQuestions: [
            { text: 'How often do you eat out at restaurants?', prepTime: 3, responseTime: 15 },
            { text: 'What is your favorite type of restaurant, and why do you enjoy eating there?', prepTime: 3, responseTime: 15 },
            { text: 'Some people prefer cooking at home while others prefer eating out. Which do you think is better for maintaining a healthy lifestyle, and why?', prepTime: 3, responseTime: 30 }
          ]
        }
      ]
    },
    {
      id: 4,
      title: 'Respond Using Information',
      description: '제공된 정보를 사용하여 질문에 답하기',
      prepTime: 3,
      responseTime: 15,
      questions: [
        {
          id: 'p4-1', questionId: 1008,
          text: 'You will see a schedule for a company workshop. Answer the questions using the information provided.',
          infoTitle: 'Annual Marketing Workshop',
          infoDetails: 'Date: March 15\nTime: 9:00 AM - 4:00 PM\nLocation: Conference Room A, 3rd Floor',
          infoSchedule: [
            { time: '9:00 - 10:30', content: 'Lecture: Brand Strategy Overview', speaker: 'Sarah Kim' },
            { time: '10:30 - 10:45', content: 'Break (Coffee & Refreshments)', speaker: '-' },
            { time: '10:45 - 12:00', content: 'Presentation: Social Media Trends', speaker: 'David Park' },
            { time: '12:00 - 1:00', content: 'Lunch (included in registration)', speaker: '-' },
            { time: '1:00 - 2:30', content: 'Workshop: Customer Engagement', speaker: 'Emily Chen' },
            { time: '2:30 - 4:00', content: 'Group discussion: Final Project', speaker: '-' }
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
      prepTime: 45,
      responseTime: 60,
      questions: [
        {
          id: 'p5-1', questionId: 1009,
          text: 'Some people believe that employees should be required to work in the office every day, while others think that remote work should be an option. Which do you prefer and why? Give specific reasons and examples to support your opinion.'
        },
        {
          id: 'p5-2', questionId: 1010,
          text: 'Do you agree or disagree with the following statement? "University students should be required to take courses outside of their major field of study." Use specific reasons and examples to support your answer.'
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
          partId: record.partId,
          partTitle: record.partTitle,
          questionIdx: record.questionIdx
        })
      })
      await fetchRecords()
    } catch (err) {
      console.error('기록 추가 실패:', err)
    }
  }

  async function clearRecords() {
    try {
      await fetch('/api/records', { method: 'DELETE' })
      records.value = []
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
      return data.responseId
    } catch (err) {
      console.error('응답 저장 실패:', err)
      return null
    }
  }

  // 앱 시작 시 사용자 정보 + 기록 로드
  fetchCurrentUser()
  fetchRecords()

  return { parts, currentUser, records, totalPracticeCount, todayPracticeCount, fetchCurrentUser, fetchRecords, addRecord, clearRecords, saveUserResponse }
})
