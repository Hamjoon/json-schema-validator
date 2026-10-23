```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(InputStream, SchemaValidatorsConfig)}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Successful loading of a JSON schema from a valid {@link InputStream}
 *       with both {@code null} and non‑null {@link SchemaValidatorsConfig} arguments.</li>
 *   <li>Proper handling of a {@code null} {@link InputStream}, which must result in a
 *       {@link JsonSchemaException} wrapping the underlying cause.</li>
 * </ul>
 *
 * <p>The {@link JsonSchemaFactory} instance is obtained via the public
 * {@link JsonSchemaFactory#getInstance(SpecVersion.VersionFlag)} method, which
 * supplies all required internal collaborators (ObjectMapper, URI factories, etc.).
 * No mocking of internal dependencies is required for these tests.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_6_Test {

    /** Factory used for all test cases. */
    private static JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Use a concrete, publicly‑available factory instance.
        // The V201909 flag is one of the supported spec versions.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        assertNotNull(factory, "Factory should be created successfully");
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test setup required at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test teardown required at the moment.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally.
    }

    /* --------------------------------------------------------------
     *  Parameterized tests for successful schema loading
     * -------------------------------------------------------------- */

    /**
     * Holds the data for each successful loading scenario.
     */
    private static class SuccessCase {
        final String json;                     // JSON text that represents the schema
        final SchemaValidatorsConfig config;  // Config passed to the method (may be null)

        SuccessCase(String json, SchemaValidatorsConfig config) {
            this.json = json;
            this.config = config;
        }

        @Override
        public String toString() {
            return "SuccessCase{json=" + json + ", config=" + (config == null ? "null" : "non‑null") + '}';
        }
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
     * results in a non‑null {@link JsonSchema} instance, irrespective of whether
     * the {@link SchemaValidatorsConfig} argument is {@code null} or not.
     *
     * The test also checks that the parsed JSON structure matches the source
     * string by using reflection to read the private {@code schemaNode}
     * field of {@link JsonSchema}.
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

        // Additional verification: the internal JsonNode should represent the same JSON we provided.
        // JsonSchema does not expose the node publicly, so we resort to reflection.
        java.lang.reflect.Field nodeField = JsonSchema.class.getDeclaredField("schemaNode");
        nodeField.setAccessible(true);
        JsonNode node = (JsonNode) nodeField.get(result);
        assertNotNull(node, "The internal JsonNode should not be null");

        // Compare the textual representation of the parsed node with the original JSON (ignoring whitespace).
        ObjectMapper mapper = new ObjectMapper(); // Jackson mapper for canonical JSON string
        String expectedCanonical = mapper.readTree(tc.json).toString();
        String actualCanonical = node.toString();
        assertEquals(expectedCanonical, actualCanonical,
                "The parsed JsonNode should be semantically equal to the input JSON");
    }

    /* --------------------------------------------------------------
     *  Test for null InputStream handling
     * -------------------------------------------------------------- */

    /**
     * Verify that passing a {@code null} {@link InputStream} results in a
     * {@link JsonSchemaException} whose cause is a {@link NullPointerException}.
     *
     * The method under test catches {@link IOException} and re‑throws a
     * {@link JsonSchemaException}; however, when the stream itself is {@code null},
     * the {@code ObjectMapper.readTree(null)} call throws a {@link NullPointerException},
     * which is wrapped as the cause of the {@code JsonSchemaException}.
     */
    @Test
    void shouldThrowJsonSchemaExceptionWhenInputStreamIsNull() {
        // GIVEN
        InputStream nullStream = null;
        SchemaValidatorsConfig config = new SchemaValidatorsConfig(); // config value does not matter

        // WHEN / THEN
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(nullStream, config),
                "Expected JsonSchemaException when InputStream is null");

        // Ensure the cause is a NullPointerException (or at least non‑null)
        assertNotNull(ex.getCause(), "The cause of JsonSchemaException should be non‑null");
        assertTrue(ex.getCause() instanceof NullPointerException,
                "Expected cause to be NullPointerException but was " + ex.getCause().getClass().getSimpleName());
    }
}
```