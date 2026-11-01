```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(String, InputFormat, SchemaValidatorsConfig)}.
 *
 * The tests cover:
 * <ul>
 *   <li>Successful creation of a {@link JsonSchema} from a valid JSON schema string.</li>
 *   <li>Proper handling of malformed JSON input – the method must wrap the {@link java.io.IOException}
 *       in a {@link JsonSchemaException}.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_15_Test {

    /** Factory instance used by all tests. */
    private static JsonSchemaFactory factory;

    /** A simple, valid JSON schema used for the happy‑path test. */
    private static final String VALID_SCHEMA = "{ \"type\": \"object\" }";

    /** An obviously malformed JSON string used to trigger an exception. */
    private static final String INVALID_SCHEMA = "this is not json";

    @BeforeAll
    static void setupBeforeAll() {
        // Use the public builder to obtain a factory instance.
        // The default meta‑schema is sufficient for these simple tests.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test state needed at the moment.
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
     * Verify that a well‑formed JSON schema string can be turned into a {@link JsonSchema}
     * instance when the appropriate {@link InputFormat} and configuration are supplied.
     */
    @Test
    void shouldCreateJsonSchemaFromValidString() {
        // given
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // when
        JsonSchema schema = factory.getSchema(VALID_SCHEMA, InputFormat.JSON, config);

        // then
        assertNotNull(schema, "The factory should return a non‑null JsonSchema instance");
        // The concrete JsonSchema implementation does not expose the parsed node directly,
        // but we can at least assert that the returned object is of the expected type.
        assertTrue(schema instanceof JsonSchema, "Returned object should be a JsonSchema");
    }

    /**
     * Verify that malformed JSON input results in a {@link JsonSchemaException}.
     * The underlying {@link java.io.IOException} thrown by Jackson must be wrapped.
     */
    @Test
    void shouldThrowJsonSchemaExceptionWhenSchemaIsInvalid() {
        // given
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // when / then
        JsonSchemaException thrown = assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(INVALID_SCHEMA, InputFormat.JSON, config),
                "Expected JsonSchemaException for malformed JSON schema");

        // The cause should be an IOException (Jackson reports parsing errors as JsonProcessingException,
        // which extends IOException).
        assertNotNull(thrown.getCause(), "Exception cause should not be null");
        assertTrue(thrown.getCause() instanceof java.io.IOException,
                "Cause of JsonSchemaException should be an IOException");
    }
}
```