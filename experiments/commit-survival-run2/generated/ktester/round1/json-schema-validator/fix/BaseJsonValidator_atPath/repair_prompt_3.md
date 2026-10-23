


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

// @ExtendWith(MockitoExtension.class)
class BaseJsonValidator_atPath_Test {

    // --------------------------------------------------------------------- //
//     @Mock
    private JsonSchema parentSchema;

//     @Mock
    private ValidatorTypeCode validatorType;

//     @Mock
    private ValidationContext validationContext;

//     @Mock
    private SchemaValidatorsConfig config;

    // --------------------------------------------------------------------- //
    private TestValidator validator;

    // --------------------------------------------------------------------- //
    private TestValidator defaultValidator;

    /**
     * Minimal {@link ValidationContext} implementation that only supplies a
     * {@link SchemaValidatorsConfig}. All other methods are inherited from the
     * library class and are not used by the tests.
     */
    private static class SimpleValidationContext extends ValidationContext {

        SimpleValidationContext(SchemaValidatorsConfig config) {
            // The constructor of {@link ValidationContext} requires a number of
            // factories. They are not needed for the {@code atPath} tests, so we
            // pass {@code null} for all of them.
            super(null, null, null, null, config);
        }
    }

    /**
     * Simple {@link SchemaValidatorsConfig} implementation used in the tests.
     */
    private static class SimpleConfig extends SchemaValidatorsConfig {

        private PathType pathType;

        private ApplyDefaultsStrategy applyDefaultsStrategy;

        private boolean failFast;

        @Override
        public PathType getPathType() {
            return pathType;
        }

        @Override
        public void setPathType(PathType pathType) {
            this.pathType = pathType;
        }

        @Override
        public ApplyDefaultsStrategy getApplyDefaultsStrategy() {
            return applyDefaultsStrategy;
        }

        @Override
        public void setApplyDefaultsStrategy(ApplyDefaultsStrategy s) {
            this.applyDefaultsStrategy = s;
        }

        @Override
        public boolean isFailFast() {
            return failFast;
        }

        @Override
        public void setFailFast(boolean f) {
            this.failFast = f;
        }
    }

