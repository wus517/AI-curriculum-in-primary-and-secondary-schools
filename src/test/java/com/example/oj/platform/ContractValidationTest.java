package com.example.oj.platform;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ContractValidationTest {
    private static final Path ROOT = Path.of("docs/ai-learning/contracts");
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    private final JsonSchemaFactory validator = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

    private JsonNode read(String name) throws Exception { return json.readTree(ROOT.resolve(name).toFile()); }

    @Test
    void openApiParsesAndEveryReferenceResolves() throws Exception {
        var spec = json.valueToTree(new Yaml().load(Files.readString(ROOT.resolve("openapi.yaml"))));
        assertEquals("3.1.0", spec.path("openapi").asText());
        assertEquals("0.1.1", spec.path("info").path("version").asText());
        checkReferences(spec, spec);
        Set<String> operations = new HashSet<>();
        spec.path("paths").forEach(path -> path.forEach(operation -> {
            assertTrue(operations.add(operation.path("operationId").asText()));
            assertTrue(operation.path("responses").has("200"));
            assertTrue(operation.path("responses").has("401"));
            assertTrue(operation.path("responses").has("404"));
        }));
        assertTrue(operations.size() >= 35);
    }

    private void checkReferences(JsonNode root, JsonNode node) {
        if (node.has("$ref")) {
            String ref = node.path("$ref").asText();
            assertTrue(ref.startsWith("#/"));
            assertFalse(root.at(ref.substring(1)).isMissingNode(), "Unresolved reference: " + ref);
        }
        node.forEach(child -> checkReferences(root, child));
    }

    @Test
    void allSchemaExamplesValidate() throws Exception {
        JsonNode spec = read("openapi.yaml");
        for (JsonNode example : read("examples/schemas.json")) {
            ObjectNode schema = json.createObjectNode();
            schema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
            schema.set("components", spec.path("components"));
            schema.put("$ref", "#/components/schemas/" + example.path("schema").asText());
            var errors = validator.getSchema(schema).validate(example.path("value"));
            assertTrue(errors.isEmpty(), example.path("schema") + ": " + errors);
        }
    }

    @Test
    void sixToolSchemasAcceptExamplesAndRejectInjectedIdentity() throws Exception {
        JsonNode tools = read("mcp-tools.json").path("tools");
        assertEquals(6, tools.size());
        Set<String> names = new HashSet<>();
        for (JsonNode tool : tools) {
            String name = tool.path("name").asText();
            assertTrue(names.add(name));
            assertFalse(tool.path("inputSchema").path("properties").has("userId"));
            var input = validator.getSchema(tool.path("inputSchema"));
            var output = validator.getSchema(tool.path("outputSchema"));
            for (JsonNode example : read("examples/tools.json")) {
                if (!name.equals(example.path("name").asText())) continue;
                assertTrue(input.validate(example.path("input")).isEmpty(), name);
                assertFalse(input.validate(example.path("invalidInput")).isEmpty(), name);
                for (String type : new String[]{"success", "empty", "failure"}) {
                    if (example.path(type).isNull()) continue;
                    assertTrue(output.validate(example.path(type)).isEmpty(), name + ": " + type);
                }
                ObjectNode invalid = example.path("success").deepCopy();
                invalid.put("ok", false);
                assertFalse(output.validate(invalid).isEmpty(), "Inconsistent result envelope accepted");
            }
        }
    }

    @Test
    void javaRecordsMatchContractAndPreserveRequiredNulls() throws Exception {
        JsonNode schemas = read("openapi.yaml").path("components").path("schemas");
        for (Class<?> enclosing : new Class<?>[]{PlatformDtos.class, PlatformRequests.class, StreamEvents.class}) {
            for (Class<?> type : enclosing.getDeclaredClasses()) {
                if (!type.isRecord() || !schemas.has(type.getSimpleName())) continue;
                JsonNode properties = schemas.path(type.getSimpleName()).path("properties");
                var fields = new HashSet<String>(); properties.fieldNames().forEachRemaining(fields::add);
                var javaFields = new HashSet<String>();
                Arrays.stream(type.getRecordComponents()).forEach(c -> javaFields.add(c.getName()));
                assertEquals(fields, javaFields, type.getSimpleName());
                for (JsonNode example : read("examples/schemas.json")) {
                    if (!type.getSimpleName().equals(example.path("schema").asText())) continue;
                    Object record = json.treeToValue(example.path("value"), type);
                    ObjectNode schema = json.createObjectNode();
                    schema.set("components", read("openapi.yaml").path("components"));
                    schema.put("$ref", "#/components/schemas/" + type.getSimpleName());
                    var errors = validator.getSchema(schema).validate(json.valueToTree(record));
                    assertTrue(errors.isEmpty(), type.getSimpleName() + ": " + errors);
                }
            }
        }
    }

    @Test
    void streamExamplesHaveStrictlyIncreasingSequence() throws Exception {
        JsonNode spec = read("openapi.yaml"); long seq = 0;
        for (JsonNode event : read("examples/stream.json")) {
            String type = switch (event.path("event").asText()) {
                case "accepted" -> "AcceptedEvent";
                case "tool_status" -> "ToolStatusEvent";
                case "delta" -> "DeltaEvent";
                case "citations" -> "CitationsEvent";
                case "completed" -> "CompletedEvent";
                default -> throw new AssertionError("Unknown event");
            };
            ObjectNode schema = json.createObjectNode();
            schema.set("components", spec.path("components"));
            schema.put("$ref", "#/components/schemas/" + type);
            assertTrue(validator.getSchema(schema).validate(event.path("data")).isEmpty());
            long next = event.path("data").path("seq").asLong();
            assertEquals(seq + 1, next); seq = next;
        }
    }
}
