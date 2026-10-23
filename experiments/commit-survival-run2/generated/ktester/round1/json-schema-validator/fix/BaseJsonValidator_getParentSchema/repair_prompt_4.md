


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import com.fasterxml.jackson.databind.node.NullNode;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BaseJsonValidator_getParentSchema_Test {

    //     @Mock
    private JsonSchema mockParentSchema;

    //     @Mock
    private JsonNode mockSchemaNode;

    //     @Mock
    private ValidatorTypeCode mockValidatorType;

    //     @Mock
    private ValidationContext mockValidationContext;

    private TestValidator validatorWithParent;

    private TestValidator validatorWithoutParent;

    /**
     * Minimal concrete implementation of the abstract {@link BaseJsonValidator}.
     */
    private static final class TestValidator extends BaseJsonValidator {

        TestValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext, boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext, suppressSubSchemaRetrieval);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at) {
            // No validation logic needed for the getParentSchema() tests.
            return Collections.emptySet();
        }
    }

    /**
     * Simple mutable concrete subclass of {@link JsonSchema}.
     * The constructor mirrors the protected constructor used by the library;
     * all arguments are {@code null} because the internal state of the schema is
     * irrelevant for these tests.
     */
    private static final class MutableJsonSchema extends JsonSchema {

        private int mutableState = 0;

        MutableJsonSchema() {
            super(null, null, null, null, null);
        }

        void setState(int newState) {
            this.mutableState = newState;
        }

        int getState() {
            return this.mutableState;
        }
    }

    //     @BeforeEach
    void setupBeforeEach() {
        mockParentSchema = new MutableJsonSchema();
        mockSchemaNode = NullNode.getInstance();
        // any enum constant works
        mockValidatorType = ValidatorTypeCode.TYPE;
        // not needed for these tests
        mockValidationContext = null;
        validatorWithParent = new TestValidator("schemaPath", mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext, false);
        validatorWithoutParent = new TestValidator("schemaPath", mockSchemaNode, null, mockValidatorType, mockValidationContext, false);
    }

    /**
     * Provides the different {@link JsonSchema} instances for the parameterized test.
     */
    private static Stream<JsonSchema> parentSchemaProvider() {
        // 1. A mutable concrete subclass.
        MutableJsonSchema mutable = new MutableJsonSchema();
        mutable.setState(1);
        // 2. Another distinct instance (could also be a mutable one).
        JsonSchema another = new MutableJsonSchema();
        return Stream.of(mutable, another);
    }

    @Test
    void getParentSchema_returnsProvidedParent() {
        JsonSchema returned = validatorWithParent.getParentSchema();
        Assertions.assertNotNull(returned, "Parent schema should not be null when supplied in constructor");
        Assertions.assertSame(mockParentSchema, returned, "Returned parent schema must be the exact instance that was injected");
    }

    @Test
    void getParentSchema_returnsNullWhenNoParentSupplied() {
        JsonSchema returned = validatorWithoutParent.getParentSchema();
        Assertions.assertNull(returned, "Parent schema should be null when none is supplied in constructor");
    }

    /**
     * Supplies two different {@link JsonSchema} implementations:
     * <ul>
     *   <li>A mutable concrete subclass ({@link MutableJsonSchema}) whose internal
     *       state is changed after the validator is constructed.</li>
     *   <li>A Mockito‑generated proxy (simulated here by another concrete instance).</li>
     * </ul>
     * The test asserts that {@code getParentSchema()} always returns the original
     * reference, confirming that the method does not create a copy or otherwise
     * alter the supplied object.
     */
    @ParameterizedTest(name = "{index}: parentSchema={0}")
    @MethodSource("parentSchemaProvider")
    void getParentSchema_preservesReferenceIntegrity(JsonSchema injectedParent) {
        TestValidator validator = new TestValidator("schemaPath", mockSchemaNode, injectedParent, mockValidatorType, mockValidationContext, false);
        // Mutate the injected instance if it is our mutable subclass.
        if (injectedParent instanceof MutableJsonSchema mutable) {
            mutable.setState(42);
        }
        JsonSchema returned = validator.getParentSchema();
        Assertions.assertSame(injectedParent, returned, "The validator must return the exact same JsonSchema instance that was injected");
    }
}

