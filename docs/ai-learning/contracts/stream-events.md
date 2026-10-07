# POST SSE contract 0.1.0

POST /api/assistant/conversations/{conversationId}/messages with Cookie Session and same-origin validation. The server validates ownership, current course access, request fields, provider availability and conversation mutual exclusion before opening the stream. No credentials or user ID are accepted in the message body.

Content-Type: text/event-stream; charset=UTF-8. Cache-Control: no-cache, no-transform. Events are `event: <name>`, then `data: <JSON>`, then a blank line. JSON never contains raw multiline data; escaped newlines are decoded after frame assembly. UTF-8 decoding is streaming. Every event contains a seq starting at 1 and increasing by one within this response. Heartbeat comments do not consume seq. Unknown events or non-increasing seq are protocol errors; partial text remains visible.

| Event | Required fields besides seq | Behavior |
|---|---|---|
| accepted | messageId, requestId | Persistence succeeded; new generation started |
| tool_status | toolName, status | RUNNING/COMPLETED/FAILED; no arguments or private payload |
| delta | messageId, text | Append incremental text |
| citations | messageId, items | Only server-validated actual citations |
| completed | messageId, status=COMPLETED | Final content persisted; optional usage with nonnegative token counts |
| failed | messageId, code, message, retryable | FAILED persisted; partial text preserved |
| cancelled | messageId | CANCELLED persisted; partial text preserved |

Exactly one terminal event ends a normally delivered stream. EOF before a terminal event is a transport interruption, not success. Refresh uses paginated messages to obtain authoritative saved state; there is no automatic replay/resume in this version. seq is per response, not a durable message offset.

clientMessageId is a UUID unique within the current user's conversation. The server hashes the complete normalized request including mode/context, and compares the hash on retries. A duplicate returns application/json MessageResponse with existing status/content, HTTP 200, and never invokes the model again. Reusing an ID with changed content/context is HTTP 409 code 40900. A different ID while a generation is QUEUED/RUNNING is also 409. Validate idempotency before rejecting a duplicate as concurrent.

Creation returns one assistant message ID linked to the user message; accepted.messageId and every subsequent messageId refer to that assistant message. The assistant message and user message share clientMessageId but are distinguished by role. No additional request may claim another user's conversation, message, submission or course.

Default generation deadline: 60 seconds; maximum tool calls: 6; per-tool deadline: 10 seconds. Limits remain server configuration, never model-selected. Cancellation is an authenticated POST to /api/assistant/messages/{messageId}/cancel. Completed/failed/interrupted/cancelled messages return their state idempotently. A browser abort triggers cancellation when detected, with deadline as fallback.

Persistent statuses: QUEUED -> RUNNING -> COMPLETED/FAILED/CANCELLED/INTERRUPTED. Both QUEUED and RUNNING need leases; expired leases become INTERRUPTED at startup and during recovery. The conversation's generation lock is released in the same transaction as terminal state. No process-local mutex alone can enforce database mutual exclusion. Streaming callbacks check cancellation/lease ownership; late callbacks cannot overwrite a terminal state.

Model availability defaults to disabled. Missing configuration returns HTTP 50300 before accepted; it never silently switches to a Mock. Explicit Mock development reports its source in configuration/UI. A tool failure may be conveyed to the model as a controlled error but does not authorize fabricated facts. Final citations and recommended problem IDs must come from actual validated tool results.

All payload schemas are in openapi.yaml; Java records are in StreamEvents and examples/stream.json exercises UTF-8 and ordering. Tests must additionally cover UTF-8 split bytes, split frames, CRLF, empty delta, cancel, errors after accepted, duplicate requests, aborted connection and restart recovery when those implementations exist.
