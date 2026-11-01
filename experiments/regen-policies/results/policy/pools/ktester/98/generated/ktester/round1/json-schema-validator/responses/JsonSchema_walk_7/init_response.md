```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchema#walk(ExecutionContext, String, InputFormat, boolean)}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_7_Test {

    /** Factory used to create {@link JsonSchema} instances for the tests. */
    private static JsonSchemaFactory factory;

    /** Simple schema that requires an object with a string property {@code name}. */
    private static final String SIMPLE_SCHEMA = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "name": { "type": "string" }
              },
              "required": [ "name" ]
            }
            """;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a factory for the 2020‑12 draft – this is sufficient for the
        // walk‑method tests and does not involve any external resources.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally.
    }

    /**
     * Walk a valid JSON document – the result must be valid and contain no messages.
     */
    @Test
    void walkValidDocument_returnsValidResult() {
        // given
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        ExecutionContext ctx = new ExecutionContext();
        String validJson = """{ "name": "John Doe" }""";

        // when
        ValidationResult result = schema.walk(ctx, validJson, InputFormat.JSON, true);

        // then
        assertTrue(result.isValid(), "Result should be valid for a correct document");
        assertTrue(result.getMessages().isEmpty(),
                "There should be no validation messages for a valid document");
    }

    /**
     * Walk an invalid JSON document – the result must be invalid and contain exactly one message.
     */
    @Test
    void walkInvalidDocument_returnsInvalidResult() {
        // given
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        ExecutionContext ctx = new ExecutionContext();
        // {@code name} is a number, which violates the {@code string} type constraint.
        String invalidJson = """{ "name": 123 }""";

        // when
        ValidationResult result = schema.walk(ctx, invalidJson, InputFormat.JSON, true);

        // then
        assertFalse(result.isValid(), "Result should be invalid for an incorrect document");
        assertEquals(1, result.getMessages().size(),
                "Exactly one validation message is expected for the type error");
    }
}
```