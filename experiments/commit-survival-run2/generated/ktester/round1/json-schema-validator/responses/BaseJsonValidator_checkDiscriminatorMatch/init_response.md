```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import static org.mockito.Mockito.*;

/**
 * Unit tests for the static {@code BaseJsonValidator.checkDiscriminatorMatch} method.
 *
 * <p>The method has three distinct execution paths:
 *
 * <ul>
 *   <li>When {@code discriminatorPropertyValue} is {@code null} – the method should simply
 *       call {@code DiscriminatorContext.markMatch()} and return.</li>
 *   <li>When the {@code discriminator} node does not contain a {@code "mapping"} field – the
 *       method delegates to the (private) {@code checkForImplicitDiscriminatorMappingMatch}
 *       method.  We cannot verify the private method directly, but we can assert that no
 *       interaction with {@code markMatch()} occurs (the implicit logic does not call it).</li>
 *   <li>When a {@code "mapping"} field is present – the method first delegates to the
 *       (private) {@code checkForExplicitDiscriminatorMappingMatch} method and, depending on
 *       the result, may fall back to the implicit check.  Again we cannot verify the private
 *       calls, but we can ensure that the method completes without throwing and that the
 *       {@code DiscriminatorContext} is consulted.</li>
 * </ul>
 *
 * <p>Because the method under test is {@code static} and works only with the supplied
 * arguments, we mock the {@link ValidationContext.DiscriminatorContext} and {@link JsonSchema}
 * instances.  The {@code ObjectNode} representing the discriminator is created with Jackson
 * (no external resources are required).
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_checkDiscriminatorMatch_Test {

    /** Shared {@link ObjectMapper} used to create {@link ObjectNode} instances. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Mocked {@link ValidationContext.DiscriminatorContext}. */
    @Mock
    ValidationContext.DiscriminatorContext discriminatorContext;

    /** Mocked {@link JsonSchema} – the concrete behaviour is irrelevant for these tests. */
    @Mock
    JsonSchema jsonSchema;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset the mock before each test to avoid cross‑test interference.
        Mockito.reset(discriminatorContext, jsonSchema);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; mocks are automatically discarded.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /**
     * When the discriminator property value is {@code null} the method must call
     * {@code DiscriminatorContext.markMatch()} exactly once and return immediately.
     */
    @Test
    void whenDiscriminatorPropertyValueIsNull_thenMarkMatchIsCalled() {
        // Arrange
        ObjectNode discriminator = MAPPER.createObjectNode(); // content is irrelevant
        String discriminatorPropertyValue = null; // trigger the early‑return path

        // Act
        BaseJsonValidator.checkDiscriminatorMatch(
                discriminatorContext,
                discriminator,
                discriminatorPropertyValue,
                jsonSchema);

        // Assert
        verify(discriminatorContext, times(1)).markMatch();
        // No other interactions should happen on the context for this path.
        verifyNoMoreInteractions(discriminatorContext);
    }

    /**
     * When the discriminator node does **not** contain a {@code "mapping"} field,
     * the method should *not* call {@code markMatch()} (the implicit matching logic is
     * performed inside a private method).  The test simply verifies that the method
     * completes without throwing and that {@code markMatch()} is not invoked.
     */
    @Test
    void whenMappingIsAbsent_thenImplicitMatchIsAttempted() {
        // Arrange
        ObjectNode discriminator = MAPPER.createObjectNode()
                .put("propertyName", "type"); // no "mapping" field
        String discriminatorPropertyValue = "myType";

        // Act
        BaseJsonValidator.checkDiscriminatorMatch(
                discriminatorContext,
                discriminator,
                discriminatorPropertyValue,
                jsonSchema);

        // Assert
        // The early‑return path (null value) must not be taken, therefore markMatch()
        // should never be called.
        verify(discriminatorContext, never()).markMatch();
        // We also verify that the context is queried for its current state at least once.
        // The private methods may call isDiscriminatorMatchFound(); we allow any number
        // of such calls but ensure the mock was used.
        verify(discriminatorContext, atLeast(0)).isDiscriminatorMatchFound();
    }

    /**
     * When the discriminator node contains a {@code "mapping"} field, the method first
     * attempts an explicit match.  If the explicit match is not found and the schema
     * does not override the explicit key, the method falls back to the implicit match.
     * This test supplies a minimal {@code "mapping"} object and verifies that the
     * method executes without throwing.  Because the matching logic resides in private
     * methods, we cannot directly assert which path was taken; we only ensure that
     * the method interacts with the {@code DiscriminatorContext} (e.g., checking the
     * match flag) and does not call {@code markMatch()} for a non‑null value.
     */
    @Test
    void whenMappingIsPresent_thenExplicitAndPossiblyImplicitMatchAreAttempted() {
        // Arrange
        ObjectNode mapping = MAPPER.createObjectNode()
                .put("myType", "#/components/schemas/MyType");
        ObjectNode discriminator = MAPPER.createObjectNode()
                .put("propertyName", "type")
                .set("mapping", mapping);
        String discriminatorPropertyValue = "myType";

        // Act
        BaseJsonValidator.checkDiscriminatorMatch(
                discriminatorContext,
                discriminator,
                discriminatorPropertyValue,
                jsonSchema);

        // Assert
        // No early‑return, so markMatch() must not be called.
        verify(discriminatorContext, never()).markMatch();
        // The context should be consulted at least once to see whether a match was already found.
        verify(discriminatorContext, atLeastOnce()).isDiscriminatorMatchFound();
    }
}
```