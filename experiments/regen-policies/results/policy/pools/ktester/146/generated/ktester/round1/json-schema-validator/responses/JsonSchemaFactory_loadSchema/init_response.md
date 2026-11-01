```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#loadSchema(SchemaLocation, SchemaValidatorsConfig)}.
 *
 * <p>The tests verify the behaviour of the method when the internal cache is
 * enabled and when it is disabled.  A {@link JsonSchemaFactory} instance is
 * created via the public {@code getInstance(...)} factory method and then
 * tweaked via reflection so that the {@code enableSchemaCache} flag can be
 * controlled without relying on any private API.</p>
 *
 * <p>We use a {@link Mockito#spy(Object)} on the factory to stub the
 * {@code getMappedSchema(..)} call – this is the only external interaction
 * that the method performs.  The returned {@link JsonSchema} is also a
 * mock, and its {@code withConfig(..)} method is stubbed to return the same
 * mock instance.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_loadSchema_Test {

    /** The factory under test – a spy so we can stub {@code getMappedSchema}. */
    private JsonSchemaFactory factorySpy;

    /** A mock {@link JsonSchema} that will be returned from {@code getMappedSchema}. */
    @Mock
    private JsonSchema mappedSchemaMock;

    /** A simple {@link SchemaValidatorsConfig} instance used for all calls. */
    private SchemaValidatorsConfig config;

    /** A {@link SchemaLocation} that will be used as the key for the cache. */
    private SchemaLocation schemaLocation;

    /** Reflection helper to invoke the protected {@code loadSchema} method. */
    private Method loadSchemaMethod;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // -----------------------------------------------------------------
        // 1️⃣  Create a real factory (cache disabled by default) and turn it
        //     into a spy so we can stub the protected method that fetches the
        //     schema.
        // -----------------------------------------------------------------
        JsonSchemaFactory realFactory = JsonSchemaFactory.getInstance(
                SpecVersion.VersionFlag.V202012);
        factorySpy = Mockito.spy(realFactory);

        // -----------------------------------------------------------------
        // 2️⃣  Prepare the objects that will be passed to the method.
        // -----------------------------------------------------------------
        config = SchemaValidatorsConfig.builder().build();
        schemaLocation = SchemaLocation.of("http://example.com/test-schema");

        // -----------------------------------------------------------------
        // 3️⃣  Stub the behaviour of the dependent objects.
        // -----------------------------------------------------------------
        // The factory calls getMappedSchema(..) – we want it to return our mock.
        doReturn(mappedSchemaMock).when(factorySpy).getMappedSchema(eq(schemaLocation), eq(config));
        // The returned schema is expected to be re‑configured with the supplied config.
        when(mappedSchemaMock.withConfig(eq(config))).thenReturn(mappedSchemaMock);

        // -----------------------------------------------------------------
        // 4️⃣  Obtain a reference to the protected method via reflection.
        // -----------------------------------------------------------------
        loadSchemaMethod = JsonSchemaFactory.class.getDeclaredMethod(
                "loadSchema", SchemaLocation.class, SchemaValidatorsConfig.class);
        loadSchemaMethod.setAccessible(true);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for now.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /**
     * Helper that invokes the protected {@code loadSchema} method on the spy.
     */
    private JsonSchema invokeLoadSchema(SchemaLocation location, SchemaValidatorsConfig cfg) throws Exception {
        return (JsonSchema) loadSchemaMethod.invoke(factorySpy, location, cfg);
    }

    /** -----------------------------------------------------------------
     *  When the internal cache is enabled the factory must call
     *  {@code getMappedSchema} only once for the same {@link SchemaLocation}
     *  and subsequently return the cached instance (still wrapped with the
     *  supplied config).
     *  ----------------------------------------------------------------- */
    @Test
    void loadSchema_cacheEnabled_callsGetMappedSchemaOnlyOnce() throws Exception {
        // -----------------------------------------------------------------
        // Enable the cache via reflection (the field is final, but we can
        // modify the underlying value because it is a primitive boolean).
        // -----------------------------------------------------------------
        Field cacheFlag = JsonSchemaFactory.class.getDeclaredField("enableSchemaCache");
        cacheFlag.setAccessible(true);
        cacheFlag.setBoolean(factorySpy, true);

        // Ensure the internal cache map is empty before the test.
        Field cacheMapField = JsonSchemaFactory.class.getDeclaredField("schemaCache");
        cacheMapField.setAccessible(true);
        cacheMapField.set(factorySpy, new ConcurrentHashMap<>());

        // -----------------------------------------------------------------
        // First call – should trigger a real load (i.e. getMappedSchema).
        // -----------------------------------------------------------------
        JsonSchema first = invokeLoadSchema(schemaLocation, config);
        assertSame(mappedSchemaMock, first, "First call should return the mocked schema");

        // -----------------------------------------------------------------
        // Second call – because the cache is enabled the factory must return
        // the cached instance and must NOT call getMappedSchema again.
        // -----------------------------------------------------------------
        JsonSchema second = invokeLoadSchema(schemaLocation, config);
        assertSame(mappedSchemaMock, second, "Second call should return the same cached schema");

        // Verify interaction count.
        verify(factorySpy, times(1)).getMappedSchema(eq(schemaLocation), eq(config));
        // The withConfig method is called for each retrieval (once per call).
        verify(mappedSchemaMock, times(2)).withConfig(eq(config));
    }

    /** -----------------------------------------------------------------
     *  When the cache is disabled the factory must delegate to
     *  {@code getMappedSchema} for every invocation.
     *  ----------------------------------------------------------------- */
    @Test
    void loadSchema_cacheDisabled_callsGetMappedSchemaEveryTime() throws Exception {
        // Ensure cache is disabled.
        Field cacheFlag = JsonSchemaFactory.class.getDeclaredField("enableSchemaCache");
        cacheFlag.setAccessible(true);
        cacheFlag.setBoolean(factorySpy, false);

        // First call.
        JsonSchema first = invokeLoadSchema(schemaLocation, config);
        assertSame(mappedSchemaMock, first, "First call should return the mocked schema");

        // Second call – should invoke getMappedSchema again.
        JsonSchema second = invokeLoadSchema(schemaLocation, config);
        assertSame(mappedSchemaMock, second, "Second call should also return the mocked schema");

        // Verify that getMappedSchema was invoked twice.
        verify(factorySpy, times(2)).getMappedSchema(eq(schemaLocation), eq(config));
        // withConfig is called for each call.
        verify(mappedSchemaMock, times(2)).withConfig(eq(config));
    }
}
```