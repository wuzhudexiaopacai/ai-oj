package com.github.wuzhudexiaopacai.oj.judge.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.wuzhudexiaopacai.oj.common.core.exception.BizException;
import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;
import com.github.wuzhudexiaopacai.oj.judge.dto.DeadLetterQuery;
import com.github.wuzhudexiaopacai.oj.judge.entity.BizDeadLetter;
import com.github.wuzhudexiaopacai.oj.judge.mapper.BizDeadLetterMapper;
import com.github.wuzhudexiaopacai.oj.judge.service.DeadLetterService;
import com.github.wuzhudexiaopacai.oj.judge.vo.DeadLetterVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

/**
 * 死信归档服务实现：分页查询 + 重放 + 忽略
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterServiceImpl implements DeadLetterService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_REPLAYED = 1;
    private static final int STATUS_IGNORED = 2;
    private static final int REMARK_MAX_LENGTH = 512;

    private final BizDeadLetterMapper bizDeadLetterMapper;

    private final RocketMQTemplate rocketMQTemplate;

    @Override
    public PageResult<DeadLetterVO> page(DeadLetterQuery query) {
        Page<BizDeadLetter> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<BizDeadLetter> wrapper = new LambdaQueryWrapper<BizDeadLetter>()
                .eq(query.getStatus() != null, BizDeadLetter::getStatus, query.getStatus())
                .and(StrUtil.isNotBlank(query.getKeyword()), w -> w
                        .like(BizDeadLetter::getMsgId, query.getKeyword())
                        .or()
                        .like(BizDeadLetter::getTopic, query.getKeyword())
                        .or()
                        .like(BizDeadLetter::getConsumerGroup, query.getKeyword()))
                .ge(query.getStartTime() != null, BizDeadLetter::getCreateTime, query.getStartTime())
                .le(query.getEndTime() != null, BizDeadLetter::getCreateTime, query.getEndTime())
                .orderByDesc(BizDeadLetter::getCreateTime)
                .orderByDesc(BizDeadLetter::getId);
        return PageResult.of(bizDeadLetterMapper.selectPage(page, wrapper), this::toVO);
    }

    @Override
    public void replay(Long id, String remark) {
        BizDeadLetter letter = loadPending(id);
        try {
            rocketMQTemplate.syncSend(letter.getTopic(), letter.getMsgBody());
        } catch (Exception e) {
            log.error("死信重放失败 id={} topic={}", id, letter.getTopic(), e);
            throw new BizException("死信重放失败：" + e.getMessage());
        }
        markHandled(letter, STATUS_REPLAYED, remark);
        log.info("死信已重放 id={} msgId={} topic={}", id, letter.getMsgId(), letter.getTopic());
    }

    @Override
    public void ignore(Long id, String remark) {
        BizDeadLetter letter = loadPending(id);
        markHandled(letter, STATUS_IGNORED, remark);
        log.info("死信已忽略 id={} msgId={}", id, letter.getMsgId());
    }

    /**
     * 读取待处理死信，不存在或已处理均拒绝操作
     *
     * @param id 记录 ID
     * @return 死信记录
     */
    private BizDeadLetter loadPending(Long id) {
        BizDeadLetter letter = bizDeadLetterMapper.selectById(id);
        if (letter == null) {
            throw new BizException("死信记录不存在");
        }
        if (!Integer.valueOf(STATUS_PENDING).equals(letter.getStatus())) {
            throw new BizException("该死信已处理，不能重复操作");
        }
        return letter;
    }

    /**
     * 条件更新状态，status 必须仍为待处理，避免并发重复处理
     *
     * @param letter 死信记录
     * @param status 目标状态
     * @param remark 处理备注
     */
    private void markHandled(BizDeadLetter letter, int status, String remark) {
        BizDeadLetter update = new BizDeadLetter();
        update.setStatus(status);
        update.setHandleRemark(StrUtil.subPre(StrUtil.blankToDefault(remark, ""), REMARK_MAX_LENGTH));
        update.setRetryTimes((letter.getRetryTimes() == null ? 0 : letter.getRetryTimes())
                + (status == STATUS_REPLAYED ? 1 : 0));
        LambdaUpdateWrapper<BizDeadLetter> wrapper = new LambdaUpdateWrapper<BizDeadLetter>()
                .eq(BizDeadLetter::getId, letter.getId())
                .eq(BizDeadLetter::getStatus, STATUS_PENDING);
        if (bizDeadLetterMapper.update(update, wrapper) == 0) {
            throw new BizException("该死信已被其他管理员处理，请刷新后重试");
        }
    }

    private DeadLetterVO toVO(BizDeadLetter entity) {
        DeadLetterVO vo = new DeadLetterVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setStatusLabel(statusLabel(entity.getStatus()));
        return vo;
    }

    private String statusLabel(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case STATUS_PENDING -> "待处理";
            case STATUS_REPLAYED -> "已重放";
            case STATUS_IGNORED -> "已忽略";
            default -> "未知";
        };
    }
}
