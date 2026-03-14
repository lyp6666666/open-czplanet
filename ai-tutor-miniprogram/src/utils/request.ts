// src/utils/request.ts

// Base URL - change this to your backend IP/Domain
// For local dev with WeChat Mini Program, use your machine's LAN IP if testing on phone, or localhost for simulator
const BASE_URL = 'http://localhost:8080';

interface RequestOptions {
  url: string;
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
  data?: any;
  header?: any;
  loading?: boolean;
}

export const request = (options: RequestOptions): Promise<any> => {
  return new Promise((resolve, reject) => {
    // Show loading if requested
    if (options.loading) {
      uni.showLoading({
        title: 'Loading...',
        mask: true
      });
    }

    // Get token from storage
    const token = uni.getStorageSync('token');
    
    // Construct headers
    const header = {
      'content-type': 'application/json',
      ...options.header
    };
    
    if (token) {
      header['Authorization'] = `Bearer ${token}`;
    }

    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data,
      header: header,
      success: (res: any) => {
        // Handle HTTP status codes
        if (res.statusCode >= 200 && res.statusCode < 300) {
          // Assuming standard response format: { code: 0, data: ..., msg: ... }
          // If backend returns code !== 0, it's a business error
          const data = res.data;
          if (data.code === 0 || data.code === 200) {
            resolve(data.data);
          } else {
            uni.showToast({
              title: data.msg || 'Error',
              icon: 'none'
            });
            reject(data);
          }
        } else if (res.statusCode === 401) {
          // Unauthorized
          uni.removeStorageSync('token');
          uni.showToast({
            title: 'Session expired, please login again',
            icon: 'none'
          });
          // Redirect to login logic if needed
        } else {
          uni.showToast({
            title: `Error: ${res.statusCode}`,
            icon: 'none'
          });
          reject(res);
        }
      },
      fail: (err) => {
        uni.showToast({
          title: 'Network Error',
          icon: 'none'
        });
        reject(err);
      },
      complete: () => {
        if (options.loading) {
          uni.hideLoading();
        }
      }
    });
  });
};

export default request;
