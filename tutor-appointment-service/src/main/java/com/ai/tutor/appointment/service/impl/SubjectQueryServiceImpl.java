package com.ai.tutor.appointment.service.impl;

import com.ai.tutor.appointment.mapper.PositionPostMapper;
import com.ai.tutor.appointment.model.entity.PositionPost;
import com.ai.tutor.appointment.model.vo.SubjectTreeNodeVO;
import com.ai.tutor.appointment.service.SubjectQueryService;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SubjectQueryServiceImpl implements SubjectQueryService {

    @Resource
    private PositionPostMapper positionPostMapper;

    @Override
    public List<SubjectTreeNodeVO> getEnabledTree() {
        List<PositionPost> posts = positionPostMapper.selectEnabledAll();
        return buildTree(posts);
    }

    @Override
    public List<SubjectTreeNodeVO> searchEnabledByKeyword(String keyword, Integer limit) {
        ThrowUtils.throwIf(keyword == null || keyword.isBlank(), ErrorCode.PARAMS_ERROR);
        int safeLimit = limit == null ? 50 : Math.min(Math.max(limit, 1), 200);
        List<PositionPost> posts = positionPostMapper.searchEnabledByKeyword(keyword, safeLimit);
        List<SubjectTreeNodeVO> list = new ArrayList<>();
        for (PositionPost post : posts) {
            list.add(toNode(post));
        }
        return list;
    }

    private static List<SubjectTreeNodeVO> buildTree(List<PositionPost> posts) {
        Map<Long, SubjectTreeNodeVO> idToNode = new HashMap<>();
        List<SubjectTreeNodeVO> roots = new ArrayList<>();

        for (PositionPost post : posts) {
            idToNode.put(post.getId(), toNode(post));
        }

        for (PositionPost post : posts) {
            SubjectTreeNodeVO node = idToNode.get(post.getId());
            Long parentId = post.getParentId();
            if (parentId == null || parentId == 0L) {
                roots.add(node);
                continue;
            }
            SubjectTreeNodeVO parent = idToNode.get(parentId);
            if (parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    private static SubjectTreeNodeVO toNode(PositionPost post) {
        SubjectTreeNodeVO vo = new SubjectTreeNodeVO();
        vo.setId(post.getId());
        vo.setParentId(post.getParentId());
        vo.setName(post.getName());
        vo.setGrade(post.getGrade());
        vo.setDescription(post.getDescription());
        return vo;
    }
}

