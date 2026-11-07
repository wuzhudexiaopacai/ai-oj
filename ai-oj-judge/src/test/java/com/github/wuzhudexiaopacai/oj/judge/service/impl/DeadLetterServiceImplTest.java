package com.github.wuzhudexiaopacai.oj.judge.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.wuzhudexiaopacai.oj.common.core.exception.BizException;
import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;
import com.github.wuzhudexiaopacai.oj.judge.dto.DeadLetterQuery;
import com.github.wuzhudexiaopacai.oj.judge.entity.BizDeadLetter;
import com.github.wuzhudexiaopacai.oj.judge.mapper.BizDeadLetterMapper;
import com.github.wuzhudexiaopacai.oj.judge.vo.DeadLetterVO;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 死信归档服务测试
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class DeadLetterServiceImplTest {

    @Mock
    private BizDeadLetterMapper bizDeadLetterMapper;

    @Mock
    private RocketMQTemplate rocketMQTemplate;

    @InjectMocks
    private DeadLetterServiceImpl deadLetterService;

    @Test
    @SuppressWarnings("unchecked")
    void pageShouldMapStatusLabel() {
        BizDeadLetter letter = new BizDeadLetter();
        letter.setId(1L);
        letter.setStatus(1);
        letter.setMsgId("msg-1");
        when(bizDeadLetterMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<BizDeadLetter> page = invocation.getArgument(0);
            page.setRecords(List.of(letter));
            page.setTotal(1);
            return page;
        });

        PageResult<DeadLetterVO> result = deadLetterService.page(new DeadLetterQuery());

        assertEquals(1, result.getTotal());
        assertEquals("已重放", result.getRecords().get(0).getStatusLabel());
    }

    @Test
    void replayShouldResendAndMarkHandled() {
        BizDeadLetter letter = pendingLetter();
        when(bizDeadLetterMapper.selectById(1L)).thenReturn(letter);
        when(bizDeadLetterMapper.update(any(BizDeadLetter.class), any())).thenReturn(1);

        deadLetterService.replay(1L, "沙箱恢复后重放");

        verify(rocketMQTemplate).syncSend("AI_OJ_JUDGE_SUBMIT", "{\"submitId\":\"abc\"}");
        ArgumentCaptor<BizDeadLetter> captor = ArgumentCaptor.forClass(BizDeadLetter.class);
        verify(bizDeadLetterMapper).update(captor.capture(), any());
        assertEquals(1, captor.getValue().getStatus());
        assertEquals(4, captor.getValue().getRetryTimes());
        assertEquals("沙箱恢复后重放", captor.getValue().getHandleRemark());
    }

    @Test
    void replayShouldRejectAlreadyHandledLetter() {
        BizDeadLetter letter = pendingLetter();
        letter.setStatus(2);
        when(bizDeadLetterMapper.selectById(1L)).thenReturn(letter);

        assertThrows(BizException.class, () -> deadLetterService.replay(1L, "重复处理"));
        verify(rocketMQTemplate, never()).syncSend(anyString(), any(Object.class));
        verify(bizDeadLetterMapper, never()).update(any(BizDeadLetter.class), any());
    }

    @Test
    void replayShouldRejectMissingLetter() {
        when(bizDeadLetterMapper.selectById(1L)).thenReturn(null);

        assertThrows(BizException.class, () -> deadLetterService.replay(1L, "不存在"));
        verify(rocketMQTemplate, never()).syncSend(anyString(), any(Object.class));
    }

    @Test
    void replayShouldNotMarkHandledWhenSendFails() {
        BizDeadLetter letter = pendingLetter();
        when(bizDeadLetterMapper.selectById(1L)).thenReturn(letter);
        when(rocketMQTemplate.syncSend(anyString(), any(Object.class))).thenThrow(new RuntimeException("broker down"));

        assertThrows(BizException.class, () -> deadLetterService.replay(1L, "发送失败"));
        verify(bizDeadLetterMapper, never()).update(any(BizDeadLetter.class), any());
    }

    @Test
    void replayShouldFailWhenConcurrentHandled() {
        BizDeadLetter letter = pendingLetter();
        when(bizDeadLetterMapper.selectById(1L)).thenReturn(letter);
        when(bizDeadLetterMapper.update(any(BizDeadLetter.class), any())).thenReturn(0);

        assertThrows(BizException.class, () -> deadLetterService.replay(1L, "并发处理"));
    }

    @Test
    void ignoreShouldNotResend() {
        BizDeadLetter letter = pendingLetter();
        when(bizDeadLetterMapper.selectById(1L)).thenReturn(letter);
        when(bizDeadLetterMapper.update(any(BizDeadLetter.class), any())).thenReturn(1);

        deadLetterService.ignore(1L, "重复消息");

        verify(rocketMQTemplate, never()).syncSend(anyString(), any(Object.class));
        ArgumentCaptor<BizDeadLetter> captor = ArgumentCaptor.forClass(BizDeadLetter.class);
        verify(bizDeadLetterMapper).update(captor.capture(), any());
        assertEquals(2, captor.getValue().getStatus());
        assertEquals(3, captor.getValue().getRetryTimes());
        assertEquals("重复消息", captor.getValue().getHandleRemark());
    }

    @Test
    void remarkShouldBeTruncatedTo512Chars() {
        BizDeadLetter letter = pendingLetter();
        when(bizDeadLetterMapper.selectById(1L)).thenReturn(letter);
        when(bizDeadLetterMapper.update(any(BizDeadLetter.class), any())).thenReturn(1);

        deadLetterService.ignore(1L, "x".repeat(600));

        ArgumentCaptor<BizDeadLetter> captor = ArgumentCaptor.forClass(BizDeadLetter.class);
        verify(bizDeadLetterMapper).update(captor.capture(), any());
        assertEquals(512, captor.getValue().getHandleRemark().length());
    }

    private BizDeadLetter pendingLetter() {
        BizDeadLetter letter = new BizDeadLetter();
        letter.setId(1L);
        letter.setTopic("AI_OJ_JUDGE_SUBMIT");
        letter.setConsumerGroup("ai-oj-judge-submit-consumer");
        letter.setMsgId("msg-1");
        letter.setMsgBody("{\"submitId\":\"abc\"}");
        letter.setRetryTimes(3);
        letter.setStatus(0);
        return letter;
    }
}