    /**
     * Concrete subclass of {@link BaseJsonValidator} exposing the protected
     * {@code atPath} method.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }

        // newer library versions also define a four‑argument overload
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at) {
            return Collections.emptySet();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // basic configuration – JSON‑Pointer is the default path type used by the
        // library and is sufficient for all the test scenarios.
        config = new SimpleConfig();
        config.setPathType(PathType.JSON_POINTER);
        config.setFailFast(false);
        config.setApplyDefaultsStrategy(ApplyDefaultsStrategy.EMPTY_APPLY_DEFAULTS_STRATEGY);
        // a very small ValidationContext implementation – only getConfig is required
        validationContext = new SimpleValidationContext(config);
        // create validator instances (the schema arguments are irrelevant for atPath)
        validator = new TestValidator("", JsonNodeFactory.instance.objectNode(), null, null, validationContext);
        defaultValidator = validator;
    }

    private static Stream<Arguments> slashPathProvider() {
        return Stream.of(Arguments.of("Appending normal token to a non‑empty path", "/users/123", "address", "/users/123/address"), Arguments.of("Current path empty – token becomes first segment", "", "root", "/root"), Arguments.of("Empty token – should result in a trailing '/'", "/items", "", "/items/"));
    }

    private static Stream<Arguments> jsonPointerPathProvider() {
        return Stream.of(Arguments.of("Normal token appended with '/' separator", "/data", "items", "/data/items"), Arguments.of("Empty current path – token becomes first segment", "", "root", "/root"), Arguments.of("Empty token – trailing '/' indicates empty segment", "/array", "", "/array/"), Arguments.of("Token containing '~' and '/' are escaped per JSON Pointer rules", "/a~b", "c/d", "/a~0b/c~1d"), Arguments.of("Null current path is treated as empty", null, "first", "/first"));
    }

    @Test
    void atPathAppendsTokenWhenCurrentPathIsNonEmpty() {
        String result = validator.atPath("/parent", "child");
        assertEquals("/parent/child", result);
    }

    @Test
    void atPathAppendsTokenWhenCurrentPathIsRoot() {
        String result = validator.atPath("", "root");
        assertEquals("/root", result);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("slashPathProvider")
    void atPath_Appends_WithSlashPathType(String description, String currentPath, String token, String expected) {
        // The library version used in this project only supports JSON_POINTER.
        // For the purpose of these tests the behaviour is identical, so we keep
        // using JSON_POINTER.
        config.setPathType(PathType.JSON_POINTER);
        TestValidator slashValidator = new TestValidator("", JsonNodeFactory.instance.objectNode(), null, null, validationContext);
        String result = slashValidator.atPath(currentPath, token);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("jsonPointerPathProvider")
    void atPath_Appends_WithJsonPointerPathType(String description, String currentPath, String token, String expected) {
        String result = defaultValidator.atPath(currentPath, token);
        assertEquals(expected, result, description);
    }

    @Test
    void atPath_NullPathType_ThrowsNullPointerException() {
        config.setPathType(null);
        assertThrows(NullPointerException.class, () -> validator.atPath("/test", "field"));
    }

    @Test
    void atPath_NullToken_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> defaultValidator.atPath("/users", null));
    }

    @Test
    void atPath_BothCurrentPathAndTokenNull_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> defaultValidator.atPath(null, null));
    }

    @Test
    void atPath_InvalidTokenForJsonPointer_ThrowsIllegalArgumentException() {
        String illegalToken = "invalid\u0000char";
        assertThrows(IllegalArgumentException.class, () -> defaultValidator.atPath("/data", illegalToken));
    }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

╷
├─ JUnit Jupiter ✔
│  └─ BaseJsonValidator_atPath_Test ✔
│     ├─ atPath_Appends_WithJsonPointerPathType(String, String, String, String) ✔
│     │  ├─ [1] Normal token appended with '/' separator ✘ URIFactory must not be null
│     │  ├─ [2] Empty current path – token becomes first segment ✘ URIFactory must not be null
│     │  ├─ [3] Empty token – trailing '/' indicates empty segment ✘ URIFactory must not be null
│     │  ├─ [4] Token containing '~' and '/' are escaped per JSON Pointer rules ✘ URIFactory must not be null
│     │  └─ [5] Null current path is treated as empty ✘ URIFactory must not be null
│     ├─ atPath_InvalidTokenForJsonPointer_ThrowsIllegalArgumentException() ✘ URIFactory must not be null
│     ├─ atPathAppendsTokenWhenCurrentPathIsRoot() ✘ URIFactory must not be null
│     ├─ atPath_NullPathType_ThrowsNullPointerException() ✘ URIFactory must not be null
│     ├─ atPathAppendsTokenWhenCurrentPathIsNonEmpty() ✘ URIFactory must not be null
│     ├─ atPath_Appends_WithSlashPathType(String, String, String, String) ✔
│     │  ├─ [1] Appending normal token to a non‑empty path ✘ URIFactory must not be null
│     │  ├─ [2] Current path empty – token becomes first segment ✘ URIFactory must not be null
│     │  └─ [3] Empty token – should result in a trailing '/' ✘ URIFactory must not be null
│     ├─ atPath_NullToken_ThrowsNullPointerException() ✘ URIFactory must not be null
│     └─ atPath_BothCurrentPathAndTokenNull_ThrowsNullPointerException() ✘ URIFactory must not be null
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (14):
  JUnit Jupiter:BaseJsonValidator_atPath_Test:atPath_Appends_WithJsonPointerPathType(String, String, String, String):[1] Normal token appended with '/' separator
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_atPath_Test', methodName = 'atPath_Appends_WithJsonPointerPathType', methodParameterTypes = 'java.lang.String, java.lang.String, java.lang.String, java.lang.String']
    => java.lang.IllegalArgumentException: URIFactory must not be null
       com.networknt.schema.ValidationContext.<init>(ValidationContext.java:42)
       com.networknt.schema.BaseJsonValidator_atPath_Test$SimpleValidationContext.<init>(BaseJsonValidator_atPath_Test.java:48)
       com.networknt.schema.BaseJsonValidator_atPath_Test.setupBeforeEach(BaseJsonValidator_atPath_Test.java:125)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]
  JUnit Jupiter:BaseJsonValidator_atPath_Test:atPath_Appends_WithJsonPointerPathType(String, String, String, String):[2] Empty current path – token becomes first segment
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_atPath_Test', methodName = 'atPath_Appends_WithJsonPointerPathType', methodParameterTypes = 'java.lang.String, java.lang.String, java.lang.String, java.lang.String']
    => java.lang.IllegalArgumentException: URIFactory must not be null
       com.networknt.schema.ValidationContext.<init>(ValidationContext.java:42)
       com.networknt.schema.BaseJsonValidator_atPath_Test$SimpleValidationContext.<init>(BaseJsonValidator_atPath_Test.java:48)
       com.networknt.schema.BaseJsonValidator_atPath_Test.setupBeforeEach(BaseJsonValidator_atPath_Test.java:125)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]
  JUnit Jupiter:BaseJsonValidator_atPath_Test:atPath_Appends_WithJsonPointerPathType(String, String, String, String):[3] Empty token – trailing '/' indicates empty segment
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_atPath_Test', methodName = 'atPath_Appends_WithJsonPointerPathType', methodParameterTypes = 'java.lang.String, java.lang.String, java.lang.String, java.lang.String']
    => java.lang.IllegalArgumentException: URIFactory must not be null
       com.networknt.schema.ValidationContext.<init>(ValidationContext.java:42)
       com.networknt.schema.BaseJsonValidator_atPath_Test$SimpleValidationContext.<init>(BaseJsonValidator_atPath_Test.java:48)
       com.networknt.schema.BaseJsonValidator_atPath_Test.setupBeforeEach(BaseJsonValidator_atPath_Test.java:125)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]
  JUnit Jupiter:BaseJsonValidator_atPath_Test:atPath_Appends_WithJsonPointerPathType(String, String, String, String):[4] Token containing '~' and '/' are escaped per JSON Pointer rules
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_atPath_Test', methodName = 'atPath_Appends_WithJsonPointerPathType', methodParameterTypes = 'java.lang.String, java.lang.String, java.lang.String, java.lang.String']
    => java.lang.IllegalArgumentException: URIFactory must not be null
       com.networknt.schema.ValidationContext.<init>(ValidationContext.java:42)
       com.networknt.schema.BaseJsonValidator_atPath_Test$SimpleValidationContext.<init>(BaseJsonValidator_atPath_Test.java:48)
       com.networknt.schema.BaseJsonValidator_atPath_Test.setupBeforeEach(BaseJsonValidator_atPath_Test.java:125)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]
  JUnit Jupiter:BaseJsonValidator_atPath_Test:atPath_Appends_WithJsonPointerPathType(String, String, String, String):[5] Null current path is treated as empty
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_atPath_Test', methodName = 'atPath_Appends_WithJsonPointerPathType', methodParameterTypes = 'java.lang.String, java.lang.String, java.lang.String, java.lang.String']
    => java.lang.IllegalArgumentException: URIFactory must not be null
       com.networknt.schema.ValidationContext.<init>(ValidationContext.java:42)
       com.networknt.schema.BaseJsonValidator_atPath_Test$SimpleValidationContext.<init>(BaseJsonValidator_atPath_Test.java:48)
       com.networknt.schema.BaseJsonValidator_atPath_Test.setupBeforeEach(BaseJsonValidator_atPath_Test.java:125)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]
  JUnit Jupiter:BaseJsonValidator_atPath_Test:atPath_InvalidTokenForJsonPointer_ThrowsIllegalArgumentException()
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_atPath_Test', methodName = 'atPath_InvalidTokenForJsonPointer_ThrowsIllegalArgumentException', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: URIFactory must not be null
       com.networknt.schema.ValidationContext.<init>(ValidationContext.java:42)
       com.networknt.schema.BaseJsonValidator_atPath_Test$SimpleValidationContext.<init>(BaseJsonValidator_atPath_Test.java:48)
       com.networknt.schema.BaseJsonValidator_atPath_Test.setupBeforeEach(BaseJsonValidator_atPath_Test.java:125)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]
  JUnit Jupiter:BaseJsonValidator_atPath_Test:atPathAppendsTokenWhenCurrentPathIsRoot()
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_atPath_Test', methodName = 'atPathAppendsTokenWhenCurrentPathIsRoot', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: URIFactory must not be null
       com.networknt.schema.ValidationContext.<init>(ValidationContext.java:42)
       com.networknt.schema.BaseJsonValidator_atPath_Test$SimpleValidationContext.<init>(BaseJsonValidator_atPath_Test.java:48)
       com.networknt.schema.BaseJsonValidator_atPath_Test.setupBeforeEach(BaseJsonValidator_atPath_Test.java:125)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]
  JUnit Jupiter:BaseJsonValidator_atPath_Test:atPath_NullPathType_ThrowsNullPointerException()
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_atPath_Test', methodName = 'atPath_NullPathType_ThrowsNullPointerException', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: URIFactory must not be null
       com.networknt.schema.ValidationContext.<init>(ValidationContext.java:42)
       com.networknt.schema.BaseJsonValidator_atPath_Test$SimpleValidationContext.<init>(BaseJsonValidator_atPath_Test.java:48)
       com.networknt.schema.BaseJsonValidator_atPath_Test.setupBeforeEach(BaseJsonValidator_atPath_Test.java:125)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]
  JUnit Jupiter:BaseJsonValidator_atPath_Test:atPathAppendsTokenWhenCurrentPathIsNonEmpty()
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_atPath_Test', methodName = 'atPathAppendsTokenWhenCurrentPathIsNonEmpty', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: URIFactory must not be null
       com.networknt.schema.ValidationContext.<init>(ValidationContext.java:42)
       com.networknt.schema.BaseJsonValidator_atPath_Test$SimpleValidationContext.<init>(BaseJsonValidator_atPath_Test.java:48)
       com.networknt.schema.BaseJsonValidator_atPath_Test.setupBeforeEach(BaseJsonValidator_atPath_Test.java:125)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]
  JUnit Jupiter:BaseJsonValidator_atPath_Test:atPath_Appends_WithSlashPathType(String, String, String, String):[1] Appending normal token to a non‑empty path
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_atPath_Test', methodName = 'atPath_Appends_WithSlashPathType', methodParameterTypes = 'java.lang.String, java.lang.String, java.lang.String, java.lang.String']
    => java.lang.IllegalArgumentException: URIFactory must not be null
       com.networknt.schema.ValidationContext.<init>(ValidationContext.java:42)
       com.networknt.schema.BaseJsonValidator_atPath_Test$SimpleValidationContext.<init>(BaseJsonValidator_atPath_Test.java:48)
       com.networknt.schema.BaseJsonValidator_atPath_Test.setupBeforeEach(BaseJsonValidator_atPath_Test.java:125)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]
  JUnit Jupiter:BaseJsonValidator_atPath_Test:atPath_Appends_WithSlashPathType(String, String, String, String):[2] Current path empty – token becomes first segment
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_atPath_Test', methodName = 'atPath_Appends_WithSlashPathType', methodParameterTypes = 'java.lang.String, java.lang.String, java.lang.String, java.lang.String']
    => java.lang.IllegalArgumentException: URIFactory must not be null
       com.networknt.schema.ValidationContext.<init>(ValidationContext.java:42)
       com.networknt.schema.BaseJsonValidator_atPath_Test$SimpleValidationContext.<init>(BaseJsonValidator_atPath_Test.java:48)
       com.networknt.schema.BaseJsonValidator_atPath_Test.setupBeforeEach(BaseJsonValidator_atPath_Test.java:125)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]
  JUnit Jupiter:BaseJsonValidator_atPath_Test:atPath_Appends_WithSlashPathType(String, String, String, String):[3] Empty token – should result in a trailing '/'
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_atPath_Test', methodName = 'atPath_Appends_WithSlashPathType', methodParameterTypes = 'java.lang.String, java.lang.String, java.lang.String, java.lang.String']
    => java.lang.IllegalArgumentException: URIFactory must not be null
       com.networknt.schema.ValidationContext.<init>(ValidationContext.java:42)
       com.networknt.schema.BaseJsonValidator_atPath_Test$SimpleValidationContext.<init>(BaseJsonValidator_atPath_Test.java:48)
       com.networknt.schema.BaseJsonValidator_atPath_Test.setupBeforeEach(BaseJsonValidator_atPath_Test.java:125)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]
  JUnit Jupiter:BaseJsonValidator_atPath_Test:atPath_NullToken_ThrowsNullPointerException()
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_atPath_Test', methodName = 'atPath_NullToken_ThrowsNullPointerException', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: URIFactory must not be null
       com.networknt.schema.ValidationContext.<init>(ValidationContext.java:42)
       com.networknt.schema.BaseJsonValidator_atPath_Test$SimpleValidationContext.<init>(BaseJsonValidator_atPath_Test.java:48)
       com.networknt.schema.BaseJsonValidator_atPath_Test.setupBeforeEach(BaseJsonValidator_atPath_Test.java:125)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]
  JUnit Jupiter:BaseJsonValidator_atPath_Test:atPath_BothCurrentPathAndTokenNull_ThrowsNullPointerException()
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_atPath_Test', methodName = 'atPath_BothCurrentPathAndTokenNull_ThrowsNullPointerException', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: URIFactory must not be null
       com.networknt.schema.ValidationContext.<init>(ValidationContext.java:42)
       com.networknt.schema.BaseJsonValidator_atPath_Test$SimpleValidationContext.<init>(BaseJsonValidator_atPath_Test.java:48)
       com.networknt.schema.BaseJsonValidator_atPath_Test.setupBeforeEach(BaseJsonValidator_atPath_Test.java:125)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]

Test run finished after 55 ms
[         6 containers found      ]
[         0 containers skipped    ]
[         6 containers started    ]
[         0 containers aborted    ]
[         6 containers successful ]
[         0 containers failed     ]
[        14 tests found           ]
[         0 tests skipped         ]
[        14 tests started         ]
[         0 tests aborted         ]
[         0 tests successful      ]
[        14 tests failed          ]


```


And you can use the following related information to assist you in your analysis and correction:

api document of method atPath(String, String): 

     * Create the path for a given child token.
     *
     * @param currentPath The current path.
     * @param token       The child token.
     * @return The complete path.
     

dependent classes: 
class `com.networknt.schema.BaseJsonValidator`:
	dependent fields:
		com.networknt.schema.PathType com.networknt.schema.BaseJsonValidator.pathType;
	related functions:
		method `protected PathType getPathType()` returns `com.networknt.schema.PathType`, related with `com.networknt.schema.BaseJsonValidator.atPath(String, String)`;
class `com.networknt.schema.utils.JsonNodeUtil`:
	related functions:
		method `private static void visitObject(PathType, String, JsonNode, Collection<String>)` returns `void`, related with `com.networknt.schema.BaseJsonValidator.atPath(String, String)`;
class `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:
	related functions:
		method `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.BaseJsonValidator.atPath(String, String)`, api document: 
     * Returns the singleton instance of this validator.
     *
     * @return the singleton instance of this validator
     ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
