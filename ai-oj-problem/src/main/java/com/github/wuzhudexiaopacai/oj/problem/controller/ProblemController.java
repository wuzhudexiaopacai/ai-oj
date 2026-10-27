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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
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
@Tag(name = "题目用户接口", description = "题目浏览与检索（用户端）：分页、标签树、详情、全文检索；均为公开只读接口，网关白名单放行，无需登录")
@RestController
@RequestMapping("/problem")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;
    private final TagService tagService;

    @Operation(summary = "题目分页", description = "分页查询题目，仅返回已上架（status=1）且公开（auth=1）的题目，按题号升序。支持关键字（标题/题号模糊）、难度（0简单 1中等 2困难）与标签多选（需同时命中全部所选标签）过滤。结果按查询条件缓存；标签列表无标签时为空列表，userStatus 恒为 null（登录用户的通过状态由调用方从 submission 服务获取后自行合并）。")
    @GetMapping("/page")
    public Result<PageResult<ProblemPageVO>> page(@ParameterObject ProblemPageQueryDTO query) {
        return Result.ok(problemService.page(query));
    }

    @Operation(summary = "标签分类树", description = "返回全部标签分类及分类下的标签，分类按 sort 升序排列。结果带 Redis 缓存，标签按分类分组，无标签的分类返回空列表。用于题目筛选下拉与标签展示。")
    @GetMapping("/tags")
    public Result<List<TagTreeVO>> tags() {
        return Result.ok(tagService.tree());
    }

    @Operation(summary = "题目详情", description = "按展示题号查询题目详情，也兼容传入纯数字题目 ID（19 位以内，服务端先换算为题号）。仅返回 seq 最小的 2 条样例，大数据文件用例只返回 inputBig/outputBig 标志而不返回内容，同时返回语言代码模板。结果带防穿透空值缓存与防击穿分布式锁；题目不存在时返回业务失败提示「题目不存在」。")
    @GetMapping("/{problemKey}")
    public Result<ProblemDetailVO> detail(
            @PathVariable("problemKey")
            @Parameter(description = "展示题号（如 A1001），或纯数字题目 ID（19 位以内）",
                    required = true, example = "A1001") String problemKey) {
        return Result.ok(problemService.detail(problemKey));
    }

    @Operation(summary = "题目全文检索", description = "按关键字全文检索题目，匹配题号、标题与描述；ES 不可用或无命中时自动降级为数据库 LIKE 查询。仅返回已上架且公开的题目，降级查询最多返回 50 条；关键字为空时报错。")
    @GetMapping("/search")
    public Result<List<ProblemPageVO>> search(
            @RequestParam("keyword")
            @Parameter(description = "搜索关键字，匹配题号/标题/描述，不能为空",
                    required = true, example = "动态规划") String keyword) {
        return Result.ok(problemService.search(keyword));
    }
}
