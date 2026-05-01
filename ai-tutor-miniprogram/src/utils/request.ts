import { goLoginWithRedirect } from '@/utils/authRedirect';

const normalizeBaseUrl = (raw: unknown): string | null => {
  if (typeof raw !== 'string') return null;
  const s = raw.trim();
  if (!s) return null;
  return s.endsWith('/') ? s.slice(0, -1) : s;
};

const ENV_BASE_URL = normalizeBaseUrl((import.meta as any).env?.VITE_API_BASE_URL);
const STORAGE_BASE_URL_KEY = 'ai_tutor_api_base_url';
const DEFAULT_BASE_URL = 'http://localhost:8080';
const PROD = (import.meta as any).env?.PROD === true;

const isLocalLikeBaseUrl = (raw: unknown) => {
  const normalized = normalizeBaseUrl(raw);
  if (!normalized) return false;
  return /^https?:\/\/(localhost|127\.0\.0\.1)(:\d+)?$/i.test(normalized);
};

export const getBaseUrl = () =>
  normalizeBaseUrl(uni.getStorageSync(STORAGE_BASE_URL_KEY)) ??
  ENV_BASE_URL ??
  DEFAULT_BASE_URL;

if (PROD) {
  const currentBaseUrl = getBaseUrl();
  if (!currentBaseUrl || isLocalLikeBaseUrl(currentBaseUrl)) {
    throw new Error('生产构建禁止使用 localhost/127.0.0.1 作为小程序 API Base URL，请设置有效的 VITE_API_BASE_URL。');
  }
}

export const setBaseUrl = (raw: string) => {
  const normalized = normalizeBaseUrl(raw);
  if (!normalized) {
    uni.removeStorageSync(STORAGE_BASE_URL_KEY);
    return getBaseUrl();
  }
  uni.setStorageSync(STORAGE_BASE_URL_KEY, normalized);
  return normalized;
};

export const clearBaseUrl = () => {
  uni.removeStorageSync(STORAGE_BASE_URL_KEY);
  return getBaseUrl();
};

export const BASE_URL = getBaseUrl();

let redirectingToLogin = false;

interface RequestOptions {
  url: string;
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
  data?: any;
  header?: any;
  loading?: boolean;
  silentError?: boolean;
}

const cleanData = (data: any) => {
  if (!data || typeof data !== 'object') return data;
  const cleaned: any = {};
  Object.keys(data).forEach(key => {
    const v = data[key];
    if (v === null || v === undefined) return;
    if (typeof v === 'string') {
      const s = v.trim();
      if (s.length === 0) return;
      if (s === 'null' || s === 'undefined') return;
      cleaned[key] = s;
      return;
    }
    cleaned[key] = v;
  });
  return cleaned;
};

export const resolveImageUrl = (path?: string) => {
  const baseUrl = getBaseUrl();
  if (!path) return '/static/avatars/default-avatar.png';
  if (path.startsWith('https://i.pravatar.cc/') || path.startsWith('http://i.pravatar.cc/')) {
    return '/static/avatars/default-avatar.png';
  }
  if (path.startsWith('http')) return path;
  if (path.endsWith('.svg')) return '/static/avatars/default-avatar.png';
  if (path.startsWith('/avatars/')) return '/static/avatars/default-avatar.png';
  if (path.startsWith('/')) return baseUrl + path;
  return baseUrl + '/' + path;
};

export const request = (options: RequestOptions): Promise<any> => {
  return new Promise((resolve, reject) => {
    let isLoadingShown = false;
    if (options.loading) {
      uni.showLoading({
        title: '加载中...',
        mask: true
      });
      isLoadingShown = true;
    }

    const token = uni.getStorageSync('token');
    
    const header = {
      'content-type': 'application/json',
      ...options.header
    };
    
    if (token) {
      header['Authorization'] = `Bearer ${token}`;
    }

    const cleanedData = cleanData(options.data);
    const baseUrl = getBaseUrl();

    uni.request({
      url: baseUrl + options.url,
      method: options.method || 'GET',
      data: cleanedData,
      header: header,
      success: (res: any) => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          const data = res.data;
          if (data.code === 0 || data.code === 200) {
            resolve(data.data);
          } else {
            if (data.code === 40100 || data.code === 401) {
              uni.removeStorageSync('token');
              if (isLoadingShown) {
                uni.hideLoading();
                isLoadingShown = false;
              }
              
              uni.showToast({
                title: data.message || data.msg || '请先登录',
                icon: 'none'
              });
              
              if (!redirectingToLogin) {
                redirectingToLogin = true;
                setTimeout(() => {
                  goLoginWithRedirect();
                  setTimeout(() => {
                    redirectingToLogin = false;
                  }, 800);
                }, 200);
              }
              reject(data);
              return;
            }
            
            if (isLoadingShown) {
                uni.hideLoading();
                isLoadingShown = false;
            }
            if (!options.silentError) {
              uni.showToast({
                title: data.msg || data.message || '请求错误',
                icon: 'none'
              });
            }
            reject(data);
          }
        } else if (res.statusCode === 401) {
          uni.removeStorageSync('token');
          if (isLoadingShown) {
            uni.hideLoading();
            isLoadingShown = false;
          }
          uni.showToast({
            title: '登录已过期，请重新登录',
            icon: 'none'
          });
          setTimeout(() => {
            goLoginWithRedirect();
          }, 500);
          reject(res);
        } else {
          if (isLoadingShown) {
            uni.hideLoading();
            isLoadingShown = false;
          }
          if (!options.silentError) {
            uni.showToast({
              title: `错误: ${res.statusCode}`,
              icon: 'none'
            });
          }
          reject(res);
        }
      },
      fail: (err) => {
        if (isLoadingShown) {
            uni.hideLoading();
            isLoadingShown = false;
        }
        if (!options.silentError) {
          uni.showToast({
            title: '网络错误',
            icon: 'none'
          });
        }
        reject(err);
      },
      complete: () => {
        if (isLoadingShown) {
          uni.hideLoading();
          isLoadingShown = false;
        }
      }
    });
  });
};

export default request;
