package com.xxkingzyy.langgraph.orchestration.nodes;

import com.xxkingzyy.langgraph.orchestration.state.PublishState;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.HashMap;
import java.util.Map;

/**
 * 升级人工节点（终止节点）：自动改写次数用尽仍未降到阈值以下时兜底。
 */
public class EscalateNode implements NodeAction<PublishState> {

    public static final String ID = "escalate";

    @Override
    public Map<String, Object> apply(PublishState state) {
        var update = new HashMap<String, Object>();
        update.put(PublishState.TRACE, state.traceAfter(ID));
        update.put(PublishState.STATUS, "ESCALATED");
        update.put(PublishState.DECISION, "ESCALATE_HUMAN");
        return update;
    }
}
