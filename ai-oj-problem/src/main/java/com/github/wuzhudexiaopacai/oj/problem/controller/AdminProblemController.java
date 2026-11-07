package com.github.wuzhudexiaopacai.oj.problem.controller;

import com.github.wuzhudexiaopacai.oj.common.core.enums.OpType;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.common.web.annotation.OperationLog;
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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
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
@Tag(name = "题目管理接口", description = "题目管理端（后台）：题目、评测用例、语言模板的增删改查与 Excel 导入导出；需登录并具备 problem:create/update/delete/import/export/testcase 权限码")
@RestController
@RequestMapping("/admin/problem")
@RequiredArgsConstructor
public class AdminProblemController {

    private final ProblemService problemService;
    private final TestCaseService testCaseService;

    @Operation(summary = "管理端分页（含下架/私有题目）", description = "管理端分页查询题目，包含下架（status=0）与私有（auth=2）题目，按题目 ID 倒序且不走缓存。查询参数 ProblemPageQueryDTO：pageNum 从 1 开始（默认 1）、pageSize 默认 20。管理端仅按关键字与难度（0简单 1中等 2困难）过滤，tagIds 不生效。需 problem:create/update/delete 任一权限码。")
    @GetMapping("/page")
    @PreAuthorize("hasAnyAuthority('problem:create','problem:update','problem:delete')")
    public Result<PageResult<Problem>> adminPage(@ParameterObject ProblemPageQueryDTO query) {
        return Result.ok(problemService.adminPage(query));
    }

    @Operation(summary = "管理端详情（含标签/用例/模板，供编辑回显）", description = "按题目 ID 获取编辑回显数据，返回题目全部字段以及标签 ID 列表、评测用例（按 seq 升序）与语言模板。题目不存在时返回业务失败提示「题目不存在」。需 problem:create 或 problem:update 权限。")
    @GetMapping("/{id}/detail")
    @PreAuthorize("hasAnyAuthority('problem:create','problem:update')")
    public Result<ProblemSaveDTO> adminDetail(
            @PathVariable("id")
            @Parameter(description = "题目 ID", required = true, example = "10001") Long id) {
        return Result.ok(problemService.adminDetail(id));
    }

    @Operation(summary = "生成下一个可用题号", description = "按字母前缀（默认 A）查询该前缀下最大题号并递增 1，返回建议题号（如 A1042），供新建题目表单自动填充；题号唯一性仍以数据库唯一索引与新增时的校验为准。需 problem:create 或 problem:update 权限。")
    @GetMapping("/next-key")
    @PreAuthorize("hasAnyAuthority('problem:create','problem:update')")
    public Result<String> nextKey(
            @RequestParam(value = "prefix", required = false, defaultValue = "A")
            @Parameter(description = "题号字母前缀（1-4 位字母，默认 A）", example = "A") String prefix) {
        return Result.ok(problemService.nextProblemKey(prefix));
    }

    @Operation(summary = "新增题目（题目+用例+语言模板一体保存）", description = "新增题目，题号 problemKey 必须全局唯一，重复时提示「题号已存在」。题目、标签、评测用例、语言模板一体保存；status/auth 未传时默认上架（1）与公开（1）。写入后发送 MQ 同步 ES 并删除详情缓存，返回新题目 ID。需 problem:create 权限。")
    @OperationLog(module = "题目管理", operation = "新增题目", type = OpType.CREATE)
    @PostMapping
    @PreAuthorize("hasAuthority('problem:create')")
    public Result<Long> create(@Valid @RequestBody ProblemSaveDTO saveDTO) {
        return Result.ok(problemService.create(saveDTO));
    }

    @Operation(summary = "更新题目", description = "按题目 ID 更新题目，题目必须存在且题号不能与其他题目重复。标签、用例、语言模板为整体替换（先物理删除再重建），对应子列表不传时保留原数据。更新后发送 MQ 同步 ES 并删除详情缓存。需 problem:update 权限。")
    @OperationLog(module = "题目管理", operation = "更新题目", type = OpType.UPDATE)
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('problem:update')")
    public Result<Void> update(
            @PathVariable("id")
            @Parameter(description = "题目 ID", required = true, example = "10001") Long id,
            @Valid @RequestBody ProblemSaveDTO saveDTO) {
        problemService.update(id, saveDTO);
        return Result.ok();
    }

