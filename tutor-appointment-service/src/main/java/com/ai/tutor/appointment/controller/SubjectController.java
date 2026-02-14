package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.mapper.PositionPostMapper;
import com.ai.tutor.appointment.model.entity.PositionPost;
import com.ai.tutor.appointment.model.vo.SubjectTreeNodeVO;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/subjects")
@Tag(name = "科目模块接口", description = "提供科目树与科目搜索接口")
public class SubjectController {

    @Resource
    private PositionPostMapper positionPostMapper;

    @GetMapping("/tree")
    @Operation(summary = "获取科目树", description = "返回启用的科目树结构（两级或多级）")
    public BaseResponse<List<SubjectTreeNodeVO>> tree() {
        List<PositionPost> posts = positionPostMapper.selectEnabledAll();
        return ResultUtils.success(buildTree(posts));
    }

    @GetMapping("/search")
    @Operation(summary = "搜索科目", description = "根据关键词模糊匹配科目名称，返回扁平列表")
    public BaseResponse<List<SubjectTreeNodeVO>> search(@RequestParam("keyword") String keyword,
                                                       @RequestParam(value = "limit", required = false, defaultValue = "50") Integer limit) {
        List<PositionPost> posts = positionPostMapper.searchEnabledByKeyword(keyword, limit);
        List<SubjectTreeNodeVO> list = new ArrayList<>();
        for (PositionPost post : posts) {
            SubjectTreeNodeVO vo = toNode(post);
            list.add(vo);
        }
        return ResultUtils.success(list);
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

