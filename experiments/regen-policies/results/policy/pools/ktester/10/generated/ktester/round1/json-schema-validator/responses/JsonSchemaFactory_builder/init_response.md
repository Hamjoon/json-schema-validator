```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for the static {@code builder()} methods of {@link JsonSchemaFactory}.
 *
 * <p>The tests focus on the contract of the public static factory methods:
 * <ul>
 *   <li>{@code JsonSchemaFactory.builder()}</li>
 *   <li>{@code JsonSchemaFactory.builder(JsonSchemaFactory)}</li>
 * </ul>
 * They verify that a non‑null {@link JsonSchemaFactory.Builder} instance is returned
 * and that the overload accepting a blueprint does not throw when a mock blueprint
 * is supplied.</p>
 *
 * <p>No private members of {@code JsonSchemaFactory} or its {@code Builder} are accessed,
 * and no external resources are required.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_builder_Test {

    /** A mock blueprint used for the overloaded {@code builder(JsonSchemaFactory)} method. */
    @Mock
    JsonSchemaFactory blueprintMock;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /**
     * Verifies that the no‑argument {@code builder()} method returns a non‑null Builder.
     */
    @Test
    void testBuilderReturnsNonNullBuilder() {
        JsonSchemaFactory.Builder builder = JsonSchemaFactory.builder();
        assertNotNull(builder, "JsonSchemaFactory.builder() should return a non‑null Builder");
    }

    /**
     * Verifies that the overloaded {@code builder(JsonSchemaFactory)} method returns a
     * non‑null Builder when a valid (mocked) blueprint is supplied.
     */
    @Test
    void testBuilderWithBlueprintReturnsNonNullBuilder() {
        // The blueprint is a Mockito mock; the method should accept it and return a Builder.
        JsonSchemaFactory.Builder builder = JsonSchemaFactory.builder(blueprintMock);
        assertNotNull(builder,
                "JsonSchemaFactory.builder(JsonSchemaFactory) should return a non‑null Builder when a blueprint is provided");
    }
}
```