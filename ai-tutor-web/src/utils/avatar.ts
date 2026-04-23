const DEFAULT_AVATAR = '/avatars/default-avatar.svg'
const PUBLIC_ASSET_PREFIX = '/api/v1/public/assets/'
const MINIO_BUCKET_PATH_PREFIX = '/ai-tutor-assets/'
const UNSAFE_MINIO_HOSTS = new Set(['127.0.0.1', 'localhost', '111.228.20.88'])

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
      if (UNSAFE_MINIO_HOSTS.has(parsed.hostname) && path.startsWith('/avatars/')) {
        return `${PUBLIC_ASSET_PREFIX}${path.slice(1)}`
      }
      if (typeof window !== 'undefined' && parsed.protocol === 'http:' && parsed.hostname === window.location.hostname) {
        return `${path}${parsed.search}${parsed.hash}`
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
