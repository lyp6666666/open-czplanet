import { beforeEach, describe, expect, it } from 'vitest'

import { HttpResponse, http } from 'msw'

import { inviteApi } from '@/api/invite'
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

describe('inviteApi', () => {
  beforeEach(() => {
    const localStorageMock = createStorage()
    Object.defineProperty(window, 'localStorage', { value: localStorageMock, configurable: true })
    Object.defineProperty(globalThis, 'localStorage', { value: localStorageMock, configurable: true })
    localStorage.clear()
    localStorage.setItem('ai_tutor_token', 'mock.token')
  })

  it('fetches invite overview with invite code and settlement config', async () => {
    server.use(
      http.get('http://localhost/invite/overview', () =>
        HttpResponse.json({
          code: 0,
          data: {
            myInviteCode: 'ABC123',
            totalInviteCount: 8,
            effectiveInviteCount: 3,
            totalRewardAmountFen: 5200,
            pendingSettlementAmountFen: 1300,
            estimatedCurrentMonthAmountFen: 800,
            teacherRewardRate: 0.13,
            studentRewardRate: 0.13,
            settlementDay: 10,
            receiverConfigured: true,
            systemInviteConfig: {
              enabled: true,
              systemInviteCode: 'CHUANGZHI',
              systemInviteLink: 'http://localhost/auth/student?inviteCode=CHUANGZHI',
              tutorInfoFeeDiscountRate: 0.5,
              studentRewardRate: 0.13,
              promoTitle: '创智推广专属福利',
              promoDesc: '教师信息费减半，学生可获得返现。',
            },
          },
          message: 'ok',
        }),
      ),
    )

    const res = await inviteApi.overview()

    expect(res.myInviteCode).toBe('ABC123')
    expect(res.settlementDay).toBe(10)
    expect(res.teacherRewardRate).toBe(0.13)
    expect(res.receiverConfigured).toBe(true)
    expect(res.systemInviteConfig?.systemInviteCode).toBe('CHUANGZHI')
  })

  it('uses direct invite endpoint path so dev proxy can forward to gateway', async () => {
    let requestedPath = ''
    server.use(
      http.get('http://localhost/invite/overview', ({ request }) => {
        requestedPath = new URL(request.url).pathname
        return HttpResponse.json({
          code: 0,
          data: {
            myInviteCode: 'U0005P',
            totalInviteCount: 0,
            effectiveInviteCount: 0,
            totalRewardAmountFen: 0,
            pendingSettlementAmountFen: 0,
            estimatedCurrentMonthAmountFen: 0,
            teacherRewardRate: 0.13,
            studentRewardRate: 0.13,
            settlementDay: 10,
            receiverConfigured: false,
            systemInviteConfig: null,
          },
          message: 'ok',
        })
      }),
    )

    await inviteApi.overview()

    expect(requestedPath).toBe('/invite/overview')
  })

  it('fetches invite records with cursor paging params', async () => {
    server.use(
      http.get('http://localhost/invite/records', ({ request }) => {
        const url = new URL(request.url)
        expect(url.searchParams.get('pageSize')).toBe('20')
        expect(url.searchParams.get('cursor')).toBe('100')
        expect(url.searchParams.get('status')).toBe('EFFECTIVE')
        return HttpResponse.json({
          code: 0,
          data: {
            nextCursor: 120,
            isLast: false,
            list: [
              {
                inviteeUid: 2001,
                inviteeDisplayName: '王同学',
                inviteePhoneMasked: '138****8000',
                inviteeUserType: 2,
                registeredAt: '2026-04-18T10:00:00',
                status: 'EFFECTIVE',
                hasReward: true,
              },
            ],
          },
          message: 'ok',
        })
      }),
    )

    const page = await inviteApi.records({ pageSize: 20, cursor: 100, status: 'EFFECTIVE' })

    expect(page.isLast).toBe(false)
    expect(page.nextCursor).toBe(120)
    expect(page.list[0]?.inviteeDisplayName).toBe('王同学')
    expect(page.list[0]?.hasReward).toBe(true)
  })

  it('fetches reward records with scene filter', async () => {
    server.use(
      http.get('http://localhost/invite/rewards', ({ request }) => {
        const url = new URL(request.url)
        expect(url.searchParams.get('scene')).toBe('INVITED_TUTOR_DEAL')
        expect(url.searchParams.get('status')).toBe('SETTLEABLE')
        return HttpResponse.json({
          code: 0,
          data: {
            nextCursor: null,
            isLast: true,
            list: [
              {
                id: 1,
                inviteeUid: 3001,
                inviteeDisplayName: '李老师',
                rewardScene: 'INVITED_TUTOR_DEAL',
                bizOrderType: 'BROKERAGE_ORDER',
                bizOrderId: 9001,
                baseAmountFen: 10000,
                rewardRate: 0.13,
                rewardAmountFen: 1300,
                status: 'SETTLEABLE',
                createdAt: '2026-04-18T12:00:00',
              },
            ],
          },
          message: 'ok',
        })
      }),
    )

    const page = await inviteApi.rewards({ status: 'SETTLEABLE', scene: 'INVITED_TUTOR_DEAL' })

    expect(page.isLast).toBe(true)
    expect(page.list[0]?.rewardAmountFen).toBe(1300)
    expect(page.list[0]?.rewardScene).toBe('INVITED_TUTOR_DEAL')
  })

  it('fetches empty settlement list correctly', async () => {
    server.use(
      http.get('http://localhost/invite/settlements', () =>
        HttpResponse.json({
          code: 0,
          data: {
            nextCursor: null,
            isLast: true,
            list: [],
          },
          message: 'ok',
        }),
      ),
    )

    const page = await inviteApi.settlements({ pageSize: 10 })

    expect(page.isLast).toBe(true)
    expect(page.nextCursor).toBeNull()
    expect(page.list).toEqual([])
  })

  it('fetches receiver account info', async () => {
    server.use(
      http.get('http://localhost/invite/receiver-account', () =>
        HttpResponse.json({
          code: 0,
          data: {
            receiverName: '张三',
            wechatNo: 'zhangsan888',
            phone: '13800138000',
            remark: '常用收款号',
            configured: true,
          },
          message: 'ok',
        }),
      ),
    )

    const res = await inviteApi.receiverAccount()

    expect(res.configured).toBe(true)
    expect(res.receiverName).toBe('张三')
    expect(res.wechatNo).toBe('zhangsan888')
  })

  it('saves receiver account and returns latest snapshot', async () => {
    server.use(
      http.post('http://localhost/invite/receiver-account', async ({ request }) => {
        const body = (await request.json()) as {
          receiverName: string
          wechatNo: string
          phone: string
          remark?: string | null
        }
        expect(body.receiverName).toBe('张三')
        expect(body.wechatNo).toBe('wx_zhangsan')
        expect(body.phone).toBe('13800138000')
        return HttpResponse.json({
          code: 0,
          data: {
            receiverName: body.receiverName,
            wechatNo: body.wechatNo,
            phone: body.phone,
            remark: body.remark ?? null,
            configured: true,
          },
          message: 'ok',
        })
      }),
    )

    const res = await inviteApi.saveReceiverAccount({
      receiverName: '张三',
      wechatNo: 'wx_zhangsan',
      phone: '13800138000',
      remark: '月底结算使用',
    })

    expect(res.configured).toBe(true)
    expect(res.remark).toBe('月底结算使用')
  })

  it('fetches invite rules from backend config', async () => {
    server.use(
      http.get('http://localhost/invite/rules', () =>
        HttpResponse.json({
          code: 0,
          data: {
            teacherRewardRate: 0.13,
            studentRewardRate: 0.12,
            settlementDay: 10,
            minSettlementAmountFen: 1000,
            enabled: true,
            receiverHint: '请确保微信收款信息真实有效',
            systemInviteConfig: {
              enabled: true,
              systemInviteCode: 'CHUANGZHI',
              systemInviteLink: 'http://localhost/auth/student?inviteCode=CHUANGZHI',
              tutorInfoFeeDiscountRate: 0.5,
              studentRewardRate: 0.13,
              promoTitle: '创智推广专属福利',
              promoDesc: '教师信息费减半，学生可获得返现。',
            },
            ruleTextList: ['邀请教师成单返利', '邀请学生有效支付返利'],
          },
          message: 'ok',
        }),
      ),
    )

    const rules = await inviteApi.rules()

    expect(rules.enabled).toBe(true)
    expect(rules.studentRewardRate).toBe(0.12)
    expect(rules.ruleTextList).toHaveLength(2)
    expect(rules.systemInviteConfig?.tutorInfoFeeDiscountRate).toBe(0.5)
  })

  it('throws business error when saving invalid receiver account payload', async () => {
    server.use(
      http.post('http://localhost/invite/receiver-account', () =>
        HttpResponse.json({
          code: 40000,
          data: null,
          message: '收款信息格式错误',
        }),
      ),
    )

    await expect(
      inviteApi.saveReceiverAccount({
        receiverName: '',
        wechatNo: '',
        phone: '',
      }),
    ).rejects.toMatchObject({
      code: 40000,
      message: '收款信息格式错误',
    })
  })
})
