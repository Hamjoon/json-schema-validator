```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.utils.JsonNodes;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#walk(ExecutionContext, JsonNode, boolean, ExecutionContextCustomizer)}.
 *
 * The test suite covers the basic happy‑path, validation disabling, null‑argument checks,
 * customizer behaviour (including mutation, exception propagation and immutable‑context handling),
 * root‑vs‑non‑root instance locations and error scenarios such as unresolved $ref.
 *
 * The {@link JsonSchema} instances are created via {@link JsonSchemaFactory}.  A fresh
 * {@link ExecutionContext} is created for each test to guarantee isolation.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Shared schema used by most tests – a simple object with a required string property {@code name}. */
    private static JsonSchema simpleSchema;

    /** Schema that contains an unresolved $ref – used for the $ref failure test. */
    private static JsonSchema unresolvedRefSchema;

    /** ExecutionContext created for each test method. */
    private ExecutionContext executionContext;

    /** No‑op customizer – used where a customizer is required but no mutation is needed. */
    private static final ExecutionContextCustomizer NO_OP_CUSTOMIZER = ctx -> {
        // intentionally left blank
    };

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // -----------------------------------------------------------------
        // 1️⃣ Simple schema – object with required string property "name"
        // -----------------------------------------------------------------
        String simpleSchemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" }
                  },
                  "required": [ "name" ]
                }
                """;
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        simpleSchema = factory.getSchema(simpleSchemaJson);

        // -----------------------------------------------------------------
        // 2️⃣ Schema with an unresolved $ref – the reference is deliberately wrong
        // -----------------------------------------------------------------
        String unresolvedRefJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "$ref": "#/nonexistent"
                }
                """;
        unresolvedRefSchema = factory.getSchema(unresolvedRefJson);
    }

    @BeforeEach
    void setupBeforeEach() {
        // a brand‑new, mutable ExecutionContext for every test
        executionContext = new ExecutionContext();
    }

    /* --------------------------------------------------------------
     * Helper utilities
     * -------------------------------------------------------------- */

    /**
     * Sets a private field via reflection. Used to manipulate {@link ExecutionContext}
     * internals such as {@code instanceLocation} or the internal attribute map.
     */
    private static void setPrivateField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves a private field via reflection.
     */
    @SuppressWarnings("unchecked")
    private static <T> T getPrivateField(Object target, String fieldName) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return (T) f.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* --------------------------------------------------------------
     * Existing happy‑path tests (kept for completeness)
     * -------------------------------------------------------------- */

    @Test
    void walk_validInstance_returnsSuccess() throws Exception {
        JsonNode instance = MAPPER.readTree("{\"name\":\"John\"}");
        ValidationResult result = simpleSchema.walk(executionContext, instance, true, NO_OP_CUSTOMIZER);
        assertTrue(result.getMessages().isEmpty(),
                "Expected no validation messages for a valid instance");
    }

    @Test
    void walk_invalidInstance_returnsFailure() throws Exception {
        JsonNode instance = MAPPER.readTree("{\"name\":123}");
        ValidationResult result = simpleSchema.walk(executionContext, instance, true, NO_OP_CUSTOMIZER);
        Set<ValidationMessage> messages = result.getMessages();
        assertFalse(messages.isEmpty(), "Expected validation messages for an invalid instance");

        boolean containsName = messages.stream()
                .anyMatch(m -> "name".equals(m.getPath()) && m.getMessage().contains("string"));
        assertTrue(containsName,
                "Expected a type‑mismatch message for property 'name'");
    }

    /* --------------------------------------------------------------
     * 1️⃣ Basic and edge cases
     * -------------------------------------------------------------- */

    @Test
    void walk_withCustomizer_mutatesContextAndSucceeds() throws Exception {
        // customizer that adds an attribute "mode"="test"
        ExecutionContextCustomizer adder = ctx -> ctx.setAttribute("mode", "test");

        JsonNode instance = MAPPER.readTree("{\"name\":\"John\"}");
        ValidationResult result = simpleSchema.walk(executionContext, instance, true, adder);

        assertTrue(result.getMessages().isEmpty(), "Result should be successful");
        assertEquals("test", executionContext.getAttribute("mode"),
                "Customizer must have added the attribute to the ExecutionContext");
    }

    @Test
    void walk_validateFlagFalse_skipsValidation() throws Exception {
        // An array node – would be invalid for the simple object schema,
        // but validation is disabled.
        JsonNode arrayNode = MAPPER.readTree("[1,2,3]");
        ValidationResult result = simpleSchema.walk(executionContext, arrayNode, false, NO_OP_CUSTOMIZER);
        assertTrue(result.getMessages().isEmpty(),
                "When validate=false the method must return a successful result");
    }

    @Test
    void walk_nullCustomizer_isHandledGracefully() throws Exception {
        JsonNode instance = MAPPER.readTree("{\"name\":\"John\"}");
        ValidationResult result = simpleSchema.walk(executionContext, instance, true, null);
        assertTrue(result.getMessages().isEmpty(),
                "Null customizer should be treated as a no‑op and not cause failure");
    }

    @Test
    void walk_nullExecutionContext_throwsIllegalArgumentException() {
        JsonNode node = MAPPER.createObjectNode().put("a", 1);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> simpleSchema.walk(null, node, true, NO_OP_CUSTOMIZER));
        assertTrue(ex.getMessage().toLowerCase().contains("executioncontext"),
                "Exception message must mention executionContext");
    }

    @Test
    void walk_nullNode_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> simpleSchema.walk(executionContext, null, true, NO_OP_CUSTOMIZER));
        assertTrue(ex.getMessage().toLowerCase().contains("node"),
                "Exception message must mention node");
    }

    @Test
    void walk_customizerThrowsRuntimeException_propagates() throws Exception {
        ExecutionContextCustomizer failingCustomizer = ctx -> {
            throw new RuntimeException("customiser failure");
        };
        JsonNode instance = MAPPER.readTree("{\"name\":\"John\"}");
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> simpleSchema.walk(executionContext, instance, true, failingCustomizer));
        assertEquals("customiser failure", ex.getMessage(),
                "The original RuntimeException must be propagated unchanged");
    }

    @Test
    void walk_atRootTrue_returnsSuccess() throws Exception {
        // Set the instanceLocation to the root ("/")
        JsonNodePath rootPath = JsonNodePath.ROOT; // JsonNodePath provides a constant for root
        setPrivateField(executionContext, "instanceLocation", rootPath);

        JsonNode instance = MAPPER.readTree("{\"type\":\"string\"}");
        ValidationResult result = simpleSchema.walk(executionContext, instance, true, NO_OP_CUSTOMIZER);
        assertTrue(result.getMessages().isEmpty(),
                "When atRoot() is true the walk should succeed for a valid node");
    }

    @Test
    void walk_atRootFalse_returnsSuccess() throws Exception {
        // Simulate a non‑root location, e.g. "/properties/name"
        JsonNodePath nonRootPath = JsonNodePath.compile("/properties/name");
        setPrivateField(executionContext, "instanceLocation", nonRootPath);

        JsonNode instance = MAPPER.readTree("{\"type\":\"string\"}");
        ValidationResult result = simpleSchema.walk(executionContext, instance, true, NO_OP_CUSTOMIZER);
        assertTrue(result.getMessages().isEmpty(),
                "When atRoot() is false the walk should still succeed for a valid node");
    }

    /* --------------------------------------------------------------
     * 2️⃣ Validation failure cases
     * -------------------------------------------------------------- */

    @Test
    void walk_typeMismatch_integerExpected_returnsFailure() throws Exception {
        // Schema expecting integer property "age"
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {
                    "age": { "type": "integer" }
                  },
                  "required": [ "age" ]
                }
                """;
        JsonSchema ageSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
                .getSchema(schemaJson);

        JsonNode instance = MAPPER.readTree("{\"age\":\"twenty\"}");
        ValidationResult result = ageSchema.walk(executionContext, instance, true, NO_OP_CUSTOMIZER);
        Set<ValidationMessage> msgs = result.getMessages();
        assertFalse(msgs.isEmpty(), "Expected validation failure for type mismatch");

        boolean containsAge = msgs.stream()
                .anyMatch(m -> "age".equals(m.getPath()) && m.getMessage().toLowerCase().contains("integer"));
        assertTrue(containsAge, "Message should indicate that 'age' must be an integer");
    }

    @Test
    void walk_nullNodeWhenSchemaDisallowsNull_returnsFailure() throws Exception {
        JsonNode nullNode = MAPPER.readTree("null");
        ValidationResult result = simpleSchema.walk(executionContext, nullNode, true, NO_OP_CUSTOMIZER);
        Set<ValidationMessage> msgs = result.getMessages();
        assertFalse(msgs.isEmpty(), "Null root node should cause validation failure");

        boolean typeMsg = msgs.stream()
                .anyMatch(m -> m.getPath().isEmpty() && m.getMessage().toLowerCase().contains("null"));
        assertTrue(typeMsg, "Expected a type‑error message for null root node");
    }

    @Test
    void walk_booleanNodeWhenObjectExpected_returnsFailure() throws Exception {
        JsonNode boolNode = MAPPER.readTree("true");
        ValidationResult result = simpleSchema.walk(executionContext, boolNode, true, NO_OP_CUSTOMIZER);
        Set<ValidationMessage> msgs = result.getMessages();
        assertFalse(msgs.isEmpty(), "Boolean root node should cause validation failure");

        boolean typeMsg = msgs.stream()
                .anyMatch(m -> m.getPath().isEmpty() && m.getMessage().toLowerCase().contains("object"));
        assertTrue(typeMsg, "Expected a type‑error message indicating object was expected");
    }

    /* --------------------------------------------------------------
     * 3️⃣ ExecutionContext immutability exception
     * -------------------------------------------------------------- */

    @Test
    void walk_immutableAttributesMap_customizerThrowsUnsupportedOperationException() throws Exception {
        // Replace the mutable attribute map with an unmodifiable one
        Map<String, Object> immutableMap = Collections.unmodifiableMap(new HashMap<>());
        setPrivateField(executionContext, "attributes", immutableMap);

        ExecutionContextCustomizer mutatingCustomizer = ctx -> ctx.setAttribute("mode", "test");

        JsonNode instance = MAPPER.readTree("{\"name\":\"Alice\"}");
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                () -> simpleSchema.walk(executionContext, instance, true, mutatingCustomizer));
        // The exact message is implementation dependent; we only assert the exception type.
    }

    /* --------------------------------------------------------------
     * 4️⃣ Unresolved $ref exception
     * -------------------------------------------------------------- */

    @Test
    void walk_unresolvedRef_throwsJsonSchemaException() throws Exception {
        JsonNode instance = MAPPER.readTree("{\"any\":\"value\"}");
        // The schema contains a $ref that cannot be resolved; walking should raise an exception.
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> unresolvedRefSchema.walk(executionContext, instance, true, NO_OP_CUSTOMIZER));
        assertTrue(ex.getMessage().toLowerCase().contains("unable to resolve reference"),
                "Exception message must mention inability to resolve $ref");
    }

    /* --------------------------------------------------------------
     * 5️⃣ atRoot() illegal state (instanceLocation == null)
     * -------------------------------------------------------------- */

    @Test
    void walk_instanceLocationNull_atRootThrowsIllegalStateException() throws Exception {
        // Ensure the private field instanceLocation is null
        setPrivateField(executionContext, "instanceLocation", null);

        JsonNode instance = MAPPER.readTree("{\"type\":\"string\"}");
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> simpleSchema.walk(executionContext, instance, true, NO_OP_CUSTOMIZER));
        assertTrue(ex.getMessage().toLowerCase().contains("instanceLocation"),
                "Exception message should mention instanceLocation");
    }
}
```