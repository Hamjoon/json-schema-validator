```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Tests for {@link JsonSchema#walk(ExecutionContext, JsonNode, boolean, Consumer)}.
 */
class JsonSchema_walk_6_Test {

    private static final String SIMPLE_SCHEMA = """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "type": "object",
          "properties": {
            "name": { "type": "string" }
          },
          "required": ["name"]
        }
        """;

    private JsonSchema jsonSchema;
    private ObjectMapper mapper;

    @BeforeEach
    void setupBeforeEach() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        jsonSchema = factory.getSchema(SIMPLE_SCHEMA);
        mapper = new ObjectMapper();
    }

    @AfterEach
    void teardownAfterEach() {
        jsonSchema = null;
        mapper = null;
    }

    @Test
    void walk_WithValidNodeAndValidateTrue_ShouldReturnEmptyValidationResult() throws Exception {
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        JsonNode validNode = mapper.readTree("{\"name\":\"John Doe\"}");
        Consumer<ExecutionContext> noOpCustomizer = ctx -> {
            /* no‑op */
        };
        ValidationResult result = jsonSchema.walk(executionContext, validNode, true, noOpCustomizer);
        assertNotNull(result, "Result must not be null");
        assertTrue(result.getValidationMessages().isEmpty(),
                "Expected no validation messages for a valid instance");
    }

    @Test
    void walk_WithInvalidNodeAndValidateTrue_ShouldReturnValidationMessages() throws Exception {
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        JsonNode invalidNode = mapper.readTree("{\"name\":123}");
        Consumer<ExecutionContext> noOpCustomizer = ctx -> {
            /* no‑op */
        };
        ValidationResult result = jsonSchema.walk(executionContext, invalidNode, true, noOpCustomizer);
        assertNotNull(result, "Result must not be null");
        assertFalse(result.getValidationMessages().isEmpty(),
                "Expected validation messages for an invalid instance");
    }

    @Test
    void walk_WithValidateFalse_ShouldSkipValidation() throws Exception {
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        JsonNode anyNode = mapper.readTree("{\"name\":123}");
        Consumer<ExecutionContext> noOpCustomizer = ctx -> {
            /* no‑op */
        };
        ValidationResult result = jsonSchema.walk(executionContext, anyNode, false, noOpCustomizer);
        assertNotNull(result, "Result must not be null");
        assertTrue(result.getValidationMessages().isEmpty(),
                "Validation messages should be empty when validation is disabled");
    }

    @Test
    void walk_WithExecutionCustomizer_ShouldBeInvoked() throws Exception {
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        JsonNode validNode = mapper.readTree("{\"name\":\"Alice\"}");
        AtomicBoolean invoked = new AtomicBoolean(false);
        Consumer<ExecutionContext> customizer = ctx -> invoked.set(true);
        ValidationResult result = jsonSchema.walk(executionContext, validNode, true, customizer);
        assertNotNull(result);
        assertTrue(result.getValidationMessages().isEmpty(), "Expected no validation errors");
        assertTrue(invoked.get(), "Customizer should have been invoked");
    }

    @Test
    void walk_NullNode_ShouldReturnValidationMessageIndicatingMissingInstance() {
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        Consumer<ExecutionContext> noOpCustomizer = ctx -> {
            /* no‑op */
        };
        ValidationResult result = jsonSchema.walk(executionContext, null, true, noOpCustomizer);
        assertNotNull(result);
        Set<ValidationMessage> messages = result.getValidationMessages();
        assertFalse(messages.isEmpty(), "Expected at least one validation message for null node");
    }

    @Test
    void walk_NullExecutionContext_ShouldThrowNullPointerException() throws Exception {
        JsonNode node = mapper.readTree("{\"name\":\"John\"}");
        Consumer<ExecutionContext> noOpCustomizer = ctx -> {
            /* no‑op */
        };
        assertThrows(NullPointerException.class,
                () -> jsonSchema.walk(null, node, true, noOpCustomizer),
                "Passing a null ExecutionContext must result in NPE");
    }

    @Test
    void walk_NullExecutionContextWithCustomizer_ShouldThrowNullPointerException() throws Exception {
        JsonNode node = mapper.readTree("{\"name\":\"Alice\"}");
        Consumer<ExecutionContext> customizer = ctx -> {
            /* no‑op */
        };
        assertThrows(NullPointerException.class,
                () -> jsonSchema.walk(null, node, true, customizer),
                "Null ExecutionContext with a customizer must result in NPE");
    }

    @Test
    void walk_ExecutionCustomizerThrowsException_ShouldPropagateRuntimeException() throws Exception {
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        JsonNode node = mapper.readTree("{\"name\":\"Bob\"}");
        Consumer<ExecutionContext> explodingCustomizer = ctx -> {
            throw new RuntimeException("customizer failure");
        };
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> jsonSchema.walk(executionContext, node, true, explodingCustomizer),
                "Exception from customizer must be propagated");
        assertEquals("customizer failure", ex.getMessage());
    }

    @Test
    void walk_WithSchemaReferenceResolution_ShouldValidateSuccessfully() throws Exception {
        String schemaWithRef = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "http://example.com/root.json",
              "$ref": "#/definitions/refSchema",
              "definitions": {
                "refSchema": {
                  "type": "object",
                  "properties": {
                    "refProp": { "type": "string" }
                  },
                  "required": ["refProp"]
                }
              }
            }
            """;
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema refSchema = factory.getSchema(schemaWithRef);
        JsonNode instance = mapper.readTree("{\"refProp\":\"value\"}");
        ExecutionContext ctx = refSchema.createExecutionContext();
        ValidationResult result = refSchema.walk(ctx, instance, true, ec -> {
            /* no‑op */
        });
        assertNotNull(result);
        assertTrue(result.getValidationMessages().isEmpty(),
                "Instance satisfies the referenced subschema, so no errors are expected");
    }

    @Test
    void walk_WithRecursiveAnchor_ShouldValidateWithoutInfiniteLoop() throws Exception {
        String recursiveSchema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "http://example.com/recursive.json",
              "$recursiveAnchor": true,
              "type": "object",
              "properties": {
                "self": { "$recursiveRef": "#" }
              }
            }
            """;
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema schema = factory.getSchema(recursiveSchema);
        JsonNode instance = mapper.readTree("{\"self\":{}}");
        ExecutionContext ctx = schema.createExecutionContext();
        ValidationResult result = schema.walk(ctx, instance, true, ec -> {
            /* no‑op */
        });
        assertNotNull(result);
        assertTrue(result.getValidationMessages().isEmpty(),
                "Recursive anchor should be handled gracefully without errors");
    }

    @Test
    void walk_MissingRequiredProperty_ShouldReturnValidationMessage() throws Exception {
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        JsonNode node = mapper.readTree("{\"age\":30}");
        Consumer<ExecutionContext> noOpCustomizer = ctx -> {
            /* no‑op */
        };
        ValidationResult result = jsonSchema.walk(executionContext, node, true, noOpCustomizer);
        assertNotNull(result);
        Set<ValidationMessage> messages = result.getValidationMessages();
        assertFalse(messages.isEmpty(),
                "Expected a validation message for missing required property");
        boolean mentionsName = messages.stream()
                .anyMatch(m -> m.getMessage().contains("name"));
        assertTrue(mentionsName,
                "Validation message should mention the missing 'name' property");
    }
}
```