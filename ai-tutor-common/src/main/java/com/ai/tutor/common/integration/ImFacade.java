package com.ai.tutor.common.integration;

/**
 * IM 领域对外的最小能力门面。
 *
 * <p>目标：在单体阶段先用本地实现快速跑通闭环；后续拆微服务时只替换实现（HTTP/RPC/MQ），
 * 业务侧不直接依赖 IM 服务内部的 Service/Mapper，从而降低拆分风险。</p>
 */
public interface ImFacade {

    /**
     * 获取或创建与指定用户的 1v1 会话房间。
     *
     * @param uid       当前用户 id
     * @param targetUid 目标用户 id
     * @return roomId
     */
    Long getOrCreateRoomWithUser(Long uid, Long targetUid);
}

