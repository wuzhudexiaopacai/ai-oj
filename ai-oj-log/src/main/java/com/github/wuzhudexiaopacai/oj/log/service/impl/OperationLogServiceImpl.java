package com.github.wuzhudexiaopacai.oj.log.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.wuzhudexiaopacai.oj.common.core.enums.OpType;
import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;
import com.github.wuzhudexiaopacai.oj.log.dto.OperationLogQuery;
import com.github.wuzhudexiaopacai.oj.log.entity.SysOperationLog;
import com.github.wuzhudexiaopacai.oj.log.mapper.SysOperationLogMapper;
import com.github.wuzhudexiaopacai.oj.log.service.OperationLogService;
import com.github.wuzhudexiaopacai.oj.log.vo.OperationLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * 操作日志查询服务实现
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final SysOperationLogMapper operationLogMapper;

    @Override
    public PageResult<OperationLogVO> pageLog(OperationLogQuery query) {
        Page<SysOperationLog> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<SysOperationLog>()
                .like(StrUtil.isNotBlank(query.getModule()), SysOperationLog::getModule, query.getModule())
                .like(StrUtil.isNotBlank(query.getOperation()), SysOperationLog::getOperation, query.getOperation())
                .like(StrUtil.isNotBlank(query.getUsername()), SysOperationLog::getUsername, query.getUsername())
                .eq(query.getSuccess() != null, SysOperationLog::getSuccess, query.getSuccess())
                .and(StrUtil.isNotBlank(query.getKeyword()), w -> w
                        .like(SysOperationLog::getUri, query.getKeyword())
                        .or()
                        .like(SysOperationLog::getClassMethod, query.getKeyword()))
                .ge(query.getStartTime() != null, SysOperationLog::getLogTime, query.getStartTime())
                .le(query.getEndTime() != null, SysOperationLog::getLogTime, query.getEndTime())
                .orderByDesc(SysOperationLog::getLogTime)
                .orderByDesc(SysOperationLog::getId);
        Page<SysOperationLog> result = operationLogMapper.selectPage(page, wrapper);
        return PageResult.of(result, this::toVO);
    }

    private OperationLogVO toVO(SysOperationLog entity) {
        OperationLogVO vo = new OperationLogVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setActionTypeLabel(Arrays.stream(OpType.values())
                .filter(type -> type.getCode() == (entity.getActionType() == null ? 0 : entity.getActionType()))
                .findFirst()
                .map(OpType::getLabel)
                .orElse(OpType.OTHER.getLabel()));
        return vo;
    }
}
