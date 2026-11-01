```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchema#validate(JsonNode)}.
 *
 * <p>The tests use {@link JsonSchemaFactory} to obtain a {@link JsonSchema}
 * instance (the constructor of {@link JsonSchema} is private). Two simple
 * JSON documents are prepared – one that complies with the schema and one that
 * violates it – and the {@code validate(JsonNode)} method is exercised for
 * both cases.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_2_Test {

    /** Shared mapper for creating {@link JsonNode}s from JSON text. */
    private static ObjectMapper mapper;

    /** Schema under test. */
    private JsonSchema schema;

    /** Valid JSON document (conforms to the schema). */
    private JsonNode validNode;

    /** Invalid JSON document (violates the schema). */
    private JsonNode invalidNode;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a thread‑safe ObjectMapper once for all tests.
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // --------------------------------------------------------------------
        // 1️⃣ Build a very small schema:
        //    - root type: object
        //    - required property "name" of type string
        // --------------------------------------------------------------------
        String schemaJson = "{\n"
                + "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n"
                + "  \"type\": \"object\",\n"
                + "  \"properties\": {\n"
                + "    \"name\": { \"type\": \"string\" }\n"
                + "  },\n"
                + "  \"required\": [\"name\"]\n"
                + "}";

        // Obtain a JsonSchema instance via the public factory API.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V201909);
        this.schema = factory.getSchema(schemaJson);

        // --------------------------------------------------------------------
        // 2️⃣ Prepare JSON documents.
        // --------------------------------------------------------------------
        // Valid document – "name" is a string.
        this.validNode = mapper.readTree("{\"name\":\"John Doe\"}");

        // Invalid document – "name" is a number (type mismatch).
        this.invalidNode = mapper.readTree("{\"name\":123}");
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and avoid accidental reuse between tests.
        this.schema = null;
        this.validNode = null;
        this.invalidNode = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No external resources to release, but clear the static mapper.
        mapper = null;
    }

    /** {@link JsonSchema#validate(JsonNode)} should return an empty set for valid input. */
    @Test
    void validate_ValidNode_ReturnsEmptySet() {
        Set<ValidationMessage> messages = schema.validate(validNode);
        assertNotNull(messages, "The returned set must never be null");
        assertTrue(messages.isEmpty(), "A valid document should produce no validation messages");
    }

    /** {@link JsonSchema#validate(JsonNode)} should report at least one message for invalid input. */
    @Test
    void validate_InvalidNode_ReturnsMessages() {
        Set<ValidationMessage> messages = schema.validate(invalidNode);
        assertNotNull(messages, "The returned set must never be null");
        assertFalse(messages.isEmpty(), "An invalid document should produce validation messages");
        // The schema only contains a single constraint, so we expect exactly one message.
        assertEquals(1, messages.size(),
                "For the given schema the type mismatch should generate exactly one validation message");
    }
}
```