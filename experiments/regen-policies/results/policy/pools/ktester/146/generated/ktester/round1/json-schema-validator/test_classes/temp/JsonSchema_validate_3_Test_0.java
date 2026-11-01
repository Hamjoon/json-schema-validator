package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_3_Test {

    /**
     * Shared Jackson mapper used to create {@link JsonNode} instances.
     */
    private static ObjectMapper mapper;

    /**
     * The factory used to create {@link JsonSchema} instances.
     */
    private JsonSchemaFactory factory;

    /**
     * The schema under test – a very small schema that requires a {@code name}
     *  property of type {@code string}.
     */
    private JsonSchema schema;

    /**
     * The default schema used by the original tests (requires a string {@code name}).
     */
    private JsonSchema defaultSchema;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise the Jackson ObjectMapper once for the whole test class.
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a factory for the 2020‑12 draft (the most recent stable draft in the
        // library). No special configuration is required for these tests.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        // Simple schema: an object with a required string property "name".
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "required": ["name"],
              "properties": {
                "name": { "type": "string" }
              }
            }
            """;
        // Create the JsonSchema instance that will be exercised.
        schema = factory.getSchema(schemaJson);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the GC – not strictly required but keeps the test tidy.
        schema = null;
        factory = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
        mapper = null;
    }

    /**
     * Verify that the {@code Consumer<ExecutionContext>} passed to {@code validate}
     * is invoked exactly once and that validation works as expected.
     */
    @Test
    void validate_withConsumer_executesConsumerAndDetectsErrors() throws Exception {
        // --------------------------------------------------------------------
        // GIVEN – a JSON instance that violates the schema (name is a number)
        // --------------------------------------------------------------------
        String invalidJson = """
            {
              "name": 123
            }
            """;
        JsonNode invalidNode = mapper.readTree(invalidJson);
        // A flag that the consumer will toggle when it is called.
        AtomicBoolean consumerCalled = new AtomicBoolean(false);
        // Consumer that simply records that it has been executed.
        Consumer<ExecutionContext> executionCustomizer = ctx -> consumerCalled.set(true);
        // --------------------------------------------------------------------
        // WHEN – the validation method is invoked
        // --------------------------------------------------------------------
        Set<ValidationMessage> messages = schema.validate(invalidNode, executionCustomizer);
        // --------------------------------------------------------------------
        // THEN – the consumer must have been called and a validation error must be present
        // --------------------------------------------------------------------
        assertTrue(consumerCalled.get(), "The execution customizer supplied to validate(...) should have been invoked.");
        // The schema requires a string for 'name', so we expect exactly one error.
        assertNotNull(messages, "The returned message set must not be null.");
        assertEquals(1, messages.size(), "For the provided invalid instance a single validation message is expected.");
        // Optional sanity check: the message should contain the property name.
        ValidationMessage msg = messages.iterator().next();
        assertTrue(msg.getMessage().contains("name"), "The validation message should reference the offending property 'name'.");
    }

    /**
     * Verify that a valid JSON instance produces an empty message set while the
     * consumer is still executed.
     */
    @Test
    void validate_withConsumer_andValidInstance_returnsEmptySet() throws Exception {
        // --------------------------------------------------------------------
        // GIVEN – a JSON instance that satisfies the schema
        // --------------------------------------------------------------------
        String validJson = """
            {
              "name": "John Doe"
            }
            """;
        JsonNode validNode = mapper.readTree(validJson);
        AtomicBoolean consumerCalled = new AtomicBoolean(false);
        Consumer<ExecutionContext> executionCustomizer = ctx -> consumerCalled.set(true);
        // --------------------------------------------------------------------
        // WHEN
        // --------------------------------------------------------------------
        Set<ValidationMessage> messages = schema.validate(validNode, executionCustomizer);
        // --------------------------------------------------------------------
        // THEN
        // --------------------------------------------------------------------
        assertTrue(consumerCalled.get(), "The execution customizer must be invoked even when validation succeeds.");
        assertNotNull(messages, "The returned message set must not be null.");
        assertTrue(messages.isEmpty(), "A valid instance should result in an empty set of validation messages.");
    }

    @Test
    void validate_nullRootNode_throwsIllegalArgumentException() {
        Consumer<ExecutionContext> customizer = ctx -> {
        };
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> defaultSchema.validate((JsonNode) null, customizer), "Calling validate with a null rootNode must throw IllegalArgumentException");
        assertTrue(ex.getMessage().contains("rootNode"), "Exception message should mention 'rootNode'");
    }

    @Test
    void validate_nullConsumer_behavesAsNoOpCustomizer() throws Exception {
        // Use the default schema – the instance satisfies it.
        String json = """
            { "name": "John" }""";
        JsonNode node = mapper.readTree(json);
        // Passing null for the consumer should be tolerated.
        Set<ValidationMessage> messages = defaultSchema.validate(node, (Consumer<ExecutionContext>) null);
        assertNotNull(messages, "Result set must not be null");
        assertTrue(messages.isEmpty(), "Valid instance should yield an empty message set");
    }

    @Test
    void validate_consumerDisablesFailFast_validInstance_returnsEmptySet() throws Exception {
        String json = """
            { "name": "John" }""";
        JsonNode node = mapper.readTree(json);
        Consumer<ExecutionContext> customizer = ctx -> ctx.setFailFast(false);
        Set<ValidationMessage> messages = defaultSchema.validate(node, customizer);
        assertNotNull(messages);
        assertTrue(messages.isEmpty(), "Even with fail‑fast disabled, a valid instance must produce no messages");
    }

    @Test
    void validate_consumerEnablesFailFast_missingRequired_returnsSingleMessage() throws Exception {
        // Empty object – violates the required "name" property.
        JsonNode node = mapper.readTree("{}");
        Consumer<ExecutionContext> customizer = ctx -> ctx.setFailFast(true);
        Set<ValidationMessage> messages = defaultSchema.validate(node, customizer);
        assertNotNull(messages);
        // Fail‑fast should stop after the first error → exactly one message.
        assertEquals(1, messages.size(), "With fail‑fast enabled the validator should stop after the first error");
        ValidationMessage msg = messages.iterator().next();
        assertTrue(msg.getMessage().toLowerCase().contains("name"), "The message should refer to the missing required property 'name'");
    }

    @Test
    void validate_consumerThrowsException_propagatesRuntimeException() throws Exception {
        JsonNode node = mapper.readTree("""
            { "name": "John" }""");
        Consumer<ExecutionContext> exploding = ctx -> {
            throw new RuntimeException("customizer failure");
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> defaultSchema.validate(node, exploding), "Exception thrown by the consumer must be propagated");
        assertEquals("customizer failure", ex.getMessage());
    }

    @Test
    void validate_multipleIndependentErrors_returnsAtLeastTwoMessages() throws Exception {
        // Schema that requires "name" (string) and "age" (minimum 0)
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "required": ["name"],
              "properties": {
                "name": { "type": "string" },
                "age": { "type": "integer", "minimum": 0 }
              }
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        // Instance violates both rules: missing name and age = -5
        JsonNode node = mapper.readTree("""
            { "age": -5 }""");
        Set<ValidationMessage> messages = schema.validate(node, ctx -> {
        });
        assertNotNull(messages);
        assertTrue(messages.size() >= 2, "Both the missing required property and the minimum violation should be reported");
        boolean hasName = messages.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("name"));
        boolean hasMinimum = messages.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("minimum"));
        assertTrue(hasName, "Expected a message about the missing required property 'name'");
        assertTrue(hasMinimum, "Expected a message about the 'minimum' constraint on 'age'");
    }

    @Test
    void validate_schemaWithRecursiveAnchor_resolvesWithoutInfiniteLoop() throws Exception {
        // Schema with a recursive anchor and a $ref to itself.
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$recursiveAnchor": true,
              "type": "object",
              "properties": {
                "self": { "$ref": "#" }
              }
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        // Instance that references itself – the validator should terminate safely.
        JsonNode node = mapper.readTree("""
            { "self": { "$ref": "#" } }""");
        Set<ValidationMessage> messages = schema.validate(node, ctx -> {
        });
        assertNotNull(messages);
        assertTrue(messages.isEmpty(), "Recursive anchor should be handled correctly and produce no validation errors");
    }

    @Test
    void validate_failFastTrueWithValidInstance_returnsEmptySet() throws Exception {
        // Same schema as default (requires string name). Add an optional "age".
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "required": ["name"],
              "properties": {
                "name": { "type": "string" },
                "age": { "type": "integer", "minimum": 0 }
              }
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = mapper.readTree("""
            { "name": "Alice", "age": 30 }""");
        Consumer<ExecutionContext> customizer = ctx -> ctx.setFailFast(true);
        Set<ValidationMessage> messages = schema.validate(node, customizer);
        assertNotNull(messages);
        assertTrue(messages.isEmpty(), "When the instance is valid, fail‑fast should not produce any messages");
    }

    @Test
    void validate_customMessageSource_prefixesAllMessages() throws Exception {
        // Schema with required name and minimum on age.
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "required": ["name"],
              "properties": {
                "name": { "type": "string" },
                "age": { "type": "integer", "minimum": 0 }
              }
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = mapper.readTree("""
            { "age": -1 }""");
        MessageSource frenchSource = new MessageSource() {

            @Override
            public String getMessage(String key, Object... args) {
                return "[FR] " + key;
            }
        };
        Consumer<ExecutionContext> customizer = ctx -> ctx.setMessageSource(frenchSource);
        Set<ValidationMessage> messages = schema.validate(node, customizer);
        assertNotNull(messages);
        assertTrue(messages.size() >= 2, "Both required and minimum violations should be present");
        for (ValidationMessage m : messages) {
            assertTrue(m.getMessage().startsWith("[FR] "), "All messages must start with the custom prefix");
        }
    }

    @Test
    void validate_pathTypeJsonPointer_instanceLocationUsesJsonPointerNotation() throws Exception {
        // Schema where "zip" must match a numeric pattern.
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "address": {
                  "type": "object",
                  "properties": {
                    "zip": { "type": "string", "pattern": "^[0-9]+$" }
                  },
                  "required": ["zip"]
                }
              },
              "required": ["address"]
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = mapper.readTree("""
            { "address": { "zip": "123A" } }""");
        Consumer<ExecutionContext> customizer = ctx -> ctx.setPathType(PathType.JSON_POINTER);
        Set<ValidationMessage> messages = schema.validate(node, customizer);
        assertNotNull(messages);
        assertFalse(messages.isEmpty(), "Invalid zip should generate a validation message");
        // At least one message must have an instance location that starts with "/address/zip"
        boolean found = messages.stream().anyMatch(m -> {
            // assumed getter
            String loc = m.getInstanceLocation();
            return loc != null && loc.startsWith("/address/zip");
        });
        assertTrue(found, "Instance location should be expressed in JSON‑Pointer format");
    }
}
