package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.core.JsonParseException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
@DisplayName("JsonSchemaFactory getSchema delegation and caching tests")
class JsonSchemaFactory_getSchema_4_Test {

    /**
     * Spy of the factory created via the public builder API.
     */
    private JsonSchemaFactory factorySpy;

    /**
     * A dummy {@link JsonSchema} instance returned by the overloaded method.
     */
    private JsonSchema dummySchema;

    /**
     * The factory instance used by the tests.
     */
    private JsonSchemaFactory factory;

    @BeforeEach
    void setupBeforeEach() {
        // Build a real factory – this is the public way to obtain an instance.
        factory = JsonSchemaFactory.builder().build();
    }

    /**
     * Supplies the test matrix – one argument set per case described in the
     * JSON document from the task description.
     */
    private static Stream<Arguments> provideSchemaScenarios() {
        return Stream.of(// valid JSON file
        Arguments.of(createTempFile("{\"type\":\"string\"}"), null, "Valid JSON schema file"), // valid YAML file
        Arguments.of(createTempFile("type: string"), null, "Valid YAML schema file"), // malformed JSON – should raise a parsing exception
        Arguments.of(createTempFile("{ invalid json }"), JsonParseException.class, "Malformed JSON should result in JsonParseException"), // malformed YAML – should raise a parsing exception
        Arguments.of(createTempFile("type string: 123"), JsonParseException.class, "Malformed YAML should result in JsonParseException"));
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

    @Test
    void getSchema_delegatesToOverload_withNullConfig() {
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
        assertSame(viaSingleArg, viaTwoArgs, "getSchema(String) should delegate to getSchema(String, null) and return the same instance.");
    }

    @Test
    void getSchema_passesThroughNullWhenConfigIsAbsent() {
        // Arrange – stub a different schema string to ensure the interaction is not
        // hard‑coded to \"dummy-schema\".
        String schemaId = "http://example.com/my-schema.json";
        JsonSchema anotherDummy = mock(JsonSchema.class);
        doReturn(anotherDummy).when(factorySpy).getSchema(Mockito.eq(schemaId), Mockito.isNull());
        // Act
        JsonSchema result = factorySpy.getSchema(schemaId);
        // Assert
        assertSame(anotherDummy, result);
        // Verify delegation.
        verify(factorySpy, times(1)).getSchema(Mockito.eq(schemaId), Mockito.isNull());
    }

    /* ---------------------------------------------------------------
     *  Parameterised tests for the exhaustive list of scenarios.
     * --------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] schema=\"{0}\" → expect {2}")
    @MethodSource("provideSchemaScenarios")
    void getSchema_variousScenarios(String schemaUri, Class<? extends Throwable> expectedException, String description) {
        if (expectedException == null) {
            // Expected successful load.
            JsonSchema loaded = factory.getSchema(schemaUri);
            assertNotNull(loaded, "Schema should be loaded successfully for: " + description);
        } else {
            // Expected an exception.
            Throwable thrown = assertThrows(expectedException, () -> factory.getSchema(schemaUri), "Expected exception for: " + description);
            assertNotNull(thrown);
        }
    }

    /* ---------------------------------------------------------------
     *  Specific test for the caching behaviour using a *real* factory.
     * --------------------------------------------------------------- */
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
        assertSame(first, second, "Subsequent calls with the same location should return the cached instance");
    }
}
