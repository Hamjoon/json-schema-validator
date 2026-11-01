
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getTypeValidator_Test {

    private static JsonSchemaFactory factory;

    static class NoTypeValidatorSchema extends JsonSchema {

        NoTypeValidatorSchema(JsonSchemaFactory factory, String schemaJson) {
            super(factory.getValidationContext(), factory.getSchemaLocation(schemaJson), JsonNodePath.ROOT, factory.getObjectMapper().readTree(schemaJson), null, false);
        }

        @Override
        public List<JsonValidator> getValidators() {
            // Return an empty list but deliberately do NOT set typeValidator.
            try {
                setField(this, "validators", Collections.emptyList());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return Collections.emptyList();
        }
    }

    static class CountingSchema extends JsonSchema {

        private final AtomicInteger counter = new AtomicInteger();

        CountingSchema(JsonSchemaFactory factory, String schemaJson) throws Exception {
            super(factory.getValidationContext(), factory.getSchemaLocation(schemaJson), JsonNodePath.ROOT, factory.getObjectMapper().readTree(schemaJson), null, false);
        }

        @Override
        public List<JsonValidator> getValidators() {
            // Increment counter each time the method is entered.
            counter.incrementAndGet();
            // Delegate to the real implementation to actually create validators.
            return super.getValidators();
        }

        int getInvocationCount() {
            return counter.get();
        }
    }

    static class ExceptionThrowingSchema extends JsonSchema {

        private final RuntimeException toThrow;

        ExceptionThrowingSchema(JsonSchemaFactory factory, String schemaJson, RuntimeException toThrow) throws Exception {
            super(factory.getValidationContext(), factory.getSchemaLocation(schemaJson), JsonNodePath.ROOT, factory.getObjectMapper().readTree(schemaJson), null, false);
            this.toThrow = toThrow;
        }

        @Override
        public List<JsonValidator> getValidators() {
            throw toThrow;
        }
    }

    /**
     * Simple custom unchecked exception used for the test.
     */
    static class SchemaException extends RuntimeException {

        SchemaException(String msg) {
            super(msg);
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Use the 2020‑12 draft – any draft works for these simple tests.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, builder -> {
            // No special configuration required for these tests.
        });
    }

    /**
     * Sets a private field via reflection.
     */
    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    /**
     * Retrieves a private field via reflection.
     */
    private static Object getField(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    @Test
    void getTypeValidator_returnsNonNullWhenTypeKeywordPresent() {
        // a minimal schema that defines a type
        String schemaJson = "{ \"type\": \"object\" }";
        JsonSchema jsonSchema = factory.getSchema(schemaJson);
        // The method under test – it lazily loads validators if needed.
        TypeValidator typeValidator = jsonSchema.getTypeValidator();
        assertNotNull(typeValidator, "TypeValidator should be non‑null when a \"type\" keyword is present");
    }

    @Test
    void getTypeValidator_returnsNullWhenNoTypeKeyword() {
        // schema without a "type" keyword
        String schemaJson = "{}";
        JsonSchema jsonSchema = factory.getSchema(schemaJson);
        TypeValidator typeValidator = jsonSchema.getTypeValidator();
        assertNull(typeValidator, "TypeValidator should be null when no \"type\" keyword is defined");
    }

    @Test
    void lazyLoadingWhenValidatorsIsNull() throws Exception {
        // Schema that defines a top‑level "type" keyword → a TypeValidator will be created.
        String schemaJson = "{ \"type\": \"object\" }";
        JsonSchema original = factory.getSchema(schemaJson);
        // spy to verify method calls
        JsonSchema spy = spy(original);
        // At this point validators are still null (lazy).
        assertNull(getField(spy, "validators"));
        // Invoke the method under test.
        TypeValidator tv = spy.getTypeValidator();
        // Verify that getValidators() was invoked exactly once.
        verify(spy, times(1)).getValidators();
        // The returned instance must be non‑null and must match the internal field.
        assertNotNull(tv, "TypeValidator should be non‑null when a \"type\" keyword is present");
        assertSame(tv, getField(spy, "typeValidator"));
    }

    @Test
    void noLazyLoadingWhenValidatorsAlreadyInitialized() throws Exception {
        // Create a schema (any schema) and then inject a pre‑populated validators list.
        JsonSchema original = factory.getSchema("{\"type\":\"string\"}");
        JsonSchema spy = spy(original);
        // Manually create a dummy TypeValidator (the real class is package‑private,
        // but we can reuse the one that getValidators would create).
        // First force initialization so we have a real instance to reuse.
        TypeValidator realTv = spy.getTypeValidator();
        // Inject the same validator and a non‑null validators list.
        setField(spy, "typeValidator", realTv);
        setField(spy, "validators", new ArrayList<>());
        // Reset interactions to ignore the previous initialization call.
        clearInvocations(spy);
        // Call getTypeValidator – should return the injected instance without invoking getValidators().
        TypeValidator returned = spy.getTypeValidator();
        assertSame(realTv, returned, "Returned TypeValidator must be the same instance that was injected");
        verify(spy, never()).getValidators();
    }

    @Test
    void lazyLoadingInvokedOnlyOnceOnRepeatedCalls() throws Exception {
        JsonSchema spy = spy(factory.getSchema("{\"type\":\"array\"}"));
        // First call – triggers lazy loading.
        TypeValidator first = spy.getTypeValidator();
        // Second call – should reuse the already created instance.
        TypeValidator second = spy.getTypeValidator();
        assertSame(first, second, "Both calls must return the same TypeValidator instance");
        // only the first call should have caused loading
        verify(spy, times(1)).getValidators();
    }

    @Test
    void returnsNullWhenNoTopLevelTypeKeyword() {
        // Schema without a top‑level "type". Nested types do not create a root TypeValidator.
        String schemaJson = "{ \"properties\": { \"name\": { \"type\": \"string\" } } }";
        JsonSchema jsonSchema = factory.getSchema(schemaJson);
        TypeValidator tv = jsonSchema.getTypeValidator();
        assertNull(tv, "TypeValidator should be null when the root schema does not define a \"type\" keyword");
    }

    @Test
    void existingValidatorsListButNullTypeValidatorReturnsNull() throws Exception {
        JsonSchema spy = spy(factory.getSchema("{\"type\":\"boolean\"}"));
        // Force lazy loading to ensure validators list is created, then discard the TypeValidator.
        // creates both fields
        spy.getTypeValidator();
        // Now replace the fields: keep validators, null out typeValidator.
        List<JsonValidator> validators = (List<JsonValidator>) getField(spy, "validators");
        setField(spy, "validators", validators);
        setField(spy, "typeValidator", null);
        // Reset interaction history.
        clearInvocations(spy);
        // Call getTypeValidator – should return null and must NOT invoke getValidators().
        TypeValidator tv = spy.getTypeValidator();
        assertNull(tv, "When typeValidator is null, getTypeValidator must return null");
        verify(spy, never()).getValidators();
    }

    @Test
    void customSubclassOverridesGetValidatorsWithoutSettingTypeValidator() throws Exception {
        // Minimal schema without a type keyword – the overridden getValidators will be used.
        NoTypeValidatorSchema schema = new NoTypeValidatorSchema(factory, "{}");
        // Spy to verify that our overridden method is indeed called.
        NoTypeValidatorSchema spy = spy(schema);
        // Invoke the method under test.
        TypeValidator tv = spy.getTypeValidator();
        // The overridden getValidators should have been called.
        verify(spy, times(1)).getValidators();
        // Because we never set typeValidator, the result must be null.
        assertNull(tv, "When overridden getValidators does not set typeValidator, the result must be null");
    }

    @Test
    void concurrentAccessYieldsSingleTypeValidatorInstance() throws Exception {
        CountingSchema schema = new CountingSchema(factory, "{ \"type\": \"object\" }");
        int threadCount = 10;
        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<TypeValidator> results = Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < threadCount; i++) {
            exec.submit(() -> {
                try {
                    // make all threads start together
                    startLatch.await();
                    results.add(schema.getTypeValidator());
                } catch (Exception e) {
                    fail("Unexpected exception: " + e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        // Release all threads
        startLatch.countDown();
        // Wait for completion
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "All threads should finish quickly");
        exec.shutdownNow();
        // All returned instances must be the same (by reference)
        TypeValidator first = results.get(0);
        for (TypeValidator tv : results) {
            assertSame(first, tv, "All threads must receive the same TypeValidator instance");
        }
        // getValidators must have been invoked at most once.
        assertTrue(schema.getInvocationCount() <= 1, "getValidators should be called at most once, but was called " + schema.getInvocationCount() + " times");
    }

    @Test
    void getTypeValidatorPropagatesNullPointerException() throws Exception {
        ExceptionThrowingSchema schema = new ExceptionThrowingSchema(factory, "{}", new NullPointerException("test NPE"));
        NullPointerException ex = assertThrows(NullPointerException.class, schema::getTypeValidator);
        assertEquals("test NPE", ex.getMessage());
    }

    @Test
    void getTypeValidatorPropagatesIllegalArgumentException() throws Exception {
        ExceptionThrowingSchema schema = new ExceptionThrowingSchema(factory, "{}", new IllegalArgumentException("bad arg"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, schema::getTypeValidator);
        assertEquals("bad arg", ex.getMessage());
    }

    @Test
    void getTypeValidatorPropagatesCustomSchemaException() throws Exception {
        ExceptionThrowingSchema schema = new ExceptionThrowingSchema(factory, "{}", new SchemaException("custom error"));
        SchemaException ex = assertThrows(SchemaException.class, schema::getTypeValidator);
        assertEquals("custom error", ex.getMessage());
    }

    @Test
    void getTypeValidatorPropagatesOutOfMemoryError() throws Exception {
        ExceptionThrowingSchema schema = new ExceptionThrowingSchema(factory, "{}", new OutOfMemoryError("OOM"));
        OutOfMemoryError err = assertThrows(OutOfMemoryError.class, schema::getTypeValidator);
        assertEquals("OOM", err.getMessage());
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_getTypeValidator_Test.java:6: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/JsonSchema_getTypeValidator_Test.java:14: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_getTypeValidator_Test.java:15: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_getTypeValidator_Test.java:17: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_getTypeValidator_Test.java:25: error: cannot find symbol
            super(factory.getValidationContext(), factory.getSchemaLocation(schemaJson), JsonNodePath.ROOT, factory.getObjectMapper().readTree(schemaJson), null, false);
                                                                                                     ^
  symbol:   variable ROOT
  location: class JsonNodePath
src/test/java/com/networknt/schema/JsonSchema_getTypeValidator_Test.java:25: error: method getObjectMapper in class JsonSchemaFactory cannot be applied to given types;
            super(factory.getValidationContext(), factory.getSchemaLocation(schemaJson), JsonNodePath.ROOT, factory.getObjectMapper().readTree(schemaJson), null, false);
                                                                                                                   ^
  required: InputFormat
  found:    no arguments
  reason: actual and formal argument lists differ in length
src/test/java/com/networknt/schema/JsonSchema_getTypeValidator_Test.java:25: error: incompatible types: String cannot be converted to SchemaLocation
            super(factory.getValidationContext(), factory.getSchemaLocation(schemaJson), JsonNodePath.ROOT, factory.getObjectMapper().readTree(schemaJson), null, false);
                                                                            ^
src/test/java/com/networknt/schema/JsonSchema_getTypeValidator_Test.java:25: error: cannot find symbol
            super(factory.getValidationContext(), factory.getSchemaLocation(schemaJson), JsonNodePath.ROOT, factory.getObjectMapper().readTree(schemaJson), null, false);
                         ^
  symbol:   method getValidationContext()
  location: variable factory of type JsonSchemaFactory
src/test/java/com/networknt/schema/JsonSchema_getTypeValidator_Test.java:45: error: cannot find symbol
            super(factory.getValidationContext(), factory.getSchemaLocation(schemaJson), JsonNodePath.ROOT, factory.getObjectMapper().readTree(schemaJson), null, false);
                                                                                                     ^
  symbol:   variable ROOT
  location: class JsonNodePath
src/test/java/com/networknt/schema/JsonSchema_getTypeValidator_Test.java:45: error: method getObjectMapper in class JsonSchemaFactory cannot be applied to given types;
            super(factory.getValidationContext(), factory.getSchemaLocation(schemaJson), JsonNodePath.ROOT, factory.getObjectMapper().readTree(schemaJson), null, false);
                                                                                                                   ^
  required: InputFormat
  found:    no arguments
  reason: actual and formal argument lists differ in length
src/test/java/com/networknt/schema/JsonSchema_getTypeValidator_Test.java:45: error: incompatible types: String cannot be converted to SchemaLocation
            super(factory.getValidationContext(), factory.getSchemaLocation(schemaJson), JsonNodePath.ROOT, factory.getObjectMapper().readTree(schemaJson), null, false);
                                                                            ^
src/test/java/com/networknt/schema/JsonSchema_getTypeValidator_Test.java:45: error: cannot find symbol
            super(factory.getValidationContext(), factory.getSchemaLocation(schemaJson), JsonNodePath.ROOT, factory.getObjectMapper().readTree(schemaJson), null, false);
                         ^
  symbol:   method getValidationContext()
  location: variable factory of type JsonSchemaFactory
src/test/java/com/networknt/schema/JsonSchema_getTypeValidator_Test.java:66: error: cannot find symbol
            super(factory.getValidationContext(), factory.getSchemaLocation(schemaJson), JsonNodePath.ROOT, factory.getObjectMapper().readTree(schemaJson), null, false);
                                                                                                     ^
  symbol:   variable ROOT
  location: class JsonNodePath
src/test/java/com/networknt/schema/JsonSchema_getTypeValidator_Test.java:66: error: method getObjectMapper in class JsonSchemaFactory cannot be applied to given types;
            super(factory.getValidationContext(), factory.getSchemaLocation(schemaJson), JsonNodePath.ROOT, factory.getObjectMapper().readTree(schemaJson), null, false);
                                                                                                                   ^
  required: InputFormat
  found:    no arguments
  reason: actual and formal argument lists differ in length
src/test/java/com/networknt/schema/JsonSchema_getTypeValidator_Test.java:66: error: incompatible types: String cannot be converted to SchemaLocation
            super(factory.getValidationContext(), factory.getSchemaLocation(schemaJson), JsonNodePath.ROOT, factory.getObjectMapper().readTree(schemaJson), null, false);
                                                                            ^
src/test/java/com/networknt/schema/JsonSchema_getTypeValidator_Test.java:66: error: cannot find symbol
            super(factory.getValidationContext(), factory.getSchemaLocation(schemaJson), JsonNodePath.ROOT, factory.getObjectMapper().readTree(schemaJson), null, false);
                         ^
  symbol:   method getValidationContext()
  location: variable factory of type JsonSchemaFactory
src/test/java/com/networknt/schema/JsonSchema_getTypeValidator_Test.java:137: error: cannot find symbol
        JsonSchema spy = spy(original);
                         ^
  symbol:   method spy(JsonSchema)
  location: class JsonSchema_getTypeValidator_Test
src/test/java/com/networknt/schema/JsonSchema_getTypeValidator_Test.java:143: error: cannot find symbol
        verify(spy, times(1)).getValidators();
                    ^
  symbol:   method times(int)
  location: class JsonSchema_getTypeValidator_Test
src/test/java/com/networknt/schema/JsonSchema_getTypeValidator_Test.java:153: error: cannot find symbol
        JsonSchema spy = spy(original);
                         ^
  symbol:   method spy(JsonSchema)
  location: class JsonSchema_getTypeValidator_Test
src/test/java/com/networknt/schema/JsonSchema_getTypeValidator_Test.java:162: error: cannot find symbol
        clearInvocations(spy);
        ^
  symbol:   method clearInvocations(JsonSchema)
  location: class JsonSchema_getTypeValidator_Test
src/test/java/com/networknt/schema/JsonSchema_getTypeValidator_Test.java:166: error: cannot find symbol
        verify(spy, never()).getValidators();
                    ^
  symbol:   method never()
  location: class JsonSchema_getTypeValidator_Test
src/test/java/com/networknt/schema/JsonSchema_getTypeValidator_Test.java:171: error: cannot find symbol
        JsonSchema spy = spy(factory.getSchema("{\"type\":\"array\"}"));
                         ^
  symbol:   method spy(JsonSchema)
  location: class JsonSchema_getTypeValidator_Test
src/test/java/com/networknt/schema/JsonSchema_getTypeValidator_Test.java:178: error: cannot find symbol
        verify(spy, times(1)).getValidators();
                    ^
  symbol:   method times(int)
  location: class JsonSchema_getTypeValidator_Test
src/test/java/com/networknt/schema/JsonSchema_getTypeValidator_Test.java:192: error: cannot find symbol
        JsonSchema spy = spy(factory.getSchema("{\"type\":\"boolean\"}"));
                         ^
  symbol:   method spy(JsonSchema)
  location: class JsonSchema_getTypeValidator_Test
src/test/java/com/networknt/schema/JsonSchema_getTypeValidator_Test.java:201: error: cannot find symbol
        clearInvocations(spy);
        ^
  symbol:   method clearInvocations(JsonSchema)
  location: class JsonSchema_getTypeValidator_Test
src/test/java/com/networknt/schema/JsonSchema_getTypeValidator_Test.java:205: error: cannot find symbol
        verify(spy, never()).getValidators();
                    ^
  symbol:   method never()
  location: class JsonSchema_getTypeValidator_Test
src/test/java/com/networknt/schema/JsonSchema_getTypeValidator_Test.java:213: error: cannot find symbol
        NoTypeValidatorSchema spy = spy(schema);
                                    ^
  symbol:   method spy(NoTypeValidatorSchema)
  location: class JsonSchema_getTypeValidator_Test
src/test/java/com/networknt/schema/JsonSchema_getTypeValidator_Test.java:217: error: cannot find symbol
        verify(spy, times(1)).getValidators();
                    ^
  symbol:   method times(int)
  location: class JsonSchema_getTypeValidator_Test
src/test/java/com/networknt/schema/JsonSchema_getTypeValidator_Test.java:280: error: incompatible types: OutOfMemoryError cannot be converted to RuntimeException
        ExceptionThrowingSchema schema = new ExceptionThrowingSchema(factory, "{}", new OutOfMemoryError("OOM"));
                                                                                    ^
Note: src/test/java/com/networknt/schema/JsonSchema_getTypeValidator_Test.java uses unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
29 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'return type': 'com.networknt.schema.TypeValidator', 'dependent classes': "class `com.networknt.schema.JsonSchema`:\n\tdependent fields:\n\t\tcom.networknt.schema.TypeValidator com.networknt.schema.JsonSchema.typeValidator;\n\t\tjava.util.List<com.networknt.schema.JsonValidator> com.networknt.schema.JsonSchema.validators;\n\trelated functions:\n\t\tmethod `public void initializeValidators()` returns `void`, related with `com.networknt.schema.JsonSchema.getTypeValidator()`, api document: \n     * Initializes the validators' {@link com.networknt.schema.JsonSchema} instances.\n     * For avoiding issues with concurrency, in 1.0.49 the {@link com.networknt.schema.JsonSchema} instances affiliated with\n     * validators were modified to no more preload the schema and lazy loading is used instead.\n     * <p>This comes with the issue that this way you cannot rely on validating important schema features, in particular\n     * <code>$ref</code> resolution at instantiation from {@link com.networknt.schema.JsonSchemaFactory}.</p>\n     * <p>By calling <code>initializeValidators</code> you can enforce preloading of the {@link com.networknt.schema.JsonSchema}\n     * instances of the validators.</p>\n     ;\n\t\tmethod `public List<JsonValidator> getValidators()` returns `java.util.List<com.networknt.schema.JsonValidator>`, related with `com.networknt.schema.JsonSchema.getTypeValidator()`;\n\t\tmethod `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.getTypeValidator()`;\n\t\tmethod `private List<JsonValidator> read(JsonNode)` returns `java.util.List<com.networknt.schema.JsonValidator>`, related with `com.networknt.schema.JsonSchema.getTypeValidator()`, api document: \n     * Please note that the key in {@link #validators} map is the evaluation path.\n     ;\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.getTypeValidator()`;\nclass `com.networknt.schema.OneOfValidator`:\n\trelated functions:\n\t\tmethod `protected boolean canShortCircuit()` returns `boolean`, related with `com.networknt.schema.JsonSchema.getTypeValidator()`;"}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


