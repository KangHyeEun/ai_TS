import { ref } from 'vue'

/**
 * 음성 녹음 및 STT(Speech-to-Text) 관련 로직
 */
export function useRecorder(store, currentPart, currentQuestionIdx) {
  const audioUrl = ref(null)
  const savedAudioUrl = ref(null)
  const lastAudioBlob = ref(null)
  const sttText = ref('')
  const sttProcessing = ref(false)

  let mediaRecorder = null
  let audioChunks = []
  let recognition = null
  let sttRestarting = false

  function startSTT() {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
    if (!SpeechRecognition) return

    sttText.value = ''
    sttRestarting = true
    recognition = new SpeechRecognition()
    recognition.lang = 'en-US'
    recognition.continuous = true
    recognition.interimResults = true
    recognition.maxAlternatives = 1

    let finalTranscript = ''

    recognition.onresult = (event) => {
      let interim = ''
      for (let i = event.resultIndex; i < event.results.length; i++) {
        if (event.results[i].isFinal) {
          finalTranscript += event.results[i][0].transcript + ' '
        } else {
          interim += event.results[i][0].transcript
        }
      }
      sttText.value = (finalTranscript + interim).trim()
    }

    recognition.onerror = (e) => {
      console.warn('STT 오류:', e.error)
      if (e.error === 'no-speech' && sttRestarting) {
        try { recognition.start() } catch (err) { /* ignore */ }
      }
    }

    recognition.onend = () => {
      if (sttRestarting) {
        try { recognition.start() } catch (e) { /* ignore */ }
      }
    }

    recognition.start()
  }

  function stopSTT() {
    sttRestarting = false
    if (recognition) {
      try { recognition.stop() } catch (e) { /* ignore */ }
    }
  }

  async function startRecording() {
    audioChunks = []
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      mediaRecorder = new MediaRecorder(stream)
      mediaRecorder.ondataavailable = (e) => audioChunks.push(e.data)
      mediaRecorder.onstop = async () => {
        const blob = new Blob(audioChunks, { type: 'audio/webm' })
        audioUrl.value = URL.createObjectURL(blob)
        lastAudioBlob.value = blob
        stream.getTracks().forEach(t => t.stop())

        // 서버에 오디오 업로드
        try {
          const formData = new FormData()
          formData.append('file', blob, 'recording.webm')
          formData.append('userId', store.currentUser?.userId || 1)
          formData.append('partNumber', currentPart.value.id)
          formData.append('questionIdx', currentQuestionIdx.value)
          const res = await fetch('/api/files/audio', { method: 'POST', body: formData })
          const data = await res.json()
          savedAudioUrl.value = data.url
        } catch (err) {
          console.warn('��디오 업로드 실패:', err)
        }
      }
      mediaRecorder.start()
      startSTT()
    } catch (err) {
      console.warn('마이크 접근 불가:', err)
    }
  }

  function stopRecording() {
    if (mediaRecorder && mediaRecorder.state === 'recording') {
      mediaRecorder.stop()
    }
    stopSTT()
  }

  function resetRecording() {
    audioUrl.value = null
    savedAudioUrl.value = null
    lastAudioBlob.value = null
    sttText.value = ''
  }

  function cleanup() {
    stopRecording()
  }

  return {
    audioUrl,
    savedAudioUrl,
    lastAudioBlob,
    sttText,
    sttProcessing,
    mediaRecorder: () => mediaRecorder,
    startSTT,
    stopSTT,
    startRecording,
    stopRecording,
    resetRecording,
    cleanup
  }
}
