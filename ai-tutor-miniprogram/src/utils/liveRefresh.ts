const LIVE_RETURN_REFRESH_KEY = 'ai_tutor_live_return_refresh';

export type LiveReturnRefreshPayload = {
  courseId?: number | null;
  sessionId?: number | null;
  eventId?: number | null;
  returnedAt: number;
  source?: string;
};

function normalizeNumber(value: unknown) {
  const num = Number(value);
  return Number.isFinite(num) && num > 0 ? num : null;
}

export function markLiveReturnRefresh(payload: {
  courseId?: number | null;
  sessionId?: number | null;
  eventId?: number | null;
  source?: string;
}) {
  const next: LiveReturnRefreshPayload = {
    courseId: normalizeNumber(payload.courseId),
    sessionId: normalizeNumber(payload.sessionId),
    eventId: normalizeNumber(payload.eventId),
    returnedAt: Date.now(),
    source: typeof payload.source === 'string' ? payload.source.trim() || 'live_webview' : 'live_webview',
  };
  uni.setStorageSync(LIVE_RETURN_REFRESH_KEY, next);
  return next;
}

export function peekLiveReturnRefresh(): LiveReturnRefreshPayload | null {
  const raw = uni.getStorageSync(LIVE_RETURN_REFRESH_KEY);
  if (!raw || typeof raw !== 'object') return null;
  const payload = raw as Record<string, unknown>;
  const returnedAt = Number(payload.returnedAt || 0);
  if (!(returnedAt > 0)) return null;
  return {
    courseId: normalizeNumber(payload.courseId),
    sessionId: normalizeNumber(payload.sessionId),
    eventId: normalizeNumber(payload.eventId),
    returnedAt,
    source: typeof payload.source === 'string' ? payload.source.trim() || 'live_webview' : 'live_webview',
  };
}

export function consumeLiveReturnRefresh(
  matcher?: (payload: LiveReturnRefreshPayload) => boolean,
): LiveReturnRefreshPayload | null {
  const payload = peekLiveReturnRefresh();
  if (!payload) return null;
  if (matcher && !matcher(payload)) return null;
  uni.removeStorageSync(LIVE_RETURN_REFRESH_KEY);
  return payload;
}
