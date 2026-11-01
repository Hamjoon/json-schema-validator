
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_checkDiscriminatorMatch_Test {

    /**
     * Simple mutable implementation of {@link DiscriminatorContext} used by the tests.
     */
    private static final class SimpleDiscriminatorContext implements DiscriminatorContext {

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

        @Override
        public void setDiscriminatorMatchFound(boolean matchFound) {
            this.matchFound = matchFound;
        }
    }

    private static Stream<Arguments> nullDiscriminatorPropertyProvider() {
        return Stream.of(Arguments.of((String) null));
    }

    /**
     * When the discriminator property value is {@code null} the method must tell the
     * {@link DiscriminatorContext} to ignore the current discriminator and return
     * without further processing.
     */
    @Test
    void shouldMarkIgnoreWhenDiscriminatorPropertyValueIsNull() {
        // Arrange
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        // Empty discriminator – no "mapping" field.
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
        JsonSchema schema = JsonSchemaFactory.getInstance().getSchema(JsonNodeFactory.instance.objectNode());
        String discriminatorPropertyValue = null;
        // Act
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, discriminatorPropertyValue, schema);
        // Assert
        assertTrue(ctx.ignoreCalled, "markIgnore() should have been called");
        // No match should have been recorded.
        assertFalse(ctx.matchFound, "No match should have been recorded");
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
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        // no "mapping"
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
        JsonSchema schema = JsonSchemaFactory.getInstance().getSchema(JsonNodeFactory.instance.objectNode());
        String discriminatorPropertyValue = "someValue";
        // Act – must not throw
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, discriminatorPropertyValue, schema);
        // Assert – markIgnore must not have been called
        assertFalse(ctx.ignoreCalled, "markIgnore() must not be called");
    }

    @Test
    @DisplayName("Null discriminator node → NullPointerException")
    void shouldThrowNPEWhenDiscriminatorNodeIsNull() {
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        JsonSchema schema = JsonSchemaFactory.getInstance().getSchema(JsonNodeFactory.instance.objectNode());
        assertThrows(NullPointerException.class, () -> BaseJsonValidator.checkDiscriminatorMatch(ctx, null, "any", schema));
    }

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

    @Test
    @DisplayName("Null DiscriminatorContext argument → NullPointerException")
    void shouldThrowNPEWhenDiscriminatorContextIsNull() {
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
        JsonSchema schema = JsonSchemaFactory.getInstance().getSchema(JsonNodeFactory.instance.objectNode());
        assertThrows(NullPointerException.class, () -> BaseJsonValidator.checkDiscriminatorMatch(null, discriminator, "value", schema));
    }

    @Test
    @DisplayName("Explicit mapping present and matches discriminator value")
    void shouldMarkMatchFoundWhenExplicitMappingMatches() {
        // Arrange
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        // explicit “no match yet”
        ctx.matchFound = false;
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
        ObjectNode mapping = JsonNodeFactory.instance.objectNode();
        mapping.set("dog", JsonNodeFactory.instance.textNode("#/components/schemas/Dog"));
        discriminator.set("mapping", mapping);
        JsonSchema schema = JsonSchemaFactory.getInstance().getSchema(JsonNodeFactory.instance.objectNode());
        String discriminatorPropertyValue = "dog";
        // Act
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, discriminatorPropertyValue, schema);
        // Assert – a match must have been recorded and markIgnore must not be called.
        assertTrue(ctx.matchFound, "Discriminator match should be recorded");
        assertFalse(ctx.ignoreCalled, "markIgnore() must not be called");
    }

    @Test
    @DisplayName("Explicit mapping not found, implicit fallback allowed")
    void shouldFallBackToImplicitWhenExplicitNotFoundAndFallbackAllowed() {
        // Arrange
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ctx.matchFound = false;
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
        ObjectNode mapping = JsonNodeFactory.instance.objectNode();
        mapping.set("cat", JsonNodeFactory.instance.textNode("#/components/schemas/Cat"));
        discriminator.set("mapping", mapping);
        JsonSchema schema = JsonSchemaFactory.getInstance().getSchema(JsonNodeFactory.instance.objectNode());
        String discriminatorPropertyValue = "bird";
        // Act – must not throw
        assertDoesNotThrow(() -> BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, discriminatorPropertyValue, schema));
        // Assert – no match and no ignore.
        assertFalse(ctx.matchFound, "No discriminator match should be recorded");
        assertFalse(ctx.ignoreCalled, "markIgnore() must not be called");
    }

    @Test
    @DisplayName("Explicit mapping not found, implicit fallback NOT allowed")
    void shouldNotFallBackWhenExplicitNotFoundAndFallbackDisallowed() {
        // Arrange
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ctx.matchFound = false;
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
        ObjectNode mapping = JsonNodeFactory.instance.objectNode();
        mapping.set("cat", JsonNodeFactory.instance.textNode("#/components/schemas/Cat"));
        discriminator.set("mapping", mapping);
        // The real library decides fallback based on the schema; using a minimal
        // schema here results in the default “fallback allowed” behaviour.
        // The test only asserts that nothing crashes and that no match is recorded.
        JsonSchema schema = JsonSchemaFactory.getInstance().getSchema(JsonNodeFactory.instance.objectNode());
        String discriminatorPropertyValue = "fish";
        // Act
        assertDoesNotThrow(() -> BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, discriminatorPropertyValue, schema));
        // Assert – still no match and no ignore.
        assertFalse(ctx.matchFound, "No discriminator match should be recorded");
        assertFalse(ctx.ignoreCalled, "markIgnore() must not be called");
    }

    @Test
    @DisplayName("Mapping field exists but is a non‑object node (TextNode)")
    void shouldCallExplicitHelperEvenWhenMappingIsNotAnObject() {
        // Arrange
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ctx.matchFound = false;
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
        discriminator.set("mapping", JsonNodeFactory.instance.textNode("notAnObject"));
        JsonSchema schema = JsonSchemaFactory.getInstance().getSchema(JsonNodeFactory.instance.objectNode());
        String discriminatorPropertyValue = "mouse";
        // Act – must not throw.
        assertDoesNotThrow(() -> BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, discriminatorPropertyValue, schema));
        // Assert – no ignore and no match.
        assertFalse(ctx.matchFound, "No discriminator match should be recorded");
        assertFalse(ctx.ignoreCalled, "markIgnore() must not be called");
    }

    @Test
    @DisplayName("Context already reports a match – implicit mapping still runs")
    void shouldRunImplicitMappingWhenContextAlreadyHasMatch() {
        // Arrange
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        // simulate existing match
        ctx.matchFound = true;
        // no mapping
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
        JsonSchema schema = JsonSchemaFactory.getInstance().getSchema(JsonNodeFactory.instance.objectNode());
        String discriminatorPropertyValue = "cat";
        // Act – must not throw.
        assertDoesNotThrow(() -> BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, discriminatorPropertyValue, schema));
        // Assert – match flag stays true and no ignore.
        assertTrue(ctx.matchFound, "Existing match flag must stay true");
        assertFalse(ctx.ignoreCalled, "markIgnore() must not be called");
    }

    @Test
    @DisplayName("Empty discriminator value matches explicit mapping key")
    void shouldMatchExplicitMappingWhenKeyIsEmptyString() {
        // Arrange
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ctx.matchFound = false;
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
        ObjectNode mapping = JsonNodeFactory.instance.objectNode();
        mapping.set("", JsonNodeFactory.instance.textNode("#/components/schemas/Empty"));
        discriminator.set("mapping", mapping);
        JsonSchema schema = JsonSchemaFactory.getInstance().getSchema(JsonNodeFactory.instance.objectNode());
        // empty string
        String discriminatorPropertyValue = "";
        // Act
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, discriminatorPropertyValue, schema);
        // Assert – match recorded, no ignore.
        assertTrue(ctx.matchFound, "Discriminator match should be recorded for empty key");
        assertFalse(ctx.ignoreCalled, "markIgnore() must not be called");
    }

    @ParameterizedTest(name = "[{index}] discriminatorPropertyValue={0}")
    @MethodSource("nullDiscriminatorPropertyProvider")
    @DisplayName("Parameterised test – discriminator property value is null")
    void parameterisedNullDiscriminatorPropertyValue(String discriminatorPropertyValue) {
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        // no mapping
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
        JsonSchema schema = JsonSchemaFactory.getInstance().getSchema(JsonNodeFactory.instance.objectNode());
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, discriminatorPropertyValue, schema);
        assertTrue(ctx.ignoreCalled, "markIgnore() should be called for null property value");
        assertFalse(ctx.matchFound, "No match should be recorded");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/BaseJsonValidator_checkDiscriminatorMatch_Test.java:14: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/BaseJsonValidator_checkDiscriminatorMatch_Test.java:23: error: interface expected here
    private static final class SimpleDiscriminatorContext implements DiscriminatorContext {
                                                                     ^
src/test/java/com/networknt/schema/BaseJsonValidator_checkDiscriminatorMatch_Test.java:17: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/BaseJsonValidator_checkDiscriminatorMatch_Test.java:29: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_checkDiscriminatorMatch_Test.java:34: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_checkDiscriminatorMatch_Test.java:39: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_checkDiscriminatorMatch_Test.java:60: error: no suitable method found for getInstance(no arguments)
        JsonSchema schema = JsonSchemaFactory.getInstance().getSchema(JsonNodeFactory.instance.objectNode());
                                             ^
    method JsonSchemaFactory.getInstance(VersionFlag) is not applicable
      (actual and formal argument lists differ in length)
    method JsonSchemaFactory.getInstance(VersionFlag,Consumer<Builder>) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/BaseJsonValidator_checkDiscriminatorMatch_Test.java:63: error: incompatible types: SimpleDiscriminatorContext cannot be converted to DiscriminatorContext
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, discriminatorPropertyValue, schema);
                                                  ^
