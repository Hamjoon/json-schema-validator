
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
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.lang.reflect.Field;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getParentSchema_Test {

    // -----------------------------------------------------------------------
    @Mock
    private SchemaLocation mockSchemaLocation;

    @Mock
    private JsonNodePath mockEvaluationPath;

    @Mock
    private JsonNode mockSchemaNode;

    @Mock
    private ValidationContext mockValidationContext;

    @Mock
    private ValidatorTypeCode mockValidatorTypeCode;

    @Mock
    private JsonSchema mockParentSchema;

    private TestValidator validatorWithParent;

    private TestValidator validatorWithoutParent;

    // -----------------------------------------------------------------------
    private static final SchemaLocation DUMMY_SCHEMA_LOCATION = null;

    private static final JsonNodePath DUMMY_EVALUATION_PATH = null;

    private static final JsonNode DUMMY_SCHEMA_NODE = null;

    private static final ValidatorTypeCode DUMMY_VALIDATOR_TYPE = null;

    private static final ValidationContext DUMMY_VALIDATION_CONTEXT = null;

    // a non‑null JsonSchema instance used as the “parent” in the first validator
    private static final JsonSchema DUMMY_PARENT_SCHEMA = new JsonSchema(// ValidationContext
    null, // SchemaLocation
    null, // JsonNodePath
    null, // JsonNode (schema node)
    null, // parent JsonSchema
    null, // isRef
    false) {
    };

    // -----------------------------------------------------------------------
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        // -------------------------------------------------------------------
        // JsonValidator abstract methods – trivial implementations
        // -------------------------------------------------------------------
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath at) {
            return Collections.emptySet();
        }

        @Override
        public String getKeyword() {
            return null;
        }

        @Override
        public SchemaLocation getSchemaLocation() {
            return null;
        }

        @Override
        public JsonNodePath getEvaluationPath() {
            return null;
        }

        @Override
        public JsonSchema getEvaluationParentSchema() {
            return null;
        }

        // -------------------------------------------------------------------
        // Helper used by a test – we deliberately do **not** call the real
        // fetchSubSchemaNode implementation because it depends on a fully‑
        // fledged ValidationContext.  Returning null is sufficient for the
        // purpose of the test that verifies the parent schema is unchanged.
        // -------------------------------------------------------------------
        public JsonSchema invokeFetchSubSchemaNode() {
            // no‑op stub
            return null;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // validator with a supplied (non‑null) parent schema
        validatorWithParent = new TestValidator(DUMMY_SCHEMA_LOCATION, DUMMY_EVALUATION_PATH, DUMMY_SCHEMA_NODE, DUMMY_PARENT_SCHEMA, DUMMY_VALIDATOR_TYPE, DUMMY_VALIDATION_CONTEXT);
        // validator where the parent schema argument is null
        validatorWithoutParent = new TestValidator(DUMMY_SCHEMA_LOCATION, DUMMY_EVALUATION_PATH, DUMMY_SCHEMA_NODE, null, DUMMY_VALIDATOR_TYPE, DUMMY_VALIDATION_CONTEXT);
    }

    // -----------------------------------------------------------------------
    @Test
    void getParentSchema_returnsSuppliedParentSchema() {
        JsonSchema result = validatorWithParent.getParentSchema();
        assertNotNull(result, "Parent schema should not be null when supplied via constructor");
        assertSame(DUMMY_PARENT_SCHEMA, result, "The returned parent schema must be the exact instance supplied to the constructor");
    }

    @Test
    void getParentSchema_returnsNullWhenNoParentSupplied() {
        JsonSchema result = validatorWithoutParent.getParentSchema();
        assertNull(result, "Parent schema should be null when the constructor receives a null argument");
    }

    @Test
    @DisplayName("Consistency Across Calls – repeated getParentSchema() returns same instance")
    void getParentSchema_isConsistentAcrossMultipleCalls() {
        JsonSchema first = validatorWithParent.getParentSchema();
        JsonSchema second = validatorWithParent.getParentSchema();
        assertSame(first, second, "Multiple invocations must return the identical parent schema instance");
    }

    @Test
    @DisplayName("Different JsonSchema Implementations – custom instance is returned unchanged")
    void getParentSchema_returnsCustomJsonSchemaImplementation() {
        JsonSchema customSchema = new JsonSchema(null, null, null, null, null, false) {
        };
        TestValidator customValidator = new TestValidator(DUMMY_SCHEMA_LOCATION, DUMMY_EVALUATION_PATH, DUMMY_SCHEMA_NODE, customSchema, DUMMY_VALIDATOR_TYPE, DUMMY_VALIDATION_CONTEXT);
        JsonSchema result = customValidator.getParentSchema();
        assertSame(customSchema, result, "The getter must return the exact custom JsonSchema instance supplied at construction");
    }

    @Test
    @DisplayName("After Sub‑Schema Fetch – parent schema remains unchanged")
    void getParentSchema_unchangedAfterFetchSubSchemaNode() {
        // invoke the stub that does not touch the parent schema
        JsonSchema fetched = validatorWithParent.invokeFetchSubSchemaNode();
        // fetched value is irrelevant; we only verify the parent schema stayed the same
        JsonSchema after = validatorWithParent.getParentSchema();
        assertSame(DUMMY_PARENT_SCHEMA, after, "Parent schema must stay unchanged after invoking fetchSubSchemaNode()");
    }

    @Test
    @DisplayName("Thread‑Safety – concurrent calls return the same instance")
    void getParentSchema_isThreadSafe() throws Exception {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Callable<JsonSchema>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> validatorWithParent.getParentSchema());
        }
        List<Future<JsonSchema>> futures = executor.invokeAll(tasks);
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Executor did not terminate in time");
        List<JsonSchema> results = futures.stream().map(f -> {
            try {
                return f.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
        JsonSchema first = results.get(0);
        for (JsonSchema schema : results) {
            assertSame(first, schema, "All threads must receive the identical parent schema instance");
        }
    }

    @Test
    @DisplayName("Null After Reset – setter changes internal state to null")
    void getParentSchema_reflectsExplicitResetToNull() {
        // pre‑condition
        assertNotNull(validatorWithParent.getParentSchema(), "Parent schema should be non‑null before reset");
        // use reflection to bypass the final field restriction
        Field field = BaseJsonValidator.class.getDeclaredField("parentSchema");
        field.setAccessible(true);
        field.set(validatorWithParent, null);
        // verification
        assertNull(validatorWithParent.getParentSchema(), "After resetting the internal parentSchema to null via reflection, getter must return null");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/BaseJsonValidator_getParentSchema_Test.java:9: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/BaseJsonValidator_getParentSchema_Test.java:10: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/BaseJsonValidator_getParentSchema_Test.java:13: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/BaseJsonValidator_getParentSchema_Test.java:17: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_getParentSchema_Test
src/test/java/com/networknt/schema/BaseJsonValidator_getParentSchema_Test.java:20: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_getParentSchema_Test
src/test/java/com/networknt/schema/BaseJsonValidator_getParentSchema_Test.java:23: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_getParentSchema_Test
src/test/java/com/networknt/schema/BaseJsonValidator_getParentSchema_Test.java:26: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_getParentSchema_Test
src/test/java/com/networknt/schema/BaseJsonValidator_getParentSchema_Test.java:29: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_getParentSchema_Test
src/test/java/com/networknt/schema/BaseJsonValidator_getParentSchema_Test.java:32: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_getParentSchema_Test
src/test/java/com/networknt/schema/BaseJsonValidator_getParentSchema_Test.java:51: error: constructor JsonSchema in class JsonSchema cannot be applied to given types;
    private static final JsonSchema DUMMY_PARENT_SCHEMA = new JsonSchema(// ValidationContext
                                                          ^
  required: ValidationContext,SchemaLocation,JsonNodePath,JsonNode,JsonSchema,boolean
  found:    <null>,<null>,<null>,<null>,<null>,boolean
  reason: JsonSchema(ValidationContext,SchemaLocation,JsonNodePath,JsonNode,JsonSchema,boolean) has private access in JsonSchema
src/test/java/com/networknt/schema/BaseJsonValidator_getParentSchema_Test.java:119: error: cannot find symbol
        assertNotNull(result, "Parent schema should not be null when supplied via constructor");
        ^
  symbol:   method assertNotNull(JsonSchema,String)
  location: class BaseJsonValidator_getParentSchema_Test
src/test/java/com/networknt/schema/BaseJsonValidator_getParentSchema_Test.java:120: error: cannot find symbol
        assertSame(DUMMY_PARENT_SCHEMA, result, "The returned parent schema must be the exact instance supplied to the constructor");
        ^
  symbol:   method assertSame(JsonSchema,JsonSchema,String)
  location: class BaseJsonValidator_getParentSchema_Test
src/test/java/com/networknt/schema/BaseJsonValidator_getParentSchema_Test.java:126: error: cannot find symbol
        assertNull(result, "Parent schema should be null when the constructor receives a null argument");
        ^
  symbol:   method assertNull(JsonSchema,String)
  location: class BaseJsonValidator_getParentSchema_Test
src/test/java/com/networknt/schema/BaseJsonValidator_getParentSchema_Test.java:134: error: cannot find symbol
        assertSame(first, second, "Multiple invocations must return the identical parent schema instance");
        ^
  symbol:   method assertSame(JsonSchema,JsonSchema,String)
  location: class BaseJsonValidator_getParentSchema_Test
src/test/java/com/networknt/schema/BaseJsonValidator_getParentSchema_Test.java:140: error: constructor JsonSchema in class JsonSchema cannot be applied to given types;
        JsonSchema customSchema = new JsonSchema(null, null, null, null, null, false) {
                                  ^
  required: ValidationContext,SchemaLocation,JsonNodePath,JsonNode,JsonSchema,boolean
  found:    <null>,<null>,<null>,<null>,<null>,boolean
  reason: JsonSchema(ValidationContext,SchemaLocation,JsonNodePath,JsonNode,JsonSchema,boolean) has private access in JsonSchema
src/test/java/com/networknt/schema/BaseJsonValidator_getParentSchema_Test.java:144: error: cannot find symbol
        assertSame(customSchema, result, "The getter must return the exact custom JsonSchema instance supplied at construction");
        ^
  symbol:   method assertSame(JsonSchema,JsonSchema,String)
  location: class BaseJsonValidator_getParentSchema_Test
src/test/java/com/networknt/schema/BaseJsonValidator_getParentSchema_Test.java:154: error: cannot find symbol
        assertSame(DUMMY_PARENT_SCHEMA, after, "Parent schema must stay unchanged after invoking fetchSubSchemaNode()");
        ^
  symbol:   method assertSame(JsonSchema,JsonSchema,String)
  location: class BaseJsonValidator_getParentSchema_Test
src/test/java/com/networknt/schema/BaseJsonValidator_getParentSchema_Test.java:168: error: cannot find symbol
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Executor did not terminate in time");
        ^
  symbol:   method assertTrue(boolean,String)
  location: class BaseJsonValidator_getParentSchema_Test
src/test/java/com/networknt/schema/BaseJsonValidator_getParentSchema_Test.java:178: error: cannot find symbol
            assertSame(first, schema, "All threads must receive the identical parent schema instance");
            ^
  symbol:   method assertSame(JsonSchema,JsonSchema,String)
  location: class BaseJsonValidator_getParentSchema_Test
src/test/java/com/networknt/schema/BaseJsonValidator_getParentSchema_Test.java:186: error: cannot find symbol
        assertNotNull(validatorWithParent.getParentSchema(), "Parent schema should be non‑null before reset");
        ^
  symbol:   method assertNotNull(JsonSchema,String)
  location: class BaseJsonValidator_getParentSchema_Test
src/test/java/com/networknt/schema/BaseJsonValidator_getParentSchema_Test.java:192: error: cannot find symbol
        assertNull(validatorWithParent.getParentSchema(), "After resetting the internal parentSchema to null via reflection, getter must return null");
        ^
  symbol:   method assertNull(JsonSchema,String)
  location: class BaseJsonValidator_getParentSchema_Test
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
21 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.BaseJsonValidator': '\n * Base {@link JsonValidator}. \n ', 'api document of method getParentSchema()': '\n     * Gets the parent schema.\n     * <p>\n     * This is the lexical parent schema.\n     * \n     * @return the parent schema\n     ', 'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.ValidationMessageHandler`:\n\tdependent fields:\n\t\tcom.networknt.schema.JsonSchema com.networknt.schema.ValidationMessageHandler.parentSchema;\n\t\nclass `com.networknt.schema.RefValidator`:\n\trelated functions:\n\t\tmethod `private static String resolve(JsonSchema, String)` returns `java.lang.String`, related with `com.networknt.schema.BaseJsonValidator.getParentSchema()`;\nclass `com.networknt.schema.MinMaxContainsValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.BaseJsonValidator.getParentSchema()`;\nclass `com.networknt.schema.TypeValidator`:\n\trelated functions:\n\t\tmethod `public boolean equalsToSchemaType(JsonNode)` returns `boolean`, related with `com.networknt.schema.BaseJsonValidator.getParentSchema()`;\nclass `com.networknt.schema.ContentMediaTypeValidator`:\n\trelated functions:\n\t\tmethod `private boolean matches(String)` returns `boolean`, related with `com.networknt.schema.BaseJsonValidator.getParentSchema()`;\nclass `com.networknt.schema.AllOfValidator`:\n\trelated functions:\n\t\tmethod `protected Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.BaseJsonValidator.getParentSchema()`;\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public JsonSchema withConfig(SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.BaseJsonValidator.getParentSchema()`;\n\t\tmethod `public JsonSchema fromRef(JsonSchema, JsonNodePath)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.BaseJsonValidator.getParentSchema()`, api document: \n     * Creates a schema using the current one as a template with the parent as the\n     * ref.\n     * <p>\n     * This is typically used if this schema is a schema resource that can be\n     * pointed to by various references.\n     *\n     * @param refEvaluationParentSchema the parent ref\n     * @param refEvaluationPath the ref evaluation path\n     * @return the schema\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


