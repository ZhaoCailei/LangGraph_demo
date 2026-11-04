# 开发文档 —— 内容发布编排 Demo（LangGraph4j / Java）

> 本文按 `skills/writing-skill/SKILL.md` 的三条硬规则写：**结论先行且带证据、结构固定、读者可复现核验**。
> 阅读顺序建议：§1 结论 → §5 复现 → §4 证据链（核对）→ §7 自检。

---

## 1. 结论摘要

- **编排已跑通**：8 个节点、2 条条件边、1 条回流、4 个终止态；`mvn -q exec:java` 输出 5 个场景全部 `PASS`
  （证据：`evidence/demo-run.log:1-40`，各场景结果见 `:9`、`:16`、`:24`、`:31`、`:38`）。
- **测试全绿**：12 个用例，0 失败 0 错误（证据：`evidence/mvn-test.log` 中 `Tests run: 12, Failures: 0, Errors: 0`）。
- **条件路由符合契约**：`score < 40` 发布、`40~69` 人工、`≥ 70` 先改写、改写 2 次仍超标升级
  （证据：`src/main/java/com/xxkingzyy/langgraph/orchestration/nodes/RiskGate.java:28-34`）。
- **阈值外置**：40 / 70 / 2 三个常量集中在 `RiskPolicy.java:27`、`:29`、`:31`，改阈值不动图。
- **拓扑图由代码生成**，非手画：`PublishGraph.mermaid()`（`PublishGraph.java:94`），产物 `evidence/mermaid.txt`。

---

## 2. 背景与范围

**背景**：这是"LangGraph 架构学习 + 后端编排 demo"作业的交付物。要求读懂 State / Node / Edge 的编排模型，
并写出一条能跑通的后端编排（体现 State 传递与多 Node 流转），同时按 `writing-skill` 的写法交开发文档。

**本次范围：**

| 做 | 不做（见 `docs/contract.md` §7） |
| --- | --- |
| 一条内容发布编排（校验 → 分析 → 打分 → 四分支） | 持久化 / 断点续跑（未接 checkpointer） |
| 状态契约、打分策略、单测、运行入口 | 人工 interrupt 挂起与恢复 |
| 架构文档、契约文档、开发文档 | 并发节点、子图、流式接口 |
| 代码生成拓扑图作为证据 | 真实模型 / 规则引擎 / 鉴权 / 幂等 |

选"内容发布风控"作为场景，是因为它天然需要：**参数校验**（门禁）、**分级决策**（多分支）、
**自动修复再判**（回流）、**修不好就升级**（兜底）——正好覆盖编排的四种基本结构。

---

## 3. 方案与结构

整体架构、状态设计、设计取舍见 **`Architecture.md`**，此处只列实现层的文件职责，避免重复叙述。

| 文件 | 职责 | 行数 |
| --- | --- | --- |
| `graph/PublishGraph.java` | 装配 8 节点 + 2 条件边 + 7 静态边；提供 `compile()` / `run()` / `mermaid()` | 94 |
| `nodes/*.java` | 8 个节点，各自单一职责 | 8 个文件，23-49 行 |
| `nodes/RiskGate.java` | 风险门禁：4 路路由 | 32 |
| `nodes/IntakeGate.java` | 校验门禁：2 路路由 | 17 |
| `policy/RiskPolicy.java` | 阈值、打分、脱敏（纯函数） | 90 |
| `state/PublishState.java` | 状态键常量 + 只读访问器 + `traceAfter` | 94 |
| `DemoApplication.java` | 5 个场景 + `--diagram` 开关 | 60 |

**依赖方向**（不可逆）：`DemoApplication → PublishGraph → nodes → RiskPolicy`；
`RiskPolicy` 不 import 任何 LangGraph 类型，因此可以脱离图单独测试（`RiskPolicyTest` 5 个用例即证明这一点）。

---

## 4. 证据链

| # | 结论 | 证据位置 | 验证方式 |
| --- | --- | --- | --- |
| E1 | 正常内容走 `intake → analyze → score → publish` | `evidence/demo-run.log:4` | 跑场景 A |
| E2 | 软违规 ×2 得 60 分，进入人工复核 | `evidence/demo-run.log:12`、`:15` | 跑场景 B |
| E3 | 软违规 ×3 得 90 分，改写 1 次后降为 0 并发布 | `evidence/demo-run.log:19`、`:21` | 跑场景 C |
| E4 | 硬违规 ×2 得 100 分，改写 2 次后升级人工 | `evidence/demo-run.log:27`、`:29` | 跑场景 D |
| E5 | 空 draft 被校验门禁拦截，未进入 analyze | `evidence/demo-run.log:34` | 跑场景 E |
| E6 | 校验门禁按 `validationError` 是否为空分流 | `IntakeGate.java:16` | `PublishGraphTest.blankDraftIsRejected` |
| E7 | 风险门禁四路路由与阈值比较 | `RiskGate.java:28-34` | `PublishGraphTest` 4 条路径用例 |
| E8 | 回流落点是 analyze（保证明细与分数同步） | `PublishGraph.java:72` | `reviseOnceThenPublish` 断言完整 trace |
| E9 | 脱敏只处理软违规词 | `RiskPolicy.java:84`（`maskSoftWords`） | `RiskPolicyTest.maskSoftWordsOnly` |
| E10 | 12 个用例全绿 | `evidence/mvn-test.log` | `mvn -B test` |
| E11 | 拓扑图与代码一致（8 节点 / 2 条件节点 / 4 出口） | `evidence/mermaid.txt` | `mvn -q exec:java -Dexec.args=--diagram` |
| E12 | 干净构建可复现（`clean verify` 全绿并产出 jar） | `evidence/mvn-clean-verify.log` | `mvn -B clean verify` |

