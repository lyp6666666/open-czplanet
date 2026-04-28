import { request } from '@/utils/request';

export type PaymentChannel = 'WECHAT' | 'ALIPAY';
export type PaymentTradeType = 'JSAPI' | 'NATIVE' | 'H5' | 'APP';

export interface PrepayRequest {
  contextType: string;
  contextId: number;
  channel: PaymentChannel;
  tradeType?: PaymentTradeType;
  openid?: string;
}

export interface PrepayResponse {
  orderNo: string;
  amountFen: number;
  channel: PaymentChannel;
  qrCodeUrl?: string;
  codeUrl?: string;
  expireTime?: string;
  payParams?: any;
}

export interface PaymentOrderStatus {
  orderNo: string;
  status: string;
  amountFen?: number;
  channel?: PaymentChannel;
  successTime?: string;
  expireTime?: string;
}

export const paymentApi = {
  prepay(data: PrepayRequest): Promise<PrepayResponse> {
    return request({
      url: '/payment/prepay',
      method: 'POST',
      data,
      loading: true,
    });
  },

  orderStatus(orderNo: string): Promise<PaymentOrderStatus> {
    return request({
      url: `/payment/orders/${orderNo}`,
      method: 'GET',
    });
  },
};