src/test/java/com/networknt/schema/BaseJsonValidator_checkDiscriminatorMatch_Test.java:82: error: no suitable method found for getInstance(no arguments)
        JsonSchema schema = JsonSchemaFactory.getInstance().getSchema(JsonNodeFactory.instance.objectNode());
                                             ^
    method JsonSchemaFactory.getInstance(VersionFlag) is not applicable
      (actual and formal argument lists differ in length)
    method JsonSchemaFactory.getInstance(VersionFlag,Consumer<Builder>) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/BaseJsonValidator_checkDiscriminatorMatch_Test.java:85: error: incompatible types: SimpleDiscriminatorContext cannot be converted to DiscriminatorContext
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, discriminatorPropertyValue, schema);
                                                  ^
src/test/java/com/networknt/schema/BaseJsonValidator_checkDiscriminatorMatch_Test.java:94: error: no suitable method found for getInstance(no arguments)
        JsonSchema schema = JsonSchemaFactory.getInstance().getSchema(JsonNodeFactory.instance.objectNode());
                                             ^
    method JsonSchemaFactory.getInstance(VersionFlag) is not applicable
      (actual and formal argument lists differ in length)
    method JsonSchemaFactory.getInstance(VersionFlag,Consumer<Builder>) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/BaseJsonValidator_checkDiscriminatorMatch_Test.java:95: error: incompatible types: SimpleDiscriminatorContext cannot be converted to DiscriminatorContext
        assertThrows(NullPointerException.class, () -> BaseJsonValidator.checkDiscriminatorMatch(ctx, null, "any", schema));
                                                                                                 ^
