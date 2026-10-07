# Contract fixtures

All IDs and content in this directory are synthetic validation fixtures, not queried learning facts or real model outputs.

- schemas.json: one legal value per wire schema, including empty pages, nullable fields, error responses and SSE payloads.
- tools.json: six inputs, legal/empty outputs, controlled failures and injected-userId invalid inputs.
- stream.json: ordered illustrative stream with Chinese UTF-8 text and a fixture citation.

ContractValidationTest parses OpenAPI as YAML, resolves references and validates examples with networknt JSON Schema 2020-12. It also round-trips corresponding Java records with the existing non_null JSON convention, preserving contract-required nulls. SDK compatibility testing is separate and uses a real synthetic tool, not business data or a model.
