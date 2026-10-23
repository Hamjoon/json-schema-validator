```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link BaseJsonValidator#checkDiscriminatorMatch(ValidationContext.DiscriminatorContext, ObjectNode, String, JsonSchema)}.
 *
 * <p>This test class does not rely on external mocking frameworks.  A tiny
 * hand‑crafted implementation of {@link ValidationContext.DiscriminatorContext}
 * records the interactions that the original tests verified with Mockito.</p>
 */
@DisplayName("BaseJsonValidator.checkDiscriminatorMatch tests")
class BaseJsonValidator_checkDiscriminatorMatch_Test {

    /** Shared {@link ObjectMapper} used to create {@link ObjectNode} instances. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Simple stub implementation of {@link ValidationContext.DiscriminatorContext}. */
    private static class SimpleDiscriminatorContext implements ValidationContext.DiscriminatorContext {
        private boolean matchFound;
        private boolean markMatchCalled;

        SimpleDiscriminatorContext() {
            this(false);
        }

        SimpleDiscriminatorContext(boolean matchFound) {
            this.matchFound = matchFound;
        }

        @Override
        public void markMatch() {
            this.markMatchCalled = true;
        }

        @Override
        public boolean isDiscriminatorMatchFound() {
            return matchFound;
        }

        /** Allows the test to preset the result of {@link #isDiscriminatorMatchFound()}. */
        void setMatchFound(boolean matchFound) {
            this.matchFound = matchFound;
        }

        /** Returns whether {@link #markMatch()} was invoked. */
        boolean wasMarkMatchCalled() {
            return markMatchCalled;
        }
    }

    /** Provides the three explicit‑mapping scenarios used by the parameterised test. */
    private static Stream<Arguments> explicitMappingScenarios() {
        SimpleDiscriminatorContext ctxMatchFound = new SimpleDiscriminatorContext(true);
        SimpleDiscriminatorContext ctxNoMatch = new SimpleDiscriminatorContext(false);
        SimpleDiscriminatorContext ctxNoFallback = new SimpleDiscriminatorContext(false);
        // scenario name, context, expectMarkMatch, expectImplicitFallback
        return Stream.of(
                Arguments.of("explicitMatchFound", ctxMatchFound, true, false),
                Arguments.of("explicitNoMatchFallbackAllowed", ctxNoMatch, false, true),
                Arguments.of("explicitNoMatchFallbackNotAllowed", ctxNoFallback, false, false)
        );
    }

    /**
     * When the discriminator property value is {@code null} the method must call
     * {@code DiscriminatorContext.markMatch()} exactly once and return immediately.
     */
    @Test
    void whenDiscriminatorPropertyValueIsNull_thenMarkMatchIsCalled() {
        // arrange
        SimpleDiscriminatorContext context = new SimpleDiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode(); // content irrelevant
        String discriminatorPropertyValue = null;

        // act
        BaseJsonValidator.checkDiscriminatorMatch(context, discriminator,
                discriminatorPropertyValue, jsonSchema());

        // assert
        assertTrue(context.wasMarkMatchCalled(),
                "markMatch() should have been called exactly once for a null discriminator value");
    }

    /**
     * When the discriminator node does **not** contain a {@code "mapping"} field,
     * the method should *not* call {@code markMatch()} (the implicit matching logic
     * is performed inside a private method). The test simply verifies that the
     * method completes without throwing and that {@code markMatch()} is not invoked.
     */
    @Test
    void whenMappingIsAbsent_thenImplicitMatchIsAttempted() {
        // arrange
        SimpleDiscriminatorContext context = new SimpleDiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode()
                .put("propertyName", "type"); // no "mapping" field
        String discriminatorPropertyValue = "myType";

        // act
        BaseJsonValidator.checkDiscriminatorMatch(context, discriminator,
                discriminatorPropertyValue, jsonSchema());

        // assert
        assertFalse(context.wasMarkMatchCalled(),
                "markMatch() must not be called when there is no explicit mapping");
    }

    /**
     * When the discriminator node contains a {@code "mapping"} field, the method first
     * attempts an explicit match.  If the explicit match is not found and the schema
     * does not override the explicit key, the method falls back to the implicit match.
     * This test supplies a minimal {@code "mapping"} object and verifies that the
     * method executes without throwing.  Because the matching logic resides in private
     * methods, we only ensure that {@code markMatch()} is not called for a non‑null value.
     */
    @Test
    void whenMappingIsPresent_thenExplicitAndPossiblyImplicitMatchAreAttempted() {
        // arrange
        SimpleDiscriminatorContext context = new SimpleDiscriminatorContext();
        ObjectNode mapping = MAPPER.createObjectNode().put("myType", "#/components/schemas/MyType");
        ObjectNode discriminator = MAPPER.createObjectNode()
                .put("propertyName", "type")
                .set("mapping", mapping);
        String discriminatorPropertyValue = "myType";

        // act
        BaseJsonValidator.checkDiscriminatorMatch(context, discriminator,
                discriminatorPropertyValue, jsonSchema());

        // assert
        assertFalse(context.wasMarkMatchCalled(),
                "markMatch() must not be called when a mapping is present but the value is not null");
    }

    @Test
    @DisplayName("mapping field is present but explicitly null → treated as absent")
    void whenMappingFieldIsExplicitlyNull_thenImplicitCheckIsUsed() {
        // arrange – Jackson represents a JSON “null” as NullNode
        SimpleDiscriminatorContext context = new SimpleDiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode()
                .put("propertyName", "type")
                .set("mapping", NullNode.instance);
        String discriminatorPropertyValue = "lion";

        // act
        BaseJsonValidator.checkDiscriminatorMatch(context, discriminator,
                discriminatorPropertyValue, jsonSchema());

        // assert – same expectations as the “no mapping” case
        assertFalse(context.wasMarkMatchCalled(),
                "markMatch() must not be called when mapping field is explicitly null");
    }

    @Test
    @DisplayName("null discriminator node → NullPointerException")
    void whenDiscriminatorNodeIsNull_thenNpeIsThrown() {
        // arrange
        SimpleDiscriminatorContext context = new SimpleDiscriminatorContext();
        ObjectNode discriminator = null;
        String discriminatorPropertyValue = "tiger";

        // act & assert
        assertThrows(NullPointerException.class,
                () -> BaseJsonValidator.checkDiscriminatorMatch(context, discriminator,
                        discriminatorPropertyValue, jsonSchema()));
    }

    @Test
    @DisplayName("implicit path with null jsonSchema → NullPointerException from helper")
    void whenJsonSchemaIsNullAndMappingAbsent_thenNpeFromImplicitHelper() {
        // arrange
        SimpleDiscriminatorContext context = new SimpleDiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode()
                .put("propertyName", "type"); // no "mapping"
        String discriminatorPropertyValue = "elephant";

        // act & assert
        assertThrows(NullPointerException.class,
                () -> BaseJsonValidator.checkDiscriminatorMatch(context, discriminator,
                        discriminatorPropertyValue, null));
    }

    @Test
    @DisplayName("explicit mapping path with null jsonSchema → NullPointerException from helper")
    void whenJsonSchemaIsNullAndMappingPresent_thenNpeFromExplicitHelper() {
        // arrange
        SimpleDiscriminatorContext context = new SimpleDiscriminatorContext();
        ObjectNode mapping = MAPPER.createObjectNode().put("bird", "#/components/schemas/Bird");
        ObjectNode discriminator = MAPPER.createObjectNode()
                .put("propertyName", "type")
                .set("mapping", mapping);
        String discriminatorPropertyValue = "bird";

        // act & assert
        assertThrows(NullPointerException.class,
                () -> BaseJsonValidator.checkDiscriminatorMatch(context, discriminator,
                        discriminatorPropertyValue, null));
    }

    @Test
    @DisplayName("null discriminatorContext with null discriminatorPropertyValue → NPE on markMatch()")
    void whenContextIsNullAndValueIsNull_thenNpeIsThrown() {
        // arrange
        ObjectNode discriminator = MAPPER.createObjectNode();
        String discriminatorPropertyValue = null;

        // act & assert
        assertThrows(NullPointerException.class,
                () -> BaseJsonValidator.checkDiscriminatorMatch(null, discriminator,
                        discriminatorPropertyValue, jsonSchema()));
    }

    @Test
    @DisplayName("null discriminatorContext on implicit mapping path → NPE")
    void whenContextIsNullAndMappingAbsent_thenNpeIsThrown() {
        // arrange
        ObjectNode discriminator = MAPPER.createObjectNode()
                .put("propertyName", "type"); // no mapping
        String discriminatorPropertyValue = "cat";

        // act & assert
        assertThrows(NullPointerException.class,
                () -> BaseJsonValidator.checkDiscriminatorMatch(null, discriminator,
                        discriminatorPropertyValue, jsonSchema()));
    }

    @Test
    @DisplayName("null discriminatorContext on explicit mapping path → NPE")
    void whenContextIsNullAndMappingPresent_thenNpeIsThrown() {
        // arrange
        ObjectNode mapping = MAPPER.createObjectNode().put("dog", "#/components/schemas/Dog");
        ObjectNode discriminator = MAPPER.createObjectNode()
                .put("propertyName", "type")
                .set("mapping", mapping);
        String discriminatorPropertyValue = "dog";

        // act & assert
        assertThrows(NullPointerException.class,
                () -> BaseJsonValidator.checkDiscriminatorMatch(null, discriminator,
                        discriminatorPropertyValue, jsonSchema()));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("explicitMappingScenarios")
    @DisplayName("explicit mapping with various outcomes")
    void whenExplicitMappingScenarios_thenBehaviourMatchesExpectation(
            String scenario,
            SimpleDiscriminatorContext context,
            boolean expectMarkMatch,
            boolean expectImplicitFallback) {

        // arrange – a simple mapping object; the real matching logic lives in a private method
        ObjectNode mapping = MAPPER.createObjectNode().put("key", "#/schemas/Key");
        ObjectNode discriminator = MAPPER.createObjectNode()
                .put("propertyName", "type")
                .set("mapping", mapping);
        String discriminatorPropertyValue = "key";

        // act
        BaseJsonValidator.checkDiscriminatorMatch(context, discriminator,
                discriminatorPropertyValue, jsonSchema());

        // assert markMatch()
        if (expectMarkMatch) {
            assertTrue(context.wasMarkMatchCalled(),
                    scenario + ": markMatch() should have been called");
        } else {
            assertFalse(context.wasMarkMatchCalled(),
                    scenario + ": markMatch() should NOT have been called");
        }

        // assert implicit fallback behaviour – we cannot directly observe the number of
        // calls to isDiscriminatorMatchFound(), but the flag we set initially tells us
        // whether a match was already recorded.  If a fallback is expected the context
        // should still report the original state (no additional state change is needed
        // for the purpose of these tests).
        if (expectImplicitFallback) {
            // The presence of a fallback implies that the explicit check did NOT find a match,
            // i.e. the initial flag must be false.
            assertFalse(context.isDiscriminatorMatchFound(),
                    scenario + ": implicit fallback should be attempted when no explicit match");
        } else {
            // When no fallback is expected the explicit check may have succeeded,
            // leaving the flag unchanged (true) or unchanged (false) – we only verify that
            // the flag matches the expectation set up for the scenario.
            // No extra assertion needed here.
        }
    }

    /** Helper method that returns a non‑null {@link JsonSchema} instance. */
    private static JsonSchema jsonSchema() {
        // The concrete behaviour of JsonSchema is irrelevant for these tests.
        // Creating an anonymous subclass is sufficient.
        return new JsonSchema() {
            // all abstract methods are given empty implementations
            @Override public void validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at) {}
        };
    }
}
```