import { describe, expect, it } from 'vitest'

import { normalizeAssetUrl, normalizeAvatarUrl } from './avatar'

describe('avatar url normalization', () => {
  it('rewrites direct minio url to proxied public asset path', () => {
    expect(normalizeAssetUrl('http://111.228.20.88:9000/ai-tutor-assets/avatars/109/a.png')).toBe('/api/v1/public/assets/avatars/109/a.png')
  })

  it('keeps already proxied asset path unchanged', () => {
    expect(normalizeAssetUrl('/api/v1/public/assets/avatars/109/a.png')).toBe('/api/v1/public/assets/avatars/109/a.png')
  })

  it('falls back to default avatar when source is empty', () => {
    expect(normalizeAvatarUrl('')).toBe('/avatars/default-avatar.svg')
  })
})
