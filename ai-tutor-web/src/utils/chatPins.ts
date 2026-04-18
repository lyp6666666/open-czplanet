const CHAT_PIN_STORAGE_PREFIX = 'ai_tutor_chat_pins:'
const CHAT_PIN_CHANGED_EVENT = 'ai-tutor-chat-pins-changed'

function buildStorageKey(uid: number) {
  return `${CHAT_PIN_STORAGE_PREFIX}${uid}`
}

function normalizeRoomIds(raw: unknown): number[] {
  if (!Array.isArray(raw)) return []
  const seen = new Set<number>()
  const out: number[] = []
  raw.forEach((item) => {
    const roomId = typeof item === 'number' ? item : Number(item)
    if (!Number.isFinite(roomId) || roomId <= 0 || seen.has(roomId)) return
    seen.add(roomId)
    out.push(roomId)
  })
  return out
}

export function loadPinnedRoomIds(uid: number | null | undefined): number[] {
  if (!(typeof uid === 'number' && uid > 0) || typeof window === 'undefined') return []
  try {
    const raw = window.localStorage.getItem(buildStorageKey(uid))
    if (!raw) return []
    return normalizeRoomIds(JSON.parse(raw) as unknown)
  } catch {
    return []
  }
}

export function setRoomPinned(uid: number | null | undefined, roomId: number, pinned: boolean): number[] {
  if (!(typeof uid === 'number' && uid > 0) || !(roomId > 0) || typeof window === 'undefined') return []
  const current = loadPinnedRoomIds(uid)
  const next = pinned ? [roomId, ...current.filter((id) => id !== roomId)] : current.filter((id) => id !== roomId)
  // 先用本地持久化把单端体验做完整，后续若接后端同步，只需替换这一层实现。
  window.localStorage.setItem(buildStorageKey(uid), JSON.stringify(next))
  window.dispatchEvent(new CustomEvent(CHAT_PIN_CHANGED_EVENT, { detail: { uid, roomId, pinned, roomIds: next } }))
  return next
}

export function isRoomPinned(uid: number | null | undefined, roomId: number): boolean {
  return loadPinnedRoomIds(uid).includes(roomId)
}

export function subscribeChatPinChange(listener: EventListener) {
  if (typeof window === 'undefined') return () => undefined
  window.addEventListener(CHAT_PIN_CHANGED_EVENT, listener)
  return () => window.removeEventListener(CHAT_PIN_CHANGED_EVENT, listener)
}
