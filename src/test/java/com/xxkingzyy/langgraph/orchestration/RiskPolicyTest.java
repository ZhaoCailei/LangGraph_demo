package com.xxkingzyy.langgraph.orchestration;

import com.xxkingzyy.langgraph.orchestration.policy.RiskPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 策略单测：把“打分/脱敏”当成纯函数锁定，图的行为测试才有稳定的地基。
 */
class RiskPolicyTest {

    @Test
    @DisplayName("干净文本得 0 分")
    void cleanTextScoresZero() {
        assertEquals(0, RiskPolicy.score("这是一段长度足够且没有任何违规词的中性文本。"));
    }

    @Test
    @DisplayName("每个软违规词 +30，每个硬违规词 +50")
    void weightsPerHit() {
        assertEquals(30, RiskPolicy.score("这里出现赌博一词，但整体长度足够长，不触发长度惩罚。"));
        assertEquals(50, RiskPolicy.score("这里出现诈骗一词，但整体长度足够长，不触发长度惩罚。"));
        assertEquals(60, RiskPolicy.score("赌博和暴力两个词同时出现在一段足够长的中性描述文本里。"));
    }

    @Test
    @DisplayName("短文本 +10、外链 +15，且总分封顶 100")
    void lengthAndUrlPenalty() {
        assertEquals(10, RiskPolicy.score("短文本"));
        assertEquals(15, RiskPolicy.score("这是一段足够长的文本并且带 https://example.com 外链"));
        assertEquals(100, RiskPolicy.score("诈骗 违禁 赌博 暴力 色情 https://x.y 短"));
    }

    @Test
    @DisplayName("脱敏只处理软违规词，硬违规词保持原样")
    void maskSoftWordsOnly() {
        var masked = RiskPolicy.maskSoftWords("赌博与诈骗并存");
        assertEquals("**与诈骗并存", masked);
        assertTrue(RiskPolicy.hasHardViolation(masked), "硬违规词不该被脱敏");
    }

    @Test
    @DisplayName("阈值常量与文档契约一致")
    void thresholdsMatchContract() {
        assertEquals(40, RiskPolicy.THRESHOLD_AUTO_PUBLISH);
        assertEquals(70, RiskPolicy.THRESHOLD_REVISE);
        assertEquals(2, RiskPolicy.MAX_RETRY);
    }
}
