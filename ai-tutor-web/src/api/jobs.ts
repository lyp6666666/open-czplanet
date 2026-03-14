import { http } from './http'
import type { CursorPageResponse, DemandViewVO, StudentJobPosting } from './types'

export interface CreateStudentJobPostingRequest {
  subjectId?: number
  subjectName: string
  subjectOther: boolean
  title: string
  description: string
  studentGender: string
  gradeCode?: string
  teacherGenderPreference?: string
  availableTime?: string
  teacherRequirementDetail: string
  childAge?: number
  classMode: string
  city?: string
  address?: string
  frequencyPerWeek: number
  budgetMin: number
  budgetMax: number
  stageCode: string
  educationRequirement: string
  publisherIdentity: string
  schedule?: string
}

export interface UpdateStudentJobPostingRequest {
  subjectId?: number
  subjectName?: string | null
  subjectOther?: boolean
  title?: string
  description?: string
  studentGender?: string
  gradeCode?: string
  teacherGenderPreference?: string
  availableTime?: string
  teacherRequirementDetail?: string
  childAge?: number
  classMode?: string
  city?: string
  address?: string
  frequencyPerWeek?: number
  budgetMin?: number
  budgetMax?: number
  stageCode?: string
  educationRequirement?: string
  publisherIdentity?: string
  schedule?: string
  status?: number
}

export const jobsApi = {
  createDemand(request: CreateStudentJobPostingRequest) {
    return http.post<unknown, number>('/api/v1/parent/jobs', request)
  },

  createOrgDemand(request: CreateStudentJobPostingRequest) {
    return http.post<unknown, number>('/api/v1/org/jobs', request)
  },

  updateDemand(id: number, request: UpdateStudentJobPostingRequest) {
    return http.put<unknown, string>(`/api/v1/parent/jobs/${id}`, request)
  },

  updateOrgDemand(id: number, request: UpdateStudentJobPostingRequest) {
    return http.put<unknown, string>(`/api/v1/org/jobs/${id}`, request)
  },

  getDemand(id: number) {
    return http.get<unknown, StudentJobPosting>(`/api/v1/parent/jobs/${id}`)
  },

  getOrgDemand(id: number) {
    return http.get<unknown, StudentJobPosting>(`/api/v1/org/jobs/${id}`)
  },

  getDemandView(id: number) {
    return http.get<unknown, DemandViewVO>(`/api/v1/parent/jobs/${id}/view`)
  },

  mineDemands(params: { pageSize?: number; cursor?: number | null }) {
    return http.get<unknown, CursorPageResponse<StudentJobPosting>>('/api/v1/parent/jobs/mine', { params })
  },

  mineOrgDemands(params: { pageSize?: number; cursor?: number | null }) {
    return http.get<unknown, CursorPageResponse<StudentJobPosting>>('/api/v1/org/jobs/mine', { params })
  },

  feedDemands(params: {
    subjectId?: number
    subject?: string
    subjectOther?: boolean
    city?: string
    classMode?: string
    stageCode?: string
    frequencyPerWeek?: number
    educationRequirement?: string
    teacherGenderPreference?: string
    budgetMin?: number
    budgetMax?: number
    pageSize?: number
    cursor?: number | null
    q?: string
    sort?: string
  }) {
    return http.get<unknown, CursorPageResponse<StudentJobPosting>>('/api/v1/parent/jobs/feed', { params })
  },
}
