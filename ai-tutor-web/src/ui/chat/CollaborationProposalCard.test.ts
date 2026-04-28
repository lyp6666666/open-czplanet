import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import CollaborationProposalCard from './CollaborationProposalCard.vue'

function baseBody(status: 'PENDING' | 'INVALIDATED' = 'PENDING') {
  return {
    type: 'collaboration_proposal' as const,
    proposalId: 88,
    pricePerHour: '按 1 小时课时费私下结算',
    trialStartAt: Date.now() + 86_400_000,
    trialEndAt: Date.now() + 93_600_000,
    status,
    creatorUserId: 3001,
  }
}

describe('CollaborationProposalCard', () => {
  it('does not allow responding to a proposal that has been replaced by a newer edit', () => {
    const wrapper = mount(CollaborationProposalCard, {
      props: {
        body: baseBody('INVALIDATED'),
        fromMe: false,
      },
    })

    expect(wrapper.text()).toContain('已被新提案替代')
    expect(wrapper.text()).toContain('请以最新一条试课合作为准')
    expect(wrapper.findAll('button')).toHaveLength(0)
  })

  it('keeps accept and reject actions only for the active pending proposal', () => {
    const wrapper = mount(CollaborationProposalCard, {
      props: {
        body: baseBody('PENDING'),
        fromMe: false,
      },
    })

    expect(wrapper.text()).toContain('待你确认')
    expect(wrapper.findAll('button').map((button) => button.text())).toEqual(['同意', '拒绝'])
  })
})
