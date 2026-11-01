```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.InputFormat;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(String, InputFormat)}.
 *
 * The test creates a {@link JsonSchemaFactory} instance via the public
 * {@code getInstance(...)} builder method – this is the recommended way to obtain
 * a factory because the constructor is package‑private and requires a large
 * number of collaborators.  The factory is created once for the whole test
 * class (in {@code @BeforeAll}) and reused for each test case.
 *
 * Two scenarios are covered:
 * <ul>
 *   <li>Providing a valid JSON schema string together with {@link InputFormat#JSON}
 *       returns a non‑null {@link JsonSchema}.</li>
 *   <li>Providing a valid YAML schema string together with {@link InputFormat#YAML}
 *       also returns a non‑null {@link JsonSchema}.</li>
 * </ul>
 *
 * No private members of {@link JsonSchemaFactory} or its collaborators are
 * accessed; the test only uses the public API.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_11_Test {

    /** Factory shared by all test methods. */
    private static JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the public builder to obtain a factory instance.
        // The default meta‑schema (draft‑2020‑12) is sufficient for these tests.
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
        // Release the static reference – not strictly necessary but keeps the
        // test class tidy.
        factory = null;
    }

    /**
     * Verify that a simple JSON schema string can be parsed when the
     * {@link InputFormat} is {@code JSON}.
     */
    @Test
    void getSchema_withJsonString_andJsonFormat_returnsSchema() {
        // GIVEN
        String jsonSchema = "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }";

        // WHEN
        JsonSchema schema = factory.getSchema(jsonSchema, InputFormat.JSON);

        // THEN
        assertNotNull(schema, "The factory should return a non‑null JsonSchema for a valid JSON schema string");
    }

    /**
     * Verify that a simple YAML schema string can be parsed when the
     * {@link InputFormat} is {@code YAML}.
     */
    @Test
    void getSchema_withYamlString_andYamlFormat_returnsSchema() {
        // GIVEN
        String yamlSchema = "type: object\n"
                          + "properties:\n"
                          + "  age:\n"
                          + "    type: integer";

        // WHEN
        JsonSchema schema = factory.getSchema(yamlSchema, InputFormat.YAML);

        // THEN
        assertNotNull(schema, "The factory should return a non‑null JsonSchema for a valid YAML schema string");
    }

    /**
     * Verify that passing {@code null} as the schema string results in an
     * {@link IllegalArgumentException}.  The factory validates its arguments
     * before attempting to read the content.
     */
    @Test
    void getSchema_withNullSchema_throwsException() {
        // GIVEN
        String nullSchema = null;

        // WHEN / THEN
        assertThrows(IllegalArgumentException.class,
                () -> factory.getSchema(nullSchema, InputFormat.JSON),
                "Calling getSchema with a null schema string should throw IllegalArgumentException");
    }

    /**
     * Verify that an unsupported {@link InputFormat} (e.g. {@code null}) is
     * rejected with an {@link IllegalArgumentException}.
     */
    @Test
    void getSchema_withNullInputFormat_throwsException() {
        // GIVEN
        String jsonSchema = "{}";

        // WHEN / THEN
        assertThrows(IllegalArgumentException.class,
                () -> factory.getSchema(jsonSchema, null),
                "Calling getSchema with a null InputFormat should throw IllegalArgumentException");
    }
}
```