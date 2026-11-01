
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("JsonSchema.getValidationContext tests")
class JsonSchema_getValidationContext_Test {
    @Mock
    private ValidationContext validationContextMock;

    @Mock
    private JsonMetaSchema metaSchemaMock;

    @Mock
    private JsonNode schemaNodeMock;

    @Mock
    private JsonNodePath evaluationPathMock;

    private JsonSchema jsonSchema;

    // simple placeholder – constructor tolerates nulls
    private SchemaLocation dummyLocation;

    /**
     * a simple {@link ValidationContext} instance created with null meta‑schema and factory
     */
    private ValidationContext validationContext;

    class MyValidationContext extends ValidationContext {
        MyValidationContext(JsonMetaSchema meta, SchemaValidatorsConfig cfg) {
            super(meta, null, cfg);
        }

        @Override
        public SchemaValidatorsConfig getConfig() {
            return new SchemaValidatorsConfig();
        }
    }

    /**
     * Simple concrete subclass used in a few tests.
     */
    @BeforeEach
    void setupBeforeEach() {
        // ----- stub behaviour of ValidationContext and its collaborators -----
        // meta‑schema related calls
        when(validationContextMock.getMetaSchema()).thenReturn(metaSchemaMock);
        when(metaSchemaMock.readAnchor(any())).thenReturn(null);
        when(metaSchemaMock.readDynamicAnchor(any())).thenReturn(null);
        // map getters – use real concurrent maps to avoid NPEs on putIfAbsent
        when(validationContextMock.getSchemaResources()).thenReturn(new ConcurrentHashMap<>());
        when(validationContextMock.getDynamicAnchors()).thenReturn(new ConcurrentHashMap<>());
        when(validationContextMock.getSchemaReferences()).thenReturn(new ConcurrentHashMap<>());
        // other methods used in the constructor
        when(validationContextMock.resolveSchemaId(any())).thenReturn(null);
        // ----- create a JsonSchema instance via the package‑private factory method -----
        // SchemaLocation can be constructed with null arguments (the constructor does not enforce non‑null)
        SchemaLocation schemaLocation = new SchemaLocation(null, null);
        // parent schema – none for this test
        jsonSchema = JsonSchema.from(validationContextMock, schemaLocation, evaluationPathMock, schemaNodeMock, null, false);
        // first two arguments because the tests never exercise those parts.
        validationContext = new ValidationContext(null, null, new SchemaValidatorsConfig());
        // Create the primary JsonSchema instance with the real validationContext.
        jsonSchema = createJsonSchema(validationContext);
    }

    /**
     * Helper that creates a {@link JsonSchema} instance via reflection.
     * The constructor of {@code JsonSchema} is package‑private and expects many
     * arguments; we bypass this by passing {@code null} for all parameters
     * and then manually inject the desired {@code ValidationContext}.
     *
     * @param ctx
     * 		the {@code ValidationContext} to associate with the schema
     * @return a new {@code JsonSchema} instance
     * @throws Exception
     * 		if reflection fails
     */
    private JsonSchema createJsonSchema(Object ctx) throws Exception {
        // Find any constructor (there is at least one) and make it accessible.
        Constructor<?> ctor = JsonSchema.class.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        // Fill the argument array with nulls – the constructor tolerates them.
        Object[] args = new Object[ctor.getParameterCount()];
        JsonSchema instance = ((JsonSchema) (ctor.newInstance(args)));
        // Inject the validationContext field.
        Field f = JsonSchema.class.getDeclaredField("validationContext");
        f.setAccessible(true);
        f.set(instance, ctx);
        return instance;
    }

    @Test
    void getValidationContext_returnsSameInstance() {
        ValidationContext returned = jsonSchema.getValidationContext();
        assertSame(validationContext, returned, "JsonSchema#getValidationContext should return the ValidationContext supplied at construction");
    }

    @Test
    void getValidationContext_multipleCalls_returnSameInstance() {
        ValidationContext first = jsonSchema.getValidationContext();
        ValidationContext second = jsonSchema.getValidationContext();
        assertSame(first, second, "Multiple invocations of getValidationContext should return the identical instance");
    }

    @Test
    void getValidationContext_whenContextIsNull_returnsNull() throws Exception {
        // Build a schema where the internal field is deliberately set to null.
        JsonSchema schemaWithNullContext = createJsonSchema(null);
        ValidationContext ctx = schemaWithNullContext.getValidationContext();
        assertNull(ctx, "When constructed with a null ValidationContext the getter should return null");
    }

