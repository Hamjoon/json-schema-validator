package com.networknt.schema;

import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

class JsonSchema_getValidators_Test {

    private static JsonSchemaFactory factory;

    private JsonSchema schema;

    /**
     * The raw JSON representation of the schema.
     */
    private final JsonNode schemaNode;

    /**
     * Cached, unmodifiable list of validators – created lazily.
     */
    private List<JsonValidator> validators;

    @BeforeAll
    static void setupBeforeAll() {
        // Use a recent draft version – the concrete version is not important for the test.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        // A minimal but valid JSON schema that defines an object with a single string property.
        String schemaData = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{" + "   \"name\":{" + "       \"type\":\"string\"" + "   }" + "}" + "}";
        // The factory creates a JsonSchema instance that internally builds its validators.
        schema = factory.getSchema(schemaData);
        assertNotNull(schema, "Factory should return a non‑null JsonSchema instance");
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the test tidy.
        schema = null;
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

    /**
     * Returns the list of {@link JsonValidator}s for this schema.  The list is
     * created on first call, cached and wrapped with {@link Collections#unmodifiableList(List)}.
     *
     * @return an unmodifiable list (never {@code null})
     */
    public List<JsonValidator> getValidators() {
        if (validators == null) {
            // Build the mutable list first, then wrap it.
            List<JsonValidator> mutable = read();
            validators = Collections.unmodifiableList(mutable);
        }
        return validators;
    }

    /**
     * Reads the schema node and creates a mutable list of validators.
     * <p>
     * The method now explicitly skips the {@code $schema} keyword because it
     * does not correspond to a validation rule.  This change makes an empty
     * schema (containing only {@code $schema}) produce an empty validator list,
     * satisfying the corresponding unit test.
     * </p>
     *
     * @return mutable list of validators (may be empty)
     */
    private List<JsonValidator> read() {
        List<JsonValidator> list = new ArrayList<>();
        // Iterate over all fields defined in the schema JSON object.
        Iterator<String> fieldNames = schemaNode.fieldNames();
        while (fieldNames.hasNext()) {
            String field = fieldNames.next();
            // *** FIX: ignore the $schema meta‑keyword ***
            if ("$schema".equals(field)) {
                // do not create a validator for $schema
                continue;
            }
            JsonNode node = schemaNode.get(field);
            JsonValidator validator = factory.getValidator(field);
            if (validator != null) {
                // Initialise the validator with the current schema and its node.
                validator.initialize(this, node);
                list.add(validator);
            }
        }
        return list;
    }

    public JsonNode getSchemaNode() {
        return schemaNode;
    }

    public JsonSchemaFactory getFactory() {
        return factory;
    }

    // specific validator.  It is kept here only to preserve compilation.
    public TypeValidator getTypeValidator() {
        // The real implementation would locate the appropriate TypeValidator.
        return null;
    }

    @Test
    void getValidatorsReturnsNonNullAndUnmodifiableList() {
        List<JsonValidator> validators = schema.getValidators();
        // The list must be non‑null.
        assertNotNull(validators, "getValidators() must never return null");
        // The list must be unmodifiable – adding an element should throw.
        assertThrows(UnsupportedOperationException.class, () -> validators.add(null), "The validators list should be unmodifiable");
    }

    @Test
    void getValidatorsCachesResult() {
        List<JsonValidator> firstCall = schema.getValidators();
        List<JsonValidator> secondCall = schema.getValidators();
        // The same instance should be returned on subsequent calls.
        assertSame(firstCall, secondCall, "getValidators() should cache and return the same list");
    }

    @Test
    void schemaWithPropertyHasAtLeastOneValidator() {
        List<JsonValidator> validators = schema.getValidators();
        // The schema defines a property, therefore at least one validator (e.g., a PropertyValidator)
        // must be present. The exact number depends on the implementation, but it must be > 0.
        assertFalse(validators.isEmpty(), "A schema that defines a property should produce at least one validator");
    }

    @Test
    @DisplayName("First call creates unmodifiable list from read()")
    void getValidatorsWhenInitiallyNull() throws Exception {
        // Minimal schema with a single property – this will cause read() to return
        // at least one validator.
        String schemaData = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{" + "   \"name\":{" + "       \"type\":\"string\"" + "   }" + "}" + "}";
        JsonSchema schema = factory.getSchema(schemaData);
        // Ensure the internal cache is cleared (it should be null on a fresh instance,
        // but we reset it explicitly for safety).
        resetValidators(schema);
        List<JsonValidator> validators = schema.getValidators();
        // The list must be non‑null and unmodifiable.
        assertNotNull(validators, "getValidators() must never return null");
        assertThrows(UnsupportedOperationException.class, () -> validators.add(null), "The validators list should be unmodifiable");
        // The schema defines a property → at least one validator should be present.
        assertFalse(validators.isEmpty(), "A schema that defines a property should produce at least one validator");
    }

    @Test
    @DisplayName("Subsequent calls return the cached list")
    void getValidatorsWhenAlreadyInitialized() {
        // Use the same schema as the previous test.
        String schemaData = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{" + "   \"name\":{" + "       \"type\":\"string\"" + "   }" + "}" + "}";
        JsonSchema schema = factory.getSchema(schemaData);
        // First call – populates the cache.
        List<JsonValidator> first = schema.getValidators();
        // Second call – should return the same instance.
        List<JsonValidator> second = schema.getValidators();
        assertSame(first, second, "getValidators() should return the cached list on subsequent calls");
    }

    @Test
    @DisplayName("Empty list from read() results in an unmodifiable empty list")
    void getValidatorsWhenReadReturnsEmptyList() throws Exception {
        // Create a schema that contains no validation keywords – read() will
        // produce an empty mutable list.
        String emptySchema = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"" + "}";
        JsonSchema schema = factory.getSchema(emptySchema);
        // Reset the internal cache so getValidators() will invoke read().
        resetValidators(schema);
        // Invoke getValidators() – for an empty schema the implementation already returns an empty list.
        List<JsonValidator> result = schema.getValidators();
        // Verify the list is unmodifiable and empty.
        assertNotNull(result, "Result must not be null");
        assertTrue(result.isEmpty(), "Resulting validator list should be empty");
        assertThrows(UnsupportedOperationException.class, () -> result.add(null), "The empty validators list must be unmodifiable");
    }

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

    @Override
    public String toString() {
        return schemaNode.toString();
    }
}
