```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BaseJsonValidator#checkDiscriminatorMatch(DiscriminatorContext, ObjectNode, String, JsonSchema)}.
 *
 * The tests focus on the public contract of the static method – i.e. how it interacts with
 * {@link DiscriminatorContext}.  The internal private helper methods are not invoked directly
 * and therefore are not verified here.
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_checkDiscriminatorMatch_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
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
     * When the discriminator property value is {@code null} the method must tell the
     * {@link DiscriminatorContext} to ignore the current discriminator and return
     * without further processing.
     */
    @Test
    void shouldMarkIgnoreWhenDiscriminatorPropertyValueIsNull() {
        // Arrange
        DiscriminatorContext ctx = mock(DiscriminatorContext.class);
        ObjectNode discriminator = mock(ObjectNode.class);
        // The mapping node is irrelevant for this path – returning null is fine.
        when(discriminator.get("mapping")).thenReturn(null);
        JsonSchema schema = mock(JsonSchema.class);
        String discriminatorPropertyValue = null; // the condition we want to test

        // Act
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, discriminatorPropertyValue, schema);

        // Assert
        verify(ctx, times(1)).markIgnore();
        // No other interaction with the context should happen for this branch.
        verifyNoMoreInteractions(ctx);
    }

    /**
     * When the discriminator mapping node is absent, the method should delegate to the
     * implicit‑mapping logic.  The private helper cannot be called directly, but we can
     * verify that the method does **not** call {@code markIgnore()} and that it proceeds
     * without throwing an exception.
     */
    @Test
    void shouldProceedToImplicitMappingWhenNoMappingNodePresent() {
        // Arrange
        DiscriminatorContext ctx = mock(DiscriminatorContext.class);
        ObjectNode discriminator = mock(ObjectNode.class);
        // No explicit "mapping" field – triggers the implicit‑mapping path.
        when(discriminator.get("mapping")).thenReturn(null);
        JsonSchema schema = mock(JsonSchema.class);
        String discriminatorPropertyValue = "someValue";

        // Act & Assert – the call must not throw and must not invoke markIgnore().
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, discriminatorPropertyValue, schema);
        verify(ctx, never()).markIgnore();
    }
}
```