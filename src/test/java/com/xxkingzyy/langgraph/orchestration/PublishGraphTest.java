package com.xxkingzyy.langgraph.orchestration;

import com.xxkingzyy.langgraph.orchestration.graph.PublishGraph;
import com.xxkingzyy.langgraph.orchestration.policy.RiskPolicy;
import com.xxkingzyy.langgraph.orchestration.state.PublishState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 编排行为测试：每个用例锁定一条“路径 + 终态 + 回流次数”，
 * 这样改动图结构或策略阈值时会立刻暴露偏差。
 */
class PublishGraphTest {

    private static PublishState run(String id, String draft) throws Exception {
        return PublishGraph.run(id, draft);
    }

    @Test
    @DisplayName("干净内容：intake -> analyze -> score -> publish")
    void cleanContentIsPublished() throws Exception {
        var state = run("t-1", "LangGraph4j 把后端编排拆成状态、节点和边三部分，便于分治与回放。");

        assertEquals("PUBLISHED", state.status());
        assertEquals("AUTO_PUBLISH", state.decision());
        assertEquals(0, state.riskScore());
        assertEquals(0, state.retryCount());
        assertEquals(List.of("intake", "analyze", "score", "publish"), state.trace());
        assertEquals("https://publish.local/content/t-1", state.publishUrl());
    }

    @Test
    @DisplayName("中度风险：软违规命中两次（60 分）走人工复核")
    void softViolationsGoToManualReview() throws Exception {
        var state = run("t-2", "这段文本包含赌博与暴力描述，需要人工确认之后才会展示。");

        assertEquals("PENDING_MANUAL", state.status());
        assertEquals("MANUAL_REVIEW", state.decision());
        assertEquals(60, state.riskScore());
        assertTrue(state.violations().contains("SOFT:赌博"));
        assertEquals(List.of("intake", "analyze", "score", "manual_review"), state.trace());
    }

    @Test
    @DisplayName("高风险软违规：改写一次后回流降至可发布")
    void reviseOnceThenPublish() throws Exception {
        var state = run("t-3", "内容涉及赌博、暴力、色情等表述，交给自动改写做脱敏处理即可。");

        assertEquals("PUBLISHED", state.status());
        assertEquals(1, state.retryCount(), "软违规可一次脱敏清除");
        assertEquals(0, state.riskScore());
        assertEquals(List.of("intake", "analyze", "score",
                "revise", "analyze", "score",
                "publish"), state.trace());
    }

    @Test
    @DisplayName("硬违规：改写无法消除，重试耗尽后升级人工")
    void hardViolationsEscalateAfterRetries() throws Exception {
        var state = run("t-4", "疑似诈骗话术，并涉及违禁品交易，自动改写无法消除，需要升级人工。");

        assertEquals("ESCALATED", state.status());
        assertEquals("ESCALATE_HUMAN", state.decision());
        assertEquals(RiskPolicy.MAX_RETRY, state.retryCount());
        assertEquals(100, state.riskScore());
        assertEquals(List.of("intake", "analyze", "score",
                "revise", "analyze", "score",
                "revise", "analyze", "score",
                "escalate"), state.trace());
    }

    @Test
    @DisplayName("空 draft：校验门禁直接拒绝，不进入分析")
    void blankDraftIsRejected() throws Exception {
        var state = run("t-5", "   \n\t ");

        assertEquals("REJECTED", state.status());
        assertEquals("REJECT_INVALID_INPUT", state.decision());
        assertTrue(state.validationError().contains("draft"));
        assertEquals(List.of("intake", "reject"), state.trace());
    }

    @Test
    @DisplayName("空 contentId：同样被校验门禁拦截")
    void blankContentIdIsRejected() throws Exception {
        var state = run(" ", "一段足够长的正常内容，用于验证 contentId 必填这条契约。");

        assertEquals("REJECTED", state.status());
        assertTrue(state.validationError().contains("contentId"));
        assertEquals(List.of("intake", "reject"), state.trace());
    }

    @Test
    @DisplayName("文本被规范化：连续空白折叠为单个空格")
    void intakeNormalizesWhitespace() throws Exception {
        var state = run("t-7", "  这里有   多个\t空白   需要折叠成单个空格，长度超过十五个字符。  ");

        assertEquals("这里有 多个 空白 需要折叠成单个空格，长度超过十五个字符。", state.normalizedText());
    }
}
