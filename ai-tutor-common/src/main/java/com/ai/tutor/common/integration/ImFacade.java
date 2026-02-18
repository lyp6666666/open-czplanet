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

    /**
     * 发送一条系统消息到指定会话房间。
     *
     * <p>说明：body 采用 Object 以便业务侧不依赖 IM 内部的消息 DTO；
     * 在单体实现中会将 body 转换为 IM 模块对应的消息体结构。</p>
     *
     * @param uid    当前用户 id（作为发送者）
     * @param roomId 会话房间 id
     * @param body   消息体（建议为 Map 结构，便于序列化与版本演进）
     * @return msgId
     */
    Long sendSystemMessage(Long uid, Long roomId, Object body);

    /**
     * 获取最近联系人（来自会话列表）。
     *
     * @param uid   当前用户 id
     * @param limit 返回数量上限
     * @return 对方用户 id 列表（按最近活跃排序）
     */
    java.util.List<Long> listRecentContactUids(Long uid, int limit);
}
