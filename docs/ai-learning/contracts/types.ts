// Generated from openapi.yaml; new contract IDs are decimal strings.
export type Id = string;

export type Instant = string;

export type LessonStatus = "DRAFT" | "PUBLISHED" | "DISABLED";

export type EventType = "LESSON_OPENED" | "RESOURCE_PROGRESS" | "LESSON_MARKED_COMPLETE" | "SUBMISSION_JUDGED" | "AI_HINT_USED";

export type AssistanceLevel = "NONE" | "HINT" | "SOLUTION" | "UNKNOWN";

export type Mode = "CONCEPT" | "HINT" | "REVIEW";

export type GenerationStatus = "QUEUED" | "RUNNING" | "COMPLETED" | "FAILED" | "CANCELLED" | "INTERRUPTED";

export type IngestionStatus = "QUEUED" | "PROCESSING" | "READY" | "FAILED" | "UNSUPPORTED";

export type TimeRange = {
  from: string | null;
  to: string | null;
};

export type KnowledgePoint = {
  id: string;
  courseId: string;
  name: string;
  description: string | null;
  active: boolean;
};

export type ResourceRef = {
  id: string;
  title: string | null;
  resourceType: "video" | "ppt";
  version: string;
  sortOrder: number;
  contentUrl: string;
};

export type ProblemRef = {
  id: string;
  title: string;
  difficulty: number;
  sortOrder: number;
  knowledgePointIds: Array<string>;
};

export type Lesson = {
  id: string;
  courseId: string;
  title: string;
  objectives: string | null;
  sortOrder: number;
  status: LessonStatus;
  resources: Array<ResourceRef>;
  problems: Array<ProblemRef>;
  knowledgePoints: Array<KnowledgePoint>;
};

export type CourseOutline = {
  courseId: string;
  title: string;
  description: string | null;
  enrolled: boolean;
  lessons: Array<Lesson>;
  knowledgePoints: Array<KnowledgePoint>;
};

export type PracticeCandidate = {
  problemId: string;
  title: string;
  difficulty: number;
  knowledgePointIds: Array<string>;
  accepted: boolean;
};

export type PracticeCandidates = {
  items: Array<PracticeCandidate>;
};

export type Interval = {
  start: number;
  end: number;
};

export type Evidence = {
  id: string;
  type: string;
  occurredAt: string;
};

export type ResourceProgress = {
  resourceId: string;
  resourceVersion: string;
  intervals: Array<Interval>;
  recordedSeconds: number;
  lastPosition: number;
};

export type LessonProgress = {
  lessonId: string;
  selfReportedComplete: boolean;
  updatedAt: string;
  resources: Array<ResourceProgress>;
};

export type LearningSummary = {
  courseId: string;
  range: TimeRange;
  asOf: string;
  accepted: number;
  judged: number;
  passRate: number | null;
  statusCounts: Record<string, number>;
  lessons: Array<LessonProgress>;
  evidence: Array<Evidence>;
};

export type JudgeStatus = "PENDING" | "JUDGING" | "ACCEPTED" | "WRONG_ANSWER" | "COMPILE_ERROR" | "RUNTIME_ERROR" | "TIME_LIMIT_EXCEEDED" | "MEMORY_LIMIT_EXCEEDED" | "SYSTEM_ERROR";

export type SubmissionSummary = {
  id: string;
  problemId: string;
  courseId: string | null;
  lessonId: string | null;
  status: JudgeStatus;
  assistanceLevel: AssistanceLevel;
  createdAt: string;
};

export type SubmissionDetail = {
  submission: SubmissionSummary;
  problemTitle: string;
  publicContent: string;
  language: "java";
  code: string;
  codeTruncated: boolean;
  timeUsed: number | null;
  memoryUsed: number | null;
  errorSummary: string | null;
};

export type SubmissionCursor = {
  items: Array<SubmissionSummary>;
  nextCursor: string | null;
};

export type Citation = {
  citationId: string;
  courseId: string;
  lessonId: string | null;
  resourceId: string | null;
  documentId: string;
  documentVersion: string;
  title: string;
  page: number | null;
  startSeconds: number | null;
  endSeconds: number | null;
  quote: string;
  contentUrl: string;
};

export type SearchHit = {
  text: string;
  score: number;
  citation: Citation;
};

