import { http } from './http'
import type { AdminHomeCarouselItem } from './types'

export function listHomeCarousel(): Promise<AdminHomeCarouselItem[]> {
  return http.get('/api/admin/home/carousel')
}

export function createHomeCarousel(payload: { title: string; subtitle?: string; linkUrl?: string; file: File }) {
  const form = new FormData()
  form.append('title', payload.title)
  form.append('subtitle', payload.subtitle || '')
  form.append('linkUrl', payload.linkUrl || '')
  form.append('file', payload.file)
  return http.post<unknown, AdminHomeCarouselItem>('/api/admin/home/carousel', form)
}

export function deleteHomeCarousel(id: number): Promise<boolean> {
  return http.delete(`/api/admin/home/carousel/${id}`)
}
