


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.lang.reflect.Field;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;

// @ExtendWith(MockitoExtension.class)
class BaseJsonValidator_fetchSubSchemaNode_Test {

//     @Mock
    private JsonNode mockSchemaNode;

//     @Mock
    private JsonSchema mockParentSchema;

//     @Mock
    private ValidatorTypeCode mockValidatorType;

//     @Mock
    private ValidationContext mockValidationContext;

    /**
     * Minimal concrete implementation of the abstract {@link BaseJsonValidator}.
     * The {@code validate} method required by {@link JsonValidator} simply
     * returns an empty set – validation itself is not relevant for these
     * tests.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext, boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext, suppressSubSchemaRetrieval);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at) {
            // Validation is out of scope for these tests.
            return Collections.emptySet();
        }
    }

    /* -------------------------------------------------------------
     * Helper to set the protected field {@code schemaNode} via reflection.
     * ------------------------------------------------------------- */
    private static void setSchemaNode(BaseJsonValidator validator, JsonNode node) {
        try {
            Field f = BaseJsonValidator.class.getDeclaredField("schemaNode");
            f.setAccessible(true);
            f.set(validator, node);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* -------------------------------------------------------------
     * Helper to set the protected field {@code suppressSubSchemaRetrieval}
     * via reflection (used only for sanity‑checking that the constructor
     * really stores the value we pass).
     * ------------------------------------------------------------- */
    private static void setSuppressFlag(BaseJsonValidator validator, boolean flag) {
        try {
            Field f = BaseJsonValidator.class.getDeclaredField("suppressSubSchemaRetrieval");
            f.setAccessible(true);
            f.setBoolean(validator, flag);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a {@link ValidationContext} instance with all factories set to {@code null}.
     */
    private static ValidationContext createEmptyValidationContext() {
        // The constructor of ValidationContext accepts the configuration objects.
        // Supplying {@code null} for all of them is sufficient for the
        // {@code fetchSubSchemaNode} implementation used in these tests.
        return new ValidationContext(null, null, null, null, null);
    }

    @Test
    void fetchSubSchemaNode_returnsNullWhenSuppressed() {
        // Arrange – validator with suppression enabled.
        TestValidator validator = new TestValidator("dummyPath", JsonNodeFactory.instance.objectNode(), null, null, createEmptyValidationContext(), true);
        // Act
        JsonSchema result = validator.fetchSubSchemaNode(createEmptyValidationContext());
        // Assert
        assertNull(result, "When sub‑schema retrieval is suppressed the method must return null");
    }

    @Test
    void fetchSubSchemaNode_delegatesWhenNotSuppressed() {
        // Arrange – validator with suppression disabled.
        TestValidator validator = new TestValidator("dummyPath", JsonNodeFactory.instance.objectNode(), null, null, createEmptyValidationContext(), false);
        // Act
        JsonSchema result = validator.fetchSubSchemaNode(createEmptyValidationContext());
        // Assert
        assertNotNull(result, "When sub‑schema retrieval is not suppressed a JsonSchema should be returned");
    }

    /* -------------------------------------------------------------
     * 1. suppressSubSchemaRetrieval = true – always returns null.
     * ------------------------------------------------------------- */
    @Test
    void fetchSubSchemaNode_returnsNullWhenSuppressed_andNonNullContext() {
        TestValidator validator = new TestValidator("unusedPath", JsonNodeFactory.instance.objectNode(), null, null, createEmptyValidationContext(), true);
        JsonSchema result = validator.fetchSubSchemaNode(createEmptyValidationContext());
        assertNull(result, "With suppressSubSchemaRetrieval=true the method must return null");
    }

    @Test
    void fetchSubSchemaNode_returnsNullWhenSuppressed_andNullContext() {
        TestValidator validator = new TestValidator("unusedPath", JsonNodeFactory.instance.objectNode(), null, null, null, true);
        JsonSchema result = validator.fetchSubSchemaNode(null);
        assertNull(result, "Even with a null ValidationContext the method must return null");
    }

    /* -------------------------------------------------------------
     * 2. suppressSubSchemaRetrieval = false – normal delegation.
     * ------------------------------------------------------------- */
    @Test
    void fetchSubSchemaNode_delegatesWhenNotSuppressed_andValidSchemaNode() {
        // Arrange – validator that does NOT suppress retrieval and has a non‑null schema node.
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        // (optional) put a dummy property so the node is not completely empty
        schemaNode.put("type", "object");
        TestValidator validator = new TestValidator("unusedPath", schemaNode, null, null, createEmptyValidationContext(), false);
        // Act
        JsonSchema result = validator.fetchSubSchemaNode(createEmptyValidationContext());
        // Assert
        assertNotNull(result, "When not suppressed a JsonSchema instance should be returned");
    }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

╷
├─ JUnit Jupiter ✔
│  └─ BaseJsonValidator_fetchSubSchemaNode_Test ✔
│     ├─ fetchSubSchemaNode_returnsNullWhenSuppressed() ✘ URIFactory must not be null
│     ├─ fetchSubSchemaNode_returnsNullWhenSuppressed_andNullContext() ✔
│     ├─ fetchSubSchemaNode_delegatesWhenNotSuppressed_andValidSchemaNode() ✘ URIFactory must not be null
│     ├─ fetchSubSchemaNode_returnsNullWhenSuppressed_andNonNullContext() ✘ URIFactory must not be null
│     └─ fetchSubSchemaNode_delegatesWhenNotSuppressed() ✘ URIFactory must not be null
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (4):
  JUnit Jupiter:BaseJsonValidator_fetchSubSchemaNode_Test:fetchSubSchemaNode_returnsNullWhenSuppressed()
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_fetchSubSchemaNode_Test', methodName = 'fetchSubSchemaNode_returnsNullWhenSuppressed', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: URIFactory must not be null
       com.networknt.schema.ValidationContext.<init>(ValidationContext.java:42)
       com.networknt.schema.BaseJsonValidator_fetchSubSchemaNode_Test.createEmptyValidationContext(BaseJsonValidator_fetchSubSchemaNode_Test.java:81)
       com.networknt.schema.BaseJsonValidator_fetchSubSchemaNode_Test.fetchSubSchemaNode_returnsNullWhenSuppressed(BaseJsonValidator_fetchSubSchemaNode_Test.java:87)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]
  JUnit Jupiter:BaseJsonValidator_fetchSubSchemaNode_Test:fetchSubSchemaNode_delegatesWhenNotSuppressed_andValidSchemaNode()
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_fetchSubSchemaNode_Test', methodName = 'fetchSubSchemaNode_delegatesWhenNotSuppressed_andValidSchemaNode', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: URIFactory must not be null
       com.networknt.schema.ValidationContext.<init>(ValidationContext.java:42)
       com.networknt.schema.BaseJsonValidator_fetchSubSchemaNode_Test.createEmptyValidationContext(BaseJsonValidator_fetchSubSchemaNode_Test.java:81)
       com.networknt.schema.BaseJsonValidator_fetchSubSchemaNode_Test.fetchSubSchemaNode_delegatesWhenNotSuppressed_andValidSchemaNode(BaseJsonValidator_fetchSubSchemaNode_Test.java:130)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]
  JUnit Jupiter:BaseJsonValidator_fetchSubSchemaNode_Test:fetchSubSchemaNode_returnsNullWhenSuppressed_andNonNullContext()
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_fetchSubSchemaNode_Test', methodName = 'fetchSubSchemaNode_returnsNullWhenSuppressed_andNonNullContext', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: URIFactory must not be null
       com.networknt.schema.ValidationContext.<init>(ValidationContext.java:42)
       com.networknt.schema.BaseJsonValidator_fetchSubSchemaNode_Test.createEmptyValidationContext(BaseJsonValidator_fetchSubSchemaNode_Test.java:81)
       com.networknt.schema.BaseJsonValidator_fetchSubSchemaNode_Test.fetchSubSchemaNode_returnsNullWhenSuppressed_andNonNullContext(BaseJsonValidator_fetchSubSchemaNode_Test.java:109)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]
  JUnit Jupiter:BaseJsonValidator_fetchSubSchemaNode_Test:fetchSubSchemaNode_delegatesWhenNotSuppressed()
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_fetchSubSchemaNode_Test', methodName = 'fetchSubSchemaNode_delegatesWhenNotSuppressed', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: URIFactory must not be null
       com.networknt.schema.ValidationContext.<init>(ValidationContext.java:42)
       com.networknt.schema.BaseJsonValidator_fetchSubSchemaNode_Test.createEmptyValidationContext(BaseJsonValidator_fetchSubSchemaNode_Test.java:81)
       com.networknt.schema.BaseJsonValidator_fetchSubSchemaNode_Test.fetchSubSchemaNode_delegatesWhenNotSuppressed(BaseJsonValidator_fetchSubSchemaNode_Test.java:97)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]

Test run finished after 42 ms
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
[         1 tests successful      ]
[         4 tests failed          ]


```


And you can use the following related information to assist you in your analysis and correction:

return type: 
com.networknt.schema.JsonSchema

dependent classes: 
class `com.networknt.schema.BaseJsonValidator`:
	dependent fields:
		com.fasterxml.jackson.databind.JsonNode com.networknt.schema.BaseJsonValidator.schemaNode;
		boolean com.networknt.schema.BaseJsonValidator.suppressSubSchemaRetrieval;
	related functions:
		method `public JsonNode getSchemaNode()` returns `com.fasterxml.jackson.databind.JsonNode`, related with `com.networknt.schema.BaseJsonValidator.fetchSubSchemaNode(ValidationContext)`;
		method `protected static void registerAndMergeDiscriminator(DiscriminatorContext, ObjectNode, JsonSchema, String)` returns `void`, related with `com.networknt.schema.BaseJsonValidator.fetchSubSchemaNode(ValidationContext)`, api document: 
     * Rolls up all nested and compatible discriminators to the root discriminator of the type. Detects attempts to redefine
     * the <code>propertyName</code> or mappings.
     *
     * @param currentDiscriminatorContext the currently active {@link DiscriminatorContext}
     * @param discriminator               the discriminator to use for the check
     * @param schema                      the value of the <code>discriminator/propertyName</code> field
     * @param at                          the logging prefix
     ;
class `com.networknt.schema.JsonSchema`:
	related functions:
		method `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, String, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.BaseJsonValidator.fetchSubSchemaNode(ValidationContext)`;
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.BaseJsonValidator.fetchSubSchemaNode(ValidationContext)`;
class `com.networknt.schema.UnevaluatedItemsValidator`:
	related functions:
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.BaseJsonValidator.fetchSubSchemaNode(ValidationContext)`;
class `com.networknt.schema.AllOfValidator`:
	related functions:
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.BaseJsonValidator.fetchSubSchemaNode(ValidationContext)`;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
