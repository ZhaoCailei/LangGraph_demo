package com.xxkingzyy.langgraph.orchestration.nodes;

import com.xxkingzyy.langgraph.orchestration.policy.RiskPolicy;
import com.xxkingzyy.langgraph.orchestration.state.PublishState;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.HashMap;
import java.util.Map;

/**
 * 打分节点：把分析结果折算成 0-100 的风险分，作为下一步条件边的唯一输入。
 */
public class ScoreNode implements NodeAction<PublishState> {

    public static final String ID = "score";

    @Override
    public Map<String, Object> apply(PublishState state) {
        var update = new HashMap<String, Object>();
        update.put(PublishState.TRACE, state.traceAfter(ID));
        update.put(PublishState.RISK_SCORE, RiskPolicy.score(state.normalizedText()));
        update.put(PublishState.STATUS, "RUNNING");
        return update;
    }
}