    @Operation(summary = "删除题目", description = "按题目 ID 逻辑删除题目，题目不存在时报错。删除后发送 MQ 删除 ES 文档并清理详情缓存。需 problem:delete 权限。")
    @OperationLog(module = "题目管理", operation = "删除题目", type = OpType.DELETE)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('problem:delete')")
    public Result<Void> delete(
            @PathVariable("id")
            @Parameter(description = "题目 ID", required = true, example = "10001") Long id) {
        problemService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "用例列表", description = "查询指定题目下的全部评测用例，按 seq 升序返回完整实体（含小数据内容与大数据 MinIO 对象名）。不校验题目是否存在，无用例时返回空列表。需 problem:testcase 权限。")
    @GetMapping("/{id}/testcase")
    @PreAuthorize("hasAuthority('problem:testcase')")
    public Result<List<TestCase>> listCases(
            @PathVariable("id")
            @Parameter(description = "题目 ID", required = true, example = "10001") Long problemId) {
        return Result.ok(testCaseService.listByProblem(problemId));
    }

    @Operation(summary = "新增加用例", description = "为指定题目新增评测用例，题目必须存在。输入与输出需各有一种载体：小数据填 inputData/outputData，大数据填 MinIO objectKey（inputFile/outputFile），缺失时校验失败；seq 必填。返回新用例 ID。需 problem:testcase 权限。")
    @PostMapping("/{id}/testcase")
    @PreAuthorize("hasAuthority('problem:testcase')")
    public Result<Long> createCase(
            @PathVariable("id")
            @Parameter(description = "题目 ID", required = true, example = "10001") Long problemId,
            @Valid @RequestBody TestCaseSaveDTO dto) {
        return Result.ok(testCaseService.create(problemId, dto));
    }

    @Operation(summary = "更新用例", description = "按题目 ID 与用例 ID 更新评测用例，题目必须存在且用例必须属于该题目。输入/输出的载体校验规则与新增一致（内容或 MinIO objectKey 至少各有一种），字段整体覆盖。需 problem:testcase 权限。")
    @PutMapping("/{id}/testcase/{caseId}")
    @PreAuthorize("hasAuthority('problem:testcase')")
    public Result<Void> updateCase(
            @PathVariable("id")
            @Parameter(description = "题目 ID", required = true, example = "10001") Long problemId,
            @PathVariable("caseId")
            @Parameter(description = "用例 ID", required = true, example = "30001") Long caseId,
            @Valid @RequestBody TestCaseSaveDTO dto) {
        testCaseService.update(problemId, caseId, dto);
        return Result.ok();
    }

    @Operation(summary = "删除用例", description = "按题目 ID 与用例 ID 逻辑删除评测用例，用例必须属于该题目，否则提示「用例不属于该题目」。需 problem:testcase 权限。")
    @DeleteMapping("/{id}/testcase/{caseId}")
    @PreAuthorize("hasAuthority('problem:testcase')")
    public Result<Void> deleteCase(
            @PathVariable("id")
            @Parameter(description = "题目 ID", required = true, example = "10001") Long problemId,
            @PathVariable("caseId")
            @Parameter(description = "用例 ID", required = true, example = "30001") Long caseId) {
        testCaseService.delete(problemId, caseId);
        return Result.ok();
    }

    @Operation(summary = "导出题目 Excel", description = "导出全部题目为 Excel（文件名 problems.xlsx，sheet 名「题目」），按题号升序，列与导入模板一致。响应为 xlsx 文件流而非 JSON，浏览器直接触发下载。需 problem:export 权限。")
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('problem:export')")
    public void export(HttpServletResponse response) {
        problemService.export(response);
    }

    @Operation(summary = "导入题目 Excel", description = "上传 Excel 批量导入题目，按题号判重：已存在则更新、不存在则新增（默认上架且公开）。逐行校验必填项与难度（0简单 1中等 2困难），时间/内存限制缺省为 1000ms/256MB，单行失败不影响其他行；每行写入后同步 ES。返回成功条数、失败条数及「第N行: 原因」形式的错误明细。需 problem:import 权限。")
    @PostMapping("/import")
    @PreAuthorize("hasAuthority('problem:import')")
    public Result<ProblemImportResultVO> importProblems(
            @RequestParam("file")
            @Parameter(description = "Excel 文件（.xlsx，列格式与导出模板一致）", required = true,
                    example = "problems.xlsx") MultipartFile file) {
        return Result.ok(problemService.importProblems(file));
    }
}
