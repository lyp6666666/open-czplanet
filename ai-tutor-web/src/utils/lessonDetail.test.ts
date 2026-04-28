import { describe, expect, it } from 'vitest'

import type { ScheduleEventVO } from '@/api/types'
import { buildLessonDetailModel, groupLessonsForOverview, resolveLessonStatus } from './lessonDetail'

function baseLesson(overrides: Partial<ScheduleEventVO> = {}): ScheduleEventVO {
  return {
    id: 1,
    courseId: 66,
    title: '第 1 节｜需求澄清',
    description: '学习如何识别关键约束',
    startAt: Date.parse('2026-04-28T19:30:00+08:00'),
    endAt: Date.parse('2026-04-28T20:30:00+08:00'),
    status: 'ACCEPTED',
    creatorUserId: 2001,
    participant: { id: 3001, name: '王老师', realName: '王老师', avatar: '', userType: 1 },
    chatRoomId: 88,
    ...overrides,
  }
}

describe('lessonDetail status flow', () => {
  it('marks future accepted lesson as not started', () => {
    const lesson = baseLesson()
    const status = resolveLessonStatus(lesson, { nowMs: Date.parse('2026-04-28T18:00:00+08:00') })
    expect(status.key).toBe('NOT_STARTED')
    expect(status.label).toBe('未开始')
  })

  it('marks accepted lesson at start time as ready to start', () => {
    const lesson = baseLesson()
    const status = resolveLessonStatus(lesson, { nowMs: Date.parse('2026-04-28T19:35:00+08:00') })
    expect(status.key).toBe('READY_TO_START')
    expect(status.label).toBe('已到预约时间，请上课')
  })

  it('marks lesson with live session started as in progress', () => {
    const lesson = baseLesson()
    const status = resolveLessonStatus(lesson, {
      nowMs: Date.parse('2026-04-28T19:40:00+08:00'),
      live: { sessionId: 99, courseId: 66, status: 'IN_PROGRESS', teacherUid: 3001, studentUid: 2001, provider: 'livekit', providerRoomName: 'abc' },
    })
    expect(status.key).toBe('IN_PROGRESS')
    expect(status.label).toBe('上课中')
  })

  it('marks lesson ended as completed and exposes summary text', () => {
    const lesson = baseLesson()
    const model = buildLessonDetailModel(lesson, {
      live: { sessionId: 99, courseId: 66, status: 'ENDED', teacherUid: 3001, studentUid: 2001, provider: 'livekit', providerRoomName: 'abc' },
      afterClassSummary: '本节课完成了一次需求澄清对话练习。',
    })
    expect(model.statusKey).toBe('COMPLETED')
    expect(model.statusLabel).toBe('已完课')
    expect(model.summaryText).toContain('需求澄清对话练习')
  })

  it('marks overdue accepted lesson without live as abnormal', () => {
    const lesson = baseLesson()
    const status = resolveLessonStatus(lesson, { nowMs: Date.parse('2026-04-28T21:10:00+08:00') })
    expect(status.key).toBe('ABNORMAL')
    expect(status.label).toBe('待确认未上课')
  })

  it('groups lessons into ongoing, next up and completed buckets', () => {
    const lessons = [
      baseLesson({ id: 1, startAt: Date.parse('2026-04-27T19:30:00+08:00'), endAt: Date.parse('2026-04-27T20:30:00+08:00'), status: 'COMPLETED' }),
      baseLesson({ id: 2, startAt: Date.parse('2026-04-28T19:30:00+08:00'), endAt: Date.parse('2026-04-28T20:30:00+08:00') }),
      baseLesson({ id: 3, startAt: Date.parse('2026-05-05T19:30:00+08:00'), endAt: Date.parse('2026-05-05T20:30:00+08:00') }),
    ]
    const groups = groupLessonsForOverview(lessons, { nowMs: Date.parse('2026-04-28T19:35:00+08:00') })
    expect(groups.nextUp.map((item) => item.id)).toEqual([2])
    expect(groups.completed.map((item) => item.id)).toEqual([1])
    expect(groups.planned.map((item) => item.id)).toEqual([3])
  })
})
