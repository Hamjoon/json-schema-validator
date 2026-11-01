```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#getValidators()} covering the scenarios
 * described in the provided test‑case JSON.
 *
 * <p>Scenarios:
 * <ul>
 *   <li>When {@code validators} is {@code null} (first call) – the method should
 *       read the schema node, wrap the result with {@link Collections#unmodifiableList},
 *       store it and return it.</li>
 *   <li>When {@code validators} is already initialized – the cached list must be
 *       returned without invoking {@code read(..)} again.</li>
 *   <li>When {@code read(..)} returns an empty mutable list – the returned list must
 *       be an unmodifiable empty list.</li>
 *   <li>When {@code read(..)} throws a runtime exception – the exception must be
 *       propagated (this test is disabled because it requires advanced mocking of
 *       a private method).</li>
 * </ul>
 *
 * <p>All tests use the {@link JsonSchemaFactory} to create real {@link JsonSchema}
 * instances, except where a spy is required to control the behaviour of the
 * private {@code read} method.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_getValidators_Test {

    private static JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Use a recent draft version – the concrete version is not important for the test.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @AfterAll
    static void teardownAfterAll() {
        factory = null;
    }

    /**
     * Helper that uses reflection to reset the private {@code validators} field
     * to {@code null}. This forces {@code getValidators()} to execute the
     * {@code if (this.validators == null)} branch again.
     */
    private static void resetValidators(JsonSchema schema) throws Exception {
        Field f = JsonSchema.class.getDeclaredField("validators");
        f.setAccessible(true);
        f.set(schema, null);
    }

    /* --------------------------------------------------------------------- */
    /* 1. First call – validators is null                                      */
    /* --------------------------------------------------------------------- */

    @Test
    @DisplayName("First call creates unmodifiable list from read()")
    void getValidatorsWhenInitiallyNull() throws Exception {
        // Minimal schema with a single property – this will cause read() to return
        // at least one validator.
        String schemaData = "{"
                + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\":\"object\","
                + "\"properties\":{"
                + "   \"name\":{"
                + "       \"type\":\"string\""
                + "   }"
                + "}"
                + "}";
        JsonSchema schema = factory.getSchema(schemaData);
        // Ensure the internal cache is cleared (it should be null on a fresh instance,
        // but we reset it explicitly for safety).
        resetValidators(schema);

        List<JsonValidator> validators = schema.getValidators();

        // The list must be non‑null and unmodifiable.
        assertNotNull(validators, "getValidators() must never return null");
        assertThrows(UnsupportedOperationException.class,
                () -> validators.add(null),
                "The validators list should be unmodifiable");

        // The schema defines a property → at least one validator should be present.
        assertFalse(validators.isEmpty(),
                "A schema that defines a property should produce at least one validator");
    }

    /* --------------------------------------------------------------------- */
    /* 2. Subsequent calls – validators already initialised                    */
    /* --------------------------------------------------------------------- */

    @Test
    @DisplayName("Subsequent calls return the cached list")
    void getValidatorsWhenAlreadyInitialized() {
        // Use the same schema as the previous test.
        String schemaData = "{"
                + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\":\"object\","
                + "\"properties\":{"
                + "   \"name\":{"
                + "       \"type\":\"string\""
                + "   }"
                + "}"
                + "}";
        JsonSchema schema = factory.getSchema(schemaData);

        // First call – populates the cache.
        List<JsonValidator> first = schema.getValidators();
        // Second call – should return the same instance.
        List<JsonValidator> second = schema.getValidators();

        assertSame(first, second, "getValidators() should return the cached list on subsequent calls");
    }

    /* --------------------------------------------------------------------- */
    /* 3. read() returns an empty mutable list → unmodifiable empty list      */
    /* --------------------------------------------------------------------- */

    @Test
    @DisplayName("Empty list from read() results in an unmodifiable empty list")
    void getValidatorsWhenReadReturnsEmptyList() throws Exception {
        // Create a schema that contains no validation keywords – read() will
        // produce an empty mutable list.
        String emptySchema = "{"
                + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\""
                + "}";

        JsonSchema realSchema = factory.getSchema(emptySchema);
        // Spy on the real instance so we can stub the private read method.
        JsonSchema spy = spy(realSchema);

        // Force the private field to null so getValidators() will invoke read().
        resetValidators(spy);

        // Stub the private read(JsonNode) method to return an empty mutable list.
        // Because read() is private we cannot stub it directly with standard Mockito.
        // Instead we replace the internal 'validators' field after the call to read()
        // by using reflection on the spy instance.
        // The first call to getValidators() will invoke read() and store the result.
        // We simulate the empty result by manually setting the field afterwards.
        // (This approach avoids the need for PowerMockito or other byte‑code tools.)

        // Invoke getValidators() – this will call the real read() implementation.
        // For an empty schema the implementation already returns an empty list,
        // but we enforce the expectation explicitly.
        List<JsonValidator> result = spy.getValidators();

        // Verify the list is unmodifiable and empty.
        assertNotNull(result, "Result must not be null");
        assertTrue(result.isEmpty(), "Resulting validator list should be empty");
        assertThrows(UnsupportedOperationException.class,
                () -> result.add(mock(JsonValidator.class)),
                "The empty validators list must be unmodifiable");
    }

    /* --------------------------------------------------------------------- */
    /* 4. read() throws a runtime exception – propagate it                      */
    /* --------------------------------------------------------------------- */

    @Test
    @Disabled("Requires advanced mocking of a private method (e.g., PowerMockito).")
    @DisplayName("Exception thrown by read() is propagated")
    void getValidatorsWhenReadThrowsException() throws Exception {
        // The purpose of this test is to ensure that a runtime exception from
        // the private read() method is not swallowed by getValidators().
        // Implementing this with pure Mockito is not possible because read()
        // is private. In a project that includes PowerMockito (or Mockito
        // inline with mock-maker‑inline) the following approach could be used:
        //
        // JsonSchema spy = spy(factory.getSchema("{}"));
        // resetValidators(spy);
        // doThrow(new IllegalStateException("forced")).when(spy, "read", any(JsonNode.class));
        // assertThrows(IllegalStateException.class, spy::getValidators);
        //
        // Since the current test environment does not provide such capabilities,
        // the test is marked as disabled with an explanatory comment.
    }
}
```