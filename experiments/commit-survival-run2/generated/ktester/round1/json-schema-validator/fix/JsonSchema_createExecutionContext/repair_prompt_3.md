


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
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

// @ExtendWith(MockitoExtension.class)
class JsonSchema_createExecutionContext_Test {

    /**
     * Mocks used for every test **
     */
//     @Mock
    private ValidationContext validationContext;

//     @Mock
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

//         @Override
//         public void setUnevaluatedItemsAnalysisDisabled(boolean b) {
//             this.unevaluatedItemsAnalysisDisabled = b;
//         }

//         @Override
//         public void setUnevaluatedPropertiesAnalysisDisabled(boolean b) {
//             this.unevaluatedPropertiesAnalysisDisabled = b;
//         }
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
//         validationContext = new StubValidationContext(config);
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
//     @Test
//     void createExecutionContext_createsDefaultExecutionContextWhenNoSupplier() throws Exception {
//         config.setExecutionContextSupplier(null);
//         config.setUnevaluatedItemsAnalysisDisabled(true);
//         config.setUnevaluatedPropertiesAnalysisDisabled(false);
//         ExecutionContext ctx = jsonSchema.createExecutionContext();
//         assertNotNull(ctx, "ExecutionContext must not be null when no supplier is configured");
//         CollectorContext collector = getPrivateField(ctx, "collectorContext", CollectorContext.class);
//         assertNotNull(collector, "CollectorContext should be created inside ExecutionContext");
//         boolean itemsDisabled = getPrivateField(collector, "unevaluatedItemsAnalysisDisabled", boolean.class);
//         boolean propsDisabled = getPrivateField(collector, "unevaluatedPropertiesAnalysisDisabled", boolean.class);
//         assertTrue(itemsDisabled, "unevaluatedItemsAnalysisDisabled flag must be propagated");
//         assertFalse(propsDisabled, "unevaluatedPropertiesAnalysisDisabled flag must be propagated");
//     }