src/test/java/com/networknt/schema/BaseJsonValidator_checkDiscriminatorMatch_Test.java:104: error: incompatible types: SimpleDiscriminatorContext cannot be converted to DiscriminatorContext
        assertThrows(NullPointerException.class, () -> BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "value", null));
                                                                                                 ^
src/test/java/com/networknt/schema/BaseJsonValidator_checkDiscriminatorMatch_Test.java:114: error: incompatible types: SimpleDiscriminatorContext cannot be converted to DiscriminatorContext
        assertThrows(NullPointerException.class, () -> BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "value", null));
                                                                                                 ^
src/test/java/com/networknt/schema/BaseJsonValidator_checkDiscriminatorMatch_Test.java:121: error: no suitable method found for getInstance(no arguments)
        JsonSchema schema = JsonSchemaFactory.getInstance().getSchema(JsonNodeFactory.instance.objectNode());
                                             ^
    method JsonSchemaFactory.getInstance(VersionFlag) is not applicable
      (actual and formal argument lists differ in length)
    method JsonSchemaFactory.getInstance(VersionFlag,Consumer<Builder>) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/BaseJsonValidator_checkDiscriminatorMatch_Test.java:136: error: no suitable method found for getInstance(no arguments)
        JsonSchema schema = JsonSchemaFactory.getInstance().getSchema(JsonNodeFactory.instance.objectNode());
                                             ^
    method JsonSchemaFactory.getInstance(VersionFlag) is not applicable
      (actual and formal argument lists differ in length)
    method JsonSchemaFactory.getInstance(VersionFlag,Consumer<Builder>) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/BaseJsonValidator_checkDiscriminatorMatch_Test.java:139: error: incompatible types: SimpleDiscriminatorContext cannot be converted to DiscriminatorContext
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, discriminatorPropertyValue, schema);
                                                  ^
