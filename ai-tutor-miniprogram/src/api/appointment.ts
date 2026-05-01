import { request } from '@/utils/request';

export interface RescheduleAppointmentRequest {
  proposedStartTime: string;
  durationMinutes?: number;
  remark?: string;
}

export const appointmentApi = {
  reschedule(id: number, data: RescheduleAppointmentRequest) {
    return request({
      url: `/appointment/${id}/reschedule`,
      method: 'POST',
      data,
    }) as Promise<string>;
  },
  confirmReschedule(id: number) {
    return request({
      url: `/appointment/${id}/confirmReschedule`,
      method: 'POST',
    }) as Promise<string>;
  },
  complete(id: number) {
    return request({
      url: `/appointment/${id}/complete`,
      method: 'POST',
    }) as Promise<string>;
  },
};
