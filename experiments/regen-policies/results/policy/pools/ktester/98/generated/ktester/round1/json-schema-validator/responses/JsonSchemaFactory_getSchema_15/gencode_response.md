```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(String, InputFormat, SchemaValidatorsConfig)}.
 *
 * <p>The test suite covers:
 * <ul>
 *   <li>Successful loading of JSON and YAML schemas with various configurations.</li>
 *   <li>Exception handling when the input is malformed, null or when required arguments are missing.</li>
 * </ul>
 *
 * <p>Each test case is derived from the JSON‑driven specification supplied with the kata.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_15_Test {

    /** Factory instance used by all tests. */
    private static JsonSchemaFactory factory;

    /** Simple valid JSON schema – used in a few tests as a baseline. */
    private static final String VALID_SCHEMA = "{ \"type\": \"object\" }";

    @BeforeAll
    static void setupBeforeAll() {
        // Obtain a factory instance for the 2020‑12 draft – sufficient for all test data.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
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
        // Nothing to clean up globally.
    }

    /* --------------------------------------------------------------------- */
    /* Successful schema loading                                            */
    /* --------------------------------------------------------------------- */

    /**
     * Parameterized test that verifies a variety of well‑formed schemas (JSON or YAML)
     * are parsed into a non‑null {@link JsonSchema} instance.
     *
     * <p>The data set mirrors the "Successful schema loading" groups from the
     * specification.  The {@code config} argument may be {@code null} – the
     * factory is expected to fall back to a default configuration.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successfulSchemaProvider")
    void shouldLoadValidSchema(String description,
                               String schema,
                               InputFormat format,
                               SchemaValidatorsConfig config) {
        // when
        JsonSchema result = factory.getSchema(schema, format, config);

        // then
        assertNotNull(result, "Factory should return a non‑null JsonSchema");
        assertTrue(result instanceof JsonSchema, "Returned object must be a JsonSchema");
    }

    /** Supplies the successful loading scenarios. */
    private static Stream<Arguments> successfulSchemaProvider() {
        return Stream.of(
                // Simple object schema – JSON input – default config
                Arguments.of(
                        "Simple object schema (JSON, default config)",
                        VALID_SCHEMA,
                        InputFormat.JSON,
                        SchemaValidatorsConfig.builder().build()),

                // YAML schema with explicit null config (factory should create a default)
                Arguments.of(
                        "YAML schema with null config",
                        "type: object\nproperties:\n  age:\n    type: integer",
                        InputFormat.YAML,
                        null),

                // Empty object schema – JSON – null config
                Arguments.of(
                        "Empty JSON object schema, null config",
                        "{}",
                        InputFormat.JSON,
                        null),

                // Array schema – JSON – default config
                Arguments.of(
                        "Array schema (JSON)",
                        "{ \"type\": \"array\", \"items\": { \"type\": \"integer\" } }",
                        InputFormat.JSON,
                        SchemaValidatorsConfig.builder().build()),

                // Array schema – YAML – explicit config
                Arguments.of(
                        "Array schema (YAML) with explicit config",
                        "type: array\nitems:\n  type: string",
                        InputFormat.YAML,
                        SchemaValidatorsConfig.builder().build()),

                // Schema containing $schema keyword – JSON – null config
                Arguments.of(
                        "Schema with $schema keyword, null config",
                        "{ \"type\": \"object\", \"properties\": { \"id\": { \"type\": \"string\" } }, \"$schema\": \"http://json-schema.org/draft-07/schema#\" }",
                        InputFormat.JSON,
                        null),

                // Schema with custom fail‑fast flag – JSON – custom config
                Arguments.of(
                        "Schema with custom fail‑fast config",
                        "{ \"type\": \"object\", \"properties\": { \"value\": { \"type\": \"number\" } } }",
                        InputFormat.JSON,
                        SchemaValidatorsConfig.builder().failFast(true).build())
        );
    }

    /* --------------------------------------------------------------------- */
    /* Exception handling (IOException path)                                 */
    /* --------------------------------------------------------------------- */

    /**
     * Parameterized test that verifies malformed input, null arguments, or mismatched
     * format cause a {@link JsonSchemaException} to be thrown.
     *
     * <p>The underlying cause is expected to be an {@link java.io.IOException}
     * (or a subclass such as {@code JsonParseException}) or a {@code NullPointerException}
     * when the method cannot even attempt parsing.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionSchemaProvider")
    void shouldThrowJsonSchemaException(String description,
                                         String schema,
                                         InputFormat format,
                                         SchemaValidatorsConfig config) {
        // when / then
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(schema, format, config),
                "Expected JsonSchemaException for: " + description);

        // The cause should be an IOException or NPE – both indicate the low‑level failure.
        assertNotNull(ex.getCause(), "Exception cause must not be null");
        assertTrue(ex.getCause() instanceof java.io.IOException
                        || ex.getCause() instanceof NullPointerException,
                "Cause should be IOException or NullPointerException, but was: "
                        + ex.getCause().getClass().getName());
    }

    /** Supplies the exception‑inducing scenarios. */
    private static Stream<Arguments> exceptionSchemaProvider() {
        return Stream.of(
                // Malformed JSON – missing closing brace
                Arguments.of(
                        "Malformed JSON schema",
                        "{ \"type\": \"object\", \"properties\": { \"invalid\": [ }",
                        InputFormat.JSON,
                        SchemaValidatorsConfig.builder().build()),

                // Null schema string – should result in NPE/IOException
                Arguments.of(
                        "Null schema string",
                        null,
                        InputFormat.JSON,
                        SchemaValidatorsConfig.builder().build()),

                // Null InputFormat – factory cannot decide how to deserialize
                Arguments.of(
                        "Null InputFormat",
                        "{ \"type\": \"object\" }",
                        null,
                        SchemaValidatorsConfig.builder().build()),

                // Empty string – not valid JSON/YAML
                Arguments.of(
                        "Empty schema string",
                        "",
                        InputFormat.JSON,
                        SchemaValidatorsConfig.builder().build()),

                // Whitespace‑only content – invalid JSON
                Arguments.of(
                        "Whitespace‑only schema",
                        "   \n   ",
                        InputFormat.JSON,
                        SchemaValidatorsConfig.builder().build()),

                // Mismatched format: YAML content declared as JSON
                Arguments.of(
                        "YAML content with JSON InputFormat",
                        "type: object\nproperties:\n  name: { type: string }",
                        InputFormat.JSON,
                        SchemaValidatorsConfig.builder().build()),

                // All arguments null
                Arguments.of(
                        "All arguments null",
                        null,
                        null,
                        null)
        );
    }
}
```