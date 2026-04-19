import { beforeEach, describe, expect, it } from 'vitest'

import { HttpResponse, http as mswHttp } from 'msw'

import {
  getInviteSystemConfig,
  listInviteRelations,
  listInviteRewards,
  listInviteSettlements,
  markInviteSettlementFailed,
  markInviteSettlementPaid,
  saveInviteSystemConfig,
} from '@/api/invite'
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

describe('admin invite api', () => {
  beforeEach(() => {
    const localStorageMock = createStorage()
    Object.defineProperty(window, 'localStorage', { value: localStorageMock, configurable: true })
    Object.defineProperty(globalThis, 'localStorage', { value: localStorageMock, configurable: true })
    localStorage.setItem('ai_tutor_admin_token', 'admin.mock.token')
  })

  it('fetches invite relations with operator filters and auth header', async () => {
    server.use(
      mswHttp.get('/api/admin/invite/relations', ({ request }) => {
        const url = new URL(request.url)
        expect(request.headers.get('Authorization')).toBe('Bearer admin.mock.token')
        expect(url.searchParams.get('page')).toBe('1')
        expect(url.searchParams.get('size')).toBe('20')
        expect(url.searchParams.get('inviterUid')).toBe('1001')
        expect(url.searchParams.get('status')).toBe('ACTIVE')

        return HttpResponse.json({
          code: 0,
          data: {
            records: [
              {
                id: 1,
                inviterUid: 1001,
                inviterName: '张老师',
                inviterPhone: '13800138000',
                inviteeUid: 2001,
                inviteeName: '李同学',
                inviteePhone: '13900139000',
                inviteeUserType: 2,
                inviteCode: 'ABC123',
                bindSource: 'REGISTER',
                status: 'ACTIVE',
                riskFlag: 0,
                bindTime: '2026-04-19T10:00:00',
                createTime: '2026-04-19T10:00:00',
              },
            ],
            total: 1,
            size: 20,
            current: 1,
          },
          message: 'ok',
        })
      }),
    )

    const page = await listInviteRelations({ page: 1, size: 20, inviterUid: 1001, status: 'ACTIVE' })

    expect(page.total).toBe(1)
    expect(page.records[0]?.inviteCode).toBe('ABC123')
    expect(page.records[0]?.inviteeUserType).toBe(2)
  })

  it('fetches reward records with scene and settlement month filters', async () => {
    server.use(
      mswHttp.get('/api/admin/invite/rewards', ({ request }) => {
        const url = new URL(request.url)
        expect(url.searchParams.get('scene')).toBe('INVITED_TUTOR_DEAL')
        expect(url.searchParams.get('settlementMonth')).toBe('2026-03')

        return HttpResponse.json({
          code: 0,
          data: {
            records: [
              {
                id: 10,
                inviterUid: 1001,
                inviterName: '张老师',
                inviteeUid: 3001,
                inviteeName: '王老师',
                rewardScene: 'INVITED_TUTOR_DEAL',
                bizOrderType: 'BROKERAGE_ORDER',
                bizOrderId: 9001,
                baseAmountFen: 10000,
                rewardRate: 0.13,
                rewardAmountFen: 1300,
                status: 'SETTLEMENT_PENDING',
                settlementMonth: '2026-03',
                createTime: '2026-04-01T09:00:00',
              },
            ],
            total: 1,
            size: 10,
            current: 1,
          },
          message: 'ok',
        })
      }),
    )

    const page = await listInviteRewards({
      page: 1,
      size: 10,
      scene: 'INVITED_TUTOR_DEAL',
      settlementMonth: '2026-03',
    })

    expect(page.records[0]?.rewardAmountFen).toBe(1300)
    expect(page.records[0]?.status).toBe('SETTLEMENT_PENDING')
  })

  it('fetches settlement records and keeps receiver snapshot for finance reconciliation', async () => {
    server.use(
      mswHttp.get('/api/admin/invite/settlements', ({ request }) => {
        const url = new URL(request.url)
        expect(url.searchParams.get('userId')).toBe('1001')
        expect(url.searchParams.get('status')).toBe('CREATED')

        return HttpResponse.json({
          code: 0,
          data: {
            records: [
              {
                id: 20,
                userId: 1001,
                userName: '张老师',
                userPhone: '13800138000',
                settlementMonth: '2026-03',
                totalAmountFen: 2600,
                paidAmountFen: 0,
                status: 'CREATED',
                receiverSnapshotJson: '{"wechatNo":"wx_zhangsan"}',
                failReason: null,
                payTime: null,
                createTime: '2026-04-10T02:00:00',
                updateTime: '2026-04-10T02:00:00',
              },
            ],
            total: 1,
            size: 10,
            current: 1,
          },
          message: 'ok',
        })
      }),
    )

    const page = await listInviteSettlements({ page: 1, size: 10, userId: 1001, status: 'CREATED' })

    expect(page.records[0]?.receiverSnapshotJson).toContain('wx_zhangsan')
    expect(page.records[0]?.totalAmountFen).toBe(2600)
  })

  it('marks settlement as paid', async () => {
    server.use(
      mswHttp.post('/api/admin/invite/settlements/20/paid', () =>
        HttpResponse.json({
          code: 0,
          data: true,
          message: 'ok',
        }),
      ),
    )

    await expect(markInviteSettlementPaid(20)).resolves.toBe(true)
  })

  it('marks settlement as failed with operator reason', async () => {
    server.use(
      mswHttp.post('/api/admin/invite/settlements/20/failed', async ({ request }) => {
        const body = (await request.json()) as { reason: string }
        expect(body.reason).toBe('微信账号异常')

        return HttpResponse.json({
          code: 0,
          data: true,
          message: 'ok',
        })
      }),
    )

    await expect(markInviteSettlementFailed(20, '微信账号异常')).resolves.toBe(true)
  })

  it('throws business error when backend rejects a status transition', async () => {
    server.use(
      mswHttp.post('/api/admin/invite/settlements/20/paid', () =>
        HttpResponse.json({
          code: 50000,
          data: null,
          message: '当前结算单状态不可标记已打款',
        }),
      ),
    )

    await expect(markInviteSettlementPaid(20)).rejects.toMatchObject({
      code: 50000,
      message: '当前结算单状态不可标记已打款',
    })
  })

  it('fetches system invite config', async () => {
    server.use(
      mswHttp.get('/api/admin/invite/system-config', () =>
        HttpResponse.json({
          code: 0,
          data: {
            enabled: true,
            systemInviteCode: 'CHUANGZHI',
            systemInviteLink: 'http://localhost/auth/student?inviteCode=CHUANGZHI',
            tutorInfoFeeDiscountRate: 0.5,
            studentRewardRate: 0.13,
            promoTitle: '创智推广专属福利',
            promoDesc: '教师信息费减半，学生可获得返现。',
          },
          message: 'ok',
        }),
      ),
    )

    const config = await getInviteSystemConfig()

    expect(config.systemInviteCode).toBe('CHUANGZHI')
    expect(config.tutorInfoFeeDiscountRate).toBe(0.5)
  })

  it('saves system invite config', async () => {
    server.use(
      mswHttp.post('/api/admin/invite/system-config', async ({ request }) => {
        const body = (await request.json()) as {
          enabled: boolean
          systemInviteCode: string
          tutorInfoFeeDiscountRate: number
        }
        expect(body.enabled).toBe(true)
        expect(body.systemInviteCode).toBe('CHUANGZHI')
        expect(body.tutorInfoFeeDiscountRate).toBe(0.5)
        return HttpResponse.json({
          code: 0,
          data: {
            enabled: true,
            systemInviteCode: 'CHUANGZHI',
            systemInviteLink: 'http://localhost/auth/student?inviteCode=CHUANGZHI',
            tutorInfoFeeDiscountRate: 0.5,
            studentRewardRate: 0.13,
            promoTitle: '创智推广专属福利',
            promoDesc: '教师信息费减半，学生可获得返现。',
          },
          message: 'ok',
        })
      }),
    )

    const config = await saveInviteSystemConfig({
      enabled: true,
      systemInviteCode: 'CHUANGZHI',
      systemInviteLink: 'http://localhost/auth/student?inviteCode=CHUANGZHI',
      tutorInfoFeeDiscountRate: 0.5,
      studentRewardRate: 0.13,
      promoTitle: '创智推广专属福利',
      promoDesc: '教师信息费减半，学生可获得返现。',
    })

    expect(config.studentRewardRate).toBe(0.13)
  })
})
