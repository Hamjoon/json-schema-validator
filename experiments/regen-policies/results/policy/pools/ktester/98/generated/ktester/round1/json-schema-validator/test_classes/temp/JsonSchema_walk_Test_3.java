package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Shared schema used by all tests.
     */
    private static JsonSchema jsonSchema;

    /**
     * ExecutionContext created for each test method.
     */
    private ExecutionContext executionContext;

    /**
     * No‑op customizer – required by the API but does not modify the context.
     */
    private ExecutionContextCustomizer noOpCustomizer = ctx -> {
        // intentionally left blank
    };

    /**
     * Shared schema used by most tests – a simple object with a required string property {@code name}.
     */
    private static JsonSchema simpleSchema;

    /**
     * Schema that contains an unresolved $ref – used for the $ref failure test.
     */
    private static JsonSchema unresolvedRefSchema;

    /**
     * No‑op customizer – used where a customizer is required but no mutation is needed.
     */
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
        // used by the first two tests
        jsonSchema = simpleSchema;
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
        // A fresh ExecutionContext for every test ensures isolation.
        executionContext = new ExecutionContext();
    }

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
     * Helper that creates a {@code JsonNodePath} instance via reflection.
     */
    private static Object createJsonNodePath(String path) {
        try {
            Class<?> clazz = JsonNodePath.class;
            var ctor = clazz.getDeclaredConstructor(String.class);
            ctor.setAccessible(true);
            return ctor.newInstance(path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Walk the schema with a valid JSON instance.
     * Expect an empty set of validation messages.
     */
    @Test
    void walk_validInstance_returnsSuccess() throws Exception {
        // {"name":"John"}
        JsonNode instance = MAPPER.readTree("{\"name\":\"John\"}");
        ValidationResult result = jsonSchema.walk(executionContext, instance, true, noOpCustomizer);
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> messages = getPrivateField(result, "messages");
        assertTrue(messages.isEmpty(), "Expected no validation messages for a valid instance");
    }

    /**
     * Walk the schema with an invalid JSON instance (wrong type for {@code name}).
     * Expect at least one validation message describing the type mismatch.
     */
    @Test
    void walk_invalidInstance_returnsFailure() throws Exception {
        // {"name":123}
        JsonNode instance = MAPPER.readTree("{\"name\":123}");
        ValidationResult result = jsonSchema.walk(executionContext, instance, true, noOpCustomizer);
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> messages = getPrivateField(result, "messages");
        assertFalse(messages.isEmpty(), "Expected validation messages for an invalid instance");
        // Verify that at least one message mentions the property "name" and expects a string.
        boolean containsName = messages.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("name") && m.getMessage().toLowerCase().contains("string"));
        assertTrue(containsName, "Expected a type‑mismatch message for property 'name'");
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

    @Test
    void walk_withCustomizer_mutatesContextAndSucceeds() throws Exception {
        // customizer that adds an attribute "mode"="test"
        ExecutionContextCustomizer adder = ctx -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> attrs = getPrivateField(ctx, "attributes");
            attrs.put("mode", "test");
            return ctx;
        };
        JsonNode instance = MAPPER.readTree("{\"name\":\"John\"}");
        ValidationResult result = simpleSchema.walk(executionContext, instance, true, adder);
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> msgs = getPrivateField(result, "messages");
        assertTrue(msgs.isEmpty(), "Result should be successful");
        @SuppressWarnings("unchecked")
        Map<String, Object> attrs = getPrivateField(executionContext, "attributes");
        assertEquals("test", attrs.get("mode"), "Customizer must have added the attribute to the ExecutionContext");
    }

    @Test
    void walk_validateFlagFalse_skipsValidation() throws Exception {
        // An array node – would be invalid for the simple object schema,
        // but validation is disabled.
        JsonNode arrayNode = MAPPER.readTree("[1,2,3]");
        ValidationResult result = simpleSchema.walk(executionContext, arrayNode, false, NO_OP_CUSTOMIZER);
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> msgs = getPrivateField(result, "messages");
        assertTrue(msgs.isEmpty(), "When validate=false the method must return a successful result");
    }

    @Test
    void walk_nullCustomizer_isHandledGracefully() throws Exception {
        JsonNode instance = MAPPER.readTree("{\"name\":\"John\"}");
        // cast null to the expected functional type to avoid ambiguity
        ValidationResult result = simpleSchema.walk(executionContext, instance, true, (ExecutionContextCustomizer) null);
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> msgs = getPrivateField(result, "messages");
        assertTrue(msgs.isEmpty(), "Null customizer should be treated as a no‑op and not cause failure");
    }

    @Test
    void walk_nullExecutionContext_throwsIllegalArgumentException() {
        JsonNode node = MAPPER.createObjectNode().put("a", 1);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> simpleSchema.walk(null, node, true, NO_OP_CUSTOMIZER));
        assertTrue(ex.getMessage().toLowerCase().contains("executioncontext"), "Exception message must mention executionContext");
    }

    @Test
    void walk_nullNode_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> simpleSchema.walk(executionContext, null, true, NO_OP_CUSTOMIZER));
        assertTrue(ex.getMessage().toLowerCase().contains("node"), "Exception message must mention node");
    }

    @Test
    void walk_customizerThrowsRuntimeException_propagates() throws Exception {
        ExecutionContextCustomizer failingCustomizer = ctx -> {
            throw new RuntimeException("customiser failure");
        };
        JsonNode instance = MAPPER.readTree("{\"name\":\"John\"}");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> simpleSchema.walk(executionContext, instance, true, failingCustomizer));
        assertEquals("customiser failure", ex.getMessage(), "The original RuntimeException must be propagated unchanged");
    }

    @Test
    void walk_atRootTrue_returnsSuccess() throws Exception {
        // Set the instanceLocation to the root ("/")
        Object rootPath = createJsonNodePath("/");
        setPrivateField(executionContext, "instanceLocation", rootPath);
        JsonNode instance = MAPPER.readTree("{\"type\":\"string\"}");
        ValidationResult result = simpleSchema.walk(executionContext, instance, true, NO_OP_CUSTOMIZER);
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> msgs = getPrivateField(result, "messages");
        assertTrue(msgs.isEmpty(), "When atRoot() is true the walk should succeed for a valid node");
    }

    @Test
    void walk_atRootFalse_returnsSuccess() throws Exception {
        // Simulate a non‑root location, e.g. "/properties/name"
        Object nonRootPath = createJsonNodePath("/properties/name");
        setPrivateField(executionContext, "instanceLocation", nonRootPath);
        JsonNode instance = MAPPER.readTree("{\"type\":\"string\"}");
        ValidationResult result = simpleSchema.walk(executionContext, instance, true, NO_OP_CUSTOMIZER);
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> msgs = getPrivateField(result, "messages");
        assertTrue(msgs.isEmpty(), "When atRoot() is false the walk should still succeed for a valid node");
    }

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
        JsonSchema ageSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(schemaJson);
        JsonNode instance = MAPPER.readTree("{\"age\":\"twenty\"}");
        ValidationResult result = ageSchema.walk(executionContext, instance, true, NO_OP_CUSTOMIZER);
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> msgs = getPrivateField(result, "messages");
        assertFalse(msgs.isEmpty(), "Expected validation failure for type mismatch");
        boolean containsAge = msgs.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("age") && m.getMessage().toLowerCase().contains("integer"));
        assertTrue(containsAge, "Message should indicate that 'age' must be an integer");
    }

    @Test
    void walk_nullNodeWhenSchemaDisallowsNull_returnsFailure() throws Exception {
        JsonNode nullNode = MAPPER.readTree("null");
        ValidationResult result = simpleSchema.walk(executionContext, nullNode, true, NO_OP_CUSTOMIZER);
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> msgs = getPrivateField(result, "messages");
        assertFalse(msgs.isEmpty(), "Null root node should cause validation failure");
        boolean typeMsg = msgs.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("null"));
        assertTrue(typeMsg, "Expected a type‑error message for null root node");
    }

    @Test
    void walk_booleanNodeWhenObjectExpected_returnsFailure() throws Exception {
        JsonNode boolNode = MAPPER.readTree("true");
        ValidationResult result = simpleSchema.walk(executionContext, boolNode, true, NO_OP_CUSTOMIZER);
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> msgs = getPrivateField(result, "messages");
        assertFalse(msgs.isEmpty(), "Boolean root node should cause validation failure");
        boolean typeMsg = msgs.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("object"));
        assertTrue(typeMsg, "Expected a type‑error message indicating object was expected");
    }

    @Test
    void walk_immutableAttributesMap_customizerThrowsUnsupportedOperationException() throws Exception {
        // Replace the mutable attribute map with an unmodifiable one
        Map<String, Object> immutableMap = Collections.unmodifiableMap(new HashMap<>());
        setPrivateField(executionContext, "attributes", immutableMap);
        ExecutionContextCustomizer mutatingCustomizer = ctx -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> attrs = getPrivateField(ctx, "attributes");
            attrs.put("mode", "test");
            return ctx;
        };
        JsonNode instance = MAPPER.readTree("{\"name\":\"Alice\"}");
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> simpleSchema.walk(executionContext, instance, true, mutatingCustomizer));
        // The exact message is implementation dependent; we only assert the exception type.
    }

    @Test
    void walk_unresolvedRef_throwsJsonSchemaException() throws Exception {
        JsonNode instance = MAPPER.readTree("{\"any\":\"value\"}");
        // The schema contains a $ref that cannot be resolved; walking should raise an exception.
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> unresolvedRefSchema.walk(executionContext, instance, true, NO_OP_CUSTOMIZER));
        assertTrue(ex.getMessage().toLowerCase().contains("unable to resolve reference"), "Exception message must mention inability to resolve $ref");
    }

    @Test
    void walk_instanceLocationNull_atRootThrowsIllegalStateException() throws Exception {
        // Ensure the private field instanceLocation is null
        setPrivateField(executionContext, "instanceLocation", null);
        JsonNode instance = MAPPER.readTree("{\"type\":\"string\"}");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> simpleSchema.walk(executionContext, instance, true, NO_OP_CUSTOMIZER));
        assertTrue(ex.getMessage().toLowerCase().contains("instancelocation"), "Exception message should mention instanceLocation");
    }
}
