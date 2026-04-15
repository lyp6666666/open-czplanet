import { http } from './http'
import { normalizeAssetUrl } from '@/utils/avatar'

export interface UploadResult {
  objectKey: string
  url: string
  contentType: string
  size: number
}

export const assetsApi = {
  async uploadImage(file: File, biz: string) {
    const form = new FormData()
    form.append('file', file)
    form.append('biz', biz)
    const result = await http.post<unknown, UploadResult>('/api/v1/assets/upload', form)
    return {
      ...result,
      url: normalizeAssetUrl(result.url),
    }
  },
}
