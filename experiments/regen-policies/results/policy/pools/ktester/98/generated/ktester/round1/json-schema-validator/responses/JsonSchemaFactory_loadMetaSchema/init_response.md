```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#loadMetaSchema(String, SchemaValidatorsConfig)}.
 *
 * <p>The tests verify two aspects:
 *
 * <ul>
 *   <li>When a custom {@link JsonMetaSchemaFactory} is supplied the call is delegated to it.</li>
 *   <li>The result of {@link JsonSchemaFactory#getMetaSchema(String, SchemaValidatorsConfig)} is cached,
 *       i.e. {@code loadMetaSchema} is invoked only once for the same IRI.</li>
 * </ul>
 *
 * <p>No private fields or methods of the target class are accessed directly; reflection is only used
 * to inject a mock {@code JsonMetaSchemaFactory} into the factory instance for the delegation test.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_loadMetaSchema_Test {

    /** Factory created by the public API – used as a base for the tests. */
    private JsonSchemaFactory factory;

    /** Mocked configuration passed to the factory – its internals are irrelevant for these tests. */
    @Mock
    private SchemaValidatorsConfig mockConfig;

    /** Mocked {@link JsonMetaSchema} that will be returned by the custom meta‑schema factory. */
    @Mock
    private JsonMetaSchema mockMetaSchema;

    /** Mocked {@link JsonMetaSchemaFactory} used to verify delegation. */
    @Mock
    private JsonMetaSchemaFactory mockMetaSchemaFactory;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Obtain a regular factory instance (uses the default meta‑schema factory).
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up at the moment.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown needed.
    }

    /**
     * When a custom {@link JsonMetaSchemaFactory} is present the {@code loadMetaSchema}
     * implementation must delegate to it.
     */
    @Test
    void loadMetaSchema_delegatesToCustomMetaSchemaFactory() throws Exception {
        // ---------------------------------------------------------------------
        // GIVEN – inject a mock JsonMetaSchemaFactory into the factory instance.
        // ---------------------------------------------------------------------
        Field metaFactoryField = JsonSchemaFactory.class.getDeclaredField("metaSchemaFactory");
        metaFactoryField.setAccessible(true);
        metaFactoryField.set(factory, mockMetaSchemaFactory);

        // Configure the mock to return a known JsonMetaSchema.
        when(mockMetaSchemaFactory.getMetaSchema(anyString(), any(JsonSchemaFactory.class), any()))
                .thenReturn(mockMetaSchema);

        // ---------------------------------------------------------------------
        // WHEN – request a meta‑schema.
        // ---------------------------------------------------------------------
        String testIri = "https://example.com/custom/meta/schema";
        JsonMetaSchema result = factory.getMetaSchema(testIri, mockConfig);

        // ---------------------------------------------------------------------
        // THEN – the custom factory must have been called exactly once and the
        //        returned instance must be the one supplied by the mock.
        // ---------------------------------------------------------------------
        assertSame(mockMetaSchema, result, "The meta‑schema returned by the factory should be the mock instance");
        verify(mockMetaSchemaFactory, times(1))
                .getMetaSchema(eq(testIri), same(factory), same(mockConfig));
    }

    /**
     * The result of {@link JsonSchemaFactory#getMetaSchema(String, SchemaValidatorsConfig)}
     * is cached; consequently {@code loadMetaSchema} should be invoked only once for the same IRI.
     */
    @Test
    void loadMetaSchema_isCachedAfterFirstInvocation() {
        // -----------------------------------------------------------------
        // GIVEN – a spy that allows us to verify protected method calls.
        // -----------------------------------------------------------------
        JsonSchemaFactory spyFactory = Mockito.spy(factory);
        String testIri = "https://json-schema.org/draft/2020-12/schema";

        // -----------------------------------------------------------------
        // WHEN – request the same meta‑schema twice.
        // -----------------------------------------------------------------
        JsonMetaSchema first = spyFactory.getMetaSchema(testIri, mockConfig);
        JsonMetaSchema second = spyFactory.getMetaSchema(testIri, mockConfig);

        // -----------------------------------------------------------------
        // THEN – both calls return the same instance and loadMetaSchema
        //        was executed only once.
        // -----------------------------------------------------------------
        assertSame(first, second, "Subsequent calls must return the cached meta‑schema instance");
        // Verify that the protected method was invoked exactly once.
        verify(spyFactory, times(1)).loadMetaSchema(eq(testIri), same(mockConfig));
    }
}
```