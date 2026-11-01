package com.networknt.schema;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.ParameterizedTest;

// @ExtendWith(MockitoExtension.class)
class BaseJsonValidator_checkDiscriminatorMatch_Test {

    /**
     * Simple mutable implementation of {@link DiscriminatorContext} used by the tests.
     */
    private static final class SimpleDiscriminatorContext extends DiscriminatorContext {

        boolean ignoreCalled = false;

        boolean matchFound = false;

        @Override
        public void markIgnore() {
            ignoreCalled = true;
        }

        @Override
        public boolean isDiscriminatorMatchFound() {
            return matchFound;
        }

//         @Override
//         public void setDiscriminatorMatchFound(boolean matchFound) {
//             this.matchFound = matchFound;
//         }
    }

    private static Stream<Arguments> nullDiscriminatorPropertyProvider() {
        return Stream.of(Arguments.of((String) null));
    }

    /**
     * When the discriminator property value is {@code null} the method must tell the
     * {@link DiscriminatorContext} to ignore the current discriminator and return
     * without further processing.
     */
//     @Test
//     void shouldMarkIgnoreWhenDiscriminatorPropertyValueIsNull() {
//         // Arrange
//         SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
//         // Empty discriminator – no "mapping" field.
//         ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
//         JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V7).getSchema(JsonNodeFactory.instance.objectNode());
//         String discriminatorPropertyValue = null;
//         // Act
//         BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, discriminatorPropertyValue, schema);
//         // Assert
//         assertTrue(ctx.ignoreCalled, "markIgnore() should have been called");
//         // No match should have been recorded.
//         assertFalse(ctx.matchFound, "No match should have been recorded");
//     }

    /**
     * When the discriminator mapping node is absent, the method should delegate to the
     * implicit‑mapping logic.  The private helper cannot be called directly, but we can
     * verify that the method does **not** call {@code markIgnore()} and that it proceeds
     * without throwing an exception.
     */
//     @Test
//     void shouldProceedToImplicitMappingWhenNoMappingNodePresent() {
//         // Arrange
//         SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
//         // no "mapping"
//         ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
//         JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V7).getSchema(JsonNodeFactory.instance.objectNode());
//         String discriminatorPropertyValue = "someValue";
//         // Act – must not throw
//         BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, discriminatorPropertyValue, schema);
//         // Assert – markIgnore must not have been called
//         assertFalse(ctx.ignoreCalled, "markIgnore() must not be called");
//     }

//     @Test
//     @DisplayName("Null discriminator node → NullPointerException")
//     void shouldThrowNPEWhenDiscriminatorNodeIsNull() {
//         SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
//         JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V7).getSchema(JsonNodeFactory.instance.objectNode());
//         assertThrows(NullPointerException.class, () -> BaseJsonValidator.checkDiscriminatorMatch(ctx, null, "any", schema));
//     }

    @Test
    @DisplayName("Null JsonSchema argument → NullPointerException (implicit path)")
    void shouldThrowNPEWhenJsonSchemaIsNullAndNoMappingNode() {
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        // no mapping
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
        assertThrows(NullPointerException.class, () -> BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "value", null));
    }

    @Test
    @DisplayName("Null JsonSchema argument → NullPointerException (explicit path)")
    void shouldThrowNPEWhenJsonSchemaIsNullAndMappingPresent() {
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
        // dummy explicit mapping
        discriminator.set("mapping", JsonNodeFactory.instance.objectNode().set("key", JsonNodeFactory.instance.textNode("ref")));
        assertThrows(NullPointerException.class, () -> BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "value", null));
    }

//     @Test
//     @DisplayName("Null DiscriminatorContext argument → NullPointerException")
//     void shouldThrowNPEWhenDiscriminatorContextIsNull() {
//         ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
//         JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V7).getSchema(JsonNodeFactory.instance.objectNode());
//         assertThrows(NullPointerException.class, () -> BaseJsonValidator.checkDiscriminatorMatch(null, discriminator, "value", schema));
//     }

//     @Test
//     @DisplayName("Explicit mapping present and matches discriminator value")
//     void shouldMarkMatchFoundWhenExplicitMappingMatches() {
//         // Arrange
//         SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
//         // explicit “no match yet”
//         ctx.matchFound = false;
//         ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
//         ObjectNode mapping = JsonNodeFactory.instance.objectNode();
//         mapping.set("dog", JsonNodeFactory.instance.textNode("#/components/schemas/Dog"));
//         discriminator.set("mapping", mapping);
//         JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V7).getSchema(JsonNodeFactory.instance.objectNode());
//         String discriminatorPropertyValue = "dog";
//         // Act
//         BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, discriminatorPropertyValue, schema);
//         // Assert – a match must have been recorded and markIgnore must not be called.
//         assertTrue(ctx.matchFound, "Discriminator match should be recorded");
//         assertFalse(ctx.ignoreCalled, "markIgnore() must not be called");
//     }

