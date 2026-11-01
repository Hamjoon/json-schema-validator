


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.*;
import java.util.List;
import java.util.Set;
import java.util.Collections;

// @ExtendWith(MockitoExtension.class)
class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test {

    /**
     * Minimal concrete subclass of {@link BaseJsonValidator} used for testing.
     *
     * <p>It stores the evaluation‑parent schema and the keyword that should be
     * returned by {@link #getKeyword()}.</p>
     */
    static class TestValidator extends BaseJsonValidator {

        private final JsonSchema evaluationParentSchema;

        private final String keyword;

        TestValidator(JsonSchema evaluationParentSchema, ValidationContext validationContext) {
            this(evaluationParentSchema, validationContext, null);
        }

        TestValidator(JsonSchema evaluationParentSchema, ValidationContext validationContext, String keyword) {
            // The super‑constructor arguments that are not needed for the test are passed as null/false.
            super(null, null, null, null, null, null, validationContext, false);
            this.evaluationParentSchema = evaluationParentSchema;
            this.keyword = keyword;
        }

        @Override
        public JsonSchema getEvaluationParentSchema() {
            return evaluationParentSchema;
        }

        @Override
        public String getKeyword() {
            return keyword;
        }

        /* --------------------------------------------------------------------- */
        /* The following methods are required by the JsonValidator interface but
           are irrelevant for the tests of hasAdjacentKeywordInEvaluationPath.
           They are therefore stubbed with harmless defaults.                */
        @Override
        public JsonNodePath getEvaluationPath() {
            return null;
        }

        @Override
        public SchemaLocation getSchemaLocation() {
            return null;
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath path) {
            return Collections.emptySet();
        }
    }

    /**
     * Helper that creates a {@link JsonSchema} proxy exposing only the two methods
     * needed by {@code BaseJsonValidator.hasAdjacentKeywordInEvaluationPath}:
     * {@code getValidators()} and {@code getEvaluationParentSchema()}.
     */
    private static JsonSchema schema(List<JsonValidator> validators, JsonSchema parent) {
        InvocationHandler handler = (proxy, method, args) -> {
            switch(method.getName()) {
                case "getValidators":
                    return validators;
                case "getEvaluationParentSchema":
                    return parent;
                default:
                    // All other methods return null (or a sensible default) – they are never used in the tests.
                    return null;
            }
        };
        return (JsonSchema) Proxy.newProxyInstance(JsonSchema.class.getClassLoader(), new Class[] { JsonSchema.class }, handler);
    }

    @Test
    void returnsTrueWhenKeywordFoundInImmediateParent() {
        // validator that matches the searched keyword
        TestValidator matchingValidator = new TestValidator(null, null, "unevaluatedItems");
        // parent schema containing the matching validator
        JsonSchema parentSchema = schema(List.of(matchingValidator), null);
        // validator under test whose evaluation‑parent is the above schema
        TestValidator validator = new TestValidator(parentSchema, null);
        assertTrue(validator.hasAdjacentKeywordInEvaluationPath("unevaluatedItems"), "Keyword should be found in the immediate evaluation‑parent schema");
    }

    @Test
    void returnsTrueWhenKeywordFoundInAncestorSchema() {
        // leaf schema – does NOT contain the keyword.
        TestValidator otherValidator = new TestValidator(null, null, "someOtherKeyword");
        JsonSchema leafSchema = schema(List.of(otherValidator), null);
        // ancestor (parent) schema – contains the keyword we are looking for.
        TestValidator matchingValidator = new TestValidator(null, null, "unevaluatedProperties");
        JsonSchema ancestorSchema = schema(List.of(matchingValidator), null);
        // wire the chain: leaf → ancestor
        JsonSchema leafWithParent = schema(List.of(otherValidator), ancestorSchema);
        TestValidator validator = new TestValidator(leafWithParent, null);
        assertTrue(validator.hasAdjacentKeywordInEvaluationPath("unevaluatedProperties"), "Keyword should be found in an ancestor evaluation‑parent schema");
    }

