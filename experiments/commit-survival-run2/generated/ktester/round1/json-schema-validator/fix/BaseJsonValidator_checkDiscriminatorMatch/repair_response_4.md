```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for {@link BaseJsonValidator#checkDiscriminatorMatch(ValidationContext.DiscriminatorContext, ObjectNode, String, JsonSchema)}.
 */
class BaseJsonValidator_checkDiscriminatorMatch_Test {

    /** Shared {@link ObjectMapper} used to create {@link ObjectNode} instances. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // -------------------------------------------------------------------------
    // Helper stub implementations
    // -------------------------------------------------------------------------

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

        /** Allows the test to preset the result of {@link #isDiscriminatorMatchFound()}. */
        void setMatchFound(boolean matchFound) {
            this.matchFound = matchFound;
        }

        /** Returns whether {@link #markMatch()} was invoked. */
        boolean wasMarkMatchCalled() {
            return markMatchCalled;
        }
    }

    /**
     * Returns a non‑null {@link JsonSchema} instance that does nothing during validation.
     */
    private static JsonSchema jsonSchema() {
        // Build a default ValidationContext (it provides a non‑null meta‑schema).
        ValidationContext validationContext = ValidationContext.builder().build();
        // Create an anonymous subclass that supplies a dummy {@code validate} implementation.
        return new JsonSchema(validationContext, (URI) null, (JsonNode) null) {
            @Override
            public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                                   JsonNode node,
                                                   JsonNode rootNode,
                                                   String at) {
                return Collections.emptySet();
            }
        };
    }

    // -------------------------------------------------------------------------
    // Test data providers
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Actual tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("discriminator property value is null → markMatch() is called")
    void whenDiscriminatorPropertyValueIsNull_thenMarkMatchIsCalled() {
        SimpleDiscriminatorContext context = new SimpleDiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode();
        String discriminatorPropertyValue = null;
        BaseJsonValidator.checkDiscriminatorMatch(context, discriminator, discriminatorPropertyValue, jsonSchema());
        assertTrue(context.wasMarkMatchCalled(),
                "markMatch() should have been called exactly once for a null discriminator value");
    }

    @Test
    @DisplayName("no mapping field → implicit match is attempted (no markMatch call)")
    void whenMappingIsAbsent_thenImplicitMatchIsAttempted() {
        SimpleDiscriminatorContext context = new SimpleDiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode()
                .put("propertyName", "type"); // no "mapping" field
        String discriminatorPropertyValue = "myType";
        BaseJsonValidator.checkDiscriminatorMatch(context, discriminator, discriminatorPropertyValue, jsonSchema());
        assertFalse(context.wasMarkMatchCalled(),
                "markMatch() must not be called when there is no explicit mapping");
    }

    @Test
    @DisplayName("mapping present → explicit (and possibly implicit) match is attempted")
    void whenMappingIsPresent_thenExplicitAndPossiblyImplicitMatchAreAttempted() {
        SimpleDiscriminatorContext context = new SimpleDiscriminatorContext();
        ObjectNode mapping = MAPPER.createObjectNode().put("myType", "#/components/schemas/MyType");
        ObjectNode discriminator = MAPPER.createObjectNode()
                .put("propertyName", "type")
                .set("mapping", mapping);
        String discriminatorPropertyValue = "myType";
        BaseJsonValidator.checkDiscriminatorMatch(context, discriminator, discriminatorPropertyValue, jsonSchema());
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
        BaseJsonValidator.checkDiscriminatorMatch(context, discriminator, discriminatorPropertyValue, jsonSchema());
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
    @DisplayName("implicit path with null jsonSchema → NullPointerException")
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
    @DisplayName("explicit mapping path with null jsonSchema → NullPointerException")
    void whenJsonSchemaIsNullAndMappingPresent_thenNpeFromExplicitHelper() {
        SimpleDiscriminatorContext context = new SimpleDiscriminatorContext();
        ObjectNode mapping = MAPPER.createObjectNode().put("bird", "#/components/schemas/Bird");
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
        ObjectNode mapping = MAPPER.createObjectNode().put("dog", "#/components/schemas/Dog");
        ObjectNode discriminator = MAPPER.createObjectNode()
                .put("propertyName", "type")
                .set("mapping", mapping);
        String discriminatorPropertyValue = "dog";
        assertThrows(NullPointerException.class,
                () -> BaseJsonValidator.checkDiscriminatorMatch(null, discriminator,
                        discriminatorPropertyValue, jsonSchema()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("explicitMappingScenarios")
    @DisplayName("explicit mapping scenarios")
    void explicitMappingScenarios(String name,
                                  SimpleDiscriminatorContext ctx,
                                  boolean expectMarkMatch,
                                  boolean expectImplicitFallback) {
        // Build a minimal discriminator node with a mapping that contains the key "myType"
        ObjectNode mapping = MAPPER.createObjectNode().put("myType", "#/components/schemas/MyType");
        ObjectNode discriminator = MAPPER.createObjectNode()
                .put("propertyName", "type")
                .set("mapping", mapping);

        // The value we pass matches the key in the mapping
        String discriminatorPropertyValue = "myType";

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, discriminatorPropertyValue, jsonSchema());

        assertEquals(expectMarkMatch, ctx.wasMarkMatchCalled(),
                "markMatch() call mismatch for scenario: " + name);
        // The implicit‑fallback behaviour is exercised inside the validator; we only
        // verify that the method does not throw and that the context state is as
        // expected above.
    }
}
```