```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

╷
├─ JUnit Jupiter ✔
│  └─ BaseJsonValidator_getParentSchema_Test ✔
│     ├─ getParentSchema_returnsNullWhenNoParentSupplied() ✘ Cannot invoke "com.networknt.schema.BaseJsonValidator_getParentSchema_Test$TestValidator.getParentSchema()" because "this.validatorWithoutParent" is null
│     ├─ getParentSchema_returnsProvidedParent() ✘ Cannot invoke "com.networknt.schema.BaseJsonValidator_getParentSchema_Test$TestValidator.getParentSchema()" because "this.validatorWithParent" is null
│     └─ getParentSchema_preservesReferenceIntegrity(JsonSchema) ✘ Cannot invoke "com.networknt.schema.ValidationContext.getMetaSchema()" because "validationContext" is null
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (3):
  JUnit Jupiter:BaseJsonValidator_getParentSchema_Test:getParentSchema_returnsNullWhenNoParentSupplied()
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_getParentSchema_Test', methodName = 'getParentSchema_returnsNullWhenNoParentSupplied', methodParameterTypes = '']
    => java.lang.NullPointerException: Cannot invoke "com.networknt.schema.BaseJsonValidator_getParentSchema_Test$TestValidator.getParentSchema()" because "this.validatorWithoutParent" is null
       com.networknt.schema.BaseJsonValidator_getParentSchema_Test.getParentSchema_returnsNullWhenNoParentSupplied(BaseJsonValidator_getParentSchema_Test.java:104)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       org.junit.jupiter.engine.extension.TimeoutExtension.intercept(TimeoutExtension.java:156)
       org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestableMethod(TimeoutExtension.java:147)
       [...]
  JUnit Jupiter:BaseJsonValidator_getParentSchema_Test:getParentSchema_returnsProvidedParent()
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_getParentSchema_Test', methodName = 'getParentSchema_returnsProvidedParent', methodParameterTypes = '']
    => java.lang.NullPointerException: Cannot invoke "com.networknt.schema.BaseJsonValidator_getParentSchema_Test$TestValidator.getParentSchema()" because "this.validatorWithParent" is null
       com.networknt.schema.BaseJsonValidator_getParentSchema_Test.getParentSchema_returnsProvidedParent(BaseJsonValidator_getParentSchema_Test.java:97)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       org.junit.jupiter.engine.extension.TimeoutExtension.intercept(TimeoutExtension.java:156)
       org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestableMethod(TimeoutExtension.java:147)
       [...]
  JUnit Jupiter:BaseJsonValidator_getParentSchema_Test:getParentSchema_preservesReferenceIntegrity(JsonSchema)
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_getParentSchema_Test', methodName = 'getParentSchema_preservesReferenceIntegrity', methodParameterTypes = 'com.networknt.schema.JsonSchema']
    => java.lang.NullPointerException: Cannot invoke "com.networknt.schema.ValidationContext.getMetaSchema()" because "validationContext" is null
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:110)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:91)
       com.networknt.schema.BaseJsonValidator_getParentSchema_Test$MutableJsonSchema.<init>(BaseJsonValidator_getParentSchema_Test.java:59)
       com.networknt.schema.BaseJsonValidator_getParentSchema_Test.parentSchemaProvider(BaseJsonValidator_getParentSchema_Test.java:88)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       [...]
       Suppressed: org.junit.platform.commons.PreconditionViolationException: Configuration error: You must configure at least one set of arguments for this @ParameterizedTest
         org.junit.platform.commons.util.Preconditions.condition(Preconditions.java:299)
         org.junit.jupiter.params.ParameterizedTestExtension.lambda$provideTestTemplateInvocationContexts$5(ParameterizedTestExtension.java:98)
         java.base/java.util.stream.AbstractPipeline.close(AbstractPipeline.java:323)
         java.base/java.util.stream.ReferencePipeline$7$1.accept(ReferencePipeline.java:273)
         [...]

Test run finished after 58 ms
[         5 containers found      ]
[         0 containers skipped    ]
[         5 containers started    ]
[         0 containers aborted    ]
[         4 containers successful ]
[         1 containers failed     ]
[         2 tests found           ]
[         0 tests skipped         ]
[         2 tests started         ]
[         0 tests aborted         ]
[         0 tests successful      ]
[         2 tests failed          ]


```


And you can use the following related information to assist you in your analysis and correction:

return type: 
com.networknt.schema.JsonSchema

dependent classes: 
class `com.networknt.schema.ValidationMessageHandler`:
	dependent fields:
		com.networknt.schema.JsonSchema com.networknt.schema.ValidationMessageHandler.parentSchema;
	related functions:
		method `private boolean isPartOfAnyOfMultipleType()` returns `boolean`, related with `com.networknt.schema.BaseJsonValidator.getParentSchema()`;
class `com.networknt.schema.BaseJsonValidator`:
	related functions:
		method `public JsonSchema getParentSchema()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.BaseJsonValidator.getParentSchema()`;
class `com.networknt.schema.MinMaxContainsValidator`:
	related functions:
		method `private void report(String, String)` returns `void`, related with `com.networknt.schema.BaseJsonValidator.getParentSchema()`;
class `com.networknt.schema.JsonSchema`:
	related functions:
		method `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, String, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.BaseJsonValidator.getParentSchema()`;
class `com.networknt.schema.AllOfValidator`:
	related functions:
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.BaseJsonValidator.getParentSchema()`;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
