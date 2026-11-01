```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OneOfValidator#validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)}.
 *
 * The tests instantiate a real {@link OneOfValidator} via {@link JsonSchemaFactory}
 * (so no reflection or private‑field access is required) and invoke the protected
 * {@code validate} method directly because the test class resides in the same
 * package {@code com.networknt.schema}.
 */
class OneOfValidator_validate_2_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /**
     * Helper that creates a OneOfValidator from a JSON schema string.
     */
    private OneOfValidator getOneOfValidator(String schemaJson) {
        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
        List<JsonValidator> validators = schema.getValidators();
        return validators.stream()
                .filter(v -> v instanceof OneOfValidator)
                .map(v -> (OneOfValidator) v)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("OneOfValidator not found in schema"));
    }

    @Test
    void whenExactlyOneSchemaMatches_thenNoErrorsReturned() throws Exception {
        // oneOf: [ {"type":"string"}, {"type":"integer"} ]
        String schema = "{ \"oneOf\": [ {\"type\":\"string\"}, {\"type\":\"integer\"} ] }";

        OneOfValidator validator = getOneOfValidator(schema);
        JsonNode node = MAPPER.readTree("\"a string\"");
        ExecutionContext ctx = new ExecutionContext();

        Set<ValidationMessage> messages = validator.validate(
                ctx,
                node,
                node,
                new JsonNodePath(JsonNodePath.PathType.JSON),
                false);

        assertTrue(messages.isEmpty(), "Expected no validation messages when exactly one schema matches");
    }

    @Test
    void whenNoSchemaMatches_thenOneOfErrorReturned() throws Exception {
        // oneOf: [ {"type":"string"}, {"type":"integer"} ]
        String schema = "{ \"oneOf\": [ {\"type\":\"string\"}, {\"type\":\"integer\"} ] }";

        OneOfValidator validator = getOneOfValidator(schema);
        // a boolean does not match either string or integer
        JsonNode node = MAPPER.readTree("true");
        ExecutionContext ctx = new ExecutionContext();

        Set<ValidationMessage> messages = validator.validate(
                ctx,
                node,
                node,
                new JsonNodePath(JsonNodePath.PathType.JSON),
                false);

        assertEquals(1, messages.size(), "Exactly one error should be reported when no schema matches");
        ValidationMessage vm = messages.iterator().next();
        assertEquals("oneOf", vm.getType());
        assertTrue(vm.getMessage().contains("must be valid to one and only one schema"),
                "Error message should contain the oneOf description");
    }

    @Test
    void whenMultipleSchemasMatch_thenOneOfErrorWithIndexesReturned() throws Exception {
        // oneOf: [ {}, {"type":"string"} ]
        // The empty schema {} matches any JSON value, so both subschemas will validate for a string.
        String schema = "{ \"oneOf\": [ {}, {\"type\":\"string\"} ] }";

        OneOfValidator validator = getOneOfValidator(schema);
        JsonNode node = MAPPER.readTree("\"test\"");
        ExecutionContext ctx = new ExecutionContext();

        Set<ValidationMessage> messages = validator.validate(
                ctx,
                node,
                node,
                new JsonNodePath(JsonNodePath.PathType.JSON),
                false);

        // Two schemas are valid → an error must be produced.
        assertEquals(1, messages.size(), "One error should be produced when more than one schema matches");
        ValidationMessage vm = messages.iterator().next();
        assertEquals("oneOf", vm.getType());
        // The message should contain the indexes of the matching schemas (0 and 1)
        assertTrue(vm.getMessage().contains("indexes '0, 1'") ||
                   vm.getMessage().contains("indexes '1, 0'"),
                "Message should list the matching schema indexes");
    }

    @Test
    void walkFlagTrueInvokesValidateWithWalkTrue() throws Exception {
        // oneOf: [ {"type":"string"}, {"type":"integer"} ]
        String schema = "{ \"oneOf\": [ {\"type\":\"string\"}, {\"type\":\"integer\"} ] }";

        OneOfValidator validator = getOneOfValidator(schema);
        JsonNode node = MAPPER.readTree("\"123\""); // a string – matches first schema only
        ExecutionContext ctx = new ExecutionContext();

        // Directly call the protected method with walk = true
        Set<ValidationMessage> messages = validator.validate(
                ctx,
                node,
                node,
                new JsonNodePath(JsonNodePath.PathType.JSON),
                true);

        // Walk flag does not change the result for a single matching schema
        assertTrue(messages.isEmpty(), "Walk flag should not affect the outcome when exactly one schema matches");
    }
}
```