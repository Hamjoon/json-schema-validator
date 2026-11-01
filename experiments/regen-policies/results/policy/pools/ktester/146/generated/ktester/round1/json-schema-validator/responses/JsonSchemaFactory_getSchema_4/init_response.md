```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(SchemaLocation)}.
 *
 * The tests focus on the delegation logic of the method:
 * <pre>
 *   public JsonSchema getSchema(final SchemaLocation schemaUri) {
 *       return getSchema(schemaUri, createSchemaValidatorsConfig());
 *   }
 * </pre>
 * By spying on a real {@code JsonSchemaFactory} instance we can verify that
 * the protected {@code createSchemaValidatorsConfig()} method is invoked and
 * that the overloaded {@code getSchema(SchemaLocation, SchemaValidatorsConfig)}
 * method receives the same configuration object.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_4_Test {

    /** Factory instance that will be spied on in each test. */
    private JsonSchemaFactory realFactory;

    /** Spy wrapping {@link #realFactory}. */
    private JsonSchemaFactory factorySpy;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        /*
         * Build a minimal factory instance using the public builder API.
         * The concrete schema content is irrelevant because we will stub
         * the overloaded getSchema method; we only need a valid instance.
         */
        realFactory = JsonSchemaFactory.getInstance(
                VersionFlag.V202012,
                builder -> builder.schemaLoaders(schemaLoaders -> {
                    // Register an empty map – the real loader will never be used.
                    schemaLoaders.schemas(java.util.Collections.emptyMap());
                })
        );

        // Create a Mockito spy so that we can stub protected methods.
        factorySpy = Mockito.spy(realFactory);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the spy to avoid cross‑test interference.
        Mockito.reset(factorySpy);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No cleanup required.
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
        assertSame(expectedSchema, actual,
                "The method should return the JsonSchema produced by the overloaded variant.");

        // Verify that the delegation happened exactly once.
        verify(factorySpy, times(1)).createSchemaValidatorsConfig();
        verify(factorySpy, times(1)).getSchema(mockLocation, mockConfig);
    }

    @Test
    void getSchema_nullSchemaLocation_throwsException() {
        // Arrange ------------------------------------------------------------
        SchemaLocation nullLocation = null;

        // Act & Assert -------------------------------------------------------
        assertThrows(NullPointerException.class,
                () -> factorySpy.getSchema(nullLocation),
                "Passing a null SchemaLocation should result in a NullPointerException.");
    }
}
```