    @Test
    void returnsFalseWhenKeywordNotPresentInAnySchema() {
        // leaf schema – no matching keyword.
        TestValidator leafValidator = new TestValidator(null, null, "leafKeyword");
        JsonSchema leafSchema = schema(List.of(leafValidator), null);
        // parent schema – also no matching keyword.
        TestValidator parentValidator = new TestValidator(null, null, "parentKeyword");
        JsonSchema parentSchema = schema(List.of(parentValidator), null);
        // chain leaf → parent.
        JsonSchema leafWithParent = schema(List.of(leafValidator), parentSchema);
        TestValidator validator = new TestValidator(leafWithParent, null);
        assertFalse(validator.hasAdjacentKeywordInEvaluationPath("nonExistingKeyword"), "Method should return false when the keyword is absent in the whole chain");
    }

    @Test
    void returnsFalseWhenNoEvaluationParentSchema() {
        // The validator has no evaluation‑parent schema at all.
        TestValidator validator = new TestValidator(null, null);
        assertFalse(validator.hasAdjacentKeywordInEvaluationPath("anyKeyword"), "Method should return false when there is no evaluation‑parent schema");
    }

    @Test
    @DisplayName("Parent schema without validators → returns false")
    void returnsFalseWhenParentSchemaHasNoValidators() {
        JsonSchema parent = schema(Collections.emptyList(), null);
        TestValidator validator = new TestValidator(parent, null);
        assertFalse(validator.hasAdjacentKeywordInEvaluationPath("minimum"), "Empty validator list should lead to false.");
    }

    @Test
    @DisplayName("Multiple validators, early break → returns true")
    void returnsTrueWhenEarlyBreakOccurs() {
        // matching validator (first in list)
        TestValidator matching = new TestValidator(null, null, "exclusiveMaximum");
        // a second validator that would throw if inspected (ensures early break)
        TestValidator shouldNotBeVisited = new TestValidator(null, null, "shouldNotBeVisited") {

            @Override
            public String getKeyword() {
                throw new AssertionError("Should not be visited");
            }
        };
        JsonSchema parent = schema(List.of(matching, shouldNotBeVisited), null);
        TestValidator validator = new TestValidator(parent, null);
        assertTrue(validator.hasAdjacentKeywordInEvaluationPath("exclusiveMaximum"), "Method should stop after finding the first matching validator.");
    }

    @Test
    @DisplayName("Validator with null keyword → returns false")
    void returnsFalseWhenValidatorKeywordIsNull() {
        TestValidator nullKeywordValidator = new TestValidator(null, null, null);
        JsonSchema parent = schema(List.of(nullKeywordValidator), null);
        TestValidator validator = new TestValidator(parent, null);
        assertFalse(validator.hasAdjacentKeywordInEvaluationPath("required"), "Null keyword from a validator must be ignored and result in false.");
    }

    @Test
    @DisplayName("Null input keyword → throws NullPointerException")
    void throwsNPEWhenKeywordIsNull() {
        JsonSchema parent = schema(Collections.emptyList(), null);
        TestValidator validator = new TestValidator(parent, null);
        assertThrows(NullPointerException.class, () -> validator.hasAdjacentKeywordInEvaluationPath(null), "Passing null keyword should result in NullPointerException.");
    }

    @Test
    @DisplayName("No match across multiple ancestors → returns false")
    void returnsFalseWhenNoMatchAcrossAncestors() {
        // leaf schema
        TestValidator leafV = new TestValidator(null, null, "leafK");
        JsonSchema leaf = schema(List.of(leafV), null);
        // middle schema
        TestValidator middleV = new TestValidator(null, null, "middleK");
        JsonSchema middle = schema(List.of(middleV), null);
        // top schema
        TestValidator topV = new TestValidator(null, null, "topK");
        JsonSchema top = schema(List.of(topV), null);
        // chain leaf → middle → top
        JsonSchema leafWithParent = schema(List.of(leafV), middle);
        JsonSchema middleWithParent = schema(List.of(middleV), top);
        // Use the leaf that has the proper parent chain
        TestValidator validator = new TestValidator(leafWithParent, null);
        assertFalse(validator.hasAdjacentKeywordInEvaluationPath("minimum"), "Keyword absent in the whole chain should yield false.");
    }

    @Test
    @DisplayName("Keyword matches validator in current schema only → returns false")
    void returnsFalseWhenKeywordOnlyInCurrentSchema() {
        // The evaluation‑parent schema has no validators.
        JsonSchema parent = schema(Collections.emptyList(), null);
        TestValidator validator = new TestValidator(parent, null);
        assertFalse(validator.hasAdjacentKeywordInEvaluationPath("type"), "Method should ignore validators belonging to the current schema.");
    }

