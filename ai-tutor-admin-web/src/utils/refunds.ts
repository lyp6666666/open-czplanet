export function refundTypeText(type?: string | null) {
  if (!type) return '-'
  if (type === 'FULL') return '全额退款'
  if (type === 'PARTIAL') return '部分退款'
  return type
}

export function refundStatusText(status?: string | null) {
  if (!status) return '-'
  if (status === 'PENDING') return '待审核'
  if (status === 'APPROVED') return '已同意'
  if (status === 'REJECTED') return '已拒绝'
  return status
}

export function refundParticipantRoleText(role?: string | null) {
  if (role === 'STUDENT') return '学生'
  if (role === 'TEACHER') return '教师'
  return '参与方'
}

export function refundParticipantDisplayName(role?: string | null, name?: string | null, uid?: number | null) {
  const prefix = refundParticipantRoleText(role)
  const suffix = name?.trim() || (uid != null ? String(uid) : '未知')
  return `${prefix}-${suffix}`
}
