package com.xxkingzyy.langgraph.orchestration.nodes;

import com.xxkingzyy.langgraph.orchestration.state.PublishState;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.HashMap;
import java.util.Map;

/**
 * 人工复核节点（终止节点）：把内容投递到风控复核队列，等待人工处理。
 */
public class ManualReviewNode implements NodeAction<PublishState> {

    public static final String ID = "manual_review";

    @Override
    public Map<String, Object> apply(PublishState state) {
        var update = new HashMap<String, Object>();
        update.put(PublishState.TRACE, state.traceAfter(ID));
        update.put(PublishState.STATUS, "PENDING_MANUAL");
        update.put(PublishState.DECISION, "MANUAL_REVIEW");
        return update;
    }
}
