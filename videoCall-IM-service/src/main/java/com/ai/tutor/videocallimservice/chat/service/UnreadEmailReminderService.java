package com.ai.tutor.videocallimservice.chat.service;

public interface UnreadEmailReminderService {
    void onMessageCreated(Long msgId);

    void processDueTasks();
}
