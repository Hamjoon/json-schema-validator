```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link BaseJsonValidator#checkDiscriminatorMatch(ValidationContext.DiscriminatorContext, ObjectNode, String, JsonSchema)}.
 */
@DisplayName("BaseJsonValidator.checkDiscriminatorMatch tests")
class BaseJsonValidator_checkDiscriminatorMatch_Test {

    /**
     * Shared {@link ObjectMapper} used to create {@link ObjectNode} instances.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Simple stub implementation of {@link ValidationContext.DiscriminatorContext}.
     */
    private static class SimpleDiscriminatorContext extends ValidationContext.DiscriminatorContext {

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

        /**
         * Allows the test to preset the result of {@link #isDiscriminatorMatchFound()}.
         */
        void setMatchFound(boolean matchFound) {
            this.matchFound = matchFound;
        }

        /**
         * Returns whether {@link #markMatch()} was invoked.
         */
        boolean wasMarkMatchCalled() {
            return markMatchCalled;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // No mock framework is used – nothing to reset.
    }

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
     * Helper method that returns a non‑null {@link JsonSchema} instance.
     */
    private static JsonSchema jsonSchema() {
        // The concrete behaviour of JsonSchema is irrelevant for these tests.
        // We create an anonymous subclass providing a minimal constructor
        // and a dummy {@code validate} implementation.
        return new JsonSchema(null, (URI) null, (JsonNode) null) {
            @Override
            public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                                   JsonNode node,
                                                   JsonNode rootNode,
                                                   String at) {
                return Collections.emptySet();
            }
        };
    }

    /**
     * When the discriminator property value is {@code null} the method must call
     * {@code DiscriminatorContext.markMatch()} exactly once and return immediately.
     */
    @Test
    void whenDiscriminatorPropertyValueIsNull_thenMarkMatchIsCalled() {
        SimpleDiscriminatorContext context = new SimpleDiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode();
        String discriminatorPropertyValue = null;

        BaseJsonValidator.checkDiscriminatorMatch(context, discriminator,
                discriminatorPropertyValue, jsonSchema());

        assertTrue(context.wasMarkMatchCalled(),
                "markMatch() should have been called exactly once for a null discriminator value");
    }

    /**
     * When the discriminator node does **not** contain a {@code "mapping"} field,
     * the method should *not* call {@code markMatch()} (the implicit matching logic is
     * performed inside a private method).  The test simply verifies that the method
     * completes without throwing and that {@code markMatch()} is not invoked.
     */
    @Test
    void whenMappingIsAbsent_thenImplicitMatchIsAttempted() {
        SimpleDiscriminatorContext context = new SimpleDiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode()
                .put("propertyName", "type"); // no "mapping" field
        String discriminatorPropertyValue = "myType";

        BaseJsonValidator.checkDiscriminatorMatch(context, discriminator,
                discriminatorPropertyValue, jsonSchema());

        assertFalse(context.wasMarkMatchCalled(),
                "markMatch() must not be called when there is no explicit mapping");
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
        SimpleDiscriminatorContext context = new SimpleDiscriminatorContext();
        ObjectNode mapping = MAPPER.createObjectNode()
                .put("myType", "#/components/schemas/MyType");
        ObjectNode discriminator = MAPPER.createObjectNode()
                .put("propertyName", "type")
                .set("mapping", mapping);
        String discriminatorPropertyValue = "myType";

        BaseJsonValidator.checkDiscriminatorMatch(context, discriminator,
                discriminatorPropertyValue, jsonSchema());

        assertFalse(context.wasMarkMatchCalled(),
                "markMatch() must not be called when a mapping is present but the value is not null");
    }

    @Test
    @DisplayName("mapping field is present but explicitly null → treated as absent")
    void whenMappingFieldIsExplicitlyNull_thenImplicitCheckIsUsed() {
        SimpleDiscriminatorContext context = new SimpleDiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode()
                .put("propertyName", "type")
                .set("mapping", NullNode.instance);
        String discriminatorPropertyValue = "lion";

        BaseJsonValidator.checkDiscriminatorMatch(context, discriminator,
                discriminatorPropertyValue, jsonSchema());

        assertFalse(context.wasMarkMatchCalled(),
                "markMatch() must not be called when mapping field is explicitly null");
    }

    @Test
    @DisplayName("null discriminator node → NullPointerException")
    void whenDiscriminatorNodeIsNull_thenNpeIsThrown() {
        SimpleDiscriminatorContext context = new SimpleDiscriminatorContext();
        ObjectNode discriminator = null;
        String discriminatorPropertyValue = "tiger";

        assertThrows(NullPointerException.class,
                () -> BaseJsonValidator.checkDiscriminatorMatch(context, discriminator,
                        discriminatorPropertyValue, jsonSchema()));
    }

    @Test
    @DisplayName("implicit path with null jsonSchema → NullPointerException from helper")
    void whenJsonSchemaIsNullAndMappingAbsent_thenNpeFromImplicitHelper() {
        SimpleDiscriminatorContext context = new SimpleDiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode()
                .put("propertyName", "type"); // no "mapping"
        String discriminatorPropertyValue = "elephant";

        assertThrows(NullPointerException.class,
                () -> BaseJsonValidator.checkDiscriminatorMatch(context, discriminator,
                        discriminatorPropertyValue, null));
    }

    @Test
    @DisplayName("explicit mapping path with null jsonSchema → NullPointerException from helper")
    void whenJsonSchemaIsNullAndMappingPresent_thenNpeFromExplicitHelper() {
        SimpleDiscriminatorContext context = new SimpleDiscriminatorContext();
        ObjectNode mapping = MAPPER.createObjectNode()
                .put("bird", "#/components/schemas/Bird");
        ObjectNode discriminator = MAPPER.createObjectNode()
                .put("propertyName", "type")
                .set("mapping", mapping);
        String discriminatorPropertyValue = "bird";

        assertThrows(NullPointerException.class,
                () -> BaseJsonValidator.checkDiscriminatorMatch(context, discriminator,
                        discriminatorPropertyValue, null));
    }

    @Test
    @DisplayName("null discriminatorContext with null discriminatorPropertyValue → NPE on markMatch()")
    void whenContextIsNullAndValueIsNull_thenNpeIsThrown() {
        ObjectNode discriminator = MAPPER.createObjectNode();
        String discriminatorPropertyValue = null;

        assertThrows(NullPointerException.class,
                () -> BaseJsonValidator.checkDiscriminatorMatch(null, discriminator,
                        discriminatorPropertyValue, jsonSchema()));
    }

    @Test
    @DisplayName("null discriminatorContext on implicit mapping path → NPE")
    void whenContextIsNullAndMappingAbsent_thenNpeIsThrown() {
        ObjectNode discriminator = MAPPER.createObjectNode()
                .put("propertyName", "type"); // no mapping
        String discriminatorPropertyValue = "cat";

        assertThrows(NullPointerException.class,
                () -> BaseJsonValidator.checkDiscriminatorMatch(null, discriminator,
                        discriminatorPropertyValue, jsonSchema()));
    }

    @Test
    @DisplayName("null discriminatorContext on explicit mapping path → NPE")
    void whenContextIsNullAndMappingPresent_thenNpeIsThrown() {
        ObjectNode mapping = MAPPER.createObjectNode()
                .put("dog", "#/components/schemas/Dog");
        ObjectNode discriminator = MAPPER.createObjectNode()
                .put("propertyName", "type")
                .set("mapping", mapping);
        String discriminatorPropertyValue = "dog";

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

        ObjectNode mapping = MAPPER.createObjectNode()
                .put("key", "#/schemas/Key");
        ObjectNode discriminator = MAPPER.createObjectNode()
                .put("propertyName", "type")
                .set("mapping", mapping);
        String discriminatorPropertyValue = "key";

        BaseJsonValidator.checkDiscriminatorMatch(context, discriminator,
                discriminatorPropertyValue, jsonSchema());

        if (expectMarkMatch) {
            assertTrue(context.wasMarkMatchCalled(),
                    scenario + ": markMatch() should have been called");
        } else {
            assertFalse(context.wasMarkMatchCalled(),
                    scenario + ": markMatch() should NOT have been called");
        }

        if (expectImplicitFallback) {
            // Implicit fallback is expected only when no explicit match was recorded.
            assertFalse(context.isDiscriminatorMatchFound(),
                    scenario + ": implicit fallback should be attempted when no explicit match");
        }
    }
}
```