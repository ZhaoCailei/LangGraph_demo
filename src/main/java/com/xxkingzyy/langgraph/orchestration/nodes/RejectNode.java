package com.xxkingzyy.langgraph.orchestration.nodes;

import com.xxkingzyy.langgraph.orchestration.state.PublishState;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.HashMap;
import java.util.Map;

/**
 * 拒绝节点（终止节点）：入参非法时直接结束，不再进入风险分析。
 */
public class RejectNode implements NodeAction<PublishState> {

    public static final String ID = "reject";

    @Override
    public Map<String, Object> apply(PublishState state) {
        var update = new HashMap<String, Object>();
        update.put(PublishState.TRACE, state.traceAfter(ID));
        update.put(PublishState.STATUS, "REJECTED");
        update.put(PublishState.DECISION, "REJECT_INVALID_INPUT");
        return update;
    }
}
