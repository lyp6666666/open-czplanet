const DEFAULT_AVATAR = '/avatars/default-avatar.svg'

export function normalizeAvatarUrl(raw: string | null | undefined): string {
  const v = typeof raw === 'string' ? raw.trim() : ''
  if (!v) return DEFAULT_AVATAR
  if (v.startsWith('http://') || v.startsWith('https://')) return v
  if (v.startsWith('/')) return v
  return `/${v}`
}

