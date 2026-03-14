export const SUBJECT_PRESETS = ['全科辅导', '语文', '数学', '英语', '科学', '物理', '化学', '生物', '历史', '地理', '政治'] as const

export type SubjectPreset = (typeof SUBJECT_PRESETS)[number]

export const SUBJECT_OTHER_VALUE = '__OTHER__'