src/test/java/com/networknt/schema/BaseJsonValidator_checkDiscriminatorMatch_Test.java:155: error: no suitable method found for getInstance(no arguments)
        JsonSchema schema = JsonSchemaFactory.getInstance().getSchema(JsonNodeFactory.instance.objectNode());
                                             ^
    method JsonSchemaFactory.getInstance(VersionFlag) is not applicable
      (actual and formal argument lists differ in length)
    method JsonSchemaFactory.getInstance(VersionFlag,Consumer<Builder>) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/BaseJsonValidator_checkDiscriminatorMatch_Test.java:158: error: incompatible types: SimpleDiscriminatorContext cannot be converted to DiscriminatorContext
        assertDoesNotThrow(() -> BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, discriminatorPropertyValue, schema));
                                                                           ^
src/test/java/com/networknt/schema/BaseJsonValidator_checkDiscriminatorMatch_Test.java:177: error: no suitable method found for getInstance(no arguments)
        JsonSchema schema = JsonSchemaFactory.getInstance().getSchema(JsonNodeFactory.instance.objectNode());
                                             ^
    method JsonSchemaFactory.getInstance(VersionFlag) is not applicable
      (actual and formal argument lists differ in length)
    method JsonSchemaFactory.getInstance(VersionFlag,Consumer<Builder>) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/BaseJsonValidator_checkDiscriminatorMatch_Test.java:180: error: incompatible types: SimpleDiscriminatorContext cannot be converted to DiscriminatorContext
        assertDoesNotThrow(() -> BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, discriminatorPropertyValue, schema));
                                                                           ^
src/test/java/com/networknt/schema/BaseJsonValidator_checkDiscriminatorMatch_Test.java:194: error: no suitable method found for getInstance(no arguments)
        JsonSchema schema = JsonSchemaFactory.getInstance().getSchema(JsonNodeFactory.instance.objectNode());
                                             ^
    method JsonSchemaFactory.getInstance(VersionFlag) is not applicable
      (actual and formal argument lists differ in length)
    method JsonSchemaFactory.getInstance(VersionFlag,Consumer<Builder>) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/BaseJsonValidator_checkDiscriminatorMatch_Test.java:197: error: incompatible types: SimpleDiscriminatorContext cannot be converted to DiscriminatorContext
        assertDoesNotThrow(() -> BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, discriminatorPropertyValue, schema));
                                                                           ^
src/test/java/com/networknt/schema/BaseJsonValidator_checkDiscriminatorMatch_Test.java:212: error: no suitable method found for getInstance(no arguments)
        JsonSchema schema = JsonSchemaFactory.getInstance().getSchema(JsonNodeFactory.instance.objectNode());
                                             ^
    method JsonSchemaFactory.getInstance(VersionFlag) is not applicable
      (actual and formal argument lists differ in length)
    method JsonSchemaFactory.getInstance(VersionFlag,Consumer<Builder>) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/BaseJsonValidator_checkDiscriminatorMatch_Test.java:215: error: incompatible types: SimpleDiscriminatorContext cannot be converted to DiscriminatorContext
        assertDoesNotThrow(() -> BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, discriminatorPropertyValue, schema));
                                                                           ^
src/test/java/com/networknt/schema/BaseJsonValidator_checkDiscriminatorMatch_Test.java:231: error: no suitable method found for getInstance(no arguments)
        JsonSchema schema = JsonSchemaFactory.getInstance().getSchema(JsonNodeFactory.instance.objectNode());
                                             ^
    method JsonSchemaFactory.getInstance(VersionFlag) is not applicable
      (actual and formal argument lists differ in length)
    method JsonSchemaFactory.getInstance(VersionFlag,Consumer<Builder>) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/BaseJsonValidator_checkDiscriminatorMatch_Test.java:235: error: incompatible types: SimpleDiscriminatorContext cannot be converted to DiscriminatorContext
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, discriminatorPropertyValue, schema);
                                                  ^
