import { http } from './http'

export interface UploadResult {
  objectKey: string
  url: string
  contentType: string
  size: number
}

export const assetsApi = {
  uploadImage(file: File, biz: string) {
    const form = new FormData()
    form.append('file', file)
    form.append('biz', biz)
    return http.post<unknown, UploadResult>('/api/v1/assets/upload', form)
  },
}
