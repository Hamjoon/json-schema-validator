```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Tests for {@link JsonSchemaFactory#getSchema(String)} delegating to the overload
 * {@link JsonSchemaFactory#getSchema(String, SchemaValidatorsConfig)} and for the
 * caching behaviour of the factory.
 *
 * The original test used Mockito for spying and stubbing, but the Mockito library
 * is not available in the compilation environment.  The tests have therefore
 * been rewritten to use only the real {@link JsonSchemaFactory} implementation.
 */
@DisplayName("JsonSchemaFactory getSchema delegation and caching tests")
class JsonSchemaFactory_getSchema_4_Test {

    /** The factory instance used by the tests. */
    private JsonSchemaFactory factory;

    @BeforeEach
    void setUp() {
        // Build a real factory – this is the public way to obtain an instance.
        factory = JsonSchemaFactory.builder().build();
    }

    /**
     * Verifies that {@code getSchema(String)} delegates to the overload that
     * accepts a {@link SchemaValidatorsConfig} argument by checking that both
     * calls return the same {@link JsonSchema} instance.
     */
    @Test
    void getSchema_delegatesToOverload_withNullConfig() throws Exception {
        // Create a temporary JSON schema file.
        Path tempFile = Files.createTempFile("temp-schema-", ".json");
        Files.writeString(tempFile, "{\"type\":\"string\"}");
        tempFile.toFile().deleteOnExit();

        String uri = tempFile.toUri().toString();

        // Call the single‑argument method.
        JsonSchema viaSingleArg = factory.getSchema(uri);

        // Call the two‑argument overload with a null config.
        JsonSchema viaTwoArgs = factory.getSchema(uri, null);

        // Both calls must return the same (cached) instance.
        assertSame(viaSingleArg, viaTwoArgs,
                "getSchema(String) should delegate to getSchema(String, null) and return the same instance.");
    }

    /**
     * Ensures that the factory caches schemas: repeated calls with the same
     * location must return the identical {@link JsonSchema} object.
     */
    @Test
    void getSchema_cachingBehavior_returnsSameInstanceOnSubsequentCalls() throws Exception {
        // Create a temporary JSON schema file.
        Path tempFile = Files.createTempFile("temp-schema-", ".json");
        Files.writeString(tempFile, "{\"type\":\"string\"}");
        tempFile.toFile().deleteOnExit();

        String uri = tempFile.toUri().toString();

        // First call – loads and caches the schema.
        JsonSchema first = factory.getSchema(uri);
        // Second call – should retrieve the cached instance.
        JsonSchema second = factory.getSchema(uri);

        assertNotNull(first, "First call must return a non‑null JsonSchema");
        assertNotNull(second, "Second call must also return a non‑null JsonSchema");
        assertSame(first, second,
                "Subsequent calls with the same location should return the cached instance");
    }

    /**
     * Provides a minimal set of schema URIs for a parameterised test that checks
     * successful loading of valid schemas and proper exception handling for
     * clearly invalid inputs.
     *
     * Only URI‑based inputs are used because the factory interprets the argument
     * as a location, not raw content.
     */
    private static Stream<Arguments> provideSchemaScenarios() {
        return Stream.of(
                // valid JSON file
                Arguments.of(createTempFile("{\"type\":\"string\"}"), null,
                        "Valid JSON schema file"),
                // valid YAML file
                Arguments.of(createTempFile("type: string"), null,
                        "Valid YAML schema file"),
                // malformed JSON – should raise a parsing exception
                Arguments.of(createTempFile("{ invalid json }"), JsonParseException.class,
                        "Malformed JSON should result in JsonParseException"),
                // malformed YAML – should raise a parsing exception
                Arguments.of(createTempFile("type string: 123"), JsonParseException.class,
                        "Malformed YAML should result in JsonParseException")
        );
    }

    /**
     * Helper that creates a temporary file containing the supplied content and
     * returns its {@code file:} URI as a string.
     */
    private static String createTempFile(String content) {
        try {
            Path temp = Files.createTempFile("schema-", ".tmp");
            Files.writeString(temp, content);
            temp.toFile().deleteOnExit();
            return temp.toUri().toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Parameterised test that loads schemas from the URIs supplied by
     * {@link #provideSchemaScenarios()} and checks for expected success or
     * failure.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideSchemaScenarios")
    void getSchema_variousScenarios(String uri,
                                    Class<? extends Throwable> expectedException,
                                    String description) {
        if (expectedException == null) {
            // Expected successful load.
            JsonSchema schema = factory.getSchema(uri);
            assertNotNull(schema, "Schema should be loaded successfully for: " + description);
        } else {
            // Expected an exception.
            Throwable thrown = assertThrows(expectedException,
                    () -> factory.getSchema(uri),
                    "Expected exception for: " + description);
            assertNotNull(thrown);
        }
    }
}
```