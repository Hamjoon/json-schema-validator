```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchema#getTypeValidator()}.
 *
 * The tests verify that the method returns a non‑null {@link TypeValidator}
 * when the schema defines a {@code type} keyword and returns {@code null}
 * when the schema does not contain a {@code type} definition.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_getTypeValidator_Test {

    private static JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the 2020‑12 draft – any draft works for this simple test.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, builder -> {
            // No special configuration required for these tests.
        });
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation needed.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up at the class level.
    }

    @Test
    void getTypeValidator_returnsNonNullWhenTypeKeywordPresent() {
        // a minimal schema that defines a type
        String schemaJson = "{ \"type\": \"object\" }";

        JsonSchema jsonSchema = factory.getSchema(schemaJson);

        // The method under test – it lazily loads validators if needed.
        TypeValidator typeValidator = jsonSchema.getTypeValidator();

        assertNotNull(typeValidator, "TypeValidator should be non‑null when a \"type\" keyword is present");
    }

    @Test
    void getTypeValidator_returnsNullWhenNoTypeKeyword() {
        // schema without a "type" keyword
        String schemaJson = "{}";

        JsonSchema jsonSchema = factory.getSchema(schemaJson);

        TypeValidator typeValidator = jsonSchema.getTypeValidator();

        assertNull(typeValidator, "TypeValidator should be null when no \"type\" keyword is defined");
    }
}
```