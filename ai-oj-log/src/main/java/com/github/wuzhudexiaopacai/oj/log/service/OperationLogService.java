package com.github.wuzhudexiaopacai.oj.log.service;

import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;
import com.github.wuzhudexiaopacai.oj.log.dto.OperationLogQuery;
import com.github.wuzhudexiaopacai.oj.log.vo.OperationLogVO;

/**
 * 操作日志查询服务
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public interface OperationLogService {

    /**
     * 分页查询操作日志
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<OperationLogVO> pageLog(OperationLogQuery query);
}
