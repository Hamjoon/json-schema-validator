package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.function.Consumer;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_8_Test {

    /**
     * Factory used for creating {@link JsonSchema} instances.
     */
    private static JsonSchemaFactory factory;

    /**
     * A simple schema that expects an object with a required property {@code name}.
     */
    private static JsonSchema schema;

    /**
     * A simple schema that expects an object with a required property {@code name}.
     */
    private static JsonSchema simpleSchema;

    /**
     * Schema that requires both {@code age} and {@code name}.
     */
    private static JsonSchema requiredAgeAndNameSchema;

    /**
     * Schema that disallows additional properties.
     */
    private static JsonSchema noAdditionalPropsSchema;

    /**
     * Schema with an unresolved $ref (used for the last test case).
     */
    private static JsonSchema unresolvedRefSchema;

    /**
     * Collects the validation messages into a {@link List}.
     */
    private static final OutputFormat<List<ValidationMessage>> COLLECTING_FORMAT = messages -> new ArrayList<>(messages);

    /**
     * Returns the number of validation messages.
     */
    private static final OutputFormat<Integer> COUNTING_FORMAT = Set::size;

    /**
     * Throws an unchecked exception when invoked – used to test propagation.
     */
    private static final OutputFormat<Object> EXCEPTION_THROWING_FORMAT = messages -> {
        throw new IllegalArgumentException("Intentional exception from OutputFormat");
    };

    /**
     * Jackson mapper used throughout the tests.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        // 1. simpleSchema – only "name" required
        simpleSchema = factory.getSchema("""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "required": [ "name" ]
            }
            """);
        // 2. schema – same as simpleSchema (used by the first two tests)
        schema = simpleSchema;
        // 3. requiredAgeAndNameSchema – both "age" (integer) and "name" (string) required
        requiredAgeAndNameSchema = factory.getSchema("""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "age": { "type": "integer" },
                "name": { "type": "string" }
              },
              "required": [ "age", "name" ]
            }
            """);
        // 4. noAdditionalPropsSchema – same as requiredAgeAndName but disallow extra fields
        noAdditionalPropsSchema = factory.getSchema("""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "age": { "type": "integer" },
                "name": { "type": "string" }
              },
              "required": [ "age", "name" ],
              "additionalProperties": false
            }
            """);
        // 5. unresolvedRefSchema – $ref points to a non‑existent definition
        unresolvedRefSchema = factory.getSchema("""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$ref": "#/definitions/NonExistent"
            }
            """);
    }

    /**
     * Verify that a valid JSON node results in an empty {@link Set} of
     * {@link ValidationMessage}s and that the supplied {@code Consumer}
     * is invoked exactly once with the {@link ExecutionContext} created by the
     * {@link JsonSchema}.
     */
    @Test
    void validate_validNode_callsCustomizer_andReturnsEmptySet(@Mock Consumer<ExecutionContext> customizer) throws Exception {
        JsonNode validNode = MAPPER.readTree("{\"name\":\"John Doe\"}");
        // a simple consumer that records how many times it is called
        AtomicInteger calls = new AtomicInteger(0);
        Consumer<ExecutionContext> customizer = ctx -> calls.incrementAndGet();
        Set<ValidationMessage> messages = schema.validate(validNode, OutputFormat.DEFAULT, customizer);
        assertTrue(messages.isEmpty(), "Expected no validation messages for a valid node");
        assertEquals(1, calls.get(), "Customizer should be called exactly once");
    }

    /**
     * Verify that an invalid JSON node yields validation messages while the
     * customizer is still invoked. The schema requires the property {@code name},
     * so omitting it should produce exactly one message.
     */
    @Test
    void validate_invalidNode_callsCustomizer_andReturnsMessages(@Mock Consumer<ExecutionContext> customizer) throws Exception {
        JsonNode invalidNode = MAPPER.readTree("{\"age\":30}");
        AtomicInteger calls = new AtomicInteger(0);
        Consumer<ExecutionContext> customizer = ctx -> calls.incrementAndGet();
        Set<ValidationMessage> messages = schema.validate(invalidNode, OutputFormat.DEFAULT, customizer);
        assertEquals(1, messages.size(), "Expected exactly one validation message for missing required property");
        ValidationMessage msg = messages.iterator().next();
        assertTrue(msg.getMessage().toLowerCase().contains("required"), "Validation message should mention the required property");
        assertEquals(1, calls.get(), "Customizer should be called exactly once");
    }

    // ---------- Successful validation – schema matches ----------
    @Test
    void successfulValidation_returnsEmptyList() throws Exception {
        JsonNode node = MAPPER.readTree("{\"age\":30,\"name\":\"Alice\"}");
        List<ValidationMessage> result = simpleSchema.validate(node, COLLECTING_FORMAT, ctx -> {
            /* no‑op */
        });
        assertTrue(result.isEmpty(), "A valid instance should produce an empty list of messages");
    }

    // ---------- Validation failures – schema violations ----------
    @Test
    void validationFailures_collectMultipleMessages() throws Exception {
        JsonNode node = MAPPER.readTree("{\"age\":\"thirty\",\"name\":null}");
        List<ValidationMessage> result = requiredAgeAndNameSchema.validate(node, COLLECTING_FORMAT, ctx -> {
            /* no‑op */
        });
        assertTrue(result.size() >= 2, "Expected at least two validation messages");
        boolean ageType = result.stream().anyMatch(m -> m.getPath().endsWith("age"));
        boolean nameRequired = result.stream().anyMatch(m -> m.getPath().endsWith("name"));
        assertTrue(ageType && nameRequired, "Messages should reference both 'age' and 'name'");
    }

    // ---------- Null rootNode handling ----------
    @Test
    void nullRootNode_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> simpleSchema.validate((JsonNode) null, COLLECTING_FORMAT, ctx -> {
            /* no‑op */
        }));
    }

    // ---------- Null OutputFormat handling ----------
    @Test
    void nullOutputFormat_throwsNullPointerException() throws Exception {
        JsonNode node = MAPPER.readTree("{\"age\":25}");
        assertThrows(NullPointerException.class, () -> simpleSchema.validate(node, null, ctx -> {
            /* no‑op */
        }));
    }

    // ---------- Consumer throws exception ----------
    @Test
    void consumerThrows_exceptionPropagates() throws Exception {
        JsonNode node = MAPPER.readTree("{\"age\":20}");
        Consumer<ExecutionContext> explodingConsumer = ctx -> {
            throw new IllegalStateException("Consumer failure");
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> simpleSchema.validate(node, COLLECTING_FORMAT, explodingConsumer));
        assertEquals("Consumer failure", ex.getMessage());
    }

    // ---------- Consumer is invoked (side‑effect verification) ----------
    @Test
    void consumerSideEffect_isObserved() throws Exception {
        JsonNode node = MAPPER.readTree("{\"age\":40}");
        AtomicBoolean flag = new AtomicBoolean(false);
        Consumer<ExecutionContext> sideEffectConsumer = ctx -> flag.set(true);
        simpleSchema.validate(node, COLLECTING_FORMAT, sideEffectConsumer);
        assertTrue(flag.get(), "Consumer should have set the flag to true");
    }

    // ---------- Non‑object rootNode (array) – type mismatch handling ----------
    @Test
    void arrayRootNode_reportsTypeMismatch() throws Exception {
        JsonNode arrayNode = MAPPER.readTree("[{\"age\":30}]");
        List<ValidationMessage> result = simpleSchema.validate(arrayNode, COLLECTING_FORMAT, ctx -> {
            /* no‑op */
        });
        assertFalse(result.isEmpty(), "Array root should produce at least one validation message");
        boolean typeMismatch = result.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("type"));
        assertTrue(typeMismatch, "Message should indicate a type mismatch (expected object)");
    }

    // ---------- Empty object – required property violations ----------
    @Test
    void emptyObject_reportsAllMissingRequired() throws Exception {
        JsonNode empty = MAPPER.readTree("{}");
        List<ValidationMessage> result = requiredAgeAndNameSchema.validate(empty, COLLECTING_FORMAT, ctx -> {
            /* no‑op */
        });
        // Two required properties -> two messages
        assertEquals(2, result.size(), "Empty object should generate two required‑property messages");
    }

    // ---------- Additional properties not allowed – unexpected field detection ----------
    @Test
    void additionalPropertiesDisallowed_reportsUnexpectedField() throws Exception {
        JsonNode node = MAPPER.readTree("{\"age\":30,\"name\":\"Bob\",\"extra\":\"value\"}");
        List<ValidationMessage> result = noAdditionalPropsSchema.validate(node, COLLECTING_FORMAT, ctx -> {
            /* no‑op */
        });
        boolean extraDetected = result.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("additional"));
        assertTrue(extraDetected, "Expected a message about the disallowed 'extra' property");
    }

    // ---------- Null Consumer handling – fail‑fast behavior ----------
    @Test
    void nullConsumer_throwsNullPointerException() throws Exception {
        JsonNode node = MAPPER.readTree("{\"age\":25,\"name\":\"Eve\"}");
        assertThrows(NullPointerException.class, () -> simpleSchema.validate(node, COLLECTING_FORMAT, (Consumer<ExecutionContext>) null));
    }

    // ---------- Custom OutputFormat – returns transformed result ----------
    @Test
    void countingOutputFormat_returnsZeroForValidInstance() throws Exception {
        JsonNode node = MAPPER.readTree("{\"age\":30,\"name\":\"Alice\"}");
        Integer count = requiredAgeAndNameSchema.validate(node, COUNTING_FORMAT, ctx -> {
            /* no‑op */
        });
        assertEquals(0, count, "Valid instance should result in count 0");
    }

    @Test
    void countingOutputFormat_countsInvalidMessages() throws Exception {
        JsonNode node = MAPPER.readTree("{\"age\":\"thirty\",\"name\":null}");
        Integer count = requiredAgeAndNameSchema.validate(node, COUNTING_FORMAT, ctx -> {
            /* no‑op */
        });
        assertTrue(count >= 2, "Invalid instance should produce count >= 2");
    }

    // ---------- OutputFormat throws unchecked exception – propagation test ----------
    @Test
    void outputFormatThrows_exceptionPropagates() throws Exception {
        JsonNode node = MAPPER.readTree("{\"age\":30,\"name\":\"Alice\"}");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> simpleSchema.validate(node, EXCEPTION_THROWING_FORMAT, ctx -> {
            /* no‑op */
        }));
        assertEquals("Intentional exception from OutputFormat", ex.getMessage());
    }

    // ---------- ExecutionContext customization – fail‑fast mode ----------
    @Test
    void failFastMode_stopsAfterFirstError() throws Exception {
        JsonNode node = MAPPER.readTree("{\"age\":\"thirty\",\"name\":\"Bob\"}");
        List<ValidationMessage> result = requiredAgeAndNameSchema.validate(node, COLLECTING_FORMAT, ctx -> ctx.setFailFast(true));
        // With fail‑fast we expect exactly one message
        assertEquals(1, result.size(), "Fail‑fast should stop after the first validation error");
    }

    // ---------- Unresolved $ref handling – exception propagation ----------
    @Test
    void unresolvedRef_throwsIllegalArgumentException() throws Exception {
        JsonNode node = MAPPER.readTree("{\"valid\":true}");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> unresolvedRefSchema.validate(node, COLLECTING_FORMAT, ctx -> {
            /* no‑op */
        }));
        assertNotNull(ex.getMessage());
    }
}
