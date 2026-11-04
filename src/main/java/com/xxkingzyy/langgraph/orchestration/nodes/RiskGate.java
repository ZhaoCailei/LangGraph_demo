package com.xxkingzyy.langgraph.orchestration.nodes;

import com.xxkingzyy.langgraph.orchestration.policy.RiskPolicy;
import com.xxkingzyy.langgraph.orchestration.state.PublishState;
import org.bsc.langgraph4j.action.EdgeAction;

/**
 * 风险门禁（第二条条件边，也是整个图的分叉核心）。
 *
 * <p>路由规则：</p>
 * <pre>
 * score &lt; 40                 → publish（自动发布）
 * 40 &lt;= score &lt; 70           → manual_review（进人工复核队列）
 * score &gt;= 70 且 retry &lt; 2   → revise（自动改写后回流到 analyze）
 * score &gt;= 70 且 retry &gt;= 2  → escalate（改写次数用尽，升级人工）
 * </pre>
 */
public class RiskGate implements EdgeAction<PublishState> {

    public static final String ROUTE_PUBLISH = "publish";
    public static final String ROUTE_MANUAL_REVIEW = "manual_review";
    public static final String ROUTE_REVISE = "revise";
    public static final String ROUTE_ESCALATE = "escalate";

    @Override
    public String apply(PublishState state) {
        int score = state.riskScore();
        if (score >= RiskPolicy.THRESHOLD_REVISE) {
            return state.retryCount() < RiskPolicy.MAX_RETRY ? ROUTE_REVISE : ROUTE_ESCALATE;
        }
        if (score >= RiskPolicy.THRESHOLD_AUTO_PUBLISH) {
            return ROUTE_MANUAL_REVIEW;
        }
        return ROUTE_PUBLISH;
    }
}
