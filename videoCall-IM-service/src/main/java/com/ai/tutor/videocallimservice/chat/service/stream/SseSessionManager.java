package com.ai.tutor.videocallimservice.chat.service.stream;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SseSessionManager {

    private final Map<Long, List<SseEmitter>> emittersByUid = new ConcurrentHashMap<>();

    public SseEmitter connect(Long uid) {
        SseEmitter emitter = new SseEmitter(0L);
        emittersByUid.computeIfAbsent(uid, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(uid, emitter));
        emitter.onTimeout(() -> remove(uid, emitter));
        emitter.onError(e -> remove(uid, emitter));

        try {
            emitter.send(SseEmitter.event().name("ready").data("ok"));
        } catch (IOException e) {
            remove(uid, emitter);
        }
        return emitter;
    }

    public void sendToUid(Long uid, String eventName, Object data) {
        List<SseEmitter> emitters = emittersByUid.get(uid);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                remove(uid, emitter);
            }
        }
    }

    private void remove(Long uid, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByUid.get(uid);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByUid.remove(uid);
        }
    }
}
