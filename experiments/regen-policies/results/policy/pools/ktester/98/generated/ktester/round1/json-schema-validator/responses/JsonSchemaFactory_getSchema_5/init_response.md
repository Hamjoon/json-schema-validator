```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(String)}.
 *
 * <p>The tests cover the basic contract of the method:
 * <ul>
 *   <li>When a valid JSON schema string is supplied the factory returns a non‑null {@link JsonSchema} instance.</li>
 *   <li>When an invalid JSON string is supplied the method propagates the {@link IOException} thrown while parsing.</li>
 * </ul>
 *
 * <p>The factory is created once for the whole test class using the public {@code getInstance}
 * factory method. No private members of {@link JsonSchemaFactory} or its collaborators are accessed.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_5_Test {

    /** Factory shared by all test methods. */
    private static JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the standard factory for Draft‑2020‑12. No customisation is required for the
        // simple schemas used in these tests.
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
        // Nothing to clean up; the factory does not hold external resources that need explicit closing.
        factory = null;
    }

    /** Simple happy‑path test: a valid JSON schema string should produce a non‑null JsonSchema. */
    @Test
    void getSchema_validJsonString_returnsJsonSchema() {
        // GIVEN a minimal JSON schema that validates a string value
        String schema = "{ \"type\": \"string\" }";

        // WHEN the factory parses the schema string
        JsonSchema jsonSchema = factory.getSchema(schema);

        // THEN a JsonSchema instance is returned and it is not null
        assertNotNull(jsonSchema, "JsonSchema should not be null for a valid schema string");
        // The concrete type is implementation‑specific; we only assert the contract.
        assertTrue(jsonSchema instanceof JsonSchema, "Returned object must implement JsonSchema");
    }

    /** Verify that an ill‑formed JSON string results in an IOException. */
    @Test
    void getSchema_invalidJsonString_throwsIOException() {
        // GIVEN an invalid JSON document (missing closing brace)
        String malformedSchema = "{ \"type\": \"string\" ";

        // WHEN/THEN the factory propagates an IOException
        assertThrows(IOException.class, () -> factory.getSchema(malformedSchema));
    }

    /** Ensure that a null argument is rejected with a NullPointerException. */
    @Test
    void getSchema_nullString_throwsNullPointerException() {
        // GIVEN a null reference
        String nullSchema = null;

        // WHEN/THEN the factory should throw NPE (the implementation checks for null)
        assertThrows(NullPointerException.class, () -> factory.getSchema(nullSchema));
    }
}
```