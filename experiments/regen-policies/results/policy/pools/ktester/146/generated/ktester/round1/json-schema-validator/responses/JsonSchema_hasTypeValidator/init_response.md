```java
package com.networknt.schema;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchema#hasTypeValidator()}.
 *
 * The tests create {@link JsonSchema} instances via {@link JsonSchemaFactory},
 * which internally builds the required {@link ValidationContext},
 * {@link SchemaLocation}, etc.  This avoids the need to call private
 * constructors directly and respects the public API of the library.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_hasTypeValidator_Test {

    private static JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Use a recent draft version; the concrete version does not affect the
        // behaviour of hasTypeValidator().
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

    @Test
    void hasTypeValidator_whenSchemaDefinesType_returnsTrue() {
        // GIVEN a schema that contains a "type" keyword
        String schemaJson = "{ \"type\": \"string\" }";

        // WHEN the schema is parsed
        JsonSchema schema = factory.getSchema(schemaJson);

        // THEN hasTypeValidator() reports true
        assertTrue(schema.hasTypeValidator(),
                "Expected hasTypeValidator() to be true when a type validator is present");
    }

    @Test
    void hasTypeValidator_whenSchemaLacksType_returnsFalse() {
        // GIVEN a schema without a "type" keyword
        String schemaJson = "{ \"properties\": { \"name\": { \"type\": \"string\" } } }";

        // WHEN the schema is parsed
        JsonSchema schema = factory.getSchema(schemaJson);

        // THEN hasTypeValidator() reports false because the root schema has no type validator
        assertFalse(schema.hasTypeValidator(),
                "Expected hasTypeValidator() to be false when no type validator is present at the root");
    }
}
```