    @Test
    @DisplayName("Validator list contains null element → throws NullPointerException")
    void throwsNPEWhenValidatorListContainsNull() {
        JsonSchema parent = schema(Collections.singletonList(null), null);
        TestValidator validator = new TestValidator(parent, null);
        assertThrows(NullPointerException.class, () -> validator.hasAdjacentKeywordInEvaluationPath("maxItems"), "Null element in validators list should cause NPE.");
    }

    @Test
    @DisplayName("Schema validators collection is null → throws NullPointerException")
    void throwsNPEWhenValidatorsCollectionIsNull() {
        // Use a proxy that returns null for getValidators()
        InvocationHandler handler = (proxy, method, args) -> {
            if ("getValidators".equals(method.getName())) {
                return null;
            }
            if ("getEvaluationParentSchema".equals(method.getName())) {
                return null;
            }
            return null;
        };
        JsonSchema parent = (JsonSchema) Proxy.newProxyInstance(JsonSchema.class.getClassLoader(), new Class[] { JsonSchema.class }, handler);
        TestValidator validator = new TestValidator(parent, null);
        assertThrows(NullPointerException.class, () -> validator.hasAdjacentKeywordInEvaluationPath("minLength"), "Null validators collection should cause NPE.");
    }

    @Test
    @DisplayName("Empty keyword matches empty validator keyword → returns true")
    void returnsTrueWhenEmptyKeywordMatchesEmptyValidatorKeyword() {
        TestValidator emptyKeywordValidator = new TestValidator(null, null, "");
        JsonSchema parent = schema(List.of(emptyKeywordValidator), null);
        TestValidator validator = new TestValidator(parent, null);
        assertTrue(validator.hasAdjacentKeywordInEvaluationPath(""), "Empty string keyword should match validator returning empty string.");
    }

    @Test
    @DisplayName("Case‑sensitivity mismatch → returns false")
    void returnsFalseWhenCaseMismatch() {
        TestValidator validatorWithKeyword = new TestValidator(null, null, "maxLength");
        JsonSchema parent = schema(List.of(validatorWithKeyword), null);
        TestValidator testValidator = new TestValidator(parent, null);
        assertFalse(testValidator.hasAdjacentKeywordInEvaluationPath("MAXLENGTH"), "Keyword comparison is case‑sensitive; mismatch should return false.");
    }

    @Test
    @DisplayName("Keyword with surrounding whitespace does not match → returns false")
    void returnsFalseWhenKeywordHasWhitespace() {
        TestValidator validatorWithKeyword = new TestValidator(null, null, "pattern");
        JsonSchema parent = schema(List.of(validatorWithKeyword), null);
        TestValidator testValidator = new TestValidator(parent, null);
        assertFalse(testValidator.hasAdjacentKeywordInEvaluationPath(" pattern "), "Whitespace around keyword should cause mismatch.");
    }

