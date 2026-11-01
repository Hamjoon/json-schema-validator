package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import java.io.IOException;
import java.net.URI;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_4_Test {

    /**
     * Factory instance that will be spied on in each test.
     */
    private JsonSchemaFactory realFactory;

    /**
     * Spy wrapping {@link #realFactory}.
     */
    private JsonSchemaFactory factorySpy;

    @BeforeEach
    void setupBeforeEach() {
        /*
         * Build a minimal factory instance using the public builder API.
         * The concrete schema content is irrelevant because we will stub
         * the overloaded getSchema method; we only need a valid instance.
         */
        realFactory = JsonSchemaFactory.getInstance(VersionFlag.V202012, builder -> builder.schemaLoaders(schemaLoaders -> {
            // Register an empty map – the real loader will never be used.
            schemaLoaders.schemas(java.util.Collections.emptyMap());
        }));
        // Create a Mockito spy so that we can stub protected methods.
        factorySpy = Mockito.spy(realFactory);
        /*
         * Build a minimal factory instance using the public builder API.
         * The concrete schema content is irrelevant because we will stub
         * the protected {@code loadSchema} method; we only need a valid
         * instance.
         */
        realFactory = JsonSchemaFactory.getInstance(VersionFlag.V202012, builder -> builder.schemaLoaders(schemaLoaders -> {
            // Register an empty map – the real loader will never be used.
            schemaLoaders.schemas(java.util.Collections.emptyMap());
        }));
        // Spy enables us to stub/verify protected methods.
        factorySpy = Mockito.spy(realFactory);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the spy to avoid cross‑test interference.
        Mockito.reset(factorySpy);
        // Reset the spy to avoid cross‑test interference.
        Mockito.reset(factorySpy);
    }

    /**
     * Provides a stream of arguments for the error‑handling parameterised test.
     * Each entry contains:
     * <ul>
     *   <li>a human‑readable description (used as the test name)</li>
     *   <li>the schema location string</li>
     *   <li>the exception class we expect the factory to propagate</li>
     * </ul>
     */
    private static Stream<Arguments> errorScenarios() {
        return Stream.of(Arguments.of("non‑existent file", "file:/tmp/nonexistent-schema.json", IOException.class), Arguments.of("invalid JSON/YAML content", "classpath:/schemas/invalid-json-schema.json", JsonSchemaException.class), Arguments.of("unsupported URI scheme", "ftp://example.com/schemas/unsupported-schema.json", IllegalArgumentException.class), Arguments.of("malformed URI string", "://bad-uri-format", IllegalArgumentException.class), Arguments.of("unreadable file (IO error)", "file:/tmp/unreadable-schema.json", IOException.class), Arguments.of("empty schema content", "classpath:/schemas/empty-schema.json", JsonSchemaException.class), Arguments.of("binary file supplied as schema", "classpath:/schemas/binary-data.bin", JsonSchemaException.class));
    }

    @Test
    void getSchema_delegatesToOverloadWithDefaultConfig() {
        // Arrange ------------------------------------------------------------
        SchemaLocation mockLocation = mock(SchemaLocation.class);
        SchemaValidatorsConfig mockConfig = mock(SchemaValidatorsConfig.class);
        JsonSchema expectedSchema = mock(JsonSchema.class);
        // Stub the protected method that creates the default config.
        doReturn(mockConfig).when(factorySpy).createSchemaValidatorsConfig();
        // Stub the overloaded getSchema method that performs the real work.
        doReturn(expectedSchema).when(factorySpy).getSchema(mockLocation, mockConfig);
        // Act ---------------------------------------------------------------
        JsonSchema actual = factorySpy.getSchema(mockLocation);
        // Assert -------------------------------------------------------------
        assertSame(expectedSchema, actual, "The method should return the JsonSchema produced by the overloaded variant.");
        // Verify that the delegation happened exactly once.
        verify(factorySpy, times(1)).createSchemaValidatorsConfig();
        verify(factorySpy, times(1)).getSchema(mockLocation, mockConfig);
    }

    @Test
    void getSchema_nullSchemaLocation_throwsException() {
        // Arrange ------------------------------------------------------------
        SchemaLocation nullLocation = null;
        // Act & Assert -------------------------------------------------------
        assertThrows(NullPointerException.class, () -> factorySpy.getSchema(nullLocation), "Passing a null SchemaLocation should result in a NullPointerException.");
    }

    @Test
    void getSchema_successfulLoad_returnsNonNullSchema() throws Exception {
        // Arrange
        SchemaLocation location = new SchemaLocation("classpath:/schemas/simple-schema.json");
        JsonSchema mockSchema = mock(JsonSchema.class);
        SchemaValidatorsConfig cfg = mock(SchemaValidatorsConfig.class);
        // Stub the config creation and the protected load method.
        doReturn(cfg).when(factorySpy).createSchemaValidatorsConfig();
        doReturn(mockSchema).when(factorySpy).loadSchema(eq(location), eq(cfg));
        // Act
        JsonSchema result = factorySpy.getSchema(location);
        // Assert
        assertNotNull(result, "A non‑null JsonSchema should be returned for a valid schema location.");
        assertSame(mockSchema, result, "The returned instance must be the one produced by loadSchema.");
        verify(factorySpy, times(1)).loadSchema(eq(location), eq(cfg));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("errorScenarios")
    void getSchema_errorScenarios_propagateException(String description, String uri, Class<? extends Exception> expectedException) throws Exception {
        // Arrange
        SchemaLocation location = new SchemaLocation(uri);
        SchemaValidatorsConfig cfg = mock(SchemaValidatorsConfig.class);
        doReturn(cfg).when(factorySpy).createSchemaValidatorsConfig();
        // The concrete exception type depends on the scenario.
        Exception toThrow;
        if (expectedException == IOException.class) {
            toThrow = new IOException("simulated I/O problem for " + uri);
        } else if (expectedException == IllegalArgumentException.class) {
            toThrow = new IllegalArgumentException("simulated illegal argument for " + uri);
        } else {
            // JsonSchemaException is the library‑specific wrapper for parsing problems.
            toThrow = new JsonSchemaException("simulated parsing problem for " + uri);
        }
        doThrow(toThrow).when(factorySpy).loadSchema(eq(location), eq(cfg));
        // Act & Assert
        Exception actual = assertThrows(expectedException, () -> factorySpy.getSchema(location), "Expected " + expectedException.getSimpleName() + " for scenario: " + description);
        assertTrue(actual.getMessage().contains(uri), "Exception message should contain the problematic URI for easier debugging.");
    }

    @Test
    void getSchema_caching_behaviour_callsLoadOnlyOnce() throws Exception {
        // Arrange
        SchemaLocation location = new SchemaLocation("classpath:/schemas/cached-schema.json");
        SchemaValidatorsConfig cfg = mock(SchemaValidatorsConfig.class);
        JsonSchema cachedSchema = mock(JsonSchema.class);
        doReturn(cfg).when(factorySpy).createSchemaValidatorsConfig();
        // First call returns the mock; subsequent calls would also return it,
        // but we will verify that the protected method is invoked only once.
        doReturn(cachedSchema).when(factorySpy).loadSchema(eq(location), eq(cfg));
        // Act – call twice
        JsonSchema first = factorySpy.getSchema(location);
        JsonSchema second = factorySpy.getSchema(location);
        // Assert
        assertSame(cachedSchema, first, "First call should return the schema produced by loadSchema.");
        assertSame(cachedSchema, second, "Second call should return the same cached instance.");
        // Verify that loadSchema was invoked only once – the second call hits the cache.
        verify(factorySpy, times(1)).loadSchema(eq(location), eq(cfg));
    }

    @Test
    void getSchema_customSchemaLoader_isInvoked() throws Exception {
        // Arrange – create a mock SchemaLoader that will be injected via the builder.
        SchemaLoader mockLoader = mock(SchemaLoader.class);
        // The loader should return a dummy JsonNode when asked to load a schema.
        // We do not need a real JsonNode – a mock is sufficient.
        JsonNode dummyNode = mock(JsonNode.class);
        when(mockLoader.loadSchema(any(SchemaLocation.class), any(SchemaValidatorsConfig.class))).thenReturn(dummyNode);
        // Build a factory that uses the mocked loader.
        JsonSchemaFactory factoryWithCustomLoader = JsonSchemaFactory.builder().schemaLoaders(loaders -> loaders.schemaLoaders(java.util.Collections.singletonMap("default", mockLoader))).build();
        // Spy the factory so we can verify the later creation steps.
        JsonSchemaFactory spyFactory = Mockito.spy(factoryWithCustomLoader);
        // Stub the downstream creation steps to return a simple JsonSchema.
        JsonSchema finalSchema = mock(JsonSchema.class);
        doReturn(finalSchema).when(spyFactory).newJsonSchema(any(SchemaLocation.class), any(JsonNode.class), any(SchemaValidatorsConfig.class));
        SchemaLocation location = new SchemaLocation("classpath:/schemas/custom-loader-schema.json");
        // Act
        JsonSchema result = spyFactory.getSchema(location);
        // Assert – the result should be the one produced by newJsonSchema.
        assertSame(finalSchema, result, "The schema returned must be the one created after the custom loader supplied a JsonNode.");
        // Verify that our mocked loader was indeed called.
        verify(mockLoader, times(1)).loadSchema(eq(location), any(SchemaValidatorsConfig.class));
    }
}
