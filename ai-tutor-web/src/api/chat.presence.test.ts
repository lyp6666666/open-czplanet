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

describe('chatApi.batchPresence', () => {
  beforeEach(() => {
    const localStorageMock = createStorage()
    Object.defineProperty(window, 'localStorage', { value: localStorageMock, configurable: true })
    Object.defineProperty(globalThis, 'localStorage', { value: localStorageMock, configurable: true })
    localStorage.clear()
    localStorage.setItem('ai_tutor_token', 'mock.token')
  })

  it('calls the presence endpoint and returns online states', async () => {
    server.use(
      http.get('http://localhost/chat/presence/batch', ({ request }) => {
        const url = new URL(request.url)
        expect(url.searchParams.get('uids')).toBe('3001,3002')
        return HttpResponse.json({
          code: 0,
          data: [
            { uid: 3001, online: true, lastOnlineAt: null },
            { uid: 3002, online: false, lastOnlineAt: '2026-04-18T10:12:00' },
          ],
          message: 'ok',
        })
      }),
    )

    const list = await chatApi.batchPresence([3001, 3002])

    expect(list).toHaveLength(2)
    expect(list[0]).toEqual({ uid: 3001, online: true, lastOnlineAt: null })
    expect(list[1]?.online).toBe(false)
    expect(String(list[1]?.lastOnlineAt)).toContain('2026-04-18')
  })
})
