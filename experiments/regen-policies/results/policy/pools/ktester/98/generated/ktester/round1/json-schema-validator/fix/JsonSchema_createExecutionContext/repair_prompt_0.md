
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.lang.reflect.Field;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;

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

    @BeforeEach
    void setupBeforeEach() {
        // ---- create a very small schema node (empty object) -----------------
        schemaNode = new ObjectMapper().createObjectNode();
        // ---- configure a real SchemaValidatorsConfig ------------------------
        config = new SchemaValidatorsConfig();
        config.setLocale(Locale.GERMAN);
        config.setFormatAssertionsEnabled(true);
        config.setFailFast(true);
        // no customizer yet – individual tests will set it if needed
        // ---- stub the ValidationContext to return the objects we need -------
        when(validationContext.getConfig()).thenReturn(config);
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getSchemaResources()).thenReturn(new ConcurrentHashMap<>());
        when(validationContext.getDynamicAnchors()).thenReturn(new ConcurrentHashMap<>());
        // resolveSchemaId is called during JsonSchema construction – we do not need
        // any special handling for this test, returning null is sufficient.
        when(validationContext.resolveSchemaId(any())).thenReturn(null);
        // ---- create the JsonSchema instance using the package‑private factory --
        jsonSchema = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, // no parent
        null, // suppressSubSchemaRetrieval
        false);
    }

    /**
     * Verify that the ExecutionConfig values are copied from the ValidationContext.
     */
    @Test
    void createExecutionContext_copiesConfigValues() {
        // Act
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        // Assert – the ExecutionConfig inside the ExecutionContext must reflect the
        // values we set on the SchemaValidatorsConfig.
        ExecutionConfig execConfig = executionContext.getExecutionConfig();
        assertNotNull(execConfig, "ExecutionConfig should not be null");
        assertEquals(config.getLocale(), execConfig.getLocale(), "Locale should be copied from the config");
        assertEquals(config.getFormatAssertionsEnabled(), execConfig.isFormatAssertionsEnabled(), "formatAssertionsEnabled should be copied from the config");
        assertEquals(config.isFailFast(), execConfig.isFailFast(), "failFast flag should be copied from the config");
    }

    /**
     * Verify that a configured ExecutionContextCustomizer is invoked.
     */
    @Test
    void createExecutionContext_invokesCustomizer_whenPresent() {
        // Arrange – create a mock customizer and register it in the config
        ExecutionContextCustomizer customizer = mock(ExecutionContextCustomizer.class);
        config.setExecutionContextCustomizer(customizer);
        // Act
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        // Assert – the customizer must have been called exactly once with the
        // newly created ExecutionContext and the ValidationContext used by the schema.
        verify(customizer, times(1)).customize(executionContext, validationContext);
    }

    /**
     * Customizer mutates the ExecutionConfig (toggles failFast).
     */
    @Test
    void createExecutionContext_customizerCanMutateConfig() {
        // Arrange – a customizer that flips the failFast flag
        ExecutionContextCustomizer customizer = (execCtx, vCtx) -> {
            ExecutionConfig ec = execCtx.getExecutionConfig();
            ec.setFailFast(!ec.isFailFast());
        };
        config.setExecutionContextCustomizer(customizer);
        // Act
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        // Assert – the flag has been toggled compared to the original config
        // original was true, we expect false
        boolean expected = !config.isFailFast();
        assertEquals(expected, executionContext.getExecutionConfig().isFailFast(), "Customizer should have toggled the failFast flag");
    }

    /**
     * If the customizer throws, the exception propagates out of createExecutionContext().
     */
    @Test
    void createExecutionContext_customizerThrows_exceptionPropagates() {
        // Arrange – a customizer that always throws
        ExecutionContextCustomizer customizer = mock(ExecutionContextCustomizer.class);
        RuntimeException boom = new RuntimeException("boom");
        doThrow(boom).when(customizer).customize(any(), any());
        config.setExecutionContextCustomizer(customizer);
        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> jsonSchema.createExecutionContext(), "Exception from customizer should propagate");
        assertSame(boom, ex, "Propagated exception should be the same instance thrown by the customizer");
    }

    /**
     * Null ValidationContext inside JsonSchema leads to NPE.
     */
    @Test
    void createExecutionContext_nullValidationContext_throwsNPE() throws Exception {
        // Use reflection to set the protected field BaseJsonValidator.validationContext to null
        Field field = BaseJsonValidator.class.getDeclaredField("validationContext");
        field.setAccessible(true);
        field.set(jsonSchema, null);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> jsonSchema.createExecutionContext(), "When validationContext is null, createExecutionContext should throw NPE");
    }

    /**
     * Null SchemaValidatorsConfig returned from ValidationContext leads to NPE.
     */
    @Test
    void createExecutionContext_nullConfig_throwsNPE() {
        // Stub getConfig() to return null
        when(validationContext.getConfig()).thenReturn(null);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> jsonSchema.createExecutionContext(), "When validationContext.getConfig() returns null, createExecutionContext should throw NPE");
    }

    /**
     * Verify that a non‑default locale is correctly transferred to the ExecutionContext.
     */
    @Test
    void createExecutionContext_localePropagation() {
        // Change locale to a non‑default value
        config.setLocale(Locale.FRENCH);
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        assertEquals(Locale.FRENCH, executionContext.getExecutionConfig().getLocale(), "Locale set in SchemaValidatorsConfig must be propagated to ExecutionContext");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:4: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:10: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:14: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:9: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:15: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:17: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:23: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:27: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:31: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:35: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:61: error: cannot find symbol
        when(validationContext.getConfig()).thenReturn(config);
        ^
  symbol:   method when(SchemaValidatorsConfig)
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:62: error: cannot find symbol
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        ^
  symbol:   method when(JsonMetaSchema)
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:63: error: cannot find symbol
        when(validationContext.getSchemaResources()).thenReturn(new ConcurrentHashMap<>());
        ^
  symbol:   method when(ConcurrentMap<String,JsonSchema>)
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:64: error: cannot find symbol
        when(validationContext.getDynamicAnchors()).thenReturn(new ConcurrentHashMap<>());
        ^
  symbol:   method when(ConcurrentMap<String,JsonSchema>)
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:67: error: cannot find symbol
        when(validationContext.resolveSchemaId(any())).thenReturn(null);
                                               ^
  symbol:   method any()
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:86: error: cannot find symbol
        assertEquals(config.getFormatAssertionsEnabled(), execConfig.isFormatAssertionsEnabled(), "formatAssertionsEnabled should be copied from the config");
                                                                    ^
  symbol:   method isFormatAssertionsEnabled()
  location: variable execConfig of type ExecutionConfig
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:96: error: cannot find symbol
        ExecutionContextCustomizer customizer = mock(ExecutionContextCustomizer.class);
                                                ^
  symbol:   method mock(Class<ExecutionContextCustomizer>)
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:102: error: cannot find symbol
        verify(customizer, times(1)).customize(executionContext, validationContext);
                           ^
  symbol:   method times(int)
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:130: error: cannot find symbol
        ExecutionContextCustomizer customizer = mock(ExecutionContextCustomizer.class);
                                                ^
  symbol:   method mock(Class<ExecutionContextCustomizer>)
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:132: error: cannot find symbol
        doThrow(boom).when(customizer).customize(any(), any());
        ^
  symbol:   method doThrow(RuntimeException)
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:132: error: cannot find symbol
        doThrow(boom).when(customizer).customize(any(), any());
                                                 ^
  symbol:   method any()
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:132: error: cannot find symbol
        doThrow(boom).when(customizer).customize(any(), any());
                                                        ^
  symbol:   method any()
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:158: error: cannot find symbol
        when(validationContext.getConfig()).thenReturn(null);
        ^
  symbol:   method when(SchemaValidatorsConfig)
  location: class JsonSchema_createExecutionContext_Test
Note: src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java uses or overrides a deprecated API.
Note: Recompile with -Xlint:deprecation for details.
23 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method createExecutionContext()': '\n     * Creates an execution context.\n     * \n     * @return the execution context\n     ', 'return type': 'com.networknt.schema.ExecutionContext', 'dependent classes': 'class `com.networknt.schema.BaseJsonValidator`:\n\tdependent fields:\n\t\tcom.networknt.schema.ValidationContext com.networknt.schema.BaseJsonValidator.validationContext;\n\t\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public T validate(ExecutionContext, JsonNode, OutputFormat<T>, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.JsonSchema.createExecutionContext()`, api document: \n     * Validates to a format.\n     * \n     * @param <T>                 the result type\n     * @param executionContext    the execution context\n     * @param node                the node\n     * @param format              the format\n     * @param executionCustomizer the customizer\n     * @return the result\n     ;\n\t\tmethod `private T walkAtNodeInternal(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean, OutputFormat<T>, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.JsonSchema.createExecutionContext()`;\n\t\tmethod `public ValidationContext getValidationContext()` returns `com.networknt.schema.ValidationContext`, related with `com.networknt.schema.JsonSchema.createExecutionContext()`;\n\t\tmethod `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.createExecutionContext()`;\nclass `com.networknt.schema.ItemsValidator`:\n\trelated functions:\n\t\tmethod `private void walkSchema(ExecutionContext, JsonSchema, JsonNode, JsonNode, JsonNodePath, boolean, Set<ValidationMessage>, String)` returns `void`, related with `com.networknt.schema.JsonSchema.createExecutionContext()`;\nclass `com.networknt.schema.MultipleOfValidator`:\n\trelated functions:\n\t\tmethod `protected BigDecimal getDividend(JsonNode)` returns `java.math.BigDecimal`, related with `com.networknt.schema.JsonSchema.createExecutionContext()`, api document: \n     * Gets the dividend to use.\n     * \n     * @param node the node\n     * @return the dividend or null if the type is incorrect\n     ;\nclass `com.networknt.schema.FormatValidator`:\n\trelated functions:\n\t\tmethod `protected boolean isStrict(ExecutionContext)` returns `boolean`, related with `com.networknt.schema.JsonSchema.createExecutionContext()`, api document: \r\n     * Determines if strict handling.\r\n     * <p>\r\n     * Note that this defaults to false.\r\n     * \r\n     * @param executionContext the execution context\r\n     * @return whether to perform strict handling\r\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


