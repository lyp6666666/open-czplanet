import { http } from './http'

export interface RescheduleAppointmentRequest {
  proposedStartTime: string
  durationMinutes?: number
  remark?: string
}

export const appointmentApi = {
  reschedule(id: number, request: RescheduleAppointmentRequest) {
    return http.post<unknown, string>(`/appointment/${id}/reschedule`, request)
  },

  confirmReschedule(id: number) {
    return http.post<unknown, string>(`/appointment/${id}/confirmReschedule`)
  },

  complete(id: number) {
    return http.post<unknown, string>(`/appointment/${id}/complete`)
  },
}
