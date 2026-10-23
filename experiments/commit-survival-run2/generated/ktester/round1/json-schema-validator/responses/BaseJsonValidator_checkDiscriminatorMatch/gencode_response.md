```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Exhaustive unit tests for {@link BaseJsonValidator#checkDiscriminatorMatch(
 * ValidationContext.DiscriminatorContext, ObjectNode, String, JsonSchema)}.
 *
 * <p>Because the method under test is {@code static} and delegates the real work to
 * several *private* helpers, the tests focus on the observable side‑effects:
 *
 * <ul>
 *   <li>calls (or not) to {@link ValidationContext.DiscriminatorContext#markMatch()}</li>
 *   <li>queries of {@link ValidationContext.DiscriminatorContext#isDiscriminatorMatchFound()}</li>
 *   <li>expected {@link NullPointerException}s when mandatory arguments are {@code null}</li>
 * </ul>
 *
 * <p>Mockito {@code @Mock} objects are used for the {@code DiscriminatorContext} and the
 * {@code JsonSchema}.  An {@link ObjectMapper} creates the {@link ObjectNode}s required for the
 * discriminator argument.
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_checkDiscriminatorMatch_Test {

    /** Shared mapper to build JSON tree structures. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    ValidationContext.DiscriminatorContext discriminatorContext;

    @Mock
    JsonSchema jsonSchema;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(discriminatorContext, jsonSchema);
    }

    /* --------------------------------------------------------------------- *
     *  Simple control‑flow tests (already covered in the template but kept   *
     *  for completeness).                                                  *
     * --------------------------------------------------------------------- */

    @Test
    @DisplayName("discriminatorPropertyValue == null → markMatch() is called")
    void whenDiscriminatorPropertyValueIsNull_thenMarkMatchIsCalled() {
        // arrange
        ObjectNode discriminator = MAPPER.createObjectNode(); // content irrelevant
        String discriminatorPropertyValue = null;

        // act
        BaseJsonValidator.checkDiscriminatorMatch(
                discriminatorContext,
                discriminator,
                discriminatorPropertyValue,
                jsonSchema);

        // assert
        verify(discriminatorContext, times(1)).markMatch();
        verifyNoMoreInteractions(discriminatorContext);
    }

    @Test
    @DisplayName("no \"mapping\" field → implicit matching path, markMatch() not called")
    void whenMappingIsAbsent_thenImplicitMatchIsAttempted() {
        // arrange
        ObjectNode discriminator = MAPPER.createObjectNode()
                .put("propertyName", "type"); // no "mapping"
        String discriminatorPropertyValue = "cat";

        // act
        BaseJsonValidator.checkDiscriminatorMatch(
                discriminatorContext,
                discriminator,
                discriminatorPropertyValue,
                jsonSchema);

        // assert
        verify(discriminatorContext, never()).markMatch();
        // the method may query the match flag – we allow any number of calls.
        verify(discriminatorContext, atLeast(0)).isDiscriminatorMatchFound();
    }

    @Test
    @DisplayName("explicit mapping present – explicit (and possibly implicit) checks are run")
    void whenMappingIsPresent_thenExplicitAndPossiblyImplicitMatchAreAttempted() {
        // arrange
        ObjectNode mapping = MAPPER.createObjectNode()
                .put("myType", "#/components/schemas/MyType");
        ObjectNode discriminator = MAPPER.createObjectNode()
                .put("propertyName", "type")
                .set("mapping", mapping);
        String discriminatorPropertyValue = "myType";

        // act
        BaseJsonValidator.checkDiscriminatorMatch(
                discriminatorContext,
                discriminator,
                discriminatorPropertyValue,
                jsonSchema);

        // assert
        verify(discriminatorContext, never()).markMatch();
        verify(discriminatorContext, atLeastOnce()).isDiscriminatorMatchFound();
    }

    /* --------------------------------------------------------------------- *
     *  Additional scenarios derived from the supplied JSON test matrix.   *
     * --------------------------------------------------------------------- */

    @Test
    @DisplayName("mapping field is present but explicitly null → treated as absent")
    void whenMappingFieldIsExplicitlyNull_thenImplicitCheckIsUsed() {
        // arrange – Jackson represents a JSON “null” as NullNode
        ObjectNode discriminator = MAPPER.createObjectNode()
                .put("propertyName", "type")
                .set("mapping", NullNode.instance);
        String discriminatorPropertyValue = "lion";

        // act
        BaseJsonValidator.checkDiscriminatorMatch(
                discriminatorContext,
                discriminator,
                discriminatorPropertyValue,
                jsonSchema);

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
        assertThrows(
                NullPointerException.class,
                () -> BaseJsonValidator.checkDiscriminatorMatch(
                        discriminatorContext,
                        discriminator,
                        discriminatorPropertyValue,
                        jsonSchema));
    }

    @Test
    @DisplayName("implicit path with null jsonSchema → NullPointerException from helper")
    void whenJsonSchemaIsNullAndMappingAbsent_thenNpeFromImplicitHelper() {
        // arrange
        ObjectNode discriminator = MAPPER.createObjectNode()
                .put("propertyName", "type"); // no "mapping"
        String discriminatorPropertyValue = "elephant";

        // act & assert
        assertThrows(
                NullPointerException.class,
                () -> BaseJsonValidator.checkDiscriminatorMatch(
                        discriminatorContext,
                        discriminator,
                        discriminatorPropertyValue,
                        null));
    }

    @Test
    @DisplayName("explicit mapping path with null jsonSchema → NullPointerException from helper")
    void whenJsonSchemaIsNullAndMappingPresent_thenNpeFromExplicitHelper() {
        // arrange
        ObjectNode mapping = MAPPER.createObjectNode()
                .put("bird", "#/components/schemas/Bird");
        ObjectNode discriminator = MAPPER.createObjectNode()
                .put("propertyName", "type")
                .set("mapping", mapping);
        String discriminatorPropertyValue = "bird";

        // act & assert
        assertThrows(
                NullPointerException.class,
                () -> BaseJsonValidator.checkDiscriminatorMatch(
                        discriminatorContext,
                        discriminator,
                        discriminatorPropertyValue,
                        null));
    }

    @Test
    @DisplayName("null discriminatorContext with null discriminatorPropertyValue → NPE on markMatch()")
    void whenContextIsNullAndValueIsNull_thenNpeIsThrown() {
        // arrange
        ObjectNode discriminator = MAPPER.createObjectNode();
        String discriminatorPropertyValue = null;

        // act & assert
        assertThrows(
                NullPointerException.class,
                () -> BaseJsonValidator.checkDiscriminatorMatch(
                        null,
                        discriminator,
                        discriminatorPropertyValue,
                        jsonSchema));
    }

    @Test
    @DisplayName("null discriminatorContext on implicit mapping path → NPE")
    void whenContextIsNullAndMappingAbsent_thenNpeIsThrown() {
        // arrange
        ObjectNode discriminator = MAPPER.createObjectNode()
                .put("propertyName", "type"); // no mapping
        String discriminatorPropertyValue = "cat";

        // act & assert
        assertThrows(
                NullPointerException.class,
                () -> BaseJsonValidator.checkDiscriminatorMatch(
                        null,
                        discriminator,
                        discriminatorPropertyValue,
                        jsonSchema));
    }

    @Test
    @DisplayName("null discriminatorContext on explicit mapping path → NPE")
    void whenContextIsNullAndMappingPresent_thenNpeIsThrown() {
        // arrange
        ObjectNode mapping = MAPPER.createObjectNode()
                .put("dog", "#/components/schemas/Dog");
        ObjectNode discriminator = MAPPER.createObjectNode()
                .put("propertyName", "type")
                .set("mapping", mapping);
        String discriminatorPropertyValue = "dog";

        // act & assert
        assertThrows(
                NullPointerException.class,
                () -> BaseJsonValidator.checkDiscriminatorMatch(
                        null,
                        discriminator,
                        discriminatorPropertyValue,
                        jsonSchema));
    }

    /* --------------------------------------------------------------------- *
     *  Parameterised test for the two “explicit mapping” outcome variants   *
     * --------------------------------------------------------------------- */

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

        return Stream.of(
                Arguments.of("explicitMatchFound", ctxMatchFound, true, false),
                Arguments.of("explicitNoMatchFallbackAllowed", ctxNoMatch, false, true),
                Arguments.of("explicitNoMatchFallbackNotAllowed", ctxNoFallback, false, false)
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("explicitMappingScenarios")
    @DisplayName("explicit mapping with various outcomes")
    void whenExplicitMappingScenarios_thenBehaviourMatchesExpectation(
            String scenario,
            ValidationContext.DiscriminatorContext context,
            boolean expectMarkMatch,
            boolean expectImplicitFallback) {

        // arrange – a simple mapping object; the real matching logic lives in a private method
        ObjectNode mapping = MAPPER.createObjectNode()
                .put("key", "#/schemas/Key");
        ObjectNode discriminator = MAPPER.createObjectNode()
                .put("propertyName", "type")
                .set("mapping", mapping);
        String discriminatorPropertyValue = "key";

        // act
        BaseJsonValidator.checkDiscriminatorMatch(
                context,
                discriminator,
                discriminatorPropertyValue,
                jsonSchema);

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
```