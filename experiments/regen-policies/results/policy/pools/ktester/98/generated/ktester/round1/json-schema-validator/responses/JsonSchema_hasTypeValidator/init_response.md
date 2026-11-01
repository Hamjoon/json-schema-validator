```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchema#hasTypeValidator()}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_hasTypeValidator_Test {

    /** Factory used to create {@link JsonSchema} instances. */
    private static JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the default draft‑2020‑12 meta‑schema; no customisation needed for these tests.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, builder -> {});
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
        // Factory does not hold external resources that need explicit disposal.
    }

    /**
     * A schema that contains a top‑level {@code "type"} keyword should report
     * that it has a {@link TypeValidator}.
     */
    @Test
    void hasTypeValidator_whenTypeIsPresent_returnsTrue() {
        // GIVEN a schema JSON string with a top‑level type definition
        String schemaWithType = "{ \"type\": \"object\" }";

        // WHEN the schema is parsed
        JsonSchema jsonSchema = factory.getSchema(schemaWithType);

        // THEN hasTypeValidator() must return true
        assertTrue(jsonSchema.hasTypeValidator(),
                "hasTypeValidator() should be true when a top‑level \"type\" keyword is present");
    }

    /**
     * A schema that does not contain a top‑level {@code "type"} keyword should
     * report that it does not have a {@link TypeValidator}.
     */
    @Test
    void hasTypeValidator_whenTypeIsAbsent_returnsFalse() {
        // GIVEN a schema JSON string without a top‑level type definition
        String schemaWithoutType = "{ \"properties\": { \"name\": { \"type\": \"string\" } } }";

        // WHEN the schema is parsed
        JsonSchema jsonSchema = factory.getSchema(schemaWithoutType);

        // THEN hasTypeValidator() must return false
        assertFalse(jsonSchema.hasTypeValidator(),
                "hasTypeValidator() should be false when no top‑level \"type\" keyword is present");
    }

    /**
     * A schema whose {@code "type"} keyword is an array (union type) should also
     * report that it has a {@link TypeValidator}.
     */
    @Test
    void hasTypeValidator_whenTypeIsUnion_returnsTrue() {
        // GIVEN a schema with a union type definition
        String schemaWithUnionType = "{ \"type\": [\"string\", \"null\"] }";

        // WHEN the schema is parsed
        JsonSchema jsonSchema = factory.getSchema(schemaWithUnionType);

        // THEN hasTypeValidator() must return true
        assertTrue(jsonSchema.hasTypeValidator(),
                "hasTypeValidator() should be true when \"type\" is a union array");
    }
}
```