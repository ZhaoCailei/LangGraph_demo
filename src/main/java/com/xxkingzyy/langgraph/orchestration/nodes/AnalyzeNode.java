package com.xxkingzyy.langgraph.orchestration.nodes;

import com.xxkingzyy.langgraph.orchestration.policy.RiskPolicy;
import com.xxkingzyy.langgraph.orchestration.state.PublishState;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.HashMap;
import java.util.Map;

/**
 * 违规分析节点：只负责“找出命中了什么”，不打分、不做决策。
 *
 * <p>把“分析”和“打分”拆成两个节点，是为了让回流（revise → analyze）只需重跑分析，
 * 打分规则变化时也不必碰分析逻辑——节点粒度就是变更边界。</p>
 */
public class AnalyzeNode implements NodeAction<PublishState> {

    public static final String ID = "analyze";

    @Override
    public Map<String, Object> apply(PublishState state) {
        var update = new HashMap<String, Object>();
        update.put(PublishState.TRACE, state.traceAfter(ID));
        update.put(PublishState.VIOLATIONS, RiskPolicy.violations(state.normalizedText()));
        update.put(PublishState.STATUS, "RUNNING");
        return update;
    }
}
