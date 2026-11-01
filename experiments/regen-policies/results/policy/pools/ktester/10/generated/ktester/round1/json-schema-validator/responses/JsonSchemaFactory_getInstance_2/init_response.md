```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link JsonSchemaFactory#getInstance(SpecVersion.VersionFlag, Consumer)}.
 *
 * The tests verify:
 * <ul>
 *   <li>the static {@code checkVersion} lookup is correctly delegated</li>
 *   <li>the default meta‑schema is registered when no customizer is supplied</li>
 *   <li>a supplied customizer is executed and can add additional meta‑schemas</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getInstance_2_Test {

    /** Common test data */
    private static final String DEFAULT_META_URI = "http://example.com/default-meta";
    private static final String EXTRA_META_URI   = "http://example.com/extra-meta";

    /** Mocks that are reused across test methods */
    private JsonSchemaVersion mockVersion;
    private JsonMetaSchema   mockDefaultMeta;
    private JsonMetaSchema   mockExtraMeta;

    /** A dummy {@code VersionFlag} – the concrete value is irrelevant because we mock the static lookup. */
    private static final SpecVersion.VersionFlag DUMMY_FLAG =
            new SpecVersion.VersionFlag(1L, "DUMMY");

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create fresh mocks for each test to avoid cross‑test interference.
        mockVersion      = mock(JsonSchemaVersion.class);
        mockDefaultMeta  = mock(JsonMetaSchema.class);
        mockExtraMeta    = mock(JsonMetaSchema.class);

        // Default meta‑schema behaviour
        when(mockDefaultMeta.getUri()).thenReturn(DEFAULT_META_URI);
        when(mockVersion.getInstance()).thenReturn(mockDefaultMeta);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; Mockito handles mock lifecycle.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /**
     * Verify that {@code getInstance(flag, null)} registers the default meta‑schema
     * and that the resulting factory can retrieve it via {@code getMetaSchema}.
     */
    @Test
    void testGetInstanceWithoutCustomizer() {
        // Mock the static method that resolves a version flag to a JsonSchemaVersion.
        try (MockedStatic<JsonSchemaFactory> mockedFactory = Mockito.mockStatic(JsonSchemaFactory.class)) {
            mockedFactory.when(() -> JsonSchemaFactory.checkVersion(DUMMY_FLAG))
                         .thenReturn(mockVersion);
            // The real builder() method is used – we do not mock it.
            mockedFactory.when(JsonSchemaFactory::builder).thenCallRealMethod();

            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(DUMMY_FLAG, null);
            assertNotNull(factory, "Factory must not be null");

            // The factory should contain the default meta‑schema we supplied.
            JsonMetaSchema retrieved = factory.getMetaSchema(DEFAULT_META_URI, new SchemaValidatorsConfig());
            assertSame(mockDefaultMeta, retrieved,
                       "Factory should return the default meta‑schema that was added by the method");
        }
    }

    /**
     * Verify that a non‑null customizer is invoked and can add additional meta‑schemas.
     */
    @Test
    void testGetInstanceWithCustomizer() {
        // Prepare the extra meta‑schema mock.
        when(mockExtraMeta.getUri()).thenReturn(EXTRA_META_URI);

        // Define a customizer that adds the extra meta‑schema to the builder.
        Consumer<JsonSchemaFactory.Builder> customizer = builder -> builder.addMetaSchema(mockExtraMeta);

        try (MockedStatic<JsonSchemaFactory> mockedFactory = Mockito.mockStatic(JsonSchemaFactory.class)) {
            mockedFactory.when(() -> JsonSchemaFactory.checkVersion(DUMMY_FLAG))
                         .thenReturn(mockVersion);
            mockedFactory.when(JsonSchemaFactory::builder).thenCallRealMethod();

            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(DUMMY_FLAG, customizer);
            assertNotNull(factory, "Factory must not be null");

            // Both the default and the extra meta‑schema must be present.
            JsonMetaSchema defaultRetrieved = factory.getMetaSchema(DEFAULT_META_URI, new SchemaValidatorsConfig());
            JsonMetaSchema extraRetrieved   = factory.getMetaSchema(EXTRA_META_URI, new SchemaValidatorsConfig());

            assertSame(mockDefaultMeta, defaultRetrieved,
                       "Default meta‑schema should be registered");
            assertSame(mockExtraMeta, extraRetrieved,
                       "Extra meta‑schema added by the customizer should be registered");
        }

        // Verify that the customizer was actually executed.
        // (If it had not been called, the extra meta‑schema would be missing.)
        verify(mockExtraMeta).getUri(); // interaction caused by the later lookup
    }
}
```