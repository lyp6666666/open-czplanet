import { http } from './http'
import type { CursorPageResponse, StudentJobPosting } from './types'

export interface CreateStudentJobPostingRequest {
  subjectId: number
  title: string
  description?: string
  childAge?: number
  classMode?: string
  city?: string
  address?: string
  budgetMin?: number
  budgetMax?: number
  stageCode?: string
  educationRequirement?: string
  schedule?: string
}

export interface UpdateStudentJobPostingRequest {
  subjectId?: number
  title?: string
  description?: string
  childAge?: number
  classMode?: string
  city?: string
  address?: string
  budgetMin?: number
  budgetMax?: number
  stageCode?: string
  educationRequirement?: string
  schedule?: string
  status?: number
}

export const jobsApi = {
  createDemand(request: CreateStudentJobPostingRequest) {
    return http.post<unknown, number>('/api/v1/parent/jobs', request)
  },

  updateDemand(id: number, request: UpdateStudentJobPostingRequest) {
    return http.put<unknown, string>(`/api/v1/parent/jobs/${id}`, request)
  },

  getDemand(id: number) {
    return http.get<unknown, StudentJobPosting>(`/api/v1/parent/jobs/${id}`)
  },

  mineDemands(params: { pageSize?: number; cursor?: number | null }) {
    return http.get<unknown, CursorPageResponse<StudentJobPosting>>('/api/v1/parent/jobs/mine', { params })
  },

  feedDemands(params: {
    subjectId?: number
    city?: string
    classMode?: string
    stageCode?: string
    educationRequirement?: string
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
