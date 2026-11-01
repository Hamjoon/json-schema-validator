```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(InputStream, SchemaValidatorsConfig)}.
 *
 * The tests focus on the public contract of the method:
 * <ul>
 *   <li>It should return a non‑null {@link JsonSchema} instance when a valid JSON schema
 *       is supplied via an {@link InputStream}.</li>
 *   <li>The returned {@link JsonSchema} must be usable for validation – a simple
 *       validation of a conforming JSON document should succeed.</li>
 *   <li>Passing a {@code null} {@link InputStream} must result in a {@link NullPointerException}
 *       (the implementation does not accept {@code null} arguments).</li>
 * </ul>
 *
 * The factory is created via {@link JsonSchemaFactory#getInstance(VersionFlag, java.util.function.Consumer)}.
 * No external resources (files, network) are required – the schema is supplied
 * directly from an in‑memory {@link InputStream}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_7_Test {

    /** Factory used for all test cases – created once because it is thread‑safe. */
    private static JsonSchemaFactory factory;

    /** A simple JSON schema used in the tests (type: object). */
    private static final String SIMPLE_SCHEMA = "{ \"type\": \"object\" }";

    /** A JSON document that conforms to {@link #SIMPLE_SCHEMA}. */
    private static final String VALID_INSTANCE = "{}";

    /** A JSON document that does NOT conform to {@link #SIMPLE_SCHEMA}. */
    private static final String INVALID_INSTANCE = "[]";

    @BeforeAll
    static void setupBeforeAll() {
        // Use the 2020‑12 draft – it is the most recent version supported by the library.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012,
                builder -> builder.schemaLoaders(schemaLoaders -> {
                    // No external schemas are required for these tests, but we keep the
                    // configuration explicit to avoid accidental look‑ups.
                }));
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
        factory = null;
    }

    @Test
    void getSchema_fromInputStream_returnsValidJsonSchema() throws Exception {
        // GIVEN: an InputStream containing a valid JSON schema and a default config
        try (InputStream schemaStream = new ByteArrayInputStream(
                SIMPLE_SCHEMA.getBytes(StandardCharsets.UTF_8))) {

            SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

            // WHEN: the factory creates a JsonSchema from the stream
            JsonSchema jsonSchema = factory.getSchema(schemaStream, config);

            // THEN: a non‑null JsonSchema is returned
            assertNotNull(jsonSchema, "JsonSchema should not be null");

            // AND: the schema can validate a conforming JSON instance
            try (InputStream instanceStream = new ByteArrayInputStream(
                    VALID_INSTANCE.getBytes(StandardCharsets.UTF_8))) {
                // The validate method throws a ValidationException when validation fails.
                // If no exception is thrown, the instance is considered valid.
                jsonSchema.validate(instanceStream);
            }

            // AND: validation of a non‑conforming instance must fail
            try (InputStream badInstance = new ByteArrayInputStream(
                    INVALID_INSTANCE.getBytes(StandardCharsets.UTF_8))) {
                assertThrows(ValidationException.class, () -> jsonSchema.validate(badInstance),
                        "Validation should fail for an instance that does not match the schema");
            }
        }
    }

    @Test
    void getSchema_nullInputStream_throwsNullPointerException() {
        // GIVEN: a null InputStream and a default config
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // WHEN / THEN: the method must throw NPE
        assertThrows(NullPointerException.class,
                () -> factory.getSchema((InputStream) null, config),
                "Calling getSchema with a null InputStream should throw NullPointerException");
    }
}
```