    @Test
    @DisplayName("Validator list contains non‑JsonValidator element → throws ClassCastException")
    void throwsClassCastExceptionWhenNonJsonValidatorInList() {
        // Use a raw List to inject a non‑JsonValidator element (e.g., a String).
        @SuppressWarnings("unchecked")
        List rawList = new java.util.ArrayList();
        rawList.add("I am not a JsonValidator");
        JsonSchema parent = schema(rawList, null);
        TestValidator validator = new TestValidator(parent, null);
        assertThrows(ClassCastException.class, () -> validator.hasAdjacentKeywordInEvaluationPath("anyKeyword"), "Non‑JsonValidator element should cause ClassCastException.");
    }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

╷
├─ JUnit Jupiter ✔
│  └─ BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test ✔
│     ├─ Validator list contains non‑JsonValidator element → throws ClassCastException ✘ com.networknt.schema.JsonSchema is not an interface
│     ├─ Validator with null keyword → returns false ✘ java.lang.NullPointerException
│     ├─ returnsTrueWhenKeywordFoundInAncestorSchema() ✘ java.lang.NullPointerException
│     ├─ No match across multiple ancestors → returns false ✘ java.lang.NullPointerException
│     ├─ Case‑sensitivity mismatch → returns false ✘ java.lang.NullPointerException
│     ├─ returnsTrueWhenKeywordFoundInImmediateParent() ✘ java.lang.NullPointerException
│     ├─ Null input keyword → throws NullPointerException ✘ com.networknt.schema.JsonSchema is not an interface
│     ├─ Multiple validators, early break → returns true ✘ java.lang.NullPointerException
│     ├─ Validator list contains null element → throws NullPointerException ✘ com.networknt.schema.JsonSchema is not an interface
│     ├─ Schema validators collection is null → throws NullPointerException ✘ com.networknt.schema.JsonSchema is not an interface
│     ├─ Keyword matches validator in current schema only → returns false ✘ com.networknt.schema.JsonSchema is not an interface
│     ├─ Keyword with surrounding whitespace does not match → returns false ✘ java.lang.NullPointerException
│     ├─ Parent schema without validators → returns false ✘ com.networknt.schema.JsonSchema is not an interface
│     ├─ Empty keyword matches empty validator keyword → returns true ✘ java.lang.NullPointerException
│     ├─ returnsFalseWhenKeywordNotPresentInAnySchema() ✘ java.lang.NullPointerException
│     └─ returnsFalseWhenNoEvaluationParentSchema() ✘ java.lang.NullPointerException
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (16):
  JUnit Jupiter:BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test:Validator list contains non‑JsonValidator element → throws ClassCastException
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test', methodName = 'throwsClassCastExceptionWhenNonJsonValidatorInList', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: com.networknt.schema.JsonSchema is not an interface
       java.base/java.lang.reflect.Proxy$ProxyBuilder.validateProxyInterfaces(Proxy.java:706)
       java.base/java.lang.reflect.Proxy$ProxyBuilder.<init>(Proxy.java:648)
       java.base/java.lang.reflect.Proxy$ProxyBuilder.<init>(Proxy.java:656)
       java.base/java.lang.reflect.Proxy.lambda$getProxyConstructor$0(Proxy.java:429)
       java.base/jdk.internal.loader.AbstractClassLoaderValue$Memoizer.get(AbstractClassLoaderValue.java:329)
       java.base/jdk.internal.loader.AbstractClassLoaderValue.computeIfAbsent(AbstractClassLoaderValue.java:205)
       java.base/java.lang.reflect.Proxy.getProxyConstructor(Proxy.java:427)
       java.base/java.lang.reflect.Proxy.newProxyInstance(Proxy.java:1037)
       com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.schema(BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:94)
       com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.throwsClassCastExceptionWhenNonJsonValidatorInList(BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:275)
       [...]
  JUnit Jupiter:BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test:Validator with null keyword → returns false
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test', methodName = 'returnsFalseWhenValidatorKeywordIsNull', methodParameterTypes = '']
    => java.lang.NullPointerException
       java.base/java.util.Objects.requireNonNull(Objects.java:209)
       com.networknt.schema.ValidationMessageHandler.<init>(ValidationMessageHandler.java:30)
       com.networknt.schema.BaseJsonValidator.<init>(BaseJsonValidator.java:52)
       com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test$TestValidator.<init>(BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:37)
       com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.returnsFalseWhenValidatorKeywordIsNull(BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:172)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test:returnsTrueWhenKeywordFoundInAncestorSchema()
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test', methodName = 'returnsTrueWhenKeywordFoundInAncestorSchema', methodParameterTypes = '']
    => java.lang.NullPointerException
       java.base/java.util.Objects.requireNonNull(Objects.java:209)
       com.networknt.schema.ValidationMessageHandler.<init>(ValidationMessageHandler.java:30)
       com.networknt.schema.BaseJsonValidator.<init>(BaseJsonValidator.java:52)
       com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test$TestValidator.<init>(BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:37)
       com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.returnsTrueWhenKeywordFoundInAncestorSchema(BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:111)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test:No match across multiple ancestors → returns false
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test', methodName = 'returnsFalseWhenNoMatchAcrossAncestors', methodParameterTypes = '']
    => java.lang.NullPointerException
       java.base/java.util.Objects.requireNonNull(Objects.java:209)
       com.networknt.schema.ValidationMessageHandler.<init>(ValidationMessageHandler.java:30)
       com.networknt.schema.BaseJsonValidator.<init>(BaseJsonValidator.java:52)
       com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test$TestValidator.<init>(BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:37)
       com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.returnsFalseWhenNoMatchAcrossAncestors(BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:190)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test:Case‑sensitivity mismatch → returns false
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test', methodName = 'returnsFalseWhenCaseMismatch', methodParameterTypes = '']
    => java.lang.NullPointerException
       java.base/java.util.Objects.requireNonNull(Objects.java:209)
       com.networknt.schema.ValidationMessageHandler.<init>(ValidationMessageHandler.java:30)
       com.networknt.schema.BaseJsonValidator.<init>(BaseJsonValidator.java:52)
       com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test$TestValidator.<init>(BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:37)
       com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.returnsFalseWhenCaseMismatch(BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:253)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test:returnsTrueWhenKeywordFoundInImmediateParent()
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test', methodName = 'returnsTrueWhenKeywordFoundInImmediateParent', methodParameterTypes = '']
    => java.lang.NullPointerException
       java.base/java.util.Objects.requireNonNull(Objects.java:209)
       com.networknt.schema.ValidationMessageHandler.<init>(ValidationMessageHandler.java:30)
       com.networknt.schema.BaseJsonValidator.<init>(BaseJsonValidator.java:52)
       com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test$TestValidator.<init>(BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:37)
       com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.returnsTrueWhenKeywordFoundInImmediateParent(BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:100)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test:Null input keyword → throws NullPointerException
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test', methodName = 'throwsNPEWhenKeywordIsNull', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: com.networknt.schema.JsonSchema is not an interface
       java.base/java.lang.reflect.Proxy$ProxyBuilder.validateProxyInterfaces(Proxy.java:706)
       java.base/java.lang.reflect.Proxy$ProxyBuilder.<init>(Proxy.java:648)
       java.base/java.lang.reflect.Proxy$ProxyBuilder.<init>(Proxy.java:656)
       java.base/java.lang.reflect.Proxy.lambda$getProxyConstructor$0(Proxy.java:429)
       java.base/jdk.internal.loader.AbstractClassLoaderValue$Memoizer.get(AbstractClassLoaderValue.java:329)
       java.base/jdk.internal.loader.AbstractClassLoaderValue.computeIfAbsent(AbstractClassLoaderValue.java:205)
       java.base/java.lang.reflect.Proxy.getProxyConstructor(Proxy.java:427)
       java.base/java.lang.reflect.Proxy.newProxyInstance(Proxy.java:1037)
       com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.schema(BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:94)
       com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.throwsNPEWhenKeywordIsNull(BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:181)
       [...]
  JUnit Jupiter:BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test:Multiple validators, early break → returns true
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test', methodName = 'returnsTrueWhenEarlyBreakOccurs', methodParameterTypes = '']
    => java.lang.NullPointerException
       java.base/java.util.Objects.requireNonNull(Objects.java:209)
       com.networknt.schema.ValidationMessageHandler.<init>(ValidationMessageHandler.java:30)
       com.networknt.schema.BaseJsonValidator.<init>(BaseJsonValidator.java:52)
       com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test$TestValidator.<init>(BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:37)
       com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.returnsTrueWhenEarlyBreakOccurs(BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:155)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test:Validator list contains null element → throws NullPointerException
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test', methodName = 'throwsNPEWhenValidatorListContainsNull', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: com.networknt.schema.JsonSchema is not an interface
       java.base/java.lang.reflect.Proxy$ProxyBuilder.validateProxyInterfaces(Proxy.java:706)
       java.base/java.lang.reflect.Proxy$ProxyBuilder.<init>(Proxy.java:648)
       java.base/java.lang.reflect.Proxy$ProxyBuilder.<init>(Proxy.java:656)
       java.base/java.lang.reflect.Proxy.lambda$getProxyConstructor$0(Proxy.java:429)
       java.base/jdk.internal.loader.AbstractClassLoaderValue$Memoizer.get(AbstractClassLoaderValue.java:329)
       java.base/jdk.internal.loader.AbstractClassLoaderValue.computeIfAbsent(AbstractClassLoaderValue.java:205)
       java.base/java.lang.reflect.Proxy.getProxyConstructor(Proxy.java:427)
       java.base/java.lang.reflect.Proxy.newProxyInstance(Proxy.java:1037)
       com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.schema(BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:94)
       com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.throwsNPEWhenValidatorListContainsNull(BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:218)
       [...]
  JUnit Jupiter:BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test:Schema validators collection is null → throws NullPointerException
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test', methodName = 'throwsNPEWhenValidatorsCollectionIsNull', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: com.networknt.schema.JsonSchema is not an interface
       java.base/java.lang.reflect.Proxy$ProxyBuilder.validateProxyInterfaces(Proxy.java:706)
       java.base/java.lang.reflect.Proxy$ProxyBuilder.<init>(Proxy.java:648)
       java.base/java.lang.reflect.Proxy$ProxyBuilder.<init>(Proxy.java:656)
       java.base/java.lang.reflect.Proxy.lambda$getProxyConstructor$0(Proxy.java:429)
       java.base/jdk.internal.loader.AbstractClassLoaderValue$Memoizer.get(AbstractClassLoaderValue.java:329)
       java.base/jdk.internal.loader.AbstractClassLoaderValue.computeIfAbsent(AbstractClassLoaderValue.java:205)
       java.base/java.lang.reflect.Proxy.getProxyConstructor(Proxy.java:427)
       java.base/java.lang.reflect.Proxy.newProxyInstance(Proxy.java:1037)
       com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.throwsNPEWhenValidatorsCollectionIsNull(BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:236)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       [...]
  JUnit Jupiter:BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test:Keyword matches validator in current schema only → returns false
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test', methodName = 'returnsFalseWhenKeywordOnlyInCurrentSchema', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: com.networknt.schema.JsonSchema is not an interface
       java.base/java.lang.reflect.Proxy$ProxyBuilder.validateProxyInterfaces(Proxy.java:706)
       java.base/java.lang.reflect.Proxy$ProxyBuilder.<init>(Proxy.java:648)
       java.base/java.lang.reflect.Proxy$ProxyBuilder.<init>(Proxy.java:656)
       java.base/java.lang.reflect.Proxy.lambda$getProxyConstructor$0(Proxy.java:429)
       java.base/jdk.internal.loader.AbstractClassLoaderValue$Memoizer.get(AbstractClassLoaderValue.java:329)
       java.base/jdk.internal.loader.AbstractClassLoaderValue.computeIfAbsent(AbstractClassLoaderValue.java:205)
       java.base/java.lang.reflect.Proxy.getProxyConstructor(Proxy.java:427)
       java.base/java.lang.reflect.Proxy.newProxyInstance(Proxy.java:1037)
       com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.schema(BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:94)
       com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.returnsFalseWhenKeywordOnlyInCurrentSchema(BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:210)
       [...]
  JUnit Jupiter:BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test:Keyword with surrounding whitespace does not match → returns false
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test', methodName = 'returnsFalseWhenKeywordHasWhitespace', methodParameterTypes = '']
    => java.lang.NullPointerException
       java.base/java.util.Objects.requireNonNull(Objects.java:209)
       com.networknt.schema.ValidationMessageHandler.<init>(ValidationMessageHandler.java:30)
       com.networknt.schema.BaseJsonValidator.<init>(BaseJsonValidator.java:52)
       com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test$TestValidator.<init>(BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:37)
       com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.returnsFalseWhenKeywordHasWhitespace(BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:262)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test:Parent schema without validators → returns false
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test', methodName = 'returnsFalseWhenParentSchemaHasNoValidators', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: com.networknt.schema.JsonSchema is not an interface
       java.base/java.lang.reflect.Proxy$ProxyBuilder.validateProxyInterfaces(Proxy.java:706)
       java.base/java.lang.reflect.Proxy$ProxyBuilder.<init>(Proxy.java:648)
       java.base/java.lang.reflect.Proxy$ProxyBuilder.<init>(Proxy.java:656)
       java.base/java.lang.reflect.Proxy.lambda$getProxyConstructor$0(Proxy.java:429)
       java.base/jdk.internal.loader.AbstractClassLoaderValue$Memoizer.get(AbstractClassLoaderValue.java:329)
       java.base/jdk.internal.loader.AbstractClassLoaderValue.computeIfAbsent(AbstractClassLoaderValue.java:205)
       java.base/java.lang.reflect.Proxy.getProxyConstructor(Proxy.java:427)
       java.base/java.lang.reflect.Proxy.newProxyInstance(Proxy.java:1037)
       com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.schema(BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:94)
       com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.returnsFalseWhenParentSchemaHasNoValidators(BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:146)
       [...]
  JUnit Jupiter:BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test:Empty keyword matches empty validator keyword → returns true
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test', methodName = 'returnsTrueWhenEmptyKeywordMatchesEmptyValidatorKeyword', methodParameterTypes = '']
    => java.lang.NullPointerException
       java.base/java.util.Objects.requireNonNull(Objects.java:209)
       com.networknt.schema.ValidationMessageHandler.<init>(ValidationMessageHandler.java:30)
       com.networknt.schema.BaseJsonValidator.<init>(BaseJsonValidator.java:52)
       com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test$TestValidator.<init>(BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:37)
       com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.returnsTrueWhenEmptyKeywordMatchesEmptyValidatorKeyword(BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:244)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test:returnsFalseWhenKeywordNotPresentInAnySchema()
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test', methodName = 'returnsFalseWhenKeywordNotPresentInAnySchema', methodParameterTypes = '']
    => java.lang.NullPointerException
       java.base/java.util.Objects.requireNonNull(Objects.java:209)
       com.networknt.schema.ValidationMessageHandler.<init>(ValidationMessageHandler.java:30)
       com.networknt.schema.BaseJsonValidator.<init>(BaseJsonValidator.java:52)
       com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test$TestValidator.<init>(BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:37)
       com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.returnsFalseWhenKeywordNotPresentInAnySchema(BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:125)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test:returnsFalseWhenNoEvaluationParentSchema()
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test', methodName = 'returnsFalseWhenNoEvaluationParentSchema', methodParameterTypes = '']
    => java.lang.NullPointerException
       java.base/java.util.Objects.requireNonNull(Objects.java:209)
       com.networknt.schema.ValidationMessageHandler.<init>(ValidationMessageHandler.java:30)
       com.networknt.schema.BaseJsonValidator.<init>(BaseJsonValidator.java:52)
       com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test$TestValidator.<init>(BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:37)
       com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test$TestValidator.<init>(BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:32)
       com.networknt.schema.BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.returnsFalseWhenNoEvaluationParentSchema(BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:139)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]

