```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonSchema#walk(ExecutionContext, JsonNode, boolean, Consumer)}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>validation with correct and incorrect instances</li>
 *   <li>skipping validation via the {@code validate} flag</li>
 *   <li>execution customizer behaviour (attribute injection, exception propagation)</li>
 *   <li>handling of {@code null} arguments (node and execution context)</li>
 *   <li>advanced schema features such as $ref and $recursiveAnchor</li>
 * </ul>
 *
 * <p>All tests use the {@link JsonSchemaFactory} to create {@link JsonSchema}
 * instances – the public way of obtaining a schema. No private members are accessed
 * directly; the only reflection used is to read an attribute from {@link ExecutionContext}
 * when the API does not expose a getter (the library provides {@code getAttribute}
 * but we keep the reflection as a safeguard).
 */
@ExtendWith(MockitoExtension.class)
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

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a JsonSchema instance using the factory – this is the only public way
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        jsonSchema = factory.getSchema(SIMPLE_SCHEMA);
        mapper = new ObjectMapper();
    }

    @AfterEach
    void teardownAfterEach() {
        jsonSchema = null;
        mapper = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /* --------------------------------------------------------------
     *  Basic validation scenarios (already present in the starter)
     * -------------------------------------------------------------- */

    @Test
    void walk_WithValidNodeAndValidateTrue_ShouldReturnEmptyValidationResult() throws Exception {
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        JsonNode validNode = mapper.readTree("{\"name\":\"John Doe\"}");
        Consumer<ExecutionContext> noOpCustomizer = ctx -> { /* no‑op */ };

        ValidationResult result = jsonSchema.walk(executionContext, validNode, true, noOpCustomizer);

        assertNotNull(result, "Result must not be null");
        assertTrue(result.getValidationMessages().isEmpty(),
                "Expected no validation messages for a valid instance");
    }

    @Test
    void walk_WithInvalidNodeAndValidateTrue_ShouldReturnValidationMessages() throws Exception {
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        JsonNode invalidNode = mapper.readTree("{\"name\":123}");
        Consumer<ExecutionContext> noOpCustomizer = ctx -> { /* no‑op */ };

        ValidationResult result = jsonSchema.walk(executionContext, invalidNode, true, noOpCustomizer);

        assertNotNull(result, "Result must not be null");
        assertFalse(result.getValidationMessages().isEmpty(),
                "Expected validation messages for an invalid instance");
    }

    @Test
    void walk_WithValidateFalse_ShouldSkipValidation() throws Exception {
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        JsonNode anyNode = mapper.readTree("{\"name\":123}");
        Consumer<ExecutionContext> noOpCustomizer = ctx -> { /* no‑op */ };

        ValidationResult result = jsonSchema.walk(executionContext, anyNode, false, noOpCustomizer);

        assertNotNull(result, "Result must not be null");
        assertTrue(result.getValidationMessages().isEmpty(),
                "Validation messages should be empty when validation is disabled");
    }

    /* --------------------------------------------------------------
     *  Execution customizer – attribute injection
     * -------------------------------------------------------------- */

    @Test
    void walk_WithExecutionCustomizer_ShouldInjectAttributeBeforeValidation() throws Exception {
        // given
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        JsonNode validNode = mapper.readTree("{\"name\":\"Alice\"}");
        Consumer<ExecutionContext> customizer = ctx -> ctx.setAttribute("custom", "value");

        // when
        ValidationResult result = jsonSchema.walk(executionContext, validNode, true, customizer);

        // then
        assertNotNull(result);
        assertTrue(result.getValidationMessages().isEmpty(), "Expected no validation errors");

        // Verify that the attribute set by the customizer is present.
        // ExecutionContext provides getAttribute; if not, fallback to reflection.
        Object customValue = null;
        try {
            customValue = executionContext.getAttribute("custom");
        } catch (Exception ignored) {
            // fallback via reflection
            var field = executionContext.getClass().getDeclaredField("attributes");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            var map = (java.util.Map<String, Object>) field.get(executionContext);
            customValue = map.get("custom");
        }
        assertEquals("value", customValue,
                "The execution customizer should have stored the attribute \"custom\"=\"value\"");
    }

    /* --------------------------------------------------------------
     *  Null arguments handling
     * -------------------------------------------------------------- */

    @Test
    void walk_NullNode_ShouldReturnValidationMessageIndicatingMissingInstance() {
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        Consumer<ExecutionContext> noOpCustomizer = ctx -> { /* no‑op */ };

        ValidationResult result = jsonSchema.walk(executionContext, null, true, noOpCustomizer);

        assertNotNull(result);
        Set<ValidationMessage> messages = result.getValidationMessages();
        assertFalse(messages.isEmpty(), "Expected at least one validation message for null node");
        // The exact message text may vary; we only assert that a message exists.
    }

    @Test
    void walk_NullExecutionContext_ShouldThrowNullPointerException() throws Exception {
        JsonNode node = mapper.readTree("{\"name\":\"John\"}");
        Consumer<ExecutionContext> noOpCustomizer = ctx -> { /* no‑op */ };

        assertThrows(NullPointerException.class,
                () -> jsonSchema.walk(null, node, true, noOpCustomizer),
                "Passing a null ExecutionContext must result in NPE");
    }

    @Test
    void walk_NullExecutionContextWithCustomizer_ShouldThrowNullPointerException() throws Exception {
        JsonNode node = mapper.readTree("{\"name\":\"Alice\"}");
        Consumer<ExecutionContext> customizer = ctx -> ctx.setAttribute("custom", "value");

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

    /* --------------------------------------------------------------
     *  Advanced schema features
     * -------------------------------------------------------------- */

    @Test
    void walk_WithSchemaReferenceResolution_ShouldValidateSuccessfully() throws Exception {
        // Schema that references a subschema requiring property "refProp" to be a string
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

        ValidationResult result = refSchema.walk(ctx, instance, true, ec -> { /* no‑op */ });

        assertNotNull(result);
        assertTrue(result.getValidationMessages().isEmpty(),
                "Instance satisfies the referenced subschema, so no errors are expected");
    }

    @Test
    void walk_WithRecursiveAnchor_ShouldValidateWithoutInfiniteLoop() throws Exception {
        // Schema using $recursiveAnchor and $recursiveRef to allow self‑reference
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

        // Instance that contains an empty object for "self" – the recursion should stop
        JsonNode instance = mapper.readTree("{\"self\":{}}");
        ExecutionContext ctx = schema.createExecutionContext();

        ValidationResult result = schema.walk(ctx, instance, true, ec -> { /* no‑op */ });

        assertNotNull(result);
        assertTrue(result.getValidationMessages().isEmpty(),
                "Recursive anchor should be handled gracefully without errors");
    }

    /* --------------------------------------------------------------
     *  Additional validation: missing required property
     * -------------------------------------------------------------- */

    @Test
    void walk_MissingRequiredProperty_ShouldReturnValidationMessage() throws Exception {
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        // Node missing the required "name" property
        JsonNode node = mapper.readTree("{\"age\":30}");
        Consumer<ExecutionContext> noOpCustomizer = ctx -> { /* no‑op */ };

        ValidationResult result = jsonSchema.walk(executionContext, node, true, noOpCustomizer);

        assertNotNull(result);
        Set<ValidationMessage> messages = result.getValidationMessages();
        assertFalse(messages.isEmpty(), "Expected a validation message for missing required property");
        // Verify that at least one message mentions the missing property name
        boolean mentionsName = messages.stream()
                .anyMatch(m -> m.getMessage().contains("name"));
        assertTrue(mentionsName, "Validation message should mention the missing 'name' property");
    }
}
```