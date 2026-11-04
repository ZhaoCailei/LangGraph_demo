package com.xxkingzyy.langgraph.orchestration;

import com.xxkingzyy.langgraph.orchestration.graph.PublishGraph;
import com.xxkingzyy.langgraph.orchestration.state.PublishState;

import java.util.List;

/**
 * 演示入口：跑 5 个场景，覆盖图里全部出口。
 *
 * <pre>
 * mvn -q compile exec:java                 # 跑全部场景
 * mvn -q compile exec:java -Dexec.args="--diagram"   # 只打印 Mermaid 拓扑图
 * </pre>
 */
public final class DemoApplication {

    private DemoApplication() {
    }

    record Scenario(String name, String contentId, String draft, String expectedStatus) {
    }

    private static final List<Scenario> SCENARIOS = List.of(
            new Scenario("A 正常内容", "c-1001",
                    "LangGraph4j 把后端编排拆成 State、Node、Edge 三件事，便于分治与回放。",
                    "PUBLISHED"),
            new Scenario("B 轻度风险（软违规 x2）", "c-1002",
                    "这段文本包含赌博与暴力描述，需要人工确认后才会展示给用户。",
                    "PENDING_MANUAL"),
            new Scenario("C 中度风险（软违规 x3）", "c-1003",
                    "内容涉及赌博、暴力、色情等表述，交给自动改写做脱敏处理即可。",
                    "PUBLISHED"),
            new Scenario("D 重度风险（硬违规 x2）", "c-1004",
                    "疑似诈骗话术，并涉及违禁品交易，自动改写无法消除，需要升级人工。",
                    "ESCALATED"),
            new Scenario("E 非法入参（空 draft）", "c-1005", "   ", "REJECTED"));

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && "--diagram".equals(args[0])) {
            System.out.println(PublishGraph.mermaid());
            return;
        }

        System.out.println("=== Content Publish Orchestration Demo (LangGraph4j) ===");
        int index = 1;
        for (Scenario scenario : SCENARIOS) {
            PublishState state = PublishGraph.run(scenario.contentId(), scenario.draft());
            boolean pass = scenario.expectedStatus().equals(state.status());
            System.out.printf("%n[%d] %s%n", index++, scenario.name());
            System.out.println("    trace   : " + String.join(" -> ", state.trace()));
            System.out.println("    status  : " + state.status() + " (expected " + scenario.expectedStatus() + ")");
            System.out.println("    decision: " + state.decision() + ", riskScore: " + state.riskScore()
                    + ", retryCount: " + state.retryCount());
            System.out.println("    detail  : " + state.summary());
            if (!state.publishUrl().isBlank()) {
                System.out.println("    url     : " + state.publishUrl());
            }
            System.out.println("    result  : " + (pass ? "PASS" : "FAIL"));
        }
        System.out.println("\n=== done ===");
    }
}
