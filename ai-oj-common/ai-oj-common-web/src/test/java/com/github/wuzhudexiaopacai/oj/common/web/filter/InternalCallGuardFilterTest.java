package com.github.wuzhudexiaopacai.oj.common.web.filter;

import com.github.wuzhudexiaopacai.oj.common.core.constant.CommonConstants;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 内部接口守卫过滤器测试
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
class InternalCallGuardFilterTest {

    private final InternalCallGuardFilter filter = new InternalCallGuardFilter();

    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/user/batch-brief");

    private final MockHttpServletResponse response = new MockHttpServletResponse();

    private final MockFilterChain chain = new MockFilterChain();

    @Test
    void shouldPassThroughWhenInternalCallHeaderPresent() throws Exception {
        request.addHeader(CommonConstants.HEADER_INTERNAL_CALL, "true");

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest());
        assertTrue(response.getContentAsString().isEmpty());
    }

    @Test
    void shouldRejectWhenInternalCallHeaderMissing() throws Exception {
        filter.doFilter(request, response, chain);

        assertNull(chain.getRequest());
        assertTrue(response.getContentAsString().contains("\"code\":403"));
    }

    @Test
    void shouldRejectWhenInternalCallHeaderNotTrue() throws Exception {
        request.addHeader(CommonConstants.HEADER_INTERNAL_CALL, "1");

        filter.doFilter(request, response, chain);

        assertNull(chain.getRequest());
        assertTrue(response.getContentAsString().contains("\"code\":403"));
    }
}
