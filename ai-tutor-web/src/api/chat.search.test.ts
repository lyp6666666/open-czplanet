import { beforeEach, describe, expect, it } from 'vitest'

import { HttpResponse, http } from 'msw'

import { chatApi } from '@/api/chat'
import { server } from '@/test/server'

function createStorage(): Storage {
  const store = new Map<string, string>()
  return {
    get length() {
      return store.size
    },
    clear() {
      store.clear()
    },
    getItem(key: string) {
      return store.has(key) ? store.get(key)! : null
    },
    key(index: number) {
      return Array.from(store.keys())[index] ?? null
    },
    removeItem(key: string) {
      store.delete(key)
    },
    setItem(key: string, value: string) {
      store.set(key, String(value))
    },
  }
}

describe('chatApi.searchMessages', () => {
  beforeEach(() => {
    const localStorageMock = createStorage()
    Object.defineProperty(window, 'localStorage', { value: localStorageMock, configurable: true })
    Object.defineProperty(globalThis, 'localStorage', { value: localStorageMock, configurable: true })
    localStorage.clear()
    localStorage.setItem('ai_tutor_token', 'mock.token')
  })

  it('calls the search endpoint and returns matched messages', async () => {
    server.use(
      http.get('http://localhost/chat/public/msg/search', ({ request }) => {
        const url = new URL(request.url)
        expect(url.searchParams.get('roomId')).toBe('10')
        expect(url.searchParams.get('keyword')).toBe('数学')
        expect(url.searchParams.get('pageSize')).toBe('20')
        return HttpResponse.json({
          code: 0,
          data: {
            cursor: null,
            isLast: true,
            list: [
              {
                fromUser: { uid: 3001 },
                message: {
                  id: 801,
                  roomId: 10,
                  sendTime: '2026-04-18T10:10:00',
                  body: { type: 'text', content: '数学作业今晚发给你' },
                },
              },
            ],
          },
          message: 'ok',
        })
      }),
    )

    const page = await chatApi.searchMessages({ roomId: 10, keyword: '数学', pageSize: 20, cursor: null })

    expect(page.isLast).toBe(true)
    expect(page.list).toHaveLength(1)
    expect(page.list[0]?.message.id).toBe(801)
    expect((page.list[0]?.message.body as { content?: string } | undefined)?.content).toContain('数学')
  })
})
