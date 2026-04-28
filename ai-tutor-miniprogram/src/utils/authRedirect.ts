import type { UserRole } from '@/types/domain';

const PENDING_REDIRECT_KEY = 'ai_tutor_pending_redirect';
const TAB_PAGES = new Set([
  '/pages/home/index',
  '/pages/chat/list',
  '/pages/me/index',
]);

export interface PendingRedirect {
  url: string;
  role?: UserRole;
  intent?: string;
}

function normalizeUrl(url: string) {
  return String(url || '').trim();
}

export function currentPageUrl() {
  const pages = getCurrentPages();
  const current = pages[pages.length - 1] as any;
  if (!current?.route) return '/pages/home/index';
  const path = current.route.startsWith('/') ? current.route : `/${current.route}`;
  const options = current.options || {};
  const query = Object.keys(options)
    .filter((key) => options[key] !== undefined && options[key] !== null && String(options[key]).length > 0)
    .map((key) => `${encodeURIComponent(key)}=${encodeURIComponent(String(options[key]))}`)
    .join('&');
  return query ? `${path}?${query}` : path;
}

export function setPendingRedirect(url: string, role?: UserRole, intent?: string) {
  const next = normalizeUrl(url);
  if (!next || next === '/pages/me/index') return;
  uni.setStorageSync(PENDING_REDIRECT_KEY, { url: next, role, intent });
}

export function getPendingRedirect(): PendingRedirect | null {
  const raw = uni.getStorageSync(PENDING_REDIRECT_KEY);
  if (!raw || typeof raw !== 'object') return null;
  const url = normalizeUrl((raw as PendingRedirect).url);
  if (!url) return null;
  return {
    url,
    role: (raw as PendingRedirect).role,
    intent: String((raw as PendingRedirect).intent || '').trim() || undefined,
  };
}

export function clearPendingRedirect() {
  uni.removeStorageSync(PENDING_REDIRECT_KEY);
}

export function goLoginWithRedirect(url = currentPageUrl(), role?: UserRole, intent?: string) {
  setPendingRedirect(url, role, intent);
  uni.switchTab({ url: '/pages/me/index' });
}

export function resumePendingRedirect() {
  const pending = getPendingRedirect();
  if (!pending) return false;
  if (pending.role) {
    const currentRole = (uni.getStorageSync('currentRole') || '') as UserRole | '';
    if (currentRole && currentRole !== pending.role) return false;
  }

  clearPendingRedirect();
  const targetUrl = pending.url;

  if (TAB_PAGES.has(pending.url)) {
    uni.switchTab({ url: targetUrl });
  } else {
    uni.navigateTo({ url: targetUrl });
  }
  return true;
}

export function consumePendingRedirect() {
  const pending = getPendingRedirect();
  if (!pending) return null;
  clearPendingRedirect();
  return pending;
}
