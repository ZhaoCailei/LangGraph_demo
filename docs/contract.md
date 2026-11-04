# 契约（Contract）——内容发布编排 Demo

> 本文件是**实现之前先定下**的接口约定：输入什么、状态怎么变、什么时候结束、什么条件永远成立。
> 代码是契约的实现，测试是契约的验证；三者不一致时，以本文件为准并回改代码。
> 对应 `dev-skill` 的"第 1 段：契约先行"（见 `skills/dev-skill/SKILL.md` §3）。

---

## 1. 输入契约

调用入口：`PublishGraph.run(String contentId, String draft)`（`graph/PublishGraph.java:85`），
等价于 `compiled.invoke(Map.of("contentId", ..., "rawDraft", ...))`。

| 字段 | 状态键 | 类型 | 必填 | 约束 | 非法时行为 |
| --- | --- | --- | --- | --- | --- |
| 内容 ID | `contentId` | String | 是 | 非空白；首尾空白会被 trim | 落 `REJECTED` + `validationError="contentId 不能为空"` |
| 原始草稿 | `rawDraft` | String | 是 | 非空白；连续空白会被折叠为单个空格 | 落 `REJECTED` + `validationError="draft 不能为空"` |

- 除上述两个键外，**其余状态键全部由图内节点推导**，调用方不应传入；
- 调用方传入的多余键会原样进入初始状态（框架行为），但契约不保证其被使用。

---

## 2. 状态契约

| 状态键 | 类型 | 写入节点 | 只读于 | 语义 | 默认值 | 可空 |
| --- | --- | --- | --- | --- | --- | --- |
| `contentId` | String | intake | 其他全部 | 内容唯一标识 | `""` | 否 |
| `rawDraft` | String | （入参） | 全部 | 原始草稿，**永不修改** | `""` | 否 |
| `normalizedText` | String | intake / revise | analyze / score | 打分唯一依据 | 回退 `rawDraft` | 否 |
| `violations` | List&lt;String&gt; | analyze | revise / 人工 | 命中明细，`HARD:`/`SOFT:` 前缀 | `[]` | 否 |
| `riskScore` | int | score | RiskGate | 0-100 风险分 | `0` | 否 |
| `retryCount` | int | revise | RiskGate | 自动改写次数 | `0` | 否 |
| `status` | String | 各节点 | 调用方 | 终态判别字段 | `"CREATED"` | 否 |
| `decision` | String | 各节点 | 调用方 | 决策原因（审计用） | `"PENDING"` | 否 |
| `publishUrl` | String | publish | 调用方 | 发布地址 | `""` | 否 |
| `validationError` | String | intake | IntakeGate | 校验失败原因 | `""` | 否 |
| `trace` | List&lt;String&gt; | 全部节点 | 调用方 / 测试 | 节点访问序列 | `[]` | 否 |

**写入规则**：节点只能返回**局部更新 Map**，不得原地修改传入的状态对象；`trace` 必须返回新列表
（`state/PublishState.java:87` 的 `traceAfter`）。

---

## 3. 打分规则契约

实现：`policy/RiskPolicy.java`。

| 规则 | 分值 | 可自动修复 |
| --- | --- | --- |
| 硬违规词命中（`诈骗`、`违禁`） | +50 / 个 | 否（保持原样） |
| 软违规词命中（`赌博`、`暴力`、`色情`） | +30 / 个 | 是（替换为 `**`） |
| 文本长度 &lt; 15 | +10 | 否（改写不补足长度） |
| 含 `http://` 或 `https://` | +15 | 否 |
| 总分 | `min(100, 累加)` | — |

阈值：

| 常量 | 值 | 含义 | 证据 |
| --- | --- | --- | --- |
| `THRESHOLD_AUTO_PUBLISH` | 40 | 低于此值直接发布 | `RiskPolicy.java:27` |
| `THRESHOLD_REVISE` | 70 | 达到此值进入自动改写 | `RiskPolicy.java:29` |
| `MAX_RETRY` | 2 | 自动改写最大次数 | `RiskPolicy.java:31` |

---

## 4. 终态契约

`status` 只有以下 5 个取值，其中 4 个为终止态：

| status | decision | 进入条件 | 是否终止 | 附带字段 |
| --- | --- | --- | --- | --- |
| `CREATED` | `PENDING` | 初始状态（调用方创建） | 否 | — |
| `RUNNING` | `PENDING` | 校验通过后 | 否 | — |
| `PUBLISHED` | `AUTO_PUBLISH` | `riskScore < 40` | 是 | `publishUrl = https://publish.local/content/{contentId}` |
| `PENDING_MANUAL` | `MANUAL_REVIEW` | `40 ≤ riskScore < 70` | 是 | `violations` 非空 |
| `ESCALATED` | `ESCALATE_HUMAN` | `riskScore ≥ 70` 且 `retryCount ≥ MAX_RETRY` | 是 | `violations` 含 `HARD:` 项 |
| `REJECTED` | `REJECT_INVALID_INPUT` | 入参校验失败 | 是 | `validationError` 非空 |

路由实现：`nodes/RiskGate.java:26-34`（风险门禁）、`nodes/IntakeGate.java:16`（校验门禁）。

---

## 5. 不变量（Invariants）

任何时刻都必须成立；测试逐条覆盖（`src/test/java/.../PublishGraphTest.java`）。

- **INV-1**：`retryCount ≤ MAX_RETRY`（2）。达到上限后不得再进入 revise。
- **INV-2**：`status` 只能是 §4 表中的 5 个值之一，不存在第六种。
- **INV-3**：`PUBLISHED` 时 `publishUrl` 非空且包含 `contentId`。
- **INV-4**：`REJECTED` 时 `validationError` 非空，且 `trace` 中**不包含** `analyze`/`score`。
- **INV-5**：`riskScore` 始终在 `[0, 100]` 闭区间内。
- **INV-6**：`trace` 的首元素恒为 `"intake"`，末元素恒为终态对应节点。
- **INV-7**：`normalizedText` 一经 revise 修改，`violations` 必须在下一次 analyze 中重算（不允许复用旧值）。

---

## 6. 失败与异常语义

| 情形 | 语义 | 理由 |
| --- | --- | --- |
| 入参非法 | 落 `REJECTED` 终态，**不抛异常** | 业务拒绝 ≠ 系统故障，调用方可直接读 `status` 分支 |
| 节点内部未预期异常 | 由框架包装为 `GraphRunnerException` 抛出 | 属于 bug，应快速失败并暴露堆栈 |
| 循环次数超框架上限 | 框架抛异常（默认 `recursionLimit=25`） | 说明图的终止条件写错了，属于设计缺陷 |

---

## 7. 未覆盖（明确不做）

1. 不做持久化与断点续跑（未接 checkpointer）；
2. 不做人工介入挂起（`PENDING_MANUAL` 只是落终态，没有 interrupt/resume）；
3. 不做并发审核（节点串行执行）；
4. 不做幂等（同一 `contentId` 重复提交会产生重复结果）；
5. 不做鉴权、限流、超时与熔断；
6. 不接真实模型与规则引擎（打分为演示级线性规则）。

---

## 8. 版本与变更记录

| 版本 | 变更 | 兼容性 |
| --- | --- | --- |
| v0.1.0 | 初版：输入/状态/终态/不变量/打分规则 | — |

**变更规则**：新增状态键或终态属于**不兼容变更**，必须同步更新本文件、`Architecture.md` §4 与测试矩阵。
