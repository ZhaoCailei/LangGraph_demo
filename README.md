# langgraph-orchestration-demo

基于 **LangGraph4j 1.9.8 + Java 17 + Maven** 的后端编排最小可运行示例：
把"内容发布风控"拆成 State（状态）+ 8 个 Node（节点）+ 2 条条件边 + 1 条回流，覆盖 4 个终止态。

> 这是"LangGraph 架构学习 + 后端编排 demo"作业的交付仓库。
> 架构理解见 [`Architecture.md`](Architecture.md)，接口约定见 [`docs/contract.md`](docs/contract.md)，
> 开发文档见 [`docs/dev-doc.md`](docs/dev-doc.md)。

---

## 1. 这张图长什么样

```text
intake ──┬─(合法)──> analyze ──> score ──┬─(<40)──────────> publish ────────> END
         │                                ├─(40~69)────────> manual_review ──> END
         └─(非法)──> reject ──> END       ├─(>=70, retry<2)─> revise ──┐
                                          └─(>=70, retry>=2)> escalate ──> END
                                                                    │
                                          analyze <─────────────────┘（回流）
```

拓扑图由代码生成：`mvn -q exec:java -Dexec.args=--diagram`，产物见 [`evidence/mermaid.txt`](evidence/mermaid.txt)。

---

## 2. 快速开始

```bash
# 环境：JDK 17+、Maven 3.9+
mvn -B -q compile      # 编译
mvn -B -q exec:java    # 跑 5 个场景
mvn -B test            # 跑 12 个单测
```

Windows 控制台中文乱码时加 `-Dstdout.encoding=UTF-8`。

预期结果：5 个场景全部 `PASS`，测试 `Tests run: 12, Failures: 0`。

---

## 3. 五个场景

| 场景 | 输入 | 轨迹 | 终态 |
| --- | --- | --- | --- |
| A 正常 | 无违规、长度足够 | `intake → analyze → score → publish` | `PUBLISHED` |
| B 轻度 | 软违规 ×2 | `intake → analyze → score → manual_review` | `PENDING_MANUAL` |
| C 中度 | 软违规 ×3 | `… → revise → analyze → score → publish` | `PUBLISHED`（retry=1） |
| D 重度 | 硬违规 ×2 | `… → revise → … → revise → … → escalate` | `ESCALATED`（retry=2） |
| E 非法 | draft 为空 | `intake → reject` | `REJECTED` |

真实输出见 [`evidence/demo-run.log`](evidence/demo-run.log)。

---

## 4. 仓库结构

```
.
├── Architecture.md                          # 架构说明（框架模型 + 本 demo 映射 + 设计取舍）
├── README.md                                # 本文件
├── pom.xml                                  # Maven 配置（langgraph4j-core 1.9.8、JUnit 5）
├── docs/
│   ├── contract.md                          # 输入/状态/终态/不变量契约
│   ├── dev-doc.md                           # 开发文档（按 writing-skill 写法）
│   └── tasks/xxkingzyy-langgraph-orchestration-demo.md   # 任务拆解与验收
├── skills/
│   ├── writing-skill/SKILL.md               # 文档写作技能（证据 / 结构 / 可核验）
│   └── dev-skill/SKILL.md                   # 研发交付规范（契约 / 实现 / 验证 / 交付）
├── evidence/                                # 运行与测试证据（日志、拓扑图）
│   ├── demo-run.log
│   ├── mvn-test.log
│   ├── mvn-clean-verify.log
│   └── mermaid.txt
└── src/
    ├── main/java/com/xxkingzyy/langgraph/orchestration/
    │   ├── DemoApplication.java             # 入口：5 个场景 / --diagram
    │   ├── graph/PublishGraph.java          # 图装配（唯一 new 节点的地方）
    │   ├── nodes/                           # 8 个节点 + 2 个条件边
    │   ├── policy/RiskPolicy.java           # 阈值 / 打分 / 脱敏
    │   └── state/PublishState.java          # 状态契约
    └── test/java/com/xxkingzyy/langgraph/orchestration/
        ├── PublishGraphTest.java            # 7 个编排路径用例
        └── RiskPolicyTest.java              # 5 个策略用例
```

---

## 5. 核心设计

| 设计 | 一句话说明 |
| --- | --- |
| 状态不可变 | 节点只返回局部更新 Map，`trace` 显式返回新列表 |
| 节点单一职责 | 校验 / 分析 / 打分 / 改写 / 发布 / 人工 / 升级 / 拒绝，各自一个类 |
| 策略外置 | 40 / 70 / 2 全部集中在 `RiskPolicy`，改阈值不动图 |
| 双保险防死循环 | 业务上限 `MAX_RETRY=2` + 框架 `recursionLimit=25` |
| 回流指向 analyze | 保证 `violations` 与 `riskScore` 基于同一份文本同步重算 |
| 显式终止 | 4 个出口各自 `addEdge(exit, END)`，不依赖"没有出边" |

---

## 6. 文档索引

| 想了解 | 读这里 |
| --- | --- |
| LangGraph 的 State / Node / Edge 怎么理解 | [`Architecture.md`](Architecture.md) §1 |
| 这张图的结构与设计取舍 | [`Architecture.md`](Architecture.md) §2、§5 |
| 输入输出、状态字段、不变量 | [`docs/contract.md`](docs/contract.md) |
| 怎么复现、证据在哪 | [`docs/dev-doc.md`](docs/dev-doc.md) §4、§5 |
| 任务怎么拆、验收怎么对 | [`docs/tasks/xxkingzyy-langgraph-orchestration-demo.md`](docs/tasks/xxkingzyy-langgraph-orchestration-demo.md) |
| 两个 skill 分别管什么 | [`skills/writing-skill/SKILL.md`](skills/writing-skill/SKILL.md)、[`skills/dev-skill/SKILL.md`](skills/dev-skill/SKILL.md) |

---

## 7. 已知限制

无持久化、无中断恢复、无并行、无幂等、策略为演示级——完整清单与演进方向见
[`Architecture.md`](Architecture.md) §6、§7。
