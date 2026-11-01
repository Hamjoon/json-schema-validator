```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchemaLoader()}.
 *
 * The tests verify that:
 * <ul>
 *   <li>The returned {@link SchemaLoader} is never {@code null}.</li>
 *   <li>Multiple invocations return the same instance (the factory holds a single field).</li>
 *   <li>A factory built with a custom {@link SchemaLoaders.Builder} can still provide a loader.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchemaLoader_Test {

    /** Factory created with the default builder (no customisation). */
    private JsonSchemaFactory defaultFactory;

    /** Factory created with a custom {@link SchemaLoaders.Builder}. */
    private JsonSchemaFactory customFactory;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Default factory – uses the standard meta‑schema for the 2020‑12 version.
        defaultFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

        // Custom factory – we inject a dummy schema loader map to prove that a
        // builder can be used without breaking the getSchemaLoader contract.
        customFactory = JsonSchemaFactory.builder()
                .schemaLoaders(schemaLoaders -> schemaLoaders
                        .schemas(Collections.singletonMap(
                                "https://example.com/dummy-schema",
                                // a minimal valid JSON schema (as a String)
                                "{ \"type\": \"object\" }")))
                .build();
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    void getSchemaLoader_defaultFactory_notNullAndSingleton() {
        SchemaLoader loader1 = defaultFactory.getSchemaLoader();
        assertNotNull(loader1, "SchemaLoader should not be null for the default factory");

        SchemaLoader loader2 = defaultFactory.getSchemaLoader();
        assertSame(loader1, loader2, "Subsequent calls must return the same SchemaLoader instance");
    }

    @Test
    void getSchemaLoader_customFactory_notNullAndSingleton() {
        SchemaLoader loader1 = customFactory.getSchemaLoader();
        assertNotNull(loader1, "SchemaLoader should not be null for a factory built with custom SchemaLoaders");

        SchemaLoader loader2 = customFactory.getSchemaLoader();
        assertSame(loader1, loader2, "Subsequent calls must return the same SchemaLoader instance even when customised");
    }
}
```