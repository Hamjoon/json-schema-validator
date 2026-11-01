```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic smoke‑test for {@link JsonSchema#validateAndCollect(ExecutionContext, JsonNode)}.
 * <p>
 * The test builds a minimal {@link JsonSchema} instance from an empty schema
 * (which imposes no constraints) and validates a simple JSON document.
 * The expectation is that the returned {@link ValidationResult} is not
 * {@code null} and contains no validation messages.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validateAndCollect_Test {

    /** Instance of {@link JsonSchema} used by all test methods. */
    private static JsonSchema schema;

    /** Executed once before all tests – creates a schema from an empty JSON object. */
    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // an empty schema ({}) accepts any JSON document
        JsonNode emptySchema = JsonMapperFactory.getInstance().readTree("{}");
        schema = JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(emptySchema);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required at the moment.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static resources if needed.
        schema = null;
    }

    /** Verifies that {@code validateAndCollect} returns a successful result for a valid document. */
    @Test
    void shouldReturnEmptyValidationResultForValidDocument() throws Exception {
        // given
        ExecutionContext executionContext = new ExecutionContext();
        JsonNode validDocument = JsonMapperFactory.getInstance().readTree("{\"name\":\"John\",\"age\":30}");

        // when
        ValidationResult result = schema.validateAndCollect(executionContext, validDocument);

        // then
        assertNotNull(result, "ValidationResult must not be null");
        // ValidationResult stores messages in a Set; an empty set means the document is valid.
        assertTrue(result.getMessages().isEmpty(),
                () -> "Expected no validation messages, but got: " + result.getMessages());
    }
}
```