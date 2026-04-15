const DEFAULT_AVATAR = '/avatars/default-avatar.svg'
const PUBLIC_ASSET_PREFIX = '/api/v1/public/assets/'
const MINIO_BUCKET_PATH_PREFIX = '/ai-tutor-assets/'

export function normalizeAssetUrl(raw: string | null | undefined): string {
  const v = typeof raw === 'string' ? raw.trim() : ''
  if (!v) return ''
  if (v.startsWith(PUBLIC_ASSET_PREFIX)) return v
  if (v.startsWith('/')) return v

  if (v.startsWith('http://') || v.startsWith('https://')) {
    try {
      const parsed = new URL(v)
      const path = parsed.pathname || ''
      if (path.startsWith(MINIO_BUCKET_PATH_PREFIX)) {
        return `${PUBLIC_ASSET_PREFIX}${path.slice(MINIO_BUCKET_PATH_PREFIX.length)}`
      }
      return v
    } catch {
      return v
    }
  }

  return `/${v}`
}

export function normalizeAvatarUrl(raw: string | null | undefined): string {
  const v = normalizeAssetUrl(raw)
  return v || DEFAULT_AVATAR
}
