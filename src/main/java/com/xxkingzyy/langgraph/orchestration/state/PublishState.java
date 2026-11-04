package com.xxkingzyy.langgraph.orchestration.state;

import org.bsc.langgraph4j.state.AgentState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 内容发布编排流程的状态对象。
 *
 * <p>LangGraph4j 的 State 是“节点之间传递的唯一事实来源”：每个节点接收一个不可变的
 * {@code PublishState} 快照，返回一份“局部更新 Map”，框架按 channel 合并成新的 State。
 * 因此状态字段的命名、默认值和可空性就是整个编排的契约（见 docs/contract.md）。</p>
 */
public class PublishState extends AgentState {

    public static final String CONTENT_ID = "contentId";
    public static final String RAW_DRAFT = "rawDraft";
    public static final String NORMALIZED_TEXT = "normalizedText";
    public static final String VIOLATIONS = "violations";
    public static final String RISK_SCORE = "riskScore";
    public static final String RETRY_COUNT = "retryCount";
    public static final String STATUS = "status";
    public static final String DECISION = "decision";
    public static final String PUBLISH_URL = "publishUrl";
    public static final String VALIDATION_ERROR = "validationError";
    public static final String TRACE = "trace";

    public PublishState(Map<String, Object> initData) {
        super(initData);
    }

    public String contentId() {
        return this.<String>value(CONTENT_ID).orElse("");
    }

    public String rawDraft() {
        return this.<String>value(RAW_DRAFT).orElse("");
    }

    /** 规范化后的文本；若尚未经过 intake，则回退到原始草稿。 */
    public String normalizedText() {
        return this.<String>value(NORMALIZED_TEXT).orElseGet(this::rawDraft);
    }

    @SuppressWarnings("unchecked")
    public List<String> violations() {
        return this.<List<String>>value(VIOLATIONS).orElseGet(List::of);
    }

    public int riskScore() {
        return this.<Integer>value(RISK_SCORE).orElse(0);
    }

    public int retryCount() {
        return this.<Integer>value(RETRY_COUNT).orElse(0);
    }

    public String status() {
        return this.<String>value(STATUS).orElse("CREATED");
    }

    public String decision() {
        return this.<String>value(DECISION).orElse("PENDING");
    }

    public String publishUrl() {
        return this.<String>value(PUBLISH_URL).orElse("");
    }

    public String validationError() {
        return this.<String>value(VALIDATION_ERROR).orElse("");
    }

    @SuppressWarnings("unchecked")
    public List<String> trace() {
        return this.<List<String>>value(TRACE).orElseGet(List::of);
    }

    /**
     * 生成“在现有轨迹后追加一步”的新列表。
     *
     * <p>默认 channel 是覆盖语义，所以节点必须显式返回完整的新列表，
     * 而不能依赖原地修改（原地修改不会进入下一轮状态）。</p>
     */
    public List<String> traceAfter(String step) {
        var next = new ArrayList<>(trace());
        next.add(step);
        return next;
    }

    /** 便于测试与文档引用的一行摘要。 */
    public String summary() {
        return "contentId=%s, status=%s, decision=%s, riskScore=%d, retryCount=%d, violations=%s"
                .formatted(contentId(), status(), decision(), riskScore(), retryCount(), violations());
    }
}
