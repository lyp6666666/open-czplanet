import { getBaseUrl, resolveImageUrl } from '@/utils/request';

export type UploadResult = {
  objectKey: string;
  url: string;
  contentType: string;
  size: number;
};

export const assetsApi = {
  uploadImage(filePath: string, biz = 'chat') {
    return new Promise<UploadResult>((resolve, reject) => {
      const token = uni.getStorageSync('token');
      uni.uploadFile({
        url: `${getBaseUrl()}/api/v1/assets/upload`,
        filePath,
        name: 'file',
        formData: { biz },
        header: token ? { Authorization: `Bearer ${token}` } : {},
        success: (res) => {
          try {
            const data = typeof res.data === 'string' ? JSON.parse(res.data) : res.data;
            if (data?.code === 0 || data?.code === 200) {
              const payload = data.data || {};
              resolve({
                objectKey: payload.objectKey || '',
                url: resolveImageUrl(payload.url),
                contentType: payload.contentType || 'image/jpeg',
                size: Number(payload.size || 0)
              });
              return;
            }
            reject(new Error(data?.msg || data?.message || '上传失败'));
          } catch (error) {
            reject(error);
          }
        },
        fail: reject
      });
    });
  }
};
