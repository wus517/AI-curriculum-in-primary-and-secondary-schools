# Java service contract 0.1.1

Source of truth: `src/main/java/com/example/oj/platform/`. All eight service interfaces compile with Java 17. These are public contracts, not implementations of all business modules.

| Interface | Owner | Methods and responsibility |
|---|---|---|
| CourseAccessService | T00 | requireDiscover, requireStudy, requireManage, requireEnroll; current course validity and role/enrollment policy |
| CurriculumQueryService | T01 | getOutline, listLessons, getLesson, listPracticeCandidates, requireSubmissionSource |
| LearningQueryService | T02 | getSummary; current student's facts in a course and optional time range |
| SubmissionQueryService | T02 | listMine, getMine; never grants admin access to another student's source |
| KnowledgeSearchService | T03 | search; authorized candidates first, effective versions only, topK 1..5 |
| CitationValidationService | T03 | validate; reconstruct citations from actual active chunks; reject missing/private/stale IDs |
| TutorToolGateway | T04 | call; production must use real MCP Client, explicit per-call context and timeout |
| TutorModelClient | T05 | generate, isAvailable; provider-independent streamed text/tool turn, cancellation and timeout |

StudentContext is an immutable record of userId, role and server-generated requestId. Roles are the existing `user` and `admin`; no invented `teacher` role. The course creator is identified by existing `Course.teacherId`. StudentContextFactory reloads the Session account from UserService and rejects deleted accounts; parameters and headers cannot select an identity. DefaultCourseAccessService reads CourseCatalog and obtains EnrollmentLookup lazily, avoiding a dependency cycle with T01. A missing adapter denies ordinary study access.

All new wire IDs are canonical positive decimal strings bounded by signed BIGINT. Service arguments use long/Long; ContractIds is the only wire conversion helper. Problem difficulty is integer 1..3 as defined in existing Problem and initialization SQL; course difficulty remains the legacy string. There is no Java enum migration of existing course status values.

PlatformDtos defines record fields used in OpenAPI, MCP and types.ts. PlatformRequests defines validated REST inputs. StreamEvents defines each SSE payload. PlatformResponse preserves BaseResponse's code/data/message shape but includes required null data. New controllers use PlatformExceptionHandler for actual HTTP statuses; legacy GlobalExceptionHandler behavior is unchanged. Foreign private IDs return 40400, permissions 40300, validation 40000, absent Session 40100; concurrent generation or changed idempotency payload 40900, unavailable model/dependency 50300. Internal exception details never reach callers.

PageQuery uses pageNum >= 1 and pageSize 1..50. PageResult uses records/current/size/total, matching the existing IPage style; current is 1-based. Submission cursors are exclusive canonical submission IDs with descending ID order, pageSize 1..20; list responses never include source code. Empty lists are [] and terminal nextCursor is null. Missing detail is an error, never an empty invented record.

SubmissionDetail includes only the public statement, current student's code and sanitized errors, never hidden tests. Code is bounded to 100000 characters with required codeTruncated indicating any truncation; errorSummary is nullable and bounded to 2000 characters. A course-sourced detail requires current STUDY access even though the submission is owned; an independent submission does not require course enrollment.

TimeRange is UTC [from, to), with null bounds unbounded. from must precede to. Summary.asOf is the query snapshot time. accepted and judged are counts, passRate is accepted/judged (0..1), null when judged=0. PENDING, JUDGING and SYSTEM_ERROR are excluded from judged; statusCounts still reports them. evidence IDs identify actual events/submissions; evidence is bounded to 100 most recent items, not the full ledger. Progress is current cumulative resource-version progress, separate from time-scoped submission counts. Completion is self-reported, not mastery.

ResourceProgress seconds are observed, merged playback intervals; byte sizes are bytes, timeUsed milliseconds, memoryUsed KB (legacy placeholder), page is a 1-based PDF page/PPTX slide. Citation.documentVersion is a positive decimal string. resourceId is null for teacher notes; documentId always identifies the saved source. Citation IDs use chunk:{id}:v{version}. Notes and resource documents have controlled content URLs. At least one actual source is required and all quotes originate from saved text.

Practice candidates use same-course active knowledge point IDs, up to 20 filters, limit 1..10 and integer difficulty 1..3. The accepted flag uses the current student's ACCEPTED history across all courses and independent submissions, as confirmed by the user; it is not the course-scoped summary count. Ordinary students receive published active lessons only. getOutline/listLessons provide structure without progress; getLesson and all resource URLs require STUDY. Management views may include disabled/draft lessons. Retrieval is explicitly KEYWORD; no embeddings or mastery score are implied.

Business implementations recheck current permissions on each call. An empty search is SearchResult(KEYWORD, []). ToolResult is exactly one of {ok:true,data:result,error:null} or {ok:false,data:null,error:ToolError}. Gateway errors do not contain paths, credentials, hidden tests or other users' data. A model callback does not authorize a tool argument or citation; Host must validate and bound them independently.

T00 compatibility tests use a real SDK transport and a synthetic single tool. They do not prove six business tools, authentication, database persistence, model integration or M1/M2/M3.
