# Contract changes

## 0.1.1 - 2026-10-07

- SubmissionDetail now requires codeTruncated, distinguishing bounded legacy code from complete code; Java, OpenAPI, MCP schemas, examples and generated TypeScript updated together before downstream implementation.
- User confirmed practice accepted checks all of the current student's course and independent history; course summary remains scoped to saved submission source.
- T02 stores resource intervals and computes recordedSeconds on read; server event hashes exclude generated timestamps for stable backend retries. First terminal save uses revision 1; rejudging is not implemented.
- Instant persistence uses an explicit UTC handler; legacy Date behavior is unchanged. Real MySQL timezone/JSON/migration acceptance remains T07 work.
- Full suite passes 45 tests; H2 JSON storage adaptation is explicitly test-only. Work paused after the T02 basic-service subsection at the user's request.

## 0.1.0 - 2026-10-07

- Added the eight Java service interfaces, explicit immutable identity and course-access foundation.
- Added complete target REST management/read interfaces, required null handling, error and paging rules.
- Added six read-only MCP schemas, validated result envelopes, examples and TypeScript types.
- Added POST SSE event/state/idempotency contract and database field/constraint blueprint.
- Kept existing string course states and integer problem difficulty based on code.
- SDK selected from actual dependency code; compatibility requires successful real Client/Server verification.
- Real model remains disabled pending user selection; no external model acceptance claimed.

Core contracts were frozen after schema, permission, SDK and parser compatibility checks passed. Current consumers use 0.1.1; T07 integrates existing entry points and migrations separately. Real model provider is an explicit pending configuration decision and blocks real M2 acceptance, not curriculum/tracking implementation. No existing REST numeric-ID API changes.
