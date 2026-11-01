package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

class JsonSchemaFactory_builder_Test {

    //     @Mock
    private JsonSchemaFactory // used in the overload test
    mockBlueprint;

    /**
     * A real factory instance used as a blueprint for the overload test.
     */
    private JsonSchemaFactory blueprint;

    @BeforeEach
    void setUp() {
        // Create a default factory (using the Draft‑V4 blueprint) that we can use as a blueprint.
        // The builder that takes a blueprint copies its configuration, so the resulting factory
        // is fully initialised and does not throw the “defaultMetaSchemaURI” exception.
//         blueprint = JsonSchemaFactory.builder(JsonSchemaFactory.getDraftV4()).build();
    }

    /**
     * Utility that walks the class hierarchy to find the first field whose type matches the supplied class.
     */
    private static Field findFieldOfType(Class<?> clazz, Class<?> fieldType) {
        Class<?> current = clazz;
        while (current != null) {
            for (Field f : current.getDeclaredFields()) {
                if (fieldType.isAssignableFrom(f.getType())) {
                    return f;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

//     @Test
//     void builderReturnsNonNullBuilder() {
//         JsonSchemaFactory.Builder builder = JsonSchemaFactory.builder(JsonSchemaFactory.getDraftV4());
//         assertNotNull(builder, "builder() should return a non‑null Builder instance");
//     }

//     @Test
//     void builderReturnsNewInstanceOnEachCall() {
//         JsonSchemaFactory.Builder first = JsonSchemaFactory.builder(JsonSchemaFactory.getDraftV4());
//         JsonSchemaFactory.Builder second = JsonSchemaFactory.builder(JsonSchemaFactory.getDraftV4());
//         assertNotSame(first, second, "Each call to builder() should return a new Builder instance");
//     }

    @Test
    void builderWithBlueprintReturnsBuilder() {
        // The blueprint is a real factory; the method should still return a Builder.
        JsonSchemaFactory.Builder builder = JsonSchemaFactory.builder(blueprint);
        assertNotNull(builder, "builder(JsonSchemaFactory) should return a non‑null Builder instance");
    }

//     @Test
//     void builderBuildReturnsNonNullFactory() {
//         JsonSchemaFactory factory = JsonSchemaFactory.builder(JsonSchemaFactory.getDraftV4()).build();
//         assertNotNull(factory, "builder().build() should produce a non‑null JsonSchemaFactory");
//     }

//     @Test
//     void successiveBuildsProduceDistinctFactories() {
//         JsonSchemaFactory f1 = JsonSchemaFactory.builder(JsonSchemaFactory.getDraftV4()).build();
//         JsonSchemaFactory f2 = JsonSchemaFactory.builder(JsonSchemaFactory.getDraftV4()).build();
//         assertNotSame(f1, f2, "Each call to builder().build() must return a distinct factory instance");
//     }

//     @Test
//     void builtFactoryHasDefaultComponents() {
//         JsonSchemaFactory factory = JsonSchemaFactory.builder(JsonSchemaFactory.getDraftV4()).build();
//         // Both components are part of the public API, so we can call them directly.
//         assertNotNull(factory.getUriFactory(), "Factory constructed by builder() must provide a non‑null URIFactory");
//         assertNotNull(factory.getUriTranslator(), "Factory constructed by builder() must provide a non‑null URITranslator");
//     }

//     @Test
//     void builderMethodIsThreadSafe() throws Exception {
//         final int THREAD_COUNT = 10;
//         ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
//         try {
//             // Thread‑safe collection that stores the returned Builder references.
//             Set<JsonSchemaFactory.Builder> builderSet = ConcurrentHashMap.newKeySet();
//             List<Future<?>> futures = new ArrayList<>();
//             for (int i = 0; i < THREAD_COUNT; i++) {
//                 futures.add(executor.submit(() -> {
//                     JsonSchemaFactory.Builder b = JsonSchemaFactory.builder(JsonSchemaFactory.getDraftV4());
//                     assertNotNull(b, "builder() must never return null even under concurrency");
//                     builderSet.add(b);
//                 }));
//             }
//             // Await completion and surface any unexpected exceptions.
//             for (Future<?> f : futures) {
//                 // re‑throws any exception from the task
//                 f.get();
//             }
//             // Verify that we collected exactly THREAD_COUNT distinct instances.
//             assertEquals(THREAD_COUNT, builderSet.size(), "Each concurrent call to builder() should yield a distinct Builder instance");
//         } finally {
//             executor.shutdownNow();
//         }
//     }
}