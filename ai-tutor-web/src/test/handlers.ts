import { HttpResponse, http } from 'msw'

function ok<T>(data: T) {
  return HttpResponse.json({ code: 0, data, message: 'ok' })
}

export const handlers = [
  http.get('http://localhost/api/v1/public/home/config', () =>
    ok({
      defaultCity: '北京',
      citySelectable: true,
      search: { placeholder: '搜索科目/老师/需求', defaultMode: 'tutor' },
      nav: [
        { key: 'findTutor', name: '找家教', link: '/tutors' },
        { key: 'beTutor', name: '当家教', link: '/become' },
      ],
      authEntry: { loginText: '登录/注册', link: '/login' },
    }),
  ),

  http.get('http://localhost/api/v1/public/geo/locate', () =>
    ok({
      ip: '127.0.0.1',
      city: '北京',
      province: '北京',
      cityCode: '110000',
      suggestCities: ['北京', '上海'],
    }),
  ),

  http.get('http://localhost/api/v1/public/home/hot-words', () =>
    ok({
      updatedAt: '2026-02-15T00:00:00Z',
      list: [
        { word: '初中数学', type: 'subject' },
        { word: '英语口语', type: 'subject' },
      ],
    }),
  ),

  http.get('http://localhost/api/v1/public/home/search/suggest', ({ request }: { request: Request }) => {
    const url = new URL(request.url)
    const q = url.searchParams.get('q') || ''
    return ok({
      q,
      list: [
        { type: 'subject', title: `${q}（科目）`, subtitle: '热门科目', payload: { subjectId: 1 } },
        { type: 'tutor', title: `${q}（老师）`, subtitle: '推荐老师', payload: { userId: 2 } },
      ],
    })
  }),

  http.get('http://localhost/api/v1/public/subjects/tree', () =>
    ok([
      {
        id: 1,
        parentId: 0,
        name: '数学',
        children: [
          { id: 11, parentId: 1, name: '初中', children: [{ id: 111, parentId: 11, name: '初一', children: [] }] },
        ],
      },
    ]),
  ),

  http.get('http://localhost/api/v1/public/home/banners', () =>
    ok({
      carousel: [
        {
          id: 'c1',
          title: '开学季名师推荐',
          subtitle: '覆盖热门科目',
          imageUrl: 'https://example.com/c1.png',
          link: { type: 'url', url: '/tutors' },
        },
      ],
      cards: [
        {
          id: 'k1',
          title: '线上一对一',
          subtitle: '随时随地开课',
          imageUrl: 'https://example.com/k1.png',
          link: { type: 'url', url: '/online' },
        },
        {
          id: 'k2',
          title: '附近好老师',
          subtitle: '按位置筛选',
          imageUrl: 'https://example.com/k2.png',
          link: { type: 'url', url: '/nearby' },
        },
      ],
    }),
  ),

  http.get('http://localhost/api/v1/public/home/hot-tabs', ({ request }: { request: Request }) => {
    const url = new URL(request.url)
    const type = url.searchParams.get('type') || 'service'
    return ok({
      type,
      tabs: [
        { tabId: 'recommend', name: '推荐', params: {} },
        { tabId: 'math', name: '数学', params: { subjectId: 1 } },
      ],
    })
  }),

  http.post('http://localhost/api/v1/public/home/hot/services', async () =>
    ok({
      nextCursor: null,
      isLast: true,
      list: [
        {
          serviceId: 1,
          title: '初中数学提分 1v1',
          subject: { id: 1, name: '数学' },
          pricePerHour: '120',
          mode: 'ONLINE',
          city: '北京',
          tutor: {
            userId: 2,
            displayName: '张老师',
            avatar: 'https://example.com/a.png',
            education: '北大',
            experienceYears: 3,
            ratePerHour: '120',
          },
          tags: ['提分', '耐心', '可试听'],
        },
      ],
    }),
  ),

  http.post('http://localhost/api/v1/public/home/hot/demands', async () =>
    ok({
      nextCursor: null,
      isLast: true,
      list: [
        {
          demandId: 10,
          title: '找初二数学家教（周末）',
          subject: { id: 1, name: '数学' },
          budget: { min: '100', max: '150', unit: '小时' },
          classMode: 'OFFLINE',
          city: '北京',
          addressSimple: '海淀',
          childAge: 14,
          scheduleText: '周六/周日 下午',
          parent: { userId: 9, displayName: '王女士', avatar: 'https://example.com/p.png' },
          tags: ['近地铁', '可试课'],
        },
      ],
    }),
  ),

  http.post('http://localhost/api/v1/public/home/hot/tutors', async () =>
    ok({
      nextCursor: null,
      isLast: true,
      list: [
        {
          userId: 2,
          displayName: '张老师',
          avatar: 'https://example.com/a.png',
          city: '北京',
          education: '北大',
          experienceYears: 3,
          ratePerHour: '120',
          subjectTags: ['初中数学', '小学奥数'],
          highlights: ['耐心细致', '方法体系'],
          representativeServices: [{ serviceId: 1, title: '初中数学提分 1v1', pricePerHour: '120' }],
        },
      ],
    }),
  ),

  http.get('http://localhost/api/v1/public/home/footer-links', () =>
    ok({
      links: [
        { name: '关于我们', url: 'https://example.com/about' },
        { name: '隐私政策', url: 'https://example.com/privacy' },
      ],
    }),
  ),

  http.post('http://localhost/user/sendcode', async () => ok('验证码发送成功(模拟)')),

  http.post('http://localhost/user/loginOrRegister', async ({ request }: { request: Request }) => {
    const body = (await request.json()) as {
      phone: string
      code: string
      userRoleEnum: 'TEACHER' | 'STUDENT'
      inviteCode?: string
    }
    const isTutor = body.userRoleEnum === 'TEACHER'
    return ok({
      id: isTutor ? 1001 : 2001,
      name: isTutor ? `教师${body.phone.slice(-4)}` : `学生${body.phone.slice(-4)}`,
      phone: body.phone,
      avatar: isTutor ? 'https://example.com/tutor.png' : 'https://example.com/student.png',
      sex: null,
      userType: isTutor ? 1 : 2,
      token: isTutor ? 'mock.teacher.token' : 'mock.student.token',
      isNew: true,
    })
  }),

  http.get('http://localhost/invite/overview', () =>
    ok({
      myInviteCode: 'ABC123',
      totalInviteCount: 2,
      effectiveInviteCount: 1,
      totalRewardAmountFen: 1300,
      pendingSettlementAmountFen: 1300,
      estimatedCurrentMonthAmountFen: 1300,
      teacherRewardRate: 0.13,
      studentRewardRate: 0.13,
      settlementDay: 10,
      receiverConfigured: false,
      systemInviteConfig: {
        enabled: true,
        systemInviteCode: 'CHUANGZHI',
        systemInviteLink: 'http://localhost/auth/student?inviteCode=CHUANGZHI',
        tutorInfoFeeDiscountRate: 0.5,
        studentRewardRate: 0.13,
        promoTitle: '创智推广专属福利',
        promoDesc: '教师信息费减半，学生可获得返现。',
      },
    }),
  ),

  http.get('http://localhost/invite/records', () =>
    ok({
      nextCursor: null,
      isLast: true,
      list: [
        {
          inviteeUid: 2001,
          inviteeDisplayName: '王同学',
          inviteePhoneMasked: '138****8000',
          inviteeUserType: 2,
          registeredAt: '2026-04-18T10:00:00',
          status: 'REGISTERED',
          hasReward: false,
        },
      ],
    }),
  ),

  http.get('http://localhost/invite/rewards', () =>
    ok({
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
          status: 'PENDING',
          createdAt: '2026-04-18T12:00:00',
        },
      ],
    }),
  ),

  http.get('http://localhost/invite/settlements', () =>
    ok({
      nextCursor: null,
      isLast: true,
      list: [],
    }),
  ),

  http.get('http://localhost/invite/receiver-account', () =>
    ok({
      receiverName: '',
      wechatNo: '',
      phone: '',
      remark: null,
      configured: false,
    }),
  ),

  http.post('http://localhost/invite/receiver-account', async ({ request }: { request: Request }) => {
    const body = (await request.json()) as {
      receiverName?: string
      wechatNo?: string
      phone?: string
      remark?: string | null
    }
    return ok({
      receiverName: body.receiverName || '',
      wechatNo: body.wechatNo || '',
      phone: body.phone || '',
      remark: body.remark ?? null,
      configured: true,
    })
  }),

  http.get('http://localhost/invite/rules', () =>
    ok({
      teacherRewardRate: 0.13,
      studentRewardRate: 0.13,
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
      ruleTextList: ['邀请教师成单返利 13%', '邀请学生有效支付返利 13%', '每月 10 号统一结算'],
    }),
  ),
]