    @Test
    void childSchema_inheritsValidationContextFromParent() {
        // Create a child schema with a null context.
        JsonSchema child = createJsonSchema(null);
        // The JsonSchema class stores its parent in a field named “parentSchema”.
        // If the field name changes in the library, this reflection will need to be updated.
        try {
            Field parentField = JsonSchema.class.getDeclaredField("parentSchema");
            parentField.setAccessible(true);
            parentField.set(child, jsonSchema);
        } catch (NoSuchFieldException e) {
            // If the field does not exist, the fallback behaviour is not testable.
            // In that case we simply fail the test.
            fail("Unable to locate parentSchema field in JsonSchema: " + e.getMessage());
        }
        assertSame(validationContext, child.getValidationContext(), "A child JsonSchema created with a null context should inherit the parent's ValidationContext");
    }

    @Test
    void withConfig_retainsOriginalValidationContext() {
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        JsonSchema configured = jsonSchema.withConfig(cfg);
        assertSame(validationContext, configured.getValidationContext(), "withConfig must keep the original ValidationContext instance");
    }

    @Test
    void getValidationContext_identityCheck() {
        ValidationContext ctx = jsonSchema.getValidationContext();
        assertTrue(ctx == validationContext, "The returned ValidationContext must be the exact same object (==) as supplied");
    }

    @Test
    void customValidationContextSubclass_isPreserved() {
        // Build a real (non‑mock) ValidationContext subclass instance.
        class MyValidationContext extends ValidationContext {
            MyValidationContext(JsonMetaSchema meta, SchemaValidatorsConfig cfg) {
                super(meta, null, cfg);
            }

            @Override
            public SchemaValidatorsConfig getConfig() {
                return new SchemaValidatorsConfig();
            }
        }
        MyValidationContext customCtx = new MyValidationContext(null, new SchemaValidatorsConfig());
        JsonSchema schema = createJsonSchema(customCtx);
        ValidationContext returned = schema.getValidationContext();
        assertSame(customCtx, returned, "The getter must return the exact custom ValidationContext instance supplied");
        assertTrue(returned instanceof MyValidationContext, "The concrete type of the returned context should be the custom subclass");
    }

