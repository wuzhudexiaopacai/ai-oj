package com.github.wuzhudexiaopacai.oj.judge.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 自测运行与用例校验入参长度上限测试
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
class JudgeRequestValidationTest {

    private static final int MAX_TEXT_LENGTH = 65535;

    private static final int MAX_CASE_COUNT = 200;

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void runShouldAcceptNormalPayload() {
        JudgeRunRequest req = new JudgeRunRequest();
        req.setLanguage("cpp");
        req.setCode("int main() { return 0; }");
        req.setInput("1 2");

        assertTrue(validator.validate(req).isEmpty());
    }

    @Test
    void runShouldRejectOversizedCode() {
        JudgeRunRequest req = new JudgeRunRequest();
        req.setLanguage("cpp");
        req.setCode("a".repeat(MAX_TEXT_LENGTH + 1));

        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    void runShouldRejectOversizedInput() {
        JudgeRunRequest req = new JudgeRunRequest();
        req.setLanguage("cpp");
        req.setCode("int main() { return 0; }");
        req.setInput("a".repeat(MAX_TEXT_LENGTH + 1));

        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    void validateCasesShouldAcceptNormalPayload() {
        assertTrue(validator.validate(validateRequest(1, 1)).isEmpty());
    }

    @Test
    void validateCasesShouldRejectTooManyCases() {
        assertFalse(validator.validate(validateRequest(MAX_CASE_COUNT + 1, 1)).isEmpty());
    }

    @Test
    void validateCasesShouldRejectOversizedCaseContent() {
        CaseValidateRequest req = validateRequest(1, MAX_TEXT_LENGTH + 1);

        assertFalse(validator.validate(req).isEmpty());
    }

    private CaseValidateRequest validateRequest(int caseCount, int contentLength) {
        CaseValidateRequest req = new CaseValidateRequest();
        req.setLanguage("cpp");
        req.setCode("int main() { return 0; }");
        List<CaseValidateRequest.CaseItem> cases = new ArrayList<>(caseCount);
        String content = "a".repeat(contentLength);
        for (int i = 0; i < caseCount; i++) {
            CaseValidateRequest.CaseItem item = new CaseValidateRequest.CaseItem();
            item.setSeq(i + 1);
            item.setInput(content);
            item.setExpectedOutput(content);
            cases.add(item);
        }
        req.setCases(cases);
        return req;
    }
}
