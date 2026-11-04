package com.xxkingzyy.langgraph.orchestration.nodes;

import com.xxkingzyy.langgraph.orchestration.state.PublishState;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.HashMap;
import java.util.Map;

/**
 * 入口节点：校验入参并规范化文本。
 *
 * <p>它同时承担“参数校验”和“文本清洗”两件事，但都不涉及业务判定；
 * 校验失败不会抛异常，而是把错误写进状态，由后面的条件边分流到 reject 节点，
 * 这样「异常分支」也是图里的一条显式路径（便于审计和测试）。</p>
 */
public class IntakeNode implements NodeAction<PublishState> {

    public static final String ID = "intake";

    @Override
    public Map<String, Object> apply(PublishState state) {
        var update = new HashMap<String, Object>();
        update.put(PublishState.TRACE, state.traceAfter(ID));

        var contentId = state.contentId();
        var draft = state.rawDraft();

        if (contentId.isBlank()) {
            update.put(PublishState.STATUS, "REJECTED");
            update.put(PublishState.DECISION, "REJECT_INVALID_INPUT");
            update.put(PublishState.VALIDATION_ERROR, "contentId 不能为空");
            return update;
        }
        if (draft.isBlank()) {
            update.put(PublishState.STATUS, "REJECTED");
            update.put(PublishState.DECISION, "REJECT_INVALID_INPUT");
            update.put(PublishState.VALIDATION_ERROR, "draft 不能为空");
            return update;
        }

        update.put(PublishState.CONTENT_ID, contentId.trim());
        update.put(PublishState.NORMALIZED_TEXT, normalize(draft));
        update.put(PublishState.STATUS, "RUNNING");
        update.put(PublishState.DECISION, "PENDING");
        return update;
    }

    /** 折叠连续空白并去掉首尾空白，保证后续打分基于稳定输入。 */
    static String normalize(String draft) {
        return draft.trim().replaceAll("\\s+", " ");
    }
}
