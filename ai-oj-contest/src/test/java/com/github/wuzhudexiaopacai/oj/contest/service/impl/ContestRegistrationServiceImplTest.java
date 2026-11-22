package com.github.wuzhudexiaopacai.oj.contest.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.github.wuzhudexiaopacai.oj.api.feign.JudgeFeignClient;
import com.github.wuzhudexiaopacai.oj.common.core.dto.LoginUser;
import com.github.wuzhudexiaopacai.oj.common.core.exception.BizException;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.contest.entity.Contest;
import com.github.wuzhudexiaopacai.oj.contest.entity.ContestRegistration;
import com.github.wuzhudexiaopacai.oj.contest.mapper.ContestMapper;
import com.github.wuzhudexiaopacai.oj.contest.mapper.ContestRegistrationMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 比赛报名测试：私有赛密码校验与历史明文密码升级
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class ContestRegistrationServiceImplTest {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @Mock
    private ContestMapper contestMapper;

    @Mock
    private ContestRegistrationMapper registrationMapper;

    @Mock
    private JudgeFeignClient judgeFeignClient;

    @InjectMocks
    private ContestRegistrationServiceImpl registrationService;

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Contest.class);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void login() {
        LoginUser loginUser = LoginUser.builder().id(7L).username("alice").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, List.of()));
    }

    private Contest contest(int auth, String password, String status) {
        Contest contest = new Contest();
        contest.setId(1L);
        contest.setAuth(auth);
        contest.setPassword(password);
        contest.setStatus(status);
        return contest;
    }

    @Test
    void registerWithHashedPasswordShouldSucceed() {
        login();
        when(contestMapper.selectById(1L)).thenReturn(contest(2, ENCODER.encode("pw"), "SCHEDULED"));
        when(judgeFeignClient.initContestRecord(1L, 7L, "alice")).thenReturn(Result.ok());

        registrationService.register(1L, "pw");

        verify(registrationMapper).insert(any(ContestRegistration.class));
        verify(contestMapper, never()).update(isNull(), any(Wrapper.class));
    }

    @Test
    void registerWithWrongPasswordShouldReject() {
        login();
        when(contestMapper.selectById(1L)).thenReturn(contest(2, ENCODER.encode("pw"), "SCHEDULED"));

        assertThrows(BizException.class, () -> registrationService.register(1L, "bad"));
        verify(registrationMapper, never()).insert(any(ContestRegistration.class));
        verify(judgeFeignClient, never()).initContestRecord(any(), any(), any());
    }

    @Test
    void registerWithLegacyPlainPasswordShouldUpgradeHash() {
        login();
        when(contestMapper.selectById(1L)).thenReturn(contest(2, "pw", "SCHEDULED"));
        when(judgeFeignClient.initContestRecord(1L, 7L, "alice")).thenReturn(Result.ok());

        registrationService.register(1L, "pw");

        verify(registrationMapper).insert(any(ContestRegistration.class));
        // 历史明文密码校验通过后升级为哈希
        verify(contestMapper).update(isNull(), any(Wrapper.class));
    }

    @Test
    void registerPublicContestShouldSkipPassword() {
        login();
        when(contestMapper.selectById(1L)).thenReturn(contest(1, "", "RUNNING"));
        when(judgeFeignClient.initContestRecord(1L, 7L, "alice")).thenReturn(Result.ok());

        registrationService.register(1L, null);

        verify(registrationMapper).insert(any(ContestRegistration.class));
        verify(contestMapper, never()).update(isNull(), any(Wrapper.class));
    }
}
