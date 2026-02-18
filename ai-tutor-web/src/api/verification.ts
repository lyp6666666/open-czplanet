import { http } from './http'

export const teacherVerificationApi = {
  submitEducation(proofUrls: string[]) {
    return http.post('/teacher/verification/education/submit', { proofUrls })
  },
  submitRealnameIdPhoto(idFrontUrl: string, idBackUrl: string) {
    return http.post('/teacher/verification/realname/submit', { method: 'ID_PHOTO', idFrontUrl, idBackUrl })
  },
  submitRealnameNameIdno(realName: string, idNo: string) {
    return http.post('/teacher/verification/realname/submit', { method: 'NAME_IDNO', realName, idNo })
  },
}

