```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link OneOfValidator#validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)}.
 * The protected method is accessed via reflection.
 */
class OneOfValidator_validate_2_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ExecutionContext executionContext;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise.
    }

    @BeforeEach
    void setupBeforeEach() {
        // A fresh execution context for each test.
        this.executionContext = new ExecutionContext();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /**
     * Helper to locate the {@code validate} method via reflection.
     */
    private Method getValidateMethod() throws Exception {
        Method m = OneOfValidator.class.getDeclaredMethod(
                "validate",
                ExecutionContext.class,
                JsonNode.class,
                JsonNode.class,
                JsonNodePath.class,
                boolean.class);
        m.setAccessible(true);
        return m;
    }

    /**
     * Returns the {@link OneOfValidator} instance from a compiled {@link JsonSchema}.
     */
    private OneOfValidator getOneOfValidator(JsonSchema schema) {
        List<JsonValidator> validators = schema.getValidators();
        List<OneOfValidator> oneOfValidators = validators.stream()
                .filter(v -> v instanceof OneOfValidator)
                .map(v -> (OneOfValidator) v)
                .collect(Collectors.toList());
        assertEquals(1, oneOfValidators.size(),
                "The test schema should contain exactly one OneOfValidator");
        return oneOfValidators.get(0);
    }

    @Test
    void validateSingleMatchingSchemaReturnsNoErrors() throws Exception {
        // oneOf with two alternatives: first matches, second does not.
        String schemaJson = """
                {
                  "oneOf": [
                    { "type": "string" },
                    { "type": "integer" }
                  ]
                }
                """;

        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
        OneOfValidator validator = getOneOfValidator(schema);

        JsonNode instance = MAPPER.readTree("\"hello\""); // a string

        Method validateMethod = getValidateMethod();
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> messages = (Set<ValidationMessage>) validateMethod.invoke(
                validator,
                executionContext,
                instance,
                instance,
                new JsonNodePath(PathType.JSON_POINTER),
                false);

        assertTrue(messages.isEmpty(), "When exactly one subschema validates, no errors should be reported");
    }

    @Test
    void validateMultipleMatchingSchemasReturnsOneOfError() throws Exception {
        // both alternatives are satisfied (string also matches integer via coercion is not allowed,
        // so we use two schemas that both accept the same instance).
        String schemaJson = """
                {
                  "oneOf": [
                    { "type": "string", "minLength": 0 },
                    { "type": "string", "maxLength": 10 }
                  ]
                }
                """;

        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
        OneOfValidator validator = getOneOfValidator(schema);

        JsonNode instance = MAPPER.readTree("\"abc\""); // a string that satisfies both subschemas

        Method validateMethod = getValidateMethod();
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> messages = (Set<ValidationMessage>) validateMethod.invoke(
                validator,
                executionContext,
                instance,
                instance,
                new JsonNodePath(PathType.JSON_POINTER),
                false);

        // Expect a single OneOf error describing the two matching indexes (0 and 1)
        assertEquals(1, messages.size(), "When more than one subschema validates, a single OneOf error should be returned");
        ValidationMessage vm = messages.iterator().next();
        assertEquals("oneOf", vm.getType());
        assertTrue(vm.getMessage().contains("2 are valid with indexes '0, 1'") ||
                   vm.getMessage().contains("2 are valid with indexes '0,1'"),
                "Message should mention that two schemas are valid and list their indexes");
    }

    @Test
    void validateNoMatchingSchemasReturnsOneOfError() throws Exception {
        // none of the alternatives match.
        String schemaJson = """
                {
                  "oneOf": [
                    { "type": "string", "pattern": "^foo$" },
                    { "type": "integer", "minimum": 10 }
                  ]
                }
                """;

        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
        OneOfValidator validator = getOneOfValidator(schema);

        JsonNode instance = MAPPER.readTree("\"bar\""); // does not match any subschema

        Method validateMethod = getValidateMethod();
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> messages = (Set<ValidationMessage>) validateMethod.invoke(
                validator,
                executionContext,
                instance,
                instance,
                new JsonNodePath(PathType.JSON_POINTER),
                false);

        // Expect a single OneOf error indicating zero valid schemas.
        assertEquals(1, messages.size(), "When no subschema validates, a single OneOf error should be returned");
        ValidationMessage vm = messages.iterator().next();
        assertEquals("oneOf", vm.getType());
        assertTrue(vm.getMessage().contains("0 are valid") || vm.getMessage().contains("0 are valid"),
                "Message should indicate that zero schemas are valid");
    }
}
```