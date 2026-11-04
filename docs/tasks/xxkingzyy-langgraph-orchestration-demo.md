# 任务拆解：LangGraph 后端编排 Demo（Java）

| 项 | 内容 |
| --- | --- |
| 任务名 | `xxkingzyy-langgraph-orchestration-demo` |
| 交付分支 | `feat/xxkingzyy-langgraph-orchestration-demo` |
| 周期 | 一周（5 个工作日 + 1 天缓冲） |
| 技术栈 | Java 17、Maven 3.9、LangGraph4j 1.9.8、JUnit 5 |
| 交付物 | 可运行 demo + `Architecture.md` + `docs/dev-doc.md` + `docs/contract.md` + 本文件 |

---

## 1. 目标（可验收的表述）

1. 用 LangGraph4j 实现一条**能跑通**的后端编排：至少体现 State 传递、多 Node 流转、条件分叉；
2. 讲清楚自己对 State / Node / Edge 编排模型的理解（`Architecture.md`，非抄模板）；
3. 开发文档按 `writing-skill` 的"证据 / 结构 / 可核验"三条硬规则写；
4. 全部验收项可被第三方用命令复现，不依赖口头说明。

---

## 2. 范围

**做：**

- 一条内容发布编排：校验 → 分析 → 打分 → 四路分支（发布 / 人工 / 改写回流 / 升级）；
- 状态契约 + 打分策略 + 单测 + 运行脚本；
- 三类文档（架构 / 契约 / 开发）。

**不做：**

- 持久化、断点续跑、人工 interrupt 挂起；
- 并发节点、子图、流式接口；
- 真实模型调用、数据库、消息队列、鉴权。

> 范围写清的理由：一周的作业，做"一条打透的窄流程"比"五条半成品"更能证明理解（见 `dev-skill` §3）。

---

## 3. 拆解与状态

| 编号 | 任务 | 产出物 | 前置 | 状态 |
| --- | --- | --- | --- | --- |
| T1 | 选型与探针：确认 LangGraph4j 版本与最小 API（`StateGraph` / `NodeAction` / `AsyncEdgeAction`） | 依赖可解析、空工程可编译 | — | ✅ 完成 |
| T2 | 定契约：输入 / 状态 / 终态 / 不变量 | `docs/contract.md` | T1 | ✅ 完成 |
| T3 | 场景设计：选一个能体现分叉与回流的业务（内容发布风控） | 5 个场景与预期终态 | T2 | ✅ 完成 |
| T4 | 实现状态与策略 | `PublishState`、`RiskPolicy` | T2 | ✅ 完成 |
| T5 | 实现 8 个节点 + 2 条条件边 | `nodes/*.java` | T4 | ✅ 完成 |
| T6 | 装配图与入口 | `PublishGraph`、`DemoApplication` | T5 | ✅ 完成 |
| T7 | 测试矩阵：正常 / 分叉 / 回流 / 升级 / 非法输入 | 12 个用例全绿 | T6 | ✅ 完成 |
| T8 | 文档：架构 + 开发文档 + README + skill 文档 | `Architecture.md`、`docs/dev-doc.md`、`README.md`、`skills/*` | T7 | ✅ 完成 |
| T9 | 交付：分支、提交、证据归档、自检 | `evidence/`、分支 `feat/...` | T8 | ✅ 完成 |

---

## 4. 里程碑（一周）

| 天 | 计划 | 实际落点 |
| --- | --- | --- |
| D1 | 选型 + 契约 + 场景 | T1-T3 |
| D2 | 状态 + 策略 + 主干三节点 | T4-T5（部分） |
| D3 | 分支与回流节点 + 图装配 | T5-T6 |
| D4 | 测试矩阵 + 修复 | T7 |
| D5 | 文档（架构 / 开发 / README） | T8 |
| D6 | 缓冲：证据归档、自检、提交 | T9 |

---

## 5. 验收标准（逐条对照）

| 验收项 | 判定方式 | 结果 |
| --- | --- | --- |
| 能口头讲清 `Architecture.md` 的主流程 | 对着 §2.3 拓扑图讲 `intake → analyze → score → 四分支` | 待现场 |
| 说清 writing-skill / dev-skill 各自干什么、何时用 | 各自 §2"何时使用 / 何时不用" | ✅ |
| demo 可运行 | `mvn -q exec:java` 输出 5 个 PASS（`evidence/demo-run.log`） | ✅ |
| 附仓库或 PR 链接 | 本地仓库 + 分支 `feat/xxkingzyy-langgraph-orchestration-demo` | ✅ |
| `Architecture.md` 已提交 | 仓库根目录 | ✅ |
| 开发文档已提交且按 writing-skill 写法完成 | `docs/dev-doc.md` §7 自检清单逐条打勾 | ✅ |

---

## 6. 风险与应对

| 风险 | 影响 | 应对 | 结果 |
| --- | --- | --- | --- |
| LangGraph4j 版本 API 变动（1.9.x 与旧版差异大） | 编译失败、样例跑不通 | 直接读 `langgraph4j-core-1.9.8-sources.jar` 确认签名，不凭记忆写 | 已规避 |
| 回流写成死循环 | 进程挂死 | `MAX_RETRY` + 框架 `recursionLimit` 双保险 | 已规避 |
| 中文控制台乱码导致日志不可用 | 文档证据不可读 | 运行参数固定 `-Dstdout.encoding=UTF-8`，日志落文件 | 已规避 |
| 文档写成"README 交差" | 验收不通过 | 按 writing-skill 骨架 + 证据链表格写 | 已规避 |
| 范围蔓延（想加持久化/并发） | 一周做不完 | 契约 §7 明确"不做"，留作演进方向 | 已规避 |

---

## 7. 交付清单

```
Architecture.md                                  # 架构（作业核心）
README.md                                        # 快速开始
docs/contract.md                                 # 契约
docs/dev-doc.md                                  # 开发文档（writing-skill 写法）
docs/tasks/xxkingzyy-langgraph-orchestration-demo.md   # 本文件
skills/writing-skill/SKILL.md                    # skill 1
skills/dev-skill/SKILL.md                        # skill 2
evidence/demo-run.log                            # 运行输出
evidence/mvn-test.log                            # 测试报告
evidence/mermaid.txt                             # 代码生成的拓扑图
src/main/java/com/xxkingzyy/langgraph/orchestration/...
src/test/java/com/xxkingzyy/langgraph/orchestration/...
```
