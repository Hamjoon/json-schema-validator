
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class JsonSchema_createExecutionContext_Test {

    /**
     * Mocks and test fixtures shared by all test methods.
     */
    @Mock
    ValidationContext // injected into JsonSchema
    validationContext;

    @Mock
    JsonMetaSchema // required by ValidationContext
    metaSchema;

    @Mock
    SchemaLocation // passed to JsonSchema.from(...)
    schemaLocation;

    @Mock
    JsonNodePath // passed to JsonSchema.from(...)
    evaluationPath;

    // real config – we set values on it
    SchemaValidatorsConfig config;

    // a minimal schema node (empty object)
    ObjectNode schemaNode;

    /**
     * Instance of {@link JsonSchema} under test.
     */
    JsonSchema jsonSchema;

    /* ----------------------------------------------------------------------
     * Very small concrete stubs – they extend the library classes (which are
     * abstract or concrete) rather than implement them as interfaces.
     * ---------------------------------------------------------------------- */
    private static class StubValidationContext extends ValidationContext {

        private final SchemaValidatorsConfig cfg;

        private final JsonMetaSchema meta;

        StubValidationContext(SchemaValidatorsConfig cfg, JsonMetaSchema meta) {
            this.cfg = cfg;
            this.meta = meta;
        }

        @Override
        public SchemaValidatorsConfig getConfig() {
            return cfg;
        }

        @Override
        public JsonMetaSchema getMetaSchema() {
            return meta;
        }

        @Override
        public ConcurrentMap<String, JsonSchema> getSchemaResources() {
            return new ConcurrentHashMap<>();
        }

        @Override
        public ConcurrentMap<String, JsonSchema> getDynamicAnchors() {
            return new ConcurrentHashMap<>();
        }

        @Override
        public String resolveSchemaId(Object id) {
            return null;
        }
    }

    /**
     * Minimal stub for {@link JsonMetaSchema}.
     */
    private static class StubJsonMetaSchema extends JsonMetaSchema {
        // No additional behaviour required.
    }

    /**
     * Minimal stub for {@link SchemaLocation}.
     */
    private static class StubSchemaLocation extends SchemaLocation {
        // No additional behaviour required.
    }

    /**
     * Minimal stub for {@link JsonNodePath}.
     */
    private static class StubJsonNodePath extends JsonNodePath {
        // No additional behaviour required.
    }

    @BeforeEach
    void setupBeforeEach() {
        // a minimal schema node (empty object)
        schemaNode = new ObjectMapper().createObjectNode();
        // real configuration – we set values on it
        config = new SchemaValidatorsConfig();
        config.setLocale(Locale.GERMAN);
        config.setFormatAssertionsEnabled(true);
        config.setFailFast(true);
        // create stub collaborators
        metaSchema = new StubJsonMetaSchema();
        schemaLocation = new StubSchemaLocation();
        evaluationPath = new StubJsonNodePath();
        validationContext = new StubValidationContext(config, metaSchema);
        // create the JsonSchema instance using the package‑private factory
        jsonSchema = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, null, false);
    }

    /**
     * Verify that the ExecutionConfig values are copied from the ValidationContext.
     */
    @Test
    void createExecutionContext_copiesConfigValues() {
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        ExecutionConfig execConfig = executionContext.getExecutionConfig();
        assertNotNull(execConfig, "ExecutionConfig should not be null");
        assertEquals(config.getLocale(), execConfig.getLocale(), "Locale should be copied from the config");
        // ExecutionConfig uses the getter name {@code getFormatAssertionsEnabled}
        assertEquals(config.getFormatAssertionsEnabled(), execConfig.getFormatAssertionsEnabled(), "formatAssertionsEnabled should be copied from the config");
        assertEquals(config.isFailFast(), execConfig.isFailFast(), "failFast flag should be copied from the config");
    }

    /**
     * Verify that a configured ExecutionContextCustomizer is invoked.
     */
    @Test
    void createExecutionContext_invokesCustomizer_whenPresent() {
        final boolean[] called = { false };
        ExecutionContextCustomizer customizer = (execCtx, vCtx) -> called[0] = true;
        config.setExecutionContextCustomizer(customizer);
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        assertTrue(called[0], "Customizer should have been invoked exactly once");
    }

    /**
     * Customizer mutates the ExecutionConfig (toggles failFast).
     */
    @Test
    void createExecutionContext_customizerCanMutateConfig() {
        ExecutionContextCustomizer customizer = (execCtx, vCtx) -> {
            ExecutionConfig ec = execCtx.getExecutionConfig();
            ec.setFailFast(!ec.isFailFast());
        };
        config.setExecutionContextCustomizer(customizer);
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        // original config value was true
        boolean expected = !config.isFailFast();
        assertEquals(expected, executionContext.getExecutionConfig().isFailFast(), "Customizer should have toggled the failFast flag");
    }

    /**
     * If the customizer throws, the exception propagates out of createExecutionContext().
     */
    @Test
    void createExecutionContext_customizerThrows_exceptionPropagates() {
        ExecutionContextCustomizer customizer = (execCtx, vCtx) -> {
            throw new RuntimeException("boom");
        };
        config.setExecutionContextCustomizer(customizer);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> jsonSchema.createExecutionContext(), "Exception from customizer should propagate");
        assertEquals("boom", ex.getMessage());
    }

    /**
     * Null ValidationContext inside JsonSchema leads to NPE.
     */
    @Test
    void createExecutionContext_nullValidationContext_throwsNPE() throws Exception {
        Field field = BaseJsonValidator.class.getDeclaredField("validationContext");
        field.setAccessible(true);
        field.set(jsonSchema, null);
        assertThrows(NullPointerException.class, () -> jsonSchema.createExecutionContext(), "When validationContext is null, createExecutionContext should throw NPE");
    }

    /**
     * Null SchemaValidatorsConfig returned from ValidationContext leads to NPE.
     */
    @Test
    void createExecutionContext_nullConfig_throwsNPE() {
        validationContext = new StubValidationContext(null, metaSchema);
        jsonSchema = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, null, false);
        assertThrows(NullPointerException.class, () -> jsonSchema.createExecutionContext(), "When validationContext.getConfig() returns null, createExecutionContext should throw NPE");
    }

    /**
     * Verify that a non‑default locale is correctly transferred to the ExecutionContext.
     */
    @Test
    void createExecutionContext_localePropagation() {
        config.setLocale(Locale.FRENCH);
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        assertEquals(Locale.FRENCH, executionContext.getExecutionConfig().getLocale(), "Locale set in SchemaValidatorsConfig must be propagated to ExecutionContext");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:12: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:13: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:16: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:22: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:26: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:30: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:34: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:59: error: no suitable constructor found for ValidationContext(no arguments)
        StubValidationContext(SchemaValidatorsConfig cfg, JsonMetaSchema meta) {
                                                                               ^
    constructor ValidationContext.ValidationContext(JsonMetaSchema,JsonSchemaFactory,SchemaValidatorsConfig) is not applicable
      (actual and formal argument lists differ in length)
    constructor ValidationContext.ValidationContext(JsonMetaSchema,JsonSchemaFactory,SchemaValidatorsConfig,ConcurrentMap<String,JsonSchema>,ConcurrentMap<String,JsonSchema>,ConcurrentMap<String,JsonSchema>) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:84: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:93: error: constructor JsonMetaSchema in class JsonMetaSchema cannot be applied to given types;
    private static class StubJsonMetaSchema extends JsonMetaSchema {
                   ^
  required: String,String,Map<String,Keyword>,Map<String,Boolean>,VersionFlag,Builder
  found:    no arguments
  reason: actual and formal argument lists differ in length
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:100: error: no suitable constructor found for SchemaLocation(no arguments)
    private static class StubSchemaLocation extends SchemaLocation {
                   ^
    constructor SchemaLocation.SchemaLocation(AbsoluteIri,JsonNodePath) is not applicable
      (actual and formal argument lists differ in length)
    constructor SchemaLocation.SchemaLocation(AbsoluteIri) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:107: error: no suitable constructor found for JsonNodePath(no arguments)
    private static class StubJsonNodePath extends JsonNodePath {
                   ^
    constructor JsonNodePath.JsonNodePath(PathType) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonNodePath.JsonNodePath(JsonNodePath,String) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonNodePath.JsonNodePath(JsonNodePath,int) is not applicable
      (actual and formal argument lists differ in length)
Note: src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java uses or overrides a deprecated API.
Note: Recompile with -Xlint:deprecation for details.
12 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method createExecutionContext()': '\n     * Creates an execution context.\n     * \n     * @return the execution context\n     ', 'return type': 'com.networknt.schema.ExecutionContext', 'dependent classes': 'class `com.networknt.schema.BaseJsonValidator`:\n\tdependent fields:\n\t\tcom.networknt.schema.ValidationContext com.networknt.schema.BaseJsonValidator.validationContext;\n\t\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public T validate(ExecutionContext, JsonNode, OutputFormat<T>, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.JsonSchema.createExecutionContext()`, api document: \n     * Validates to a format.\n     * \n     * @param <T>                 the result type\n     * @param executionContext    the execution context\n     * @param node                the node\n     * @param format              the format\n     * @param executionCustomizer the customizer\n     * @return the result\n     ;\n\t\tmethod `private T walkAtNodeInternal(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean, OutputFormat<T>, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.JsonSchema.createExecutionContext()`;\n\t\tmethod `public ValidationContext getValidationContext()` returns `com.networknt.schema.ValidationContext`, related with `com.networknt.schema.JsonSchema.createExecutionContext()`;\n\t\tmethod `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.createExecutionContext()`;\nclass `com.networknt.schema.ItemsValidator`:\n\trelated functions:\n\t\tmethod `private void walkSchema(ExecutionContext, JsonSchema, JsonNode, JsonNode, JsonNodePath, boolean, Set<ValidationMessage>, String)` returns `void`, related with `com.networknt.schema.JsonSchema.createExecutionContext()`;\nclass `com.networknt.schema.MultipleOfValidator`:\n\trelated functions:\n\t\tmethod `protected BigDecimal getDividend(JsonNode)` returns `java.math.BigDecimal`, related with `com.networknt.schema.JsonSchema.createExecutionContext()`, api document: \n     * Gets the dividend to use.\n     * \n     * @param node the node\n     * @return the dividend or null if the type is incorrect\n     ;\nclass `com.networknt.schema.FormatValidator`:\n\trelated functions:\n\t\tmethod `protected boolean isStrict(ExecutionContext)` returns `boolean`, related with `com.networknt.schema.JsonSchema.createExecutionContext()`, api document: \r\n     * Determines if strict handling.\r\n     * <p>\r\n     * Note that this defaults to false.\r\n     * \r\n     * @param executionContext the execution context\r\n     * @return whether to perform strict handling\r\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


