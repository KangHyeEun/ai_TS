import { ref, computed } from 'vue'

/**
 * Part 1 발음 분석 및 TTS 관련 로직
 */
export function usePronunciation() {
  const pronunciationResult = ref(null)
  const pronunciationLoading = ref(false)
  const isSpeaking = ref(false)

  const pronScoreClass = computed(() => {
    if (!pronunciationResult.value) return ''
    const s = pronunciationResult.value.score
    if (s >= 90) return 'score-great'
    if (s >= 70) return 'score-good'
    if (s >= 50) return 'score-fair'
    return 'score-poor'
  })

  async function sendPronunciationAnalysis(originalText, recognizedText) {
    try {
      const res = await fetch('/api/gemini/analyze-pronunciation', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ originalText, sttText: recognizedText })
      })
      pronunciationResult.value = await res.json()
    } catch (err) {
      pronunciationResult.value = {
        score: 0,
        overallFeedback: '발음 분석에 실패했습니다.',
        incorrectWords: []
      }
    } finally {
      pronunciationLoading.value = false
    }
  }

  function startPronunciationAnalysis(questionText, sttText, stopSTTFn) {
    if (stopSTTFn) stopSTTFn()
    pronunciationResult.value = null
    pronunciationLoading.value = true

    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
    if (!SpeechRecognition) {
      pronunciationLoading.value = false
      pronunciationResult.value = {
        score: 0,
        overallFeedback: '이 브라우저는 음성 인식을 지원하�� 않습니다. Chrome을 사용해주세요.',
        incorrectWords: []
      }
      return
    }

    if (sttText) {
      sendPronunciationAnalysis(questionText, sttText)
    } else {
      pronunciationLoading.value = false
      pronunciationResult.value = {
        score: 0,
        overallFeedback: '음성 인식 결과가 없습니다. Chrome 브라우저에서 다시 시도해주세요.',
        incorrectWords: []
      }
    }
  }

  function speakWord(word) {
    const utterance = new SpeechSynthesisUtterance(word)
    utterance.lang = 'en-US'
    utterance.rate = 0.8
    speechSynthesis.speak(utterance)
  }

  function speakFullText(text) {
    if (!text) return
    speechSynthesis.cancel()
    const utterance = new SpeechSynthesisUtterance(text)
    utterance.lang = 'en-US'
    utterance.rate = 0.9
    utterance.onstart = () => { isSpeaking.value = true }
    utterance.onend = () => { isSpeaking.value = false }
    speechSynthesis.speak(utterance)
  }

  function stopSpeaking() {
    speechSynthesis.cancel()
    isSpeaking.value = false
  }

  function resetPronunciation() {
    pronunciationResult.value = null
    pronunciationLoading.value = false
  }

  return {
    pronunciationResult,
    pronunciationLoading,
    isSpeaking,
    pronScoreClass,
    startPronunciationAnalysis,
    speakWord,
    speakFullText,
    stopSpeaking,
    resetPronunciation
  }
}
