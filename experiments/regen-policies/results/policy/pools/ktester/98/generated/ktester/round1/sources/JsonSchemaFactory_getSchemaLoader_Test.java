package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Collections;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchemaLoader_Test {

    /**
     * Factory used in the tests.
     */
    private JsonSchemaFactory factory;

    /**
     * Simple JSON schema used for the custom loader test.
     */
    private static final String SIMPLE_SCHEMA = "{ \"type\": \"object\" }";

    /**
     * URI under which the simple schema is registered.
     */
    private static final String SIMPLE_SCHEMA_URI = "https://example.com/simple-schema";

    class MySchemaLoader extends DefaultSchemaLoader {

        MySchemaLoader() {
            super(SchemaMappers.builder().build(), SchemaLoaders.builder().build(), SchemaMappers.builder().build(), new ConcurrentHashMap<>());
        }
    }

    class MySchemaLoader extends DefaultSchemaLoader {

        MySchemaLoader() {
            super(SchemaMappers.builder().build(), SchemaLoaders.builder().build(), SchemaMappers.builder().build(), new ConcurrentHashMap<>());
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        /*
         * Build a factory that registers a single schema (SIMPLE_SCHEMA) under the
         * URI SIMPLE_SCHEMA_URI.  The builder API mirrors the usage shown in the
         * existing JsonSchemaFactoryTest.
         */
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, builder -> builder.schemaLoaders(schemaLoaders -> schemaLoaders.schemas(Collections.singletonMap(SIMPLE_SCHEMA_URI, SIMPLE_SCHEMA))));
    }

    @Test
    void getSchemaLoader_ShouldReturnNonNullInstance() {
        SchemaLoader loader = factory.getSchemaLoader();
        assertNotNull(loader, "SchemaLoader must not be null");
    }

    @Test
    void getSchemaLoader_ShouldReturnSameInstanceOnRepeatedCalls() {
        SchemaLoader first = factory.getSchemaLoader();
        SchemaLoader second = factory.getSchemaLoader();
        assertSame(first, second, "Repeated calls must return the same SchemaLoader instance");
    }

    @Test
    void getSchemaLoader_ShouldLoadSchemaRegisteredViaBuilder() {
        // Arrange
        SchemaLoader loader = factory.getSchemaLoader();
        SchemaLocation location = new SchemaLocation(SIMPLE_SCHEMA_URI);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        // Act
        JsonSchema loadedSchema = loader.loadSchema(location, config);
        // Assert
        assertNotNull(loadedSchema, "The loaded JsonSchema must not be null");
        // The schema node should represent an object type – we verify this via the public API.
        assertEquals("object", loadedSchema.getSchemaNode().get("type").asText(), "Loaded schema should have type 'object'");
    }

    @Test
    void getSchemaLoader_DefaultInstance_ShouldReturnNonNullAndSameReference() {
        // Use the default factory (V201909) as described in the first JSON case.
        JsonSchemaFactory defaultFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        SchemaLoader loader1 = defaultFactory.getSchemaLoader();
        SchemaLoader loader2 = defaultFactory.getSchemaLoader();
        assertNotNull(loader1, "Default factory must provide a non‑null SchemaLoader");
        assertSame(loader1, loader2, "Repeated calls must return the identical SchemaLoader instance");
    }

    @Test
    void getSchemaLoader_CustomLoaderInjectedViaBuilder_ShouldReturnExactInstance() {
        // Create a mock SchemaLoader and inject it through the builder.
        SchemaLoader mockLoader = mock(SchemaLoader.class);
        JsonSchemaFactory customFactory = JsonSchemaFactory.builder().schemaLoader(// builder API for custom loader
        mockLoader).build();
        SchemaLoader returned = customFactory.getSchemaLoader();
        assertSame(mockLoader, returned, "Factory must return the custom SchemaLoader supplied via the builder");
    }

    @Test
    void getSchemaLoader_DistinctFactories_ShouldHaveDifferentLoaders() {
        JsonSchemaFactory factoryA = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        JsonSchemaFactory factoryB = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        SchemaLoader loaderA = factoryA.getSchemaLoader();
        SchemaLoader loaderB = factoryB.getSchemaLoader();
        assertNotSame(loaderA, loaderB, "Two factories must not share the same SchemaLoader instance");
    }

    @Test
    void getSchemaLoader_AfterOtherOperations_ShouldRemainSameInstance() {
        SchemaLoader before = factory.getSchemaLoader();
        // Invoke a few different public methods that could potentially re‑initialise state.
        factory.getSchema("{\"type\":\"string\"}");
        factory.getSchema(URI.create("https://example.com/any"));
        factory.getSchema(SIMPLE_SCHEMA);
        SchemaLoader after = factory.getSchemaLoader();
        assertSame(before, after, "SchemaLoader must stay the same after other factory operations");
    }

    @Test
    void getSchemaLoader_CustomSubclassPreservation() {
        // Define a simple subclass of DefaultSchemaLoader.
        class MySchemaLoader extends DefaultSchemaLoader {

            MySchemaLoader() {
                super(SchemaMappers.builder().build(), SchemaLoaders.builder().build(), SchemaMappers.builder().build(), new ConcurrentHashMap<>());
            }
        }
        MySchemaLoader myLoader = new MySchemaLoader();
        JsonSchemaFactory customFactory = JsonSchemaFactory.builder().schemaLoader(myLoader).build();
        SchemaLoader returned = customFactory.getSchemaLoader();
        assertSame(myLoader, returned, "Returned loader must be the exact subclass instance supplied");
        assertTrue(returned instanceof MySchemaLoader, "Runtime type of the returned loader should be MySchemaLoader");
    }

    @Test
    void getSchemaLoader_ConcurrentAccess_ShouldReturnSameInstance() throws Exception {
        final int THREAD_COUNT = 20;
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        try {
            Callable<SchemaLoader> task = () -> factory.getSchemaLoader();
            // Submit many parallel tasks.
            Set<Future<SchemaLoader>> futures = new HashSet<>();
            for (int i = 0; i < THREAD_COUNT; i++) {
                futures.add(executor.submit(task));
            }
            // Collect results.
            SchemaLoader first = null;
            for (Future<SchemaLoader> f : futures) {
                SchemaLoader loader = f.get(2, TimeUnit.SECONDS);
                if (first == null) {
                    first = loader;
                } else {
                    assertSame(first, loader, "All concurrent invocations must return the identical SchemaLoader instance");
                }
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