export type SearchResult = {
  retrievalMode: "KEYWORD";
  items: Array<SearchHit>;
};

export type ToolError = {
  code: "INVALID_ARGUMENT" | "NOT_FOUND" | "FORBIDDEN" | "TIMEOUT" | "UNAVAILABLE" | "INTERNAL";
  message: string;
  retryable: boolean;
};

export type Enrollment = {
  id: string;
  userId: string;
  courseId: string;
  status: "ACTIVE" | "INACTIVE";
  joinedAt: string;
};

export type EnrollRequest = {
  courseId: string;
};

export type LessonWrite = {
  title: string;
  objectives: string | null;
  sortOrder: number;
  status: LessonStatus;
};

export type LinkWrite = {
  sortOrder: number;
};

export type KnowledgePointWrite = {
  name: string;
  description: string | null;
};

export type ProgressPayload = {
  resourceId: string;
  resourceVersion: string;
  intervals: Array<Interval>;
  lastPosition: number;
  durationSeconds: number;
};

export type EmptyPayload = {

};

export type ClientEvent = {
  eventId: string;
  courseId: string;
  lessonId: string;
  type: "LESSON_OPENED" | "RESOURCE_PROGRESS" | "LESSON_MARKED_COMPLETE";
  occurredAt: string;
  payload: ProgressPayload | EmptyPayload;
};

export type EventReceipt = {
  eventId: string;
  duplicate: boolean;
  processingStatus: "PENDING" | "PROCESSING" | "PROCESSED" | "FAILED";
};

export type NoteWrite = {
  lessonId: string | null;
  title: string;
  text: string;
};

export type Document = {
  id: string;
  courseId: string;
  lessonId: string | null;
  resourceId: string | null;
  title: string;
  version: string;
  status: IngestionStatus;
  active: boolean;
};

export type JobRequest = {
  resourceId?: string;
  documentId?: string;
};

export type IngestionJob = {
  jobId: string;
  documentId: string;
  version: string;
  status: IngestionStatus;
  attempts: number;
  error: string | null;
};

export type ConversationCreate = {
  courseId: string;
  lessonId?: string | null;
};

export type Conversation = {
  id: string;
  courseId: string;
  lessonId: string | null;
  createdAt: string;
};

export type MessageCreate = {
  clientMessageId: string;
  content: string;
  mode: Mode;
  problemId?: string | null;
  submissionId?: string | null;
};

export type Message = {
  id: string;
  clientMessageId: string;
  role: "user" | "assistant";
  status: GenerationStatus;
  mode: Mode;
  content: string;
  citations: Array<Citation>;
  createdAt: string;
  updatedAt: string;
};

export type Cancellation = {
  messageId: string;
  status: GenerationStatus;
};

export type LessonPage = {
  records: Array<Lesson>;
  current: number;
  size: number;
  total: number;
};

export type KnowledgePointPage = {
  records: Array<KnowledgePoint>;
  current: number;
  size: number;
  total: number;
};

export type DocumentPage = {
  records: Array<Document>;
  current: number;
  size: number;
  total: number;
};

export type ConversationPage = {
  records: Array<Conversation>;
  current: number;
  size: number;
  total: number;
};

export type MessagePage = {
  records: Array<Message>;
  current: number;
  size: number;
  total: number;
};

export type IdResult = {
  id: string;
};

export type BooleanResult = boolean;

export type AcceptedEvent = {
  messageId: string;
  requestId: string;
  seq: number;
};

export type ToolStatusEvent = {
  toolName: "get_course_outline" | "get_my_learning_summary" | "list_my_submissions" | "get_my_submission_detail" | "search_course_knowledge" | "list_practice_candidates";
  status: "RUNNING" | "COMPLETED" | "FAILED";
  seq: number;
};

export type DeltaEvent = {
  messageId: string;
  seq: number;
  text: string;
};

export type CitationsEvent = {
  messageId: string;
  seq: number;
  items: Array<Citation>;
};

export type CompletedEvent = {
  messageId: string;
  seq: number;
  status: "COMPLETED";
  usage?: Record<string, number>;
};

export type FailedEvent = {
  messageId: string;
  seq: number;
  code: string;
  message: string;
  retryable: boolean;
};

export type CancelledEvent = {
  messageId: string;
  seq: number;
};
