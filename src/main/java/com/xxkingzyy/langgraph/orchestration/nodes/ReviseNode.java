package com.xxkingzyy.langgraph.orchestration.nodes;

import com.xxkingzyy.langgraph.orchestration.policy.RiskPolicy;
import com.xxkingzyy.langgraph.orchestration.state.PublishState;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.HashMap;
import java.util.Map;

/**
 * 自动改写节点：对软违规词脱敏，并累加重试次数。
 *
 * <p>它是图中唯一会“回流”的节点：执行完回到 analyze 重新分析、重新打分，
 * 直到分数下降到可发布/可人工区间，或重试次数耗尽被 {@link RiskGate} 升级人工。</p>
 */
public class ReviseNode implements NodeAction<PublishState> {

    public static final String ID = "revise";

    @Override
    public Map<String, Object> apply(PublishState state) {
        var update = new HashMap<String, Object>();
        update.put(PublishState.TRACE, state.traceAfter(ID));
        update.put(PublishState.NORMALIZED_TEXT, RiskPolicy.maskSoftWords(state.normalizedText()));
        update.put(PublishState.RETRY_COUNT, state.retryCount() + 1);
        update.put(PublishState.STATUS, "RUNNING");
        return update;
    }
}
