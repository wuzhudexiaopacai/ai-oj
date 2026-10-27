package com.github.wuzhudexiaopacai.oj.contest.controller;

import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.contest.dto.ContestProblemBindDTO;
import com.github.wuzhudexiaopacai.oj.contest.dto.ContestSaveDTO;
import com.github.wuzhudexiaopacai.oj.contest.service.ContestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 比赛管理端接口（权限码：contest:manage）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Tag(name = "比赛管理接口", description = "比赛管理端模块：比赛新增、更新、删除与题目绑定，全部接口需登录并具备 contest:manage 权限码（由 @PreAuthorize 校验）")
@RestController
@RequestMapping("/contest")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('contest:manage')")
public class AdminContestController {

    private final ContestService contestService;

    @Operation(summary = "新增比赛", description = "创建比赛，初始状态固定为 SCHEDULED（未开始），创建者取当前登录用户。结束时间必须晚于开始时间，封榜时间必须落在比赛时间范围内；私有赛（auth=2）必须设置密码。返回新增比赛 ID。")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ContestSaveDTO saveDTO) {
        return Result.ok(contestService.create(saveDTO));
    }

    @Operation(summary = "更新比赛", description = "更新指定比赛信息（不修改状态，状态由状态机扫描迁移），使用乐观锁控制并发，版本冲突时更新失败。私有赛密码留空表示沿用原密码，改回公开赛会清空密码。更新成功后失效详情缓存并向比赛 WebSocket 推送最新时间快照。")
    @PutMapping("/{id}")
    public Result<Void> update(
            @Parameter(description = "比赛 ID", required = true, example = "1") @PathVariable("id") Long id,
            @Valid @RequestBody ContestSaveDTO saveDTO) {
        contestService.update(id, saveDTO);
        return Result.ok();
    }

    @Operation(summary = "删除比赛", description = "逻辑删除指定比赛，并级联逻辑删除已绑定的比赛题目；报名与成绩记录保留，用于历史追溯。删除后失效详情缓存。")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "比赛 ID", required = true, example = "1") @PathVariable("id") Long id) {
        contestService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "绑定题目（display_id 按已有数量自动生成 A/B/C...）", description = "将题目绑定到指定比赛，同一题目重复绑定会被拒绝，单场比赛最多 26 题。赛中题号 display_id 与排序 sort 按当前已绑定数量自动生成（从 A 开始）；展示标题为空时前端回退展示原题标题，OI 分值未填默认 0。返回比赛题目关联 ID。")
    @PostMapping("/{id}/problem")
    public Result<Long> bindProblem(
            @Parameter(description = "比赛 ID", required = true, example = "1") @PathVariable("id") Long id,
            @Valid @RequestBody ContestProblemBindDTO bindDTO) {
        return Result.ok(contestService.bindProblem(id, bindDTO));
    }
}
