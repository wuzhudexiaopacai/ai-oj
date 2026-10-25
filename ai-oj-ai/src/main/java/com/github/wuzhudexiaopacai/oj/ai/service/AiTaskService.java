package com.github.wuzhudexiaopacai.oj.ai.service;

import com.github.wuzhudexiaopacai.oj.ai.dto.TaskSubmitDTO;
import com.github.wuzhudexiaopacai.oj.ai.vo.TaskStatusVO;

/**
 * AI 异步任务服务（RocketMQ 解耦的非实时任务：出题/批量评审）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public interface AiTaskService {

    /**
     * 提交异步任务（投递 MQ 后立即返回 taskId）
     *
     * @param req 提交请求
     * @return 任务 ID
     */
    String submitTask(TaskSubmitDTO req);

    /**
     * 查询任务状态与结果（Redis ai:task:{taskId}）
     *
     * @param taskId 任务 ID
     * @return 任务状态
     */
    TaskStatusVO getTask(String taskId);
}
