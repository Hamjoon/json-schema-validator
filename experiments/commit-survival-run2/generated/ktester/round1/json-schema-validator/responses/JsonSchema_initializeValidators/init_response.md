```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Tests for {@link JsonSchema#initializeValidators()}.
 *
 * The tests focus on the observable contract of the method:
 * <ul>
 *   <li>It can be invoked without throwing an exception.</li>
 *   <li>It is idempotent – calling it multiple times does not cause failures.</li>
 * </ul>
 *
 * The {@link JsonSchema} instance is created via the package‑private factory method
 * {@code JsonSchema.from(...)} using a minimal, valid schema. All collaborators are
 * supplied as Mockito mocks, keeping the test lightweight and free from external I/O.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_initializeValidators_Test {

    /** Minimal JSON schema used for constructing a {@link JsonSchema} instance. */
    private static final String SIMPLE_SCHEMA = "{ \"type\": \"object\" }";

    /** The {@link JsonSchema} instance under test. */
    private JsonSchema jsonSchema;

    /** Mocked {@link ValidationContext} required by the factory method. */
    @Mock
    private ValidationContext validationContext;

    /** Mocked {@link JsonMetaSchema} returned from the validation context. */
    @Mock
    private JsonMetaSchema metaSchema;

    /** Mocked {@link SchemaValidatorsConfig} – not needed for this test, can be {@code null}. */
    @Mock
    private SchemaValidatorsConfig config;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Configure the ValidationContext mock to satisfy the constructor requirements.
        // Only getMetaSchema() and getConfig() are accessed during construction.
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getConfig()).thenReturn(null); // No keyword‑walk listeners needed.

        // Parse a very small JSON schema.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode schemaNode = mapper.readTree(SIMPLE_SCHEMA);

        // Build the JsonSchema instance using the package‑private factory method.
        jsonSchema = JsonSchema.from(
                validationContext,
                "",                         // schemaPath – empty for the root schema
                new URI("http://example.com/simple-schema.json"),
                schemaNode,
                null,                       // no parent schema
                false                       // do not suppress sub‑schema retrieval
        );
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    @DisplayName("initializeValidators runs without throwing")
    void testInitializeValidatorsRunsWithoutException() {
        assertDoesNotThrow(() -> jsonSchema.initializeValidators(),
                "Calling initializeValidators should not throw any exception");
    }

    @Test
    @DisplayName("initializeValidators is idempotent")
    void testInitializeValidatorsIdempotent() {
        // First invocation – should succeed.
        assertDoesNotThrow(() -> jsonSchema.initializeValidators(),
                "First call to initializeValidators should not throw");

        // Second invocation – the method is designed to be a no‑op after the first call.
        assertDoesNotThrow(() -> jsonSchema.initializeValidators(),
                "Second call to initializeValidators should also not throw");
    }
}
```