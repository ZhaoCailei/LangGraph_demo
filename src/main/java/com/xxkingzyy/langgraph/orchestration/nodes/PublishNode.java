package com.xxkingzyy.langgraph.orchestration.nodes;

import com.xxkingzyy.langgraph.orchestration.state.PublishState;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.HashMap;
import java.util.Map;

/**
 * 发布节点（终止节点）：生成发布地址并落终态。
 *
 * <p>真实系统里这里会调用发布服务；示例用确定性 URL 便于断言与文档引用。</p>
 */
public class PublishNode implements NodeAction<PublishState> {

    public static final String ID = "publish";

    @Override
    public Map<String, Object> apply(PublishState state) {
        var update = new HashMap<String, Object>();
        update.put(PublishState.TRACE, state.traceAfter(ID));
        update.put(PublishState.STATUS, "PUBLISHED");
        update.put(PublishState.DECISION, "AUTO_PUBLISH");
        update.put(PublishState.PUBLISH_URL, "https://publish.local/content/" + state.contentId());
        return update;
    }
}