Test run finished after 117 ms
[         4 containers found      ]
[         0 containers skipped    ]
[         4 containers started    ]
[         0 containers aborted    ]
[         4 containers successful ]
[         0 containers failed     ]
[        16 tests found           ]
[         0 tests skipped         ]
[        16 tests started         ]
[         0 tests aborted         ]
[         0 tests successful      ]
[        16 tests failed          ]


```


And you can use the following related information to assist you in your analysis and correction:

api document of class com.networknt.schema.BaseJsonValidator: 

 * Base {@link JsonValidator}. 
 

api document of method hasAdjacentKeywordInEvaluationPath(String): 

     * Determines if the keyword exists adjacent in the evaluation path.
     * <p>
     * This does not check if the keyword exists in the current meta schema as this
     * can be a cross-draft case where the properties keyword is in a Draft 7 schema
     * and the unevaluatedProperties keyword is in an outer Draft 2020-12 schema.
     * <p>
     * The fact that the validator exists in the evaluation path implies that the
     * keyword was valid in whatever meta schema for that schema it was created for.
     * 
     * @param keyword the keyword to check
     * @return true if found
     

return type: 
boolean

dependent classes: 
class `com.networknt.schema.OneOfValidator`:
	related functions:
		method `protected boolean canShortCircuit()` returns `boolean`, related with `com.networknt.schema.BaseJsonValidator.hasAdjacentKeywordInEvaluationPath(String)`;
class `com.networknt.schema.utils.StringChecker`:
	related functions:
		method `public static boolean isBoolean(String)` returns `boolean`, related with `com.networknt.schema.BaseJsonValidator.hasAdjacentKeywordInEvaluationPath(String)`;
class `com.networknt.org.apache.commons.validator.routines.DomainValidator.IDNBUGHOLDER`:
	related functions:
		method `private static boolean keepsTrailingDot()` returns `boolean`, related with `com.networknt.schema.BaseJsonValidator.hasAdjacentKeywordInEvaluationPath(String)`;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
