
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_createExecutionContext_Test {

    /**
     * Mocks used for every test **
     */
    @Mock
    private ValidationContext validationContext;

    @Mock
    private SchemaValidatorsConfig config;

    /**
     * Instance under test **
     */
    private JsonSchema jsonSchema;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // ValidationContext#getConfig() must return our mocked config
        when(validationContext.getConfig()).thenReturn(config);
        // Build the JsonSchema instance that will be exercised
        jsonSchema = createJsonSchema(validationContext);
    }

    /**
     * Helper – creates a JsonSchema instance via the private constructor **
     */
    private JsonSchema createJsonSchema(ValidationContext vc) throws Exception {
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(ValidationContext.class, String.class, URI.class, JsonNode.class, JsonSchema.class, boolean.class);
        ctor.setAccessible(true);
        // all other arguments can be null / false – they are not used by createExecutionContext()
        return ctor.newInstance(vc, null, null, null, null, false);
    }

    /**
     * Helper – extracts a private field value via reflection **
     */
    private static <T> T getPrivateField(Object target, String fieldName, Class<T> type) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return type.cast(f.get(target));
    }

    /**
     * Scenario 1 – a supplier is supplied in the config **
     */
    @Test
    void createExecutionContext_usesSuppliedExecutionContext() {
        // Arrange
        @SuppressWarnings("unchecked")
        Supplier<ExecutionContext> supplier = mock(Supplier.class);
        ExecutionContext suppliedCtx = mock(ExecutionContext.class);
        when(supplier.get()).thenReturn(suppliedCtx);
        when(config.getExecutionContextSupplier()).thenReturn(supplier);
        // Act
        ExecutionContext ctx = jsonSchema.createExecutionContext();
        // Assert
        assertSame(suppliedCtx, ctx, "The ExecutionContext returned should be the one supplied by the config");
        verify(supplier, times(1)).get();
    }

    /**
     * Scenario 2 – no supplier, default CollectorContext is created **
     */
    @Test
    void createExecutionContext_createsDefaultExecutionContextWhenNoSupplier() throws Exception {
        // Arrange
        when(config.getExecutionContextSupplier()).thenReturn(null);
        when(config.isUnevaluatedItemsAnalysisDisabled()).thenReturn(true);
        when(config.isUnevaluatedPropertiesAnalysisDisabled()).thenReturn(false);
        // Act
        ExecutionContext ctx = jsonSchema.createExecutionContext();
        // Assert – a non‑null ExecutionContext must be produced
        assertNotNull(ctx, "ExecutionContext must not be null when no supplier is configured");
        // Verify that the internal CollectorContext reflects the config flags
        CollectorContext collector = getPrivateField(ctx, "collectorContext", CollectorContext.class);
        assertNotNull(collector, "CollectorContext should be created inside ExecutionContext");
        // The CollectorContext constructor stores the two booleans; we can read them via reflection as well
        boolean itemsDisabled = getPrivateField(collector, "unevaluatedItemsAnalysisDisabled", boolean.class);
        boolean propsDisabled = getPrivateField(collector, "unevaluatedPropertiesAnalysisDisabled", boolean.class);
        assertTrue(itemsDisabled, "unevaluatedItemsAnalysisDisabled flag must be propagated");
        assertFalse(propsDisabled, "unevaluatedPropertiesAnalysisDisabled flag must be propagated");
    }

    /**
     * Parameterized test for all flag combinations when the supplier is null **
     */
    @ParameterizedTest(name = "flags – items:{0} props:{1}")
    @CsvSource({ "false, false", "true, true", "false, true", "true, false" })
    void createExecutionContext_defaultFlags(boolean itemsDisabled, boolean propsDisabled) throws Exception {
        // Arrange
        when(config.getExecutionContextSupplier()).thenReturn(null);
        when(config.isUnevaluatedItemsAnalysisDisabled()).thenReturn(itemsDisabled);
        when(config.isUnevaluatedPropertiesAnalysisDisabled()).thenReturn(propsDisabled);
        // Act
        ExecutionContext ctx = jsonSchema.createExecutionContext();
        // Assert
        assertNotNull(ctx, "ExecutionContext must not be null");
        CollectorContext collector = getPrivateField(ctx, "collectorContext", CollectorContext.class);
        assertNotNull(collector, "CollectorContext must be present");
        boolean actualItems = getPrivateField(collector, "unevaluatedItemsAnalysisDisabled", boolean.class);
        boolean actualProps = getPrivateField(collector, "unevaluatedPropertiesAnalysisDisabled", boolean.class);
        assertEquals(itemsDisabled, actualItems, "Items flag should match config");
        assertEquals(propsDisabled, actualProps, "Properties flag should match config");
    }

    /**
     * Scenario – supplier returns null **
     */
    @Test
    void createExecutionContext_supplierReturnsNull() {
        // Arrange
        @SuppressWarnings("unchecked")
        Supplier<ExecutionContext> supplier = mock(Supplier.class);
        when(supplier.get()).thenReturn(null);
        when(config.getExecutionContextSupplier()).thenReturn(supplier);
        // Act
        ExecutionContext ctx = jsonSchema.createExecutionContext();
        // Assert
        assertNull(ctx, "When the supplier returns null the method should return null");
        verify(supplier, times(1)).get();
    }

    /**
     * Scenario – supplier throws a RuntimeException **
     */
    @Test
    void createExecutionContext_supplierThrowsException() {
        // Arrange
        @SuppressWarnings("unchecked")
        Supplier<ExecutionContext> supplier = mock(Supplier.class);
        RuntimeException ex = new RuntimeException("supplier failure");
        when(supplier.get()).thenThrow(ex);
        when(config.getExecutionContextSupplier()).thenReturn(supplier);
        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> jsonSchema.createExecutionContext(), "The RuntimeException from the supplier should propagate");
        assertSame(ex, thrown, "The thrown exception must be the same instance");
    }

    /**
     * Scenario – multiple invocations produce distinct instances **
     */
    @Test
    void createExecutionContext_multipleInvocationsProduceDistinctInstances() throws Exception {
        // Arrange
        when(config.getExecutionContextSupplier()).thenReturn(null);
        when(config.isUnevaluatedItemsAnalysisDisabled()).thenReturn(false);
        when(config.isUnevaluatedPropertiesAnalysisDisabled()).thenReturn(false);
        // Act
        ExecutionContext first = jsonSchema.createExecutionContext();
        ExecutionContext second = jsonSchema.createExecutionContext();
        // Assert
        assertNotSame(first, second, "Each call should return a new ExecutionContext instance");
        CollectorContext firstCollector = getPrivateField(first, "collectorContext", CollectorContext.class);
        CollectorContext secondCollector = getPrivateField(second, "collectorContext", CollectorContext.class);
        assertNotNull(firstCollector, "First ExecutionContext must contain a CollectorContext");
        assertNotNull(secondCollector, "Second ExecutionContext must contain a CollectorContext");
        // Ensure they are distinct objects as well
        assertNotSame(firstCollector, secondCollector, "Each ExecutionContext should have its own CollectorContext");
    }

    /**
     * Scenario – ValidationContext returns null config (NPE expected) **
     */
    @Test
    void createExecutionContext_validationContextReturnsNullConfig() {
        // Arrange
        when(validationContext.getConfig()).thenReturn(null);
        // Act & Assert
        NullPointerException npe = assertThrows(NullPointerException.class, () -> jsonSchema.createExecutionContext(), "A null config should cause a NullPointerException");
        // message is not important, just ensure NPE occurs
        assertNotNull(npe.getMessage());
    }

    /**
     * Scenario – JsonSchema constructed with a null ValidationContext (NPE expected) **
     */
    @Test
    void createExecutionContext_nullValidationContext() throws Exception {
        // Create a JsonSchema where the internal validationContext field is null
        JsonSchema schemaWithNullVc = createJsonSchema(null);
        // Act & Assert
        NullPointerException npe = assertThrows(NullPointerException.class, () -> schemaWithNullVc.createExecutionContext(), "When validationContext is null, a NullPointerException should be thrown");
        assertNotNull(npe.getMessage());
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:6: error: package org.mockito does not exist
import org.mockito.Mock;
                  ^
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:7: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:13: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:16: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:17: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:19: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:25: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:28: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:39: error: cannot find symbol
        when(validationContext.getConfig()).thenReturn(config);
        ^
  symbol:   method when(SchemaValidatorsConfig)
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:70: error: cannot find symbol
        Supplier<ExecutionContext> supplier = mock(Supplier.class);
                                              ^
  symbol:   method mock(Class<Supplier>)
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:71: error: cannot find symbol
        ExecutionContext suppliedCtx = mock(ExecutionContext.class);
                                       ^
  symbol:   method mock(Class<ExecutionContext>)
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:72: error: cannot find symbol
        when(supplier.get()).thenReturn(suppliedCtx);
        ^
  symbol:   method when(ExecutionContext)
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:73: error: cannot find symbol
        when(config.getExecutionContextSupplier()).thenReturn(supplier);
        ^
  symbol:   method when(Supplier<ExecutionContext>)
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:78: error: cannot find symbol
        verify(supplier, times(1)).get();
                         ^
  symbol:   method times(int)
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:87: error: cannot find symbol
        when(config.getExecutionContextSupplier()).thenReturn(null);
        ^
  symbol:   method when(Supplier<ExecutionContext>)
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:88: error: cannot find symbol
        when(config.isUnevaluatedItemsAnalysisDisabled()).thenReturn(true);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:89: error: cannot find symbol
        when(config.isUnevaluatedPropertiesAnalysisDisabled()).thenReturn(false);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:111: error: cannot find symbol
        when(config.getExecutionContextSupplier()).thenReturn(null);
        ^
  symbol:   method when(Supplier<ExecutionContext>)
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:112: error: cannot find symbol
        when(config.isUnevaluatedItemsAnalysisDisabled()).thenReturn(itemsDisabled);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:113: error: cannot find symbol
        when(config.isUnevaluatedPropertiesAnalysisDisabled()).thenReturn(propsDisabled);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:133: error: cannot find symbol
        Supplier<ExecutionContext> supplier = mock(Supplier.class);
                                              ^
  symbol:   method mock(Class<Supplier>)
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:134: error: cannot find symbol
        when(supplier.get()).thenReturn(null);
        ^
  symbol:   method when(ExecutionContext)
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:135: error: cannot find symbol
        when(config.getExecutionContextSupplier()).thenReturn(supplier);
        ^
  symbol:   method when(Supplier<ExecutionContext>)
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:140: error: cannot find symbol
        verify(supplier, times(1)).get();
                         ^
  symbol:   method times(int)
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:150: error: cannot find symbol
        Supplier<ExecutionContext> supplier = mock(Supplier.class);
                                              ^
  symbol:   method mock(Class<Supplier>)
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:152: error: cannot find symbol
        when(supplier.get()).thenThrow(ex);
        ^
  symbol:   method when(ExecutionContext)
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:153: error: cannot find symbol
        when(config.getExecutionContextSupplier()).thenReturn(supplier);
        ^
  symbol:   method when(Supplier<ExecutionContext>)
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:165: error: cannot find symbol
        when(config.getExecutionContextSupplier()).thenReturn(null);
        ^
  symbol:   method when(Supplier<ExecutionContext>)
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:166: error: cannot find symbol
        when(config.isUnevaluatedItemsAnalysisDisabled()).thenReturn(false);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:167: error: cannot find symbol
        when(config.isUnevaluatedPropertiesAnalysisDisabled()).thenReturn(false);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:187: error: cannot find symbol
        when(validationContext.getConfig()).thenReturn(null);
        ^
  symbol:   method when(SchemaValidatorsConfig)
  location: class JsonSchema_createExecutionContext_Test
31 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n ', 'api document of method createExecutionContext()': '\n     * Creates an execution context.\n     * \n     * @return the execution context\n     ', 'return type': 'com.networknt.schema.ExecutionContext', 'dependent classes': 'class `com.networknt.schema.BaseJsonValidator`:\n\tdependent fields:\n\t\tcom.networknt.schema.ValidationContext com.networknt.schema.BaseJsonValidator.validationContext;\n\t\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod ` ValidationContext getValidationContext()` returns `com.networknt.schema.ValidationContext`, related with `com.networknt.schema.JsonSchema.createExecutionContext()`;\n\t\tmethod `private String getCustomMessage(JsonNode, String)` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.createExecutionContext()`;\n\t\tmethod `private ValidationResult validateAndCollect(ExecutionContext, JsonNode, JsonNode, String)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.createExecutionContext()`, api document: \n     * This method both validates and collects the data in a CollectorContext.\n     * Unlike others this methods cleans and removes everything from collector\n     * context before returning.\n     * @param executionContext ExecutionContext\n     * @param jsonNode JsonNode\n     * @param rootNode JsonNode\n     * @param at       String path\n     *\n     * @return ValidationResult\n     ;\nclass `com.networknt.schema.ItemsValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.createExecutionContext()`;\nclass `com.networknt.schema.PropertiesValidator`:\n\trelated functions:\n\t\tmethod `private void walkSchema(ExecutionContext, Map.Entry<String, JsonSchema>, JsonNode, JsonNode, String, boolean, Set<ValidationMessage>, WalkListenerRunner)` returns `void`, related with `com.networknt.schema.JsonSchema.createExecutionContext()`;\nclass `com.networknt.schema.EnumValidator`:\n\trelated functions:\n\t\tmethod `private boolean isTypeLooseContainsInEnum(JsonNode)` returns `boolean`, related with `com.networknt.schema.JsonSchema.createExecutionContext()`, api document: \n     * Check whether enum contains the value of the JsonNode if the typeLoose is enabled.\n     *\n     * @param node JsonNode to check\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


