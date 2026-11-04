package com.xxkingzyy.langgraph.orchestration.nodes;

import com.xxkingzyy.langgraph.orchestration.state.PublishState;
import org.bsc.langgraph4j.action.EdgeAction;

/**
 * 校验门禁（第一条条件边）：入参合法才进入分析，否则直接落到 reject。
 */
public class IntakeGate implements EdgeAction<PublishState> {

    public static final String ROUTE_ANALYZE = "analyze";
    public static final String ROUTE_REJECT = "reject";

    @Override
    public String apply(PublishState state) {
        return state.validationError().isBlank() ? ROUTE_ANALYZE : ROUTE_REJECT;
    }
}
