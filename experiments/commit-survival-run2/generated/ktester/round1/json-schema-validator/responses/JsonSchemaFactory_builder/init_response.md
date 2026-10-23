```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#builder()} and its overloads.
 *
 * The tests verify that:
 * <ul>
 *   <li>Calling {@code builder()} returns a non‑null {@link JsonSchemaFactory.Builder} instance.</li>
 *   <li>Each call to {@code builder()} yields a distinct builder object.</li>
 *   <li>The overload {@code builder(JsonSchemaFactory)} also returns a valid builder
 *       and does not throw when supplied with a mock blueprint.</li>
 * </ul>
 *
 * No private members of {@link JsonSchemaFactory} or its dependencies are accessed.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_builder_Test {

    @Mock
    private JsonSchemaFactory mockBlueprint; // used in the overload test

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these simple tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // MockitoExtension takes care of initializing @Mock fields.
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
    void builderReturnsNonNullBuilder() {
        JsonSchemaFactory.Builder builder = JsonSchemaFactory.builder();
        assertNotNull(builder, "builder() should return a non‑null Builder instance");
    }

    @Test
    void builderReturnsNewInstanceOnEachCall() {
        JsonSchemaFactory.Builder first = JsonSchemaFactory.builder();
        JsonSchemaFactory.Builder second = JsonSchemaFactory.builder();
        assertNotSame(first, second, "Each call to builder() should return a new Builder instance");
    }

    @Test
    void builderWithBlueprintReturnsBuilder() {
        // The blueprint is a mock; the method should still return a Builder.
        JsonSchemaFactory.Builder builder = JsonSchemaFactory.builder(mockBlueprint);
        assertNotNull(builder, "builder(JsonSchemaFactory) should return a non‑null Builder instance");
    }
}
```