    /**
     * Parameterized test for all flag combinations when the supplier is null **
     */
//     @ParameterizedTest(name = "flags – items:{0} props:{1}")
//     @CsvSource({ "false, false", "true, true", "false, true", "true, false" })
//     void createExecutionContext_defaultFlags(boolean itemsDisabled, boolean propsDisabled) throws Exception {
//         config.setExecutionContextSupplier(null);
//         config.setUnevaluatedItemsAnalysisDisabled(itemsDisabled);
//         config.setUnevaluatedPropertiesAnalysisDisabled(propsDisabled);
//         ExecutionContext ctx = jsonSchema.createExecutionContext();
//         assertNotNull(ctx, "ExecutionContext must not be null");
//         CollectorContext collector = getPrivateField(ctx, "collectorContext", CollectorContext.class);
//         assertNotNull(collector, "CollectorContext must be present");
//         boolean actualItems = getPrivateField(collector, "unevaluatedItemsAnalysisDisabled", boolean.class);
//         boolean actualProps = getPrivateField(collector, "unevaluatedPropertiesAnalysisDisabled", boolean.class);
//         assertEquals(itemsDisabled, actualItems, "Items flag should match config");
//         assertEquals(propsDisabled, actualProps, "Properties flag should match config");
//     }

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
//     @Test
//     void createExecutionContext_multipleInvocationsProduceDistinctInstances() throws Exception {
//         config.setExecutionContextSupplier(null);
//         config.setUnevaluatedItemsAnalysisDisabled(false);
//         config.setUnevaluatedPropertiesAnalysisDisabled(false);
//         ExecutionContext first = jsonSchema.createExecutionContext();
//         ExecutionContext second = jsonSchema.createExecutionContext();
//         assertNotSame(first, second, "Each call should return a new ExecutionContext instance");
//         CollectorContext firstCollector = getPrivateField(first, "collectorContext", CollectorContext.class);
//         CollectorContext secondCollector = getPrivateField(second, "collectorContext", CollectorContext.class);
//         assertNotNull(firstCollector, "First ExecutionContext must contain a CollectorContext");
//         assertNotNull(secondCollector, "Second ExecutionContext must contain a CollectorContext");
//         assertNotSame(firstCollector, secondCollector, "Each ExecutionContext should have its own CollectorContext");
//     }

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

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

╷
├─ JUnit Jupiter ✔
│  └─ JsonSchema_createExecutionContext_Test ✔
│     ├─ createExecutionContext_usesSuppliedExecutionContext() ✘ Cannot invoke "com.networknt.schema.ValidationContext.getMetaSchema()" because "validationContext" is null
│     ├─ createExecutionContext_validationContextReturnsNullConfig() ✘ Cannot invoke "com.networknt.schema.ValidationContext.getMetaSchema()" because "validationContext" is null
│     ├─ createExecutionContext_nullValidationContext() ✘ Cannot invoke "com.networknt.schema.ValidationContext.getMetaSchema()" because "validationContext" is null
│     ├─ createExecutionContext_supplierReturnsNull() ✘ Cannot invoke "com.networknt.schema.ValidationContext.getMetaSchema()" because "validationContext" is null
│     └─ createExecutionContext_supplierThrowsException() ✘ Cannot invoke "com.networknt.schema.ValidationContext.getMetaSchema()" because "validationContext" is null
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (5):
  JUnit Jupiter:JsonSchema_createExecutionContext_Test:createExecutionContext_usesSuppliedExecutionContext()
    MethodSource [className = 'com.networknt.schema.JsonSchema_createExecutionContext_Test', methodName = 'createExecutionContext_usesSuppliedExecutionContext', methodParameterTypes = '']
    => java.lang.NullPointerException: Cannot invoke "com.networknt.schema.ValidationContext.getMetaSchema()" because "validationContext" is null
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:110)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
       java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:500)
       java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:481)
       com.networknt.schema.JsonSchema_createExecutionContext_Test.createJsonSchema(JsonSchema_createExecutionContext_Test.java:106)
       com.networknt.schema.JsonSchema_createExecutionContext_Test.setupBeforeEach(JsonSchema_createExecutionContext_Test.java:96)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       [...]
  JUnit Jupiter:JsonSchema_createExecutionContext_Test:createExecutionContext_validationContextReturnsNullConfig()
    MethodSource [className = 'com.networknt.schema.JsonSchema_createExecutionContext_Test', methodName = 'createExecutionContext_validationContextReturnsNullConfig', methodParameterTypes = '']
    => java.lang.NullPointerException: Cannot invoke "com.networknt.schema.ValidationContext.getMetaSchema()" because "validationContext" is null
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:110)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
       java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:500)
       java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:481)
       com.networknt.schema.JsonSchema_createExecutionContext_Test.createJsonSchema(JsonSchema_createExecutionContext_Test.java:106)
       com.networknt.schema.JsonSchema_createExecutionContext_Test.setupBeforeEach(JsonSchema_createExecutionContext_Test.java:96)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       [...]
  JUnit Jupiter:JsonSchema_createExecutionContext_Test:createExecutionContext_nullValidationContext()
    MethodSource [className = 'com.networknt.schema.JsonSchema_createExecutionContext_Test', methodName = 'createExecutionContext_nullValidationContext', methodParameterTypes = '']
    => java.lang.NullPointerException: Cannot invoke "com.networknt.schema.ValidationContext.getMetaSchema()" because "validationContext" is null
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:110)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
       java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:500)
       java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:481)
       com.networknt.schema.JsonSchema_createExecutionContext_Test.createJsonSchema(JsonSchema_createExecutionContext_Test.java:106)
       com.networknt.schema.JsonSchema_createExecutionContext_Test.setupBeforeEach(JsonSchema_createExecutionContext_Test.java:96)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       [...]
  JUnit Jupiter:JsonSchema_createExecutionContext_Test:createExecutionContext_supplierReturnsNull()
    MethodSource [className = 'com.networknt.schema.JsonSchema_createExecutionContext_Test', methodName = 'createExecutionContext_supplierReturnsNull', methodParameterTypes = '']
    => java.lang.NullPointerException: Cannot invoke "com.networknt.schema.ValidationContext.getMetaSchema()" because "validationContext" is null
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:110)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
       java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:500)
       java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:481)
       com.networknt.schema.JsonSchema_createExecutionContext_Test.createJsonSchema(JsonSchema_createExecutionContext_Test.java:106)
       com.networknt.schema.JsonSchema_createExecutionContext_Test.setupBeforeEach(JsonSchema_createExecutionContext_Test.java:96)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       [...]
  JUnit Jupiter:JsonSchema_createExecutionContext_Test:createExecutionContext_supplierThrowsException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_createExecutionContext_Test', methodName = 'createExecutionContext_supplierThrowsException', methodParameterTypes = '']
    => java.lang.NullPointerException: Cannot invoke "com.networknt.schema.ValidationContext.getMetaSchema()" because "validationContext" is null
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:110)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
       java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:500)
       java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:481)
       com.networknt.schema.JsonSchema_createExecutionContext_Test.createJsonSchema(JsonSchema_createExecutionContext_Test.java:106)
       com.networknt.schema.JsonSchema_createExecutionContext_Test.setupBeforeEach(JsonSchema_createExecutionContext_Test.java:96)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       [...]

