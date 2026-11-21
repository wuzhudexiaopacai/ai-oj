package com.github.wuzhudexiaopacai.oj.judge.websocket;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.github.wuzhudexiaopacai.oj.common.core.constant.RedisKeys;
import com.github.wuzhudexiaopacai.oj.common.core.dto.LoginUser;
import com.github.wuzhudexiaopacai.oj.common.core.util.JwtUtils;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeSubmission;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeSubmissionMapper;
import io.jsonwebtoken.JwtException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 判题 WebSocket 握手鉴权测试
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class JudgeHandshakeInterceptorTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private JudgeSubmissionMapper judgeSubmissionMapper;

    @InjectMocks
    private JudgeHandshakeInterceptor interceptor;

    private MockHttpServletResponse servletResponse;

    private ServletServerHttpResponse response;

    private Map<String, Object> attributes;

    @BeforeAll
    static void initMybatisPlus() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                JudgeSubmission.class);
    }

    @BeforeEach
    void setUp() {
        servletResponse = new MockHttpServletResponse();
        response = new ServletServerHttpResponse(servletResponse);
        attributes = new HashMap<>();
    }

    @Test
    void validTokenAndOwnedSubmissionShouldPass() {
        givenValidLoginUser();
        when(judgeSubmissionMapper.selectCount(any())).thenReturn(1L);

        boolean allowed = handshake("?submitId=submit-1&token=valid-token");

        assertTrue(allowed);
        assertEquals("submit-1", attributes.get(JudgeWebSocketHandler.ATTR_SUBMIT_ID));
        assertEquals(HttpStatus.OK.value(), servletResponse.getStatus());
    }

    @Test
    void missingTokenShouldRejectWithUnauthorized() {
        boolean allowed = handshake("?submitId=submit-1");

        assertFalse(allowed);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), servletResponse.getStatus());
        verifyNoInteractions(jwtUtils, stringRedisTemplate, judgeSubmissionMapper);
    }

    @Test
    void missingSubmitIdShouldRejectWithUnauthorized() {
        boolean allowed = handshake("?token=valid-token");

        assertFalse(allowed);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), servletResponse.getStatus());
        verifyNoInteractions(jwtUtils, stringRedisTemplate, judgeSubmissionMapper);
    }

    @Test
    void invalidTokenShouldRejectWithUnauthorized() {
        when(jwtUtils.parseToken("bad-token")).thenThrow(new JwtException("invalid"));

        boolean allowed = handshake("?submitId=submit-1&token=bad-token");

        assertFalse(allowed);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), servletResponse.getStatus());
        verifyNoInteractions(stringRedisTemplate, judgeSubmissionMapper);
    }

    @Test
    void blacklistedTokenShouldRejectWithUnauthorized() {
        when(jwtUtils.parseToken("valid-token")).thenReturn(loginUser());
        when(stringRedisTemplate.hasKey(RedisKeys.TOKEN_BLACKLIST + "token-id")).thenReturn(true);

        boolean allowed = handshake("?submitId=submit-1&token=valid-token");

        assertFalse(allowed);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), servletResponse.getStatus());
        verifyNoInteractions(judgeSubmissionMapper);
    }

    @Test
    void anotherUsersSubmissionShouldRejectWithForbidden() {
        givenValidLoginUser();
        when(judgeSubmissionMapper.selectCount(any())).thenReturn(0L);

        boolean allowed = handshake("?submitId=submit-1&token=valid-token");

        assertFalse(allowed);
        assertEquals(HttpStatus.FORBIDDEN.value(), servletResponse.getStatus());
        assertTrue(attributes.isEmpty());
    }

    private void givenValidLoginUser() {
        when(jwtUtils.parseToken("valid-token")).thenReturn(loginUser());
        when(stringRedisTemplate.hasKey(RedisKeys.TOKEN_BLACKLIST + "token-id")).thenReturn(false);
    }

    private LoginUser loginUser() {
        return LoginUser.builder().id(7L).tokenId("token-id").build();
    }

    private boolean handshake(String query) {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getURI()).thenReturn(URI.create("http://localhost/ws/judge" + query));
        return interceptor.beforeHandshake(request, response, null, attributes);
    }
}