src/test/java/com/networknt/schema/BaseJsonValidator_checkDiscriminatorMatch_Test.java:248: error: no suitable method found for getInstance(no arguments)
        JsonSchema schema = JsonSchemaFactory.getInstance().getSchema(JsonNodeFactory.instance.objectNode());
                                             ^
    method JsonSchemaFactory.getInstance(VersionFlag) is not applicable
      (actual and formal argument lists differ in length)
    method JsonSchemaFactory.getInstance(VersionFlag,Consumer<Builder>) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/BaseJsonValidator_checkDiscriminatorMatch_Test.java:249: error: incompatible types: SimpleDiscriminatorContext cannot be converted to DiscriminatorContext
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, discriminatorPropertyValue, schema);
                                                  ^
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
29 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.BaseJsonValidator': '\n * Base {@link JsonValidator}. \n ', 'api document of method checkDiscriminatorMatch(DiscriminatorContext, ObjectNode, String, JsonSchema)': '\n     * Checks based on the current {@link DiscriminatorContext} whether the provided {@link JsonSchema} a match against\n     * against the current discriminator.\n     *\n     * @param currentDiscriminatorContext the currently active {@link DiscriminatorContext}\n     * @param discriminator               the discriminator to use for the check\n     * @param discriminatorPropertyValue  the value of the <code>discriminator/propertyName</code> field\n     * @param jsonSchema                  the {@link JsonSchema} to check\n     ', 'dependent classes': 'class `com.networknt.schema.JsonSchema`:\n\t\nclass `com.networknt.schema.BaseJsonValidator`:\n\trelated functions:\n\t\tmethod `protected static void registerAndMergeDiscriminator(DiscriminatorContext, ObjectNode, JsonSchema, JsonNodePath)` returns `void`, related with `com.networknt.schema.BaseJsonValidator.checkDiscriminatorMatch(DiscriminatorContext, ObjectNode, String, JsonSchema)`, api document: \n     * Rolls up all nested and compatible discriminators to the root discriminator of the type. Detects attempts to redefine\n     * the <code>propertyName</code> or mappings.\n     *\n     * @param currentDiscriminatorContext the currently active {@link DiscriminatorContext}\n     * @param discriminator               the discriminator to use for the check\n     * @param schema                      the value of the <code>discriminator/propertyName</code> field\n     * @param instanceLocation                          the logging prefix\n     ;\n\t\tmethod `protected static void checkDiscriminatorMatch(DiscriminatorContext, ObjectNode, String, JsonSchema)` returns `void`, related with `com.networknt.schema.BaseJsonValidator.checkDiscriminatorMatch(DiscriminatorContext, ObjectNode, String, JsonSchema)`, api document: \n     * Checks based on the current {@link DiscriminatorContext} whether the provided {@link JsonSchema} a match against\n     * against the current discriminator.\n     *\n     * @param currentDiscriminatorContext the currently active {@link DiscriminatorContext}\n     * @param discriminator               the discriminator to use for the check\n     * @param discriminatorPropertyValue  the value of the <code>discriminator/propertyName</code> field\n     * @param jsonSchema                  the {@link JsonSchema} to check\n     ;\nclass `com.networknt.schema.PropertiesValidator`:\n\trelated functions:\n\t\tmethod `private void applyPropertyDefaults(ObjectNode)` returns `void`, related with `com.networknt.schema.BaseJsonValidator.checkDiscriminatorMatch(DiscriminatorContext, ObjectNode, String, JsonSchema)`;\nclass `com.networknt.schema.AllOfValidator`:\n\trelated functions:\n\t\tmethod `protected Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.BaseJsonValidator.checkDiscriminatorMatch(DiscriminatorContext, ObjectNode, String, JsonSchema)`;\nclass `com.networknt.schema.OneOfValidator`:\n\trelated functions:\n\t\tmethod `protected Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.BaseJsonValidator.checkDiscriminatorMatch(DiscriminatorContext, ObjectNode, String, JsonSchema)`;\nclass `com.networknt.schema.AnyOfValidator`:\n\trelated functions:\n\t\tmethod `protected Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.BaseJsonValidator.checkDiscriminatorMatch(DiscriminatorContext, ObjectNode, String, JsonSchema)`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.BaseJsonValidator.checkDiscriminatorMatch(DiscriminatorContext, ObjectNode, String, JsonSchema)`, api document: \n     * Returns the singleton instance of this validator.\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


