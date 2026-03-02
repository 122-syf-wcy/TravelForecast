package com.travel.ai.controller;

import com.travel.ai.common.Result;
import com.travel.ai.dto.KnowledgeRequest;
import com.travel.ai.entity.Knowledge;
import com.travel.ai.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库管理控制器
 * 提供知识库的增删查及AI问答
 */
@RestController
@RequestMapping("/knowledge")
@Tag(name = "知识库", description = "AI知识库管理与检索")
public class KnowledgeController {

    @Autowired
    private RagService ragService;

    /**
     * 知识库检索
     */
    @PostMapping("/search")
    @Operation(summary = "知识库检索")
    public Result<List<Knowledge>> search(@RequestBody KnowledgeRequest request) {
        List<Knowledge> results = ragService.search(request);
        return Result.success(results);
    }

    /**
     * 基于知识库的AI问答
     */
    @GetMapping("/answer")
    @Operation(summary = "基于知识库的AI问答")
    public Result<String> answer(@RequestParam String question,
                                 @RequestParam(required = false) Long scenicId) {
        String answer = ragService.answerWithKnowledge(question, scenicId);
        if (answer == null || answer.isEmpty()) {
            return Result.success("暂无相关知识信息");
        }
        return Result.success(answer);
    }

    /**
     * 获取知识文档列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取知识文档列表")
    public Result<List<Knowledge>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long scenicId) {
        return Result.success(ragService.listKnowledge(category, scenicId));
    }

    /**
     * 添加知识文档
     */
    @PostMapping("/add")
    @Operation(summary = "添加知识文档")
    public Result<Knowledge> add(@RequestBody Knowledge knowledge) {
        return Result.success(ragService.addKnowledge(knowledge));
    }

    /**
     * 删除知识文档
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识文档")
    public Result<Void> delete(@PathVariable Long id) {
        ragService.deleteKnowledge(id);
        return Result.success(null, "已删除");
    }
}
