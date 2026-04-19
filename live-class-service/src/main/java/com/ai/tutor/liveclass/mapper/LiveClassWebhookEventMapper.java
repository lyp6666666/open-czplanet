package com.ai.tutor.liveclass.mapper;

import com.ai.tutor.liveclass.domain.entity.LiveClassWebhookEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LiveClassWebhookEventMapper {
    LiveClassWebhookEvent selectByProviderAndEventId(@Param("provider") String provider, @Param("providerEventId") String providerEventId);

    int insert(LiveClassWebhookEvent event);

    int markProcessed(@Param("id") Long id);
}
