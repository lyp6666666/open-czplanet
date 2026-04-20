package com.ai.tutor.appointment.service;

import com.ai.tutor.appointment.model.entity.LessonPaymentOrder;
import com.ai.tutor.appointment.model.entity.TutorAppointment;
import com.ai.tutor.common.event.PaymentSuccessEvent;
import com.ai.tutor.common.integration.LessonPaymentPayInfo;

public interface LessonPaymentOrderService {

    LessonPaymentOrder createAfterLessonCompleted(TutorAppointment appointment);

    LessonPaymentPayInfo getPayableOrder(Long orderId, Long uid);

    void handlePaymentSuccess(PaymentSuccessEvent event);

    LessonPaymentOrder getByLessonId(Long lessonId);

    LessonPaymentOrder findUnpaidByCourseId(Long courseId);
}
