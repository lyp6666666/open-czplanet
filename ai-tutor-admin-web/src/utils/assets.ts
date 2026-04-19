const PUBLIC_ASSET_PREFIX = '/api/v1/public/assets/'
const MINIO_BUCKET_PATH_PREFIX = '/ai-tutor-assets/'

function assetOrigin(): string {
  const configured = typeof import.meta.env.VITE_API_BASE_URL === 'string' ? import.meta.env.VITE_API_BASE_URL.trim() : ''
  if (!configured) return ''
  try {
    return new URL(configured, window.location.origin).origin
  } catch {
    return ''
  }
}

function withAssetOrigin(path: string): string {
  if (!path.startsWith('/')) return path
  const origin = assetOrigin()
  return origin ? `${origin}${path}` : path
}

export function normalizeAssetUrl(raw: string | null | undefined): string {
  const value = typeof raw === 'string' ? raw.trim() : ''
  if (!value) return ''
  if (value.startsWith(PUBLIC_ASSET_PREFIX)) return withAssetOrigin(value)
  if (value.startsWith(MINIO_BUCKET_PATH_PREFIX)) {
    return withAssetOrigin(`${PUBLIC_ASSET_PREFIX}${value.slice(MINIO_BUCKET_PATH_PREFIX.length)}`)
  }
  if (value.startsWith('/')) return withAssetOrigin(value)

  if (value.startsWith('http://') || value.startsWith('https://')) {
    try {
      const parsed = new URL(value)
      const path = parsed.pathname || ''
      if (path.startsWith(MINIO_BUCKET_PATH_PREFIX)) {
        return withAssetOrigin(`${PUBLIC_ASSET_PREFIX}${path.slice(MINIO_BUCKET_PATH_PREFIX.length)}`)
      }
      return value
    } catch {
      return value
    }
  }

  return withAssetOrigin(`/${value}`)
}