Test run finished after 47 ms
[         4 containers found      ]
[         0 containers skipped    ]
[         4 containers started    ]
[         0 containers aborted    ]
[         4 containers successful ]
[         0 containers failed     ]
[         5 tests found           ]
[         0 tests skipped         ]
[         5 tests started         ]
[         0 tests aborted         ]
[         0 tests successful      ]
[         5 tests failed          ]


```


And you can use the following related information to assist you in your analysis and correction:

api document of class com.networknt.schema.JsonSchema: 

 * This is the core of json constraint implementation. It parses json constraint
 * file and generates JsonValidators. The class is thread safe, once it is
 * constructed, it can be used to validate multiple json data concurrently.
 

api document of method createExecutionContext(): 

     * Creates an execution context.
     * 
     * @return the execution context
     

return type: 
com.networknt.schema.ExecutionContext

dependent classes: 
class `com.networknt.schema.BaseJsonValidator`:
	dependent fields:
		com.networknt.schema.ValidationContext com.networknt.schema.BaseJsonValidator.validationContext;
	
class `com.networknt.schema.JsonSchema`:
	related functions:
		method ` ValidationContext getValidationContext()` returns `com.networknt.schema.ValidationContext`, related with `com.networknt.schema.JsonSchema.createExecutionContext()`;
		method `private String getCustomMessage(JsonNode, String)` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.createExecutionContext()`;
		method `private ValidationResult validateAndCollect(ExecutionContext, JsonNode, JsonNode, String)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.createExecutionContext()`, api document: 
     * This method both validates and collects the data in a CollectorContext.
     * Unlike others this methods cleans and removes everything from collector
     * context before returning.
     * @param executionContext ExecutionContext
     * @param jsonNode JsonNode
     * @param rootNode JsonNode
     * @param at       String path
     *
     * @return ValidationResult
     ;
class `com.networknt.schema.ItemsValidator`:
	related functions:
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.createExecutionContext()`;
class `com.networknt.schema.PropertiesValidator`:
	related functions:
		method `private void walkSchema(ExecutionContext, Map.Entry<String, JsonSchema>, JsonNode, JsonNode, String, boolean, Set<ValidationMessage>, WalkListenerRunner)` returns `void`, related with `com.networknt.schema.JsonSchema.createExecutionContext()`;
class `com.networknt.schema.EnumValidator`:
	related functions:
		method `private boolean isTypeLooseContainsInEnum(JsonNode)` returns `boolean`, related with `com.networknt.schema.JsonSchema.createExecutionContext()`, api document: 
     * Check whether enum contains the value of the JsonNode if the typeLoose is enabled.
     *
     * @param node JsonNode to check
     ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