---

## 5. 复现步骤

### 5.1 前置条件

| 项 | 版本 | 检查命令 |
| --- | --- | --- |
| JDK | 17+（本机 25 实测通过） | `java -version` |
| Maven | 3.9+ | `mvn -v` |
| 网络 | 首次构建需访问 Maven Central（下载 langgraph4j 1.9.8） | `mvn -B -q compile` |

### 5.2 执行

```bash
# 1) 编译
mvn -B -q compile

# 2) 跑全部场景
mvn -B -q exec:java

# 3) 跑测试
mvn -B test

# 4) 导出拓扑图（Mermaid）
mvn -B -q exec:java -Dexec.args=--diagram
```

> Windows 控制台若出现中文乱码，加 `-Dstdout.encoding=UTF-8`（本仓库证据日志即用该参数生成）。

### 5.3 预期输出

**场景运行**（节选，完整见 `evidence/demo-run.log`）：

```text
=== Content Publish Orchestration Demo (LangGraph4j) ===

[1] A 正常内容
    trace   : intake -> analyze -> score -> publish
    status  : PUBLISHED (expected PUBLISHED)
    result  : PASS

[4] D 重度风险（硬违规 x2）
    trace   : intake -> analyze -> score -> revise -> analyze -> score -> revise -> analyze -> score -> escalate
    status  : ESCALATED (expected ESCALATED)
    decision: ESCALATE_HUMAN, riskScore: 100, retryCount: 2
    result  : PASS
```

**测试**（节选）：

```text
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

判断标准：**5 行 `result : PASS` + 测试 `Failures: 0`**，缺一即未通过。

---

## 6. 边界与限制

| 限制 | 影响 | 现状 |
| --- | --- | --- |
| 无持久化 | 进程退出后状态丢失，无法续跑 | 未接 checkpointer（`Architecture.md` §6 给出接法） |
| 无中断恢复 | `PENDING_MANUAL` 只是终态，人工结论无法回填 | 需 `interruptBefore` + `updateState` |
| 节点串行 | 未利用超步并行，吞吐有限 | 并行化需处理 `violations` 的合并（`AppenderChannel`） |
| 策略是演示级 | 词表硬编码、线性加权 | 真实系统应接模型打分 + 规则引擎 |
| 无幂等 | 同一 `contentId` 重复提交会重复发布 | 需在入口加去重 |
| 阈值是硬编码常量 | 调参需改代码重新发布 | 后续应外置为配置 |

---

## 7. 验收对照

### 7.1 作业验收项

| 验收项 | 结果 | 证据 |
| --- | --- | --- |
| 能口头讲清 `Architecture.md` 主流程 | 待现场 | `Architecture.md` §2.3 拓扑图 + §3.4 轨迹表 |
| 说清 writing-skill / dev-skill 的用途与时机 | 满足 | `skills/writing-skill/SKILL.md` §2、`skills/dev-skill/SKILL.md` §2 |
| demo 可运行 | 满足 | `evidence/demo-run.log` 5 个 PASS |
| 附仓库 / PR | 满足 | 分支 `feat/xxkingzyy-langgraph-orchestration-demo` |
| `Architecture.md` 已提交 | 满足 | 仓库根目录 |
| 开发文档按 writing-skill 写法完成 | 满足 | 本文 §1 证据、§4 证据链、§5 可复现 |

### 7.2 writing-skill 自检清单

| 清单项 | 状态 | 说明 |
| --- | --- | --- |
| 开头 5 行内有结论且带证据 | ✅ | §1 五条结论，均带 `路径:行号` 或日志行 |
| 每个"因为/所以"能对应证据链 | ✅ | §4 共 11 条，覆盖全部结论 |
| 文件引用精确到 `路径:行号` | ✅ | §4 表格逐条列出 |
| 命令可直接复制且写了预期输出 | ✅ | §5.2 / §5.3 |
| 关键数字可复算 | ✅ | 40 / 70 / 2、12 用例、5 场景 |
| 写明"不做什么"与已知限制 | ✅ | §2 范围表、§6 限制表 |
| 验收标准逐条对照 | ✅ | §7.1 |
| 图与代码一致 | ✅ | 图由 `getGraph(MERMAID)` 生成（`evidence/mermaid.txt`） |
| 推断已标注 | ✅ | 无未标注推断；"待现场"项已显式标注 |
| 新人只读文档可复现 | ✅ | §5 从干净环境写起 |

---

## 8. 维护指南：三种常见改动怎么做

| 改动 | 步骤 | 影响面 | 必改的文档 |
| --- | --- | --- | --- |
| 调阈值（如 70 → 80） | 改 `RiskPolicy.java:29` → 跑测试 | 仅策略类；`PublishGraphTest` 若无覆盖需补用例 | `docs/contract.md` §3、`Architecture.md` §5 D5 |
| 加一个节点（如"敏感图片检测"） | 新建节点类 → `PublishGraph` 里 `addNode` + 插入边 → 补测试 | 图装配 + 测试 | `Architecture.md` §2.2、`docs/contract.md` §2 |
| 加一个新终态（如 `ARCHIVED`） | 加节点 + `→ END` 边 + 路由键 | 图装配、Gate、测试、契约 | `docs/contract.md` §4（属于不兼容变更） |

**原则**：先改契约，再改代码，最后改文档；三者不一致时以契约文档为准。
