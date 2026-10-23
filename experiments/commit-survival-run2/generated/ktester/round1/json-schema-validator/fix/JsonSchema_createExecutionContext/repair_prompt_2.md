
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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;

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

    /**
     * Stub for {@link SchemaValidatorsConfig}.
     */
    private static class StubConfig extends SchemaValidatorsConfig {

        private Supplier<ExecutionContext> executionContextSupplier;

        private boolean unevaluatedItemsAnalysisDisabled;

        private boolean unevaluatedPropertiesAnalysisDisabled;

        @Override
        public Supplier<ExecutionContext> getExecutionContextSupplier() {
            return executionContextSupplier;
        }

        @Override
        public boolean isUnevaluatedItemsAnalysisDisabled() {
            return unevaluatedItemsAnalysisDisabled;
        }

        @Override
        public boolean isUnevaluatedPropertiesAnalysisDisabled() {
            return unevaluatedPropertiesAnalysisDisabled;
        }

        @Override
        public void setExecutionContextSupplier(Supplier<ExecutionContext> s) {
            this.executionContextSupplier = s;
        }

        @Override
        public void setUnevaluatedItemsAnalysisDisabled(boolean b) {
            this.unevaluatedItemsAnalysisDisabled = b;
        }

        @Override
        public void setUnevaluatedPropertiesAnalysisDisabled(boolean b) {
            this.unevaluatedPropertiesAnalysisDisabled = b;
        }
    }

    /**
     * Stub for {@link ValidationContext}.
     */
    private static class StubValidationContext extends ValidationContext {

        private final StubConfig stubConfig;

        StubValidationContext(StubConfig cfg) {
            // The super‑class has a protected constructor that expects many arguments.
            // For the purpose of these tests we can invoke the no‑arg constructor via reflection.
            super(null, null, null, null, null);
            this.stubConfig = cfg;
        }

        @Override
        public SchemaValidatorsConfig getConfig() {
            return stubConfig;
        }
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        config = new StubConfig();
        validationContext = new StubValidationContext(config);
        jsonSchema = createJsonSchema(validationContext);
    }

    /**
     * Helper – creates a JsonSchema instance via the private constructor **
     */
    private JsonSchema createJsonSchema(ValidationContext vc) throws Exception {
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(ValidationContext.class, String.class, URI.class, JsonNode.class, JsonSchema.class, boolean.class);
        ctor.setAccessible(true);
        // other arguments are not needed for the execution‑context tests
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
        // arrange – a supplier that returns a freshly created ExecutionContext
        ExecutionContext supplied = new ExecutionContext();
        Supplier<ExecutionContext> supplier = () -> supplied;
        config.setExecutionContextSupplier(supplier);
        // act
        ExecutionContext ctx = jsonSchema.createExecutionContext();
        // assert
        assertSame(supplied, ctx, "The ExecutionContext returned should be the one supplied by the config");
    }

    /**
     * Scenario 2 – no supplier, default CollectorContext is created **
     */
    @Test
    void createExecutionContext_createsDefaultExecutionContextWhenNoSupplier() throws Exception {
        config.setExecutionContextSupplier(null);
        config.setUnevaluatedItemsAnalysisDisabled(true);
        config.setUnevaluatedPropertiesAnalysisDisabled(false);
        ExecutionContext ctx = jsonSchema.createExecutionContext();
        assertNotNull(ctx, "ExecutionContext must not be null when no supplier is configured");
        CollectorContext collector = getPrivateField(ctx, "collectorContext", CollectorContext.class);
        assertNotNull(collector, "CollectorContext should be created inside ExecutionContext");
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
        config.setExecutionContextSupplier(null);
        config.setUnevaluatedItemsAnalysisDisabled(itemsDisabled);
        config.setUnevaluatedPropertiesAnalysisDisabled(propsDisabled);
        ExecutionContext ctx = jsonSchema.createExecutionContext();
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
        config.setExecutionContextSupplier(() -> null);
        ExecutionContext ctx = jsonSchema.createExecutionContext();
        assertNull(ctx, "When the supplier returns null the method should return null");
    }

    /**
     * Scenario – supplier throws a RuntimeException **
     */
    @Test
    void createExecutionContext_supplierThrowsException() {
        RuntimeException ex = new RuntimeException("supplier failure");
        config.setExecutionContextSupplier(() -> {
            throw ex;
        });
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> jsonSchema.createExecutionContext(), "The RuntimeException from the supplier should propagate");
        assertSame(ex, thrown, "The thrown exception must be the same instance");
    }

    /**
     * Scenario – multiple invocations produce distinct instances **
     */
    @Test
    void createExecutionContext_multipleInvocationsProduceDistinctInstances() throws Exception {
        config.setExecutionContextSupplier(null);
        config.setUnevaluatedItemsAnalysisDisabled(false);
        config.setUnevaluatedPropertiesAnalysisDisabled(false);
        ExecutionContext first = jsonSchema.createExecutionContext();
        ExecutionContext second = jsonSchema.createExecutionContext();
        assertNotSame(first, second, "Each call should return a new ExecutionContext instance");
        CollectorContext firstCollector = getPrivateField(first, "collectorContext", CollectorContext.class);
        CollectorContext secondCollector = getPrivateField(second, "collectorContext", CollectorContext.class);
        assertNotNull(firstCollector, "First ExecutionContext must contain a CollectorContext");
        assertNotNull(secondCollector, "Second ExecutionContext must contain a CollectorContext");
        assertNotSame(firstCollector, secondCollector, "Each ExecutionContext should have its own CollectorContext");
    }

    /**
     * Scenario – ValidationContext returns null config (NPE expected) **
     */
    @Test
    void createExecutionContext_validationContextReturnsNullConfig() {
        // replace the validation context with one that returns null config
        validationContext = new StubValidationContext(null);
        JsonSchema schema = assertDoesNotThrow(() -> createJsonSchema(validationContext));
        NullPointerException npe = assertThrows(NullPointerException.class, schema::createExecutionContext, "A null config should cause a NullPointerException");
        assertNotNull(npe.getMessage());
    }

    /**
     * Scenario – JsonSchema constructed with a null ValidationContext (NPE expected) **
     */
    @Test
    void createExecutionContext_nullValidationContext() throws Exception {
        JsonSchema schemaWithNullVc = createJsonSchema(null);
        NullPointerException npe = assertThrows(NullPointerException.class, schemaWithNullVc::createExecutionContext, "When validationContext is null, a NullPointerException should be thrown");
        assertNotNull(npe.getMessage());
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
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:25: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_createExecutionContext_Test
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:64: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:69: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:98: error: incompatible types: SchemaValidatorsConfig cannot be converted to StubConfig
        validationContext = new StubValidationContext(config);
                                                      ^
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:142: error: cannot find symbol
        config.setUnevaluatedItemsAnalysisDisabled(true);
              ^
  symbol:   method setUnevaluatedItemsAnalysisDisabled(boolean)
  location: variable config of type SchemaValidatorsConfig
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:143: error: cannot find symbol
        config.setUnevaluatedPropertiesAnalysisDisabled(false);
              ^
  symbol:   method setUnevaluatedPropertiesAnalysisDisabled(boolean)
  location: variable config of type SchemaValidatorsConfig
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:161: error: cannot find symbol
        config.setUnevaluatedItemsAnalysisDisabled(itemsDisabled);
              ^
  symbol:   method setUnevaluatedItemsAnalysisDisabled(boolean)
  location: variable config of type SchemaValidatorsConfig
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:162: error: cannot find symbol
        config.setUnevaluatedPropertiesAnalysisDisabled(propsDisabled);
              ^
  symbol:   method setUnevaluatedPropertiesAnalysisDisabled(boolean)
  location: variable config of type SchemaValidatorsConfig
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:202: error: cannot find symbol
        config.setUnevaluatedItemsAnalysisDisabled(false);
              ^
  symbol:   method setUnevaluatedItemsAnalysisDisabled(boolean)
  location: variable config of type SchemaValidatorsConfig
src/test/java/com/networknt/schema/JsonSchema_createExecutionContext_Test.java:203: error: cannot find symbol
        config.setUnevaluatedPropertiesAnalysisDisabled(false);
              ^
  symbol:   method setUnevaluatedPropertiesAnalysisDisabled(boolean)
  location: variable config of type SchemaValidatorsConfig
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
14 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n ', 'api document of method createExecutionContext()': '\n     * Creates an execution context.\n     * \n     * @return the execution context\n     ', 'return type': 'com.networknt.schema.ExecutionContext', 'dependent classes': 'class `com.networknt.schema.BaseJsonValidator`:\n\tdependent fields:\n\t\tcom.networknt.schema.ValidationContext com.networknt.schema.BaseJsonValidator.validationContext;\n\t\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod ` ValidationContext getValidationContext()` returns `com.networknt.schema.ValidationContext`, related with `com.networknt.schema.JsonSchema.createExecutionContext()`;\n\t\tmethod `private String getCustomMessage(JsonNode, String)` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.createExecutionContext()`;\n\t\tmethod `private ValidationResult validateAndCollect(ExecutionContext, JsonNode, JsonNode, String)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.createExecutionContext()`, api document: \n     * This method both validates and collects the data in a CollectorContext.\n     * Unlike others this methods cleans and removes everything from collector\n     * context before returning.\n     * @param executionContext ExecutionContext\n     * @param jsonNode JsonNode\n     * @param rootNode JsonNode\n     * @param at       String path\n     *\n     * @return ValidationResult\n     ;\nclass `com.networknt.schema.ItemsValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.createExecutionContext()`;\nclass `com.networknt.schema.PropertiesValidator`:\n\trelated functions:\n\t\tmethod `private void walkSchema(ExecutionContext, Map.Entry<String, JsonSchema>, JsonNode, JsonNode, String, boolean, Set<ValidationMessage>, WalkListenerRunner)` returns `void`, related with `com.networknt.schema.JsonSchema.createExecutionContext()`;\nclass `com.networknt.schema.EnumValidator`:\n\trelated functions:\n\t\tmethod `private boolean isTypeLooseContainsInEnum(JsonNode)` returns `boolean`, related with `com.networknt.schema.JsonSchema.createExecutionContext()`, api document: \n     * Check whether enum contains the value of the JsonNode if the typeLoose is enabled.\n     *\n     * @param node JsonNode to check\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


