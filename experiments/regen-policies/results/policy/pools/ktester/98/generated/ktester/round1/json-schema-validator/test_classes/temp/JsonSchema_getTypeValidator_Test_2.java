package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getTypeValidator_Test {

    private static JsonSchemaFactory factory;

    /**
     * Subclass that does NOT set a {@code typeValidator} in {@code getValidators()}.
     */
    static class NoTypeValidatorSchema extends JsonSchema {

        NoTypeValidatorSchema(JsonSchemaFactory factory, String schemaJson) throws Exception {
            // The super‑constructor of {@link JsonSchema} requires a number of arguments.
            // For the purpose of these tests we can pass {@code null} values – the fields
            // that matter (validators, typeValidator) are set later via reflection.
            super(null, null, null, null, null, false);
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

    /**
     * Subclass that counts how many times {@code getValidators()} is invoked.
     */
    static class CountingSchema extends JsonSchema {

        private final AtomicInteger counter = new AtomicInteger();

        CountingSchema(JsonSchemaFactory factory, String schemaJson) throws Exception {
            super(null, null, null, null, null, false);
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

    /**
     * Subclass that always throws the supplied {@code Throwable} from {@code getValidators()}.
     */
    static class ExceptionThrowingSchema extends JsonSchema {

        private final Throwable toThrow;

        ExceptionThrowingSchema(JsonSchemaFactory factory, String schemaJson, Throwable toThrow) throws Exception {
            super(null, null, null, null, null, false);
            this.toThrow = toThrow;
        }

        @Override
        public List<JsonValidator> getValidators() {
            if (toThrow instanceof RuntimeException) {
                throw (RuntimeException) toThrow;
            } else if (toThrow instanceof Error) {
                throw (Error) toThrow;
            } else {
                throw new RuntimeException(toThrow);
            }
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
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, builder -> {
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
        CountingSchema schema = new CountingSchema(factory, schemaJson);
        // At this point validators are still null (lazy).
        assertNull(getField(schema, "validators"));
        // Invoke the method under test.
        TypeValidator tv = schema.getTypeValidator();
        // Verify that getValidators() was invoked exactly once.
        assertEquals(1, schema.getInvocationCount(), "getValidators() should be called exactly once during lazy loading");
        // The returned instance must be non‑null and must match the internal field.
        assertNotNull(tv, "TypeValidator should be non‑null when a \"type\" keyword is present");
        assertSame(tv, getField(schema, "typeValidator"));
    }

    @Test
    void noLazyLoadingWhenValidatorsAlreadyInitialized() throws Exception {
        // Create a schema (any schema) and then inject a pre‑populated validators list.
        JsonSchema jsonSchema = factory.getSchema("{\"type\":\"string\"}");
        // Force initialization so we have a real TypeValidator instance.
        TypeValidator realTv = jsonSchema.getTypeValidator();
        // Inject the same validator and a non‑null validators list.
        setField(jsonSchema, "typeValidator", realTv);
        setField(jsonSchema, "validators", new ArrayList<>());
        // Call getTypeValidator – should return the injected instance without re‑initialising.
        TypeValidator returned = jsonSchema.getTypeValidator();
        assertSame(realTv, returned, "Returned TypeValidator must be the same instance that was injected");
    }

    @Test
    void lazyLoadingInvokedOnlyOnceOnRepeatedCalls() throws Exception {
        CountingSchema schema = new CountingSchema(factory, "{\"type\":\"array\"}");
        // First call – triggers lazy loading.
        TypeValidator first = schema.getTypeValidator();
        // Second call – should reuse the already created instance.
        TypeValidator second = schema.getTypeValidator();
        assertSame(first, second, "Both calls must return the same TypeValidator instance");
        // only the first call should have caused loading
        assertEquals(1, schema.getInvocationCount(), "getValidators() should be called only once");
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
        JsonSchema jsonSchema = factory.getSchema("{\"type\":\"boolean\"}");
        // Force lazy loading to ensure validators list is created, then discard the TypeValidator.
        jsonSchema.getTypeValidator();
        // Now replace the fields: keep validators, null out typeValidator.
        List<JsonValidator> validators = (List<JsonValidator>) getField(jsonSchema, "validators");
        setField(jsonSchema, "validators", validators);
        setField(jsonSchema, "typeValidator", null);
        // Call getTypeValidator – should return null and must NOT re‑initialize validators.
        TypeValidator tv = jsonSchema.getTypeValidator();
        assertNull(tv, "When typeValidator is null, getTypeValidator must return null");
    }

    @Test
    void customSubclassOverridesGetValidatorsWithoutSettingTypeValidator() throws Exception {
        // Minimal schema without a type keyword – the overridden getValidators will be used.
        NoTypeValidatorSchema schema = new NoTypeValidatorSchema(factory, "{}");
        // Invoke the method under test.
        TypeValidator tv = schema.getTypeValidator();
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
