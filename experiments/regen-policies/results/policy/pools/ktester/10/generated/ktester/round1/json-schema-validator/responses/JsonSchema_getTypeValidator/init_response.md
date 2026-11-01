```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Tests for {@link JsonSchema#getTypeValidator()}.
 *
 * The tests use real {@link JsonSchemaFactory} instances to create
 * {@link JsonSchema} objects from JSON schema strings. This avoids the need
 * for heavy mocking of the internal {@link ValidationContext} hierarchy while
 * still exercising the lazy‑loading logic inside {@code getTypeValidator()}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_getTypeValidator_Test {

    /** Shared factory used to build {@link JsonSchema} instances. */
    private static JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Use a concrete Draft‑07 factory – any supported version works for the
        // purpose of these tests.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test state required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up at the class level.
    }

    /**
     * When the schema contains a {@code "type"} keyword the {@link JsonSchema}
     * should create a {@link TypeValidator} and {@code getTypeValidator()}
     * must return a non‑null instance.
     */
    @Test
    void getTypeValidatorReturnsNonNullWhenTypeKeywordPresent() {
        // a simple schema that defines a type
        String schemaJson = "{\"type\":\"string\"}";
        JsonSchema schema = factory.getSchema(schemaJson);

        // invoke the method under test
        TypeValidator typeValidator = schema.getTypeValidator();

        // verify that a TypeValidator was created
        assertNotNull(typeValidator, "TypeValidator should be non‑null for schema with a 'type' keyword");
    }

    /**
     * When the schema does **not** contain a {@code "type"} keyword the
     * {@link JsonSchema} must not create a {@link TypeValidator} and
     * {@code getTypeValidator()} should return {@code null}.
     */
    @Test
    void getTypeValidatorReturnsNullWhenNoTypeKeyword() {
        // a schema without a "type" keyword (only a "minimum" constraint)
        String schemaJson = "{\"minimum\":5}";
        JsonSchema schema = factory.getSchema(schemaJson);

        // invoke the method under test
        TypeValidator typeValidator = schema.getTypeValidator();

        // verify that no TypeValidator exists
        assertNull(typeValidator, "TypeValidator should be null for schema without a 'type' keyword");
    }
}
```