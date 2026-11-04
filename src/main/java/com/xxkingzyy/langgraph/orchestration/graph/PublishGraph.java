package com.xxkingzyy.langgraph.orchestration.graph;

import com.xxkingzyy.langgraph.orchestration.nodes.AnalyzeNode;
import com.xxkingzyy.langgraph.orchestration.nodes.EscalateNode;
import com.xxkingzyy.langgraph.orchestration.nodes.IntakeGate;
import com.xxkingzyy.langgraph.orchestration.nodes.IntakeNode;
import com.xxkingzyy.langgraph.orchestration.nodes.ManualReviewNode;
import com.xxkingzyy.langgraph.orchestration.nodes.PublishNode;
import com.xxkingzyy.langgraph.orchestration.nodes.RejectNode;
import com.xxkingzyy.langgraph.orchestration.nodes.ReviseNode;
import com.xxkingzyy.langgraph.orchestration.nodes.RiskGate;
import com.xxkingzyy.langgraph.orchestration.nodes.ScoreNode;
import com.xxkingzyy.langgraph.orchestration.state.PublishState;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.action.AsyncNodeAction;

import java.util.Map;

/**
 * 内容发布编排图：把“节点 + 边”装配成一张可执行的图。
 *
 * <p>这张图刻意覆盖后端编排最常用的四类结构：</p>
 * <ol>
 *   <li><b>线性段</b>：intake → analyze → score；</li>
 *   <li><b>条件分叉</b>：intake 后的校验门禁、score 后的风险门禁；</li>
 *   <li><b>回流（循环）</b>：revise → analyze，受 retryCount 与 recursionLimit 双重约束；</li>
 *   <li><b>多终止态</b>：publish / manual_review / escalate / reject 四个出口。</li>
 * </ol>
 */
public final class PublishGraph {

    public static final String GRAPH_NAME = "content-publish-pipeline";

    private PublishGraph() {
    }

    /** 装配（未编译）的图定义，便于单测直接检查节点与边的拓扑。 */
    public static StateGraph<PublishState> definition() throws GraphStateException {
        return new StateGraph<>(PublishState::new)
                .addNode(IntakeNode.ID, AsyncNodeAction.node_async(new IntakeNode()))
                .addNode(AnalyzeNode.ID, AsyncNodeAction.node_async(new AnalyzeNode()))
                .addNode(ScoreNode.ID, AsyncNodeAction.node_async(new ScoreNode()))
                .addNode(ReviseNode.ID, AsyncNodeAction.node_async(new ReviseNode()))
                .addNode(PublishNode.ID, AsyncNodeAction.node_async(new PublishNode()))
                .addNode(ManualReviewNode.ID, AsyncNodeAction.node_async(new ManualReviewNode()))
                .addNode(EscalateNode.ID, AsyncNodeAction.node_async(new EscalateNode()))
                .addNode(RejectNode.ID, AsyncNodeAction.node_async(new RejectNode()))

                .addEdge(StateGraph.START, IntakeNode.ID)

                // 第一条条件边：参数校验门禁
                .addConditionalEdges(IntakeNode.ID,
                        AsyncEdgeAction.edge_async(new IntakeGate()),
                        Map.of(IntakeGate.ROUTE_ANALYZE, AnalyzeNode.ID,
                                IntakeGate.ROUTE_REJECT, RejectNode.ID))

                .addEdge(AnalyzeNode.ID, ScoreNode.ID)

                // 第二条条件边：风险门禁（发布 / 人工 / 改写回流 / 升级）
                .addConditionalEdges(ScoreNode.ID,
                        AsyncEdgeAction.edge_async(new RiskGate()),
                        Map.of(RiskGate.ROUTE_PUBLISH, PublishNode.ID,
                                RiskGate.ROUTE_MANUAL_REVIEW, ManualReviewNode.ID,
                                RiskGate.ROUTE_REVISE, ReviseNode.ID,
                                RiskGate.ROUTE_ESCALATE, EscalateNode.ID))

                // 回流：改写后重新分析，而不是直接重打分（保证违规明细与分数一致）
                .addEdge(ReviseNode.ID, AnalyzeNode.ID)

                .addEdge(PublishNode.ID, StateGraph.END)
                .addEdge(ManualReviewNode.ID, StateGraph.END)
                .addEdge(EscalateNode.ID, StateGraph.END)
                .addEdge(RejectNode.ID, StateGraph.END);
    }

    public static CompiledGraph<PublishState> compile() throws GraphStateException {
        return definition().compile();
    }

    /** 执行一次编排：输入只有 contentId 与 draft，其余全部由图内节点推导。 */
    public static PublishState run(String contentId, String draft) throws Exception {
        return compile()
                .invoke(Map.<String, Object>of(
                        PublishState.CONTENT_ID, contentId,
                        PublishState.RAW_DRAFT, draft))
                .orElseThrow(() -> new IllegalStateException("编排没有产出终态"));
    }

    /** 导出 Mermaid 拓扑图，作为 Architecture.md 的证据来源。 */
    public static String mermaid() throws GraphStateException {
        return compile().getGraph(GraphRepresentation.Type.MERMAID, GRAPH_NAME).content();
    }
}
