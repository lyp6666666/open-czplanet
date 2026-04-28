import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import BrokerageRequiredCard from './BrokerageRequiredCard.vue'

describe('BrokerageRequiredCard', () => {
  it('renders teacher-facing copy and emits openPolicy', async () => {
    const wrapper = mount(BrokerageRequiredCard, {
      props: {
        body: {
          type: 'brokerage_required',
          orderId: 1,
          proposalId: null,
          amountFen: 19900,
          status: 'PENDING',
          payerUserId: 2001,
        },
        canPay: true,
        viewerRole: 'teacher',
      },
    })

    expect(wrapper.text()).toContain('支付后可继续确认详细需求与合作安排。')
    expect(wrapper.text()).toContain('去支付')

    await wrapper.get('.policy-link').trigger('click')

    expect(wrapper.emitted('openPolicy')).toHaveLength(1)
  })

  it('renders student-facing waiting copy without pay button', () => {
    const wrapper = mount(BrokerageRequiredCard, {
      props: {
        body: {
          type: 'brokerage_required',
          orderId: 2,
          proposalId: null,
          amountFen: 19900,
          status: 'PENDING',
          payerUserId: 3001,
        },
        canPay: false,
        viewerRole: 'student',
      },
    })

    expect(wrapper.text()).toContain('教师支付后，双方可继续确认详细需求与合作安排。')
    expect(wrapper.text()).toContain('待教师支付信息费')
    expect(wrapper.find('.btn-primary').exists()).toBe(false)
  })
})
