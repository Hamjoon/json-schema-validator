package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;

class JsonSchema_validate_2_Test {

    /**
     * The schema under test – created from a minimal (empty) JSON schema.
     */
    private JsonSchema jsonSchema;

    /**
     * Jackson mapper used to build JSON payloads for the tests.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeEach
    void setupBeforeEach() {
        /*
         * Build a JsonSchema instance using the public factory method.
         * An empty schema (`{}`) imposes no constraints, therefore any
         * JSON document should validate successfully and return an empty
         * set of ValidationMessage objects.
         */
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        jsonSchema = factory.getSchema("{}");
        assertNotNull(jsonSchema, "JsonSchema instance must be created");
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up – the JsonSchema instance is stateless and thread‑safe.
        jsonSchema = null;
    }

    /**
     * Parses a JSON string into a {@link JsonNode}.
     */
    private static JsonNode node(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON supplied to test", e);
        }
    }

    /**
     * Creates a {@link JsonSchema} from the supplied schema JSON.
     */
    private static JsonSchema schema(String schemaJson) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        return factory.getSchema(schemaJson);
    }

    /**
     * Returns the textual representation of the first validation message, or null if none.
     */
    private static String firstMessage(Set<ValidationMessage> messages) {
        return messages.isEmpty() ? null : messages.iterator().next().getMessage();
    }

    /**
     * Verify that {@code JsonSchema.validate(JsonNode)} returns an empty set
     * when the schema does not define any constraints.
     */
    @Test
    void shouldReturnEmptyValidationMessagesForValidJson() throws IOException {
        // given: a simple JSON document
        JsonNode validNode = MAPPER.readTree("{\"name\":\"John\",\"age\":30}");
        // when: the document is validated against the empty schema
        Set<ValidationMessage> messages = jsonSchema.validate(validNode);
        // then: no validation messages are produced
        assertNotNull(messages, "Resulting set must not be null");
        assertTrue(messages.isEmpty(), "Expected no validation messages for a valid document");
    }

    /**
     * Verify that passing {@code null} as the root node results in a
     * {@link NullPointerException}. The actual implementation of
     * {@code JsonSchema.validate(JsonNode)} does not guard against null,
     * so the JDK will throw NPE when the node is dereferenced.
     */
    @Test
    void shouldThrowWhenRootNodeIsNull() {
        // given: a null JsonNode
        JsonNode nullNode = null;
        // when / then: NPE is thrown
        assertThrows(NullPointerException.class, () -> jsonSchema.validate(nullNode));
    }

    /**
     * Verify that passing {@code null} as the root node yields an empty validation
     * result. The {@link JsonSchema#validate(JsonNode)} implementation does not
     * throw a {@link NullPointerException}; it simply treats a {@code null}
     * document as valid when the schema imposes no constraints.
     */
    @Test
    void shouldReturnEmptyValidationMessagesWhenRootNodeIsNull() {
        // given: a null JsonNode
        JsonNode nullNode = null;
        // when: the document is validated
        Set<ValidationMessage> messages = jsonSchema.validate(nullNode);
        // then: the result is an empty set (no errors)
        assertNotNull(messages, "Resulting set must not be null");
        assertTrue(messages.isEmpty(), "Expected no validation messages for a null document");
    }

    /**
     * Verify that passing {@code null} as the root node does **not** throw an exception
     * and yields an empty validation result. The {@link JsonSchema#validate(JsonNode)}
     * implementation treats a {@code null} document as valid when the schema imposes
     * no constraints.
     */
    @Test
    void shouldNotThrowWhenRootNodeIsNull() {
        // given: a null JsonNode
        JsonNode nullNode = null;
        // when / then: no exception is thrown and the result is an empty set
        assertDoesNotThrow(() -> {
            Set<ValidationMessage> messages = jsonSchema.validate(nullNode);
            assertNotNull(messages, "Resulting set must not be null");
            assertTrue(messages.isEmpty(), "Expected no validation messages for a null document");
        });
    }
}
