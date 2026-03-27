import { ref } from 'vue'

/**
 * AI 모범답안, 피드백, 번역 관련 로직
 */
export function useAiFeedback(store, currentPart, currentQuestion) {
  const aiLoading = ref(false)
  const aiMode = ref('')
  const aiResult = ref('')
  const feedbackData = ref(null)  // 구조화된 피드백 JSON 데이터
  const cachedSampleAnswer = ref('')
  const cachedFeedback = ref(null)
  const userAnswer = ref('')
  const showFeedbackArea = ref(false)
  const translationResult = ref('')
  const translating = ref(false)
  const lastResponseId = ref(null)

  // JSON 문자열에서 구조화 데이터 추출 시도
  function tryParseJson(raw) {
    if (!raw) return null
    try {
      let text = raw.trim()
      // 마크다운 코드블록 제거
      if (text.startsWith('```')) text = text.replace(/```(?:json)?\s*/g, '').replace(/```\s*$/g, '').trim()
      // { } 블록 추출
      const start = text.indexOf('{')
      const end = text.lastIndexOf('}')
      if (start >= 0 && end > start) {
        const jsonStr = text.substring(start, end + 1)
        const parsed = JSON.parse(jsonStr)
        if (parsed.estimatedScore !== undefined) return parsed
      }
    } catch (e) { /* 무시 */ }
    return null
  }

  // 번역 JSON 파싱 시도
  function tryParseTranslation(raw) {
    if (!raw) return null
    try {
      let text = raw.trim()
      if (text.startsWith('```')) text = text.replace(/```(?:json)?\s*/g, '').replace(/```\s*$/g, '').trim()
      const start = text.indexOf('{')
      const end = text.lastIndexOf('}')
      if (start >= 0 && end > start) {
        const parsed = JSON.parse(text.substring(start, end + 1))
        if (parsed.translation || parsed.improved) return parsed
      }
    } catch (e) { /* 무시 */ }
    return null
  }

  function getErrorMessage(status, serverMsg) {
    // API 요청 한도 초과
    if (status === 500 && serverMsg && serverMsg.includes('요청 한도 초과')) {
      return 'AI 요청이 많아 일시적으로 처리할 수 없습니다. 1분 후 다시 시도해 주세요.'
    }
    if (status === 500 && serverMsg && serverMsg.includes('API')) {
      return 'AI 서비스에 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.'
    }
    // 서버 내부 오류
    if (status === 500) {
      return '서버에서 오류가 발생했습니다. 잠시 후 다시 시도해 주세요. 계속 동일한 문제 발생 시 관리자에게 문의하세요.'
    }
    // 403 Forbidden
    if (status === 403) {
      return '요청이 거부되었습니다. 페이지를 새로고침한 후 다시 시도해 주세요.'
    }
    // 네트워크 오류 (서버 미실행)
    if (status === 0) {
      if (serverMsg) return serverMsg
      return '서버에 연결할 수 없습니다. 인터넷 연결을 확인하거나, 계속 문제가 발생하면 관리자에게 문의하세요.'
    }
    // 기타
    return '피드백을 가져오지 못했습니다. 잠시 후 다시 시도해 주세요.'
  }

  async function getSampleAnswer() {
    if (cachedSampleAnswer.value) {
      aiMode.value = 'sample'
      aiResult.value = cachedSampleAnswer.value
      return
    }

    aiLoading.value = true
    aiMode.value = 'sample'
    aiResult.value = ''
    try {
      const q = currentQuestion.value
      const part = currentPart.value
      const res = await fetch('/api/gemini/sample-answer', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          partId: part.id,
          questionText: part.id === 2 && q.hint
            ? q.text + '\n[Scene Description]: ' + q.hint
            : q.text,
          info: q.info || null,
          subQuestions: q.subQuestions || null,
          targetScore: store.currentUser?.targetScore || 130,
          responseTime: q.responseTime || part.responseTime
        })
      })
      if (!res.ok) throw new Error(`서버 응답 오류: ${res.status}`)
      const data = await res.json()
      aiResult.value = data.answer
      cachedSampleAnswer.value = data.answer
    } catch (err) {
      const msg = err?.message || ''
      if (msg.includes('500')) {
        aiResult.value = 'AI 요청이 많아 일시적으로 처리할 수 없습니다. 1분 후 다시 시도해 주세요.'
      } else {
        aiResult.value = '응답을 가져오지 못했습니다. 잠시 후 다시 시도해 주세요.'
      }
    } finally {
      aiLoading.value = false
    }
  }

  function requestFeedback(isSetPart, subAnswers, sttText, allSetQuestions, questionInfo) {
    if (cachedFeedback.value) {
      showFeedbackArea.value = false
      aiMode.value = 'feedback'
      if (cachedFeedback.value.parseError) {
        aiResult.value = cachedFeedback.value.raw
        feedbackData.value = null
      } else {
        feedbackData.value = cachedFeedback.value
        aiResult.value = ''
      }
      return
    }

    if (isSetPart && subAnswers.length > 0) {
      getFeedback(isSetPart, subAnswers, sttText, allSetQuestions, questionInfo)
      return
    }

    showFeedbackArea.value = true
  }

  async function getFeedback(isSetPart, subAnswers, sttText, allSetQuestions, questionInfo) {
    if (cachedFeedback.value) {
      aiMode.value = 'feedback'
      if (cachedFeedback.value.parseError) {
        aiResult.value = cachedFeedback.value.raw
        feedbackData.value = null
      } else {
        feedbackData.value = cachedFeedback.value
        aiResult.value = ''
      }
      return
    }

    let answer = ''
    let questionText = ''
    let subQuestionPayload = null

    if (isSetPart && subAnswers.length > 0) {
      // 세트형: 문제별 질문+답변+응답시간을 구조화
      subQuestionPayload = subAnswers.map((ans, i) => {
        const setQ = allSetQuestions ? allSetQuestions[i] : null
        return {
          question: ans.text || (setQ ? setQ.text : `Q${i + 1}`),
          answer: ans.sttText || '',
          responseTime: setQ ? setQ.responseTime : 15
        }
      })
      questionText = subAnswers.map((ans, i) => `Q${i + 1}: ${ans.text}`).join('\n')
      answer = subAnswers.map((ans, i) => `Q${i + 1}: ${ans.sttText || '(답변 없음)'}`).join('\n')
    } else {
      answer = sttText || userAnswer.value || '(답변 없음)'
      const q = currentQuestion.value
      questionText = q?.text || '질문 없음'
      if (currentPart.value.id === 2 && q?.hint) {
        questionText = questionText + '\n[Scene Description]: ' + q.hint
      }
    }

    function saveAllFeedback(fbData) {
      if (!lastResponseId.value || !store.saveEvaluation) return
      console.log('[saveAllFeedback] keys:', Object.keys(fbData).filter(k => k !== 'raw'))
      store.saveEvaluation({
        responseId: lastResponseId.value,
        score: fbData.estimatedScore || null,
        scoreComment: fbData.scoreComment || null,
        targetAnalysis: fbData.targetAnalysis || null,
        targetTips: fbData.targetTips ? JSON.stringify(fbData.targetTips) : null,
        strengthsText: fbData.strengths ? JSON.stringify(fbData.strengths) : null,
        feedbackText: fbData.improvements ? JSON.stringify(fbData.improvements) : null,
        correctedAnswers: fbData.correctedAnswers ? JSON.stringify(fbData.correctedAnswers) : null,
        keyExpressions: fbData.keyExpressions ? JSON.stringify(fbData.keyExpressions) : null
      })
    }

    aiLoading.value = true
    aiMode.value = 'feedback'
    aiResult.value = ''
    feedbackData.value = null
    showFeedbackArea.value = false
    try {
      const payload = {
        partId: currentPart.value.id,
        questionText,
        userAnswer: answer,
        targetScore: store.currentUser?.targetScore || 130,
        responseTime: currentQuestion.value?.responseTime || currentPart.value.responseTime,
        subQuestions: subQuestionPayload,
        info: questionInfo || null
      }
      const res = await fetch('/api/gemini/feedback', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      })
      if (!res.ok) {
        // 서버 에러 응답에서 메시지 추출
        let serverMsg = ''
        try { const errData = await res.json(); serverMsg = errData.error || '' } catch(e) {}
        throw { status: res.status, serverMsg }
      }
      const data = await res.json()
      if (data.error) {
        aiResult.value = getErrorMessage(0, data.error)
      } else if (data.parseError) {
        // 백엔드 파싱 실패 → 프론트에서 재시도
        const parsed = tryParseJson(data.raw)
        if (parsed) {
          feedbackData.value = parsed
          cachedFeedback.value = parsed
          aiResult.value = ''
          saveAllFeedback(parsed)
        } else {
          aiResult.value = data.raw
          cachedFeedback.value = data
        }
      } else {
        // 구조화된 피드백 데이터
        feedbackData.value = data
        cachedFeedback.value = data
        aiResult.value = ''
        saveAllFeedback(data)
      }
    } catch (err) {
      console.error('피드백 요청 실패:', err)
      const status = err?.status || 0
      const serverMsg = err?.serverMsg || ''
      aiResult.value = getErrorMessage(status, serverMsg)
    } finally {
      aiLoading.value = false
    }
  }

  const translationData = ref(null) // 구조화된 번역 데이터

  async function submitKoreanAnswer(koreanAnswer, questionText, partId, practiceMode, targetScore, responseTime) {
    if (!koreanAnswer.trim()) return
    translating.value = true
    translationResult.value = ''
    translationData.value = null
    try {
      const res = await fetch('/api/gemini/translate-to-english', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          koreanText: koreanAnswer,
          questionText,
          partId: String(partId),
          targetScore: String(targetScore || 130),
          responseTime: String(responseTime || 30)
        })
      })
      if (!res.ok) throw new Error(`서버 응답 오류: ${res.status}`)
      const data = await res.json()
      if (data.parseError) {
        // 파싱 실패 → 프론트에서 JSON 재파싱 시도
        const raw = data.translation || data.raw || ''
        const parsed = tryParseTranslation(raw)
        if (parsed) {
          translationData.value = parsed
        } else {
          translationResult.value = raw
        }
      } else if (data.translation && typeof data.translation === 'string' && !data.improved) {
        // 구조화 안 된 단순 문자열 → 재파싱 시도
        const parsed = tryParseTranslation(data.translation)
        if (parsed) {
          translationData.value = parsed
        } else {
          translationResult.value = data.translation
        }
      } else {
        // 구조화 데이터
        translationData.value = data
      }
    } catch (err) {
      translationResult.value = '번역을 가져오지 못했습니다. 잠시 후 다시 시도해 주세요.'
    } finally {
      translating.value = false
    }
  }

  function setResponseId(id) {
    lastResponseId.value = id
  }

  function resetAi() {
    aiResult.value = ''
    aiMode.value = ''
    userAnswer.value = ''
    showFeedbackArea.value = false
    cachedSampleAnswer.value = ''
    cachedFeedback.value = null
    feedbackData.value = null
    translationResult.value = ''
    translationData.value = null
    lastResponseId.value = null
  }

  return {
    aiLoading,
    aiMode,
    aiResult,
    feedbackData,
    cachedSampleAnswer,
    cachedFeedback,
    userAnswer,
    showFeedbackArea,
    translationResult,
    translationData,
    translating,
    lastResponseId,
    getSampleAnswer,
    requestFeedback,
    getFeedback,
    submitKoreanAnswer,
    setResponseId,
    resetAi
  }
}
