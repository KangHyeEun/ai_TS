<template>
  <!-- Part 4: 구조화된 3열 표 (infoSchedule 있을 때) -->
  <div v-if="question.hasSchedule" class="info-table-area">
    <div class="info-table-title">{{ question.infoTitle }}</div>
    <div class="info-table-details">{{ question.infoDetails }}</div>
    <table class="schedule-table">
      <thead>
        <tr>
          <th class="col-time">시간</th>
          <th class="col-content">내용</th>
          <th class="col-speaker">담당</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(row, i) in question.infoSchedule" :key="i">
          <td class="time-cell">{{ row.time }}</td>
          <td v-html="formatContent(row.content)"></td>
          <td class="speaker-cell">{{ row.speaker }}</td>
        </tr>
      </tbody>
    </table>
  </div>
  <!-- 텍스트 info (마크다운 테이블 포함) -->
  <div v-else-if="question.info" class="info-table-area">
    <div class="info-table-header">제공된 정보</div>
    <div class="info-table" v-html="formatInfoToTable(question.info)"></div>
  </div>
</template>

<script setup>
defineProps({
  question: { type: Object, required: true }
})

function formatContent(text) {
  if (!text) return ''
  return text.replace(/^((?:Lecture|Presentation|Group discussion|Workshop|Keynote|Panel|Lunch|Break|Welcome|Opening|Closing|Registration|Meditation|Q&A|Healthy|Gentle|Nutrition)[^:]*:?)/i,
    '<strong>$1</strong>')
}

function formatInfoToTable(info) {
  if (!info) return ''
  const lines = info.split('\n').map(l => l.trim()).filter(l => l)

  // 마크다운 테이블 감지: | 로 시작하는 줄이 있는지
  const mdTableLines = lines.filter(l => l.startsWith('|') && l.endsWith('|'))
  if (mdTableLines.length >= 2) {
    // 테이블이 아닌 줄 = 상단 헤더 정보 (제목, 날짜, 장소 등)
    const nonTableLines = lines.filter(l => !(l.startsWith('|') && l.endsWith('|')))
    let html = ''
    if (nonTableLines.length > 0) {
      html += '<div class="info-header-section">'
      for (const h of nonTableLines) {
        const colonIdx = h.indexOf(':')
        if (colonIdx > 0 && colonIdx < 30) {
          const key = h.substring(0, colonIdx).trim()
          const val = h.substring(colonIdx + 1).trim()
          html += `<div class="info-meta"><span class="info-key">${key}</span><span class="info-val">${val}</span></div>`
        } else {
          html += `<div class="info-title">${h}</div>`
        }
      }
      html += '</div>'
    }
    html += parseMarkdownTable(mdTableLines)
    return html
  }

  // 기존 파싱 (시간표 형태)
  const headerLines = []
  const scheduleLines = []

  for (const line of lines) {
    if (/\d{1,2}:\d{2}/.test(line) && /[-–~]/.test(line)) {
      scheduleLines.push(line)
    } else {
      headerLines.push(line)
    }
  }

  let html = ''

  if (headerLines.length > 0) {
    html += '<div class="info-header-section">'
    for (const h of headerLines) {
      const colonIdx = h.indexOf(':')
      if (colonIdx > 0 && colonIdx < 30) {
        const key = h.substring(0, colonIdx).trim()
        const val = h.substring(colonIdx + 1).trim()
        html += `<div class="info-meta"><span class="info-key">${key}</span><span class="info-val">${val}</span></div>`
      } else {
        html += `<div class="info-title">${h}</div>`
      }
    }
    html += '</div>'
  }

  if (scheduleLines.length > 0) {
    html += '<table class="schedule-table"><thead><tr><th>시간</th><th>내용</th></tr></thead><tbody>'
    for (const line of scheduleLines) {
      const match = line.match(/^(\d{1,2}:\d{2}[\s]*(?:AM|PM)?[\s]*[-–~][\s]*\d{1,2}:\d{2}[\s]*(?:AM|PM)?)\s+(.+)$/i)
      if (match) {
        html += `<tr><td class="time-cell">${match[1].trim()}</td><td>${match[2].trim()}</td></tr>`
      } else {
        html += `<tr><td colspan="2">${line}</td></tr>`
      }
    }
    html += '</tbody></table>'
  }

  if (scheduleLines.length === 0 && headerLines.length === 0) {
    html = `<pre class="info-raw">${info}</pre>`
  }

  return html
}

/**
 * 마크다운 테이블 → 3열 표(시간/내용/담당)로 변환
 */
function parseMarkdownTable(mdLines) {
  // 구분선(---|---) 제거
  const dataLines = mdLines.filter(l => {
    // 구분선 제거: | --- | :--- | :---: | 등
    const inner = l.replace(/^\|/, '').replace(/\|$/, '')
    return !inner.split('|').every(cell => /^[\s:*-]+$/.test(cell))
  })
  if (dataLines.length === 0) return ''

  const headerCells = parseMdRow(dataLines[0])
  const bodyRows = dataLines.slice(1)

  // 헤더에서 시간/내용/담당 컬럼 인덱스 찾기
  const timeIdx = headerCells.findIndex(h => /time|시간/i.test(h))
  const contentIdx = headerCells.findIndex(h => /session|title|content|내용|topic/i.test(h))
  const speakerIdx = headerCells.findIndex(h => /speaker|instructor|담당|강사/i.test(h))
  const locationIdx = headerCells.findIndex(h => /location|장소/i.test(h))
  const notesIdx = headerCells.findIndex(h => /notes|비고|note/i.test(h))

  let html = '<table class="schedule-table"><thead><tr>'
  html += '<th class="col-time">시간</th><th class="col-content">내용</th><th class="col-speaker">담당</th>'
  html += '</tr></thead><tbody>'

  for (const row of bodyRows) {
    const cells = parseMdRow(row)

    // 시간
    const time = timeIdx >= 0 ? (cells[timeIdx] || '') : (cells[0] || '')

    // 내용: Session + Location + Notes 합치기
    let content = ''
    if (contentIdx >= 0) content = cells[contentIdx] || ''
    else if (cells[1]) content = cells[1]
    // 위치, 비고 정보가 있으면 내용에 추가
    const extras = []
    if (locationIdx >= 0 && cells[locationIdx] && cells[locationIdx] !== '-') extras.push(cells[locationIdx])
    if (notesIdx >= 0 && cells[notesIdx] && cells[notesIdx] !== '-') extras.push(cells[notesIdx])
    if (extras.length > 0) content += ' (' + extras.join(', ') + ')'

    // 담당
    const speaker = speakerIdx >= 0 ? (cells[speakerIdx] || '-') : (cells[2] || '-')

    html += '<tr>'
    html += `<td class="time-cell">${time}</td>`
    html += `<td>${formatContent(content)}</td>`
    html += `<td class="speaker-cell">${speaker}</td>`
    html += '</tr>'
  }

  html += '</tbody></table>'
  return html
}

function parseMdRow(line) {
  return line.split('|').map(s => s.trim()).filter((s, i, arr) => i > 0 && i < arr.length - 1)
}
</script>

<style src="../../styles/practice.css"></style>
