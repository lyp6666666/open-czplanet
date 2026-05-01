export const customerServiceConfig = {
  enabled: true,
  channelType: 'WECHAT_WORK' as const,
  displayName: '创智星球客服',
  wechatNo: 'ai_tutor_service',
  qqNo: '123456789',
  qrCodeUrl: null as string | null,
  serviceTime: '09:00 - 22:00',
  description: '添加客服时请备注：家长/老师 + 手机号',
}

export const customerServiceHiddenPathPrefixes = [
  '/auth/',
  '/privacy',
  '/student/onboarding/first-demand',
  '/tutor/onboarding/basic',
  '/tutor/onboarding/profile',
  '/live/classroom/',
]
