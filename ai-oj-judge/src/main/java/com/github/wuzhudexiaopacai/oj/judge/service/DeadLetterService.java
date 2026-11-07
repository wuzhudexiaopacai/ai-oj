package com.github.wuzhudexiaopacai.oj.judge.service;

import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;
import com.github.wuzhudexiaopacai.oj.judge.dto.DeadLetterQuery;
import com.github.wuzhudexiaopacai.oj.judge.vo.DeadLetterVO;

/**
 * 死信归档服务
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public interface DeadLetterService {

    /**
     * 分页查询死信归档
     *
     * @param query 查询参数
     * @return 分页结果
     */
    PageResult<DeadLetterVO> page(DeadLetterQuery query);

    /**
     * 重放死信消息（重新投递到原 Topic）
     *
     * @param id     死信记录 ID
     * @param remark 处理备注
     */
    void replay(Long id, String remark);

    /**
     * 忽略死信（仅标记为已忽略，不重投）
     *
     * @param id     死信记录 ID
     * @param remark 处理备注
     */
    void ignore(Long id, String remark);
}
