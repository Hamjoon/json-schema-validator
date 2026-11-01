```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(String, InputFormat, SchemaValidatorsConfig)}.
 *
 * The tests are organised by the groups defined in the supplied JSON test‑case description.
 * Each group is covered by a dedicated {@code @ParameterizedTest} that receives the
 * parameters for every case via a {@code MethodSource}.
 *
 * The real {@link JsonSchemaFactory} implementation is used – no mocking is required
 * because the method under test delegates to Jackson/YAML parsers which already throw
 * {@code IOException} for malformed input. Those IOExceptions are wrapped in
 * {@link JsonSchemaException} by the method under test, which is what we verify.
 */
class JsonSchemaFactory_getSchema_15_Test {

    private static JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the 2020‑12 draft version – the default meta‑schema is loaded automatically.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    /* --------------------------------------------------------------------- */
    /* -------------------------- Successful parsing ----------------------- */
    /* --------------------------------------------------------------------- */

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successfulSchemaParsingProvider")
    void shouldParseValidSchema(String description,
                                String schema,
                                InputFormat inputFormat,
                                SchemaValidatorsConfig config) {
        // when
        JsonSchema jsonSchema = factory.getSchema(schema, inputFormat, config);

        // then
        assertNotNull(jsonSchema, description + " – JsonSchema must not be null");
        assertNotNull(jsonSchema.getSchemaNode(),
                description + " – underlying JsonNode must be present");
    }

    private static Stream<Arguments> successfulSchemaParsingProvider() {
        return Stream.of(
                // JSON string, explicit JSON format, explicit config
                Arguments.of(
                        "Valid JSON schema string with explicit JSON InputFormat",
                        "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}",
                        InputFormat.JSON,
                        SchemaValidatorsConfig.builder().build()),

                // YAML string, explicit YAML format, explicit config
                Arguments.of(
                        "Valid YAML schema string with explicit YAML InputFormat",
                        "type: object\nproperties:\n  age:\n    type: integer",
                        InputFormat.YAML,
                        SchemaValidatorsConfig.builder().build()),

                // JSON string, null format & config (defaults should be used)
                Arguments.of(
                        "Valid JSON schema with null InputFormat and null config (defaults)",
                        "{\"type\":\"string\"}",
                        null,
                        null),

                // JSON with null config (factory default config)
                Arguments.of(
                        "Valid JSON schema with null config",
                        "{\"type\":\"number\"}",
                        InputFormat.JSON,
                        null),

                // YAML with null config
                Arguments.of(
                        "Valid YAML schema with null config",
                        "type: string\nminLength: 3",
                        InputFormat.YAML,
                        null),

                // JSON content but YAML format – YAML mapper can read JSON
                Arguments.of(
                        "JSON document supplied with InputFormat.YAML (YAML parser accepts JSON)",
                        "{\"type\":\"array\",\"items\":{\"type\":\"integer\"}}",
                        InputFormat.YAML,
                        SchemaValidatorsConfig.builder().build()),

                // Complex JSON, null InputFormat (auto‑detect)
                Arguments.of(
                        "Complex JSON schema with null InputFormat (auto‑detect)",
                        "{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\"}},\"additionalProperties\":false}",
                        null,
                        SchemaValidatorsConfig.builder().build())
        );
    }

    /* --------------------------------------------------------------------- */
    /* --------------------------- IOException branch ---------------------- */
    /* --------------------------------------------------------------------- */

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("ioExceptionHandlingProvider")
    void shouldWrapIOExceptionInJsonSchemaException(String description,
                                                    String schema,
                                                    InputFormat inputFormat,
                                                    SchemaValidatorsConfig config) {
        // when / then
        JsonSchemaException thrown = assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(schema, inputFormat, config),
                description + " – expected JsonSchemaException");
        // The cause should be an IOException (Jackson/YAML parser failure)
        assertTrue(thrown.getCause() instanceof java.io.IOException,
                description + " – cause must be IOException");
    }

    private static Stream<Arguments> ioExceptionHandlingProvider() {
        return Stream.of(
                // Completely invalid content for JSON parser
                Arguments.of(
                        "Malformed JSON string (cannot be parsed)",
                        "this is not a valid json nor yaml",
                        InputFormat.JSON,
                        SchemaValidatorsConfig.builder().build()),

                // Empty string – Jackson treats it as EOF and throws IOException
                Arguments.of(
                        "Empty string schema (JSON parser fails)",
                        "",
                        InputFormat.JSON,
                        SchemaValidatorsConfig.builder().build()),

                // Whitespace‑only content – also results in IOException
                Arguments.of(
                        "Whitespace‑only schema (JSON parser fails)",
                        "   \n\t  ",
                        null,
                        null),

                // YAML content with JSON format – JSON parser fails
                Arguments.of(
                        "YAML content supplied with InputFormat.JSON (JSON parser fails)",
                        "type: object\nproperties:\n  id:\n    type: integer",
                        InputFormat.JSON,
                        SchemaValidatorsConfig.builder().build()),

                // JSON with comments – not valid JSON, parser throws IOException
                Arguments.of(
                        "JSON containing Java‑style comments (invalid JSON)",
                        "{/* comment */\"type\":\"string\"}",
                        InputFormat.JSON,
                        SchemaValidatorsConfig.builder().build())
        );
    }

    /* --------------------------------------------------------------------- */
    /* --------------------------- Null handling -------------------------- */
    /* --------------------------------------------------------------------- */

    @Test
    void shouldThrowNullPointerExceptionWhenSchemaIsNull() {
        // given
        String schema = null;
        InputFormat format = InputFormat.JSON;
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // when / then
        assertThrows(NullPointerException.class,
                () -> factory.getSchema(schema, format, config),
                "Passing a null schema string must raise NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    /* -------------------------- Mixed format handling ------------------- */
    /* --------------------------------------------------------------------- */

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("mixedFormatHandlingProvider")
    void mixedFormatHandling(String description,
                             String schema,
                             InputFormat inputFormat,
                             SchemaValidatorsConfig config,
                             Class<? extends Throwable> expectedException) {
        if (expectedException == null) {
            // Expected a successful parse
            JsonSchema jsonSchema = factory.getSchema(schema, inputFormat, config);
            assertNotNull(jsonSchema, description + " – JsonSchema must not be null");
        } else {
            // Expected an exception (wrapped in JsonSchemaException)
            JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                    () -> factory.getSchema(schema, inputFormat, config),
                    description + " – expected JsonSchemaException");
            assertTrue(expectedException.isInstance(ex.getCause()),
                    description + " – cause should be " + expectedException.getSimpleName());
        }
    }

    private static Stream<Arguments> mixedFormatHandlingProvider() {
        return Stream.of(
                // JSON content with YAML format – succeeds
                Arguments.of(
                        "JSON document with InputFormat.YAML (YAML mapper accepts JSON)",
                        "{\"type\":\"array\",\"items\":{\"type\":\"integer\"}}",
                        InputFormat.YAML,
                        SchemaValidatorsConfig.builder().build(),
                        null),

                // YAML content with JSON format – fails
                Arguments.of(
                        "YAML content with InputFormat.JSON (JSON parser fails)",
                        "type: object\nproperties:\n  id:\n    type: integer",
                        InputFormat.JSON,
                        SchemaValidatorsConfig.builder().build(),
                        java.io.IOException.class)
        );
    }

    /* --------------------------------------------------------------------- */
    /* -------------------------- Parsing edge cases ---------------------- */
    /* --------------------------------------------------------------------- */

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("parsingEdgeCasesProvider")
    void parsingEdgeCases(String description,
                         String schema,
                         InputFormat inputFormat,
                         SchemaValidatorsConfig config,
                         boolean shouldSucceed) {
        if (shouldSucceed) {
            JsonSchema jsonSchema = factory.getSchema(schema, inputFormat, config);
            assertNotNull(jsonSchema, description + " – JsonSchema must not be null");
        } else {
            JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                    () -> factory.getSchema(schema, inputFormat, config),
                    description + " – expected JsonSchemaException");
            assertTrue(ex.getCause() instanceof java.io.IOException,
                    description + " – cause must be IOException");
        }
    }

    private static Stream<Arguments> parsingEdgeCasesProvider() {
        return Stream.of(
                // JSON with comments – invalid
                Arguments.of(
                        "JSON with Java‑style comments (invalid JSON)",
                        "{/* comment */\"type\":\"string\"}",
                        InputFormat.JSON,
                        SchemaValidatorsConfig.builder().build(),
                        false),

                // Valid complex JSON, null InputFormat – should succeed
                Arguments.of(
                        "Complex JSON schema with null InputFormat (auto‑detect)",
                        "{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\"}},\"additionalProperties\":false}",
                        null,
                        SchemaValidatorsConfig.builder().build(),
                        true)
        );
    }
}
```