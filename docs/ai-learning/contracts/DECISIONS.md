# Decisions 0.1.1

Evidence is restricted to repository code and downloaded dependency code, as requested. Development documents express requirements, never evidence that a feature exists. No external articles, latest-version claims or account assumptions are used.

| Decision | Code evidence and verification |
|---|---|
| Keep Java 17 / Spring Boot 3.3.5 | pom.xml; actual javac and existing application context test |
| Session identity, user/admin roles | UserServiceImpl, User; reload the account before constructing StudentContext |
| Course owner uses teacherId; draft/published/offline | Course, CourseStatusEnum, CourseServiceImpl |
| Problem difficulty 1..3, string IDs only at new boundaries | Problem, sql/oj.sql, ContractIds |
| MCP SDK 0.11.3; Streamable HTTP protocol 2025-03-26 | Downloaded io.modelcontextprotocol.sdk:mcp:0.11.3 bytecode and sources; HttpServletStreamableServerTransportProvider.protocolVersions; real compatibility test |
| Avoid SDK 0.10.0 for this contract | Actual Tool and CallToolResult signatures lack outputSchema/structuredContent |
| Do not derive protocol from LATEST alone | 0.11.3 McpSchema.LATEST_PROTOCOL_VERSION is 2025-03-26; actual transport supported-version list and negotiated initialization are checked |
| New APIs map HTTP statuses, legacy APIs remain compatible | BaseResponse, GlobalExceptionHandler, PlatformResponse, PlatformExceptionHandler |
| Retrieval baseline KEYWORD | Contract selection; no vector index or embedding dependency currently exists in pom.xml |
| PDFBox 3.0.5 / POI OOXML 5.4.1 | Installed through pom.xml; ParserDependencyCompatibilityTest round-trips actual two-page PDF and two-slide PPTX bytes and locates second-page/slide text. This proves dependency compatibility, not T03 ingestion/retrieval |
| Real model disabled pending user configuration | Current code contains no model adapter, model credential or provider selection. User was asked for supplier/model/API URL. No API fees are triggered by default tests |
| Practice accepted uses all personal submission sources | User confirmed all course/independent history; CurriculumService query and TrackingServiceTest demonstrate actual own-only behavior. This is a user business decision, not an inferred historical rule |
| First terminal result is final in this version | JudgedResultService locks the row, saves revision 1 and event in one transaction; duplicate/late callbacks do not overwrite it. Rejudging needs a future explicit design |
| New Instant timestamps persist as UTC | UtcInstantTypeHandler uses explicit UTC JDBC Calendar, registered only for Instant; existing Date fields remain unchanged. Actual MySQL timezone acceptance is pending |

Production MCP topology for T04: same application, SDK Servlet transport at an internal endpoint, actual SDK Client over a loopback-configured base URL. Restrict network exposure at the endpoint, validate a short-lived signature per call, and never store a current student in a shared client. Signing key is server environment configuration; request metadata/context is not a model tool argument. T04 must verify authentication errors and two-user concurrent calls. T00's unauthenticated compatibility fixture exists only in test configuration and is not a production endpoint.

Dependency management stays with the existing Spring Boot BOM. Effective resolved versions and successful SDK test, rather than an untested advertised dependency list, determine compatibility. There is no framework upgrade as part of T00. The downloaded SDK declares newer provided Servlet dependencies; the real test must prove the used transport works with embedded Tomcat/Servlet from the current BOM. This does not claim every unused SDK transport is compatible.

Deployment choices: keep existing MySQL storage and local uploads for trusted development; persistent database job queues and SQL keyword retrieval first. No external vector database, embedding bill or model account is selected without user configuration. Parser failures must remain observable and must not block upload success.

Required external inputs for real model acceptance: server-side API base URL, provider/model name, API key environment variable, and enabled flag. Required runtime verification for M1+: isolated MySQL test database and migrations. Existing OjApplicationTests only starts the context and does not query MySQL; it cannot establish database availability or migrations.
