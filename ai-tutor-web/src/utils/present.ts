const weekdayMap: Record<string, string> = {
  mon: '周一',
  tue: '周二',
  wed: '周三',
  thu: '周四',
  fri: '周五',
  sat: '周六',
  sun: '周日',
}

function pad2(n: number) {
  return String(n).padStart(2, '0')
}

function normalizeTimeRange(raw: string): string | null {
  const s = raw.trim().replace(/：/g, ':').replace(/[点时]/g, '')
  const m = s.match(/^(\d{1,2})(?::(\d{1,2}))?\s*-\s*(\d{1,2})(?::(\d{1,2}))?$/)
  if (!m) return null

  const h1 = Number(m[1])
  const m1 = m[2] != null ? Number(m[2]) : 0
  const h2 = Number(m[3])
  const m2 = m[4] != null ? Number(m[4]) : 0

  if (![h1, h2].every((h) => Number.isFinite(h) && h >= 0 && h <= 23)) return null
  if (![m1, m2].every((mm) => Number.isFinite(mm) && mm >= 0 && mm <= 59)) return null

  return `${pad2(h1)}:${pad2(m1)}-${pad2(h2)}:${pad2(m2)}`
}

export function formatClassMode(raw: string | null | undefined): string {
  if (!raw) return ''
  const v = raw.trim().toLowerCase()
  if (v === 'online') return '线上'
  if (v === 'offline') return '线下'
  if (v === 'both') return '线上/线下均可'
  return raw
}

export function formatBudgetUnit(raw: string | null | undefined): string {
  if (!raw) return ''
  const v = raw.trim().toLowerCase()
  if (v === 'h' || v === 'hour' || v === 'hours' || v === 'hr') return '小时'
  if (v === 'day' || v === 'days') return '天'
  if (v === 'week' || v === 'weeks') return '周'
  if (v === 'month' || v === 'months') return '月'
  return raw
}

export function formatStageCode(raw: string | null | undefined): string {
  if (!raw) return ''
  const v = raw.trim().toUpperCase()
  if (v === 'PRESCHOOL') return '幼教育'
  if (v === 'PRIMARY') return '小学'
  if (v === 'JUNIOR') return '初中'
  if (v === 'SENIOR') return '高中'
  if (v === 'OTHER') return '其他'
  return raw
}

export function formatEducationRequirement(raw: string | null | undefined): string {
  if (!raw) return '不限'
  const v = raw.trim().toUpperCase()
  if (v === 'UNLIMITED') return '不限'
  if (v === 'TOP2') return 'top2'
  if (v === 'C985') return '985'
  if (v === 'C211') return '211'
  if (v === 'DOUBLE_FIRST_CLASS') return '双一流'
  if (v === 'FIRST_TIER') return '一本'
  if (v === 'BACHELOR') return '本科'
  if (v === 'OVERSEAS') return '海归'
  if (v === 'QS50') return 'QS前50'
  return raw
}

export function formatScheduleItem(raw: string): string {
  const s = raw.trim().replace(/^['"]|['"]$/g, '')
  if (!s) return ''

  const en = s.match(/^(Mon|Tue|Wed|Thu|Fri|Sat|Sun)\b\.?\s*(.*)$/i)
  if (en) {
    const token = en[1]
    if (!token) return s
    const day = weekdayMap[token.slice(0, 3).toLowerCase()] ?? token
    const rest = (en[2] || '').trim()
    if (!rest) return day
    const range = normalizeTimeRange(rest)
    return range ? `${day} ${range}` : `${day} ${rest}`
  }

  const cn = s.match(/^(周[一二三四五六日天])\s*(.*)$/)
  if (cn) {
    const token = cn[1]
    if (!token) return s
    const day = token === '周天' ? '周日' : token
    const rest = (cn[2] || '').trim()
    if (!rest) return day
    const range = normalizeTimeRange(rest)
    return range ? `${day} ${range}` : `${day} ${rest}`
  }

  const rangeOnly = normalizeTimeRange(s)
  if (rangeOnly) return rangeOnly

  return s
}

export function formatScheduleText(raw: string | null | undefined): string {
  if (!raw) return ''
  let t = raw.trim()
  if (!t) return ''

  for (let i = 0; i < 2; i++) {
    if (!(t.startsWith('[') && t.endsWith(']')) && !(t.startsWith('"') && t.endsWith('"'))) break
    try {
      const parsed: unknown = JSON.parse(t)
      if (Array.isArray(parsed)) {
        const parts = parsed
          .filter((x): x is string => typeof x === 'string')
          .map((x) => formatScheduleItem(x))
          .filter(Boolean)
        return parts.join('；')
      }
      if (typeof parsed === 'string') {
        const next = parsed.trim()
        if (!next || next === t) break
        t = next
        continue
      }
    } catch {
      break
    }
    break
  }

  const parts = t
    .split(/[,，;；]/g)
    .map((x) => x.trim())
    .filter(Boolean)
    .map((x) => formatScheduleItem(x))
    .filter(Boolean)

  if (parts.length > 1) return parts.join('；')
  return formatScheduleItem(t)
}

export function formatDemandBizStatus(bizStatus: number | null | undefined, publishStatus?: number | null): string {
  const v = bizStatus == null ? null : Number(bizStatus)
  if (v == null || !Number.isFinite(v)) {
    if (publishStatus === 0) return '已关闭'
    return '匹配中'
  }
  if (v === 1) return '匹配中'
  if (v === 2) return '待支付解锁'
  if (v === 3) return '沟通中'
  if (v === 4) return '合作中'
  if (v === 5) return '已结课'
  if (v === 6) return '已关闭'
  return '匹配中'
}
