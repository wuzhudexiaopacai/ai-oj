package com.github.wuzhudexiaopacai.oj.problem.controller;

import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;
import com.github.wuzhudexiaopacai.oj.problem.dto.ProblemPageQueryDTO;
import com.github.wuzhudexiaopacai.oj.problem.dto.ProblemSaveDTO;
import com.github.wuzhudexiaopacai.oj.problem.dto.TestCaseSaveDTO;
import com.github.wuzhudexiaopacai.oj.problem.entity.Problem;
import com.github.wuzhudexiaopacai.oj.problem.entity.TestCase;
import com.github.wuzhudexiaopacai.oj.problem.service.ProblemService;
import com.github.wuzhudexiaopacai.oj.problem.service.TestCaseService;
import com.github.wuzhudexiaopacai.oj.problem.vo.ProblemImportResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 题目管理端接口（权限码：problem:create/update/delete/import/export/testcase）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Tag(name = "题目管理接口")
@RestController
@RequestMapping("/admin/problem")
@RequiredArgsConstructor
public class AdminProblemController {

    private final ProblemService problemService;
    private final TestCaseService testCaseService;

    @Operation(summary = "管理端分页（含下架/私有题目）")
    @GetMapping("/page")
    @PreAuthorize("hasAnyAuthority('problem:create','problem:update','problem:delete')")
    public Result<PageResult<Problem>> adminPage(ProblemPageQueryDTO query) {
        return Result.ok(problemService.adminPage(query));
    }

    @Operation(summary = "管理端详情（含标签/用例/模板，供编辑回显）")
    @GetMapping("/{id}/detail")
    @PreAuthorize("hasAnyAuthority('problem:create','problem:update')")
    public Result<ProblemSaveDTO> adminDetail(@PathVariable("id") Long id) {
        return Result.ok(problemService.adminDetail(id));
    }

    @Operation(summary = "新增题目（题目+用例+语言模板一体保存）")
    @PostMapping
    @PreAuthorize("hasAuthority('problem:create')")
    public Result<Long> create(@Valid @RequestBody ProblemSaveDTO saveDTO) {
        return Result.ok(problemService.create(saveDTO));
    }

    @Operation(summary = "更新题目")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('problem:update')")
    public Result<Void> update(@PathVariable("id") Long id, @Valid @RequestBody ProblemSaveDTO saveDTO) {
        problemService.update(id, saveDTO);
        return Result.ok();
    }

    @Operation(summary = "删除题目")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('problem:delete')")
    public Result<Void> delete(@PathVariable("id") Long id) {
        problemService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "用例列表")
    @GetMapping("/{id}/testcase")
    @PreAuthorize("hasAuthority('problem:testcase')")
    public Result<List<TestCase>> listCases(@PathVariable("id") Long problemId) {
        return Result.ok(testCaseService.listByProblem(problemId));
    }

    @Operation(summary = "新增加用例")
    @PostMapping("/{id}/testcase")
    @PreAuthorize("hasAuthority('problem:testcase')")
    public Result<Long> createCase(@PathVariable("id") Long problemId,
                                   @Valid @RequestBody TestCaseSaveDTO dto) {
        return Result.ok(testCaseService.create(problemId, dto));
    }

    @Operation(summary = "更新用例")
    @PutMapping("/{id}/testcase/{caseId}")
    @PreAuthorize("hasAuthority('problem:testcase')")
    public Result<Void> updateCase(@PathVariable("id") Long problemId,
                                   @PathVariable("caseId") Long caseId,
                                   @Valid @RequestBody TestCaseSaveDTO dto) {
        testCaseService.update(problemId, caseId, dto);
        return Result.ok();
    }

    @Operation(summary = "删除用例")
    @DeleteMapping("/{id}/testcase/{caseId}")
    @PreAuthorize("hasAuthority('problem:testcase')")
    public Result<Void> deleteCase(@PathVariable("id") Long problemId,
                                   @PathVariable("caseId") Long caseId) {
        testCaseService.delete(problemId, caseId);
        return Result.ok();
    }

    @Operation(summary = "导出题目 Excel")
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('problem:export')")
    public void export(HttpServletResponse response) {
        problemService.export(response);
    }

    @Operation(summary = "导入题目 Excel")
    @PostMapping("/import")
    @PreAuthorize("hasAuthority('problem:import')")
    public Result<ProblemImportResultVO> importProblems(@RequestParam("file") MultipartFile file) {
        return Result.ok(problemService.importProblems(file));
    }
}
