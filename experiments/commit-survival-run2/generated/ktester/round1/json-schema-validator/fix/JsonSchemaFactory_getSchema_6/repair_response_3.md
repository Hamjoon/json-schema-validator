```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests for {@link JsonSchemaFactory#getSchema(InputStream, SchemaValidatorsConfig)}.
 *
 * The original test suite expected a {@link JsonSchemaException} when a {@code null}
 * {@link InputStream} was supplied and accessed a private {@code schemaNode} field via
 * reflection. In the current version of the library the method throws an
 * {@link IllegalArgumentException} for a {@code null} stream and the schema node is
 * exposed through the public {@code getSchemaNode()} accessor. Both expectations have
 * been updated accordingly.
 */
class JsonSchemaFactory_getSchema_6_Test {

    /**
     * Factory used for all test cases.
     */
    private static JsonSchemaFactory factory;

    /**
     * Holds the data for each successful loading scenario.
     */
    private static class SuccessCase {
        // JSON text that represents the schema
        final String json;
        // Config passed to the method (may be null)
        final SchemaValidatorsConfig config;

        SuccessCase(String json, SchemaValidatorsConfig config) {
            this.json = json;
            this.config = config;
        }

        @Override
        public String toString() {
            return "SuccessCase{json=" + json + ", config=" + (config == null ? "null" : "non‑null") + '}';
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Use a concrete, publicly‑available factory instance.
        // The V201909 flag is one of the supported spec versions.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        assertNotNull(factory, "Factory should be created successfully");
    }

    /**
     * Supplies the successful test cases defined in the JSON test matrix.
     */
    private static Stream<SuccessCase> successfulSchemaProvider() {
        return Stream.of(
                // case 1 – object schema with a non‑null config
                new SuccessCase("{\"type\":\"object\"}", new SchemaValidatorsConfig()),
                // case 2 – array schema with a null config
                new SuccessCase("{\"type\":\"array\",\"items\":{\"type\":\"string\"}}", null)
        );
    }

    /**
     * Verify that a valid JSON schema supplied as an {@link InputStream}
     * results in a non‑null {@link JsonSchema} instance when the configuration
     * argument is {@code null}.
     */
    @Test
    void shouldReturnJsonSchemaWhenInputStreamIsValidAndConfigIsNull() {
        // GIVEN
        String json = "{\"type\":\"object\"}";
        InputStream schemaStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        // WHEN
        JsonSchema result = factory.getSchema(schemaStream, null);
        // THEN
        assertNotNull(result, "The returned JsonSchema should not be null");
    }

    /**
     * Verify that a valid JSON schema supplied as an {@link InputStream}
     * results in a non‑null {@link JsonSchema} instance when a non‑null
     * {@link SchemaValidatorsConfig} is supplied.
     */
    @Test
    void shouldReturnJsonSchemaWhenInputStreamIsValidAndConfigIsProvided() {
        // GIVEN
        String json = "{\"type\":\"object\"}";
        InputStream schemaStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        // WHEN
        JsonSchema result = factory.getSchema(schemaStream, config);
        // THEN
        assertNotNull(result, "The returned JsonSchema should not be null");
    }

    /**
     * Verify that passing a {@code null} {@link InputStream} results in an
     * {@link IllegalArgumentException}.  The library throws this exception
     * directly rather than wrapping it in a {@link JsonSchemaException}.
     */
    @Test
    void shouldThrowIllegalArgumentExceptionWhenInputStreamIsNull() {
        // GIVEN
        InputStream nullStream = null;
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> factory.getSchema(nullStream, config),
                "Expected IllegalArgumentException when InputStream is null"
        );
        // Ensure the message mentions the argument name – this is optional but useful.
        assertTrue(ex.getMessage().contains("in"), "Exception message should indicate the null argument");
    }

    /**
     * Verify that a valid JSON schema supplied as an {@link InputStream}
     * results in a non‑null {@link JsonSchema} instance, irrespective of whether
     * the {@link SchemaValidatorsConfig} argument is {@code null} or not.
     *
     * The test also checks that the parsed JSON structure matches the source
     * string by using the public {@code getSchemaNode()} accessor of
     * {@link JsonSchema}.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successfulSchemaProvider")
    void shouldReturnJsonSchemaWhenInputStreamIsValid(SuccessCase tc) throws Exception {
        // GIVEN
        InputStream schemaStream = new ByteArrayInputStream(tc.json.getBytes(StandardCharsets.UTF_8));
        // WHEN
        JsonSchema result = factory.getSchema(schemaStream, tc.config);
        // THEN
        assertNotNull(result, "The returned JsonSchema should not be null");

        // Retrieve the internal JsonNode via the public accessor.
        JsonNode node = result.getSchemaNode();
        assertNotNull(node, "The internal JsonNode should not be null");

        // Compare the canonical (whitespace‑agnostic) representation of the
        // parsed node with the original JSON.
        ObjectMapper mapper = new ObjectMapper();
        String expectedCanonical = mapper.readTree(tc.json).toString();
        String actualCanonical = node.toString();
        assertEquals(expectedCanonical, actualCanonical,
                "The parsed JsonNode should be semantically equal to the input JSON");
    }
}
```