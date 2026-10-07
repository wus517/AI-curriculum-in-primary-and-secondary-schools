# AI 学习平台契约入口

当前契约状态：核心契约 0.1.1 已冻结。Java 公共接口与权限基础已实现；OpenAPI 描述全部目标接口，不表示全部端点已经可用。T01 服务及 T02 基础服务已有代码和隔离测试，实际进度见各任务记录。真实模型供应商待用户配置，不宣称 M2 通过。

T00 提供以下产物，对应 `com.example.oj.platform` 的公共接口与 DTO：

- `openapi.yaml`：所有新增 REST 的完整请求、响应、错误、权限和二进制下载说明。
- `java-services.md`：公共服务签名、DTO 字段、状态和调用职责。
- `mcp-tools.json`：六个工具的 inputSchema、outputSchema 和错误约定。
- `stream-events.md`：POST SSE 的事件、序号、状态、取消和幂等约定。
- `data-model.md`：四个增量迁移的字段、类型、唯一键、更新与删除语义。
- `examples/`：摘要、提交、检索、工具错误、引用、流式事件的可验证样例。
- `DECISIONS.md`：身份策略、SDK 版本、模型、解析和索引的决定及依据。
- `CHANGELOG.md`：契约版本、变更、兼容性和受影响任务。
- `frozen-ddl.sql`：字段与约束蓝图，不是已执行的迁移。
- `types.ts`、`generate-types.mjs`：从 OpenAPI 生成的 TypeScript 类型及一致性检查。

验证入口：`./mvnw test`、`node docs/ai-learning/contracts/generate-types.mjs --check`。SDK 使用真实 HTTP 往返的合成工具验证，PDF/PPTX 使用实际字节夹具验证；均不代替业务模块集成验收。模型供应商配置和数据库迁移仍需单独验收。

业务窗口只读此目录。需要变更时在本人任务文件中提出，由集成负责人统一更新。
