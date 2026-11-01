```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * Unit tests for the static helper {@code BaseJsonValidator.checkDiscriminatorMatch}.
 *
 * <p>The tests focus on the observable side‑effects on {@link DiscriminatorContext}
 * because the internal matching logic is encapsulated in private static methods.
 * By exercising the different branches (null discriminator value, missing mapping,
 * and present mapping) we can verify that the method does not throw unexpected
 * exceptions and that it interacts with the {@code DiscriminatorContext}
 * as specified for the “null value” case.</p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_checkDiscriminatorMatch_Test {

    /** Mocked context that records calls made by the method under test. */
    @Mock
    DiscriminatorContext mockContext;

    /** Mocked discriminator object (the JSON node that contains the "mapping" field). */
    @Mock
    ObjectNode mockDiscriminator;

    /** Mocked schema – the actual schema is not consulted by the public method. */
    @Mock
    JsonSchema mockSchema;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Mockito creates fresh mocks for each test method.
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /**
     * When the discriminator property value is {@code null} the method must
     * immediately mark the match on the {@link DiscriminatorContext} and return.
     */
    @Test
    void whenDiscriminatorPropertyValueIsNull_shouldMarkMatchAndReturn() {
        // Act
        BaseJsonValidator.checkDiscriminatorMatch(
                mockContext,
                mockDiscriminator,
                /* discriminatorPropertyValue = */ null,
                mockSchema);

        // Assert – the only expected interaction is a single call to markMatch()
        verify(mockContext, times(1)).markMatch();
        // No further interactions should have occurred
        verifyNoMoreInteractions(mockContext);
    }

    /**
     * When the discriminator property value is non‑null but the discriminator
     * definition does not contain a “mapping” field, the method should delegate
     * to the implicit‑mapping logic. The test only verifies that the method
     * completes without throwing and does not invoke {@code markMatch()} at this
     * point (the implicit logic may decide later whether a match is found).
     */
    @Test
    void whenMappingIsMissing_shouldDelegateToImplicitMatch() {
        // Arrange – the discriminator node does not contain a "mapping" field
        when(mockDiscriminator.get("mapping")).thenReturn(null);

        // Act – property value is a non‑null string
        BaseJsonValidator.checkDiscriminatorMatch(
                mockContext,
                mockDiscriminator,
                "someValue",
                mockSchema);

        // Assert – the method should not have called markMatch() yet
        verify(mockContext, never()).markMatch();
        // No exception means the delegation path was exercised successfully
    }

    /**
     * When the discriminator definition contains a “mapping” field the method
     * first attempts an explicit‑mapping match.  If the explicit match does not
     * succeed and the configuration permits a fallback, the method will then try
     * the implicit‑mapping logic.  This test supplies a dummy mapping node and
     * simply verifies that the method runs to completion without throwing.
     */
    @Test
    void whenMappingIsPresent_shouldAttemptExplicitThenImplicitMatch() {
        // Arrange – a non‑null mapping node (its internal structure is irrelevant
        // for this test because the matching logic is private)
        JsonNode dummyMapping = mock(JsonNode.class);
        when(mockDiscriminator.get("mapping")).thenReturn(dummyMapping);

        // Act
        BaseJsonValidator.checkDiscriminatorMatch(
                mockContext,
                mockDiscriminator,
                "valueFromInstance",
                mockSchema);

        // Assert – no exception means both explicit and (potential) implicit paths were
        // exercised.  We do not verify internal calls because they are private.
        verify(mockContext, never()).markMatch(); // still may be false at this stage
    }
}
```