//     @Test
//     @DisplayName("Explicit mapping not found, implicit fallback allowed")
//     void shouldFallBackToImplicitWhenExplicitNotFoundAndFallbackAllowed() {
//         // Arrange
//         SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
//         ctx.matchFound = false;
//         ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
//         ObjectNode mapping = JsonNodeFactory.instance.objectNode();
//         mapping.set("cat", JsonNodeFactory.instance.textNode("#/components/schemas/Cat"));
//         discriminator.set("mapping", mapping);
//         JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V7).getSchema(JsonNodeFactory.instance.objectNode());
//         String discriminatorPropertyValue = "bird";
//         // Act – must not throw
//         assertDoesNotThrow(() -> BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, discriminatorPropertyValue, schema));
//         // Assert – no match and no ignore.
//         assertFalse(ctx.matchFound, "No discriminator match should be recorded");
//         assertFalse(ctx.ignoreCalled, "markIgnore() must not be called");
//     }

//     @Test
//     @DisplayName("Explicit mapping not found, implicit fallback NOT allowed")
//     void shouldNotFallBackWhenExplicitNotFoundAndFallbackDisallowed() {
//         // Arrange
//         SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
//         ctx.matchFound = false;
//         ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
//         ObjectNode mapping = JsonNodeFactory.instance.objectNode();
//         mapping.set("cat", JsonNodeFactory.instance.textNode("#/components/schemas/Cat"));
//         discriminator.set("mapping", mapping);
//         // The real library decides fallback based on the schema; using a minimal
//         // schema here results in the default “fallback allowed” behaviour.
//         // The test only asserts that nothing crashes and that no match is recorded.
//         JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V7).getSchema(JsonNodeFactory.instance.objectNode());
//         String discriminatorPropertyValue = "fish";
//         // Act
//         assertDoesNotThrow(() -> BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, discriminatorPropertyValue, schema));
//         // Assert – still no match and no ignore.
//         assertFalse(ctx.matchFound, "No discriminator match should be recorded");
//         assertFalse(ctx.ignoreCalled, "markIgnore() must not be called");
//     }

//     @Test
//     @DisplayName("Mapping field exists but is a non‑object node (TextNode)")
//     void shouldCallExplicitHelperEvenWhenMappingIsNotAnObject() {
//         // Arrange
//         SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
//         ctx.matchFound = false;
//         ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
//         discriminator.set("mapping", JsonNodeFactory.instance.textNode("notAnObject"));
//         JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V7).getSchema(JsonNodeFactory.instance.objectNode());
//         String discriminatorPropertyValue = "mouse";
//         // Act – must not throw.
//         assertDoesNotThrow(() -> BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, discriminatorPropertyValue, schema));
//         // Assert – no ignore and no match.
//         assertFalse(ctx.matchFound, "No discriminator match should be recorded");
//         assertFalse(ctx.ignoreCalled, "markIgnore() must not be called");
//     }

//     @Test
//     @DisplayName("Context already reports a match – implicit mapping still runs")
//     void shouldRunImplicitMappingWhenContextAlreadyHasMatch() {
//         // Arrange
//         SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
//         // simulate existing match
//         ctx.matchFound = true;
//         // no mapping
//         ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
//         JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V7).getSchema(JsonNodeFactory.instance.objectNode());
//         String discriminatorPropertyValue = "cat";
//         // Act – must not throw.
//         assertDoesNotThrow(() -> BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, discriminatorPropertyValue, schema));
//         // Assert – match flag stays true and no ignore.
//         assertTrue(ctx.matchFound, "Existing match flag must stay true");
//         assertFalse(ctx.ignoreCalled, "markIgnore() must not be called");
//     }

//     @Test
//     @DisplayName("Empty discriminator value matches explicit mapping key")
//     void shouldMatchExplicitMappingWhenKeyIsEmptyString() {
//         // Arrange
//         SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
//         ctx.matchFound = false;
//         ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
//         ObjectNode mapping = JsonNodeFactory.instance.objectNode();
//         mapping.set("", JsonNodeFactory.instance.textNode("#/components/schemas/Empty"));
//         discriminator.set("mapping", mapping);
//         JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V7).getSchema(JsonNodeFactory.instance.objectNode());
//         // empty string
//         String discriminatorPropertyValue = "";
//         // Act
//         BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, discriminatorPropertyValue, schema);
//         // Assert – match recorded, no ignore.
//         assertTrue(ctx.matchFound, "Discriminator match should be recorded for empty key");
//         assertFalse(ctx.ignoreCalled, "markIgnore() must not be called");
//     }

//     @ParameterizedTest(name = "[{index}] discriminatorPropertyValue={0}")
//     @MethodSource("nullDiscriminatorPropertyProvider")
//     @DisplayName("Parameterised test – discriminator property value is null")
//     void parameterisedNullDiscriminatorPropertyValue(String discriminatorPropertyValue) {
//         SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
//         // no mapping
//         ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
//         JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V7).getSchema(JsonNodeFactory.instance.objectNode());
//         BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, discriminatorPropertyValue, schema);
//         assertTrue(ctx.ignoreCalled, "markIgnore() should be called for null property value");
//         assertFalse(ctx.matchFound, "No match should be recorded");
//     }
}