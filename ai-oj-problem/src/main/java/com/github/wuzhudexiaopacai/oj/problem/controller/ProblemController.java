package com.github.wuzhudexiaopacai.oj.problem.controller;

import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;
import com.github.wuzhudexiaopacai.oj.problem.dto.ProblemPageQueryDTO;
import com.github.wuzhudexiaopacai.oj.problem.service.ProblemService;
import com.github.wuzhudexiaopacai.oj.problem.service.TagService;
import com.github.wuzhudexiaopacai.oj.problem.vo.ProblemDetailVO;
import com.github.wuzhudexiaopacai.oj.problem.vo.ProblemPageVO;
import com.github.wuzhudexiaopacai.oj.problem.vo.TagTreeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 题目用户侧接口
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Tag(name = "题目用户接口")
@RestController
@RequestMapping("/problem")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;
    private final TagService tagService;

    @Operation(summary = "题目分页")
    @GetMapping("/page")
    public Result<PageResult<ProblemPageVO>> page(ProblemPageQueryDTO query) {
        return Result.ok(problemService.page(query));
    }

    @Operation(summary = "标签分类树")
    @GetMapping("/tags")
    public Result<List<TagTreeVO>> tags() {
        return Result.ok(tagService.tree());
    }

    @Operation(summary = "题目详情")
    @GetMapping("/{problemKey}")
    public Result<ProblemDetailVO> detail(@PathVariable("problemKey") String problemKey) {
        return Result.ok(problemService.detail(problemKey));
    }

    @Operation(summary = "题目全文检索")
    @GetMapping("/search")
    public Result<List<ProblemPageVO>> search(@RequestParam("keyword") String keyword) {
        return Result.ok(problemService.search(keyword));
    }
}
