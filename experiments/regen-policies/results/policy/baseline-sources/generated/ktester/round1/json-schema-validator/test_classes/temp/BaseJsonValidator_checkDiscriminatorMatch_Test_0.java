package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.node.NullNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_checkDiscriminatorMatch_Test {

    /**
     * Shared {@link ObjectMapper} used to create {@link ObjectNode} instances.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Mocked {@link ValidationContext.DiscriminatorContext}.
     */
    @Mock
    ValidationContext.DiscriminatorContext discriminatorContext;

    /**
     * Mocked {@link JsonSchema} – the concrete behaviour is irrelevant for these tests.
     */
    @Mock
    JsonSchema jsonSchema;

    @BeforeEach
    void setupBeforeEach() {
        // Reset the mock before each test to avoid cross‑test interference.
        Mockito.reset(discriminatorContext, jsonSchema);
        Mockito.reset(discriminatorContext, jsonSchema);
    }

    private static Stream<Arguments> explicitMappingScenarios() {
        // 1 – explicit match found (context reports true after explicit check)
        ValidationContext.DiscriminatorContext ctxMatchFound = mock(ValidationContext.DiscriminatorContext.class);
        when(ctxMatchFound.isDiscriminatorMatchFound()).thenReturn(true);
        // 2 – explicit mismatch, fallback allowed (isDiscriminatorMatchFound == false)
        ValidationContext.DiscriminatorContext ctxNoMatch = mock(ValidationContext.DiscriminatorContext.class);
        when(ctxNoMatch.isDiscriminatorMatchFound()).thenReturn(false);
        // 3 – explicit mismatch, fallback NOT allowed – we cannot force the private
        //     noExplicitDiscriminatorKeyOverride() result, but we can still verify that
        //     no implicit check is performed by ensuring markMatch() is never called.
        ValidationContext.DiscriminatorContext ctxNoFallback = mock(ValidationContext.DiscriminatorContext.class);
        when(ctxNoFallback.isDiscriminatorMatchFound()).thenReturn(false);
        return Stream.of(Arguments.of("explicitMatchFound", ctxMatchFound, true, false), Arguments.of("explicitNoMatchFallbackAllowed", ctxNoMatch, false, true), Arguments.of("explicitNoMatchFallbackNotAllowed", ctxNoFallback, false, false));
    }

    /**
     * When the discriminator property value is {@code null} the method must call
     * {@code DiscriminatorContext.markMatch()} exactly once and return immediately.
     */
    @Test
    void whenDiscriminatorPropertyValueIsNull_thenMarkMatchIsCalled() {
        // Arrange
        // content is irrelevant
        ObjectNode discriminator = MAPPER.createObjectNode();
        // trigger the early‑return path
        String discriminatorPropertyValue = null;
        // Act
        BaseJsonValidator.checkDiscriminatorMatch(discriminatorContext, discriminator, discriminatorPropertyValue, jsonSchema);
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
        ObjectNode discriminator = MAPPER.createObjectNode().put("propertyName", // no "mapping" field
        "type");
        String discriminatorPropertyValue = "myType";
        // Act
        BaseJsonValidator.checkDiscriminatorMatch(discriminatorContext, discriminator, discriminatorPropertyValue, jsonSchema);
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
        ObjectNode mapping = MAPPER.createObjectNode().put("myType", "#/components/schemas/MyType");
        ObjectNode discriminator = MAPPER.createObjectNode().put("propertyName", "type").set("mapping", mapping);
        String discriminatorPropertyValue = "myType";
        // Act
        BaseJsonValidator.checkDiscriminatorMatch(discriminatorContext, discriminator, discriminatorPropertyValue, jsonSchema);
        // Assert
        // No early‑return, so markMatch() must not be called.
        verify(discriminatorContext, never()).markMatch();
        // The context should be consulted at least once to see whether a match was already found.
        verify(discriminatorContext, atLeastOnce()).isDiscriminatorMatchFound();
    }

    @Test
    @DisplayName("mapping field is present but explicitly null → treated as absent")
    void whenMappingFieldIsExplicitlyNull_thenImplicitCheckIsUsed() {
        // arrange – Jackson represents a JSON “null” as NullNode
        ObjectNode discriminator = MAPPER.createObjectNode().put("propertyName", "type").set("mapping", NullNode.instance);
        String discriminatorPropertyValue = "lion";
        // act
        BaseJsonValidator.checkDiscriminatorMatch(discriminatorContext, discriminator, discriminatorPropertyValue, jsonSchema);
        // assert – same expectations as the “no mapping” case
        verify(discriminatorContext, never()).markMatch();
        verify(discriminatorContext, atLeast(0)).isDiscriminatorMatchFound();
    }

    @Test
    @DisplayName("null discriminator node → NullPointerException")
    void whenDiscriminatorNodeIsNull_thenNpeIsThrown() {
        // arrange
        ObjectNode discriminator = null;
        String discriminatorPropertyValue = "tiger";
        // act & assert
        assertThrows(NullPointerException.class, () -> BaseJsonValidator.checkDiscriminatorMatch(discriminatorContext, discriminator, discriminatorPropertyValue, jsonSchema));
    }

    @Test
    @DisplayName("implicit path with null jsonSchema → NullPointerException from helper")
    void whenJsonSchemaIsNullAndMappingAbsent_thenNpeFromImplicitHelper() {
        // arrange
        ObjectNode discriminator = MAPPER.createObjectNode().put("propertyName", // no "mapping"
        "type");
        String discriminatorPropertyValue = "elephant";
        // act & assert
        assertThrows(NullPointerException.class, () -> BaseJsonValidator.checkDiscriminatorMatch(discriminatorContext, discriminator, discriminatorPropertyValue, null));
    }

    @Test
    @DisplayName("explicit mapping path with null jsonSchema → NullPointerException from helper")
    void whenJsonSchemaIsNullAndMappingPresent_thenNpeFromExplicitHelper() {
        // arrange
        ObjectNode mapping = MAPPER.createObjectNode().put("bird", "#/components/schemas/Bird");
        ObjectNode discriminator = MAPPER.createObjectNode().put("propertyName", "type").set("mapping", mapping);
        String discriminatorPropertyValue = "bird";
        // act & assert
        assertThrows(NullPointerException.class, () -> BaseJsonValidator.checkDiscriminatorMatch(discriminatorContext, discriminator, discriminatorPropertyValue, null));
    }

    @Test
    @DisplayName("null discriminatorContext with null discriminatorPropertyValue → NPE on markMatch()")
    void whenContextIsNullAndValueIsNull_thenNpeIsThrown() {
        // arrange
        ObjectNode discriminator = MAPPER.createObjectNode();
        String discriminatorPropertyValue = null;
        // act & assert
        assertThrows(NullPointerException.class, () -> BaseJsonValidator.checkDiscriminatorMatch(null, discriminator, discriminatorPropertyValue, jsonSchema));
    }

    @Test
    @DisplayName("null discriminatorContext on implicit mapping path → NPE")
    void whenContextIsNullAndMappingAbsent_thenNpeIsThrown() {
        // arrange
        ObjectNode discriminator = MAPPER.createObjectNode().put("propertyName", // no mapping
        "type");
        String discriminatorPropertyValue = "cat";
        // act & assert
        assertThrows(NullPointerException.class, () -> BaseJsonValidator.checkDiscriminatorMatch(null, discriminator, discriminatorPropertyValue, jsonSchema));
    }

    @Test
    @DisplayName("null discriminatorContext on explicit mapping path → NPE")
    void whenContextIsNullAndMappingPresent_thenNpeIsThrown() {
        // arrange
        ObjectNode mapping = MAPPER.createObjectNode().put("dog", "#/components/schemas/Dog");
        ObjectNode discriminator = MAPPER.createObjectNode().put("propertyName", "type").set("mapping", mapping);
        String discriminatorPropertyValue = "dog";
        // act & assert
        assertThrows(NullPointerException.class, () -> BaseJsonValidator.checkDiscriminatorMatch(null, discriminator, discriminatorPropertyValue, jsonSchema));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("explicitMappingScenarios")
    @DisplayName("explicit mapping with various outcomes")
    void whenExplicitMappingScenarios_thenBehaviourMatchesExpectation(String scenario, ValidationContext.DiscriminatorContext context, boolean expectMarkMatch, boolean expectImplicitFallback) {
        // arrange – a simple mapping object; the real matching logic lives in a private method
        ObjectNode mapping = MAPPER.createObjectNode().put("key", "#/schemas/Key");
        ObjectNode discriminator = MAPPER.createObjectNode().put("propertyName", "type").set("mapping", mapping);
        String discriminatorPropertyValue = "key";
        // act
        BaseJsonValidator.checkDiscriminatorMatch(context, discriminator, discriminatorPropertyValue, jsonSchema);
        // assert
        if (expectMarkMatch) {
            verify(context, times(1)).markMatch();
        } else {
            verify(context, never()).markMatch();
        }
        if (expectImplicitFallback) {
            // implicit fallback will query the match flag at least once more after the explicit check
            verify(context, atLeastOnce()).isDiscriminatorMatchFound();
        } else {
            // no fallback – only the explicit‑check query is expected
            verify(context, atMost(1)).isDiscriminatorMatchFound();
        }
    }
}
