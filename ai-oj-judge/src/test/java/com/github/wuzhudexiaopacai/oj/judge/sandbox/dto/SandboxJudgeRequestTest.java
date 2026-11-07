package com.github.wuzhudexiaopacai.oj.judge.sandbox.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 沙箱判题请求契约测试：确保 snake_case 字段与 Rust 侧一致
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
class SandboxJudgeRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSerializeStackLimitAndIoScore() throws Exception {
        SandboxJudgeRequest request = new SandboxJudgeRequest(
                1L, "cpp", "int main(){}", 1000, 256, 64, true, List.of(), null);

        String json = objectMapper.writeValueAsString(request);

        assertTrue(json.contains("\"stack_limit_mb\":64"), json);
        assertTrue(json.contains("\"io_score\":true"), json);
    }

    @Test
    void shouldKeepBackwardCompatibleCaseFields() throws Exception {
        SandboxJudgeRequest.CaseItem caseItem = new SandboxJudgeRequest.CaseItem(1, "1 2", "3", 10);
        SandboxJudgeRequest request = new SandboxJudgeRequest(
                1L, "java", "class Main{}", 2000, 512, 256, false, List.of(caseItem), null);

        String json = objectMapper.writeValueAsString(request);

        assertEquals(1, objectMapper.readTree(json).get("cases").get(0).get("seq").asInt());
        assertTrue(json.contains("\"expected_output\":\"3\""), json);
    }
}
