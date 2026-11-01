
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.networknt.schema.i18n.MessageSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getEvaluationParentSchema_Test {

    // Mocks that are shared between tests
    @Mock
    ValidationContext validationContext;

    @Mock
    MessageSource messageSource;

    @Mock
    Keyword keyword;

    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNodePath evaluationPath;

    @Mock
    JsonNode schemaNode;

    /**
     * Concrete subclass of {@link BaseJsonValidator} that allows us to
     * control the behaviour of {@code getParentSchema()} and to count its
     * invocations.
     */
    private static class TestValidator extends BaseJsonValidator {

        private final JsonSchema parentSchemaToReturn;

        // may be RuntimeException or Error
        private final Throwable parentException;

        private int parentCallCount = 0;

        TestValidator(boolean suppressSubSchemaRetrieval, JsonNode schemaNode, ValidationContext validationContext, ErrorMessageType errorMessageType, String errorMessageKeyword, MessageSource messageSource, Keyword keyword, JsonSchema parentSchema, SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonSchema evaluationParentSchema, JsonSchema parentSchemaToReturn, Throwable parentException) {
            super(suppressSubSchemaRetrieval, schemaNode, validationContext, errorMessageType, errorMessageKeyword, messageSource, keyword, parentSchema, schemaLocation, evaluationPath, evaluationParentSchema, Collections.emptyMap());
            this.parentSchemaToReturn = parentSchemaToReturn;
            this.parentException = parentException;
        }

        @Override
        public JsonSchema getParentSchema() {
            parentCallCount++;
            if (parentException != null) {
                if (parentException instanceof RuntimeException) {
                    throw (RuntimeException) parentException;
                }
                if (parentException instanceof Error) {
                    throw (Error) parentException;
                }
                // wrap any other throwable
                throw new RuntimeException(parentException);
            }
            return parentSchemaToReturn;
        }

        int getParentCallCount() {
            return parentCallCount;
        }

        // -----------------------------------------------------------------
        // Minimal implementations required by the JsonValidator interface
        // -----------------------------------------------------------------
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath path) {
            return Collections.emptySet();
        }

        @Override
        public String getKeyword() {
            return "test-keyword";
        }

        @Override
        public JsonNodePath getEvaluationPath() {
            return null;
        }
    }

    // -----------------------------------------------------------------
    private static JsonSchema newSchema() {
        // JsonSchema(ValidationContext, SchemaLocation, JsonNodePath,
        //            JsonNode, JsonSchema, boolean)
        return new JsonSchema(null, null, null, null, null, false);
    }

    @Test
    void shouldReturnEvaluationParentSchemaWhenItIsSet() {
        // GIVEN an evaluationParentSchema is supplied
        JsonSchema evaluationParent = newSchema();
        // not used
        JsonSchema dummyParent = newSchema();
        TestValidator validator = new TestValidator(// suppressSubSchemaRetrieval
        false, // schemaNode
        null, // validationContext
        null, // errorMessageType
        null, // errorMessageKeyword
        null, // messageSource
        null, // keyword
        null, // parentSchema (won’t be used)
        null, // schemaLocation
        dummyParent, // evaluationPath
        null, // evaluationParentSchema
        null, // parentSchemaToReturn (unused)
        evaluationParent, // parentException
        null);
        // WHEN the method is invoked
        JsonSchema result = validator.getEvaluationParentSchema();
        // THEN it returns the supplied evaluationParentSchema
        assertSame(evaluationParent, result, "When evaluationParentSchema is non‑null, getEvaluationParentSchema() must return it");
    }

    @Test
    void shouldReturnParentSchemaWhenEvaluationParentSchemaIsNull() {
        // GIVEN no evaluationParentSchema (null) and a parent schema
        JsonSchema parent = newSchema();
        TestValidator validator = new TestValidator(false, null, null, null, null, null, null, // parentSchema (used as fallback)
        parent, // schemaLocation, evaluationPath
        null, // schemaLocation, evaluationPath
        null, // evaluationParentSchema is null
        null, // parentSchemaToReturn
        null, // no exception
        null);
        // WHEN the method is invoked
        JsonSchema result = validator.getEvaluationParentSchema();
        // THEN it falls back to the parent schema
        assertSame(parent, result, "When evaluationParentSchema is null, getEvaluationParentSchema() must return the parent schema");
    }

    @Test
    void shouldNotInvokeGetParentSchemaWhenEvaluationParentSchemaIsSet() {
        // GIVEN a validator with a non‑null evaluationParentSchema and a
        // getParentSchema() implementation that would throw if called
        JsonSchema evaluationParent = newSchema();
        TestValidator validator = new TestValidator(false, null, null, null, null, null, null, // parentSchema, schemaLocation, evaluationPath
        null, // parentSchema, schemaLocation, evaluationPath
        null, // parentSchema, schemaLocation, evaluationPath
        null, // evaluationParentSchema
        evaluationParent, // parentSchemaToReturn (unused)
        evaluationParent, // exception if called
        new AssertionError("getParentSchema should not be called"));
        // WHEN the method is invoked
        JsonSchema result = validator.getEvaluationParentSchema();
        // THEN no exception is thrown and the evaluationParent is returned
        assertSame(evaluationParent, result);
        // also verify that getParentSchema was not called
        assertEquals(0, validator.getParentCallCount(), "getParentSchema must not be invoked when evaluationParentSchema is present");
    }

    @Test
    void shouldInvokeGetParentSchemaExactlyOnceWhenEvaluationParentSchemaIsNull() {
        // GIVEN a validator with a mock parent schema
        JsonSchema parent = newSchema();
        TestValidator validator = new TestValidator(false, null, null, null, null, null, null, // parentSchema, schemaLocation, evaluationPath
        null, // parentSchema, schemaLocation, evaluationPath
        null, // parentSchema, schemaLocation, evaluationPath
        null, // evaluationParentSchema is null
        null, // parentSchemaToReturn
        parent, // no exception
        null);
        // WHEN the method is invoked
        validator.getEvaluationParentSchema();
        // THEN getParentSchema() is called exactly once
        assertEquals(1, validator.getParentCallCount(), "getParentSchema() should be invoked once when evaluationParentSchema is null");
    }

    // -----------------------------------------------------------------
    @Test
    void shouldReturnNullWhenBothEvaluationParentSchemaAndParentSchemaAreNull() {
        // GIVEN both evaluationParentSchema and the parent schema are null
        TestValidator validator = new TestValidator(false, null, null, null, null, null, null, // parentSchema, schemaLocation, evaluationPath
        null, // parentSchema, schemaLocation, evaluationPath
        null, // parentSchema, schemaLocation, evaluationPath
        null, // evaluationParentSchema
        null, // parentSchemaToReturn
        null, // no exception
        null);
        // WHEN the method is invoked
        JsonSchema result = validator.getEvaluationParentSchema();
        // THEN the result is null
        assertNull(result);
    }

    // -----------------------------------------------------------------
    @Test
    void shouldPropagateRuntimeExceptionWhenParentSchemaThrows() {
        // GIVEN a parent schema that throws a RuntimeException
        RuntimeException boom = new RuntimeException("boom");
        TestValidator validator = new TestValidator(false, null, null, null, null, null, null, // parentSchema, schemaLocation, evaluationPath
        null, // parentSchema, schemaLocation, evaluationPath
        null, // parentSchema, schemaLocation, evaluationPath
        null, // evaluationParentSchema
        null, // parentSchemaToReturn (unused)
        null, // exception to throw
        boom);
        // WHEN the method is invoked -> THEN the exception propagates
        RuntimeException ex = assertThrows(RuntimeException.class, validator::getEvaluationParentSchema);
        assertSame(boom, ex);
    }

    // -----------------------------------------------------------------
    @Test
    void shouldReturnEvaluationParentSchemaEvenIfParentSchemaThrows() {
        // GIVEN an evaluationParentSchema and a parent schema that would throw
        JsonSchema evaluationParent = newSchema();
        RuntimeException boom = new RuntimeException("boom");
        TestValidator validator = new TestValidator(false, null, null, null, null, null, null, // parentSchema, schemaLocation, evaluationPath
        null, // parentSchema, schemaLocation, evaluationPath
        null, // parentSchema, schemaLocation, evaluationPath
        null, // evaluationParentSchema is set
        evaluationParent, // parentSchemaToReturn (unused)
        null, // parent would throw if called
        boom);
        // WHEN the method is invoked
        JsonSchema result = validator.getEvaluationParentSchema();
        // THEN the evaluationParentSchema is returned and no exception is thrown
        assertSame(evaluationParent, result);
    }

    // -----------------------------------------------------------------
    @Test
    void shouldReturnSameInstanceWhenEvaluationParentSchemaAndParentSchemaAreIdentical() {
        // GIVEN the same mock instance used for both fields
        JsonSchema sameInstance = newSchema();
        TestValidator validator = new TestValidator(false, null, null, null, null, null, null, // parentSchema, schemaLocation, evaluationPath
        null, // parentSchema, schemaLocation, evaluationPath
        null, // parentSchema, schemaLocation, evaluationPath
        null, // evaluationParentSchema
        sameInstance, // parentSchemaToReturn
        sameInstance, // no exception
        null);
        // WHEN the method is invoked
        JsonSchema result = validator.getEvaluationParentSchema();
        // THEN the exact same instance is returned
        assertSame(sameInstance, result);
    }

    // -----------------------------------------------------------------
    @Test
    void shouldPreferEvaluationParentSchemaWhenItDiffersFromParentSchema() {
        // GIVEN different mock instances for evaluationParentSchema and parent
        JsonSchema evaluationParent = newSchema();
        JsonSchema parent = newSchema();
        TestValidator validator = new TestValidator(false, null, null, null, null, null, null, // parentSchema, schemaLocation, evaluationPath
        null, // parentSchema, schemaLocation, evaluationPath
        null, // parentSchema, schemaLocation, evaluationPath
        null, // evaluationParentSchema
        evaluationParent, // parentSchemaToReturn
        parent, // no exception
        null);
        // WHEN the method is invoked
        JsonSchema result = validator.getEvaluationParentSchema();
        // THEN the evaluationParentSchema is returned, ignoring the parent
        assertSame(evaluationParent, result);
    }

    // -----------------------------------------------------------------
    @Test
    void shouldPropagateIllegalArgumentExceptionFromParentSchema() {
        // GIVEN a parent schema that throws IllegalArgumentException
        IllegalArgumentException iae = new IllegalArgumentException("bad arg");
        TestValidator validator = new TestValidator(false, null, null, null, null, null, null, // parentSchema, schemaLocation, evaluationPath
        null, // parentSchema, schemaLocation, evaluationPath
        null, // parentSchema, schemaLocation, evaluationPath
        null, // evaluationParentSchema
        null, // parentSchemaToReturn
        null, // exception to throw
        iae);
        // WHEN the method is invoked -> THEN the same exception propagates
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, validator::getEvaluationParentSchema);
        assertSame(iae, ex);
    }

    // -----------------------------------------------------------------
    @Test
    void shouldPropagateErrorFromParentSchema() {
        // GIVEN a parent schema that throws an AssertionError (subclass of Error)
        AssertionError error = new AssertionError("assertion failed");
        TestValidator validator = new TestValidator(false, null, null, null, null, null, null, // parentSchema, schemaLocation, evaluationPath
        null, // parentSchema, schemaLocation, evaluationPath
        null, // parentSchema, schemaLocation, evaluationPath
        null, // evaluationParentSchema
        null, // parentSchemaToReturn
        null, // exception to throw
        error);
        // WHEN the method is invoked -> THEN the Error propagates unchanged
        AssertionError ex = assertThrows(AssertionError.class, validator::getEvaluationParentSchema);
        assertSame(error, ex);
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/BaseJsonValidator_getEvaluationParentSchema_Test.java:11: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/BaseJsonValidator_getEvaluationParentSchema_Test.java:12: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/BaseJsonValidator_getEvaluationParentSchema_Test.java:14: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/BaseJsonValidator_getEvaluationParentSchema_Test.java:18: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_getEvaluationParentSchema_Test
src/test/java/com/networknt/schema/BaseJsonValidator_getEvaluationParentSchema_Test.java:21: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_getEvaluationParentSchema_Test
src/test/java/com/networknt/schema/BaseJsonValidator_getEvaluationParentSchema_Test.java:24: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_getEvaluationParentSchema_Test
src/test/java/com/networknt/schema/BaseJsonValidator_getEvaluationParentSchema_Test.java:27: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_getEvaluationParentSchema_Test
src/test/java/com/networknt/schema/BaseJsonValidator_getEvaluationParentSchema_Test.java:30: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_getEvaluationParentSchema_Test
src/test/java/com/networknt/schema/BaseJsonValidator_getEvaluationParentSchema_Test.java:33: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_getEvaluationParentSchema_Test
src/test/java/com/networknt/schema/BaseJsonValidator_getEvaluationParentSchema_Test.java:99: error: constructor JsonSchema in class JsonSchema cannot be applied to given types;
        return new JsonSchema(null, null, null, null, null, false);
               ^
  required: ValidationContext,SchemaLocation,JsonNodePath,JsonNode,JsonSchema,boolean
  found:    <null>,<null>,<null>,<null>,<null>,boolean
  reason: JsonSchema(ValidationContext,SchemaLocation,JsonNodePath,JsonNode,JsonSchema,boolean) has private access in JsonSchema
src/test/java/com/networknt/schema/BaseJsonValidator_getEvaluationParentSchema_Test.java:117: error: incompatible types: JsonSchema cannot be converted to SchemaLocation
        dummyParent, // evaluationPath
        ^
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
11 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.BaseJsonValidator': '\n * Base {@link JsonValidator}. \n ', 'api document of method getEvaluationParentSchema()': '\n     * Gets the evaluation parent schema.\n     * <p>\n     * This is the dynamic parent schema when following references.\n     * \n     * @see JsonSchema#fromRef(JsonSchema, JsonNodePath)\n     * @return the evaluation parent schema\n     ', 'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.ValidationMessageHandler`:\n\tdependent fields:\n\t\tcom.networknt.schema.JsonSchema com.networknt.schema.ValidationMessageHandler.evaluationParentSchema;\n\trelated functions:\n\t\tmethod `protected JsonNode getMessageNode(String, JsonNode, JsonSchema, String)` returns `com.fasterxml.jackson.databind.JsonNode`, related with `com.networknt.schema.BaseJsonValidator.getEvaluationParentSchema()`;\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public JsonSchema withConfig(SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.BaseJsonValidator.getEvaluationParentSchema()`;\n\t\tmethod `public JsonSchema findLexicalRoot()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.BaseJsonValidator.getEvaluationParentSchema()`;\n\t\tmethod `public boolean isSchemaResourceRoot()` returns `boolean`, related with `com.networknt.schema.BaseJsonValidator.getEvaluationParentSchema()`, api document: \n     * Determines if this schema resource is a schema resource root.\n     * <p>\n     * This is either the schema document root or the subschema resource root.\n     *\n     * @return if this schema is a schema resource root\n     ;\nclass `com.networknt.schema.BaseJsonValidator`:\n\trelated functions:\n\t\tmethod `protected String getNodeFieldType()` returns `java.lang.String`, related with `com.networknt.schema.BaseJsonValidator.getEvaluationParentSchema()`;\nclass `com.networknt.schema.utils.JsonNodeUtil`:\n\trelated functions:\n\t\tmethod `public static boolean equalsToSchemaType(JsonNode, JsonType, JsonSchema, ValidationContext)` returns `boolean`, related with `com.networknt.schema.BaseJsonValidator.getEvaluationParentSchema()`;\nclass `com.networknt.schema.OneOfValidator`:\n\trelated functions:\n\t\tmethod `protected Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.BaseJsonValidator.getEvaluationParentSchema()`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


