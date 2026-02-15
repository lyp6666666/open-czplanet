package com.ai.tutor.appointment.service;

import com.ai.tutor.appointment.model.vo.SubjectTreeNodeVO;

import java.util.List;

/**
 * 科目查询服务（只读）。
 *
 * 设计目标：
 * - 复用：供“科目模块接口”和“未登录首页接口”共同使用；
 * - 去重：避免 Controller 内出现复杂组装逻辑；
 * - 可演进：后续可加缓存/热度排序而不影响外部 API。
 */
public interface SubjectQueryService {

    /**
     * 获取启用的科目树。
     */
    List<SubjectTreeNodeVO> getEnabledTree();

    /**
     * 根据关键词搜索启用的科目（扁平列表）。
     */
    List<SubjectTreeNodeVO> searchEnabledByKeyword(String keyword, Integer limit);
}

