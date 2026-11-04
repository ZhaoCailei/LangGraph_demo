package com.xxkingzyy.langgraph.orchestration.policy;

import java.util.ArrayList;
import java.util.List;

/**
 * 风险策略：把“审核规则”从编排图里抽出来，保证节点只做流转、不写死业务阈值。
 *
 * <p>规则刻意做得可解释、可断言（便于单测与文档证据）：</p>
 * <ul>
 *   <li>硬违规词（{@link #HARD_WORDS}）：无法自动改写，命中一次 +50 分；</li>
 *   <li>软违规词（{@link #SOFT_WORDS}）：可通过改写脱敏，命中一次 +30 分；</li>
 *   <li>文本过短（不足 {@link #MIN_LENGTH} 字）：+10 分；</li>
 *   <li>包含外链（http/https）：+15 分；</li>
 *   <li>总分封顶 {@link #MAX_SCORE}。</li>
 * </ul>
 */
public final class RiskPolicy {

    public static final List<String> HARD_WORDS = List.of("诈骗", "违禁");
    public static final List<String> SOFT_WORDS = List.of("赌博", "暴力", "色情");

    public static final int MIN_LENGTH = 15;
    public static final int MAX_SCORE = 100;

    /** 低于该分数直接发布。 */
    public static final int THRESHOLD_AUTO_PUBLISH = 40;
    /** 达到该分数进入自动改写回流；改写次数用尽仍超标则升级人工。 */
    public static final int THRESHOLD_REVISE = 70;
    /** 自动改写最大重试次数（对应“双回流”里的自动回流上限）。 */
    public static final int MAX_RETRY = 2;

    public static final String MASK = "**";

    private RiskPolicy() {
        // 纯静态策略类
    }

    /** 命中明细，形如 {@code HARD:诈骗}、{@code SOFT:赌博}。 */
    public static List<String> violations(String text) {
        var target = text == null ? "" : text;
        var hits = new ArrayList<String>();
        for (String hard : HARD_WORDS) {
            if (target.contains(hard)) {
                hits.add("HARD:" + hard);
            }
        }
        for (String soft : SOFT_WORDS) {
            if (target.contains(soft)) {
                hits.add("SOFT:" + soft);
            }
        }
        return List.copyOf(hits);
    }

    public static boolean hasHardViolation(String text) {
        var target = text == null ? "" : text;
        return HARD_WORDS.stream().anyMatch(target::contains);
    }

    public static int score(String text) {
        var target = text == null ? "" : text;
        int score = 0;
        for (String hard : HARD_WORDS) {
            if (target.contains(hard)) {
                score += 50;
            }
        }
        for (String soft : SOFT_WORDS) {
            if (target.contains(soft)) {
                score += 30;
            }
        }
        if (target.length() < MIN_LENGTH) {
            score += 10;
        }
        if (target.contains("http://") || target.contains("https://")) {
            score += 15;
        }
        return Math.min(MAX_SCORE, score);
    }

    /** 只对软违规词做脱敏，硬违规词保持原样（这正是升级人工的原因）。 */
    public static String maskSoftWords(String text) {
        var result = text == null ? "" : text;
        for (String soft : SOFT_WORDS) {
            result = result.replace(soft, MASK);
        }
        return result;
    }
}