    @Test
    void fromRef_retainsOriginalValidationContext() {
        // The fromRef method expects a JsonNodePath; passing null is acceptable for this test.
        JsonSchema refSchema = jsonSchema.fromRef(jsonSchema, null);
        assertSame(validationContext, refSchema.getValidationContext(), "A schema obtained via fromRef should retain the original ValidationContext");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:1: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
 ^
  symbol: class ExtendWith
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:2: error: cannot find symbol
@DisplayName("JsonSchema.getValidationContext tests")
 ^
  symbol: class DisplayName
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:5: error: cannot find symbol
    private ValidationContext validationContextMock;
            ^
  symbol:   class ValidationContext
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:8: error: cannot find symbol
    private JsonMetaSchema metaSchemaMock;
            ^
  symbol:   class JsonMetaSchema
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:11: error: cannot find symbol
    private JsonNode schemaNodeMock;
            ^
  symbol:   class JsonNode
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:14: error: cannot find symbol
    private JsonNodePath evaluationPathMock;
            ^
  symbol:   class JsonNodePath
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:16: error: cannot find symbol
    private JsonSchema jsonSchema;
            ^
  symbol:   class JsonSchema
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:19: error: cannot find symbol
    private SchemaLocation dummyLocation;
            ^
  symbol:   class SchemaLocation
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:24: error: cannot find symbol
    private ValidationContext validationContext;
            ^
  symbol:   class ValidationContext
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:76: error: cannot find symbol
    private JsonSchema createJsonSchema(Object ctx) throws Exception {
            ^
  symbol:   class JsonSchema
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:26: error: cannot find symbol
    class MyValidationContext extends ValidationContext {
                                      ^
  symbol:   class ValidationContext
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:27: error: cannot find symbol
        MyValidationContext(JsonMetaSchema meta, SchemaValidatorsConfig cfg) {
                            ^
  symbol:   class JsonMetaSchema
  location: class JsonSchema_getValidationContext_Test.MyValidationContext
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:27: error: cannot find symbol
        MyValidationContext(JsonMetaSchema meta, SchemaValidatorsConfig cfg) {
                                                 ^
  symbol:   class SchemaValidatorsConfig
  location: class JsonSchema_getValidationContext_Test.MyValidationContext
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:32: error: cannot find symbol
        public SchemaValidatorsConfig getConfig() {
               ^
  symbol:   class SchemaValidatorsConfig
  location: class JsonSchema_getValidationContext_Test.MyValidationContext
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:1: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:4: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:7: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:10: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:13: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:40: error: cannot find symbol
    @BeforeEach
     ^
  symbol:   class BeforeEach
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:90: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:96: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:103: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:111: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:129: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:136: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:142: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:162: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:31: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:33: error: cannot find symbol
            return new SchemaValidatorsConfig();
                       ^
  symbol:   class SchemaValidatorsConfig
  location: class JsonSchema_getValidationContext_Test.MyValidationContext
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:45: error: cannot find symbol
        when(metaSchemaMock.readAnchor(any())).thenReturn(null);
                                       ^
  symbol:   method any()
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:46: error: cannot find symbol
        when(metaSchemaMock.readDynamicAnchor(any())).thenReturn(null);
                                              ^
  symbol:   method any()
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:48: error: cannot find symbol
        when(validationContextMock.getSchemaResources()).thenReturn(new ConcurrentHashMap<>());
                                                                        ^
  symbol:   class ConcurrentHashMap
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:49: error: cannot find symbol
        when(validationContextMock.getDynamicAnchors()).thenReturn(new ConcurrentHashMap<>());
                                                                       ^
  symbol:   class ConcurrentHashMap
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:50: error: cannot find symbol
        when(validationContextMock.getSchemaReferences()).thenReturn(new ConcurrentHashMap<>());
                                                                         ^
  symbol:   class ConcurrentHashMap
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:52: error: cannot find symbol
        when(validationContextMock.resolveSchemaId(any())).thenReturn(null);
                                                   ^
  symbol:   method any()
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:55: error: cannot find symbol
        SchemaLocation schemaLocation = new SchemaLocation(null, null);
        ^
  symbol:   class SchemaLocation
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:55: error: cannot find symbol
        SchemaLocation schemaLocation = new SchemaLocation(null, null);
                                            ^
  symbol:   class SchemaLocation
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:57: error: cannot find symbol
        jsonSchema = JsonSchema.from(validationContextMock, schemaLocation, evaluationPathMock, schemaNodeMock, null, false);
                     ^
  symbol:   variable JsonSchema
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:59: error: cannot find symbol
        validationContext = new ValidationContext(null, null, new SchemaValidatorsConfig());
                                ^
  symbol:   class ValidationContext
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:59: error: cannot find symbol
        validationContext = new ValidationContext(null, null, new SchemaValidatorsConfig());
                                                                  ^
  symbol:   class SchemaValidatorsConfig
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:78: error: cannot find symbol
        Constructor<?> ctor = JsonSchema.class.getDeclaredConstructors()[0];
        ^
  symbol:   class Constructor
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:78: error: cannot find symbol
        Constructor<?> ctor = JsonSchema.class.getDeclaredConstructors()[0];
                              ^
  symbol:   class JsonSchema
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:82: error: cannot find symbol
        JsonSchema instance = ((JsonSchema) (ctor.newInstance(args)));
        ^
  symbol:   class JsonSchema
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:82: error: cannot find symbol
        JsonSchema instance = ((JsonSchema) (ctor.newInstance(args)));
                                ^
  symbol:   class JsonSchema
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:84: error: cannot find symbol
        Field f = JsonSchema.class.getDeclaredField("validationContext");
        ^
  symbol:   class Field
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:84: error: cannot find symbol
        Field f = JsonSchema.class.getDeclaredField("validationContext");
                  ^
  symbol:   class JsonSchema
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:92: error: cannot find symbol
        ValidationContext returned = jsonSchema.getValidationContext();
        ^
  symbol:   class ValidationContext
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:98: error: cannot find symbol
        ValidationContext first = jsonSchema.getValidationContext();
        ^
  symbol:   class ValidationContext
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:99: error: cannot find symbol
        ValidationContext second = jsonSchema.getValidationContext();
        ^
  symbol:   class ValidationContext
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:106: error: cannot find symbol
        JsonSchema schemaWithNullContext = createJsonSchema(null);
        ^
  symbol:   class JsonSchema
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:107: error: cannot find symbol
        ValidationContext ctx = schemaWithNullContext.getValidationContext();
        ^
  symbol:   class ValidationContext
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:114: error: cannot find symbol
        JsonSchema child = createJsonSchema(null);
        ^
  symbol:   class JsonSchema
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:118: error: cannot find symbol
            Field parentField = JsonSchema.class.getDeclaredField("parentSchema");
            ^
  symbol:   class Field
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:118: error: cannot find symbol
            Field parentField = JsonSchema.class.getDeclaredField("parentSchema");
                                ^
  symbol:   class JsonSchema
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:124: error: cannot find symbol
            fail("Unable to locate parentSchema field in JsonSchema: " + e.getMessage());
            ^
  symbol:   method fail(String)
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:131: error: cannot find symbol
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        ^
  symbol:   class SchemaValidatorsConfig
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:131: error: cannot find symbol
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
                                         ^
  symbol:   class SchemaValidatorsConfig
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:132: error: cannot find symbol
        JsonSchema configured = jsonSchema.withConfig(cfg);
        ^
  symbol:   class JsonSchema
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:138: error: cannot find symbol
        ValidationContext ctx = jsonSchema.getValidationContext();
        ^
  symbol:   class ValidationContext
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:145: error: cannot find symbol
        class MyValidationContext extends ValidationContext {
                                          ^
  symbol:   class ValidationContext
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:146: error: cannot find symbol
            MyValidationContext(JsonMetaSchema meta, SchemaValidatorsConfig cfg) {
                                ^
  symbol:   class JsonMetaSchema
  location: class MyValidationContext
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:146: error: cannot find symbol
            MyValidationContext(JsonMetaSchema meta, SchemaValidatorsConfig cfg) {
                                                     ^
  symbol:   class SchemaValidatorsConfig
  location: class MyValidationContext
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:151: error: cannot find symbol
            public SchemaValidatorsConfig getConfig() {
                   ^
  symbol:   class SchemaValidatorsConfig
  location: class MyValidationContext
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:150: error: method does not override or implement a method from a supertype
            @Override
            ^
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:152: error: cannot find symbol
                return new SchemaValidatorsConfig();
                           ^
  symbol:   class SchemaValidatorsConfig
  location: class MyValidationContext
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:155: error: cannot find symbol
        MyValidationContext customCtx = new MyValidationContext(null, new SchemaValidatorsConfig());
                                                                          ^
  symbol:   class SchemaValidatorsConfig
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:156: error: cannot find symbol
        JsonSchema schema = createJsonSchema(customCtx);
        ^
  symbol:   class JsonSchema
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:157: error: cannot find symbol
        ValidationContext returned = schema.getValidationContext();
        ^
  symbol:   class ValidationContext
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:159: error: cannot find symbol
        assertTrue(returned instanceof MyValidationContext, "The concrete type of the returned context should be the custom subclass");
        ^
  symbol:   method assertTrue(boolean,String)
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:165: error: cannot find symbol
        JsonSchema refSchema = jsonSchema.fromRef(jsonSchema, null);
        ^
  symbol:   class JsonSchema
  location: class JsonSchema_getValidationContext_Test
71 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'return type': 'com.networknt.schema.ValidationContext', 'dependent classes': 'class `com.networknt.schema.BaseJsonValidator`:\n\tdependent fields:\n\t\tcom.networknt.schema.ValidationContext com.networknt.schema.BaseJsonValidator.validationContext;\n\t\nclass `com.networknt.schema.MultipleOfValidator`:\n\trelated functions:\n\t\tmethod `protected BigDecimal getDividend(JsonNode)` returns `java.math.BigDecimal`, related with `com.networknt.schema.JsonSchema.getValidationContext()`, api document: \n     * Gets the dividend to use.\n     * \n     * @param node the node\n     * @return the dividend or null if the type is incorrect\n     ;\nclass `com.networknt.schema.FormatValidator`:\n\trelated functions:\n\t\tmethod `protected boolean isStrict(ExecutionContext)` returns `boolean`, related with `com.networknt.schema.JsonSchema.getValidationContext()`, api document: \r\n     * Determines if strict handling.\r\n     * <p>\r\n     * Note that this defaults to false.\r\n     * \r\n     * @param executionContext the execution context\r\n     * @return whether to perform strict handling\r\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


